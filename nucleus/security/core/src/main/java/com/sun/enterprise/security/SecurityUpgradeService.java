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

package com.sun.enterprise.security;

import com.sun.enterprise.config.serverbeans.*;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;


/**
 *The only thing that needs to added Extra for SecurityService migration
 * is the addition of the new JACC provider. This would be required when
 * migrating from V2, for V3-Prelude it is already present.
 *
 * The rest of the security related upgrade is handled implicitly by the actions of the
 * upgrade service itself.
 * 
 */

@Service
public class SecurityUpgradeService implements ConfigurationUpgrade, PostConstruct {

    @Inject
    Configs configs;

    @Inject
    ServerEnvironment env;

    private static final String DIR_GENERATED_POLICY = "generated" + File.separator + "policy";
    private static final String DIR_CONFIG = "config";
    private static final String JKS = ".jks";
    private static final String NSS = ".db";
  //  private static final String KEYSTORE = "keystore.jks";
  //  private static final String TRUSTSTORE = "cacerts.jks";

    private static final String JDBC_REALM_CLASSNAME = "com.sun.enterprise.security.ee.auth.realm.jdbc.JDBCRealm";
    public static final String PARAM_DIGEST_ALGORITHM = "digest-algorithm";
    private static final Logger _logger = SecurityLoggerInfo.getLogger();

    
    public void postConstruct()  {
        for (Config config : configs.getConfig()) {
            SecurityService service = config.getSecurityService();
            if (service != null) {
                upgradeJACCProvider(service);
            }
        }

        //Clear up the old policy files for applications
        String instanceRoot = env.getInstanceRoot().getAbsolutePath();
        File genPolicyDir = new File(instanceRoot, DIR_GENERATED_POLICY);
        if(genPolicyDir != null) {
            File[] applicationDirs = genPolicyDir.listFiles();
            if(applicationDirs != null) {
                for(File policyDir:applicationDirs) {
                    deleteFile(policyDir);
                }
            }
        }

        //Update an existing JDBC realm-Change the digest algorithm to MD5 if none exists
        //Since the default algorithm is SHA-256 in v3.1, but was MD5 prior to 3.1

        for (Config config : configs.getConfig()) {
            SecurityService service = config.getSecurityService();
            List<AuthRealm> authRealms = service.getAuthRealm();

            try {
                for (AuthRealm authRealm : authRealms) {
                    if (JDBC_REALM_CLASSNAME.equals(authRealm.getClassname())) {
                        Property digestAlgoProp = authRealm.getProperty(PARAM_DIGEST_ALGORITHM);
                        if (digestAlgoProp != null) {
                            String digestAlgo = digestAlgoProp.getValue();
                            if (digestAlgo == null || digestAlgo.isEmpty()) {
                                digestAlgoProp.setValue("MD5");
                            }
                        } else {
                            ConfigSupport.apply(new SingleConfigCode<AuthRealm>() {
                                public Object run(AuthRealm updatedAuthRealm) throws PropertyVetoException, TransactionFailure {
                                    Property prop1 = updatedAuthRealm.createChild(Property.class);
                                    prop1.setName(PARAM_DIGEST_ALGORITHM);
                                    prop1.setValue("MD5");
                                    updatedAuthRealm.getProperty().add(prop1);
                                    return null;
                                }
                            }, authRealm);
                        }
                    }
                }
            } catch (PropertyVetoException pve) {
                _logger.log(Level.SEVERE, SecurityLoggerInfo.securityUpgradeServiceException, pve);
                throw new RuntimeException(pve);
            } catch (TransactionFailure tf) {
               _logger.log(Level.SEVERE, SecurityLoggerInfo.securityUpgradeServiceException, tf);
                throw new RuntimeException(tf);

            }
        }

        //Detect an NSS upgrade scenario and point to the steps wiki

        if (requiresSecureAdmin()) {

            _logger.log(Level.WARNING, SecurityLoggerInfo.securityUpgradeServiceWarning);
        }

    }


    /*
     * Method to detect an NSS install.
     */

    public boolean requiresSecureAdmin() {

        String instanceRoot = env.getInstanceRoot().getAbsolutePath();
        File configDir = new File(instanceRoot, "config");
        //default KS password


        if (configDir.isDirectory()) {
            for (File configFile : configDir.listFiles()) {
                    if (configFile.getName().endsWith(NSS)) {
                        return true;
                    }
                }
            }

        return false;
    }

    private void upgradeJACCProvider(SecurityService securityService) {
        try {
            List<JaccProvider> jaccProviders = securityService.getJaccProvider();
            for (JaccProvider jacc : jaccProviders) {
                if ("com.sun.enterprise.security.jacc.provider.SimplePolicyConfigurationFactory".equals(jacc.getPolicyConfigurationFactoryProvider())) {
                    //simple policy provider already present
                    return;
                }
            }
            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {
                @Override
                public Object run(SecurityService secServ) throws PropertyVetoException, TransactionFailure {
                    JaccProvider jacc = secServ.createChild(JaccProvider.class);
                    //add the simple provider to the domain's security service
                    jacc.setName("simple");
                    jacc.setPolicyConfigurationFactoryProvider("com.sun.enterprise.security.jacc.provider.SimplePolicyConfigurationFactory");
                    jacc.setPolicyProvider("com.sun.enterprise.security.jacc.provider.SimplePolicyProvider");
                    secServ.getJaccProvider().add(jacc);
                    return secServ;
                }
            }, securityService);
        } catch (TransactionFailure ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }

    }


    private boolean deleteFile(File path) {
        if (path != null && path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for(File file:files) {
                    if(file.isDirectory()){
                        deleteFile(file);
                        if(file.delete())
                            continue;
                    }
                    else {
                        if(file.delete())
                            continue;
                    }
                }
            }
            if(!path.delete()) {
                return false;
            }
        }
        return true;
    }

}
