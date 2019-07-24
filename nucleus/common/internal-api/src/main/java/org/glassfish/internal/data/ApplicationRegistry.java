/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package org.glassfish.internal.data;


import org.jvnet.hk2.annotations.Service;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;

import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.glassfish.internal.deployment.Deployment;

/**
 * Registry for deployed Applications
 *
 * TODO : dochez
 * this class needs to go, I think we should use the configured tree (applications)
 * to store this list. This could be achieve once hk2 configured support Transient
 * objects attachment.
 */
@Service
@Singleton
public class ApplicationRegistry implements MonitoringDataSource {

    private Map<String, ApplicationInfo> apps = new HashMap<>();
    private final Map<String, Deployment.ApplicationDeployment> transientDeployments = new HashMap<>();

    public synchronized void add(String name, ApplicationInfo info) {
        apps.put(name, info);
    }

    public ApplicationInfo get(String name) {
        return apps.get(name);
    }

    public synchronized void remove(String name) {

        apps.remove(name);
    }

    public Set<String> getAllApplicationNames() {
        return apps.keySet();
    }

    /**
     * transient apps are deployed, but not initialized yet
     *
     * @param depl
     */
    public void addTransient(Deployment.ApplicationDeployment depl) {
        if(depl != null && depl.appInfo != null) {
            transientDeployments.put(depl.appInfo.getName(), depl);
        }
    }

    public void removeTransient(String appName) {
        transientDeployments.remove(appName);
    }

    public Deployment.ApplicationDeployment getTransient(String appName) {
        return transientDeployments.get(appName);
    }

    @Override
    public void collect(MonitoringDataCollector rootCollector) {
        rootCollector.in("application")
            .collectAll(apps, (collector, app) -> collector
                .collectNonZero("sniffers", app.getSniffers().size())
                .collectNonZero("engines", app.getEngineRefs().size())
                .collectNonZero("properties", app.getModuleProps().size())
                .collectNonZero("modules", app.getModuleInfos().size())
                .collectObjects(app.getModuleInfos(), ApplicationRegistry::collectModule));
    }

    private static void collectModule(MonitoringDataCollector collector, ModuleInfo module) {
        collector.tag("module", module.getName())
            .collectNonZero("sniffers", module.getSniffers().size())
            .collectNonZero("engines", module.getEngineRefs().size())
            .collectNonZero("properties", module.getModuleProps().size());
    }
}
