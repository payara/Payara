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

package com.sun.enterprise.util.io;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.*;
import java.io.File;
import java.io.IOException;

/**
 * A class for keeping track of the directories that an instance lives in and under.
 * All the methods throw checked exception to avoid the inevitable NPE otherwise - 
 * when working with invalid directories...
 *
 * Example:
 * new InstanceDirs(new File("/glassfish4/glassfish/nodes/mymachine/instance1"));
 *
 * getInstanceDir()   == /glassfish4/glassfish/nodes/mymachine/instance1
 * getNodeAgentDir()  == /glassfish4/glassfish/nodes/mymachine
 * getNodeAgentsDir() == /glassfish4/glassfish/nodes
 * getInstanceName()  == instance1
 *
 *
 * @author Byron Nevins
 * @since 3.1
 * Created: April 19, 2010
 */
public final class InstanceDirs {

    /**
     * This constructor is used when the instance dir is known
     *
     * @param instanceDir The instance's directory
     * @throws IOException If any error including not having a grandparent directory.
     */
    public InstanceDirs(File theInstanceDir) throws IOException {
        dirs = new ServerDirs(theInstanceDir);

        if (dirs.getServerGrandParentDir() == null) {
            throw new IOException(ServerDirs.strings.get("InstanceDirs.noGrandParent", dirs.getServerDir()));
        }
    }

    /**
     * This constructor handles 0, 1, 2 or 3 null args.
     * It is smart enough to figure out many defaults.
     * @param nodeDirParent E.g. install-dir/nodes
     * @param nodeDir E.g. install-dir/nodes/localhost
     * @param instanceName E.g. i1
     */
    public InstanceDirs(String nodeDirParentPath, String nodeDirName, String instanceName) throws IOException {
        if (!StringUtils.ok(nodeDirParentPath))
            nodeDirParentPath = getNodeDirRootDefault();

        File nodeDirParent = new File(nodeDirParentPath);

        if (!nodeDirParent.isDirectory()) {
            dirs = null;
            throw new IOException(Strings.get("InstanceDirs.noNodeParent"));
        }

        File nodeDir;

        if (StringUtils.ok(nodeDirName))
            nodeDir = new File(nodeDirParent, nodeDirName);
        else
            nodeDir = getTheOneAndOnlyNode(nodeDirParent);

        if (!nodeDir.isDirectory()) {
            dirs = null;
            throw new IOException(Strings.get("InstanceDirs.badNodeDir", nodeDir));
        }

        File instanceDir;

        if (StringUtils.ok(instanceName))
            instanceDir = new File(nodeDir, instanceName);
        else
            instanceDir = getTheOneAndOnlyInstance(nodeDir);

        if (!instanceDir.isDirectory()) {
            dirs = null;
            throw new IOException(Strings.get("InstanceDirs.badInstanceDir", instanceDir));
        }

        // whew!!!

        dirs = new ServerDirs(instanceDir);
    }

    private File getTheOneAndOnlyNode(File parent) throws IOException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = parent.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return f != null && f.isDirectory();
            }
        });

        // ERROR:  No node dirs
        if (files == null || files.length < 1) {
            throw new IOException(
                    Strings.get("InstanceDirs.noNodes", parent));
        }
        // ERROR:  more than one node dir child
        if (files.length > 1) {
            throw new IOException(
                    Strings.get("InstanceDirs.tooManyNodes", parent, files.length));
        }

        // the usual case -- one node dir child
        return files[0];
    }

    private File getTheOneAndOnlyInstance(File nodeDir) throws IOException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = nodeDir.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return f != null && f.isDirectory() && !"agent".equals(f.getName());
            }
        });

        // ERROR:  No instance dirs
        if (files == null || files.length < 1) {
            throw new IOException(
                    Strings.get("InstanceDirs.noInstances", nodeDir));
        }
        // ERROR:  more than one instance dir
        if (files.length > 1) {
            throw new IOException(
                    Strings.get("InstanceDirs.tooManyInstances", files.length, nodeDir));
        }

        // the usual case -- one instance dir
        return files[0];
    }

    /**
     * Return the default value for nodeDirRoot, first checking if com.sun.aas.agentRoot
     * was specified in asenv.conf and returning this value. If not specified,
     * then the default value is the {GlassFish_Install_Root}/nodes.
     * nodeDirRoot is the parent directory of the node(s).
     *
     * @return String default nodeDirRoot - parent directory of node(s)
     * @throws CommandException if the GlassFish install root is not found
     */
    private String getNodeDirRootDefault() throws IOException {
        String nodeDirDefault = System.getProperty(
                SystemPropertyConstants.AGENT_ROOT_PROPERTY);

        if (StringUtils.ok(nodeDirDefault))
            return nodeDirDefault;

        String installRootPath = getInstallRootPath();
        return installRootPath + "/" + "nodes";
    }

    /**
     * Gets the GlassFish installation root (using property com.sun.aas.installRoot),
     *
     * @return path of GlassFish install root
     * @throws CommandException if the GlassFish install root is not found
     */
    protected String getInstallRootPath() throws IOException {
        String installRootPath = System.getProperty(
                SystemPropertyConstants.INSTALL_ROOT_PROPERTY);

        if (!StringUtils.ok(installRootPath))
            throw new IOException("noInstallDirPath");

        return installRootPath;
    }

    /**
     * Create a InstanceDir from the more general ServerDirs instance.
     * along with getServerDirs() you can convert freely back and forth
     *
     * @param aServerDir
     */
    public InstanceDirs(ServerDirs sd) {
        dirs = sd;
    }

    public final String getInstanceName() {
        return dirs.getServerName();
    }

    public final File getInstanceDir() {
        return dirs.getServerDir();
    }

    public final File getNodeAgentDir() {
        return dirs.getServerParentDir();
    }

    public final File getNodeAgentsDir() {
        return dirs.getServerGrandParentDir();
    }

    public final ServerDirs getServerDirs() {
        return dirs;
    }

    public final File getDasPropertiesFile() {
        return dirs.getDasPropertiesFile();
    }
    ///////////////////////////////////////////////////////////////////////////
    ///////////           All Private Below           /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    private final ServerDirs dirs;
}
