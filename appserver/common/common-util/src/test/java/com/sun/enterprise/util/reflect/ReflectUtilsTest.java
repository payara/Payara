/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.reflect;

import java.lang.reflect.Method;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Byron Nevins
 */
public class ReflectUtilsTest {
    public ReflectUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of equalSignatures method, of class ReflectUtils.
     */
    @Test
    public void testEqualSignatures() throws NoSuchMethodException {
        Method m1 = getClass().getDeclaredMethod("met1", String.class, Long.class);
        Method m2 = getClass().getDeclaredMethod("met2", String.class, Long.class);
        Method m3 = getClass().getDeclaredMethod("met3", String.class);
        Method m4 = getClass().getDeclaredMethod("met4", String.class, Integer.class);
        Method m5 = getClass().getDeclaredMethod("met5", String.class, Integer.class);
        Method m6 = getClass().getDeclaredMethod("met6", String.class, String.class);

        String s1 = ReflectUtils.equalSignatures(m1, m2);
        String s2 = ReflectUtils.equalSignatures(m1, m3);
        String s3 = ReflectUtils.equalSignatures(m1, m4);
        String s4 = ReflectUtils.equalSignatures(m2, m1);
        String s5 = ReflectUtils.equalSignatures(m2, m3);
        String s6 = ReflectUtils.equalSignatures(m2, m4);
        String s7 = ReflectUtils.equalSignatures(m3, m4);
        String s8 = ReflectUtils.equalSignatures(m4, m5);
        String s9 = ReflectUtils.equalSignatures(m5, m6);

        assertNull(s1);
        assertNull(s4);
        assertNotNull(s2);
        assertNotNull(s3);
        assertNotNull(s5);
        assertNotNull(s6);
        assertNotNull(s7);
        assertNotNull(s8);
        assertNotNull(s9);
        System.out.println("---------SUCCESSful MISMATCH STRINGS: ------------");
        System.out.printf("%s\n%s\n%s\n%s\n%s\n%s\n%s\n", s2, s3, s5, s6, s7, s8, s9);
        System.out.println("--------------------------------------------------");
    }

    public void met1(String s, Long l) {
    }

    public void met2(String s, Long l) {
    }

    public void met3(String s) {
    }

    public void met4(String s, Integer i) {
    }

    public String met5(String s, Integer i) {
        return "";
    }

    public Long met6(String s, String s2) {
        return 22L;
    }
}
