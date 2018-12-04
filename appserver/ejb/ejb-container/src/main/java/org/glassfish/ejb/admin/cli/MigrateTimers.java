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

package org.glassfish.ejb.admin.cli;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.EjbContainerUtil;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.internal.api.Target;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

@Service(name = "migrate-timers")
@PerLookup
@I18n("migrate.timers")
@org.glassfish.api.admin.ExecuteOn(value = {RuntimeType.INSTANCE}, ifNeverStarted = FailurePolicy.Error)
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="migrate-timers", 
        description="Migrate Timers")
})
public class MigrateTimers implements AdminCommand {

    static StringManager localStrings = StringManager.getManager(MigrateTimers.class);

    private static final Logger logger =
        LogDomains.getLogger(MigrateTimers.class, LogDomains.EJB_LOGGER);

    @Param(name = "target", optional = true, alias="destination",
        defaultValue=SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    public String target;

    private boolean needRedirect;

    @Param(name = "fromServer", primary = true, optional = false)
    public String fromServer;

    @Inject
    private EjbContainerUtil ejbContainerUtil;

    @Inject
    private Domain domain;

    @Inject
    Target targetUtil;

    @Inject
    private ServiceLocator habitat;

    /**
     * Executes the command
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        String error = validate();
        if (error != null) {
            report.setMessage(error);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            if (needRedirect) {
                needRedirect = false;
                ParameterMapExtractor mapExtractor = new ParameterMapExtractor(this);
                ParameterMap params = mapExtractor.extract();
                logger.info(localStrings.getString("migrate.timers.redirect",
                        target, params.toCommaSeparatedString()));

                ClusterOperationUtil.replicateCommand("migrate-timers", 
                        FailurePolicy.Error, FailurePolicy.Error, FailurePolicy.Error,
                        Arrays.asList(new String[]{target}),
                        context, params, habitat);
                return;
            }
            
            int totalTimersMigrated = migrateTimers(fromServer);
            report.setMessage(localStrings.getString("migrate.timers.count", 
                    totalTimersMigrated, fromServer, target));
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private String validate() {
        //verify fromServer is clusteredInstance
        Cluster fromServerCluster = targetUtil.getClusterForInstance(fromServer);
        if(fromServerCluster == null) {
            return localStrings.getString("migrate.timers.fromServerNotClusteredInstance", fromServer);
        }

        //verify fromServer is not running
        if (isServerRunning(fromServer)) {
            return localStrings.getString(
                    "migrate.timers.migrateFromServerStillRunning", fromServer);
        }
        
        //if destinationServer is not set, or set to DAS, pick a running instance
        //in the same cluster as fromServer
        if(target.equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
            List<Server> instances = fromServerCluster.getInstances();
            for(Server instance : instances) {
                if(instance.isRunning()) {
                    target = instance.getName();
                    needRedirect = true;
                }
            }
            //if destination is still DAS, that means no running server is available
            if(target.equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                return localStrings.getString("migrate.timers.noRunningInstanceToChoose",
                        target);
            }
        } else {
            //verify fromServer and destinationServer are in the same cluster, and
            //verify destination is a clustered instance.
            Cluster destinationServerCluster = targetUtil.getClusterForInstance(target);
            if (!fromServerCluster.getName().equals(destinationServerCluster.getName())) {
                return localStrings.getString(
                        "migrate.timers.fromServerAndTargetNotInSameCluster", fromServer, target);
            }
            //verify destinationServer is running
            if (!isServerRunning(target)) {
                return localStrings.getString("migrate.timers.destinationServerIsNotAlive", target);
            }
        }
        
        return null;
    }

    private boolean isServerRunning(String serverName) {
        Server server = domain.getServerNamed(serverName);
        return (server == null) ? false : server.isRunning();
    }

    private int migrateTimers( String serverId ) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "[MigrateTimers] migrating timers from {0}", serverId);
        }

        int result = 0;
        if (EJBTimerService.isPersistentTimerServiceLoaded()) {
            EJBTimerService ejbTimerService = EJBTimerService.getPersistentTimerService();
            if (ejbTimerService != null) {
                result = ejbTimerService.migrateTimers( serverId );
            }
        } else {
            //throw new IllegalStateException("EJB Timer service is null. "
                    //+ "Cannot migrate timers for: " + serverId);
        }

        return result;
    }
}
