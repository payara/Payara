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

import fish.payara.microprofile.config.spi.PayaraConfig;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProducerFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 * @author Steve Millidge <Payara Services Limited>
 */
public class CDIExtension implements Extension {

    public void createConfigProducer(@Observes BeforeBeanDiscovery event, BeanManager bm) {
        AnnotatedType<ConfigProducer> at = bm.createAnnotatedType(ConfigProducer.class);
        event.addAnnotatedType(at);
    }

    public void addDynamicProducers(@Observes AfterBeanDiscovery event, BeanManager bm) {

        Config config = ConfigProvider.getConfig();
        if (config instanceof PayaraConfig) {
            AnnotatedType<ConfigValueProducer> atype = bm.createAnnotatedType(ConfigValueProducer.class);
            BeanAttributes<?> beanAttr = null;
            AnnotatedMethod<? super ConfigValueProducer> method = null;
            for (AnnotatedMethod m : atype.getMethods()) {
                if (m.getJavaMember().getName().equals("getGenericProperty")) {
                    beanAttr = bm.createBeanAttributes(m);
                    method = m;
                    break;
                }
            }
            if (beanAttr != null) {
                HashSet<Type> types = new HashSet<>();
                types.addAll(((PayaraConfig) config).getConverterTypes());
                // add string
                types.add(String.class);
                for (final Type converterType : types) {
                    // create a bean using the neam factory and with custom bean attributes
                    Bean<?> bean = bm.createBean(new TypesBeanAttributes<Object>(beanAttr) {
                        @Override
                        public Set<Type> getTypes() {
                            HashSet<Type> result = new HashSet<>();
                            result.add(converterType);
                            return result;
                        }
                    }, ConfigValueProducer.class, bm.getProducerFactory(method, null));
                    event.addBean(bean);
                }
            }
        }
    }
}
