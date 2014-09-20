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

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Node;
import org.glassfish.internal.api.ServerContext;
import com.sun.enterprise.util.io.InstanceDirs;


import java.io.File;
import java.io.IOException;


final class InstanceDirUtils{
    Node node;
    ServerContext serverContext;


    InstanceDirUtils(Node node, ServerContext serverContext){
        this.node = node;
        this.serverContext = serverContext;
    }

    /**
     * Returns the directory for the selected instance that is on the local
     * system.
     * @param instanceName name of the instance
     * @return File for the local file system location of the instance directory
     * @throws IOException
     */
    File getLocalInstanceDir(String instance) throws IOException {
        /*
         * Pass the node directory parent and the node directory name explicitly
         * or else InstanceDirs will not work as we want if there are multiple
         * nodes registered on this node.
         *
         * If the configuration recorded an explicit directory for the node,
         * then use it.  Otherwise, use the default node directory of
         * ${installDir}/glassfish/nodes/${nodeName}.
         */
        String nodeDir = node.getNodeDirAbsolute();
        final File nodeDirFile = (nodeDir != null ?
            new File(nodeDir) :
            defaultLocalNodeDirFile());
        InstanceDirs instanceDirs = new InstanceDirs(nodeDirFile.toString(), node.getName(), instance);
        return instanceDirs.getInstanceDir();
    }

    File defaultLocalNodeDirFile() {
        /*
         * The "nodes" directory we want to use is a child of
         * the install directory.
         *
         * The installDir field contains the installation directory which the
         * administrator specified, if s/he specified one, when the target node
         * was first created.  It is null if the administrator did not specify
         * an installation directory for the node.  In that case we should
         * use the DAS's install directory (because this method applies in the
         * local instance case).
         */
        String installDir = node.getInstallDir();
        final File nodeParentDir = (
                installDir == null
                    ? serverContext.getInstallRoot()
                    : new File(installDir, "glassfish"));
        return new File(nodeParentDir, "nodes");
    }
}
