/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.micro.services.command;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.appserver.micro.services.PayaraMicroInstance;
import fish.payara.appserver.micro.services.data.ApplicationDescriptor;
import fish.payara.appserver.micro.services.data.InstanceDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
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
 *
 * @author Andrew Pielage
 */
@Service(name = "list-payara-micro-instances")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.payara.micro.instances")
@ExecuteOn(value = RuntimeType.DAS)
@TargetType(value = CommandTarget.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "list-payara-micro-instances",
            description = "List Payara Micro Instances")
})
public class ListPayaraMicroInstancesCommand implements AdminCommand
{
    @Inject
    PayaraMicroInstance payaraMicro;
    
    @Override
    public void execute(AdminCommandContext context)
    {
        final ActionReport actionReport = context.getActionReport();
        
        // Update the instance descriptor to check if the DAS is in the Hazelcast cluster or not
        if (payaraMicro.isClustered())
        {
            Set<InstanceDescriptor> instances = payaraMicro.getClusteredPayaras();
        
            String[] headers = {"Instance Name", "Host Name", "HTTP Port", 
                "HTTPS Port", "Lite Member", "Deployed Applications"};
            ColumnFormatter columnFormatter = new ColumnFormatter(headers);

            List members = new ArrayList();
            Properties extraProps = new Properties();

            for (InstanceDescriptor instance : instances) {
                // We only want to take note of the Micro instances
                if (instance.isMicroInstance()) {
                    Object values[] = new Object[6];
                    values[0] = instance.getInstanceName();
                    values[1] = instance.getHostName();
                    values[2] = instance.getHttpPort();
                    if (instance.getHttpsPort() == 0) {
                        values[3] = "Disabled";
                    } else {
                        values[3] = instance.getHttpsPort();
                    }
                    values[4] = instance.isLiteMember();

                    List<String> applications = new ArrayList<>();
                    Collection<ApplicationDescriptor> applicationDescriptors = instance.getDeployedApplications();
                    if (applicationDescriptors != null) {
                        for (ApplicationDescriptor application : applicationDescriptors) {
                            applications.add(application.getName());
                        }
                        values[5] = Arrays.toString(applications.toArray());
                    } else {
                        values[5] = "";
                    }

                    columnFormatter.addRow(values);

                    Map<String, Object> map = new HashMap<>(6);

                    map.put("InstanceName", values[0]);
                    map.put("HostName", values[1]);
                    map.put("HttpPort", values[2]);
                    map.put("HttpsPort", values[3]);
                    map.put("LiteMember", values[4]);
                    map.put("Applications", values[5]);

                    members.add(map);
                }             
            }

            extraProps.put("Members", members);
            actionReport.setExtraProperties(extraProps);
            actionReport.setMessage(columnFormatter.toString());
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } else {
            Properties extraProps = new Properties();
            extraProps.put("Members", "Hazelcast is not enabled");
            actionReport.setExtraProperties(extraProps);
            actionReport.setMessage("Hazelcast is not enabled");
        }
    }  
}
