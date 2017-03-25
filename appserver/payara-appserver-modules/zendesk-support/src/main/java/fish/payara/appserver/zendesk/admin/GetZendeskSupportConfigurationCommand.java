/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.appserver.zendesk.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.appserver.zendesk.config.ZendeskSupportConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "get-zendesk-support-configuration")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = ZendeskSupportConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-zendesk-support-configuration",
            description = "Gets the Zendesk Support Configuration")
})
public class GetZendeskSupportConfigurationCommand implements AdminCommand {

    @Inject
    private Target targetUtil;
    private final String config = "server-config";
    
    @Override
    public void execute(AdminCommandContext acc) {
        Config configNode = targetUtil.getConfig(config);
        ZendeskSupportConfiguration zendeskSupportConfiguration = 
                configNode.getExtensionByType(ZendeskSupportConfiguration.class);
        
        ActionReport actionReport = acc.getActionReport();
   
        final String[] outputHeaders = {"Email Address"};
        ColumnFormatter columnFormatter = new ColumnFormatter(outputHeaders);
        Object[] outputValues = {zendeskSupportConfiguration.getEmailAddress()};
        columnFormatter.addRow(outputValues);
        actionReport.appendMessage(columnFormatter.toString());
        
        Map<String, Object> extraPropsMap = new HashMap<>();
        extraPropsMap.put("emailAddress", zendeskSupportConfiguration.getEmailAddress());
        
        Properties extraProps = new Properties();
        extraProps.put("zendeskSupportConfiguration", extraPropsMap);
        
        actionReport.setExtraProperties(extraProps);
    }
}
