/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.cluster.ssh.connect;

import com.sun.enterprise.util.io.WindowsRemoteFile;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.universal.process.WindowsCredentials;
import com.sun.enterprise.universal.process.WindowsException;
import com.sun.enterprise.universal.process.WindowsRemoteScripter;
import org.glassfish.api.admin.SSHCommandExecutionException;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.WindowsRemoteFileSystem;
import org.glassfish.cluster.ssh.util.DcomInfo;
import org.glassfish.cluster.ssh.util.DcomUtils;
import org.glassfish.common.util.admin.AsadminInput;
import static com.sun.enterprise.util.StringUtils.ok;

public class NodeRunnerDcom {
    private final Logger logger;
    private Node node;
    private WindowsRemoteFile authTokenFile;
    private String authTokenFilePath;
    private DcomInfo dcomInfo;

    public NodeRunnerDcom(Logger logger) {
        this.logger = logger;
    }

    /*
     * return 0 is success, otherwise failure
     */
    public final int runAdminCommandOnRemoteNode(Node thisNode, StringBuilder output,
            List<String> args,
            List<String> stdinLines) throws
            SSHCommandExecutionException, IllegalArgumentException,
            UnsupportedOperationException {

        String commandAsString = null;
        try {
            this.node = thisNode;
            dcomInfo = new DcomInfo(node);
            WindowsCredentials bonafides = dcomInfo.getCredentials();
            List<String> fullcommand = new ArrayList<String>();
            fullcommand.add(dcomInfo.getNadminPath());

            if(stdinLines != null && !stdinLines.isEmpty())
                setupAuthTokenFile(fullcommand, stdinLines);

            fullcommand.addAll(args);
            commandAsString = commandListToString(fullcommand);
            WindowsRemoteScripter scripter = new WindowsRemoteScripter(bonafides);
            String out = scripter.run(commandAsString);
            logger.info(Strings.get("remote.command.summary", commandAsString, out));
            return 0;
        }
        catch (WindowsException ex) {
            throw new SSHCommandExecutionException(Strings.get(
                    "remote.command.error", ex.getMessage(), commandAsString), ex);
        }
        finally {
            teardownAuthTokenFile();
        }
    }

    private void trace(String s) {
        logger.fine(String.format("%s: %s", this.getClass().getSimpleName(), s));
    }

    private static String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }

    /*
     * 1. creae a remote file
     * 2. copy the token/auth stuff into it
     * 3. add the correct args to the remote commandline
     *    Put the file in the same directory that nadmin lives in (lib)
     */
    private void setupAuthTokenFile(List<String> cmd, List<String> stdin) throws WindowsException {
        WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(dcomInfo.getCredentials());
        authTokenFilePath = dcomInfo.getNadminParentPath() + "\\stdin";
        authTokenFile = new WindowsRemoteFile(wrfs, authTokenFilePath);
        authTokenFile.copyFrom(stdin);

        // jira 17731 -- Asadmin can't handle Windows full paths (the ":" is the problem)
        URI authTokenFileUri = new File(authTokenFilePath).toURI();

        cmd.add(AsadminInput.CLI_INPUT_OPTION);
        cmd.add(authTokenFileUri.toString());
    }

    private void teardownAuthTokenFile() {
        if(authTokenFile != null)
            try {
            authTokenFile.delete();
        }
        catch (WindowsException ex) {
            logger.warning(Strings.get("cant.delete", dcomInfo.getHost(), authTokenFilePath));
        }
    }

}
