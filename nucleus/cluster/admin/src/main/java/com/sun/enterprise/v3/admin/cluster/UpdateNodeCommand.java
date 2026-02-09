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
// Portions Copyright [2018-2026] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.cluster.RemoteType;
import com.sun.enterprise.util.cluster.SshAuthType;
import com.sun.enterprise.util.net.NetUtils;

import java.beans.PropertyVetoException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Remote AdminCommand to update a config node. This command is run only on DAS.
 * <p>
 * Update the config node on DAS
 *
 * @author Carla Mott
 */
@Service(name = "_update-node")
@I18n("update.node")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Node.class,
        opType=RestEndpoint.OpType.POST,
        path="_update-node",
        description="Update Node",
        params={
            @RestParam(name="name", value="$parent")
        })
})
public class UpdateNodeCommand implements AdminCommand {

    private static final Logger LOG = Logger.getLogger(UpdateNodeCommand.class.getName());

    @Inject
    Nodes nodes;

    @Inject
    Domain domain;

    @Param(name="name", primary = true)
    String name;

    @Param(name="nodedir", optional=true)
    String nodedir;

    @Param(name="nodehost", optional=true)
    String nodehost;

    @Param(name = "installdir", optional=true)
    String installdir;

    @Param(name="sshport", optional=true)
    String sshport;

    @Param(name="sshuser", optional=true)
    String sshuser;

    @Param(name="sshnodehost", optional=true)
    String sshnodehost;

    /** {@link SshAuthType} name */
    @Param(name = "sshauthtype", optional=true)
    String sshAuthType;

    @Param(name="sshkeyfile", optional=true)
    String sshkeyfile;

    @Param(name = "sshkeypassphrase", optional = true, password = true)
    String sshkeypassphrase;

    @Param(name = "sshpassword", optional = true, password=true)
    String sshpassword;

    @Param(name = "windowsdomain", optional = true)
    String windowsdomain;

    @Param(name = "dockerImage", optional = true)
    String dockerImage;

    @Param(name = "dockerPasswordFile", optional = true)
    String dockerPasswordFile;

    @Param(name = "dockerPort", optional = true)
    Integer dockerPort;

    @Param(name = "useTls", optional = true)
    String useTls;

    /** {@link RemoteType} name */
    @Param(name = "type", optional=true)
    String type;

    @Override
    public void execute(AdminCommandContext context) {
        LOG.finest(() -> String.format("execute(context=%s)", context));
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();
        final Node node = nodes.getNode(name);
        if (node == null) {
            //node doesn't exist
            String msg = Strings.get("noSuchNode", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        // Validate installdir if passed and running on localhost and not a Docker node
        if (StringUtils.ok(nodehost) && NetUtils.isThisHostLocal(nodehost) && StringUtils.ok(installdir)
            && !node.getType().equals(RemoteType.DOCKER.name())) {

            // Create a resolver that can replace system properties in strings
            Map<String, String> systemPropsMap = new HashMap<String, String>((Map) (System.getProperties()));
            TokenResolver resolver = new TokenResolver(systemPropsMap);
            String resolvedInstallDir = resolver.resolve(installdir);
            File actualInstallDir = new File(resolvedInstallDir + File.separatorChar + NodeUtils.LANDMARK_FILE);
            if (!actualInstallDir.exists()) {
                report.setMessage(Strings.get("invalid.installdir", installdir));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        // If the node is in use then we can't change certain attributes
        // like the install directory or node directory.
        if (node.nodeInUse()) {
            String badparam = null;
            String configNodedir = node.getNodeDir();
            String configInstalldir = node.getInstallDir();
            if (!allowableChange(nodedir, configNodedir)){
                badparam = "nodedir";
            }
            if (!allowableChange(installdir, configInstalldir)) {
                badparam = "installdir";
            }
            if (StringUtils.ok(badparam)) {
                String msg = Strings.get("noUpdate.nodeInUse", name, badparam);
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        }

        try {
            updateNodeElement(name);
        } catch (TransactionFailure e) {
            logger.log(Level.WARNING, Strings.get("failed.to.update.node {0}", name), e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }
    }


    private void updateNodeElement(final String nodeName) throws TransactionFailure {
        LOG.fine(() -> String.format("updateNodeElement(nodeName=%s)", nodeName));
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                Transaction t = Transaction.getTransaction(param);
                if (t != null) {
                    Nodes nodes = ((Domain) param).getNodes();
                    Node node = nodes.getNode(nodeName);
                    Node writeableNode = t.enroll(node);
                    if (windowsdomain != null) {
                        writeableNode.setWindowsDomain(windowsdomain);
                    }
                    if (nodedir != null) {
                        writeableNode.setNodeDir(nodedir);
                    }
                    if (nodehost != null) {
                        writeableNode.setNodeHost(nodehost);
                    }
                    if (installdir != null) {
                        writeableNode.setInstallDir(installdir);
                    }
                    if (type != null) {
                        writeableNode.setType(type);
                    }
                    if (RemoteType.SSH.name().equals(type)) {
                        SshConnector sshConnector = writeableNode.getSshConnector();
                        if (sshConnector == null)  {
                            sshConnector = writeableNode.createChild(SshConnector.class);
                        } else {
                            sshConnector = t.enroll(sshConnector);
                        }

                        if (sshport != null) {
                            sshConnector.setSshPort(sshport);
                        }
                        if(sshnodehost != null) {
                            sshConnector.setSshHost(sshnodehost);
                        }
                        writeableNode.setSshConnector(sshConnector);
                        if (sshAuthType != null || sshuser != null //
                            || sshkeyfile != null || sshpassword != null || sshkeypassphrase != null) {
                            SshAuth sshAuth = sshConnector.getSshAuth();
                            if (sshAuth == null) {
                               sshAuth = sshConnector.createChild(SshAuth.class);
                            } else {
                                sshAuth = t.enroll(sshAuth);
                            }

                            if (sshuser != null) {
                                sshAuth.setUserName(sshuser);
                            }
                            if (sshkeypassphrase != null) {
                                sshAuth.setKeyPassphrase(sshkeypassphrase);
                            }
                            if (sshAuthType == null) {
                                // if both set, keyfile wins
                                if (sshpassword != null) {
                                    sshAuth.setKeyfile(null);
                                    sshAuth.setPassword(sshpassword);
                                }
                                if (sshkeyfile != null) {
                                    sshAuth.setKeyfile(sshkeyfile);
                                    sshAuth.setPassword(null);
                                }
                            } else {
                                if (SshAuthType.KEY.name().equals(sshAuthType)) {
                                    // keyfile is set even if null, it would not be possible to
                                    // return to default from UI otherwise
                                    sshAuth.setKeyfile(sshkeyfile);
                                    sshAuth.setPassword(null);
                                } else if (SshAuthType.PASSWORD.name().equals(sshAuthType)) {
                                    sshAuth.setKeyfile(null);
                                    sshAuth.setKeyPassphrase(null);
                                    if (sshpassword != null) {
                                        sshAuth.setPassword(sshpassword);
                                    }
                                }
                            }
                            sshConnector.setSshAuth(sshAuth);
                        }
                    }

                    if (dockerImage != null) {
                        writeableNode.setDockerImage(dockerImage);
                    }

                    if (dockerPasswordFile != null) {
                        writeableNode.setDockerPasswordFile(dockerPasswordFile);
                    }

                    if (dockerPort != null) {
                        writeableNode.setDockerPort(Integer.toString(dockerPort));
                    }

                    if (useTls != null) {
                        writeableNode.setUseTls(useTls);
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }

    /**
     * If the node is in use, is it OK to change currentvalue to newvalue?
     */
    private static boolean allowableChange(final String newvalue, final String currentvalue) {
        LOG.finest(() -> String.format("allowableChange(newvalue=%s, currentvalue=%s)", newvalue, currentvalue));

        // If the new value is not specified, then we aren't changing anything
        if (newvalue == null) {
            return true;
        }

        // If the current (config) value is null or "" then let it be changed.
        // We need to do this for the offline config case where the user has
        // created a config node with no values, created instances using those
        // nodes, then updates the values later. This has the undersireable
        // effect of letting you, for example, set a nodedir on a node
        // that was created without one.
        if (!StringUtils.ok(currentvalue)) {
            return true;
        }

        // If the values are the same, then we aren't changing anything.
        if (newvalue.equals(currentvalue)) {
            return true;
        }

        if (newvalue.contains("$") || currentvalue.contains("$")) {
            // One or both of the values may contain an unexpanded
            // property. Expand them then compare
            Map<String, String> systemPropsMap =
                        new HashMap<String, String>((Map)(System.getProperties()));
            TokenResolver resolver = new TokenResolver(systemPropsMap);
            final String resolvedNewValue = resolver.resolve(newvalue);
            final String resolvedCurrentValue = resolver.resolve(currentvalue);
            return resolvedNewValue.equals(resolvedCurrentValue);
        }

        // Values don't match.
        return false;
    }

}
