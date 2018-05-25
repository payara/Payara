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

import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.notification.requesttracing.RequestTraceSpan.SpanContextRelationshipType;
import fish.payara.opentracing.span.ActiveSpanSource;
import io.opentracing.ActiveSpan;
import io.opentracing.BaseSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the OpenTracing Tracer class.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class Tracer extends ActiveSpanSource implements io.opentracing.Tracer {

    private final String applicationName;
    
    /**
     * Constructor that registers this Tracer to an application.
     * 
     * @param applicationName The application to register this tracer to
     */
    public Tracer(String applicationName) {
        this.applicationName = applicationName;
    }
    
    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new SpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        // To Do
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        // To Do
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    /**
     * Implementation of the OpenTracing SpanBuilder class.
     */
    public class SpanBuilder implements io.opentracing.Tracer.SpanBuilder {

        private boolean ignoreActiveSpan;
        private long microsecondsStartTime;
        private final fish.payara.opentracing.span.Span span;

        /**
         * Constructor that gives the Span an operation name.
         * 
         * @param operationName The name to give the Span.
         */
        public SpanBuilder(String operationName) {
            ignoreActiveSpan = false;
            microsecondsStartTime = 0;
            span = new fish.payara.opentracing.span.Span(operationName, applicationName);
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parentSpanContext) {
            span.addSpanReference(((fish.payara.opentracing.span.Span.SpanContext) parentSpanContext),
                    RequestTraceSpan.SpanContextRelationshipType.ChildOf);
            return this;
        }

        @Override
        public SpanBuilder asChildOf(BaseSpan<?> bs) {
            span.addSpanReference((fish.payara.opentracing.span.Span.SpanContext) bs.context(), 
                    SpanContextRelationshipType.ChildOf);
            return this;
        }
        
        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            span.addSpanReference((fish.payara.opentracing.span.Span.SpanContext) referencedContext,
                    SpanContextRelationshipType.valueOf(referenceType));
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
        public ActiveSpan startActive() {
            return makeActive(startManual());
        }
        
        @Override
        public Span startManual() {
            // If we shouldn't ignore the currently active span, set it as this span's parent
            if (!ignoreActiveSpan) {
                fish.payara.opentracing.span.ActiveSpan activeSpan = (fish.payara.opentracing.span.ActiveSpan) activeSpan();
                
                if (activeSpan != null) {
                    span.addSpanReference(activeSpan.getWrappedSpan().getSpanContext(), SpanContextRelationshipType.ChildOf);
                }
            }

            // If we haven't set a start time manually, set it to now
            if (microsecondsStartTime == 0) {
                span.setStartTime(microsecondsStartTime);
            } else {
                span.setStartTime(TimeUnit.MILLISECONDS.convert(System.currentTimeMillis(), TimeUnit.MICROSECONDS));
            }

            return span;
        }

        @Override
        public Span start() {
            return startManual();
        }

    }

}
