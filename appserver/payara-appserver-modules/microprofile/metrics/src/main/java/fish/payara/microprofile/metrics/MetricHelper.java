/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.metrics.cdi.extension.MetricCDIExtension;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.jmx.JMXBeanMetadata;
import fish.payara.microprofile.metrics.jmx.JMXMetadataConfig;
import fish.payara.microprofile.metrics.jmx.JmxWorker;
import fish.payara.microprofile.metrics.jmx.MCounterImpl;
import fish.payara.microprofile.metrics.jmx.MGaugeImpl;
import fish.payara.microprofile.metrics.cdi.MetricRegistryRepository;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import static java.util.Collections.singletonMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXB;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import static org.eclipse.microprofile.metrics.Metadata.GLOBAL_TAGS_VARIABLE;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import org.glassfish.internal.api.Globals;

@ApplicationScoped
public class MetricHelper {
    
    private static final Logger LOGGER = Logger.getLogger(MetricCDIExtension.class.getName());

    @Inject
    private MetricRegistryRepository repository;
            
    public Map<String, org.eclipse.microprofile.metrics.Metric> getMetricsAsMap(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, org.eclipse.microprofile.metrics.Metric> metricMap = registry.getMetrics();
        if (metricMap.containsKey(metricName)) {
            return singletonMap(metricName, metricMap.get(metricName));
        } else {
            throw new NoSuchMetricException();
        }
    }

    public Map<String, org.eclipse.microprofile.metrics.Metric> getMetricsAsMap(String registryName) throws NoSuchRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        return registry.getMetrics();
    }

    public Map<String, Metadata> getMetadataAsMap(String registryName) throws NoSuchRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        return registry.getMetadata();
    }

    public Map<String, Metadata> getMetadataAsMap(String registryName, String metric) throws NoSuchRegistryException, NoSuchMetricException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metadata> metricMetadataMap = registry.getMetadata();
        if (metricMetadataMap.containsKey(metric)) {
            return singletonMap(metric, metricMetadataMap.get(metric));
        } else {
            throw new NoSuchMetricException();
        }
    }

    private MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        if (!Constants.REGISTRY_NAMES_LIST.contains(registryName)) {
            throw new NoSuchRegistryException();
        }
        return repository.getOrAdd(MetricRegistry.Type.valueOf(registryName.toUpperCase()));
    }

    public static Map<String, Number> getTimerNumbers(Timer timer) {
        Map<String, Number> results = new HashMap<>();
        results.putAll(getMeteredNumbers(timer));
        results.putAll(getSnapshotNumbers(timer.getSnapshot()));
        return results;
    }

    public static Map<String, Number> getHistogramNumbers(Histogram histogram) {
        Map<String, Number> results = new HashMap<>();
        results.put(Constants.COUNT, histogram.getCount());
        results.putAll(getSnapshotNumbers(histogram.getSnapshot()));
        return results;
    }

    public static Map<String, Number> getMeterNumbers(Meter meter) {
        Map<String, Number> results = new HashMap<>();
        results.putAll(getMeteredNumbers(meter));
        return results;
    }

    private static Map<String, Number> getMeteredNumbers(Metered metered) {
        Map<String, Number> results = new HashMap<>();
        results.put(Constants.COUNT, metered.getCount());
        results.put(Constants.MEAN_RATE, metered.getMeanRate());
        results.put(Constants.ONE_MINUTE_RATE, metered.getOneMinuteRate());
        results.put(Constants.FIVE_MINUTE_RATE, metered.getFiveMinuteRate());
        results.put(Constants.FIFTEEN_MINUTE_RATE, metered.getFifteenMinuteRate());
        return results;
    }

    private static Map<String, Number> getSnapshotNumbers(Snapshot snapshot) {
        Map<String, Number> results = new HashMap<>();
        results.put(Constants.MAX, snapshot.getMax());
        results.put(Constants.MEAN, snapshot.getMean());
        results.put(Constants.MIN, snapshot.getMin());
        results.put(Constants.STD_DEV, snapshot.getStdDev());
        results.put(Constants.MEDIAN, snapshot.getMedian());
        results.put(Constants.PERCENTILE_75TH, snapshot.get75thPercentile());
        results.put(Constants.PERCENTILE_95TH, snapshot.get95thPercentile());
        results.put(Constants.PERCENTILE_98TH, snapshot.get98thPercentile());
        results.put(Constants.PERCENTILE_99TH, snapshot.get99thPercentile());
        results.put(Constants.PERCENTILE_999TH, snapshot.get999thPercentile());
        return results;
    }
    
    public static List<Tag> convertToTags(String globalTagsString) {
        List<Tag> tags = Collections.emptyList();
        if (globalTagsString != null) {
            String[] singleTags = globalTagsString.split(",");
            tags = Stream.of(singleTags)
                    .map(tag -> tag.trim())
                    .map(tag -> new Tag(tag))
                    .collect(toList());
        }
        return tags;
    }
    
    
    public String metricNameOf(InjectionPoint ip) {
        Annotated annotated = ip.getAnnotated();
        if (annotated instanceof AnnotatedMember) {
            return metricNameOf((AnnotatedMember<?>) annotated);
        } else if (annotated instanceof AnnotatedParameter) {
            return metricNameOf((AnnotatedParameter<?>) annotated);
        } else {
            throw new IllegalArgumentException("Unable to retrieve metric name for injection point [" + ip + "], only members and parameters are supported");
        }
    }

    private String metricNameOf(AnnotatedMember<?> member) {
        if (member.isAnnotationPresent(Metric.class)) {
            Metric metric = member.getAnnotation(Metric.class);
            String name = metric.name().isEmpty() ? member.getJavaMember().getName() : metric.name();
            return metric.absolute() ? name : MetricRegistry.name(member.getJavaMember().getDeclaringClass(), name);
        } else {
            String name = member.getJavaMember().getName();
            return MetricRegistry.name(member.getJavaMember().getDeclaringClass(), name);
        }
    }

    private String metricNameOf(AnnotatedParameter<?> parameter) {
        if (parameter.isAnnotationPresent(Metric.class)) {
            Metric metric = parameter.getAnnotation(Metric.class);
            String name = metric.name().isEmpty() ? getParameterName(parameter) : metric.name();
            return metric.absolute() ? name : MetricRegistry.name(parameter.getDeclaringCallable().getJavaMember().getDeclaringClass(), name);
        } else {
            String name = getParameterName(parameter);
            return MetricRegistry.name(parameter.getDeclaringCallable().getJavaMember().getDeclaringClass(), name);
        }
    }
    
    private String getParameterName(AnnotatedParameter<?> parameter) {
        try {
            Method method = Method.class.getMethod("getParameters");
            Object[] parameters = (Object[]) method.invoke(parameter.getDeclaringCallable().getJavaMember());
            Object param = parameters[parameter.getPosition()];
            Class<?> Parameter = Class.forName("java.lang.reflect.Parameter");
            if ((Boolean) Parameter.getMethod("isNamePresent").invoke(param)) {
                return (String) Parameter.getMethod("getName").invoke(param);
            } else {
                throw new UnsupportedOperationException("Unable to retrieve name for parameter [" + parameter + "], activate the -parameters compiler argument or annotate the injected parameter with the @Metric annotation");
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException cause) {
            throw new UnsupportedOperationException("Unable to retrieve name for parameter [" + parameter + "], @Metric annotation on injected parameter is required before Java 8");
        }
    }

    public Metadata metadataOf(InjectionPoint ip, Class<?> type) {
        Annotated annotated = ip.getAnnotated();
        String name = metricNameOf(ip);
        return metadataOf(annotated, type, name);
    }

    public Metadata metadataOf(AnnotatedMember<?> member) {
        String typeName = member.getBaseType().getTypeName();
        if (typeName.startsWith(Gauge.class.getName())) {
            return metadataOf(member, Gauge.class);
        } else if (typeName.startsWith(Counter.class.getName())) {
            return metadataOf(member, Counter.class);
        } else if (typeName.startsWith(Meter.class.getName())) {
            return metadataOf(member, Meter.class);
        } else if (typeName.startsWith(Histogram.class.getName())) {
            return metadataOf(member, Histogram.class);
        } else if (typeName.startsWith(Timer.class.getName())) {
            return metadataOf(member, Timer.class);
        }
        return null;
    }

    private Metadata metadataOf(AnnotatedMember<?> member, Class<?> type) {
        return metadataOf(member, type, metricNameOf(member));
    }
    
    private Metadata metadataOf(Annotated annotated, Class<?> type, String name) {
        Metadata metadata = new Metadata(name, MetricType.from(type));
        if (annotated.isAnnotationPresent(Metric.class)) {
            Metric metric = annotated.getAnnotation(Metric.class);
            metadata.setDescription(metric.description() == null || metric.description().trim().isEmpty() ? null
                    : metric.description());
            metadata.setDisplayName(metric.displayName() == null || metric.displayName().trim().isEmpty() ? null
                    : metric.displayName());
            metadata.setUnit(metric.unit() == null || metric.unit().trim().isEmpty() ? null
                    : metric.unit());
            for (String tag : metric.tags()) {
                metadata.addTag(tag);
            }
        }
        return metadata;
    }
    
    public static boolean isMetricEnabled() {
        MetricsService metricService = Globals.getDefaultBaseServiceLocator()
                .getService(MetricsService.class);
        Config config = null;
        try {
            config = ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "No config could be found", ex);
        }
        String appName = metricService.getApplicationName();
        return metricService.isMetricEnabled(appName, config);
    }
    
    /**
     * metrics.xml contains the base & vendor metrics metadata.
     *
     * @param metadataConfig
     */
    public static void initMetadataConfig(JMXMetadataConfig metadataConfig){
        List<Tag> globalTags = convertToTags(System.getenv(GLOBAL_TAGS_VARIABLE));
        
        registerMetadata(MetricRegistryRepository.BASE_METRIC_REGISTRY,
                metadataConfig.getBase(), globalTags);
        registerMetadata(MetricRegistryRepository.VENDOR_METRIC_REGISTRY,
                metadataConfig.getVendor(), globalTags);
    }
    
    private static void registerMetadata(MetricRegistry metricRegistry,
            List<JMXBeanMetadata> metadataList, List<Tag> globalTags) {
        JmxWorker.getInstance().expandMultiValueEntries(metadataList);
        metadataList.forEach(beanMetadata -> {
            beanMetadata.processTags(globalTags);
            org.eclipse.microprofile.metrics.Metric type;
            if (beanMetadata.getTypeRaw() == GAUGE) {
                type = new MGaugeImpl(beanMetadata.getMbean());
            } else if (beanMetadata.getTypeRaw() == COUNTER) {
                type = new MCounterImpl(beanMetadata.getMbean());
            } else {
                throw new IllegalStateException("Not yet supported: " + beanMetadata);
            }
            metricRegistry.register(beanMetadata.getName(), type, beanMetadata);
        });
    }
    

    
}
