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
import com.sun.enterprise.security.integration.RealmInitializer;
import com.sun.enterprise.security.integration.SecurityConstants;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.enterprise.web.WebModule;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.api.invocation.ComponentInvocation;
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
 * <p>
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

    private boolean servicesInitialised = false;

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

        // Look up services using ServiceLocator obtained from ServerContext if they haven't been injected
        if (invocationManager == null || injectionManager == null || transactionManager == null) {
            initialiseServices(false);
        } else {
            servicesInitialised = true;
        }
    }

    /**
     * Look up {@link InvocationManager}, {@link InjectionManager}, and {@link JavaEETransactionManager} using
     * {@link ServiceLocator} obtained from {@link ServerContext} if any of them have not already been injected or
     * looked up.
     *
     * @param throwException if an {@link IllegalStateException} should be thrown if a {@link ServerContext} could not
     *                       be obtained from the {@link WebModule}
     * @throws IllegalStateException if a {@link ServerContext} could not be obtained from the {@link WebModule} and
     *                               throwException parameter was set to true
     */
    private void initialiseServices(boolean throwException) throws IllegalStateException {
        ServiceLocator services;
        try {
            services = WebModuleGlueUtil.getServerContext(webModule).getDefaultServices();
        } catch (IllegalStateException illegalStateException) {
            if (throwException) {
                throw illegalStateException;
            } else {
                LOGGER.log(Level.FINE, illegalStateException.getMessage());
                return;
            }
        }

        if (invocationManager == null) {
            invocationManager = services.getService(InvocationManager.class);
        }

        if (transactionManager == null) {
            transactionManager = getJavaEETransactionManager(services);
        }

        if (injectionManager == null) {
            injectionManager = services.getService(InjectionManager.class);
        }

        if (invocationManager != null && injectionManager != null && transactionManager != null) {
            servicesInitialised = true;
        }
    }

    /**
     * Look up the {@link JavaEETransactionManager} using the given {@link ServiceLocator}.
     *
     * @param services the {@link ServiceLocator} to use to look up the {@link JavaEETransactionManager} service
     * @return the {@link JavaEETransactionManager} service, or null if an active one could not be found
     */
    private JavaEETransactionManager getJavaEETransactionManager(ServiceLocator services) {
        JavaEETransactionManager tm = null;
        ServiceHandle<JavaEETransactionManager> inhabitant = services.getServiceHandle(JavaEETransactionManager.class);
        if (inhabitant != null && inhabitant.isActive()) {
            tm = inhabitant.getService();
        }

        return tm;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, IllegalStateException, ServletException {
        // Double check we have everything we need and exit out if we don't
        if (!servicesInitialised) {
            initialiseServices(true);
        }

        // The container should be StandardWrapper, from which we can get the Servlet instance
        StandardWrapper standardWrapper = (StandardWrapper) getContainer();
        WebComponentInvocation webComponentInvocation = new WebComponentInvocation(webModule,
                standardWrapper.getServlet());

        preInvoke(request, webComponentInvocation);
        try {
            getNext().invoke(request, response);
        } finally {
            afterInvoke(response, webComponentInvocation);
        }
    }

    /**
     * Method that handles pre-processing of the {@link Request} as it moves through the
     * {@link org.apache.catalina.Pipeline}. This covers setting the {@link AppServSecurityContext} with the
     * {@link Principal} obtained from the {@link Request}, calling
     * {@link InvocationManager#preInvoke(ComponentInvocation)} using the provided {@link WebComponentInvocation},
     * and enlisting the component with the {@link JavaEETransactionManager}.
     *
     * @param request                The {@link Request} to pre-process.
     * @param webComponentInvocation The {@link WebComponentInvocation} used to call
     *                               {@link InvocationManager#preInvoke(ComponentInvocation)}.
     */
    private void preInvoke(Request request, WebComponentInvocation webComponentInvocation) {
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

        try {
            invocationManager.preInvoke(webComponentInvocation);

            // Emit monitoring probe event using name of the StandardWrapper
            StandardWrapper standardWrapper = (StandardWrapper) getContainer();
            webModule.beforeServiceEvent(standardWrapper.getName());

            // Enlist resources with TransactionManager for service method
            if (transactionManager != null) {
                transactionManager.enlistComponentResources();
            }
        } catch (Exception ex) {
            invocationManager.postInvoke(webComponentInvocation);
            String msg = RESOURCE_BUNDLE.getString(LogFacade.EXCEPTION_DURING_VALVE_EVENT);
            msg = MessageFormat.format(msg, webModule);
            throw new RuntimeException(msg, ex);
        }
    }

    /**
     * Retrieves the {@link AppServSecurityContext} service and sets the security context with the given
     * {@link Principal}.
     *
     * @param principal The {@link Principal} to set the security context with.
     * @throws IllegalStateException if a {@link ServerContext} to get a {@link ServiceLocator} from
     *                               could not be obtained from the {@link WebModule} this valve is attached to.
     */
    private void setSecurityContextWithPrincipal(Principal principal) throws IllegalStateException {
        ServerContext serverContext = WebModuleGlueUtil.getServerContext(webModule);

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

    /**
     * Check that the "doAsPrivileged" permission is granted for the given {@link Object}
     *
     * @param object the {@link Object} to check the "doAsPrivileged" permission is granted for
     * @throws AccessControlException if the {@link Object} does not have the "doAsPrivileged" permission granted
     */
    private void checkObjectForDoAsPermission(final Object object) throws AccessControlException {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                ProtectionDomain protectionDomain = object.getClass().getProtectionDomain();
                Policy policy = Policy.getPolicy();
                if (!policy.implies(protectionDomain, doAsPrivilegedPerm)) {
                    throw new AccessControlException("permission required to override getUserPrincipal",
                            doAsPrivilegedPerm);
                }
                return null;
            });
        }
    }

    /**
     * Method that handles post-processing of the {@link Response} as it moves through the
     * {@link org.apache.catalina.Pipeline}. This covers calling
     * {@link InvocationManager#postInvoke(ComponentInvocation)} using the provided {@link WebComponentInvocation},
     * clearing the {@link AppServSecurityContext}, and cleaning up any transactions in the
     * {@link JavaEETransactionManager}.
     *
     * @param response               The {@link Response} to post-process.
     * @param webComponentInvocation The {@link WebComponentInvocation} used to call
     *                               {@link InvocationManager#preInvoke(ComponentInvocation)}.
     */
    private void afterInvoke(Response response, WebComponentInvocation webComponentInvocation) {
        try {
            invocationManager.postInvoke(webComponentInvocation);
        } catch (Exception exception) {
            String msg = RESOURCE_BUNDLE.getString(LogFacade.EXCEPTION_DURING_VALVE_EVENT);
            msg = MessageFormat.format(msg, webModule);
            LOGGER.log(Level.SEVERE, msg, exception);
            throw exception;
        } finally {
            // Emit monitoring probe event
            ServletResponse servletResponse = response.getResponse();
            int status = -1;
            if (servletResponse != null && servletResponse instanceof HttpServletResponse) {
                status = ((HttpServletResponse) servletResponse).getStatus();
            }
            webModule.afterServiceEvent(getContainer().getName(), status);

            // Check it's the top level invocation
            if (invocationManager.getCurrentInvocation() == null) {
                try {
                    // Clear security context
                    Realm realm = webModule.getRealm();
                    if (realm != null && (realm instanceof RealmInitializer)) {
                        // Cleanup not only AppServSecurityContext but also PolicyContext
                        ((RealmInitializer) realm).logout();
                    }
                } catch (Exception exception) {
                    String msg = RESOURCE_BUNDLE.getString(LogFacade.EXCEPTION_DURING_VALVE_EVENT);
                    msg = MessageFormat.format(msg, webModule);
                    LOGGER.log(Level.SEVERE, msg, exception);
                }

                if (transactionManager != null) {
                    try {
                        if (transactionManager.getTransaction() != null) {
                            transactionManager.rollback();
                        }
                        transactionManager.cleanTxnTimeout();
                    } catch (Exception ex) {
                    }
                }
            }

            if (transactionManager != null) {
                // The container should be StandardWrapper, from which we can get the Servlet instance
                StandardWrapper standardWrapper = (StandardWrapper) getContainer();
                transactionManager.componentDestroyed(standardWrapper.getServlet(), webComponentInvocation);
            }
        }
    }
}
