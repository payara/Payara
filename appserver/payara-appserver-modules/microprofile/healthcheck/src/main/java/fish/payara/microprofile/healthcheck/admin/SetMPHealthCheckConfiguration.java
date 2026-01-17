/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.microprofile.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.microprofile.SetSecureMicroprofileConfigurationCommand;
import fish.payara.microprofile.healthcheck.config.MicroprofileHealthCheckConfiguration;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author jonathan coustick
 * @since 4.1.2.182
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@Service(name = "set-microprofile-healthcheck-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set-microprofile-healthcheck-configuration")
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileHealthCheckConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            description = "Configures Microprofile HealthCheck")
})
public class SetMPHealthCheckConfiguration extends SetSecureMicroprofileConfigurationCommand {

    private static final Logger LOGGER = Logger.getLogger("MP-HealthCheck");

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "endpoint", optional = true)
    private String endpoint;

    @Param(name = "virtualServers", optional = true)
    private String virtualServers;

    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Inject
    UnprocessedConfigListener unprocessedListener;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Subject subject = context.getSubject();
        Config targetConfig = targetUtil.getConfig(target);
        MicroprofileHealthCheckConfiguration config = targetConfig.getExtensionByType(MicroprofileHealthCheckConfiguration.class);

        if (Boolean.TRUE.equals(securityEnabled)
                || Boolean.parseBoolean(config.getSecurityEnabled())) {
            ActionReport checkUserReport = actionReport.addSubActionsReport();
            ActionReport createUserReport = actionReport.addSubActionsReport();
            if (!defaultMicroprofileUserExists(checkUserReport, subject) && !checkUserReport.hasFailures()) {
                createDefaultMicroprofileUser(createUserReport, subject);
            }
            if (checkUserReport.hasFailures() || createUserReport.hasFailures()) {
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        try {
            ConfigSupport.apply(configProxy -> {
                if (enabled != null) {
                    configProxy.setEnabled(enabled.toString());
                }
                if (endpoint != null) {
                    configProxy.setEndpoint(endpoint);
                }
                if (virtualServers != null) {
                    configProxy.setVirtualServers(virtualServers);
                }
                if (securityEnabled != null) {
                    configProxy.setSecurityEnabled(securityEnabled.toString());
                }
                if (roles != null) {
                    configProxy.setRoles(roles);
                }
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return configProxy;
            }, config);

            actionReport.setMessage("Restart server for change to take effect");
        } catch (TransactionFailure ex) {
            actionReport.failure(LOGGER, "Failed to update HealthCheck configuration", ex);
        }

        // If everything has passed, scrap the subaction reports as we don't want to print them out
        if (!actionReport.hasFailures() && !actionReport.hasWarnings()) {
            actionReport.getSubActionsReport().clear();
        }
    }

}
