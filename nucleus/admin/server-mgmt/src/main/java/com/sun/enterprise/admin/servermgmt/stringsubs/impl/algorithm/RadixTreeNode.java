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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl.algorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Node for {@link RadixTree}.
 */
class RadixTreeNode {
    private static final Logger _logger = SLogger.getLogger();
    
    private static final LocalStringsImpl _strings = new LocalStringsImpl(RadixTreeNode.class);

    // Node key.
    private String _key;
    // Value of node.
    private String _value;
    // Associated child nodes.
    private Map<Character, RadixTreeNode> _childNodes;
    // Reference to parent node.
    private RadixTreeNode _parentNode;

    /**
     * Construct {@link RadixTreeNode} for the give key, value pair.
     */
    RadixTreeNode (String key, String value) {
        _key = key;
        _value = value;
    }

    /**
     * Get's the key.
     *
     * @return node key.
     */
    String getKey() {
        return _key;
    }

    /**
     * Set's the node key.
     *
     * @param key the key to set.
     */
    void setKey(String key) {
        this._key = key;
    }

    /**
     * Get's the node value.
     *
     * @return node value.
     */
    String getValue() {
        return _value;
    }

    /**
     * Set's the node value.
     *
     * @param value the value to set.
     */
    void setValue(String value) {
        this._value = value;
    }

    /**
     * Get's the parent node.
     *
     * @return the parentNode.
     */
    RadixTreeNode getParentNode() {
        return _parentNode;
    }

    /**
     * Get's the {@link Collection} of child nodes.
     * Returns empty {@link Collection} object if no child data found.
     *
     * @return associated child nodes.
     */
    Collection<RadixTreeNode> getChildNodes() {
        if (_childNodes != null) {
            return _childNodes.values();
        } else {
            List<RadixTreeNode> list  = Collections.emptyList();
            return  list;
        }
    }

    /**
     * Add's a child node.
     * <p>
     * NOTE: Addition of child with empty or null key is not allowed.
     * </p>
     *
     * @param node Node to add.
     */
    void addChildNode(RadixTreeNode node) {
        if (node == null || node._key == null || node._key.isEmpty()) {
            throw new IllegalArgumentException(_strings.get("errorInEmptyNullKeyInstertion"));
        }
        char c = node._key.charAt(0);
        if (_childNodes == null) {
            _childNodes = new HashMap<Character, RadixTreeNode>();
        }
        RadixTreeNode oldNode = _childNodes.put(c, node);
        if (oldNode != null) {
            _logger.log(Level.WARNING, SLogger.CHILD_NODE_EXISTS, 
            		new Object[] {this.toString(), oldNode.toString(), node.toString()});
            oldNode._parentNode = null;
        }
        node._parentNode = this;
    }

    /**
     * Removes a child node.
     *
     * @param node child node.
     */
    void removeChildNode(RadixTreeNode node) {
        if (node == null || node._key == null || node._key.isEmpty()) {
            throw new IllegalArgumentException(_strings.get("invalidNodeKey"));
        }
        char c = node._key.charAt(0);
        if (_childNodes != null) {
            RadixTreeNode matchedNode = _childNodes.get(c);
            if (matchedNode == node) {
                node = _childNodes.remove(c);
                node._parentNode = null;
            } else {
                throw new IllegalArgumentException(_strings.get("invalidChildNode", node, this));
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
        return _childNodes == null ? null : _childNodes.get(c);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Node Key : " + _key + ", Value : " + _value);
        return buffer.toString();
    }
}