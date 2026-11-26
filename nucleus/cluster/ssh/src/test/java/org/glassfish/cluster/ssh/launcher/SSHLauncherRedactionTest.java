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
package org.glassfish.cluster.ssh.launcher;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that sensitive option values are redacted from commands logged by the
 * public {@link SSHLauncher} API.
 * <p>
 * The SSH session is mocked and the exec channel reports a timeout, so every log
 * statement on the path runs, including the WARNING that is visible by default.
 */
public class SSHLauncherRedactionTest {

    /**
     * A value containing a space, a single quote and a double quote, which
     * {@code FileUtils.quoteString} quotes differently per platform. The marker
     * lets the test detect a partially redacted value.
     */
    private static final String MARKER = "REDACTME";
    private static final String QUOTED_VALUE = "a'b\"c " + MARKER;

    private final List<LogRecord> records = new ArrayList<>();
    private SSHLauncher launcher;
    private ClientSession session;

    @Before
    public void setUp() throws Exception {
        OpenFuture open = mock(OpenFuture.class);
        when(open.verify(anyLong())).thenReturn(open);
        ChannelExec channel = mock(ChannelExec.class);
        when(channel.open()).thenReturn(open);
        when(channel.waitFor(anyCollection(), anyLong())).thenReturn(EnumSet.of(ClientChannelEvent.TIMEOUT));
        when(channel.getExitStatus()).thenReturn(null);
        session = mock(ClientSession.class);
        when(session.createExecChannel(anyString())).thenReturn(channel);

        launcher = new SSHLauncher() {
            @Override
            SshClient buildClient() {
                return mock(SshClient.class);
            }

            @Override
            ClientSession openSession(SshClient client) {
                return session;
            }
        };

        Logger logger = Logger.getLogger(SSHLauncherRedactionTest.class.getName());
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { records.add(r); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        launcher.init(logger);
    }

    @Test
    public void runCommand_redactsValueNeedingQuoting() throws Exception {
        launcher.runCommand(startInstanceCommand(QUOTED_VALUE), new ByteArrayOutputStream(), List.of());
        assertRedacted();
    }

    @Test
    public void runCommandAsIs_redactsValueNeedingQuoting() throws Exception {
        launcher.runCommandAsIs(startInstanceCommand(QUOTED_VALUE), new ByteArrayOutputStream(), List.of(), null);
        assertRedacted();
    }

    @Test
    public void runCommand_redactsSimpleValue() throws Exception {
        launcher.runCommand(startInstanceCommand(MARKER), new ByteArrayOutputStream(), List.of());
        assertRedacted();
    }

    /**
     * Redaction must only affect what is logged: the command sent to the remote host
     * still has to carry the real value of the sensitive option.
     */
    @Test
    public void runCommand_sendsUnredactedCommandToRemoteHost() throws Exception {
        launcher.runCommand(startInstanceCommand(MARKER), new ByteArrayOutputStream(), List.of());

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(session).createExecChannel(sent.capture());
        assertEquals("/opt/payara/glassfish/lib/nadmin --_auxinput - start-local-instance --sync normal"
                + " --windowspassword " + MARKER + " inst1", sent.getValue());
    }

    /**
     * What is logged must be exactly what is executed, apart from the masked value,
     * so admins can rely on logged paths and arguments.
     */
    @Test
    public void displayCommandMatchesWireCommandExceptForMaskedValue() {
        List<String> cmd = List.of("C:\\payara\\glassfish\\lib\\nadmin.bat", "start-local-instance",
                "--nodedir", "C:\\payara\\nodes", "--windowspassword", MARKER, "inst1");
        String wire = SSHLauncher.toWireCommand(cmd);
        assertEquals(wire.replace(MARKER, "********"), SSHLauncher.toDisplayCommand(cmd));
    }

    private static List<String> startInstanceCommand(String sensitiveValue) {
        return List.of("/opt/payara/glassfish/lib/nadmin", "--_auxinput", "-",
                "start-local-instance", "--sync", "normal",
                "--windowspassword", sensitiveValue, "inst1");
    }

    private void assertRedacted() {
        assertTrue("expected the command to be logged", records.stream()
                .anyMatch(r -> r.getMessage().contains("--windowspassword")));
        for (LogRecord r : records) {
            if (r.getMessage().contains(MARKER)) {
                fail("Sensitive value logged at " + r.getLevel() + ": " + r.getMessage());
            }
        }
    }
}
