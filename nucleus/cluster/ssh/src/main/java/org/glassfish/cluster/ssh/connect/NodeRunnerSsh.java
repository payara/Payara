/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.glassfish.common.util.admin.AsadminInput;
import org.glassfish.api.admin.SSHCommandExecutionException;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.StringUtils;

import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.ByteArrayOutputStream;

public class NodeRunnerSsh  {

    private static final String NL = System.getProperty("line.separator");

    private ServiceLocator habitat;
    private Logger logger;

    private String lastCommandRun = null;

    private int commandStatus;

    private SSHLauncher sshL = null;

    public NodeRunnerSsh(ServiceLocator habitat, Logger logger) {
        this.logger = logger;
        this.habitat = habitat;
    }


    public boolean isSshNode(Node node) {

        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        if (node.getType() ==null)
            return false;
        return node.getType().equals("SSH");
    }

    String getLastCommandRun() {
        return lastCommandRun;
    }

    public int runAdminCommandOnRemoteNode(Node node, StringBuilder output,
                                       List<String> args,
                                       List<String> stdinLines) throws
            SSHCommandExecutionException, IllegalArgumentException,
            UnsupportedOperationException {

        args.add(0, AsadminInput.CLI_INPUT_OPTION);
        args.add(1, AsadminInput.SYSTEM_IN_INDICATOR); // specified to read from System.in

        if (! isSshNode(node)) {
            throw new UnsupportedOperationException(
                    "Node is not of type SSH");
        }

        String installDir = node.getInstallDirUnixStyle() + "/" +
            SystemPropertyConstants.getComponentName();
        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException("Node does not have an installDir");
        }

        List<String> fullcommand = new ArrayList<String>();

        // We can just use "nadmin" even on Windows since the SSHD provider
        // will locate the command (.exe or .bat) for us
        fullcommand.add(installDir + "/lib/nadmin");
        fullcommand.addAll(args);

        try{
            lastCommandRun = commandListToString(fullcommand);
            trace("Running command on " + node.getNodeHost() + ": " +
                    lastCommandRun);
            sshL=habitat.getService(SSHLauncher.class);
            sshL.init(node, logger);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            commandStatus = sshL.runCommand(fullcommand, outStream, stdinLines);
            output.append(outStream.toString());
            return commandStatus;

        }catch (IOException ex) {
            String m1 = " Command execution failed. " +ex.getMessage();
            String m2 = "";
            Throwable e2 = ex.getCause();
            if(e2 != null) {
                m2 = e2.getMessage();
            }
            logger.severe("Command execution failed for "+ lastCommandRun);
            SSHCommandExecutionException cee = new SSHCommandExecutionException(StringUtils.cat(":",
                                            m1, m2));
            cee.setSSHSettings(sshL.toString());
            cee.setCommandRun(lastCommandRun);
            throw cee;

        } catch (java.lang.InterruptedException ei){
            ei.printStackTrace();
            String m1 = ei.getMessage();
            String m2 = "";
            Throwable e2 = ei.getCause();
            if(e2 != null) {
                m2 = e2.getMessage();
            }
            logger.severe("Command interrupted "+ lastCommandRun);
            SSHCommandExecutionException cee = new SSHCommandExecutionException(StringUtils.cat(":",
                                             m1, m2));
            cee.setSSHSettings(sshL.toString());
            cee.setCommandRun(lastCommandRun);
            throw cee;
        }
    }
    private void trace(String s) {
        logger.fine(String.format("%s: %s", this.getClass().getSimpleName(), s));
   }

    private String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }
}
