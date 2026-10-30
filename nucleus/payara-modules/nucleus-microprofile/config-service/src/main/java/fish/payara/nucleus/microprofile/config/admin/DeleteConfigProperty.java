/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2026] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.microprofile.config.ConfigModificationService;
import fish.payara.nucleus.microprofile.config.source.extension.ExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.source.extension.ExtensionConfigSourceService;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.TransactionFailure;

import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.*;

/**
 * asAdmin command to delete the value of a config property.
 *
 * @since 4.1.2.173
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "delete-config-property")
@PerLookup
@ExecuteOn()
@TargetType()
@RestEndpoints({
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "delete-config-property",
            description = "Deletes a configuration property")
})
public class DeleteConfigProperty implements AdminCommand {

    @Param(optional = true, acceptableValues = "domain,config,server,application,module,cluster,jndi,cloud", defaultValue = DOMAIN)
    String source;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param
    String propertyName;

    @Param(optional = true)
    String sourceName;

    @Param(optional = true)
    String moduleName;

    @Inject
    private ConfigModificationService modificationService;

    @Inject
    private ExtensionConfigSourceService extensionService;

    @Override
    public void execute(AdminCommandContext context) {
        try {
            boolean success;
            var modifiableSource = modificationService.getModifiable(source, sourceName, moduleName, target);
            if (modifiableSource.isPresent()) {
                success = modifiableSource.get().deleteValue(propertyName);
            } else {
                success = deleteFromExtensionSource();
            }
            if (!success) {
                context.getActionReport().failure(
                        Logger.getLogger(DeleteConfigProperty.class.getName()),
                        "Failed to delete MicroProfile Config property \"" + propertyName + "\" from source " + source
                        + (sourceName != null ? " (name=" + sourceName + ")" : ""));
            }
        } catch (TransactionFailure e) {
            context.getActionReport().failure(
                    Logger.getLogger(DeleteConfigProperty.class.getName()),
                    "Failed to delete MicroProfile Config property: " + e.getMessage(), e);
        }
    }

    private boolean deleteFromExtensionSource() {
        Collection<ExtensionConfigSource> extensionSources = extensionService.getExtensionSources();
        for (ExtensionConfigSource extension : extensionSources) {
            if (CLOUD.equals(source) && extension.getName().equals(sourceName)) {
                extension.deleteValue(propertyName);
                return true;
            }
        }
        return false;
    }
}
