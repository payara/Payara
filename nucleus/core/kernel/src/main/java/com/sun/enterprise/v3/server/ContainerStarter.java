/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import org.glassfish.server.ServerEnvironmentImpl;
import com.sun.enterprise.module.*;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.data.ContainerRegistry;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.container.Container;
import org.glassfish.api.container.Sniffer;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for starting containers, it will look for the container
 * installation location, will eventually download the container and install it locally.
 *
 * @author Jerome Dochez
 */
@Service
public class ContainerStarter {
    @Inject
    ModulesRegistry modulesRegistry;

    @Inject
    Habitat habitat;

    @Inject
    Logger logger;

    @Inject
    ServerEnvironmentImpl env;

    public Collection<EngineInfo> startContainer(Sniffer sniffer, Module snifferModule) {

        assert sniffer!=null;
        String containerName = sniffer.getModuleType();
        assert containerName!=null;
        
        // get the container installation
        String containerHome = StringUtils.getProperty(containerName + ".home");
        if (containerHome==null) {
            // the container could be installed at the default location
            // which is in <Root Installation>/modules/containerName
            String root = System.getProperty("com.sun.aas.installRoot");
            if(root!=null) {
                File location = new File(root);
                location = new File(location, "modules");
                location = new File(location, containerName);
                containerHome = location.getAbsolutePath();
                System.setProperty(containerName + ".home", containerHome);
            }
        }

        Module[] modules;
        ClassLoader containerClassLoader;
        // I do the container setup first so the code has a chance to set up
        // repositories which would allow access to the connector module.
        try {

            modules = sniffer.setup(containerHome, logger);
            if (modules!=null && modules.length>0) {
                containerClassLoader = setContainerClassLoader(modules);
            } else {
                containerClassLoader = snifferModule.getClassLoader();
            }
        } catch(FileNotFoundException fnf) {
            logger.log(Level.SEVERE, fnf.getMessage());
            return null;
        } catch(IOException ioe) {
            logger.log(Level.SEVERE, ioe.getMessage(), ioe);
            return null;

        }

        // first the right container from that module.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<EngineInfo> containers = new ArrayList<EngineInfo>();
        for (String name : sniffer.getContainersNames()) {

            try {
                Inhabitant<? extends Container> provider = habitat.getInhabitant(Container.class, name);
                if (provider==null) {
                    try {
                        Class<? extends Container> containerClass = containerClassLoader.loadClass(name).asSubclass(Container.class);
                        if (containerClass!=null) {
                            provider = habitat.getInhabitantByType(containerClass);
                            if (provider==null) {
                                logger.severe("Cannot find the container " + name + " in the services, is it annotated with @Service");
                                return null;
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.SEVERE, "Exception while loading container class ", e);
                    }

                    if (provider==null) {
                        logger.severe("Cannot find Container named " + name);
                        logger.severe("Cannot start " + sniffer.getModuleType() + " container");
                        return null;
                    }
                }
                Thread.currentThread().setContextClassLoader(containerClassLoader);
                EngineInfo info = new EngineInfo(provider, sniffer, containerClassLoader);

                ContainerRegistry registry = habitat.getComponent(ContainerRegistry.class);
                registry.addContainer(name, info);
                containers.add(info);
            } catch (ComponentException e) {
                logger.log(Level.SEVERE, "Cannot create or inject Container", e);
                return null;
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }

        }                                       
        return containers;
    }

    /**
     * Sets the class loader associated with a container, this class loader will be used to
     * load any classes and resources the container provides. This will also be used to set the context
     * class loader.
     *
     * @param modules array of modules that are forming the container
     * @return a class loader capable of loading classes from these modules
     */
    ClassLoader setContainerClassLoader(Module[] modules) {
        if (modules==null) {
            throw new IllegalArgumentException("passed null module list to setContainerClassLoader");
        }
        if (modules.length==1) {
            // this is quite simple, I am going to return that module's class loader
            return modules[0].getClassLoader();
        } else {
            List<ModuleDefinition> defs = new ArrayList<ModuleDefinition>(modules.length);
            for (Module module : modules) {
                defs.add(module.getModuleDefinition());
            }
            return modulesRegistry.getModulesClassLoader(modules[0].getClassLoader(), defs);
        }
    }


    public void explodeJar(File source, File destination) throws IOException {

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(source);
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String fileSystemName = entry.getName().replace('/', File.separatorChar);
                File out = new File(destination, fileSystemName);

                if (entry.isDirectory()) {
                    if (!out.mkdirs()) {
                       logger.log(Level.INFO, "Cannot create directory " + out.getAbsolutePath());
                    }
                } else {
                    InputStream is = null;
                    FileOutputStream fos = null;
                    try {
                        if (!out.getParentFile().exists()) {
                            if (!out.getParentFile().mkdirs()) {
                                logger.log(Level.SEVERE, "Cannot create directory " + out.getParentFile());
                            }
                        }
                        is = new BufferedInputStream(jarFile.getInputStream(entry));
                        fos = new FileOutputStream(out);
                        ReadableByteChannel inChannel = Channels.newChannel(is);
                        FileChannel outChannel = fos.getChannel();
                        outChannel.transferFrom(inChannel, 0, entry.getSize());
                    } finally {
                        try {
                            if (is!=null)
                                is.close();
                        } catch(IOException ex) {
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException ex2) {
                               // do nothing
                            }
                            // throw original exception
                            throw ex;
                        }
                        if (fos!=null)
                            fos.close();
                    }
                }
            }
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }
}
