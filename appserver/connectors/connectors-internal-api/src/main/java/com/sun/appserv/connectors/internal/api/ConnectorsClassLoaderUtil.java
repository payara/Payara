/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.connectors.internal.api;

import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.glassfish.api.admin.*;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import com.sun.enterprise.loader.ASURLClassLoader;
import com.sun.logging.LogDomains;
import java.util.Locale;

import javax.inject.Inject;


/**
 * Classloader util to create a new classloader for the provided .rar deploy directory.
 *
 * @author Jagadish Ramu
 */
@Service
@Singleton
public class ConnectorsClassLoaderUtil {

    @Inject
    private ClassLoaderHierarchy clh;

    //private static List<ConnectorClassFinder> systemRARClassLoaders;

    private Logger _logger = LogDomains.getLogger(ConnectorRuntime.class, LogDomains.RSR_LOGGER);

    @Inject
    private ServerEnvironment env;

    @Inject
    private ProcessEnvironment processEnv;

    @Inject
    Events events;


    private volatile boolean rarsInitializedInEmbeddedServerMode;

    public ConnectorClassFinder createRARClassLoader(String moduleDir, ClassLoader deploymentParent,
                                                     String moduleName, List<URI> appLibs)
            throws ConnectorRuntimeException {

        ClassLoader parent = null;

        //For standalone rar :
        //this is not a normal application and hence cannot use the provided parent during deployment.
        //setting the parent to connector-class-loader's parent (common class-loader) as this is a .rar
        //For embedded rar :
        //use the deploymentParent as the class-finder created won't be part of connector class loader
        //service hierarchy
        if(deploymentParent == null){
            parent = clh.getCommonClassLoader();
        }else{
            parent = deploymentParent;
        }
        return createRARClassLoader(parent, moduleDir, moduleName, appLibs);
    }

    private DelegatingClassLoader.ClassFinder getLibrariesClassLoader(final List<URI> appLibs)
            throws MalformedURLException, ConnectorRuntimeException {
        try {
            return (DelegatingClassLoader.ClassFinder) AccessController.doPrivileged(new PrivilegedExceptionAction(){
                public Object run() throws Exception {
                    return clh.getAppLibClassFinder(appLibs);
                }
            });
        } catch (PrivilegedActionException e) {
            _logger.log(Level.SEVERE, "error.creating.libraries.classloader", e);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(e.getMessage());
            cre.initCause(e);
            throw cre;
        }
    }

    private ConnectorClassFinder createRARClassLoader(final ClassLoader parent, String moduleDir,
                                                      final String moduleName, List<URI> appLibs)
            throws ConnectorRuntimeException{
        ConnectorClassFinder cl = null;

        try{
            final DelegatingClassLoader.ClassFinder librariesCL = getLibrariesClassLoader(appLibs);
            cl = (ConnectorClassFinder)AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                        final ConnectorClassFinder ccf = new ConnectorClassFinder(parent, moduleName, librariesCL);
                        if (processEnv.getProcessType().isEmbedded()) {
                            events.register(new EventListener() {
                                public void event(Event event) {
                                    if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
                                        ccf.done();
                                    }
                                }
                            });
                        }
                        return ccf;
                }
            });
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "error.creating.connector.classloader", ex);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(ex.getMessage());
            cre.initCause(ex);
            throw cre;
        }

        File file = new File(moduleDir);
        try {
            cl.appendURL(file.toURI().toURL());
            appendJars(file, cl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return cl;
    }

    private boolean extractRar(String rarName, String destination){
        String rarFileName = rarName + ConnectorConstants.RAR_EXTENSION;
        return ConnectorsUtil.extractRar(destination+rarFileName, rarFileName, destination);
    }

    public Collection<ConnectorClassFinder> getSystemRARClassLoaders() throws ConnectorRuntimeException {
            //if (systemRARClassLoaders == null) {

            if (processEnv.getProcessType().isEmbedded() && !rarsInitializedInEmbeddedServerMode) {
                synchronized (ConnectorsClassLoaderUtil.class){
                    if(!rarsInitializedInEmbeddedServerMode){
                        String installDir = System.getProperty(ConnectorConstants.INSTALL_ROOT) + File.separator;
                        for (String jdbcRarName : ConnectorConstants.jdbcSystemRarNames) {
                            String rarPath = ConnectorsUtil.getSystemModuleLocation(jdbcRarName);
                            File rarDir = new File(rarPath);
                            if(!rarDir.exists()){
                                extractRar(jdbcRarName, installDir);
                            }
                        }
                        rarsInitializedInEmbeddedServerMode = true;
                    }
                }
            }

            List<ConnectorClassFinder> classLoaders = new ArrayList<ConnectorClassFinder>();
            for (String rarName : ConnectorsUtil.getSystemRARs()) {

                String location = ConnectorsUtil.getSystemModuleLocation(rarName);

                List<URI> libraries ;

                if (processEnv.getProcessType().isEmbedded()) {
                    libraries = new ArrayList<URI>();
                } else {
                    libraries = ConnectorsUtil.getInstalledLibrariesFromManifest(location, env);
                }

                ConnectorClassFinder ccf = createRARClassLoader(location, null, rarName, libraries);
                classLoaders.add(ccf);
            }
        //    systemRARClassLoaders = classLoaders;
        //}
        //return systemRARClassLoaders;
        return classLoaders;
    }


    public ConnectorClassFinder getSystemRARClassLoader(String rarName) throws ConnectorRuntimeException {
        if (ConnectorsUtil.belongsToSystemRA(rarName)) {
            DelegatingClassLoader dch = clh.getConnectorClassLoader(null);
            for (DelegatingClassLoader.ClassFinder cf : dch.getDelegates()) {
                if (cf instanceof ConnectorClassFinder) {
                    if (rarName.equals(((ConnectorClassFinder) cf).getResourceAdapterName())) {
                        return (ConnectorClassFinder) cf;
                    }
                }
            }
        }
        throw new ConnectorRuntimeException("No Classloader found for RA [ " + rarName + " ]");
    }

    private void appendJars(File moduleDir, ASURLClassLoader cl) throws MalformedURLException {
        //TODO for embedded rars -consider MANIFEST.MF's classpath attribute
        if (moduleDir.isDirectory()) {
            for (File file : moduleDir.listFiles()) {
                if (file.getName().toUpperCase(Locale.getDefault()).endsWith(".JAR")) {
                    cl.appendURL(file.toURI().toURL());
                } else if (file.isDirectory()) {
                    appendJars(file, cl); //recursive add
                }
            }
        }
    }
}
