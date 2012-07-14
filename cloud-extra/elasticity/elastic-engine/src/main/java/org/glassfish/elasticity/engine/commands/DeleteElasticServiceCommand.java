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
import com.sun.enterprise.config.serverbeans.Domain;

import java.beans.PropertyVetoException;

import org.glassfish.elasticity.engine.container.ElasticServiceContainer;
import org.glassfish.elasticity.engine.container.ElasticEnvironmentContainer;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;

/**
 ** Remote AdminCommand to create an alert element.  This command is run only on DAS.
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/19/11
 */
@Service(name = "_delete-elastic-service")
@I18n("delete.ealastic.service")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
public class DeleteElasticServiceCommand implements AdminCommand {

  @Inject @Optional
  ElasticServices elasticServices;

  @Inject
  Domain domain;

  @Inject
  ElasticEnvironmentContainer elasticServiceManager;

  @Param(name="name", primary = true)
   String name;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.getLogger();
        if(elasticServices != null){
            ElasticServiceConfig elasticService= elasticServices.getElasticService(name);
            if (elasticService == null) {
                //service doesn't exist
                String msg = Strings.get("noSuchService", name);
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        }else{
                //service doesn't exist
                String msg = Strings.get("noSuchService", name);
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
        }

        //notify elastic container to run this alert
        ElasticServiceContainer service = (ElasticServiceContainer) elasticServiceManager.getElasticServiceContainer(name);
        // need to stop the alerts and the metric gatherers
//       service.stopContainer();

        try {
            deleteESElement(name);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.delete.service " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }

    }

    public void deleteESElement(final String alertName) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticServiceConfig elasticService = elasticServices.getElasticService(name);
                    if (elasticService != null ){
                        ElasticServices writeableService = t.enroll(elasticServices);
                        writeableService.getElasticService().remove(elasticService);
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
