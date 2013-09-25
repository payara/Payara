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
import java.util.ArrayList;
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
 * This is a remote command that creates health-checker config for cluster or
 * server.
 * @author Yamini K B
 */
@Service(name = "create-http-health-checker")
@PerLookup
@TargetType(value={CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-http-health-checker", 
        description="",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-http-health-checker", 
        description="",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public final class CreateHTTPHealthCheckerCommand implements AdminCommand {

    @Param(optional=true, defaultValue="10")
    String timeout;

    @Param(optional=true, defaultValue="30")
    String interval;

    @Param(optional=true, defaultValue="/")
    String url;

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
        new LocalStringManagerImpl(CreateHTTPHealthCheckerCommand.class);    

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
            createHealthCheckerInternal(url,interval,timeout,lbConfig,
            config ,target);
        } else {
            List<LbConfig> lbConfigs = lbconfigs.getLbConfig();
            if (lbConfigs.size() == 0) {
                String msg = localStrings.getLocalString("NoLbConfigsElement", "No LB configs defined");
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            List<LbConfig> match = null;
            
            match = matchLbConfigToTarget(lbConfigs, target);
            
            if ( (match == null) || (match.size() == 0) ) {
                String msg = localStrings.getLocalString("UnassociatedTarget", "No LB config references target {0}", target);
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            for (LbConfig lc:match){
                createHealthCheckerInternal(url,interval,timeout,
                    lc, lc.getName(), target);
            }
        }
   
    }

    /**
     * This is to create a health checker for a cluster configuration. By
     * default the healh checker will be configured.  This applies only
     * to our native load balancer.
     *
     * @param   url   the URL to ping so as to determine the health state
     *   of a listener.
     *
     * @param   interval   specifies the interval in seconds at which health
     *   checks of unhealthy instances carried out to check if the instances
     *   has turned healthy. Default value is 30 seconds. A value of 0 would
     *   imply that health check is disabled.
     *
     * @param   timeout    timeout interval in seconds within which response
     *   should be obtained for a health check request; else the instance would
     *   be considered unhealthy.Default value is 10 seconds.
     *
     * @param   lbConfig    the load balancer configuration bean
     * @param   lbConfigName    the load balancer configuration's name
     *
     * @param   target      name of the target - cluster or stand alone
     *  server instance
     *
     * @throws CommandException   If the operation is failed
     */
    private void createHealthCheckerInternal(String url, String interval,
            String timeout, LbConfig lbConfig, String lbConfigName, String target)
    {
        // invalid lb config name
        if (lbConfigName == null) {
            String msg = localStrings.getLocalString("InvalidLbConfigName", "Invalid LB configuration.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        lbConfigName = lbConfig.getName();
        // print diagnostics msg
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[LB-ADMIN] createHealthChecker called - URL "
                + url + ", Interval " + interval + ", Time out "
                + timeout + ", LB Config  " + lbConfigName
                + ", Target " + target);
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
            if (cRef == null){
                String msg = localStrings.getLocalString("UnassociatedCluster",
                        "Load balancer configuration [{0}] does not have a reference to the given cluster [{1}].",
                        lbConfigName, target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            if (cRef.getHealthChecker() == null) {
                try {
                    addHealthChecker(cRef);
                } catch (TransactionFailure ex) {
                    String msg = localStrings.getLocalString("FailedToAddHC", "Failed to add health checker");
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    report.setFailureCause(ex);
                    return;
                }
                logger.info(localStrings.getLocalString("http_lb_admin.HealthCheckerCreated",
                        "Health checker created for target {0}", target));
            } else {                
                String msg = localStrings.getLocalString("HealthCheckerExists",
                        "Health checker server/cluster [{0}] already exists.", target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        // target is a server
        } else if (domain.isServer(target)) {
            ServerRef sRef = lbConfig.getRefByRef(ServerRef.class, target);

            // server is not associated to this lb config
            if (sRef == null){
                String msg = localStrings.getLocalString("UnassociatedServer",
                        "Load balancer configuration [{0}] does not have a reference to the given server [{1}].",
                        lbConfigName, target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            if (sRef.getHealthChecker() == null) {
                try {
                    addHealthChecker(sRef);
                } catch (TransactionFailure ex) {
                    String msg = localStrings.getLocalString("FailedToAddHC", "Failed to add health checker");
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    report.setFailureCause(ex);
                    return;
                }
                logger.info(localStrings.getLocalString("http_lb_admin.HealthCheckerCreated",
                        "Health checker created for target {0}", target));
            } else {
                String msg = localStrings.getLocalString("HealthCheckerExists",
                        "Health checker server/cluster [{0}] already exists.", target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

        // unknown target
        } else {
            String msg = localStrings.getLocalString("InvalidTarget", "Invalid target", target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }        
    }

    private void addHealthChecker(final ClusterRef ref)
                                throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<ClusterRef>() {
                @Override
                public Object run(ClusterRef param) throws PropertyVetoException, TransactionFailure {
                    HealthChecker hc = param.createChild(HealthChecker.class);
                    if (url != null)       { hc.setUrl(url);                    }
                    if (interval != null)  { hc.setIntervalInSeconds(interval); }
                    if (timeout != null)   { hc.setTimeoutInSeconds(timeout);   }

                    param.setHealthChecker(hc);
                    return Boolean.TRUE;
                }
        }, ref);
    }

    private void addHealthChecker(final ServerRef ref)
                                throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<ServerRef>() {
                @Override
                public Object run(ServerRef param) throws PropertyVetoException, TransactionFailure {
                    HealthChecker hc = param.createChild(HealthChecker.class);
                    if (url != null)       { hc.setUrl(url);                    }
                    if (interval != null)  { hc.setIntervalInSeconds(interval); }
                    if (timeout != null)   { hc.setTimeoutInSeconds(timeout);   }

                    param.setHealthChecker(hc);
                    return Boolean.TRUE;
                }
        }, ref);
    }

    /**
     * Returns an array of LbConfigs that has a reference to the target
     * server or cluster. If there are no references found for the
     * target or the arguments are null, this method returns null.
     *
     * @param  lbConfigs  array of existing LbConfigs in the system
     * @param  target     name of server or cluster
     *
     * @return array of LbConfigs that has a ref to the target server
     * 
     */
    private List<LbConfig> matchLbConfigToTarget(List<LbConfig> lbConfigs,
            String target)
    {
        List<LbConfig> list = null;

        // bad target
        if (target == null) {
            String msg = localStrings.getLocalString("Nulltarget", "Null target");
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return null;
        }

        // system has lb configs defined
        if (!lbConfigs.isEmpty()) {
            list = new ArrayList<LbConfig>();

            for (int i=0; i<lbConfigs.size(); i++) {

                // target is a cluster
                if (tgt.isCluster(target)) {
                    ClusterRef  cRef = lbConfigs.get(i).getRefByRef(ClusterRef.class, target);

                    // this lb config has a reference to the target cluster
                    if (cRef != null) {
                        list.add(lbConfigs.get(i));
                    }

                // target is a server
                } else if (domain.isServer(target)) {
                    ServerRef sRef = lbConfigs.get(i).getRefByRef(ServerRef.class, target);

                    // this lb config has a reference to the target server
                    if (sRef != null) {
                        list.add(lbConfigs.get(i));
                    }
                }
            }
        }
        return list;
    }
}
