/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2021-2022 Payara Foundation and/or its affiliates

package fish.payara.web;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.security.integration.AppServSecurityContext;
import com.sun.enterprise.security.integration.SecurityConstants;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.enterprise.web.WebModule;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.web.LogFacade;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reimplementation of the J2EEInstanceListener class (see Payara 5.x sources) which previously handled the
 * INIT, DESTROY, SERVICE, and FILTER events that got fired by Tomcat during request processing.
 *
 * Specifically this class looks to associate servlet requests with the Payara TransactionManager, link up the
 * Tomcat Catalina Realm with the {@link AppServSecurityContext}, and ensure managed objects are destroyed in Weld.
 */
public class WebModuleValve extends ValveBase {

    private static final Logger LOGGER = LogFacade.getLogger();
    private static final ResourceBundle RESOURCE_BUNDLE = LOGGER.getResourceBundle();

    private static final javax.security.auth.AuthPermission doAsPrivilegedPerm =
            new javax.security.auth.AuthPermission("doAsPrivileged");

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private JavaEETransactionManager transactionManager;

    @Inject
    private InjectionManager injectionManager;

    private WebModule webModule;

    public WebModuleValve() {
        this(null);
    }

    public WebModuleValve(WebModule webModule) {
        super(true);
        this.webModule = webModule;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();

        // Backup
        if (webModule == null) {
            // The container should be StandardWrapper, whose parent should be WebModule (StandardContext) - if it's not
            // panic and exit out
            Container parentContainer = getContainer().getParent();
            if (parentContainer instanceof WebModule) {
                webModule = (WebModule) getContainer().getParent();
            } else {
                throw new IllegalStateException("Expected parent container of WebModuleValve to be WebModule but was " +
                        parentContainer.getClass().getName());
            }
        }

        // Look up and services if they haven't been injected
        if (invocationManager == null || injectionManager == null || transactionManager == null) {
            ServerContext serverContext = getServerContext();
            ServiceLocator services = serverContext.getDefaultServices();
            invocationManager = services.getService(InvocationManager.class);
            transactionManager = getJavaEETransactionManager(services);
            injectionManager = services.getService(InjectionManager.class);
        }
    }

    private ServerContext getServerContext() throws IllegalStateException {
        ServerContext serverContext = webModule.getServerContext();
        if (serverContext == null) {
            String msg = RESOURCE_BUNDLE.getString(LogFacade.NO_SERVER_CONTEXT);
            msg = MessageFormat.format(msg, webModule.getName());
            throw new IllegalStateException(msg);
        }

        return serverContext;
    }

    private JavaEETransactionManager getJavaEETransactionManager(ServiceLocator services) {
        JavaEETransactionManager tm = null;
        ServiceHandle<JavaEETransactionManager> inhabitant = services.getServiceHandle(JavaEETransactionManager.class);
        if (inhabitant != null && inhabitant.isActive()) {
            tm = inhabitant.getService();
        }

        return tm;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        beforeEvents(request);
        try {
            getNext().invoke(request, response);
        } finally {
            afterEvents(request, response);
        }
    }

    private void beforeEvents(Request request) {
        Realm realm = webModule.getRealm();
        if (realm != null) {
            HttpServletRequest httpServletRequest = request.getRequest();
            HttpServletRequest baseHttpServletRequest = httpServletRequest;
            Principal userPrincipal = httpServletRequest.getUserPrincipal();
            Principal basePrincipal = userPrincipal;

            while (userPrincipal != null) {
                if (baseHttpServletRequest instanceof ServletRequestWrapper) {
                    // Unwrap any wrappers to find the base object
                    ServletRequest servletRequest = ((ServletRequestWrapper) baseHttpServletRequest).getRequest();

                    if (servletRequest instanceof HttpServletRequest) {
                        baseHttpServletRequest = (HttpServletRequest) servletRequest;
                        continue;
                    }
                }

                basePrincipal = baseHttpServletRequest.getUserPrincipal();

                break;
            }

            if (userPrincipal != null && userPrincipal == basePrincipal && userPrincipal.getClass().getName()
                    .equals(SecurityConstants.WEB_PRINCIPAL_CLASS)) {
                setSecurityContextWithPrincipal(userPrincipal);
            } else if (userPrincipal != basePrincipal) {
                // The wrapper has overridden getUserPrincipal reject the request if the wrapper does not have
                // the necessary permission.
                checkObjectForDoAsPermission(httpServletRequest);
                setSecurityContextWithPrincipal(userPrincipal);
            }
        }

        // The container should be StandardWrapper, from which we can get the Servlet instance
        StandardWrapper standardWrapper = (StandardWrapper) getContainer();
        WebComponentInvocation webComponentInvocation = new WebComponentInvocation(webModule,
                standardWrapper.getServlet());
        try {
            invocationManager.preInvoke(webComponentInvocation);

            // Emit monitoring probe event
            webModule.beforeServiceEvent(standardWrapper.getName());

            // Enlist resources with TransactionManager for service method
            if (transactionManager != null) {
                transactionManager.enlistComponentResources();
            }
        } catch (Exception ex) {
            invocationManager.postInvoke(webComponentInvocation); // See CR 6920895
            String msg = RESOURCE_BUNDLE.getString(LogFacade.EXCEPTION_DURING_VALVE_EVENT);
            msg = MessageFormat.format(msg, webModule);
            throw new RuntimeException(msg, ex);
        }
    }

    private void setSecurityContextWithPrincipal(Principal principal) throws IllegalStateException {
        ServerContext serverContext = getServerContext();

        AppServSecurityContext securityContext = serverContext.getDefaultServices().getService(
                AppServSecurityContext.class);
        if (securityContext != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, LogFacade.SECURITY_CONTEXT_OBTAINED, securityContext);
            }

            securityContext.setSecurityContextWithPrincipal(principal);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, LogFacade.SECURITY_CONTEXT_FAILED);
            }
        }
    }



    private void checkObjectForDoAsPermission(final Object o) throws AccessControlException {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                ProtectionDomain protectionDomain = o.getClass().getProtectionDomain();
                Policy policy = Policy.getPolicy(); if (!policy.implies(protectionDomain, doAsPrivilegedPerm)) {
                    throw new AccessControlException("permission required to override getUserPrincipal",
                            doAsPrivilegedPerm);
                } return null;
            });
        }
    }

    private void afterEvents(Request request, Response response) {

    }

}
