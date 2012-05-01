/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.elasticity.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.Domain;

import java.beans.PropertyVetoException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;

/**
 ** Remote AdminCommand to create an alert element.  This command is run only on DAS.
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/22/11
 */
@Service(name = "create-jvm-memory-alert")
@I18n("create.alert")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({ @RestEndpoint(configBean = AlertConfig.class, opType = OpType.POST, path = "create-jvm-memory-alert", description = "Create JVM memeory alert") })
public class CreateJVMMemoryAlertCommand implements AdminCommand {

  @Inject @Optional
  ElasticServices elasticServices;

  @Inject
  Domain domain;

  @Inject
  ElasticEngine elasticEngine;

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

   String serviceName = "gf-service-";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        serviceName = serviceName+envname;

        if (elasticServices == null)   {
            //service doesn't exist
            String msg = Strings.get("elasticity.not.found", envname);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        ElasticServiceConfig elasticService= elasticServices.getElasticService(serviceName);
        if (elasticService == null) {
            //service doesn't exist
            String msg = Strings.get("noSuchService", envname);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        // make sure the name of the alert is unique within this service
        if (elasticService.getAlerts().getAlert(name) != null) {
            String msg = Strings.get("alertNameExists", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        try {
            createAlertElement(name);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.create.alert " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }

        //notify elastic container to run this alert
        AlertConfig alert  = elasticServices.getElasticService(serviceName).getAlerts().getAlert(name);
        elasticEngine.getElasticEnvironment(envname).addAlert(alert);
    }

    public void createAlertElement(final String alertName) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticServiceConfig elasticService = elasticServices.getElasticService(serviceName);
                    if (elasticService != null ){
                    ElasticServiceConfig writeableService = t.enroll(elasticService);
                    Alerts writeableAlerts = elasticService.getAlerts();
                    if (writeableAlerts == null)
                        writeableAlerts =writeableService.createChild(Alerts.class);
                    else
                         writeableAlerts = t.enroll(writeableAlerts);

                    AlertConfig writeableAlert = writeableAlerts.createChild(AlertConfig.class);
                    if (name != null)
                        writeableAlert.setName(name);
                    if (schedule != null)
                        writeableAlert.setSchedule(schedule);
                    if (sampleInterval != 0)
                        writeableAlert.setSampleInterval(sampleInterval);
                    if (!enabled)
                        writeableAlert.setEnabled(enabled);
                    writeableAlert.setType("jvm_memory");
                    writeableAlerts.getAlert().add(writeableAlert);
                    writeableService.setAlerts(writeableAlerts);
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
