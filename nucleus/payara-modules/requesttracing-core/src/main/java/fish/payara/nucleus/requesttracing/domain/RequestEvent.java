/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.domain;

import java.text.DateFormat;
import java.util.Date;
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
    private final long timeOccured;
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
        this.timeOccured = System.currentTimeMillis();
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
    
    /**
     * Gets the time in milliseconds since the epoch (midnight, January 1st 1970)
     * when the the request event occurred.
     * 
     * @return the time the trace occured
     */
    public long getTimeOccured(){
        return timeOccured;
    }

    /**
     * Gets the elapsed time since the current request trace has started
     * @return nanoseconds since the current request trace has started.
     */
    public long getTraceTime() {
        return traceTime;
    }

    /**
     * Sets the elapsed time since the current request trace has started
     * @param traceTime Nanoseconds since the current request trace has started
     */
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
                .append("\"timeOccured=\":\"").append(DateFormat.getDateTimeInstance().format(new Date(timeOccured))).append("\",");

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
