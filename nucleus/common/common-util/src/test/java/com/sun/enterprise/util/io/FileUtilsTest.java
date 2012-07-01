/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.universal.io.SmartFile;
import java.io.File;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author wnevins
 */
public class FileUtilsTest {
    /**
     * Test of mkdirsMaybe method, of class FileUtils.
     */
    @Test
    public void testMkdirsMaybe() {
        assertFalse(FileUtils.mkdirsMaybe(null));
        File f = new File(".").getAbsoluteFile();
        assertFalse(FileUtils.mkdirsMaybe(null));
        File d1 = new File("junk" + System.currentTimeMillis());
        File d2 = new File("gunk" + System.currentTimeMillis());

        assertTrue(d1.mkdirs());
        assertFalse(d1.mkdirs());
        assertTrue(FileUtils.mkdirsMaybe(d1));
        assertTrue(FileUtils.mkdirsMaybe(d1));
        assertTrue(FileUtils.mkdirsMaybe(d2));
        assertTrue(FileUtils.mkdirsMaybe(d2));
        assertFalse(d2.mkdirs());

        if (!d1.delete())
            d1.deleteOnExit();

        if (!d2.delete())
            d2.deleteOnExit();

    }
    @Test
    public void testParent() {
        File f = null;
        assertNull(FileUtils.getParentFile(f));
        f = new File("/foo/././././.");
        File wrongGrandParent = f.getParentFile().getParentFile();
        File correctParent = FileUtils.getParentFile(f);
        File sanitizedChild = SmartFile.sanitize(f);
        File sanitizedWrongGrandParent = SmartFile.sanitize(wrongGrandParent);
        File shouldBeSameAsChild = new File(correctParent, "foo");

        // check this out -- surprise!!!!
        assertEquals(sanitizedWrongGrandParent, sanitizedChild);
        assertEquals(shouldBeSameAsChild, sanitizedChild);
    }

    @Test
    public void testResourceToString() {
        String resname = "simplestring.txt";
        String contents = "Simple String Here!";
        String fetched = FileUtils.resourceToString(resname);
        assertEquals(contents, fetched);
    }
    @Test
    public void testEmptyButExistingResourceToString() {
        String resname = "empty.txt";
        String fetched = FileUtils.resourceToString(resname);
        assertNotNull(fetched);
        assertTrue(fetched.length() == 0);
    }

    @Test
    public void testNonExistingResourceToString() {
        String resname = "doesnotexist.txt";
        String fetched = FileUtils.resourceToString(resname);
        assertNull(fetched);
    }
    @Test
    public void testNonExistingResourceToBytes() {
        String resname = "doesnotexist.txt";
        byte[] fetched = FileUtils.resourceToBytes(resname);
        // null -- not an empty array!
        assertNull(fetched);
    }
    @Test
    public void testResourceToBytes() {
        String resname = "verysimplestring.txt";
        byte[] fetched = FileUtils.resourceToBytes(resname);

        assertEquals(fetched[0], 65);
        assertEquals(fetched[1], 66);
        assertEquals(fetched[2], 67);
        assertEquals(fetched.length, 3);
    }
}
