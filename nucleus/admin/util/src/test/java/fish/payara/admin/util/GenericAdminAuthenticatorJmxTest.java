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

    @Test
    public void nonArrayCredentialsAreRejected() {
        givenClientHostIsLocal();
        assertRejected("");
        assertRejected("admin");
        assertRejected(new Object());
    }

    @Test
    public void emptyAndSingleElementArraysAreRejected() {
        givenClientHostIsLocal();
        assertRejected(new String[0]);
        assertRejected(new String[] {"admin"});
    }

    @Test
    public void arraysWithHostElementAreRejected() {
        givenClientHostIsLocal();
        assertRejected(new String[] {"admin", "password", "localhost"});
        assertRejected(new String[] {"", "", "localhost"});
    }

    @Test
    public void nullElementsAreRejected() {
        givenClientHostIsLocal();
        assertRejected(new String[] {null, "password"});
        assertRejected(new String[] {"admin", null});
    }

    @Test
    public void unknownClientHostIsRejected() {
        // If the client host cannot be determined at all, authentication must fail closed,
        // regardless of what credentials were supplied.
        givenClientHostUnknown();
        assertRejected(null);
        assertRejected(new String[] {"admin", "password"});
        assertRejected(new String[] {"", ""});
    }

    @Test
    public void emptyCredentialsPassTheLocalRemoteCheckFromLocalHost() {
        // Null, and a 2-element array of empty strings, are both treated as "no credentials
        // supplied" (e.g. JConsole with blank fields) and must not be rejected by the
        // local/remote check for a local client, to preserve the vanilla no-password
        // development setup. This authenticator has no injected AdminService (this is a plain
        // unit test, not one running in a container), so authenticate() still fails past that
        // point with a NullPointerException; that failure, coming from code reached only after
        // the local/remote check, is itself proof the check did not reject these credentials.
        givenClientHostIsLocal();
        assertFailsPastLocalRemoteCheck(null);
        assertFailsPastLocalRemoteCheck(new String[] {"", ""});
    }

    private void assertFailsPastLocalRemoteCheck(Object credentials) {
        try {
            authenticator.authenticate(credentials);
            fail("Expected authenticate() to fail due to the missing AdminService injection: " + credentials);
        } catch (SecurityException unexpected) {
            fail("Empty credentials from a local host should not be rejected by the "
                    + "local/remote check: " + unexpected);
        } catch (NullPointerException expected) {
            // expected: as.getAuthRealmName() NPEs because `as` is never injected in this plain
            // unit test. Reaching this point confirms the local/remote check accepted the
            // credentials.
        }
    }

    @Test
    public void emptyCredentialsAreRejectedFromRemoteHost() {
        // The same two "no credentials supplied" shapes as above must instead be rejected
        // outright when the client host is not local.
        givenClientHostIsRemote();
        assertRejected(null);
        assertRejected(new String[] {"", ""});
    }
}
