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
package fish.payara.microprofile.config.extensions.azure.admin;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import com.sun.enterprise.util.StringUtils;
import fish.payara.microprofile.config.extensions.azure.AzureSecretsConfigSourceConfiguration;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import fish.payara.nucleus.microprofile.config.source.extension.BaseSetConfigSourceConfigurationCommand;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import org.glassfish.api.ActionReport;

@Service(name = "set-azure-config-source-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-azure-config-source-configuration",
            description = "Configures Azure Secrets Config Source")
})
public class SetAzureSecretsConfigSourceConfigurationCommand extends BaseSetConfigSourceConfigurationCommand<AzureSecretsConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(SetAzureSecretsConfigSourceConfigurationCommand.class.getPackage().getName());

    @Param(optional = true, name = "tenant-id", alias = "tenantId")
    protected String tenantId;

    @Param(optional = true, name = "client-id", alias = "clientId")
    protected String clientId;

    @Param(optional = true, name = "key-vault-name", alias = "keyVaultName")
    protected String keyVaultName;

    @Param(optional = true, name = "private-key-file", alias = "privateKeyFile")
    private File privateKeyFile;

    @Param(optional = true)
    private String thumbprint;

    @Inject
    private ServerEnvironment env;

    @Override
    protected void applyValues(ActionReport report, AzureSecretsConfigSourceConfiguration configuration) throws PropertyVetoException {
        super.applyValues(report, configuration);
        if (StringUtils.ok(tenantId)) {
            configuration.setTenantId(tenantId);
        }

        if (StringUtils.ok(clientId)) {
            configuration.setClientId(clientId);
        }

        if (StringUtils.ok(keyVaultName)) {
            configuration.setKeyVaultName(keyVaultName);
        }

        if (StringUtils.ok(thumbprint)) {
            configuration.setThumbprint(thumbprint);
        }

        if (privateKeyFile != null) {
            if (!privateKeyFile.exists() || !privateKeyFile.isFile()) {
                throw new PropertyVetoException("Private Key file not found", new PropertyChangeEvent(configuration, "privateKeyFile", null, privateKeyFile));
            }
            try {
                Path configDirPath = env.getConfigDirPath().toPath();
                Path output = Files.copy(privateKeyFile.toPath(), configDirPath.resolve(privateKeyFile.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
                configuration.setPrivateKeyFilePath(configDirPath.relativize(output).toString());
                LOGGER.info("File copied to " + output);
            } catch (IOException e) {
                final String message = "Unable to find or access target file";
                LOGGER.log(Level.WARNING, message, e);
                throw new PropertyVetoException(message, new PropertyChangeEvent(configuration, "privateKeyFile", null, privateKeyFile));
            }
        }
    }
}
