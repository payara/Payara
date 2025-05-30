/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.config.activation;

import java.util.Collection;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.Extension;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.JandexIndexer.Index;
import org.glassfish.weld.WeldDeployer;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.config.cdi.ConfigCdiExtension;
import fish.payara.microprofile.connector.MicroProfileDeployer;

@Service
@PerLookup
public class ConfigDeployer extends MicroProfileDeployer<ConfigContainer, ConfigApplicationContainer> {
    @Inject
    JandexIndexer jandexIndexer;

    @Override
    @SuppressWarnings("unchecked")
    public ConfigApplicationContainer load(ConfigContainer container,
            DeploymentContext deploymentContext) {

        // Perform annotation scanning to see if CDI extension is required here
        // This is performed here so that the ApplicationContainer executes regardless of CDI extension state
        Index index = jandexIndexer.getRootIndex(deploymentContext);
        if (index != null) {
            boolean found = false;
            found |= !index.getIndex().getAnnotations(ConfigProperty.class).isEmpty();
            found |= !index.getIndex().getAnnotations(ConfigProperties.class).isEmpty();
            found |= !index.getIndex().getAnnotations(Config.class).isEmpty();
            if (found) {
                // Register the CDI extension
                final Collection<Supplier<Extension>> snifferExtensions = deploymentContext.getTransientAppMetaData(WeldDeployer.SNIFFER_EXTENSIONS, Collection.class);
                if (snifferExtensions != null) {
                    snifferExtensions.add(ConfigCdiExtension::new);
                }
            }
        }

        return new ConfigApplicationContainer(deploymentContext);
    }

    @Override
    public void unload(ConfigApplicationContainer applicationContainer, DeploymentContext ctx) {
    }
    
}
