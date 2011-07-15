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

package com.sun.enterprise.universal.glassfish;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class TokenResolverTest {

    public TokenResolverTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        testMap = new HashMap<String,String>();
        testMap.put("name1", "value1");
        testMap.put("name2", "value2");
        testMap.put("name3", "value3");
        testMap.put("name4", "value4");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of resolve method, of class TokenResolver.
     */
    @Test
    public void testResolve_Map() {
        Map<String,String> map2 = new HashMap<String,String>();

        map2.put("foo", "${name1}");
        map2.put("foo2", "${name111}");
        map2.put("zzz${name3}zzz", "zzz");
        map2.put("qqq${name2}qqq", "${name4}");

        TokenResolver instance = new TokenResolver(testMap);
        instance.resolve(map2);
        assertEquals(map2.get("foo"), "value1");
        assertEquals(map2.get("foo2"), "${name111}");
        // this entry should be gone:
        assertNull(map2.get("qqq${name2}qqq"));

        // and replaced with this:
        assertEquals(map2.get("qqqvalue2qqq"), "value4");

        assertEquals(map2.get("zzzvalue3zzz"), "zzz");
        
        instance.resolve(map2);
    }
    /**
     * Test of resolve method, of class TokenResolver.
     */
    @Test
    public void testResolve_List() {
        List<String> list = null;
        TokenResolver instance = new TokenResolver(testMap);
        //instance.resolve(list);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of resolve method, of class TokenResolver.
     */
    @Test
    public void testResolve_String() {
        TokenResolver instance = new TokenResolver(testMap);
        String expResult = "xyzvalue1xyz";
        String result = instance.resolve("xyz${name1}xyz");
        assertEquals(expResult, result);

        expResult = "xyz$value1xyz";
        result = instance.resolve("xyz$${name1}xyz");
        assertEquals(expResult, result);

        expResult = "xyzvalue1}xyz";
        result = instance.resolve("xyz${name1}}xyz");
        assertEquals(expResult, result);

        expResult = "xyzvalue4xyz";
        result = instance.resolve("xyz${name4}xyz");
        assertEquals(expResult, result);

        expResult = "xyz${name5}xyz";
        result = instance.resolve("xyz${name5}xyz");
        assertEquals(expResult, result);
    }

    private Map<String,String> testMap;
}
