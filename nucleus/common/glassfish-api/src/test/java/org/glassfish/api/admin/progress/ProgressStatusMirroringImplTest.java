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
package org.glassfish.api.admin.progress;

import org.glassfish.api.admin.ProgressStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author mmares
 */
public class ProgressStatusMirroringImplTest {
    
    private DummyParent parent;
    
    public ProgressStatusMirroringImplTest() {
    }

//    @BeforeClass
//    public static void setUpClass() throws Exception {
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Exception {
//    }
    
    @Before
    public void setUp() {
        parent = new DummyParent();
    }
    
    @Test
    public void testTotalStepCount() {
        ProgressStatusMirroringImpl prog = new ProgressStatusMirroringImpl("first", parent, null);
        assertEquals(-1, prog.getTotalStepCount());
        assertEquals(0, prog.currentStepCount);
        ProgressStatus ch1 = prog.createChild("A1", 0);
        assertNotNull(ch1);
        ProgressStatus ch2 = prog.createChild("A2", 0);
        assertNotNull(ch2);
        ProgressStatus chm = prog.createChild(null, 0, 10);
        assertNotNull(chm);
        assertEquals(-1, prog.getTotalStepCount());
        assertEquals(0, prog.currentStepCount);
        ch1.setTotalStepCount(4);
        assertEquals(-1, prog.getTotalStepCount());
        assertEquals(0, prog.currentStepCount);
        ch2.setTotalStepCount(6);
        assertEquals(20, prog.getTotalStepCount());
        assertEquals(0, prog.currentStepCount);
        prog = new ProgressStatusMirroringImpl("second", parent, null);
        assertEquals(-1, prog.getTotalStepCount());
        ch1 = prog.createChild("A1", 0, 10);
        assertEquals(10, prog.getTotalStepCount());
        ch2 = prog.createChild("A2", 0);
        assertEquals(-1, prog.getTotalStepCount());
    }
    
    @Test
    public void testProgress() {
        ProgressStatusMirroringImpl prog = new ProgressStatusMirroringImpl("first", parent, null);
        ProgressStatus ch1 = prog.createChild("A1", 0);
        assertNotNull(ch1);
        ProgressStatus ch2 = prog.createChild("A2", 0);
        assertNotNull(ch2);
        assertEquals(0, prog.currentStepCount);
        parent.lastEvent = null;
        ch1.progress(1);
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        assertEquals(1, prog.currentStepCount);
        ch2.progress(2, "Some message");
        assertEquals(3, prog.currentStepCount);
    }
    
    @Test
    public void testComplete() {
        ProgressStatusMirroringImpl prog = new ProgressStatusMirroringImpl("first", parent, null);
        ProgressStatus ch1 = prog.createChild("A1", 0, 10);
        assertNotNull(ch1);
        ProgressStatus ch2 = prog.createChild("A2", 0, 20);
        assertNotNull(ch2);
        assertEquals(0, prog.currentStepCount);
        ch1.progress(2);
        ch2.progress(3);
        assertEquals(25, prog.getRemainingStepCount());
        assertFalse(prog.isComplete());
        assertFalse(ch1.isComplete());
        assertFalse(ch2.isComplete());
        ch2.complete();
        assertTrue(ch2.isComplete());
        assertEquals(8, prog.getRemainingStepCount());
        prog.complete();
        assertTrue(ch2.isComplete());
        assertTrue(prog.isComplete());
    }

    
}
