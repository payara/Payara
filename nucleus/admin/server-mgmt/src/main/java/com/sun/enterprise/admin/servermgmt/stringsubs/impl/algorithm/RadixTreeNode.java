/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt.stringsubs.impl.algorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Node for {@link RadixTree}.
 */
class RadixTreeNode {
    private static final Logger LOGGER = SLogger.getLogger();
    
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(RadixTreeNode.class);

    /** Node key. */
    private String key;
    /** Value of node. */
    private String value;
    /** Associated child nodes. */
    private Map<Character, RadixTreeNode> childNodes;
    /** Reference to parent node. */
    private RadixTreeNode parentNode;

    /**
     * Construct {@link RadixTreeNode} for the give key, value pair.
     */
    RadixTreeNode (String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key.
     *
     * @return node key.
     */
    String getKey() {
        return key;
    }

    /**
     * Sets the node key.
     *
     * @param key the key to set.
     */
    void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the node value.
     *
     * @return node value.
     */
    String getValue() {
        return value;
    }

    /**
     * Sets the node value.
     *
     * @param value the value to set.
     */
    void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the parent node.
     *
     * @return the parentNode.
     */
    RadixTreeNode getParentNode() {
        return parentNode;
    }

    /**
     * Gets the {@link Collection} of child nodes.
     * Returns empty {@link Collection} object if no child data found.
     *
     * @return associated child nodes.
     */
    Collection<RadixTreeNode> getChildNodes() {
        if (childNodes != null) {
            return childNodes.values();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Adds a child node.
     * <p>
     * NOTE: Addition of child with empty or null key is not allowed.
     * </p>
     *
     * @param node Node to add.
     */
    void addChildNode(RadixTreeNode node) {
        if (node == null || node.key == null || node.key.isEmpty()) {
            throw new IllegalArgumentException(STRINGS.get("errorInEmptyNullKeyInstertion"));
        }
        char c = node.key.charAt(0);
        if (childNodes == null) {
            childNodes = new HashMap<Character, RadixTreeNode>();
        }
        RadixTreeNode oldNode = childNodes.put(c, node);
        if (oldNode != null) {
            LOGGER.log(Level.WARNING, SLogger.CHILD_NODE_EXISTS, new Object[] {this.toString(), oldNode.toString(), node.toString()});
            oldNode.parentNode = null;
        }
        node.parentNode = this;
    }

    /**
     * Removes a child node.
     *
     * @param node child node.
     */
    void removeChildNode(RadixTreeNode node) {
        if (node == null || node.key == null || node.key.isEmpty()) {
            throw new IllegalArgumentException(STRINGS.get("invalidNodeKey"));
        }
        char c = node.key.charAt(0);
        if (childNodes != null) {
            RadixTreeNode matchedNode = childNodes.get(c);
            if (matchedNode == node) {
                node = childNodes.remove(c);
                node.parentNode = null;
            } else {
                throw new IllegalArgumentException(STRINGS.get("invalidChildNode", node, this));
            }
        }
    }

    /**
     * Gets a child node associated with a given character.
     * 
     * @param c input char to retrieve associated child node.
     * @return The associated node or <code>null</code> if no association found.
     */
    RadixTreeNode getChildNode(char c) {
        return childNodes == null ? null : childNodes.get(c);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Node Key : ").append(key).append(", Value : ").append(value);
        return buffer.toString();
    }
}