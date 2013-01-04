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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test class for {@link RadixTreeSubstitution}.
 */
@Test
public class TestRadixTreeSubstitution {

    private RadixTree _tree;
    private RadixTreeSubstitution _substitution;

    @BeforeClass
    public void init() {
        _tree = new RadixTree();
        _substitution = new RadixTreeSubstitution(_tree);
        populateTree();
    }

    /**
     * Test algorithm instance for null tree. 
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSubstitutionForNullTree() {
        new RadixTreeSubstitution(null);
    }

    /**
     * Test substitution for an extended match.
     */
    @Test
    public void testInputExtendedMatch() {
        assertEquals(callSubstitution("abe"), "abVale");
        assertEquals(callSubstitution("acidic acidi"), "acidicVal acidVali");
    }

    /**
     * Test if the last word in the input string matched completely.
     */
    @Test
    public void testLastWordExactlyMatched() {
        assertEquals(callSubstitution("  aci so abet ... &&& soft"), "  aci so abetVal ... &&& softVal");
    }

    /**
     * Test substitution for continuous input i.e input without any space.
     */
    @Test
    public void testInputWithoutSpace() {
        assertEquals(callSubstitution("acidysonacidyso"), "acidValysonValacidValyso");
    }

    /**
     * Test substitution if last word partially matched.
     */
    @Test
    public void testLastWordPartiallyMatched() {
        assertEquals(callSubstitution("acidic abet softy"), "acidicVal abetVal softValy");
    }

    /**
     * Test substitution if nothing matched in the given input.
     */
    @Test
    public void testUnmatchedInput() {
        assertEquals(callSubstitution("@##ttt {{}:P"), "@##ttt {{}:P");
    }

    /**
     * Test substitution for multiple scenarios.
     * <li>Maintaining the last matching node value, and using the same if no extended match found</li>
     * <li>Covering multiple scenarios for the last word (partially/fully or looking for extended match)</li>
     * <li>Run time tree modification and checking the substitution output.</li>
     * 
     * <p>Test-case depends on another methods as this method changes the tree reference 
     *  which will cause the failure for other test cases.</p>
     */
    @Test(dependsOnMethods = {"testLastWordPartiallyMatched", "testLastWordExactlyMatched",
            "testInputWithoutSpace", "testInputExtendedMatch"})
    public void testBackTrack() {
        _tree = new RadixTree();
        _tree.insert("acidicity", "acidicityVal");
        _tree.insert("acidical", "acidicalVal");
        _tree.insert("acid", "acidVal");
        _substitution = new RadixTreeSubstitution(_tree);
        assertEquals(callSubstitution("acidicit acidicit"), "acidValicit acidValicit");
        _tree.insert("c", "cVal");
        assertEquals(callSubstitution("acidicit acidicit"), "acidValicValit acidValicValit");
        _tree.insert("ci", "ciVal");
        assertEquals(callSubstitution("acidicit acidicit"), "acidValiciValt acidValiciValt");
        _tree.insert("t", "tVal");
        assertEquals(callSubstitution("acidicit"), "acidValiciValtVal");
        _tree.insert("icit", "icitVal");
        assertEquals(callSubstitution("acidicit acidicit"), "acidValicitVal acidValicitVal");
    }

    /**
     * Calls the algorithm and return the replacement for the
     * given input string.
     *
     * @param input input string for substitution.
     * @return substituted string.
     */
    private String callSubstitution(String input) {
        StringBuffer outputBuffer = new StringBuffer();
        String substitution = null;
        for (char c : input.toCharArray()) {
            substitution = _substitution.substitute(c);
            if (substitution != null) {
                outputBuffer.append(substitution);
            }
        }
        substitution = _substitution.substitute(null);
        if (substitution != null) {
            outputBuffer.append(substitution);
        }
        return outputBuffer.toString();
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