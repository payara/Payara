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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.web.server;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.enterprise.web.WebModule;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.naming.NamingException;
import javax.validation.ValidatorFactory;
import java.lang.String;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
//END OF IASRI 4660742

/**
 * This class implements the Tomcat ContainerListener interface and
 * handles Context and Session related events.
 * @author Tony Ng
 */
public final class WebContainerListener 
    implements ContainerListener {

    private static final Logger _logger = com.sun.enterprise.web.WebContainer.logger;

    private static final ResourceBundle rb = _logger.getResourceBundle();

    @LogMessageInfo(
            message = "ContainerEvent: {0}",
            level = "FINEST")
    public static final String CONTAINER_EVENT = "AS-WEB-GLUE-00270";

    @LogMessageInfo(
            message = "Exception during invocation of InjectionManager.destroyManagedObject on {0} of web module {1}",
            level = "SEVERE",
            cause = "An exception occurred during destroyManagedObject",
            action = "Check the exception for the error")
    public static final String EXCEPTION_DURING_DESTROY_MANAGED_OBJECT = "AS-WEB-GLUE-00271";

    @LogMessageInfo(
        message = "Exception getting Validator Factory from JNDI: {0}",
        level = "WARNING")
    public static final String EXCEPTION_GETTING_VALIDATOR_FACTORY = "AS-WEB-GLUE-00285";

    static private HashSet<String> beforeEvents = new HashSet<String>();
    static private HashSet<String> afterEvents = new HashSet<String>();

    static {
        // preInvoke events
        beforeEvents.add(ContainerEvent.BEFORE_CONTEXT_INITIALIZED);
        beforeEvents.add(ContainerEvent.BEFORE_CONTEXT_DESTROYED);
        beforeEvents.add(ContainerEvent.BEFORE_CONTEXT_ATTRIBUTE_ADDED);
        beforeEvents.add(ContainerEvent.BEFORE_CONTEXT_ATTRIBUTE_REMOVED);
        beforeEvents.add(ContainerEvent.BEFORE_CONTEXT_ATTRIBUTE_REPLACED);
        beforeEvents.add(ContainerEvent.BEFORE_REQUEST_INITIALIZED);
        beforeEvents.add(ContainerEvent.BEFORE_REQUEST_DESTROYED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_CREATED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_DESTROYED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_ID_CHANGED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_ATTRIBUTE_ADDED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_ATTRIBUTE_REMOVED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_ATTRIBUTE_REPLACED);
        beforeEvents.add(ContainerEvent.BEFORE_SESSION_VALUE_UNBOUND);
        beforeEvents.add(ContainerEvent.BEFORE_FILTER_INITIALIZED);
        beforeEvents.add(ContainerEvent.BEFORE_FILTER_DESTROYED);
        beforeEvents.add(ContainerEvent.BEFORE_UPGRADE_HANDLER_INITIALIZED);
        beforeEvents.add(ContainerEvent.BEFORE_UPGRADE_HANDLER_DESTROYED);
        beforeEvents.add(ContainerEvent.BEFORE_READ_LISTENER_ON_DATA_AVAILABLE);
        beforeEvents.add(ContainerEvent.BEFORE_READ_LISTENER_ON_ALL_DATA_READ);
        beforeEvents.add(ContainerEvent.BEFORE_READ_LISTENER_ON_ERROR);
        beforeEvents.add(ContainerEvent.BEFORE_WRITE_LISTENER_ON_WRITE_POSSIBLE);
        beforeEvents.add(ContainerEvent.BEFORE_WRITE_LISTENER_ON_ERROR);

        // postInvoke events
        afterEvents.add(ContainerEvent.AFTER_CONTEXT_INITIALIZED);
        afterEvents.add(ContainerEvent.AFTER_CONTEXT_DESTROYED);
        afterEvents.add(ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_ADDED);
        afterEvents.add(ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_REMOVED);
        afterEvents.add(ContainerEvent.AFTER_CONTEXT_ATTRIBUTE_REPLACED);
        afterEvents.add(ContainerEvent.AFTER_REQUEST_INITIALIZED);
        afterEvents.add(ContainerEvent.AFTER_REQUEST_DESTROYED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_CREATED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_DESTROYED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_ID_CHANGED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_ATTRIBUTE_ADDED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_ATTRIBUTE_REMOVED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_ATTRIBUTE_REPLACED);
        afterEvents.add(ContainerEvent.AFTER_SESSION_VALUE_UNBOUND);
        afterEvents.add(ContainerEvent.AFTER_FILTER_INITIALIZED);
        afterEvents.add(ContainerEvent.AFTER_FILTER_DESTROYED);
        afterEvents.add(ContainerEvent.AFTER_UPGRADE_HANDLER_INITIALIZED);
        afterEvents.add(ContainerEvent.AFTER_UPGRADE_HANDLER_DESTROYED);
        afterEvents.add(ContainerEvent.AFTER_READ_LISTENER_ON_DATA_AVAILABLE);
        afterEvents.add(ContainerEvent.AFTER_READ_LISTENER_ON_ALL_DATA_READ);
        afterEvents.add(ContainerEvent.AFTER_READ_LISTENER_ON_ERROR);
        afterEvents.add(ContainerEvent.AFTER_WRITE_LISTENER_ON_WRITE_POSSIBLE);
        afterEvents.add(ContainerEvent.AFTER_WRITE_LISTENER_ON_ERROR);

    }

    private InvocationManager invocationMgr;
    private InjectionManager injectionMgr;
    private NamedNamingObjectProxy validationNamingProxy;

    public WebContainerListener(InvocationManager invocationMgr,
                                InjectionManager injectionMgr,
                                NamedNamingObjectProxy validationNamingProxy) {
        this.invocationMgr = invocationMgr;
        this.injectionMgr = injectionMgr;
        this.validationNamingProxy = validationNamingProxy;
    }

    public void containerEvent(ContainerEvent event) {
        if(_logger.isLoggable(Level.FINEST)) {
	    _logger.log(Level.FINEST, CONTAINER_EVENT,
                        event.getType() + "," +
                        event.getContainer() + "," +
                        event.getData());
        }

        String type = event.getType();

        try {
            WebModule wm = (WebModule) event.getContainer();
            if (beforeEvents.contains(type)) {
                preInvoke(wm);

                if ( type.equals(ContainerEvent.BEFORE_CONTEXT_DESTROYED ) ) {
                    try {
                        // must close the validator factory
                        if ( validationNamingProxy != null ) {
                            Object validatorFactory = validationNamingProxy.handle("java:comp/ValidatorFactory");
                            if (validatorFactory != null) {
                                ((ValidatorFactory)validatorFactory).close();
                            }
                        }
                    } catch (NamingException exc) {
                        if(_logger.isLoggable(Level.WARNING)) {
                            _logger.log(Level.FINEST, EXCEPTION_GETTING_VALIDATOR_FACTORY, exc );
                        }
                    }
                }
            } else if (afterEvents.contains(type)) {
                if (type.equals(ContainerEvent.AFTER_FILTER_DESTROYED) ||
                        type.equals(ContainerEvent.AFTER_CONTEXT_DESTROYED)) {
                    preDestroy(event);
                }
                postInvoke(wm);
            } else if (ContainerEvent.PRE_DESTROY.equals(type)) {
                preInvoke(wm);
                preDestroy(event);
                postInvoke(wm);
            }
        } catch (Throwable t) {
            String msg = rb.getString(J2EEInstanceListener.EXCEPTION_DURING_HANDLE_EVENT);
            msg = MessageFormat.format(msg,
                new Object[] { type, event.getContainer() });
            _logger.log(Level.SEVERE, msg, t);
        }
    }

    private void preInvoke(WebModule ctx) {
        WebModule wm = (WebModule)ctx;
        ComponentInvocation inv = new WebComponentInvocation(wm);
        invocationMgr.preInvoke(inv);
    }

    private void postInvoke(WebModule ctx) {
        WebModule wm = (WebModule)ctx;
        ComponentInvocation inv = new WebComponentInvocation(wm);
        invocationMgr.postInvoke(inv);
    }

    /**
     * Invokes preDestroy on the instance embedded in the given ContainerEvent.
     *
     * @param event The ContainerEvent to process
     */
    private void preDestroy(ContainerEvent event) {
        try {
            injectionMgr.destroyManagedObject(event.getData(), false);
        } catch (Throwable t) {
            String msg = rb.getString(EXCEPTION_DURING_DESTROY_MANAGED_OBJECT);
            msg = MessageFormat.format(msg,
                new Object[] { event.getData(), event.getContainer() });
            _logger.log(Level.SEVERE, msg, t);
        }
    }
}
