package fish.payara.microprofile.opentracing.jaxrs.client;

import fish.payara.nucleus.requesttracing.domain.PropagationHeaders;
import fish.payara.requesttracing.jaxrs.client.SpanPropagator;
import org.glassfish.jersey.client.spi.PreInvocationInterceptor;

import javax.ws.rs.client.ClientRequestContext;

public class OpenTracingPreInvocationInterceptor implements PreInvocationInterceptor {

    @Override
    public void beforeRequest(ClientRequestContext requestContext) {
        // If there is an active span, add its context to the request as a property so it can be picked up by the filter
        requestContext.setProperty(PropagationHeaders.OPENTRACING_PROPAGATED_SPANCONTEXT, SpanPropagator.activeContext());
    }

}
