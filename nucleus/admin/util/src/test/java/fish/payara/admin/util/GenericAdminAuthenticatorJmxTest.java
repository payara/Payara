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

import com.sun.enterprise.admin.util.GenericAdminAuthenticator;
import com.sun.enterprise.util.net.NetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

import static org.junit.Assert.fail;

/**
 * Tests that the JMX authentication entry point rejects credentials that could be used to
 * spoof the origin of a connection. None of these cases may reach the injected services.
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

    /** An opaque placeholder; its value is never inspected because {@link NetUtils#isThisHostLocal} is mocked. */
    private static final String SOME_HOST = "client";

    private final GenericAdminAuthenticator authenticator = new GenericAdminAuthenticator();

    private MockedStatic<RemoteServer> remoteServer;
    private MockedStatic<NetUtils> netUtils;

    @Before
    public void mockStatics() {
        remoteServer = Mockito.mockStatic(RemoteServer.class);
        netUtils = Mockito.mockStatic(NetUtils.class);
    }

    @After
    public void closeStaticMocks() {
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

    /**
     * Asserts that {@code credentials} are not rejected by the credentials-parsing or
     * local/remote checks, by verifying that execution reaches {@code as.getAuthRealmName()}
     * (the line immediately after those checks), rather than inferring this from some
     * exception being thrown, which could happen for unrelated reasons and give a false pass.
     * Authentication still fails past that point with a NullPointerException, because this
     * authenticator has no injected AdminService (this is a plain unit test, not one running
     * in a container); that is expected and is not what this assertion is checking.
     */
    private void assertPassesCredentialAndLocalRemoteChecks(Object credentials) {
        try {
            authenticator.authenticate(credentials);
            fail("Expected authenticate() to fail due to the missing AdminService injection: " + credentials);
        } catch (SecurityException unexpected) {
            fail("Credentials should not be rejected by the credentials or local/remote checks: " + unexpected);
        } catch (NullPointerException expected) {
            // expected: as.getAuthRealmName() NPEs because `as` is never injected in this plain
            // unit test. Reaching this point confirms neither check rejected the credentials.
        }
    }

    /**
     * Credentials that are not a {@code String[]} at all (a bare {@code String}, or any other
     * object) must be rejected outright, rather than treated as an empty user name and
     * password the way a {@code null} credentials object is.
     */
    @Test
    public void nonArrayCredentialsAreRejected() {
        givenClientHostIsLocal();
        assertRejected("admin");
        assertRejected(new Object());
    }

    /**
     * A credentials array with a third element must be rejected, regardless of its value,
     * since the peer host must come from the transport, never from the caller.
     */
    @Test
    public void arraysWithHostElementAreRejected() {
        givenClientHostIsLocal();
        assertRejected(new String[] {"admin", "password", "localhost"});
    }

    /**
     * If the client host cannot be determined at all (no RMI call in progress), authentication
     * must fail closed, regardless of what credentials were supplied.
     */
    @Test
    public void unknownClientHostIsRejected() {
        givenClientHostUnknown();
        assertRejected(new String[] {"admin", "password"});
    }

    /**
     * Null, and a 2-element array of empty strings, are both treated as "no credentials
     * supplied" and must be rejected by the local/remote check for a remote client, mirroring
     * {@link #emptyCredentialsPassTheLocalRemoteCheckFromLocalHost()}.
     */
    @Test
    public void emptyCredentialsAreRejectedFromRemoteHost() {
        givenClientHostIsRemote();
        assertRejected(null);
        assertRejected(new String[] {"", ""});
    }

    /**
     * A missing user name and/or password (a 0- or 1-element array) is treated the same as an
     * empty string for that field, exactly as a null array element is; see
     * {@link #emptyCredentialsPassTheLocalRemoteCheckFromLocalHost()}.
     */
    @Test
    public void emptyAndSingleElementArraysAreAcceptedFromLocalHost() {
        givenClientHostIsLocal();
        assertPassesCredentialAndLocalRemoteChecks(new String[0]);
        assertPassesCredentialAndLocalRemoteChecks(new String[] {"admin"});
    }

    /**
     * Null, and a 2-element array of empty strings, are both treated as "no credentials
     * supplied" (e.g. JConsole with blank fields) and must not be rejected by the local/remote
     * check for a local client, to preserve the vanilla no-password development setup.
     */
    @Test
    public void emptyCredentialsPassTheLocalRemoteCheckFromLocalHost() {
        givenClientHostIsLocal();
        assertPassesCredentialAndLocalRemoteChecks(null);
        assertPassesCredentialAndLocalRemoteChecks(new String[] {"", ""});
    }

    /**
     * A null user name or password element is treated the same as an empty string for that
     * field, not rejected outright. In particular, a user name with no password (e.g.
     * JConsole with the password field left blank) sends a null password element.
     */
    @Test
    public void nullArrayElementsAreTreatedAsEmptyFromLocalHost() {
        givenClientHostIsLocal();
        assertPassesCredentialAndLocalRemoteChecks(new String[] {null, "password"});
        assertPassesCredentialAndLocalRemoteChecks(new String[] {"admin", null});
    }

    /**
     * A null user name or password element is treated the same as an empty string for that
     * field, not rejected outright for being null. From a remote host, though, that empty field
     * is still rejected by the local/remote check, mirroring
     * {@link #nullArrayElementsAreTreatedAsEmptyFromLocalHost()}.
     */
    @Test
    public void nullArrayElementsAreTreatedAsEmptyFromRemoteHost() {
        givenClientHostIsRemote();
        assertRejected(new String[] {null, "password"});
        assertRejected(new String[] {"admin", null});
    }
}
