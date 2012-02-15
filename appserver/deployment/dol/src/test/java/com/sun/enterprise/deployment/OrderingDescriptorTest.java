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

package com.sun.enterprise.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.enterprise.deployment.OrderingDescriptor;
import com.sun.enterprise.deployment.OrderingOrderingDescriptor;
import com.sun.enterprise.deployment.WebFragmentDescriptor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This class tests relative order sorting.
 * The first part of tests are ported from javax.faces TestFacesConfigOrdering.java.
 *
 * @author Shing Wai Chan
 */
public class OrderingDescriptorTest {
    private static final String OTHERS = "@others";

    public OrderingDescriptorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDocumentOrderingWrapperInit() {
        try {
            createWebFragmentDescriptor("myWf", new String[] { "A" }, new String[] { "A" });
            fail("Expected OrderingDescriptor to throw exception for before and after containing the same element");
        } catch(Exception ex) {
        }

        WebFragmentDescriptor wfDesc = createWebFragmentDescriptor(null, new String[] { "A" }, null);
        assertEquals("Expected WebFragmentDescriptor.getName() to return an empty String when no ID was specified. Received: " + wfDesc.getName(), "", wfDesc.getName());
        OrderingDescriptor orderingDesc = wfDesc.getOrderingDescriptor();
        assertNotNull(orderingDesc);
        Set<String> names = new HashSet<String>();
        names.add("A");
        assertEquals(names, orderingDesc.getAfter().getNames());
        assertNull(orderingDesc.getBefore());

    }

    @Test
    public void testAfterAfterOthersBeforeBeforeOthers() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { OTHERS, "C" }, null));
        wfs.add(createWebFragmentDescriptor("B", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("C", new String[] { OTHERS }, null));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        wfs.add(createWebFragmentDescriptor("E", null, null));
        wfs.add(createWebFragmentDescriptor("F", null, new String[] { "B", OTHERS }));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "F", "B", "D", "E", "C", "A" };
        validate(ids, wfs);
    }

    @Test
    public void testBeforeAfterOthersSorting() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor(null, new String[] { OTHERS }, new String[] { "C" }));
        wfs.add(createWebFragmentDescriptor("B", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("C", null, null));
        wfs.add(createWebFragmentDescriptor("D", new String[] { OTHERS }, null));
        wfs.add(createWebFragmentDescriptor("E", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("F", null, null));
        OrderingDescriptor.sort(wfs);
        //an alternative result from javax.faces
        //String[] ids = { "B", "E", "F", "", "C", "D" };
        String[] ids = { "E", "B", "F", "D", "", "C" };
        validate(ids, wfs);
    }

    @Test
    public void testAfterBeforeOthersSorting() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { OTHERS }, null));
        wfs.add(createWebFragmentDescriptor("B", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("C", null, null));
        wfs.add(createWebFragmentDescriptor("D", new String[] { OTHERS }, null));
        wfs.add(createWebFragmentDescriptor("E", new String[] { "C" }, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("F", null, null));
        OrderingDescriptor.sort(wfs);
        //an alternative result from javax.faces
        //String[] ids = { "B", "C", "E", "F", "A", "D" };
        String[] ids = { "C", "E", "B", "F", "D", "A" };
        validate(ids, wfs);
    }

    @Test
    public void testSpecSimple() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { "B" }, null));
        wfs.add(createWebFragmentDescriptor("B", null, null));
        wfs.add(createWebFragmentDescriptor("C", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        OrderingDescriptor.sort(wfs);
        //an alternative result from javax.faces
        //String[] ids = { "C", "B", "D", "A" };
        String[] ids = { "C", "D", "B", "A" };
        validate(ids, wfs);
    }

    @Test
    public void testBeforeIdAfterOthers() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, null));
        wfs.add(createWebFragmentDescriptor("B", null, null));
        wfs.add(createWebFragmentDescriptor("C", new String[] { OTHERS }, new String[] { "B" }));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "A", "D", "C", "B" };
        validate(ids, wfs);
    }

    @Test
    public void testAfterIdBeforeOthers() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, null));
        wfs.add(createWebFragmentDescriptor("B", null, null));
        wfs.add(createWebFragmentDescriptor("C", new String[] { "D" }, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "D", "C", "A", "B" };
        validate(ids, wfs);
    }

    @Test
    public void testAllAfterSpecificIds() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { "B" }, null));
        wfs.add(createWebFragmentDescriptor("B", new String[] { "C" }, null));
        wfs.add(createWebFragmentDescriptor("C", new String[] { "D" }, null));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "D", "C", "B", "A" };
        validate(ids, wfs);
    }

    @Test
    public void testAllBeforeSpecificIds() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, null));
        wfs.add(createWebFragmentDescriptor("B", null, new String[] { "A" }));
        wfs.add(createWebFragmentDescriptor("C", null, new String[] { "B" }));
        wfs.add(createWebFragmentDescriptor("D", null, new String[] { "C" }));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "D", "C", "B", "A" };
        validate(ids, wfs);
    }

    @Test
    public void testMixed1() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, null));
        wfs.add(createWebFragmentDescriptor("B", new String[] { "C" }, null));
        wfs.add(createWebFragmentDescriptor("C", null, new String[] { "B" }));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        OrderingDescriptor.sort(wfs);
        //an alternative result from javax.faces
        //String[] ids = { "A", "C", "D", "B" };
        String[] ids = { "C", "B", "A", "D" };
        validate(ids, wfs);
    }

    @Test
    public void testCyclic1() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, new String[] { "C" }));
        wfs.add(createWebFragmentDescriptor("B", null, new String[] { "A" }));
        wfs.add(createWebFragmentDescriptor("C", null, new String[] { "B" }));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        try {
            OrderingDescriptor.sort(wfs);
            fail("No exception thrown when circular document dependency is present");
        } catch(IllegalStateException ex) {
            // expected
            System.out.println("Expected exception: " + ex);
        }
    }

    @Test
    public void testCyclic2() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { "B" }, null));
        wfs.add(createWebFragmentDescriptor("B", new String[] { "C" }, null));
        wfs.add(createWebFragmentDescriptor("C", new String[] { "A" }, null));
        wfs.add(createWebFragmentDescriptor("D", null, null));
        try {
            OrderingDescriptor.sort(wfs);
            fail("No exception thrown when circular document dependency is present");
        } catch(IllegalStateException ex) {
            // expected
            System.out.println("Expected exception: " + ex);
        }
    }

    // ----- additional test cases

    @Test
    public void testCyclic3() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("B", new String[] { OTHERS } , new String[] { "A" }));
        wfs.add(createWebFragmentDescriptor("C", null, null));
        try {
            OrderingDescriptor.sort(wfs);
            fail("No exception thrown when circular document dependency is present");
        } catch(IllegalStateException ex) {
            // expected
            System.out.println("Expected exception: " + ex);
        }
    }

    @Test
    public void testCyclic4() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", null, new String[] { "B" }));
        wfs.add(createWebFragmentDescriptor("B", null, new String[] { "C" }));
        wfs.add(createWebFragmentDescriptor("C", null, new String[] { "D" }));
        wfs.add(createWebFragmentDescriptor("D", null, new String[] { "A" }));
        wfs.add(createWebFragmentDescriptor("E", null, null));
        
        try {
            OrderingDescriptor.sort(wfs);
            fail("No exception thrown when circular document dependency is present");
        } catch(IllegalStateException ex) {
            // expected
            System.out.println("Expected exception: " + ex);
        }
    }

    @Test
    public void testCircleWithEmptyOthers() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { "C" }, null));
        wfs.add(createWebFragmentDescriptor("B", new String[] { "A" }, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("C", new String[] { OTHERS }, null));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "C", "A", "B" };
        validate(ids, wfs);
    }

    @Test
    public void testDisconnectedGraphWithBeforeOthers() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("C", null, new String[] { OTHERS }));
        wfs.add(createWebFragmentDescriptor("A", new String[] { "B" }, null));
        wfs.add(createWebFragmentDescriptor("B", null, null));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "C", "B", "A" };
        validate(ids, wfs);
    }

    @Test
    public void testDisconnectedGraphWithAfterOthers() {
        List<WebFragmentDescriptor> wfs = new ArrayList<WebFragmentDescriptor>();
        wfs.add(createWebFragmentDescriptor("A", new String[] { "B" }, null));
        wfs.add(createWebFragmentDescriptor("B", null, null));
        wfs.add(createWebFragmentDescriptor("C", new String[] { OTHERS }, null));
        OrderingDescriptor.sort(wfs);
        String[] ids = { "B", "A" , "C" };
        validate(ids, wfs);
    }

    // ----- private methods
 
    private WebFragmentDescriptor createWebFragmentDescriptor(String name, String[] after, String[] before) {
        WebFragmentDescriptor wfDesc = new WebFragmentDescriptor();
        if (name != null) {
            wfDesc.setName(name);
        }

        boolean hasAfter = (after != null && after.length > 0);
        boolean hasBefore = (before != null && before.length > 0);
        if (hasAfter || hasBefore) {
            OrderingDescriptor orderDesc = new OrderingDescriptor();
            wfDesc.setOrderingDescriptor(orderDesc);
            orderDesc.setAfter(createOrderingOrderingDescriptor(after));
            orderDesc.setBefore(createOrderingOrderingDescriptor(before));
        }

        return wfDesc;
    }

    private OrderingOrderingDescriptor createOrderingOrderingDescriptor(String[] order) {
        OrderingOrderingDescriptor orderingOrderingDesc = null;
        if (order != null && order.length > 0) {
            orderingOrderingDesc = new OrderingOrderingDescriptor();
            for (String n : order) {
                if (OTHERS.equals(n)) {
                    orderingOrderingDesc.addOthers();
                } else {
                    orderingOrderingDesc.addName(n);
                }
            }
        }

        return orderingOrderingDesc;
    }

    private void validate(String[] ids, List<WebFragmentDescriptor> wfs) {
        StringBuilder namesBuilder = new StringBuilder("[");
        for (int j = 0; j < wfs.size(); j++) {
            if (j > 0) {
                namesBuilder.append(", ");
            }
            namesBuilder.append(wfs.get(j).getName());
        }
        namesBuilder.append("]");
        String namesStr = namesBuilder.toString();
        String expectedNamesStr = Arrays.asList(ids).toString();

        for (int i = 0; i < wfs.size(); i++) {
            assertEquals("Expected " + expectedNamesStr + ", but received " + namesStr + ", mismatch at index " + i,
                   ids[i], wfs.get(i).getName());
        }
    }
}
