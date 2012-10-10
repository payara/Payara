/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.I18n;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.TypeAndNameResolver;
import org.glassfish.config.support.TypeResolver;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.DuckTyped;

import java.util.List;
import java.util.ArrayList;

/**
 * List of configured servers.
 */
@Configured
public interface Servers extends ConfigBeanProxy  {

    /**
     * Return the list of currently configured server. Servers can
     * be added or removed by using the returned {@link java.util.List}
     * instance
     * 
     * @return the list of configured {@link Server}
     */
    @Element
    // example below on how to annotate a CRUD command with cluster specific data.
    @Create(value="_register-instance", resolver= TypeResolver.class, decorator= Server.CreateDecorator.class,
        cluster=@org.glassfish.api.admin.ExecuteOn(value = RuntimeType.DAS),
        i18n=@I18n("_register.instance.command"))
    @Delete(value="_unregister-instance", resolver= TypeAndNameResolver.class,
            decorator=Server.DeleteDecorator.class,
            cluster=@org.glassfish.api.admin.ExecuteOn(value = {RuntimeType.DAS,RuntimeType.INSTANCE}),
            i18n=@I18n("_unregister.instance.command"))    
    public List<Server> getServer();

    /**
     * Return the server with the given name, or null if no such server exists.
     *
     * @param   name    the name of the server
     * @return          the Server object, or null if no such server
     */
    @DuckTyped
    public Server getServer(String name);

    /**
     * Return the list of Servers that reference a Node
     *
     * @param   node    Node to get servers that reference
     * @return          List of Server objects that reference the passed node.
     *                  List will be of length 0 if no servers reference node.
     */
    @DuckTyped
    public List<Server> getServersOnNode(Node node);

    class Duck {
        public static Server getServer(Servers instance, String name) {
            for (Server server : instance.getServer()) {
                if (server.getName().equals(name)) {
                    return server;
                }
            }
            return null;
        }

        public static List<Server> getServersOnNode(Servers servers, Node node) {
            List<Server> serverList = servers.getServer();
            List<Server> serverListOnNode = new ArrayList<Server>();
            Server instance = null;
            String nodeName = node.getName();
            if (serverList.size() > 0) {
                for (Server server: serverList){
                    if (nodeName.equals(server.getNodeRef())){
                        serverListOnNode.add(server);
                    }
                }
            }
            return serverListOnNode;
        }

    }
}
