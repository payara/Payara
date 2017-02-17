/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.micro.services.data;

import fish.payara.micro.data.ModuleDescriptor;
import fish.payara.micro.data.ApplicationDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ModuleInfo;

/**
 *
 * @author steve
 */
public class ApplicationDescriptorImpl implements ApplicationDescriptor {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final String libraries;
    private final boolean isJavaEE;
    private final List<ModuleDescriptor> modules;

    public ApplicationDescriptorImpl(ApplicationInfo info) {
        
        name = info.getName();
        libraries = info.getLibraries();
        isJavaEE = info.isJavaEEApp();
        Collection<ModuleInfo> moduleInfos = info.getModuleInfos();
        modules = new ArrayList<>(moduleInfos.size());
        for (ModuleInfo moduleInfo : moduleInfos) {
            modules.add(new ModuleDescriptorImpl(moduleInfo));
        }
    }

    @Override
    public String toString() {
        return "ApplicationDescriptor{" + "name=" + name + ", libraries=" + libraries + ", isJavaEE=" + isJavaEE + ", modules=" + modules + '}';
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return the libraries
     */
    @Override
    public String getLibraries() {
        return libraries;
    }

    /**
     * @return the isJavaEE
     */
    @Override
    public boolean isJavaEE() {
        return isJavaEE;
    }
    
    /**
     * Return the module descriptors
     * @return 
     */
    @Override
    public List<ModuleDescriptor> getModuleDescriptors() {
        return modules;
    }

    /**
     * @return the module names
     */
    @Override
    public List<String> getModules() {
        List<String> moduleNames = new ArrayList<>(modules.size());
        for (ModuleDescriptor descriptor : modules) {
            moduleNames.add(descriptor.getName());
        }
        return moduleNames;
    }
  
    
}
