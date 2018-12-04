/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.requesttracing;

import java.io.Serializable;
import java.util.ArrayList;;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * @author mertcaliskan
 *
 * Event class that stores traced values.
 */
public class RequestTraceSpan implements Serializable, Comparable<RequestTraceSpan> {
    private final RequestTraceSpanContext spanContext;
    private long timestamp;
    private long startTime;
    private long endTime;
    private long spanDuration;
    private EventType eventType;
    private final Map<String, String> spanTags;
    private final List<RequestTraceSpanLog> spanLogs;
    private String eventName;
    private final List<SpanReference> spanReferences;

    protected RequestTraceSpan() {
        this.spanContext = new RequestTraceSpanContext();
        this.spanTags = new HashMap<>();
        this.spanLogs = new ArrayList<>();
        this.spanReferences = new ArrayList<>();
    }
    
    public RequestTraceSpan(String eventName) {
        this(EventType.REQUEST_EVENT, eventName);
    }

    public RequestTraceSpan(EventType eventType, String eventName) {
        this.spanContext = new RequestTraceSpanContext();
        this.timestamp = System.nanoTime();
        this.startTime = System.currentTimeMillis();
        this.eventType = eventType;
        this.eventName = eventName;
        this.spanTags = new HashMap<>();
        this.spanLogs = new ArrayList<>();
        this.spanReferences = new ArrayList<>();
    }
    
    public RequestTraceSpan(EventType eventType, String eventName, UUID propagatedTraceId, UUID propagatedParentId, 
            SpanContextRelationshipType spanContextRelationship) {
        this.spanContext = new RequestTraceSpanContext(propagatedTraceId);
        this.timestamp = System.nanoTime();
        this.startTime = System.currentTimeMillis();
        this.eventType = eventType;
        this.eventName = eventName;
        this.spanTags = new HashMap<>();
        this.spanLogs = new ArrayList<>();
        this.spanReferences = new ArrayList<>();
        spanReferences.add(new SpanReference(new RequestTraceSpanContext(propagatedTraceId, propagatedParentId), 
                spanContextRelationship));
    }

    public UUID getId() {
        return spanContext.getSpanId();
    }

    public UUID getTraceId() {
        return spanContext.getTraceId();
    }
    
    public void setTraceId(UUID traceId) {
        spanContext.setTraceId(traceId);
    }
    
    public RequestTraceSpanContext getSpanContext() {
        return spanContext;
    }
    
    /**
     * Gets the nanotime when the event occurred.
     * 
     * This time is NOT related to the epoch or start time of anything, but an
     * arbitrary point. This is only to be used for comparison.
     * <p>
     * See also {@link System#nanoTime()}
     * @return 
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the time in milliseconds since the epoch (midnight, January 1st 1970)
     * when the the request event occurred.
     * 
     * @return the time the trace occurred
     */
    public long getTimeOccured() {
        return startTime;
    }
    
    public void setTimeOccured(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the elapsed time since the current request trace has started
     * @return nanoseconds since the current request trace has started.
     */
    public long getSpanDuration() {
        return spanDuration;
    }

    /**
     * Sets the elapsed time since the current request trace has started
     * @param spanTime Nanoseconds since the current request trace has started
     */
    public void setSpanDuration(long spanTime) {
        this.spanDuration = spanTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    /**
     * Adds more information about a span
     * @param name
     * @param value 
     */
    public void addSpanTag(String name, String value) {
        if (value != null) {
            // Escape any quotes
            spanTags.put(name, value.replaceAll("\"", "\\\\\""));
        } else {
            spanTags.put(name, value);
        }
    }
    
    public String getSpanTag(String tag) {
        return spanTags.get(tag);
    }
    
    public Map<String, String> getSpanTags() {
        return spanTags;
    }
    
    public void addSpanLog(RequestTraceSpanLog spanLog) {
        spanLogs.add(spanLog);
    }
    
    public void addSpanReference(RequestTraceSpanContext spanContext, SpanContextRelationshipType relationshipType) {
        spanReferences.add(new SpanReference(spanContext, relationshipType));
    }
    
    public List<SpanReference> getSpanReferences() {
        return spanReferences;
    }
    
    public long getTraceEndTime() {
        return endTime;
    }
    
    public void setTraceEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public void setEventType(EventType spanType) {
        this.eventType = spanType;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("\n{");
        result.append("\"operationName\":\"").append(eventName).append("\",")
                .append("\"spanContext\":{")
                .append("\"spanId\":\"").append(spanContext.getSpanId()).append("\",")
                .append("\"traceId\":\"").append(spanContext.getTraceId()).append("\"");
        
        result.append("},");
        
        result.append("\"startTime\":\"").append(startTime).append("\",");
        result.append("\"endTime\":\"").append(endTime).append("\",");
        result.append("\"traceDuration\":\"").append(spanDuration).append("\"");
        
        if (spanTags != null && !spanTags.isEmpty()) {
            result.append(",\"spanTags\":[");
            for (String key : spanTags.keySet()) {
                result.append("{\"").append(key).append("\": \"").append(spanTags.get(key)).append("\"},");
            }
            result.deleteCharAt(result.length() - 1);
            result.append("]");
        }
        
        if (spanLogs != null && !spanLogs.isEmpty()) {
            result.append(",\"spanLogs\":[");
            for (RequestTraceSpanLog spanLog : spanLogs) {
                Map<String, String> logEntries = spanLog.getLogEntries();
                
                result.append("{");
                result.append("\"logDetails\":[");
                for (Entry<String, String> logEntry : logEntries.entrySet()) {
                    result.append("{\"");
                    result.append(logEntry.getKey());
                    result.append("\":\"");
                    result.append(logEntry.getValue());
                    result.append("\"},");
                }
                result.deleteCharAt(result.length() - 1);
                result.append("],");
                result.append("\"logTime\":\"");
                result.append(spanLog.getTimeMillis());
                result.append("\"},");
            }
            result.deleteCharAt(result.length() - 1);
            result.append("]");
        }
        
        if (spanReferences != null && !spanReferences.isEmpty()) {
            result.append(",\"references\":[");
            for (SpanReference reference : spanReferences) {
                result.append("{\"spanContext\":{")
                        .append("\"spanId\":\"").append(reference.getReferenceSpanContext().getSpanId()).append("\",")
                        .append("\"traceId\":\"").append(reference.getReferenceSpanContext().getTraceId())
                        .append("\"},");
                
                result.append("\"relationshipType\":\"").append(reference.getSpanContextRelationshipType())
                        .append("\"},");
            }
            result.deleteCharAt(result.length() - 1);
            result.append("]");
        }        
        
        result.append("}");
        return result.toString();
    }      

    @Override
    public int compareTo(RequestTraceSpan span) {
        return Long.compare(startTime, span.startTime);
    }
    public class SpanReference implements Serializable {
        
        private final RequestTraceSpanContext referenceSpanContext;
        private final SpanContextRelationshipType relationshipType;
        
        private SpanReference(RequestTraceSpanContext referenceSpanContext, SpanContextRelationshipType relationshipType) {
            this.referenceSpanContext = referenceSpanContext;
            this.relationshipType = relationshipType;
        }
        
        public RequestTraceSpanContext getReferenceSpanContext() {
            return referenceSpanContext;
        }
        
        public SpanContextRelationshipType getSpanContextRelationshipType() {
            return relationshipType;
        }
    }
    
    public enum SpanContextRelationshipType {
        ChildOf,
        FollowsFrom;
    }
    
}
