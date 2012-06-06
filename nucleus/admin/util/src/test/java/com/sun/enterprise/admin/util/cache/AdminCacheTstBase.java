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
package com.sun.enterprise.admin.util.cache;

import com.sun.enterprise.security.store.AsadminSecurityUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import org.glassfish.tests.utils.Utils;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/** General test for AdminCache implementations which has file system 
 * on background
 *
 * @author mmares
 */
public abstract class AdminCacheTstBase {

    public static final String TEST_CACHE_COTEXT = "junit-test-temp/";
    private static boolean skipThisTest = false;
    private AdminCache cache;

    public AdminCacheTstBase(AdminCache cache) {
        this.cache = cache;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Clean up temp directory
        File dir = AsadminSecurityUtil.getDefaultClientDir();
        dir = new File(dir, TEST_CACHE_COTEXT);
        try {
            recursiveDelete(dir);
            if (dir.exists()) {
                skipThisTest = true;
                System.out.println("JUNIT: AdminCache tests: Can not do this test, because can not purify " + dir.getPath() + " directory.");
            } else {
                //Test to create and write data
                if (!dir.mkdirs()) {
                    skipThisTest = true;
                    System.out.println("JUNIT: AdminCache tests: Can not do this test. Can not create " + dir.getPath() + " directory.");
                    return;
                }
                File f = new File(dir, "qeen.junit");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    fos.write("Another One Bites the Dust".getBytes());
                } finally {
                    try {
                        fos.close();
                    } catch (Exception ex) {
                    }
                }
                if (!f.exists()) {
                    skipThisTest = true;
                    System.out.println("JUNIT: AdminCache tests: Can not do this test. Can not write to files in " + dir.getPath() + " directory.");
                }
                recursiveDelete(dir);
            }
        } catch (Exception ex) {
            skipThisTest = true;
            System.out.println("JUNIT: AdminCache tests: Can not do this test. Can not write to files in " + dir.getPath() + " directory.");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File dir = AsadminSecurityUtil.getDefaultClientDir();
        dir = new File(dir, TEST_CACHE_COTEXT);
        recursiveDelete(dir);
    }

    public static void recursiveDelete(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (".".equals(f.getName()) || "..".equals(f.getName())) {
            return;
        }
        if (f.isDirectory()) {
            File[] subFiles = f.listFiles();
            for (File subFile : subFiles) {
                recursiveDelete(subFile);
            }
        }
        f.delete();
    }

    protected static boolean isSkipThisTest() {
        return skipThisTest;
    }

    public AdminCache getCache() {
        return cache;
    }
    
    @Test
    public void testPutGet() {
        if (isSkipThisTest()) {
            System.out.println(this.getClass().getName() + ".testPutGet(): Must skip this unit test, because something is wrong with file cache writing during build");
        } else {
            System.out.println(this.getClass().getName() + ".testPutGet()");
        }
        //1
        String qeen1 = "Crazy Little Thing Called Love";
        String qeen1Key = TEST_CACHE_COTEXT + "Qeen1";
        cache.put(qeen1Key, qeen1);
        File f = new File(AsadminSecurityUtil.getDefaultClientDir(), qeen1Key);
        assertTrue(f.exists());
        assertTrue(f.isFile());
        String str = cache.get(qeen1Key, String.class);
        assertNotNull(str);
        assertEquals(qeen1, str);
        //2
        String qeen2 = "You\'re My Best Friend";
        String qeen2Key = TEST_CACHE_COTEXT + "A-Night-at-the-Opera/Qeen2";
        cache.put(qeen2Key, qeen2);
        f = new File(AsadminSecurityUtil.getDefaultClientDir(), qeen2Key);
        assertTrue(f.exists());
        assertTrue(f.isFile());
        str = cache.get(qeen2Key, String.class);
        assertNotNull(str);
        assertEquals(qeen2, str);
        //1 - re read
        str = cache.get(qeen1Key, String.class);
        assertNotNull(str);
        assertEquals(qeen1, str);
        //2 - update
        String qeen2b = "Bohemian Rhapsody";
        cache.put(qeen2Key, qeen2b);
        str = cache.get(qeen2Key, String.class);
        assertNotNull(str);
        assertEquals(qeen2b, str);
        //Done
        System.out.println(this.getClass().getName() + ".testPutGet(): Done");
    }
    
    @Test
    public void testExistence() throws InterruptedException {
        if (isSkipThisTest()) {
            System.out.println(this.getClass().getName() + ".testExistence(): Must skip this unit test, because something is wrong with file cache writing during build");
        } else {
            System.out.println(this.getClass().getName() + ".testExistence()");
        }
        //1
        String stones1 = "Paint it black";
        String stones1Key = TEST_CACHE_COTEXT + "Rolling.Stones.1";
        cache.put(stones1Key, stones1);
        //2
        String stones2 = "Jumpin\' Jack Flash";
        String stones2Key = TEST_CACHE_COTEXT + "Rolling.Stones.2";
        cache.put(stones2Key, stones2);
        //contains
        assertTrue(cache.contains(stones1Key));
        assertTrue(cache.contains(stones2Key));
        assertFalse(cache.contains(stones1Key+"ooops"));
        //lastUpdated
        Date lastUpdated1 = cache.lastUpdated(stones1Key);
        assertNotNull(lastUpdated1);
        Thread.sleep(2000L);
        String stones1b = "Good times, bad times";
        cache.put(stones1Key, stones1b);
        Date lastUpdated2 = cache.lastUpdated(stones1Key);
        assertNotNull(lastUpdated2);
        assertTrue(lastUpdated1.getTime() < lastUpdated2.getTime());
        System.out.println(this.getClass().getName() + ".testExistence()");
    }
}
