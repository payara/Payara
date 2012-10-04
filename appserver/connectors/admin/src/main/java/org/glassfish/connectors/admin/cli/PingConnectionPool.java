/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Ping Connection Pool Command
 * 
 */
@Service(name="ping-connection-pool")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value={RuntimeType.DAS})
@I18n("ping.connection.pool")
@RestEndpoints({
    @RestEndpoint(configBean=JdbcConnectionPool.class,
        opType=RestEndpoint.OpType.GET, 
        path="ping", 
        description="Ping",
        params={
            @RestParam(name="id", value="$parent")
        }),
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET, 
        path="ping-connection-pool", 
        description="Ping")
})
public class PingConnectionPool implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(PingConnectionPool.class);
                                                    
    @Param(name="pool_name", primary=true)
    private String poolName;

    @Inject
    private ConnectorRuntime connRuntime;

    @Inject
    private Domain domain;

    @Param(name="appname", optional=true)
    private String applicationName;

    @Param(name="modulename", optional=true)
    private String moduleName;

    @Inject
    private ConnectionPoolUtil poolUtil;

    @Inject
    private Applications applications;

    @Param(optional = true, alias = "targetName", obsolete = true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        boolean status = false;
        Resources resources = domain.getResources();
        String scope = "";
        if(moduleName != null){
            if(!poolUtil.isValidModule(applicationName, moduleName, poolName, report)){
                return ;
            }
            Application application = applications.getApplication(applicationName);
            Module module = application.getModule(moduleName);
            resources = module.getResources();
            scope = "java:module/";
        }else if(applicationName != null){
            if(!poolUtil.isValidApplication(applicationName, poolName, report)){
                return;    
            }
            Application application = applications.getApplication(applicationName);
            resources = application.getResources();
            scope = "java:app/";
        }

        if(!poolUtil.isValidPool(resources, poolName, scope, report)){
            return;
        }

        PoolInfo poolInfo = new PoolInfo(poolName, applicationName, moduleName);
        try {
            status = connRuntime.pingConnectionPool(poolInfo);
            if (status) {
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(
                        localStrings.getLocalString("ping.connection.pool.fail",
                                "Ping Connection Pool for {0} Failed", poolInfo));
            }
        } catch (Exception e) {
            report.setMessage(
                    localStrings.getLocalString("ping.connection.pool.fail",
                            "Ping Connection Pool for {0} Failed", poolInfo));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
