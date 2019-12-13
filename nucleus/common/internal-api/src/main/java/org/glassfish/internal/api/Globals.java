/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2019] Payara Foundation and/or affiliates
 */

package org.glassfish.internal.api;

import java.io.File;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.net.MalformedURLException;
import java.net.URL;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Global class for storing the service locator for all hk2 services
 * <p>
 * Very sensitive class, anything stored here cannot be garbage collected
 *
 * @author Jerome Dochez
 */
@Service(name = "globals")
@Singleton
public class Globals {

    private static volatile ServiceLocator defaultHabitat;
    private static volatile ServiceLocator defaultHabitatWithModules;

    private static final Object STATIC_LOCK = new Object();
    
    // dochez : remove this once we can get rid of ConfigBeanUtilities class
    @SuppressWarnings("unused")
    @Inject
    private ConfigBeansUtilities utilities;
    
    @Inject
    private Globals(ServiceLocator habitat) {
        defaultHabitat = habitat;
    }

    /**
     * Gets the default service locator
     * <p>
     * This method is identical to {@link #getDefaultBaseServiceLocator}
     * @return 
     */
    public static ServiceLocator getDefaultBaseServiceLocator() {
    	return getDefaultHabitat();
    }
    
    /**
     * Gets the default service locator
     * <p>
     * This method is identical to {@link #getDefaultBaseServiceLocator}
     * @return 
     */
    public static ServiceLocator getDefaultHabitat() {
        return defaultHabitat;
    }

    /**
     * Gets the service from the default service locator with the specified type
     * <p>
     * See {@link ServiceLocator#getService(java.lang.Class, java.lang.annotation.Annotation...) }
     * @param <T>
     * @param type
     * @return 
     */
    public static <T> T get(Class<T> type) {
        return defaultHabitat.getService(type);
    }

    /**
     * Sets the default service locator to a different one
     * @param habitat 
     */
    public static void setDefaultHabitat(final ServiceLocator habitat) {
        defaultHabitat = habitat;
    }

    /**
     * Returns the default service locator. If it does not exist, one will be created.
     * <p>
     * See {@link #getStaticHabitat()}
     * @return 
     */
    public static ServiceLocator getStaticBaseServiceLocator() {
    	return getStaticHabitat();
    }
    
    /**
     * Returns the default service locator. If it does not exist, one will be created.
     * @return 
     */
    public static ServiceLocator getStaticHabitat() {
        if (defaultHabitat == null) {
            synchronized (STATIC_LOCK) {
                if (defaultHabitat == null) {
                    ModulesRegistry modulesRegistry = new StaticModulesRegistry(Globals.class.getClassLoader());
                    defaultHabitat = modulesRegistry.createServiceLocator("default");
                }
            }
        }

        return defaultHabitat;
    }

    /**
     * Returns the static service locator, but using the classloader used to load
     * the modules/ directory. This is useful when you need access to classes from
     * the modules/ directory from client code.
     * 
     * @return the static service locator with modules loaded
     * @throws MalformedURLException if the URL to a module is invalid
     */
    public static ServiceLocator getStaticHabitatWithModules() throws MalformedURLException {
        if (defaultHabitatWithModules == null) {
            synchronized(STATIC_LOCK) {
                if (defaultHabitatWithModules == null) {
                    defaultHabitatWithModules = createStaticHabitatWithModules();
                }
            }
        }
        return defaultHabitatWithModules;
    }
    
    private static ServiceLocator createStaticHabitatWithModules() throws MalformedURLException {
        // get the list of JAR files from the modules directory
        ArrayList<URL> urls = new ArrayList<>();
        File idir = new File(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        File mdir = new File(idir, "modules");
        for (File f : mdir.listFiles()) {
            if (f.toString().endsWith(".jar")) {
                urls.add(f.toURI().toURL());
            }
        }

        final URL[] urlsA = urls.toArray(new URL[urls.size()]);

        ClassLoader cl = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                return new URLClassLoader(urlsA, Globals.class.getClassLoader());
            }
        });

        ModulesRegistry registry = new StaticModulesRegistry(cl);
        return registry.createServiceLocator("default");
    }

    /**
     * The point of this service is to ensure that the Globals
     * service is properly initialized by the RunLevelService
     * at the InitRunLevel.  However, Globals itself must be
     * of scope Singleton because it us used in contexts where
     * the RunLevelService is not there
     * 
     * @author jwells
     *
     */
    @Service
    @RunLevel(value=(InitRunLevel.VAL - 1), mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING)
    public static class GlobalsInitializer {
        @SuppressWarnings("unused")
        @Inject
        private Globals globals;
    }
}
