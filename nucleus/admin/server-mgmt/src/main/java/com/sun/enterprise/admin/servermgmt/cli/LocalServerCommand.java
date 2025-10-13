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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2016-2025] [Payara Foundation and/or affiliates]

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.domain.DomainConstants;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.Jps;
import com.sun.enterprise.universal.process.ProcessState;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.io.ServerDirs;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.MASTERPASSWORD_FILE;
import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.MASTERPASSWORD_LOCATION_FILE;

/**
 * A class that's supposed to capture all the behavior common to operation on a
 * "local" server. It's getting fairly complicated thus the "section headers"
 * comments. This class plays two roles,
 * <UL>
 * <LI>a place for putting common code - which are final methods. A parent class
 * that is communicating with its own unknown sub-classes. These are non-final
 * methods
 *
 * @author Byron Nevins
 */
public abstract class LocalServerCommand extends CLICommand {

    ////////////////////////////////////////////////////////////////
    /// Section: private variables
    ////////////////////////////////////////////////////////////////
    private ServerDirs serverDirs;
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(LocalDomainCommand.class);
    private final static int IS_RUNNING_DEFAULT_TIMEOUT = 2000;

    ////////////////////////////////////////////////////////////////
    /// Section: protected variables
    ////////////////////////////////////////////////////////////////
    protected static final String DEFAULT_MASTER_PASSWORD = "changeit";

    ////////////////////////////////////////////////////////////////
    /// Section: protected methods that are OK to override
    ////////////////////////////////////////////////////////////////

    /**
     * Override this method and return false to turn-off the file validation. E.g.
     * it demands that config/domain.xml be present. In special cases like
     * Synchronization -- this is how you turn off the testing.
     *
     * @return true - do the checks, false - don't do the checks
     */
    protected boolean checkForSpecialFiles() {
        return true;
    }

    ////////////////////////////////////////////////////////////////
    /// Section: protected methods that are notOK to override.
    ////////////////////////////////////////////////////////////////

    /**
     * Returns the admin address of the local domain. Note that this method should
     * be called only when you own the domain that is available on an accessible
     * file system.
     *
     * @return HostAndPort object with admin server address
     * @throws CommandException in case of parsing errors
     */
    protected final HostAndPort getAdminAddress() throws CommandException {
        // default: DAS which always has the name "server"
        return getAdminAddress("server");
    }

    /**
     * Returns the admin address of a particular server. Note that this method
     * should be called only when you own the server that is available on an
     * accessible file system.
     *
     * @param serverName the server name
     * @return HostAndPort object with admin server address
     * @throws CommandException in case of parsing errors
     */
    protected final HostAndPort getAdminAddress(String serverName) throws CommandException {

        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), serverName);
            List<HostAndPort> addrSet = parser.getAdminAddresses();

            if (!addrSet.isEmpty()) {
                return addrSet.get(0);
            } else {
                throw new CommandException(STRINGS.get("NoAdminPort"));
            }
        } catch (MiniXmlParserException ex) {
            throw new CommandException(STRINGS.get("NoAdminPortEx", ex), ex);
        }
    }

    protected final void setServerDirs(ServerDirs sd) {
        serverDirs = sd;
    }

    protected final boolean isLocal() {
        return serverDirs != null && serverDirs.getServerName() != null;
    }

    protected final boolean isRemote() {
        return !isLocal();
    }

    private void resetLocalPassword() throws IOException {
        resetServerDirs();
        setLocalPassword();
    }

    protected final void setLocalPassword() {
        String pw = serverDirs == null ? null : serverDirs.getLocalPassword();

        if (ok(pw)) {
            programOpts.setPassword(pw != null ? pw.toCharArray() : null,
                    ProgramOptions.PasswordLocation.LOCAL_PASSWORD);
            logger.finer("Using local password");
        } else
            logger.finer("Not using local password");
    }

    protected final void unsetLocalPassword() {
        programOpts.setPassword(null, ProgramOptions.PasswordLocation.LOCAL_PASSWORD);
    }

    protected final void resetServerDirs() throws IOException {
        if (serverDirs == null)
            throw new RuntimeException(Strings.get("NoServerDirs"));

        serverDirs = serverDirs.refresh();
    }

    protected final ServerDirs getServerDirs() {
        return serverDirs;
    }

    protected final File getDomainXml() {
        if (serverDirs == null)
            throw new RuntimeException(Strings.get("NoServerDirs"));

        return serverDirs.getDomainXml();
    }

    /**
     * Checks if the create-domain was created using --savemasterpassword flag which
     * obtains security by obfuscation! Returns null in case of failure of any kind.
     *
     * @return String representing the password from the JCEKS store named
     * master-password in domain folder
     */
    protected final String readFromMasterPasswordFile() {
        File mpf = getMasterPasswordFile();
        if (mpf == null)
            return null; // no master password saved
        try {
            PasswordAdapter pw = new PasswordAdapter(mpf.getAbsolutePath(), MASTERPASSWORD_FILE.toCharArray()); // fixed
            // key
            return pw.getPasswordForAlias(MASTERPASSWORD_FILE);
        } catch (Exception e) {
            logger.log(Level.FINER, "master password file reading error: {0}", e.getMessage());
            return null;
        }
    }

    protected final boolean verifyMasterPassword(String mpv) {
        // issue : 14971, should ideally use javax.net.ssl.keyStore and
        // javax.net.ssl.keyStoreType system props here but they are
        // unavailable to asadmin start-domain hence falling back to
        // cacerts.p12 instead of keystore.p12. Since the truststore
        // is less-likely to be Non-JKS

        return loadAndVerifyKeystore(getKeyStoreFile(), mpv);
    }

    protected boolean loadAndVerifyKeystore(File jks, String mpv) {
        try {
            // try to load the keystore with the provided keystore password
            KeyStore.getInstance(jks, mpv.toCharArray());
            return true;
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINER))
                logger.finer(e.getMessage());
            return false;
        }
    }

    /**
     * Get the master password, either from a password file or by asking the user.
     *
     * @return the actual master password
     */
    protected final String getMasterPassword() throws CommandException {
        // Sets the password into the launcher info.
        // Yes, returning master password as a string is not right ...
        final int RETRIES = 3;
        long t0 = now();
        String mpv = passwords.get(CLIConstants.MASTER_PASSWORD);
        if (mpv == null) { // not specified in the password file
            mpv = DEFAULT_MASTER_PASSWORD; // optimization for the default case -- see 9592
            if (!verifyMasterPassword(mpv)) {
                mpv = readFromMasterPasswordFile();
                if (!verifyMasterPassword(mpv)) {
                    mpv = retry(RETRIES);
                }
            }
        } else { // the passwordfile contains AS_ADMIN_MASTERPASSWORD, use it
            if (!verifyMasterPassword(mpv))
                mpv = retry(RETRIES);
        }
        long t1 = now();
        logger.log(Level.FINER, "Time spent in master password extraction: {0} msec", (t1 - t0));
        return mpv;
    }

    /**
     * See if the server is alive and is the one at the specified directory.
     *
     * @param ourDir       the directory to check if the server is alive agains
     * @param directoryKey the key for the directory
     * @return true if it's the DAS at this domain directory
     */
    protected final boolean isThisServer(File ourDir, String directoryKey) {
        if (!ok(directoryKey))
            throw new NullPointerException();

        ourDir = getUniquePath(ourDir);
        logger.log(Level.FINER, "Check if server is at location {0}", ourDir);

        try {
            programOpts.setHostAndPort(getAdminAddress());
            RemoteCLICommand cmd = new RemoteCLICommand("__locations", programOpts, env);
            ActionReport report = cmd.executeAndReturnActionReport("__locations");
            String theirDirPath = report.findProperty(directoryKey);
            logger.log(Level.FINER, "Remote server has root directory {0}", theirDirPath);

            if (ok(theirDirPath)) {
                File theirDir = getUniquePath(new File(theirDirPath));
                return theirDir.equals(ourDir);
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    protected final int getServerPid() {
        try {
            return Integer.parseInt(new RemoteCLICommand("__locations", programOpts, env)
                    .executeAndReturnActionReport("__locations").findProperty("Pid"));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * There is sometimes a need for subclasses to know if a
     * <code> local domain </code> is running.An example of such a command is
     * change-master-password command.The stop-domain command also needs to know if
     * a domain is running <i> without </i> having to provide user name and password
     * on command line (this is the case when I own a domain that has non-default
     * admin user and password) and want to stop it without providing it.
     * <p>
     * In such cases, we need to know if the domain is running and this method
     * provides a way to do that.
     *
     * @param host the host to check
     * @param port the port to check agains
     * @return boolean indicating whether the server is running
     */
    protected final boolean isRunning(String host, int port) {

        try (Socket server = new Socket()) {
            if (host == null) {
                host = InetAddress.getByName(null).getHostName();
            }

            server.connect(new InetSocketAddress(host, port), IS_RUNNING_DEFAULT_TIMEOUT);
            return true;
        } catch (Exception ex) {
            logger.log(Level.FINER, "\nisRunning got exception: {0}", ex);
            return false;
        }
    }

    /**
     * Is the server still running? This is only called when we're hanging around
     * waiting for the server to die. Byron Nevins, Nov 7, 2010 - Check to see if
     * the process itself is still running We use OS tools to figure this out. See
     * ProcessUtils for details. Failover to the JPS check if necessary
     * <p>
     * bnevins, May 2013
     * http://serverfault.com/questions/181015/how-do-you-free-up-a-port-being-held-open-by-dead-process
     * In WIndows the admin port may be held open for a while -- if there happens to
     * be an attached running child process. This is the key message from the url:
     * <p>
     * If your program spawned any processes while it was running, try killing them.
     * That should cause its process record to be freed and the TCP port to be
     * cleaned up. Apparently windows does this when the record is released not when
     * the process exits as I would have expected.
     *
     * @return
     */
    protected boolean isRunning() {
        int pp = getPrevPid();

        if (pp < 0) {
            return isRunningByCheckingForPidFile();
        }

        ProcessState b = ProcessUtils.getProcessRunningState(pp);

        if (b == ProcessState.ERROR) { // this means it couldn't find out!
            return isRunningUsingJps();
        } else {
            return b == ProcessState.RUNNING;
        }
    }

    protected final void waitForRestart(final int oldServerPid) throws CommandException {
        waitForRestart(oldServerPid, CLIConstants.WAIT_FOR_DAS_TIME_MS);
    }

    /**
     * Byron Nevins Says: We have quite a historical assortment of ways to determine
     * if a server has restarted. There are little teeny timing issues with all of
     * them. I'm confident that this new technique will clear them all up. Here we
     * are just monitoring the PID of the new server and comparing it to the pid of
     * the old server. The oldServerPid is guaranteed to be either the PID of the
     * "old" server or -1 if we couldn't get it -- or it isn't running. If it is -1
     * then we make the assumption that once we DO get a valid pid that the server
     * has started. If the old pid is valid we simply poll until we get a different
     * pid. Notice that we will never get a valid pid back unless the server is
     * officially up and running and "STARTED" Created April 2013
     *
     * @param oldServerPid The pid of the server which is being restarted.
     * @throws CommandException if we time out.
     */
    protected final void waitForRestart(final int oldServerPid, long timeout) throws CommandException {
        long end = getEndTime(timeout);

        while (now() < end) {
            try {
                if (isLocal())
                    resetLocalPassword();

                int newServerPid = getServerPid();

                if (newServerPid > 0 && newServerPid != oldServerPid) {
                    logger.log(Level.FINER, "oldserver-pid, newserver-pid = {0} --- {1}",
                            new Object[]{oldServerPid, newServerPid});
                    return;
                }
                Thread.sleep(CLIConstants.RESTART_CHECK_INTERVAL_MSEC);
            } catch (Exception e) {
                // continue
            }
        }
        // if we get here -- we timed out
        throw new CommandException(STRINGS.get("restartDomain.noGFStart"));
    }

    // todo move prevpid to ServerDirs ???
    protected final int getPrevPid() {
        try {
            File prevPidFile = new File(getServerDirs().getPidFile().getPath() + ".prev");

            if (!prevPidFile.canRead())
                return -1;

            String pids = FileUtils.readSmallFile(prevPidFile).trim();
            return Integer.parseInt(pids);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Is the server still running? This is only called when we're hanging around
     * waiting for the server to die. Byron Nevins, Nov 7, 2010 - Check to see if
     * the process itself is still running We use jps to check If there are any
     * problems fall back to the previous implementation of isRunning() which looks
     * for the pidfile to get deleted
     */
    private boolean isRunningUsingJps() {
        int pp = getPrevPid();

        if (pp < 0)
            return isRunningByCheckingForPidFile();

        return Jps.isPid(pp);
    }

    /**
     * Is the server still running? This is only called when we're hanging around
     * waiting for the server to die.
     */
    private boolean isRunningByCheckingForPidFile() {
        File pf = getServerDirs().getPidFile();

        if (pf != null) {
            return pf.exists();
        } else
            return isRunning(programOpts.getHost(), // remote case
                    programOpts.getPort());
    }

    /**
     * Get uptime from the server.
     *
     * @return uptime in milliseconds
     * @throws CommandException if the server is not running
     */
    protected final long getUptime() throws CommandException {
        RemoteCLICommand cmd = new RemoteCLICommand("uptime", programOpts, env);
        String up = cmd.executeAndReturnOutput("uptime", "--milliseconds").trim();
        long uptimeMillis = parseUptime(up);

        if (uptimeMillis <= 0) {
            throw new CommandException(STRINGS.get("restart.dasNotRunning"));
        }

        logger.log(Level.FINER, "server uptime: {0}", uptimeMillis);
        return uptimeMillis;
    }

    /**
     * See if the server is restartable As of March 2011 -- this only returns false
     * if a passwordfile argument was given when the server started -- but it is no
     * longer available - i.e.the user deleted it or made it unreadable.
     *
     * @return true if the server is restartable
     * @throws CommandException
     */
    protected final boolean isRestartable() throws CommandException {
        // false negative is worse than false positive.
        // there is one and only one case where we return false
        RemoteCLICommand cmd = new RemoteCLICommand("_get-runtime-info", programOpts, env);
        ActionReport report = cmd.executeAndReturnActionReport("_get-runtime-info");

        if (report != null) {
            String val = report.findProperty("restartable_value");

            return !ok(val) || !val.equals("false");
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////
    /// Section: private methods
    ////////////////////////////////////////////////////////////////

    /**
     * The remote uptime command returns a string like: Uptime: 10 minutes, 53
     * seconds, Total milliseconds: 653859\n We find that last number and extract
     * it. XXX - this is pretty gross, and fragile
     */
    private long parseUptime(String up) {
        try {
            return Long.parseLong(up);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Load KeyStore. By default, it is cacerts.p12. If not found, search for cacerts.jks.
     *
     * @return File of keystore (p12 or jks) in config directory, null if none is found
     */
    protected File getKeyStoreFile() {
        if (serverDirs == null) {
            return null;
        }

        File configDir = serverDirs.getConfigDir();
        File mp = Stream.of(new File(configDir, DomainConstants.TRUSTSTORE_FILE),
                        new File(configDir, DomainConstants.TRUSTSTORE_JKS_FILE))
                .filter(f -> f.canRead())
                .findFirst()
                .orElse(null);
        return mp;
    }

    protected File getMasterPasswordFile() {
        if (serverDirs == null) {
            return null;
        }

        File mp;
        File mpLocation = new File(serverDirs.getConfigDir(), MASTERPASSWORD_LOCATION_FILE);
        if (mpLocation.canRead()) {
            try {
                String path = Files.readString(mpLocation.toPath(), StandardCharsets.UTF_8);
                mp = new File(path);
            }
            catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.WARNING,
                    "Failed to read master-password-location file due error: " + e);
                mp = new File(serverDirs.getConfigDir(), MASTERPASSWORD_FILE);
            }
        } else {
            mp = new File(serverDirs.getConfigDir(), MASTERPASSWORD_FILE);
        }

        if (!mp.canRead())
            return null;

        return mp;
    }

    private String retry(int times) throws CommandException {
        String mpv;
        // prompt times times
        for (int i = 0; i < times; i++) {
            // XXX - I18N
            String prompt = STRINGS.get("mp.prompt", (times - i));
            char[] mpvArr = super.readPassword(prompt);
            mpv = mpvArr != null ? new String(mpvArr) : null;
            if (mpv == null)
                throw new CommandException(STRINGS.get("no.console"));
            // ignore retries :)
            if (verifyMasterPassword(mpv))
                return mpv;
            if (i < (times - 1))
                logger.info(STRINGS.get("retry.mp"));
            // make them pay for typos?
            // Thread.currentThread().sleep((i+1)*10000);
        }
        throw new CommandException(STRINGS.get("mp.giveup", times));
    }

    private File getUniquePath(File f) {
        try {
            f = f.getCanonicalFile();
        } catch (IOException ioex) {
            f = SmartFile.sanitize(f);
        }
        return f;
    }

    private long now() {
        // it's just *so* ugly to call this directly!
        return System.currentTimeMillis();
    }

    private long getEndTime(long timeout) {
        return timeout + now();
    }

    protected boolean dataGridEncryptionEnabled() throws IOException, XMLStreamException {
        // We can't access config beans from this invocation due to it being CLI vs.
        // ASAdmin command - it's not
        // executing against a running server. This means we need to read directly from
        // the domain.xml.
        XMLEventReader xmlReader = XMLInputFactory.newInstance()
                .createXMLEventReader(new FileInputStream(getDomainXml()));
        while (xmlReader.hasNext()) {
            XMLEvent event = xmlReader.nextEvent();

            if (event.isStartElement()
                    && event.asStartElement().getName().getLocalPart().equals("hazelcast-runtime-configuration")) {
                Attribute attribute = event.asStartElement()
                        .getAttributeByName(new QName("datagrid-encryption-enabled"));
                if (attribute == null) {
                    return false;
                }
                return Boolean.parseBoolean(attribute.getValue());
            }
        }

        logger.warning("Could not determine if data grid encryption is enabled - "
                + "you will need to regenerate the encryption key if it is");
        return false;
    }

    /**
     * Gets the GlassFish installation root (using property
     * com.sun.aas.installRoot), first from asenv.conf. If that's not available,
     * then from java.lang.System.
     *
     * @return path of GlassFish install root
     * @throws CommandException if the GlassFish install root is not found
     */
    protected String getInstallRootPath() throws CommandException {
        String installRootPath = getSystemProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);

        if (!StringUtils.ok(installRootPath)) {
            installRootPath = System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        }

        if (!StringUtils.ok(installRootPath)) {
            throw new CommandException("noInstallDirPath");
        }
        return installRootPath;
    }

    protected void checkAdditionalTrustAndKeyStores() throws IOException, XMLStreamException {
        HashMap<String, String> additionalTrustandKeyStores = new HashMap<>();
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(getDomainXml());
            NodeList jvmOptionsNodes = document.getElementsByTagName("jvm-options");

            for (int i = 0; i < jvmOptionsNodes.getLength(); i++) {
                String jvmOption = jvmOptionsNodes.item(i).getTextContent();
                if (jvmOption.startsWith("-Dfish.payara.ssl.additionalKeyStores")) {
                    String additionalKeyStores = jvmOption.split("=")[1];
                    additionalTrustandKeyStores.compute("additionalKeyStores", (key, value) -> value == null ? additionalKeyStores : value.concat(", " + additionalKeyStores));
                    continue;
                }
                if (jvmOption.startsWith("-Dfish.payara.ssl.additionalTrustStores")) {
                    String additionalTrustStores = jvmOption.split("=")[1];
                    additionalTrustandKeyStores.compute("additionalTrustStores", (key, value) -> value == null ? additionalTrustStores : value.concat(", " + additionalTrustStores));
                    continue;
                }
            }
            if (additionalTrustandKeyStores.containsKey("additionalKeyStores")) {
                logger.log(Level.INFO,
                        "The passwords of additional KeyStores {0} have not been changed - please update these manually to continue using them.",
                        Arrays.toString(additionalTrustandKeyStores.get("additionalKeyStores").split(File.pathSeparator)));
            }
            if (additionalTrustandKeyStores.containsKey("additionalTrustStores")) {
                logger.log(Level.INFO,
                        "The passwords of additional TrustStores {0} have not been changed - please update these manually to continue using them.",
                        Arrays.toString(additionalTrustandKeyStores.get("additionalTrustStores").split(File.pathSeparator)));
            }
        } catch (ParserConfigurationException | SAXException exception) {
            logger.warning(
                    "Could not determine if there were additional Key Stores or Trust stores, if the master-password has been updated, the password for the additional stores need updating in order to continue using them.");
        }
    }
}