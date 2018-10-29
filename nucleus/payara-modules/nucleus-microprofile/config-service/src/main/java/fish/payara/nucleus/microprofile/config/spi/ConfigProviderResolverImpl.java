/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.spi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ModuleInfo;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import fish.payara.nucleus.microprofile.config.converters.BooleanConverter;
import fish.payara.nucleus.microprofile.config.converters.ClassConverter;
import fish.payara.nucleus.microprofile.config.converters.DoubleConverter;
import fish.payara.nucleus.microprofile.config.converters.FloatConverter;
import fish.payara.nucleus.microprofile.config.converters.InetAddressConverter;
import fish.payara.nucleus.microprofile.config.converters.IntegerConverter;
import fish.payara.nucleus.microprofile.config.converters.LongConverter;
import fish.payara.nucleus.microprofile.config.converters.StringConverter;
import fish.payara.nucleus.microprofile.config.source.ApplicationConfigSource;
import fish.payara.nucleus.microprofile.config.source.ClusterConfigSource;
import fish.payara.nucleus.microprofile.config.source.ConfigConfigSource;
import fish.payara.nucleus.microprofile.config.source.DomainConfigSource;
import fish.payara.nucleus.microprofile.config.source.EnvironmentConfigSource;
import fish.payara.nucleus.microprofile.config.source.JNDIConfigSource;
import fish.payara.nucleus.microprofile.config.source.ModuleConfigSource;
import fish.payara.nucleus.microprofile.config.source.PasswordAliasConfigSource;
import fish.payara.nucleus.microprofile.config.source.PayaraServerProperties;
import fish.payara.nucleus.microprofile.config.source.PropertiesConfigSource;
import fish.payara.nucleus.microprofile.config.source.SecretsDirConfigSource;
import fish.payara.nucleus.microprofile.config.source.ServerConfigSource;
import fish.payara.nucleus.microprofile.config.source.SystemPropertyConfigSource;

/**
 * This Service implements the Microprofile Config API and provides integration
 * into the guts of Payara Server.
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "microprofile-config-provider") // this specifies that the classis an HK2 service
@RunLevel(StartupRunLevel.VAL)
public class ConfigProviderResolverImpl extends ConfigProviderResolver {

    private static final String METADATA_KEY = "MICROPROFILE_APP_CONFIG";
    private static final String CUSTOM_SOURCES_KEY = "MICROPROFILE_CUSTOM_SOURCES";
    private static final String CUSTOM_CONVERTERS_KEY = "MICROPROFILE_CUSTOM_CONVERTERS";
    private final static String APP_METADATA_KEY = "payara.microprofile.config";

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private ServerContext context;

    // Gives access to deployed applications
    @Inject
    ApplicationRegistry applicationRegistry;
    
    // This injects the configuration from the domain.xml magically
    // and for the correct server configuation
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional // PAYARA-2255 make optional due to race condition writing a missing entry into domain.xml
    MicroprofileConfigConfiguration configuration;

    // a config used at the server level when there is no application associated with the thread
    private Config serverLevelConfig;

    public ConfigProviderResolverImpl() {
    }

    @PostConstruct
    public void postConstruct() {
        ConfigProviderResolver.setInstance(this);
    }


    public MicroprofileConfigConfiguration getMPConfig() {
        if (configuration == null) {
            configuration = context.getConfigBean().getConfig().getExtensionByType(MicroprofileConfigConfiguration.class);
        }
        return configuration;
    }

    @Override
    public Config getConfig() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    ApplicationInfo getAppInfo(ClassLoader loader) {
        ApplicationInfo appInfo = null;
        // fast check against current app
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        if (currentInvocation == null) {
            // OK we are not a normal request see if we can find the app name from the 
            // app registry via the classloader
            Set<String> allApplicationNames = applicationRegistry.getAllApplicationNames();
            for (String allApplicationName : allApplicationNames) {
                ApplicationInfo testInfo = applicationRegistry.get(allApplicationName);
                if (loader.equals(testInfo.getAppClassLoader())) {
                    appInfo = testInfo;
                    return appInfo;
                } else {
                    // search the modules within the app info to see if they have the classloader
                    for (ModuleInfo mi : testInfo.getModuleInfos()) {
                        if (loader.equals(mi.getModuleClassLoader())) {
                            return testInfo;
                        }
                    }
                }
            }
        } else {
            String appName = currentInvocation.getAppName();
            appInfo = applicationRegistry.get(appName);
        }

        if (appInfo != null && appInfo.getAppClassLoader().equals(loader)) {
            return appInfo;
        }

        // search the modules
        if (appInfo != null) {
            for (ModuleInfo mInfo : appInfo.getModuleInfos()) {
                if (loader.equals(mInfo.getModuleClassLoader())) {
                    return appInfo;
                }
            }
        }

        // fast check fails search the app registry 
        for (String name : applicationRegistry.getAllApplicationNames()) {
            ApplicationInfo testInfo = applicationRegistry.get(name);
            if (testInfo.getClassLoaders().contains(loader) ||
                    // when loading an application, no class loader is assigned
                    (testInfo.getAppClassLoader() != null && testInfo.getAppClassLoader().equals(loader))) {
                return testInfo;
            }
        }
        return appInfo;
    }

    Config getConfig(ApplicationInfo appInfo) {
        Config result;
        // manage server level config first
        if (appInfo == null) {
            result = serverLevelConfig;
            if (result == null) {
                LinkedList<ConfigSource> sources = new LinkedList<>();
                Map<Type, Converter> converters = new HashMap<>();
                sources.addAll(getDefaultSources());
                converters.putAll(getDefaultConverters());
                serverLevelConfig = new PayaraConfig(sources, converters);
                result = serverLevelConfig;
            }
        } else { // look for an application specific one
            result = appInfo.getTransientAppMetaData(METADATA_KEY, Config.class);
            if (result == null) {
                // build an application specific configuration
                initialiseApplicationConfig(appInfo);
                LinkedList<ConfigSource> sources = new LinkedList<>();
                Map<Type, Converter> converters = new HashMap<>();
                sources.addAll(getDefaultSources(appInfo));
                sources.addAll(getDiscoveredSources(appInfo));
                converters.putAll(getDefaultConverters());
                converters.putAll(getDiscoveredConverters(appInfo));
                result = new PayaraConfig(sources, converters);
                appInfo.addTransientAppMetaData(METADATA_KEY, result);
            }
        }
        return result;
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return getConfig(getAppInfo(loader));
    }

    @Override
    public ConfigBuilder getBuilder() {
        return new PayaraConfigBuilder(this);
    }

    Config getNamedConfig(String applicationName) {
        Config result = null;
        ApplicationInfo info = applicationRegistry.get(applicationName);
        if (info != null) {
            result = info.getTransientAppMetaData(METADATA_KEY, Config.class);
            if (result == null) {
                // rebuild it form scratch
                result = getConfig(info);
            }
        }
        return result;
    }

    private List<ConfigSource> getDefaultSources(String appName, String moduleName) {
        LinkedList<ConfigSource> sources = new LinkedList<>();
        String serverName = context.getInstanceName();
        String configName = context.getConfigBean().getConfig().getName();
        sources.add(new DomainConfigSource());
        sources.add(new ClusterConfigSource());
        sources.add(new ConfigConfigSource(configName));
        sources.add(new ServerConfigSource(serverName));
        sources.add(new EnvironmentConfigSource());
        sources.add(new SystemPropertyConfigSource());
        sources.add(new JNDIConfigSource());
        sources.add(new PayaraServerProperties());
        sources.add(new SecretsDirConfigSource());
        sources.add(new PasswordAliasConfigSource());
        if (appName != null) {
            sources.add(new ApplicationConfigSource(appName));
            sources.add(new ModuleConfigSource(appName, moduleName));
            for (Properties props : getDeployedApplicationProperties(appName)) {
                sources.add(new PropertiesConfigSource(props, appName));
            }
        }
        return sources;
    }
    List<ConfigSource> getDefaultSources() {
        return getDefaultSources(null);
    }

    List<ConfigSource> getDefaultSources(ApplicationInfo appInfo) {
        String appName = null;
        String moduleName = null;
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        if (currentInvocation == null) {
            if (appInfo == null) {
                appInfo = getAppInfo(Thread.currentThread().getContextClassLoader());
            }
            if (appInfo != null) {
                appName = appInfo.getName();
                moduleName = appName;
            }
        } else {
            appName = currentInvocation.getAppName();
            moduleName = currentInvocation.getModuleName();
        }
        return getDefaultSources(appName, moduleName);
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        ApplicationInfo appInfo = getAppInfo(classLoader);
        appInfo.addTransientAppMetaData(METADATA_KEY, config);
    }

    @Override
    public void releaseConfig(Config config) {
        ApplicationInfo appInfo = getAppInfo(Thread.currentThread().getContextClassLoader());
        appInfo.removeTransientAppMetaData(METADATA_KEY);

    }

    public List<Properties> getDeployedApplicationProperties(String applicationName) {
        ApplicationInfo info = applicationRegistry.get(applicationName);
        List<Properties> result = Collections.EMPTY_LIST;
        if (info != null) {
            List<Properties> transientAppMetaData = info.getTransientAppMetaData(APP_METADATA_KEY, LinkedList.class);
            if (transientAppMetaData != null) {
                result = transientAppMetaData;
            }
        }
        return result;
    }

    public String getDeployedApplicationProperty(String applicationName, String name) {
        String result = null;
        ApplicationInfo info = applicationRegistry.get(applicationName);
        if (info != null) {
            LinkedList<Properties> metadata = info.getTransientAppMetaData(APP_METADATA_KEY, LinkedList.class);
            if (metadata != null) {
                for (Properties properties : metadata) {
                    result = properties.getProperty(name);
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    List<ConfigSource> getDiscoveredSources(ApplicationInfo appInfo) {

        LinkedList<ConfigSource> sources = appInfo.getTransientAppMetaData(CUSTOM_SOURCES_KEY, LinkedList.class);
        if (sources == null) {
            sources = new LinkedList<>();
            // resolve custom config sources
            ServiceLoader<ConfigSource> serviceLoader = ServiceLoader.load(ConfigSource.class, appInfo.getAppClassLoader());
            for (ConfigSource configSource : serviceLoader) {
                sources.add(configSource);
            }

            //
            ServiceLoader<ConfigSourceProvider> serviceProvideLoader = ServiceLoader.load(ConfigSourceProvider.class, appInfo.getAppClassLoader());
            for (ConfigSourceProvider configSourceProvider : serviceProvideLoader) {
                Iterable<ConfigSource> configSources = configSourceProvider.getConfigSources(appInfo.getAppClassLoader());
                for (ConfigSource configSource : configSources) {
                    sources.add(configSource);
                }
            }
            appInfo.addTransientAppMetaData(CUSTOM_SOURCES_KEY, sources);
        }
        return sources;
    }

    Map<Type,Converter> getDefaultConverters() {
        Map<Type,Converter> result = new HashMap<>();
        result.put(Boolean.class, new BooleanConverter());
        result.put(Integer.class, new IntegerConverter());
        result.put(Long.class, new LongConverter());
        result.put(Float.class, new FloatConverter());
        result.put(Double.class, new DoubleConverter());
        result.put(InetAddress.class, new InetAddressConverter());
        result.put(Class.class, new ClassConverter());
        result.put(String.class, new StringConverter());
        return result;

    }

    Map<Type, Converter> getDiscoveredConverters(ApplicationInfo appInfo) {
        Map<Type, Converter> converters = appInfo.getTransientAppMetaData(CUSTOM_CONVERTERS_KEY, Map.class);
        if (converters == null) {
            converters = new HashMap<>();
            // resolve custom config sources
            ServiceLoader<Converter> serviceLoader = ServiceLoader.load(Converter.class, appInfo.getAppClassLoader());
            for (Converter converter : serviceLoader) {
                Type type = PayaraConfigBuilder.getTypeForConverter(converter);
                if (type != null) {
                    converters.put(type,converter);
                }
            }
            appInfo.addTransientAppMetaData(CUSTOM_CONVERTERS_KEY, converters);
        }
        return converters;
    }
    
    private void initialiseApplicationConfig(ApplicationInfo info) {
        LinkedList<Properties> appConfigProperties = new LinkedList<>();
        info.addTransientAppMetaData(APP_METADATA_KEY, appConfigProperties);
        try {
            // Read application defined properties and add as transient metadata
            appConfigProperties.add(getPropertiesFromFile(info.getAppClassLoader(), "META-INF/microprofile-config.properties"));
            appConfigProperties.add(getPropertiesFromFile(info.getAppClassLoader(), "../../META-INF/microprofile-config.properties"));
        } catch (IOException ex) {
            Logger.getLogger(ConfigProviderResolverImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Properties getPropertiesFromFile(ClassLoader appClassLoader, String fileName) throws IOException {
        // Read application defined properties and add as transient metadata
        Enumeration<URL> resources = appClassLoader.getResources(fileName);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            Properties p = new Properties();
            try (InputStream is = url.openStream()) {
                p.load(url.openStream());
            }
            return p;
        }
        return new Properties();
    }

}
