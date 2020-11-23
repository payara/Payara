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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;

/**
 * Writes {@link Metric}s according to the MicroPrfile Metrics 2.3 standard for JSON format as defined in <a href=
 * "https://download.eclipse.org/microprofile/microprofile-metrics-2.3/microprofile-metrics-spec-2.3.pdf">microprofile-metrics-spec-2.3.pdf</a>.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class JsonExporter implements MetricExporter {

    public enum Mode { GET, OPTIONS }

    private final MetricRegistry.Type scope;
    private final JsonWriter out;
    private final Mode mode;
    private final JsonObjectBuilder documentObj;
    private final JsonObjectBuilder scopeObj;
    private JsonObjectBuilder groupObj;
    private JsonArrayBuilder tagsArray;
    private MetricID exportedBefore;
    private Metadata exportedBeforeMetadata;

    public JsonExporter(Writer out, Mode mode, boolean prettyPrint) {
        this(null, writer(out, prettyPrint), mode, Json.createObjectBuilder(), null);
    }

    private static JsonWriter writer(Writer out, boolean prettyPrint) {
        return Json.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, prettyPrint)).createWriter(out);
    }

    private JsonExporter(MetricRegistry.Type scope, JsonWriter out, Mode mode, JsonObjectBuilder documentObj,
            JsonObjectBuilder scopeObj) {
        this.scope = scope;
        this.out = out;
        this.mode = mode;
        this.documentObj = documentObj;
        this.scopeObj = scopeObj;
    }

    @Override
    public MetricExporter in(Type scope, boolean asNode) {
        completeScope();
        return new JsonExporter(scope, out, mode, documentObj, asNode ? Json.createObjectBuilder() : null);
    }

    @Override
    public void export(MetricID metricID, Counter counter, Metadata metadata) {
        completeGroup(metricID, metadata);
        appendMember(metricID, counter.getCount());
    }

    @Override
    public void export(MetricID metricID, ConcurrentGauge gauge, Metadata metadata) {
        completeOrUpdateGroup(metricID, metadata);
        appendMember(metricID, "current", gauge.getCount());
        appendMember(metricID, "min", gauge.getMin());
        appendMember(metricID, "max", gauge.getMax());
    }

    @Override
    public void export(MetricID metricID, Gauge<?> gauge, Metadata metadata) {
        completeGroup(metricID, metadata);
        if (mode == Mode.OPTIONS) {
            return;
        }
        Object value = null;
        try {
            value = gauge.getValue();
        } catch (IllegalStateException ex) {
            // The forwarding gauge is unloaded
            return;
        }
        if (!(value instanceof Number)) {
            LOGGER.log(Level.FINER, "Skipping OpenMetrics output for Gauge: {0} of type {1}",
                    new Object[] { metricID, value.getClass() });
            return;
        }
        appendMember(metricID, (Number) value);
    }

    @Override
    public void export(MetricID metricID, Histogram histogram, Metadata metadata) {
        completeOrUpdateGroup(metricID, metadata);
        appendMember(metricID, "count", histogram.getCount());
        appendMember(metricID, "sum", histogram.getSum());
        exportSampling(metricID, histogram);
    }

    private void exportSampling(MetricID metricID, Sampling sampling) {
        Snapshot snapshot = sampling.getSnapshot();
        appendMember(metricID, "min", snapshot.getMin());
        appendMember(metricID, "max", snapshot.getMax());
        appendMember(metricID, "mean", snapshot.getMean());
        appendMember(metricID, "stddev", snapshot.getStdDev());
        appendMember(metricID, "p50", snapshot.getMedian());
        appendMember(metricID, "p75", snapshot.get75thPercentile());
        appendMember(metricID, "p95", snapshot.get95thPercentile());
        appendMember(metricID, "p98", snapshot.get98thPercentile());
        appendMember(metricID, "p99", snapshot.get99thPercentile());
        appendMember(metricID, "p999", snapshot.get999thPercentile());
    }

    @Override
    public void export(MetricID metricID, Meter meter, Metadata metadata) {
        completeOrUpdateGroup(metricID, metadata);
        exportMetered(metricID, meter);
    }

    private void exportMetered(MetricID metricID, Metered metered) {
        appendMember(metricID, "count", metered.getCount());
        appendMember(metricID, "meanRate", metered.getMeanRate());
        appendMember(metricID, "oneMinRate", metered.getOneMinuteRate());
        appendMember(metricID, "fiveMinRate", metered.getFiveMinuteRate());
        appendMember(metricID, "fifteenMinRate", metered.getFifteenMinuteRate());
    }

    @Override
    public void export(MetricID metricID, SimpleTimer timer, Metadata metadata) {
        completeOrUpdateGroup(metricID, metadata);
        appendMember(metricID, "count", timer.getCount());
        appendMember(metricID, "elapsedTime", timer.getElapsedTime().toMillis());
        appendMember(metricID, "maxTimeDuration", millisOrNull(timer.getMaxTimeDuration()));
        appendMember(metricID, "minTimeDuration", millisOrNull(timer.getMinTimeDuration()));
    }

    private static Long millisOrNull(Duration d) {
        return d == null ? null : d.toMillis();
    }

    @Override
    public void export(MetricID metricID, Timer timer, Metadata metadata) {
        completeOrUpdateGroup(metricID, metadata);
        appendMember(metricID, "elapsedTime", millisOrNull(timer.getElapsedTime()));
        exportMetered(metricID, timer);
        exportSampling(metricID, timer);
    }

    @Override
    public void exportComplete() {
        completeGroup(null, null);
        completeScope();
        out.write(documentObj.build());
    }

    private void exportMetadata() {
        if (exportedBefore == null) {
            return;
        }
        JsonObjectBuilder target = scopeObj != null ? scopeObj : documentObj;
        JsonObjectBuilder metadataObj = Json.createObjectBuilder();
        Metadata metadata = exportedBeforeMetadata;
        metadataObj.add("unit", metadata.unit().orElse(MetricUnits.NONE));
        metadataObj.add("type", metadata.getTypeRaw().toString());
        if (metadata.description().isPresent()) {
            String desc = metadata.getDescription();
            if (!desc.isEmpty()) {
                metadataObj.add("description", desc);
            }
        }
        String displayName = metadata.getDisplayName();
        String name = exportedBefore.getName();
        if (!displayName.isEmpty() && !displayName.equals(name)) {
            metadataObj.add("displayName", displayName);
        }
        if (tagsArray != null) {
            metadataObj.add("tags", tagsArray.build());
            tagsArray = null;
        }
        target.add(name, metadataObj.build());
    }

    private void completeScope() {
        completeGroup(null, null);
        if (scopeObj != null) {
            JsonObject obj = scopeObj.build();
            if (obj.size() > 0) {
                documentObj.add(scope.getName(), obj);
            }
        }
    }

    private void completeGroup(MetricID current, Metadata metadata) {
        if (mode == Mode.GET && groupObj != null) {
            JsonObjectBuilder target = scopeObj != null ? scopeObj : documentObj;
            target.add(exportedBefore.getName(), groupObj);
            groupObj = null;
        }
        if (mode == Mode.OPTIONS) {
            if (isNameChange(current)) {
                exportMetadata();
            }
            List<Tag> tags = tagsAlphabeticallySorted(current);
            if (!tags.isEmpty()) {
                JsonArrayBuilder currentTags = Json.createArrayBuilder();
                for (Tag tag : tags) {
                    currentTags.add(tagAsString(tag));
                }
                if (tagsArray == null) {
                    tagsArray = Json.createArrayBuilder();
                }
                tagsArray.add(currentTags.build());
            }
        }
        exportedBefore = current;
        exportedBeforeMetadata = metadata;
    }

    private void completeOrUpdateGroup(MetricID current, Metadata metadata) {
        if (mode == Mode.OPTIONS || isNameChange(current)) {
            completeGroup(current, metadata);
        }
        if (mode == Mode.GET && groupObj == null) {
            groupObj = Json.createObjectBuilder();
        }
    }

    private boolean isNameChange(MetricID current) {
        return current == null || exportedBefore == null || !exportedBefore.getName().equals(current.getName());
    }

    private void appendMember(MetricID metricID, Number value) {
        appendMember(metricID, null, value);
    }

    private void appendMember(MetricID metricID, String field, Number value) {
        if (mode == Mode.OPTIONS) {
            return; // nothing to do, metadata written in connection with group update
        }
        JsonObjectBuilder target = groupObj != null ? groupObj : scopeObj != null ? scopeObj : documentObj;
        String name = field != null ? field : metricID.getName();
        List<Tag> tags = tagsAlphabeticallySorted(metricID);
        if (!tags.isEmpty()) {
            for (Tag tag : tags) {
                name += ';' + tagAsString(tag);
            }
        }
        if (value instanceof Float || value instanceof Double) {
            target.add(name, value.doubleValue());
        } else if (value instanceof BigDecimal) {
            target.add(name, (BigDecimal) value);
        } else if (value instanceof BigInteger) {
            target.add(name, (BigInteger) value);
        } else if (value == null) {
            target.addNull(name);
        } else {
            target.add(name, value.longValue());
        }
    }

    private static String tagAsString(Tag tag) {
        return tag.getTagName() + '=' + tag.getTagValue().replace(';', '_');
    }

    private static List<Tag> tagsAlphabeticallySorted(MetricID metricID) {
        if (metricID == null) {
            return emptyList();
        }
        Tag[] tags = metricID.getTagsAsArray();
        if(tags.length == 0) {
            return emptyList();
        }
        Arrays.sort(tags, (a, b) -> a.getTagName().compareTo(b.getTagName()));
        return asList(tags);
    }
}
