/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2023-2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.telemetry.tracing.jaxrs;

import fish.payara.opentracing.OtelRouteState;
import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.opentracing.PropagationHelper;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.ExceptionAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

class OpenTelemetryRequestEventListener implements RequestEventListener {

    private static final Logger LOG = Logger.getLogger(OpenTelemetryRequestEventListener.class.getName());

    private final ResourceInfo resourceInfo;

    private final OpenTelemetryService openTelemetryService;

    private final OpenTracingHelper openTracingHelper;

    private PropagationHelper helper;

    public OpenTelemetryRequestEventListener(final ResourceInfo resourceInfo,
                                             final OpenTelemetryService openTelemetryService,
                                             final OpenTracingHelper openTracingHelper) {
        this.resourceInfo = resourceInfo;
        this.openTelemetryService = openTelemetryService;
        this.openTracingHelper = openTracingHelper;
    }

    @Override
    public void onEvent(RequestEvent requestEvent) {
        LOG.fine(() -> "onEvent(event.type=" + requestEvent.getType() + ", path=" + getPath(requestEvent) + ")");
        // onException is special, it can come in any phase of request processing.
        // early phases are simply ignored.
        try {
            switch (requestEvent.getType()) {
                case START:
                case MATCHING_START:
                case LOCATOR_MATCHED:
                case SUBRESOURCE_LOCATED:
                case REQUEST_FILTERED:
                case EXCEPTION_MAPPER_FOUND:
                case EXCEPTION_MAPPING_FINISHED:
                    // these events are not interesting
                    return;
            }

            if (requestEvent.getType() == RequestEvent.Type.REQUEST_MATCHED) {
                final ContainerRequest requestContext = requestEvent.getContainerRequest();
                final String operationName = openTracingHelper.determineOperationName(resourceInfo, requestContext);
                onIncomingRequest(requestEvent, operationName);
                return;
            }

            final Span activeSpan = helper != null ? helper.span() : Span.current();
            if (!activeSpan.isRecording()) {
                LOG.finest(() -> "Could not find any active span, nothing to do.");
                return;
            }

            switch (requestEvent.getType()) {
                case ON_EXCEPTION:
                    onException(requestEvent, activeSpan);
                    break;
                case RESOURCE_METHOD_FINISHED:
                    // remove scope from processing thread
                    if (helper != null) {
                        helper.closeContext();
                    }
                    break;
                case RESP_FILTERS_FINISHED:
                    onOutgoingResponse(requestEvent, activeSpan);
                    break;
                case FINISHED:
                    if (helper != null) {
                        finish(requestEvent);
                    } else {
                        // helper == null means StandardWrapper owns the SERVER span lifecycle;
                        // do NOT end it here — StandardWrapper's finally/AsyncListener will.
                        LOG.log(Level.FINE, "Request finished; SERVER span is owned by StandardWrapper");
                    }
                    break;
            }
        } catch (final RuntimeException e) {
            LOG.log(Level.CONFIG, "Exception thrown by the listener!", e);
            throw e;
        }
    }

    private String getPath(final RequestEvent event) {
        return event.getUriInfo() == null ? "<unknown>" : event.getUriInfo().getPath();
    }

    private void onException(final RequestEvent event, final Span activeSpan) {
        LOG.fine(() -> "onException(event=" + event.getType() + ")");
        activeSpan.setStatus(StatusCode.ERROR, event.getException().getMessage());
        activeSpan.setAttribute("error", true);
        activeSpan.setAttribute(ExceptionAttributes.EXCEPTION_TYPE, Throwable.class.getName());
        activeSpan.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        activeSpan.addEvent(ExceptionAttributes.EXCEPTION_TYPE.toString(),
                Attributes.of(ExceptionAttributes.EXCEPTION_MESSAGE, event.getException().getMessage()));
        activeSpan.recordException(event.getException());
    }

    private void onOutgoingResponse(final RequestEvent event, final Span activeSpan) {
        LOG.fine(() -> "onOutgoingRequest(event=" + event.getType() + ")");
        final ContainerResponse response = requireNonNull(event.getContainerResponse(), "response");
        final Response.StatusType statusInfo = response.getStatusInfo();
        LOG.fine(() -> "Response context: status code=" + statusInfo.getStatusCode() //
                + ", hasEntity=" + response.hasEntity());
        LOG.finest("Setting the HTTP response status etc. to the active span...");
        activeSpan.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, statusInfo.getStatusCode());

        // If the response status is an error, add error information to the span
        if (statusInfo.getFamily() == Response.Status.Family.SERVER_ERROR) {
            activeSpan.setAttribute("error", true);
            activeSpan.setStatus(StatusCode.ERROR);
            // If there's an attached exception, add it to the span
            if (response.hasEntity() && response.getEntity() instanceof Throwable) {
                activeSpan.recordException((Throwable) response.getEntity());
                activeSpan.setAttribute(ExceptionAttributes.EXCEPTION_TYPE, Throwable.class.getName());
                activeSpan.addEvent(ExceptionAttributes.EXCEPTION_TYPE.toString(),
                        Attributes.of(ExceptionAttributes.EXCEPTION_MESSAGE, event.getException().getMessage()));
                activeSpan.setStatus(StatusCode.ERROR, event.getException().getMessage());
            }
        }
    }

    private void finish(final RequestEvent event) {
        LOG.fine(() -> "finish(event=" + event.getType() + ")");
        helper.close();
        LOG.finest("Finished.");
    }

    private void onIncomingRequest(final RequestEvent event, final String operationName) {
        LOG.fine(() -> "onIncomingRequest(event=" + event.getType() + ", operationName=" + operationName + ")");

        final ContainerRequest requestContext = event.getContainerRequest();

        // Detect whether StandardWrapper already owns the SERVER span for this request.
        // Presence of OtelRouteState in Context.current() is the signal — StandardWrapper
        // folds it into the span context, so it is visible to any code running under that scope.
        OtelRouteState routeState = OtelRouteState.fromContext(Context.current());

        if (routeState != null) {
            // Fast path: StandardWrapper owns the span
            // Contribute the full JAX-RS route (baseUri path + matched @Path template).
            // StandardWrapper reads it from the stashed context at span-end via OtelRouteState.
            String httpRoute = openTracingHelper.getHttpRoute(requestContext, resourceInfo);
            if (httpRoute != null && !httpRoute.isEmpty()) {
                routeState.setFullRoute(httpRoute);
            }
            // helper remains null. All event-handler branches guard on helper != null;
            // activeSpan falls back to Span.current() which is the StandardWrapper SERVER span.
            LOG.fine(() -> "JAX-RS deferring to existing SERVER span for uri=" + toString(requestContext.getUriInfo()));
            return;
        }

        // Fallback path: no StandardWrapper span (e.g. OTel disabled for server but enabled
        // for app, or in a non-standard deployment). Create a SERVER span as before.
        final Tracer tracer = openTelemetryService.getCurrentTracer();
        final String httpRoute = openTracingHelper.getHttpRoute(requestContext, resourceInfo);

        var queryParam = requestContext.getRequestUri().getQuery() == null
                ? null : requestContext.getRequestUri().getQuery();
        final SpanBuilder spanBuilder = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, requestContext.getMethod())
                .setAttribute(UrlAttributes.URL_FULL, requestContext.getRequestUri().toString())
                .setAttribute(UrlAttributes.URL_PATH,
                        requestContext.getUriInfo().getRequestUri().getPath())
                .setAttribute(UrlAttributes.URL_QUERY, queryParam)
                .setAttribute(UrlAttributes.URL_SCHEME, requestContext.getRequestUri().getScheme())
                .setAttribute(ServerAttributes.SERVER_ADDRESS, requestContext.getRequestUri().getHost())
                .setAttribute(HttpAttributes.HTTP_ROUTE, httpRoute)
                .setAttribute("component", "jaxrs");

        if (requestContext.getRequestUri().getPort() != -1) {
            spanBuilder.setAttribute(ServerAttributes.SERVER_PORT, (long)requestContext.getRequestUri().getPort());
        }

        openTracingHelper.augmentSpan(spanBuilder);

        // Fallback path: StandardWrapper did not create a SERVER span (OTel disabled or
        // non-standard deployment), so Context.current() has no extracted remote parent.
        // Extract from headers here so the span is correctly parented to the remote caller.
        var spanContext = extractContext(requestContext);
        spanBuilder.setParent(spanContext);

        final Span span = spanBuilder.startSpan();
        helper = PropagationHelper.start(span, spanContext);
        requestContext.setProperty(PropagationHelper.class.getName(), helper);
        LOG.fine(() -> "Request tracing enabled for request=" + requestContext.getRequest() + " on uri=" + toString(requestContext.getUriInfo()));
    }

    private String toString(final UriInfo uriInfo) {
        try {
            return uriInfo.getRequestUri().toURL().toString();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Invalid uriInfo: " + uriInfo, e);
        }
    }

    private Context extractContext(ContainerRequest request) {
        // Extract remote trace context from inbound headers, continuing from the current context
        // (not Context.root()) so that a parent StandardWrapper SERVER span is preserved.
        return openTelemetryService.getCurrentSdk().getPropagators().getTextMapPropagator().extract(Context.current(),
                request, new TextMapGetter<ContainerRequest>() {
                    @Override
                    public Iterable<String> keys(ContainerRequest containerRequest) {
                        return containerRequest.getHeaders().keySet();
                    }

                    @Override
                    public String get(ContainerRequest containerRequest, String s) {
                        return containerRequest.getHeaderString(s);
                    }
                });
    }

}
