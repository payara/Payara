/*
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.source.extension;

import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

/**
 * The base admin command to set the configuration of a specified config source.
 * Extend this class to configure custom config source configuration options
 * from @Param injected fields.
 * 
 * @author Matthew Gill
 */
public abstract class BaseSetConfigSourceConfigurationCommand<C extends ConfigSourceConfiguration> implements AdminCommand {

    @Inject
    protected Target targetUtil;

    @Inject
    protected ServerEnvironment server;

    @Inject
    private ExtensionConfigSourceService extensionService;

    @Inject
    protected Logger logger;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "enabled")
    protected Boolean enabled;

    @Override
    public void execute(final AdminCommandContext context) {

        // Get the command report
        final ActionReport report = context.getActionReport();

        // Get the target configuration
        final Config targetConfig = targetUtil.getConfig(target);
        if (targetConfig == null) {
            report.setMessage("No such config named: " + target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // Initialise the extra properties
        Properties extraProperties = report.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            report.setExtraProperties(extraProperties);
        }

        final MicroprofileConfigConfiguration mpConfigConfiguration = targetConfig.getExtensionByType(MicroprofileConfigConfiguration.class);
        Class<C> configSourceConfigClass = ConfigSourceExtensions.getConfigurationClass(getClass());

        C c = mpConfigConfiguration.getConfigSourceConfigurationByType(configSourceConfigClass);

        try {
            if (c == null) {
                ConfigSupport.apply(new SingleConfigCode<MicroprofileConfigConfiguration>() {
                    @Override
                    public Object run(final MicroprofileConfigConfiguration configurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        C c = configurationProxy.createChild(configSourceConfigClass);
                        applyValues(report, c);
                        if (report.getActionExitCode() != ActionReport.ExitCode.FAILURE) {
                            configurationProxy.getConfigSourceConfigurationList().add(c);
                        } else {
                            c = null;
                        }
                        return c;
                    }
                }, mpConfigConfiguration);
                c = mpConfigConfiguration.getConfigSourceConfigurationByType(configSourceConfigClass);
            }
            else {
                ConfigSupport.apply(new SingleConfigCode<C>() {
                    public Object run(C cProxy) throws PropertyVetoException, TransactionFailure {
                        applyValues(report, cProxy);
                        return cProxy;
                    }
                }, c);
            }

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
            report.setMessage(ex.getCause().getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
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
    protected void applyValues(ActionReport report, C configuration) throws PropertyVetoException {
        if (this.enabled != null) {
            configuration.setEnabled(Boolean.toString(this.enabled));
        }
    }

    /**
     * Called after static configuration if the dynamic field is enabled and this
     * instance is the one selected.
     */
    protected void configureDynamically(C configuration) {
        extensionService.reconfigure(configuration);
    }
}