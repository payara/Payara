/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2024] [Payara Foundation and/or its affiliates]

package com.sun.appserv.connectors.internal.api;

import jakarta.inject.Provider;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.api.*;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.logging.LogDomains;

import jakarta.inject.Inject;

/**
 * We support two policies:
 * 1. All standalone RARs are available to all other applications. This is the
 * Java EE 5 specific behavior.
 * 2. An application has visbility to only those standalone RARs that it
 * depends on. This is the new behavior defined in Java EE 6 as well as
 * JCA 1.6 spec. 
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Service
public class ConnectorClassLoaderServiceImpl implements ConnectorClassLoaderService {

    /**
     * This class loader is used when we have just a single connector
     * class loader for all applications. In other words, we make every
     * standalone RARs available to all applications.
     */
    private final AtomicReference<DelegatingClassLoader> globalConnectorCL = new AtomicReference<>();
    private final AtomicReference<DelegatingClassLoader> globalConnectorWithWarLibCL = new AtomicReference<>();

    @Inject
    private AppSpecificConnectorClassLoaderUtil appsSpecificCCLUtil;
    @Inject
    private Provider<ConnectorsClassLoaderUtil> connectorsClassLoaderUtil;

    private Logger logger = LogDomains.getLogger(ConnectorClassLoaderServiceImpl.class, LogDomains.RSR_LOGGER);


    /**
     * provides connector-class-loader for the specified application
     * If application is null, global connector class loader will be provided
     * @param appName application-name
     * @return class-loader
     */
    public DelegatingClassLoader getConnectorClassLoader(String appName) {
        DelegatingClassLoader loader = null;

        if (hasGlobalAccessForRARs(appName)) {
            loader = getGlobalConnectorClassLoader();
        } else {
            appsSpecificCCLUtil.detectReferredRARs(appName);
            loader = createConnectorClassLoaderForApplication(appName);
        }
        return loader;
    }

    private boolean hasGlobalAccessForRARs(String appName) {
        return appName == null || appsSpecificCCLUtil.useGlobalConnectorClassLoader() ||
                appsSpecificCCLUtil.getRequiredResourceAdapters(appName).contains
                        (ConnectorConstants.RAR_VISIBILITY_GLOBAL_ACCESS);
    }

    private DelegatingClassLoader getGlobalConnectorClassLoader() {
        // We do not have dependency on common-class-loader explicitly
        // and also cannot initialize globalConnectorCL during postConstruct via ClassLoaderHierarchy
        // which will result in circular dependency injection between kernel and connector module
        // Hence initializing globalConnectorCL lazily
        UnaryOperator<DelegatingClassLoader> updateOperator = currentValue -> {
            if (currentValue == null) {
                //[parent is assumed to be common-class-loader in ConnectorClassLoaderUtil.createRARClassLoader() also]
                var newValue = AccessController.doPrivileged(new PrivilegedAction<DelegatingClassLoader>() {
                    public DelegatingClassLoader run() {
                        DelegatingClassLoader dcl = new DelegatingClassLoader(connectorsClassLoaderUtil.get().getCommonClassLoader());
                        for (DelegatingClassLoader.ClassFinder cf : appsSpecificCCLUtil.getSystemRARClassLoaders()) {
                            dcl.addDelegate(cf);
                        }
                        return dcl;
                    }
                });

                for (DelegatingClassLoader.ClassFinder cf : appsSpecificCCLUtil.getSystemRARClassLoaders()) {
                    newValue.addDelegate(cf);
                }
                return newValue;
            }
            return currentValue;
        };
        if (DeploymentUtils.useWarLibraries(DeploymentUtils.getCurrentDeploymentContext())) {
            return globalConnectorWithWarLibCL.updateAndGet(updateOperator);
        } else {
            return globalConnectorCL.updateAndGet(updateOperator);
        }
    }

    private DelegatingClassLoader createConnectorClassLoaderForApplication(String appName){

        DelegatingClassLoader appSpecificConnectorClassLoader =
                new DelegatingClassLoader(connectorsClassLoaderUtil.get().getCommonClassLoader());

        //add system ra classloaders
        for(DelegatingClassLoader.ClassFinder cf : appsSpecificCCLUtil.getSystemRARClassLoaders()){
            appSpecificConnectorClassLoader.addDelegate(cf);
        }
        
        for(String raName : appsSpecificCCLUtil.getRARsReferredByApplication(appName)){
            addRarClassLoader(appName, appSpecificConnectorClassLoader, raName);
        }

        for(String raName : appsSpecificCCLUtil.getRequiredResourceAdapters(appName)){
            addRarClassLoader(appName, appSpecificConnectorClassLoader, raName);
        }
        return appSpecificConnectorClassLoader;
    }

    private void addRarClassLoader(String appName, DelegatingClassLoader appSpecificConnectorClassLoader, 
                                   String raName) {
        if(logger.isLoggable(Level.FINEST)){
                logger.finest("raName for app [ "+appName+" ] : " + raName);
        }
        DelegatingClassLoader.ClassFinder cf = getClassFinder(raName);

        if(cf != null){
            appSpecificConnectorClassLoader.addDelegate(cf);
        }else{
            //not possible
/*          TODO V3
            if(!ConnectorsUtil.isEmbedded(appName, raName)){
                throw new IllegalStateException("RAR Classloader of RAR [ "+raName+" ] not found for " +
                    "application [ "+appName+" ]");
            }
*/
        }
    }

    private DelegatingClassLoader.ClassFinder getClassFinder(String raName) {
        List<DelegatingClassLoader.ClassFinder> delegates = getGlobalConnectorClassLoader().getDelegates();
        DelegatingClassLoader.ClassFinder classFinder = null;
        for(DelegatingClassLoader.ClassFinder cf : delegates){
            if(raName.equals(((ConnectorClassFinder)cf).getResourceAdapterName())){
                classFinder = cf;
                break;
            }
        }
        return classFinder;
    }
}
