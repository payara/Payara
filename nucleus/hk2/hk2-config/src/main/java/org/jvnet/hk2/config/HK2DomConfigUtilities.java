/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.jvnet.hk2.config;

import javax.inject.Singleton;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.IndexedFilter;
import org.glassfish.hk2.api.InstanceLifecycleListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.DescriptorImpl;

/**
 * Utilities for working with HK2 config
 * 
 * @author jwells
 *
 */
public class HK2DomConfigUtilities {
    /**
     * This method enables HK2 Dom based XML configuration parsing for
     * systems that do not use HK2 metadata files or use a non-default
     * name for HK2 metadata files.  This method is idempotent, so that
     * if the services already are available in the locator they will
     * not get added again
     * 
     * @param locator The non-null locator to add the hk2 dom based
     * configuration services to
     */
    public static void enableHK2DomConfiguration(ServiceLocator locator, HK2Loader loader) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        boolean dirty = false;
        boolean operationDirty;
        
        operationDirty = addIfNotThere(locator, config, getConfigSupport(), loader);
        dirty = dirty || operationDirty;
        
        operationDirty = addIfNotThere(locator, config, getConfigurationPopulator(), loader);
        dirty = dirty || operationDirty;
        
        operationDirty = addIfNotThere(locator, config, getTransactions(), loader);
        dirty = dirty || operationDirty;
        
        operationDirty = addIfNotThere(locator, config, getConfigInstanceListener(), loader);
        dirty = dirty || operationDirty;
        
        if (dirty) {
            config.commit();
        }
        
    }
    
    
    /**
     * This method enables HK2 Dom based XML configuration parsing for
     * systems that do not use HK2 metadata files or use a non-default
     * name for HK2 metadata files.  This method is idempotent, so that
     * if the services already are available in the locator they will
     * not get added again
     * 
     * @param locator The non-null locator to add the hk2 dom based
     * configuration services to
     */
    public static void enableHK2DomConfiguration(ServiceLocator locator) {
        enableHK2DomConfiguration(locator, null);
    }
    
    private final static String CONFIG_SUPPORT_IMPL = "org.jvnet.hk2.config.ConfigSupport";
    private final static String CONFIGURATION_UTILITIES = "org.jvnet.hk2.config.api.ConfigurationUtilities";
    private static DescriptorImpl getConfigSupport() {
        return BuilderHelper.link(CONFIG_SUPPORT_IMPL). 
            to(CONFIGURATION_UTILITIES).
            in(Singleton.class.getName()).build();
    }
    
    private final static String CONFIGURATION_POPULATOR_IMPL = "org.jvnet.hk2.config.ConfigurationPopulator";
    private final static String CONFIG_POPULATOR = "org.glassfish.hk2.bootstrap.ConfigPopulator";
    private static DescriptorImpl getConfigurationPopulator() {
        return BuilderHelper.link(CONFIGURATION_POPULATOR_IMPL). 
            to(CONFIG_POPULATOR).
            in(Singleton.class.getName()).build();
    }
    
    private final static String TRANSACTIONS_IMPL = "org.jvnet.hk2.config.Transactions";
    private static DescriptorImpl getTransactions() {
        return BuilderHelper.link(TRANSACTIONS_IMPL).
            in(Singleton.class.getName()).build();
    }
    
    private final static String CONFIG_INSTANCE_LISTENER_IMPL = "org.jvnet.hk2.config.provider.internal.ConfigInstanceListener";
    private static DescriptorImpl getConfigInstanceListener() {
        return BuilderHelper.link(CONFIG_INSTANCE_LISTENER_IMPL). 
            to(InstanceLifecycleListener.class.getName()).
            in(Singleton.class.getName()).build();
    }
    
    private static boolean addIfNotThere(ServiceLocator locator, DynamicConfiguration config, DescriptorImpl desc, HK2Loader loader) {
        IndexedFilter filter = BuilderHelper.createContractFilter(desc.getImplementation());
        if (locator.getBestDescriptor(filter) != null) return false;
        
        if (loader != null) {
            desc.setLoader(loader);
        }
        config.bind(desc);
        return true;
    }

}
