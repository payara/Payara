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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.cluster.NodeInfo;
import java.util.logging.Logger;
import java.util.List;
import java.util.LinkedList;


public class ListNodesHelper {


    private Servers servers;

    private static final String EOL = "\n";

    String listType;
    boolean long_opt;
    boolean terse;
    List<Node>  nodeList;
    List<NodeInfo> infos = new LinkedList<NodeInfo>();

    public ListNodesHelper(Logger _logger, Servers servers, Nodes nodes, String type, boolean long_opt, boolean terse) {
        this.listType = type;
        this.long_opt = long_opt;
        this.terse = terse;
        this.servers = servers;
        nodeList=nodes.getNode();
        infos = new LinkedList<NodeInfo>();
    }

    public String getNodeList() {

        StringBuilder sb = new StringBuilder();
        boolean firstNode = true;

        for (Node n : nodeList) {

            String name = n.getName();
            String nodeType = n.getType();
            String host = n.getNodeHost();
            String installDir = n.getInstallDir();

            if (!listType.equals(nodeType) && !listType.equals("ALL")) {
                continue;
            }

            if (firstNode) {
                firstNode = false;
            } else {
                sb.append(EOL);
            }

            if (terse) {
                sb.append(name);
            } else if (!long_opt) {
                sb.append(name).append("  ").append(nodeType).append("  ").append(host);
            }

            if (long_opt){
                List<Server> serversOnNode = servers.getServersOnNode(n);
                StringBuilder instanceList = new StringBuilder();
                if (!serversOnNode.isEmpty()) {
                    int i = 0;
                    for (Server server: serversOnNode){
                        if (i > 0)
                            instanceList.append(", ");
                        instanceList.append(server.getName());
                        i++;
                    }
                }
                NodeInfo ni = new NodeInfo(name, host, installDir, nodeType, instanceList.toString());
                infos.add(ni);
            }
        }
        if (long_opt) {
            return  NodeInfo.format(infos);
        } else {
            return sb.toString();
        }

    }
}
