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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.audit.AdminAuditConfiguration;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
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
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;

/**
 * Gets the current configuration of the admin audit service
 * @author jonathan coustick
 * @since 5.192
 */
@Service(name = "get-admin-audit-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG,
    CommandTarget.DEPLOYMENT_GROUP})
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
    ServiceLocator serviceLocator;
    
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;
    private Config targetConfig;
    
    @Inject
    private Target targetUtil;
    
    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport actionReport = context.getActionReport();
        
        ColumnFormatter columnFormatter = new ColumnFormatter(ATTRIBUTE_HEADERS);
        ColumnFormatter notifiersColumnFormatter = new ColumnFormatter(NOTIFIER_HEADERS);   
        
        targetConfig = targetUtil.getConfig(target);
        AdminAuditConfiguration config = targetConfig.getExtensionByType(AdminAuditConfiguration.class);
        
        Object[] configValues = {config.getEnabled(), config.getAuditLevel()};
        columnFormatter.addRow(configValues);
        
        Map<String, Object> map = new HashMap<>();
        Properties extraProperties = new Properties();
        map.put("enabled", config.getEnabled());
        map.put("auditLevel", config.getAuditLevel());
        extraProperties.put("adminauditConfiguration", map);
        
        ActionReport notifiersReport = actionReport.addSubActionsReport();
        
        List<ServiceHandle<BaseNotifierService>> allNotifierServiceHandles = serviceLocator.getAllServiceHandles(BaseNotifierService.class);
        
        Properties notifierProps = new Properties();
        if (!config.getNotifierList().isEmpty()) {
            List<Class<Notifier>> notifierClassList = config.getNotifierList().stream().map((input) -> {
                return resolveNotifierClass(input);
            }).collect(Collectors.toList());

            for (ServiceHandle<BaseNotifierService> serviceHandle : allNotifierServiceHandles) {
                Notifier notifier = config.getNotifierByType(serviceHandle.getService().getNotifierType());
                if (notifier != null) {
                    ConfigView view = ConfigSupport.getImpl(notifier);
                    NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);

                    if (notifierClassList.contains(view.<Notifier>getProxyType())) {
                        Object values[] = new Object[2];
                        values[0] = annotation.type();
                        values[1] = notifier.getEnabled();
                        notifiersColumnFormatter.addRow(values);

                        Map<String, Object> mapNotifiers = new HashMap<>(2);
                        mapNotifiers.put("notifierName", values[0]);
                        mapNotifiers.put("notifierEnabled", values[1]);

                        notifierProps.put("notifierList" + annotation.type(), mapNotifiers);
                    }
                }

            }
        }
        notifiersReport.setMessage(notifiersColumnFormatter.toString());
        extraProperties.put("notifiers", notifierProps);
        
        
        actionReport.setExtraProperties(extraProperties);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
    private Class<Notifier> resolveNotifierClass(Notifier input) {
        ConfigView view = ConfigSupport.getImpl(input);
        return view.getProxyType();
    }
    
}
