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
package org.glassfish.nucleus.admin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.tests.utils.NucleusTestUtils;
import static org.glassfish.tests.utils.NucleusTestUtils.*;
import static org.testng.AssertJUnit.*;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public class NucleusStartStopTest {

    private static final String TEST_LIBS_KEY = "TEST_LIBS";
    private static final Map<String, String> COPY_LIB;
    static {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("modules", "modules");
        COPY_LIB = Collections.unmodifiableMap(map);
    }

    @BeforeSuite
    public void setUp(ITestContext context) throws IOException {
        //Copy testing libraries into Nucleus distribution
        Collection<File> testLibs = new ArrayList<File>();
        context.setAttribute(TEST_LIBS_KEY, testLibs);
        String basedir = System.getProperty("basedir");
        assertNotNull(basedir);
        File addondir = new File(basedir, "target/addon");
        for (Map.Entry<String, String> entry : COPY_LIB.entrySet()) {
            copyLibraries(new File(addondir, entry.getKey()), 
                    new File(NucleusTestUtils.getNucleusRoot(), 
                    entry.getValue()), testLibs);
        }
        //Start
        assertTrue(nadmin("start-domain"));
    }

    @AfterSuite(alwaysRun = true)
    public void tearDown(ITestContext context) {
        try {
            assertTrue(nadmin("stop-domain"));
        } finally {
            Collection<File> libs = (Collection<File>) context.getAttribute(TEST_LIBS_KEY);
            if (libs != null) {
                for (File lib : libs) {
                    if (lib.exists()) {
                        try {
                            lib.delete();
                        } catch (Exception ex) {
                            System.out.println("Can not delete " + lib.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }
    
    private void copyLibraries(File src, File dest, Collection<File> copiedLibs) throws IOException {
        if (src.exists() && src.isDirectory()) {
            File[] dirs = src.listFiles(new FileFilter() {
                                       @Override
                                       public boolean accept(File f) {
                                           return f.isDirectory();
                                       }
                                   });
            for (File dir : dirs) {
                File target = new File(dir, "target");
                if (target.exists() && target.isDirectory()) {
                    File[] jars = target.listFiles(new FileFilter() {
                                                   @Override
                                                   public boolean accept(File f) {
                                                       return f.isFile() && f.getName().toLowerCase().endsWith(".jar");
                                                   }
                                               });
                    for (File jar : jars) {
                        File df = new File(dest, jar.getName());
                        copiedLibs.add(df);
                        copy(jar, df);
                        System.out.println("TESTING LIBRARY: " + df.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static void copy(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.createNewFile();
        }
        FileChannel sch = null;
        FileChannel dch = null;
        try {
            sch = new FileInputStream(src).getChannel();
            dch = new FileOutputStream(dest).getChannel();
            dch.transferFrom(sch, 0, sch.size());
        } finally {
            try { sch.close(); } catch (Exception ex) {}
            try { dch.close(); } catch (Exception ex) {}
        }
    }

    
}