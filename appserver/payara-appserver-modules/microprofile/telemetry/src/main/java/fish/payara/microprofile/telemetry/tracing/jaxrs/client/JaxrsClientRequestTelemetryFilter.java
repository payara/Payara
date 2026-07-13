/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.telemetry.tracing.jaxrs.client;

import fish.payara.microprofile.telemetry.tracing.PayaraTracingServices;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.PropagationHeaders;
import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.opentracing.PropagationHelper;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JaxrsClientRequestTelemetryFilter implements ClientRequestFilter, ClientResponseFilter {

    public static final String REQUEST_CONTEXT_TRACING_PREDICATE = "fish.payara.requesttracing.jaxrs.client.TracingPredicate";

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        final ServiceLocator baseServiceLocator = Globals.getStaticBaseServiceLocator();
        var openTelemetryService = getFromServiceHandle(baseServiceLocator, OpenTelemetryService.class);
        final PayaraTracingServices payaraTracingServices = new PayaraTracingServices();
        final RequestTracingService requestTracing = payaraTracingServices.getRequestTracingService();
        if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
            // ***** Request Tracing Service Instrumentation *****
            // If there is a trace in progress, add the propagation headers with the relevant details
            if (requestTracing.isTraceInProgress()) {
                // Check that we aren't overwriting a header
                if (!requestContext.getHeaders().containsKey(PropagationHeaders.PROPAGATED_TRACE_ID)) {
                    requestContext.getHeaders().add(PropagationHeaders.PROPAGATED_TRACE_ID,
                            requestTracing.getConversationID());
                }

                // Check that we aren't overwriting a header
                if (!requestContext.getHeaders().containsKey(PropagationHeaders.PROPAGATED_PARENT_ID)) {
                    requestContext.getHeaders().add(PropagationHeaders.PROPAGATED_PARENT_ID,
                            requestTracing.getStartingTraceID());
                }

                // Check that we aren't overwriting a relationship type
                if (!requestContext.getHeaders().containsKey(PropagationHeaders.PROPAGATED_RELATIONSHIP_TYPE)) {
                    if (requestContext.getMethod().equals("POST")) {
                        requestContext.getHeaders().add(PropagationHeaders.PROPAGATED_RELATIONSHIP_TYPE,
                                RequestTraceSpan.SpanContextRelationshipType.FollowsFrom);
                    } else {
                        requestContext.getHeaders().add(PropagationHeaders.PROPAGATED_RELATIONSHIP_TYPE,
                                RequestTraceSpan.SpanContextRelationshipType.ChildOf);
                    }
                }
            }
        }

        // ***** OpenTracing Instrumentation *****
        // Check if we should trace this client call
        if (openTelemetryService != null && openTelemetryService.isEnabled() && shouldTrace(requestContext)) {
            // Get or create the tracer instance for this application
            final Tracer tracer = payaraTracingServices.getActiveTracer();

            // Build a span with the required MicroProfile Telemetry attributes
            SpanBuilder spanBuilder = tracer.spanBuilder(requestContext.getMethod())
                    .setAttribute(SemanticAttributes.HTTP_URL, requestContext.getUri().toString())
                    .setAttribute(SemanticAttributes.HTTP_METHOD, requestContext.getMethod())
                    .setAttribute(SemanticAttributes.NET_PEER_NAME, requestContext.getUri().getHost())
                    .setAttribute("component", "jaxrs")
                    .setAttribute("span.kind", "client")
                    .setSpanKind(SpanKind.CLIENT);

            if (requestContext.getUri().getPort() != -1) {
                spanBuilder.setAttribute(SemanticAttributes.NET_PEER_PORT, (long)requestContext.getUri().getPort());
            }

            // Get the propagated span context from the request if present
            // This is required to account for asynchronous client requests
            var parentSpanContext = (Context) requestContext.getProperty(PropagationHeaders.TELEMETRY_PROPAGATED_SPANCONTEXT);
            if (parentSpanContext != null) {
                spanBuilder.setParent(parentSpanContext);
            } else {
                spanBuilder.setParent(Context.current());
            }

            // Start the span and mark it as active
            Span span = spanBuilder.startSpan();
            requestContext.setProperty(PropagationHelper.class.getName(), PropagationHelper.start(span, parentSpanContext));
            // Inject the active span context for propagation
            openTelemetryService.getCurrentSdk().getPropagators().getTextMapPropagator().inject(
                    Context.current(), requestContext, (clientRequestContext, s, s1) -> clientRequestContext.getHeaders().put(s, Collections.singletonList(s1))
            );
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        // If request tracing is enabled, and there's a trace actually in progress, add info about method
        if (requestContext.hasProperty(PropagationHelper.class.getName())) {
            // Get the active span from the application's tracer instance
            var helper = (PropagationHelper)requestContext.getProperty(PropagationHelper.class.getName());
            Span activeSpan = helper.span();
            if (!activeSpan.isRecording()) {
                Logger.getLogger(JaxrsClientRequestTelemetryFilter.class.getName()).log(Level.FINEST, "Could not find any active span, nothing to do.");
                return;
            }

            // Get the response status and add it to the active span
            Response.StatusType statusInfo = responseContext.getStatusInfo();
            activeSpan.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, statusInfo.getStatusCode());

            // If the response status is an error, add error info to the active span
            if (statusInfo.getFamily() == Response.Status.Family.CLIENT_ERROR || statusInfo.getFamily() == Response.Status.Family.SERVER_ERROR) {
                activeSpan.setAttribute("error", true);
                activeSpan.setStatus(StatusCode.ERROR);
                activeSpan.addEvent(SemanticAttributes.EXCEPTION_EVENT_NAME,
                        Attributes.of(SemanticAttributes.EXCEPTION_TYPE, statusInfo.getFamily().name()));
            }
            helper.end();
            helper.close();
            requestContext.removeProperty(PropagationHelper.class.getName());
        }
    }

    private boolean shouldTrace(ClientRequestContext requestContext) {
        Object traceFilter = requestContext.getConfiguration().getProperty(REQUEST_CONTEXT_TRACING_PREDICATE);
        if (traceFilter instanceof Predicate) {
            return ((Predicate<ClientRequestContext>) traceFilter).test(requestContext);
        }
        return true;
    }

    /**
     * Create a service from the given service locator. Throw an exception if the
     * service handle is available but not the service.
     *
     * @return the specified service, or null if the service handle isn't available.
     * @throws RuntimeException if the service initialisation failed.
     */
    private static final <T> T getFromServiceHandle(ServiceLocator serviceLocator, Class<T> serviceClass) {
        ServiceHandle<T> serviceHandle = serviceLocator.getServiceHandle(serviceClass);
        if (serviceHandle != null && serviceHandle.isActive()) {
            return serviceHandle.getService();
        }
        return null;
    }
}
