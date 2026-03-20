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
// Portions Copyright 2016-2025 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license.

package com.sun.enterprise.security.ee.authorization;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityRoleMapperFactoryGen;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.audit.AuditManager;
import org.glassfish.security.common.Role;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.ee.authorization.cache.CachedPermission;
import com.sun.enterprise.security.ee.authorization.cache.CachedPermissionImpl;
import com.sun.enterprise.security.ee.authorization.cache.PermissionCache;
import com.sun.enterprise.security.ee.authorization.cache.PermissionCacheFactory;
import com.sun.enterprise.security.web.integration.GlassFishPrincipalMapper;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.logging.LogDomains;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyFactory;
import jakarta.security.jacc.WebResourcePermission;
import jakarta.security.jacc.WebUserDataPermission;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.glassfish.exousia.AuthorizationService;

import static com.sun.enterprise.security.ee.authorization.GlassFishToExousiaConverter.getConstraintsFromBundle;
import static com.sun.enterprise.security.ee.authorization.GlassFishToExousiaConverter.getSecurityRoleRefsFromBundle;
import static com.sun.enterprise.security.ee.authorization.cache.PermissionCacheFactory.createPermissionCache;
import static java.util.logging.Level.FINE;

/**
 * This class is the entry point for authorization decisions in the web container. It implements JACC,
 * the JSR 115 - JavaTM Authorization Contract for Containers. This class is a
 * companion class of EJBSecurityManager.
 *
 * <p>
 * All the authorization decisions required to allow access to a resource in the web container should happen
 * via this class.
 *
 * <p>
 * Note that according to the JACC specification, for the actual authorization decision we delegate our queries
 * to a JACC aware {@link Policy}, which is pluggable (can be replaced by the user).
 *
 * @author Jean-Francois Arcand
 * @author Harpreet Singh.
 * @author Ondro Mihalyi
 * @todo introduce a new class called AbstractSecurityManager. Move functionality from this class and EJBSecurityManager
 * class and extend this class from AbstractSecurityManager
 */
public class WebAuthorizationManagerService {

    private static final Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);

    /**
     * Request path. Copied from org.apache.catalina.Globals; Required to break dependence on WebTier of Security Module
     */
    public static final String CONSTRAINT_URI = "org.apache.catalina.CONSTRAINT_URI";

    private static final String RESOURCE = "hasResourcePermission";
    private static final String USERDATA = "hasUserDataPermission";
    private static final String EMPTY_STRING = "";

    // The context ID associated with this instance. This is the name of the application
    private final String contextId;

    private static final WebResourcePermission allResources = new WebResourcePermission("/*", (String) null);
    private static final WebUserDataPermission allConnections = new WebUserDataPermission("/*", null);
    private static Permission[] protoPerms = { allResources, allConnections };
    // The JACC policy provider. This is the pluggable lower level authorization module
    // to which this class delegates all authorization queries.
    protected Policy policy = PolicyProvider.getInstance();

    // permissions tied to unchecked permission cache, and used
    // to determine if the effective policy is grant all
    // WebUserData and WebResource permisions.
    private CachedPermission allResourcesCachedPermission;

    private CachedPermission allConnectionsCachedPermission;

    // Unchecked permission cache used by the CachedPermissions defined above.
    private PermissionCache uncheckedPermissionCache;

    private static Set<Principal> defaultPrincipalSet = SecurityContext.getDefaultSecurityContext().getPrincipalSet();

    private final boolean register;

    private static final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();
    private final AuthorizationService exousiaAuthorizationService;
    
    public WebAuthorizationManagerService(WebBundleDescriptor webBundleDescriptor, boolean register) throws PolicyContextException {
        this.register = register;
        this.contextId = SecurityUtil.getContextID(webBundleDescriptor);

        String appName = webBundleDescriptor.getApplication().getRegistrationName();
        SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory().setAppNameForContext(appName, contextId);

        initPermissionCache();

        webBundleDescriptor.getContextParametersSet()
                           .stream()
                           .filter(param -> param.getName().equals(PolicyConfigurationFactory.FACTORY_NAME))
                           .findAny()
                           .map(param -> loadFactory(webBundleDescriptor, param.getValue()))
                           .ifPresent(clazz -> installPolicyConfigurationFactory(webBundleDescriptor, clazz));

        webBundleDescriptor.getContextParametersSet()
                           .stream()
                           .filter(param -> param.getName().equals(PolicyFactory.FACTORY_NAME))
                           .findAny()
                           .map(param -> loadFactory(webBundleDescriptor, param.getValue()))
                           .ifPresent(clazz -> installPolicyFactory(webBundleDescriptor, clazz));

        exousiaAuthorizationService = new AuthorizationService(
            contextId,
            () -> SecurityContext.getCurrent().getSubject(),
            () -> new GlassFishPrincipalMapper(contextId));

        exousiaAuthorizationService.setConstrainedUriRequestAttribute(CONSTRAINT_URI);
        exousiaAuthorizationService.setRequestSupplier(contextId,
            () -> currentRequest.get());

        exousiaAuthorizationService.addConstraintsToPolicy(
            getConstraintsFromBundle(webBundleDescriptor),
            webBundleDescriptor.getRoles()
               .stream()
               .map(Role::getName)
               .collect(Collectors.toSet()),
            webBundleDescriptor.isDenyUncoveredHttpMethods(),
            getSecurityRoleRefsFromBundle(webBundleDescriptor));
    }

    /**
     * Returns true to indicate that a policy check was made and there were no constrained resources.
     *
     * when caching is disabled must always return false, which will ensure that policy is consulted to authorize each
     * request.
     */
    public boolean hasNoConstrainedResources() {
        boolean result = false;

        if (allResourcesCachedPermission != null && allConnectionsCachedPermission != null) {
            boolean x = allResourcesCachedPermission.checkPermission();
            boolean y = allConnectionsCachedPermission.checkPermission();
            result = x && y;
            if (result) {
                try {
                    AuthorizationService.setThreadContextId(contextId);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }

        return result;
    }

    public boolean permitAll(HttpServletRequest httpServletRequest) {
        setSecurityInfo(httpServletRequest);
        return exousiaAuthorizationService.checkWebResourcePermission(httpServletRequest, (Subject) null);
    }

    public String getContextID() {
        return contextId;
    }

    // fix for CR 6155144
    // used to get the policy context id. Also used by the RealmAdapter
    public static String getContextID(WebBundleDescriptor webBundleDescriptor) {
        return SecurityUtil.getContextID(webBundleDescriptor);
    }

    /**
     * Checks if for the given request and the given request URI and method are the target of any user-data-constraint with a
     * and whether any such constraint is already satisfied.
     *
     * <p>
     * if uri == null, determine if the connection characteristics of the request satisfy the applicable policy. If the uri is not
     * null, determine if the uri and Http method require a CONFIDENTIAL transport. The uri value does not include
     * the context path, and any colons occurring in the uri must be escaped.
     *
     * <p>
     * Note: this method is not intended to be called if the request is secure. It checks whether the resource can be
     * accessed over the current connection type (which is presumed to be insecure), and if an insecure connection type is
     * not permitted it checks if the resource can be accessed via a confidential transport.
     *
     * <p>
     * If the request is secure, the second check is skipped, and the proper result is returned (but that is not the
     * intended use model).
     *
     * @param httpServletRequest the request that may be redirected
     * @param uri the request URI (minus the context path) to check
     * @param httpMethod the request method to check
     *
     * @return 1 if access is permitted (as is or without SSL). -1 if the the access will be permitted after a redirect to
     * SSL. return 0 if access will be denied independent of whether a redirect to SSL is done.
     *
     */
    public int hasUserDataPermission(HttpServletRequest httpServletRequest, String uri, String httpMethod) {
        setSecurityInfo(httpServletRequest);

        boolean isGranted = false;
        if (uri == null) {
            isGranted = exousiaAuthorizationService.checkWebUserDataPermission(httpServletRequest);
        } else {
            isGranted = exousiaAuthorizationService.checkWebUserDataPermission(uri, httpMethod, httpServletRequest.isSecure());
        }

        int result = 0;

        if (isGranted) {
            result = 1;
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] hasUserDataPermission isGranted: {0}", isGranted);
        }

        // Audit the grant
        recordWebInvocation(httpServletRequest, USERDATA, isGranted);

        // Try to see if the caller would have access to the secure variant of this request.
        // If so, we can signal that a redirect is likely going to succeed.
        if (!isGranted && !httpServletRequest.isSecure()) {

            if (uri == null) {
                uri = getUriMinusContextPath(httpServletRequest);
                httpMethod = httpServletRequest.getMethod();
            }

            isGranted = exousiaAuthorizationService.checkWebUserDataPermission(uri, httpMethod, true, defaultPrincipalSet);

            if (isGranted) {
                result = -1;
            }
        }

        return result;
    }

    /**
     * Perform access control based on the <code>HttpServletRequest</code>. Return <code>true</code> if this constraint is satisfied
     * and processing should continue, or <code>false</code> otherwise.
     *
     * @return true is the resource is granted, false if denied
     */
    public boolean hasResourcePermission(HttpServletRequest httpServletRequest) {
        setSecurityInfo(httpServletRequest);
        SecurityContext.setCurrent(getSecurityContext(httpServletRequest.getUserPrincipal()));

        boolean isGranted = exousiaAuthorizationService.checkWebResourcePermission(httpServletRequest);

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] hasResource isGranted: {0}", isGranted);
            logger.log(FINE, "[Web-Security] hasResource perm: {0}", getUriMinusContextPath(httpServletRequest));
        }

        recordWebInvocation(httpServletRequest, RESOURCE, isGranted);

        return isGranted;
    }

    /**
     * Return <code>true</code> if the specified servletName has the specified security role, within the context of the
     * <code>WebRoleRefPermission</code>; otherwise return <code>false</code>.
     *
     * @param servletName the resource's name
     * @param role Security role to be checked
     * @param callerPrincipal Principal for whom the role is to be checked
     *
     * @return true is the resource is granted, false if denied
     */
    public boolean hasRoleRefPermission(String servletName, String role, Principal callerPrincipal) {
        boolean isGranted = exousiaAuthorizationService.checkWebRoleRefPermission(
            servletName,
            role,
            getSecurityContext(callerPrincipal).getSubject());

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] hasRoleRef perm: {0}", servletName + " " + role);
            logger.log(FINE, "[Web-Security] hasRoleRef isGranted: {0}", isGranted);
        }

        return isGranted;
    }

    public boolean linkPolicy(String linkedContextId, boolean lastInService) {
        return exousiaAuthorizationService.linkPolicy(linkedContextId, lastInService);
    }

    public static boolean linkPolicy(String contextId, String linkedContextId, boolean lastInService) {
        return AuthorizationService.linkPolicy(contextId, linkedContextId, lastInService);
    }

    public void commitPolicy() {
        exousiaAuthorizationService.commitPolicy();
    }

    public static void commitPolicy(String contextId) {
        AuthorizationService.commitPolicy(contextId);
    }

    public void refresh() {
        exousiaAuthorizationService.refresh();
    }

    public void deletePolicy() {
        exousiaAuthorizationService.deletePolicy();
    }

    public static void deletePolicy(String contextId) {
        AuthorizationService.deletePolicy(contextId);
    }

    public void setSecurityInfo(HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            currentRequest.set(httpRequest);
        }

        AuthorizationService.setThreadContextId(contextId);
    }

    public void onLogin(HttpServletRequest httpServletRequest) {
        this.setSecurityInfo(httpServletRequest);
    }

    public void onLogout() {
        this.resetSecurityInfo();
    }

    public void resetSecurityInfo() {
        currentRequest.remove();
        PolicyContext.setContextID(null);
    }

    /**
     * Analogous to destroy, except does not remove links from Policy Context, and does not remove context_id from role
     * mapper factory. Used to support Policy Changes that occur via ServletContextListener.
     *
     * @throws PolicyContextException
     */
    public void release() throws PolicyContextException {
        exousiaAuthorizationService.removeStatementsFromPolicy(null);

        PermissionCacheFactory.removePermissionCache(uncheckedPermissionCache);
        uncheckedPermissionCache = null;
    }

    public void destroy() throws PolicyContextException {
        exousiaAuthorizationService.refresh();
        exousiaAuthorizationService.destroy();

        PermissionCacheFactory.removePermissionCache(uncheckedPermissionCache);
        uncheckedPermissionCache = null;
        SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory().removeAppNameForContext(contextId);
    }


    // ### Private methods

    private Class<?> loadFactory(WebBundleDescriptor webBundleDescriptor, String factoryClassName) {
        try {
            return
                webBundleDescriptor.getApplicationClassLoader()
                                   .loadClass(factoryClassName);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void installPolicyConfigurationFactory(WebBundleDescriptor webBundleDescriptor, Class<?> factoryClass) {
        ClassLoader existing = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(webBundleDescriptor.getApplicationClassLoader());
            AuthorizationService.installPolicyConfigurationFactory(factoryClass);
        } finally {
            Thread.currentThread().setContextClassLoader(existing);
        }
    }

    private void installPolicyFactory(WebBundleDescriptor webBundleDescriptor, Class<?> factoryClass) {
        ClassLoader existing = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(webBundleDescriptor.getApplicationClassLoader());
            AuthorizationService.installPolicyFactory(factoryClass);
        } finally {
            Thread.currentThread().setContextClassLoader(existing);
        }
    }

    private void initPermissionCache() {
        if (uncheckedPermissionCache == null) {
            if (register) {
                uncheckedPermissionCache = createPermissionCache(contextId, protoPerms, null);
                allResourcesCachedPermission = new CachedPermissionImpl(uncheckedPermissionCache, allResources);
                allConnectionsCachedPermission = new CachedPermissionImpl(uncheckedPermissionCache, allConnections);
            }
        } else {
            uncheckedPermissionCache.reset();
        }
    }

    /**
     * This is an private method for transforming principal into a SecurityContext
     *
     * @param principal expected to be a WebPrincipal
     * @return SecurityContext
     */
    private SecurityContext getSecurityContext(Principal principal) {
        SecurityContext securityContext = null;

        if (principal != null) {
            if (principal instanceof WebPrincipal) {
                WebPrincipal webPrincipal = (WebPrincipal) principal;
                securityContext = webPrincipal.getSecurityContext();
            } else {
                securityContext = SecurityContext.getCurrent();
            }
        }

        if (securityContext == null) {
            securityContext = SecurityContext.getDefaultSecurityContext();
        }

        return securityContext;
    }

    private void recordWebInvocation(final HttpServletRequest httpServletRequest, final String type, final boolean isGranted) {
        AuditManager auditManager = SecurityServicesUtil.getInstance().getAuditManager();

        if (auditManager != null && auditManager.isAuditOn() && auditManager instanceof AppServerAuditManager appServerAuditManager) {
            Principal callerPrincipal = httpServletRequest.getUserPrincipal();
            String caller = callerPrincipal != null ? callerPrincipal.getName() : null;
            appServerAuditManager.webInvocation(caller, httpServletRequest, type, isGranted);
        }
    }

    private static String getUriMinusContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri == null) {
            return EMPTY_STRING;
        }

        String contextPath = request.getContextPath();
        int contextLength = contextPath == null ? 0 : contextPath.length();

        if (contextLength > 0) {
            uri = uri.substring(contextLength);
        }

        if (uri.equals("/")) {
            return EMPTY_STRING;
        }

        // Encode all colons
        return uri.replaceAll(":", "%3A");
    }

    public static HttpServletRequest getCurrentRequest() {
        return currentRequest.get();
    }
}
