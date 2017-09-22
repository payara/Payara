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
package fish.payara.nucleus.microprofile.config.service;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerTags;
import fish.payara.nucleus.store.ClusteredStore;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.resources.admin.cli.CustomResourceManager;
import org.glassfish.resources.admin.cli.ResourceConstants;
import static org.glassfish.resources.admin.cli.ResourceConstants.JNDI_NAME;
import org.glassfish.resources.config.CustomResource;
import org.glassfish.resourcebase.resources.util.BindableResourcesHelper;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.config.types.Property;

/**
 * Implementation of the internal Payara Service that provides support to
 * Microprofile. In particular this service exposes many internals of the server
 * as configuration values.
 *
 * @author Steve Millidge (Payara Foundation)
 * @since 4.1.2.173
 */
@Service(name = "microprofile-config-service") // this specifies that the classis an HK2 service
@RunLevel(StartupRunLevel.VAL)  // this specifies the servce is created at server boot time
public class MicroprofileConfigService implements EventListener, ConfigListener {

    public final static String CLUSTERED_CONFIG_STORE = "payara.microprofile.config";
    public final static String PROPERTY_PREFIX = "payara.microprofile.";
    public final static String METADATA_KEY = "payara.microprofile.config";
    public final static String ORDINAL_PROPERTY = "config_ordinal";
    public final static String DEFAULT_ORDINAL_VALUE = "100";

    //Provides access to the event manager to hook into server lifecycle events
    // or to raise various events
    @Inject
    private Events events;

    // This gives access to the Hazelcast Based cluster wide in-memory data store
    // this is a bunch of named key-value stores accessible across the Hazelcast Cluster
    @Inject
    private ClusteredStore clusterStore;

    // Gives access to deployed applications
    @Inject
    ApplicationRegistry applicationRegistry;

    // Provides access to information on the server including;
    // command line, initial context, service locator, installation
    // Classloaders, config root for the server
    @Inject
    ServerContext context;

    @Inject
    Domain domainConfiguration;

    @Inject
    private CustomResourceManager customResMgr;

    @Inject
    private BindableResourcesHelper bindableResourcesHelper;

    // This injects the configuration from the domain.xml magically
    // and for the correct server configuation
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    MicroprofileConfigConfiguration configuration;

    // Provides ability to register a configuration listener
    @Inject
    Transactions transactions;

    @PostConstruct
    public void postConstruct() {
        events.register(this);
        transactions.addListenerForType(MicroprofileConfigConfiguration.class, this);
    }

    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_LOADED)) {
            // TO DO look for microprofile config files in the application classloader
            ApplicationInfo info = (ApplicationInfo) event.hook();
            LinkedList<Properties> appConfigProperties = new LinkedList<>();
            info.addTransientAppMetaData(METADATA_KEY, appConfigProperties);
            try {
                Enumeration<URL> resources = info.getAppClassLoader().getResources("META-INF/microprofile-config.properties");
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    Properties p = new Properties();
                    try (InputStream is = url.openStream()) {
                        p.load(url.openStream());
                    }
                    appConfigProperties.add(p);
                }
            } catch (IOException ex) {
                Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] pces) {
        return null;
    }

    public void setClusteredProperty(String name, String value) {
        clusterStore.set(CLUSTERED_CONFIG_STORE, name, value);
    }

    public String getClusteredProperty(String name) {
        return (String) clusterStore.get(CLUSTERED_CONFIG_STORE, name);
    }

    public void setApplicationProperty(String applicationName, final String name, final String value) {
        Application app = domainConfiguration.getApplications().getApplication(applicationName);
        if (app != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<Application>() {
                    @Override
                    public Object run(Application config) throws TransactionFailure, PropertyVetoException {
                        if (config.getProperty(PROPERTY_PREFIX + name) != null) {
                            config.removeProperty(PROPERTY_PREFIX + name);
                        }
                        Property prop = config.createChild(Property.class);
                        prop.setName(PROPERTY_PREFIX + name);
                        prop.setValue(value);
                        config.getProperty().add(prop);
                        return null;
                    }
                }, app);
            } catch (TransactionFailure ex) {
                Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.WARNING, "Unable to set Domain level Microprofile Property " + name, ex);
            }
        }
    }

    public String getModuleProperty(String applicationName, String moduleName, String name) {
        String result = null;
        Application app = domainConfiguration.getApplications().getApplication(applicationName);
        if (app != null) {
            Module m = app.getModule(moduleName);
            if (m != null) {
                result = m.getPropertyValue(PROPERTY_PREFIX + name);
            }
        }
        return result;
    }

    public void setModuleProperty(String applicationName, String moduleName, final String name, final String value) {
        Application app = domainConfiguration.getApplications().getApplication(applicationName);
        if (app != null) {
            Module m = app.getModule(moduleName);
            if (m != null) {
                try {
                    ConfigSupport.apply(new SingleConfigCode<Module>() {
                        @Override
                        public Object run(Module config) throws TransactionFailure, PropertyVetoException {
                            if (config.getProperty(PROPERTY_PREFIX + name) != null) {
                                config.removeProperty(PROPERTY_PREFIX + name);
                            }
                            Property prop = config.createChild(Property.class);
                            prop.setName(PROPERTY_PREFIX + name);
                            prop.setValue(value);
                            config.getProperty().add(prop);
                            return null;
                        }
                    }, m);
                } catch (TransactionFailure ex) {
                    Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.WARNING, "Unable to set Domain level Microprofile Property " + name, ex);
                }
            }
        }
    }

    public String getApplicationProperty(String applicationName, String name) {
        String result = null;
        Application app = domainConfiguration.getApplications().getApplication(applicationName);
        if (app != null) {
            result = app.getPropertyValue(PROPERTY_PREFIX + name);
        }
        return result;
    }

    public void setDomainProperty(final String name, final String value) {
        try {
            ConfigSupport.apply(new SingleConfigCode<Domain>() {
                @Override
                public Object run(Domain config) throws TransactionFailure, PropertyVetoException {
                    if (config.containsProperty(PROPERTY_PREFIX + name)) {
                        config.removeProperty(PROPERTY_PREFIX + name);
                    }
                    Property prop = config.createChild(Property.class);
                    prop.setName(PROPERTY_PREFIX + name);
                    prop.setValue(value);
                    config.getProperty().add(prop);
                    return null;
                }
            }, domainConfiguration);
        } catch (TransactionFailure ex) {
            Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.WARNING, "Unable to set Domain level Microprofile Property " + name, ex);
        }
    }

    public String getDomainProperty(String name) {
        return domainConfiguration.getPropertyValue(PROPERTY_PREFIX + name);
    }

    public void setConfigProperty(final String configName, final String name, final String value) {
        Config config = domainConfiguration.getConfigs().getConfigByName(configName);
        if (config != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<Config>() {
                    @Override
                    public Object run(Config configuration) throws TransactionFailure, PropertyVetoException {
                        if (configuration.containsProperty(PROPERTY_PREFIX + name)) {
                            configuration.removeProperty(PROPERTY_PREFIX + name);
                        }
                        Property prop = configuration.createChild(Property.class);
                        prop.setName(PROPERTY_PREFIX + name);
                        prop.setValue(value);
                        configuration.getProperty().add(prop);

                        return null;
                    }
                }, config);
            } catch (TransactionFailure ex) {
                Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.WARNING, "Unable to set Domain level Microprofile Property " + name, ex);
            }
        }
    }

    public String getConfigProperty(String configName, String name) {
        String result = null;
        Config config = domainConfiguration.getConfigs().getConfigByName(configName);
        if (config != null) {
            result = config.getPropertyValue(PROPERTY_PREFIX + name);
        }
        return result;
    }

    public void setServerProperty(final String serverName, final String name, final String value) {
        Server config = domainConfiguration.getServerNamed(serverName);
        if (config != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<Server>() {
                    @Override
                    public Object run(Server configuration) throws TransactionFailure, PropertyVetoException {
                        if (configuration.containsProperty(PROPERTY_PREFIX + name)) {
                            configuration.removeProperty(PROPERTY_PREFIX + name);
                        }
                        Property prop = configuration.createChild(Property.class);
                        prop.setName(PROPERTY_PREFIX + name);
                        prop.setValue(value);
                        configuration.getProperty().add(prop);

                        return null;
                    }
                }, config);
            } catch (TransactionFailure ex) {
                Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.WARNING, "Unable to set Domain level Microprofile Property " + name, ex);
            }
        }
    }

    public String getServerProperty(String serverName, String name) {
        String result = null;
        Server config = domainConfiguration.getServerNamed(serverName);
        if (config != null) {
            result = config.getPropertyValue(PROPERTY_PREFIX + name);
        }
        return result;
    }

    public String getSystemProperty(String name) {
        String result;
        result = System.getProperty(name);
        if (result == null) {
            result = context.getConfigBean().getSystemPropertyValue(name);
            if (result == null) {
                result = domainConfiguration.getSystemPropertyValue(name);
            }
        }
        return result;
    }

    public List<Properties> getDeployedApplicationProperties(String applicationName) {
        ApplicationInfo info = applicationRegistry.get(applicationName);
        List<Properties> result = Collections.EMPTY_LIST;
        if (info != null) {
            List<Properties> transientAppMetaData = info.getTransientAppMetaData(METADATA_KEY, LinkedList.class);
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
            LinkedList<Properties> metadata = info.getTransientAppMetaData(METADATA_KEY, LinkedList.class);
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

    public void setJNDIProperty(final String name, final String value, final String target) {

        HashMap attrList = new HashMap();
        attrList.put("factory-class", "org.glassfish.resources.custom.factory.PrimitivesAndStringFactory");
        attrList.put("res-type", "java.lang.String");
        attrList.put(ResourceConstants.ENABLED, Boolean.TRUE.toString());
        attrList.put(JNDI_NAME, name);
        attrList.put(ServerTags.DESCRIPTION, "MicroProfile Config property for " + name);

        Properties props = new Properties();

        props.put("value", value);

        try {
            customResMgr.create(domainConfiguration.getResources(), attrList, props, target);
        } catch (Exception ex) {
            Logger.getLogger(MicroprofileConfigService.class.getName()).log(Level.WARNING, "Unable to set MicroProfile Config property " + name, ex);
        }
    }

    public String getJNDIProperty(String name, String target) {
        Collection<CustomResource> customResources
                = domainConfiguration.getResources().getResources(CustomResource.class);
        for (CustomResource customResource : customResources) {
            if (bindableResourcesHelper.resourceExists(customResource.getJndiName(), target)) {
                if (customResource.getJndiName().equals(name)) {
                    return customResource.getPropertyValue("value");
                }
            }
        }
        return null;
    }

    public String getEnvironmentVariable(String name) {
        return System.getenv(name);
    }

    public MicroprofileConfigConfiguration getConfig() {
        return configuration;
    }

    public Map<String, String> getEnvironmentPropertyMap() {
        return System.getenv();
    }

    public Map<String, String> getSystemPropertiesMap() {
        Properties props = System.getProperties();
        HashMap<String, String> result = new HashMap<>(props.size());
        for (String propertyName : props.stringPropertyNames()) {
            result.put(propertyName, props.getProperty(propertyName));
        }
        return result;
    }

    public Map<String, String> getDomainProperyMap() {
        List<Property> properties = domainConfiguration.getProperty();
        HashMap<String, String> result = new HashMap<>(properties.size());
        for (Property property : properties) {
            if (property.getName().startsWith(PROPERTY_PREFIX)) {
                result.put(property.getName().substring(PROPERTY_PREFIX.length()), property.getValue());
            }
        }
        return result;
    }

    public Map<String, String> getConfigPropertyMap(String configName) {
        Config config = domainConfiguration.getConfigNamed(configName);
        HashMap<String, String> result = new HashMap<>();
        if (config != null) {
            List<Property> properties = config.getProperty();
            for (Property property : properties) {
                if (property.getName().startsWith(PROPERTY_PREFIX)) {
                    result.put(property.getName().substring(PROPERTY_PREFIX.length()), property.getValue());
                }
            }
        }
        return result;
    }

    public Map<String, String> getServerPropertyMap(String configName) {
        Server config = domainConfiguration.getServerNamed(configName);
        HashMap<String, String> result = new HashMap<>();
        if (config != null) {
            List<Property> properties = config.getProperty();
            for (Property property : properties) {
                if (property.getName().startsWith(PROPERTY_PREFIX)) {
                    result.put(property.getName().substring(PROPERTY_PREFIX.length()), property.getValue());
                }
            }
        }
        return result;
    }

    public Map<String, String> getApplicationPropertyMap(String configName) {
        Application config = domainConfiguration.getApplications().getApplication(configName);
        HashMap<String, String> result = new HashMap<>();
        if (config != null) {
            List<Property> properties = config.getProperty();
            for (Property property : properties) {
                if (property.getName().startsWith(PROPERTY_PREFIX)) {
                    result.put(property.getName().substring(PROPERTY_PREFIX.length()), property.getValue());
                }
            }
        }
        return result;
    }

    public Map<String, String> getModulePropertyMap(String configName, String moduleName) {
        Application config = domainConfiguration.getApplications().getApplication(configName);
        HashMap<String, String> result = new HashMap<>();
        if (config != null) {
            Module module = config.getModule(moduleName);
            if (module != null) {
                List<Property> properties = module.getProperty();
                for (Property property : properties) {
                    if (property.getName().startsWith(PROPERTY_PREFIX)) {
                        result.put(property.getName().substring(PROPERTY_PREFIX.length()), property.getValue());
                    }
                }
            }
        }
        return result;
    }

    public Map<String, String> getClusteredPropertyMap() {
        Map<Serializable, Serializable> map = clusterStore.getMap(CLUSTERED_CONFIG_STORE);
        HashMap<String, String> result = new HashMap<>();
        for (Serializable key : map.keySet()) {
            result.put((String) key, (String) map.get(key));
        }
        return result;
    }

}
