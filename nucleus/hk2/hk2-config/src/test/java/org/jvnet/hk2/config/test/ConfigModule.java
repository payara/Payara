/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config.test;

import javax.inject.Singleton;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AliasDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.DescriptorBuilder;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigInjector;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigurationPopulator;
import org.jvnet.hk2.config.DomDecorator;
import org.jvnet.hk2.config.InjectionTarget;
import org.jvnet.hk2.config.Populator;
import org.jvnet.hk2.config.Transactions;


import java.util.*;

/**
 * TODO:  This should be done via auto-depends (via Service and contract
 * and all that).  However, since those don't work yet with the new
 * API, we must code this up by hand.
 * 
 * @author jwells
 *
 */
public class ConfigModule {

    ServiceLocator serviceLocator;

    public ConfigModule(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    /**
     * Configures the HK2 instance
     * 
     * @param configurator
     */
    public void configure(DynamicConfiguration configurator) {

//        configurator.bind(
//                BuilderHelper.link(XmlPopulator.class).
//                        to(Populator.class).
//                        in(Singleton.class.getName()).
//                        build());

        configurator.bind(BuilderHelper.link(ConfigSupport.class)
                .in(Singleton.class.getName())
                .build());
        configurator.bind(BuilderHelper.link(Transactions.class)
                .in(Singleton.class.getName())
                .build());
        configurator.bind(BuilderHelper.link(SimpleConfigBeanDomDecorator.class)
                .to(DomDecorator.class).in(Singleton.class.getName())
                .build());
        configurator.bind(BuilderHelper.link(ConfigurationPopulator.class)
                .in(Singleton.class.getName())
                .build());
        configurator.bind(BuilderHelper.link(DummyPopulator.class)
                .to(Populator.class).in(Singleton.class.getName())
                .build());
        configurator.addActiveDescriptor(ConfigErrorService.class);
        bindInjector(configurator, "simple-connector", SimpleConnector.class, SimpleConnectorInjector.class);
        bindInjector(configurator, "ejb-container-availability", EjbContainerAvailability.class, EjbContainerAvailabilityInjector.class);
        bindInjector(configurator, "web-container-availability", WebContainerAvailability.class, WebContainerAvailabilityInjector.class);
        bindInjector(configurator, "generic-container",          GenericContainer.class,         GenericContainerInjector.class);
        bindInjector(configurator, "generic-config",          GenericConfig.class,         GenericConfigInjector.class);

    }
    
    private void bindInjector(DynamicConfiguration configurator, String elementName, Class contract, final Class clz) {
        DescriptorBuilder db = BuilderHelper.link(clz).
                to(ConfigInjector.class).to(InjectionTarget.class).to(contract).
                in(Singleton.class.getName()).
                qualifiedBy(clz.getAnnotation(InjectionTarget.class)).
                named(elementName).andLoadWith(new MyHk2Loader(clz.getClassLoader()));

        String metaData = ((Service) clz.getAnnotation(Service.class)).metadata();
        Map<String, List<String>> metaMap = new HashMap<String, List<String>>();
        for (StringTokenizer st = new StringTokenizer(metaData, ","); st.hasMoreTokens(); ) {
            String tok = st.nextToken();
            int index = tok.indexOf('=');
            if (index > 0) {
                String key = tok.substring(0, index);
                String value = tok.substring(index + 1);
                List<String> lst = metaMap.get(key);
                if (lst == null) {
                    lst = new LinkedList<String>();
                    metaMap.put(key, lst);
                }
                lst.add(value);
                //System.out.println("**     Added Metadata: " + tok.substring(0, index) + "  : " + tok.substring(index+1));
            }
            //db.andLoadWith(new MyHk2Loader(clz.getClassLoader()));
        }
        
        for (String key : metaMap.keySet()) {
            db.has(key, metaMap.get(key));
        }
        ActiveDescriptor desc = configurator.bind(db.build());
        configurator.bind(new AliasDescriptor(serviceLocator, desc, InjectionTarget.class.getName(), contract.getName()));
        System.out.println("**Successfully bound an alias descriptor for: " + elementName);
    }
    
    class MyHk2Loader
        implements HK2Loader {
        
        
        private ClassLoader loader;

        MyHk2Loader(ClassLoader cl) {
            loader = cl;
        }
        
        @Override
        public Class<?> loadClass(final String className) throws MultiException {
            try {
                return loader.loadClass(className);
            } catch (Exception ex) {
                throw new MultiException(ex);
            }
        }
    }

}
