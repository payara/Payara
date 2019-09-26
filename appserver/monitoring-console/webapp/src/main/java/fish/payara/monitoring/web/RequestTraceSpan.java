package fish.payara.monitoring.web;

import java.util.Map;
import java.util.UUID;

import fish.payara.nucleus.requesttracing.RequestTracingService;

public class RequestTraceSpan {

    public final UUID id;
    public final String operation;
    public final long startTime;
    public final long endTime;
    public final long duration;
    public final Map<String, String> tags;

    public RequestTraceSpan(fish.payara.notification.requesttracing.RequestTraceSpan span) {
       this.id = span.getId();
       this.operation = RequestTracingService.stripPackageName(span.getEventName());
       this.startTime = span.getTimeOccured();
       this.endTime = span.getTraceEndTime().toEpochMilli();
       this.duration = span.getSpanDuration();
       this.tags = span.getSpanTags();
    }
}
