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
// Portions Copyright [2017-2019] Payara Foundation and/or Affiliates

package com.sun.enterprise.admin.servermgmt.cli;

import static com.sun.enterprise.admin.cli.CLIConstants.WAIT_FOR_DAS_TIME_MS;
import static com.sun.enterprise.util.StringUtils.ok;
import static com.sun.enterprise.util.net.NetUtils.isRunning;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.FINER;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.glassfish.api.admin.CommandException;

import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.CLIUtil;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.launcher.GFLauncher;
import com.sun.enterprise.admin.launcher.GFLauncherException;
import com.sun.enterprise.admin.launcher.GFLauncherInfo;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.io.ServerDirs;
import com.sun.enterprise.util.net.NetUtils;

/**
 * Java does not allow multiple inheritance.  Both StartDomainCommand and
 * StartInstanceCommand have common code but they are already in a different
 * hierarchy of classes.  The first common baseclass is too far away -- e.g.
 * no "launcher" variable, etc.
 *
 * Instead -- put common code in here and call it as common utilities
 * This class is designed to be thread-safe and IMMUTABLE
 * @author bnevins
 */
public class StartServerHelper {

    private final boolean terse;
    private final GFLauncher launcher;
    private final Logger logger;
    private final File pidFile;
    private final GFLauncherInfo info;
    private final List<HostAndPort> addresses;
    private final ServerDirs serverDirs;
    private final String masterPassword;
    private final String serverOrDomainName;
    private final boolean debug;
    private final int debugPort;
    private final boolean isDebugSuspend;
    // only set when actively trouble-shooting or investigating...
    private static final  boolean DEBUG_MESSAGES_ON = false;
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(StartServerHelper.class);

    public StartServerHelper(Logger logger0, boolean terse0,
            ServerDirs serverDirs0, GFLauncher launcher0,
            String masterPassword0, boolean debug0) {
        logger = logger0;
        terse = terse0;
        launcher = launcher0;
        info = launcher.getInfo();

        if (info.isDomain()) {
            serverOrDomainName = info.getDomainName();
        } else {
            serverOrDomainName = info.getInstanceName();
        }

        addresses = info.getAdminAddresses();
        serverDirs = serverDirs0;
        pidFile = serverDirs.getPidFile();
        masterPassword = masterPassword0;
        debug = debug0;
        // it will be < 0 if both --debug is false and debug-enabled=false in jvm-config
        debugPort = launcher.getDebugPort();
        isDebugSuspend = launcher.isDebugSuspend();

        if (isDebugSuspend && debugPort >= 0) {
            logger.info(STRINGS.get("ServerStart.DebuggerSuspendedMessage", "" + debugPort));
        }
    }
    
    public void waitForServer() throws CommandException {
        waitForServer(CLIConstants.WAIT_FOR_DAS_TIME_MS, MILLISECONDS); // 10 minutes
    }

    public void waitForServer(long timeout, TimeUnit unit) throws CommandException {
        long startWait = System.currentTimeMillis();
        if (!terse) {
            // use stdout because logger always appends a newline
            System.out.print(STRINGS.get("WaitServer", serverOrDomainName) + " ");
        }

        boolean alive = false;
        int count = 0;

        pinged:
        while (!timedOut(startWait, unit.toMillis(timeout))) {
            if (pidFile != null) {
                logger.log(FINER, "Check for pid file: {0}", pidFile);
                if (pidFile.exists()) {
                    alive = true;
                    break pinged;
                }
            } else {
                // First, see if the admin port is responding.
                // If it is, the DAS is up
                for (HostAndPort address : addresses) {
                    if (isRunning(address.getHost(), address.getPort())) {
                        alive = true;
                        break pinged;
                    }
                }
            }

            // Check to make sure the DAS process is still running.
            // If it isn't, startup failed.
            try {
                int exitCode = launcher.getProcess().exitValue();
                
                // If we reach this location, there's an exit code, so uh oh, DAS died
                // If the DAS is still alive an exception is thrown here
                String sname;

                if (info.isDomain()) {
                    sname = "domain " + info.getDomainName();
                } else {
                    sname = "instance " + info.getInstanceName();
                }

                String output = launcher.getProcessStreamDrainer().getOutErrString();
                if (ok(output)) {
                    throw new CommandException(STRINGS.get("serverDiedOutput", sname, exitCode, output));
                } else {
                    throw new CommandException(STRINGS.get("serverDied", sname, exitCode));
                }
            } catch (GFLauncherException ex) {
                // should never happen
            } catch (IllegalThreadStateException ex) {
                // process is still alive
            }

            // Wait before checking again
            try {
                Thread.sleep(100);
                if (!terse && count++ % 10 == 0) {
                    System.out.print(".");
                }
            } catch (InterruptedException ex) {
                // don't care
            }
        }

        if (!terse) {
            System.out.println();
        }

        if (!alive) {
            String msg;
            String time = "" + unit.toSeconds(timeout);
            if (info.isDomain()) {
                msg = STRINGS.get("serverNoStart", STRINGS.get("DAS"), info.getDomainName(), time);
            } else {
                // e.g. No response from the Instance Server (instance1) after 600 seconds.
                msg = STRINGS.get("serverNoStart", STRINGS.get("INSTANCE"), info.getInstanceName(), time);
            }

            throw new CommandException(msg);
        }
    }

    /**
     * Run a series of commands to prepare for a launch.
     *
     * @return false if there was a problem.
     * @throws CommandException If there was a timeout waiting for the parent to die
     * or admin port to free up
     */
    public boolean prepareForLaunch() throws CommandException {

        waitForParentToDie();
        setSecurity();

        if (!checkPorts()) {
            return false;
        }
        deletePidFile();

        return true;
    }

    public void report() {
        String logfile;

        try {
            logfile = launcher.getLogFilename();
        }
        catch (GFLauncherException ex) {
            logfile = "UNKNOWN";        // should never happen
        }

        int adminPort = -1;
        String adminPortString = "-1";

        try {
            if (addresses != null && !addresses.isEmpty()) {
                adminPort = addresses.get(0).getPort();
            }
            // To avoid having the logger do this: port = 4,848
            // so we do the conversion to a string ourselves
            adminPortString = "" + adminPort;
        }
        catch (Exception e) {
            //ignore
        }

        logger.info(STRINGS.get(
                "ServerStart.SuccessMessage",
                info.isDomain() ? "domain " : "instance",
                serverDirs.getServerName(),
                serverDirs.getServerDir(),
                logfile,
                adminPortString));

        if (debugPort >= 0) {
            logger.info(STRINGS.get("ServerStart.DebuggerMessage", "" + debugPort));
        }
    }

    /**
     * If the parent is a GF server -- then wait for it to die.  This is part
     * of the Client-Server Restart Dance!
     * THe dying server called us with the system property AS_RESTART set to its pid
     * @throws CommandException if we timeout waiting for the parent to die or
     *  if the admin ports never free up
     */
    private void waitForParentToDie() throws CommandException {
        // we also come here with just a regular start in which case there is
        // no parent, and the System Property is NOT set to anything...
        String pids = System.getProperty("AS_RESTART");

        if (!ok(pids)) {
            return;
        }

        int pid = -1;

        try {
            pid = Integer.parseInt(pids);
        } catch (Exception e) {
            pid = -1;
        }
        waitForParentDeath(pid);
    }

    private boolean checkPorts() {
        String err = adminPortInUse();

        if (err != null) {
            logger.warning(err);
            return false;
        }

        return true;
    }

    private void deletePidFile() {
        String msg = serverDirs.deletePidFile();

        if (msg != null && logger.isLoggable(FINER)) {
            logger.finer(msg);
        }
    }

    private void setSecurity() {
        info.addSecurityToken(CLIConstants.MASTER_PASSWORD, masterPassword);
    }

    private String adminPortInUse() {
        return adminPortInUse(info.getAdminAddresses());
    }

    private String adminPortInUse(List<HostAndPort> adminAddresses) {
        // it returns a String for logging --- if desired
        for (HostAndPort addr : adminAddresses) {
            if (!NetUtils.isPortFree(addr.getHost(), addr.getPort())) {
                return STRINGS.get("ServerRunning",
                        Integer.toString(addr.getPort()));
            }
        }

        return null;
    }

    // use the pid we received from the parent server and platform specific tools
    // to see FOR SURE when the entire JVM process is gone.  This solves
    // potential niggling bugs.
    private void waitForParentDeath(int pid) throws CommandException {
        if (pid < 0) {
            // can not happen.  (Famous Last Words!)
            new ParentDeathWaiterPureJava();
            return;
        }

        long start = System.currentTimeMillis();
        try {
            do {
                Boolean b = ProcessUtils.isProcessRunning(pid);
                if (b == null) {
                    // this means we were unable to find out from the OS if the process
                    // is running or not
                    debugMessage("ProcessUtils.isProcessRunning(" + pid + ") "
                            + "returned null which means we can't get process "
                            + "info on this platform.");

                    new ParentDeathWaiterPureJava();
                    return;
                }
                if (!b) {
                    debugMessage("Parent process (" + pid + ") is dead.");
                    return;
                }
                // else parent is still breathing...
                debugMessage("Wait one more second for parent to die...");
                Thread.sleep(1000);
            } while (!timedOut(start, CLIConstants.DEATH_TIMEOUT_MS));

        } catch (Exception e) {
            // fall through.  Normal returns are in the block above
        }

        // abnormal return path
        throw new CommandException(STRINGS.get("deathwait_timeout", CLIConstants.DEATH_TIMEOUT_MS));
    }

    private static boolean timedOut(long startTime) {
        return timedOut(startTime, WAIT_FOR_DAS_TIME_MS);
    }

    private static boolean timedOut(long startTime, long span) {
        return (System.currentTimeMillis() - startTime) > span;
    }

    private static void debugMessage(String s) {
        // very difficult to see output from this process when part of restart-domain.
        // Normally there is no console.
        // There are **three** JVMs in a restart -- old server, new server, cli
        // we will not even see AS_DEBUG!
        if (DEBUG_MESSAGES_ON) {
            Environment env = new Environment();
            CLIUtil.writeCommandToDebugLog("restart-debug", env, new String[]{"DEBUG MESSAGE FROM RESTART JVM", s}, 99999);
        }
    }

    /**
     * bnevins
     * the restart flag is set by the RestartDomain command in the local
     * server.  The dying server has started a new JVM process and is
     * running this code.  Our official parent process is the dying server.
     * The ParentDeathWaiterPureJava waits for the parent process to disappear.
     * see RestartDomainCommand in core/kernel for more details
     */
    private class ParentDeathWaiterPureJava implements Runnable {

        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            try {
                // When parent process is almost dead, in.read returns -1 (EOF)
                // as the pipe breaks.

                while (System.in.read() >= 0);
            } catch (IOException ex) {
                // ignore
            }

            // The port may take some time to become free after the pipe breaks
            while (adminPortInUse(addresses) != null)
                ;
            success = true;
        }

        private ParentDeathWaiterPureJava() throws CommandException {
            try {
                Thread t = new Thread(this);
                t.start();
                t.join(CLIConstants.DEATH_TIMEOUT_MS);
            } catch (Exception e) {
                // ignore!
            }

            if (!success) {
                throw new CommandException(
                        STRINGS.get("deathwait_timeout", CLIConstants.DEATH_TIMEOUT_MS));
            }
        }
        boolean success = false;
    }

}
