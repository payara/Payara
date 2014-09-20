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

import java.util.logging.*;

//V3:Commented import com.sun.enterprise.server.ApplicationServer;
import com.sun.enterprise.config.serverbeans.JaccProvider;
//V3:Commented import com.sun.enterprise.config.serverbeans.ElementProperty;
//V3:Commented import com.sun.enterprise.config.ConfigContext;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.util.i18n.StringManager;
import java.util.List;
import org.glassfish.api.admin.ServerEnvironment;
import javax.inject.Inject;
import javax.inject.Named;


import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 * Loads the Default Policy File into the system.
 *
 * @author Harpreet Singh
 * @author Jyri J. Virkki
 *
 */
@Service
@Singleton
public class PolicyLoader{
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private SecurityService securityService;
    
    @Inject
    private IterableProvider<JaccProvider> jaccProviders;
     
    private static Logger _logger = null;
    static {
        _logger = SecurityLoggerInfo.getLogger();
    }
    private static StringManager sm = StringManager.getManager(PolicyLoader.class);

    private static final String POLICY_PROVIDER_14 = 
        "javax.security.jacc.policy.provider";
    private static final String POLICY_PROVIDER_13 = 
        "javax.security.jacc.auth.policy.provider";
    private static final String POLICY_CONF_FACTORY = 
        "javax.security.jacc.PolicyConfigurationFactory.provider";
    private static final String POLICY_PROP_PREFIX =
        "com.sun.enterprise.jaccprovider.property.";
    private boolean isPolicyInstalled = false;

    
    /**
     * Attempts to install the policy-provider. The policy-provider
     * element in domain.xml is consulted for the class to use. Note
     * that if the javax.security.jacc.policy.provider system property
     * is set it will override the domain.xml configuration. This will
     * normally not be the case in S1AS.
     *
     * <P>The J2EE 1.3 property javax.security.jacc.auth.policy.provider is
     * checked as a last resort. It should not be set in J2EE 1.4.
     *
     */
    public void loadPolicy() {

        if (isPolicyInstalled) {
            _logger.log(Level.FINE,
                        "Policy already installed. Will not re-install.");
            return;
        }
        // get config object
        JaccProvider jacc = getConfiguredJaccProvider();
        // set config properties (see method comments)
        setPolicyConfigurationFactory(jacc);
        
        boolean j2ee13 = false;
        

        // check if system property is set
        String javaPolicy = System.getProperty(POLICY_PROVIDER_14);

        if (javaPolicy !=null) {
            // inform user domain.xml is being ignored
            _logger.log(Level.INFO, SecurityLoggerInfo.policyProviderConfigOverrideMsg,
                        new String[] { POLICY_PROVIDER_14, javaPolicy } );
        } else {
            // otherwise obtain JACC policy-provider from domain.xml
            if (jacc != null) {
                javaPolicy = jacc.getPolicyProvider();
            }
        }
        
        if (javaPolicy == null) {
            javaPolicy = System.getProperty(POLICY_PROVIDER_13);
            if (javaPolicy != null) {
                // warn user j2ee13 property is being used
                j2ee13 = true;
                _logger.log(Level.WARNING, SecurityLoggerInfo.policyProviderConfigOverrideWarning,
                            new String[] { POLICY_PROVIDER_13, javaPolicy} );
            }
        }

        // now install the policy provider if one was identified
        if (javaPolicy != null) {

            try {
                _logger.log(Level.INFO, SecurityLoggerInfo.policyLoading, javaPolicy);
                
                //Object obj = Class.forName(javaPolicy).newInstance();
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class javaPolicyClass = loader.loadClass(javaPolicy);
                Object obj = javaPolicyClass.newInstance();
                if (j2ee13) {
                    // Use JDK 1.3 classes if j2ee1 3 property being used
                    if (!(obj instanceof javax.security.auth.Policy)) {
                        String msg = 
                            sm.getString("enterprise.security.plcyload.not13");
                        throw new RuntimeException(msg);
                    }
                    javax.security.auth.Policy policy =
                        (javax.security.auth.Policy)obj;
                    javax.security.auth.Policy.setPolicy(policy);
                    policy.refresh();
                    
                } else {
                    // Otherwise use JDK 1.4 classes.
                    if (!(obj instanceof java.security.Policy)) {
                        String msg = 
                            sm.getString("enterprise.security.plcyload.not14");
                        throw new RuntimeException(msg);
                    }
                    java.security.Policy policy = (java.security.Policy)obj;
                    java.security.Policy.setPolicy(policy);
                    //TODO: causing ClassCircularity error when SM ON and
                    //deployment use library feature and ApplibClassLoader
                    //it is likely a problem caused by the way classloading is done
                    //in this case.
                    if (System.getSecurityManager() == null) {
                        policy.refresh();
                    }
                }

            } catch (Exception e) {
                _logger.log(Level.SEVERE, SecurityLoggerInfo.policyInstallError,
                            e.getLocalizedMessage());
                throw new RuntimeException(e);
            }
            // Success.
            _logger.fine("Policy set to: " + javaPolicy);
            isPolicyInstalled = true;
            
        } else {
            // no value for policy provider found
            _logger.warning(SecurityLoggerInfo.policyNotLoadingWarning);
        }
    }


    /**
     * Returns a JaccProvider object representing the jacc element from
     * domain.xml which is configured in security-service.
     *
     * @return The config object or null on errors.
     *
     */
    private JaccProvider getConfiguredJaccProvider() {
        JaccProvider jacc = null;
        try {
            String name = securityService.getJacc();
            jacc = getJaccProviderByName(name);
            if (jacc == null) {
                _logger.log(Level.WARNING, SecurityLoggerInfo.policyNoSuchName, name);
            }
        } catch (Exception e) {
            _logger.warning(SecurityLoggerInfo.policyReadingError);
            jacc = null;
        }
        return jacc;
    }

    private JaccProvider getJaccProviderByName(String name) {
       if (jaccProviders == null || name == null) {
           return null;    
       }

       for (JaccProvider jaccProvider : jaccProviders) {
           if (jaccProvider.getName().equals(name)) {
               return jaccProvider;
           }
       }
       return null;
    }
    
    /**
     * Set internal properties based on domain.xml configuration.
     *
     * <P>The POLICY_CONF_FACTORY property is consumed by the jacc-api
     * as documented in JACC specification. It's value is set here to the
     * value given in domain.xml <i>unless</i> it is already set in which
     * case the value is not modified.
     *
     * <P>Then and properties associated with this jacc provider from
     * domain.xml are set as internal properties prefixed with
     * POLICY_PROP_PREFIX. This is currently a workaround for bug 4846938.
     * A cleaner interface should be adopted.
     *
     */
    private void setPolicyConfigurationFactory(JaccProvider jacc) {

        if (jacc == null) {
            return;
        }
        // Handle JACC-specified property for factory
        //TODO:V3 system property being read here
        String prop = System.getProperty(POLICY_CONF_FACTORY);
        if (prop != null) {
            // warn user of override
            _logger.log(Level.WARNING, SecurityLoggerInfo.policyFactoryOverride,
                        new String[] { POLICY_CONF_FACTORY, prop } );
            
        } else {
            // use domain.xml value by setting the property to it
            String factory = jacc.getPolicyConfigurationFactoryProvider();
            if (factory == null) {
                _logger.log(Level.WARNING, SecurityLoggerInfo.policyConfigFactoryNotDefined);
            } else {
                System.setProperty(POLICY_CONF_FACTORY, factory);
            }
        }
        
        // Next, make properties of this jacc provider available to provider
        List<Property> props = jacc.getProperty();
        for (Property p: props) {
            String name = POLICY_PROP_PREFIX + p.getName();
            String value = p.getValue();
            _logger.finest("PolicyLoader set ["+name+"] to ["+value+"]");
            System.setProperty(name, value);
        }
    }
}
