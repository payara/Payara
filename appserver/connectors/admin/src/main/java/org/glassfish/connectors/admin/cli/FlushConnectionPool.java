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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service(name = "flush-connection-pool")
@PerLookup
@I18n("flush.connection.pool")
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.POST, 
        path="flush-connection-pool", 
        description="flush-connection-pool")
})
public class FlushConnectionPool implements AdminCommand {
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(FlushConnectionPool.class);

    @Param(name = "pool_name", primary = true)
    private String poolName;

    @Inject
    private Domain domain;

    @Param(name="appname", optional=true)
    private String applicationName;

    @Param(name="modulename", optional=true)
    private String moduleName;

    @Inject
    private Applications applications;

    @Inject
    private ConnectionPoolUtil poolUtil;

    @Inject
    private ConnectorRuntime _runtime;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Resources resources = domain.getResources();
        String scope = "";
        if(moduleName != null){
            if(!poolUtil.isValidModule(applicationName, moduleName, poolName, report)){
                return ;
            }
            Application application = applications.getApplication(applicationName);
            Module module = application.getModule(moduleName);
            resources = module.getResources();
            scope = ConnectorConstants.JAVA_MODULE_SCOPE_PREFIX;
        }else if(applicationName != null){
            if(!poolUtil.isValidApplication(applicationName, poolName, report)){
                return;
            }
            Application application = applications.getApplication(applicationName);
            resources = application.getResources();
            scope = ConnectorConstants.JAVA_APP_SCOPE_PREFIX;
        }

        if(!poolUtil.isValidPool(resources, poolName, scope, report)){
            return;
        }

        boolean poolingEnabled = false;
        ResourcePool pool =
                (ResourcePool) ConnectorsUtil.getResourceByName(resources, ResourcePool.class, poolName);
        if(pool instanceof ConnectorConnectionPool){
            ConnectorConnectionPool ccp = (ConnectorConnectionPool)pool;
            poolingEnabled = Boolean.valueOf(ccp.getPooling());
        }else{
            JdbcConnectionPool ccp = (JdbcConnectionPool)pool;
            poolingEnabled = Boolean.valueOf(ccp.getPooling());
        }

        if(!poolingEnabled){
            String i18nMsg = localStrings.getLocalString("flush.connection.pool.pooling.disabled",
                    "Attempt to Flush Connection Pool failed because Pooling is disabled for pool : {0}", poolName);
            report.setMessage(i18nMsg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            PoolInfo poolInfo = new PoolInfo(poolName, applicationName, moduleName);
            _runtime.flushConnectionPool(poolInfo);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (ConnectorRuntimeException e) {
            report.setMessage(localStrings.getLocalString("flush.connection.pool.fail",
                    "Flush connection pool for {0} failed", poolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
