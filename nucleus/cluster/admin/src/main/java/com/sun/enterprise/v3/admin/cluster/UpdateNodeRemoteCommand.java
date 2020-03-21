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
// Portions Copyright [2018-2020] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.util.cluster.RemoteType;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.SshConnector;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.inject.Inject;

/**
 * Remote AdminCommand to update a remote node.  This command is run only on DAS.
 *
 * @author Joe Di Pol
 * @author Byron Nevins
 */
public abstract class UpdateNodeRemoteCommand implements AdminCommand  {

    private static final Logger LOG = Logger.getLogger(UpdateNodeRemoteCommand.class.getName());

    @Inject
    private CommandRunner cr;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Nodes nodes;

    @Param(name="name", primary = true)
    private String name;

    @Param(name="nodehost", optional=true)
    private String nodehost;

    @Param(name = "installdir", optional=true)
    private String installdir;

    @Param(name = "nodedir", optional=true)
    private String nodedir;

    // these are the variables set as parameters in subclasses
    // we can't set them as parameters in this class bacause of the names
    protected String remotePort;
    protected String remoteUser;
    protected String sshkeyfile;
    protected String sshkeypassphrase;
    protected String remotepassword;
    protected String windowsdomain;

    @Param(name =  "force", optional = true, defaultValue = "false")
    private boolean force;

    private static final String NL = System.lineSeparator();

    protected abstract void populateParameters();
    protected abstract RemoteType getType();
    protected abstract String getDefaultPort();

    protected final void executeInternal(AdminCommandContext context) {
        LOG.finest(() -> String.format("executeInternal(context=%s)", context));
        ActionReport report = context.getActionReport();
        StringBuilder msg = new StringBuilder();
        Node node = null;

        Logger logger = context.getLogger();

        // Make sure Node is valid
        node = nodes.getNode(name);
        if (node == null) {
            String m = Strings.get("noSuchNode", name);
            logger.warning(m);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(m);
            return;
        }
        if (node.isDefaultLocalNode()) {
            String m = Strings.get("update.node.config.defaultnode", name);
            logger.warning(m);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(m);
            return;
        }

        // Ah the problems caused by hard-coding ssh into parameter names!
        populateParameters();

        // First create a map that holds the parameters and reflects what
        // the user passed on the command line.
        ParameterMap commandParameters = new ParameterMap();
        commandParameters.add("DEFAULT", name);
        commandParameters.add(NodeUtils.PARAM_INSTALLDIR, installdir);
        commandParameters.add(NodeUtils.PARAM_NODEHOST, nodehost);
        commandParameters.add(NodeUtils.PARAM_NODEDIR, nodedir);
        commandParameters.add(NodeUtils.PARAM_REMOTEPORT, remotePort);
        commandParameters.add(NodeUtils.PARAM_REMOTEUSER, remoteUser);
        if (sshkeyfile == null || sshkeyfile.isEmpty()) {
            commandParameters.add(NodeUtils.PARAM_SSHPASSWORD, remotepassword);
        } else {
            commandParameters.add(NodeUtils.PARAM_SSHKEYFILE, sshkeyfile);
            commandParameters.add(NodeUtils.PARAM_SSHKEYPASSPHRASE, sshkeypassphrase);
        }
        commandParameters.add(NodeUtils.PARAM_WINDOWSDOMAINNAME, windowsdomain);
        commandParameters.add(NodeUtils.PARAM_TYPE, getType().toString());

        // Validate the settings
        try {
            NodeUtils nodeUtils = new NodeUtils(serviceLocator, logger);
            nodeUtils.validate(createValidationParameters(node));
        } catch (CommandValidationException e) {
            String m1 = Strings.get("node.ssh.invalid.params");
            if (force) {
                String m2 = Strings.get("update.node.ssh.continue.force");
                msg.append(StringUtils.cat(NL, m1, e.getMessage(), m2));
            } else {
                String m2 = Strings.get("update.node.ssh.not.updated");
                msg.append(StringUtils.cat(NL, m1, m2, e.getMessage()));
                report.setMessage(msg.toString());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        // Settings are valid. Now use the generic update-node command to
        // update the node.
        CommandInvocation ci = cr.getCommandInvocation("_update-node", report, context.getSubject());
        ci.parameters(commandParameters);
        ci.execute();

        if (StringUtils.ok(report.getMessage())) {
            if (msg.length() > 0) {
                msg.append(NL);
            }
            msg.append(report.getMessage());
        }
        report.setMessage(msg.toString());
    }

    /**
     * Creates map used for validation, based on current node's values.
     * Values which are not set
     */
    private ParameterMap createValidationParameters(final Node node) {
        final ParameterMap parameters = new ParameterMap();
        parameters.insert("DEFAULT", name);
        parameters.insert(NodeUtils.PARAM_TYPE, getType().toString());
        parameters.insert(NodeUtils.PARAM_NODEHOST, nodehost, node.getNodeHost());
        parameters.insert(NodeUtils.PARAM_INSTALLDIR, installdir, node.getInstallDir());
        parameters.insert(NodeUtils.PARAM_NODEDIR, nodedir, node.getNodeDir());
        parameters.insert(NodeUtils.PARAM_WINDOWSDOMAINNAME, windowsdomain, node.getWindowsDomain());

        final SshConnector sshc = node.getSshConnector();
        parameters.insert(NodeUtils.PARAM_REMOTEPORT, remotePort, getSupplier(sshc, sshc::getSshPort));

        final SshAuth ssha = sshc.getSshAuth();
        parameters.insert(NodeUtils.PARAM_REMOTEUSER, remoteUser, getSupplier(ssha, ssha::getUserName));

        if (sshkeyfile == null && remotepassword == null) {
            // use previous values if they are set
            parameters.insert(NodeUtils.PARAM_SSHPASSWORD, null, getSupplier(ssha, ssha::getPassword));
            parameters.insert(NodeUtils.PARAM_SSHKEYFILE, null, getSupplier(ssha, ssha::getKeyfile));
            parameters.insert(NodeUtils.PARAM_SSHKEYPASSPHRASE, null, getSupplier(ssha, ssha::getKeyPassphrase));
        } else if (sshkeyfile == null) {
            // keyfile is preferred over password.
            // if keyfile is not set, configure password.
            parameters.insert(NodeUtils.PARAM_SSHPASSWORD, remotepassword, getSupplier(ssha, ssha::getPassword));
        } else {
            // if keyfile is set, don't configure password.
            parameters.insert(NodeUtils.PARAM_SSHKEYFILE, sshkeyfile, getSupplier(ssha, ssha::getKeyfile));
            parameters.insert(NodeUtils.PARAM_SSHKEYPASSPHRASE, //
                sshkeypassphrase, getSupplier(ssha, ssha::getKeyPassphrase));
        }
        return parameters;
    }


    private Supplier<String> getSupplier(final Object getterOwner, final Supplier<String> getter) {
        if (getterOwner == null) {
            return () -> null;
        }
        return () -> getter.get();
    }

}
