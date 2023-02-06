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
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;

import org.eclipse.microprofile.metrics.Tag;

import fish.payara.microprofile.metrics.MetricsService.MetricsContext;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;

public class MetricsWriterImpl implements MetricsWriter {

    private final MetricExporter exporter;
    private final Set<String> contextNames;
    private final Function<String, MetricsContext> getContextByName;
    private final Tag[] globalTags;

    public MetricsWriterImpl(MetricExporter exporter, Set<String> contextNames,
            Function<String, MetricsContext> getContextByName, Tag... globalTags) {
        this.exporter = exporter;
        this.contextNames = contextNames;
        this.getContextByName = getContextByName;
        this.globalTags = globalTags;
    }

    @Override
    public void write(Type scope, String metricName)
            throws NoSuchRegistryException, NoSuchMetricException {
        MetricExporter exporter = this.exporter.in(scope, false);
        writeMetricFamily(exporter, scope, metricName);
        exporter.exportComplete();
    }

    @Override
    public void write(Type scope) throws NoSuchRegistryException {
        MetricExporter exporter = this.exporter.in(scope, false);
        writeRegistries(exporter, scope);
        exporter.exportComplete();
    }

    @Override
    public void write() throws IOException {
        MetricExporter exporter = this.exporter;
        exporter = exporter.in(Type.BASE);
        writeRegistries(exporter, Type.BASE);
        exporter = exporter.in(Type.VENDOR);
        writeRegistries(exporter, Type.VENDOR);
        exporter = exporter.in(Type.APPLICATION);
        writeRegistries(exporter, Type.APPLICATION);
        exporter.exportComplete();
    }

    private void writeRegistries(MetricExporter exporter, Type scope) {
        for (String metricName : allMetricNames(scope)) {
            writeMetricFamily(exporter, scope, metricName);
        }
    }

    private void writeMetricFamily(MetricExporter exporter, Type scope, String metricName) {
        for (String contextName : contextNames) {
            MetricRegistryImpl registry = getMetricsRegistry(contextName, scope);
            if (registry != null && registry.getMetadata(metricName) != null) { // it has metrics with that name
                writeMetricFamily(exporter, contextName, metricName, registry);
            }
        }
    }

    private void writeMetricFamily(MetricExporter exporter, String contextName, String metricName,
            MetricRegistryImpl registry) {
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
            if (!contextName.isEmpty()) {
                Tag[] tagsWithoutApp = metricID.getTagsAsArray();
                Tag[] tags = Arrays.copyOf(tagsWithoutApp, tagsWithoutApp.length + 1);
                tags[tagsWithoutApp.length] = new Tag("_app", contextName);
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
    private MetricRegistryImpl getMetricsRegistry(String contextName, Type scope) {
        MetricsContext context = getContextByName.apply(contextName);
        return scope == Type.APPLICATION && context.isServerContext() ? null : (MetricRegistryImpl) context.getRegistry(scope);
    }

    private Set<String> allMetricNames(Type scope) {
        Set<String> allNames = new TreeSet<>();
        for (String contextName : contextNames) {
            MetricRegistry registry = getMetricsRegistry(contextName, scope);
            if (registry != null) {
                allNames.addAll(registry.getNames());
            }
        }
        return allNames;
    }
}
