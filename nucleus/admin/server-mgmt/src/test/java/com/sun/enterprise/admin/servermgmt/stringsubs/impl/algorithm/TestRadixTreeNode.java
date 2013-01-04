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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test class for {@link RadixTreeNode}.
 */
public class TestRadixTreeNode {

    private static final String UNIT_TEST = "unitTest";

    /**
     * Test the node creation.
     */
    @Test
    public void testNodeCreation() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        Assert.assertEquals(rootNode.getKey(), UNIT_TEST);
        Assert.assertEquals(rootNode.getValue(), UNIT_TEST);
        rootNode.setKey("newTest");
        Assert.assertEquals(rootNode.getKey(), "newTest");
        rootNode.setValue("newValue");
        Assert.assertEquals(rootNode.getValue(), "newValue");
        Assert.assertNull(rootNode.getParentNode());
        Assert.assertTrue(rootNode.getChildNodes().isEmpty());
    }

    /**
     * Test the child node addition for <code>null</code> child.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAdditionForNullNode() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        rootNode.addChildNode(null);
    }

    /**
     * Test addition of child node having empty key.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAdditionForEmptyChildKey() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode("", "");
        rootNode.addChildNode(firstChildNode);
    }

    /**
     * Test addition of child node having <code>null</code> key.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAdditionForNullChildKey() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode(null, "");
        rootNode.addChildNode(firstChildNode);
    }

    /**
     * Test child node addition.
     */
    @Test
    public void testChildNodeAddition() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode("firstChildKey", "firstChildValue");
        rootNode.addChildNode(firstChildNode);
        Assert.assertTrue(rootNode.getChildNodes().size() == 1);
        Assert.assertEquals(rootNode.getChildNode('f'), firstChildNode);
    }

    /**
     * Test addition of duplicate child node.
     */
    @Test
    public void testDuplicateChildNodeAddition() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode("test", "oldtest");
        rootNode.addChildNode(firstChildNode);
        Assert.assertEquals(firstChildNode.getParentNode(), rootNode);
        Assert.assertEquals(rootNode.getChildNode('t'), firstChildNode);
        RadixTreeNode duplicateNode = new RadixTreeNode("test", "newtest");
        rootNode.addChildNode(duplicateNode);
        Assert.assertNull(firstChildNode.getParentNode());
        Assert.assertEquals(rootNode.getChildNode('t'), duplicateNode);
        Assert.assertTrue(rootNode.getChildNodes().size() == 1);
    }

    /**
     *  Test the child node removal for <code>null</code> child.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRemovalForNullNode() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        rootNode.removeChildNode(null);
    }

    /**
     * Test removal of child node having empty key.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRemovalForEmptyChildKey() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode("", "");
        rootNode.removeChildNode(firstChildNode);
    }

    /**
     * Test removal of child node having <code>null</code> key.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRemovalForNullChildKey() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode(null, "");
        rootNode.removeChildNode(firstChildNode);
    }

    /**
     * Test child node removal for invalid child node.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidChildNodeRemoval() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode("firstChildKey", "firstChildValue");
        rootNode.addChildNode(firstChildNode);
        RadixTreeNode invalidNode = new RadixTreeNode("InvalidChildKey", "InvalidChildValue");
        rootNode.removeChildNode(invalidNode);
    }  

    /**
     * Test child node removal.
     */
    @Test
    public void testChildNodeRemoval() {
        RadixTreeNode rootNode = new RadixTreeNode(UNIT_TEST, UNIT_TEST);
        RadixTreeNode firstChildNode = new RadixTreeNode("firstChildKey", "firstChildValue");
        rootNode.addChildNode(firstChildNode);
        Assert.assertTrue(rootNode.getChildNodes().size() == 1);
        Assert.assertEquals(rootNode.getChildNode('f'), firstChildNode);
        rootNode.removeChildNode(firstChildNode);
        Assert.assertTrue(rootNode.getChildNodes().isEmpty());
    }
}