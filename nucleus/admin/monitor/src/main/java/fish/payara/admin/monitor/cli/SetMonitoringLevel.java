/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.admin.monitor.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Asadmin command to set Module Monitoring Level.
 *
 * @author Susan Rai
 */
@Service(name = "set-monitoring-level")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.monitoring.level")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER,
    CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-monitoring-level",
            description = "Set Module Monitoring level")
})
public class SetMonitoringLevel implements AdminCommand {

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "module", optional = false)
    private String moduleNames;

    @Param(name = "level", optional = false)
    private String moduleMonitoringLevels;

    @Inject
    private Target targetUtil;

    @Inject
    private Logger logger;

    private MonitoringService monitoringService;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        Config config = targetUtil.getConfig(target);
        if (config != null) {
            monitoringService = config.getMonitoringService();
        } else {
            actionReport.setMessage("Cound not find target: " + target);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        if (moduleNames != null && moduleMonitoringLevels != null) {
            String modifiedModuleNames = moduleNames.replace(":", ",");
            String modifiedModuleMonitoringLevels = moduleMonitoringLevels.replace(":", ",");
            List<String> moduleNameList = Arrays.asList(modifiedModuleNames.split(","));
            List<String> moduleLevelList = Arrays.asList(modifiedModuleMonitoringLevels.split(","));

            if (moduleNameList.size() == moduleLevelList.size()) {
                for (int i = 0; i < moduleLevelList.size(); i++) {
                    boolean isValidMoudle = false;
                    List<String> validModuleList = new ArrayList<>(Arrays.asList(Constants.validModuleNames));
                    String moduleName = moduleNameList.get(i).trim().toLowerCase();
                    for (String module : validModuleList) {
                        if (module.trim().equalsIgnoreCase(moduleName)) {
                            String moduleLevel = moduleLevelList.get(i).trim().toUpperCase();
                            try {
                                ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {
                                    @Override
                                    public Object run(final MonitoringService monitoringServiceProxy)
                                            throws PropertyVetoException, TransactionFailure {
                                        monitoringServiceProxy.setMonitoringLevel(moduleName, moduleLevel);
                                        return monitoringServiceProxy;
                                    }

                                }, monitoringService);
                            } catch (TransactionFailure ex) {
                                logger.log(Level.WARNING, "Failed to execute the command set-monitoring-level: {0}", ex.getCause().getMessage());
                                actionReport.setMessage(ex.getCause().getMessage());
                                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            }

                            isValidMoudle = true;
                            break;
                        }
                    }

                    if (!isValidMoudle) {
                        actionReport.setMessage(moduleNameList.get(i) + " isn't a valid Module name");
                        actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }
                }
            } else {
                actionReport.setMessage("Number of Module and Level entered doesn't match");
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}