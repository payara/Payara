/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.microprofile.faulttolerance.FaultToleranceServiceConfiguration;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.api.Param;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
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
import org.jvnet.hk2.config.TransactionFailure;

/**
 * @author Andrew Pielage (initial)
 * @author Jan Bernitt (change to pool size)
 */
@Service(name = "set-fault-tolerance-configuration")
@PerLookup
@ExecuteOn({ RuntimeType.DAS })
@TargetType(value = { CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG })
@RestEndpoints({
        @RestEndpoint(configBean = FaultToleranceServiceConfiguration.class, opType = RestEndpoint.OpType.POST, path = "set-fault-tolerance-configuration", description = "Sets the Fault Tolerance Configuration") })
public class SetFaultToleranceConfigurationCommand implements AdminCommand {

    private static final Logger LOGGER = Logger.getLogger(SetFaultToleranceConfigurationCommand.class.getName());

    @Inject
    private Target targetUtil;

    @Param(optional = true, alias = "managedexecutorservicename")
    private String managedExecutorServiceName;

    @Param(optional = true, alias = "managedscheduledexecutorservicename")
    private String managedScheduledExecutorServiceName;

    @Param(optional = true, defaultValue = "server-config")
    private String target;

    @Override
    public void execute(AdminCommandContext acc) {
        final ActionReport report = acc.getActionReport();

        final Config targetConfig = targetUtil.getConfig(target);

        if (targetConfig == null) {
            report.setMessage("No such config name: " + targetUtil);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        final FaultToleranceServiceConfiguration faultToleranceServiceConfiguration = targetConfig
                .getExtensionByType(FaultToleranceServiceConfiguration.class);

        try {
            ConfigSupport.apply((FaultToleranceServiceConfiguration configProxy) -> {
                if (managedExecutorServiceName != null
                        && validateManagedExecutor(managedExecutorServiceName, report)) {
                    configProxy.setManagedExecutorService(managedExecutorServiceName);
                }
                if (managedScheduledExecutorServiceName != null
                        && validateManagedScheduledExecutor(managedScheduledExecutorServiceName, report)) {
                    configProxy.setManagedScheduledExecutorService(managedScheduledExecutorServiceName);
                }
                return null;
            }, faultToleranceServiceConfiguration);
        } catch (TransactionFailure ex) {
            report.failure(LOGGER, "Failed to update Fault Tolerance configuration", ex);
        }
    }

    private static boolean validateManagedScheduledExecutor(String name, ActionReport report) {
        return validateLookup(name, report, ManagedScheduledExecutorService.class);
    }

    private static boolean validateManagedExecutor(String name, ActionReport report) {
        return validateLookup(name, report, ManagedExecutorService.class);
    }

    private static boolean validateLookup(String name, ActionReport report, Class<?> type) {
        try {
            // Lookup the object
            final InitialContext ctx = new InitialContext();
            final Object result = ctx.lookup(name);

            // Type check the object
            if (!type.isInstance(result)) {
                throw new IllegalArgumentException(String.format("Invalid object type for: %s. Was: %s Expected: %s.",
                        name, result.getClass().getName(), type.getName()));
            }
        } catch (NamingException ex) {
            // Error finding the object
            LOGGER.log(Level.WARNING, "Cannot find referenced object: " + name, ex);
            report.failure(LOGGER, "Cannot find referenced object: " + name);
            report.setActionExitCode(ExitCode.FAILURE);
            return false;
        } catch (Exception ex) {
            // Error performing extra checks (such as the type check)
            LOGGER.log(Level.WARNING, "Invalid object: " + name, ex);
            report.failure(LOGGER, ex.getMessage());
            report.setActionExitCode(ExitCode.FAILURE);
            return false;
        }
        return true;
    }

}
