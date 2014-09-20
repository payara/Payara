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
import org.glassfish.config.support.*;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.DuckTyped;

import java.beans.PropertyVetoException;
import java.util.List;


/**
 * Nodes configuration. Maintain a list of {@link Node}
 * active configurations.
 */
@Configured
public interface Nodes extends ConfigBeanProxy {

    /**
     * Sets the value of the freeze attribute on the nodes list.
     * If the nodes list is frozen then no new nodes are allowed
     * to be created.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setFreeze(String value) throws PropertyVetoException;

    /**
     * Check if nodes list is frozen. That is prevent creation of new nodes.
     *
     * @return value of freeze
     */
    @Attribute(defaultValue = "false", dataType=Boolean.class)
    String getFreeze();

     /**
      * Return the list of nodes currently configured
      *
      * @return list of {@link Node }
      */
    @Element
    @Create(value="_create-node", decorator=Node.Decorator.class, i18n=@I18n("_create.node.command") )
    @Delete(value="_delete-node", resolver= TypeAndNameResolver.class,
            decorator=Node.DeleteDecorator.class,
            i18n=@I18n("delete.node.command"))

    public List<Node> getNode();
    
    /**
     * Return the default local node, localhost-<domain_name>, or null if no such node exists.
     *
     * @return          the Node object, or null if no such node
     */
    @DuckTyped
    public Node getDefaultLocalNode();

    /**
     * Return the node with the given name, or null if no such node exists.
     *
     * @param   name    the name of the node
     * @return          the Node object, or null if no such node
     */
    @DuckTyped
    public Node getNode(String name);

    /**
     * Can we create a node?
     * @param node
     * @return true if node creation is allowed, else false
     */
    @DuckTyped
    public boolean nodeCreationAllowed();

    class Duck {
        public static Node getNode(Nodes nodes, String name) {
            if (name == null || nodes == null) {
                return null;
            }
            for (Node node : nodes.getNode()) {
                if (node.getName().equals(name)) {
                    return node;
                }
            }
            return null;
        }

        public static Node getDefaultLocalNode(Nodes nodes) {
            if (nodes == null) {
                return null;
            }
            Dom serverDom = Dom.unwrap(nodes);
            Domain domain = serverDom.getHabitat().getService(Domain.class);
            for (Node node : nodes.getNode()) {
                if (node.getName().equals("localhost-"+domain.getName())) {
                    return node;
                }
            }
            return null;
        }

        public static boolean nodeCreationAllowed(Nodes nodes) {
            return ! Boolean.parseBoolean(nodes.getFreeze());
        }
    }
}
