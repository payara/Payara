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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package org.glassfish.cluster.ssh.launcher;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests resource management and auth correctness in SSHLauncher.
 */
public class SSHLauncherResourceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private SSHLauncher launcher;

    @Before
    public void setUp() {
        launcher = new SSHLauncher();
        launcher.init(Logger.getLogger(SSHLauncherResourceTest.class.getName()));
    }

    @Test
    public void getSFTPClient_closesSessionWhenSFTPClientCreationFails()
            throws IOException {
        ClientSession mockSession = mock(ClientSession.class);

        SSHLauncher testLauncher = new SSHLauncher() {
            @Override
            SshClient buildClient() {
                return mock(SshClient.class);
            }

            @Override
            ClientSession openSession(SshClient client) {
                return mockSession;
            }

            @Override
            SFTPClient newSFTPClient(SshClient client, ClientSession session)
                    throws IOException {
                throw new IOException("simulated: SFTP subsystem unavailable");
            }
        };
        testLauncher.init(Logger.getLogger("test"));

        try {
            testLauncher.getSFTPClient();
            fail("Expected IOException from newSFTPClient");
        } catch (IOException expected) {
            // expected
        }

        // The session must be closed even though it is the SFTPClient that failed,
        // not the session setup itself.
        verify(mockSession).close();
    }

    @Test
    public void tryKeyFileAuth_removesIdentitiesFromSessionAfterFailure() throws Exception {
        // Generate a real RSA key pair and write it as PKCS8 PEM so MINA SSHD
        // can load it without needing BouncyCastle in a special configuration.
        File keyFile = writeTestPrivateKeyFile();

        AuthFuture mockFuture = mock(AuthFuture.class);
        when(mockFuture.verify(anyLong())).thenReturn(mockFuture);
        when(mockFuture.isSuccess()).thenReturn(false); // auth fails

        ClientSession mockSession = mock(ClientSession.class);
        when(mockSession.auth()).thenReturn(mockFuture);

        launcher.tryKeyFileAuth(mockSession, keyFile);

        // Auth failed, so identities that were added must be removed to prevent
        // accumulation across subsequent auth method calls.
        verify(mockSession, atLeastOnce()).addPublicKeyIdentity(any(KeyPair.class));
        verify(mockSession, atLeastOnce()).removePublicKeyIdentity(any(KeyPair.class));
    }

    private File writeTestPrivateKeyFile() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(1024);
        KeyPair kp = gen.generateKeyPair();

        byte[] pkcs8Der = kp.getPrivate().getEncoded();
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(pkcs8Der);
        String pem = "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----\n";

        File keyFile = tmp.newFile("test_id_rsa");
        Files.writeString(keyFile.toPath(), pem);
        return keyFile;
    }
}
