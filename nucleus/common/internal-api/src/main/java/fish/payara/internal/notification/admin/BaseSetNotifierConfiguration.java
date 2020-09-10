/*
 *
 * Copyright (c) [2016-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.internal.notification.admin;

import static java.lang.Boolean.valueOf;

import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotifier;
import fish.payara.internal.notification.PayaraNotifierConfiguration;

/**
 * The base admin command to set the configuration of a specified notifier.
 * Extend this class to configure custom notifier configuration options
 * from @Param injected fields.
 * 
 * @author Matthew Gill
 * @author mertcaliskan
 */
public abstract class BaseSetNotifierConfiguration<NC extends PayaraNotifierConfiguration, N extends PayaraNotifier> implements AdminCommand {

    @Inject
    protected Target targetUtil;

    @Inject
    protected ServerEnvironment server;

    @Inject
    private N service;

    @Inject
    protected Logger logger;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "enabled")
    protected Boolean enabled;

    @Param(name = "noisy", optional = true, defaultValue = "true")
    protected Boolean noisy;

    @Override
    public void execute(final AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config configuration = targetUtil.getConfig(target);
        final NotificationServiceConfiguration notificationServiceConfiguration = configuration.getExtensionByType(NotificationServiceConfiguration.class);
        Class<NC> notifierConfigurationClass = PayaraConfiguredNotifier.getConfigurationClass(service.getClass());

        NC c = notificationServiceConfiguration.getNotifierConfigurationByType(notifierConfigurationClass);

        try {
            ConfigSupport.apply(new SingleConfigCode<NC>() {
                public Object run(NC cProxy) throws PropertyVetoException, TransactionFailure {
                    applyValues(cProxy);
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return cProxy;
                }
            }, c);

            if (dynamic && valueOf(notificationServiceConfiguration.getEnabled())) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                         configureDynamically();
                    }
                } else {
                    configureDynamically();
                }
            }
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    /**
     * Configure the configuration ConfigBeanProxy from the @Param injected class
     * fields.
     * 
     * @param configuration the ConfigBeanProxy to configure
     * @throws PropertyVetoException an exception thrown if the property doesn't
     *                               pass the configured validation.
     */
    protected void applyValues(NC configuration) throws PropertyVetoException {
        if (this.enabled != null) {
            configuration.enabled(this.enabled);
        }
        if (this.noisy != null) {
            configuration.noisy(this.noisy);
        }
    }

    /**
     * Called after static configuration if the dynamic field is enabled and this
     * instance is the one selected.
     */
    protected void configureDynamically() {
        service.destroy();
        service.bootstrap();
    }
}