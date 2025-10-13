/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010, 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
// Portions Copyright 2024-2025 Payara Foundation and/or affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license.

package com.sun.enterprise.connectors.work.context;

import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.connectors.work.LogFacade;

import org.glassfish.epicyro.config.helper.Caller;
import org.glassfish.epicyro.config.helper.CallerPrincipal;
import org.glassfish.security.common.UserNameAndPassword;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.UserPrincipal;


import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Principal;

/**
 * Connector callback handler to intercept the callbacks provided by the work instance
 * in order to map the security credentials between container and EIS domain
 *
 * @author Jagadish Ramu
 * @since GlassFish v3
 */
//TODO V3 need contract based handlers for individual callbacks ?
public class ConnectorCallbackHandler implements CallbackHandler {

    private static final Logger logger = LogFacade.getLogger();

    public static final List<String> supportedCallbacks = new ArrayList<String>();

    static {
        supportedCallbacks.add(GroupPrincipalCallback.class.getName());
        supportedCallbacks.add(CallerPrincipalCallback.class.getName());
    }

    private CallbackHandler handler;
    private boolean needMapping;
    private Map securityMap;
    private Subject executionSubject;

    public ConnectorCallbackHandler(Subject executionSubject, CallbackHandler handler, Map securityMap) {
        this.handler = handler;
        if (securityMap != null && securityMap.size() > 0) {
            needMapping = true;
            if(logger.isLoggable(Level.FINEST)){
                logger.finest("translation required for security info ");
            }
        } else {
            if(logger.isLoggable(Level.FINEST)){
                logger.finest("no translation required for security info ");
            }
        }
        this.executionSubject = executionSubject;
        this.securityMap = securityMap;
    }

    @LogMessageInfo(
            message = "Unsupported callback {0} during credential mapping.",
            comment = "Unsupported callback class.",
            level = "WARNING",
            cause = "Resource adapter has used a callback that is not supported by application server.",
            action = "Check whether the callback in question is supported by application server.",
            publish = true)
    private static final String RAR_UNSUPPORT_CALLBACK = "AS-RAR-05012";
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        Callback[] mappedCallbacks = callbacks;
        if (callbacks != null) {
            List<Callback> asCallbacks = new ArrayList<Callback>();

            boolean hasCallerPrincipalCallback = hasCallerPrincipalCallback(callbacks);

            if (needMapping) {
                for (Callback callback : callbacks) {
                    boolean callbackSupported = false;
                    for (String supportedCallback : supportedCallbacks) {
                        try {
                            //TODO V3 what if there is a callback impl that implements multiple callbacks ?
                            if (Class.forName(supportedCallback).isAssignableFrom(callback.getClass())) {
                                callbackSupported = true;
                                asCallbacks.add(handleSupportedCallback(callback));
                            }
                        } catch (ClassNotFoundException cnfe) {
                            if(logger.isLoggable(Level.FINEST)){
                                logger.log(Level.FINEST, "class not found", cnfe);
                            }
                        }
                    }
                    if (!callbackSupported) {
                        UnsupportedCallbackException uce = new UnsupportedCallbackException(callback);
                        logger.log(Level.WARNING, RAR_UNSUPPORT_CALLBACK, new Object[]{callback.getClass().getName(), uce});
                        throw uce;
                    }
                }

                mappedCallbacks = new Callback[asCallbacks.size()];
                for (int i = 0; i < asCallbacks.size(); i++) {
                    mappedCallbacks[i] = asCallbacks.get(i);
                }
            }
            //TODO V3 what happens to multiple callbacks ?
            handler.handle(mappedCallbacks);

            processResults(mappedCallbacks, hasCallerPrincipalCallback);
        }
    }

    private boolean hasCallerPrincipalCallback(Callback[] callbacks) {
        if (callbacks != null) {
            for (Callback c : callbacks) {
                if (c instanceof CallerPrincipalCallback) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processResults(Callback[] mappedCallbacks, boolean hasCallerPrincipalCallback) {
        if (mappedCallbacks != null) {
            Subject s = new Subject();

            // Handle Single Principal as the caller identity
            if (!hasCallerPrincipalCallback) {
                Set<Principal> principals = executionSubject.getPrincipals();
                if (principals != null && principals.size() == 1) {
                    //process if there is only one principal
                    for (Principal p : principals) {
                        Principal mappedPrincipal = null;
                        if (needMapping) {
                            mappedPrincipal = getMappedPrincipal(p, null);
                        } else {
                            mappedPrincipal = p;
                        }

                        if (mappedPrincipal != null) {
                            s.getPrincipals().add(mappedPrincipal);
                        }
                    }
                    s.getPublicCredentials().addAll(executionSubject.getPublicCredentials());
                    s.getPrivateCredentials().addAll(executionSubject.getPrivateCredentials());
                }
            }

            //TODO V3 what happens for Public/Private Credentials of Mapped case (Case II) 
            for (Callback callback : mappedCallbacks) {
                if (callback instanceof CallerPrincipalCallback) {
                    CallerPrincipalCallback callerPrincipalCallback = (CallerPrincipalCallback) callback;

                    Caller caller = getCaller(callerPrincipalCallback.getSubject());
                    if (caller != null) {
                        Principal glassFishCallerPrincipal = getGlassFishCallerPrincipal(caller);

                        s.getPrincipals().add(glassFishCallerPrincipal);

                        for (String group : caller.getGroups()) {
                            s.getPrincipals().add(new Group(group));
                        }
                    }

                    copySubject(s, callerPrincipalCallback.getSubject());
                } else if (callback instanceof GroupPrincipalCallback) {
                    GroupPrincipalCallback groupPrincipalCallback = (GroupPrincipalCallback) callback;

                    copySubject(s, groupPrincipalCallback.getSubject());
                } else if (callback instanceof PasswordValidationCallback) {
                    PasswordValidationCallback passwordValidationCallback = (PasswordValidationCallback) callback;

                    copySubject(s, passwordValidationCallback.getSubject());
                }
            }
            SecurityContext.setCurrent(new SecurityContext(s));
        }
    }

    private Caller getCaller(Subject subject) {
        Set<Caller> callers = subject.getPrincipals(Caller.class);
        if (callers.isEmpty()) {
            return null;
        }

        return callers.iterator().next();
    }

    private void copySubject(Subject target, Subject source) {
        target.getPrincipals().addAll(source.getPrincipals());
        target.getPublicCredentials().addAll(source.getPublicCredentials());
        target.getPrivateCredentials().addAll(source.getPrivateCredentials());
    }

    private Principal getGlassFishCallerPrincipal(Caller caller) {
        Principal callerPrincipal = caller.getCallerPrincipal();

        // Check custom principal
        if (!(callerPrincipal instanceof CallerPrincipal)) {
            return callerPrincipal;
        }

        // Check anonymous principal
        if (callerPrincipal.getName() == null) {
            return SecurityContext.getDefaultCallerPrincipal();
        }

        return new UserNameAndPassword(callerPrincipal.getName());
    }

    private Callback handleSupportedCallback(Callback callback) throws UnsupportedCallbackException {
        /* TODO V3 need to merge the principals/maps after calling all the callbacks and then
           TODO V3 set the security context ? */
        if (callback instanceof CallerPrincipalCallback) {
            return handleCallerPrincipalCallbackWithMapping((CallerPrincipalCallback) callback);
        } else if (callback instanceof GroupPrincipalCallback) {
            return handleGroupPrincipalCallbackWithMapping((GroupPrincipalCallback) callback);
        } else {
            throw new UnsupportedCallbackException(callback);
        }
    }

    private Callback handleGroupPrincipalCallbackWithMapping(GroupPrincipalCallback gpc) {

        String[] groups = gpc.getGroups();
        List<String> asGroupNames = new ArrayList<String>();

        for (String groupName : groups) {
            Group mappedGroup = (Group) securityMap.get(new Group(groupName));
            if (mappedGroup != null) {
                if(logger.isLoggable(Level.FINEST)){
                    logger.finest("got mapped group as [" + groupName + "] for eis-group [" + mappedGroup.getName() + "]");
                }
                asGroupNames.add(mappedGroup.getName());
            }
        }

        String[] asGroupsString = new String[asGroupNames.size()];
        for (int i = 0; i < asGroupNames.size(); i++) {
            asGroupsString[i] = asGroupNames.get(i);
        }
        return new GroupPrincipalCallback(gpc.getSubject(), asGroupsString);

        //SecurityContext.setCurrent(new SecurityContext(gpc.getSubject()));
    }

    public Callback handleCallerPrincipalCallbackWithMapping(CallerPrincipalCallback cpc) {

        CallerPrincipalCallback asCPC;

        Principal eisPrincipal = cpc.getPrincipal();
        String eisName = cpc.getName();

        Principal asPrincipal = getMappedPrincipal(eisPrincipal, eisName);

        asCPC = new CallerPrincipalCallback(cpc.getSubject(), asPrincipal);

        return asCPC;
/*
        Set<Principal> principals = cpc.getSubject().getPrincipals();
        for (Principal p : principals) {
            Principal mappedPrincipal = (Principal) securityMap.get(p);
            if (mappedPrincipal != null) {
                DistinguishedPrincipalCredential dpc = new DistinguishedPrincipalCredential(mappedPrincipal);
                cpc.getSubject().getPublicCredentials().add(dpc);
            }
        }
        SecurityContext.setCurrent(new SecurityContext(cpc.getSubject()));
*/
    }

    private Principal getMappedPrincipal(Principal eisPrincipal, String eisName) {
        Principal asPrincipal = null;
        if (eisPrincipal != null) {
            asPrincipal = (UserPrincipal) securityMap.get(eisPrincipal);
            if(logger.isLoggable(Level.FINEST)){
                logger.finest("got mapped principal as [" + asPrincipal + "] for eis-group [" + eisPrincipal.getName() + "]");
            }
        } else if (eisName != null) {
            asPrincipal = ((UserPrincipal) securityMap.get(new UserNameAndPassword(eisName)));
            if(logger.isLoggable(Level.FINEST)){
                logger.finest("got mapped principal as [" + asPrincipal + "] for eis-group [" + eisName + "]");
            }
        }
        return asPrincipal;
    }

}
