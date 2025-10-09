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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.jacc;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.runtime.common.wls.SecurityRoleAssignment;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityRoleMapperFactoryGen;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.jacc.cache.CachedPermission;
import com.sun.enterprise.security.jacc.cache.CachedPermissionImpl;
import com.sun.enterprise.security.jacc.cache.PermissionCache;
import com.sun.enterprise.security.jacc.cache.PermissionCacheFactory;
import com.sun.enterprise.security.web.integration.WebPrincipal;
import com.sun.enterprise.security.web.integration.WebSecurityManagerFactory;
import com.sun.logging.LogDomains;

import fish.payara.jacc.JaccConfigurationFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.WebResourcePermission;
import jakarta.security.jacc.WebRoleRefPermission;
import jakarta.security.jacc.WebUserDataPermission;
import jakarta.servlet.http.HttpServletRequest;

import org.glassfish.exousia.AuthorizationService;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.security.common.Role;

import static com.sun.enterprise.security.common.AppservAccessController.privilegedException;
import java.util.HashSet;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import jakarta.security.enterprise.CallerPrincipal;
import static org.glassfish.api.web.Constants.ADMIN_VS;

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
public class JaccWebAuthorizationManager {

    private static final Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);

    /**
     * Request path. Copied from org.apache.catalina.Globals; Required to break dependence on WebTier of Security Module
     */
    public static final String CONSTRAINT_URI = "org.apache.catalina.CONSTRAINT_URI";

    private static final String RESOURCE = "hasResourcePermission";
    private static final String USERDATA = "hasUserDataPermission";
    private static final String EMPTY_STRING = "";

    private static final WebResourcePermission allResources = new WebResourcePermission("/*", (String) null);
    private static final WebUserDataPermission allConnections = new WebUserDataPermission("/*", null);
    private static Permission[] protoPerms = { allResources, allConnections };
    private static Set<Principal> defaultPrincipalSet = SecurityContext.getDefaultSecurityContext().getPrincipalSet();

    // The context ID associated with this instance. This is the name
    // of the application
    private final String CONTEXT_ID;
    private String CODEBASE;

    // The JACC policy provider. This is the pluggable lower level authorization module
    // to which this class delegates all authorization queries.
    protected Policy policy = AuthorizationService.getPolicy();
    protected PolicyConfigurationFactory policyConfigurationFactory;
    protected PolicyConfiguration policyConfiguration;
    protected CodeSource codesource;

    // protection domain cache
    private final Map<Set<Principal>, ProtectionDomain> protectionDomainCache = Collections.synchronizedMap(new WeakHashMap<>());

    // Permissions tied to unchecked permission cache, and used
    // to determine if the effective policy is grant all
    // WebUserData and WebResource permisions.
    private CachedPermission allResourcesCachedPermission;
    private CachedPermission allConnectionsCachedPermission;

    // Unchecked permission cache used by the CachedPermissions defined above.
    private PermissionCache uncheckedPermissionCache;

    private final WebSecurityManagerFactory webSecurityManagerFactory;
    private final ServerContext serverContext;

    private final WebBundleDescriptor webBundleDescriptor;

    // ProbeProvider
    private final WebSecurityDeployerProbeProvider probeProvider = new WebSecurityDeployerProbeProvider();
    private boolean register = true;

    public JaccWebAuthorizationManager(WebBundleDescriptor webBundleDescriptor, ServerContext serverContext, WebSecurityManagerFactory webSecurityManagerFactory, boolean register) throws PolicyContextException {
        this.register = register;
        this.webBundleDescriptor = webBundleDescriptor;
        this.CONTEXT_ID = getContextID(webBundleDescriptor);
        this.serverContext = serverContext;
        this.webSecurityManagerFactory = webSecurityManagerFactory;

        String appname = getAppId();
        SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory().setAppNameForContext(getAppId(), CONTEXT_ID);
        initialise(appname);
    }

    // fix for CR 6155144
    // used to get the policy context id. Also used by the RealmAdapter
    public static String getContextID(WebBundleDescriptor webBundleDescriptor) {
        return SecurityUtil.getContextID(webBundleDescriptor);
    }

    /**
     * This method returns true to indicate that a policy check was made and there were no constrained resources.
     *
     * <p>
     * When caching is disabled must always return false, which will ensure that policy is consulted to authorize
     * each request.
     *
     * @return true when there are no constrained resources, false otherwise
     */
    public boolean hasNoConstrainedResources() {
        boolean noConstrainedResources = false;

        if (allResourcesCachedPermission != null && allConnectionsCachedPermission != null) {
            boolean x = allResourcesCachedPermission.checkPermission();
            boolean y = allConnectionsCachedPermission.checkPermission();
            noConstrainedResources = x && y;

            if (noConstrainedResources) {
                try {
                    setPolicyContext(CONTEXT_ID);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }

        return noConstrainedResources;
    }

    /**
     * Checks if for the given request and the given request URI and method are the target of any user-data-constraint with a
     * and whether any such constraint is already satisfied.
     *
     * <p>
     * if uri == null, determine if the connection characteristics of the request satisfy the applicable policy. If the uri
     * is not null, determine if the uri and Http method require a CONFIDENTIAL transport. The uri value does not include
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
     * @param servletRequest the request that may be redirected
     * @param uri the request URI (minus the context path) to check
     * @param method the request method to check
     *
     * @return 1 if access is permitted (as is or without SSL). -1 if the the access will be permitted after a redirect to
     * SSL. return 0 if access will be denied independent of whether a redirect to SSL is done.
     *
     */
    public int hasUserDataPermission(HttpServletRequest servletRequest, String uri, String httpMethod) {
        setServletRequestForJACC(servletRequest);

        WebUserDataPermission dataPermission;
        boolean requestIsSecure = servletRequest.isSecure();
        if (uri == null) {
            dataPermission = new WebUserDataPermission(servletRequest);
        } else {
            dataPermission = new WebUserDataPermission(uri, httpMethod == null ? null : new String[] { httpMethod }, requestIsSecure ? "CONFIDENTIAL" : null);
        }

        boolean isGranted = checkPermission(dataPermission, defaultPrincipalSet);
        int result = 0;

        if (isGranted) {
            result = 1;
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] hasUserDataPermission permission: {0}", dataPermission);
            logger.log(FINE, "[Web-Security] hasUserDataPermission isGranted: {0}", isGranted);
        }

        // Audit the grant
        recordWebInvocation(servletRequest, USERDATA, isGranted);

        if (!isGranted && !requestIsSecure) {

            if (uri == null) {
                httpMethod = servletRequest.getMethod();
            }

            dataPermission = new WebUserDataPermission(dataPermission.getName(), httpMethod == null ? null : new String[] { httpMethod }, "CONFIDENTIAL");

            isGranted = checkPermission(dataPermission, defaultPrincipalSet);

            if (isGranted) {
                result = -1;
            }
        }

        return result;
    }

    public boolean isPermitAll(HttpServletRequest request) {
        boolean isPermitAll = false;

        WebResourcePermission webResourcePermission = createWebResourcePermission(request);

        if (uncheckedPermissionCache != null) {
            isPermitAll = uncheckedPermissionCache.checkPermission(webResourcePermission);
        }

        if (isPermitAll == false) {
            isPermitAll = checkPermissionWithoutCache(webResourcePermission, null);
        }

        return isPermitAll;
    }

    /**
     * Perform access control based on the <code>HttpServletRequest</code>. Return <code>true</code> if this constraint is
     * satisfied and processing should continue, or <code>false</code> otherwise.
     *
     * @return true is the resource is granted, false if denied
     */
    public boolean hasResourcePermission(HttpServletRequest servletRequest) {
        SecurityContext securityContect = getSecurityContext(servletRequest.getUserPrincipal());

        WebResourcePermission webResourcePermission = createWebResourcePermission(servletRequest);
        setServletRequestForJACC(servletRequest);

        boolean isGranted = checkPermission(webResourcePermission, securityContect.getPrincipalSet());

        SecurityContext.setCurrent(securityContect);

        if (logger.isLoggable(FINE)) {
            logger.log(Level.FINE, "[Web-Security] hasResource isGranted: {0}", isGranted);
            logger.log(Level.FINE, "[Web-Security] hasResource perm: {0}", webResourcePermission);
        }

        recordWebInvocation(servletRequest, RESOURCE, isGranted);

        return isGranted;
    }

    /**
     * Return <code>true</code> if the specified servletName has the specified security role, within the context of the
     * <code>WebRoleRefPermission</code>; otherwise return <code>false</code>.
     *
     * @param servletName the resource's name
     * @param role Security role to be checked
     * @param principal Principal for whom the role is to be checked
     *
     * @return true is the resource is granted, false if denied
     */
    public boolean hasRoleRefPermission(String servletName, String role, Principal principal) {
        WebRoleRefPermission requestedPermission = new WebRoleRefPermission(servletName, role);

        Set<Principal> principalSetFromSecurityContext = getSecurityContext(principal).getPrincipalSet();
        boolean isGranted = checkPermission(requestedPermission, principalSetFromSecurityContext);
        if (!isGranted) {
            isGranted = checkPermissionForModifiedPrincipalSet(principalSetFromSecurityContext, isGranted, requestedPermission);
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.log(FINE, "[Web-Security] hasRoleRef perm: {0}", requestedPermission);
            logger.log(FINE, "[Web-Security] hasRoleRef isGranted: {0}", isGranted);
        }

        return isGranted;
    }

    /* If the principal set contains CallerPrincipal, replace it with PrincipalImpl. 
       This is because CallerPrincipal isn't equal to PrincipalImpl and doesn't imply it.
       CallerPrincipal doesn't even implement equals method, so 2 CallerPrincipals with the same name are not equal. 
       Because CallerPrincipal is from Jakarta EE, we can't change it.
    */
    private boolean checkPermissionForModifiedPrincipalSet(Set<Principal> principalSetFromSecurityContext, boolean isGranted, WebRoleRefPermission requestedPermission) {
        boolean principalSetContainsCallerPrincipal = false;
        Set<Principal> modifiedPrincipalSet = new HashSet<Principal>(principalSetFromSecurityContext.size());
        for (Principal p : principalSetFromSecurityContext) {
            if (p instanceof CallerPrincipal) {
                principalSetContainsCallerPrincipal = true;
                modifiedPrincipalSet.add(new PrincipalImpl(p.getName()));
            } else {
                modifiedPrincipalSet.add(p);
            }
        }
        if (principalSetContainsCallerPrincipal) {
            isGranted = checkPermission(requestedPermission, modifiedPrincipalSet);
        }
        return isGranted;
    }

    /**
     * Analogous to destroy, except does not remove links from Policy Context, and does not remove context_id from role
     * mapper factory. Used to support Policy Changes that occur via ServletContextListener.
     *
     * @throws PolicyContextException
     */
    public void release() throws PolicyContextException {
        logger.config(() -> "release(); id of the context: " + CONTEXT_ID);
        boolean wasInService = getPolicyFactory().inService(CONTEXT_ID);
        PolicyConfiguration config = getPolicyFactory().getPolicyConfiguration(CONTEXT_ID, false);
        removePolicyStatements(config, webBundleDescriptor);

        // Refresh policy if the context was in service
        if (wasInService) {
            AuthorizationService.getPolicy().refresh();
        }

        PermissionCacheFactory.removePermissionCache(uncheckedPermissionCache);
        uncheckedPermissionCache = null;
        webSecurityManagerFactory.getManager(CONTEXT_ID, null, true);
    }

    public void destroy() throws PolicyContextException {
        logger.config(() -> "destroy(); id of the context: " + CONTEXT_ID);
        PolicyConfigurationFactory policyFactory = getPolicyFactory();

        boolean wasInService = policyFactory.inService(CONTEXT_ID);
        if (wasInService) {
            policy.refresh();
        }

        PermissionCacheFactory.removePermissionCache(uncheckedPermissionCache);
        uncheckedPermissionCache = null;
        SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory().removeAppNameForContext(CONTEXT_ID);

        if (policyFactory instanceof JaccConfigurationFactory) {
            ((JaccConfigurationFactory) policyFactory).removeContextProviderByPolicyContextId(CONTEXT_ID);
            ((JaccConfigurationFactory) policyFactory).removeContextIdMappingByPolicyContextId(CONTEXT_ID);
        }

        webSecurityManagerFactory.getManager(CONTEXT_ID, null, true);
    }


    /**
     * Initialise this class and specifically load permissions into the JACC Policy Configuration.
     *
     * @param appName
     * @throws PolicyContextException
     */
    private void initialise(String appName) throws PolicyContextException {
        logger.finest(() -> String.format("initialise(appName=%s)", appName));
        getPolicyFactory();
        CODEBASE = removeSpaces(CONTEXT_ID);

        if (ADMIN_VS.equals(getVirtualServers(appName))) {
            LoginConfiguration loginConfiguration = webBundleDescriptor.getLoginConfiguration();
            if (loginConfiguration != null) {
                String realmName = loginConfiguration.getRealmName();

                // Process mappings from sun-web.xml
                SunWebApp sunDes = webBundleDescriptor.getSunDescriptor();
                if (sunDes != null) {

                    SecurityRoleMapping[] roleMappings = sunDes.getSecurityRoleMapping();
                    if (roleMappings != null) {
                        for (SecurityRoleMapping roleMapping : roleMappings) {
                            for (String principal : roleMapping.getPrincipalName()) {
                                webSecurityManagerFactory.addAdminPrincipal(principal, realmName, new PrincipalImpl(principal));
                            }
                            for (String group : roleMapping.getGroupNames()) {
                                webSecurityManagerFactory.addAdminGroup(group, realmName, new Group(group));
                            }
                        }
                    }

                    SecurityRoleAssignment[] roleAssignments = sunDes.getSecurityRoleAssignments();
                    if (roleAssignments != null) {
                        for (SecurityRoleAssignment roleAssignment : roleAssignments) {
                            if (roleAssignment.isExternallyDefined()) {
                                webSecurityManagerFactory.addAdminGroup(roleAssignment.getRoleName(), realmName, new Group(roleAssignment.getRoleName()));
                                continue;
                            }

                            for (String principal : roleAssignment.getPrincipalNames()) {
                                webSecurityManagerFactory.addAdminPrincipal(principal, realmName, new PrincipalImpl(principal));
                            }
                        }
                    }
                }
            }
        }

        // Will require stuff in hash format for reference later on.
        try {
            try {
                logger.log(FINE, "[Web-Security] Creating a Codebase URI with = {0}", CODEBASE);

                URI uri = new URI("file:///" + CODEBASE);
                if (uri != null) {
                    codesource = new CodeSource(new URL(uri.toString()), (Certificate[]) null);
                }

            } catch (URISyntaxException use) {
                // Manually create the URL
                logger.log(FINE, "[Web-Security] Error Creating URI ", use);
                throw new RuntimeException(use);
            }

        } catch (MalformedURLException mue) {
            logger.log(SEVERE, "[Web-Security] Exception while getting the CodeSource", mue);
            throw new RuntimeException(mue);
        }

        logger.log(FINE, "[Web-Security] Context id (id under which  WEB component in application will be created) = {0}", CONTEXT_ID);
        logger.log(FINE, "[Web-Security] Codebase (module id for web component) {0}", CODEBASE);

        // Generate permissions and store these into the JACC policyConfiguration
        // The JACC Policy (to which we delegate) will use these permissions later to make authorization decisions.
        loadPermissionsInToPolicyConfiguration();

        if (uncheckedPermissionCache == null) {
            if (register) {
                uncheckedPermissionCache = PermissionCacheFactory.createPermissionCache(CONTEXT_ID, codesource, protoPerms, null);

                allResourcesCachedPermission = new CachedPermissionImpl(uncheckedPermissionCache, allResources);
                allConnectionsCachedPermission = new CachedPermissionImpl(uncheckedPermissionCache, allConnections);
            }
        } else {
            uncheckedPermissionCache.reset();
        }
    }

    private void loadPermissionsInToPolicyConfiguration() throws PolicyContextException {
        PolicyConfigurationFactory policyFactory = getPolicyFactory();

        // Only regenerate policy file if it isn't already in service.
        //
        // Consequently all things that deploy modules (as opposed to loading already deployed modules)
        // must make sure a pre-exiting PolicyConfiguration is either in deleted or open state before
        // this method (i.e. initialize) is called. That is, before constructing the WebSecurityManager.
        //
        // Note that policy statements are not removed to allow multiple web modules to be represented by
        // the same PolicyConfiguration.

        if (!policyFactory.inService(CONTEXT_ID)) {

            // Get the JACC PolicyConfiguration. If we are a single web application (with only one web module)
            // this will be still empty, otherwise it may already contain permissions.
            //
            // Note that the PolicyConfiguration is pluggable and can have been replaced by the user
            policyConfiguration = policyFactory.getPolicyConfiguration(CONTEXT_ID, false);
            try {

                // Translate the constraints in the webBundleDescriptor into permissions that will be stored
                // in the policyConfiguration.
                JaccWebConstraintsTranslator.translateConstraintsToPermissions(webBundleDescriptor, policyConfiguration);
            } catch (PolicyContextException pce) {
                logger.log(FINE, "[Web-Security] FATAL Permission Translation: " + pce.getMessage());
                throw pce;
            }
        }

    }

    private String removeSpaces(String withSpaces) {
        return withSpaces.replace(' ', '_');
    }



    // #### Other private methods

    // this will change too - get the application id name
    private String getAppId() {
        return webBundleDescriptor.getApplication().getRegistrationName();
    }

    /**
     * Invoke the <code>Policy</code> to determine if the <code>Permission</code> object has security permission.
     *
     * @param requestedPermission an instance of <code>Permission</code>.
     * @param principalSet a set containing the principals to check for authorization
     *
     * @return true if granted, false if denied.
     */
    private boolean checkPermission(Permission requestedPermission, Set<Principal> principalSet) {
        boolean hasPermission = false;

        if (uncheckedPermissionCache != null) {
            hasPermission = uncheckedPermissionCache.checkPermission(requestedPermission);
        }

        if (hasPermission == false) {
            hasPermission = checkPermissionWithoutCache(requestedPermission, principalSet);
        } else {
            try {
                setPolicyContext(CONTEXT_ID);
            } catch (Throwable t) {
                if (logger.isLoggable(FINE)) {
                    logger.log(FINE, "[Web-Security] Web Permission Access Denied.", t);
                }
                hasPermission = false;
            }
        }

        return hasPermission;
    }

    private boolean checkPermissionWithoutCache(Permission requestedPermission, Set<Principal> principals) {
        try {
            // NOTE: there is an assumption here, that this setting of the Policy Context will
            // remain in affect through the component dispatch, and that the/ component will not
            // call into any other policy contexts.
            //
            // Even so, could likely reset on failed check.
            setPolicyContext(CONTEXT_ID);

        } catch (Throwable t) {
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "[Web-Security] Web Permission Access Denied.", t);
            }
            return false;
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] Codesource with Web URL: {0}", codesource.getLocation().toString());
            logger.log(FINE, "[Web-Security] Checking Web Permission with Principals : {0}", principalSetToString(principals));
            logger.log(FINE, "[Web-Security] Web Permission = {0}", requestedPermission.toString());
        }

        // Check whether the requested permission is granted to any of the given principals
        return policy.implies(getProtectionDomain(principals), requestedPermission);
    }

    private PolicyConfigurationFactory getPolicyFactory() throws PolicyContextException {
        if (policyConfigurationFactory != null) {
            return policyConfigurationFactory;
        }

        return _getPolicyFactory();
    }

    private synchronized PolicyConfigurationFactory _getPolicyFactory() throws PolicyContextException {
        if (policyConfigurationFactory == null) {
            try {
                policyConfigurationFactory = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            } catch (ClassNotFoundException cnfe) {
                logger.severe("WebSecurityManager - Exception while getting the PolicyFactory");
                throw new PolicyContextException(cnfe);
            } catch (PolicyContextException pce) {
                logger.severe("WebSecurityManager - Exception while getting the PolicyFactory");
                throw pce;
            }
        }

        return policyConfigurationFactory;
    }

    private ProtectionDomain getProtectionDomain(Set<Principal> principalSet) {
        return protectionDomainCache.computeIfAbsent(principalSet, e -> {
            Principal[] principals = (principalSet == null ? null : (Principal[]) principalSet.toArray(new Principal[0]));

            logProtectionDomainCreated(principals);

            return new ProtectionDomain(codesource, null, null, principals);
        });
    }

    private WebResourcePermission createWebResourcePermission(HttpServletRequest servletRequest) {
        String uri = (String) servletRequest.getAttribute(CONSTRAINT_URI);

        if (uri == null) {
            uri = servletRequest.getRequestURI();
            if (uri != null) {
                // FIX TO BE CONFIRMED (after ~12 years): subtract the context path
                String contextPath = servletRequest.getContextPath();
                int contextLength = contextPath == null ? 0 : contextPath.length();
                if (contextLength > 0) {
                    uri = uri.substring(contextLength);
                }
            }
        }

        if (uri == null) {
            logger.fine("[Web-Security] mappedUri is null");
            throw new RuntimeException("Fatal Error in creating WebResourcePermission");
        }

        if (uri.equals("/")) {
            uri = EMPTY_STRING;
        } else {
            // FIX TO BE CONFIRMED: encode all colons
            uri = uri.replaceAll(":", "%3A");
        }

        return new WebResourcePermission(uri, servletRequest.getMethod());
    }

    private static String setPolicyContext(String newContextID) throws Throwable {
        String oldContextID = PolicyContext.getContextID();

        if (oldContextID != newContextID && (oldContextID == null || newContextID == null || !oldContextID.equals(newContextID))) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "[Web-Security] Setting Policy Context ID: old = {0} ctxID = {1}", new Object[] { oldContextID, newContextID });
            }

            try {
                privilegedException(() -> PolicyContext.setContextID(newContextID));
            } catch (PrivilegedActionException pae) {
                Throwable cause = pae.getCause();
                if (cause instanceof AccessControlException) {
                    logger.log(SEVERE, "[Web-Security] setPolicy SecurityPermission required to call PolicyContext.setContextID", cause);
                } else {
                    logger.log(SEVERE, "[Web-Security] Unexpected Exception while setting policy context", cause);
                }
                throw cause;
            }
        } else {
            logger.log(FINE, "[Web-Security] Policy Context ID was: {0}", oldContextID);
        }

        return oldContextID;
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

    /**
     * This is an private method for policy context handler data info
     *
     * @param httpRequest
     */
    private void setServletRequestForJACC(HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            webSecurityManagerFactory.pcHandlerImpl.getHandlerData().setHttpServletRequest(httpRequest);
        }
    }

    private void recordWebInvocation(HttpServletRequest servletRequest, String type, boolean isGranted) {
        AuditManager auditManager = SecurityServicesUtil.getInstance().getAuditManager();

        if (auditManager != null && auditManager.isAuditOn() && (auditManager instanceof AppServerAuditManager)) {
            AppServerAuditManager appServerAuditManager = (AppServerAuditManager) auditManager;
            Principal principal = servletRequest.getUserPrincipal();
            String user = (principal != null) ? principal.getName() : null;

            appServerAuditManager.webInvocation(user, servletRequest, type, isGranted);
        }
    }

    /**
     * Remove All Policy Statements from Configuration config must be in open state when this method is called
     *
     * @param policyConfiguration
     * @param webBundleDescriptor
     * @throws PolicyContextException
     */
    private void removePolicyStatements(PolicyConfiguration policyConfiguration, WebBundleDescriptor webBundleDescriptor) throws PolicyContextException {
        policyConfiguration.removeUncheckedPolicy();
        policyConfiguration.removeExcludedPolicy();

        // Iteration done for old providers
        for (Role role : webBundleDescriptor.getRoles()) {
            policyConfiguration.removeRole(role.getName());
        }

        // 1st call will remove "*" role if present. 2nd will remove all roles (if supported).
        policyConfiguration.removeRole("*");
        policyConfiguration.removeRole("*");
    }

    private String principalSetToString(Set<Principal> principalSet) {
        StringBuilder principalStringBuilder = null;

        if (principalSet != null) {
            Principal[] principals = principalSet.toArray(new Principal[0]);
            for (int i = 0; i < principals.length; i++) {
                if (i == 0) {
                    principalStringBuilder = new StringBuilder(principals[i].toString());
                } else {
                    principalStringBuilder.append(", ").append(principals[i].toString());
                }
            }
        }

        return principalStringBuilder != null ? principalStringBuilder.toString() : null;
    }

    /**
     * Virtual servers are maintained in the reference contained in Server element. First, we need to find the server and
     * then get the virtual server from the correct reference
     *
     * @param applicationName Name of the application for which to get the virtual servers
     *
     * @return virtual servers as a string (separated by space or comma)
     */
    private String getVirtualServers(String applicationName) {
        Server server = serverContext.getDefaultServices().getService(Server.class);
        for (ApplicationRef applicationRef : server.getApplicationRef()) {
            if (applicationRef.getRef().equals(applicationName)) {
                return applicationRef.getVirtualServers();
            }
        }

        return null;
    }

    private void logProtectionDomainCreated(Principal[] principals) {
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] Generating a protection domain for Permission check.");

            if (principals != null) {
                for (Principal principal : principals) {
                    logger.log(FINE, "[Web-Security] Checking with Principal : {0}", principal.toString());
                }
            } else {
                logger.log(FINE, "[Web-Security] Checking with Principals: null");
            }
        }
    }

}
        
