/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.glassfish.api.admin.CommandRunner;
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
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
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
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (hazelcast.isEnabled()) {
            HazelcastInstance instance = hazelcast.getInstance();
            if (instance != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("{ ");
                for (Member member : instance.getCluster().getMembers()) {
                    String memberName = member.getStringAttribute(hazelcast.INSTANCE_ATTRIBUTE);
                    if (memberName != null) {
                        builder.append(memberName).append("-");
                    }
                    builder.append(member.getSocketAddress().toString());
                    if (member.localMember()) {
                        builder.append("-this");
                    }
                    builder.append(" ");
                }
                builder.append('}');
                actionReport.setMessage(builder.toString());
            }
        } else {
            actionReport.setMessage("Hazelcast is not enabled");
        }
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
