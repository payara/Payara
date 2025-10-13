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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee;

import static java.util.logging.Level.FINE;

import java.util.Collection;
import java.util.logging.Logger;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyFactory;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.deployment.versioning.VersioningUtils;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.SecurityRoleMapperFactoryGen;
import com.sun.enterprise.security.util.IASSecurityException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;

/**
 * This utility class contains Jakarta Authorization related utilities.
 * 
 * <p>
 * This is mostly used by the SecurityDeployer, but the getContextID method
 * is broadly shared, and the other public methods are used by the EJBSecurityManagaer 
 * and EJBDeployer.
 * 
 * @author Harpreet Singh
 * @author Shing Wai Chan
 */
public class SecurityUtil {
    
    private static final Logger _logger = LogDomains.getLogger(SecurityUtil.class, LogDomains.SECURITY_LOGGER);
    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SecurityUtil.class);

    // Set in PolicyLoader from domain.xml
    private static final String REPOSITORY_HOME_PROP = "com.sun.enterprise.jaccprovider.property.repository";
    
    // TODO remove use of system property
    // The repository is defined in PolicyFileMgr.
    // It is repeated here since JACC provider is not reference directly.
    public static final String repository = System.getProperty(REPOSITORY_HOME_PROP);
    
    
    public static String getContextID(WebBundleDescriptor webBundleDescriptor) {
        if (webBundleDescriptor == null) {
            return null;
        }
        
        return 
            VersioningUtils.getRepositoryName(
                webBundleDescriptor.getApplication().getRegistrationName()) + 
                '/' + 
                webBundleDescriptor.getUniqueFriendlyId();
    }
    
    public static String getContextID(EjbBundleDescriptor ejbBundleDesc) {
        String cid = null;
        if (ejbBundleDesc != null) {
            /*
             * detect special case of EJBs embedded in a war, and make sure psuedo policy context id is unique within app
             */
            Object root = ejbBundleDesc.getModuleDescriptor().getDescriptor();
            if ((root != ejbBundleDesc) && (root instanceof WebBundleDescriptor)) {
                cid = createUniquePseudoModuleID(ejbBundleDesc);
            } else {
                cid = VersioningUtils.getRepositoryName(ejbBundleDesc.getApplication().getRegistrationName()) + '/' + ejbBundleDesc.getUniqueFriendlyId();
            }
        }
        return cid;
    }
    
    /**
     * Inform the policy module to take the named policy context out of service. The policy context is transitioned to the
     * deleted state. In our provider implementation, the corresponding policy file is deleted, as the presence of a policy
     * file in the repository is how we persistently remember which policy contexts are in service.
     * 
     * @param String name - the module id which serves to identify the corresponding policy context. The name shall not be
     * null.
     */
    public static void removePolicy(String name) throws IASSecurityException {
        if (name == null) {
            throw new IASSecurityException("Invalid Module Name");
        }
        
        try {
            boolean wasInService = PolicyConfigurationFactory.getPolicyConfigurationFactory().inService(name);
            
            // Find the PolicyConfig and delete it.
            PolicyConfiguration pc = PolicyConfigurationFactory.getPolicyConfigurationFactory().getPolicyConfiguration(name, false);
            pc.delete();
            
            // Only do refresh policy if the deleted context was in service
            if (wasInService) {
                PolicyFactory.getPolicyFactory().getPolicy().refresh();
            }

        } catch (java.lang.ClassNotFoundException cnfe) {
            throw new IASSecurityException(localStrings.getLocalString(
                "enterprise.security.securityutil.classnotfound",
                "Could not find PolicyConfigurationFactory class. Check jakarta.security.jacc.PolicyConfigurationFactory.provider property"));
        } catch (PolicyContextException pce) {
            throw new IASSecurityException(pce.toString());
        }
    }
    
    public static SecurityRoleMapperFactory getRoleMapperFactory() {
        SecurityRoleMapperFactory factory = SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory();
        if (factory == null) {
            throw new IllegalArgumentException("This application has no role mapper factory defined");
        }
        return factory;
    }

    public static void removeRoleMapper(DeploymentContext dc) {
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        if (params.origin != OpsParams.Origin.undeploy) {
            return;
        }
        String appName = params.name();
        getRoleMapperFactory().removeRoleMapper(appName);
    }
    
    

    /**
     * This method obtains the policy configuration object corresponding to the name, and causes the corresponding policy
     * statements to be put in service. This method also informs the policy module to refresh its in service policy
     * contexts. Note that policy statements have already been added to the pc, this method works to put them in Service.
     * 
     * @param String name - the module id which serves to identify the corresponding policy context. The name shall not be
     * null. If the underlying PolicyModule is the RI PolicyModule, A SecurityRoleMapper must have been bound to the policy
     * context before this method is called or the embedded call to pc.commit will throw an exception.
     */
    static void generatePolicyFile(String name) throws IASSecurityException {
        if (name == null) {
            throw new IASSecurityException("Invalid Module Name");
        }

        try {
            boolean inService = PolicyConfigurationFactory.getPolicyConfigurationFactory().inService(name);
            if (!inService) {
                // find the PolicyConfig using remove=false to ensure policy stmts
                // are retained.

                // Note that it is presumed that the pc exists, and that
                // it is populated with the desired policy statements.
                // If this is not true, the call to commit will not
                // result in the correct policy statements being made
                // available to the policy module.
                PolicyConfigurationFactory pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
                PolicyConfiguration pc = pcf.getPolicyConfiguration(name, false);
                pc.commit();
                
                if (_logger.isLoggable(FINE)) {
                    _logger.fine("JACC: committed policy for context: " + name);
                }
            }

            PolicyFactory.getPolicyFactory().getPolicy().refresh();
        } catch (ClassNotFoundException | PolicyContextException cnfe) {
            throw new IASSecurityException(cnfe);
        }
    }

    /**
     * This method obtains the policy configuration object corresponding to the name, and links it, for roleMapping purposes
     * to another. If the pc is already InService when this method is called, this method does nothing.
     * 
     * @param String name - the module id which serves to identify the corresponding policy context. The name shall not be
     * null.
     * @param String linkName - the module id of the module being linked to this context. This value may be null, in which
     * case, no link is done, but the inService state of the named PC is returned.
     * @param boolean lastInService - the inService state returned by the previous call to this method. The value of this
     * argument is only significant when linkName is not null.
     * @return boolean if linkName is null, returns the inService state of the PC identified in the name argument. Otherwise
     * returns the value passed to lastInService.
     */
    static boolean linkPolicyFile(String name, String linkName, boolean lastInService) throws IASSecurityException {
        boolean rvalue = lastInService;

        if (name == null) {
            throw new IASSecurityException("Invalid Module Name");
        }
        
        try {
            PolicyConfigurationFactory pcf = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            boolean inService = pcf.inService(name);

            if (linkName == null) {
                rvalue = inService;
            } else if (inService == lastInService) {

                // only do the link if the named PC is not inService.
                if (!inService) {

                    // find the PolicyConfigs using remove=false to ensure policy stmts
                    // are retained.

                    PolicyConfiguration pc = pcf.getPolicyConfiguration(name, false);
                    PolicyConfiguration linkPc = pcf.getPolicyConfiguration(linkName, false);
                    pc.linkConfiguration(linkPc);
                }
            } else {
                throw new IASSecurityException("Inconsistent Module State");
            }

        } catch (ClassNotFoundException cnfe) {
            throw new IASSecurityException(
                localStrings.getLocalString(
                        "enterprise.security.securityutil.classnotfound",
                        "Could not find PolicyConfigurationFactory class. Check jakarta.security.jacc.PolicyConfigurationFactory.provider property"));
        } catch (jakarta.security.jacc.PolicyContextException pce) {
            throw new IASSecurityException(pce.toString());
        }
        
        return rvalue;
    }
    
    /**
     * create pseudo module context id, and make sure it is unique, by chacking it against the names of all the other
     * modules in the app.
     * 
     * @param ejbDesc
     * @return
     */
    private static String createUniquePseudoModuleID(EjbBundleDescriptor ejbDesc) {

        Application app = ejbDesc.getApplication();
        Collection<WebBundleDescriptor> webModules = app.getBundleDescriptors(WebBundleDescriptor.class);
        Collection<EjbBundleDescriptor> ejbModules = app.getBundleDescriptors(EjbBundleDescriptor.class);

        String moduleName = ejbDesc.getUniqueFriendlyId();
        String pseudonym;
        int uniquifier = 0;
        boolean unique;
        do {
            unique = true;
            pseudonym = moduleName + (uniquifier == 0 ? "_internal" : "_internal_" + uniquifier);
            if (webModules != null) {
                for (WebBundleDescriptor w : webModules) {
                    if (pseudonym.equals(w.getUniqueFriendlyId())) {
                        unique = false;
                        break;
                    }
                }
            }
            
            if (unique && ejbModules != null) {
                for (EjbBundleDescriptor e : ejbModules) {
                    if (pseudonym.equals(e.getUniqueFriendlyId())) {
                        unique = false;
                        break;
                    }
                }
            }
            uniquifier += 1;

        } while (!unique);

        return VersioningUtils.getRepositoryName(app.getRegistrationName()) + "/" + pseudonym;
    }
}
