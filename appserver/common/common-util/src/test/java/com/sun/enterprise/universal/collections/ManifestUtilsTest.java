/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal.collections;

import java.util.Map;
import java.util.jar.*;
import java.util.jar.Manifest;
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
public class ManifestUtilsTest {

    public ManifestUtilsTest() {
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

    /**
     * Test of normalize method, of class ManifestUtils.
     */
    @Test
    public void normalize() {
        Manifest m = new Manifest();
        String hasToken = "abc" + ManifestUtils.EOL_TOKEN + "def";
        String convertedHasToken = "abc" + ManifestUtils.EOL + "def";
        Attributes mainAtt = m.getMainAttributes();
        Map<String,Attributes> entries =  m.getEntries();
        Attributes fooAtt = new Attributes();
        entries.put("foo", fooAtt);
        fooAtt.putValue("fooKey", "fooValue");
        fooAtt.putValue("fooKey2", hasToken);
        mainAtt.putValue("mainKey", "mainValue");
        
        Map<String,Map<String,String>> norm = ManifestUtils.normalize(m);
        Map<String,String> normMainAtt = norm.get(ManifestUtils.MAIN_ATTS);
        Map<String,String> normFooAtt = norm.get("foo");
        
        assertTrue(norm.size() == 2);
        assertNotNull(normMainAtt);
        assertNotNull(normFooAtt);
        assertTrue(normMainAtt.size() == 1);
        assertTrue(normFooAtt.size() == 2);
        assertFalse(normFooAtt.get("fooKey2").equals(hasToken));
        assertTrue(normFooAtt.get("fooKey2").equals(convertedHasToken));
        assertFalse(hasToken.equals(convertedHasToken));
        assertEquals(normMainAtt.get("mainKey"), "mainValue");
    }

    @Test
    public void encode() {
        String noLinefeed = "abc";
        String linefeed = "abc\ndef";
        String dosfeed = "abc\r\ndef";
        String s1 = ManifestUtils.encode(noLinefeed);
        String s2 = ManifestUtils.encode(linefeed);
        String s3 = ManifestUtils.encode(dosfeed);
        
        String desired = "abc" + ManifestUtils.EOL_TOKEN + "def";
        
        assertEquals(noLinefeed, s1);
        assertFalse(linefeed.equals(s2));
        assertFalse(dosfeed.equals(s3));
        assertEquals(s2, desired);
        assertEquals(s3, desired);
    }
}
