/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2022] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.util.TimeoutParamDefaultCalculator;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.ObjectAnalyzer;
import jakarta.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Restart a local server instance.
 */
@Service(name = "restart-local-instance")
@ExecuteOn(RuntimeType.DAS)
@PerLookup
public class RestartLocalInstanceCommand extends SynchronizeInstanceCommand {

    @Param(optional = true, shortName = "d", defaultValue = "false")
    private Boolean debug;

    @Param(optional = true, defaultCalculator = TimeoutParamDefaultCalculator.class)
    private int timeout;

    @Inject
    private ServiceLocator habitat;

    @Override
    protected int executeCommand() throws CommandException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(toString());
        }

        File serverDir = getServerDirs().getServerDir();

        if (serverDir == null || !serverDir.isDirectory()) {
            return noSuchInstance();
        }

        String serverName = getServerDirs().getServerName();
        HostAndPort addr = getAdminAddress(serverName);

        if (!isRunning() && !isRunning(addr.getHost(), addr.getPort())) {
            return instanceNotRunning();
        }

        if (sync.equals("none")) {
            logger.info(Strings.get("Instance.nosync"));
        } else if (!synchronizeInstance()) {
            File domainXml = new File(new File(instanceDir, "config"), "domain.xml");
            if (!domainXml.exists()) {
                logger.info(Strings.get("Instance.nodomainxml"));
                return ERROR;
            }
            logger.info(Strings.get("Instance.syncFailed"));
        }

        programOpts.setHostAndPort(addr);

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Stopping server at {0}", addr);
        }

        logger.finer("It's the correct Instance");
        return doRemoteCommand();

    }

    protected final int doRemoteCommand() throws CommandException {
        // see StopLocalInstance for comments.  These 2 lines can be refactored.
        setLocalPassword();
        programOpts.setInteractive(false);

        if (!isRestartable()) {
            throw new CommandException(Strings.get("restart.notRestartable"));
        }

        int oldServerPid = getServerPid(); // might be < 0

        // run the remote restart-domain command and throw away the output
        RemoteCLICommand cmd = new RemoteCLICommand("_restart-instance", programOpts, env);

        if (debug != null) {
            cmd.executeAndReturnOutput("_restart-instance", "--debug", debug.toString());
        } else {
            cmd.executeAndReturnOutput("_restart-instance");
        }

        waitForRestart(oldServerPid, (timeout * 1000));

        return 0;
    }

    protected int instanceNotRunning() throws CommandException {
        logger.warning(Strings.get("restart.instanceNotRunning"));
        CLICommand cmd = habitat.getService(CLICommand.class, "start-local-instance");
        /*
         * Collect the arguments that also apply to start-instance-domain.
         * The start-local-instance CLICommand object will already have the
         * ProgramOptions injected into it so we don't need to worry
         * about them here.
         *
         * Usage: asadmin [asadmin-utility-options] start-local-instance
         *    [--verbose[=<verbose(default:false)>]]
         *    [--debug[=<debug(default:false)>]] [--sync <sync(default:normal)>]
         *    [--nodedir <nodedir>] [--node <node>]
         *    [-?|--help[=<help(default:false)>]] [instance_name]
         *
         * Only --debug, --nodedir, -node, and the operand apply here.
         */
        List<String> opts = new ArrayList<>();
        opts.add("start-local-instance");
        if (debug != null) {
            opts.add("--debug");
            opts.add(debug.toString());
        }
        if (nodeDir != null) {
            opts.add("--nodedir");
            opts.add(nodeDir);
        }
        if (node != null) {
            opts.add("--node");
            opts.add(node);
        }
        if (sync != null) {
            opts.add("--sync");
            opts.add(sync);
        }
        if (instanceName != null) {
            opts.add(instanceName);
        }

        return cmd.execute(opts.toArray(new String[opts.size()]));
    }

    @Override
    protected boolean mkdirs(File f) {
        // we definitely do NOT want dirs created for this instance if they don't exist!
        return false;
    }

    @Override
    protected void validate() throws CommandException {
        super.validate();

        if (timeout <= 0) {
            throw new CommandException("Timeout must be at least 1 second long.");
        }

        File dir = getServerDirs().getServerDir();

        if (!dir.isDirectory()) {
            throw new CommandException(Strings.get("Instance.noSuchInstance"));
        }
    }

    /**
     * Print message and return exit code when we detect that there is no such
     * instance
     */
    private int noSuchInstance() {
        // by definition this is not an error
        // https://glassfish.dev.java.net/issues/show_bug.cgi?id=8387
        logger.warning(Strings.get("Instance.noSuchInstance"));
        return 0;
    }

    @Override
    public String toString() {
        return ObjectAnalyzer.toStringWithSuper(this);
    }

}
