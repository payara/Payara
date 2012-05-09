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

import org.glassfish.elasticity.config.serverbeans.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;
import java.beans.PropertyVetoException;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.elasticity.engine.container.ElasticServiceContainer;
import org.glassfish.elasticity.engine.container.ElasticEnvironmentContainer;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;
/*
  * command used by GUI for OOW
 */

@Service(name = "configure-elastic-service-limits")
@I18n("configure.elastic.service.limits")
@Scoped(PerLookup.class)
@RestEndpoints({ @RestEndpoint(configBean = ElasticServiceConfig.class, opType = OpType.POST, path = "configure-elastic-service-limits", description = "Configure Elastic Service limits") })
public class ConfigureElasticServiceLimits implements AdminCommand{

    @Inject (optional = true)
    ElasticServices elasticServices;

    @Inject
    Domain domain;

    @Inject
    ElasticEnvironmentContainer elasticServiceManager;

    @Param(name="service", primary=true)
    String servicename;

    @Param(name="min", optional=true)
    int min=-1;

    @Param(name="max", optional=true)
    int max=-1;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.getLogger();

        if (elasticServices == null)   {
            //service doesn't exist
            String msg = Strings.get("elasticity.not.enabled");
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        ElasticServiceConfig elasticService= elasticServices.getElasticService(servicename);
        if (elasticService == null) {
            //service doesn't exist
            String msg = Strings.get("noSuchService", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        int currentMax = elasticService.getMax();
        if (min > currentMax && min > max)  {
            String msg =  "Min must be less than max limit";
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
         }

        try {
            updateESElement(servicename);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.configure..elastic-service " + servicename);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }
         // notify elastic engine of cluster size change
        ElasticServiceContainer service = (ElasticServiceContainer) elasticServiceManager.getElasticServiceContainer(servicename);
        service.reconfigureClusterLimits(min, max);
    }

         public void updateESElement(final String servicename) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticServiceConfig welasticService = elasticServices.getElasticService(servicename);
                    if (welasticService != null ){
                        welasticService = t.enroll(welasticService);
                        if (min != -1)
                            welasticService.setMin(min);

                        if(max != -1 )
                             welasticService.setMax(max);

                     }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
