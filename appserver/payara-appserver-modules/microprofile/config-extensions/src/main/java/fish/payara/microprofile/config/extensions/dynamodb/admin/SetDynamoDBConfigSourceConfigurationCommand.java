/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
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
package fish.payara.microprofile.config.extensions.dynamodb.admin;

import java.beans.PropertyVetoException;

import jakarta.validation.constraints.Min;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.util.StringUtils;

import fish.payara.microprofile.config.extensions.dynamodb.DynamoDBConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.source.extension.BaseSetConfigSourceConfigurationCommand;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import org.glassfish.api.ActionReport;

@Service(name = "set-dynamodb-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-dynamodb-config-source-configuration",
            description = "Configures DynamoDB Config Source")
})
public class SetDynamoDBConfigSourceConfigurationCommand extends BaseSetConfigSourceConfigurationCommand<DynamoDBConfigSourceConfiguration> {

    @Param(optional = true, name = "region-name", alias = "regionName")
    protected String regionName;

    @Param(optional = true, name = "table-name", alias = "tableName")
    protected String tableName;

    @Param(optional = true, name = "key-column-name", alias = "keyColumnName")
    protected String keyColumnName;

    @Param(optional = true, name = "value-column-name", alias = "valueColumnName")
    protected String valueColumnName;

    @Param(optional = true, defaultValue = "100")
    @Min(value = 1, message = "Limit value must be 1 or more")
    protected String limit;

    @Override
    protected void applyValues(ActionReport report, DynamoDBConfigSourceConfiguration configuration) throws PropertyVetoException {
        super.applyValues(report, configuration);
        if (StringUtils.ok(regionName)) {
            configuration.setRegionName(regionName);
        }
        if (StringUtils.ok(tableName)) {
            configuration.setTableName(tableName);
        }

        if (StringUtils.ok(keyColumnName)) {
            configuration.setKeyColumnName(keyColumnName);
        }

        if (StringUtils.ok(valueColumnName)) {
            configuration.setValueColumnName(valueColumnName);
        }

        configuration.setLimit(limit);
    }
}
