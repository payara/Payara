/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.hazelcast.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.TargetBasedExecutor;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author steve
 */
@Service(name = "restart-hazelcast")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("restart-hazelcast")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "restart-hazelcast",
            description = "Restart Hazelcast")
})
public class RestartHazelcast implements AdminCommand {
    
    @Inject
    HazelcastCore hazelcast;    
    
    @Inject
    TargetBasedExecutor executor;
    
    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Inject
    private ServerEnvironment serverEnv;
    
    @Inject
    Target targetUtil;

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        // what is wrong is that this does not work for the local server as it will never match
        boolean forMe = isThisForMe();
        if (forMe && hazelcast.isEnabled()) {
            hazelcast.setEnabled(false);
            hazelcast.setEnabled(true);
            actionReport.setMessage("Hazelcast Restarted");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } else if (forMe && !hazelcast.isEnabled()) {
            actionReport.setMessage("Hazelcast not enabled. Restart ignored");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } else {
            actionReport.setMessage("Restart Ignored");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }

    }
    
    private boolean isThisForMe() {
        boolean result = false;
        String instanceName = serverEnv.getInstanceName();
        
        if (instanceName.equals(target)) {
            result = true;
        }
        
        if(targetUtil.isCluster(target)) {
            List<Server> servers = targetUtil.getInstances(target);
            for (Server server : servers) {
                if (server.getName().equals(instanceName)) {
                    result = true;
                    break;
                }
            }
            
        }
        return result;
    }
    
}
