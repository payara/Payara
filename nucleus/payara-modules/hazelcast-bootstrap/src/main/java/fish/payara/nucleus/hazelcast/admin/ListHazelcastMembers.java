/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
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
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

@Service(name = "list-hazelcast-members")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list-hazelcast-members")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "list-hazelcast-members",
            description = "Lists Hazelcast cluster members")
})
/**
 * Lists the Hazelcast Members in the cluster
 *
 * @author steve
 */
public class ListHazelcastMembers implements AdminCommand {

    @Inject
    HazelcastCore hazelcast;

    @Inject
    protected Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport actionReport = context.getActionReport();

        if (hazelcast.isEnabled()) {
            HazelcastInstance instance = hazelcast.getInstance();
            if (instance != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("{ ");
                for (Member member : instance.getCluster().getMembers()) {
                    String memberName = member.getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
                    if (memberName != null) {
                        builder.append(memberName).append("-");
                    }
                    builder.append(member.getSocketAddress().toString());
                    if (member.localMember()) {
                        builder.append("-this");
                    }
                    
                    if (member.isLiteMember()) {
                        builder.append("-LITE");
                    }
                    builder.append(" ");
                }
                builder.append('}');
                actionReport.setMessage(builder.toString());
                
                // build extra message
                Properties extraProps = new Properties();
                StringBuilder extraBuilder = new StringBuilder(actionReport.getMessage());
                extraBuilder.append("<br/>");
                for (ActionReport subReport : actionReport.getSubActionsReport()) {
                    extraBuilder.append(subReport.getMessage()).append("<br/>");
                }
                extraProps.put("Members", extraBuilder.toString());
                actionReport.setExtraProperties(extraProps);
            }
        } else {
            Properties extraProps = new Properties();
            extraProps.put("Members", "Hazelcast is not enabled");
            actionReport.setExtraProperties(extraProps);
            actionReport.setMessage("Hazelcast is not enabled");
        }
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
