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
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.microprofile.telemetry.tracing;

import fish.payara.opentracing.OpenTelemetryService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.glassfish.internal.api.Globals;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@ApplicationScoped
public class OpenTelemetryTracerProducer {
    private final OpenTelemetryService openTelemetry;

    public OpenTelemetryTracerProducer() {
        openTelemetry = Globals.getDefaultBaseServiceLocator().getService(OpenTelemetryService.class);
    }

    @Produces
    @ApplicationScoped
    Tracer createTracer() {
        return openTelemetry.getCurrentTracer();
    }

    @Produces
    Baggage currentBaggage() {
        // baggage is very sensitive to time when it is created, it is ultimately the best just use Baggage.current()
        // for every invocation.
        return FORWARDED_BAGGAGE;
    }

    @Produces
    Span currentSpan() {
        // span is also very sensitive to invocation vs creation time, so we forward to current as well
        return FORWARDED_SPAN;
    }

    @Produces
    @ApplicationScoped
    OpenTelemetry currentTelemetry() {
        return openTelemetry.getCurrentSdk();
    }

    private static final Baggage FORWARDED_BAGGAGE = new ForwardedBaggage();

    static class ForwardedBaggage implements Baggage {
        @Override
        public int size() {
            return Baggage.current().size();
        }

        @Override
        public void forEach(BiConsumer<? super String, ? super BaggageEntry> consumer) {
            Baggage.current().forEach(consumer);
        }

        @Override
        public Map<String, BaggageEntry> asMap() {
            return Baggage.current().asMap();
        }

        @Override
        public String getEntryValue(String entryKey) {
            return Baggage.current().getEntryValue(entryKey);
        }

        @Override
        public BaggageBuilder toBuilder() {
            return Baggage.current().toBuilder();
        }

        @Override
        public Context storeInContext(Context context) {
            return Baggage.current().storeInContext(context);
        }

        @Override
        public boolean isEmpty() {
            return Baggage.current().isEmpty();
        }
    }

    private static final Span FORWARDED_SPAN = new ForwardedSpan();

    static class ForwardedSpan implements Span {
        @Override
        public Span setAttribute(String key, String value) {
            return Span.current().setAttribute(key, value);
        }

        @Override
        public Span setAttribute(String key, long value) {
            return Span.current().setAttribute(key, value);
        }

        @Override
        public Span setAttribute(String key, double value) {
            return Span.current().setAttribute(key, value);
        }

        @Override
        public Span setAttribute(String key, boolean value) {
            return Span.current().setAttribute(key, value);
        }

        @Override
        public <T> Span setAttribute(AttributeKey<T> key, T value) {
            return Span.current().setAttribute(key, value);
        }

        @Override
        public Span setAttribute(AttributeKey<Long> key, int value) {
            return Span.current().setAttribute(key, value);
        }

        @Override
        public Span setAllAttributes(Attributes attributes) {
            return Span.current().setAllAttributes(attributes);
        }

        @Override
        public Span addEvent(String name) {
            return Span.current().addEvent(name);
        }

        @Override
        public Span addEvent(String name, long timestamp, TimeUnit unit) {
            return Span.current().addEvent(name, timestamp, unit);
        }

        @Override
        public Span addEvent(String name, Instant timestamp) {
            return Span.current().addEvent(name, timestamp);
        }

        @Override
        public Span addEvent(String name, Attributes attributes) {
            return Span.current().addEvent(name, attributes);
        }

        @Override
        public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
            return Span.current().addEvent(name, attributes, timestamp, unit);
        }

        @Override
        public Span addEvent(String name, Attributes attributes, Instant timestamp) {
            return Span.current().addEvent(name, attributes, timestamp);
        }

        @Override
        public Span setStatus(StatusCode statusCode) {
            return Span.current().setStatus(statusCode);
        }

        @Override
        public Span setStatus(StatusCode statusCode, String description) {
            return Span.current().setStatus(statusCode, description);
        }

        @Override
        public Span recordException(Throwable exception) {
            return Span.current().recordException(exception);
        }

        @Override
        public Span recordException(Throwable exception, Attributes additionalAttributes) {
            return Span.current().recordException(exception, additionalAttributes);
        }

        @Override
        public Span updateName(String name) {
            return Span.current().updateName(name);
        }

        @Override
        public void end() {
            Span.current().end();
        }

        @Override
        public void end(long timestamp, TimeUnit unit) {
            Span.current().end(timestamp, unit);
        }

        @Override
        public void end(Instant timestamp) {
            Span.current().end(timestamp);
        }

        @Override
        public SpanContext getSpanContext() {
            return Span.current().getSpanContext();
        }

        @Override
        public boolean isRecording() {
            return Span.current().isRecording();
        }

        @Override
        public Context storeInContext(Context context) {
            return Span.current().storeInContext(context);
        }
    }
}
