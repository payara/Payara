/*
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

package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import java.text.DateFormat;
import java.util.Date;
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
    private long startTimeEpoched;
    private long endTime;
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
                startTimeEpoched = requestEvent.getTimeOccured();
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
                endTime = requestEvent.getTimeOccured();
                completed = true;
                    break;
                }
            default:
                break;
        }
    }
    
    /**
     * 
     * @return Time for trace in milliseconds
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\"RequestTrace\": {");
        sb.append("\"startTime\":\"").append(DateFormat.getDateTimeInstance().format(new Date(startTimeEpoched))).append('"')
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

    /**
     * Gets the start time of the request trace in nanoseconds. 
     * 
     * This time is NOT related to the epoch or start time of anything, but an
     * arbitrary point. This is only to be used for comparison.
     * <p>
     * See {@link System#nanoTime()} for how this time is generated.
     * <p>
     * See {@link #getStartTimeEpoched} for a time that can be used for an absolute value.
     * @return 
     * 
     */
    long getStartTime() {
        return startTime;
    }
    
    
    
    /**
     * Gets the time in milliseconds since the epoch (midnight, January 1st 1970)
     * when the the request trace started.
     * <p>
     * See {@link System#currentTimeMillis()} for how this time is generated.
     * @return 
     */
    public long getStartTimeEpoched(){
        return startTimeEpoched;
    }
    
    /**
     * Gets the end time of the request trace in milliseconds since the epoch
     * (midnight, January 1st 1970).
     * <p>
     * This value is 0 until the request trace in finished.
     * @return 
     */
    public long getEndTime(){
        return endTime;
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
