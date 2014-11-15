/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.hazelcast.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
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

@Service(name = "get-hazelcast-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.hazelcast.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-hazelcast-configuration",
            description = "List Hazelcast Configuration")
})
public class GetHazelcastConfiguration implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    private String target;

    @Override
    public void execute(AdminCommandContext context) {
        
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        HazelcastRuntimeConfiguration runtimeConfiguration = config.getExtensionByType(HazelcastRuntimeConfiguration.class);
        final ActionReport actionReport = context.getActionReport();
        String headers[] = {"Configuration File","Enabled","Start Port","MulticastGroup","MulticastPort","JNDIName"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[6];
        values[0] = runtimeConfiguration.getHazelcastConfigurationFile();
        values[1] = runtimeConfiguration.getEnabled();
        values[2] = runtimeConfiguration.getStartPort();
        values[3] = runtimeConfiguration.getMulticastGroup();
        values[4] = runtimeConfiguration.getMulticastPort();
        values[5] = runtimeConfiguration.getJNDIName();
        columnFormatter.addRow(values);
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
