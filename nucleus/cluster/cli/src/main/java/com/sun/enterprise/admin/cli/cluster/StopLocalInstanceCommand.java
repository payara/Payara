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
package com.sun.enterprise.admin.cli.cluster;

import java.util.logging.Level;
import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.admin.cli.cluster.Strings;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Stop a local server instance.
 * @author Bill Shannon
 * @author Byron Nevins
 *
 *
 */
@Service(name = "stop-local-instance")
@PerLookup
public class StopLocalInstanceCommand extends LocalInstanceCommand {
    @Param(optional = true, defaultValue = "true")
    private Boolean force;
    @Param(name = "instance_name", primary = true, optional = true)
    private String userArgInstanceName;
    @Param(optional = true, defaultValue = "false")
    Boolean kill;

    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        instanceName = userArgInstanceName;
        super.validate();
    }

    @Override
    protected boolean mkdirs(File f) {
        // we definitely do NOT want dirs created for this instance if
        // they don't exist!
        return false;
    }

    /**
     * Big trouble if you allow the super implementation to run
     * because it creates directories.  If this command is called with
     * an instance that doesn't exist -- new dirs will be created which
     * can cause other problems.
     */
    @Override
    protected void initInstance() throws CommandException {
        super.initInstance();
    }

    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {
        // if the local password isn't available, the instance isn't running
        // (localPassword is set by initInstance)
        File serverDir = getServerDirs().getServerDir();

        if (serverDir == null || !serverDir.isDirectory())
            return noSuchInstance();

        if (getServerDirs().getLocalPassword() == null)
            return instanceNotRunning();

        String serverName = getServerDirs().getServerName();
        HostAndPort addr = getAdminAddress(serverName);
        programOpts.setHostAndPort(addr);

        if (logger.isLoggable(Level.FINER))
            logger.finer("Stopping server at " + addr.toString());

        if (!isRunning())
            return instanceNotRunning();

        logger.finer("It's the correct Instance");
        return doRemoteCommand();
    }

    /**
     * Print message and return exit code when
     * we detect that the DAS is not running.
     */
    protected int instanceNotRunning() throws CommandException {
        if (kill)
            return kill();

        // by definition this is not an error
        // https://glassfish.dev.java.net/issues/show_bug.cgi?id=8387

        logger.warning(Strings.get("StopInstance.instanceNotRunning"));
        return 0;
    }

    /**
     * Print message and return exit code when
     * we detect that there is no such instance
     */
    private int noSuchInstance() {
        // by definition this is not an error
        // https://glassfish.dev.java.net/issues/show_bug.cgi?id=8387
        logger.warning(Strings.get("Instance.noSuchInstance"));
        return 0;
    }

    /**
     * Execute the actual stop-domain command.
     */
    protected int doRemoteCommand() throws CommandException {

        // put the local-password for the instance  in programOpts
        // we don't do this for ALL local-instance commands because if they call
        // DAS with the instance's local-password it will cause BIG trouble...
        setLocalPassword();

        /*
         * If we're using the local password, we don't want to prompt
         * for a new password.  If the local password doesn't work it
         * most likely means we're talking to the wrong server.
         */
        programOpts.setInteractive(false);
        Exception remoteException = null;

        try {
            remoteException = runRemoteStop();
            waitForDeath();
        }
        catch (CommandException e) {
            if(remoteException != null)
                logger.warning("Remote Exception: " + e);

            // 1.  We can't access the server at all
            // 2.  We timed-out waiting for it to die
            if(!kill)
                throw e;
        }

        if (kill) {
            // do NOT make this an error -- user specified a kill
            // if kill throws a CE -- then it WILL get tossed back as an error
            kill();
        }
        return 0;
    }

    /**
     * run the remote stop-domain command and throw away the output
     * the calling code has already verified the server is running.
     * Careful changing this code, starting and stopping can be an intricate dance.
     * Note how we are catching *ALL* Exceptions.  This is because the ReST code
     * does NOT catch unchecked exceptions.  The key idea here is that:
     * 1.  we first verify the server is indeed running
     * 2. we tell the server to stop.  This is a special case.  IO streams might be
     *    broken, all sorts of things that are 'bad' for normal commands can happen.
     *    But we don't care!  We only care that the server is now dead:
     * 3. Verify the server is NOT running any longer.
     * When this method is called , (1) is already true.  We do (2) here and ignore
     * all errors.  Then we return to the caller which will do (3)
     *
     * @return Exception in case the server didn't stop we can log the exception...
     */
    private Exception runRemoteStop() {
        // 2 catch blocks just to make things crystal clear.
        try {
            RemoteCLICommand cmd = new RemoteCLICommand("_stop-instance", programOpts, env);
            cmd.executeAndReturnOutput("_stop-instance", "--force", force.toString());
            return null;
        }
        catch (CommandException e) {
            // ReST may have thrown a checked Exception because the server died faster than the
            // server could communicate back!   The ReST client misinterprets it
            // to mean the server is not reachable.  This is a special case.  And it
            // is NOT an error.
            return e;
        }
        catch(Exception e) {
            // Perhaps Jersey or who-knows-what threw a unchecked Exception.
            // We don't care.  See the huge javadoc comment above...
            return e;
        }
    }
    /**
     * Wait for the server to die.
     */
    private void waitForDeath() throws CommandException {
        if (!programOpts.isTerse()) {
            // use stdout because logger always appends a newline
            System.out.print(Strings.get("StopInstance.waitForDeath") + " ");
        }
        long startWait = System.currentTimeMillis();
        boolean alive = true;
        int count = 0;

        while (!timedOut(startWait)) {
            if (!isRunning()) {
                alive = false;
                break;
            }
            try {
                Thread.sleep(100);
                if (!programOpts.isTerse() && count++ % 10 == 0)
                    System.out.print(".");
            }
            catch (InterruptedException ex) {
                // don't care
            }
        }

        if (!programOpts.isTerse())
            System.out.println();

        if (alive) {
            throw new CommandException(Strings.get("StopInstance.instanceNotDead",
                    (CLIConstants.DEATH_TIMEOUT_MS / 1000)));
        }
    }

    private boolean timedOut(long startTime) {
        return (System.currentTimeMillis() - startTime) > CLIConstants.DEATH_TIMEOUT_MS;
    }

    private int kill() throws CommandException {
        File prevPid = null;
        String pids = null;

        try {
            prevPid = new File(getServerDirs().getPidFile().getPath() + ".prev");

            if (!prevPid.canRead())
                throw new CommandException(Strings.get("StopInstance.nopidprev", prevPid));

            pids = FileUtils.readSmallFile(prevPid).trim();
            String s = ProcessUtils.kill(Integer.parseInt(pids));

            if (s != null && logger.isLoggable(Level.FINER))
                logger.finer(s);
        }
        catch (CommandException ce) {
            throw ce;
        }
        catch (Exception ex) {
            throw new CommandException(Strings.get("StopInstance.pidprevreaderror",
                    prevPid, ex.getMessage()));
        }
        return 0;
    }
}
