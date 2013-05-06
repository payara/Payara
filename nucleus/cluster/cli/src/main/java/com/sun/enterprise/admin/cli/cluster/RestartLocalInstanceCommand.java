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

import java.io.*;
import java.util.*;

import javax.inject.Inject;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.remote.*;

/**
 *
 * @author Byron Nevins
 */
@Service(name = "restart-local-instance")
@PerLookup
public class RestartLocalInstanceCommand extends StopLocalInstanceCommand {

    @Param(name = "debug", optional = true)
    private Boolean debug;

    @Inject
    private ServiceLocator habitat;

    @Override
    protected final int doRemoteCommand() throws CommandException {
        // see StopLocalInstance for comments.  These 2 lines can be refactored.
        setLocalPassword();
        programOpts.setInteractive(false);

        if(!isRestartable())
            throw new CommandException(Strings.get("restart.notRestartable"));

        int oldServerPid = getServerPid(); // might be < 0

        // run the remote restart-domain command and throw away the output
        RemoteCLICommand cmd = new RemoteCLICommand("_restart-instance", programOpts, env);

        if (debug != null)
            cmd.executeAndReturnOutput("_restart-instance", "--debug", debug.toString());
        else
            cmd.executeAndReturnOutput("_restart-instance");

        waitForRestart(oldServerPid);
        return 0;
    }

    @Override
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
        List<String> opts = new ArrayList<String>();
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
        if (instanceName != null)
            opts.add(instanceName);

        return cmd.execute(opts.toArray(new String[opts.size()]));
    }
}
