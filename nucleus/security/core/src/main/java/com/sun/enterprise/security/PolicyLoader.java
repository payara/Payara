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
// Portions Copyright [2017-2025] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security;

import static com.sun.enterprise.security.SecurityLoggerInfo.policyConfigFactoryNotDefined;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyFactoryOverride;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyInstallError;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyNoSuchName;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyNotLoadingWarning;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyProviderConfigOverrideMsg;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyProviderConfigOverrideWarning;
import static com.sun.enterprise.security.SecurityLoggerInfo.policyReadingError;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.util.i18n.StringManager;
import java.lang.reflect.InvocationTargetException;
import javassist.ClassPool;
import javassist.CtClass;
import static javassist.Modifier.PUBLIC;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import java.security.Policy;

/**
 * Loads the default JACC Policy Provider into the system.
 *
 * @author Harpreet Singh
 * @author Jyri J. Virkki
 */
@Service
@Singleton
public class PolicyLoader {
    
    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();
    private static final StringManager STRING_MANAGER = StringManager.getManager(PolicyLoader.class);
    
    private static final String POLICY_PROVIDER_14 = "jakarta.security.jacc.policy.provider";
    private static final String POLICY_PROVIDER_13 = "jakarta.security.jacc.auth.policy.provider";
    private static final String POLICY_CONF_FACTORY = "jakarta.security.jacc.PolicyConfigurationFactory.provider";
    private static final String POLICY_PROP_PREFIX = "com.sun.enterprise.jaccprovider.property.";

    private static final String AUTH_PROXY_HANDLER = "com.sun.enterprise.security.AuthenticationProxyHandler";
    private static final String DEFAULT_POLICY_PROVIDER = "fish.payara.security.jacc.provider.PolicyProviderImpl";

    @Inject
    @Named(DEFAULT_INSTANCE_NAME)
    private SecurityService securityService;

    @Inject
    private IterableProvider<JaccProvider> jaccProviders;
   
    private boolean isPolicyInstalled;

    /**
     * Attempts to install the JACC policy-provider (authorization module) 
     * 
     * <p>
     * The policy-provider element in domain.xml is consulted for the class to use.
     * 
     * <p>
     * Note that if the <code>jakarta.security.jacc.policy.provider</code> system property is set it will override 
     * the domain.xml configuration. This will normally not be the case in Payara.
     *
     * <P>
     * The J2EE 1.3 property <code>jakarta.security.jacc.auth.policy.provider</code> is checked as a last resort. 
     * It should not be set in J2EE 1.4.
     */
    public void loadPolicy() {
        if (isPolicyInstalled) {
            LOGGER.fine("Policy already installed. Will not re-install.");
            return;
        }
        
        // Get configuration object for the JACC provider (which handles the policies)
        JaccProvider jaccProvider = getConfiguredJaccProvider();
        
        // Set config properties (see method comments)
        setPolicyConfigurationFactory(jaccProvider);

        boolean j2ee13 = false;

        // Get policy class name via the "normal" ways
        String policyClassName = getConfiguredPolicyClassName(jaccProvider);

        if (policyClassName == null) {
            // Try legacy fallback (at some point we might want to stop supporting this)
            policyClassName = System.getProperty(POLICY_PROVIDER_13);
            if (policyClassName != null) {
                // Warn user j2ee13 property is being used
                j2ee13 = true;
                LOGGER.log(WARNING, policyProviderConfigOverrideWarning, new String[] { POLICY_PROVIDER_13, policyClassName });
            }
        }
        
        if (policyClassName != null) {
            // Now install the policy provider if one was identified
            installPolicyFromClassName(policyClassName, j2ee13);
        } else {
            // No value for policy provider found
            LOGGER.warning(policyNotLoadingWarning);
        }
    }
    
    private String getConfiguredPolicyClassName(JaccProvider jaccProvider) {
        // Check if system property is set
        String policyClassName = System.getProperty(POLICY_PROVIDER_14);

        if (policyClassName != null) {
            // Inform user domain.xml is being ignored
            LOGGER.log(INFO, policyProviderConfigOverrideMsg, new String[] { POLICY_PROVIDER_14, policyClassName });
        } else if (jaccProvider != null) {
            // Otherwise obtain JACC policy-provider from domain.xml
            policyClassName = jaccProvider.getPolicyProvider();
        }
        
        return policyClassName;
    }

    /**
     * Returns a JaccProvider object representing the jacc element from domain.xml which is configured in security-service.
     *
     * @return The config object or null on errors.
     */
    private JaccProvider getConfiguredJaccProvider() {
        JaccProvider jaccProvider = null;
        try {
            String name = securityService.getJacc();
            jaccProvider = getJaccProviderByName(name);
            
            if (jaccProvider == null) {
                LOGGER.log(WARNING, policyNoSuchName, name);
            }
        } catch (Exception e) {
            LOGGER.warning(policyReadingError);
            jaccProvider = null;
        }
        
        return jaccProvider;
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
     * <P>
     * The POLICY_CONF_FACTORY property is consumed by the jacc-api as documented in JACC specification. It's value is set
     * here to the value given in domain.xml <i>unless</i> it is already set in which case the value is not modified.
     *
     * <P>
     * Then and properties associated with this jacc provider from domain.xml are set as internal properties prefixed with
     * POLICY_PROP_PREFIX. This is currently a workaround for bug 4846938. A cleaner interface should be adopted.
     *
     */
    private void setPolicyConfigurationFactory(JaccProvider jaccProvider) {
        if (jaccProvider == null) {
            return;
        }
        
        // Handle JACC-specified property for factory
        // TODO:V3 system property being read here
        String prop = System.getProperty(POLICY_CONF_FACTORY);
        if (prop != null) {
            // Warn user of override
            LOGGER.log(WARNING, policyFactoryOverride, new String[] { POLICY_CONF_FACTORY, prop });

        } else {
            // Use domain.xml value by setting the property to it
            String factory = jaccProvider.getPolicyConfigurationFactoryProvider();
            if (factory == null) {
                LOGGER.log(WARNING, policyConfigFactoryNotDefined);
            } else {
                System.setProperty(POLICY_CONF_FACTORY, factory);
            }
        }

        // Next, make properties of this JACC provider available to provider
        for (Property jaccProperty : jaccProvider.getProperty()) {
            String name = POLICY_PROP_PREFIX + jaccProperty.getName();
            String value = jaccProperty.getValue();
            LOGGER.log(FINEST, () -> "PolicyLoader set [" + name + "] to [" + value + "]");
            
            System.setProperty(name, value);
        }
    }

    private void installPolicyFromClassName(String policyClassName, boolean j2ee13) {
        try {
            LOGGER.log(INFO, SecurityLoggerInfo.policyLoading, policyClassName);
            Object policyInstance;
            if (System.getSecurityManager() == null
                    || policyClassName.equals(DEFAULT_POLICY_PROVIDER)) {
                policyInstance = loadClass(policyClassName);
            } else {
                policyInstance = loadPolicyAsProxy(policyClassName);
            }
            
            installPolicy14(policyInstance);

        } catch (Exception e) {
            LOGGER.log(SEVERE, policyInstallError, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        
        // Success.
        LOGGER.log(FINE, () -> "Policy set to: " + policyClassName);
        isPolicyInstalled = true;
    }
    
    private void installPolicy14(Object policyInstance) throws ReflectiveOperationException {
        if (!(policyInstance instanceof java.security.Policy)) {
            throw new RuntimeException(STRING_MANAGER.getString("enterprise.security.plcyload.not14"));
        }

        Policy policy = (java.security.Policy) policyInstance;
        try {
            Policy.setPolicy(policy);
        } catch (UnsupportedOperationException e) {
            Class<?> authorizationServiceClass = Class.forName("org.glassfish.exousia.AuthorizationService");

            Method setPolicyMethod = authorizationServiceClass.getMethod("setPolicy", Policy.class);
            setPolicyMethod.invoke(null, policy);
        }

        // TODO: causing ClassCircularity error when SM ON and deployment use library feature and
        // ApplibClassLoader
        //
        // It is likely a problem caused by the way class loading is done in this case.
        if (System.getSecurityManager() == null) {
            policy.refresh();
        }
    }
    
    
    private Object loadClass(String policyClassName)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {
        return Thread.currentThread()
                .getContextClassLoader()
                .loadClass(policyClassName)
                .getDeclaredConstructor()
                .newInstance();
    }

    private Policy loadPolicyAsProxy(String javaPolicyClassName) throws Exception {

        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(javaPolicyClassName);
        clazz.defrost();
        clazz.setModifiers(PUBLIC);
        Class targetClass = clazz.toClass(
                Thread.currentThread()
                        .getContextClassLoader()
                        .loadClass(System.getProperty(POLICY_CONF_FACTORY)));

        ProxyObject instance;
        
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(targetClass);
        instance = (ProxyObject) factory.createClass().getDeclaredConstructor().newInstance();

        clazz = pool.get(AUTH_PROXY_HANDLER);
        Class handlerClass = clazz.toClass(targetClass.getClassLoader(), targetClass.getProtectionDomain());
        MethodHandler handler = (MethodHandler) handlerClass
                .getDeclaredConstructor(Policy.class)
                .newInstance(Policy.getPolicy());
        instance.setHandler(handler);

        if (!(instance instanceof Policy)) {
            throw new RuntimeException(STRING_MANAGER.getString("enterprise.security.plcyload.not14"));
        }

        instance.toString();

        return (Policy) instance;
    }

}
