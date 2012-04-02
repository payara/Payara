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
package org.glassfish.elasticity.engine.commands;

import java.util.logging.Level;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.elasticity.engine.container.ElasticServiceContainer;
import org.glassfish.elasticity.engine.container.ElasticServiceManager;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.glassfish.hk2.scopes.PerLookup;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;
import org.glassfish.elasticity.config.serverbeans.ElasticServices;
import org.glassfish.elasticity.config.serverbeans.ElasticServiceConfig;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;

import com.sun.enterprise.config.serverbeans.Domain;

/**
 * Enable auto scaling
 * 
 * @author Mahesh Kannan
 *
 */
@Service(name="enable-auto-scaling")
@Scoped(PerLookup.class)
public class EnableAutoScaling
	implements AdminCommand {

	@Inject
	EngineUtil util;

    @Inject @Optional
    ElasticServices elasticServices;

    @Inject
    Domain domain;

	@Param(name="name", primary=true)
	private String name;

    @Inject
    ElasticServiceManager elasticServiceManager;
	
    public void execute(AdminCommandContext context) {
    	ElasticServiceContainer service = elasticServiceManager.getElasticServiceContainer(name);
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

         if (elasticServices == null)   {
            //service doesn't exist
            String msg = Strings.get("elasticity.not.enabled");
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        ElasticServiceConfig elasticService = elasticServices.getElasticService(name);
    	if (service != null && elasticService != null) {
            try {
                updateEnabledElement(name);
                service.setEnabled(true);
                util.getLogger().log(Level.INFO, "enabled elastic-service: " + name);
            } catch (org.jvnet.hk2.config.TransactionFailure ex){
                util.getLogger().log(Level.WARNING, "Exception while setting enabled flag: " + name, ex);
            }
    	} else {
	    	util.getLogger().log(Level.WARNING, "No such elastic-service named: " + name);
    	}

    }
        public void updateEnabledElement(final String name) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticServiceConfig welasticService = elasticServices.getElasticService(name);
                    welasticService = t.enroll(welasticService);
                    welasticService.setEnabled(true);
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
