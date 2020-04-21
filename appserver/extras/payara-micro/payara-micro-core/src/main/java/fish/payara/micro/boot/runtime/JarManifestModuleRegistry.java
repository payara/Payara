/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package fish.payara.micro.boot.runtime;

import com.sun.enterprise.module.HK2Module;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ResolveError;
import com.sun.enterprise.module.impl.ModulesRegistryImpl;
import com.sun.enterprise.module.single.ManifestProxy;
import com.sun.enterprise.module.single.ProxyModule;
import com.sun.enterprise.module.single.ProxyModuleDefinition;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Populator;
import org.glassfish.hk2.api.PopulatorPostProcessor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.bootstrap.impl.Hk2LoaderPopulatorPostProcessor;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Normal modules registry with configuration handling backed up
 * by a single class loader. There is one virtual module available in the modules
 * registry and that module's class loader is the single class loader used to
 * load all artifacts.
 *
 * This is adaptation of HK2's SingleModulesRegistry for cases where modules are defined by Manifest's Class-Path attribute.
 *
 * @author Jerome Dochez
 * @author Patrik Dudits
 */
class JarManifestModuleRegistry extends ModulesRegistryImpl {

    final ClassLoader singleClassLoader;
    final ProxyModule module;
    final Collection<HK2Module> singleton;

    public JarManifestModuleRegistry(ClassLoader singleCL, URI[] locations) {
        this(singleCL, null, locations);
    }


    public JarManifestModuleRegistry(ClassLoader singleCL, List<ManifestProxy.SeparatorMappings> mappings, URI[] locations) {

        super(null);
        this.singleClassLoader = singleCL;
        setParentClassLoader(singleClassLoader);

        ModuleDefinition moduleDef = null;
        try {
            moduleDef = new ProxyModuleDefinition(singleClassLoader, mappings) {
                @Override
                public URI[] getLocations() {
                    return locations;
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        module = new ProxyModule(this, moduleDef, singleClassLoader);
        singleton = Collections.singleton(module);
    }

    void initialize() {
        add(module.getModuleDefinition());
    }

    @Override
    public HK2Module find(Class clazz) {
        HK2Module m = super.find(clazz);
        if (m == null)
            return module;
        return m;
    }

    @Override
    public Collection<HK2Module> getModules(String moduleName) {
        // I could not care less about the modules names
        return getModules();
    }

    @Override
    public Collection<HK2Module> getModules() {
        return singleton;
    }

    @Override
    public HK2Module makeModuleFor(String name, String version, boolean resolve) throws ResolveError {
        return module;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected List<ActiveDescriptor> parseInhabitants(HK2Module module, String name, ServiceLocator serviceLocator, List<PopulatorPostProcessor> postProcessors)
            throws IOException {

        ArrayList<PopulatorPostProcessor> allPostProcessors = new ArrayList<PopulatorPostProcessor>();

        allPostProcessors.add(new Hk2LoaderPopulatorPostProcessor(singleClassLoader));
        if (postProcessors != null) {
          allPostProcessors.addAll(postProcessors);
        }

        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);
        Populator populator = dcs.getPopulator();
        
    	List<ActiveDescriptor<?>> retVal = populator.populate(
                new ClasspathDescriptorFileFinder(singleClassLoader, name),
                allPostProcessors.toArray(new PopulatorPostProcessor[allPostProcessors.size()]));
    	
    	return (List<ActiveDescriptor>) ((List) retVal);
    }

}
