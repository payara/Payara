/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.opentracing.jaxws;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import fish.payara.opentracing.ScopeManager;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.TextMap;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.opentracing.Traced;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.webservices.monitoring.MonitorContext;
import org.glassfish.webservices.monitoring.MonitorFilter;
import org.jvnet.hk2.annotations.Service;

import static io.opentracing.propagation.Format.Builtin.HTTP_HEADERS;
import static io.opentracing.tag.Tags.COMPONENT;
import static io.opentracing.tag.Tags.ERROR;
import static io.opentracing.tag.Tags.HTTP_METHOD;
import static io.opentracing.tag.Tags.HTTP_STATUS;
import static io.opentracing.tag.Tags.HTTP_URL;
import static io.opentracing.tag.Tags.SPAN_KIND;
import static io.opentracing.tag.Tags.SPAN_KIND_SERVER;
import static java.util.Collections.list;
import static java.util.Collections.singletonMap;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static jakarta.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static jakarta.xml.ws.handler.MessageContext.HTTP_RESPONSE_CODE;
import static jakarta.xml.ws.handler.MessageContext.SERVLET_REQUEST;
import static jakarta.xml.ws.handler.MessageContext.SERVLET_RESPONSE;


@Service(name = "jaxws-opentracing-filter")
public class JaxwsContainerRequestTracingFilter implements MonitorFilter {

    private static final Logger logger = Logger.getLogger(JaxwsContainerRequestTracingFilter.class.getName());

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private RequestTracingService requestTracing;

    @Inject
    private OpenTracingService openTracing;


    // Before method invocation
    @Override
    public void filterRequest(Packet pipeRequest, MonitorContext monitorContext) {
        // If request tracing is enabled, and there's a trace in progress (which there should be!)
        if (isTraceInProgress()) {

            // Get the Traced annotation from the target method if CDI is initialised
            Traced tracedAnnotation = getTraceAnnotation(monitorContext);

            // If there is no matching skip pattern and no traced annotation, or if there is there is no matching skip
            // pattern and a traced annotation set to true (via annotation or config override)
            if (shouldTrace(pipeRequest) && shouldTrace(monitorContext, tracedAnnotation)) {

                // Get the application's tracer instance
                Tracer tracer = getTracer();

                HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);

                // Create a Span and instrument it with details about the request
                SpanBuilder spanBuilder = tracer.buildSpan(determineOperationName(pipeRequest, monitorContext, tracedAnnotation))
                        .withTag(SPAN_KIND.getKey(), SPAN_KIND_SERVER)
                        .withTag(HTTP_METHOD.getKey(), httpRequest.getMethod())
                        .withTag(HTTP_URL.getKey(),  httpRequest.getRequestURL().toString())
                        .withTag(COMPONENT.getKey(), "jaxws");

                SpanContext spanContext = null;
                try {
                    // Extract the context from the tracer if there is one
                    spanContext = tracer.extract(HTTP_HEADERS, new MultivaluedMapToTextMap(getHeaders(httpRequest)));
                } catch (IllegalArgumentException e){
                    logger.log(WARNING, e.getMessage());
                }

                // If there was a context injected into the tracer, add it as a parent of the new span
                if (spanContext != null) {
                    spanBuilder.asChildOf(spanContext);
                }

                // Start the span and continue on to the targeted method
                Scope scope = tracer.activateSpan(spanBuilder.start());
                httpRequest.setAttribute(Scope.class.getName(), scope);
            }
        }
    }

    // After method invocation
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
                    Span activeSpan = getTracer().scopeManager().activeSpan();
                    if (activeSpan == null) {
                        return;
                    }
                    HttpServletRequest httpRequest = (HttpServletRequest) pipeRequest.get(SERVLET_REQUEST);
                    try {

                        // Get and add the response status to the active span
                        Response.StatusType statusInfo = getResponseStatus(pipeRequest, pipeRequest);

                        activeSpan.setTag(HTTP_STATUS.getKey(), statusInfo.getStatusCode());

                        // If the response status is an error, add error information to the span
                        if (statusInfo.getFamily() == CLIENT_ERROR || statusInfo.getFamily() == SERVER_ERROR) {
                            activeSpan.setTag(ERROR.getKey(), true);
                            activeSpan.log(singletonMap("event", "error"));

                            // If there's an attached exception, add it to the span

                            Message message = pipeResponse.getMessage();
                            if (message != null && message.isFault()) {
                                activeSpan.log(singletonMap("error.object", getErrorObject(message)));
                            }
                        }
                    } finally {
                        activeSpan.finish();
                        Object scopeObj = httpRequest.getAttribute(Scope.class.getName());
                        if (scopeObj != null && scopeObj instanceof Scope) {
                            try (Scope scope = (Scope) scopeObj) {
                                httpRequest.removeAttribute(Scope.class.getName());
                            }
                        }
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

    private Object getErrorObject(Message message) {
        try {
            return message.copy().readAsSOAPMessage().getSOAPBody().getFault();
        } catch (SOAPException e) {
            logger.log(SEVERE, "Error while reading fault from message ", e);
            return null;
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
            String operationName =
                OpenTracingJaxwsCdiUtils
                    .getConfigOverrideValue(
                        Traced.class, "operationName", monitorContext, String.class)
                    .orElse(
                        tracedAnnotation.operationName());

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
        return
            httpRequest.getMethod() + ":" +
            monitorContext.getImplementationClass().getCanonicalName() + "." +
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

        return OpenTracingJaxwsCdiUtils
            .getConfigOverrideValue(Traced.class, "value", monitorContext, boolean.class)
            .orElse(tracedAnnotation.value());
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
        if (uriPath.equals("/health")
                || uriPath.equals("/metrics")
                || uriPath.contains("/metrics/base")
                || uriPath.contains("/metrics/vendor")
                || uriPath.contains("/metrics/application")) {
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

    private Response.StatusType getResponseStatus(Packet pipeRequest, Packet pipeResponse) {

        Integer statusCode = (Integer) pipeResponse.get(HTTP_RESPONSE_CODE);

        if (statusCode == null || statusCode.equals(0)) {
            HttpServletResponse httpResponse = (HttpServletResponse) pipeRequest.get(SERVLET_RESPONSE);
            statusCode = httpResponse.getStatus();
        }

        return Response.Status.fromStatusCode(statusCode);
    }

    private Throwable getResponseException(MonitorContext monitorContext, Packet pipeResponse) {
        return monitorContext
                .getSeiModel()
                .getDatabinding()
                .deserializeResponse(pipeResponse.copy(true), monitorContext.getCallInfo())
                .getException();
    }

    private Traced getTraceAnnotation(MonitorContext monitorContext) {
     // Check if CDI has been initialised by trying to get the BeanManager
        BeanManager beanManager = getBeanManager();

        // Get the Traced annotation from the target method if CDI is initialised
        if (beanManager != null) {
            return OpenTracingJaxwsCdiUtils.getAnnotation(beanManager, Traced.class, monitorContext);
        }

        return null;
    }

    private BeanManager getBeanManager() {
        try {
            return CDI.current().getBeanManager();
        } catch (IllegalStateException ise) {
            // *Should* only get here if CDI hasn't been initialised, indicating that the app isn't using it
            logger.log(FINE, "Error getting Bean Manager, presumably due to this application not using CDI",
                    ise);
        }

        return null;
    }

    private Config getConfig() {
        try {
            return ConfigProvider.getConfig();
        } catch (IllegalArgumentException ex) {
            logger.log(INFO, "No config could be found", ex);
        }

        return null;
    }

    private Tracer getTracer() {
        return openTracing.getTracer(openTracing.getApplicationName(serviceLocator.getService(InvocationManager.class)));
    }

    private MultivaluedMap<String, String> getHeaders(HttpServletRequest httpRequest) {

        MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();

        for (String headerName : list(httpRequest.getHeaderNames())) {
            headerMap.addAll(headerName, list(httpRequest.getHeaders(headerName)));
        }

        return headerMap;
    }

    /**
     * Class used for converting a MultivaluedMap from Headers to a TextMap, to allow it to be extracted from the Tracer.
     */
    private class MultivaluedMapToTextMap implements TextMap {

        private final MultivaluedMap<String, String> map;

        /**
         * Initialises this object with the MultivaluedMap to wrap.
         *
         * @param map The MultivaluedMap to convert to a TextMap
         */
        public MultivaluedMapToTextMap(MultivaluedMap<String, String> map) {
            this.map = map;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            // Use the helper iterator
            return new MultivaluedMapIterator<>(map.entrySet());
        }

        @Override
        public void put(String key, String value) {
            map.add(key, value);
        }

    }

    /**
     * Helper Class used for iterating over the MultivaluedMapToTextMap class.
     *
     * @param <K> The map key class
     * @param <V> The map value class
     */
    private class MultivaluedMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        private final Iterator<Map.Entry<K, List<V>>> mapIterator;

        private Map.Entry<K, List<V>> mapEntry;
        private Iterator<V> mapEntryIterator;

        /**
         * Initialise the iterator to use on this map.
         *
         * @param multiValuesEntrySet The map to initialise the iterator from.
         */
        public MultivaluedMapIterator(Set<Map.Entry<K, List<V>>> multiValuesEntrySet) {
            this.mapIterator = multiValuesEntrySet.iterator();
        }

        @Override
        public boolean hasNext() {
            // True if the MapEntry (value) is not equal to null and has another value, or if there is another key
            return ((mapEntryIterator != null && mapEntryIterator.hasNext()) || mapIterator.hasNext());
        }

        @Override
        public Map.Entry<K, V> next() {
            if (mapEntry == null || (!mapEntryIterator.hasNext() && mapIterator.hasNext())) {
                mapEntry = mapIterator.next();
                mapEntryIterator = mapEntry.getValue().iterator();
            }

            // Return either the next map entry, or an entry with no value if there isn't one
            if (mapEntryIterator.hasNext()) {
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), mapEntryIterator.next());
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), null);
            }
        }

        @Override
        public void remove() {
            // Not needed; we're only iterating over the map, not editing it
            throw new UnsupportedOperationException();
        }

    }

}
