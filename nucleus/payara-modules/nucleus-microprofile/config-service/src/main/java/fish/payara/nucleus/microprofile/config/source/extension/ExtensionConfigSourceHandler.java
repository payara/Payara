/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.source.extension;

import static java.lang.Boolean.valueOf;

import org.glassfish.hk2.api.ServiceHandle;

import fish.payara.nucleus.microprofile.config.source.extension.proxy.ConfigSourceProxy;
import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;

public class ExtensionConfigSourceHandler {

    private final ExtensionConfigSource configSource;
    private final ConfigSourceProxy proxyConfigSource;
    private final Class<ConfigSourceConfiguration> configClass;

    private final String configSourceName;

    private ConfigSourceConfiguration config;

    public ExtensionConfigSourceHandler(final ServiceHandle<ExtensionConfigSource> configSourceHandle) {
        this(configSourceHandle, null, null);
    }

    public ExtensionConfigSourceHandler(final ServiceHandle<ExtensionConfigSource> configSourceHandle, final Class<ConfigSourceConfiguration> configClass, final ConfigSourceConfiguration config) {
        this.configSource = configSourceHandle.getService();
        this.configSourceName = ConfigSourceExtensions.getName(configSourceHandle);
        this.configClass = configClass;
        this.config = config;
        this.proxyConfigSource = new ConfigSourceProxy(configSourceName);
    }

    public Class<ConfigSourceConfiguration> getConfigClass() {
        return configClass;
    }

    protected String getName() {
        return configSourceName;
    }

    protected void reconfigure(ConfigSourceConfiguration config) {
        this.config = config;

        // Get the current configuration
        ConfigSourceConfiguration currentConfig = ConfiguredExtensionConfigSource.class.cast(configSource).getConfiguration();

        final boolean enabled = config != null && valueOf(config.getEnabled());
        final boolean wasEnabled = currentConfig != null && valueOf(currentConfig.getEnabled());

        if (!enabled) {
            if (wasEnabled) {
                // If the config source isn't enabled but was before
                destroy();
            }
        } else {
            if (wasEnabled) {
                // If the config source is enabled and was before
                destroy();
                bootstrap();
            } else {
                // If the config source is enabled and wasn't before
                bootstrap();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected synchronized void destroy() {
        // Should only destroy a config source if it's enabled before any configuration change
        final boolean wasEnabled = isEnabled();

        // Set the configuration before destroying the config source
        if (configClass != null) {
            ConfiguredExtensionConfigSource.class.cast(configSource).setConfiguration(config);
        }
        if (wasEnabled) {
            proxyConfigSource.setDelegate(null);
            configSource.destroy();
        }
    }

    @SuppressWarnings("unchecked")
    protected synchronized void bootstrap() {
        // Set the configuration before bootstrapping the config source
        if (configClass != null) {
            ConfiguredExtensionConfigSource.class.cast(configSource).setConfiguration(config);
        }
        if (isEnabled()) {
            configSource.bootstrap();
            proxyConfigSource.setDelegate(configSource);
        }
    }

    public ConfigSourceProxy getProxyConfigSource() {
        return proxyConfigSource;
    }

    /**
     * @return true if the current config source is enabled, or false otherwise
     */
    private boolean isEnabled() {
        if (configClass != null) {
            ConfigSourceConfiguration config = ConfiguredExtensionConfigSource.class.cast(configSource).getConfiguration();
            if (config == null) {
                return false;
            }
            return config != null && valueOf(config.getEnabled());
        }
        return true;
    }

}
