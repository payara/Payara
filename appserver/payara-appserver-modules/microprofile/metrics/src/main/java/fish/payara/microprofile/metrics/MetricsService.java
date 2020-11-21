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
package fish.payara.microprofile.metrics;

import fish.payara.microprofile.metrics.admin.MetricsServiceConfiguration;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.impl.MetricRegistrationListener;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.jmx.MBeanMetadata;
import fish.payara.microprofile.metrics.jmx.MBeanMetadataConfig;
import fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import java.beans.PropertyChangeEvent;

import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import org.eclipse.microprofile.metrics.MetricID;

import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;
import org.glassfish.internal.data.ApplicationRegistry;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

@Service(name = "microprofile-metrics-service")
@RunLevel(StartupRunLevel.VAL)
public class MetricsService implements EventListener, ConfigListener, MonitoringDataSource {

    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Inject
    Events events;

    @Inject
    ApplicationRegistry applicationRegistry;

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

    /**
     * A record per registry so that any registry related state can be created or removed consistently to avoid memory
     * leaks or complicated cleanup work of related state that can be disposed in connection with the registry.
     */
    private static final class MetricRegistryState implements MetricRegistrationListener {

        final String registryName;
        final MetricRegistryImpl registry = new MetricRegistryImpl();
        final Queue<MetricID> registeredNotAnnotated = new ConcurrentLinkedQueue<>();

        public MetricRegistryState(String registryName) {
            this.registryName = registryName;
            registry.addListener(this);
        }

        @Override
        public void onRegistration(MetricID registered, MetricRegistry registry) {
            registeredNotAnnotated.add(registered);
        }

    }

    /**
     * stores registries of base, vendor, app1, app2, ... app(n) etc
     */
    private final Map<String, MetricRegistryState> registriesByName = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if(events == null){
            events = Globals.getDefaultBaseServiceLocator().getService(Events.class);
        }
        events.register(this);
        metricsServiceConfiguration = serviceLocator.getService(MetricsServiceConfiguration.class);
        // Only start if metrics are enabled
        if (isEnabled()) {
            PayaraExecutorService payaraExecutor = serviceLocator.getService(PayaraExecutorService.class, new Annotation[0]);
            payaraExecutor.submit(() -> {
                bootstrap();
            });
        }
    }

    @Override
    public void collect(MonitoringDataCollector rootCollector) {
        if (!isEnabled())
            return;
        MonitoringDataCollector metricsCollector = rootCollector.in("metric");
        for (MetricRegistryState state : registriesByName.values()) {
            collectRegistry(state, metricsCollector);
        }
    }

    private static void collectRegistry(MetricRegistryState state, MonitoringDataCollector collector) {
        processMetadataToAnnotations(state, collector);
        // OBS: this way of iterating the metrics in the registry is optimal because of its internal data organisation
        for (String name : state.registry.getNames()) {
            for (Entry<MetricID, Metric> entry : state.registry.getMetrics(name).entrySet()) {
                MetricID metricID = entry.getKey();
                Metric metric = entry.getValue();
                MonitoringDataCollector metricCollector = tagCollector(metricID, collector);
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
                                getMetricUnitSuffix(state.registry.getMetadata(name).getUnit())), ((Number) value));
                    }
                }
            }
        }
    }

    private static void processMetadataToAnnotations(MetricRegistryState state, MonitoringDataCollector collector) {
        MetricID metricID = state.registeredNotAnnotated.poll();
        while (metricID != null) {
            MonitoringDataCollector metricCollector = tagCollector(metricID, collector);
            Metadata metadata = state.registry.getMetadata(metricID.getName());
            String suffix = "Count";
            String property = "Count";
            boolean isGauge = metadata.getTypeRaw() == MetricType.GAUGE;
            if (isGauge) {
                suffix = getMetricUnitSuffix(metadata.getUnit());
                property = "Value";
            }
            // Note that by convention an annotation with value 0 done before the series collected any value is considered permanent
            metricCollector.annotate(toName(metricID, suffix), 0, false, metadataToAnnotations(state.registryName, metadata, property));
            metricID = state.registeredNotAnnotated.poll();
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

    private static String[] metadataToAnnotations(String registryName, Metadata metadata, String property) {
        String unit = metadata.getUnit().orElse(MetricUnits.NONE);
        return new String[] {
                "App", registryName, //
                "Name", metadata.getName(), //
                "Type", metadata.getType(), //
                "Unit", unit, //
                "DisplayName", metadata.getDisplayName(), //
                "Description", metadata.getDescription().orElse(""), //
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

    private static MonitoringDataCollector tagCollector(MetricID metric, MonitoringDataCollector collector) {
        Map<String, String> tags = metric.getTags();
        if (tags.isEmpty()) {
            String name = metric.getName();
            int dotIndex = name.indexOf('.');
            if (dotIndex > 0 ) {
                return collector.group(name.substring(0, dotIndex));
            }
            return collector;
        }
        StringBuilder tag = new StringBuilder();
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

    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_LOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            registerApplication(info.getName());
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
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
                    getOrAddRegistry(BASE.getName()),
                    baseMetadataList,
                    isRetry);
        }
        if (!vendorMetadataList.isEmpty()) {
            unresolvedVendorMetadataList = helper.registerMetadata(
                    getOrAddRegistry(VENDOR.getName()),
                    vendorMetadataList,
                    isRetry);
        }
    }

    /**
     * Registers unresolved MBeans if they have been started after the metrics
     * service.
     */
    public void reregisterMetadataConfig() {
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

    public Boolean isEnabled() {
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

    public <T extends Metric> T getApplicationMetric(MetricID metricID, Class<T> type) {
        return getOrAddRegistryInternal(getApplicationName()).getMetric(metricID, type);
    }

    public Set<MetricID> getMetricsIDs(String registryName, String metricName) throws NoSuchRegistryException {
        return getRegistryInternal(registryName).getMetricsIDs(metricName);
    }

    /**
     * Returns the Metrics registry based on respective registry name
     *
     * @param registryName
     * @return
     * @throws fish.payara.microprofile.metrics.exception.NoSuchRegistryException
     */
    public MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        return getRegistryInternal(registryName);
    }

    private MetricRegistryImpl getRegistryInternal(String registryName) throws NoSuchRegistryException {
        MetricRegistryState state = registriesByName.get(registryName.toLowerCase());
        if (state == null) {
            throw new NoSuchRegistryException(registryName);
        }
        return state.registry;
    }
    
    public List<MetricRegistry> getAllRegistry() {
        List<MetricRegistry> metricRegistries = new ArrayList<>();
        registriesByName.values().forEach((entry) -> {
            metricRegistries.add(entry.registry);
        });
        return metricRegistries;
    }

    public Set<String> getAllRegistryNames() {
        return registriesByName.keySet();
    }

    /**
     * Returns the Metrics registry based on respective registry name,
     * if not available then add the new MetricRegistry instance
     *
     * @param registryName
     * @return
     */
    public MetricRegistry getOrAddRegistry(String registryName) {
        return getOrAddRegistryInternal(registryName);
    }

    private MetricRegistryImpl getOrAddRegistryInternal(String registryName) {
        return registriesByName.computeIfAbsent(registryName.toLowerCase(), key -> new MetricRegistryState(registryName)).registry;
    }

    public MetricRegistry getApplicationRegistry() {
        return getOrAddRegistry(getApplicationName());
    }

    /**
     * Remove the Metrics registry
     *
     * @param registryName
     * @return
     */
    public MetricRegistry removeRegistry(String registryName) {
        MetricRegistryState state = registriesByName.remove(registryName.toLowerCase());
        return state == null ? null : state.registry;
    }

    /**
     * Adds an application to the enabled map
     *
     * @param applicationName The name of the application to remove
     */
    private void registerApplication(String applicationName) {
        getOrAddRegistry(applicationName);
    }

    /**
     * Removes an application from the enabled map
     *
     * @param applicationName The name of the application to remove
     */
    private void deregisterApplication(String applicationName) {
        removeRegistry(applicationName);
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
        String appName = current.getAppName();
        if (appName == null) {
            appName = current.getModuleName();
        }
        if (appName == null) {
            appName = current.getComponentId();
        }
        return appName;
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
