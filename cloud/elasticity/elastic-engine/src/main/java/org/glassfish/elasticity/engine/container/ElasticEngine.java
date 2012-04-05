/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.engine.container;


import org.glassfish.api.Startup;
import org.glassfish.common.util.timer.TimerSchedule;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.hk2.PostConstruct;
import javax.inject.Inject;

import org.glassfish.hk2.Services;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeEvent;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeListener;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.elasticity.config.serverbeans.ElasticServiceConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticServices;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import  org.glassfish.elasticity.engine.util.SetElasticTenantId;


/**
 * Elastic Engine for a service. An instance of ElasticEngine keeps track
 * of the Alerts for a service.
 *
 * @author Mahesh Kannan
 */
@Service
public class ElasticEngine
        implements Startup, PostConstruct, ServiceChangeListener {

    @Inject
    Services services;

    @Inject @Optional
    ElasticServices elasticServices;

    @Inject
    ElasticEngineThreadPool threadPool;

    @Inject
    MetricGathererContainer metricGathererContainer;


    @Inject
    ElasticServiceManager elasticServiceManager;

    @Inject
    TenantManager tm;

    private String serviceName;

    public void initialize(String serviceName) {
        this.serviceName = serviceName;
    }

    public void postConstruct() {
        if (serviceName == null) {
            //throw new IllegalStateException("ElasticEngine not initialized with a service name");
        }

//        metricGathererContainer.start();

//        System.out.println("Elastic Services: " + elasticServices);
        if (elasticServices != null && elasticServices.getElasticService() != null) {
            for (ElasticServiceConfig service : elasticServices.getElasticService()) {
                startElasticService(service);
            }
        }

        org.glassfish.common.util.timer.TimerSchedule ts = 
                new TimerSchedule().second("10");
        System.out.println("Now: "+ System.currentTimeMillis()
            + "; 10s from now: " + ts.getNextTimeout().getTimeInMillis());

    }

    public void startElasticService(ElasticServiceConfig service) {
        //need to create the tenant first then set it
//        SetElasticTenantId.setId();      // for now elasticity hardcodes the tenant Id

        if (service.getEnabled()) {
            ElasticServiceContainer container = services.byType(ElasticServiceContainer.class).get();
            container.initialize(service);
            elasticServiceManager.addElasticServiceContainer(service.getName(), container);
            container.startContainer();
        }
    }

    public void stop() {
        for (ElasticServiceContainer container : elasticServiceManager.containers()) {
            container.stopContainer();
        }

        metricGathererContainer.stop();
    }

    public Lifecycle getLifecycle() {
        return Startup.Lifecycle.START;
    }

    @Override
    public void onEvent(ServiceChangeEvent event) {
        System.out.println("ElasticEngine ServiceChangeEvent::onEvent " + event);
       // elasticServiceManager.onEvent(event);
//        try {
//            tm.setCurrentTenant("t1");
//
//            Tenant t = tm.get(Tenant.class);
//
//            System.out.println("Tenant t = " + t);
//
//            System.out.println("ElasticEngine ServiceChangeEvent::onEvent==> "
//                    + " name = " + event.getNewValue().getName()  + "; type = " + event.getNewValue().getServiceType().getName());
//
//        } catch (Exception ex) {
//            System.out.println("** NO TENANT AVAILABLE...");
//        }
    }
}
