/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security;

import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
import com.sun.enterprise.security.common.AbstractSecurityContext;
import com.sun.enterprise.security.integration.AppServSecurityContext;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.UserNameAndPassword;
import org.jvnet.hk2.annotations.Service;

import static com.sun.enterprise.security.SecurityLoggerInfo.*;
import static java.util.logging.Level.SEVERE;

/**
 * This class that extends AbstractSecurityContext that gets stored in Thread Local Storage. If the current thread
 * creates child threads, the SecurityContext stored in the current thread is automatically propagated to the child
 * threads.
 * 
 * This class is used on the server side to represent the security context.
 * 
 * @see java.lang.ThreadLocal
 * @see java.lang.InheritableThreadLocal
 * 
 * @author Harish Prabandham
 * @author Harpreet Singh
 */
@Service
@PerLookup
public class SecurityContext extends AbstractSecurityContext {

    private static final long serialVersionUID = -1061816185561416857L;
    
    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();
    private static InheritableThreadLocal<SecurityContext> currentSecurityContext = new InheritableThreadLocal<>();
    private static SecurityContext defaultSecurityContext = generateDefaultSecurityContext();

    // Did the client log in as or did the server generate the context
    private boolean serverGeneratedCredentials;

    private Principal sessionPrincipal;
    
    // ### Static public methods
    
    
    /**
     * Initialize the SecurityContext and handle the unauthenticated principal case
     */
    public static SecurityContext init() {
        SecurityContext securityContext = currentSecurityContext.get();
        if (securityContext == null) { // there is no current security context...
            securityContext = defaultSecurityContext;
        }
        
        return securityContext;
    }

    public static SecurityContext getDefaultSecurityContext() {
        // Unauthenticated Security Context.
        return defaultSecurityContext;
    }

    public static Subject getDefaultSubject() {
        // Subject of unauthenticated Security Context.
        return defaultSecurityContext.subject;
    }

    // Get caller principal of unauthenticated Security Context
    public static Principal getDefaultCallerPrincipal() {
        synchronized (SecurityContext.class) {
            if (defaultSecurityContext.callerPrincipal == null) {
                String guestUser = null;
                try {
                    SecurityService securityService =
                            SecurityServicesUtil
                                    .getInstance()
                                    .getHabitat()
                                    .getService(
                                            SecurityService.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);

                    if (securityService == null) {
                        return null;
                    }
                    
                    guestUser = securityService.getDefaultPrincipal();
                } catch (Exception e) {
                    LOGGER.log(SEVERE, defaultUserLoginError, e);
                } finally {
                    if (guestUser == null) {
                        guestUser = "ANONYMOUS";
                    }
                }

                defaultSecurityContext.callerPrincipal = new UserNameAndPassword(guestUser);
            }
        }
        return defaultSecurityContext.callerPrincipal;
    }

    /**
     * No need to unmarshall the unauthenticated principal....
     */
    public static void reset(SecurityContext securityContext) {
        setCurrent(securityContext);
    }

    /**
     * This method gets the SecurityContext stored in the Thread Local Store (TLS) of the current thread.
     * 
     * @return The current Security Context stored in TLS. It returns null if SecurityContext could not be found in the
     * current thread.
     */
    public static SecurityContext getCurrent() {
        SecurityContext securityContext = currentSecurityContext.get();
        if (securityContext == null) {
            securityContext = defaultSecurityContext;
        }
        
        return securityContext;
    }

    /**
     * This method sets the SecurityContext stored in the TLS.
     * 
     * @param securityContext The Security Context that should be stored in TLS. This public static method needs to be protected such
     * that it can only be called by container code. Otherwise it can be called by application code to set its subject
     * (which the EJB security manager will use to create a domain combiner, and then everything the ejb does will be run as
     * the corresponding subject.
     */
    public static void setCurrent(SecurityContext securityContext) {
        if (securityContext == null || securityContext == defaultSecurityContext) {
            currentSecurityContext.set(securityContext);
            return;
        }

        SecurityContext current = currentSecurityContext.get();
        if (securityContext == current) {
            return;
        }

        currentSecurityContext.set(securityContext);
    }

    public static void setUnauthenticatedContext() {
        currentSecurityContext.set(defaultSecurityContext);
    }

    
    
    // ### Public instance methods
    

    /*
     * This creates a new SecurityContext object. Note: that the docs for Subject state that the internal sets (eg. the
     * principal set) cannot be modified unless the caller has the modifyPrincipals AuthPermission. That said, there may be
     * some value to setting the Subject read only. Note: changing the principals in the embedded subject (after
     * construction will likely cause problem in the principal set keyed HashMaps of EJBSecurityManager.
     * 
     * @param username The name of the user/caller principal.
     * 
     * @param subject contains the authenticated principals and credential.
     */
    public SecurityContext(String username, Subject subject) {
        Subject localSubject = nullSafeSubject(subject);
        
        this.callerPrincipal = new UserNameAndPassword(username);
        localSubject.getPrincipals().add(callerPrincipal);
        this.subject = localSubject;
    }

    /**
     * Create a SecurityContext with the given subject having a DistinguishedPrincipalCredential. 
     * 
     * <p>
     * This is used for a JASPIC environment.
     * 
     * @param subject
     */
    public SecurityContext(Subject subject) {
        Subject localSubject = nullSafeSubject(subject);

        this.subject = localSubject;

        Principal principal = null;
        for (Object publicCredential : subject.getPublicCredentials()) {
            if (publicCredential instanceof DistinguishedPrincipalCredential distinguishedPrincipalCredential) {
                principal = distinguishedPrincipalCredential.principal();
                break;
            }
        }

        if (principal == null) {
            for (Principal publicCredential : subject.getPrincipals()) {
                if (publicCredential instanceof DistinguishedPrincipalCredential distinguishedPrincipalCredential) {
                    principal = distinguishedPrincipalCredential.principal();
                    break;
                }
            }
        }

        // For old auth module
        if (principal == null) {
            Iterator<Principal> prinIter = subject.getPrincipals().iterator();
            if (prinIter.hasNext()) {
                principal = prinIter.next();
            }
        }

        this.callerPrincipal = principal;


        postConstruct();
    }

    public SecurityContext(String username, Subject subject, String realm) {
        Subject localSubject = nullSafeSubject(subject);
        
        PrincipalGroupFactory factory = Globals.get(PrincipalGroupFactory.class);
        if (factory != null) {
            callerPrincipal = factory.getPrincipalInstance(username, realm);
        }

        localSubject.getPrincipals().add(callerPrincipal);
        this.subject = localSubject;
    }

    /*
     * private constructor for constructing default security context
     */
    public SecurityContext() {
        LOGGER.fine("Default CTOR of SecurityContext called");
        
        this.subject = new Subject();
        
        // Delay assignment of caller principal until it is requested
        this.callerPrincipal = null;
        setServerGeneratedCredentials();
        
        // Read only is only done for guest logins.
        subject.setReadOnly();
    }



    public boolean didServerGenerateCredentials() {
        return serverGeneratedCredentials;
    }

    /**
     * This method returns the caller principal. This information may be redundant since the same information can be
     * inferred by inspecting the Credentials of the caller.
     * 
     * @return The caller Principal.
     */
    @Override
    public Principal getCallerPrincipal() {
        return this == defaultSecurityContext ? getDefaultCallerPrincipal() : callerPrincipal;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    public Principal getSessionPrincipal() {
        return sessionPrincipal;
    }

    public void setSessionPrincipal(Principal sessionPrincipal) {
        this.sessionPrincipal = sessionPrincipal;
    }

    public Set<Principal> getPrincipalSet() {
        return subject.getPrincipals();
    }

    public void postConstruct() {
        initDefaultCallerPrincipal();
    }

    @Override
    public AppServSecurityContext newInstance(String username, Subject subject, String realm) {
        LOGGER.fine("SecurityContext: newInstance method called");
        return new SecurityContext(username, subject, realm);
    }

    @Override
    public AppServSecurityContext newInstance(String username, Subject subject) {
        LOGGER.fine("SecurityContext: newInstance method called");
        return new SecurityContext(username, subject);
    }

    @Override
    public void setCurrentSecurityContext(AppServSecurityContext context) {
        LOGGER.fine("SecurityContext: setCurrentSecurityContext method called");
        
        if (context == null) {
            setCurrent(null);
            return;
        }
        
        if (context instanceof SecurityContext) {
            setCurrent((SecurityContext) context);
            return;
        }
        
        throw new IllegalArgumentException("Expected SecurityContext, found " + context);
    }

    @Override
    public AppServSecurityContext getCurrentSecurityContext() {
        LOGGER.fine("SecurityContext: getCurrent() method called");
        return getCurrent();
    }

    @Override
    public void setUnauthenticatedSecurityContext() {
        LOGGER.fine("SecurityContext: setUnauthenticatedSecurityContext method called");
        setUnauthenticatedContext();
    }

    @Override
    public void setSecurityContextWithPrincipal(Principal principal) {
        setCurrent(getSecurityContextForPrincipal(principal));
    }
    
    @Override
    public String toString() {
        return "SecurityContext[ " + "Initiator: " + callerPrincipal + "Subject " + subject + " ]";
    }
    
    
    
    // ### Private methods

    private static SecurityContext generateDefaultSecurityContext() {
        synchronized (SecurityContext.class) {
            try {
                return new SecurityContext();
            } catch (Exception e) {
                LOGGER.log(SEVERE, defaultSecurityContextError, e);
            }
        }

        return null;
    }
    
    private Subject nullSafeSubject(Subject subject) {
        if (subject == null) {
            subject = new Subject();
            LOGGER.warning(nullSubjectWarning);
        }
        
        return subject;
    }
    
    private void setServerGeneratedCredentials() {
        serverGeneratedCredentials = true;
    }
    
    private void initDefaultCallerPrincipal() {
        if (callerPrincipal == null) {
            callerPrincipal = getDefaultCallerPrincipal();
        }
    }

    private SecurityContext getSecurityContextForPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        if (principal instanceof SecurityContextProxy) {
            return ((SecurityContextProxy) principal).getSecurityContext();
        }


        Subject subject = new Subject();
        subject.getPrincipals().add(principal);

        return new SecurityContext(principal.getName(), subject);
    }

}
