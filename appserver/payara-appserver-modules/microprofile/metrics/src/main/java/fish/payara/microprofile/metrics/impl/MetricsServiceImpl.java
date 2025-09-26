/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2018-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.metrics.impl;

import static java.util.Collections.unmodifiableSet;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXB;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.admin.MetricsServiceConfiguration;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.jmx.MetricsMetadata;
import fish.payara.microprofile.metrics.jmx.MetricsMetadataConfig;
import fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.healthcheck.HealthCheckService;

@Service(name = "microprofile-metrics-service")
@RunLevel(StartupRunLevel.VAL)
public class MetricsServiceImpl implements MetricsService, ConfigListener {

    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Inject
    private HealthCheckService healthCheckService;

    @Inject
    private MetricsServiceConfiguration configuration;

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    private MetricsMetadataHelper helper;

    private MetricsServiceConfiguration metricsServiceConfiguration;

    private Boolean metricsEnabled;

    private Boolean metricsSecure;

    private List<MetricsMetadata> unresolvedBaseMetadataList;

    private List<MetricsMetadata> unresolvedVendorMetadataList;

    private static final class RegisteredMetric {

        final String scope;
        final MetricID id;

        RegisteredMetric(String scope, MetricID metric) {
            this.scope = scope;
            this.id = metric;
        }
    }

    /**
     * A record per registry so that any registry related state can be created or removed consistently to avoid memory
     * leaks or complicated cleanup work of related state that can be disposed in connection with the registry.
     */
    private static final class MetricsContextImpl implements MetricsContext, MetricRegistrationListener {

        private final String name;
        private final MetricRegistryImpl base;
        private final MetricRegistryImpl vendor;
        private final MetricRegistryImpl application;

        private final ConcurrentMap<String, MetricRegistry> registries = new ConcurrentHashMap<>();
        private final Queue<RegisteredMetric> newlyRegistered = new ConcurrentLinkedQueue<>();

        public MetricsContextImpl(String name) {
            this.name = name;
            this.base = (MetricRegistryImpl) getOrCreateRegistry(MetricRegistry.BASE_SCOPE);
            this.vendor = (MetricRegistryImpl)getOrCreateRegistry(MetricRegistry.VENDOR_SCOPE);
            this.application = isServerContext() ? null : (MetricRegistryImpl)getOrCreateRegistry(MetricRegistry.APPLICATION_SCOPE);
            base.addListener(this);
            vendor.addListener(this);
            if (application != null)
                application.addListener(this);
        }

        @Override
        public String getName() {
            return name;
        }

        public MetricRegistry add(String name, MetricRegistry registry) {
            return registries.putIfAbsent(name, registry);
        }

        @Override
        public MetricRegistry getOrCreateRegistry(String registryName) throws NoSuchRegistryException {
            MetricRegistry registry = registries.get(registryName);
            if(registry == null) {
                MetricRegistry created = new MetricRegistryImpl(registryName);
                MetricRegistry referenced = add(registryName, created);
                if(referenced == null) {
                    return created;
                }
                return referenced;
            }
            return registry;
        }

        @Override
        public void onRegistration(MetricID registered, String scope) {
            String type = null;
            if(scope.equals(MetricRegistry.BASE_SCOPE)) {
                type = MetricRegistry.BASE_SCOPE;
            }

            if(scope.equals(MetricRegistry.VENDOR_SCOPE)) {
                type = MetricRegistry.VENDOR_SCOPE;
            }

            if(scope.equals(MetricRegistry.APPLICATION_SCOPE)) {
                type = MetricRegistry.APPLICATION_SCOPE;
            }

            newlyRegistered.add(new RegisteredMetric(type, registered));
        }

        RegisteredMetric pollNewlyRegistered() {
            return newlyRegistered.poll();
        }

        @Override
        public ConcurrentMap<String, MetricRegistry> getRegistries() {
            return registries;
        }
    }

    private final Map<String, MetricsContextImpl> contextByName = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        metricsServiceConfiguration = serviceLocator.getService(MetricsServiceConfiguration.class);
        // Only start if metrics are enabled
        if (isEnabled()) {
            PayaraExecutorService payaraExecutor = serviceLocator.getService(PayaraExecutorService.class, new Annotation[0]);
            payaraExecutor.submit(this::bootstrap);
        }
    }

    @Override
    public Set<String> getContextNames() {
        return unmodifiableSet(contextByName.keySet());
    }

    @Override
    public MetricsContext getContext(boolean createIfNotExists) {
        if (!isEnabled()) {
            return null;
        }
        String name = appKeyOf(getApplicationName());
        if (!createIfNotExists) {
            return contextByName.get(name);
        }
        return contextByName.computeIfAbsent(name, key -> new MetricsContextImpl(key));
    }

    @Override
    public MetricsContext getContext(String name) {
        if (!isEnabled()) {
            return null;
        }
        name = appKeyOf(name);
        if (MetricsContext.SERVER_CONTEXT_NAME.equals(name)) {
            return contextByName.computeIfAbsent(name, key -> new MetricsContextImpl(key));
        }
        return contextByName.get(name);
    }

    private static void checkSystemCpuLoadIssue(MetricsMetadataConfig metadataConfig) {
        // Could be constant but placed it in method as it is a workaround until fixed in JVM.
        // TODO Make this check dependent on the JDK version (as it hopefully will get solved in the future) -> Azul fix request made.
        String mbeanSystemCPULoad = "java.lang:type=OperatingSystem/SystemCpuLoad";

        long count = metadataConfig.getBaseMetadata().stream()
                .map(MetricsMetadata::getMBean)
                .filter(mbeanSystemCPULoad::equalsIgnoreCase)
                .count();

        count += metadataConfig.getVendorMetadata().stream()
                .map(MetricsMetadata::getMBean)
                .filter(mbeanSystemCPULoad::equalsIgnoreCase)
                .count();

        if (count > 1) {
            LOGGER.warning(String.format("Referencing the MBean value %s multiple times possibly leads to inconsistent values for the MBean value.", mbeanSystemCPULoad));
        }
    }

    /**
     * Initialize metrics from the metrics.xml containing the base & vendor
     * metrics metadata.
     *
     */
    private void initMetadataConfig(List<MetricsMetadata> baseMetadataList, List<MetricsMetadata> vendorMetadataList, boolean isRetry) {
        if (!baseMetadataList.isEmpty()) {
            unresolvedBaseMetadataList = helper.registerMetadata(
                    getContext(MetricsContext.SERVER_CONTEXT_NAME).getBaseRegistry(),
                    baseMetadataList,
                    isRetry);
        }
        if (!vendorMetadataList.isEmpty()) {
            unresolvedVendorMetadataList = helper.registerMetadata(
                    getContext(MetricsContext.SERVER_CONTEXT_NAME).getVendorRegistry(),
                    vendorMetadataList,
                    isRetry);
        }
    }

    /**
     * Registers unresolved MBeans if they have been started after the metrics
     * service.
     */
    @Override
    public void refresh() {
        // Initialise the metadata lists if they haven't yet
        if (unresolvedBaseMetadataList == null || unresolvedVendorMetadataList == null) {
            bootstrap();
        } else {
            initMetadataConfig(unresolvedBaseMetadataList, unresolvedVendorMetadataList, true);
        }
    }

    private MetricsMetadataConfig getConfig() {
        InputStream defaultConfig = MetricsService.class.getResourceAsStream("/metrics.xml");
        MetricsMetadataConfig config = JAXB.unmarshal(defaultConfig, MetricsMetadataConfig.class);

        File metricsResource = new File(serverEnv.getConfigDirPath(), "metrics.xml");
        if (metricsResource.exists()) {
            try {
                InputStream userMetrics = new FileInputStream(metricsResource);
                MetricsMetadataConfig extraConfig = JAXB.unmarshal(userMetrics, MetricsMetadataConfig.class);
                config.addBaseMetadata(extraConfig.getBaseMetadata());
                config.addVendorMetadata(extraConfig.getVendorMetadata());
            } catch (FileNotFoundException ex) {
                //ignore
            }
        }
        return config;
    }

    @Override
    public boolean isEnabled() {
        if (metricsEnabled == null) {
            metricsEnabled = Boolean.valueOf(metricsServiceConfiguration.getEnabled());
        }
        return metricsEnabled;
    }

    public void resetMetricsEnabledProperty() {
        metricsEnabled = null;
    }

    public Boolean isMetricsSecure() {
        if (metricsSecure == null) {
            metricsSecure = Boolean.valueOf(metricsServiceConfiguration.getSecureMetrics());
        }
        return metricsSecure;
    }

    public void resetMetricsSecureProperty() {
        metricsSecure = null;
    }

    public boolean isSecurityEnabled() {
        return Boolean.parseBoolean(metricsServiceConfiguration.getSecurityEnabled());
    }

    /**
     * Adds an application to the enabled map
     *
     * @param applicationName The name of the application to create
     */
    public void registerApplication(String applicationName) {
        // creation is lazy
    }

    /**
     * Removes an application from the enabled map
     *
     * @param applicationName The name of the application to remove
     */
    public void deregisterApplication(String applicationName) {
        contextByName.remove(appKeyOf(applicationName));
    }

    private static String appKeyOf(String name) {
        return name == null ? null : name.toLowerCase();
    }

    /**
     * Gets the application name from the invocation manager.
     *
     * @return The application name
     */
    public String getApplicationName() {
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        ComponentInvocation current = invocationManager.getCurrentInvocation();
        if (current == null) {
            return invocationManager.peekAppEnvironment().getName();
        }
        return current.getAppName();
    }

    private void bootstrap() {
        MetricsMetadataConfig metadataConfig = getConfig();
        checkSystemCpuLoadIssue(metadataConfig); // PAYARA 2938
        //removing processing of base metrics
        initMetadataConfig(metadataConfig.getBaseMetadata(), metadataConfig.getVendorMetadata(), false);
    }
    
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unchangedList = new ArrayList<>();
        for(PropertyChangeEvent event : events) {
                unchangedList.add(new UnprocessedChangeEvent(event, "Microprofile Metrics configuration changed:" + event.getPropertyName()
                        + " was changed from " + event.getOldValue().toString() + " to " + event.getNewValue().toString()));
            }
        return new UnprocessedChangeEvents(unchangedList);
    }
}
