/*
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.asadmin.recorder.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.asadmin.recorder.AsadminRecorderConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.inject.Inject;
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
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "get-asadmin-recorder-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.asadmin.recorder.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class, 
            opType = RestEndpoint.OpType.GET,
            path = "get-asadmin-recorder-configuration",
            description = "Gets the current configuration settings of the Asadmin Recorder Service")
})
public class GetAsadminRecorderConfiguration implements AdminCommand
{
    @Inject
    private Target targetUtil;
    
    private final String target = "server";
    private final String[] headers = {"Enabled", "Filter Commands", "Output Location", "Filtered Commands", 
        "Prepend Enabled", "Prepended Options"};
            
    @Override
    public void execute(AdminCommandContext context)
    {
        Config config = targetUtil.getConfig(target);
        if (config == null) 
        {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        AsadminRecorderConfiguration asadminRecorderConfiguration = 
                config.getExtensionByType(AsadminRecorderConfiguration.class);
        final ActionReport actionReport = context.getActionReport();
        
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = {asadminRecorderConfiguration.isEnabled(), 
                asadminRecorderConfiguration.filterCommands(), 
                asadminRecorderConfiguration.getOutputLocation(),
                asadminRecorderConfiguration.getFilteredCommands(),
                asadminRecorderConfiguration.prependEnabled(),
                asadminRecorderConfiguration.getPrependedOptions()
        };
        
        columnFormatter.addRow(values);
        
        Map<String, Object> map = new HashMap<String, Object>();
        Properties extraProps = new Properties();
        map.put("enabled", values[0]);
        map.put("filterCommands", values[1]);
        map.put("outputLocation", values[2]);
        map.put("filteredCommands", values[3]);
        map.put("prependEnabled", values[4]);
        map.put("prependedOptions", values[5]);
        extraProps.put("getAsadminRecorderConfiguration",map);
        
        actionReport.setExtraProperties(extraProps);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }  
}
