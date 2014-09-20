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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import static org.glassfish.tests.utils.NucleusTestUtils.*;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author martinmares
 */
@Test(testName="ProgressStatusBasicTest")
public class ProgressStatusBasicTest {
    
    public void simple() {
        NadminReturn result = nadminWithOutput("progress-simple");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertEquals(12, prgs.size());
        for (int i = 0; i < 11; i++) {
            assertEquals(10 * i, prgs.get(i + 1).getValue());
            assertTrue(prgs.get(i + 1).isPercentage());
        }
        assertTrue(ProgressMessage.isNonDecreasing(prgs));
    }
    
    public void simpleNoTotal() {
        NadminReturn result = nadminWithOutput("progress-simple", "--nototalsteps");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        boolean nonPercentageExists = false;
        for (ProgressMessage prg : prgs) {
            if (prg.getValue() != 0 && prg.getValue() != 100) {
                assertFalse(prg.isPercentage());
                nonPercentageExists = true;
            }
        }
        assertTrue(nonPercentageExists);
        assertTrue(ProgressMessage.isNonDecreasing(prgs));
    }
    
    public void simpleSpecInAnnotation() {
        NadminReturn result = nadminWithOutput("progress-full-annotated");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertEquals(12, prgs.size());
        for (int i = 0; i < 11; i++) {
            assertEquals(10 * i, prgs.get(i + 1).getValue());
            assertTrue(prgs.get(i + 1).isPercentage());
        }
        assertTrue(ProgressMessage.isNonDecreasing(prgs));
        assertEquals("annotated:", prgs.get(5).getScope());
    }
    
    public void simpleTerse() {
        NadminReturn result = nadminWithOutput("--terse", "progress-simple");
        assertTrue(result.returnValue);
        List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
        assertTrue(prgs.isEmpty());
    }
    
//    public void commandWithPayloud() throws IOException {
//        File tmp = File.createTempFile(String.valueOf(System.currentTimeMillis()) + "ms_", "test");
//        FileWriter writer = null;
//        try {
//            writer = new FileWriter(tmp);
//            writer.write("This is testing file for nucleus admin tests.\n");
//            writer.write("Created - " + System.currentTimeMillis() + "\n");
//            writer.close();
//            NadminReturn result = nadminWithOutput("progress-payload", tmp.getCanonicalPath());
//            assertTrue(result.returnValue);
//            List<ProgressMessage> prgs = ProgressMessage.grepProgressMessages(result.out);
//            assertTrue(prgs.size() > 0);
//        } finally {
//            try {tmp.delete();} catch (Exception ex) {}
//        }
//    }
    
}
