/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigProviderResolverSync extends ConfigProviderResolver {
    private static final Logger LOG = Logger.getLogger(ConfigProviderResolverImpl.class.getName());

    private ConfigProviderResolver await() {
        IllegalStateException exception = new IllegalStateException("Payara Microprofile Config needs running server environment to work. " +
                "Either it's not running, or you're experiencing a race condition");
        LOG.log(Level.FINE, "Premature call to MP Config", exception);
        try {
            if (ConfigProviderResolverImpl.initialized.await(5, TimeUnit.SECONDS)) {
                // the real resolver initialized, and have set the right instance already
                // but it might have done it at unfortunate moment where it was overwritten by this instance
                ConfigProviderResolver.setInstance(ConfigProviderResolverImpl.instance);
                return ConfigProviderResolverImpl.instance;
            } else {
                // we log and throw, as these exceptions might get swallowed as
                // java.lang.NoClassDefFoundError: Could not initialize class org.eclipse.microprofile.config.ConfigProvider
                LOG.log(Level.WARNING, "Timeout out waiting for Microprofile Config startup", exception);
            }
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Interrupted while waiting for Microprofile Config to initialize", e);
        }
        throw exception;
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
}
