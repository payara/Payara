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
import org.junit.*;
import static org.junit.Assert.*;

/** 
 *
 * @author mmares
 */
public class ProgressStatusImplTest {
    
    private DummyParent parent;
    
    public ProgressStatusImplTest() {
    }
    
    @Before
    public void prepareParent() {
        parent = new DummyParent();
    }

    @Test
    public void testGetSetTotalStepCount() {
        ProgressStatusImpl psi = new ProgressStatusImpl("first", parent, null);
        assertTrue(psi.getTotalStepCount() < 0);
        psi.setTotalStepCount(10);
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        assertEquals(10, psi.getTotalStepCount());
        psi = new ProgressStatusImpl("first", 10, parent, null);
        assertEquals(10, psi.getTotalStepCount());
        psi.progress(8);
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        psi.setTotalStepCount(6);
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        assertEquals(6, psi.getTotalStepCount());
        assertEquals(0, psi.getRemainingStepCount());
        psi.setTotalStepCount(10);
        assertEquals(10, psi.getTotalStepCount());
        assertEquals(4, psi.getRemainingStepCount());
        psi.complete();
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        psi.setTotalStepCount(15);
        assertNull(parent.lastEvent);
        assertEquals(10, psi.getTotalStepCount());
    }

    @Test
    public void testProgressAndGetRemainingStepCount() {
        ProgressStatusImpl psi = new ProgressStatusImpl("first", 10, parent, null);
        assertEquals(10, psi.getRemainingStepCount());
        psi.progress(1);
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        assertEquals(9, psi.getRemainingStepCount());
        psi.progress(2);
        assertEquals(7, psi.getRemainingStepCount());
        psi.progress("Some message");
        assertEquals(7, psi.getRemainingStepCount());
        psi.progress(4, "Other message");
        assertEquals(3, psi.getRemainingStepCount());
        psi.progress(null);
        assertEquals(3, psi.getRemainingStepCount());
        psi.progress(2, null);
        assertEquals(1, psi.getRemainingStepCount());
        psi.progress(1);
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        assertEquals(0, psi.getRemainingStepCount());
        psi.progress(1);
        assertNull(parent.lastEvent);
        assertEquals(0, psi.getRemainingStepCount());
        psi = new ProgressStatusImpl("second", parent, null);
        assertTrue(psi.getRemainingStepCount() < 0);
        psi.progress(1);
        assertTrue(psi.getRemainingStepCount() < 0);
        psi.setTotalStepCount(10);
        assertEquals(9, psi.getRemainingStepCount());
        psi.complete();
        assertEquals(0, psi.getRemainingStepCount());
    }

    @Test
    public void testSetCurrentStepCount() {
        ProgressStatusImpl psi = new ProgressStatusImpl("first", 10, parent, null);
        psi.setCurrentStepCount(5);
        assertEquals(5, psi.getRemainingStepCount());
        psi.progress(1);
        assertEquals(4, psi.getRemainingStepCount());
        psi.setCurrentStepCount(8);
        assertEquals(2, psi.getRemainingStepCount());
        psi.setCurrentStepCount(12);
        assertEquals(0, psi.getRemainingStepCount());
        psi.setTotalStepCount(15);
        assertEquals(5, psi.getRemainingStepCount());
        psi.setCurrentStepCount(5);
        assertEquals(10, psi.getRemainingStepCount());
    }

    @Test
    public void testComplete() {
        ProgressStatusImpl psi = new ProgressStatusImpl("first", 10, parent, null);
        assertFalse(psi.isComplete());
        psi.complete();
        assertTrue(psi.isComplete());
        assertNotNull(parent.lastEvent);
        parent.lastEvent = null;
        psi = new ProgressStatusImpl("first", parent, null);
        assertFalse(psi.isComplete());
        psi.complete();
        assertTrue(psi.isComplete());
        psi = new ProgressStatusImpl("first", 10, parent, null);
        psi.progress(8);
        assertFalse(psi.isComplete());
        psi.complete();
        assertTrue(psi.isComplete());
        psi = new ProgressStatusImpl("first", 10, parent, null);
        psi.progress(5);
        psi.progress(5);
        assertFalse(psi.isComplete());
        psi.progress(1);
        assertFalse(psi.isComplete());
        psi = new ProgressStatusImpl("first", 10, parent, null);
        psi.complete();
        parent.lastEvent = null;
        psi.complete();
        assertNull(parent.lastEvent);
    }

    @Test
    public void testCreateChild() {
        ProgressStatusImpl psi = new ProgressStatusImpl("A", 10, parent, null);
        ProgressStatus ch1 = psi.createChild(2);
        ProgressStatus ch2 = psi.createChild("A.2", 3);
        assertEquals(5, psi.getRemainingStepCount());
        ProgressStatus ch3 = psi.createChild("A.3", 10);
        assertEquals(0, psi.getRemainingStepCount());
        assertEquals(15, psi.getTotalStepCount());
        parent.lastEvent = null;
        ch1.progress(1);
        assertNotNull(parent.lastEvent);
        psi.complete();
        assertTrue(psi.isComplete());
        assertTrue(ch1.isComplete());
        assertTrue(ch2.isComplete());
        assertTrue(ch3.isComplete());
        psi = new ProgressStatusImpl("B", 10, parent, null);
        ch1 = psi.createChild("B.1", 3);
        psi.progress(2);
        assertEquals(5, psi.getRemainingStepCount());
        ch2 = psi.createChild("B.2", 8);
        assertEquals(11, psi.getTotalStepCount());
        psi.setTotalStepCount(15);
        assertEquals(4, psi.getRemainingStepCount());
        psi.complete();
        assertTrue(psi.isComplete());
        assertTrue(ch1.isComplete());
        assertTrue(ch2.isComplete());
    }
    
    @Test
    public void testIdGeneration() {
        ProgressStatusImpl psi = new ProgressStatusImpl("A", 10, null, null);
        assertNotNull(psi.id);
    }

}
