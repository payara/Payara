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
import com.sun.enterprise.security.jacc.JaccEJBConstraintsTranslator;
import com.sun.enterprise.security.jacc.cache.CachedPermission;
import com.sun.enterprise.security.jacc.cache.CachedPermissionImpl;
import com.sun.enterprise.security.jacc.cache.PermissionCache;
import com.sun.enterprise.security.jacc.cache.PermissionCacheFactory;
import com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.security.factory.EJBSecurityManagerFactory;
import org.glassfish.exousia.AuthorizationService;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;
import jakarta.security.jacc.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.enterprise.security.common.AppservAccessController.doPrivileged;
import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

/**
 * This class is used by the EJB server to manage security. All the container object only call into this object for
 * managing security. This class cannot be subclassed.
 * <p/>
 * An instance of this class should be created per deployment unit.
 *
 * @author Harpreet Singh, monzillo
 */
public final class EJBSecurityManager implements SecurityManager {

    private static final Logger _logger = LogDomains.getLogger(EJBSecurityManager.class, LogDomains.EJB_LOGGER);

    private AppServerAuditManager auditManager;

    private static final PolicyContextHandlerImpl pcHandlerImpl = (PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance();

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
    private CodeSource codesource;
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

    private final Policy policy;

    private static final CodeSource managerCodeSource = EJBSecurityManager.class.getProtectionDomain().getCodeSource();

    private final InvocationManager invocationManager;
    private final EJBSecurityManagerFactory securityManagerFactory;
    private final EjbSecurityProbeProvider probeProvider = new EjbSecurityProbeProvider();
    private static volatile EjbSecurityStatsProvider ejbStatsProvider;

    public EJBSecurityManager(EjbDescriptor ejbDescriptor, InvocationManager invocationManager, EJBSecurityManagerFactory fact) throws Exception {

        this.deploymentDescriptor = ejbDescriptor;
        this.invocationManager = invocationManager;
        roleMapperFactory = SecurityUtil.getRoleMapperFactory();
        // get the default policy
        policy = AuthorizationService.getPolicy();
        securityManagerFactory = fact;

        boolean runas = !(deploymentDescriptor.getUsesCallerIdentity());
        if (runas) {
            runAs = deploymentDescriptor.getRunAsIdentity();

            // Note: runAs may be null even when runas==true if this EJB
            // is an MDB.
            if (runAs != null) {
                if (_logger.isLoggable(FINE)) {
                    _logger.fine(deploymentDescriptor.getEjbClassName() + " will run-as: " + runAs.getPrincipal() + " ("
                            + runAs.getRoleName() + ")");
                }
            }
        } else {
            runAs = null;
        }

        initialize();
    }

    public static String getContextID(EjbDescriptor ejbDescriptor) {
        return SecurityUtil.getContextID(ejbDescriptor.getEjbBundleDescriptor());
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

    public void loadPolicyConfiguration(EjbDescriptor ejbDescriptor) throws Exception {
        PolicyConfigurationFactory factory = getPolicyFactory();

        boolean inService = factory.inService(contextId);

        // Only load the policy configuration if it isn't already in service.
        //
        // Consequently, all things that deploy modules (as apposed to/ loading already
        // deployed modules) must make sure a pre-existing PolicyConfiguration
        // is either in deleted or open state before this method is called.
        //
        // Note that policy statements are not removed to allow multiple EJBs to be
        // represented by same PolicyConfiguration.

        if (!inService) {
            // Translate the deployment descriptor to configure the policy rules
            JaccEJBConstraintsTranslator.translateConstraintsToPermissions(ejbDescriptor, factory.getPolicyConfiguration(contextId, false));

            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC: policy translated for policy context:" + contextId);
            }
        }
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

        boolean isAuthorized = false;

        CachedPermission cachedPermission = null;
        Permission permission = null;

        if (ejbInvocation.invocationInfo == null || ejbInvocation.invocationInfo.cachedPermission == null) {
            permission = new EJBMethodPermission(ejbName, ejbInvocation.getMethodInterface(), ejbInvocation.method);
            cachedPermission = new CachedPermissionImpl(uncheckedMethodPermissionCache, permission);

            if (ejbInvocation.invocationInfo != null) {
                ejbInvocation.invocationInfo.cachedPermission = cachedPermission;
                if (_logger.isLoggable(FINE)) {
                    _logger.fine("JACC: permission initialized in InvocationInfo: EJBMethodPermission (Name) = " + permission.getName()
                            + " (Action) = " + permission.getActions());
                }
            }
        } else {
            cachedPermission = ejbInvocation.invocationInfo.cachedPermission;
            permission = cachedPermission.getPermission();
        }

        String caller = null;
        SecurityContext securityContext = null;

        pcHandlerImpl.getHandlerData().setInvocation(ejbInvocation);

        isAuthorized = cachedPermission.checkPermission();

        if (!isAuthorized) {

            securityContext = SecurityContext.getCurrent();

            try {
                // Set the policy context in the TLS.
                String oldContextId = setPolicyContext(contextId);

                try {
                    isAuthorized = policy.implies(getCachedProtectionDomain(securityContext.getPrincipalSet(), true), permission);
                } catch (Throwable t) {
                    _logger.log(SEVERE, "jacc_access_exception", t);
                    isAuthorized = false;
                } finally {
                    resetPolicyContext(oldContextId, contextId);
                }
            } catch (Throwable t) {
                _logger.log(SEVERE, "jacc_policy_context_exception", t);
                isAuthorized = false;
            }
        }

        ejbInvocation.setAuth(isAuthorized);

        if (auditManager.isAuditOn()) {
            if (securityContext == null) {
                securityContext = SecurityContext.getCurrent();
            }
            caller = securityContext.getCallerPrincipal().getName();
            auditManager.ejbInvocation(caller, ejbName, ejbInvocation.method.toString(), isAuthorized);
        }

        if (isAuthorized && ejbInvocation.isWebService && !ejbInvocation.isPreInvokeDone()) {
            preInvoke(ejbInvocation);
        }

        if (_logger.isLoggable(FINE)) {
            _logger.fine(
                "JACC: Access Control Decision Result: " + isAuthorized +
                " EJBMethodPermission (Name) = " + permission.getName() +
                " (Action) = " + permission.getActions() +
                " (Caller) = " + caller);
        }

        return isAuthorized;
    }

    /**
     * This method returns a boolean value indicating whether or not the caller is in the specified role.
     *
     * @param role role name in the form of java.lang.String
     * @return A boolean true/false depending on whether or not the caller has the specified role.
     */
    @Override
    public boolean isCallerInRole(String role) {
        /*
         * In case of Run As - Should check isCallerInRole with respect to the old security context.
         */
        boolean isCallerInRole = false;

        if (_logger.isLoggable(FINE)) {
            _logger.entering("EJBSecurityManager", "isCallerInRole", role);

        }
        EJBRoleRefPermission ejbRoleRefPermission = new EJBRoleRefPermission(ejbName, role);

        SecurityContext securityContext;
        if (runAs != null) {
            securityContext = (SecurityContext) invocationManager.getCurrentInvocation().getOldSecurityContext();
        } else {
            securityContext = SecurityContext.getCurrent();
        }

        Set<Principal> principalSet = securityContext != null ? securityContext.getPrincipalSet() : null;
        ProtectionDomain prdm = getCachedProtectionDomain(principalSet, true);

        String oldContextId = null;
        try {
            // set the policy context in the TLS.
            oldContextId = setPolicyContext(this.contextId);
            isCallerInRole = policy.implies(prdm, ejbRoleRefPermission);
        } catch (Throwable t) {
            _logger.log(Level.SEVERE, "jacc_is_caller_in_role_exception", t);
            isCallerInRole = false;
        } finally {
            try {
                resetPolicyContext(oldContextId, contextId);
            } catch (Throwable ex) {
                _logger.log(Level.SEVERE, "jacc_policy_context_exception", ex);
                isCallerInRole = false;
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("JACC: isCallerInRole Result: " + isCallerInRole + " EJBRoleRefPermission (Name) = " + ejbRoleRefPermission.getName() + " (Action) = "
                    + ejbRoleRefPermission.getActions() + " (Codesource) = " + prdm.getCodeSource());
        }

        return isCallerInRole;
    }

    /**
     * This method is similiar to the runMethod, except it keeps the semantics same as the one in reflection. On failure, if
     * the exception is caused due to reflection, it returns the InvocationTargetException. This method is called from the
     * containers for ejbTimeout, WebService and MDBs.
     *
     * @param beanClassMethod, the bean class method to be invoked
     * @param isLocal, true if this invocation is through the local EJB view
     * @param beanObject the object on which this method is to be invoked in this case the ejb,
     * @param parameters the parameters for the method,
     * @param c, the container instance can be a null value, where in the container will be queried to find its security
     * manager.
     * @return Object, the result of the execution of the method.
     */
    @Override
    public Object invoke(Method beanClassMethod, boolean isLocal, Object beanObject, Object[] parameters) throws Throwable {

        // Optimization. Skip doAsPrivileged call if this is a local invocation and the target EJB
        // uses caller identity or the System Security Manager is disabled.
        //
        // Still need to execute it within the target bean's policy context.
        // see CR 6331550
        if ((isLocal && getUsesCallerIdentity()) || System.getSecurityManager() == null) {
            return runMethod(beanClassMethod, beanObject, parameters);
        }

        try {
            return doAsPrivileged(()-> beanClassMethod.invoke(beanObject, parameters));
        } catch (PrivilegedActionException pae) {
            throw pae.getCause();
        }
    }

    @Override
    public void resetPolicyContext() {
        try {
            doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    ((PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance()).reset();
                    PolicyContext.setContextID(null);
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Throwable cause = pae.getCause();
            if (cause instanceof java.security.AccessControlException) {
                _logger.log(Level.SEVERE, "jacc_policy_context_security_exception", cause);
            } else {
                _logger.log(Level.SEVERE, "jacc_policy_context_exception", cause);
            }
            throw new RuntimeException(cause);
        }
    }


    /**
     * This method returns the Client Principal who initiated the current Invocation.
     *
     * @return A Principal object of the client who made this invocation. or null if the SecurityContext has not been
     * established by the client.
     */
    @Override
    public Principal getCallerPrincipal() {
        SecurityContext securityContext = null;

        if (runAs != null) { // Run As
            // return the principal associated with the old security context
            ComponentInvocation componentInvocation = invocationManager.getCurrentInvocation();

            if (componentInvocation == null) {
                throw new InvocationException(); // 4646060
            }
            securityContext = (SecurityContext) componentInvocation.getOldSecurityContext();

        } else {
            // Lets optimize a little. No need to look up oldsecctx
            // its the same as the new one
            securityContext = SecurityContext.getCurrent();
        }

        if (securityContext != null) {
            return securityContext.getCallerPrincipal();
        }

        return SecurityContext.getDefaultCallerPrincipal();
    }

    @Override
    public void destroy() {
        try {

            boolean wasInService = getPolicyFactory().inService(this.contextId);
            if (wasInService) {
                policy.refresh();
            }
            /*
             * all ejbs of module share same policy context, but each has its own permission cache, which must be unregistered from
             * factory to avoid leak.
             */
            PermissionCacheFactory.removePermissionCache(uncheckedMethodPermissionCache);
            uncheckedMethodPermissionCache = null;
            roleMapperFactory.removeAppNameForContext(this.contextId);

        } catch (PolicyContextException pce) {
            // Just log it.
            _logger.log(Level.WARNING, "ejbsm.could_not_delete", pce);
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

    /*
     * This method is used by SecurityUtil runMethod to run the action as the subject encapsulated in the current
     * SecurityContext.
     */
    @Override
    public Object doAsPrivileged(PrivilegedExceptionAction pea) throws Throwable {

        SecurityContext sc = SecurityContext.getCurrent();
        Set principalSet = sc.getPrincipalSet();
        AccessControlContext acc = (AccessControlContext) accessControlContextCache.get(principalSet);

        if (acc == null) {
            final ProtectionDomain[] pdArray = new ProtectionDomain[1];
            pdArray[0] = getCachedProtectionDomain(principalSet, false);
            try {
                if (principalSet != null) {

                    final Subject s = sc.getSubject();

                    acc = (AccessControlContext) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws Exception {
                            return new AccessControlContext(new AccessControlContext(pdArray), new SubjectDomainCombiner(s));
                        }
                    });
                } else {
                    acc = new AccessControlContext(pdArray);
                }

                // form a new key set so that it does not share with
                // cacheProtectionDomain and protectionDomainCache
                if (principalSet != null) {
                    accessControlContextCache.put(new HashSet(principalSet), acc);
                }

                _logger.fine("JACC: new AccessControlContext added to cache");

            } catch (Exception e) {
                _logger.log(Level.SEVERE, "java_security.security_context_exception", e);
                acc = null;
                throw e;
            }
        }

        Object rvalue = null;
        String oldContextId = setPolicyContext(this.contextId);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("JACC: doAsPrivileged contextId(" + this.contextId + ")");
        }

        try {
            rvalue = AccessController.doPrivileged(pea, acc);
        } finally {
            resetPolicyContext(oldContextId, this.contextId);
        }
        return rvalue;
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


    // ### Private methods


    private void initialize() throws Exception {
        if (ejbStatsProvider == null) {
            synchronized (EjbSecurityStatsProvider.class) {
                if (ejbStatsProvider == null) {
                    ejbStatsProvider = new EjbSecurityStatsProvider();
                    StatsProviderManager.register("security", PluginPoint.SERVER, "security/ejb", ejbStatsProvider);
                }
            }
        }

        contextId = getContextID(deploymentDescriptor);
        String appName = deploymentDescriptor.getApplication().getRegistrationName();
        roleMapperFactory.setAppNameForContext(appName, contextId);
        codesource = getApplicationCodeSource(contextId);
        ejbName = deploymentDescriptor.getName();

        realmName = deploymentDescriptor.getApplication().getRealm();

        if (realmName == null) {
            Set<EjbIORConfigurationDescriptor> iorConfigs = deploymentDescriptor.getIORConfigurationDescriptors();
            // iorConfigs is not null from implementation of EjbDescriptor
            Iterator<EjbIORConfigurationDescriptor> iter = iorConfigs.iterator();
            if (iter != null) {
                // there should be at most one element in the loop from
                // definition of dtd
                while (iter.hasNext()) {
                    realmName = iter.next().getRealmName();
                }
            }
        }

        _logger.fine(() -> "JACC: EJB name = '" + ejbName
            + "'. Context id (id under which all EJB's in application will be created) = '" + contextId + "'");
        loadPolicyConfiguration(deploymentDescriptor);
        // translate the deployment descriptor to populate the role-ref permission cache
        // addEJBRoleReferenceToCache(deploymentDescriptor);

        // create and initialize the unchecked permission cache.
        uncheckedMethodPermissionCache = PermissionCacheFactory.createPermissionCache(this.contextId, this.codesource,
                EJBMethodPermission.class, this.ejbName);

        auditManager = this.securityManagerFactory.getAuditManager();

    }

    private ProtectionDomain getCachedProtectionDomain(Set principalSet, boolean applicationCodeSource) {
        ProtectionDomain prdm = null;
        Principal[] principals = null;

        /*
         * Need to use the application codeSource for permission evaluations as the manager codesource is granted all
         * permissions in server.policy. The manager codesource needs to be used for doPrivileged to allow system apps to have
         * all permissions, but we either need to revert to real doAsPrivileged, or find a way to distinguish system apps.
         */

        CodeSource cs = null;

        if (applicationCodeSource) {
            prdm = (ProtectionDomain) cacheProtectionDomain.get(principalSet);
            cs = codesource;
        } else {
            prdm = (ProtectionDomain) protectionDomainCache.get(principalSet);
            cs = managerCodeSource;
        }

        if (prdm == null) {

            principals = (principalSet == null ? null : (Principal[]) principalSet.toArray(new Principal[principalSet.size()]));

            prdm = new ProtectionDomain(cs, null, null, principals);

            // form a new key set so that it does not share with others
            Set newKeySet = ((principalSet != null) ? new HashSet(principalSet) : new HashSet());

            if (applicationCodeSource) {
                cacheProtectionDomain.put(newKeySet, prdm);
            } else {
                // form a new key set so that it does not share with others
                protectionDomainCache.put(newKeySet, prdm);
            }

            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC: new ProtectionDomain added to cache");
            }

        }

        if (_logger.isLoggable(FINE)) {
            if (principalSet == null) {
                _logger.fine("JACC: returning cached ProtectionDomain PrincipalSet: null");
            } else {
                StringBuilder pBuf = null;
                principals = (Principal[]) principalSet.toArray(new Principal[principalSet.size()]);
                for (int i = 0; i < principals.length; i++) {
                    if (i == 0) {
                        pBuf = new StringBuilder(principals[i].toString());
                    } else {
                        pBuf.append(" " + principals[i].toString());
                    }
                }
                _logger.fine("JACC: returning cached ProtectionDomain - CodeSource: (" + cs + ") PrincipalSet: " + pBuf);
            }
        }

        return prdm;
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
