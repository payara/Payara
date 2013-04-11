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

package org.glassfish.ejb.security.application;

import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.security.factory.EJBSecurityManagerFactory;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.Role;

import com.sun.ejb.EjbInvocation;
import com.sun.enterprise.deployment.EjbIORConfigurationDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.deployment.RoleReference;
import com.sun.enterprise.deployment.RunAsIdentityDescriptor;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.authorize.PolicyContextHandlerImpl;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.ee.CachedPermission;
import com.sun.enterprise.security.ee.CachedPermissionImpl;
import com.sun.enterprise.security.ee.PermissionCache;
import com.sun.enterprise.security.ee.PermissionCacheFactory;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.logging.LogDomains;

/**
 * This class is used by the EJB server to manage security. All
 * the container object only call into this object for managing
 * security. This class cannot be subclassed.
 * <p/>
 * An instance of this class should be created per deployment unit.
 *
 * @author Harpreet Singh, monzillo
 */
public final class EJBSecurityManager
         /*extends SecurityManagerFactory*/ implements SecurityManager {

    private static final Logger _logger
        = LogDomains.getLogger(EJBSecurityManager.class, LogDomains.EJB_LOGGER);

    private  AppServerAuditManager auditManager;

    private static final PolicyContextHandlerImpl pcHandlerImpl =
            (PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance();

    private final SecurityRoleMapperFactory roleMapperFactory;
            //SecurityRoleMapperFactoryMgr.getFactory();

    private final EjbDescriptor deploymentDescriptor;
    // Objects required for Run-AS
    private final RunAsIdentityDescriptor runAs;

    // jacc related
    private static PolicyConfigurationFactory pcf = null;
    private String ejbName = null;
    // contextId id is the same as an appname. This will be used to get
    // a PolicyConfiguration object per application.
    private String contextId = null;
    private String codebase = null;
    private CodeSource codesource = null;
    private String realmName = null;
    // this stores the role ref permissions. So will not need to spend runtime 
    // resources generating permissions.
    //private Hashtable cacheRoleToPerm = new Hashtable();

    // we use two protection domain caches until we decide how to 
    // set the codesource in the protection domain of system apps.
    // PD's in protectionDomainCache have the (privileged) codesource
    // of the EJBSecurityManager class. The PD used in pre-dispatch
    // authorization decisions MUST not be constructed using a privileged
    // codesource (or else all pre-distpatch access decisions will be granted).
    private final Map cacheProtectionDomain =
            Collections.synchronizedMap(new WeakHashMap());
    private final Map protectionDomainCache =
            Collections.synchronizedMap(new WeakHashMap());

    private final Map accessControlContextCache =
            Collections.synchronizedMap(new WeakHashMap());

    private PermissionCache uncheckedMethodPermissionCache = null;

    private final Policy policy;

    private static final CodeSource managerCodeSource =
            EJBSecurityManager.class.getProtectionDomain().getCodeSource();

    private final InvocationManager invMgr;
    private final EJBSecurityManagerFactory ejbSFM;
    private final EjbSecurityProbeProvider probeProvider = new EjbSecurityProbeProvider();
    private static volatile EjbSecurityStatsProvider ejbStatsProvider = null;

    public EJBSecurityManager(EjbDescriptor ejbDescriptor, InvocationManager invMgr,
                                EJBSecurityManagerFactory fact) throws Exception {

        this.deploymentDescriptor = (EjbDescriptor) ejbDescriptor;
        this.invMgr = invMgr;
        roleMapperFactory = SecurityUtil.getRoleMapperFactory();
        // get the default policy
        policy = Policy.getPolicy();
        ejbSFM = fact;

        boolean runas = !(deploymentDescriptor.getUsesCallerIdentity());
        if (runas) {
            runAs = deploymentDescriptor.getRunAsIdentity();

            // Note: runAs may be null even when runas==true if this EJB
            // is an MDB. 
            if (runAs != null) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, deploymentDescriptor.getEjbClassName() +
                            " will run-as: " + runAs.getPrincipal() +
                            " (" + runAs.getRoleName() + ")");
                }
            }
        } else {
            runAs = null;
        }

        initialize();
    }

    private static CodeSource getApplicationCodeSource(String pcid) throws Exception {
        CodeSource result = null;
        String archiveURI = "file:///" + pcid.replace(' ', '_');
        try {
            java.net.URI uri = null;
            try {
                uri = new java.net.URI(archiveURI);
                if (uri != null) {
                    result = new CodeSource(uri.toURL(),
                            (java.security.cert.Certificate[]) null);
                }
            } catch (java.net.URISyntaxException use) {
                // manually create the URL
                _logger.log(Level.SEVERE, "JACC_createurierror", use);
                throw new RuntimeException(use);
            }

        } catch (java.net.MalformedURLException mue) {
            // should never come here.
            _logger.log(Level.SEVERE, "JACC_ejbsm.codesourceerror", mue);
            throw new RuntimeException(mue);
        }
        return result;
    }

    // obtains PolicyConfigurationFactory once for class
    private static PolicyConfigurationFactory getPolicyFactory()
            throws PolicyContextException {
        synchronized (EJBSecurityManager.class) {
            if (pcf == null) {
                try {
                    pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
                } catch (ClassNotFoundException cnfe) {
                    _logger.severe("jaccfactory.notfound");
                    throw new PolicyContextException(cnfe);
                } catch (PolicyContextException pce) {
                    _logger.severe("jaccfactory.notfound");
                    throw pce;
                }
            }
        }
        return pcf;
    }

    public boolean getUsesCallerIdentity() {
        return (runAs == null);
    }

    public void loadPolicyConfiguration(EjbDescriptor eDescriptor) throws Exception {

        boolean inService = getPolicyFactory().inService(contextId);

        // only load the policy configuration if it isn't already in service.
        // Consequently, all things that deploy modules (as apposed to
        // loading already deployed modules) must make sure pre-exiting
        // pc is either in deleted or open state before this method
        // is called. Note that policy statements are not
        // removed to allow multiple EJB's to be represented by same pc.

        if (!inService) {
            // translate the deployment descriptor to configure the policy rules
            convertEJBMethodPermissions(eDescriptor, contextId);
            convertEJBRoleReferences(eDescriptor, contextId);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC: policy translated for policy context:" + contextId);
            }
        }
    }

    public static String getContextID(EjbDescriptor ejbDesc) {
        return SecurityUtil.getContextID(ejbDesc.getEjbBundleDescriptor());
    }

//    public static String getContextID(EjbBundleDescriptor ejbBundleDesc) {
//        String cid = null;
//        if (ejbBundleDesc != null) {
//            cid = ejbBundleDesc.getApplication().getRegistrationName() +
//                    '/' + ejbBundleDesc.getUniqueFriendlyId();
//        }
//        return cid;
//    }


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

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("JACC: Context id (id under which all EJB's in application will be created) = " + contextId);
            _logger.fine("Codebase (module id for ejb " + ejbName + ") = " + codebase);
        }
        loadPolicyConfiguration(deploymentDescriptor);
        // translate the deployment descriptor to populate the role-ref permission cache
        //addEJBRoleReferenceToCache(deploymentDescriptor);

        // create and initialize the unchecked permission cache.
        uncheckedMethodPermissionCache =
                PermissionCacheFactory.createPermissionCache(
                        this.contextId, this.codesource,
                        EJBMethodPermission.class,
                        this.ejbName);
        
        auditManager = this.ejbSFM.getAuditManager();
        
    }

    /**
     * This method converts ejb role references to jacc permission objects
     * and adds them to the policy configuration object
     * It gets the list of role references from the ejb descriptor. For each
     * such role reference, create a EJBRoleRefPermission and add it to the
     * PolicyConfiguration object.
     *
     * @param eDescriptor the ejb descriptor
     * @param pcid,       the policy context identifier
     */
    private static void
    convertEJBRoleReferences(EjbDescriptor eDescriptor, String pcid)
            throws PolicyContextException {
        
        PolicyConfiguration pc =
                getPolicyFactory().getPolicyConfiguration(pcid, false);
        // pc will always has a value which is provided by implementation
        // of PolicyConfigurationFactory
        assert pc != null;
        // Get the set of roles declared
        Set<Role> roleset = eDescriptor.getEjbBundleDescriptor().getRoles();
        Role anyAuthUserRole = new Role("**");
        boolean rolesetContainsAnyAuthUserRole = roleset.contains(anyAuthUserRole);
        List<Role> role = new ArrayList<Role>();
        String eName = eDescriptor.getName();
        for (RoleReference roleRef : eDescriptor.getRoleReferences()) {
            String rolename = roleRef.getRoleName();
            EJBRoleRefPermission ejbrr =
                    new EJBRoleRefPermission(eName, rolename);
            String rolelink = roleRef.getSecurityRoleLink().getName();

            role.add(new Role(rolename));
            pc.addToRole(rolelink, ejbrr);

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC: Converting role-ref -> " + roleRef.toString() +
                        " to permission with name(" + ejbrr.getName() +
                        ") and actions (" + ejbrr.getActions() +
                        ")" + "mapped to role (" + rolelink + ")");
            }
        }
        if (_logger.isLoggable(Level.FINE)){
        	_logger.log(Level.FINE,"JACC: Converting role-ref: Going through the list of roles not present in RoleRef elements and creating EJBRoleRefPermissions ");
        }
        for (Role r : roleset) {
        	if (_logger.isLoggable(Level.FINE)){
        		_logger.log(Level.FINE,"JACC: Converting role-ref: Looking at Role =  "+r.getName());
        	}
        	if (!role.contains(r)) {
        		String action = r.getName();
        		EJBRoleRefPermission ejbrr = new EJBRoleRefPermission(eName, action);
        		pc.addToRole(action, ejbrr);
        		if (_logger.isLoggable(Level.FINE)) {
        			_logger.fine("JACC: Converting role-ref: Role =  " + r.getName() +
        					" is added as a permission with name(" + ejbrr.getName() +
        					") and actions (" + ejbrr.getActions() +
        					")" + "mapped to role (" + action + ")");
        		}
        	}
        }
        /**
         * JACC MR8 add EJBRoleRefPermission for the any authenticated user role '**'
         */
        if ((!role.contains(anyAuthUserRole)) && !rolesetContainsAnyAuthUserRole) {
            String rolename = anyAuthUserRole.getName();
            EJBRoleRefPermission ejbrr =
                    new EJBRoleRefPermission(eName, rolename);
            pc.addToRole(rolename, ejbrr);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC: Converting role-ref: Adding any authenticated user role-ref " +
                        " to permission with name(" + ejbrr.getName() +
                        ") and actions (" + ejbrr.getActions() +
                        ")" + "mapped to role (" + rolename + ")");
            }
        	
        }
    }

    /**
     * This method converts ejb role references to jacc permission objects
     * and adds them to the corresponding permission cache.
     *
     * @param eDescriptor the ejb descriptor
     
    private void addEJBRoleReferenceToCache(EjbDescriptor eDescriptor) {

        String eName = eDescriptor.getName();

        Iterator iroleref = eDescriptor.getRoleReferences().iterator();
        while (iroleref.hasNext()) {
            SecurityRoleReference roleRef =
                    (SecurityRoleReference) iroleref.next();
            String rolename = roleRef.getRolename();
            EJBRoleRefPermission ejbrr =
                    new EJBRoleRefPermission(eName, rolename);
            String rolelink = roleRef.getSecurityRoleLink().getName();

            cacheRoleToPerm.put(eName + "_" + rolename, ejbrr);

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC: Converting role-ref -> " + roleRef.toString() +
                        " to permission with name(" + ejbrr.getName() +
                        ") and actions (" + ejbrr.getActions() +
                        ")" + "mapped to role (" + rolelink + ")");
            }
        }
    }*/

    // utility to collect role permisisions in table of collections
    private static HashMap addToRolePermissionsTable(HashMap table,
                                                     MethodPermission mp,
                                                     EJBMethodPermission ejbmp) {
        if (mp.isRoleBased()) {
            if (table == null) {
                table = new HashMap();
            }
            String roleName = mp.getRole().getName();
            Permissions rolePermissions =
                    (Permissions) table.get(roleName);
            if (rolePermissions == null) {
                rolePermissions = new Permissions();
                table.put(roleName, rolePermissions);
            }
            rolePermissions.add(ejbmp);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC DD conversion: EJBMethodPermission ->(" +
                        ejbmp.getName() + " " + ejbmp.getActions() +
                        ")protected by role -> " + roleName);
            }
        }
        return table;
    }

    // utility to collect unchecked permissions in collection
    private static Permissions addToUncheckedPermissions(Permissions permissions,
                                                         MethodPermission mp,
                                                         EJBMethodPermission ejbmp) {
        if (mp.isUnchecked()) {
            if (permissions == null) {
                permissions = new Permissions();
            }
            permissions.add(ejbmp);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC DD conversion: EJBMethodPermission ->("
                        + ejbmp.getName() + " " + ejbmp.getActions() +
                        ") is (unchecked)");
            }
        }
        return permissions;
    }

    // utility to collect excluded permissions in collection
    private static Permissions addToExcludedPermissions(Permissions permissions,
                                                        MethodPermission mp,
                                                        EJBMethodPermission ejbmp) {
        if (mp.isExcluded()) {
            if (permissions == null) {
                permissions = new Permissions();
            }
            permissions.add(ejbmp);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC DD conversion: EJBMethodPermission ->("
                        + ejbmp.getName() + " " + ejbmp.getActions() +
                        ") is (excluded)");
            }
        }
        return permissions;
    }

    /**
     * This method converts the dd in two phases.
     * Phase 1:
     * gets a map representing the methodPermission elements exactly as they
     * occured for the ejb in the dd. The map is keyed by method-permission
     * element and each method-permission is mapped to a list of method
     * elements representing the method elements of the method permision
     * element. Each method element is converted to a corresponding
     * EJBMethodPermission and added, based on its associated method-permission,
     * to the policy configuration object.
     * phase 2:
     * configures additional EJBMethodPermission policy statements
     * for the purpose of optimizing Permissions.implies matching by the
     * policy provider. This phase also configures unchecked policy
     * statements for any uncovered methods. This method gets the list
     * of method descriptors for the ejb from the EjbDescriptor object.
     * For each method descriptor, it will get a list of MethodPermission
     * objects that signify the method permissions for the Method and
     * convert each to a corresponding EJBMethodPermission to be added
     * to the policy configuration object.
     *
     * @param eDescriptor the ejb descriptor for this EJB.
     * @param pcid,       the policy context identifier.
     */
    private static void
    convertEJBMethodPermissions(EjbDescriptor eDescriptor, String pcid)
            throws PolicyContextException {

        PolicyConfiguration pc =
                getPolicyFactory().getPolicyConfiguration(pcid, false);

        // pc will always has a value which is provided by implementation
        // of PolicyConfigurationFactory
        assert pc != null;

        String eName = eDescriptor.getName();

        Permissions uncheckedPermissions = null;
        Permissions excludedPermissions = null;
        HashMap rolePermissionsTable = null;

        EJBMethodPermission ejbmp = null;

        // phase 1
        Map mpMap = eDescriptor.getMethodPermissionsFromDD();
        if (mpMap != null) {

            Iterator mpIt = mpMap.entrySet().iterator();

            while (mpIt.hasNext()) {

                Map.Entry entry = (Map.Entry)mpIt.next();
                MethodPermission mp = (MethodPermission) entry.getKey();

                Iterator mdIt = ((ArrayList) entry.getValue()).iterator();

                while (mdIt.hasNext()) {

                    MethodDescriptor md = (MethodDescriptor) mdIt.next();

                    String mthdName = md.getName();
                    String mthdIntf = md.getEjbClassSymbol();
                    String mthdParams[] = md.getStyle() == 3 ?
                            md.getParameterClassNames() : null;

                    ejbmp = new EJBMethodPermission(eName, mthdName.equals("*") ?
                            null : mthdName,
                            mthdIntf, mthdParams);
                    rolePermissionsTable =
                            addToRolePermissionsTable(rolePermissionsTable, mp, ejbmp);

                    uncheckedPermissions =
                            addToUncheckedPermissions(uncheckedPermissions, mp, ejbmp);

                    excludedPermissions =
                            addToExcludedPermissions(excludedPermissions, mp, ejbmp);
                }
            }
        }

        // phase 2 - configures additional perms:
        //      . to optimize performance of Permissions.implies
        //      . to cause any uncovered methods to be unchecked

        Iterator mdIt = eDescriptor.getMethodDescriptors().iterator();
        while (mdIt.hasNext()) {

            MethodDescriptor md = (MethodDescriptor) mdIt.next();
            Method mthd = md.getMethod(eDescriptor);
            String mthdIntf = md.getEjbClassSymbol();

            if (mthd == null) {
                continue;
            }

            if (mthdIntf == null || mthdIntf.equals("")) {
                _logger.log(Level.SEVERE, "method_descriptor_not_defined" , new Object[] {eName,
                        md.getName(), md.getParameterClassNames()});

                continue;
            }

            ejbmp = new EJBMethodPermission(eName, mthdIntf, mthd);

            Iterator mpIt = eDescriptor.getMethodPermissionsFor(md).iterator();

            while (mpIt.hasNext()) {

                MethodPermission mp = (MethodPermission) mpIt.next();

                rolePermissionsTable =
                        addToRolePermissionsTable(rolePermissionsTable, mp, ejbmp);

                uncheckedPermissions =
                        addToUncheckedPermissions(uncheckedPermissions, mp, ejbmp);

                excludedPermissions =
                        addToExcludedPermissions(excludedPermissions, mp, ejbmp);
            }
        }

        if (uncheckedPermissions != null) {
            pc.addToUncheckedPolicy(uncheckedPermissions);
        }
        if (excludedPermissions != null) {
            pc.addToExcludedPolicy(excludedPermissions);
        }
        if (rolePermissionsTable != null) {

            Iterator roleIt = rolePermissionsTable.entrySet().iterator();

            while (roleIt.hasNext()) {
                Map.Entry entry = (Map.Entry)roleIt.next();
                pc.addToRole((String) entry.getKey(),
                        (Permissions) entry.getValue());
            }
        }
    }

    private ProtectionDomain getCachedProtectionDomain(Set principalSet,
                                                       boolean applicationCodeSource) {

        ProtectionDomain prdm = null;
        Principal[] principals = null;

        /* Need to use the application codeSource for permission evaluations
       * as the manager codesource is granted all permissions in server.policy.
       * The manager codesource needs to be used for doPrivileged to allow system
       * apps to have all permissions, but we either need to revert to
       * real doAsPrivileged, or find a way to distinguish system apps.
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

            principals = (principalSet == null ? null :
                    (Principal[]) principalSet.toArray(new Principal[principalSet.size()]));

            prdm = new ProtectionDomain(cs, null, null, principals);

            // form a new key set so that it does not share with others
            Set newKeySet = ((principalSet != null) ? new HashSet(principalSet) : new HashSet());

            if (applicationCodeSource) {
                cacheProtectionDomain.put(newKeySet, prdm);
            } else {
                // form a new key set so that it does not share with others
                protectionDomainCache.put(newKeySet, prdm);
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC: new ProtectionDomain added to cache");
            }

        }

        if (_logger.isLoggable(Level.FINE)) {
            if (principalSet == null) {
                _logger.fine("JACC: returning cached ProtectionDomain PrincipalSet: null");
            } else {
                StringBuffer pBuf = null;
                principals = (Principal[]) principalSet.toArray(new Principal[principalSet.size()]);
                for (int i = 0; i < principals.length; i++) {
                    if (i == 0) pBuf = new StringBuffer(principals[i].toString());
                    else pBuf.append(" " + principals[i].toString());
                }
                _logger.fine("JACC: returning cached ProtectionDomain - CodeSource: ("
                        + cs + ") PrincipalSet: " + pBuf);
            }
        }

        return prdm;
    }


    /**
     * This method is called by the EJB container to decide whether or not
     * a method specified in the Invocation should be allowed.
     *
     * @param compInv invocation object that contains all the details of the
     *                invocation.
     * @return A boolean value indicating if the client should be allowed
     *         to invoke the EJB.
     */
    public boolean authorize(ComponentInvocation compInv) {
        if (!(compInv instanceof EjbInvocation)) {
            return false;
        }

        EjbInvocation inv = (EjbInvocation) compInv;    //FIXME: Param type should be EjbInvocation
        if (inv.getAuth() != null) {
            return inv.getAuth().booleanValue();
        }

        boolean ret = false;

        CachedPermission cp = null;
        Permission ejbmp = null;

        if (inv.invocationInfo == null || inv.invocationInfo.cachedPermission == null) {
            ejbmp = new EJBMethodPermission(ejbName, inv.getMethodInterface(), inv.method);
            cp = new CachedPermissionImpl(uncheckedMethodPermissionCache, ejbmp);
            if (inv.invocationInfo != null) {
                inv.invocationInfo.cachedPermission = cp;
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("JACC: permission initialized in InvocationInfo: EJBMethodPermission (Name) = " + ejbmp.getName() + " (Action) = " + ejbmp.getActions());
                }
            }
        } else {
            cp = inv.invocationInfo.cachedPermission;
            ejbmp = cp.getPermission();
        }

        String caller = null;
        SecurityContext sc = null;

        pcHandlerImpl.getHandlerData().setInvocation(inv);
        ret = cp.checkPermission();

        if (!ret) {

            sc = SecurityContext.getCurrent();
            Set principalSet = sc.getPrincipalSet();
            ProtectionDomain prdm = getCachedProtectionDomain(principalSet, true);
            try {
                // set the policy context in the TLS.
                String oldContextId = setPolicyContext(this.contextId);
                try {
                    ret = policy.implies(prdm, ejbmp);
                } catch (SecurityException se) {
                    _logger.log(Level.SEVERE, "jacc_access_exception", se);
                    ret = false;
                } catch (Throwable t) {
                    _logger.log(Level.SEVERE, "jacc_access_exception", t);
                    ret = false;
                } finally {
                    resetPolicyContext(oldContextId, this.contextId);
                }

            } catch (Throwable t) {
                _logger.log(Level.SEVERE, "jacc_policy_context_exception", t);
                ret = false;
            }
        }

        inv.setAuth((ret) ? Boolean.TRUE : Boolean.FALSE);

        if (auditManager.isAuditOn()) {
            if (sc == null) {
                sc = SecurityContext.getCurrent();
            }
            caller = sc.getCallerPrincipal().getName();
            auditManager.ejbInvocation(caller, ejbName, inv.method.toString(), ret);
        }

        if (ret && inv.isWebService && !inv.isPreInvokeDone()) {
            preInvoke(inv);
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("JACC: Access Control Decision Result: " + ret + " EJBMethodPermission (Name) = " + ejbmp.getName() + " (Action) = " + ejbmp.getActions() + " (Caller) = " + caller);
        }

        return ret;
    }

    /**
     * This method is used by MDB Container - Invocation Manager  to setup
     * the run-as identity information. It has to be coupled with
     * the postSetRunAsIdentity method.
     * This method is called for EJB/MDB Containers
     */
    public void preInvoke(ComponentInvocation inv) {

        //Optimization to avoid the expensive call

        if(runAs == null) {
            inv.setPreInvokeDone(true);
            return;
        }

        boolean isWebService = false;
        if (inv instanceof EjbInvocation) {
            isWebService = ((EjbInvocation) inv).isWebService;
        }

        // if it is not a webservice or successful authorization
        // and preInvoke is not call before
        if ((!isWebService || (inv.getAuth() != null && inv.getAuth().booleanValue()))
                && !inv.isPreInvokeDone()) {
            inv.setOldSecurityContext(SecurityContext.getCurrent());
            loginForRunAs();
            inv.setPreInvokeDone(true);
        }
    }

    /**
     * This method is used by Message Driven Bean Container to remove
     * the run-as identity information that was set up using the
     * preSetRunAsIdentity method
     */
    public void postInvoke(ComponentInvocation inv) {
        if (runAs != null && inv.isPreInvokeDone()) {
            final ComponentInvocation finv = inv;
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    SecurityContext.setCurrent(
                            (SecurityContext) finv.getOldSecurityContext());
                    return null;
                }
            });
        }
    }

    /**
     * Logs in a principal for run-as. This method is called if the
     * run-as principal is required. The user has already logged in -
     * now it needs to change to the new principal. In order that all
     * the correct permissions work - this method logs the new principal
     * with no password -generating valid credentials.
     */
    private void loginForRunAs() {
        AppservAccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                LoginContextDriver.loginPrincipal(runAs.getPrincipal(), realmName);
                return null;
            }
        });
    }


    /**
     * This method returns a boolean value indicating whether or not the
     * caller is in the specified role.
     *
     * @param role role name in the form of java.lang.String
     * @return A boolean true/false depending on whether or not the caller
     *         has the specified role.
     */
    public boolean isCallerInRole(String role) {
        /* In case of Run As - Should check isCallerInRole with
       * respect to the old security context.
       */

        boolean ret = false;

        if (_logger.isLoggable(Level.FINE)) {
            _logger.entering("EJBSecurityManager", "isCallerInRole", role);

        }
        EJBRoleRefPermission ejbrr = new EJBRoleRefPermission(ejbName, role);
        
        SecurityContext sc;
        if (runAs != null) {
            ComponentInvocation ci = invMgr.getCurrentInvocation();
            sc = (SecurityContext) ci.getOldSecurityContext();
        } else {
            sc = SecurityContext.getCurrent();
        }

        Set principalSet = (sc != null) ? sc.getPrincipalSet() : null;
        ProtectionDomain prdm = getCachedProtectionDomain(principalSet, true);

        String oldContextId = null;
        try {
            // set the policy context in the TLS.
            oldContextId = setPolicyContext(this.contextId);
            ret = policy.implies(prdm, ejbrr);
        } catch (SecurityException se) {
            _logger.log(Level.SEVERE, "jacc_is_caller_in_role_exception", se);
            ret = false;
        } catch (Throwable t) {
            _logger.log(Level.SEVERE, "jacc_is_caller_in_role_exception", t);
            ret = false;
        } finally {
            try {
                resetPolicyContext(oldContextId, this.contextId);
            } catch (Throwable ex) {
                _logger.log(Level.SEVERE, "jacc_policy_context_exception", ex);
                ret = false;
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("JACC: isCallerInRole Result: " + ret + " EJBRoleRefPermission (Name) = " + ejbrr.getName() + " (Action) = " + ejbrr.getActions() + " (Codesource) = " + prdm.getCodeSource());
        }

        return ret;
    }

    /**
     * This method returns the Client Principal who initiated the current
     * Invocation.
     *
     * @return A Principal object of the client who made this invocation.
     *         or null if the SecurityContext has not been established by the client.
     */
    public Principal getCallerPrincipal() {
        SecurityContext sc = null;
        if (runAs != null) { // Run As
            // return the principal associated with the old security context
            ComponentInvocation ci = invMgr.getCurrentInvocation();

            if (ci == null) {
                throw new InvocationException(); // 4646060
            }
            sc = (SecurityContext) ci.getOldSecurityContext();

        } else {
            // lets optimize a little. no need to look up oldsecctx
            // its the same as the new one
            sc = SecurityContext.getCurrent();
        }

        Principal prin;

        if (sc != null) {
            prin = sc.getCallerPrincipal();
        } else {
            prin = SecurityContext.getDefaultCallerPrincipal();
        }
        return prin;
    }

    public void destroy() {

        try {

            boolean wasInService = getPolicyFactory().inService(this.contextId);
            if (wasInService) {
                policy.refresh();
            }
            /*
             * all ejbs of module share same policy context, but each has its
             * own permission cache, which must be unregistered from factory to
             * avoid leak.
             */
            PermissionCacheFactory.removePermissionCache(uncheckedMethodPermissionCache);
            uncheckedMethodPermissionCache = null; 
            roleMapperFactory.removeAppNameForContext(this.contextId);

        } catch (PolicyContextException pce) {
            String msg = "ejbsm.could_not_delete";
            // Just log it.
            _logger.log(Level.WARNING, msg, pce);
        }
        probeProvider.securityManagerDestructionStartedEvent(ejbName);
        ejbSFM.getManager(contextId,ejbName,true);
        probeProvider.securityManagerDestructionEndedEvent(ejbName);
       
        probeProvider.securityManagerDestructionEvent(ejbName);
       
    }

    /**
     * This will return the subject associated with the current call. If the
     * run as subject is in effect. It will return that subject. This is done
     * to support the JACC specification which says if the runas principal is
     * in effect,  that principal should be used for making a component call.
     *
     * @return Subject the current subject. Null if this is not the run-as
     *         case
     */
    public Subject getCurrentSubject() {
        // just get the security context will return the empt subject
        // of the default securityContext when appropriate.
        return SecurityContext.getCurrent().getSubject();
    }

    /* This method is used by SecurityUtil runMethod to run the
     * action as the subject encapsulated in the current
     * SecurityContext.
     */
    public Object doAsPrivileged(PrivilegedExceptionAction pea)
            throws Throwable {

        SecurityContext sc = SecurityContext.getCurrent();
        Set principalSet = sc.getPrincipalSet();
        AccessControlContext acc =
                (AccessControlContext) accessControlContextCache.get(principalSet);

        if (acc == null) {
            final ProtectionDomain[] pdArray = new ProtectionDomain[1];
            pdArray[0] = getCachedProtectionDomain(principalSet, false);
            try {
                if (principalSet != null) {

                    final Subject s = sc.getSubject();

                    acc = (AccessControlContext)
                            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                                public Object run() throws Exception {
                                    return new AccessControlContext
                                            (new AccessControlContext(pdArray),
                                                    new SubjectDomainCombiner(s));
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
                _logger.log(Level.SEVERE,
                        "java_security.security_context_exception", e);
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
     * Runs a business method of an EJB within the bean's policy context.
     * The original policy context is restored after method execution.
     * This method should only be used by com.sun.enterprise.security.SecurityUtil.
     *
     * @param beanClassMethod the EJB business method
     * @param obj             the EJB bean instance
     * @param oa              parameters passed to beanClassMethod
     * @return return value from beanClassMethod
     * @throws java.lang.reflect.InvocationTargetException if the underlying method throws an exception
     * @throws Throwable                 other throwables in other cases
     */
    public Object runMethod(Method beanClassMethod, Object obj, Object[] oa)
            throws Throwable {
        String oldCtxID = setPolicyContext(this.contextId);
        Object ret = null;
        try {
            ret = beanClassMethod.invoke(obj, oa);
        } finally {
            resetPolicyContext(oldCtxID, this.contextId);
        }
        return ret;
    }

    private static void resetPolicyContext(final String newV, String oldV)
            throws Throwable {
        if (oldV != newV && newV != null && (oldV == null || !oldV.equals(newV))) {

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("JACC: Changing Policy Context ID: oldV = "
                        + oldV + " newV = " + newV);
            }
            try {
                AppservAccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        PolicyContext.setContextID(newV);
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
                throw cause;
            }
        }
    }

    private static String setPolicyContext(String newV) throws Throwable {
        String oldV = PolicyContext.getContextID();
        resetPolicyContext(newV, oldV);
        return oldV;
    }

    /**
     * This method is similiar to the runMethod, except it keeps the
     * semantics same as the one in reflection. On failure, if the
     * exception is caused due to reflection, it returns the
     * InvocationTargetException.  This method is called from the
     * containers for ejbTimeout, WebService and MDBs.
     *
     * @param beanClassMethod, the bean class method to be invoked
     * @param isLocal,         true if this invocation is through the local EJB view
     * @param o                the object on which this method is to be
     *                         invoked in this case the ejb,
     * @param oa               the parameters for the method,
     * @param c,               the container instance
     *                         can be a null value, where in the container will be queried to
     *                         find its security manager.
     * @return Object, the result of the execution of the method.
     */
    public Object invoke(Method beanClassMethod, boolean isLocal, Object o, Object[] oa)
            throws Throwable {

        final Method meth = beanClassMethod;
        final Object obj = o;
        final Object[] objArr = oa;
        Object ret = null;

        // Optimization.  Skip doAsPrivileged call if this is a local
        // invocation and the target ejb uses caller identity or the
        // System Security Manager is disabled.
        // Still need to execute it within the target bean's policy context.
        // see CR 6331550
        if ((isLocal && this.getUsesCallerIdentity()) ||
                System.getSecurityManager() == null) {
            ret = this.runMethod(meth, obj, objArr);
        } else {

            PrivilegedExceptionAction pea =
                    new PrivilegedExceptionAction() {
                        public Object run() throws Exception {
                            return meth.invoke(obj, objArr);
                        }
                    };

            try {
                ret = this.doAsPrivileged(pea);
            } catch (PrivilegedActionException pae) {
                Throwable cause = pae.getCause();
                throw cause;
            }
        }
        return ret;
    }

    @Override
    public void resetPolicyContext() {
        if (System.getSecurityManager() == null) {
            ((PolicyContextHandlerImpl)PolicyContextHandlerImpl.getInstance()).reset();
            PolicyContext.setContextID(null);
            return;
        }
        
        try {
                AppservAccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                         ((PolicyContextHandlerImpl)PolicyContextHandlerImpl.getInstance()).
                                 reset();
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
   
}

