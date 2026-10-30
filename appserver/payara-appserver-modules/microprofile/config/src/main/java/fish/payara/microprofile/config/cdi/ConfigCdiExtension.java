/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Provider;

/**
 * CDI extension that implements the Microprofile Config API ConfigProperty
 * injection
 * 
 * @author Steve Millidge <Payara Services Limited>
 */
public class ConfigCdiExtension implements Extension {

    private Set<Type> configPropertiesBeanTypes = new HashSet<>();

    public void validateInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {

        // we need to validate the injection point for the ConfigProperty to meet the 3
        // TCK tests
        // (1) Deployment should fail if there is no converter for a class
        // (2) Deployment should fail if there is no value for an injected primitive
        // (3) Deployment should fail if a primitive conversion will fail from the
        // property value

        // ok check this is a config property injection point
        ConfigProperty property = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (property != null) {
            // see if we can resolve the injection point in the future
            try {
                Type t = pip.getInjectionPoint().getType();
                if (Class.class.isInstance(pip.getInjectionPoint().getType())) {
                    ConfigPropertyProducer.getGenericProperty(pip.getInjectionPoint());
                } else if (t instanceof ParameterizedType) {
                    Class rawClazz = (Class) ((ParameterizedType) t).getRawType();
                    if (rawClazz != Provider.class && rawClazz != Optional.class) {
                        ConfigPropertyProducer.getGenericProperty(pip.getInjectionPoint());
                    }
                }
            } catch (Throwable de) {
                Class failingClass = null;
                Bean bean = pip.getInjectionPoint().getBean();
                if (bean == null) {
                    failingClass = pip.getInjectionPoint().getMember().getDeclaringClass();
                } else {
                    failingClass = pip.getInjectionPoint().getBean().getBeanClass();
                }
                pip.addDefinitionError(
                        new DeploymentException("Deployment Failure for ConfigProperty " + property.name()
                                + " in class " + failingClass.getCanonicalName() + " Reason " + de.getMessage(), de));
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

    public <T> void storeConfigPropertiesType(@Observes @WithAnnotations(ConfigProperties.class) ProcessAnnotatedType<T> event) {
        
        final AnnotatedType<?> type = event.getAnnotatedType();

        if (type.getJavaClass().isAnnotationPresent(ConfigProperties.class)) {
            event.veto();
            return;
        }
        for (AnnotatedField<?> field : type.getFields()) {
            final Class<?> memberClass = field.getJavaMember().getType();
            if (memberClass.isAnnotationPresent(ConfigProperties.class)) {
                configPropertiesBeanTypes.add(memberClass);
            }
        }
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

            final AnnotatedType<ConfigPropertyProducer> propertyProducerType = bm.createAnnotatedType(ConfigPropertyProducer.class);
            final AnnotatedType<ConfigPropertiesProducer> objectProducerType = bm.createAnnotatedType(ConfigPropertiesProducer.class);

            // create a synthetic bean based on the ConfigPropertyProducer which 
            // has a method we can use to create the correct objects based on 
            // the InjectionPoint
            BeanAttributes<?> propertyBeanAttributes = null;
            BeanAttributes<?> objectBeanAttributes = null;
            
            // first find the producer method
            AnnotatedMethod<? super ConfigPropertyProducer> propertyProducerMethod = null;
            AnnotatedMethod<? super ConfigPropertiesProducer> objectProducerMethod = null;

            for (AnnotatedMethod m : propertyProducerType.getMethods()) {
                final String methodName = m.getJavaMember().getName();
                if (methodName.equals("getGenericProperty")) {
                    // create a bean attributes based on this method
                    propertyBeanAttributes = bm.createBeanAttributes(m);
                    propertyProducerMethod = m;
                    break;
                }
            }
            for (AnnotatedMethod m : objectProducerType.getMethods()) {
                final String methodName = m.getJavaMember().getName();
                if (methodName.equals("getGenericObject")) {
                    objectBeanAttributes = bm.createBeanAttributes(m);
                    objectProducerMethod = m;
                    break;
                }
            }

            if (objectProducerMethod != null & !configPropertiesBeanTypes.isEmpty()) {
                Bean<?> bean = bm.createBean(new TypesBeanAttributes<Object>(objectBeanAttributes){
                    @Override
                    public Set<Type> getTypes() {
                        return configPropertiesBeanTypes;
                    }
                }, ConfigPropertiesProducer.class,
                        bm.getProducerFactory(objectProducerMethod, null));
                event.addBean(bean);
            }
            
            // if we have the producer method
            if (propertyProducerMethod != null) {
                HashSet<Type> types = new HashSet<>();
                types.add(ConfigValue.class);
                types.addAll(((PayaraConfig) config).getConverterTypes());
                // add String explictly
                types.add(String.class);
                
                // go through each type which has a converter either custom or built in
                // create a bean with a Producer method using the bean factory and with custom bean attributes
                // also override the set of types depending on the type of the converter
                for (final Type converterType : types) {
                    Bean<?> bean = bm.createBean(new TypesBeanAttributes<Object>(propertyBeanAttributes) {
                        
                        // overrides the bean types to return the types registered for a Converter
                        @Override
                        public Set<Type> getTypes() {
                            HashSet<Type> result = new HashSet<>();
                            // add the type that the converter converts
                            result.add(converterType);
                            
                            // also indicate support array of the type.
                            if (converterType instanceof Class) {
                                Object array = Array.newInstance((Class)converterType, 0);
                                result.add(array.getClass());
                            }
                            
                            // ok we will have to do specific code for wrappers
                            // for each wrapper indicate we can also convert the primitive
                            // and we can convert the primitive array
                            if (converterType == Long.class) {
                                result.add(long.class);
                                result.add((new long[0]).getClass());
                            } else if (converterType == Boolean.class) {
                                result.add(boolean.class);
                                result.add((new boolean[0]).getClass());
                            } else if (converterType == Integer.class) {
                                result.add(int.class);
                                result.add((new int[0]).getClass());
                            } else if (converterType == Float.class) {
                                result.add(float.class);
                                result.add((new float[0]).getClass());
                            } else if (converterType == Double.class) {
                                result.add(double.class);
                                result.add((new double[0]).getClass());
                            }
                            
                            return result;
                        }
                    }, ConfigPropertyProducer.class, bm.getProducerFactory(propertyProducerMethod, null));
                    event.addBean(bean);
                }
            }
        }
    }
}
