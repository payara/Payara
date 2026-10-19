package org.glassfish.concurrent.runtime;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

interface MonitoringFacade {
    /**
     * Returns the active {@link Tracer} for the current application scope,
     * or {@code null} if OpenTelemetry is not enabled. See also {@link #getOpenTelemetry()}.
     */
    Tracer getTracer();
    /**
     * Returns the active {@link OpenTelemetry} instance for the current application scope,
     * or {@code null} if OpenTelemetry is not enabled. Callers must treat {@code null} as
     * "OTel not applicable" and skip any telemetry work. See also {@link #getTracer()}.
     */
    OpenTelemetry getOpenTelemetry();
    boolean isRequestTracingEnabled();
    void endTrace();
    void registerStuckThread(long threadId);
    void deregisterStuckThread(long threadId);
}
