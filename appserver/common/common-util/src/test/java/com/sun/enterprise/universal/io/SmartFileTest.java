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

package com.sun.enterprise.universal.io;

import com.sun.enterprise.util.OS;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author bnevins
 */
public class SmartFileTest {

    public SmartFileTest() {
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
     * Test of sanitizePaths method, of class SmartFile.
     */
    @Test
    public void sanitizePaths() {
        String sep = File.pathSeparator;

        // where are we now?
        String here = SmartFile.sanitize(".");

        String cp1before = "/a/b/c" + sep + "qqq" + sep + "qqq" + sep + "qqq" + sep + "qqq" + sep + "qqq" + sep + "./././qqq/./." + sep + "z/e";
        String cp1expected = "/a/b/c" + sep + here + "/qqq" + sep + here + "/z/e";

        if (sep.equals(";")) {
            // Windows -- drive letter is needed...
            String drive = here.substring(0, 2);
            cp1expected = drive + "/a/b/c;" + here + "/qqq;" + here + "/z/e";
        }
        assertEquals(cp1expected, SmartFile.sanitizePaths(cp1before));
    }

    /**
     * Test of sanitizePaths method, of class SmartFile.
     */
    @Test
    public void sanitizePaths2() {
        String sep = File.pathSeparator;
        if (OS.isWindows()) {
            String badPaths = "c:/xyz;\"c:\\a b\";c:\\foo";
            String convert = SmartFile.sanitizePaths(badPaths);
            String expect = "C:/xyz;C:/a b;C:/foo";
            assertEquals(convert, expect);
        }
        else {
            String badPaths = "/xyz:\"/a b\":/foo";
            String convert = SmartFile.sanitizePaths(badPaths);
            String expect = "/xyz:/a b:/foo";
            assertEquals(convert, expect);
        }
    }

    @Test
    public void edgeCase() {
        if(OS.isWindows())
            return;
        
        String fn = "/../../../../../../../../foo";
        assertEquals(SmartFile.sanitize(fn), "/foo");
        fn = "/../foo";
        assertEquals(SmartFile.sanitize(fn), "/foo");

        fn = "/foo/../foo";
        assertEquals(SmartFile.sanitize(fn), "/foo");

        fn = "/foo/../../foo";
        assertEquals(SmartFile.sanitize(fn), "/foo");
    }


    private static final String[] FILENAMES = new String[]{
        "c:/",
        "c:",
        "",
        "\\foo",
        "/",
        "/xxx/yyy/././././../yyy",
        "/x/y/z/../../../temp",
        //"\\\\",
        //"\\\\foo\\goo\\hoo",
        "x/y/../../../..",
        "/x/y/../../../..",
        "/./../.././../",
        "/::::/x/yy",};
}
