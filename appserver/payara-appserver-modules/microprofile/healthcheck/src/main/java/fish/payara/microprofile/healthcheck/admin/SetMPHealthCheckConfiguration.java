/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import java.util.logging.Logger;
import javax.inject.Inject;
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
    @RestEndpoint(configBean = MetricsHealthCheckConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            description = "Configures Microprofile HealthCheck")
})
public class SetMPHealthCheckConfiguration implements AdminCommand {

    private static final Logger LOGGER = Logger.getLogger("MP-HealthCheck");

    @Param(name = "enabled", optional = true)
    private Boolean enabled;
    
    @Param(name = "secureHealth", optional = true)
    private Boolean secure;

    @Param(name = "endpoint", optional = true)
    private String endpoint;

    @Param(name = "virtualServers", optional = true)
    private String virtualServers;

    @Param(name = "target", optional = true, defaultValue = "server")
    private String target;

    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Inject
    UnprocessedConfigListener unprocessedListener;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();

        Config targetConfig = targetUtil.getConfig(target);
        MetricsHealthCheckConfiguration config = targetConfig.getExtensionByType(MetricsHealthCheckConfiguration.class);

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
                if (secure != null) {
                    configProxy.setSecureHealthCheck(secure.toString());
                }
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return configProxy;
            }, config);

            actionReport.setMessage("Restart server for change to take effect");
        } catch (TransactionFailure ex) {
            actionReport.failure(LOGGER, "Failed to update HealthCheck configuration", ex);
        }
    }

}
