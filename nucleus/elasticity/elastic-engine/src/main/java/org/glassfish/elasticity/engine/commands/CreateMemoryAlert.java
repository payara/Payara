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

/*
  * command used by GUI for OOW
 */

@Service(name = "create-memory-alert")
@I18n("create.memory.alert")
@Scoped(PerLookup.class)
public class CreateMemoryAlert implements AdminCommand{

    @Inject
    ElasticServices elasticServices;

    @Inject
    private CommandRunner cr;

    @Param (name ="alert", primary = true)
    String alertname;

    @Param(name="service")
    String servicename;

    @Param(name="threshold")
    int threshold;

    @Param(name="alarm-state", optional = true)
    String alarmstate;

    @Param(name="ok-state", optional = true)
    String okstate;

    @Param(name="sample-interval", optional = true)
    String sampleInterval;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        ElasticService elasticService= elasticServices.getElasticService(servicename);
        if (elasticService == null) {
            //service doesn't exist
            String msg = Strings.get("noSuchService", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        AlertConfig  alert = elasticService.getAlerts().getAlert(alertname);
        if (elasticService == null) {
            //alert doesn't exist
            String msg = Strings.get("noSuchAlert", alertname);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        //delete the existing alert

        CommandInvocation ci = cr.getCommandInvocation("delete-alert", report);
        ParameterMap map = new ParameterMap();
        map.add("service", servicename);
        map.add("DEFAULT", alertname);
        ci.parameters(map);
        ci.execute();

        //create a new alert with new values
        String expression = "any([jvm.memory.heap >" + threshold+"])" ;

        ci = cr.getCommandInvocation("create-alert", report);
        map = new ParameterMap();
        map.add("service", servicename);
        if (sampleInterval != null)
            map.add("sampleinterval",sampleInterval);
        map.add("expression", expression);
        map.add("DEFAULT", alertname);
        ci.parameters(map);
        ci.execute();
    }
}
