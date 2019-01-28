/*
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.admin;

import java.beans.PropertyVetoException;
import java.util.EnumMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.notification.configuration.CDIEventbusNotifier;
import fish.payara.nucleus.notification.configuration.DatadogNotifier;
import fish.payara.nucleus.notification.configuration.EmailNotifier;
import fish.payara.nucleus.notification.configuration.EventbusNotifier;
import fish.payara.nucleus.notification.configuration.HipchatNotifier;
import fish.payara.nucleus.notification.configuration.JmsNotifier;
import fish.payara.nucleus.notification.configuration.NewRelicNotifier;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.configuration.SlackNotifier;
import fish.payara.nucleus.notification.configuration.SnmpNotifier;
import fish.payara.nucleus.notification.configuration.XmppNotifier;
import fish.payara.nucleus.notification.log.LogNotifier;

@Service(name = "set-healthcheck-service-notifier-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
                opType = RestEndpoint.OpType.POST,
                path = "set-healthcheck-service-notifier-configuration",
                description = "Sets Configuration for targetd Notifier of the HealthCheck Service")
})
public class SetHealthCheckServiceNotifierConfiguration implements AdminCommand {

    private static final EnumMap<NotifierType, Class<? extends Notifier>> NOTFIER_CLASS_BY_TYPE = new EnumMap<>(NotifierType.class);

    static {
        mapType(CDIEventbusNotifier.class);
        mapType(DatadogNotifier.class);
        mapType(EmailNotifier.class);
        mapType(EventbusNotifier.class);
        mapType(HipchatNotifier.class);
        mapType(JmsNotifier.class);
        mapType(LogNotifier.class);
        mapType(NewRelicNotifier.class);
        mapType(SlackNotifier.class);
        mapType(SnmpNotifier.class);
        mapType(XmppNotifier.class);
    }

    private static void mapType(Class<? extends Notifier> type) {
        NotifierConfigurationType config = type.getAnnotation(NotifierConfigurationType.class);
        if (config != null) {
            NOTFIER_CLASS_BY_TYPE.put(config.type(), type);
        }
    }

    @Inject
    private Target targetUtil;

    @Inject
    private ServerEnvironment server;

    @Inject
    private Logger logger;

    @Inject
    private HealthCheckService healthCheckService;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(name = "enabled")
    private Boolean enabled;

    @Param(name = "noisy", optional = true)
    private Boolean noisy;

    @Param(name = "notifier", optional = true)
    private String notifierType;

    private ActionReport actionReport;

    @Override
    public void execute(AdminCommandContext context) {
        actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config configuration = targetUtil.getConfig(target);
        final HealthCheckServiceConfiguration config = configuration.getExtensionByType(HealthCheckServiceConfiguration.class);
        Class<? extends Notifier> notifierClass = NOTFIER_CLASS_BY_TYPE.get(NotifierType.valueOf(notifierType.toUpperCase()));

        Notifier notifier = config.getNotifierByType(notifierClass);

        try {
            if (notifier == null) {
                ConfigSupport.apply((SingleConfigCode<HealthCheckServiceConfiguration>) proxy -> {
                  Notifier newNotifier = proxy.createChild(notifierClass);
                  proxy.getNotifierList().add(newNotifier);
                  applyValues(newNotifier);
                  return proxy;
               }, config);
            }
            else {
                ConfigSupport.apply(proxy -> {
                    applyValues(proxy);
                    return proxy;
                }, notifier);
            }

            if (dynamic && (!server.isDas() || targetUtil.getConfig(target).isDas())) {
                healthCheckService.reboot();
            }
        }
        catch(TransactionFailure ex){
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    private void applyValues(Notifier notifier) throws PropertyVetoException {
        if(this.enabled != null) {
            notifier.enabled(enabled);
        }
        if (this.noisy != null) {
            notifier.noisy(noisy);
        }
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
