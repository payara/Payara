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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;
import java.io.*;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.io.FileUtils;

import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.universal.process.ProcessUtils;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.SCPClient;
import org.glassfish.internal.api.RelativePathResolver;
import org.glassfish.cluster.ssh.util.HostVerifier;
import org.glassfish.cluster.ssh.util.SSHUtil;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.ExceptionUtil;
import org.glassfish.cluster.ssh.sftp.SFTPClient;

import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Mordani
 */


@Service(name="SSHLauncher")
@PerLookup
public class SSHLauncher {

    private static final String SSH_DIR = ".ssh" + File.separator;
    private static final String AUTH_KEY_FILE = "authorized_keys";
    private static final int DEFAULT_TIMEOUT_MSEC = 120000; // 2 minutes
    private static final String SSH_KEYGEN = "ssh-keygen";
    private static final char LINE_SEP = System.getProperty("line.separator").charAt(0);
    
  /**
     * Database of known hosts.
     */
    private static KnownHosts knownHostsDatabase = new KnownHosts();

  /**
     * The host name which to connect to via ssh
     */
    private String host;

  /**
     * The port on which the ssh daemon is running
     */
    private int port;

  /**
     * The user name to use for authenticating with the ssh daemon
     */
    private String userName;

  /**
     * The name of private key file.
     */
    private String keyFile;

    /**
     * The private key to use to authenticate. Usually either keyFile or
     * privateKey will be used (since a keyFile contains a private key)
     */
    private char[] privateKey;

  /**
     * The connection object that represents the connection to the host
     * via ssh
     */
    private Connection connection;

    private String authType;

    private String keyPassPhrase;

    private File knownHosts;

    private Logger logger;

    private String password;

    // Password before it has been expanded. Used for debugging.
    private String rawPassword = null;
    private String rawKeyPassPhrase = null;

    public void init(Logger logger) {
        this.logger = logger;
    }

    /**
     * Initialize the SSHLauncher use a Node config object
     * @param node
     * @param logger 
     */
    public void init(Node node, Logger logger) {
        this.logger = logger;
        int port;
        String host;

        SshConnector connector = node.getSshConnector();

        host = connector.getSshHost();
        if (SSHUtil.checkString(host) != null) {
            this.host = host;
        } else {
            this.host = node.getNodeHost();
        }
        if (logger.isLoggable(Level.FINE)) {
	    logger.fine("Connecting to host " + host); 
        }

        //XXX Why do we need this again?  This is already done above and set to host
        String sshHost = connector.getSshHost();
        if (sshHost != null)
            this.host = sshHost;
        
        SshAuth sshAuth = connector.getSshAuth();
        String userName = null;
        if (sshAuth != null) {
            userName = sshAuth.getUserName();
            this.keyFile = sshAuth.getKeyfile();
            this.rawPassword = sshAuth.getPassword();
            this.rawKeyPassPhrase = sshAuth.getKeyPassphrase();
        }
        try {
            port = Integer.parseInt(connector.getSshPort());
        } catch(NumberFormatException nfe) {
            port = 22;
        }

        init(userName, this.host, port, this.rawPassword, keyFile,
                this.rawKeyPassPhrase, logger);

    }

    /**
     * Initialize the SSHLauncher using a private key
     * 
     * @param userName
     * @param host
     * @param port
     * @param password
     * @param privateKey
     * @param logger 
     */
    public void init(String userName, String host, int port, String password, char[] privateKey, Logger logger) {
        init(userName, host, port, password, null, null, privateKey, logger);
    }

    /**
     * Initialize the SSHLauncher using a private key file
     * 
     * @param userName
     * @param host
     * @param port
     * @param password
     * @param keyFile
     * @param keyPassPhrase
     * @param logger 
     */
    public void init(String userName, String host, int port, String password, String keyFile, String keyPassPhrase, Logger logger) {
        init(userName, host, port, password, keyFile, keyPassPhrase, null, logger);
    }

    private void init(String userName, String host, int port, String password, String keyFile, String keyPassPhrase, char[] privateKey,  Logger logger) {


        this.port = port == 0 ? 22 : port;

        this.host = host;
        this.privateKey = (privateKey != null) ? Arrays.copyOf(privateKey, privateKey.length) : null;
        this.keyFile = (keyFile == null && privateKey == null) ?
                                    SSHUtil.getExistingKeyFile(): keyFile;
        this.logger = logger;

        this.userName = SSHUtil.checkString(userName) == null ?
                    System.getProperty("user.name") : userName;

        
        this.rawPassword = password;
        this.password = expandPasswordAlias(password);
        this.rawKeyPassPhrase = keyPassPhrase;
        this.keyPassPhrase = expandPasswordAlias(keyPassPhrase);

        if (knownHosts == null) {
            File home = new File(System.getProperty("user.home"));
            knownHosts = new File(home,".ssh/known_hosts");
        }
        if (knownHosts.exists()) {
            try {
                knownHostsDatabase.addHostkeys(knownHosts);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("SSH info is " + toString());
        }
    }


  /**
     * Opens the connection to the host and authenticates with public
     * key.
     * 
     */
    private void openConnection() throws IOException {

    boolean isAuthenticated = false;
    String message= "";
    connection = new Connection(host, port);

        connection.connect(new HostVerifier(knownHostsDatabase));
        if(SSHUtil.checkString(keyFile) == null && SSHUtil.checkString(password) == null &&
                privateKey == null) {
            message += "No key or password specified - trying default keys \n";
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("keyfile and password are null. Will try to authenticate with default key file if available");
            }
            // check the default key locations if no authentication
            // method is explicitly configured.
            File home = new File(System.getProperty("user.home"));
            for (String keyName : Arrays.asList("id_rsa","id_dsa",
                                                "identity"))
            {
                message += "Tried to authenticate using " + keyName + "\n";
                File key = new File(home,".ssh/"+keyName);
                if (key.exists()) {
                    isAuthenticated =
                        connection.authenticateWithPublicKey(userName,
                                                             key, null);
                }
                if (isAuthenticated) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Authentication successful using key " + keyName);
                    }

                    message = null;

                    break;
                }

            }
        }

        if (!isAuthenticated && SSHUtil.checkString(password) != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Authenticating with password " + getPrintablePassword(password));
            }
            try {
              isAuthenticated = connection.authenticateWithPassword(userName, password);
            } catch (IOException iex) {
                message = "SSH authentication with password failed: " +
                                ExceptionUtil.getRootCause(iex).getMessage();
                logger.log(Level.WARNING,message,iex);
            }
      }

        if (!isAuthenticated && privateKey != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Authenticating with privateKey");
            }
            try {
                  isAuthenticated = connection.authenticateWithPublicKey(
                                                      userName, privateKey, keyPassPhrase);
            } catch (IOException iex) {
                message = "SSH authentication with private key failed: " +
                                ExceptionUtil.getRootCause(iex).getMessage();
                logger.log(Level.WARNING,message,iex);
            }
        }
      
        if (!isAuthenticated && SSHUtil.checkString(keyFile) != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Specified key file is " + keyFile);
            }
            File key = new File(keyFile);
            if (key.exists()) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Specified key file exists at " + key);
                }
                try {
                  isAuthenticated = connection.authenticateWithPublicKey(
                                                      userName, key, keyPassPhrase);
                } catch (IOException iex){
                    message = "SSH authentication with key file " + key +
                                    " failed: " +
                                ExceptionUtil.getRootCause(iex).getMessage();
                    logger.log(Level.WARNING,message,iex);
                }

            }
        }


        if (!isAuthenticated && !connection.isAuthenticationComplete()) {
            connection.close();
            connection = null;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Could not authenticate");
            }
            throw new IOException("Could not authenticate. " + message);
        }
        message = null;
        SSHUtil.register(connection);

    }

    /**
     * Executes a command on the remote system via ssh, optionally sending
     * lines of data to the remote process's System.in.
     *
     * @param command the command to execute in the form of an argv style list
     * @param os stream to receive the output from the command
     * @param stdinLines optional data to be sent to the process's System.in
     *        stream; null if no input should be sent
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public int runCommand(List<String> command, OutputStream os,
            List<String> stdinLines) throws IOException,
                                            InterruptedException
    {
        return runCommand(commandListToQuotedString(command), os, stdinLines);
    }

    public int runCommand(List<String> command, OutputStream os)
                                            throws IOException,
                                            InterruptedException
    {
        return runCommand(command, os, null);
    }

    /**
     * WARNING! This method does not handle paths with spaces in them.
     * To use this method you must make sure all paths in the command string
     * are quoted correctly.  Otherwise use the methods that take command as
     * a list instead.
     */
    public int runCommand(String command, OutputStream os) throws IOException,
                                            InterruptedException
    {
        return runCommand(command, os, null);
    }

    /**
     * Executes a command on the remote system via ssh, optionally sending
     * lines of data to the remote process's System.in.
     *
     * WARNING! This method does not handle paths with spaces in them.
     * To use this method you must make sure all paths in the command string
     * are quoted correctly.  Otherwise use the methods that take command as
     * a list instead.
     *
     * @param command the command to execute
     * @param os stream to receive the output from the command
     * @param stdinLines optional data to be sent to the process's System.in stream; null if no input should be sent
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public int runCommand(String command, OutputStream os,
            List<String> stdinLines) throws IOException,
                                            InterruptedException
    {
        command = SFTPClient.normalizePath(command);
        return runCommandAsIs(command, os, stdinLines);
    }
    
    /**
     * Executes a command on the remote system via ssh without normalizing 
     * the command line
     * 
     * @param command the command to execute
     * @param os stream to receive the output from the command
     * @param stdinLines optional data to be sent to the process's System.in 
     *        stream; null if no input should be sent
     * @return
     * @throws IOException
     * @throws InterruptedException
     **/
    public int runCommandAsIs(List<String> command, OutputStream os,
            List<String> stdinLines) throws IOException,
                                            InterruptedException
    {
        return runCommandAsIs(commandListToQuotedString(command), os, stdinLines);
    }
    
    private int runCommandAsIs(String command, OutputStream os,
            List<String> stdinLines) throws IOException,
                                            InterruptedException
    {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Running command " + command + " on host: " + this.host);
        }

        openConnection();
        final Session sess = connection.openSession();

        int status = exec(sess, command, os, listInputStream(stdinLines));

        // XXX: Should we close connection after each command or cache it
        // and re-use it?
        SSHUtil.unregister(connection);
        connection = null;
        return status;
    }

    /**
     * Executes a command on the remote system via ssh without normalizing
     * the command line
     *
     * @param command    the command to execute
     * @param os         stream to receive the output from the command
     * @param stdinLines optional data to be sent to the process's System.in
     *                   stream; null if no input should be sent
     * @param env        list of environment variables to set before executing the command. each array cell is like varname=varvalue. This only supports on csh, t-csh and bash
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public int runCommandAsIs(List<String> command, OutputStream os,
                              List<String> stdinLines, String[] env) throws IOException,
            InterruptedException {
        return runCommandAsIs(commandListToQuotedString(command), os, stdinLines, env);
    }

    private int runCommandAsIs(String command, OutputStream os,
                               List<String> stdinLines, String[] env) throws IOException,
            InterruptedException {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Running command " + command + " on host: " + this.host);
        }

        openConnection();
        StringBuffer buff = new StringBuffer();
        if (env != null) {
            Session tempSession = connection.openSession();
            OutputStream ous = new ByteArrayOutputStream();
            exec(tempSession, "ps -p $$ | tail -1 | awk '{print $NF}'", ous, null);
            String prefix;
            if (ous.toString().contains("csh")) {
                logger.fine("CSH shell");
                prefix = "setenv";
            } else {
                logger.fine("BASH shell");
                prefix = "export";
            }
            for (String st : env) {
                String cmd = prefix + " " + st;
                buff.append(cmd).append(";");
            }
        }
        buff.append(command);
        final Session sess = connection.openSession();
        int status = exec(sess, buff.toString(), os, listInputStream(stdinLines));

        // XXX: Should we close connection after each command or cache it
        // and re-use it?
        SSHUtil.unregister(connection);
        connection = null;
        return status;
    }

    private int exec(final Session session, final String command, final OutputStream os,
                     final InputStream is)
            throws IOException, InterruptedException {
        try {
            session.execCommand(command);
            PumpThread t1 = new PumpThread(session.getStdout(), os);
            t1.start();
            PumpThread t2 = new PumpThread(session.getStderr(), os);
            t2.start();
            final OutputStream stdin = session.getStdin();
            if (is != null) {
                final PumpThread inputPump = new PumpThread(is,
                        stdin);
                inputPump.run();
            }
            stdin.close();
            t1.join();
            t2.join();

            // wait for some time since the delivery of the exit status often gets delayed
            session.waitForCondition(ChannelCondition.EXIT_STATUS,3000);
            Integer r = session.getExitStatus();
            if(r!=null) return r.intValue();
            return -1;
        } finally {
            session.close();
        }
    }

    private InputStream listInputStream(final List<String> stdinLines) throws IOException {
        if (stdinLines == null) {
            return null;
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String line : stdinLines) {
            baos.write(line.getBytes());
            baos.write(LINE_SEP);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Pumps {@link InputStream} to {@link OutputStream}.
     *
     * @author Kohsuke Kawaguchi
     */
    private static final class PumpThread extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public PumpThread(InputStream in, OutputStream out) {
            super("pump thread");
            this.in = in;
            this.out = out;
        }

        public void run() {
            byte[] buf = new byte[1024];
            try {
                while(true) {
                    int len = in.read(buf);
                    if(len<0) {
                        in.close();
                        return;
                    }
                    out.write(buf,0,len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void pingConnection() throws IOException, InterruptedException
    {
        logger.fine("Pinging connection for host: " + this.host);
        openConnection();
        SSHUtil.unregister(connection);
        connection = null;
    }

    /* validate user provided ars
     *          check connecton to host
     *          check that the install dir is correct
     *          landmarkPath must be relative to the installdir
     */

    public void validate(String host, int port,
                             String userName, String password,
                             String keyFile, String keyPassPhrase,
                             String installDir, String landmarkPath,
                             Logger logger) throws IOException
    {
        boolean validInstallDir = false;
        init(userName, host,  port, password, keyFile, keyPassPhrase, logger);

        openConnection();
        logger.fine("Connection settings valid");
        String testPath = installDir;
        if (StringUtils.ok(testPath)) {
            // Validate if installDir exists
            SFTPClient sftpClient = new SFTPClient(connection);
            if (sftpClient.exists(testPath)) {
                // installDir exists. Now check for landmark if provided
                if (StringUtils.ok(landmarkPath)) {                    
                    testPath = installDir + "/" + landmarkPath;
                }
                validInstallDir = sftpClient.exists(testPath);
            } else {
                validInstallDir = false;
            }
            SSHUtil.unregister(connection);
            connection = null;

            if (!validInstallDir) {
                String msg = "Invalid install directory: could not find " +
                        testPath + " on " + host;
                throw new FileNotFoundException(msg);
            }
            logger.fine("Node home validated");
        }
    }

    public void validate(String host, int port,
                             String userName, String password,
                             String keyFile, String keyPassPhrase,
                             String installDir, Logger logger) throws IOException
    {
        // Validate with no landmark file
        validate(host, port, userName, password, keyFile, keyPassPhrase,
                             installDir, null, logger);
    }

    public SFTPClient getSFTPClient() throws IOException {
        openConnection();
        SFTPClient sftpClient = new SFTPClient(connection);
        return sftpClient;
    }

    public SCPClient getSCPClient() throws IOException {
        openConnection();
        return new SCPClient(connection);
    }

    public String expandPasswordAlias(String alias) {

        String expandedPassword = null;

        if (alias == null) {
            return null;
        }

        try {
            expandedPassword = RelativePathResolver.getRealPasswordFromAlias(alias);
        } catch (Exception e) {
            logger.warning(StringUtils.cat(": ", alias, e.getMessage()));
            return null;
        }

        return expandedPassword;
    }

    public boolean isPasswordAlias(String alias) {
        // Check if the passed string is specified using the alias syntax
        String aliasName = RelativePathResolver.getAlias(alias);
        return (aliasName != null);
    }

    /**
     * Return a version of the password that is printable.
     * @param p  password string
     * @return   printable version of password
     */
    private String getPrintablePassword(String p) {
        // We only display the password if it is an alias, else
        // we display "<concealed>".
        String printable = "null";
        if (p != null) {
            if (isPasswordAlias(p)) {
                printable = p;
            } else {
                printable = "<concealed>";
            }
        }
        return printable;
    }

    /**
     * Setting up the key involves the following steps:
     * -If a key exists and we can connect using the key, do nothing.
     * -Generate a key pair if there isn't one
     * -Connect to remote host using password auth and do the following:
     *  1. create .ssh directory if it doesn't exist
     *  2. copy over the key as key.tmp
     *  3. Append the key to authorized_keys file
     *  4. Remove the temporary key file key.tmp
     *  5. Fix permissions for home, .ssh and authorized_keys
     * @param node        - remote host
     * @param pubKeyFile  - .pub file
     * @param generateKey - flag to indicate if key needs to be generated or not
     * @param passwd      - ssh user password
     * @throws IOException
     * @throws InterruptedException
     */
    public void setupKey(String node, String pubKeyFile, boolean generateKey, String passwd)
             throws IOException, InterruptedException {
        boolean connected = false;

        File key = new File(keyFile);
        if(logger.isLoggable(Level.FINER))
            logger.finer("Key = " + keyFile);
        if (key.exists()) {
            if (checkConnection()) {
                throw new IOException("SSH public key authentication is already configured for " + userName + "@" + node);
            }            
        } else {
            if (generateKey) {
                if(!generateKeyPair()) {
                    throw new IOException("SSH key pair generation failed. Please generate key manually.");
                }
            } else {                
                throw new IOException("SSH key pair not present. Please generate a key pair manually or specify an existing one and re-run the command.");
            }
        }

        //password is must for key distribution
        if (passwd == null) {
            throw new IOException("SSH password is required for distributing the public key. You can specify the SSH password in a password file and pass it through --passwordfile option.");
        }
        connection = new Connection(node, port);
        connection.connect();
        connected = connection.authenticateWithPassword(userName, passwd);

        if(!connected) {
            throw new IOException("SSH password authentication failed for user " + userName + " on host " + node);
        }
        
        //We open up a second connection for scp and exec. For some reason, a hang
        //is seen in MKS if we try to do everything using the same connection.
        Connection conn = new Connection(node, port);
        conn.connect();
        boolean ret = conn.authenticateWithPassword(userName, passwd);
        
        if (!ret) {
            throw new IOException("SSH password authentication failed for user " + userName + " on host " + node);
        }
        //initiate scp client
        SCPClient scp = new SCPClient(conn);
        SFTPClient sftp = new SFTPClient(connection);

        if (key.exists()) {

            //fixes .ssh file mode
            setupSSHDir();

            if (pubKeyFile == null) {
                pubKeyFile = keyFile + ".pub";
            }

            File pubKey = new File(pubKeyFile);
            if(!pubKey.exists()) {
                throw new IOException("Public key file " + pubKeyFile + " does not exist.");
            }

            try {
                if(!sftp.exists(SSH_DIR)) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine(SSH_DIR + " does not exist");
                    }
                    sftp.mkdirs(".ssh", 0700);
                }
            } catch (Exception e) {
                if(logger.isLoggable(Level.FINER)) {
                    e.printStackTrace();
                }
                throw new IOException("Error while creating .ssh directory on remote host:" + e.getMessage());
            }

            //copy over the public key to remote host
            scp.put(pubKey.getAbsolutePath(), "key.tmp", ".ssh", "0600");            

            //append the public key file contents to authorized_keys file on remote host
            String mergeCommand = "cd .ssh; cat key.tmp >> " + AUTH_KEY_FILE;
            if(logger.isLoggable(Level.FINER)) {
                logger.finer("mergeCommand = " + mergeCommand);
            }
            if(conn.exec(mergeCommand, new ByteArrayOutputStream())!=0) {
                throw new IOException("Failed to propogate the public key " + pubKeyFile + " to " + host);
            }
            logger.info("Copied keyfile " + pubKeyFile + " to " + userName + "@" + host);

            //remove the public key file on remote host
            if(conn.exec("rm .ssh/key.tmp", new ByteArrayOutputStream())!=0) {
                logger.warning("WARNING: Failed to remove the public key file key.tmp on remote host " + host);
            }
            if(logger.isLoggable(Level.FINER)) {
                logger.finer("Removed the temporary key file on remote host");
            }
            
            //Lets fix all the permissions
            //On MKS, chmod doesn't work as expected. StrictMode needs to be disabled
            //for connection to go through
            logger.info("Fixing file permissions for home(755), .ssh(700) and authorized_keys file(644)");
            sftp.chmod(".", 0755);
            sftp.chmod(SSH_DIR, 0700);
            sftp.chmod(SSH_DIR + AUTH_KEY_FILE, 0644);
            //release the connections
            sftp.close();
            conn.close();
        }
    }

    public static byte[] toByteArray( InputStream input )
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = input.read(buf)) >= 0) {
           output.write(buf, 0, len);
        }
        byte[] o = output.toByteArray();
        output.close();
        return o;
    }

    /**
     * Check if we can authenticate using public key auth
     * @return true|false
     */
    public boolean checkConnection() {
        boolean status = false;
        Connection c = null;
        c = new Connection(host, port);
        try {
            c.connect();
            File f = new File(keyFile);
            if(logger.isLoggable(Level.FINER)) {
                logger.finer("Checking connection...");
            }
            status = c.authenticateWithPublicKey(userName, f, rawKeyPassPhrase);
            if (status) {
                logger.info("Successfully connected to " + userName + "@" + host + " using keyfile " + keyFile);
            }
        } catch(IOException ioe) {
            Throwable t = ioe.getCause();
            if (t != null) {
                String msg = t.getMessage();
                logger.warning("Failed to connect or authenticate: " + msg);
            }
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Failed to connect or autheticate: ", ioe);
            }
        } finally {
            c.close();
        }
        return status;
    }

    /**
     * Check if we can connect using password auth
     * @return true|false
     */
    public boolean checkPasswordAuth() {
        boolean status = false;
        Connection c = null;
        try {
            c = new Connection(host, port);
            c.connect();
            if(logger.isLoggable(Level.FINER)) {
                logger.finer("Checking connection...");
            }
            status = c.authenticateWithPassword(userName, password);
            if (status) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Successfully connected to " + userName + "@" + host + " using password authentication");
                }
            }
        } catch(IOException ioe) {
            //logger.printExceptionStackTrace(ioe);
            if (logger.isLoggable(Level.FINER)) {
                ioe.printStackTrace();
            }
        } finally {
            if ( c!= null) {
                c.close();
            }
        }
        return status;
    }

    /**
      * Invoke ssh-keygen using ProcessManager API
      */
    private boolean generateKeyPair() throws IOException {
        String keygenCmd = findSSHKeygen();
        if(logger.isLoggable(Level.FINER)) {
            logger.finer("Using " + keygenCmd + " to generate key pair");
        }

        if (!setupSSHDir()) {
            throw new IOException("Failed to set proper permissions on .ssh directory");
        }

        StringBuffer k = new StringBuffer();
        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(keygenCmd);
        k.append(keygenCmd);
        cmdLine.add("-t");
        k.append(" ").append("-t");
        cmdLine.add("rsa");
        k.append(" ").append("rsa");
        cmdLine.add("-N");
        k.append(" ").append("-N");

        if (rawKeyPassPhrase != null && rawKeyPassPhrase.length() > 0) {
            cmdLine.add(rawKeyPassPhrase);
            k.append(" ").append(getPrintablePassword(rawKeyPassPhrase));
        } else {
            //special handling for empty passphrase on Windows
            if(OS.isWindows()) {
                cmdLine.add("\"\"");
                k.append(" ").append("\"\"");
            } else {
                cmdLine.add("");
                k.append(" ").append("");
            }
        }
        cmdLine.add("-f");
        k.append(" ").append("-f");
        cmdLine.add(keyFile);
        k.append(" ").append(keyFile);
        //cmdLine.add("-vvv");

        ProcessManager pm = new ProcessManager(cmdLine);

        if(logger.isLoggable(Level.FINER)) {
            logger.finer("Command = " + k);
        }
        pm.setTimeoutMsec(DEFAULT_TIMEOUT_MSEC);

        if (logger.isLoggable(Level.FINER))
            pm.setEcho(true);
        else
            pm.setEcho(false);
        int exit;

        try {
            exit = pm.execute();            
        }
        catch (ProcessManagerException ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Error while executing ssh-keygen: " + ex.getMessage());
            }
            exit = 1;
        }
        if (exit == 0){
            logger.info(keygenCmd + " successfully generated the identification " + keyFile);
        } else {
            if(logger.isLoggable(Level.FINER)) {
                logger.finer(pm.getStderr());
            }
            logger.info(keygenCmd + " failed");
        }

        return (exit == 0) ? true : false;
    }

    /**
     * Method to locate ssh-keygen. If found in path, return the same or else look
     * for it in a pre defined list of search paths.
     * @return ssh-keygen command
     */
    private String findSSHKeygen() {
        List<String> paths = new ArrayList<String>(Arrays.asList(
                    "/usr/bin/",
                    "/usr/local/bin/"));

        if (OS.isWindows()) {
            paths.add("C:/cygwin/bin/");
            //Windows MKS Toolkit install path
            String mks = System.getenv("ROOTDIR");
            if (mks != null) {
                paths.add(mks + "/bin/");
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Paths = " + paths);
        }
        
        File exe = ProcessUtils.getExe(SSH_KEYGEN);
        if( exe != null){
            return exe.getPath();
        }

        for (String s :paths) {
            File f = new File(s + SSH_KEYGEN);
            if (f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return SSH_KEYGEN;
    }

    /**
      * Create .ssh directory and set the permissions correctly
      */
    private boolean setupSSHDir() throws IOException {
        boolean ret = true;
        File home = new File(System.getProperty("user.home"));
        File f = new File(home, SSH_DIR);

        if(!FileUtils.safeIsDirectory(f)) {
            if (!f.mkdirs()) {
                throw new IOException("Failed to create " + f.getPath());
            }
            logger.info("Created directory " + f.toString());
        }
        
        if (!f.setReadable(false, false) || !f.setReadable(true)) {
            ret = false;
        }
        
        if (!f.setWritable(false,false) || !f.setWritable(true)) {
            ret = false;
        }

        if (!f.setExecutable(false, false) || !f.setExecutable(true)) {
            ret = false;
        }

        if(logger.isLoggable(Level.FINER)) {
            logger.finer("Fixed the .ssh directory permissions to 0700");
        }
        return ret;
    }
    
    @Override
    public String toString() {

        String knownHostsPath  = "null";
        if (knownHosts != null) {
            try {
                knownHostsPath = knownHosts.getCanonicalPath();
            } catch (IOException e) {
                knownHostsPath = knownHosts.getAbsolutePath();
            }
        }

        String displayPassword = getPrintablePassword(rawPassword);
        String displayKeyPassPhrase = getPrintablePassword(rawKeyPassPhrase);

        return String.format("host=%s port=%d user=%s password=%s keyFile=%s keyPassPhrase=%s authType=%s knownHostFile=%s",
            host, port, userName, displayPassword, keyFile,
            displayKeyPassPhrase, authType, knownHostsPath);
    }

    /**
     * Take a command in the form of a list and convert it to a command string.
     * If any string in the list has spaces then the string is quoted before
     * being added to the final command string.
     *
     * @param command
     * @return
     */
    private static String commandListToQuotedString(List<String> command) {
        if(command.size()==1) return command.get(0);
        StringBuilder commandBuilder  = new StringBuilder();
        boolean first = true;

        for (String s : command) {
            if (!first) {
                commandBuilder.append(" ");
            } else {
                first = false;
            }
            if (s.contains(" ")) {
                // Quote parts of the command that contain a space
                commandBuilder.append(FileUtils.quoteString(s));
            } else {
                commandBuilder.append(s);
            }
        }
        return commandBuilder.toString();
    }
}
