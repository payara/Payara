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
package fish.payara.nucleus.requesttracing.domain;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author mertcaliskan
 *
 * Event class that stores traced values.
 */
public class RequestEvent {

    private final UUID id;
    private UUID conversationId;
    private final long timestamp;
    private long traceTime;
    private final EventType eventType;
    private HashMap<String,String> properties;
    private String eventName;

    public RequestEvent(String eventName) {
        this(EventType.REQUEST_EVENT,eventName);
    }

    public RequestEvent(EventType eventType, String eventName) {
        this.id = UUID.randomUUID();
        this.timestamp = System.nanoTime();
        this.eventType = eventType;
        this.eventName = eventName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getTraceTime() {
        return traceTime;
    }

    public void setTraceTime(long traceTime) {
        this.traceTime = traceTime;
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
    
    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, value);
    }
    
    @Override
    public String toString() {
        
        StringBuilder result = new StringBuilder("\n\"TraceEvent\": {");
        result.append("\"eventType\": \"").append(eventType).append("\",")
                .append("\"eventName\":\"").append(eventName).append("\",")
                .append("\"id=\":\"").append(id).append("\",")
                .append("\"conversationId=\":\"").append(conversationId).append("\",")
                .append("\"timestamp=\":\"").append(timestamp).append("\",");

        if (properties != null) {
            for (String key : properties.keySet()) {
                result.append("\"").append(key).append("\": \"").append(properties.get(key)).append("\",");
            }
        }
        
        result.append("\"traceTime=\":\"").append(traceTime).append('"');
        result.append('}');
        return result.toString();
    }
}
