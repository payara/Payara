/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static fish.payara.notification.requesttracing.EventType.*;

/**
 * Class representing a full Request Trace. Stored in a ThreadLocal in the
 * Request Event Store
 * @author steve
 */
public class RequestTrace implements Serializable, Comparable<RequestTrace> {
    private static final Logger LOGGER = Logger.getLogger(RequestTrace.class.getName());

    public RequestTrace() {
        trace = new LinkedList<>();
        spanLogs = new LinkedList<>();
    }

    private boolean started;
    private boolean completed;
    private Instant startTime;
    private Instant endTime;
    private long elapsedTime;
    private final LinkedList<RequestTraceSpan> trace;
    private final List<RequestTraceSpanLog> spanLogs;

    /**
     * Add a new event to the series being traced
     * @param span 
     */
    public void addEvent(RequestTraceSpan span) {
        // Do not add trace events if completed
        if (completed 
                && span.getEventType() != TRACE_START
                && span.getEventType() != PROPAGATED_TRACE) {
            return;
        }

        if (null != span.getEventType()) {
            switch (span.getEventType()) {
                case TRACE_START:
                    handleTraceStart(span);
                    break;
                case PROPAGATED_TRACE:
                    handlePropagatedTrace(span);
                    break;
                case REQUEST_EVENT:
                    handleRequestEvent(span);
                    break;
                default:
                    break;
            }
        }
    }

    public void addEvent(RequestTraceSpan span, long timestampMillis) {
        // Do not add trace events if completed
        if (completed
                && span.getEventType() != TRACE_START
                && span.getEventType() != PROPAGATED_TRACE) {
            return;
        }

        if (null != span.getEventType()) {
            switch (span.getEventType()) {
                case TRACE_START:
                    handleTraceStart(span);
                    break;
                case PROPAGATED_TRACE:
                    handlePropagatedTrace(span);
                    break;
                case REQUEST_EVENT:
                    handleRequestEvent(span, timestampMillis);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleTraceStart(RequestTraceSpan span) {
        trace.clear();
        startTime = span.getStartInstant();
        trace.add(span);
        started = true;
        completed = false;
    }

    private void handlePropagatedTrace(RequestTraceSpan span) {
        trace.clear();
        startTime = span.getStartInstant();
        trace.add(span);
        started = true;
        completed = false;
    }

    private void handleRequestEvent(RequestTraceSpan span) {
        if (!started) {
            return;
        }

        RequestTraceSpan rootSpan = trace.getFirst();
        span.setTraceId(rootSpan.getTraceId());
        span.setSpanDuration(span.getStartInstant().until(Instant.now(), ChronoUnit.NANOS));
        span.setTraceEndTime(Instant.now());
        trace.add(span);
    }

    private void handleRequestEvent(RequestTraceSpan span, long timestampMillis) {
        if (!started) {
            return;
        }

        RequestTraceSpan rootSpan = trace.getFirst();
        span.setTraceId(rootSpan.getTraceId());
        span.setSpanDuration(span.getStartInstant().until(Instant.ofEpochMilli(timestampMillis), ChronoUnit.NANOS));
        span.setTraceEndTime(Instant.ofEpochMilli(timestampMillis));
        trace.add(span);
    }

    public void endTrace() {
        endTrace(Instant.now().toEpochMilli());
    }

    public void endTrace(long timestampMillis) {
        if (!started) {
            return;
        }

        Collections.sort(trace);

        RequestTraceSpan startSpan = trace.getFirst();
        endTime = Instant.ofEpochMilli(timestampMillis);
        startSpan.setSpanDuration(startTime.until(endTime, ChronoUnit.NANOS));
        startSpan.setTraceEndTime(endTime);
        elapsedTime = TimeUnit.MILLISECONDS.convert(startSpan.getSpanDuration(), TimeUnit.NANOSECONDS);
        completed = true;
        assignLogs();
        assignReferences();
    }
    
    /**
     * Gets how long the trace took.
     * If the trace has not finished then this will be 0.
     * @return Time for trace in milliseconds
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{\"traceSpans\":[");
        
        for (RequestTraceSpan span : trace) {
            sb.append(span.toString());
            
            if (trace.indexOf(span) != trace.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("\n]}");
        
        return sb.toString();
    }
    
    // methods for testing
    /**
     * Returns true if a trace has started.
     * This will return true even if the trace has completed.
     * @return 
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * Returns a list of all the events that make up the trace.
     * 
     * @return A list containing all of the Spans that constitute this trace.
     */
    public LinkedList<RequestTraceSpan> getTraceSpans() {
        return trace;
    }

    /**
     * Gets the Instant when the span was started
     * See {@link java.time.Instant#now()} for how this time is generated.
     * 
     * @return The Instant for when this span was started.
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the end time of the request trace in milliseconds since the epoch
     * (midnight, January 1st 1970).
     * <p>
     * This value is 0 until the request trace in finished.
     * @return 
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Returns a unique identifier for the trace,
     * which comes from the first event.
     * @return {@code null} if no trace started
     */
    public UUID getTraceId() {
        UUID result = null;
        RequestTraceSpan re = trace.getFirst();
        if (re != null) {
            result = re.getTraceId();
        }
        return result;
    }
    
    public void setTraceId(UUID newID) {
        for (RequestTraceSpan span : trace) {
            span.setTraceId(newID);
        }
    }

    /**
     * Returns true if a complete trace has finished
     * @return 
     */
    public boolean isCompleted() {
        return completed;
    }

    public void addSpanLog(RequestTraceSpanLog spanLog) {
        spanLogs.add(spanLog);
    }
    
    private void assignLogs() {
        for (RequestTraceSpanLog spanLog : spanLogs) {
            
            ListIterator<RequestTraceSpan> iterator = trace.listIterator(trace.size());
            
            while (iterator.hasPrevious()) {
                RequestTraceSpan span = iterator.previous();
                if (spanLog.getTimeMillis() > span.getTimeOccured() 
                        && spanLog.getTimeMillis() < span.getTraceEndTime().toEpochMilli()) {
                    span.addSpanLog(spanLog);
                    break;
                }
            }
        }
    }
    
    private void assignReferences() {
        boolean root = true;
        for (RequestTraceSpan span : trace) {
            if (root) {
                // skip the first trace
                root = false;
            } else {
                RequestTraceSpan bestMatchingParent = null;
                if (span.getTraceEndTime() == null) {
                    LOGGER.info(() -> logUnfinishedSpan(span));
                    span.setTraceEndTime(trace.getFirst().getTraceEndTime());
                    continue;
                }
                for (RequestTraceSpan comparisonSpan : trace) {
                    if (comparisonSpan.getTraceEndTime() == null || span == comparisonSpan) {
                        continue;
                    }
                    if (span.getTimeOccured() > comparisonSpan.getTimeOccured()
                            && span.getTraceEndTime().compareTo(comparisonSpan.getTraceEndTime()) < 0) {
                        if (bestMatchingParent == null) {
                            bestMatchingParent = comparisonSpan;
                        } else {
                            if (bestMatchingParent.getTimeOccured() < comparisonSpan.getTimeOccured()) {
                                bestMatchingParent = comparisonSpan;
                            }
                        }
                    } 
                }
                
                if (bestMatchingParent != null) {
                    span.addSpanReference(bestMatchingParent.getSpanContext(), 
                            RequestTraceSpan.SpanContextRelationshipType.ChildOf);
                }
            }
        }
    }

    private String logUnfinishedSpan(RequestTraceSpan span) {
        var root = trace.getFirst();
        var sb = new StringBuilder(300);
        sb.append("Unfinished trace found during completion of trace ")
                .append(root.getTraceId())
                .append(" tagged ")
                .append(root.getSpanTags())
                .append("\nUnfinished span is ")
                .append(span.getTraceId())
                .append(" tagged ")
                .append(span.getSpanTags());
        return sb.toString();
    }

    @Override
    public int compareTo(RequestTrace requestTrace) {
        int compareElapsedTime = Long.compare(requestTrace.elapsedTime, elapsedTime);
        if (compareElapsedTime != 0) {
            return compareElapsedTime;
        }
        return requestTrace.startTime.compareTo(startTime);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        RequestTrace that = (RequestTrace) o;

        return elapsedTime == that.elapsedTime && (this.toString() != null
                ? this.toString().equals(that.toString()) : that.toString() == null);
    }
    
    @Override
    public int hashCode() {
        int result = (int) (elapsedTime ^ (elapsedTime >>> 32));
        result = 31 * result + (this.toString() != null ? this.toString().hashCode() : 0);
        return result;
    }
}
