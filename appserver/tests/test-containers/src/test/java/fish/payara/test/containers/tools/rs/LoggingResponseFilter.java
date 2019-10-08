package fish.payara.test.containers.tools.rs;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Logging filter. Logs REST requests and responses.
 *
 * @author David Matějček
 */
@Provider
public class LoggingResponseFilter implements ClientResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingResponseFilter.class);


    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) {
        if (!LOG.isInfoEnabled()) {
            return;
        }
        LOG.info("filter(requestContext, responseContext);" //
            + "\nrequestContext: {}\nrequest headers: {}\nrequest cookies: {}"
            + "\nresponseContext: {}\nresponse headers: {}\nresponse cookies: {}\nresponse hasEntity: {}",
            ReflectionToStringBuilder.toStringExclude(requestContext, "entity"), requestContext.getHeaders(),
            requestContext.getCookies(), responseContext, responseContext.getHeaders(), responseContext.getCookies(),
            responseContext.hasEntity());
    }
}
