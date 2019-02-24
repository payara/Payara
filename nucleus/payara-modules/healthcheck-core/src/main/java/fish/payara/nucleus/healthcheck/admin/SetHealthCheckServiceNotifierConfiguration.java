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
import java.lang.reflect.Proxy;
import java.util.List;
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

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.configuration.NotifierType;

/**
 * Admin command to set enabled and noisy flags for a specific {@link Notifier} configuration as part of the
 * {@link HealthCheckServiceConfiguration}.
 * 
 * This should not be confused with the {@link NotifierConfiguration} itself for that specific {@link Notifier} that
 * exists as part of {@link NotificationServiceConfiguration}.
 * 
 * When {@link #noisy} parameter is not specified the according {@link NotifierConfiguration} is loaded from the
 * {@link NotificationServiceConfiguration} to use its {@link NotifierConfiguration#getNoisy()} as automatic fallback
 * setting.
 *
 * @author Jan Bernitt
 */
@Service(name = "set-healthcheck-service-notifier-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
                opType = RestEndpoint.OpType.POST,
                path = "set-healthcheck-service-notifier-configuration",
                description = "Sets the Configuration for specific Notifier for the HealthCheck Service")
})
public class SetHealthCheckServiceNotifierConfiguration implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Inject
    private ServerEnvironment server;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Inject
    private Logger logger;

    @Inject
    private HealthCheckService healthCheckService;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;
    private Config targetConfig;

    @Param(name = "enabled")
    private boolean enabled;

    @Param(name = "noisy", optional = true)
    private Boolean noisy;

    @Param(name = "notifier")
    private String notifierName;
    private NotifierType notifierType;

    private ActionReport report;

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        Properties extraProperties = report.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            report.setExtraProperties(extraProperties);
        }
        try {
            notifierType = NotifierType.valueOf(notifierName.toUpperCase());
        } catch (IllegalArgumentException e) {
            StringBuilder values = new StringBuilder();
            for (NotifierType type : NotifierType.values()) {
                if (values.length() > 0) {
                    values.append(',');
                }
                values.append(type.name().toLowerCase());
            }
            report.setMessage("Unknown notifier `" + notifierName + "`. Known are: " + values);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        targetConfig = targetUtil.getConfig(target);

        final HealthCheckServiceConfiguration config = targetConfig.getExtensionByType(HealthCheckServiceConfiguration.class);
        final Notifier notifier = selectByType(Notifier.class, config.getNotifierList());
        try {
            if (notifier == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(HealthCheckServiceConfiguration configProxy)
                            throws PropertyVetoException, TransactionFailure {
                                @SuppressWarnings("unchecked")
                                Notifier newNotifier = (Notifier)configProxy.createChild(selectByType(Class.class,
                                        configModularityUtils.getInstalledExtensions(Notifier.class)));
                                configProxy.getNotifierList().add(newNotifier);
                                applyValues(newNotifier);
                                return configProxy;
                            }
                }, config);
            } else {
                ConfigSupport.apply(new SingleConfigCode<Notifier>() {
                    @Override
                    public Object run(Notifier notifierProxy) throws PropertyVetoException, TransactionFailure {
                        applyValues(notifierProxy);
                        return notifierProxy;
                    }
                }, notifier);
            }
            if (dynamic && (!server.isDas() || targetUtil.getConfig(target).isDas())) {
                healthCheckService.reboot();
            }
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            report.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    void applyValues(Notifier notifier) throws PropertyVetoException {
        if (Boolean.parseBoolean(notifier.getEnabled()) != enabled) {
            report.appendMessage(notifierName + ".enabled was " + notifier.getEnabled() + " set to " + enabled + "\n");
            notifier.enabled(enabled);
        }
        if (this.noisy == null) {
            // inherit setting from the notifier's configuration
            NotifierConfiguration config = selectByType(NotifierConfiguration.class, targetConfig
                    .getExtensionByType(NotificationServiceConfiguration.class).getNotifierConfigurationList());
            if (config != null) {
                noisy = "true".equalsIgnoreCase("" + config.getNoisy());
            }
        }
        if (noisy != null && Boolean.parseBoolean(notifier.getNoisy()) != noisy.booleanValue()) {
            report.appendMessage(notifierName + ".noisy was " + notifier.getNoisy() + " set to " + noisy + "\n");
            notifier.noisy(noisy);
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    <T> T selectByType(Class<? super T> commonInterface, List<T> candidates) {
        for (T candidate : candidates) {
            Class<?> annotatedType = candidate instanceof Class ? (Class<?>) candidate : candidate.getClass();
            if (Proxy.isProxyClass(annotatedType)) {
                for (Class<?> i : annotatedType.getInterfaces()) {
                    if (commonInterface.isAssignableFrom(i)) {
                        annotatedType = i;
                    }
                }
            }
            NotifierConfigurationType type = annotatedType.getAnnotation(NotifierConfigurationType.class);
            if (type != null && type.type() == notifierType) {
                return candidate;
            }
        }
        return null;
    }
}
