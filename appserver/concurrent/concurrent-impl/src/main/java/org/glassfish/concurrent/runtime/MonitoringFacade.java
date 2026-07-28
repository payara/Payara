package org.glassfish.concurrent.runtime;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

interface MonitoringFacade {
    Tracer getTracer();
    OpenTelemetry getOpenTelemetry();
    boolean isRequestTracingEnabled();
    void endTrace();
    void registerStuckThread(long threadId);
    void deregisterStuckThread(long threadId);
}
