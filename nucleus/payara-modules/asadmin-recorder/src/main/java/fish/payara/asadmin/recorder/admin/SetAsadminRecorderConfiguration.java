/*
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.asadmin.recorder.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.asadmin.recorder.AsadminRecorderConfiguration;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "set-asadmin-recorder-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.asadmin.recorder.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-asadmin-recorder-configuration",
            description = "Sets the configuration for the asadmin command recorder service")
})
public class SetAsadminRecorderConfiguration implements AdminCommand {

    @Inject
    AsadminRecorderConfiguration asadminRecorderConfiguration;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "outputLocation", optional = true)
    private String outputLocation;

    @Param(name = "filterCommands", optional = true)
    private Boolean filterCommands;

    @Param(name = "filteredCommands", optional = true)
    private String filteredCommands;

    @Param(name = "prependEnabled", optional = true)
    private Boolean prependEnabled;

    @Param(name = "prependedOptions", optional = true)
    private String prependedOptions;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<AsadminRecorderConfiguration>() {
                @Override
                public Object run(AsadminRecorderConfiguration asadminRecorderConfigurationProxy) throws PropertyVetoException {

                    if (enabled != null) {
                        asadminRecorderConfigurationProxy.setEnabled(enabled);
                    }

                    if (filterCommands != null) {
                        asadminRecorderConfigurationProxy.setFilterCommands(filterCommands);
                    }

                    if (outputLocation != null) {
                        if (outputLocation.endsWith("/") || outputLocation.endsWith("\\")) {
                            outputLocation += "asadmin-commands.txt";
                        }
                        if (!outputLocation.endsWith(".txt")) {
                            outputLocation += ".txt";
                        }
                        asadminRecorderConfigurationProxy.setOutputLocation(outputLocation);
                    }

                    if (filteredCommands != null) {
                        asadminRecorderConfigurationProxy.setFilteredCommands(filteredCommands);
                    }

                    if (prependEnabled != null) {
                        asadminRecorderConfigurationProxy.setPrependEnabled(prependEnabled);
                    }

                    String allowedPrependedOptionsRegex = "("
                            + "(H[ =].*)|(host[ =].*)|(p[ =].*)|(port[ =].*)"
                            + "|(U[ =].*)|(user[ =].*)|(W[ =].*)|(passwordfile[ =].*)"
                            + "|(detach)|(notify)|(terse)|(t)|(echo)|(e)|(secure)|(s)|(interactive)|(i))";
                    
                    if (prependedOptions != null) {
                        if (!prependedOptions.isEmpty()) {
                            for (String prependedOption : prependedOptions.split(",")) {
                                if (prependedOption.matches(allowedPrependedOptionsRegex)) {
                                    asadminRecorderConfigurationProxy.setPrependedOptions(prependedOptions);
                                } else {
                                    throw new PropertyVetoException("Invalid prepended option \"" + prependedOption + "\"", new PropertyChangeEvent(asadminRecorderConfiguration, "prependedOptions", asadminRecorderConfiguration.getPrependedOptions(), prependedOption));
                                }
                            }
                        } else {
                            asadminRecorderConfigurationProxy.setPrependedOptions("");
                        }
                    }
                    return null;
                }
            }, asadminRecorderConfiguration);
        } catch (TransactionFailure ex) {
            actionReport.failure(Logger.getLogger(SetAsadminRecorderConfiguration.class.getName()),
                    "Failed to update configuration due to: " + ex.getLocalizedMessage(), ex);
        }
    }
}
