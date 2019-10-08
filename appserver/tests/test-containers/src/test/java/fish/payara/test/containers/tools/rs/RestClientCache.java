package fish.payara.test.containers.tools.rs;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.BasicBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clients are {@link Closeable}, thread-safe, and expensive.
 * This is a simple cache for test clients.
 *
 * @author David Matějček
 */
public class RestClientCache implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientCache.class);

    private final ConcurrentHashMap<ClientCacheKey, Client> cache;


    /**
     * Initializes cache for {@link Client} instances.
     */
    public RestClientCache() {
        this.cache = new ConcurrentHashMap<>();
    }


    /**
     * @return basic JAX-RS client, does not follow HTTP redirects automatically.
     */
    public Client getAnonymousClient() {
        final ClientCacheKey key = new ClientCacheKey(false, true, null, null);
        return this.cache.computeIfAbsent(key, this::build);
    }


    /**
     * This client sends login and password only if the last response is "unauthenticated".
     *
     * @param followRedirects - may be used if you want to have control over the all communication
     * @param user - user's login
     * @param password - user's password
     * @return basic JAX-RS client
     */
    public Client getPreemptiveClient(final boolean followRedirects, final String user, final String password) {
        return buildAndCache(followRedirects, true, user, password);
    }


    /**
     * This client sends login directly in the first request.
     *
     * @param followRedirects - may be used if you want to have control over the all communication
     * @param user - user's login
     * @param password - user's password
     * @return basic JAX-RS client
     */
    public Client getNonPreemptiveClient(final boolean followRedirects, final String user, final String password) {
        return buildAndCache(followRedirects, false, user, password);
    }


    private Client buildAndCache(final boolean followRedirects, final boolean preemptive, final String username,
        final String password) {
        LOG.debug("buildAndCache(followRedirects={}, preemptive={})", followRedirects, preemptive);
        final ClientCacheKey key = new ClientCacheKey(followRedirects, preemptive, username, password);
        return this.cache.computeIfAbsent(key, this::build);
    }


    private Client build(final ClientCacheKey key) {
        final ClientConfig clientCfg = new ClientConfig();
        clientCfg.register(new JacksonFeature());
        clientCfg.register(new ObjectMapper());
        if (key.username != null) {
            clientCfg.register(createAuthFeature(key));
        }
        clientCfg.register(LoggingResponseFilter.class);
        clientCfg.property(ClientProperties.FOLLOW_REDIRECTS, key.followRedirects);
        return ClientBuilder.newClient(clientCfg);
    }


    private HttpAuthenticationFeature createAuthFeature(final ClientCacheKey key) {
        final BasicBuilder authFeature = HttpAuthenticationFeature.basicBuilder();
        authFeature.credentials(key.username, key.password);
        if (!key.preemptive) {
            authFeature.nonPreemptive();
        }
        return authFeature.build();
    }


    @Override
    public void close() {
        LOG.debug("close()");
        this.cache.values().stream().forEach(Client::close);
    }

    private static final class ClientCacheKey {

        private final boolean followRedirects;
        private final boolean preemptive;
        private final String username;
        private final String password;


        private ClientCacheKey(final boolean followRedirects, final boolean preemptive, final String username,
            final String password) {
            this.followRedirects = followRedirects;
            this.preemptive = preemptive;
            this.username = username;
            this.password = password;
        }


        @Override
        public int hashCode() {
            return Objects.hash(this.followRedirects, this.preemptive, this.username, this.password);
        }


        @Override
        public boolean equals(final Object another) {
            if (another == null || !another.getClass().equals(this.getClass())) {
                return false;
            }
            final ClientCacheKey anotherKey = (ClientCacheKey) another;
            return this.followRedirects == anotherKey.followRedirects && this.preemptive == anotherKey.preemptive
                && Objects.equals(anotherKey.username, this.username)
                && Objects.equals(anotherKey.password, this.password);
        }
    }
}
