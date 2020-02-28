package fish.payara.microprofile.faulttolerance.service;

import javax.interceptor.InvocationContext;

import fish.payara.notification.requesttracing.RequestTraceSpan;

public interface FaultToleranceRequestTracing {

    void startSpan(RequestTraceSpan span, InvocationContext context);

    void endSpan();
}
