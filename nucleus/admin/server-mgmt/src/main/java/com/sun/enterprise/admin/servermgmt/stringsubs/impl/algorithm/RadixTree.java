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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * This class implements the operation of a radix tree. A radix tree
 * is a specialized set data structure based on the tree/trie that is
 * used to store a set of strings. The key for nodes of a radix tree
 * are labeled with one or more characters rather than only a single
 * characters.
 */
class RadixTree {

    private static final Logger _logger = SLogger.getLogger();
    private static final LocalStringsImpl _strings = new LocalStringsImpl(RadixTree.class);
    // Reference to root node.
    private RadixTreeNode _rootNode;

    /**
     * Construct {@link RadixTree} with default root node.
     */
    public RadixTree() {
        // Creating root node.
        _rootNode = new RadixTreeNode("", null);
    }

    /**
     * Insert a new entry in tree with the given key, value pair.
     * <p>
     * <b>NOTE:</b> 
     *   <li>
     *     If user tries to insert the duplicate key than the
     *     value of that key will be updated.
     *   </li>
     *   <li> 
     *     Insertion of node having empty or null key is not allowed.
     *   </li>
     * </p>
     * <br/>
     * @param key The input key.
     * @param value The value that need to be stored corresponding
     *  to the given key.
     * @return <code>true</code> if key inserted successfully.<br/>
     *         <code>false</code> if insertion failed.
     */
    public void insert(String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(_strings.get("errorInEmptyNullKeyInstertion"));
        }
        char[] inputChars = key.toCharArray();
        int noOfMatchedChars = 0;
        RadixTreeNode node = _rootNode;
        RadixTreeNode newNode = null;
        int keyLength = inputChars.length;
        OUTER_LOOP : while (noOfMatchedChars < keyLength) {
            String nodeKey = node.getKey();
            int i = 0;
            int maxLoop = nodeKey.length() > (keyLength - noOfMatchedChars) ? keyLength - noOfMatchedChars :
                nodeKey.length();
            for (; i < maxLoop ; i++) {
                if (nodeKey.charAt(i) != inputChars[noOfMatchedChars]) {
                    // e.g new key/value : successive/successive
                    // before, 
                    // |-node (key: successful, value: successful)
                    // | |-node_childs...
                    //after...
                    // |-newNode (key: success, value: "")
                    // | |-node (key: ful, value: successful)
                    // | | |-node_childs...
                    // | |-secondNewNode (key: ive, value: successive)
                    newNode = new RadixTreeNode(nodeKey.substring(0, i), null);
                    RadixTreeNode parentNode = node.getParentNode();
                    parentNode.removeChildNode(node);
                    parentNode.addChildNode(newNode);
                    node.setKey(nodeKey.substring(i));
                    newNode.addChildNode(node);
                    newNode.addChildNode(new RadixTreeNode(key.substring(noOfMatchedChars), value));
                    break OUTER_LOOP;
                }
                noOfMatchedChars++;
            }

            // If the given key is smaller than the matched node key
            if (nodeKey.length() > maxLoop) {
                // e.g new key/value : acid/acid
                // before, 
                // |-node (key: acidic, value: acidic)
                // | |-node_childs...
                //after...
                // |-newNode (key: acid, value: acid)
                // | |-node (key: ic, value: acidic)
                // | | |-node_childs...
                newNode = new RadixTreeNode(nodeKey.substring(0, i), value);
                RadixTreeNode parentNode = node.getParentNode();
                parentNode.removeChildNode(node);
                parentNode.addChildNode(newNode);
                node.setKey(nodeKey.substring(i));
                newNode.addChildNode(node);
                break;
            }

            if (noOfMatchedChars == keyLength) {
                if (node.getValue() != null && !node.getValue().isEmpty()) {
                    _logger.log(Level.INFO, SLogger.CHANGE_IN_VALUE, new Object[] {node.getValue(), value});
                }
                node.setValue(value);
                break;
            }

            RadixTreeNode matchedNode = node.getChildNode(inputChars[noOfMatchedChars]);
            // Add as a child node if no match found.
            if (matchedNode == null) {
                node.addChildNode(new RadixTreeNode(key.substring(noOfMatchedChars), value));
                break;
            }
            node = matchedNode;
        } 
    }

    /**
     * Return's the root node helps to traverse the tree.
     * 
     * @return Root node of tree.
     */
    RadixTreeNode getRootNode() {
        return _rootNode;
    }
}