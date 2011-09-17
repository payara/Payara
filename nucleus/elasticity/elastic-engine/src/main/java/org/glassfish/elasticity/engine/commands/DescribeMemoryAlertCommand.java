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
import java.beans.PropertyVetoException;
 import com.sun.enterprise.config.serverbeans.Domain;
 import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;
/*
  * command used by GUI for OOW
 */

@Service(name = "describe-memory-alert")
@I18n("describe.memory.alert")
@Scoped(PerLookup.class)
@RestEndpoints({ @RestEndpoint(configBean = ElasticServices.class, opType = OpType.GET, path = "describe-memory-alert", description = "Describe memory alert")})
public class DescribeMemoryAlertCommand implements AdminCommand{

    @Inject
    ElasticServices elasticServices;

    @Inject
    Domain domain;

    @Param(name="servicename",  optional=false)
    String servicename;

    @Param(name="alertname", primary=true)
    String alertname;

    private static final String EOL = "\n";

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
        //get the threshold value
        String expr = alert.getExpression();
        String threshold = "";
        int i =expr.indexOf(">");
        threshold = expr.substring(++i);

        String enabled = Boolean.toString(alert.getEnabled());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage("threshold=" + threshold + "; enabled=" + enabled);
        MessagePart mp = report.getTopMessagePart();
        mp.addProperty("service ", servicename );
        mp.addProperty("alert", alertname);
        mp.addProperty("threshold", threshold);
        mp.addProperty("enabled", enabled);
    }
}
