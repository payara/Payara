/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.services;

import org.glassfish.ejb.api.EjbContainerServices;
import org.glassfish.internal.api.Globals;
import org.jboss.weld.injection.spi.InjectionContext;
import org.jboss.weld.injection.spi.InjectionServices;
import org.jvnet.hk2.component.Habitat;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.ManagedBeanDescriptor;


public class InjectionServicesImpl implements InjectionServices {

    private InjectionManager injectionManager;

    // Associated bundle context
    private BundleDescriptor bundleContext;

    public InjectionServicesImpl(InjectionManager injectionMgr, BundleDescriptor context) {
        injectionManager = injectionMgr;
        bundleContext = context;
    }


    public <T> void aroundInject(InjectionContext<T> injectionContext) {

        try {


            Habitat h = Globals.getDefaultHabitat();
            ComponentEnvManager compEnvManager = (ComponentEnvManager) h.getByContract(ComponentEnvManager.class);

            EjbContainerServices containerServices = h.getByContract(EjbContainerServices.class);

            JndiNameEnvironment componentEnv = compEnvManager.getCurrentJndiNameEnvironment();

            ManagedBeanDescriptor mbDesc = null;

            JndiNameEnvironment injectionEnv = (JndiNameEnvironment) bundleContext;
            
            Object target = injectionContext.getTarget();
            String targetClass = target.getClass().getName();

            if( componentEnv == null ) {
                //throw new IllegalStateException("No valid EE environment for injection of " + targetClass);
                System.err.println("No valid EE environment for injection of " + targetClass);
                injectionContext.proceed();
                return; 
            }

            // Perform EE-style injection on the target.  Skip PostConstruct since
            // in this case 299 impl is responsible for calling it.

            if( componentEnv instanceof EjbDescriptor ) {

                EjbDescriptor ejbDesc = (EjbDescriptor) componentEnv;

                if( containerServices.isEjbManagedObject(ejbDesc, target.getClass())) {
                    injectionEnv = componentEnv;
                } else {

                    if( bundleContext instanceof EjbBundleDescriptor ) {

                        // Check if it's a @ManagedBean class within an ejb-jar.  In that case,
                        // special handling is needed to locate the EE env dependencies
                        mbDesc = bundleContext.getManagedBeanByBeanClass(targetClass);
                    }                    
                }
            }

            if( mbDesc != null ) {
                injectionManager.injectInstance(target, mbDesc.getGlobalJndiName(), false);
            } else {
                if( injectionEnv instanceof EjbBundleDescriptor ) {

                    // CDI-style managed bean that doesn't have @ManagedBean annotation but
                    // is injected within the context of an ejb.  Need to explicitly
                    // set the environment of the ejb bundle.
                    injectionManager.injectInstance(target, compEnvManager.getComponentEnvId(injectionEnv)
                        ,false);

                } else {
                    injectionManager.injectInstance(target, injectionEnv, false);
                }
            }

            injectionContext.proceed();

        } catch(InjectionException ie) {
            throw new IllegalStateException(ie.getMessage(), ie);
        }

        
    }

    public void cleanup() {

    }
  
}