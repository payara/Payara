/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util.io;

import com.sun.enterprise.util.ObjectAnalyzer;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
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
public class ServerDirsTest {
    public ServerDirsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        final ClassLoader cl = ServerDirsTest.class.getClassLoader();
        childFile = new File(cl.getResource("grandparent/parent/child").toURI());
        parentFile = new File(cl.getResource("grandparent/parent").toURI());
        grandParentFile = new File(cl.getResource("grandparent").toURI());
        initUserDirs();
        assertTrue(new File(childFile, "readme.txt").isFile());
        assertTrue(childFile.isDirectory());
        assertTrue(parentFile.isDirectory());
        assertTrue(grandParentFile.isDirectory());
        assertTrue(userNextToTopLevelFile.isDirectory());
        assertTrue(userTopLevelFile.isDirectory());
    }

    /**
     * It is not allowed to use a dir that has no parent...
     * @throws Exception
     */
    @Test(expected = IOException.class)
    public void testNoParent() throws Exception {
        assertNotNull(userTopLevelFile);
        assertTrue(userTopLevelFile.isDirectory());
        assertNull(userTopLevelFile.getParentFile());

        try {
            ServerDirs sd = new ServerDirs(userTopLevelFile);
        }
        catch (IOException e) {
            throw e;
        }
    }

    /**
     * Test is no good anymore -- the ServerDirs now always look for "config" dir
     * and domain name.
     *
    @Test
    public void testNoGrandParent() throws Exception {
    assertNotNull(userNextToTopLevelFile);
    assertTrue(userNextToTopLevelFile.isDirectory());
    File parent = userNextToTopLevelFile.getParentFile();
    assertNotNull(parent);
    assertNull(parent.getParentFile());
    assertEquals(parent, userTopLevelFile);

    ServerDirs sd = new ServerDirs(userNextToTopLevelFile);
    }
     **/
    @Test
    public void testSpecialFiles() throws IOException {
        ServerDirs sd = new ServerDirs(childFile);
        assertTrue(sd.getConfigDir() != null);
        assertTrue(sd.getDomainXml() != null);
    }

    @Test
    public void testNoArgConstructor() {
        ServerDirs sd = new ServerDirs();
        // check 3 volunteers for nullness...
        assertNull(sd.getPidFile());
        assertNull(sd.getServerGrandParentDir());
        assertFalse(sd.isValid());
    }

    private static void initUserDirs() {
        // this is totally developer-environment dependent!
        // very inefficient but who cares -- this is a unit test.
        // we need this info to simulate an illegal condition like
        // specifying a directory that has no parent and/or grandparent

        Stack<File> stack = new Stack<File>();
        File f = childFile;  // guaranteed to have a valid parent and grandparent

        do {
            stack.push(f);
            f = f.getParentFile();
        }
        while(f != null);

        // the first pop has the top-level
        // the next pop has the next-to-top-level
        userTopLevelFile = stack.pop();
        userNextToTopLevelFile = stack.pop();
    }
    private static File childFile;
    private static File parentFile;
    private static File grandParentFile;
    private static File userTopLevelFile;
    private static File userNextToTopLevelFile;
}
