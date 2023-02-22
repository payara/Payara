/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.cdi;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import java.util.stream.*;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

/**
 * Unfortunately the {@link MetricRegistry} has no generic versions of the get or register methods for the different
 * types of {@link Metric}s. Therefore this utility provides a generic API by mapping the generic methods to the type
 * specific ones.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public final class MetricUtils<T extends Metric> {

    @FunctionalInterface
    private interface By<A, B, R> {

        R apply(MetricRegistry registry, A a, B b);
    }

    @FunctionalInterface
    private interface ByJust<A, R> {

        R apply(MetricRegistry registry, A a);
    }

    private static final Map<Class<? extends Metric>, MetricUtils<?>> TYPES = new HashMap<>();

    public static final String TAG_METRIC_MP_SCOPE_NAME = "mp_scope";

    public static final String TAG_METRIC_MP_APP_NAME = "mp_app";

    private final ByJust<String, T> byName;
    private final By<String, Tag[], T> byNameAndTags;
    private final By<Metadata, Tag[], T> byMetadataAndTags;

    private MetricUtils(
            ByJust<String, T> byName,
            By<String, Tag[], T> byNameAndTags,
            By<Metadata, Tag[], T> byMetadataAndTags) {
        this.byName = byName;
        this.byNameAndTags = byNameAndTags;
        this.byMetadataAndTags = byMetadataAndTags;
    }

    static <T extends Metric> void register(
            Class<T> type, ByJust<String, T> byName,
            By<String, Tag[], T> byNameAndTags,
            By<Metadata, Tag[], T> byMetadataAndTags) {
        TYPES.put(type, new MetricUtils<>(byName, byNameAndTags, byMetadataAndTags));
    }

    static {
        register(Counter.class, MetricRegistry::counter, MetricRegistry::counter, MetricRegistry::counter);
        register(Histogram.class, MetricRegistry::histogram, MetricRegistry::histogram, MetricRegistry::histogram);
        register(Timer.class, MetricRegistry::timer, MetricRegistry::timer, MetricRegistry::timer);
        register(Gauge.class, MetricUtils::getGauge, MetricUtils::getGauge, MetricUtils::getGauge);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Metric> T getOrRegisterByName(MetricRegistry registry, Class<T> metric, String name) {
        return (T) getOrRegister(metric).byName.apply(registry, name);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Metric> T getOrRegisterByNameAndTags(MetricRegistry registry, Class<T> metric, String name,
            Tag... tags) {
        return (T) getOrRegister(metric).byNameAndTags.apply(registry, name, tags);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Metric> T getOrRegisterByMetadataAndTags(MetricRegistry registry, Class<T> metric,
            Metadata metadata, Tag... tags) {
        return (T) getOrRegister(metric).byMetadataAndTags.apply(registry, metadata, tags);
    }

    public static Tag[] setScopeTagForMetric(String scope, Tag... tags) {
        Tag[] tArray = new Tag[1];
        if(scope.equals(MetricRegistry.BASE_SCOPE)) {
            Tag t = new Tag("mp_scope", MetricRegistry.BASE_SCOPE);
            tArray[0] = t;
        } else if(scope.equals(MetricRegistry.VENDOR_SCOPE)) {
            Tag t = new Tag("mp_scope", MetricRegistry.VENDOR_SCOPE);
            tArray[0] = t;
        } else if(scope.equals(MetricRegistry.APPLICATION_SCOPE)) {
            Tag t = new Tag("mp_scope", MetricRegistry.APPLICATION_SCOPE);
            tArray[0] = t;
        } else if(scope != null) {
            Tag t = new Tag("mp_scope", scope);
            tArray[0] = t;
        }
        Tag[] mergeArray = Stream.concat(Arrays.stream(tags),
                        Arrays.stream(tArray)).
                toArray(v -> (Tag[]) Array.newInstance(tags.getClass().getComponentType(), v));
        return mergeArray;
    }

    private static <T extends Metric> MetricUtils<?> getOrRegister(Class<T> metric) {
        MetricUtils<?> getOrRegister = TYPES.get(metric);
        if (getOrRegister == null) {
            throw new IllegalArgumentException("Cannot get or register metrics of type " + metric);
        }
        return getOrRegister;
    }

    /*
     * Gauges can only be resolved as registering requires the actual gauge instance.
     */

    private static Gauge<?> getGauge(MetricRegistry registry, String name) {
        return getGauge(registry, name, new Tag[0]);
    }

    private static Gauge<?> getGauge(MetricRegistry registry, Metadata metadata, Tag[] tags) {
        return getGauge(registry, metadata.getName(), tags);
    }

    @SuppressWarnings("unchecked")
    private static Gauge<?> getGauge(MetricRegistry registry, String name, Tag[] tags) {
        MetricID complementedMetricID = new MetricID(name, tags);//MetricUtils.validateAndComplementTags();
        Gauge<?> gauge = registry.getGauges().get(complementedMetricID);
        return gauge != null ? gauge : new LazyGauge<>(() -> registry.getGauges().get(complementedMetricID));
    }

    private static final class LazyGauge<T extends Number> implements Gauge<T> {

        private final AtomicReference<Gauge<T>> gauge = new AtomicReference<>();
        private final Supplier<Gauge<T>> lookup;
        LazyGauge(Supplier<Gauge<T>> lookup) {
            this.lookup = lookup;
        }
        @Override
        public T getValue() {
            Gauge<T> lazy = gauge.updateAndGet(instance -> instance != null ? instance : lookup.get());
            return lazy == null ? null : lazy.getValue();
        }
    }


}
