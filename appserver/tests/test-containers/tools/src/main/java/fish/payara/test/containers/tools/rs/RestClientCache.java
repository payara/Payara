/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.test.containers.tools.rs;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
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
 * The {@link Client} does not implement {@link Closeable} (but they are closeable), thread-safe,
 * and expensive.
 * <p>
 * This is a simple cache for test clients - you don't need to close them or recreate them. <br>
 * The cache is useful only for testing where you have only a limited set of variants of clients.
 * <p>
 * The {@link #close()} method closes all provided clients.
 *
 * @author David Matějček
 */
public class RestClientCache implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientCache.class);

    private final ConcurrentHashMap<ClientCacheKey, Client> cache;
    private final SSLContext sslContext;


    /**
     * Initializes cache for {@link Client} instances.
     */
    public RestClientCache() {
        this.cache = new ConcurrentHashMap<>();
        this.sslContext = null;
    }

    /**
     * Initializes cache for {@link Client} instances.
     *
     * @param sslContext
     */
    public RestClientCache(final SSLContext sslContext) {
        this.cache = new ConcurrentHashMap<>();
        this.sslContext = sslContext;
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
        final ClientBuilder builder = ClientBuilder.newBuilder().withConfig(clientCfg);
        if (this.sslContext != null) {
            builder.sslContext(this.sslContext);
        }
        return builder.build();
    }


    private HttpAuthenticationFeature createAuthFeature(final ClientCacheKey key) {
        final BasicBuilder authFeature = HttpAuthenticationFeature.basicBuilder();
        authFeature.credentials(key.username, key.password);
        if (!key.preemptive) {
            authFeature.nonPreemptive();
        }
        return authFeature.build();
    }


    /**
     * Closes all cached clients.
     * <p>
     * This makes this instance reset to the state after creation (except internal capacity
     * of the cache).
     */
    @Override
    public void close() {
        LOG.debug("close()");
        this.cache.values().stream().forEach(Client::close);
        this.cache.clear();
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
