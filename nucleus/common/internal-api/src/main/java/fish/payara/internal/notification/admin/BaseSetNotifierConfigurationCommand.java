/*
 *
 * Copyright (c) 2016-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.internal.notification.admin;

import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.internal.notification.EventLevel;
import jakarta.inject.Inject;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.internal.notification.NotifierManager;
import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.PayaraNotifierConfiguration;

/**
 * The base admin command to set the configuration of a specified notifier.
 * Extend this class to configure custom notifier configuration options
 * from @Param injected fields.
 * 
 * @author Matthew Gill
 * @author mertcaliskan
 */
public abstract class BaseSetNotifierConfigurationCommand<C extends PayaraNotifierConfiguration> implements AdminCommand {

    @Inject
    protected Target targetUtil;

    @Inject
    protected ServerEnvironment server;

    @Inject
    private NotifierManager notificationService;

    @Inject
    protected Logger logger;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "enabled")
    protected Boolean enabled;

    @Param(name = "filter", optional = true, acceptableValues = "INFO,WARNING,SEVERE")
    protected String filter;

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
        Class<C> notifierConfigurationClass = NotifierUtils.getConfigurationClass(getClass());

        C c = notificationServiceConfiguration.getNotifierConfigurationByType(notifierConfigurationClass);

        // Getting via HK2 Duck Typing only works if the default config tags are present in the domain.xml, which they
        // won't be if they aren't present in the default domain template (which in this case they're not). The default
        // config tags get generated for an instance's config when the instance is started, but this cannot be relied
        // on for two reasons: the "default-config" config is never started; and non-DAS instances will only generate
        // the tags for their local config (it doesn't get synced back to the DAS).
        if (c == null) {
            try {
                // Create a transaction around the notifier config we want to add the element to, grab it's list
                // of notifiers, and add a new child element to it
                ConfigSupport.apply(notificationServiceConfigurationProxy -> {
                     notificationServiceConfigurationProxy.getNotifierConfigurationList().add(
                             notificationServiceConfigurationProxy.createChild(notifierConfigurationClass));
                     return notificationServiceConfigurationProxy;
                }, notificationServiceConfiguration);
            } catch (TransactionFailure ex) {
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            // Attempt to grab the new child element config as the config we want to configure
            c = notificationServiceConfiguration.getNotifierConfigurationByType(notifierConfigurationClass);

            // If we still can't find the config, exit out - something has gone wrong
            if (c == null) {
                actionReport.setMessage("Could not locate notifier config");
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        try {
            ConfigSupport.apply(cProxy -> {
                applyValues(cProxy);
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return cProxy;
            }, c);

            // If the service is being changed dynamically
            if (dynamic) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                         configureDynamically(c);
                    }
                } else {
                    configureDynamically(c);
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
    protected void applyValues(C configuration) throws PropertyVetoException {
        if (this.enabled != null) {
            configuration.enabled(this.enabled);
        }
        if (this.filter != null) {
            configuration.filter(EventLevel.fromNameOrWarning(this.filter).toString());
        }
    }

    /**
     * Called after static configuration if the dynamic field is enabled and this
     * instance is the one selected.
     */
    protected void configureDynamically(C configuration) {
        notificationService.reconfigureNotifier(configuration);
    }
}