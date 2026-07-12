/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own applicable information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.samples.rest.management;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.ServerOperations;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.junit.Assert.assertEquals;

@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class RestAuthenticationTest {

    // AuthenticationAttemptTracker.MAX_CONCURRENT_DELAYS
    private static final int MAX_CONCURRENT_DELAYS = 3;

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "";
    private static final String RESPONSE_TYPE = "application/json";

    @ArquillianResource
    private URL baseUrl;

    private Client client;
    private URI sessionsUri;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return PayaraTestShrinkWrap.getWebArchive()
                .addClass(RestAuthenticationTest.class);
    }

    @Before
    public void setUp() throws MalformedURLException, URISyntaxException {
        client = ClientBuilder.newBuilder()
                .register(CsrfProtectionFilter.class)
                .build();
        sessionsUri = ServerOperations.toAdminPort(baseUrl).toURI().resolve("/management/sessions");
    }

    @After
    public void tearDown() {
        client.close();
    }

    /**
     * Reverse proxy / REST API scenario: X-GlassFish-Remote-Host is set, remoteHostName is omitted.
     *
     * <p>The /management/sessions auth check uses the real TCP connection address (127.0.0.1 = local),
     * so it succeeds without Secure Admin. Rate limiting tracks the remote host from X-GlassFish-Remote-Host.
     *
     * <p>Rate limiting only triggers under concurrent load: each failed attempt blocks inside an
     * exponential delay, and once MAX_CONCURRENT_DELAYS slots are occupied the next request is
     * rejected immediately with 429 rather than being delayed indefinitely.
     */
    @Test
    public void testRateLimitingViaReverseProxy() throws Exception {
        // Happy path: valid credentials succeed
        try (Response response = request(ADMIN_USER, ADMIN_PASSWORD)
                .header("X-GlassFish-Remote-Host", "example.org")
                .post(Entity.json("{}"), Response.class)) {
            assertEquals(200, response.getStatus());
        }

        // Rate limiting: send MAX_CONCURRENT_DELAYS requests with wrong credentials concurrently.
        // Each fails auth and blocks inside recordFailureAndDelay for an exponential sleep (1s, 2s, 4s).
        // While those slots are occupied a further request exceeds the threshold and gets 429.
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_DELAYS);
        List<Future<Integer>> blocking = new ArrayList<>();
        for (int i = 0; i < MAX_CONCURRENT_DELAYS; i++) {
            blocking.add(executor.submit(() -> {
                try (Response r = request(ADMIN_USER, "wrongpassword")
                        .header("X-GlassFish-Remote-Host", "example.org")
                        .post(Entity.json("{}"), Response.class)) {
                    return r.getStatus();
                }
            }));
        }

        // Allow time for all concurrent requests to fail auth and enter their delay sleep.
        // The shortest delay is 1 second, so there is ample headroom before the first slot frees up.
        Thread.sleep(500);

        try (Response response = request(ADMIN_USER, "wrongpassword")
                .header("X-GlassFish-Remote-Host", "example.org")
                .post(Entity.json("{}"), Response.class)) {
            assertEquals("Expected rate-limiting once all concurrent delay slots are occupied", 429, response.getStatus());
        }

        // Wait for the sleeping threads to finish (max delay is 4 s for the third failure)
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        for (Future<Integer> f : blocking) {
            assertEquals(401, f.get().intValue());
        }

        // Reset the failure state so it does not bleed into other tests
        try (Response r = request(ADMIN_USER, ADMIN_PASSWORD)
                .header("X-GlassFish-Remote-Host", "example.org")
                .post(Entity.json("{}"), Response.class)) {
            assertEquals(200, r.getStatus());
        }
    }

    /**
     * Admin Console simulation: both X-GlassFish-Remote-Host header and remoteHostName body property are sent,
     * mirroring what AdminConsoleAuthModule does when forwarding a browser login.
     *
     * <p>Without Secure Admin enabled, /management/sessions re-authenticates using remoteHostName="example.org"
     * as the remote host. Because "example.org" is not a local address and Secure Admin is disabled,
     * RemoteAdminAccessException is thrown and the response is 403.
     *
     * <p>Note: rate limiting does not apply to this path. The first /management/sessions auth check
     * (in RestAdapter, before Jersey dispatches to SessionsResource) uses the real TCP address (127.0.0.1)
     * for the remote-admin check — that passes — but with wrong credentials it records a failure for
     * "example.org" and returns 401 without ever reaching SessionsResource. With correct credentials,
     * the first check records a success (resetting the counter) before SessionsResource produces the 403.
     * Either way there is no failure accumulation from the 403 path itself. Secure Admin must be enabled
     * for the Admin Console to function correctly.
     */
    @Test
    public void testAdminConsoleWithoutSecureAdmin() {
        MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
        body.putSingle("remoteHostName", "example.org");
        try (Response response = request(ADMIN_USER, ADMIN_PASSWORD)
                .header("X-GlassFish-Remote-Host", "example.org")
                .post(Entity.entity(body, APPLICATION_FORM_URLENCODED), Response.class)) {
            assertEquals("remoteHostName on a non-local host requires Secure Admin", 403, response.getStatus());
        }
    }

    /**
     * Local access scenario: neither X-GlassFish-Remote-Host nor remoteHostName is present.
     *
     * <p>The tracking host resolves to the loopback address, which is always excluded from rate limiting.
     * Repeated auth failures with a wrong password yield 401 but never escalate to 429.
     */
    @Test
    public void testLocalRequestsNotRateLimited() {
        // Loop past MAX_CONCURRENT_DELAYS to confirm that localhost failures never trigger 429
        for (int i = 0; i < MAX_CONCURRENT_DELAYS + 1; i++) {
            try (Response response = request(ADMIN_USER, "wrongpassword")
                    .post(Entity.json("{}"), Response.class)) {
                assertEquals("Local failed auth should yield 401, never 429", 401, response.getStatus());
            }
        }
    }

    private Invocation.Builder request(String username, String password) {
        return client.target(sessionsUri)
                .register(HttpAuthenticationFeature.basic(username, password))
                .request(RESPONSE_TYPE)
                .header("X-Requested-By", "Payara");
    }
}
