/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.audit.admin;

import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.audit.AdminAuditConfiguration;
import fish.payara.nucleus.notification.configuration.Notifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
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
 * Gets the current configuration of the admin audit service
 * @author jonathan coustick
 * @since 5.192
 */
@Service(name = "get-admin-audit-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = AdminAuditConfiguration.class, 
            opType = RestEndpoint.OpType.GET,
            path = "get-admin-audit-configuration",
            description = "Gets the current configuration settings of the admin audit Service")
})
public class GetAdminAuditServiceConfiguration implements AdminCommand {
    
    private final static String[] ATTRIBUTE_HEADERS = {"Enabled", "Audit Level"};
    private final static String[] NOTIFIER_HEADERS= {"Name", "Notifier Enabled"};
    
    @Inject
    private AdminAuditConfiguration config;
    
    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport actionReport = context.getActionReport();
        
        ColumnFormatter columnFormatter = new ColumnFormatter(ATTRIBUTE_HEADERS);
        ColumnFormatter notifiersColumnFormatter = new ColumnFormatter(NOTIFIER_HEADERS);        
        
        Object[] values = {config.getEnabled(), config.getAuditLevel()};
        columnFormatter.addRow(values);
        
        Map<String, Object> map = new HashMap<>();
        Properties extraProperties = new Properties();
        map.put("enabled", config.getEnabled());
        map.put("auditLevel", config.getAuditLevel());
        extraProperties.put("adminauditConfiguration", map);
        
        ActionReport notifiersReport = actionReport.addSubActionsReport();
        
        Properties notifierProps = new Properties();
        for (Notifier notifier: config.getNotifierList()) {
            Object[] notifierValues = { notifier.toString(), notifier.getEnabled()};
            notifiersColumnFormatter.addRow(notifierValues);
            notifierProps.put(notifier.toString(), notifier.getEnabled());
        }
        notifiersReport.setMessage(notifiersColumnFormatter.toString());
        extraProperties.put("notifiers", notifierProps);
        
        
        actionReport.setExtraProperties(extraProperties);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
}
