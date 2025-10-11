/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXB;

import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Timer;
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

import fish.payara.microprofile.metrics.MetricUnitsUtils;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.admin.MetricsServiceConfiguration;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.jmx.MetricsMetadata;
import fish.payara.microprofile.metrics.jmx.MetricsMetadataConfig;
import fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.HealthCheckStatsProvider;
import java.util.logging.Level;

@Service(name = "microprofile-metrics-service")
@RunLevel(StartupRunLevel.VAL)
public class MetricsServiceImpl implements MetricsService, ConfigListener, MonitoringDataSource {

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

    @Override
    public void collect(MonitoringDataCollector rootCollector) {
        if (!isEnabled())
            return;
        MonitoringDataCollector metricsCollector = rootCollector.in("metric");
        for (MetricsContextImpl context : contextByName.values()) {
            processMetadataToAnnotations(context, metricsCollector);
            String contextName = context.getName();
            collectRegistry(contextName, context.getBaseRegistry(), metricsCollector);
            collectRegistry(contextName, context.getVendorRegistry(), metricsCollector);
            if (!context.isServerContext()) {
                collectRegistry(contextName, context.getApplicationRegistry(), metricsCollector);
            }
        }
    }

    private void collectRegistry(String contextName, MetricRegistry registry, MonitoringDataCollector collector) {

        // OBS: this way of iterating the metrics in the registry is optimal because of its internal data organisation
        for (String name : registry.getNames()) {
            for (Entry<MetricID, Metric> entry : ((MetricRegistryImpl) registry).getMetrics(name).entrySet()) {
                MetricID metricID = entry.getKey();
                Metric metric = entry.getValue();
                try {
                    MonitoringDataCollector metricCollector = tagCollector(contextName, metricID, collector);
                    if (metric instanceof HealthCheckStatsProvider
                            && (!((HealthCheckStatsProvider) metric).isEnabled() || !healthCheckService.isEnabled())) {
                        continue;
                    }
                    if (metric instanceof Counting) {
                        metricCollector.collect(toName(metricID, "Count"), ((Counting) metric).getCount());
                    }
                    if (metric instanceof Timer) {
                        metricCollector.collect(toName(metricID, "MaxDuration"), ((Timer) metric).getSnapshot().getMax());
                    }
                    if (metric instanceof Gauge) { 
                        Object value = ((Gauge<?>) metric).getValue();
                        if (value instanceof Number) {
                            metricCollector.collect(toName(metricID,
                                    getMetricUnitSuffix(registry.getMetadata(name).unit())), ((Number) value));
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to retrieve metric: {0}", metricID);
                }
            }
        }
    }

    private static void processMetadataToAnnotations(MetricsContextImpl context, MonitoringDataCollector collector) {
       RegisteredMetric metric = context.pollNewlyRegistered();
        while (metric != null) {
            MetricID metricID = metric.id;
            MetricRegistry registry = context.getOrCreateRegistry(metric.scope);
            MonitoringDataCollector metricCollector = tagCollector(context.getName(), metricID, collector);
            Metadata metadata = registry.getMetadata(metricID.getName());
            String suffix = "Count";
            String property = "Count";
            boolean isGauge = metadata.getName() == Gauge.class.getName();
            if (isGauge) {
                suffix = getMetricUnitSuffix(metadata.unit());
                property = "Value";
            }
            // Note that by convention an annotation with value 0 done before the series collected any value is considered permanent
            metricCollector.annotate(toName(metricID, suffix), 0, false,
                    metadataToAnnotations(context.getName(), metric.scope, metadata, property));
            metric = context.pollNewlyRegistered();
        }
    }

    private static String getMetricUnitSuffix(Optional<String> unit) {
        if (!unit.isPresent()) {
            return "";
        }
        String value = unit.get();
        if (MetricUnits.NONE.equalsIgnoreCase(value) || value.isEmpty()) {
            return "";
        }
        return toFirstLetterUpperCase(value);
    }

    private static String toFirstLetterUpperCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String[] metadataToAnnotations(String contextName, String scope, Metadata metadata, String property) {
        String unit = metadata.unit().orElse(MetricUnits.NONE);
        return new String[] {
                "App", contextName, //
                "Scope", scope, //
                "Name", metadata.getName(), //
                "Unit", unit, //
                "Description", metadata.getDescription(), //
                "Property", property, //
                "BaseUnit", MetricUnitsUtils.baseUnit(unit), //
                "ScaleToBaseUnit", MetricUnitsUtils.scaleToBaseUnit(1L, unit).toString()
        };
    }

    private static String toName(MetricID metric, String suffix) {
        String name = metric.getName();
        if (name.indexOf(' ') >= 0) { // trying to avoid replace
            name = name.replace(' ', '_');
        } else {
            int dotIndex = name.indexOf('.');
            if (dotIndex > 0) {
                name = name.substring(dotIndex + 1);
            }
            if (name.indexOf('.') > 0) {
                String[] words = name.split("\\.");
                StringBuilder nameBuilder = new StringBuilder();
                for (String word : words) {
                    nameBuilder.append(toFirstLetterUpperCase(word));
                }
                name += nameBuilder.toString();
            }
        }
        name = toFirstLetterUpperCase(name);
        return name.endsWith(suffix) || suffix.isEmpty() ? name : name + suffix;
    }

    private static MonitoringDataCollector tagCollector(String contextName, MetricID metric, MonitoringDataCollector collector) {
        Map<String, String> tags = metric.getTags();
        if (tags.isEmpty()) {
            String name = metric.getName();
            int dotIndex = name.indexOf('.');
            if (dotIndex > 0 ) {
                String firstWord = name.substring(0, dotIndex);
                return collector.group(firstWord);
            }
            return contextName.isEmpty() ? collector : collector.group(contextName);
        }
        StringBuilder tag = new StringBuilder();
        if (!contextName.isEmpty()) {
            tag.append(contextName).append('_');
        }
        for (Entry<String, String> e : metric.getTags().entrySet()) {
            if (tag.length() > 0) {
                tag.append('_');
            }
            if (!"name".equals(e.getKey())) {
                tag.append(e.getKey().replace(' ', '_'));
            }
            tag.append(e.getValue().replace(' ', '_'));
        }
        return collector.group(tag);
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
