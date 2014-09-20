/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamespacePrefixes;
import org.glassfish.hk2.api.ServiceLocator;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Proxy for javax.validation based lookups (java:comp/Validator, java:comp/ValidatorFactory) when CDI enabled
 */
@Service
@Named("ValidationNamingProxy")
public class ValidationNamingProxy implements NamedNamingObjectProxy {

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private WeldDeployer weldDeployer;

    static final String VALIDATOR_CONTEXT = "java:comp/Validator";
    static final String VALIDATOR_FACTORY_CONTEXT = "java:comp/ValidatorFactory";

    /**
     * get and create an instance of a bean from the beanManager
     *
     * @param beanManager
     * @param clazz
     * @return
     */
    private static final Object getAndCreateBean(BeanManager beanManager, Class clazz) {

        Set<Bean<?>> beans = beanManager.getBeans(clazz);

        if (!beans.isEmpty()) {
            Bean<?> bean = beans.iterator().next();

            return bean.create(null);
        }

        return null;
    }

    @Override
    public Object handle(String name) throws NamingException {

        // delegate to the java:comp/BeanManager handler to obtain the appropriate BeanManager
        BeanManager beanManager = obtainBeanManager();

        if (beanManager == null) {
          return null; // There is no bean manager available, return and let BeanValidatorNamingProxy handle lookup..
        }

        if (VALIDATOR_FACTORY_CONTEXT.equals(name)) {

            try {

                ValidatorFactory validatorFactory = (ValidatorFactory) getAndCreateBean(beanManager, ValidatorFactory.class);

                if (validatorFactory != null) {
                    return validatorFactory;
                } else {
                    throw new NamingException("Error retrieving " + name);
                }

            } catch (Throwable t) {
                NamingException ne = new NamingException("Error retrieving " + name);
                ne.initCause(t);
                throw ne;
            }
        } else if (VALIDATOR_CONTEXT.equals(name)) {

            try {

                Validator validator = (Validator) getAndCreateBean(beanManager, Validator.class);

                if (validator != null) {
                    return validator;
                } else {
                    throw new NamingException("Error retrieving " + name);
                }

            } catch (Throwable t) {
                NamingException ne = new NamingException("Error retrieving " + name);
                ne.initCause(t);
                throw ne;
            }
        } else {
            throw new NamingException("wrong handler for " + name);
        }
    }

    /**
     * Obtain the BeanManagerNamingProxy from hk2, so the BeanManager can be looked up
     *
     * @throws NamingException
     */
    private synchronized BeanManager obtainBeanManager() throws NamingException {

        BeanManager beanManager = null;

        // Use invocation context to find applicable BeanDeploymentArchive.
        ComponentInvocation inv = invocationManager.getCurrentInvocation();

        if( inv != null ) {

            JndiNameEnvironment componentEnv = compEnvManager.getJndiNameEnvironment(inv.getComponentId());

            if( componentEnv != null ) {

                BundleDescriptor bundle = null;

                if( componentEnv instanceof EjbDescriptor) {
                    bundle = (BundleDescriptor)
                            ((EjbDescriptor) componentEnv).getEjbBundleDescriptor().
                                    getModuleDescriptor().getDescriptor();

                } else if( componentEnv instanceof WebBundleDescriptor) {
                    bundle = (BundleDescriptor) componentEnv;

                }

                if( bundle != null ) {
                    BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(bundle);
                    if( bda != null ) {
                        WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());

                        beanManager = bootstrap.getManager(bda);
                    }
                }

            }
        }

        return beanManager;
    }
}
