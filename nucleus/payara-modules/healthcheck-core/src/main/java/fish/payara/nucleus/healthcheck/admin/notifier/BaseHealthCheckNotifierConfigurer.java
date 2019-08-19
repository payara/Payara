/*
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.admin.notifier;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.notification.configuration.Notifier;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.security.auth.Subject;

import java.beans.PropertyVetoException;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
public abstract class BaseHealthCheckNotifierConfigurer<C extends Notifier> implements AdminCommand {

    @Inject
    protected Target targetUtil;

    @Inject
    protected ServerEnvironment server;

    @Inject
    protected Logger logger;

    @Inject
    protected HealthCheckService healthCheckService;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "enabled")
    protected Boolean enabled;

    private Class<C> notifierClass;

    @Param(name = "noisy", optional = true)
    protected Boolean noisy;

    @Inject
    protected ServiceLocator serviceLocator;

    private ActionReport actionReport;

    private Subject subject;

    private CommandRunner.CommandInvocation inv;

    protected abstract void applyValues(C c) throws PropertyVetoException;

    @Override
    public void execute(AdminCommandContext context) {
        actionReport = context.getActionReport();
        subject = context.getSubject();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config configuration = targetUtil.getConfig(target);
        final HealthCheckServiceConfiguration healthCheckServiceConfiguration = configuration.getExtensionByType(HealthCheckServiceConfiguration.class);
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        notifierClass = (Class<C>) genericSuperclass.getActualTypeArguments()[0];

        C notifier = healthCheckServiceConfiguration.getNotifierByType(notifierClass);

        try {
            if (notifier == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration healthCheckServiceConfigurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        C c = healthCheckServiceConfigurationProxy.createChild(notifierClass);
                        applyValues(c);
                        healthCheckServiceConfigurationProxy.getNotifierList().add(c);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return healthCheckServiceConfigurationProxy;
                    }
                }, healthCheckServiceConfiguration);
            }
            else {
                ConfigSupport.apply(new SingleConfigCode<C>() {
                    public Object run(C cProxy) throws PropertyVetoException, TransactionFailure {
                        applyValues(cProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return cProxy;
                    }
                }, notifier);
            }

            if (dynamic) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                        configureDynamically();
                    }
                }
                else {
                    configureDynamically();
                }
            }
        }
        catch(TransactionFailure ex){
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    protected void configureDynamically() {
        healthCheckService.reboot();
    }

    @SuppressWarnings("rawtypes")
    protected boolean getNotifierNoisy(String commandName) {
        boolean ret = true;
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = actionReport.addSubActionsReport();
        inv = runner.getCommandInvocation(commandName, subReport, subject);
        inv.execute();
        if (subReport.hasSuccesses()) {
            Properties properties = subReport.getExtraProperties();
            if (properties != null) {
                Map notifierConfigurationMap = (Map) properties.get("notifierConfiguration");
                ret = "true".equalsIgnoreCase("" + notifierConfigurationMap.get("noisy"));
            }
        }
        return ret;
    }
}