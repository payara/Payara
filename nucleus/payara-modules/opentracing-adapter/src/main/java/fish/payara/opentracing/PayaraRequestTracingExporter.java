/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.opentracing;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class PayaraRequestTracingExporter implements SpanExporter {
    private static final Logger LOGGER = Logger.getLogger(PayaraRequestTracingExporter.class.getName());

    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    private final RequestTracingService requestTracingService;

    private final ExecutorService executorService;

    public PayaraRequestTracingExporter(RequestTracingService requestTracingService, ExecutorService payaraExecutorService) {
        this.requestTracingService = requestTracingService;
        this.executorService = payaraExecutorService;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        CompletableResultCode result = new CompletableResultCode();
        this.executorService.submit(() -> {
            try {
                collection.forEach((data) -> requestTracingService.traceSpan(convert(data)));
                result.succeed();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to export OpenTelemetry span to Payara Request Tracing", e);
                result.fail();
            }
        });
        return result;
    }

    RequestTraceSpan convert(SpanData data) {
        RequestTraceSpan result = new RequestTraceSpan(EventType.PROPAGATED_TRACE, data.getName());
        result.getSpanContext().setTraceId(parseTraceId(data));
        // we cannot set spanId
        result.setStartInstant(Instant.ofEpochSecond(
                data.getStartEpochNanos() / NANOS_PER_SECOND,
                data.getStartEpochNanos() % NANOS_PER_SECOND));
        result.setSpanDuration(data.getEndEpochNanos() - data.getStartEpochNanos());
        data.getAttributes().forEach((key, value) -> result.addSpanTag(key.getKey(), value.toString()));
        // exporter has no access to baggage, that goes to propagators only.
        return result;
    }

    private UUID parseTraceId(SpanData data) {
        return parseTraceId(data.getTraceId());
    }

    static UUID parseTraceId(String traceId) {
        if (traceId.length() >= 32) {
            // Long.parseLong does not like unsigned data
            // we trim everything beyond 32nd byte
            long high = parseUnsignedHex(traceId, 0, 0);
            long low = parseUnsignedHex(traceId, 16, 0);
            return new UUID(high, low);
        } else {
            long high = 0;
            if (traceId.length() > 16) {
                high = parseUnsignedHex(traceId, 0, 32 - traceId.length());
            }
            long low = parseUnsignedHex(traceId, Math.max(traceId.length() - 16, 0), Math.max(16 - traceId.length(), 0));
            return new UUID(high, low);
        }
    }

    static long parseUnsignedHex(CharSequence input, int start, int padding) {
        long result = 0;
        for (int i = start, shift = 60 - 4 * padding; shift >= 0; i++, shift -= 4) {
            long digit = Character.digit(input.charAt(i), 16);
            result |= digit << shift;
        }
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
