/*
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
// Portions Copyright [2019-2024] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.ee.authorization;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.util.IASSecurityException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyFactory;
import java.util.Collection;
import org.glassfish.deployment.versioning.VersioningUtils;

/**
 * This utility class contains several methods for working with (Jakarta) Authorization.
 *
 * @author Harpreet Singh
 * @author Shing Wai Chan
 */
public class AuthorizationUtil {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AuthorizationUtil.class);
    public static final String VENDOR_PRESENT = "com.sun.enterprise.security.ee.provider.jaccvendorpresent";

    // set in PolicyLoader from domain.xml
    private static final String REPOSITORY_HOME_PROP = "com.sun.enterprise.jaccprovider.property.repository";

    // TODO remove use of system property
    // The repository is defined in PolicyFileMgr.
    // It is repeated here since JACC provider is not reference directly.
    public static final String repository = System.getProperty(REPOSITORY_HOME_PROP);
    
    /**
     * Inform the policy module to take the named policy context out of service. The policy context is transitioned to the
     * deleted state. In our provider implementation, the corresponding policy file is deleted, as the presence of a policy
     * file in the repository is how we persistently remember which policy contexts are in service.
     *
     * @param String contextId - the module id which serves to identify the corresponding policy context. The name shall not be
     * null.
     */
    public static void removePolicy(String contextId) throws IASSecurityException {
        if (contextId == null) {
            throw new IASSecurityException("Invalid Module Name");
        }

        try {
            boolean wasInService = PolicyConfigurationFactory.getPolicyConfigurationFactory().inService(contextId);

            // Find the PolicyConfig and delete it.
            PolicyConfiguration policyConfiguration = PolicyConfigurationFactory.getPolicyConfigurationFactory().getPolicyConfiguration(contextId, false);
            policyConfiguration.delete();

            // Only do refresh policy if the deleted context was in service
            if (wasInService) {
                PolicyFactory.getPolicyFactory().getPolicy().refresh();
            }

        } catch (ClassNotFoundException cnfe) {
            throw new IASSecurityException(localStrings.getLocalString(
                "enterprise.security.securityutil.classnotfound",
                "Could not find PolicyConfigurationFactory class. Check jakarta.security.jacc.PolicyConfigurationFactory.provider property"));
        } catch (PolicyContextException pce) {
            throw new IASSecurityException(pce.toString());
        }
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
