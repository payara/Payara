/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Class representing a full Request Trace. Stored in a ThreadLocal in the
 * Request Event Store
 * @author steve
 */
public class RequestTrace {

    public RequestTrace() {
        trace = new LinkedList<>();
    }
    
    private boolean started;
    private boolean completed;
    private long startTime;
    private long elapsedTime;
    private LinkedList<RequestEvent> trace;
    
    void addEvent(RequestEvent requestEvent) {
        // Do not add trace events if completed
        if (completed && requestEvent.getEventType() != EventType.TRACE_START) {
            return;
        }
        
        if (null != requestEvent.getEventType()) switch (requestEvent.getEventType()) {
            case TRACE_START:
                trace.clear();
                startTime = requestEvent.getTimestamp();
                requestEvent.setConversationId(requestEvent.getId());
                trace.add(requestEvent);
                started = true;
                completed = false;
                break;
            case REQUEST_EVENT:{
                if (!started) {
                    return;
                }
                RequestEvent startEvent = trace.getFirst();  
                requestEvent.setConversationId(startEvent.getConversationId());
                requestEvent.setTraceTime(requestEvent.getTimestamp()- startTime);
                trace.add(requestEvent);
                    break;
                }
            case TRACE_END:{
                if (!started) {
                    return;
                }
                RequestEvent startEvent = trace.getFirst();
                requestEvent.setConversationId(startEvent.getConversationId());
                requestEvent.setTraceTime(requestEvent.getTimestamp() - startTime);
                trace.add(requestEvent);
                elapsedTime = TimeUnit.MILLISECONDS.convert(requestEvent.getTimestamp() - startTime, TimeUnit.NANOSECONDS);
                completed = true;
                    break;
                }
            default:
                break;
        }
    }
    
    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\"RequestTrace\": {");
        sb.append("\"startTime\":\"").append(startTime).append('"')
          .append(",\"elapsedTime\":\"").append(elapsedTime).append('"').append(',');
        for (RequestEvent re : trace) {
            sb.append(re.toString()); 
            if (re.getEventType() != EventType.TRACE_END) {
                sb.append(',');
            }
        }
        sb.append("}}");
        return sb.toString();
    }
    
    // methods for testing
    boolean isStarted() {
        return started;
    }
    
    LinkedList<RequestEvent> getTrace() {
        return trace;
    }

    long getStartTime() {
        return startTime;
    }

    void setConversationID(UUID newID) {
        for (RequestEvent requestEvent : trace) {
            requestEvent.setConversationId(newID);
        }
    }

    UUID getConversationID() {
        UUID result = null;
        RequestEvent re = trace.getFirst();
        if (re != null) {
            result = re.getConversationId();
        }
        return result;
    }

    boolean isCompleted() {
        return completed;
    }
}
