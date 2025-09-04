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
// Portions Copyright 2016-2025 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license.

package org.glassfish.ejb.security.application;

import com.sun.ejb.EjbInvocation;
import com.sun.enterprise.deployment.EjbIORConfigurationDescriptor;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.ee.authorization.WebAuthorizationManagerService;
import com.sun.enterprise.security.ee.authorization.cache.PermissionCache;
import com.sun.enterprise.security.ee.authorization.cache.PermissionCacheFactory;
import com.sun.enterprise.security.web.integration.GlassFishPrincipalMapper;
import com.sun.logging.LogDomains;
import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.security.factory.EJBSecurityManagerFactory;
import org.glassfish.exousia.AuthorizationService;
import org.glassfish.exousia.permissions.RolesToPermissionsTransformer;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.security.common.Role;

import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static java.util.logging.Level.*;

/**
 * This class is used by the EJB server to manage security. All the container object only call into this object for
 * managing security. This class cannot be subclassed.
 * <p>
 * An instance of this class should be created per deployment unit.
 *
 * @author Harpreet Singh, monzillo
 */
public final class EJBSecurityManager implements SecurityManager {

    private static final Logger _logger = LogDomains.getLogger(EJBSecurityManager.class, LogDomains.EJB_LOGGER);

    private AppServerAuditManager auditManager;
    
    private final SecurityRoleMapperFactory roleMapperFactory;

    private final EjbDescriptor deploymentDescriptor;

    // Objects required for Run-AS
    private final RunAsIdentityDescriptor runAs;

    // JACC related
    private static PolicyConfigurationFactory policyConfigurationFactory;
    private String ejbName;

    // contextId id is the same as an appname. This will be used to get
    // a PolicyConfiguration object per application.
    private String contextId;
    private String realmName;

    // We use two protection domain caches until we decide how to
    // set the codesource in the protection domain of system apps.
    // PD's in protectionDomainCache have the (privileged) codesource
    // of the EJBSecurityManager class. The PD used in pre-dispatch
    // authorization decisions MUST not be constructed using a privileged
    // codesource (or else all pre-distpatch access decisions will be granted).
    private final Map cacheProtectionDomain = Collections.synchronizedMap(new WeakHashMap());
    private final Map protectionDomainCache = Collections.synchronizedMap(new WeakHashMap());

    private final Map accessControlContextCache = Collections.synchronizedMap(new WeakHashMap());

    private PermissionCache uncheckedMethodPermissionCache;
    
    private static final CodeSource managerCodeSource = EJBSecurityManager.class.getProtectionDomain().getCodeSource();

    private final InvocationManager invocationManager;
    private final EJBSecurityManagerFactory securityManagerFactory;
    private final EjbSecurityProbeProvider probeProvider = new EjbSecurityProbeProvider();
    private static volatile EjbSecurityStatsProvider ejbStatsProvider;

    private final AuthorizationService authorizationService;

    public EJBSecurityManager(EjbDescriptor ejbDescriptor, InvocationManager invocationManager, EJBSecurityManagerFactory fact) throws Exception {

        this.deploymentDescriptor = ejbDescriptor;
        this.invocationManager = invocationManager;
        roleMapperFactory = SecurityUtil.getRoleMapperFactory();
        securityManagerFactory = fact;

        runAs = getRunAs(deploymentDescriptor);

        setEnterpriseBeansStatsProvider();
        contextId = getContextID(deploymentDescriptor);
        String appName = deploymentDescriptor.getApplication().getRegistrationName();
        roleMapperFactory.setAppNameForContext(appName, contextId);
        ejbName = deploymentDescriptor.getName();

        realmName = getRealmName(deploymentDescriptor);
        

        _logger.fine(() -> "JACC: EJB name = '" + ejbName
                + "'. Context id (id under which all EJB's in application will be created) = '" + contextId + "'");

        // create and initialize the unchecked permission cache.
        uncheckedMethodPermissionCache = PermissionCacheFactory.createPermissionCache(this.contextId,
                EJBMethodPermission.class, this.ejbName);

        auditManager = this.securityManagerFactory.getAuditManager();

        authorizationService = new AuthorizationService(
                getContextID(ejbDescriptor),
                () -> SecurityContext.getCurrent().getSubject(),
                () -> new GlassFishPrincipalMapper(contextId));

        authorizationService.setRequestSupplier(contextId, WebAuthorizationManagerService::getCurrentRequest);
        authorizationService.addPermissionsToPolicy(
                PayaraToExousiaConverter.convertEJBMethodPermissions(ejbDescriptor));

        authorizationService.addPermissionsToPolicy(RolesToPermissionsTransformer.createEnterpriseBeansRoleRefPermission(
                ejbDescriptor.getEjbBundleDescriptor()
                        .getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),

                PayaraToExousiaConverter.getSecurityRoleRefsFromBundle(ejbDescriptor)));
    }

    private String getRealmName(EjbDescriptor deploymentDescriptor) {
        String realmName = deploymentDescriptor.getApplication().getRealm();

        if (realmName == null) {
            for (EjbIORConfigurationDescriptor iorConfig : deploymentDescriptor.getIORConfigurationDescriptors()) {
                // There should be at most one element in the loop from
                // definition of dtd
                realmName = iorConfig.getRealmName();
            }
        }

        return realmName;
    }

    public static String getContextID(EjbDescriptor ejbDescriptor) {
        return SecurityUtil.getContextID(ejbDescriptor.getEjbBundleDescriptor());
    }

    private void setEnterpriseBeansStatsProvider() {
        if (ejbStatsProvider == null) {
            synchronized (EjbSecurityStatsProvider.class) {
                if (ejbStatsProvider == null) {
                    ejbStatsProvider = new EjbSecurityStatsProvider();
                    StatsProviderManager.register("security", PluginPoint.SERVER, "security/ejb", ejbStatsProvider);
                }
            }
        }
    }

    private RunAsIdentityDescriptor getRunAs(EjbDescriptor deploymentDescriptor) {
        if (deploymentDescriptor.getUsesCallerIdentity()) {
            return null;
        }

        RunAsIdentityDescriptor runAs = deploymentDescriptor.getRunAsIdentity();

        // Note: runAs may be null even when runas==true if this Enterprise Bean
        // is an MDB.
        if (runAs != null) {
            if (_logger.isLoggable(FINE)) {
                _logger.log(FINE,
                        deploymentDescriptor.getEjbClassName() + " will run-as: " + runAs.getPrincipal() +
                                " (" + runAs.getRoleName() + ")");
            }
        }

        return runAs;

    }

    /**
     * This method is used by MDB Container - Invocation Manager to setup the run-as identity information. It has to be
     * coupled with the postSetRunAsIdentity method. This method is called for EJB/MDB Containers
     */
    @Override
    public void preInvoke(ComponentInvocation inv) {

        // Optimization to avoid the expensive call

        if (runAs == null) {
            inv.setPreInvokeDone(true);
            return;
        }

        boolean isWebService = false;
        if (inv instanceof EjbInvocation) {
            isWebService = ((EjbInvocation) inv).isWebService;
        }

        // If it is not a webservice or successful authorization
        // and preInvoke is not call before
        if ((!isWebService || (inv.getAuth() != null && inv.getAuth().booleanValue())) && !inv.isPreInvokeDone()) {
            inv.setOldSecurityContext(SecurityContext.getCurrent());
            loginForRunAs();
            inv.setPreInvokeDone(true);
        }
    }

    @Override
    public Object invoke(Object bean, Method beanClassMethod, Object[] methodParameters) throws Throwable {
        return authorizationService.invokeBeanMethod(bean, beanClassMethod, methodParameters);
    }

    /**
     * This method is used by Message Driven Bean Container to remove the run-as identity information that was set up using
     * the preSetRunAsIdentity method
     */
    @Override
    public void postInvoke(ComponentInvocation inv) {
        if (runAs != null && inv.isPreInvokeDone()) {
            privileged(() -> SecurityContext.setCurrent((SecurityContext) inv.getOldSecurityContext()));
        }
    }

    public boolean getUsesCallerIdentity() {
        return runAs == null;
    }
    
    /**
     * This method is called by the EJB container to decide whether or not a method specified in the Invocation should be
     * allowed.
     *
     * @param componentInvocation invocation object that contains all the details of the invocation.
     * @return A boolean value indicating if the client should be allowed to invoke the EJB.
     */
    @Override
    public boolean authorize(ComponentInvocation componentInvocation) {
        if (!(componentInvocation instanceof EjbInvocation)) {
            return false;
        }

        EjbInvocation ejbInvocation = (EjbInvocation) componentInvocation; // FIXME: Param type should be EjbInvocation
        if (ejbInvocation.getAuth() != null) {
            return ejbInvocation.getAuth().booleanValue();
        }

        SecurityContext securityContext = SecurityContext.getCurrent();

        boolean authorized = false;
        try {
            authorized = authorizationService.checkBeanMethodPermission(
                    ejbName,
                    ejbInvocation.getMethodInterface(),
                    ejbInvocation.method,
                    securityContext.getPrincipalSet());
        } catch (Throwable t) {
            _logger.log(SEVERE, "Unexpected exception manipulating policy context", t);
            authorized = false;
        }

        ejbInvocation.setAuth(authorized);

        doAuditAuthorize(securityContext, ejbInvocation, authorized);

        if (authorized && ejbInvocation.isWebService && !ejbInvocation.isPreInvokeDone()) {
            preInvoke(ejbInvocation);
        }
        

        if (_logger.isLoggable(FINE)) {
            _logger.fine(
                "JACC: Access Control Decision Result: " + authorized);
        }

        return authorized;
    }

    private void doAuditAuthorize(SecurityContext securityContext, EjbInvocation ejbInvocation, boolean authorized) {
        if (auditManager.isAuditOn()) {
            String caller = securityContext.getCallerPrincipal().getName();
            auditManager.ejbInvocation(caller, ejbName, ejbInvocation.method.toString(), authorized);

            _logger.fine(() -> " (Caller) = " + caller);
        }
    }

    /**
     * This method returns a boolean value indicating whether or not the caller is in the specified role.
     *
     * @param role role name in the form of java.lang.String
     * @return A boolean true/false depending on whether or not the caller has the specified role.
     */
    @Override
    public boolean isCallerInRole(String role) {
        if (_logger.isLoggable(FINE)) {
            _logger.entering("EJBSecurityManager", "isCallerInRole", role);
        }

        SecurityContext securityContext = getSecurityContext();
        Set<Principal> principalSet = securityContext != null ? securityContext.getPrincipalSet() : null;

        return authorizationService.checkBeanRoleRefPermission(ejbName, role, principalSet);
    }

    private SecurityContext getSecurityContext() {
        if (runAs == null) {
            return SecurityContext.getCurrent();
        }

        // Return the principal associated with the old security context
        ComponentInvocation componentInvocation = invocationManager.getCurrentInvocation();

        if (componentInvocation == null) {
            throw new InvocationException();
        }

        return (SecurityContext) componentInvocation.getOldSecurityContext();
    }
    
    @Override
    public void resetPolicyContext() {
        PolicyContext.setContextID(null);
    }


    /**
     * This method returns the Client Principal who initiated the current Invocation.
     *
     * @return A Principal object of the client who made this invocation. or null if the SecurityContext has not been
     * established by the client.
     */
    @Override
    public Principal getCallerPrincipal() {

        SecurityContext securityContext = getSecurityContext();
        if (securityContext == null) {
            return SecurityContext.getDefaultCallerPrincipal();
        }

        return securityContext.getCallerPrincipal();
    }

    @Override
    public void destroy() {
        try {
            authorizationService.refresh();

            /*
             * All enterprise beans of module share same policy context, but each has its
             * own permission cache, which must be unregistered from factory to
             * avoid leaks.
             */
            PermissionCacheFactory.removePermissionCache(uncheckedMethodPermissionCache);
            uncheckedMethodPermissionCache = null;
            roleMapperFactory.removeAppNameForContext(contextId);

        } catch (IllegalStateException e) {
            _logger.log(WARNING, "ejbsm.could_not_delete", e);
        }

        probeProvider.securityManagerDestructionStartedEvent(ejbName);
        securityManagerFactory.getManager(contextId, ejbName, true);
        probeProvider.securityManagerDestructionEndedEvent(ejbName);

        probeProvider.securityManagerDestructionEvent(ejbName);

    }

    /**
     * This will return the subject associated with the current call. If the run as subject is in effect. It will return
     * that subject. This is done to support the JACC specification which says if the runas principal is in effect, that
     * principal should be used for making a component call.
     *
     * @return Subject the current subject. Null if this is not the run-as case
     */
    @Override
    public Subject getCurrentSubject() {
        // just get the security context will return the empt subject
        // of the default securityContext when appropriate.
        return SecurityContext.getCurrent().getSubject();
    }

    /**
     * Runs a business method of an EJB within the bean's policy context. The original policy context is restored after
     * method execution. This method should only be used by com.sun.enterprise.security.SecurityUtil.
     *
     * @param beanClassMethod the EJB business method
     * @param beanObject the EJB bean instance
     * @param parameters parameters passed to beanClassMethod
     * @return return value from beanClassMethod
     * @throws java.lang.reflect.InvocationTargetException if the underlying method throws an exception
     * @throws Throwable other throwables in other cases
     */
    public Object runMethod(Method beanClassMethod, Object beanObject, Object[] parameters) throws Throwable {
        String oldContextId = setPolicyContext(contextId);
        try {
            return beanClassMethod.invoke(beanObject, parameters);
        } finally {
            resetPolicyContext(oldContextId, contextId);
        }
    }

    /**
     * Logs in a principal for run-as. This method is called if the run-as principal is required. The user has already
     * logged in - now it needs to change to the new principal. In order that all the correct permissions work - this method
     * logs the new principal with no password -generating valid credentials.
     */
    private void loginForRunAs() {
        privileged(() -> WebAndEjbToJaasBridge.loginPrincipal(runAs.getPrincipal(), realmName));
    }

    private static CodeSource getApplicationCodeSource(String pcid) throws Exception {
        CodeSource result = null;
        String archiveURI = "file:///" + pcid.replace(' ', '_');
        try {
            URI uri = null;
            try {
                uri = new URI(archiveURI);
                if (uri != null) {
                    result = new CodeSource(uri.toURL(), (java.security.cert.Certificate[]) null);
                }
            } catch (URISyntaxException use) {
                // manually create the URL
                _logger.log(SEVERE, "JACC_createurierror", use);
                throw new RuntimeException(use);
            }

        } catch (MalformedURLException mue) {
            // should never come here.
            _logger.log(SEVERE, "JACC_ejbsm.codesourceerror", mue);
            throw new RuntimeException(mue);
        }

        return result;
    }

    // Obtains PolicyConfigurationFactory once for class
    private static PolicyConfigurationFactory getPolicyFactory() throws PolicyContextException {
        synchronized (EJBSecurityManager.class) {
            if (policyConfigurationFactory == null) {
                try {
                    policyConfigurationFactory = PolicyConfigurationFactory.getPolicyConfigurationFactory();
                } catch (ClassNotFoundException cnfe) {
                    _logger.severe("jaccfactory.notfound");
                    throw new PolicyContextException(cnfe);
                } catch (PolicyContextException pce) {
                    _logger.severe("jaccfactory.notfound");
                    throw pce;
                }
            }
        }

        return policyConfigurationFactory;
    }

    private static void resetPolicyContext(final String newV, String oldV) throws Throwable {
        if (oldV != newV && newV != null && (oldV == null || !oldV.equals(newV))) {

            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC: Changing Policy Context ID: oldV = " + oldV + " newV = " + newV);
            }

            try {
                AppservAccessController.doPrivileged(new PrivilegedExceptionAction() {
                    @Override
                    public Object run() throws Exception {
                        PolicyContext.setContextID(newV);
                        return null;
                    }
                });
            } catch (PrivilegedActionException pae) {
                Throwable cause = pae.getCause();
                if (cause instanceof AccessControlException) {
                    _logger.log(SEVERE, "jacc_policy_context_security_exception", cause);
                } else {
                    _logger.log(SEVERE, "jacc_policy_context_exception", cause);
                }
                throw cause;
            }
        }
    }

    private static String setPolicyContext(String newV) throws Throwable {
        String oldV = PolicyContext.getContextID();
        resetPolicyContext(newV, oldV);
        return oldV;
    }


}
