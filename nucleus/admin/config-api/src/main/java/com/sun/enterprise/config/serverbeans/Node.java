/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.*;
import static org.glassfish.config.support.Constants.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Injectable;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;

import javax.validation.Payload;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.constraints.Pattern;

/**
 * A cluster defines a homogeneous set of server instances that share the same
 * applications, resources, and configuration.
 */
@Configured
@SuppressWarnings("unused")
@NotDuplicateTargetName(message = "{node.duplicate.name}", payload = Node.class)
public interface Node extends ConfigBeanProxy, Injectable, Named, ReferenceContainer, RefContainer, Payload {
    /**
     * Sets the node name
     * @param value node name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "name", primary = true)
    @Override
    public void setName(String value) throws PropertyVetoException;

    @NotTargetKeyword(message = "{node.reserved.name}", payload = Node.class)
    @Pattern(regexp = NAME_SERVER_REGEX, message = "{node.invalid.name}", payload = Node.class)
    @Override
    public String getName();

    /**
     * points to the parent directory of the node(s) directory.
     *
     * @return path location of node-dir
     */
    @Attribute
    String getNodeDir();

    /**
     * Sets the value of the node-dir, top-level parent directory of node(s)
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "nodedir", optional = true)
    void setNodeDir(String value) throws PropertyVetoException;

    /**
     * points to a named host.
     *
     * @return a named host name
     */
    @Attribute
    @Pattern(regexp = NAME_REGEX, message = "{nodehost.invalid.name}", payload = Node.class)
    String getNodeHost();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "nodehost", optional = true)
    void setNodeHost(String value) throws PropertyVetoException;

    /**
     * points to a GlassFish installation root
     *
     * @return value of install-dir
     */
    @Attribute
    String getInstallDir();

    /**
     * Sets the value of install-dir, the GlassFish installation root.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "installdir", optional = true)
    void setInstallDir(String value) throws PropertyVetoException;

    @Attribute()
    String getType();

    /**
     * Sets the value of type of this node.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "type")
    void setType(String value) throws PropertyVetoException;

    @Element
    SshConnector getSshConnector();

    void setSshConnector(SshConnector connector);

    /**
     * Returns the install dir with separators as forward slashes.  This is needed to run commands
     * over SSH tools on Windows where the backslashes are interpruted as escape chars.
     *
     * @return the install dir with separators as forward slashes
     */
    @DuckTyped
    String getInstallDirUnixStyle();

    /**
     * Returns the node dir with separators as forward slashes.  This is needed to run commands
     * over SSH tools on Windows where the backslashes are interpruted as escape chars.
     *
     * @return the node dir with separators as forward slashes
     */
    @DuckTyped
    String getNodeDirUnixStyle();

    /**
     * Returns the node dir as an absolute path. If the node dir path
     * in the Node element is relative this will make it absolute relative
     * to the node's installdir.
     * @return the node's nodedir as an absolute path. Null if no nodedir.
     */
    @DuckTyped
    String getNodeDirAbsolute();

    @DuckTyped
    String getNodeDirAbsoluteUnixStyle();

    /**
     * Is a node being used by any server instance?
     * @return true if node is referenced by any server instance, else false.
     */
    @DuckTyped
    boolean nodeInUse();

    /**
     * True if this is the default local node. Example: localhost-domain1
     * @return
     */
    @DuckTyped
    boolean isDefaultLocalNode();

    /**
     * True if the node's nodeHost is local to this
     * @return
     */
    @DuckTyped
    boolean isLocal();

    class Duck {
        public static String getInstallDirUnixStyle(Node node) {
            String installDir = node.getInstallDir();
            if (installDir == null)
                return null;
            return installDir.replaceAll("\\\\", "/");
        }

        public static String getNodeDirUnixStyle(Node node) {
            String nodeDir = node.getNodeDir();
            if (nodeDir == null)
                return null;
            return nodeDir.replaceAll("\\\\", "/");
        }

        public static String getNodeDirAbsolute(Node node) {
            // If nodedir is relative make it absolute relative to installRoot
            String nodeDir = node.getNodeDir();
            if (nodeDir == null || nodeDir.length() == 0)
                return null;
            File nodeDirFile = new File(nodeDir);
            if (nodeDirFile.isAbsolute()) {
                return nodeDir;
            }
            // node-dir is relative. Make it absolute. We root it under the
            // GlassFish root install directory.
            String installDir = node.getInstallDir();
            File installRootFile = new File(installDir, "glassfish");
            File absoluteNodeDirFile = new File(installRootFile, nodeDir);
            return absoluteNodeDirFile.getPath();
        }

        public static String getNodeDirAbsoluteUnixStyle(Node node) {
            String nodeDirAbsolute = getNodeDirAbsolute(node);
            if (nodeDirAbsolute == null)
                return null;
            return nodeDirAbsolute.replaceAll("\\\\", "/");
        }

        public static boolean isDefaultLocalNode(Node node) {
            Dom serverDom = Dom.unwrap(node);
            Domain domain = serverDom.getHabitat().getComponent(Domain.class);
            if (node.getName().equals("localhost-" + domain.getName())) {
                return true;
            }
            return false;
        }

        public static boolean isLocal(Node node) {
            // Short circuit common case for efficiency
            Dom serverDom = Dom.unwrap(node);
            Domain domain = serverDom.getHabitat().getComponent(Domain.class);
            if (node.getName().equals("localhost-" + domain.getName())) {
                return true;
            }
            String nodeHost = node.getNodeHost();
            if (nodeHost == null || nodeHost.length() == 0) {
                return false;
            }
            return NetUtils.isThisHostLocal(nodeHost);
        }

        public static boolean nodeInUse(Node node) {
            //check if node is referenced by an instance
            String nodeName = node.getName();
            Dom serverDom = Dom.unwrap(node);
            Servers servers = serverDom.getHabitat().getComponent(Servers.class);
            List<Server> serverList = servers.getServer();
            if (serverList != null) {
                for (Server server : serverList) {
                    if (nodeName.equals(server.getNodeRef())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Service
    @Scoped(PerLookup.class)
    class Decorator implements CreationDecorator<Node> {
        @Param(name = "nodedir", optional = true)
        String nodedir = null;
        @Param(name = "nodehost", optional = true)
        String nodehost = null;
        @Param(name = "installdir", optional = true)
        String installdir = null;
        @Param(name = "type")
        String type = null;
        @Param(name = "sshport", optional = true)
        String sshPort = null;
        @Param(name = "sshnodehost", optional = true)
        String sshHost = null;
        @Param(name = "sshuser", optional = true)
        String sshuser = null;
        @Param(name = "sshkeyfile", optional = true)
        String sshkeyfile;
        @Param(name = "sshpassword", optional = true)
        String sshpassword;
        @Param(name = "sshkeypassphrase", optional = true)
        String sshkeypassphrase;
        @Inject
        Habitat habitat;
        @Inject
        ServerEnvironment env;
        @Inject
        Domain domain;

        /**
         * Decorates the newly CRUD created cluster configuration instance.
         * tasks :
         *      - ensures that it references an existing configuration
         *      - creates a new config from the default-config if no config-ref
         *        was provided.
         *      - check for deprecated parameters.
         *
         * @param context administration command context
         * @param instance newly created configuration element
         * @throws TransactionFailure
         * @throws PropertyVetoException
         */
        @Override
        public void decorate(AdminCommandContext context, final Node instance) throws TransactionFailure, PropertyVetoException {

            // If these options were passed a value of the empty string then
            // we want to make sure they are null in the Node. The
            // admin console often passes the empty string instead of null.
            // See bug 14873
            if (!StringUtils.ok(nodedir))
                instance.setNodeDir(null);
            if (!StringUtils.ok(installdir))
                instance.setInstallDir(null);
            if (!StringUtils.ok(nodehost))
                instance.setNodeHost(null);

            //only create-node-ssh and update-node-ssh should be changing the type to SSH
            instance.setType(type);

            if (type.equals("CONFIG"))
                return;
            SshConnector sshC = instance.createChild(SshConnector.class);

            SshAuth sshA = sshC.createChild(SshAuth.class);
            if (StringUtils.ok(sshuser))
                sshA.setUserName(sshuser);
            if (StringUtils.ok(sshkeyfile))
                sshA.setKeyfile(sshkeyfile);
            if (StringUtils.ok(sshpassword))
                sshA.setPassword(sshpassword);
            if (StringUtils.ok(sshkeypassphrase))
                sshA.setKeyPassphrase(sshkeypassphrase);
            sshC.setSshAuth(sshA);

            if (StringUtils.ok(sshPort))
                sshC.setSshPort(sshPort);

            if (StringUtils.ok(sshHost))
                sshC.setSshHost(sshHost);

            instance.setSshConnector(sshC);

        }
    }

    @Service
    @Scoped(PerLookup.class)
    class DeleteDecorator implements DeletionDecorator<Nodes, Node> {
        @Inject
        private Domain domain;
        @Inject
        Nodes nodes;
        @Inject
        Servers servers;
        @Inject
        private ServerEnvironment env;

        @Override
        public void decorate(AdminCommandContext context, Nodes parent, Node child) throws
                PropertyVetoException, TransactionFailure {
            Logger logger = LogDomains.getLogger(Node.class, LogDomains.ADMIN_LOGGER);
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Node.class);
            String nodeName = child.getName();

            if (nodeName.equals("localhost-" + domain.getName())) { // can't delete localhost node
                final String msg = localStrings.getLocalString(
                        "Node.localhost",
                        "Cannot remove Node {0}. ", child.getName());

                logger.log(Level.SEVERE, msg);
                throw new TransactionFailure(msg);
            }


            List<Node> nodeList = nodes.getNode();

            // See if any servers are using this node
            List<Server> serversOnNode = servers.getServersOnNode(child);
            int n = 0;
            if (serversOnNode != null && serversOnNode.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Server server : serversOnNode) {
                    if (n > 0)
                        sb.append(", ");
                    sb.append(server.getName());
                    n++;
                }

                final String msg = localStrings.getLocalString(
                        "Node.referencedByInstance",
                        "Node {0} referenced in server instance(s): {1}.  Remove instances before removing node.", child.getName(), sb.toString());
                logger.log(Level.SEVERE, msg);
                throw new TransactionFailure(msg);
            }

            nodeList.remove(child);
        }
    }
}
