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
package org.glassfish.paas.lbplugin.cli;

import java.util.Properties;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.paas.lbplugin.LBProvisionerFactory;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.hk2.api.PerLookup;

/**
 * @author Jagadish Ramu
 */
@Service(name = "_associate-lb-service")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
public class AssociateLBService extends BaseLBService implements AdminCommand {

    @Inject
    private Habitat habitat;

    @Inject
    private CommandRunner commandRunner;

    @Param(name = "reconfig", optional=true, defaultValue="false")
    boolean isReconfig;

    @Param(name = "clustername")
    String clusterName;
    
    @Param(name = "domainname", optional=true)
    String domainName;

    @Param(name = "first", optional=true, defaultValue="true")
    boolean isFirst;
    
    @Param(name="props", optional=true, separator=':')
    Properties healthProps;

    @Inject
    private ServerContext serverContext;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        LBPluginLogger.getLogger().log(Level.INFO,"_associate-lb-service called.");
        try {
            retrieveVirtualMachine();
            LBProvisionerFactory.getInstance().getLBProvisioner()
                    .associateApplicationServerWithLB(appName, virtualMachine,
                    serviceName, domainName, commandRunner, clusterName, habitat,
                    serverContext.getInstallRoot().getAbsolutePath(),
                    isFirst, isReconfig,healthProps);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage("Service with name [" + serviceName + "] is associated with cluster  " + "[ " + clusterName + " ] successfully.");
        } catch (Exception ex) {
            LBPluginLogger.getLogger().log(Level.INFO,"exception",ex);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to associate service with name [" + serviceName + "] with cluster  " + "[ " + clusterName + " ] successfully.");
        }
    }
}
