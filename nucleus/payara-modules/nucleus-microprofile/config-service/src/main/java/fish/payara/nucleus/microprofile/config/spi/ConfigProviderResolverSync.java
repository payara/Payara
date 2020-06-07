/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.nucleus.microprofile.config.spi;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class was created to resolve race condition, caused by independent parallel initialization
 * of the server components, where some may init undesired automatical service lookup.
 * That lookup should lead to this class, which will block all requests until
 * this global resolver instance provided by the {@link ConfigProviderResolver#instance()} would
 * be replaced by the HK2 service.
 * <p>
 * The problem cannot be resolved by any other way, because Microprofile components use the global
 * service mechanism, which is not capable to inject HK2 dependencies.
 *
 * @author Patrik Dudits
 * @author David Matejcek
 */
public class ConfigProviderResolverSync extends ConfigProviderResolver {

    private static final Logger LOG = Logger.getLogger(ConfigProviderResolverSync.class.getName());

    /**
     * Logs the creation of this class on finest level.
     */
    public ConfigProviderResolverSync() {
        LOG.finest("ConfigProviderResolverSync()");
    }

    @Override
    public Config getConfig() {
        return await().getConfig();
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return await().getConfig(loader);
    }

    @Override
    public ConfigBuilder getBuilder() {
        return await().getBuilder();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        await().registerConfig(config, classLoader);
    }

    @Override
    public void releaseConfig(Config config) {
        await().releaseConfig(config);
    }


    private ConfigProviderResolver await() {
        LOG.log(Level.WARNING, "Payara Microprofile Config requested too early, the HK2 service is not initialized yet."
            + " Waiting until it will be active.");
        while (true) {
            final ConfigProviderResolver resolver = instance();
            if (resolver != null && resolver != this) {
                return resolver;
            }
            Thread.yield();
        }
    }
}
