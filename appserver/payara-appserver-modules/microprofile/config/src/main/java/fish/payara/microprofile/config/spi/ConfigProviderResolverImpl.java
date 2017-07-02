/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.microprofile.config.spi;

import fish.payara.microprofile.config.source.ApplicationConfigSource;
import fish.payara.microprofile.config.source.ClusterConfigSource;
import fish.payara.microprofile.config.source.ConfigConfigSource;
import fish.payara.microprofile.config.source.DomainConfigSource;
import fish.payara.microprofile.config.source.ModuleConfigSource;
import fish.payara.microprofile.config.source.PropertiesConfigSource;
import fish.payara.microprofile.config.source.ServerConfigSource;
import fish.payara.nucleus.microprofile.config.service.MicroprofileConfigService;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class ConfigProviderResolverImpl extends ConfigProviderResolver {
    
    private final MicroprofileConfigService configService;
    private final InvocationManager invocationManager;
    private final ServerContext context;
    private WeakHashMap<ClassLoader,Config> registeredConfigs;
    
    public ConfigProviderResolverImpl() {
        configService = Globals.getDefaultHabitat().getService(MicroprofileConfigService.class);
        invocationManager = Globals.getDefaultHabitat().getService(InvocationManager.class);
        context = Globals.getDefaultHabitat().getService(ServerContext.class);
        registeredConfigs = new WeakHashMap<>();
    }

    @Override
    public Config getConfig() {
        
        Config result = registeredConfigs.get(Thread.currentThread().getContextClassLoader());
        if (result == null) {
            ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
            String appName = currentInvocation.getAppName();
            String moduleName = currentInvocation.getModuleName();
            String serverName = context.getInstanceName();
            String configName = context.getConfigBean().getConfig().getName();
            // build config hierachy
            SortedSet<ConfigSource> sources = new TreeSet();
            sources.add(new DomainConfigSource());
            sources.add(new ClusterConfigSource());
            sources.add(new ConfigConfigSource(configName));
            sources.add(new ServerConfigSource(serverName));
            sources.add(new ApplicationConfigSource(appName));
            sources.add(new ModuleConfigSource(appName, moduleName));
            for (Properties props : configService.getDeployedApplicationProperties(appName)) {
                sources.add(new PropertiesConfigSource(props, appName));
            }
            result = new PayaraConfig(sources);
            registeredConfigs.put(Thread.currentThread().getContextClassLoader(), result);
        }
        return result;
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        Config result = registeredConfigs.get(loader);
        if (result == null) {
            // TBD search the application registry for the given classloader
            
            
            ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
            String appName = currentInvocation.getAppName();
            String moduleName = currentInvocation.getModuleName();
            String serverName = context.getInstanceName();
            String configName = context.getConfigBean().getConfig().getName();
            // build config hierachy
            SortedSet<ConfigSource> sources = new TreeSet();
            sources.add(new DomainConfigSource());
            sources.add(new ClusterConfigSource());
            sources.add(new ConfigConfigSource(configName));
            sources.add(new ServerConfigSource(serverName));
            sources.add(new ApplicationConfigSource(appName));
            sources.add(new ModuleConfigSource(appName, moduleName));
            for (Properties props : configService.getDeployedApplicationProperties(appName)) {
                sources.add(new PropertiesConfigSource(props, appName));
            }
            
        }
        return result;
    }

    @Override
    public ConfigBuilder getBuilder() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void releaseConfig(Config config) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
