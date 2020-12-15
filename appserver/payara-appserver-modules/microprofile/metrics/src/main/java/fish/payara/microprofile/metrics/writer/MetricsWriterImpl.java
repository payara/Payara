/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.writer;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;

import org.eclipse.microprofile.metrics.Tag;

import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;

public class MetricsWriterImpl implements MetricsWriter {

    private final MetricExporter exporter;
    private final Supplier<Set<String>> registryNames;
    private final Function<String, MetricRegistry> getMetricsRegistryByName;
    private final Tag[] globalTags;

    public MetricsWriterImpl(MetricExporter exporter, Supplier<Set<String>> registryNames,
            Function<String, MetricRegistry> getMetricsRegistryByName, Tag... globalTags) {
        this.exporter = exporter;
        this.registryNames = registryNames;
        this.getMetricsRegistryByName = getMetricsRegistryByName;
        this.globalTags = globalTags;
    }

    @Override
    public void write(Type scope, String metricName)
            throws NoSuchRegistryException, NoSuchMetricException {
        MetricExporter exporter = this.exporter.in(scope, false);
        if (scope == Type.APPLICATION) {
            writeApplicationRegistries(exporter, registryNames.get(), metricName);
        } else {
            String registryName = scope.getName();
            writeMetricFamily(exporter, registryName, metricName, getMetricsRegistry(registryName), false);
        }
        exporter.exportComplete();
    }

    @Override
    public void write(Type scope) throws NoSuchRegistryException {
        MetricExporter exporter = this.exporter.in(scope, false);
        if (scope == Type.APPLICATION) {
            writeApplicationRegistries(exporter, registryNames.get());
        } else {
            String registryName = scope.getName();
            writeRegistry(exporter, registryName, getMetricsRegistry(registryName), false);
        }
        exporter.exportComplete();
    }

    @Override
    public void write() throws IOException {
        Set<String> allNames = registryNames.get();
        MetricExporter exporter = this.exporter;
        exporter = exporter.in(Type.BASE);
        writeRegistry(exporter, Type.BASE.getName(), getMetricsRegistry(Type.BASE.getName()), false);
        exporter = exporter.in(Type.VENDOR);
        writeRegistry(exporter, Type.VENDOR.getName(), getMetricsRegistry(Type.VENDOR.getName()), false);
        exporter = exporter.in(Type.APPLICATION);
        writeApplicationRegistries(exporter, allNames);
        exporter.exportComplete();
    }

    private void writeApplicationRegistries(MetricExporter exporter, Set<String> allNames, String... metricNames) {
        Map<String, MetricRegistryImpl> registries = new TreeMap<>();
        for (String registryName : getApplicationRegistryNames(allNames)) {
            registries.put(registryName, getMetricsRegistry(registryName));
        }
        Set<String> filterNames = new HashSet<>(asList(metricNames));
        Predicate<String> filter = metricNames.length == 0 ? name -> true : name -> filterNames.contains(name);
        if (!hasNameCollisions(registries.values(), filter)) {
            for (Entry<String, MetricRegistryImpl> registry : registries.entrySet()) {
                writeRegistry(exporter, registry.getKey(), registry.getValue(), false);
            }
            return;
        }
        for (String metricName : allMetricNames(registries.values(), filter)) {
            for (Entry<String, MetricRegistryImpl> registry : registries.entrySet()) {
                if (registry.getValue().getMetadata(metricName) != null) { // it has metrics with that name
                    writeMetricFamily(exporter, registry.getKey(), metricName, registry.getValue(), true);
                }
            }
        }
    }

    private void writeRegistry(MetricExporter exporter, String registryName, MetricRegistryImpl registry,
            boolean addAppTag) {
        for (String metricName : registry.getNames()) {
            writeMetricFamily(exporter, registryName, metricName, registry, addAppTag);
        }
    }

    private void writeMetricFamily(MetricExporter exporter, String registryName, String metricName,
            MetricRegistryImpl registry, boolean addAppTag) {
        Metadata metadata = registry.getMetadata(metricName);
        for (Entry<MetricID, Metric> metric : registry.getMetrics(metricName).entrySet()) {
            MetricID metricID = metric.getKey();
            if (globalTags.length > 0) {
                Tag[]  tagsWithoutGlobal = metricID.getTagsAsArray();
                Tag[] tags = new Tag[tagsWithoutGlobal.length +  globalTags.length];
                arraycopy(globalTags, 0, tags, 0, globalTags.length);
                arraycopy(tagsWithoutGlobal, 0, tags, globalTags.length, tagsWithoutGlobal.length);
                metricID = new MetricID(metricName, tags);
            }
            if (addAppTag) {
                Tag[] tagsWithoutApp = metricID.getTagsAsArray();
                Tag[] tags = Arrays.copyOf(tagsWithoutApp, tagsWithoutApp.length + 1);
                tags[tagsWithoutApp.length] = new Tag("_app", registryName);
                metricID = new MetricID(metricName, tags);
            }
            exporter.export(metricID, metric.getValue(), metadata);
        }
    }

    /**
     * Casting to {@link MetricRegistryImpl} is dirty but chosen as an intermediate solution as
     * https://github.com/eclipse/microprofile-metrics/pull/548 adds needed methods to the API so they will be available
     * in the {@link MetricRegistry} interface in 3.0 and this cast can be removed.
     */
    private MetricRegistryImpl getMetricsRegistry(String name) {
        return (MetricRegistryImpl) getMetricsRegistryByName.apply(name);
    }

    private static Set<String> getApplicationRegistryNames(Set<String> names) {
        Set<String> appNames = new TreeSet<>(names);
        appNames.remove(Type.BASE.getName());
        appNames.remove(Type.VENDOR.getName());
        return appNames;
    }

    private static boolean hasNameCollisions(Collection<? extends MetricRegistry> registries, Predicate<String> filter) {
        Set<String> allMetricNames = new HashSet<>();
        for (MetricRegistry registry : registries) {
            SortedSet<String> names = registry.getNames();
            for (String name : names) {
                if (filter.test(name)) {
                    if (allMetricNames.contains(name)) {
                        return true;
                    }
                    allMetricNames.add(name);
                }
            }
        }
        return false;
    }

    private static Set<String> allMetricNames(Collection<? extends MetricRegistry> registries, Predicate<String> filter) {
        Set<String> allNames = new TreeSet<>();
        for (MetricRegistry registry : registries) {
            for (String metricName : registry.getNames()) {
                if (filter.test(metricName)) {
                    allNames.add(metricName);
                }
            }
        }
        return allNames;
    }
}
