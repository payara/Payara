/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.config.cdi;

import fish.payara.nucleus.microprofile.config.spi.PayaraConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * CDI extension that implements the Microprofile Config API ConfigProperty injection
 * @author Steve Millidge <Payara Services Limited>
 */
public class CDIExtension implements Extension {
    
    public void validateInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
        
        // we need to validate the injection point for the ConfigProperty to meet the 3 TCK tests
        // (1) Deployment should fail if there is no converter for a class
        // (2) Deployment should fail if there is no value for an injected primitive
        // (3) Deployment should fail if a primitive conversion will fail from the property value
        
        // ok check this is a config property injection point
        ConfigProperty property = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (property != null) {
            // see if we can resolve the injection point in the future
            try {
                if (Class.class.isInstance(pip.getInjectionPoint().getType())) {
                    ConfigPropertyProducer.getGenericProperty(pip.getInjectionPoint());
                }
            }catch (Throwable de ) {
                Class failingClass = null;
                Bean bean = pip.getInjectionPoint().getBean();
                if (bean == null) {
                    failingClass = pip.getInjectionPoint().getMember().getDeclaringClass();
                } else {
                    failingClass = pip.getInjectionPoint().getBean().getBeanClass();
                }
                pip.addDefinitionError(new DeploymentException("Deploment Failure for ConfigProperty " + property.name() + " in class " + failingClass.getCanonicalName() + " Reason " + de.getMessage(),de));
            }
        }
    }

    /**
     * Register the ConfigProducer bean that has producer methods for Config and Optional
     * @param event
     * @param bm 
     */
    public void createConfigProducer(@Observes BeforeBeanDiscovery event, BeanManager bm) {
        AnnotatedType<ConfigProducer> at = bm.createAnnotatedType(ConfigProducer.class);
        event.addAnnotatedType(at, ConfigProducer.class.getName());
    }

    /**
     * CDI observer creates a synthetic bean with Producer method
     * for each Converter type registered in the Config object associated with 
     * this application.
     * 
     * @param event
     * @param bm 
     */
    public void addDynamicProducers(@Observes AfterBeanDiscovery event, BeanManager bm) {

        // Retrieve the config for the application
        Config config = ConfigProvider.getConfig();
        if (config instanceof PayaraConfig) {
            
            // create a synthetic bean based on the ConfigPropertyProducer which 
            // has a method we can use to create the correct objects based on 
            // the InjectionPoint
            AnnotatedType<ConfigPropertyProducer> atype = bm.createAnnotatedType(ConfigPropertyProducer.class);
            BeanAttributes<?> beanAttr = null;
            
            // first find the producer method
            AnnotatedMethod<? super ConfigPropertyProducer> method = null;
            for (AnnotatedMethod m : atype.getMethods()) {
                if (m.getJavaMember().getName().equals("getGenericProperty")) {
                    // create a bean attributes based on this method
                    beanAttr = bm.createBeanAttributes(m);
                    method = m;
                    break;
                }
            }
            
            if (beanAttr != null) {
                HashSet<Type> types = new HashSet<>();
                types.addAll(((PayaraConfig) config).getConverterTypes());
                // add string explictly
                types.add(String.class);
                
                for (final Type converterType : types) {
                    // go through each type which has a converter
                    // create a bean with a Producer method using the bean factory and with custom bean attributes
                    Bean<?> bean = bm.createBean(new TypesBeanAttributes<Object>(beanAttr) {
                        
                        // overrides the bean types to return the type registered for a Converter
                        @Override
                        public Set<Type> getTypes() {
                            HashSet<Type> result = new HashSet<>();
                            result.add(converterType);
                            return result;
                        }
                    }, ConfigPropertyProducer.class, bm.getProducerFactory(method, null));
                    event.addBean(bean);
                }
            }
        }
    }
}
