/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sun.enterprise.config.serverbeans.Config;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import fish.payara.nucleus.microprofile.config.spi.ConfigSourceConfiguration;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

@Service(name = "config-source-extension-handler")
public class ExtensionConfigSourceService implements EventListener, ConfigListener {

    private static final Logger logger = Logger.getLogger(ExtensionConfigSourceService.class.getName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private MicroprofileConfigConfiguration configuration;

    @Inject
    private ServiceLocator locator;

    @Inject
    private Events events;

    @Inject
    private Transactions transactions;

    private final Set<ExtensionConfigSourceHandler> handlers;

    private boolean isInstance;

    public ExtensionConfigSourceService() {
        this.handlers = new HashSet<>();
    }

    @PostConstruct
    void initialize() {
        // Get the config if it's not been injected
        if (configuration == null) {
            configuration = locator.getService(MicroprofileConfigConfiguration.class);
        }

        // Register an event listener
        if (events != null) {
            events.register(this);
        }

        // Is this service running on an instance?
        final ServerEnvironment env = locator.getService(ServerEnvironment.class);
        isInstance = env.isInstance();

        // Populate the config sources list
        List<ServiceHandle<ExtensionConfigSource>> configSourceHandles = locator.getAllServiceHandles(ExtensionConfigSource.class);
        for (ServiceHandle<ExtensionConfigSource> configSourceHandle : configSourceHandles) {
            Class<?> extensionClass = configSourceHandle.getActiveDescriptor().getImplementationClass();
            Class<ConfigSourceConfiguration> configClass = ConfigSourceExtensions.getConfigurationClass(extensionClass);
            if (configClass != null) {
                handlers.add(new ExtensionConfigSourceHandler(configSourceHandle, configClass, configuration.getConfigSourceConfigurationByType(configClass)));
            } else {
                handlers.add(new ExtensionConfigSourceHandler(configSourceHandle));
            }
        }
    }

    @PreDestroy
    void destroy() {
        handlers.clear();

        if (events != null) {
            events.unregister(this);
        }
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapConfigSources();
        }
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutdownConfigSources();
        }
        transactions.addListenerForType(MicroprofileConfigConfiguration.class, this);
    }

    public Set<ExtensionConfigSource> getExtensionSources() {
        Set<ExtensionConfigSource> sources = new HashSet<>();
        for (ExtensionConfigSourceHandler handler : handlers) {
            sources.add(handler.getProxyConfigSource());
        }
        return sources;
    }

    private void bootstrapConfigSources() {
        handlers.forEach(ExtensionConfigSourceHandler::bootstrap);
    }

    private void shutdownConfigSources() {
        handlers.forEach(ExtensionConfigSourceHandler::destroy);
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (isInstance) {
            isCurrentInstanceMatchTarget = true;
        } else {
            for (PropertyChangeEvent pe : events) {
                ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
                while (proxy != null && !(proxy instanceof Config)) {
                    proxy = proxy.getParent();
                }

                if (proxy != null && ((Config) proxy).isDas()) {
                    isCurrentInstanceMatchTarget = true;
                    break;
                }
            }
        }

        if (isCurrentInstanceMatchTarget) {
            return ConfigSupport.sortAndDispatch(events, new Changed() {
                @Override
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {

                    if (changedType.equals(MicroprofileConfigConfiguration.class)) {
                        configuration = (MicroprofileConfigConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }

    public void reconfigure(ConfigSourceConfiguration config) {
        for (ExtensionConfigSourceHandler handler : handlers) {
            Class<ConfigSourceConfiguration> configClass = handler.getConfigClass();
            if (configClass != null && configClass.isAssignableFrom(config.getClass())) {
                handler.reconfigure(config);
                break;
            }
        }
    }

}
