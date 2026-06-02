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
import org.apache.sshd.client.session.ClientSession;
import org.glassfish.cluster.ssh.sftp.SFTPClient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A live SSH connection to one host that can be used for both SFTP and exec
 * operations on the same underlying {@link ClientSession}, avoiding redundant
 * TCP and auth handshakes.
 *
 * <p>Obtain via {@link SSHLauncher#openConnection()}. Always use try-with-resources:
 * <pre>{@code
 * try (SSHConnection conn = launcher.openConnection()) {
 *     try (SFTPClient ftp = conn.openSftp()) { ... }
 *     conn.runCommand(...);
 * }
 * }</pre>
 *
 * <p>{@link #close()} closes the session, which cascades to any still-open channels —
 * so there is no leak even if {@link #openSftp()} is not explicitly closed first.
 */
public class SSHConnection implements AutoCloseable {

    private final SshClient client;
    private final ClientSession session;
    private final SSHLauncher launcher;

    SSHConnection(SshClient client, ClientSession session, SSHLauncher launcher) {
        this.client = client;
        this.session = session;
        this.launcher = launcher;
    }

    /**
     * Opens an SFTP subsystem on this connection.
     * Closing the returned client closes only the SFTP subsystem;
     * the underlying session remains open until this {@code SSHConnection} is closed.
     */
    public SFTPClient openSftp() throws IOException {
        return new SFTPClient(session);
    }

    /**
     * Runs a pre-quoted command string on this connection's session.
     * Use this when the command is already fully constructed with proper quoting
     * (e.g. Windows {@code cd /d "C:/path" && jar -xvf "file.zip"}).
     *
     * @param command the complete command string, already quoted
     * @param out     receives both stdout and stderr
     * @return the remote exit status, or {@code -1} if none was reported
     */
    public int runCommand(String command, OutputStream out) throws IOException, InterruptedException {
        return launcher.exec(session, SFTPClient.normalizePath(command), out, null);
    }

    /**
     * Runs a command on this connection's session.
     *
     * @param command    the command tokens (joined and quoted as needed)
     * @param out        receives both stdout and stderr
     * @param stdinLines lines to write to stdin, or {@code null} for no input
     * @return the remote exit status, or {@code -1} if none was reported
     */
    public int runCommand(List<String> command, OutputStream out, List<String> stdinLines)
            throws IOException, InterruptedException {
        String cmd = SFTPClient.normalizePath(SSHCommandUtils.commandListToQuotedString(command));
        return launcher.exec(session, cmd, out, SSHCommandUtils.listInputStream(stdinLines));
    }

    /**
     * Closes the underlying session and SSH client.
     * Any channels still open on the session are also closed by the session close cascade.
     */
    @Override
    public void close() {
        try { session.close(); } catch (IOException ignored) { }
        try { client.close(); } catch (IOException ignored) { }
    }
}
