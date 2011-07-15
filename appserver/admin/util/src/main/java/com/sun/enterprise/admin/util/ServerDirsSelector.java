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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.util.io.DomainDirs;
import com.sun.enterprise.util.io.InstanceDirs;
import com.sun.enterprise.util.io.ServerDirs;
import java.io.File;
import java.io.IOException;
import org.glassfish.api.admin.CommandException;

/**
 * Based on the presence or absence of values for:
 * <ul>
 * <li>domain directory parent (e.g., ${installDir}/domains
 * <li>server name (domain or instance)
 * <li>node directory
 * <li>node name
 * </ul>
 *
 * select the correct directories object for the requested domain or node.
 *
 * @author Byron Nevins
 * @author Tim Quinn (just refactoring, rearranging the code to here)
 */
public class ServerDirsSelector {

    private File userSpecifiedDomainDirParent;
    private String userSpecifiedServerName;
    private String userSpecifiedNodeDir;           // nodeDirRoot
    private String userSpecifiedNode;

    private DomainDirs domainDirs = null;
    private InstanceDirs instanceDirs = null;


    /**
     * Creates a selector for choosing the correct set of directories.
     * @param domainDirParent parent of the domain directory file(s)
     * @param serverName name of the requested instance or domain
     * @param nodeDir path to the node directory
     * @param node name of the node
     * @return
     * @throws CommandException
     * @throws IOException
     */
    public static ServerDirsSelector getInstance(
            final File domainDirParent,
            final String serverName,
            final String nodeDir,
            final String node) throws CommandException, IOException {

        final ServerDirsSelector helper = new ServerDirsSelector(
                domainDirParent,
                serverName,
                nodeDir,
                node);

        helper.validateDomainOrInstance();
        return helper;
    }

    public ServerDirs dirs() {
        return selectDirs();
    }

    private ServerDirsSelector(
            final File domainDirParent,
            final String serverName,
            final String nodeDir,
            final String node) {
        userSpecifiedDomainDirParent = domainDirParent;
        userSpecifiedServerName = serverName;
        userSpecifiedNodeDir = nodeDir;
        userSpecifiedNode = node;
    }


    /**
     * make sure the parameters make sense for either an instance or a domain.
     */
    private void validateDomainOrInstance() throws CommandException, IOException {

        
        // case 1: since ddp is specified - it MUST be a domain
        if (userSpecifiedDomainDirParent != null) {
            domainDirs = new DomainDirs(userSpecifiedDomainDirParent, userSpecifiedServerName);
        }
        //case 2: if either of these are set then it MUST be an instance
        else if (userSpecifiedNode != null || userSpecifiedNodeDir != null) {
            instanceDirs = new InstanceDirs(userSpecifiedNodeDir, userSpecifiedNode, userSpecifiedServerName);
        }
        // case 3: nothing is specified -- use default domain as in v3.0
        else if (userSpecifiedServerName == null) {
            domainDirs = new DomainDirs(userSpecifiedDomainDirParent, userSpecifiedServerName);
        }
        // case 4: userSpecifiedServerName is set and the other 3 are all null
        // we need to figure out if it's a DAS or an instance
        else {
            try {
                domainDirs = new DomainDirs(userSpecifiedDomainDirParent, userSpecifiedServerName);
                return;
            }
            catch (IOException e) {
                // handled below
            }

            instanceDirs = new InstanceDirs(userSpecifiedNodeDir, userSpecifiedNode, userSpecifiedServerName);
        }
    }

    public boolean isInstance() {
        return instanceDirs != null;
    }

    private ServerDirs selectDirs() {
        if (isInstance())
                return instanceDirs.getServerDirs();
            else
                return domainDirs.getServerDirs();
    }
}
