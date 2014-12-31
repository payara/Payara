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
import fish.payara.nucleus.hazelcast.HazelcastCore;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;


/**
 * Internal Admin command to enable or disable Hazelcast on demand
 * Used by the set-hazelcast-configuration command to dynamically boot or shutdown hazelcast
 * @author steve
 */
@Service(name = "__enable-hazelcast-internal")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("__enable-hazelcast-internal")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "__enable-hazelcast-internal",
            description = "Enables Hazelcast")
})
public class EnableHazelcastInternal implements AdminCommand {
    
    @Inject
    HazelcastCore hazelcast;   
    
    @Param(name = "enabled", optional = false)
    private Boolean enabled;
    
    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }
        
        hazelcast.setEnabled(enabled);
        actionReport.appendMessage("Hazelcast status set to " + enabled + " on " + target);
    }
    
}
