/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.opentracing.Traced;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 * ContainerFilter used for instrumenting JaxRs methods with tracing.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Priority(500)
public class JaxrsContainerRequestTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = Logger.getLogger(JaxrsContainerRequestTracingFilter.class.getName());

    private ServiceLocator serviceLocator;
    private RequestTracingService requestTracing;
    private OpenTracingService openTracing;

    @Context
    private ResourceInfo resourceInfo;

    @PostConstruct
    public void postConstruct() {
        // Get the default Payara service locator - injecting a service locator will give you the one used by Jersey
        serviceLocator = Globals.getDefaultBaseServiceLocator();

        if (serviceLocator != null) {
            requestTracing = serviceLocator.getService(RequestTracingService.class);
            openTracing = serviceLocator.getService(OpenTracingService.class);
        }
    }

    // Before method invocation
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // If request tracing is enabled, and there's a trace in progress (which there should be!)
        if (requestTracing != null && requestTracing.isRequestTracingEnabled() && requestTracing.isTraceInProgress()) {
            // Check if CDI has been initialised by trying to get the BeanManager
            BeanManager beanManager = null;
            try {
                beanManager = CDI.current().getBeanManager();
            } catch (IllegalStateException ise) {
                // *Should* only get here if CDI hasn't been initialised, indicating that the app isn't using it
                logger.log(Level.FINE, "Error getting Bean Manager, presumably due to this application not using CDI", 
                        ise);
            }

            // Get the Traced annotation from the target method if CDI is initialised
            Traced tracedAnnotation = null;
            if (beanManager != null) {
                tracedAnnotation = OpenTracingCdiUtils.getAnnotation(beanManager, Traced.class, resourceInfo);
            }
            
            // If there is no annotation, or if there is an annotation and a config override indicating that we should
            // trace the method...
            if (tracedAnnotation == null || (boolean) OpenTracingCdiUtils.getConfigOverrideValue(
                    Traced.class, "value", resourceInfo, boolean.class)
                    .orElse(tracedAnnotation.value())) {
                // Get the application's tracer instance
                Tracer tracer = openTracing.getTracer(openTracing.getApplicationName(serviceLocator.getService(
                        InvocationManager.class)));
                
                // Create a Span and instrument it with details about the request
                SpanBuilder spanBuilder = tracer.buildSpan(determineOperationName(requestContext, tracedAnnotation))
                        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                        .withTag(Tags.HTTP_METHOD.getKey(), requestContext.getMethod())
                        .withTag(Tags.HTTP_URL.getKey(), requestContext.getUriInfo().getRequestUri().toURL().toString())
                        .withTag(Tags.COMPONENT.getKey(), "jaxrs");

                SpanContext spanContext = null;
                try {
                    // Extract the context from the tracer if there is one
                    spanContext = tracer.extract(Format.Builtin.HTTP_HEADERS, 
                            new MultivaluedMapToTextMap(requestContext.getHeaders()));
                } catch (IllegalArgumentException e){
                    logger.log(Level.WARNING, e.getMessage());
                }

                // If there was a context injected into the tracer, add it as a parent of the new span
                if (spanContext != null) {
                    spanBuilder.asChildOf(spanContext);
                }

                // Start the span and continue on to the targeted method
                spanBuilder.startActive(true);
            }
        }
    }
    
    // After method invocation
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) 
            throws IOException {
        // Try block so that we can always attach error info if there is any
        try {
            // If request tracing is enabled, and there is a trace in progress (which there should be!)
            if (requestTracing != null
                    && requestTracing.isRequestTracingEnabled()
                    && requestTracing.isTraceInProgress()) {

                // Check if CDI has been initialised by trying to get the BeanManager
                BeanManager beanManager = null;
                try {
                    beanManager = CDI.current().getBeanManager();
                } catch (IllegalStateException ise) {
                    // *Should* only get here if CDI hasn't been initialised, indicating that the app isn't using it
                    logger.log(Level.FINE, "Error getting Bean Manager, presumably due to this application not using CDI", 
                            ise);
                }

                // Get the Traced annotation from the target method if CDI is initialised
                Traced tracedAnnotation = null;
                if (beanManager != null) {
                    tracedAnnotation = OpenTracingCdiUtils.getAnnotation(beanManager, Traced.class, resourceInfo);
                }

                // If there is no annotation, or if there is an annotation and a config override indicating that we 
                // should trace the method...
                if (tracedAnnotation == null || (boolean) OpenTracingCdiUtils.getConfigOverrideValue(
                        Traced.class, "value", resourceInfo, boolean.class).orElse(tracedAnnotation.value())) {
                    // Get the active scope and span from the application's tracer - this *should* never be null
                    try (Scope activeScope = openTracing.getTracer(openTracing.getApplicationName(
                        serviceLocator.getService(InvocationManager.class))).scopeManager().active()) {
                        Span activeSpan = activeScope.span();
                        
                        // Get and add the response status to the active span
                        Response.StatusType statusInfo = responseContext.getStatusInfo();
                        activeSpan.setTag(Tags.HTTP_STATUS.getKey(), statusInfo.getStatusCode());

                        // If the response status is an error, add error information to the span
                        if (statusInfo.getFamily() == Response.Status.Family.CLIENT_ERROR
                                || statusInfo.getFamily() == Response.Status.Family.SERVER_ERROR) {
                            activeSpan.setTag(Tags.ERROR.getKey(), true);
                            activeSpan.log(Collections.singletonMap("event", "error"));

                            // If there's an attached exception, add it to the span
                            if (responseContext.hasEntity() && responseContext.getEntity() instanceof Throwable) {
                                activeSpan.log(Collections.singletonMap("error.object", responseContext.getEntity()));
                            }
                        }
                    }
                }
            }
        } finally {
            // If there's an attached error on the response, log the exception and set the response entity as the 
            // error message (instead of as the error object)
            if (responseContext.hasEntity() && responseContext.getEntity() instanceof Throwable) {
                Throwable throwable = (Throwable) responseContext.getEntity();
                logger.log(Level.SEVERE, throwable.toString(), throwable);
                responseContext.setEntity(throwable.toString());
            }
        }

    }

    /**
     * Helper method that determines what the operation name of the span.
     * 
     * @param requestContext The context of the request, obtained from the filter methods
     * @param tracedAnnotation The Traced annotation obtained from the target method
     * @return The name to use as the Span's operation name
     */
    private String determineOperationName(ContainerRequestContext requestContext, Traced tracedAnnotation) {
        // If there is no annotation, or if there is an annotation and a config override providing an empty name
        if (tracedAnnotation == null || ((String) OpenTracingCdiUtils.getConfigOverrideValue(
                Traced.class, "operationName", resourceInfo, String.class)
                .orElse(tracedAnnotation.operationName()))
                .equals("")) {
            // Set the name equal to the HTTP Method, followed by the method signature
            return requestContext.getMethod()
                    + ":"
                    + resourceInfo.getResourceClass().getCanonicalName()
                    + "."
                    + resourceInfo.getResourceMethod().getName();
        } else {
            // If there is no annotation, or if there is an annotation and a config override that provides
            // a non-empty name, use it as the operation name
            return (String) OpenTracingCdiUtils.getConfigOverrideValue(
                    Traced.class, "operationName", resourceInfo, String.class)
                    .orElse(tracedAnnotation.operationName());
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
