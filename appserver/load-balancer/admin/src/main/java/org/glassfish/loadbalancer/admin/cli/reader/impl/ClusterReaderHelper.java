/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loadbalancer.admin.cli.reader.impl;

import com.sun.enterprise.config.serverbeans.Application;

import java.util.List;
import java.util.ArrayList;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;

import org.glassfish.loadbalancer.admin.cli.reader.api.WebModuleReader;


import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import java.util.HashMap;
import java.util.HashSet;
import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;

/**
 * Impl class for ClusterReader. This provides loadbalancer 
 * data for a cluster.
 *
 * @author Kshitiz Saxena
 */
public class ClusterReaderHelper {

    /**
     * Returns the web module readers for a set of application refs.
     *
     * @param   _configCtx      Current Config context
     * @param   refs            Application ref(s) from cluster or stand alone
     *                          instance
     * @param   target          Name of the cluster or stand alone instance
     *
     * @return  WebModuleReader[]   Array of the corresponding web module 
     *                              reader(s).
     *
     * @throws  LbReaderException   In case of any error(s).
     */
    public static WebModuleReader[] getWebModules(Domain domain, ApplicationRegistry appRegistry,
            List<ApplicationRef> refs, String target) {

        List<WebModuleReader> list = new ArrayList<WebModuleReader>();
        Set<String> contextRoots = new HashSet<String>();

        Iterator<ApplicationRef> refAppsIter = refs.iterator();
        HashMap<String, ApplicationRef> refferedApps =
                new HashMap<String, ApplicationRef>();
        while (refAppsIter.hasNext()) {
            ApplicationRef appRef = refAppsIter.next();
            refferedApps.put(appRef.getRef(), appRef);
        }

        Applications applications = domain.getApplications();
        Set<Application> apps = new HashSet<Application>();
        apps.addAll(applications.getApplicationsWithSnifferType("web"));
        apps.addAll(applications.getApplicationsWithSnifferType("webservices"));

        Iterator<Application> appsIter = apps.iterator();
        while (appsIter.hasNext()) {
            Application app = appsIter.next();
            String appName = app.getName();
            if (!refferedApps.containsKey(appName)) {
                continue;
            }
            ApplicationInfo appInfo = appRegistry.get(appName);
            if (appInfo == null) {
                String msg = LbLogUtil.getStringManager().getString("UnableToGetAppInfo", appName);
                LbLogUtil.getLogger().log(Level.WARNING, msg);
                continue;
            }
            com.sun.enterprise.deployment.Application depApp = appInfo.getMetaData(com.sun.enterprise.deployment.Application.class);
            Iterator<BundleDescriptor> bundleDescriptorIter = depApp.getBundleDescriptors().iterator();

            while (bundleDescriptorIter.hasNext()) {
                BundleDescriptor bundleDescriptor = bundleDescriptorIter.next();
                try{
                if (bundleDescriptor instanceof WebBundleDescriptor) {
                    WebModuleReader wmr = new WebModuleReaderImpl(appName, refferedApps.get(appName),
                            app, (WebBundleDescriptor) bundleDescriptor);
                    if(!contextRoots.contains(wmr.getContextRoot())){
                        contextRoots.add(wmr.getContextRoot());
                        list.add(wmr);
                    }
                } else if (bundleDescriptor instanceof EjbBundleDescriptor) {
                    EjbBundleDescriptor ejbBundleDescriptor = (EjbBundleDescriptor) bundleDescriptor;
                    if (!ejbBundleDescriptor.hasWebServices()) {
                        continue;
                    }
                    Iterator<WebServiceEndpoint> wsIter = ejbBundleDescriptor.getWebServices().getEndpoints().iterator();
                    while (wsIter.hasNext()) {
                        WebServiceEndpointReaderImpl wsr = new WebServiceEndpointReaderImpl(appName, refferedApps.get(appName), app, wsIter.next());
                        if(!contextRoots.contains(wsr.getContextRoot())){
                            contextRoots.add(wsr.getContextRoot());
                            list.add(wsr);
                        }
                    }
                }
                }catch(LbReaderException ex){
                    String msg = LbLogUtil.getStringManager().getString("UnableToGetContextRoot", appName, ex.getMessage());
                    LbLogUtil.getLogger().log(Level.WARNING, msg);
                    if (LbLogUtil.getLogger().isLoggable(Level.FINE)) {
                        LbLogUtil.getLogger().log(Level.FINE, "Exception when getting context root for application", ex);
                    }
                }
            }
        }
        contextRoots.clear();
        // returns the web module reader as array
        WebModuleReader[] webModules = new WebModuleReader[list.size()];
        return (WebModuleReader[]) list.toArray(webModules);
    }
}
