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
import static com.sun.enterprise.util.StringUtils.ok;

public class NodeRunnerDcom {
    private final Logger logger;
    private Node node;

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

        this.node = thisNode;
        WindowsCredentials bonafides = validate();
        List<String> fullcommand = new ArrayList<String>();
        fullcommand.add(getNadminPath());
        fullcommand.addAll(args);
        String commandAsString = commandListToString(fullcommand);
        WindowsRemoteScripter scripter = new WindowsRemoteScripter(bonafides);
        try {
            String out = scripter.run(commandAsString);
            logger.info(Strings.get("remote.command.summary", commandAsString, out));
            return 0;
        }
        catch (WindowsException ex) {
            throw new SSHCommandExecutionException(Strings.get(
                    "remote.command.error", ex.getMessage(), commandAsString), ex);
        }
    }

    /**
     * Hide the ghastly messy configuration stuff in this method!
     * @return a valid WindowsCredentials object
     * @throws SSHCommandExecutionException
     */
    private WindowsCredentials validate() throws SSHCommandExecutionException {
        if (node == null)
            throw new IllegalArgumentException(Strings.get("internal.error", "Node is null"));

        if (!isDcomNode(node))
            throw new SSHCommandExecutionException(Strings.get("internal.error", "Node is not of type DCOM"));

        SshConnector conn = node.getSshConnector();
        if (conn == null)
            throw new SSHCommandExecutionException(Strings.get("no.password"));

        SshAuth auth = conn.getSshAuth();
        if (auth == null)
            throw new SSHCommandExecutionException(Strings.get("no.password"));

        String password = auth.getPassword();
        if (!ok(password))
            throw new SSHCommandExecutionException(Strings.get("no.password"));

        String host = node.getNodeHost();
        if (!ok(host))
            host = conn.getSshHost();
        if (!ok(host))
            throw new SSHCommandExecutionException(Strings.get("no.host"));

        String user = auth.getUserName();

        if (!ok(user))
            user = System.getProperty("user.name");
        if (!ok(user))
            throw new SSHCommandExecutionException(Strings.get("no.username"));

        String windowsDomain = node.getWindowsDomain();
        if (!ok(windowsDomain))
            windowsDomain = host;

        return new WindowsCredentials(host, windowsDomain, user, password);
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

    private static boolean isDcomNode(Node node) {
        return "DCOM".equals(node.getType());
    }

    private String getNadminPath() throws SSHCommandExecutionException {
        String path = node.getInstallDirUnixStyle();

        if (!ok(path))
            throw new SSHCommandExecutionException(Strings.get("no.lib.dir"));

        if (!path.endsWith("/"))
            path += "/";

        path += SystemPropertyConstants.getComponentName();
        path += "/lib/nadmin.bat";
        path = path.replace('/', '\\');
        path = StringUtils.quotePathIfNecessary(path);
        return path;
    }
}
