/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Cluster;
import java.util.logging.Logger;
import javax.inject.Inject;


import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;

@I18n("stop.cluster.command")
@Service(name="stop-cluster")
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="stop-cluster", 
        description="Stop Cluster",
        params={
            @RestParam(name="id", value="$parent")
        })
})
@Progress
public class StopClusterCommand implements AdminCommand {

    @Param(optional=false, primary=true)
    private String clusterName;

    @Param(optional=true, defaultValue="false")
    private boolean kill = false;

    @Inject
    private ServerEnvironment env;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner runner;

    @Param(optional = true, defaultValue = "false")
    private boolean verbose;

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        Logger logger = context.getLogger();

        logger.info(Strings.get("stop.cluster", clusterName));

        // Require that we be a DAS
        if(!env.isDas()) {
            String msg = Strings.get("cluster.command.notDas");
            logger.warning(msg);
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        ClusterCommandHelper clusterHelper = new ClusterCommandHelper(domain,
                runner);

        ParameterMap map = null;
        if (kill) {
            map = new ParameterMap();
            map.add("kill", "true");
        }
        try {
            // Run start-instance against each instance in the cluster
            String commandName = "stop-instance";
            clusterHelper.runCommand(commandName, map, clusterName, context,
                    verbose);
        } catch (CommandException e) {
            String msg = e.getLocalizedMessage();
            logger.warning(msg);
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(msg);
        }
    }
}
