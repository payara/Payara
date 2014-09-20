/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.nucleus.admin.progress;

import static org.glassfish.tests.utils.NucleusTestUtils.*;
import static org.testng.AssertJUnit.*;
import java.util.List;
import org.glassfish.tests.utils.NucleusTestUtils;
import org.testng.annotations.Test;

/**
 *
 * @author martinmares
 */
@Test(testName="ProgressStatusComplexTest")
public class ProgressStatusComplexTest {
    
    public void executeCommandFromCommand() {
        NucleusTestUtils.NadminReturn result = nadminWithOutput("progress-exec-other");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertArrayEquals(new String[]{"Starting", "Preparing", "Parsing", 
            "Working on main part", "Cleaning", "Finished", 
            "Finishing outer command", "Finished outer command"}, ProgressMessage.uniqueMessages(prgs));
    }
    
    public void executeCommandWithSupplements() {
        NucleusTestUtils.NadminReturn result = nadminWithOutput("progress-supplement");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertArrayEquals(new String[]{"Starting", "2 seconds supplemental command",
            "Parsing", "Working on main part", "Finished",
            "3 seconds supplemental"}, ProgressMessage.uniqueMessages(prgs));
        assertTrue(prgs.size() > 10);
        assertFalse(prgs.get(4).isPercentage());
        assertTrue(prgs.get(10).isPercentage());
        assertTrue(ProgressMessage.isNonDecreasing(prgs));
    }
    
    public void executeVeryComplexCommand() {
        NucleusTestUtils.NadminReturn result = nadminWithOutput("progress-complex");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertTrue(prgs.size() > 40);
        assertTrue(scopeCount(prgs, "complex:") >= 4);
        assertEquals(0, scopeCount(prgs, "complex.ch1:"));
        assertEquals(5, scopeCount(prgs, "complex.ch2-paral:"));
        assertEquals(4, scopeCount(prgs, "complex.ch3:"));
        assertEquals(5, scopeCount(prgs, "complex.ch1.ch11:"));
        assertEquals(6, scopeCount(prgs, "complex.ch1.ch12:"));
        assertEquals(25, scopeCount(prgs, "complex.ch2-paral.ch21:"));
        assertEquals(25, scopeCount(prgs, "complex.ch2-paral.ch22:"));
        assertEquals(25, scopeCount(prgs, "complex.ch2-paral.ch23:"));
        assertEquals(25, scopeCount(prgs, "complex.ch2-paral.ch24:"));
        assertEquals(5, scopeCount(prgs, "complex.ch3.ch31:"));
        assertEquals(5, scopeCount(prgs, "complex.ch3.ch32:"));
        assertTrue(ProgressMessage.isNonDecreasing(prgs));
    }
    
    private int scopeCount(List<ProgressMessage> prgs, String scope) {
        int result = 0;
        for (ProgressMessage prg : prgs) {
            if (scope.equals(prg.getScope())) {
                result++;
            }
        }
        return result;
    }
    
}
