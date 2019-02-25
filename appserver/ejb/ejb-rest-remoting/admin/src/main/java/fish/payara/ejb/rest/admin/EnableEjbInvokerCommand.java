/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.ejb.rest.admin;

import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CLUSTERED_INSTANCE;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;

import javax.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.deployment.autodeploy.AutoDeploymentOperation;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * This command enables the EJB invoker endpoint and optionally allows it to set an
 * alternative context root.
 * 
 * <p>
 * This happens by the deploying a small application in <code>/domains/[domain]/endpoints/ejb-invoker</code>
 * which by default is not deployed. The default context root of this application is 
 * <code>/ejb-invoker</code>.
 * 
 * @author Arjan Tijms
 *
 */
@Service(name = "enable-ejb-invoker")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = { DAS, STANDALONE_INSTANCE, CLUSTER, CLUSTERED_INSTANCE, CONFIG })
public class EnableEjbInvokerCommand implements AdminCommand {

    private final String ENDPOINT = "endpoints/ejb-invoker";
    
    @Param(name = "contextRoot", primary = true, optional = true)
    private String contextRoot;
    
    @Inject
    private ServerEnvironment serverEnvironment;
    
    @Inject
    private ServiceLocator serviceLocator;

    @Override
    public void execute(AdminCommandContext context) {
        AutoDeploymentOperation autoDeploymentOperation = AutoDeploymentOperation.newInstance(
                serviceLocator,
                serverEnvironment.getInstanceRoot().toPath().resolve(ENDPOINT).toFile(), 
                getDefaultVirtualServer(),
                getTarget(),
                contextRoot
                );
        
        AutodeploymentStatus deploymentStatus = autoDeploymentOperation.run();
        
        ActionReport report = context.getActionReport();
        report.setActionExitCode(deploymentStatus.getExitCode());
    }
    
    private String getDefaultVirtualServer() {
        // XXX write this?
        return null;
    }

    private String getTarget() {
        // XXX write this? Note that AutoDeploymentOperation doesn't support it now.
        return null;
    }
}
