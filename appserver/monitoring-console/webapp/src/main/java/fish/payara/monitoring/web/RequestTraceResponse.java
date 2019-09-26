package fish.payara.monitoring.web;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fish.payara.notification.requesttracing.RequestTrace;

public class RequestTraceResponse {

    public final UUID id;
    public final long startTime;
    public final long endTime;
    public final long elapsedTime; 
    public final List<RequestTraceSpan> spans = new ArrayList<>();

    public RequestTraceResponse(RequestTrace trace) {
        this.id = trace.getTraceId();
        this.startTime = trace.getStartTime().toEpochMilli();
        this.endTime = trace.getEndTime().toEpochMilli();
        this.elapsedTime = trace.getElapsedTime();
        for (fish.payara.notification.requesttracing.RequestTraceSpan span : trace.getTraceSpans()) {
            this.spans.add(new RequestTraceSpan(span));
        }
    }
}
