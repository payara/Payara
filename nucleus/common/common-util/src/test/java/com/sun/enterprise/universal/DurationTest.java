/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class DurationTest {

    public DurationTest() {
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
    public void test1() {
        long msec = Duration.MSEC_PER_WEEK * 3 +
                    Duration.MSEC_PER_DAY * 6 +
                    Duration.MSEC_PER_HOUR * 23 +
                    Duration.MSEC_PER_MINUTE * 59 +
                    Duration.MSEC_PER_SECOND * 59;
        
        Duration d = new Duration(msec);
        assertTrue(d.numWeeks == 3);
        assertTrue(d.numDays == 6);
        assertTrue(d.numHours == 23);
        assertTrue(d.numMinutes == 59);
        assertTrue(d.numSeconds == 59);
        
    }
    @Test
    public void test2() {
        long msec = Duration.MSEC_PER_WEEK * 7 +
                    Duration.MSEC_PER_DAY * 6 +
                    Duration.MSEC_PER_HOUR * 23 +
                    Duration.MSEC_PER_MINUTE * 59 +
                    Duration.MSEC_PER_SECOND * 59 +
                    999;
                    
        
        Duration d = new Duration(msec);
        assertTrue(d.numWeeks == 7);
        assertTrue(d.numDays == 6);
        assertTrue(d.numHours == 23);
        assertTrue(d.numMinutes == 59);
        assertTrue(d.numSeconds == 59);
        assertTrue(d.numMilliSeconds == 999);
    }
    @Test
    public void test3() {
        long msec = System.currentTimeMillis();
        Duration d = new Duration(msec);
        assertTrue(d.numWeeks > 38 * 52);
    }
    @Test
    public void test4() {
        Duration d = new Duration(27188);
        assertTrue(d.numSeconds == 27);
        assertTrue(d.numMilliSeconds == 188);
    }
    @Test
    public void test5() {
        Duration d = new Duration(2);
        assertTrue(d.numSeconds == 0);
        assertTrue(d.numMilliSeconds == 2);
    }
}
    
