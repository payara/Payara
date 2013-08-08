/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.glassfish.bootstrap;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.Main;
import com.sun.enterprise.module.bootstrap.Which;
import com.sun.enterprise.module.common_impl.AbstractFactory;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.spi.RuntimeBuilder;
import org.glassfish.hk2.bootstrap.impl.Hk2LoaderPopulatorPostProcessor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class StaticGlassFishRuntimeBuilder implements RuntimeBuilder {

    private static Logger logger = Util.getLogger();
    private static final String JAR_EXT = ".jar";
    final List<String> moduleExcludes = Arrays.asList("jsftemplating.jar", "gf-client-module.jar");

    public GlassFishRuntime build(BootstrapProperties bsProps) throws GlassFishException {
        /* Step 1. Build the classloader. */
        // The classloader should contain installRoot/modules/**/*.jar files.
        String installRoot = getInstallRoot(bsProps);
        if (installRoot != null) {
            System.setProperty("org.glassfish.embeddable.installRoot", installRoot);
        }
        // Required to add moduleJarURLs to support 'java -jar modules/glassfish.jar case'
        List<URL> moduleJarURLs = getModuleJarURLs(installRoot);
        ClassLoader cl = getClass().getClassLoader();
        if (!moduleJarURLs.isEmpty()) {
            cl = new StaticClassLoader(getClass().getClassLoader(), moduleJarURLs);
        }

        // Step 2. Setup the module subsystem.
        Main main = new EmbeddedMain(cl);
        SingleHK2Factory.initialize(cl);
        ModulesRegistry modulesRegistry = AbstractFactory.getInstance().createModulesRegistry();
        modulesRegistry.setParentClassLoader(cl);

        // Step 3. Create NonOSGIGlassFishRuntime
        GlassFishRuntime glassFishRuntime = new StaticGlassFishRuntime(main);
        logger.logp(Level.FINER, getClass().getName(), "build",
                "Created GlassFishRuntime {0} with InstallRoot {1}, Bootstrap Options {2}",
                new Object[]{glassFishRuntime, installRoot, bsProps});
        return glassFishRuntime;
    }

    public boolean handles(BootstrapProperties bsProps) {
        // See GLASSFISH-16743 for the reason behind additional check
        final String builderName = bsProps.getProperty(Constants.BUILDER_NAME_PROPERTY);
        if (builderName != null && !builderName.equals(getClass().getName())) {
            return false;
        }
        String platform = bsProps.getProperty(Constants.PLATFORM_PROPERTY_KEY);
        return platform == null || Constants.Platform.Static.toString().equalsIgnoreCase(platform);
    }

    private String getInstallRoot(BootstrapProperties props) {
        String installRootProp = props.getInstallRoot();
        if(installRootProp == null) {
            File installRoot = MainHelper.findInstallRoot();
            if(isValidInstallRoot(installRoot)) {
                installRootProp = installRoot.getAbsolutePath();
            }
        }
        return installRootProp;
    }

    private boolean isValidInstallRoot(File installRoot) {
        return installRoot == null ? false : isValidInstallRoot(installRoot.getAbsolutePath());
    }

    private List<URL> getModuleJarURLs(String installRoot) {
        if(installRoot == null) {
            return new ArrayList();
        }
        JarFile jarfile = null;
        try {
            // When running off the uber jar don't add extras module URLs to classpath.
            jarfile = new JarFile(Which.jarFile(getClass()));
            String mainClassName = jarfile.getManifest().
                    getMainAttributes().getValue("Main-Class");
            if (UberMain.class.getName().equals(mainClassName)) {
                return new ArrayList();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, LogFacade.CAUGHT_EXCEPTION, ex);
        } finally {
            if (jarfile != null) {
                try {
                    jarfile.close();
                } catch (IOException ex) {
                    // ignored
                }
            }
        }
        
        File modulesDir = new File(installRoot, "modules/");
        final File autostartModulesDir = new File(modulesDir, "autostart/");
        final List<URL> moduleJarURLs = new ArrayList<URL>();
        modulesDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isDirectory() && !pathname.equals(autostartModulesDir)) {
                    pathname.listFiles(this);
                } else if (pathname.getName().endsWith(JAR_EXT) &&
                        !moduleExcludes.contains(pathname.getName())) {
                    try {
                        moduleJarURLs.add(pathname.toURI().toURL());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, LogFacade.CAUGHT_EXCEPTION, ex);
                    }
                }
                return false;
            }
        });
        return moduleJarURLs;
    }

    private boolean isValidInstallRoot(String installRootPath) {
        if(installRootPath == null || !new File(installRootPath).exists()) {
            return false;
        }
        if(!new File(installRootPath, "modules").exists()) {
            return false;
        }
        if(!new File(installRootPath, "lib/dtds").exists()) {
            return false;
        }
        return true;
    }
    
    private static class StaticClassLoader extends URLClassLoader {
        public StaticClassLoader(ClassLoader parent, List<URL> moduleJarURLs) {
            super(moduleJarURLs.toArray(new URL[moduleJarURLs.size()]), parent);
        }
    }

}
