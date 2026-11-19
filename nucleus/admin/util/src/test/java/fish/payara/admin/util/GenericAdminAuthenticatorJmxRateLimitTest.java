/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
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
package fish.payara.admin.util;

import com.sun.enterprise.admin.util.AuthenticationAttemptTracker;
import com.sun.enterprise.admin.util.GenericAdminAuthenticator;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import org.glassfish.security.services.api.authentication.AuthenticationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that brute-force protection applies to the JMX connector, mirroring the REST scenario covered by
 * {@code RestAuthenticationTest} in the payara-samples rest-management module.
 *
 * <p>A real RMI connector server is started in this JVM with a {@link GenericAdminAuthenticator} as its
 * authenticator, and is connected to over real RMI, so the client host is determined from the connection
 * exactly as in the server. That client is necessarily local, which the server-wide tracker exempts, so the
 * authenticator is given an {@link AuthenticationAttemptTracker} that exempts no hosts. The tracker's
 * delays and concurrent-delay limit are the production ones.
 *
 * <p>The admin realm login itself is stubbed, as there is no security service outside the server. The
 * authenticator's collaborators are set by reflection in place of HK2 injection. Without a
 * realm to look users up in, failed attempts are tracked under {@link AuthenticationAttemptTracker#UNKNOWN_USER_KEY}.
 */
public class GenericAdminAuthenticatorJmxRateLimitTest {

    // AuthenticationAttemptTracker.MAX_CONCURRENT_DELAYS
    private static final int MAX_CONCURRENT_DELAYS = 3;

    private static final String RATE_LIMITED_MESSAGE = "Too many concurrent authentication attempts";

    private static final String ADMIN_REALM = "admin-realm";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "adminadmin";
    private static final String WRONG_PASSWORD = "wrongpassword";

    private JMXConnectorServer connectorServer;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        AdminService adminService = mock(AdminService.class);
        when(adminService.getAuthRealmName()).thenReturn(ADMIN_REALM);
        when(adminService.getAssociatedAuthRealm()).thenReturn(mock(AuthRealm.class));

        AuthenticationService authService = mock(AuthenticationService.class);
        when(authService.login(eq(ADMIN_USER), any(char[].class), isNull())).thenAnswer(invocation -> {
            if (Arrays.equals(ADMIN_PASSWORD.toCharArray(), invocation.<char[]>getArgument(1))) {
                return new Subject();
            }
            throw new LoginException();
        });

        // RMI may report the client by a non-loopback address of this machine, so permit remote admin access
        SecureAdmin secureAdmin = mock(SecureAdmin.class);
        when(secureAdmin.getEnabled()).thenReturn("true");

        GenericAdminAuthenticator authenticator = new GenericAdminAuthenticator();
        setField(authenticator, "as", adminService);
        setField(authenticator, "authService", authService);
        setField(authenticator, "secureAdmin", secureAdmin);
        setField(authenticator, "attemptTracker", new AuthenticationAttemptTracker(host -> false));

        connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
                new JMXServiceURL("service:jmx:rmi://"),
                Map.of(JMXConnectorServer.AUTHENTICATOR, authenticator),
                MBeanServerFactory.newMBeanServer());
        connectorServer.start();

        executor = Executors.newFixedThreadPool(MAX_CONCURRENT_DELAYS);
    }

    @After
    public void tearDown() throws IOException {
        executor.shutdownNow();
        connectorServer.stop();
    }

    /**
     * Brute-force scenario over a real JMX connection.
     *
     * <p>Rate limiting only triggers under concurrent load: each failed attempt blocks inside an
     * exponential delay, and once MAX_CONCURRENT_DELAYS slots are occupied the next attempt is
     * rejected immediately rather than being delayed indefinitely.
     */
    @Test
    public void testRateLimiting() throws Exception {
        // Happy path: valid credentials succeed
        assertNull("Valid credentials should be accepted", tryConnect(ADMIN_PASSWORD));

        // Rate limiting: send MAX_CONCURRENT_DELAYS attempts with wrong credentials concurrently.
        // Each fails auth and blocks inside recordFailureAndDelay for an exponential sleep (1s, 2s, 4s).
        // While those slots are occupied a further attempt exceeds the threshold and is rejected.
        List<Future<SecurityException>> blocking = new ArrayList<>();
        for (int i = 0; i < MAX_CONCURRENT_DELAYS; i++) {
            blocking.add(executor.submit(() -> tryConnect(WRONG_PASSWORD)));
        }

        // Allow time for all concurrent attempts to fail auth and enter their delay sleep.
        // The shortest delay is 1 second, so there is ample headroom before the first slot frees up.
        Thread.sleep(500);

        SecurityException rejected = tryConnect(WRONG_PASSWORD);
        assertNotNull("Expected rejection once all concurrent delay slots are occupied", rejected);
        assertTrue("Expected rate-limiting once all concurrent delay slots are occupied, but got: " + rejected,
                isRateLimited(rejected));

        // Wait for the sleeping threads to finish (max delay is 4 s for the third failure)
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        for (Future<SecurityException> f : blocking) {
            SecurityException failure = f.get();
            assertNotNull("Wrong credentials should be rejected", failure);
            assertFalse("Delayed attempts should fail as a failed login, not be rate-limited", isRateLimited(failure));
        }

        // Valid credentials still succeed once the delay slots are free
        assertNull("Valid credentials should be accepted", tryConnect(ADMIN_PASSWORD));
    }

    /**
     * Connects as the admin user and immediately disconnects.
     *
     * @return the exception the connection was rejected with, or {@code null} if it succeeded
     */
    private SecurityException tryConnect(String password) throws IOException {
        Map<String, Object> env = Map.of(JMXConnector.CREDENTIALS, new String[] {ADMIN_USER, password});
        try (JMXConnector connector = JMXConnectorFactory.connect(connectorServer.getAddress(), env)) {
            connector.getMBeanServerConnection().getMBeanCount();
            return null;
        } catch (SecurityException e) {
            return e;
        }
    }

    private static boolean isRateLimited(SecurityException e) {
        return e.getMessage() != null && e.getMessage().contains(RATE_LIMITED_MESSAGE);
    }

    private static void setField(GenericAdminAuthenticator authenticator, String name, Object value)
            throws ReflectiveOperationException {
        Field field = GenericAdminAuthenticator.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(authenticator, value);
    }
}
