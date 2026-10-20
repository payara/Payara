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
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.security.services.api.authentication.AuthenticationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Tests for the JMX authentication entry point in {@link GenericAdminAuthenticator#authenticate(Object)}.
 *
 * <p>Tests suffixed {@code AreAccepted}/{@code IsAccepted} document credential shapes that reached
 * the authentication layer before FISH-14190; a test failing on the current code signals a
 * regression. Tests suffixed {@code AreRejected}/{@code IsRejected} verify deliberate security
 * fixes that must be preserved.
 *
 * <p>{@link GenericAdminAuthenticator#authenticate(Object)} determines the caller's host via
 * {@link RemoteServer#getClientHost()}, which only returns a value while an RMI call is
 * actually being serviced, and decides whether that host is local via
 * {@link NetUtils#isThisHostLocal}, which resolves and inspects this machine's real network
 * interfaces. A plain unit test does neither of those things, so both are mocked statically:
 * this keeps the local/remote outcome explicit and under the test's control, rather than
 * depending on some IP address actually behaving as non-local in whatever environment the
 * test happens to run in.
 */
public class GenericAdminAuthenticatorJmxTest {

    private static final String SOME_HOST = "client";
    private static final String REALM = "admin-realm";

    private final GenericAdminAuthenticator authenticator = new GenericAdminAuthenticator();

    private MockedStatic<RemoteServer> remoteServer;
    private MockedStatic<NetUtils> netUtils;

    private AuthenticationAttemptTracker attemptTracker;

    @Before
    public void setUp() throws Exception {
        remoteServer = Mockito.mockStatic(RemoteServer.class);
        netUtils = Mockito.mockStatic(NetUtils.class);

        // isThisHostLocal(null) returns true in real code; non-array credentials handling
        // before FISH-14190 never called getClientHost(), so host stayed null and this path was taken.
        netUtils.when(() -> NetUtils.isThisHostLocal((String) null)).thenReturn(true);

        AuthRealm authRealm = mock(AuthRealm.class);
        when(authRealm.getGroupMapping()).thenReturn(null); // null → isInAdminGroup() returns true

        AdminService adminService = mock(AdminService.class);
        when(adminService.getAuthRealmName()).thenReturn(REALM);
        when(adminService.getAssociatedAuthRealm()).thenReturn(authRealm);

        attemptTracker = mock(AuthenticationAttemptTracker.class);

        // authService is kept as a mock to prevent NPE after checkBeforeAuthentication is reached;
        // the login result is not relevant to any assertion in this suite.
        AuthenticationService authService = mock(AuthenticationService.class);
        when(authService.login(any(), any(char[].class), any())).thenReturn(null);

        inject("as", adminService);
        inject("attemptTracker", attemptTracker);
        inject("authService", authService);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = GenericAdminAuthenticator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(authenticator, value);
    }

    @After
    public void tearDown() {
        remoteServer.close();
        netUtils.close();
    }

    private void givenClientHostIsLocal() {
        remoteServer.when(RemoteServer::getClientHost).thenReturn(SOME_HOST);
        netUtils.when(() -> NetUtils.isThisHostLocal(SOME_HOST)).thenReturn(true);
    }

    private void givenClientHostIsRemote() {
        remoteServer.when(RemoteServer::getClientHost).thenReturn(SOME_HOST);
        netUtils.when(() -> NetUtils.isThisHostLocal(SOME_HOST)).thenReturn(false);
    }

    private void givenClientHostUnknown() {
        remoteServer.when(RemoteServer::getClientHost)
                .thenThrow(new ServerNotActiveException("no RMI call is in progress (test)"));
    }

    private void assertRejected(Object credentials) {
        try {
            authenticator.authenticate(credentials);
            fail("Credentials should have been rejected: " + credentials);
        } catch (SecurityException expected) {
            // expected
        }
    }

    // "Accepted" = checkBeforeAuthentication was reached before FISH-14190; failing on current code is a regression.
    // "Rejected" = deliberate fix in FISH-14190.

    @Test
    public void nonArrayCredentialsAreAccepted() throws Exception {
        givenClientHostIsLocal();

        authenticator.authenticate("admin");
        verify(attemptTracker).checkBeforeAuthentication(isNull(), isNull(), any());

        reset(attemptTracker);

        authenticator.authenticate(new Object());
        verify(attemptTracker).checkBeforeAuthentication(isNull(), isNull(), any());
    }

    @Test
    public void emptyAndSingleElementArraysAreAccepted() throws Exception {
        givenClientHostIsLocal();

        // String[0]: user="" → null (via getDefaultAdminUser), host=SOME_HOST (getClientHost was called).
        authenticator.authenticate(new String[0]);
        verify(attemptTracker).checkBeforeAuthentication(isNull(), eq(SOME_HOST), any());

        reset(attemptTracker);

        // String[1]: user="admin" → UNKNOWN_USER_KEY (Realm.getInstance() throws without HK2 infrastructure), host=SOME_HOST.
        authenticator.authenticate(new String[] {"admin"});
        verify(attemptTracker).checkBeforeAuthentication(
                eq(AuthenticationAttemptTracker.UNKNOWN_USER_KEY), eq(SOME_HOST), any());
    }

    @Test
    public void arraysWithHostElementAreRejected() {
        givenClientHostIsRemote();
        netUtils.when(() -> NetUtils.isThisHostLocal("localhost")).thenReturn(true);
        assertRejected(new String[] {"admin", "password", "localhost"});
    }

    @Test
    public void nullElementsAreAccepted() throws Exception {
        givenClientHostIsLocal();

        // Null username NPEs at user.isEmpty() before checkBeforeAuthentication is reached in both
        // old and new code — the only meaningful check is that it was not a SecurityException.
        try {
            authenticator.authenticate(new String[] {null, "password"});
            fail("Expected authenticate() to throw");
        } catch (SecurityException e) {
            fail("Null username should not be rejected with SecurityException: " + e.getMessage());
        } catch (Exception ignored) {
            // NPE at user.isEmpty() in both old and new code — not a security rejection.
        }
        verifyNoInteractions(attemptTracker);

        // Null password became ""; execution reached checkBeforeAuthentication (UNKNOWN_USER_KEY because Realm.getInstance() throws without HK2 infrastructure).
        authenticator.authenticate(new String[] {"admin", null});
        verify(attemptTracker).checkBeforeAuthentication(
                eq(AuthenticationAttemptTracker.UNKNOWN_USER_KEY), eq(SOME_HOST), any());
    }

    @Test
    public void unknownClientHostThrowsRuntimeException() {
        // Before FISH-14190: ServerNotActiveException was wrapped in RuntimeException (not SecurityException),
        // so it propagated past any SecurityException catch — it was never a security rejection.
        // checkBeforeAuthentication was never reached because the exception was thrown at getClientHost().
        givenClientHostUnknown();
        try {
            authenticator.authenticate(new String[] {"admin", "password"});
            fail("Expected authenticate() to throw");
        } catch (SecurityException e) {
            fail("Unknown client host should not produce SecurityException: " + e.getMessage());
        } catch (RuntimeException ignored) {
            // Before FISH-14190: ServerNotActiveException wrapped in RuntimeException.
        }
        verifyNoInteractions(attemptTracker);
    }

    @Test
    public void emptyCredentialsAreRejectedFromRemoteHost() {
        givenClientHostIsRemote();
        assertRejected(null);
        assertRejected(new String[] {"", ""});
        assertRejected(new String[] {"admin", ""});
    }
}
