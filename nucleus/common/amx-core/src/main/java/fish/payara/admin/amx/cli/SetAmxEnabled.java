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
package fish.payara.admin.amx.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.admin.amx.AMXBootService;
import fish.payara.admin.amx.config.AMXConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.admin.amx.util.AMXLoggerInfo;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CLUSTERED_INSTANCE;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.DEPLOYMENT_GROUP;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;

/**
 * Comment to enable AMX, as separate from JMX.
 * <p>
 * Note that while AMX can be started dynamically, it cannot be stopped dynamically and requires a server restart.
 * @author jonathan coustick
 * @since 4.1.2.182
 */
@Service(name = "set-amx-enabled")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({DAS, DEPLOYMENT_GROUP, STANDALONE_INSTANCE, CLUSTER, CLUSTERED_INSTANCE, CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-amx-enabled",
            description = "Sets whether AMX in enabled")
})
public class SetAmxEnabled implements AdminCommand {

    private static final Logger LOGGER = AMXLoggerInfo.getLogger();

    @Param(name = "enabled", optional = true, defaultValue = "true")
    Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "true")
    private Boolean dynamic;

    @Param(name="target", optional = true, defaultValue = "server-config")
    private String target;

    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();

        Config targetConfig = targetUtil.getConfig(target);
        AMXConfiguration metricsConfiguration = targetConfig.getExtensionByType(AMXConfiguration.class);

        try {
            ConfigSupport.apply(new SingleConfigCode<AMXConfiguration>() {
                @Override
                public Object run(final AMXConfiguration configProxy) throws PropertyVetoException, TransactionFailure {
                    configProxy.setEnabled(enabled.toString());
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return configProxy;
                }
            }, metricsConfiguration);

            AMXBootService bootService = habitat.getService(AMXBootService.class);
            bootService.setEnabled(enabled, dynamic);

        } catch (TransactionFailure ex) {
            LOGGER.log(Level.WARNING, "Exception during command set-amx-enabled: {0}", ex.getCause().getMessage());
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

}
