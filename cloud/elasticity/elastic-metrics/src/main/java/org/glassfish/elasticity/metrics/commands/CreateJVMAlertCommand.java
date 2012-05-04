/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.metrics.commands;

import org.glassfish.elasticity.api.ElasticEngine;
import org.glassfish.elasticity.api.RootElementFinder;
import org.glassfish.elasticity.config.serverbeans.*;
import org.glassfish.elasticity.engine.util.ElasticCpasParentFinder;

import java.beans.PropertyVetoException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.Services;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;

import javax.inject.Inject;

//  import javax.inject.*;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/10/12
 */
@Service(name = "create-jvm-alert")
@I18n("create.jvm.alert")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({ @RestEndpoint(configBean = AlertConfig.class, opType = OpType.POST, path = "create-jvm-alert", description = "Create JVM alert") })
public class CreateJVMAlertCommand implements AdminCommand{

  @Inject
   TenantManager tenantManager;

  @Inject
  ElasticEngine elasticEngine;

   @Inject
   Services services;

  @Param(name="name", primary = true)
   String name;

  @Param(name="environment")
  String envname;

  @Param(name="schedule", optional = true)
  String schedule;

  @Param(name="sampleinterval", optional = true)
  int sampleInterval;

  @Param(name="enabled", defaultValue = "true", optional = true)
  boolean enabled;

  @Param(name="tenantid")
  String tenantid;

    Elastic elastic=null;

    Tenant tenant;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        tenantManager.setCurrentTenant(tenantid);
        /*
        tenant = tenantManager.get(Tenant.class);
        TenantServices ts = tenant.getServices();
        elastic =  (Elastic)ts.getServiceByType(Elastic.class);
        if (elastic != null) {
            System.out.println("Elastic element already exists");
            return;
        }
        */
        RootElementFinder elasticParentFinder = services.forContract(RootElementFinder.class).named("CPAS").get();
        ElasticAlerts elasticAlerts = elasticParentFinder.getAlertsParent(tenantid );
        ElasticAlert alert = null;
        try {
             alert = elasticParentFinder.addAlertElement(elasticAlerts);

//            createESElement();
         } catch(TransactionFailure e) {
            e.printStackTrace();
        }
        /*
        elastic =  (Elastic)ts.getServiceByType(Elastic.class);
         ElasticAlerts ea = elastic.getElasticAlerts();
        ElasticAlert alert = ea.getElasticAlert(name);
 */
        elasticEngine.getElasticEnvironment(envname).addAlert(alert);
        }

  private static class MyCode implements  SingleConfigCode<ElasticAlerts> {
                @Override
                public Object run(final ElasticAlerts eAlerts) throws TransactionFailure {

                    ElasticAlert alert = eAlerts.createChild(ElasticAlert.class);
                    alert.setName(("alert1"));
                    alert.setSchedule("10s");
                    alert.setType("jvm_memory");
                    eAlerts.getElasticAlert().add(alert);
                    return eAlerts;
                }
        }
}
