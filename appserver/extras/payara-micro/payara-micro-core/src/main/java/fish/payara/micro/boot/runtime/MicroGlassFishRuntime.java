/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.boot.runtime;

import com.sun.enterprise.glassfish.bootstrap.EmbeddedInhabitantsParser;
import com.sun.enterprise.glassfish.bootstrap.SingleHK2Factory;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.single.SingleModulesRegistry;
import java.util.Arrays;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.DuplicatePostProcessor;

/**
 *
 * @author Steve Millidge
 */
public class MicroGlassFishRuntime extends GlassFishRuntime {
    
    ModulesRegistry registry;
    ServiceLocator habitat;
    MicroGlassFish gf;

    MicroGlassFishRuntime() {
     }

    @Override
    public void shutdown() throws GlassFishException {
        gf.dispose();
        shutdownInternal();
    }

    @Override
    public GlassFish newGlassFish(GlassFishProperties glassfishProperties) throws GlassFishException {
        System.setProperty("com.sun.aas.installRoot",System.getProperty("com.sun.aas.instanceRoot"));
        System.setProperty("com.sun.aas.installRootURI",System.getProperty("com.sun.aas.instanceRootURI"));
        
        glassfishProperties.setProperty("com.sun.aas.installRoot", System.getProperty("com.sun.aas.instanceRoot"));
        glassfishProperties.setProperty("com.sun.aas.installRootURI", System.getProperty("com.sun.aas.instanceRootURI"));
        
        StartupContext context = new StartupContext(glassfishProperties.getProperties());
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        SingleHK2Factory.initialize(tccl);
        registry = new SingleModulesRegistry(tccl);
        registry.setParentClassLoader(tccl);  
        habitat = registry.newServiceLocator();
        DynamicConfigurationService dcs = habitat.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(context));
        config.commit();
        registry.populateServiceLocator("default", habitat, Arrays.asList(new PayaraMicroInhabitantsParser(), new EmbeddedInhabitantsParser(), new DuplicatePostProcessor()));
        registry.populateConfig(habitat);
        ModuleStartup kernel = habitat.getService(ModuleStartup.class);
        gf = new MicroGlassFish(kernel, habitat, glassfishProperties.getProperties());
        return gf;
    }
    
}
