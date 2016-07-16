/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
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

import java.util.UUID;

/**
 * @author mertcaliskan
 *
 * Base event class that stores traced values with the help of interceptors.
 */
public class RequestEvent implements Comparable<RequestEvent> {

    private UUID id;
    private UUID conversationId;
    private String server;
    private String domain;
    private long timestamp;
    private Long elapsedTime;
    private EventType eventType;

    RequestEvent() {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }

    public RequestEvent(EventType eventType, String domain, String server) {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.eventType = eventType;
        this.domain = domain;
        this.server = server;
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

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "RequestEvent{" +
                "alarmType=" + eventType +
                ", id=" + id +
                ", conversationId=" + conversationId +
                ", server='" + server + '\'' +
                ", domain='" + domain + '\'' +
                ", timestamp=" + timestamp +
                ", elapsedTime=" + elapsedTime +
                '}';
    }

    /**
     * sort request traces in descending order according to the timestamp.
     * it means that the latest occurring trace will be on top.
     *
     * @param o
     * @return
     */
    public int compareTo(RequestEvent o) {
        return this.timestamp > o.timestamp ? 1 : -1;
    }
}
