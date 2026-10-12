# OpenTelemetry Instrumentation Guide

## Rule: Use OpenTelemetry API Only

New server instrumentation **must use the OpenTelemetry API only** (`io.opentelemetry.api.*`), obtained through `fish.payara.opentracing.OpenTelemetryService`. Never create `RequestTraceSpan`/`RequestTraceSpanLog` or call `RequestTracingService` directly from new code.

## How Legacy Request Tracing Receives Spans

The `PayaraRequestTracingProcessor` SpanProcessor bridge (in `nucleus/payara-modules/opentelemetry-tracing-starter`, registered by `RequestTracingBridgeProvider` only when request tracing is enabled at SDK creation) starts a legacy trace for each sampled span with no local parent (local root or remote parent) and stores every other sampled span when it ends. Consumers of Request Tracing (list-requesttraces, notifiers, UI) receive data unchanged through this bridge.

## Guidance for Instrumentation

### Span Kind Selection
- **SERVER**: inbound request entry (e.g., inbound HTTP servlet/webservice request)
- **CONSUMER**: message delivery (e.g., MDB delivery)
- **INTERNAL**: in-process work (e.g., EJB method execution, WebSocket, timers, async tasks)
- **CLIENT**: outbound calls (handled by HTTP client libraries)

### Span Events vs Spans
Use span events for points inside an existing span where a new span is not needed (e.g., EJB method entry/exit, JTA context changes, JDBC operations).

### Always Guard Instrumentation
- Guard with `openTelemetryService.isEnabled()` and `Span.current().isRecording()`
- Wrap span creation so telemetry can never break the request
- Close the Scope before ending the span on the same thread
- No high-cardinality attributes on metrics

## Spans and events emitted by Payara core

Other OpenTelemetry emitters exist (JAX-RS client filter, JAX-WS handler, @WithSpan interceptor, IIOP interceptors) and the list below is not exhaustive.

| Source | Instrumentation | Span Kind | Location |
|--------|-----------------|-----------|----------|
| Servlet/WebService request | OTel SERVER span | SERVER | StandardWrapper (OtelSupport) |
| WebSocket request | INTERNAL span | INTERNAL | InputBuffer |
| MDB delivery | OTel CONSUMER span | CONSUMER | MessageBeanListenerImpl |
| EJB timer task | INTERNAL span | INTERNAL | EJBTimerService |
| FT delayRetry/runAsynchronous/executeFallbackMethod | INTERNAL spans | INTERNAL | FaultToleranceServiceImpl |
| EJB method entry/exit | span events (BaseContainer starts an INTERNAL span itself when no recording span is current) | (event) | BaseContainer |
| JTA context begin/end | span events | (event) | JavaEETransactionManagerSimplified |
| JDBC operation | span event | (event) | RequestTracingListener |
| JBatch execution context | span event | (event) | JBatchJDBCPersistenceManager |

## Behaviour Changes vs Legacy Direct Instrumentation

- **(a) Root trace naming**: The root legacy RT trace is now named after the OTel root span (e.g., `GET` for an HTTP request) instead of `processContainerRequest`; legacy RT HTTP tags (every request header, Class, QueryString, ResponseStatus) are replaced by OTel semantic-convention attributes (http.request.method, url.path, url.query, http.response.status_code, etc.).

- **(b) OpenTelemetry disabled**: If OpenTelemetry is disabled, Request Tracing receives no spans, it is fed only by the bridge.

- **(c) Legacy inbound propagation headers dropped**: Legacy inbound propagation headers (`PROPAGATED_TRACE_ID`, `PROPAGATED_PARENT_ID`, `PROPAGATED_RELATIONSHIP_TYPE`) are no longer read by the server; W3C `traceparent` is used instead. The JAX-RS client filter still adds the legacy headers on outbound calls for backward compatibility.

- **(d) MDB conversation-id propagation dropped**: The MDB `#BAF-CID` conversation-id propagation is no longer used.

- **(e) JTA span events simplified**: JTA span events no longer carry the redundant `logEvent` attribute and use the OTel event timestamp.

- **(f) JDBC listener registration per-connection**: JDBC listener registration is now gated per connection checkout on `OpenTelemetryService.isEnabled()` evaluated on the caller thread; with per-application OpenTelemetry only, a connection obtained on a thread with no application invocation does not get the listener.

- **(g) WebSocket stuck-thread registration**: WebSocket stuck-thread registration no longer depends on request tracing being enabled.

- **(h) Request trace store thread-bound**: The request trace store is thread-bound; a root span that ends on a different thread than it started on does not close its legacy trace.
