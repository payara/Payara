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

import java.util.Map;
import org.eclipse.microprofile.metrics.Counter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;

/**
 * OpenTelemetry-backed implementation of MicroProfile Metrics {@link Counter}.
 * Delegates all counter operations to an underlying OTel {@link LongCounter}.
 * Tags from MetricID are converted to OTel attributes.
 */
final class OtelCounter implements Counter {

    private final LongCounter delegate;
    private final Attributes attributes;

    OtelCounter(LongCounter delegate) {
        this.delegate = delegate;
        this.attributes = Attributes.empty();
    }

    OtelCounter(LongCounter delegate, String scope, Map<String, String> tags) {
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
    public void inc() {
        delegate.add(1, attributes);
    }

    @Override
    public void inc(long n) {
        delegate.add(n, attributes);
    }



    @Override
    public long getCount() {
        throw new UnsupportedOperationException(
            "Synchronous counter reads are not supported in the OpenTelemetry bridge. "
            + "Use a monitoring backend to query exported metrics."
        );
    }

}
