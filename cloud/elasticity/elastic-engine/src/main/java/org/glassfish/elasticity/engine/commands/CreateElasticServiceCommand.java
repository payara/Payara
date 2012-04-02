/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.engine.commands;

import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.elasticity.config.serverbeans.*;

import com.sun.enterprise.config.serverbeans.Domain;

import java.beans.PropertyVetoException;

import org.glassfish.elasticity.engine.container.ElasticEngine;
import org.glassfish.elasticity.engine.container.ElasticServiceManager;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;

/**
 ** Remote AdminCommand to create an elastic service element.  This command is run only on DAS.
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/19/11
 */
@Service(name = "_create-elastic-service")
@I18n("create.ealastic.service")
@Scoped(PerLookup.class)
@ExecuteOn(value={RuntimeType.ALL})
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
public class CreateElasticServiceCommand implements AdminCommand  {
 
	@Inject
	Services services;
	
  @Inject
  Domain domain;

  @Inject
  private CommandRunner cr;

  @Inject
  ElasticEngine elasticEngine;

  @Inject
   ElasticServiceManager elasticServiceManager;

  @Inject
  ServerEnvironment serverEnv;
  
  @Param(name="name", primary = true)
   String name;

    @Param(name="min", optional=true)
    int min=-1;

    @Param(name="max", optional=true)
    int max=-1;

    @Param(name="enabled", optional=true, defaultValue="true")
    boolean enabled;

    @Param(optional=true)
    private String target = "domain";


    ElasticServices elasticServices =null;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;
        
        logger.log(Level.INFO, "**_CREATE_ELASTIC_SERVICES_ELEMENT called....");

        CommandInvocation ci = cr.getCommandInvocation("_create-elastic-services-element", report);
        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);
        ci.parameters(map);
        ci.execute();

        elasticServices =  domain.getExtensionByType (ElasticServices.class);

        if  (min > max)  {
            String msg =  "Min must be less than max limit";
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
/*
            ci = cr.getCommandInvocation("_delete-elastic-service", report);
            map = new ParameterMap();
            map.add("DEFAULT", name);
            ci.parameters(map);
            ci.execute();
            */
            return;
         }

        try {
            createESElement(name);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.create.service " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }

        //notify elastic container to run
        ElasticServiceConfig elasticService = elasticServices.getElasticService(name);
        elasticEngine.startElasticService(elasticService);

        logger.log(Level.FINE, "Executed elasticEngine.startElasticService(" + elasticService.getName() + ")");

    }

    public void createESElement(final String name) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticServiceConfig elasticService = elasticServices.getElasticService(name);
                    if (elasticService != null ){
                        ElasticServiceConfig writeableService = t.enroll(elasticService);

                        if (min !=-1)
                            writeableService.setMin(min);
                        if (max != -1)
                         writeableService.setMax(max);
                        writeableService.setEnabled(enabled);
                        MetricGatherers mg = writeableService.createChild(MetricGatherers.class);
                        writeableService.setMetricGatherers(mg);

                        // create the scale up action element
                        Actions actionsS = writeableService.createChild(Actions.class);
                        ScaleUpAction scaleUpAction = actionsS.createChild(ScaleUpAction.class);
                        scaleUpAction.setName("scale-up-action");
                        scaleUpAction.setType("scale-up");
                        actionsS.setScaleUpAction(scaleUpAction);
                        writeableService.setActions(actionsS);

                        Alerts alerts = writeableService.createChild(Alerts.class);
                        writeableService.setAlerts(alerts);
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
