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

import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.notification.requesttracing.RequestTraceSpanLog;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentracing.SpanContext;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 * Implementation of the OpenTracing Span class. These Spans get recorded by the Request Tracing Service.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class Span extends RequestTraceSpan implements io.opentracing.Span {
    
    private transient RequestTracingService requestTracing;
    private final String applicationName;
    
    @PostConstruct
    public void postConstruct() {
        // Initialise the HK2 service variables
        ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();

        if (serviceLocator != null) {
            requestTracing = serviceLocator.getService(RequestTracingService.class);
        }
    }

    /**
     * Constructor that creates a RequestTraceSpan object and sets the application name that this Span was created from.
     * 
     * @param operationName The name of the span
     * @param applicationName The name of the application that this Span was created from
     */
    public Span(String operationName, String applicationName) {
        super(operationName);
        this.applicationName = applicationName;
    }
    
    /**
     * Returns the name of the application that this Span was created from.
     * 
     * @return The name of the application that this Span was created from.
     */
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public io.opentracing.SpanContext context() {
        return (SpanContext) getSpanContext();
    }

    @Override
    public io.opentracing.Span setTag(String tagName, String tagValue) {
        // Pass through to the Request Tracing Service
        addSpanTag(tagName, tagValue);
        return this;
    }

    @Override
    public io.opentracing.Span setTag(String tagName, boolean tagValue) {
        // Pass through to the Request Tracing Service
        addSpanTag(tagName, Boolean.toString(tagValue));
        return this;
    }

    @Override
    public io.opentracing.Span setTag(String tagName, Number tagValue) {
        // Pass through to the Request Tracing Service
        addSpanTag(tagName, String.valueOf(tagValue));
        return this;
    }

    @Override
    public io.opentracing.Span log(Map<String, ?> map) {
        // Create a RequestTracingSpanLog, add all of the map entries, and pass it through to the Request Tracing Service
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            spanLog.addLogEntry(entry.getKey(), String.valueOf(entry.getValue()));
        }

        addSpanLog(spanLog);

        return this;
    }

    @Override
    public io.opentracing.Span log(long timestampMicroseconds, Map<String, ?> map) {
        // Create a RequestTracingSpanLog, add all of the map entries, and pass it through to the Request Tracing Service
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog(
                convertTimestampMicrosToTimestampMillis(timestampMicroseconds));

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            spanLog.addLogEntry(entry.getKey(), String.valueOf(entry.getValue()));
        }
        
        addSpanLog(spanLog);

        return this;
    }

    @Override
    public io.opentracing.Span log(String logEvent) {
        // Create a RequestTracingSpanLog and pass it through to the Request Tracing Service
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog(logEvent);
        addSpanLog(spanLog);

        return this;
    }

    @Override
    public io.opentracing.Span log(long timestampMicroseconds, String logEvent) {
        // Create a RequestTracingSpanLog and pass it through to the Request Tracing Service
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog(
                convertTimestampMicrosToTimestampMillis(timestampMicroseconds),
                logEvent);
        addSpanLog(spanLog);

        return this;
    }

    @Override
    public io.opentracing.Span setBaggageItem(String key, String value) {
        // Pass through to the Request Tracing Service
        getSpanContext().addBaggageItem(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        // Pass through to the Request Tracing Service
        return getSpanContext().getBaggageItems().get(key);
    }

    @Override
    public io.opentracing.Span setOperationName(String operationName) {
        // Pass through to the Request Tracing Service
        setEventName(operationName);
        return this;
    }

    @Override
    public void finish() {
        // Pass through to the Request Tracing Service
        getRequestTracingServiceIfNull();
        requestTracing.traceSpan(this);
    }

    @Override
    public void finish(long finishMicros) {
        // Convert the timestamp to that required by the Request Tracing Service
        long finishMillis = convertTimestampMicrosToTimestampMillis(finishMicros);
        
        // Pass through to the Request Tracing Service
        getRequestTracingServiceIfNull();
        requestTracing.traceSpan(this, finishMillis);
    }

    /**
     * Sets the start time of the Span.
     * 
     * @param startTimeMicros The start time of the Span in Microseconds
     */
    public void setStartTime(long startTimeMicros) {
        // Convert the timestamp to that required by the Request Tracing Service and pass through
        super.setTimeOccured(convertTimestampMicrosToTimestampMillis(startTimeMicros));
    }

    /**
     * Helper method that converts a Microsecond timestamp into a Millisecond one.
     * 
     * @param timestampMicroseconds The microseconds timestamp to convert
     * @return The timestamp in Milliseconds
     */
    private long convertTimestampMicrosToTimestampMillis(long timestampMicroseconds) {
        return TimeUnit.MILLISECONDS.convert(timestampMicroseconds, TimeUnit.MICROSECONDS);
    }

    /**
     * Helper method that gets the Request Tracing Service if this instance does not have it.
     */
    private void getRequestTracingServiceIfNull() {
        if (requestTracing == null) {
            ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();

            if (serviceLocator != null) {
                requestTracing = serviceLocator.getService(RequestTracingService.class);
            }
        }
    }

}
