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
package fish.payara.opentracing.span;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the OpenTracing ActiveSpan class.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class ActiveSpan implements io.opentracing.ActiveSpan {

    private final Span wrappedSpan;
    private final AtomicInteger referenceCount;
    private final ActiveSpan previouslyActiveSpan;
    private final ActiveSpanSource activeSpanSource;

    /**
     * Construct an ActiveSpan object with a reference to the actual Span instance, the previous ActiveSpan instance, 
     * and the ActiveSpanSource.
     * 
     * @param span The Span that this instance refers to
     * @param previouslyActiveSpan The ActiveSpan to restore in the ActiveSpanSource when done
     * @param activeSpanSource The ActiveSpanSource that this is registered to
     */
    public ActiveSpan(Span span, ActiveSpan previouslyActiveSpan, ActiveSpanSource activeSpanSource) {
        this.wrappedSpan = span;
        this.previouslyActiveSpan = previouslyActiveSpan;
        this.activeSpanSource = activeSpanSource;
        referenceCount = new AtomicInteger(1);
    }

    @Override
    public io.opentracing.SpanContext context() {
        return wrappedSpan.context();
    }

    @Override
    public io.opentracing.ActiveSpan setTag(String tagName, String tagValue) {
        wrappedSpan.setTag(tagName, tagValue);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan setTag(String tagName, boolean tagValue) {
        wrappedSpan.setTag(tagName, tagValue);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan setTag(String tagName, Number tagValue) {
        wrappedSpan.setTag(tagName, tagValue);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan log(Map<String, ?> map) {
        wrappedSpan.log(map);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan log(long timestampMicroseconds, Map<String, ?> map) {
        wrappedSpan.log(timestampMicroseconds, map);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan log(String logEvent) {
        wrappedSpan.log(logEvent);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan log(long timestampMicroseconds, String logEvent) {
        wrappedSpan.log(timestampMicroseconds, logEvent);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan log(String key, Object value) {
        wrappedSpan.log(key, value);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan log(long timestamp, String key, Object value) {
        wrappedSpan.log(key, value);
        return this;
    }

    @Override
    public io.opentracing.ActiveSpan setBaggageItem(String key, String value) {
        wrappedSpan.setBaggageItem(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return wrappedSpan.getBaggageItem(key);
    }

    @Override
    public io.opentracing.ActiveSpan setOperationName(String operationName) {
        wrappedSpan.setOperationName(operationName);
        return this;
    }

    @Override
    public void deactivate() {
        // Check that the currently active span is actually this instance
        if (activeSpanSource.activeSpan() == this) {
            // Restore the previous ActiveSpan
            activeSpanSource.makeActive(previouslyActiveSpan);
            
            // Decrement the reference count and check if we should finish the actual Span
            referenceCount.decrementAndGet();
            if (shouldFinishSpan()) {
                wrappedSpan.finish();
            }
        }
    }

    @Override
    public void close() {
        deactivate();
    }

    @Override
    public Continuation capture() {
        referenceCount.incrementAndGet();
        return new Continuation();
    }

    /**
     * Sets the start time of this ActiveSpan.
     * 
     * @param startTimeMicros The start time in Microseconds
     */
    public void setStartTime(long startTimeMicros) {
        wrappedSpan.setStartTime(startTimeMicros);
    }

    /**
     * Returns the Span that this ActiveSpan refers to.
     * 
     * @return The Span that this instance refers to.
     */
    public Span getWrappedSpan() {
        return wrappedSpan;
    }

    /**
     * Helper method to determine if we should finish the Span this instance refers to.
     * 
     * @return True if the wrapped Span should be finished
     */
    public boolean shouldFinishSpan() {
        // If the reference count is 0 or lower (shouldn't ever be lower!), the Span should be finished
        if (referenceCount.get() <= 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the OpenTracing Continuation class.
     */
    public class Continuation implements io.opentracing.ActiveSpan.Continuation {

        @Override
        public io.opentracing.ActiveSpan activate() {
            // Make the enclosing instance the active span and return it
            activeSpanSource.makeActive(ActiveSpan.this);
            return ActiveSpan.this;
        }

    }

}
