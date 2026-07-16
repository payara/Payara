/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.ExceptionAttributes;
import io.opentelemetry.semconv.HttpAttributes;
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

            final Span activeSpan = Span.current();
            if (!activeSpan.isRecording()) {
                LOG.finest(() -> "Could not find any active span, nothing to do.");
                return;
            }

            switch (requestEvent.getType()) {
                case ON_EXCEPTION:
                    onException(requestEvent, activeSpan);
                    break;
                case RESP_FILTERS_FINISHED:
                    onOutgoingResponse(requestEvent, activeSpan);
                    break;
                case FINISHED:
                    // StandardWrapper owns the SERVER span lifecycle;
                    // do NOT end it here — StandardWrapper's finally/AsyncListener will.
                    LOG.log(Level.FINE, "Request finished; SERVER span is owned by StandardWrapper");
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
        Throwable ex = event.getException();
        activeSpan.setStatus(StatusCode.ERROR, ex.getMessage());
        activeSpan.setAttribute(ExceptionAttributes.EXCEPTION_TYPE, ex.getClass().getName());
        activeSpan.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        activeSpan.recordException(ex);
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
            activeSpan.setStatus(StatusCode.ERROR);
            // If there's an attached exception, add it to the span
            if (response.hasEntity() && response.getEntity() instanceof Throwable) {
                Throwable throwable = (Throwable) response.getEntity();
                activeSpan.recordException(throwable);
                activeSpan.setAttribute(ExceptionAttributes.EXCEPTION_TYPE, throwable.getClass().getName());
                activeSpan.addEvent(ExceptionAttributes.EXCEPTION_TYPE.toString(),
                        Attributes.of(ExceptionAttributes.EXCEPTION_MESSAGE, throwable.getMessage()));
                activeSpan.setStatus(StatusCode.ERROR, throwable.getMessage());
            }
        }
    }

    private void onIncomingRequest(final RequestEvent event, final String operationName) {
        LOG.fine(() -> "onIncomingRequest(event=" + event.getType() + ", operationName=" + operationName + ")");

        final ContainerRequest requestContext = event.getContainerRequest();

        // StandardWrapper always owns the SERVER span for this request.
        // Presence of OtelRouteState in Context.current() is the signal — StandardWrapper
        // folds it into the span context, so it is visible to any code running under that scope.
        OtelRouteState routeState = OtelRouteState.fromContext(Context.current());

        if (routeState != null) {
            // Contribute the full JAX-RS route (baseUri path + matched @Path template).
            // StandardWrapper reads it from the stashed context at span-end via OtelRouteState.
            String httpRoute = openTracingHelper.getHttpRoute(requestContext, resourceInfo);
            if (httpRoute != null && !httpRoute.isEmpty()) {
                routeState.setFullRoute(httpRoute);
            }
            routeState.overrideSpanName(operationName);
            Span.current().setAttribute("payara.subsystem", "jakarta-rest");
            LOG.fine(() -> "JAX-RS deferring to existing SERVER span for uri=" + toString(requestContext.getUriInfo()));
        } else {
            LOG.warning(() -> "No active SERVER span found in Context for JAX-RS request. "
                    + "Verify that StandardWrapper OTel tracing is enabled.");
        }
    }

    private String toString(final UriInfo uriInfo) {
        try {
            return uriInfo.getRequestUri().toURL().toString();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Invalid uriInfo: " + uriInfo, e);
        }
    }

}
