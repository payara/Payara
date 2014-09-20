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

import static org.testng.Assert.assertEquals;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test class for {@link RadixTree}.
 */
public class TestRadixTree {

    private RadixTree _tree;

    @BeforeClass
    public void init() {
        _tree = new RadixTree();
        populateTree();
    }

    /**
     * Test insertion of null key in tree.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInsertionForNullKey() {
        _tree.insert(null, "value");
    }

    /**
     * Test insertion of null key in tree.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInsertionForEmptyKey() {
        _tree.insert(null, "value");
    }

    /**
     * Test the tree structure.
     */
    @Test
    public void testTreeStructure() {
        RadixTreeNode rootNode = _tree.getRootNode();

        // Validate root node
        assertEquals(rootNode.getKey(), "");
        assertEquals(rootNode.getValue(), null);
        assertEquals(rootNode.getParentNode(), null);

        // Validate first child node of rootNode.
        assertEquals(rootNode.getChildNodes().size(), 2);
        RadixTreeNode firstNode = rootNode.getChildNode('a');
        RadixTreeNode secondNode = rootNode.getChildNode('s');;

        Assert.assertNotNull(firstNode);
        assertEquals(firstNode.getParentNode(), rootNode);
        Assert.assertNull(firstNode.getValue());
        RadixTreeNode firstNodeFirstChild = firstNode.getChildNode('b');
        assertEquals(firstNodeFirstChild.getValue(), "abVal");
        assertEquals(firstNodeFirstChild.getParentNode(), firstNode);
        RadixTreeNode node = firstNodeFirstChild.getChildNode('e');
        assertEquals(node.getValue(), "abetVal");
        assertEquals(node.getKey(), "et");
        assertEquals(node.getParentNode(), firstNodeFirstChild);
        node = firstNodeFirstChild.getChildNode('a');
        assertEquals(node.getValue(), "abaitVal");
        assertEquals(node.getKey(), "ait");
        assertEquals(node.getParentNode(), firstNodeFirstChild);
        Assert.assertTrue(node.getChildNodes().isEmpty());
        RadixTreeNode firstNodeSecondChild = firstNode.getChildNode('c');
        assertEquals(firstNodeSecondChild.getValue(), "acidVal");
        assertEquals(firstNodeSecondChild.getParentNode(), firstNode);
        assertEquals(firstNodeSecondChild.getKey(), "cid");
        assertEquals(firstNodeSecondChild.getChildNodes().size(), 1);
        node = firstNodeSecondChild.getChildNode('i');
        assertEquals(node.getValue(), "acidicVal");
        assertEquals(node.getParentNode(), firstNodeSecondChild);
        Assert.assertTrue(node.getChildNodes().isEmpty());

        assertEquals(secondNode.getParentNode(), rootNode);
        Assert.assertNull(secondNode.getValue());
        RadixTreeNode secondNodeFirstChild = secondNode.getChildNode('i');
        assertEquals(secondNodeFirstChild.getValue(), "sickVal");
        assertEquals(secondNodeFirstChild.getParentNode(), secondNode);
        RadixTreeNode secondNodeSecondChild = secondNode.getChildNode('o');
        assertEquals(secondNodeSecondChild.getValue(), null);
        assertEquals(secondNodeSecondChild.getParentNode(), secondNode);
        assertEquals(rootNode.getChildNodes().size(), 2);
        node = secondNodeSecondChild.getChildNode('n');
        assertEquals(node.getValue(), "sonVal");
        assertEquals(node.getParentNode(), secondNodeSecondChild);
        node = secondNodeSecondChild.getChildNode('f');
        assertEquals(node.getValue(), "softVal");
        assertEquals(node.getKey(), "ft");
        assertEquals(node.getParentNode(), secondNodeSecondChild);
    }


    /**
     * Test insert and duplicate insert in RadixTree.
     * Test-case depends on another method as this method changes the tree
     * structure which may cause the failure for other tests.
     */
    @Test(dependsOnMethods = {"testTreeStructure"})
    public void testInsertExistingKey() {
        _tree.insert("sick", "newValue");
        assertEquals(_tree.getRootNode().getChildNode('s').getChildNode('i').getValue(), "newValue");
    }

    /**
     * Populate tree.
     */
    private void populateTree() {
        _tree.insert("acid", "acidVal");
        _tree.insert("son", "sonVal");
        _tree.insert("abet", "abetVal");
        _tree.insert("ab", "abVal");
        _tree.insert("sick", "sickVal");
        _tree.insert("abait", "abaitVal");
        _tree.insert("soft", "softVal");
        _tree.insert("acidic", "acidicVal");
    }
}