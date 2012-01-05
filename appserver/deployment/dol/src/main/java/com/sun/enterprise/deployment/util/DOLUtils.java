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

package com.sun.enterprise.deployment.util;

import com.sun.logging.LogDomains;

import java.util.logging.Logger;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.DeploymentContext;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.config.serverbeans.Applications;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.jvnet.hk2.component.Habitat;


/**
 * Utility class for convenienve methods
 *
 * @author  Jerome Dochez
 * @version 
 */
public class DOLUtils {
    
    private static Logger logger=null;
    

    /** no need to creates new DOLUtils */
    private DOLUtils() {
    }

    /**
     * @return a logger to use in the DOL implementation classes
     */
    public static Logger getDefaultLogger() {
        if (logger==null) {
            logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);
        }
        return logger;
    }

    public static boolean equals(Object a, Object b) {
        return ((a == null && b == null) ||
                (a != null && a.equals(b)));
    }

    public static List<URL> getLibraryJars(BundleDescriptor bundleDesc, ReadableArchive archive) throws IOException {
        List<URL> libraryURLs = new ArrayList<URL>();

        // add libraries referenced through manifest
        libraryURLs.addAll(DeploymentUtils.getManifestLibraries(archive));

        ReadableArchive parentArchive = archive.getParentArchive();

        if (parentArchive == null || bundleDesc == null) {
            // ear level or standalone module
            return libraryURLs;
        }

        File appRoot = new File(parentArchive.getURI());

        ModuleDescriptor moduleDesc = ((BundleDescriptor)bundleDesc).getModuleDescriptor();
        Application app = ((BundleDescriptor)moduleDesc.getDescriptor()).getApplication();

        // add libraries jars inside application lib directory
        libraryURLs.addAll(ASClassLoaderUtil.getAppLibDirLibrariesAsList(
            appRoot, app.getLibraryDirectory(), null));

       return libraryURLs;
   } 

   public static BundleDescriptor getCurrentBundleForContext(
       DeploymentContext context) {
       ExtendedDeploymentContext ctx = (ExtendedDeploymentContext)context;
       Application application = context.getModuleMetaData(Application.class);
       if (application == null) return null; // this can happen for non-JavaEE type deployment. e.g., issue 15869
       if (ctx.getParentContext() == null) {
           if (application.isVirtual()) {
               // standalone module
               return application.getStandaloneBundleDescriptor();
           } else {
               // top level 
               return application;
           }
       } else {
           // a sub module of ear
           return application.getModuleByUri(ctx.getModuleUri());
       }
   }

    public static boolean isRAConnectionFactory(Habitat habitat, 
        String type, Application thisApp) {
        // first check if this is a connection factory defined in a resource
        // adapter in this application
        if (isRAConnectionFactory(type, thisApp)) {
            return true;
        }

        // then check if this is a connection factory defined in a standalone 
        // resource adapter
        Applications applications = habitat.getComponent(Applications.class);
        if (applications != null) {
            List<com.sun.enterprise.config.serverbeans.Application> raApps = applications.getApplicationsWithSnifferType(com.sun.enterprise.config.serverbeans.Application.CONNECTOR_SNIFFER_TYPE, true);
            ApplicationRegistry appRegistry = habitat.getComponent(ApplicationRegistry.class);
            for (com.sun.enterprise.config.serverbeans.Application raApp : raApps) {
                ApplicationInfo appInfo = appRegistry.get(raApp.getName());
                if (isRAConnectionFactory(type, appInfo.getMetaData(Application.class))) {   
                    return true;
                }   
            }
        }
        return false; 
    }

    private static boolean isRAConnectionFactory(String type, Application app) {
        if (app == null) {
            return false;
        }
        for (ConnectorDescriptor cd : app.getBundleDescriptors(ConnectorDescriptor.class)) {
            if (cd.getConnectionDefinitionByCFType(type) != null) {
                return true;
            }
        }
        return false;
    }
}
