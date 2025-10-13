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
 *
 * Portions Copyright [2017-2023] [Payara Foundation and/or its affiliates]
 */
package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.remote.DASUtils;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.net.NetUtils;
import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import jakarta.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.*;

/**
 * The stop-domain command.
 *
 * @author Byron Nevins
 * @author Bill Shannon
 */
@Service(name = "stop-domain")
@PerLookup
public class StopDomainCommand extends LocalDomainCommand {

    @Inject
    ServiceLocator serviceLocator;

    @Param(name = "domain_name", primary = true, optional = true)
    private String userArgDomainName;
    @Param(name = "force", optional = true, defaultValue = "true")
    Boolean force;
    @Param(optional = true, defaultValue = "false")
    Boolean kill;

    @Param(optional = true, defaultValue = "60")
    protected int timeout;

    @Override
    protected void validate()
            throws CommandException {
        setDomainName(userArgDomainName);

        if (timeout < 1) {
            throw new CommandValidationException("Timeout must be at least 1 second long.");
        }

        super.validate(); // which calls initDomain() !!
    }

    /**
     * Override initDomain in LocalDomainCommand to only initialize the local domain information (name, directory) in
     * the local case, when no --host has been specified.
     *
     * @throws org.glassfish.api.admin.CommandException
     */
    @Override
    protected void initDomain() throws CommandException {
        // only initialize local domain information if it's a local operation

        if (NetUtils.isThisHostLocal(programOpts.getHost())) {
            super.initDomain();
        } else if (userArgDomainName != null) {  // remote case
            throw new CommandException(Strings.get("StopDomain.noDomainNameAllowed"));
        }
    }

    @Override
    protected int executeCommand() throws CommandException {

        if (isLocal()) {
            // if the local password isn't available, the domain isn't running
            // (localPassword is set by initDomain)
            if (getServerDirs().getLocalPassword() == null) {
                return dasNotRunning();
            }
            programOpts.setHostAndPort(getAdminAddress());
            logger.log(Level.FINER, "Stopping local domain on port {0}", programOpts.getPort());

            /*
             * If we're using the local password, we don't want to prompt
             * for a new password.  If the local password doesn't work it
             * most likely means we're talking to the wrong server.
             */
            programOpts.setInteractive(false);

            // in the local case, make sure we're talking to the correct DAS
            if (!isThisDAS(getDomainRootDir())) {
                return dasNotRunning();
            }

            logger.finer("It's the correct DAS");
        } else { // remote
            // Verify that the DAS is running and reachable
            if (!DASUtils.pingDASQuietly(programOpts, env)) {
                return dasNotRunning();
            }

            logger.finer("DAS is running");
            programOpts.setInteractive(false);
        }

        /*
         * At this point any options will have been prompted for, and
         * the password will have been prompted for by pingDASQuietly,
         * so even if the password is wrong we don't want any more
         * prompting here.
         */
        doCommand();
        return 0;
    }

    /**
     * Print message and return exit code when we detect that the DAS is not running.
     *
     * @return Success in all cases
     * @throws org.glassfish.api.admin.CommandException
     */
    protected int dasNotRunning() throws CommandException {
        if (kill) {
            if (isLocal()) {
                return kill();
            } else {
                // remote.  We can NOT kill and we can't ask it to kill itself.
                throw new CommandException(Strings.get("StopDomain.dasNotRunningRemotely"));
            }
        }

        // by definition this is not an error
        // https://glassfish.dev.java.net/issues/show_bug.cgi?id=8387
        if (isLocal()) {
            ListDomainsCommand listDomains = serviceLocator.getService(ListDomainsCommand.class);
            StringBuilder runningDomains = new StringBuilder();
            try {
                Map<String, Boolean> domains = listDomains.getDomains();
                for (Entry<String, Boolean> entry : domains.entrySet()) {
                    String domain = entry.getKey();
                    if (entry.getValue()) {
                        runningDomains.append("\n").append(domain);
                    }
                }
            } catch(Exception e) {        
            }
            if (runningDomains.length() < 1) {
                logger.warning(Strings.get("StopDomain.noDomainsRunning", getDomainRootDir()));
            } else {
                logger.warning(Strings.get("StopDomain.selectedDomainNotRunning", getDomainRootDir(), runningDomains));
            }
        } else {
            logger.warning(Strings.get("StopDomain.dasNotRunningRemotely"));
            
        }
        // If it has gotten this far, the domain has failed to be stopped so the command has failed
        return 1;
    }

    /**
     * Execute the actual stop-domain command.
     *
     * @throws org.glassfish.api.admin.CommandException
     */
    protected void doCommand() throws CommandException {
        // run the remote stop-domain command and throw away the output
        RemoteCLICommand cmd = new RemoteCLICommand(getName(), programOpts, env);
        cmd.setReadTimeout(timeout * 1000);
        try {
            cmd.executeAndReturnOutput("stop-domain", "--force", force.toString());
        } catch (Exception e) {
            // The domain server may have died so fast we didn't have time to
            // get the (always successful!!) return data.  This is NOT AN ERROR!
            // see: http://java.net/jira/browse/GLASSFISH-19672
        }
        try {
            waitForDeath();
        } catch (CommandException ex) {
            if (kill && isLocal()) {
                kill();
            } else {
                throw ex;
            }
        }
    }

    /**
     * Wait for the server to die.
     *
     * @throws org.glassfish.api.admin.CommandException
     */
    protected void waitForDeath() throws CommandException {
        if (!programOpts.isTerse()) {
            // use stdout because logger always appends a newline
            System.out.print(Strings.get("StopDomain.WaitDASDeath") + " ");
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
                if (!programOpts.isTerse() && count++ % 10 == 0) {
                    System.out.print(".");
                }
            } catch (InterruptedException ex) {
                // don't care
            }
        }

        if (!programOpts.isTerse()) {
            System.out.println();
        }

        if (alive) {
            throw new CommandException(Strings.get("StopDomain.DASNotDead",
                    timeout));
        }
    }

    private boolean timedOut(long startTime) {
        return (System.currentTimeMillis() - startTime) > (timeout * 1000);
    }

    protected int kill() throws CommandException {
        File prevPid = null;
        String pids = null;

        try {
            prevPid = new File(getServerDirs().getPidFile().getPath() + ".prev");

            if (!prevPid.canRead()) {
                throw new CommandException(Strings.get("StopDomain.nopidprev", prevPid));
            }

            pids = FileUtils.readSmallFile(prevPid).trim();
            String s = ProcessUtils.kill(Integer.parseInt(pids));

            if (s != null) {
                logger.finer(s);
            }
        } catch (CommandException ce) {
            throw ce;
        } catch (Exception ex) {
            throw new CommandException(Strings.get("StopDomain.pidprevreaderror",
                    prevPid, ex.getMessage()));
        }
        return 0;
    }
}
