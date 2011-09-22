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
package org.glassfish.elasticity.engine.commands;

import org.glassfish.elasticity.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.StringUtils;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.elasticity.config.serverbeans.*;
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
import java.util.logging.Logger;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;

/**
 ** Remote AdminCommand to create an alert element.  This command is run only on DAS.
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/19/11
 */
@Service(name = "_create-elastic-service")
@I18n("create.ealastic.service")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
public class CreateElasticServiceCommand implements AdminCommand {

  @Inject
  Domain domain;

  @Inject
  private CommandRunner cr;

  @Inject
  ElasticEngine elasticEngine;

  @Inject
   ElasticServiceManager elasticServiceManager;

  @Param(name="name", primary = true)
   String name;

    @Param(name="min", optional=true)
    int min=-1;

    @Param(name="max", optional=true)
    int max=-1;

    @Param(name="enabled", optional=true, defaultValue="true")
    boolean enabled;

    ElasticServices elasticServices =null;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

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
        ElasticService elasticService = elasticServices.getElasticService(name);
        elasticEngine.startElasticService(elasticService);

        //create lower bound alert for memory demo
        String expression = "any[avg(jvm_memory.heap.used)*100/jvm_memory.maxMemory]  <  20" ;

        ci = cr.getCommandInvocation("create-alert", report);
        map = new ParameterMap();
        map.add("service", name);
        map.add("expression", expression);
        map.add("DEFAULT", "__low-bound-alert");
        ci.parameters(map);
        ci.execute();

        //add alarm to the alert , only add alarm state
        ci = cr.getCommandInvocation("add-alert-action",report);
        map = new ParameterMap();
        map.add("service", name);
        map.add("actionref","scale-down-action");
        map.add("state","alarm-state");
        map.add("DEFAULT", "__low-bound-alert");
        ci.parameters(map);
        ci.execute();

    }

    public void createESElement(final String name) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticService elasticService = elasticServices.getElasticService(name);
                    if (elasticService != null ){
                        ElasticService writeableService = t.enroll(elasticService);

                        if (min !=-1)
                            writeableService.setMin(min);
                        if (max != -1)
                         writeableService.setMax(max);
                        writeableService.setEnabled(enabled);

                        MetricGatherers mgs = writeableService.createChild(MetricGatherers.class);
                        MetricGatherer mg  =  mgs.createChild(MetricGatherer.class);
                        mg.setName( "memory");
                        mgs.getMetricGatherer().add(mg);
                        writeableService.setMetricGatherers(mgs);

             // create the scale up action element

                        Actions actionsS = writeableService.createChild(Actions.class);
                        ScaleUpAction scaleUpAction = actionsS.createChild(ScaleUpAction.class);
                        scaleUpAction.setName("scale-up-action");
                        actionsS.setScaleUpAction(scaleUpAction);
                        writeableService.setActions(actionsS);

                        Alerts alerts = writeableService.createChild(Alerts.class);
                        writeableService.setAlerts(alerts);
                        //commit the transaction and create the scale down action or create a command to do it
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
