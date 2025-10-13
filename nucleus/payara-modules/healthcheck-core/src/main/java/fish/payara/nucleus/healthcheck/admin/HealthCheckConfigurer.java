/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.util.LocalStringManagerImpl;

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
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.TimeUtil;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;

/**
 * Admin command to enable/disable all health check services defined in
 * domain.xml.
 *
 * @author mertcaliskan
 * @deprecated Replaced by {@link SetHealthCheckConfiguration}
 */
@Deprecated
@Service(name = "healthcheck-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-configure",
            description = "Enables/Disables Health Check Service")
})
public class HealthCheckConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckConfigurer.class);
    
    @Inject
    ServerEnvironment server;

    @Inject
    protected Logger logger;

    @Inject
    HealthCheckService service;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "enabled")
    private Boolean enabled;

    @Deprecated
    @Param(name = "notifierEnabled", optional = true)
    private Boolean notifierEnabled;

    @Param(name = "historicalTraceEnabled", optional = true)
    private Boolean historicalTraceEnabled;
  
    @Param(name = "historicalTraceStoreSize", optional = true, defaultValue = "20")
    @Min(value = 1, message = "Store size must be greater than 0")
    private Integer historicalTraceStoreSize;

    @Param(name = "historicalTraceStoreTimeout", optional = true)
    private String historicalTraceStoreTimeout;

    @Param(name = "enableNotifiers", alias = "enable-notifiers", optional = true)
    private List<String> enableNotifiers;

    @Param(name = "disableNotifiers", alias = "disable-notifiers", optional = true)
    private List<String> disableNotifiers;

    @Param(name = "setNotifiers", alias = "set-notifiers", optional = true)
    private List<String> setNotifiers;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config config = targetUtil.getConfig(target);
        final HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
        if (healthCheckServiceConfiguration != null) {
            try {
                final Set<String> notifierNames = NotifierUtils.getNotifierNames(serviceLocator);
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration proxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (enabled != null) {
                            proxy.enabled(enabled.toString());
                        }
                        if (historicalTraceEnabled != null) {
                            proxy.setHistoricalTraceEnabled(historicalTraceEnabled.toString());
                        }
                        if (historicalTraceStoreSize != null) {
                            proxy.setHistoricalTraceStoreSize(historicalTraceStoreSize.toString());
                        }
                        if (historicalTraceStoreTimeout != null) {
                            proxy.setHistoricalTraceStoreTimeout(historicalTraceStoreTimeout.toString());
                        }

                        List<String> notifiers = proxy.getNotifierList();
                        if (enableNotifiers != null) {
                            for (String notifier : enableNotifiers) {
                                if (notifierNames.contains(notifier)) {
                                    notifiers.add(notifier);
                                } else {
                                    throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                            new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                                }
                            }
                        }
                        if (disableNotifiers != null) {
                            for (String notifier : disableNotifiers) {
                                if (notifierNames.contains(notifier)) {
                                    notifiers.remove(notifier);
                                } else {
                                    throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                            new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
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
                                            new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                                }
                            }
                        }

                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return proxy;
                    }

                }, healthCheckServiceConfiguration);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (dynamic) {
            if (server.isDas()) {
                if (targetUtil.getConfig(target).isDas()) {
                    configureDynamically();
                }
            } else {
                // apply as not the DAS so implicitly it is for us
                configureDynamically();
            }
        }
    }

    private void configureDynamically() {
        service.setEnabled(enabled);

        if (historicalTraceEnabled != null) {
            service.setHistoricalTraceEnabled(historicalTraceEnabled);
        }
        if (historicalTraceStoreSize != null) {
            service.setHistoricalTraceStoreSize(historicalTraceStoreSize);
        }
        if (historicalTraceStoreTimeout != null) {
            long timeout = TimeUtil.setStoreTimeLimit(this.historicalTraceStoreTimeout);
            service.setHistoricalTraceStoreTimeout(timeout);
        }

        Set<String> notifiers = service.getEnabledNotifiers();
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