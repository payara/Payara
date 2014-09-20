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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import java.beans.PropertyVetoException;


import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.ClusterRef;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Cluster;
import org.glassfish.loadbalancer.config.LbConfigs;
import org.glassfish.loadbalancer.config.LbConfig;
import org.glassfish.loadbalancer.config.LoadBalancer;
import org.glassfish.loadbalancer.config.LoadBalancers;
import com.sun.enterprise.config.serverbeans.Applications;

import org.glassfish.api.admin.*;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;

import javax.inject.Inject;

/**
 *
 * This is a remote command that supports the create-http-lb-ref CLI command.
 * It creates a server-ref|cluster-ref, health-checker by using the given
 * parameters.
 * lbname: the name of the load-balancer element that exists
 * config: the name of the lb-config element that exists
 * target: cluster-ref or server-ref parameter of lb-config *
 * healthcheckerurl: url attribute of health-checker
 * healthcheckerinterval: interval-in-seconds parameter of health-checker
 * healthcheckertimeout: timeout-in-seconds parameter of health-checker
 * @author Yamini K B
 */
@Service(name = "create-http-lb-ref")
@PerLookup
@I18n("create.http.lb.ref")
@TargetType(value={CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=LbConfig.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-http-lb-ref", 
        description="create-http-lb-ref",
        params={
            @RestParam(name="config", value="$parent")
        })
})
public final class CreateHTTPLBRefCommand extends LBCommandsBase
        implements AdminCommand {

    @Param(optional=true)
    String config;

    @Param(optional=true)
    String lbname;

    @Param(optional=true)
    String lbpolicy;

    @Param(optional=true)
    String lbpolicymodule;

    @Param(optional=true, defaultValue="/")
    String healthcheckerurl;

    @Param(optional=true, defaultValue="30")
    String healthcheckerinterval;

    @Param(optional=true, defaultValue="10")
    String healthcheckertimeout;

    @Param(optional=true)
    String lbenableallapplications;

    @Param(optional=true)
    String lbenableallinstances;

    @Param(optional=true)
    String lbweight;

    @Param(primary=true)
    String target;

    @Inject
    Target tgt;

    @Inject
    Logger logger;

    @Inject
    CommandRunner runner;

    @Inject
    Applications applications;

    private ActionReport report;

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateHTTPLBRefCommand.class);
    
    @Override
    public void execute(AdminCommandContext context) {

        report = context.getActionReport();

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        boolean isCluster = (target!=null) ? tgt.isCluster(target): false;

        if (config!=null && lbname!=null) {
            String msg = localStrings.getLocalString("EitherConfigOrLBName",
                    "Either LB name or LB config name, not both");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if (config==null && lbname==null) {
            String msg = localStrings.getLocalString("SpecifyConfigOrLBName",
                    "Please specify either LB name or LB config name.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        LbConfigs lbconfigs = domain.getExtensionByType(LbConfigs.class);
        if (lbconfigs == null) {
            String msg = localStrings.getLocalString("NoLbConfigsElement",
                    "Empty lb-configs");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if (config != null) {
            if (lbconfigs.getLbConfig(config) == null) {
                String msg = localStrings.getLocalString("LbConfigDoesNotExist",
                        "Specified LB config {0} does not exist", config);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        } else if (lbname != null) {
            LoadBalancers lbs = domain.getExtensionByType(LoadBalancers.class);
            if (lbs == null) {
                String msg = localStrings.getLocalString("NoLoadBalancersElement",
                        "No Load balancers defined.");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            LoadBalancer lb = lbs.getLoadBalancer(lbname);
            if (lb == null) {
                String msg = localStrings.getLocalString("LoadBalancerNotDefined",
                        "Load balancer [{0}] not found.", lbname);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            config = lb.getLbConfigName();
        }

        if((lbpolicy != null) || (lbpolicymodule != null)) {
            if (!isCluster) {
                String msg = localStrings.getLocalString("NotCluster",
                        "{0} not a cluster", target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        }
        
        Cluster c = null;
        Server s = null;
        if (isCluster) {
            c = domain.getClusterNamed(target);
            if (c == null ) {
                String msg = localStrings.getLocalString("ClusterNotDefined",
                        "Cluster {0} cannot be used as target", target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        } else {
            s = domain.getServerNamed(target);
            if (s == null ) {
                String msg = localStrings.getLocalString("ServerNotDefined",
                        "Server {0} cannot be used as target", target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        }

        // create lb ref
        createLBRef(lbconfigs, target, config);
        if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
            return;
        }
        
        if(healthcheckerurl != null ){
            try {
                final CreateHTTPHealthCheckerCommand command =
                        (CreateHTTPHealthCheckerCommand) runner
                        .getCommand("create-http-health-checker", report, context.getLogger());
                command.url = healthcheckerurl;
                command.interval=healthcheckerinterval;
                command.timeout=healthcheckertimeout;
                command.config=config;
                command.target=target;
                command.execute(context);
                checkCommandStatus(context);
            } catch (CommandException e) {
                String msg = e.getLocalizedMessage();
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        }
        
        if(Boolean.parseBoolean(lbenableallinstances)) {
            try {
                final EnableHTTPLBServerCommand command = (EnableHTTPLBServerCommand)runner
                        .getCommand("enable-http-lb-server", report, context.getLogger());
                command.target = target;
                command.execute(context);
                checkCommandStatus(context);
            } catch (CommandException e) {
                String msg = e.getLocalizedMessage();
                logger.warning(msg);
//                    report.setActionExitCode(ExitCode.FAILURE);
//                    report.setMessage(msg);
//                    return;
            }

        }
        if(Boolean.parseBoolean(lbenableallapplications)) {
            List<ApplicationRef> appRefs = domain.getApplicationRefsInTarget(target);

            if ((appRefs.size() > 0) && Boolean.parseBoolean(lbenableallapplications)) {
                for(ApplicationRef ref:appRefs) {
                    //enable only user applications
                    if(isUserApp(ref.getRef())) {
                        enableApp(context, ref.getRef());
                    }
                }
            }            
        }
    }

    public void createLBRef(LbConfigs lbconfigs, String target, String configName) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[LB-ADMIN] createLBRef called for target " + target);
        }

        // target is a cluster
        if (tgt.isCluster(target)) {
            addClusterToLbConfig(lbconfigs, configName, target);
            logger.info(localStrings.getLocalString("http_lb_admin.AddClusterToConfig",
                    "Added cluster {0} to load balancer {1}", target, configName));


        // target is a server
        } else if (domain.isServer(target)) {
            addServerToLBConfig(lbconfigs, configName, target);
            logger.info(localStrings.getLocalString("http_lb_admin.AddServerToConfig",
                    "Added server {0} to load balancer {1}", target, configName));

        } else {
            String msg = localStrings.getLocalString("InvalidTarget", "Invalid target", target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
    }

    private void addServerToLBConfig(final LbConfigs lbconfigs, final String configName, final String serverName) {
        LbConfig lbConfig = lbconfigs.getLbConfig(configName);

        ServerRef sRef = lbConfig.getRefByRef(ServerRef.class, serverName);
        if (sRef != null) {
            String msg = localStrings.getLocalString("LBServerRefExists",
                   "LB config already contains a server-ref for target {0}", target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        Server server = domain.getServerNamed(serverName);
        boolean isStandAlone = server.getCluster() == null && server.isInstance();
        if (!isStandAlone) {
            String msg = localStrings.getLocalString("NotStandAloneInstance",
                    "[{0}] is not a stand alone instance. Only stand alone instance can be added to a load balancer.",
                    serverName);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<LbConfig>() {
                @Override
                public Object run(LbConfig param) throws PropertyVetoException, TransactionFailure {
                    ServerRef ref = param.createChild(ServerRef.class);
                    ref.setRef(serverName);
                    param.getClusterRefOrServerRef().add(ref);
                    return Boolean.TRUE;
                }
            }, lbConfig);
        } catch (TransactionFailure ex) {
            String msg = localStrings.getLocalString("FailedToAddServerRef",
                    "Failed to add server-ref");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            report.setFailureCause(ex);
        }
    }

    private void addClusterToLbConfig(final LbConfigs lbconfigs, final String configName, final String clusterName) {
        LbConfig lbConfig = lbconfigs.getLbConfig(configName);

        ClusterRef cRef = lbConfig.getRefByRef(ClusterRef.class, clusterName);
        if (cRef != null) {
            String msg = localStrings.getLocalString("LBClusterRefExists",
                   "LB config already contains a cluster-ref for target {0}", target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<LbConfig>() {
                @Override
                public Object run(LbConfig param) throws PropertyVetoException, TransactionFailure {
                    ClusterRef ref = param.createChild(ClusterRef.class);
                    ref.setRef(clusterName);
                    if(lbpolicy != null) {
                        ref.setLbPolicy(lbpolicy);
                    }
                    if(lbpolicymodule != null) {
                        ref.setLbPolicyModule(lbpolicymodule);
                    }
                    param.getClusterRefOrServerRef().add(ref);
                    return Boolean.TRUE;
                }
            }, lbConfig);
        } catch (TransactionFailure ex) {
            String msg = localStrings.getLocalString("FailedToAddClusterRef",
                    "Failed to add cluster-ref");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            report.setFailureCause(ex);
        }
    }

    private void enableApp(AdminCommandContext context, String appName) {
        try {
            final EnableHTTPLBApplicationCommand command =
                    (EnableHTTPLBApplicationCommand) runner
                    .getCommand("enable-http-lb-application", report, context.getLogger());
            command.target = target;
            command.name=appName;
            command.execute(context);
            checkCommandStatus(context);
        } catch (CommandException e) {
            String msg = e.getLocalizedMessage();
            logger.warning(msg);
//            report.setActionExitCode(ExitCode.FAILURE);
//            report.setMessage(msg);
//            return;
        }
    }

    private boolean isUserApp(String id) {
        if(applications.getApplication(id).getObjectType().equals("user")) {
            return true;
        }
        return false;
    }
}
