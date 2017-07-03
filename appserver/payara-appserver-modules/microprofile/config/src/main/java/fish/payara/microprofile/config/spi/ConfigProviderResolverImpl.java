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
import fish.payara.microprofile.config.source.EnvironmentConfigSource;
import fish.payara.microprofile.config.source.ModuleConfigSource;
import fish.payara.microprofile.config.source.PropertiesConfigSource;
import fish.payara.microprofile.config.source.ServerConfigSource;
import fish.payara.microprofile.config.source.SystemPropertyConfigSource;
import fish.payara.nucleus.microprofile.config.service.MicroprofileConfigService;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "microprofile-config-provider") // this specifies that the classis an HK2 service
@RunLevel(StartupRunLevel.VAL) 
public class ConfigProviderResolverImpl extends ConfigProviderResolver {

    private static final String METADATA_KEY = "MICROPROFILE_APP_CONFIG";
    
    @Inject
    private MicroprofileConfigService configService;
    
    @Inject
    private InvocationManager invocationManager;
    
    @Inject
    private  ServerContext context;
    
    @Inject
    private ApplicationRegistry appRegistry;

    public ConfigProviderResolverImpl() {
    }
    
    @PostConstruct
    public void postConstruct() {
      ConfigProviderResolver.setInstance(this);
    }

    @Override
    public Config getConfig() {
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        String appName = currentInvocation.getAppName();
        ApplicationInfo info = appRegistry.get(appName);
        Config result = info.getTransientAppMetaData(METADATA_KEY, Config.class);
        if (result == null) {
            String moduleName = currentInvocation.getModuleName();
            String serverName = context.getInstanceName();
            String configName = context.getConfigBean().getConfig().getName();
            // build config hierachy
            LinkedList<ConfigSource> sources = new LinkedList<>();
            sources.add(new DomainConfigSource());
            sources.add(new ClusterConfigSource());
            sources.add(new ConfigConfigSource(configName));
            sources.add(new ServerConfigSource(serverName));
            sources.add(new ApplicationConfigSource(appName));
            sources.add(new ModuleConfigSource(appName, moduleName));
            sources.add(new EnvironmentConfigSource());
            sources.add(new SystemPropertyConfigSource());
            for (Properties props : configService.getDeployedApplicationProperties(appName)) {
                sources.add(new PropertiesConfigSource(props, appName));
            }
            result = new PayaraConfig(sources);
            info.addTransientAppMetaData(METADATA_KEY, result);
        }
        return result;
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        Config result = null;
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        String appName = currentInvocation.getAppName();
        ApplicationInfo appInfo = appRegistry.get(appName);
        if (appInfo != null && appInfo.getClassLoaders().contains(loader)) {
            result = appInfo.getTransientAppMetaData(appName, Config.class);
            if (result == null) {
                String moduleName = currentInvocation.getModuleName();
                String serverName = context.getInstanceName();
                String configName = context.getConfigBean().getConfig().getName();
                // build config hierachy
                LinkedList<ConfigSource> sources = new LinkedList<>();
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
