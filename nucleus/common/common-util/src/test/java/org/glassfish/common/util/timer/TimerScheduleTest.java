/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util.timer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author mvatkina
 */
public class TimerScheduleTest {

    public TimerScheduleTest() {
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
    public void testSundays() {
        Date fromDate = new Date(112, 9, 16, 10, 35);
        Date timeoutDate = new Date(112, 9, 21, 12, 0);
        Locale localeDefault = Locale.getDefault();
        Locale[] availableLocales = Locale.getAvailableLocales();
        for (Locale l : availableLocales) {
            Locale.setDefault(l);
            testSundays(fromDate, timeoutDate, false);
        }

        // Test couple of locales explicitly - see GLASSFISH-18804 and GLASSFISH-19154
        Locale l1 = new Locale("es", "PE");
        Locale.setDefault(l1);
        testSundays(fromDate, timeoutDate, true);

        l1 = new Locale("it", "IT");
        Locale.setDefault(l1);
        testSundays(fromDate, timeoutDate, true);

        Locale.setDefault(localeDefault);

    }

    @Test
    public void testDays1To5() {
        // 2013 Jul 7 - Sun
        Date fromDate = new Date(113, 6, 7, 10, 35);
        // 2013 Jul 8 - Mon
        Date timeoutDate = new Date(113, 6, 8, 20, 15);
        Locale localeDefault = Locale.getDefault();
        Locale[] availableLocales = Locale.getAvailableLocales();
        for (Locale l : availableLocales) {
            Locale.setDefault(l);
            testDays1To5(fromDate, timeoutDate, false);
        }

        // Test DE_de locale explicitly - see GLASSFISH-20673
        Locale l1 = new Locale("de", "DE");
        Locale.setDefault(l1);
        testDays1To5(fromDate, timeoutDate, true);

        Locale.setDefault(localeDefault);
    }

    private void testSundays(Date fromDate, Date timeoutDate, boolean log) {
        TimerSchedule ts = new TimerSchedule().dayOfWeek("7").
                hour("12").
                minute("0");


        Date newDate = ts.getNextTimeout(fromDate).getTime();
        if (log)
            System.out.println("Expected date: " + timeoutDate + " Got date: " + newDate + " in Locale: "+Locale.getDefault());

        if (!newDate.equals(timeoutDate)) {
            System.out.println("ERROR - Expected date: " + timeoutDate + " Got date: " + newDate + " in Locale: "+Locale.getDefault());
            assert(false);
        } else {
            assert(true);
        }
    }

    private void testDays1To5(Date fromDate, Date timeoutDate, boolean log) {
        TimerSchedule ts = new TimerSchedule().dayOfWeek("1-5").
                hour("20").
                minute("15");


        Date newDate = ts.getNextTimeout(fromDate).getTime();
        if (log)
            System.out.println("Expected date: " + timeoutDate + " Got date: " + newDate + " in Locale: "+Locale.getDefault());

        if (!newDate.equals(timeoutDate)) {
            System.out.println("ERROR - Expected date: " + timeoutDate + " Got date: " + newDate + " in Locale: "+Locale.getDefault());
            assert(false);
        } else {
            assert(true);
        }
    }

}
    
