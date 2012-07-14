/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.api;

import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.single.SingleModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.common.util.admin.GlassFishErrorServiceImpl;
import org.glassfish.common.util.admin.HK2BindTracingService;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.bootstrap.HK2Populator;
import org.glassfish.hk2.bootstrap.PopulatorPostProcessor;
import org.glassfish.hk2.bootstrap.impl.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.bootstrap.impl.Hk2LoaderPopulatorPostProcessor;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.DescriptorImpl;
import org.glassfish.internal.api.Init;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;

import javax.inject.Inject;

/**
 * Very sensitive class, anything stored here cannot be garbage collected
 *
 * @author Jerome Dochez
 */
@Service(name = "globals")
public class Globals implements Init {

    private static volatile Habitat defaultHabitat;

    private static Object staticLock = new Object();
    
    // dochez : remove this once we can get rid of ConfigBeanUtilities class
    @Inject
    private ConfigBeansUtilities utilities;
    
    @Inject
    private Globals(Habitat habitat) {
        if (defaultHabitat == null) {
            defaultHabitat = habitat;
        }
    }

    public static BaseServiceLocator getDefaultBaseServiceLocator() {
    	return getDefaultHabitat();
    }
    
    public static Habitat getDefaultHabitat() {
        return defaultHabitat;
    }

    public static <T> T get(Class<T> type) {
        return defaultHabitat.getComponent(type);
    }

    public static void setDefaultHabitat(final Habitat habitat) {
        defaultHabitat = habitat;
    }

    public static BaseServiceLocator getStaticBaseServiceLocator() {
    	return getStaticHabitat();
    }
    
    public static Habitat getStaticHabitat() {
        if (defaultHabitat == null) {
            synchronized (staticLock) {
                if (defaultHabitat == null) {
                    ServiceLocator locator = ServiceLocatorFactory.getInstance().create("default");
                    
                    Habitat previouslyCreated = locator.getService(Habitat.class);
                    if (previouslyCreated != null) {
                        defaultHabitat = previouslyCreated;
                        return defaultHabitat;
                    }
                    
                    defaultHabitat = new Habitat();
                    initializeClient(locator);
                }
            }
        }

        return defaultHabitat;
    }

	public static void setDefaultHabitat(BaseServiceLocator habitat) {
		setDefaultHabitat((Habitat)habitat);
	}
	
	private static void initializeClient(ServiceLocator locator) {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    if (cl == null) {
	        cl = Globals.class.getClassLoader();
	    }
	    
	    DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
	    DynamicConfiguration config = dcs.createDynamicConfiguration();
	    
	    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	    AbstractActiveDescriptor<Logger> di = BuilderHelper.createConstantDescriptor(logger);
	    di.addContractType(Logger.class);
	    
	    config.addActiveDescriptor(di);
	    
	    config.addActiveDescriptor(HK2BindTracingService.class);
	    config.addActiveDescriptor(GlassFishErrorServiceImpl.class);
	    
	    SingleModulesRegistry smr = new SingleModulesRegistry(cl);
	    
	    config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(smr));
	    config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(new StartupContext()));
	    
	    config.commit();
	    
	    try {
            HK2Populator.populate(locator,
                    new ClasspathDescriptorFileFinder(cl),
                    new ClientPostProcessor());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing HK2", e);
        }
	}
	
	private static final String SERVER_CONTEXT_IMPL = "com.sun.enterprise.v3.server.ServerContextImpl";
    private static final HashSet<String> VERBOTEN_WORDS = new HashSet<String>();
    
    static {
        VERBOTEN_WORDS.add(SERVER_CONTEXT_IMPL);
    }
    
    private static class ClientPostProcessor implements PopulatorPostProcessor {

        /* (non-Javadoc)
         * @see org.glassfish.hk2.bootstrap.PopulatorPostProcessor#process(org.glassfish.hk2.utilities.DescriptorImpl)
         */
        @Override
        public DescriptorImpl process(DescriptorImpl descriptorImpl) {
            if (VERBOTEN_WORDS.contains(descriptorImpl.getImplementation())) {
                // This is a client
                return null;
            }
            
            return descriptorImpl;
        }
        
    }

}
