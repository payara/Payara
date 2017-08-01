/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.appserver.environment.warning.config.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.environment.warning.config.EnvironmentWarningConfiguration;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
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
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Matt Gill
 */
@Service(name = "set-environment-warning-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-environment-warning-configuration",
            description = "Sets the Environment Warning Configuration")
})
public class SetEnvironmentWarningConfigurationCommand implements AdminCommand {

    @Inject
    private Target targetUtil;

    @Param(name = "enabled", alias = "Enabled", optional = true)
    private Boolean enabled;

    @Param(name = "message", alias = "Message", optional = true)
    private String message;

    @Param(name = "backgroundColour", alias = "BackgroundColour", optional = true)
    private String backgroundColour;

    @Param(name = "textColour", alias = "TextColour", optional = true)
    private String textColour;

    private final String target = "server-config";

    @Override
    public void execute(AdminCommandContext acc) {
        Config config = targetUtil.getConfig(target);
        ActionReport actionReport = acc.getActionReport();

        EnvironmentWarningConfiguration environmentWarningConfiguration
                = config.getExtensionByType(EnvironmentWarningConfiguration.class);

        if (environmentWarningConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<EnvironmentWarningConfiguration>() {
                    @Override
                    public Object run(EnvironmentWarningConfiguration config) throws PropertyVetoException {

                        if (enabled != null) {
                            config.setEnabled(enabled);
                        }

                        if (message != null) {
                            config.setMessage(message);
                        }

                        if (backgroundColour != null) {
                            if (backgroundColour.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                                config.setBackgroundColour(backgroundColour);
                            } else {
                                throw new PropertyVetoException(
                                        "Invalid data for background colour, must be a hex value.",
                                        new PropertyChangeEvent(config, "backgroundColour",
                                                config.getBackgroundColour(), backgroundColour));
                            }
                        }

                        if (textColour != null) {
                            if (textColour.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                                config.setTextColour(textColour);
                            } else {
                                throw new PropertyVetoException(
                                        "Invalid data for text colour, must be a hex value.",
                                        new PropertyChangeEvent(config, "textColour", 
                                                config.getTextColour(), textColour));
                            }
                        }
                        return null;
                    }
                }, environmentWarningConfiguration);
            } catch (TransactionFailure ex) {
                // Set failure
                actionReport.failure(Logger.getLogger(SetEnvironmentWarningConfigurationCommand.class.getName()),
                        "Failed to update configuration", ex);
            }
        }
    }
}
