/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.bootstrap.HK2Populator;
import org.glassfish.hk2.bootstrap.impl.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.DomDocument;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;

/**
 * Created Dec 18, 2008
 *
 * @author <a href="mailto:justin.lee@sun.com">Justin Lee</a>
 */
public class Utils {
    private static final Logger LOGGER = Grizzly.logger(Utils.class);

    public static ServiceLocator getServiceLocator(final String fileURL) {
        URL url = Utils.class.getClassLoader().getResource(fileURL);
        if (url == null) {
            try {
                url = new URL(fileURL);
            } catch (MalformedURLException e) {
                throw new GrizzlyConfigException(e.getMessage());
            }
        }
        ServiceLocator habitat;
        try {
            habitat = getServiceLocator(url.openStream(), url.toString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return habitat;
    }

    public static ServiceLocator getServiceLocator(final InputStream inputStream, String name) {
        try {
            final ServiceLocator habitat = getNewServiceLocator(name);
            final ConfigParser parser = new ConfigParser(habitat);
            XMLInputFactory xif = XMLInputFactory.class.getClassLoader() == null
                    ? XMLInputFactory.newFactory()
                    : XMLInputFactory.newFactory(XMLInputFactory.class.getName(),
                    XMLInputFactory.class.getClassLoader());
            final DomDocument document = parser.parse(xif.createXMLStreamReader(inputStream));

            ServiceLocatorUtilities.addOneConstant(habitat, document);
            
            return habitat;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrizzlyConfigException(e.getMessage(), e);
        }
    }

    public static ServiceLocator getNewServiceLocator(String name) {
        ServiceLocator habitat = null;

        if (ServiceLocatorFactory.getInstance().find(name) == null) {
            ServiceLocator serviceLocator = ServiceLocatorFactory.getInstance()
                    .create(name);

            DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);
            DynamicConfiguration config = dcs.createDynamicConfiguration();

            config.commit();

            habitat = ServiceLocatorFactory.getInstance().create(name);

            try {
                HK2Populator.populate(serviceLocator,
                        new ClasspathDescriptorFileFinder(Utils.class.getClassLoader()), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return (habitat != null) ? habitat : ServiceLocatorFactory.getInstance().create(name);
    }
    
    @SuppressWarnings("UnusedDeclaration")
    public static String composeThreadPoolName(final NetworkListener networkListener) {
        return networkListener.getThreadPool() + '-' + networkListener.getPort();
    }

    /**
     * Load or create an Object with the specific service name and class name.
     *
     * @param habitat the HK2 {@link ServiceLocator}
     * @param clazz the class as mapped within the {@link ServiceLocator}
     * @param name the service name
     * @param realClassName the class name of the service
     * @return a service matching based on name and realClassName input
     *  arguments.
     */
    @SuppressWarnings({"unchecked"})
    public static <E> E newInstance(ServiceLocator habitat, Class<E> clazz,
            final String name, final String realClassName) {
        return newInstance(habitat, clazz, name, realClassName, null, null);
    }
    
    /**
         * Load or create an Object with the specific service name and class name.
         *
         * @param habitat the HK2 {@link ServiceLocator}
         * @param clazz the class as mapped within the {@link ServiceLocator}
         * @param name the service name
         * @param realClassName the class name of the service
         * @return a service matching based on name and realClassName input
         *  arguments.
         */
        @SuppressWarnings({"unchecked"})
        public static <E> E newInstance(ServiceLocator habitat, 
                                        Class<E> clazz,
                                        final String name, 
                                        final String realClassName, 
                                        Class<?>[] argTypes, 
                                        Object[] args) {
            boolean isInitialized = false;
            E instance = null;
            if (habitat != null) {
                instance = habitat.getService(clazz, name);
            }
            if (instance == null) {
                try {
                    if (argTypes == null || argTypes.length == 0) {
                        instance = (E) newInstance(realClassName);
                    } else {
                        instance = (E) newInstance(realClassName, argTypes, args);
                    }
                    isInitialized = true;
                } catch (Exception ignored) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, ignored.toString(), ignored);
                    }
                }
            } else {
                isInitialized = true;
            }
    
            if (!isInitialized) {
                LOGGER.log(Level.WARNING, "Instance could not be initialized. "
                        + "Class={0}, name={1}, realClassName={2}",
                        new Object[]{clazz, name, realClassName});
                return null;
            }
    
            return instance;
        }

    public static Object newInstance(String classname) throws Exception {
        return loadClass(classname).newInstance();
    }
    
    public static Object newInstance(String classname, 
                                     Class<?>[] argTypes, 
                                     Object[] args) throws Exception {
        final Class<?> clazz = loadClass(classname);        
        final Constructor c = clazz.getConstructor(argTypes);
        assert (c != null);
        return c.newInstance(args);
    }

    public static Class loadClass(String classname) throws ClassNotFoundException {
        Class clazz = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            try {
                clazz = cl.loadClass(classname);
            } catch (Exception ignored) {
            }
        }
        if (clazz == null) {
            clazz = Utils.class.getClassLoader().loadClass(classname);
        }
        return clazz;
    }

    public static boolean isDebugVM() {
        boolean debugMode = false;
        final List<String> l = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String s : l) {
            if (s.trim().startsWith("-Xrunjdwp:") || s.contains("jdwp")) {
                debugMode = true;
                break;
            }
        }
        return debugMode;
    }
}
