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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
import java.util.HashMap;
import java.util.Map;
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
        
        Map<String, Object> map = new HashMap<String,Object>();
        Properties extraProps = new Properties();
        map.put("hazelcastConfigurationFile", values[0]);
        map.put("enabled", values[1]);
        map.put("startPort", values[2]);
        map.put("multicastGroup", values[3]);
        map.put("multicastPort", values[4]);
        map.put("jndiName", values[5]);
        extraProps.put("getHazelcastConfiguration",map);
                
        actionReport.setExtraProperties(extraProps);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
