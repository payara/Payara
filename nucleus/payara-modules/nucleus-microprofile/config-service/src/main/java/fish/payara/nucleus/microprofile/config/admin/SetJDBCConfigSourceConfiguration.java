/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.microprofile.config.spi.JDBCConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import java.util.logging.Logger;
import jakarta.inject.Inject;
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
import org.jvnet.hk2.config.TransactionFailure;

@Service(name = "set-jdbc-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-jdbc-config-source-configuration",
            description = "Configures JDBC Config Source")
})
public class SetJDBCConfigSourceConfiguration implements AdminCommand {

    private static final Logger LOGGER = Logger.getLogger(SetJDBCConfigSourceConfiguration.class.getName());

    @Param
    private String jndiName;

    @Param
    private String tableName;

    @Param
    private String keyColumnName;

    @Param
    private String valueColumnName;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Override
    public void execute(AdminCommandContext acc) {
        final ActionReport actionReport = acc.getActionReport();

        final Config targetConfig = targetUtil.getConfig(target);
        if (targetConfig == null) {
            actionReport.setMessage("No such config named: " + target);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        JDBCConfigSourceConfiguration jdbcConfigSourceConfiguration = targetConfig
                .getExtensionByType(JDBCConfigSourceConfiguration.class);

        try {
            ConfigSupport.apply(configProxy -> {
                if (jndiName != null) {
                    configProxy.setJndiName(jndiName);
                }
                if (tableName != null) {
                    configProxy.setTableName(tableName);
                }
                if (keyColumnName != null) {
                    configProxy.setKeyColumnName(keyColumnName);
                }
                if (valueColumnName != null) {
                    configProxy.setValueColumnName(valueColumnName);
                }
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return configProxy;
            }, jdbcConfigSourceConfiguration);
        } catch (TransactionFailure ex) {
            actionReport.failure(LOGGER, "Failed to update JDBC Config Source configuration", ex);
        }

    }

}
