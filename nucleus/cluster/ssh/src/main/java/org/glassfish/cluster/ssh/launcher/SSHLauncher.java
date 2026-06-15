/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2019-2026 Payara Foundation and/or affiliates

package org.glassfish.cluster.ssh.launcher;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.ExceptionUtil;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.FileUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.SSHUtil;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.RelativePathResolver;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Mordani
 */
@Service(name = "SSHLauncher")
@PerLookup
public class SSHLauncher {

    private static final String SSH_DIR = ".ssh" + File.separator;
    private static final String AUTH_KEY_FILE = "authorized_keys";
    private static final String KNOWN_HOSTS_FILE = ".ssh/known_hosts";

    protected static final String TIMEOUT_PROPERTY = "fish.payara.node.ssh.timeout";
    protected static final int DEFAULT_TIMEOUT_MSEC = 120000;
    protected static final int MINIMUM_TIMEOUT_MSEC = 1;

    /**
     * Separate timeout for waiting on a running command to complete.
     * Connection setup (TCP + auth) uses {@link #getTimeout()} which defaults to 120 s.
     * Command execution (e.g. start-local-instance --timeout 300) can legitimately take
     * much longer, so we use a larger fixed ceiling here.
     * Configurable via {@value #EXEC_TIMEOUT_PROPERTY}.
     */
    protected static final String EXEC_TIMEOUT_PROPERTY = "fish.payara.node.ssh.execTimeout";
    protected static final int DEFAULT_EXEC_TIMEOUT_MSEC = 600000; // 10 minutes

    private static final String SSH_KEYGEN = "ssh-keygen";

    private String host;
    private int port;
    private String userName;
    private String keyFile;
    private char[] privateKey;
    private String keyPassPhrase;
    private File knownHosts;
    private Logger logger;
    private String password;
    private String rawPassword;
    private String rawKeyPassPhrase;

    public void init(Logger logger) {
        this.logger = logger;
    }

    /**
     * Initialises using a Node configuration object.
     */
    public void init(Node node, Logger logger) {
        this.logger = logger;

        SshConnector connector = node.getSshConnector();

        host = connector.getSshHost();
        if (SSHUtil.checkString(host) == null) {
            host = node.getNodeHost();
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Connecting to host " + host);
        }

        SshAuth sshAuth = connector.getSshAuth();
        String userName = null;
        if (sshAuth != null) {
            userName = sshAuth.getUserName();
            this.keyFile = sshAuth.getKeyfile();
            this.rawPassword = sshAuth.getPassword();
            this.rawKeyPassPhrase = sshAuth.getKeyPassphrase();
        }

        int port;
        try {
            port = Integer.parseInt(connector.getSshPort());
        } catch (NumberFormatException nfe) {
            port = 22;
        }

        init(userName, host, port, rawPassword, keyFile, rawKeyPassPhrase, logger);
    }

    public void init(String userName, String host, int port, String password, char[] privateKey, Logger logger) {
        init(userName, host, port, password, null, null, privateKey, logger);
    }

    public void init(String userName, String host, int port, String password, String keyFile, String keyPassPhrase, Logger logger) {
        init(userName, host, port, password, keyFile, keyPassPhrase, null, logger);
    }

    private void init(String userName, String host, int port, String password, String keyFile,
                      String keyPassPhrase, char[] privateKey, Logger logger) {
        this.port = port == 0 ? 22 : port;
        this.host = host;
        this.privateKey = (privateKey != null) ? Arrays.copyOf(privateKey, privateKey.length) : null;
        this.keyFile = (keyFile == null && privateKey == null) ? SSHUtil.getExistingKeyFile() : keyFile;
        this.logger = logger;
        this.userName = SSHUtil.checkString(userName) == null ? System.getProperty("user.name") : userName;
        this.rawPassword = password;
        this.password = expandPasswordAlias(password);
        this.rawKeyPassPhrase = keyPassPhrase;
        this.keyPassPhrase = expandPasswordAlias(keyPassPhrase);

        if (knownHosts == null) {
            knownHosts = new File(System.getProperty("user.home"), KNOWN_HOSTS_FILE);
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("SSH info is " + this);
        }
    }

    /**
     * Creates and starts an {@link SshClient} backed by the user's known_hosts file.
     *
     * <p>New (unknown) hosts are accepted and their keys recorded automatically,
     * consistent with previous behaviour. However, hosts whose key has <em>changed</em>
     * since the last connection are rejected with a loud warning — a changed key is the
     * signature of an active MITM attack and must never be silently accepted.
     */
    SshClient buildClient() throws IOException {
        logger.finer("Building SSH client, knownHosts=" + knownHosts);
        SshClient client = SshClient.setUpDefaultClient();

        // Ensure the known_hosts file exists so the verifier can record new keys (TOFU).
        // If creation fails we fall back to accept-all and log a warning.
        if (knownHosts != null && !knownHosts.exists()) {
            File sshDir = knownHosts.getParentFile();
            if (sshDir != null && !sshDir.exists()) {
                sshDir.mkdirs();
            }
            try {
                knownHosts.createNewFile();
            } catch (IOException ex) {
                client.stop();
                throw new IOException("Cannot create known_hosts file at " + knownHosts
                        + " — refusing SSH connection to avoid unverified host keys. "
                        + "Ensure the .ssh directory exists and is writable.", ex);
            }
        }

        KnownHostsServerKeyVerifier verifier =
                new KnownHostsServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE,
                                               Objects.requireNonNull(knownHosts).toPath()) {

                    @Override
                    public boolean acceptModifiedServerKey(
                            ClientSession session,
                            java.net.SocketAddress remoteAddress,
                            KnownHostEntry entry,
                            java.security.PublicKey expected,
                            java.security.PublicKey actual) {
                        logger.severe("POSSIBLE MITM ATTACK: the host key for " + remoteAddress
                                + " has changed. The connection has been rejected."
                                + " If this change is legitimate, remove the old entry from "
                                + knownHosts + " and reconnect.");
                        return false;
                    }
                };
        client.setServerKeyVerifier(verifier);

        client.start();
        return client;
    }

    /**
     * Opens a session to {@link #host}:{@link #port}, authenticates it, and
     * returns it. The caller is responsible for closing both the returned session
     * and the supplied {@code client}.
     */
    ClientSession openSession(SshClient client)
            throws IOException, GeneralSecurityException {

        logger.finer("Connecting to " + userName + "@" + host + ":" + port);

        ClientSession session = client.connect(userName, host, port)
                .verify(getTimeout())
                .getSession();

        boolean authenticated = false;
        StringBuilder message = new StringBuilder();

        if (!authenticated && privateKey != null) {
            try {
                authenticated = tryPrivateKeyAuth(session, null, privateKey);
            } catch (IOException | GeneralSecurityException ex) {
                appendWarning(message, "SSH auth with private key failed: "
                        + ExceptionUtil.getRootCause(ex).getMessage(), ex);
            }
        }

        if (!authenticated && SSHUtil.checkString(keyFile) != null) {
            logger.finer("Attempting key-file auth: " + keyFile);
            File key = new File(keyFile);
            if (key.exists()) {
                try {
                    authenticated = tryKeyFileAuth(session, key);
                    logger.finer("Key-file auth result: " + authenticated);
                } catch (IOException | GeneralSecurityException ex) {
                    appendWarning(message, "SSH auth with key file " + key + " failed: "
                            + ExceptionUtil.getRootCause(ex).getMessage(), ex);
                }
            } else {
                logger.warning("Key file does not exist: " + keyFile);
            }
        }

        if (!authenticated && SSHUtil.checkString(password) != null) {
            logger.finer("Attempting password auth");
            try {
                session.addPasswordIdentity(password);
                authenticated = session.auth().verify(getTimeout()).isSuccess();
                logger.finer("Password auth result: " + authenticated);
            } catch (IOException ex) {
                appendWarning(message, "SSH auth with password failed: "
                        + ExceptionUtil.getRootCause(ex).getMessage(), ex);
            }
        }

        if (!authenticated && SSHUtil.checkString(keyFile) == null
                && SSHUtil.checkString(password) == null && privateKey == null) {
            logger.finer("No credentials configured, probing default key files");
            message.append("No key or password specified – trying default keys\n");
            File home = new File(System.getProperty("user.home"));
            for (String keyName : Arrays.asList("id_rsa", "id_dsa", "identity")) {
                File key = new File(home, ".ssh/" + keyName);
                if (key.exists()) {
                    logger.finer("Trying default key: " + key);
                    try {
                        authenticated = tryKeyFileAuth(session, key);
                        if (authenticated) {
                            logger.fine("Default key auth succeeded: " + key);
                            break;
                        }
                    } catch (IOException | GeneralSecurityException ignored) {
                    }
                }
            }
        }

        if (!authenticated) {
            session.close();
            throw new SSHAuthenticationException("Could not authenticate to " + host + ". " + message);
        }

        logger.fine("Successfully connected to " + userName + "@" + host);
        return session;
    }

    boolean tryKeyFileAuth(ClientSession session, File key)
            throws IOException, GeneralSecurityException {
        FilePasswordProvider pwProvider = GenericUtils.isEmpty(keyPassPhrase)
                ? FilePasswordProvider.EMPTY
                : FilePasswordProvider.of(keyPassPhrase);

        Iterable<KeyPair> pairs = SecurityUtils.getKeyPairResourceParser()
                .loadKeyPairs(null, key.toPath(), pwProvider);
        if (pairs == null) {
            return false;
        }
        List<KeyPair> loaded = new ArrayList<>();
        for (KeyPair kp : pairs) {
            loaded.add(kp);
            session.addPublicKeyIdentity(kp);
        }
        if (loaded.isEmpty()) {
            return false;
        }
        boolean ok = session.auth().verify(getTimeout()).isSuccess();
        if (!ok) {
            loaded.forEach(session::removePublicKeyIdentity);
        }
        return ok;
    }

    boolean tryPrivateKeyAuth(ClientSession session, File keyFile, char[] privateKeyChars)
            throws IOException, GeneralSecurityException {
        FilePasswordProvider pwProvider = GenericUtils.isEmpty(keyPassPhrase)
                ? FilePasswordProvider.EMPTY
                : FilePasswordProvider.of(keyPassPhrase);

        // Encode without creating a String so the key material stays off the immutable string pool
        // and the intermediate ByteBuffer can be zeroed after use.
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(privateKeyChars));
        byte[] keyBytes = new byte[bb.remaining()];
        bb.get(keyBytes);
        Arrays.fill(bb.array(), (byte) 0);
        List<KeyPair> loaded = new ArrayList<>();
        try (InputStream keyStream = new ByteArrayInputStream(keyBytes)) {
            Iterable<KeyPair> pairs = SecurityUtils.getKeyPairResourceParser()
                    .loadKeyPairs(null, (NamedResource) () -> "in-memory", pwProvider, keyStream);
            if (pairs == null) {
                return false;
            }
            for (KeyPair kp : pairs) {
                loaded.add(kp);
                session.addPublicKeyIdentity(kp);
            }
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
        if (loaded.isEmpty()) {
            return false;
        }
        boolean ok = session.auth().verify(getTimeout()).isSuccess();
        if (!ok) {
            loaded.forEach(session::removePublicKeyIdentity);
        }
        return ok;
    }

    private void appendWarning(StringBuilder sb, String msg, Exception ex) {
        sb.append(msg).append("\n");
        logger.log(Level.WARNING, msg, ex);
    }

    public int runCommand(List<String> command, OutputStream os, List<String> stdinLines)
            throws IOException, InterruptedException {
        return runCommand(commandListToQuotedString(command), os, stdinLines);
    }

    public int runCommand(List<String> command, OutputStream os)
            throws IOException, InterruptedException {
        return runCommand(command, os, null);
    }

    /** WARNING: does not handle paths with spaces — prefer the List overloads. */
    public int runCommand(String command, OutputStream os)
            throws IOException, InterruptedException {
        return runCommand(command, os, null);
    }

    /** WARNING: does not handle paths with spaces — prefer the List overloads. */
    public int runCommand(String command, OutputStream os, List<String> stdinLines)
            throws IOException, InterruptedException {
        command = SFTPClient.normalizePath(command);
        return runCommandAsIs(command, os, stdinLines);
    }

    private int runCommandAsIs(String command, OutputStream os, List<String> stdinLines)
            throws IOException, InterruptedException {
        logger.fine("Running command on " + host + ": " + command);

        SshClient client = buildClient();
        try {
            ClientSession session = openSession(client);
            try {
                int result = exec(session, command, os, listInputStream(stdinLines));
                logger.fine("Command completed with exit status: " + result);
                return result;
            } finally {
                try { session.close(); } catch (IOException ignored) { }
            }
        } catch (GeneralSecurityException ex) {
            throw new IOException("Security error during SSH connection", ex);
        } catch (IOException ex) {
            throw ex;
        } finally {
            try { client.close(); } catch (IOException ignored) { }
        }
    }

    public int runCommandAsIs(List<String> command, OutputStream os,
                              List<String> stdinLines, String[] env)
            throws IOException, InterruptedException {
        return runCommandAsIs(commandListToQuotedString(command), os, stdinLines, env);
    }

    private int runCommandAsIs(String command, OutputStream os,
                               List<String> stdinLines, String[] env)
            throws IOException, InterruptedException {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Running command " + command + " on host: " + host);
        }

        SshClient client = buildClient();
        try {
            ClientSession session = openSession(client);
            try {
                StringBuilder fullCommand = new StringBuilder();
                if (env != null && env.length > 0) {
                    ByteArrayOutputStream shellOut = new ByteArrayOutputStream();
                    exec(session, "ps -p $$ | tail -1 | awk '{print $NF}'", shellOut, null);
                    String shellName = shellOut.toString().trim();
                    boolean isCsh = shellName.contains("csh");
                    String prefix = isCsh ? "setenv" : "export";
                    logger.fine((isCsh ? "CSH" : "BASH") + " shell detected");
                    for (String envVar : env) {
                        fullCommand.append(prefix).append(" ").append(envVar).append(";");
                    }
                }
                fullCommand.append(command);
                return exec(session, fullCommand.toString(), os, listInputStream(stdinLines));
            } finally {
                try { session.close(); } catch (IOException ignored) { }
            }
        } catch (GeneralSecurityException ex) {
            throw new IOException("Security error during SSH connection", ex);
        } finally {
            try { client.close(); } catch (IOException ignored) { }
        }
    }

    int exec(ClientSession session, String command, OutputStream os, InputStream is)
            throws IOException, InterruptedException {
        logger.finer("Executing: " + command);
        try (ChannelExec channel = session.createExecChannel(command)) {
            channel.setOut(os);
            channel.setErr(os);

            // Use setIn() BEFORE open() so MINA SSHD reads the InputStream in a background
            // thread and — critically — sends SSH_MSG_CHANNEL_EOF automatically when the
            // stream is exhausted.
            if (is != null) {
                channel.setIn(is);
            }

            channel.open().verify(getTimeout());
            logger.fine("Channel opened, waiting for exit (timeout=" + getExecTimeout() + "ms)");

            // Wait for either CLOSED or EXIT_STATUS.
            // On Windows, nadmin.bat spawns a child JVM that inherits the SSH channel's
            // file descriptors. OpenSSH keeps the channel open until every process holding
            // those handles exits, so CLOSED never fires while the instance runs. Windows
            // OpenSSH does send exit-status before that, so EXIT_STATUS lets us return as
            // soon as the command completes without waiting for the channel to drain.
            Set<ClientChannelEvent> events =
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED, ClientChannelEvent.EXIT_STATUS), getExecTimeout());

            logger.finer("waitFor returned events=" + events);

            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                logger.warning("Command timed out: " + command);
            }

            Integer exitStatus = channel.getExitStatus();
            logger.finer("Exit status: " + exitStatus);
            return exitStatus != null ? exitStatus : -1;
        }
    }

    private InputStream listInputStream(List<String> stdinLines) throws IOException {
        return SSHCommandUtils.listInputStream(stdinLines);
    }

    /** Package-private factory hook so tests can intercept SFTPClient construction. */
    SFTPClient newSFTPClient(SshClient client, ClientSession session) throws IOException {
        return new SFTPClient(client, session);
    }

    /**
     * Opens an SFTP connection and returns a client. The caller must call
     * {@link SFTPClient#close()} when finished.
     */
    public SFTPClient getSFTPClient() throws IOException {
        SshClient client = buildClient();
        ClientSession session = null;
        try {
            session = openSession(client);
            return newSFTPClient(client, session);
        } catch (IOException ex) {
            if (session != null) { try { session.close(); } catch (IOException ignored) { } }
            try { client.close(); } catch (IOException ignored) { }
            throw ex;
        } catch (GeneralSecurityException ex) {
            if (session != null) { try { session.close(); } catch (IOException ignored) { } }
            try { client.close(); } catch (IOException ignored) { }
            throw new IOException("Security error opening SFTP connection", ex);
        }
    }

    /**
     * Opens an authenticated connection to the configured host and returns it.
     * The caller must close the returned {@link SSHConnection} when done — use try-with-resources.
     *
     * @throws SSHAuthenticationException if all configured auth methods are rejected
     * @throws IOException on any other connection failure
     */
    public SSHConnection openConnection() throws IOException {
        SshClient client = buildClient();
        try {
            ClientSession session = openSession(client);
            return new SSHConnection(client, session, this);
        } catch (IOException ex) {
            try { client.close(); } catch (IOException ignored) { }
            throw ex;
        } catch (GeneralSecurityException ex) {
            try { client.close(); } catch (IOException ignored) { }
            throw new IOException("Security error opening SSH connection", ex);
        }
    }

    public void pingConnection() throws IOException, InterruptedException {
        logger.fine("Pinging connection for host: " + host);
        SshClient client = buildClient();
        try (ClientSession session = openSession(client)) {
            // connection verified
        } catch (GeneralSecurityException ex) {
            throw new IOException("Security error during ping", ex);
        } finally {
            try { client.close(); } catch (IOException ignored) { }
        }
    }

    public void validate(String host, int port, String userName, String password,
                         String keyFile, String keyPassPhrase,
                         String installDir, String landmarkPath, Logger logger) throws IOException {
        boolean validInstallDir = false;
        init(userName, host, port, password, keyFile, keyPassPhrase, logger);

        logger.fine("Connection settings valid");

        if (StringUtils.ok(installDir)) {
            try (SFTPClient sftpClient = getSFTPClient()) {
                String testPath = installDir;
                if (sftpClient.exists(testPath)) {
                    if (StringUtils.ok(landmarkPath)) {
                        testPath = installDir + "/" + landmarkPath;
                    }
                    validInstallDir = sftpClient.exists(testPath);
                }

                if (!validInstallDir) {
                    String msg = "Invalid install directory: could not find " + testPath + " on " + host;
                    throw new FileNotFoundException(msg);
                }
            }
            logger.fine("Node home validated");
        }
    }

    public void validate(String host, int port, String userName, String password,
                         String keyFile, String keyPassPhrase,
                         String installDir, Logger logger) throws IOException {
        validate(host, port, userName, password, keyFile, keyPassPhrase, installDir, null, logger);
    }

    /**
     * Returns {@code true} if public-key authentication with the configured key
     * succeeds.
     */
    public boolean checkConnection() {
        SshClient client = null;
        try {
            client = buildClient();
            try (ClientSession ignored = openSession(client)) {
                logger.info("Successfully connected to " + userName + "@" + host
                        + " using keyfile " + keyFile);
            }
            return true;
        } catch (IOException | GeneralSecurityException ex) {
            Throwable t = ex.getCause() != null ? ex.getCause() : ex;
            logger.warning("Failed to connect or authenticate: " + t.getMessage());
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Failed to connect or authenticate", ex);
            }
            return false;
        } finally {
            if (client != null) {
                try { client.close(); } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Returns {@code true} if password authentication succeeds.
     */
    public boolean checkPasswordAuth() {
        // Temporarily clear key-based auth to force password-only attempt
        String savedKeyFile = this.keyFile;
        char[] savedPrivateKey = this.privateKey;
        this.keyFile = null;
        this.privateKey = null;

        SshClient client = null;
        try {
            client = buildClient();
            try (ClientSession session = openSession(client)) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Password auth successful for " + userName + "@" + host);
                }
            }
            return true;
        } catch (IOException | GeneralSecurityException ex) {
            logger.log(Level.FINER, "Password auth failed for " + userName + "@" + host, ex);
            return false;
        } finally {
            this.keyFile = savedKeyFile;
            this.privateKey = savedPrivateKey;
            if (client != null) {
                try { client.close(); } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Distributes the local public key to the remote host's authorized_keys file
     * using password authentication, then fixes remote permissions via SFTP.
     */
    public void setupKey(String node, String pubKeyFile, boolean generateKey, String passwd)
            throws IOException, InterruptedException {

        File key = new File(keyFile);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Key = " + keyFile);
        }

        if (key.exists()) {
            if (checkConnection()) {
                throw new IOException("SSH public key authentication is already configured for "
                        + userName + "@" + node);
            }
        } else {
            if (generateKey) {
                if (!generateKeyPair()) {
                    throw new IOException(
                            "SSH key pair generation failed. Please generate key manually.");
                }
            } else {
                throw new IOException(
                        "SSH key pair not present. Please generate a key pair manually or "
                        + "specify an existing one and re-run the command.");
            }
        }

        if (passwd == null) {
            throw new IOException(
                    "SSH password is required for distributing the public key. "
                    + "You can specify the SSH password in a password file and pass it "
                    + "through --passwordfile option.");
        }

        String savedKeyFile = this.keyFile;
        char[] savedPrivateKey = this.privateKey;
        String savedPassword = this.password;
        this.keyFile = null;
        this.privateKey = null;
        this.password = passwd;

        SshClient client = null;
        ClientSession session = null;
        try {
            client = buildClient();
            session = openSession(client);
        } catch (IOException | GeneralSecurityException ex) {
            if (client != null) {
                try { client.close(); } catch (IOException ignored) { }
            }
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new IOException("Security error setting up key", ex);
        } finally {
            this.keyFile = savedKeyFile;
            this.privateKey = savedPrivateKey;
            this.password = savedPassword;
        }

        SFTPClient sftp;
        try {
            sftp = new SFTPClient(client, session);
        } catch (IOException ex) {
            try { session.close(); } catch (IOException ignored) { }
            try { client.close(); } catch (IOException ignored) { }
            throw ex;
        }
        try (sftp) {
            if (pubKeyFile == null) {
                pubKeyFile = keyFile + ".pub";
            }
            File pubKey = new File(pubKeyFile);
            if (!pubKey.exists()) {
                throw new IOException("Public key file " + pubKeyFile + " does not exist.");
            }

            setupSSHDir();

            if (!sftp.exists(SSH_DIR)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(SSH_DIR + " does not exist on remote host");
                }
                sftp.mkdirs(".ssh", 0700);
            }

            try (OutputStream out = sftp.writeToFile(".ssh/key.tmp");
                 FileInputStream fis = new FileInputStream(pubKey)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = fis.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                }
            }

            ByteArrayOutputStream execOut = new ByteArrayOutputStream();
            int mergeStatus = exec(session, "cd .ssh; cat key.tmp >> " + AUTH_KEY_FILE, execOut, null);
            if (mergeStatus != 0) {
                throw new IOException("Failed to propagate public key " + pubKeyFile + " to " + host
                        + " (exit=" + mergeStatus + "): " + execOut);
            }
            logger.info("Copied keyfile " + pubKeyFile + " to " + userName + "@" + host);

            try {
                sftp.rm(".ssh/key.tmp");
            } catch (IOException ex) {
                logger.warning("Failed to remove .ssh/key.tmp on remote host " + host);
            }

            // Fix permissions — authorized_keys must be 0600 so OpenSSH StrictModes accepts it
            logger.info("Fixing file permissions for home(755), .ssh(700) and authorized_keys(600)");
            sftp.chmod(".", 0755);
            sftp.chmod(SSH_DIR, 0700);
            sftp.chmod(SSH_DIR + AUTH_KEY_FILE, 0600);
        }
    }

    public String expandPasswordAlias(String alias) {
        if (alias == null) {
            return null;
        }
        try {
            return RelativePathResolver.getRealPasswordFromAlias(alias);
        } catch (Exception e) {
            logger.warning(StringUtils.cat(": ", alias, e.getMessage()));
            return null;
        }
    }

    public boolean isPasswordAlias(String alias) {
        return RelativePathResolver.getAlias(alias) != null;
    }

    private String getPrintablePassword(String p) {
        if (p == null) {
            return "null";
        }
        return isPasswordAlias(p) ? p : "<concealed>";
    }

    private boolean generateKeyPair() throws IOException {
        String keygenCmd = findSSHKeygen();
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Using " + keygenCmd + " to generate key pair");
        }

        if (!setupSSHDir()) {
            throw new IOException("Failed to set proper permissions on .ssh directory");
        }

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(keygenCmd);
        cmdLine.add("-t");
        cmdLine.add("rsa");
        cmdLine.add("-N");

        if (rawKeyPassPhrase != null && rawKeyPassPhrase.length() > 0) {
            cmdLine.add(rawKeyPassPhrase);
        } else {
            cmdLine.add(OS.isWindows() ? "\"\"" : "");
        }

        cmdLine.add("-f");
        cmdLine.add(keyFile);

        ProcessManager pm = new ProcessManager(cmdLine);
        pm.setTimeoutMsec(getTimeout());
        pm.setEcho(logger.isLoggable(Level.FINER));

        int exit;
        try {
            exit = pm.execute();
        } catch (ProcessManagerException ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Error while executing ssh-keygen: " + ex.getMessage());
            }
            exit = 1;
        }

        if (exit == 0) {
            logger.info(keygenCmd + " successfully generated " + keyFile);
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(pm.getStderr());
            }
            logger.info(keygenCmd + " failed");
        }
        return exit == 0;
    }

    private String findSSHKeygen() {
        List<String> paths = new ArrayList<>(Arrays.asList("/usr/bin/", "/usr/local/bin/"));

        if (OS.isWindows()) {
            paths.add("C:/cygwin/bin/");
            String mks = System.getenv("ROOTDIR");
            if (mks != null) {
                paths.add(mks + "/bin/");
            }
        }

        File exe = ProcessUtils.getExe(SSH_KEYGEN);
        if (exe != null) {
            return exe.getPath();
        }

        for (String s : paths) {
            File f = new File(s + SSH_KEYGEN);
            if (f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return SSH_KEYGEN;
    }

    private boolean setupSSHDir() throws IOException {
        boolean permissionsSet = true;
        File home = new File(System.getProperty("user.home"));
        File f = new File(home, SSH_DIR);

        if (!FileUtils.safeIsDirectory(f)) {
            if (!f.mkdirs()) {
                throw new IOException("Failed to create " + f.getPath());
            }
            logger.info("Created directory " + f);
        }

        if (!f.setReadable(false, false) || !f.setReadable(true)) {
            permissionsSet = false;
        }
        if (!f.setWritable(false, false) || !f.setWritable(true)) {
            permissionsSet = false;
        }
        if (!f.setExecutable(false, false) || !f.setExecutable(true)) {
            permissionsSet = false;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Fixed the .ssh directory permissions to 0700");
        }
        return permissionsSet;
    }

    protected int getTimeout() {
        return getTimeoutProperty(TIMEOUT_PROPERTY, DEFAULT_TIMEOUT_MSEC);
    }

    /**
     * Timeout for waiting on a running remote command to finish.
     * Distinct from {@link #getTimeout()} which governs connection setup.
     * Long-running commands such as {@code start-local-instance --timeout 300}
     * keep the SSH exec channel open for the duration of their own timeout, so
     * the exec channel timeout must be larger than that.
     */
    protected int getExecTimeout() {
        return getTimeoutProperty(EXEC_TIMEOUT_PROPERTY, DEFAULT_EXEC_TIMEOUT_MSEC);
    }

    private int getTimeoutProperty(String property, int defaultValue) {
        int timeout = defaultValue;
        try {
            String val = System.getProperty(property);
            if (StringUtils.ok(val)) {
                timeout = Integer.parseInt(val);
                if (timeout < MINIMUM_TIMEOUT_MSEC) {
                    logger.log(Level.WARNING,
                            "Value of {0} is out of range, defaulting to {1}ms: {2}",
                            new Object[]{property, defaultValue, timeout});
                    timeout = defaultValue;
                }
            }
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,
                    "Value of {0} is not a valid integer, defaulting to {1}ms: {2}",
                    new Object[]{property, defaultValue, ex});
        }
        return timeout;
    }

    private static String commandListToQuotedString(List<String> command) {
        return SSHCommandUtils.commandListToQuotedString(command);
    }

    @Override
    public String toString() {
        String knownHostsPath = "null";
        if (knownHosts != null) {
            try {
                knownHostsPath = knownHosts.getCanonicalPath();
            } catch (IOException e) {
                knownHostsPath = knownHosts.getAbsolutePath();
            }
        }
        return String.format(
                "host=%s port=%d user=%s password=%s keyFile=%s keyPassPhrase=%s knownHostFile=%s",
                host, port, userName, getPrintablePassword(rawPassword), keyFile,
                getPrintablePassword(rawKeyPassPhrase), knownHostsPath);
    }
}
