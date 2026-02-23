/*
 * Copyright (c) 2019-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.admin;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;

import com.sun.enterprise.config.serverbeans.Config;

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
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.TimeUtil;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;

/**
 * Service to configure the {@link HealthCheckServiceConfiguration} of the {@link Target}.
 *
 * Updating the service configuration also updates the {@link fish.payara.nucleus.notification.log.LogNotifierConfiguration} accordingly.
 *
 * @author Jan Bernitt
 * @since 5.191
 */
@Service(name = "set-healthcheck-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-healthcheck-configuration",
            description = "Enables/Disables Health Check Service")
})
public class SetHealthCheckConfiguration implements AdminCommand {

    @Inject
    ServerEnvironment server;

    @Inject
    private Logger logger;

    @Inject
    HealthCheckService healthCheck;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    private Target targetUtil;
    private Config targetConfig;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    private String target;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "historical-trace-enabled", optional = true)
    private Boolean historicalTraceEnabled;

    @Param(name = "historical-trace-store-size", optional = true, defaultValue = "20")
    @Min(value = 1, message = "Store size must be greater than 0")
    private int historicalTraceStoreSize;

    @Param(name = "historical-trace-store-timeout", optional = true)
    private String historicalTraceStoreTimeout;

    @Param(name = "enableNotifiers", alias = "enable-notifiers", optional = true)
    private List<String> enableNotifiers;

    @Param(name = "disableNotifiers", alias = "disable-notifiers", optional = true)
    private List<String> disableNotifiers;

    @Param(name = "setNotifiers", alias = "set-notifiers", optional = true)
    private List<String> setNotifiers;

    @Override
    public void execute(AdminCommandContext context) {
        targetConfig = targetUtil.getConfig(target);
        HealthCheckServiceConfiguration config = targetConfig.getExtensionByType(HealthCheckServiceConfiguration.class);
        if (config != null) {
            updateConfig(config, context);
        }
        if (dynamic && (!server.isDas() || targetConfig.isDas())) {
            configureDynamically();
        }
    }

    private static ActionReport initActionReport(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        if (report.getExtraProperties() == null) {
            report.setExtraProperties(new Properties());
        }
        return report;
    }

    private void updateConfig(HealthCheckServiceConfiguration config, AdminCommandContext context) {
        final ActionReport report = initActionReport(context);
        try {
            final Set<String> notifierNames = NotifierUtils.getNotifierNames(serviceLocator);
            ConfigSupport.apply(configProxy -> {
                    if (enabled != null) {
                        configProxy.enabled(enabled.toString());
                    }

                    configProxy.setHistoricalTraceStoreSize(String.valueOf(historicalTraceStoreSize));
                    if (historicalTraceEnabled != null) {
                        configProxy.setHistoricalTraceEnabled(historicalTraceEnabled.toString());
                    }
                    if (historicalTraceStoreTimeout != null) {
                        configProxy.setHistoricalTraceStoreTimeout(historicalTraceStoreTimeout);
                    }
                    List<String> notifiers = configProxy.getNotifierList();
                    if (enableNotifiers != null) {
                        for (String notifier : enableNotifiers) {
                            if (notifierNames.contains(notifier)) {
                                if (!notifiers.contains(notifier)) {
                                    notifiers.add(notifier);
                                }
                            } else {
                                throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                        new PropertyChangeEvent(configProxy, "notifiers", notifiers, notifiers));
                            }
                        }
                    }
                    if (disableNotifiers != null) {
                        for (String notifier : disableNotifiers) {
                            if (notifierNames.contains(notifier)) {
                                notifiers.remove(notifier);
                            } else {
                                throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                        new PropertyChangeEvent(configProxy, "notifiers", notifiers, notifiers));
                            }
                        }
                    }
                    if (setNotifiers != null) {
                        notifiers.clear();
                        for (String notifier : setNotifiers) {
                            if (notifierNames.contains(notifier)) {
                                if (!notifiers.contains(notifier)) {
                                    notifiers.add(notifier);
                                }
                            } else {
                                throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                        new PropertyChangeEvent(configProxy, "notifiers", notifiers, notifiers));
                            }
                        }
                    }

                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return configProxy;
                }, config);
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            report.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }

    private void configureDynamically() {
        if (enabled != null) {
            healthCheck.setEnabled(enabled);
        }
        healthCheck.setHistoricalTraceStoreSize(historicalTraceStoreSize);
        if (historicalTraceEnabled != null) {
            healthCheck.setHistoricalTraceEnabled(historicalTraceEnabled);
        }
        if (historicalTraceStoreTimeout != null) {
            healthCheck.setHistoricalTraceStoreTimeout(TimeUtil.setStoreTimeLimit(this.historicalTraceStoreTimeout));
        }

        Set<String> notifiers = healthCheck.getEnabledNotifiers();
        if (enableNotifiers != null) {
            notifiers.addAll(enableNotifiers);
        }
        if (disableNotifiers != null) {
            disableNotifiers.forEach(notifiers::remove);
        }
        if (setNotifiers != null) {
            notifiers.clear();
            notifiers.addAll(setNotifiers);
        }
    }
}
