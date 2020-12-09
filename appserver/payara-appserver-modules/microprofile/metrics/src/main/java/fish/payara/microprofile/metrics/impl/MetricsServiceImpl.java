/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
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
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
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
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXB;

import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
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
import fish.payara.microprofile.metrics.jmx.MBeanMetadata;
import fish.payara.microprofile.metrics.jmx.MBeanMetadataConfig;
import fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.nucleus.executorservice.PayaraExecutorService;

@Service(name = "microprofile-metrics-service")
@RunLevel(StartupRunLevel.VAL)
public class MetricsServiceImpl implements MetricsService, ConfigListener, MonitoringDataSource {

    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Inject
    MetricsServiceConfiguration configuration;

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    private MBeanMetadataHelper helper;

    private MetricsServiceConfiguration metricsServiceConfiguration;

    private Boolean metricsEnabled;

    private Boolean metricsSecure;

    private List<MBeanMetadata> unresolvedBaseMetadataList;

    private List<MBeanMetadata> unresolvedVendorMetadataList;

    private static final class RegisteredMetric {

        final Type scope;
        final MetricID id;

        RegisteredMetric(Type scope, MetricID metric) {
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
        private final Queue<RegisteredMetric> newlyRegistered = new ConcurrentLinkedQueue<>();

        public MetricsContextImpl(String name) {
            this.name = name;
            this.base = new MetricRegistryImpl(BASE);
            this.vendor = new MetricRegistryImpl(Type.VENDOR);
            this.application = isServerContext() ? null : new MetricRegistryImpl(Type.APPLICATION);
            base.addListener(this);
            vendor.addListener(this);
            if (application != null)
                application.addListener(this);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public MetricRegistryImpl getRegistry(Type type) throws NoSuchRegistryException {
            switch (type) {
            case BASE: return base;
            case VENDOR: return vendor;
            case APPLICATION:
            default:
                if (isServerContext()) {
                    throw new NoSuchRegistryException("Server context does not have an application registry");
                }
                return application;
            }
        }

        @Override
        public void onRegistration(MetricID registered, MetricRegistry registry) {
            newlyRegistered.add(new RegisteredMetric(registry.getType(), registered));
        }

        RegisteredMetric pollNewlyRegistered() {
            return newlyRegistered.poll();
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

    private static void collectRegistry(String contextName, MetricRegistry registry, MonitoringDataCollector collector) {

        // OBS: this way of iterating the metrics in the registry is optimal because of its internal data organisation
        for (String name : registry.getNames()) {
            for (Entry<MetricID, Metric> entry : ((MetricRegistryImpl) registry).getMetrics(name).entrySet()) {
                MetricID metricID = entry.getKey();
                Metric metric = entry.getValue();
                MonitoringDataCollector metricCollector = tagCollector(contextName, metricID, collector);
                if (metric instanceof Counting) {
                    metricCollector.collect(toName(metricID, "Count"), ((Counting) metric).getCount());
                }
                if (metric instanceof SimpleTimer) {
                    metricCollector.collect(toName(metricID, "Duration"), ((SimpleTimer) metric).getElapsedTime().toMillis());
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
            }
        }
    }

    private static void processMetadataToAnnotations(MetricsContextImpl context, MonitoringDataCollector collector) {
        RegisteredMetric metric = context.pollNewlyRegistered();
        while (metric != null) {
            MetricID metricID = metric.id;
            Type scope = metric.scope;
            MetricRegistry registry = context.getRegistry(scope);
            MonitoringDataCollector metricCollector = tagCollector(context.getName(), metricID, collector);
            Metadata metadata = registry.getMetadata(metricID.getName());
            String suffix = "Count";
            String property = "Count";
            boolean isGauge = metadata.getTypeRaw() == MetricType.GAUGE;
            if (isGauge) {
                suffix = getMetricUnitSuffix(metadata.unit());
                property = "Value";
            }
            // Note that by convention an annotation with value 0 done before the series collected any value is considered permanent
            metricCollector.annotate(toName(metricID, suffix), 0, false,
                    metadataToAnnotations(context.getName(), scope, metadata, property));
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

    private static String[] metadataToAnnotations(String contextName, MetricRegistry.Type scope, Metadata metadata, String property) {
        String unit = metadata.unit().orElse(MetricUnits.NONE);
        return new String[] {
                "App", contextName, //
                "Scope", scope.getName(), //
                "Name", metadata.getName(), //
                "Type", metadata.getType(), //
                "Unit", unit, //
                "DisplayName", metadata.getDisplayName(), //
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
                name = "";
                for (String word : words) {
                    name += toFirstLetterUpperCase(word);
                }
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

    private static void checkSystemCpuLoadIssue(MBeanMetadataConfig metadataConfig) {
        // Could be constant but placed it in method as it is a workaround until fixed in JVM.
        // TODO Make this check dependent on the JDK version (as it hopefully will get solved in the future) -> Azul fix request made.
        String mbeanSystemCPULoad = "java.lang:type=OperatingSystem/SystemCpuLoad";
        long count = metadataConfig.getBaseMetadata().stream()
                .map(MBeanMetadata::getMBean)
                .filter(mbeanSystemCPULoad::equalsIgnoreCase)
                .count();

        count += metadataConfig.getVendorMetadata().stream()
                .map(MBeanMetadata::getMBean)
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
     * @param metadataConfig
     */
    private void initMetadataConfig(List<MBeanMetadata> baseMetadataList, List<MBeanMetadata> vendorMetadataList, boolean isRetry) {
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

    private MBeanMetadataConfig getConfig() {
        InputStream defaultConfig = MetricsService.class.getResourceAsStream("/metrics.xml");
        MBeanMetadataConfig config = JAXB.unmarshal(defaultConfig, MBeanMetadataConfig.class);

        File metricsResource = new File(serverEnv.getConfigDirPath(), "metrics.xml");
        if (metricsResource.exists()) {
            try {
                InputStream userMetrics = new FileInputStream(metricsResource);
                MBeanMetadataConfig extraConfig = JAXB.unmarshal(userMetrics, MBeanMetadataConfig.class);
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
        return metricsEnabled.booleanValue();
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
        MBeanMetadataConfig metadataConfig = getConfig();
        checkSystemCpuLoadIssue(metadataConfig); // PAYARA 2938
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
