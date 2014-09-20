/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.admin.cli.remote.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.util.logging.Level;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.*;

/**
 * A local version command.
 * Prints the version of the server, if running. Prints the version from locally
 * available Version class if server is not running, if the --local flag is passed
 * or if the version could not
 * be obtained from a server for some reason. The idea is to get the version
 * of server software, the server process need not be running. This command
 * does not return the version of local server installation if its
 * options (host, port, user, passwordfile) identify a running server.
 * 
 * @author km@dev.java.net
 * @author Bill Shannon
 */
@Service(name = "version")
@PerLookup
public class VersionCommand extends CLICommand {

    @Param(optional = true, shortName = "v")
    private boolean verbose;

    @Param(optional = true)
    private boolean local;

    @Param(optional = true)
    private boolean terse;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(VersionCommand.class);

    @Override
    protected int executeCommand() throws CommandException {
        if (local) {
            invokeLocal();
            return 0;
        }
        try {
            RemoteCLICommand cmd = new RemoteCLICommand("version", programOpts, env);
            String version;
            if (verbose)
                version = cmd.executeAndReturnOutput("version", "--verbose");
            else
                version = cmd.executeAndReturnOutput("version");
            version = version.trim();   // get rid of gratuitous newlines
            logger.info(terse ? version : strings.get("version.remote", version));
        } catch (Exception e) {
            // suppress all output and infer that the server is not running
            printRemoteException(e);
            invokeLocal();
        }
        return 0;       // always succeeds
    }

    private void invokeLocal() {
        String fv = Version.getFullVersion();

        logger.info(terse ? fv : strings.get("version.local", fv));
        if (verbose)
            logger.info(strings.get("version.local.java",
				    System.getProperty("java.version")));
    }

    private void printRemoteException(Exception e) {
        logger.info(strings.get("remote.version.failed",
                programOpts.getHost(), programOpts.getPort() + ""));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(e.getMessage());
        }
        else {
            logger.info(strings.get("remote.version.failed.debug", 
                Environment.getDebugVar()));
        }
    }
}
