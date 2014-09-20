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
package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.KeyStore;
import org.glassfish.api.ActionReport;

import org.glassfish.api.admin.CommandException;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.Jps;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.io.ServerDirs;
import java.util.logging.Level;

/**
 * A class that's supposed to capture all the behavior common to operation
 * on a "local" server.
 * It's getting fairly complicated thus the "section headers" comments.
 * This class plays two roles, <UL><LI>a place for putting common code - which
 * are final methods.  A parent class that is communicating with its own unknown
 * sub-classes.  These are non-final methods
 *
 * @author Byron Nevins
 */
public abstract class LocalServerCommand extends CLICommand {
    ////////////////////////////////////////////////////////////////
    /// Section:  protected methods that are OK to override
    ////////////////////////////////////////////////////////////////
    /**
     * Override this method and return false to turn-off the file validation.
     * E.g. it demands that config/domain.xml be present.  In special cases like
     * Synchronization -- this is how you turn off the testing.
     * @return true - do the checks, false - don't do the checks
     */
    protected boolean checkForSpecialFiles() {
        return true;
    }

    ////////////////////////////////////////////////////////////////
    /// Section:  protected methods that are notOK to override.
    ////////////////////////////////////////////////////////////////
    /**
     * Returns the admin address of the local domain. Note that this method
     * should be called only when you own the domain that is available on
     * an accessible file system.
     *
     * @return HostAndPort object with admin server address
     * @throws CommandException in case of parsing errors
     */
    protected final HostAndPort getAdminAddress() throws CommandException {
        // default:  DAS which always has the name "server"
        return getAdminAddress("server");
    }

    /**
     * Returns the admin address of a particular server. Note that this method
     * should be called only when you own the server that is available on
     * an accessible file system.
     *
     * @return HostAndPort object with admin server address
     * @throws CommandException in case of parsing errors
     */
    protected final HostAndPort getAdminAddress(String serverName)
            throws CommandException {

        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), serverName);
            List<HostAndPort> addrSet = parser.getAdminAddresses();

            if (addrSet.size() > 0)
                return addrSet.get(0);
            else
                throw new CommandException(strings.get("NoAdminPort"));
        }
        catch (MiniXmlParserException ex) {
            throw new CommandException(strings.get("NoAdminPortEx", ex), ex);
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

    private final void resetLocalPassword() throws IOException {
        resetServerDirs();
        setLocalPassword();
    }


    protected final void setLocalPassword() {
        String pw = serverDirs == null ? null : serverDirs.getLocalPassword();

        if (ok(pw)) {
            programOpts.setPassword(pw,
                    ProgramOptions.PasswordLocation.LOCAL_PASSWORD);
            logger.finer("Using local password");
        }
        else
            logger.finer("Not using local password");
    }

    protected final void unsetLocalPassword() {
        programOpts.setPassword(null,
                ProgramOptions.PasswordLocation.LOCAL_PASSWORD);
    }

    protected final void resetServerDirs() throws IOException {
        if(serverDirs == null)
            throw new RuntimeException(Strings.get("NoServerDirs"));

        serverDirs = serverDirs.refresh();
    }

    protected final ServerDirs getServerDirs() {
        return serverDirs;
    }

    protected final File getDomainXml() {
        if(serverDirs == null)
            throw new RuntimeException(Strings.get("NoServerDirs"));

        return serverDirs.getDomainXml();
    }

    /**
     * Checks if the create-domain was created using --savemasterpassword flag
     * which obtains security by obfuscation! Returns null in case of failure
     * of any kind.
     * @return String representing the password from the JCEKS store named
     *          master-password in domain folder
     */
    protected final String readFromMasterPasswordFile() {
        File mpf = getMasterPasswordFile();
        if (mpf == null)
            return null;   // no master password  saved
        try {
            PasswordAdapter pw = new PasswordAdapter(mpf.getAbsolutePath(),
                    "master-password".toCharArray()); // fixed key
            return pw.getPasswordForAlias("master-password");
        }
        catch (Exception e) {
            logger.log(Level.FINER, "master password file reading error: {0}", e.getMessage());
            return null;
        }
    }

    protected final boolean verifyMasterPassword(String mpv) {
        //issue : 14971, should ideally use javax.net.ssl.keyStore and
        //javax.net.ssl.keyStoreType system props here but they are
        //unavailable to asadmin start-domain hence falling back to
        //cacerts.jks instead of keystore.jks. Since the truststore
        //is less-likely to be Non-JKS

        return loadAndVerifyKeystore(getJKS(),mpv);
    }

    protected boolean loadAndVerifyKeystore(File jks,String mpv) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(jks);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(fis, mpv.toCharArray());
            return true;
        }
        catch (Exception e) {
            if (logger.isLoggable(Level.FINER))
                logger.finer(e.getMessage());
            return false;
        }
        finally {
            try {
                if (fis != null)
                    fis.close();
            }
            catch (IOException ioe) {
                // ignore, I know ...
            }
        }
    }

    /**
     * Get the master password, either from a password file or
     * by asking the user.
     */
    protected final String getMasterPassword() throws CommandException {
        // Sets the password into the launcher info.
        // Yes, returning master password as a string is not right ...
        final int RETRIES = 3;
        long t0 = now();
        String mpv = passwords.get(CLIConstants.MASTER_PASSWORD);
        if (mpv == null) { //not specified in the password file
            mpv = "changeit";  //optimization for the default case -- see 9592
            if (!verifyMasterPassword(mpv)) {
                mpv = readFromMasterPasswordFile();
                if (!verifyMasterPassword(mpv)) {
                    mpv = retry(RETRIES);
                }
            }
        }
        else { // the passwordfile contains AS_ADMIN_MASTERPASSWORD, use it
            if (!verifyMasterPassword(mpv))
                mpv = retry(RETRIES);
        }
        long t1 = now();
        logger.log(Level.FINER, "Time spent in master password extraction: {0} msec", (t1 - t0));       //TODO
        return mpv;
    }

    /**
     * See if the server is alive and is the one at the specified directory.
     *
     * @return true if it's the DAS at this domain directory
     */
    protected final boolean isThisServer(File ourDir, String directoryKey) {
        if (!ok(directoryKey))
            throw new NullPointerException();

        ourDir = getUniquePath(ourDir);
        logger.log(Level.FINER, "Check if server is at location {0}", ourDir);

        try {
            RemoteCLICommand cmd =
                    new RemoteCLICommand("__locations", programOpts, env);
            ActionReport report =
                    cmd.executeAndReturnActionReport(new String[]{"__locations"});
            String theirDirPath = report.findProperty(directoryKey);
            logger.log(Level.FINER, "Remote server has root directory {0}", theirDirPath);

            if (ok(theirDirPath)) {
                File theirDir = getUniquePath(new File(theirDirPath));
                return theirDir.equals(ourDir);
            }
            return false;
        }
        catch (Exception ex) {
            return false;
        }
    }

    protected final int getServerPid() {
        try {
            return Integer.parseInt(
                    new RemoteCLICommand("__locations", programOpts, env)
                    .executeAndReturnActionReport(new String[]{"__locations"})
                    .findProperty("Pid"));
        }
        catch (Exception e) {
            return -1;
        }
    }

    /**
     * There is sometimes a need for subclasses to know if a
     * <code> local domain </code> is running. An example of such a command is
     * change-master-password command. The stop-domain command also needs to
     * know if a domain is running <i> without </i> having to provide user
     * name and password on command line (this is the case when I own a domain
     * that has non-default admin user and password) and want to stop it
     * without providing it.
     * <p>
     * In such cases, we need to know if the domain is running and this method
     * provides a way to do that.
     *
     * @return boolean indicating whether the server is running
     */
    protected final boolean isRunning(String host, int port) {
        Socket server = null;
        try {
            server = new Socket(host, port);
            return true;
        }
        catch (Exception ex) {
            logger.log(Level.FINER, "\nisRunning got exception: {0}", ex);
            return false;
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }

    /**
     * Is the server still running?
     * This is only called when we're hanging around waiting for the server to die.
     * Byron Nevins, Nov 7, 2010 - Check to see if the process itself is still running
     * We use OS tools to figure this out.  See ProcessUtils for details.
     * Failover to the JPS check if necessary
     *
     * bnevins, May 2013
     * http://serverfault.com/questions/181015/how-do-you-free-up-a-port-being-held-open-by-dead-process
     * In WIndows the admin port may be held open for a while -- if there happens to
     * be an attached running child process.  This is the key message from the url:
     *
     * If your program spawned any processes while it was running, try killing them.
     * That should cause its process record to be freed and the TCP port to be
     * cleaned up. Apparently windows does this when the record is released not
     * when the process exits as I would have expected.
     */
    protected boolean isRunning() {
        int pp = getPrevPid();

        if (pp < 0)
            return isRunningByCheckingForPidFile();

        Boolean b = ProcessUtils.isProcessRunning(pp);

        if (b == null) // this means it couldn't find out!
            return isRunningUsingJps();
        else
            return b.booleanValue();
    }

    /**
     * Byron Nevins Says: We have quite a historical assortment of ways to
     * determine if a server has restarted. There are little teeny timing issues
     * with all of them. I'm confident that this new technique will clear them
     * all up. Here we are just monitoring the PID of the new server and
     * comparing it to the pid of the old server. The oldServerPid is guaranteed
     * to be either the PID of the "old" server or -1 if we couldn't get it --
     * or it isn't running. If it is -1 then we make the assumption that once we
     * DO get a valid pid that the server has started. If the old pid is valid
     * we simply poll until we get a different pid. Notice that we will never
     * get a valid pid back unless the server is officially up and running and
     * "STARTED" Created April 2013
     *
     * @param oldServerPid The pid of the server which is being restarted.
     * @throws CommandException if we time out.
     */
    protected final void waitForRestart(final int oldServerPid) throws CommandException {
        long end = getEndTime();

        while (now() < end) {
            try {
                if(isLocal())
                    resetLocalPassword();

                int newServerPid = getServerPid();

                if (newServerPid > 0 && newServerPid != oldServerPid) {
                    logger.log(Level.FINER, "oldserver-pid, newserver-pid = {0} --- {1}",
                            new Object[]{oldServerPid, newServerPid});
                    return;
                }
                Thread.sleep(CLIConstants.RESTART_CHECK_INTERVAL_MSEC);
            }
            catch (Exception e) {
                // continue
            }
        }
        // if we get here -- we timed out
        throw new CommandException(strings.get("restartDomain.noGFStart"));
    }

    // todo move prevpid to ServerDirs ???
    protected final int getPrevPid() {
        try {
            File prevPidFile = new File(getServerDirs().getPidFile().getPath() + ".prev");

            if (!prevPidFile.canRead())
                return -1;

            String pids = FileUtils.readSmallFile(prevPidFile).trim();
            return Integer.parseInt(pids);
        }
        catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Is the server still running?
     * This is only called when we're hanging around waiting for the server to die.
     * Byron Nevins, Nov 7, 2010 - Check to see if the process itself is still running
     * We use jps to check
     * If there are any problems fall back to the previous implementation of
     * isRunning() which looks for the pidfile to get deleted
     */
    private boolean isRunningUsingJps() {
        int pp = getPrevPid();

        if (pp < 0)
            return isRunningByCheckingForPidFile();

        return Jps.isPid(pp);
    }

    /**
     * Is the server still running?
     * This is only called when we're hanging around waiting for the server to die.
     */
    private boolean isRunningByCheckingForPidFile() {
        File pf = getServerDirs().getPidFile();

        if (pf != null) {
            return pf.exists();
        }
        else
            return isRunning(programOpts.getHost(), // remote case
                    programOpts.getPort());
    }

    /**
     * Get uptime from the server.
     */
    protected final long getUptime() throws CommandException {
        RemoteCLICommand cmd = new RemoteCLICommand("uptime", programOpts, env);
        String up = cmd.executeAndReturnOutput("uptime", "--milliseconds").trim();
        long up_ms = parseUptime(up);

        if (up_ms <= 0) {
            throw new CommandException(strings.get("restart.dasNotRunning"));
        }

        logger.log(Level.FINER, "server uptime: {0}", up_ms);
        return up_ms;
    }
    /**
     * See if the server is restartable
     * As of March 2011 -- this only returns false if a passwordfile argument was given
     * when the server started -- but it is no longer available - i.e. the user
     * deleted it or made it unreadable.
     */
    protected final boolean isRestartable() throws CommandException {
        // false negative is worse than false positive.
        // there is one and only one case where we return false
        RemoteCLICommand cmd = new RemoteCLICommand("_get-runtime-info", programOpts, env);
        ActionReport report = cmd.executeAndReturnActionReport("_get-runtime-info");

        if (report != null) {
            String val = report.findProperty("restartable_value");

            if (ok(val) && val.equals("false"))
                return false;
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////
    /// Section:  private methods
    ////////////////////////////////////////////////////////////////
    /**
     * The remote uptime command returns a string like:
     * Uptime: 10 minutes, 53 seconds, Total milliseconds: 653859\n
     * We find that last number and extract it.
     * XXX - this is pretty gross, and fragile
     */
    private long parseUptime(String up) {
        try {
            return Long.parseLong(up);
        }
        catch (Exception e) {
            return 0;
        }
    }

    private File getJKS() {
        if (serverDirs == null)
            return null;

        File mp = new File(new File(serverDirs.getServerDir(), "config"), "cacerts.jks");
        if (!mp.canRead())
            return null;
        return mp;
    }

    protected File getMasterPasswordFile() {

        if (serverDirs == null)
            return null;

        File mp = new File(serverDirs.getServerDir(), "master-password");
        if (!mp.canRead())
            return null;

        return mp;
    }

    private String retry(int times) throws CommandException {
        String mpv;
        // prompt times times
        for (int i = 0; i < times; i++) {
            // XXX - I18N
            String prompt = strings.get("mp.prompt", (times - i));
            mpv = super.readPassword(prompt);
            if (mpv == null)
                throw new CommandException(strings.get("no.console"));
            // ignore retries :)
            if (verifyMasterPassword(mpv))
                return mpv;
            if (i < (times - 1))
                logger.info(strings.get("retry.mp"));
            // make them pay for typos?
            //Thread.currentThread().sleep((i+1)*10000);
        }
        throw new CommandException(strings.get("mp.giveup", times));
    }

    private File getUniquePath(File f) {
        try {
            f = f.getCanonicalFile();
        }
        catch (IOException ioex) {
            f = SmartFile.sanitize(f);
        }
        return f;
    }

    private long now() {
        // it's just *so* ugly to call this directly!
        return System.currentTimeMillis();
    }

    private long getEndTime() {
        // it's a method in case we someday allow configuring this VERY long
        // timeout at runtime.
        return CLIConstants.WAIT_FOR_DAS_TIME_MS + now();
    }
    ////////////////////////////////////////////////////////////////
    /// Section:  private variables
    ////////////////////////////////////////////////////////////////
    private ServerDirs serverDirs;
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(LocalDomainCommand.class);
}
