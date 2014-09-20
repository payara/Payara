/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loadbalancer.admin.cli;

import com.sun.enterprise.config.serverbeans.Cluster;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import java.beans.PropertyVetoException;


import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ClusterRef;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.HealthChecker;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.loadbalancer.config.LbConfigs;
import org.glassfish.loadbalancer.config.LbConfig;

import org.glassfish.api.admin.*;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;

import javax.inject.Inject;

/**
 * This is a remote command that deletes health-checker config for cluster or
 * server.
 * @author Yamini K B
 */
@Service(name = "delete-http-health-checker")
@PerLookup
@TargetType(value={CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="delete-http-health-checker", 
        description="delete-http-health-checker",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="delete-http-health-checker", 
        description="delete-http-health-checker",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public final class DeleteHTTPHealthCheckerCommand implements AdminCommand {

    @Param(optional=true)
    String config;

    @Param(primary=true)
    String target;

    @Inject
    Domain domain;

    @Inject
    Target tgt;

    @Inject
    Logger logger;

    private ActionReport report;

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(DeleteHTTPHealthCheckerCommand.class);

    @Override
    public void execute(AdminCommandContext context) {

        report = context.getActionReport();

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        LbConfigs lbconfigs = domain.getExtensionByType(LbConfigs.class);
        if (lbconfigs == null) {
            String msg = localStrings.getLocalString("NoLbConfigsElement",
                    "Empty lb-configs");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
         
        if (config != null) {
            LbConfig lbConfig = lbconfigs.getLbConfig(config);
            deleteHealthCheckerInternal(lbConfig, target, false);
        } else {
            List<LbConfig> lbConfigs = lbconfigs.getLbConfig();
            if (lbConfigs.isEmpty()) {
                String msg = localStrings.getLocalString("NoLbConfigsElement",
                        "Empty lb-configs");
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            for (LbConfig lc:lbConfigs) {
                deleteHealthCheckerInternal(lc, target, true);
            }
        }   
    }

    /**
     * Deletes a health checker from a load balancer configuration.
     *
     * @param   lbConfig        Http load balancer configuration bean
     * @param   target          Name of a cluster or stand alone server instance
     * @param   ignoreFailure   if ignoreError is true, exceptions are not
     *                          thrown in the following cases
     *                          1). The specified server instance or cluster
     *                          does not exist in the LB config.
     *                          2).The target already contains the health checker
     *
     */
    private void deleteHealthCheckerInternal(LbConfig lbConfig, String target,
        boolean ignoreFailure) {

        // invalid lb config name
        if (lbConfig == null) {
            String msg = localStrings.getLocalString("InvalidLbConfigName", "Invalid LB configuration.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        String lbConfigName = lbConfig.getName();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[LB-ADMIN] deleteHealthChecker called - LB Config Name: "
                + lbConfigName + ", Target: " + target);
        }

        // null target
        if (target == null) {
            String msg = localStrings.getLocalString("Nulltarget", "Null target");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        // target is a cluster
        if (tgt.isCluster(target)) {
            ClusterRef  cRef = lbConfig.getRefByRef(ClusterRef.class, target);

            // cluster is not associated to this lb config
            if ((cRef == null) && (ignoreFailure == false)){
                String msg = localStrings.getLocalString("UnassociatedCluster",
                        "Load balancer configuration [{0}] does not have a reference to the given cluster [{1}].",
                        lbConfigName, target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            if (cRef != null) {
                HealthChecker hc = cRef.getHealthChecker();
                if (hc != null) {
                    removeHealthCheckerFromClusterRef(cRef);
                    String msg = localStrings.getLocalString("http_lb_admin.HealthCheckerDeleted",
                            "Health checker deleted for target {0}", target);
                    logger.info(msg);
                } else {
                   if (ignoreFailure == false) {
                       String msg = localStrings.getLocalString("HealthCheckerDoesNotExist",
                               "Health checker does not exist for target {0} in LB {1}", target, lbConfigName);
                       report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                       report.setMessage(msg);
                       return;
                    }
                }
            }

        // target is a server
        } else if (domain.isServer(target)) {
            ServerRef  sRef   = lbConfig.getRefByRef(ServerRef.class, target);

            // server is not associated to this lb config
            if ((sRef == null) && (ignoreFailure == false)) {
                String msg = localStrings.getLocalString("UnassociatedServer",
                        "Load balancer configuration [{0}] does not have a reference to the given server [{1}].",
                        lbConfigName, target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            if (sRef != null) {
                HealthChecker hc  = sRef.getHealthChecker();
                if (hc != null) {
                    removeHealthCheckerFromServerRef(sRef);
                    String msg = localStrings.getLocalString("http_lb_admin.HealthCheckerDeleted",
                            "Health checker deleted for target {0}", target);
                    logger.info(msg);
                } else {
                    if (ignoreFailure == false) {
                        String msg = localStrings.getLocalString("HealthCheckerDoesNotExist",
                               "Health checker does not exist for target {0} in LB {1}", target, lbConfigName);
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage(msg);
                        return;
                    }
                }
            }
        } else {
            String msg = localStrings.getLocalString("InvalidTarget", "Invalid target", target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
    }

    private void removeHealthCheckerFromClusterRef(ClusterRef cRef) {
        try {
            ConfigSupport.apply(new SingleConfigCode<ClusterRef>() {
            @Override
                public Object run(ClusterRef param) throws PropertyVetoException, TransactionFailure {                    
                    param.setHealthChecker(null);
                    return Boolean.TRUE;
                }
            }, cRef);
        } catch (TransactionFailure ex) {
            String msg = localStrings.getLocalString("FailedToRemoveHC", "Failed to remove health-checker");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            report.setFailureCause(ex);
            return;
        }
    }

    private void removeHealthCheckerFromServerRef(ServerRef sRef) {
        try {

            ConfigSupport.apply(new SingleConfigCode<ServerRef>() {
            @Override
                public Object run(ServerRef param) throws PropertyVetoException, TransactionFailure {
                    param.setHealthChecker(null);
                    return Boolean.TRUE;
                }
            }, sRef);
        } catch (TransactionFailure ex) {
            String msg = localStrings.getLocalString("FailedToRemoveHC", "Failed to remove health-checker");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            report.setFailureCause(ex);
            return;
        }
    }
}
