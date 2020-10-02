package fish.payara.microprofile.config.extensions;

import java.io.StringReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.nimbusds.jwt.SignedJWT;

/**
 * 
 */
public class OAuth2Client {

    private final Supplier<Response> invocation;

    private String lastToken;
    private Instant expiryTime;

    public OAuth2Client(String authUrl, SignedJWT token) {
        // Create the base target
        final WebTarget target = ClientBuilder.newClient().target(authUrl);

        // Create form data
        final Form input = new Form();
        input.param("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        input.param("assertion", token.serialize());
        final Entity<Form> entity = Entity.entity(input, MediaType.APPLICATION_FORM_URLENCODED);

        // Build the request
        final Builder requestBuilder = target.request()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .accept("application/json");
        this.invocation = () -> requestBuilder.post(entity);
    }

    public synchronized String getAccessToken() {

        if (lastToken == null || expiryTime == null || expiryTime.isBefore(Instant.now())) {
            final Response response = invocation.get();
            final String authResponseData = response.readEntity(String.class);

            if (response.getStatus() != 200) {
                System.out.println(authResponseData);
                return null;
            }

            JsonObject data = Json.createReader(new StringReader(authResponseData))
                    .readObject();
            
            Integer expirySeconds = data.getInt("expires_in");
            this.expiryTime = Instant.now().plus(expirySeconds - 10, ChronoUnit.SECONDS);
            this.lastToken = data.getString("access_token");
        }

        return lastToken;
    }
    
}
