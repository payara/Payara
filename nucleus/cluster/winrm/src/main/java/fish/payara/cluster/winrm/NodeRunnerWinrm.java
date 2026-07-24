/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.cluster.winrm;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.api.admin.WinrmCommandExecutionException;
import org.glassfish.common.util.admin.AsadminInput;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NodeRunnerWinrm {
    private static final String NL = System.lineSeparator();
    private ServiceLocator habitat;
    private Logger logger;
    private String lastCommandRun = null;
    private WinRMHelper winrm = null;

    public NodeRunnerWinrm(ServiceLocator habitat, Logger logger) {
        this.logger = logger;
        this.habitat = habitat;
    }

    public boolean isWinRMNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }

        if (node.getType() == null) {
            return false;
        }

        return node.getType().equals("WINRM");
    }

    public String getLastCommandRun() {
        return lastCommandRun;
    }

    public int runAdminCommandOnRemoteNode(
            Node node,
            StringBuilder output,
            List<String> args,
            List<String> stdinLines
    ) throws IllegalArgumentException, UnsupportedOperationException, WinrmCommandExecutionException {

        args.add(0, AsadminInput.CLI_INPUT_OPTION);
        args.add(1, AsadminInput.SYSTEM_IN_INDICATOR); // specified to read from System.in

        if (!isWinRMNode(node)) {
            throw new UnsupportedOperationException("Node is not of type WinRM");
        }

        String nodeInstallDir = node.getInstallDirUnixStyle();
        if (!StringUtils.ok(nodeInstallDir)) {
            throw new IllegalArgumentException("Node does not have an installDir");
        }
        String installDir = nodeInstallDir + "/" + SystemPropertyConstants.getComponentName();

        List<String> fullcommand = new ArrayList<>();

        // On Windows nodes the install dir begins with a drive letter (e.g. C:/).
        // cmd.exe does not auto-resolve a fully-qualified path without an extension,
        // so we must explicitly use nadmin.bat on Windows hosts.
        String nadmin = installDir + "/lib/nadmin.bat";
        fullcommand.add(nadmin);
        fullcommand.addAll(args);

        try {
            lastCommandRun = commandListToString(fullcommand);
            logger.fine("Preparing WinRM command on " + node.getNodeHost() + ": " + lastCommandRun);

            winrm = habitat.getService(WinRMHelper.class);
            winrm.init(node);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            int commandStatus = winrm.executeCommand(String.join(" ", fullcommand), outStream).getStatusCode();
            logger.fine("Command returned status=" + commandStatus + " output=[" + outStream.toString().trim() + "]");
            output.append(outStream.toString());
            return commandStatus;

        } catch (IOException e) {
            String m1 = " Command execution failed. " + e.getMessage();
            String m2 = "";
            Throwable e2 = e.getCause();
            if (e2 != null) {
                m2 = e2.getMessage();
            }
            logger.severe("Command execution failed for " + lastCommandRun);
            WinrmCommandExecutionException exception = new WinrmCommandExecutionException(StringUtils.cat(":", m1, m2));
            exception.setCommand(lastCommandRun);
            throw exception;
        }
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
