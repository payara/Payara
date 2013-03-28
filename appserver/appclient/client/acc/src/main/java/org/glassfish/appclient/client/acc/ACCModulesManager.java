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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.ContextDuplicatePostProcessor;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.enterprise.naming.impl.ClientNamingConfiguratorImpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.naming.ClientNamingConfigurator;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.bootstrap.HK2Populator;
import org.glassfish.hk2.bootstrap.PopulatorPostProcessor;
import org.glassfish.hk2.bootstrap.impl.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.Globals;

/**
 * Encapsulates details of preparing the HK2 habitat while also providing
 * a "main" HK2 module the HK2 bootstrap logic can start.
 * <p>
 * The HK2 habitat must be initialized before any AppClientContainer can be
 * created, because each ACC is an HK2 service so it can use injection.  The
 * AppClientContainerBuilder uses the habitat directly (without injection)
 * to create new ACCs. Part of initializing the habitat involves (at least
 * currently) finding and starting a "main HK2 module."  This class serves
 * that purpose, even though this class is not the main program.  To support
 * embedded ACCs we do not assume we provide the actual main program, but we
 * seem to need to offer a main module to HK2.  So this class implements
 * ModuleStartup even though it does little.
 *
 * @author tjquinn
 */
public class ACCModulesManager /*implements ModuleStartup*/ {

    private static ServiceLocator habitat = null;

    public synchronized static void initialize(final ClassLoader loader) throws URISyntaxException {
        /*
         * The habitat might have been initialized earlier.  Currently
         * we use a single habitat for the JVM.  
         */
        if (habitat == null) {
            habitat = prepareHabitat(
                    loader);

            /*
             * Set up the default habitat in Globals as soon as we know
             * which habitat we'll use.
             */
            Globals.setDefaultHabitat(habitat);
            
            ServiceLocator locator = habitat;
            
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration config = dcs.createDynamicConfiguration();
            
            /*
             * Remove any already-loaded startup context so we can replace it
             * with the ACC one.
             */
            config.addUnbindFilter(BuilderHelper.createContractFilter(StartupContext.class.getName()));
            
            /*
             * Following the example from AppServerStartup, remove any
             * pre-loaded lazy inhabitant for ProcessEnvironment that exists
             * from HK2's scan for services.  Then add in
             * an ACC ProcessEnvironment.
             */
            config.addUnbindFilter(BuilderHelper.createContractFilter(ProcessEnvironment.class.getName()));
            
            config.commit();
            
            config = dcs.createDynamicConfiguration();
            
            StartupContext startupContext = new ACCStartupContext();
            AbstractActiveDescriptor<?> startupContextDescriptor = BuilderHelper.createConstantDescriptor(startupContext);
            startupContextDescriptor.addContractType(StartupContext.class);
            config.addActiveDescriptor(startupContextDescriptor);
            
            ModulesRegistry modulesRegistry = new StaticModulesRegistry(ACCModulesManager.class.getClassLoader());
            config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(modulesRegistry));
            
            config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(
                    new ProcessEnvironment(ProcessEnvironment.ProcessType.ACC)));

            /*
             * Create the ClientNamingConfigurator used by naming.
             */
            ClientNamingConfigurator cnc = new ClientNamingConfiguratorImpl();
            config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(cnc));
            
            Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            AbstractActiveDescriptor<Logger> di = BuilderHelper.createConstantDescriptor(logger);
            di.addContractType(Logger.class);
            config.addActiveDescriptor(di);
            
            config.commit();
       }
    }


    static ServiceLocator getHabitat() {
        return habitat;
    }

    public static <T> T getService(Class<T> c) {
        return habitat.getService(c);
    }

    /**
     * Sets up the HK2 habitat.
     * <p>
     * Must be invoked at least once before an AppClientContainerBuilder
     * returns a new AppClientContainer to the caller.
     * @param classLoader
     * @param logger
     * @throws com.sun.enterprise.module.bootstrap.BootException
     * @throws java.net.URISyntaxException
     */
    private static ServiceLocator prepareHabitat(
            final ClassLoader loader) {
        ServiceLocator serviceLocator = ServiceLocatorFactory.getInstance().create("default");

        habitat = serviceLocator;
        
        ContextDuplicatePostProcessor duplicateProcessor = new ContextDuplicatePostProcessor();
        List<PopulatorPostProcessor> postProcessors = new LinkedList<PopulatorPostProcessor>();
        postProcessors.add(duplicateProcessor);
        
        try {
        	HK2Populator.populate(serviceLocator, new ClasspathDescriptorFileFinder(loader), postProcessors);
        } catch (IOException e) {
        	e.printStackTrace();
        }
  
        return habitat;
    }
}
