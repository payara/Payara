/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * only if the code is changed by the third party and used as a new code
 * in combination with Open Source Software developed by Glassfish/Payara
 * or its successors.
 */
package fish.payara.microprofile.faulttolerance.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;

import java.util.Map;

/**
 * OpenTelemetry-backed implementation of MicroProfile Metrics {@link Histogram}.
 * Delegates update operations to an underlying OTel {@link DoubleHistogram}.
 * Tags from MetricID are converted to OTel attributes.
 * 
 * Note: OTel histograms record observations but do not expose synchronous
 * percentile/quantile reads. The {@link #getSnapshot()} method is unsupported.
 */
final class OtelHistogram implements Histogram {

    private final DoubleHistogram delegate;
    private final Attributes attributes;

    OtelHistogram(DoubleHistogram delegate) {
        this.delegate = delegate;
        this.attributes = Attributes.empty();
    }

    OtelHistogram(DoubleHistogram delegate, String scope, Map<String, String> tags) {
        this.delegate = delegate;
        this.attributes = tagsAndScopeToAttributes(scope, tags);
    }

    private static Attributes tagsAndScopeToAttributes(String scope, Map<String, String> tags) {
        AttributesBuilder builder = Attributes.builder();
        if (scope != null) {
            builder.put("mp.scope", scope);
        }
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(builder::put);
        }
        return builder.build();
    }

    @Override
    public void update(int value) {
        // FT MP Histograms are in nanoseconds, OTEL ones in seconds
        delegate.record(value / 1_000_000_000d, attributes);
    }

    @Override
    public void update(long value) {
        delegate.record(value / 1_000_000_000d, attributes);
    }

    @Override
    public long getCount() {
        throw new UnsupportedOperationException(
            "Synchronous histogram reads are not supported in the OpenTelemetry bridge. "
            + "Use a monitoring backend to query exported metrics."
        );
    }

    @Override
    public long getSum() {
        throw new UnsupportedOperationException(
            "Synchronous histogram reads are not supported in the OpenTelemetry bridge. "
            + "Use a monitoring backend to query exported metrics."
        );
    }



    @Override
    public Snapshot getSnapshot() {
        throw new UnsupportedOperationException(
            "Percentile snapshots are not supported in the OpenTelemetry bridge. "
            + "Use a monitoring backend to query exported percentiles."
        );
    }



}
