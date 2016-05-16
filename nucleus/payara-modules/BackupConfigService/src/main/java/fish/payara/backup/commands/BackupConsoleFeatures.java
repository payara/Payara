/**<!--
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
 -->*/
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.backup.service.BackupConfigConfiguration;
import java.beans.PropertyVetoException;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Daniel
 */
@Service(name="get-console-features")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.features")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value={CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class ,
        opType = RestEndpoint.OpType.GET,
        path = "get-console-features",
        description = "Gets the stuff")
    
})

public class BackupConsoleFeatures implements AdminCommand{
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
        
        BackupConfigConfiguration runtimeConfiguration = config.getExtensionByType(BackupConfigConfiguration.class);
        final ActionReport actionReport = context.getActionReport();
        String headers[] = {"minutes", "param2"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[2];
        values[0] = runtimeConfiguration.getMinutes();
        values[1] = runtimeConfiguration.getparam2();
        
        columnFormatter.addRow(values);
        
        Map<String, Object> map = new HashMap<String,Object>(2);
        Properties extraProps = new Properties();
        map.put("param1", values[0]);
        map.put("param2", values[1]);
        
        extraProps.put("helloes",map);
                
        actionReport.setExtraProperties(extraProps);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    
}
