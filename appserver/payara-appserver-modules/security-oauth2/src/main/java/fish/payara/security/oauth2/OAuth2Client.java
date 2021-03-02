package fish.payara.security.oauth2;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A client for accessing an OAuth2 token endpoint. Synchronised for concurrent
 * access, and supports keeping the same token active until it expires.
 * 
 * @author Matthew Gill
 */
public class OAuth2Client {

    private final Supplier<Response> invocation;

    private Response lastResponse;
    private volatile Instant lastResponseTime;
    private volatile Duration expiryTime;

    public OAuth2Client(String authUrl, String referrer, Map<? extends Serializable, ? extends Serializable> data) {
        // Create the base target
        final WebTarget target = ClientBuilder.newClient().target(authUrl);

        // Create form data
        final Form input = new Form();
        data.forEach((key, value) -> input.param(key.toString(), value.toString()));
        final Entity<Form> entity = Entity.entity(input, MediaType.APPLICATION_FORM_URLENCODED);

        // Build the request
        Builder requestBuilder = target.request()
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON);
        if (referrer != null && !referrer.isEmpty()) {
            requestBuilder = requestBuilder.header("referrer", referrer);
        }
        final Builder invocationTarget = requestBuilder;
        this.invocation = () -> invocationTarget.post(entity);
    }

    public OAuth2Client(String authUrl, Map<? extends Serializable, ? extends Serializable> data) {
        this(authUrl, null, data);
    }

    /**
     * Get the response from an authentication request. Caches the value until the
     * expiration time elapses if it's configured.
     * 
     * @return the most recent valid authentication response, or a new one if
     *         required.
     */
    public Response authenticate() {
        if (lastResponse == null || checkExpiry()) {
            synchronized (this) {
                if (lastResponse == null || checkExpiry()) {
                    Response result = invocation.get();
                    lastResponseTime = Instant.now();
                    if (result.getStatus() == 200) {
                        lastResponse = result;
                        lastResponse.bufferEntity();
                    }
                    return result;
                }
            }
        }
        return lastResponse;
    }

    /**
     * Configure the authentication request expiry time.
     * 
     * @param expiryTime
     */
    public void expire(Duration expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
     * Check if the expiry has passed or is null
     * @return
     */
    private final boolean checkExpiry() {
        return lastResponseTime == null
                || expiryTime == null
                || lastResponseTime.plus(expiryTime).isBefore(Instant.now());
    }
    
}
