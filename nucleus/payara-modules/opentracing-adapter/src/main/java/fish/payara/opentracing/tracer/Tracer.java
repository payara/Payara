/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.opentracing.tracer;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.notification.requesttracing.RequestTraceSpan.SpanContextRelationshipType;
import fish.payara.notification.requesttracing.RequestTraceSpanContext;
import fish.payara.nucleus.requesttracing.RequestTracingService;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.api.Globals;

/**
 * Implementation of the OpenTracing Tracer class.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class Tracer implements io.opentracing.Tracer {

    private final String applicationName;
    private final ScopeManager scopeManager;
    
    private static final String TRACEID_KEY = "traceid";
    private static final String SPANID_KEY = "spanid";
    
    /**
     * Constructor that registers this Tracer to an application.
     *
     * @param applicationName The application to register this tracer to
     */
    public Tracer(String applicationName) {
        this.applicationName = applicationName;
        scopeManager = new fish.payara.opentracing.ScopeManager();
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new SpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        RequestTraceSpanContext payaraSpanContext = (RequestTraceSpanContext) spanContext;
        Iterable<Map.Entry<String, String>> baggageItems = payaraSpanContext.baggageItems();
        
        if (carrier instanceof TextMap) {
            TextMap map = (TextMap) carrier;
            
            if (format.equals(Format.Builtin.HTTP_HEADERS)) {          
                for (Map.Entry<String, String> baggage : baggageItems) {
                    map.put(encodeURLString(baggage.getKey()), encodeURLString(baggage.getValue()));
                }
                
                map.put(TRACEID_KEY, encodeURLString(payaraSpanContext.getTraceId().toString()));
                map.put(SPANID_KEY, encodeURLString(payaraSpanContext.getSpanId().toString()));
            } else if (format.equals(Format.Builtin.TEXT_MAP)) {
                for (Map.Entry<String, String> baggage : baggageItems) {
                    map.put(baggage.getKey(), baggage.getValue());
                }
                
                map.put(TRACEID_KEY, payaraSpanContext.getTraceId().toString());
                map.put(SPANID_KEY, payaraSpanContext.getSpanId().toString());
            } else {
                throw new InvalidCarrierFormatException(format, carrier);
            }
        } else if (carrier instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) carrier;
            if (format.equals(Format.Builtin.BINARY)) {
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream objectWriter = new ObjectOutputStream(bytesOut);
                    objectWriter.writeObject(spanContext);
                    objectWriter.flush();
                    buffer.put(bytesOut.toByteArray());
                } catch (IOException ex) {
                    Logger.getLogger(Tracer.class.getName()).log(Level.WARNING, null, ex);
                    throw new UncheckedIOException(ex);
                }                
                
            } else {
                throw new InvalidCarrierFormatException(format, carrier);
            }
        } else {
            throw new InvalidCarrierFormatException(format, carrier);
        }
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        boolean traceIDRecieved = false;
        
        if (carrier instanceof TextMap) {
            TextMap map = (TextMap) carrier;
            Map<String,String> baggageItems = new HashMap<>();
            Iterator<Map.Entry<String, String>> allEntries = map.iterator();
            
            UUID traceId = null;
            UUID spanId = null;

            if (format.equals(Format.Builtin.HTTP_HEADERS)) {
                while (allEntries.hasNext()) {
                    Entry<String, String> entry = allEntries.next();
                    switch (entry.getKey()) {
                        case TRACEID_KEY:
                            traceId = UUID.fromString(decodeURLString(entry.getValue()));
                            traceIDRecieved = true;
                            break;
                        case SPANID_KEY:
                            spanId = UUID.fromString(decodeURLString(entry.getValue()));
                            break;
                        default:
                            baggageItems.put(decodeURLString(entry.getKey()), decodeURLString(entry.getValue()));
                            break;
                    }
                }
            
            } else if (format.equals(Format.Builtin.TEXT_MAP)){
                while (allEntries.hasNext()) {
                    Entry<String, String> entry = allEntries.next();
                    switch (entry.getKey()) {
                        case TRACEID_KEY:
                            traceId = UUID.fromString(entry.getValue());
                            traceIDRecieved = true;
                            break;
                        case SPANID_KEY:
                            spanId = UUID.fromString(entry.getValue());
                            break;
                        default:
                            baggageItems.put(entry.getKey(), entry.getValue());
                            break;
                    }
                }
            } else {
                throw new InvalidCarrierFormatException(format, carrier);
            }
            if (traceIDRecieved) {
                if (spanId == null){
                    throw new IllegalArgumentException("No SpanId recieved");
                }
                return new RequestTraceSpanContext(traceId, spanId);
            } else {
                return null; //Did not recieve a SpanContext
            }
        } else if (carrier instanceof ByteBuffer){
            ByteBuffer buffer = (ByteBuffer) carrier;
            if (format.equals(Format.Builtin.BINARY)){
                try {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.array());
                    ObjectInputStream objectStream = new ObjectInputStream(inputStream);
                    return (SpanContext) objectStream.readObject();
                } catch (IOException | ClassNotFoundException | ClassCastException ex) {
                    Logger.getLogger(Tracer.class.getName()).log(Level.FINER, null, ex);
                    throw new IllegalArgumentException(ex); //Serialised state is invalid
                }
            } else {
                throw new InvalidCarrierFormatException(format, carrier);
            }
        }

        throw new InvalidCarrierFormatException(format, carrier);
    }

    @Override
    public ScopeManager scopeManager() {
        return scopeManager;
    }

    @Override
    public Span activeSpan() {
        Scope activeScope = scopeManager().active();
        if (activeScope != null) {
            return activeScope.span();
        }
        return null;
    }
    
    private String decodeURLString(String toDecode) {
        try {
            return URLDecoder.decode(toDecode, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private String encodeURLString(String toEncode) {
        try {
            return URLEncoder.encode(toEncode, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Implementation of the OpenTracing SpanBuilder class.
     */
    public class SpanBuilder implements io.opentracing.Tracer.SpanBuilder {

        private boolean ignoreActiveSpan;
        private long microsecondsStartTime;
        private final fish.payara.opentracing.span.Span span;
        private RequestTracingService requestTracing;

        /**
         * Constructor that gives the Span an operation name.
         *
         * @param operationName The name to give the Span.
         */
        public SpanBuilder(String operationName) {
            ignoreActiveSpan = false;
            microsecondsStartTime = 0;
            span = new fish.payara.opentracing.span.Span(operationName, applicationName);
            requestTracing = Globals.getDefaultBaseServiceLocator().getService(RequestTracingService.class);
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parentSpanContext) {
            span.addSpanReference(((RequestTraceSpanContext) parentSpanContext),
                    RequestTraceSpan.SpanContextRelationshipType.ChildOf);
            return this;
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            span.addSpanReference((RequestTraceSpanContext) referencedContext, SpanContextRelationshipType.valueOf(referenceType));
            return this;
        }

        @Override
        public SpanBuilder asChildOf(Span parentSpan) {
            asChildOf(parentSpan.context());
            return this;
        }

        @Override
        public SpanBuilder ignoreActiveSpan() {
            ignoreActiveSpan = true;
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value) {
            span.setTag(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value) {
            span.setTag(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value) {
            span.setTag(key, value);
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds) {
            microsecondsStartTime = microseconds;
            return this;
        }

        @Override
        public Scope startActive(boolean bln) {
            Scope origin = scopeManager().active();
            if (origin != null) {
                Span parent = origin.span();
                asChildOf(parent);
            }
            origin = scopeManager.activate(span, bln);
            
            if (requestTracing != null && !requestTracing.isTraceInProgress()) {
                if (span.getSpanReferences().isEmpty()) {
                    span.setEventType(EventType.TRACE_START);
                } else {
                    span.setEventType(EventType.PROPAGATED_TRACE);
                    
                    // Assume the first parent reference found has the correct traceId - would it ever not be?
                    span.setTraceId(span.getSpanReferences().get(0).getReferenceSpanContext().getTraceId());
                }
                
                requestTracing.startTrace(span);
            }
            
            return origin;
            
        }

        @Override
        public Span startManual() {
            // If we shouldn't ignore the currently active span, set it as this span's parent
            if (!ignoreActiveSpan) {
                fish.payara.opentracing.span.Span activeSpan = (fish.payara.opentracing.span.Span) activeSpan();

                if (activeSpan != null) {
                    span.addSpanReference(activeSpan.getSpanContext(), SpanContextRelationshipType.ChildOf);
                }
            }

            // If we've specified a start time, set it, otherwise set it to now
            if (microsecondsStartTime != 0) {
                span.setStartTime(microsecondsStartTime);
            } else {
                span.setStartTime(TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
            }

            if (requestTracing != null && !requestTracing.isTraceInProgress()) {
                if (span.getSpanReferences().isEmpty()) {
                    span.setEventType(EventType.TRACE_START);
                } else {
                    span.setEventType(EventType.PROPAGATED_TRACE);
                    
                    // Assume the first parent reference found has the correct traceId - would it ever not be?
                    span.setTraceId(span.getSpanReferences().get(0).getReferenceSpanContext().getTraceId());
                }
                
                requestTracing.startTrace(span);
            } 
            
            return span;
        }

        @Override
        public Span start() {
            return startManual();
        }

    }
    
    /**
     * Present here for JDK7 compatibilty
     * @see java.io.UncheckedIOException
     */
    public class UncheckedIOException extends RuntimeException {
        
        public UncheckedIOException(Exception ex){
            super(ex);
        }
    }

}
