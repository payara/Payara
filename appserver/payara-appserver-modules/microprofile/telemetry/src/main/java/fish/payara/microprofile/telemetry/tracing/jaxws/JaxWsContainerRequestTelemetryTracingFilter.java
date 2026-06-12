/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */
package fish.payara.microprofile.telemetry.tracing.jaxws;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import fish.payara.microprofile.telemetry.tracing.Traced;
import fish.payara.microprofile.telemetry.tracing.jaxrs.OpenTracingCdiUtils;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.soap.SOAPException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.webservices.monitoring.MonitorContext;
import org.glassfish.webservices.monitoring.MonitorFilter;
import org.jvnet.hk2.annotations.Service;

import static jakarta.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static jakarta.xml.ws.handler.MessageContext.*;
import static java.util.Collections.list;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

@Service(name = "jaxws-telemetry-tracing-filter")
public class JaxWsContainerRequestTelemetryTracingFilter implements MonitorFilter {

    private static final Logger logger = Logger.getLogger(JaxWsContainerRequestTelemetryTracingFilter.class.getName());

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private RequestTracingService requestTracing;

    @Inject
    private OpenTracingService openTracing;

    @Override
    public void filterRequest(Packet pipeRequest, MonitorContext monitorContext) {
        // If request tracing is enabled, and there's a trace in progress (which there should be!)
        if (isTraceInProgress()) {
            TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
                @Override
                public Iterable<String> keys(Map<String, String> carrier) {
                    return carrier.keySet();
                }

                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier.get(key);
                }
            };
            // Get the Traced annotation from the target method if CDI is initialised
            Traced tracedAnnotation = getTraceAnnotation(monitorContext);

            // If there is no matching skip pattern and no traced annotation, or if there is there is no matching skip
            // pattern and a traced annotation set to true (via annotation or config override)
            if (shouldTrace(pipeRequest) && shouldTrace(monitorContext, tracedAnnotation)) {

                // Get the application's tracer instance
                Tracer tracer = getTracer();
                HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);
                Map<String, String> headers = getHeaders(httpRequest);
                Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                        .extract(Context.current(), headers, getter);
                SpanBuilder spanBuilder = null;
                if (extractedContext != null) {
                    // Create a Span and instrument it with details about the request
                     spanBuilder = tracer.spanBuilder(determineOperationName(pipeRequest, monitorContext, tracedAnnotation))
                            .setSpanKind(SpanKind.SERVER).setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, httpRequest.getMethod())
                            .setAttribute(UrlAttributes.URL_FULL, httpRequest.getRequestURL().toString())
                            .setAttribute("component", "jaxws").setParent(extractedContext);
                } else {
                    spanBuilder = tracer.spanBuilder(determineOperationName(pipeRequest, monitorContext, tracedAnnotation))
                            .setSpanKind(SpanKind.SERVER).setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, httpRequest.getMethod())
                            .setAttribute(UrlAttributes.URL_FULL, httpRequest.getRequestURL().toString())
                            .setAttribute("component", "jaxws");
                }
                
                Span span = spanBuilder.startSpan();
                Scope scope = span.makeCurrent();
                httpRequest.setAttribute(Scope.class.getName(), scope);
                httpRequest.setAttribute(Span.class.getName(), span);
            }
        }
    }

    @Override
    public void filterResponse(Packet pipeRequest, Packet pipeResponse, MonitorContext monitorContext) {
        // Try block so that we can always attach error info if there is any
        try {
            // If request tracing is enabled, and there is a trace in progress (which there should be!)
            if (isTraceInProgress()) {

                // Get the Traced annotation from the target method if CDI is initialised
                Traced tracedAnnotation = getTraceAnnotation(monitorContext);

                // If there is no matching skip pattern and no traced annotation, or if there is there is no matching skip
                // pattern and a traced annotation set to true (via annotation or config override)
                if (shouldTrace(pipeRequest) && shouldTrace(monitorContext, tracedAnnotation)) {
                    HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);
                    Object httpSpan = httpRequest.getAttribute(Span.class.getName());
                    if (httpSpan == null || !(httpSpan instanceof Span)) {
                        return;
                    }

                    Span activeSpan = (Span) httpSpan;

                    if (!activeSpan.getSpanContext().isValid()) {
                        return;
                    }

                    try {

                        // Get and add the response status to the active span
                        Response.StatusType statusInfo = getResponseStatus(pipeRequest, pipeResponse);

                        activeSpan.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, statusInfo.getStatusCode());

                        // If the response status is an error, add error information to the span
                        if (statusInfo.getFamily() == CLIENT_ERROR || statusInfo.getFamily() == SERVER_ERROR) {
                            activeSpan.setStatus(StatusCode.ERROR, statusInfo.getReasonPhrase());
                            activeSpan.setAttribute(ErrorAttributes.ERROR_TYPE, String.valueOf(statusInfo.getStatusCode()));

                            // If there's an attached exception, add it to the span

                            Message message = pipeResponse.getMessage();
                            if (message != null && message.isFault()) {
                                Object errorObject = getErrorObject(message);
                                if (errorObject instanceof Throwable) {
                                    activeSpan.recordException((Throwable) errorObject);
                                } else if (errorObject != null) {
                                    activeSpan.addEvent("error.object:" + errorObject.toString());
                                }
                            }
                        }
                    } finally {
                        activeSpan.end();
                        Object scopeObj = httpRequest.getAttribute(Scope.class.getName());
                        if (scopeObj instanceof Scope) {
                            try (Scope scope = (Scope) scopeObj) {
                                httpRequest.removeAttribute(Scope.class.getName());
                            }
                        }
                        httpRequest.removeAttribute(Span.class.getName());
                    }
                }
            }
        } finally {
            // If there's an attached error on the response, log the exception
            Message message = pipeResponse.getMessage();

            if (message != null && message.isFault()) {
                Object errorObject = getErrorObject(message);

                if (errorObject != null) {
                    // TODO: have an actual detail formatter for fault
                    logger.log(SEVERE, getErrorObject(message).toString());
                }
            }

        }
    }

    /**
     * Helper method that determines what the operation name of the span.
     *
     * @param tracedAnnotation The Traced annotation obtained from the target method
     * @return The name to use as the Span's operation name
     */
    private String determineOperationName(Packet pipeRequest, MonitorContext monitorContext, Traced tracedAnnotation) {
        HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);

        if (tracedAnnotation != null) {
            String operationName = OpenTracingCdiUtils.getConfigOverrideValue(Traced.class, "operationName",
                    monitorContext, String.class).orElse(tracedAnnotation.operationName());

            // If the annotation or config override providing an empty name, just set it equal to the HTTP Method,
            // followed by the method signature
            if (operationName.equals("")) {
                operationName = createFallbackName(httpRequest, monitorContext);
            }

            return operationName;
        }

        // If there is no @Traced annotation
        Config config = getConfig();

        // Determine if an operation name provider has been given
        Optional<String> operationNameProviderOptional = config.getOptionalValue("mp.opentracing.server.operation-name-provider", String.class);
        if (operationNameProviderOptional.isPresent()) {
            String operationNameProvider = operationNameProviderOptional.get();

            // TODO: Take webservices.xml into account
            WebService classLevelAnnotation = monitorContext.getImplementationClass().getAnnotation(WebService.class);
            WebMethod methodLevelAnnotation = monitorContext.getCallInfo().getMethod().getAnnotation(WebMethod.class);

            // If the provider is set to "http-path" and the class-level @WebService annotation is actually present
            if (operationNameProvider.equals("http-path") && classLevelAnnotation != null) {

                String operationName = httpRequest.getMethod() + ":";

                operationName += "/" + classLevelAnnotation.name();

                // If the method-level WebMethod annotation is present, use its value
                if (methodLevelAnnotation != null) {
                    operationName += "/" + methodLevelAnnotation.operationName();
                }

                return operationName;
            }
        }

        // TODO: consider the WSDL info in MonitorContext?


        // If we haven't returned by now, just go with the default ("class-method")
        return createFallbackName(httpRequest, monitorContext);
    }

    private String createFallbackName(HttpServletRequest httpRequest, MonitorContext monitorContext) {
        return httpRequest.getMethod() + ":" + monitorContext.getImplementationClass().getCanonicalName() + "." + 
                monitorContext.getCallInfo().getMethod().getName();
    }


    private boolean shouldTrace(Packet pipeRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);

        return shouldTrace(httpRequest.getContextPath());
    }

    private boolean shouldTrace(MonitorContext monitorContext, Traced tracedAnnotation) {
        if (tracedAnnotation == null) {
            // No annotation present means we trace
            return true;
        }

        return OpenTracingCdiUtils.getConfigOverrideValue(Traced.class, "value", monitorContext, 
                boolean.class).orElse(tracedAnnotation.value());
    }

    private Object getErrorObject(Message message) {
        try {
            return message.copy().readAsSOAPMessage().getSOAPBody().getFault();
        } catch (SOAPException e) {
            logger.log(SEVERE, "Error while reading fault from message ", e);
            return null;
        }
    }

    /**
     * Helper method that checks if any specified skip patterns match this method name
     *
     * @return
     */
    private boolean shouldTrace(String path) {
        // Prepend a slash for safety (so that a pattern of "/blah" or just "blah" will both match)
        String uriPath = "/" + path;

        // First, check for the mandatory skips
        if (uriPath.equals("/health") || uriPath.equals("/metrics") || uriPath.contains("/metrics/base") || uriPath.contains("/metrics/vendor") || uriPath.contains("/metrics/application")) {
            return false;
        }

        Config config = getConfig();

        if (config != null) {
            // If a skip pattern property has been given, check if any of them match the method
            Optional<String> skipPatternOptional = config.getOptionalValue("mp.opentracing.server.skip-pattern", String.class);
            if (skipPatternOptional.isPresent()) {
                for (String skipPattern : skipPatternOptional.get().split("\\|")) {
                    if (uriPath.matches(skipPattern)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isTraceInProgress() {
        return requestTracing != null && requestTracing.isRequestTracingEnabled() && requestTracing.isTraceInProgress();
    }

    private Config getConfig() {
        try {
            return ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(INFO, "No config could be found", ex);
        }

        return null;
    }

    private Traced getTraceAnnotation(MonitorContext monitorContext) {
        // Check if CDI has been initialised by trying to get the BeanManager
        BeanManager beanManager = getBeanManager();

        // Get the Traced annotation from the target method if CDI is initialised
        if (beanManager != null) {
            return OpenTracingCdiUtils.getAnnotation(beanManager, Traced.class, monitorContext);
        }

        return null;
    }

    private Tracer getTracer() {
        return openTracing.getTracer(openTracing.getApplicationName(serviceLocator.getService(InvocationManager.class)));
    }

    private Response.StatusType getResponseStatus(Packet pipeRequest, Packet pipeResponse) {
        Integer statusCode = (Integer) pipeResponse.get(HTTP_RESPONSE_CODE);

        if (statusCode == null || statusCode.equals(0)) {
            HttpServletResponse httpResponse = (HttpServletResponse) pipeRequest.get(SERVLET_RESPONSE);
            statusCode = httpResponse.getStatus();
        }

        return Response.Status.fromStatusCode(statusCode);
    }

    private Throwable getResponseException(MonitorContext monitorContext, Packet pipeResponse) {
        return monitorContext.getSeiModel().getDatabinding().deserializeResponse(pipeResponse.copy(true), 
                monitorContext.getCallInfo()).getException();
    }

    private BeanManager getBeanManager() {
        try {
            return CDI.current().getBeanManager();
        } catch (IllegalStateException ise) {
            // *Should* only get here if CDI hasn't been initialised, indicating that the app isn't using it
            logger.log(FINE, "Error getting Bean Manager, presumably due to this application not using CDI", ise);
        }

        return null;
    }

    private Map<String, String> getHeaders(HttpServletRequest httpRequest) {
        MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();

        for (String headerName : list(httpRequest.getHeaderNames())) {
            headerMap.addAll(headerName, list(httpRequest.getHeaders(headerName)));
        }

        return headerMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> String.join(",", entry.getValue())));
    }
}
