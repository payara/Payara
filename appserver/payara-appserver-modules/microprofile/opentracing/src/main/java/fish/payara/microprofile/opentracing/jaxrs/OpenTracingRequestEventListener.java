/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.opentracing.jaxrs;

import fish.payara.microprofile.opentracing.cdi.OpenTracingCdiUtils;
import fish.payara.opentracing.OpenTracingService;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

import java.net.MalformedURLException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.opentracing.Traced;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEvent.Type;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import static java.util.Objects.requireNonNull;

/**
 * Request event listener tracing Jersey requests if the tracing is enabled and possible.
 *
 * @author David Matejcek
 */
public class OpenTracingRequestEventListener implements RequestEventListener {

    private static final Logger LOG = Logger.getLogger(OpenTracingRequestEventListener.class.getName());
    private final String applicationName;
    private final ResourceInfo resourceInfo;
    private final OpenTracingService openTracing;


    /**
     * Instantiates the listener.
     *
     * @param applicationName
     * @param resourceInfo
     * @param openTracing
     */
    public OpenTracingRequestEventListener(final String applicationName, final ResourceInfo resourceInfo,
        final OpenTracingService openTracing) {
        this.applicationName = applicationName;
        this.resourceInfo = resourceInfo;
        this.openTracing = openTracing;
    }


    @Override
    public void onEvent(final RequestEvent event) {
        LOG.fine(() -> "onEvent(event.type=" + event.getType() + ", path=" + getPath(event) + ")");

        // onException is special, it can come in any phase of request processing.
        // early phases are simply ignored.
        if (Arrays.asList(Type.START, Type.MATCHING_START).contains(event.getType())) {
            return;
        }

        try {
            if (event.getType() == Type.REQUEST_MATCHED) {
                final Traced tracedAnnotation = getTracedAnnotation();
                final ContainerRequest requestContext = event.getContainerRequest();
                if (!canTrace(requestContext, tracedAnnotation)) {
                    LOG.finest(() -> "canTrace(...) returned false, nothing to do.");
                    return;
                }
                final String operationName = determineOperationName(requestContext, tracedAnnotation);
                onIncomingRequest(event, operationName);
                return;
            }

            final Span activeSpan = openTracing.getTracer(this.applicationName).scopeManager().activeSpan();
            if (activeSpan == null) {
                LOG.finest(() -> "Could not find any active span, nothing to do.");
                return;
            }

            if (event.getType() == Type.ON_EXCEPTION) {
                onException(event);
            } else if (event.getType() == Type.RESP_FILTERS_FINISHED) {
                onOutgoingResponse(event);
            } else if (event.getType() == Type.FINISHED) {
                finish(event);
            }
        } catch (final RuntimeException e) {
            LOG.log(Level.CONFIG, "Exception thrown by the listener!", e);
            throw e;
        }
    }


    private String getPath(final RequestEvent event) {
        return event.getUriInfo() == null ? "<unknown>" : event.getUriInfo().getPath();
    }

    private void onIncomingRequest(final RequestEvent event, final String operationName) {
        LOG.fine(() -> "onIncomingRequest(event=" + event.getType() + ", operationName=" + operationName + ")");

        final ContainerRequest requestContext = event.getContainerRequest();
        final Tracer tracer = openTracing.getTracer(this.applicationName);

        // Create a Span and instrument it with details about the request
        final SpanBuilder spanBuilder = tracer.buildSpan(operationName) //
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER) //
                .withTag(Tags.HTTP_METHOD.getKey(), requestContext.getMethod()) //
                .withTag(Tags.HTTP_URL.getKey(), toString(requestContext.getUriInfo())) //
                .withTag(Tags.COMPONENT.getKey(), "jaxrs");

        // If there was a context injected into the tracer, add it as a parent of the new span
        final SpanContext spanContext = findSpanContext(requestContext, tracer);
        if (spanContext != null) {
            spanBuilder.asChildOf(spanContext);
        }
        // Start the span and continue on to the targeted method
        Scope scope = tracer.activateSpan(spanBuilder.start());
        requestContext.setProperty(Scope.class.getName(), scope);//tracer.activeSpan();
        LOG.fine(() -> "Request tracing enabled for request=" + requestContext.getRequest() + " to application="
            + this.applicationName + " on uri=" + toString(requestContext.getUriInfo()));

    }


    private void onException(final RequestEvent event) {
        LOG.fine(() -> "onException(event=" + event.getType() + ")");
        final Span activeSpan = getAlreadyActiveSpan();
        activeSpan.setTag(Tags.ERROR.getKey(), true);
        activeSpan.setTag(Tags.HTTP_STATUS.getKey(), Status.INTERNAL_SERVER_ERROR.getStatusCode());
        activeSpan.log(Collections.singletonMap(Fields.EVENT, "error"));
        activeSpan.log(Collections.singletonMap(Fields.ERROR_OBJECT, event.getException()));
    }


    private void onOutgoingResponse(final RequestEvent event) {
        LOG.fine(() -> "onOutgoingRequest(event=" + event.getType() + ")");

        final ContainerResponse response = requireNonNull(event.getContainerResponse(), "response");
        final StatusType statusInfo = response.getStatusInfo();
        LOG.fine(() -> "Response context: status code=" + statusInfo.getStatusCode() //
            + ", hasEntity=" + response.hasEntity());

        final Span activeSpan = getAlreadyActiveSpan();
        LOG.finest("Setting the HTTP response status etc. to the active span...");
        activeSpan.setTag(Tags.HTTP_STATUS.getKey(), statusInfo.getStatusCode());

        // If the response status is an error, add error information to the span
        if (statusInfo.getFamily() == Response.Status.Family.CLIENT_ERROR
            || statusInfo.getFamily() == Response.Status.Family.SERVER_ERROR) {
            activeSpan.setTag(Tags.ERROR.getKey(), true);
            activeSpan.log(Collections.singletonMap(Fields.EVENT, "error"));

            // If there's an attached exception, add it to the span
            if (response.hasEntity() && response.getEntity() instanceof Throwable) {
                activeSpan.log(Collections.singletonMap(Fields.ERROR_OBJECT, response.getEntity()));
            }
        }
    }


    private void finish(final RequestEvent event) {
        LOG.fine(() -> "finish(event=" + event.getType() + ")");
        ScopeManager scopeManager = openTracing.getTracer(this.applicationName).scopeManager();
        final Span activeSpan = scopeManager.activeSpan();
        if (activeSpan == null) {
            LOG.finest("Active span is null, nothing to do.");
            return;
        }
        scopeManager.activeSpan().finish();
        Object scopeObj = event.getContainerRequest().getProperty(Scope.class.getName());
        if(scopeObj !=null && scopeObj instanceof Scope){
            try(Scope scope = (Scope)scopeObj) {
                event.getContainerRequest().removeProperty(Scope.class.getName());
            }
        }
        LOG.finest("Finished.");
    }


    /**
     * Check if CDI has been initialised by trying to get the BeanManager
     * Get the Traced annotation from the target method if CDI is initialised
     *
     * @return {@link Traced} annotation or null
     */
    private Traced getTracedAnnotation() {
        final BeanManager beanManager = getBeanManager();
        if (beanManager == null) {
            return null;
        }
        return OpenTracingCdiUtils.getAnnotation(beanManager, Traced.class, resourceInfo);
    }


    private BeanManager getBeanManager() {
        try {
            return CDI.current().getBeanManager();
        } catch (final IllegalStateException ise) {
            // *Should* only get here if CDI hasn't been initialised, indicating that the app isn't using it
            LOG.log(Level.FINE, "Error getting Bean Manager, presumably due to this application not using CDI",
                    ise);
            return null;
        }
    }


    /**
     * Helper method that checks if any specified skip patterns match this method name
     *
     * @param request the request to check if we should skip
     * @return
     */
    private boolean canTrace(final ContainerRequest request, final Traced tracedAnnotation) {
        // we cannot trace if we don't have enough information.
        // this can occur on early request processing stages
        if (request == null || resourceInfo.getResourceClass() == null || resourceInfo.getResourceMethod() == null) {
            return false;
        }

        // Prepend a slash for safety (so that a pattern of "/blah" or just "blah" will both match)
        final String uriPath = "/" + request.getUriInfo().getPath();
        // Because the openapi resource path is always empty we need to use the base path
        final String baseUriPath = request.getUriInfo().getBaseUri().getPath();
        // First, check for the mandatory skips
        if (uriPath.equals("/health")
                || uriPath.equals("/metrics")
                || uriPath.contains("/metrics/base")
                || uriPath.contains("/metrics/vendor")
                || uriPath.contains("/metrics/application")
                || baseUriPath.equals("/openapi/")) {
            return false;
        }

        final Config config = getConfig();
        if (config != null) {
            // If a skip pattern property has been given, check if any of them match the method
            final Optional<String> skipPatternOptional = config.getOptionalValue("mp.opentracing.server.skip-pattern",
                    String.class);
            if (skipPatternOptional.isPresent()) {
                final String skipPatterns = skipPatternOptional.get();

                final String[] splitSkipPatterns = skipPatterns.split("\\|");

                for (final String skipPattern : splitSkipPatterns) {
                    if (uriPath.matches(skipPattern)) {
                        return false;
                    }
                }
            }
        }

        return tracedAnnotation == null || getTracingFromConfig().orElse(tracedAnnotation.value());
    }

    private Config getConfig() {
        try {
            return ConfigProvider.getConfig();
        } catch (final IllegalArgumentException ex) {
            LOG.log(Level.CONFIG, "No config could be found", ex);
            return null;
        }
    }


    private Optional<Boolean> getTracingFromConfig() {
        return OpenTracingCdiUtils.getConfigOverrideValue(Traced.class, "value", resourceInfo, Boolean.class);
    }

    private Span getAlreadyActiveSpan() {
        final Span activeSpan = openTracing.getTracer(this.applicationName).scopeManager().activeSpan();
        if (activeSpan == null) {
            throw new IllegalStateException("Active span is null, something closed it.");
        }
        return activeSpan;
    }

    /**
     * Helper method that determines what the operation name of the span.
     * Depends on completely initialized resourceInfo.
     *
     * @return The name to use as the Span's operation name
     */
    private String determineOperationName(final ContainerRequestContext request, final Traced tracedAnnotation) {
        if (tracedAnnotation != null) {
            String operationName = OpenTracingCdiUtils.getConfigOverrideValue(
                    Traced.class, "operationName", resourceInfo, String.class)
                    .orElse(tracedAnnotation.operationName());

            // If the annotation or config override providing an empty name, just set it equal to the HTTP Method,
            // followed by the method signature
            if (operationName.equals("")) {
                operationName = request.getMethod() + ":"
                        + resourceInfo.getResourceClass().getCanonicalName() + "."
                        + resourceInfo.getResourceMethod().getName();
            }

            return operationName;
        }

        // If there is no @Traced annotation
        final Config config = getConfig();

        // Determine if an operation name provider has been given
        final Optional<String> operationNameProviderOptional = config == null
                ? Optional.empty()
                : config.getOptionalValue("mp.opentracing.server.operation-name-provider", String.class);
        if (operationNameProviderOptional.isPresent()) {
            final String operationNameProvider = operationNameProviderOptional.get();

            final Path classLevelAnnotation = resourceInfo.getResourceClass().getAnnotation(Path.class);
            final Path methodLevelAnnotation = resourceInfo.getResourceMethod().getAnnotation(Path.class);

            // If the provider is set to "http-path" and the class-level @Path annotation is actually present
            if (operationNameProvider.equals("http-path") && classLevelAnnotation != null) {
                String operationName = request.getMethod() + ":";

                if (classLevelAnnotation.value().startsWith("/")) {
                    operationName += classLevelAnnotation.value();
                } else {
                    operationName += "/" + classLevelAnnotation.value();
                }

                // If the method-level @Path annotation is present, use its value
                if (methodLevelAnnotation != null) {
                    if (methodLevelAnnotation.value().startsWith("/")) {
                        operationName += methodLevelAnnotation.value();
                    } else {
                        operationName += "/" + methodLevelAnnotation.value();
                    }
                }

                return operationName;
            }
        }

        // If we haven't returned by now, just go with the default ("class-method")
        return request.getMethod() + ":"
                + resourceInfo.getResourceClass().getCanonicalName() + "."
                + resourceInfo.getResourceMethod().getName();
    }


    private SpanContext findSpanContext(final ContainerRequestContext requestContext, final Tracer tracer) {
        try {
            // Extract the context from the tracer if there is one
            return tracer.extract(Format.Builtin.HTTP_HEADERS,
                new MultivaluedMapToTextMap(requestContext.getHeaders()));
        } catch (final IllegalArgumentException e) {
            LOG.log(Level.WARNING, e.getMessage());
            return null;
        }
    }


    private String toString(final UriInfo uriInfo) {
        try {
            return uriInfo.getRequestUri().toURL().toString();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Invalid uriInfo: " + uriInfo, e);
        }
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
        public MultivaluedMapToTextMap(final MultivaluedMap<String, String> map) {
            this.map = map;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            // Use the helper iterator
            return new MultivaluedMapIterator<>(map.entrySet());
        }

        @Override
        public void put(final String key, final String value) {
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
        public MultivaluedMapIterator(final Set<Map.Entry<K, List<V>>> multiValuesEntrySet) {
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
            }
            return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), null);
        }

        @Override
        public void remove() {
            // Not needed; we're only iterating over the map, not editing it
            throw new UnsupportedOperationException();
        }

    }
}
