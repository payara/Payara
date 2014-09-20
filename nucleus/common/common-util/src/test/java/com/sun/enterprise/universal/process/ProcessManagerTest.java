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

package com.sun.enterprise.universal.process;

import com.sun.enterprise.util.OS;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
public class ProcessManagerTest {

    public ProcessManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        textfile = new File(ProcessManagerTest.class.getClassLoader().getResource("process/lots_o_text.txt").getPath()).getAbsolutePath();
        assertTrue(textfile != null && textfile.length() > 0);
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
     * Temporary Test of ProcessManager
     * This stuff is platform dependent.  
     */
    @Test
    public void test1() throws ProcessManagerException {
        ProcessManager pm;

        System.out.println("If it is FROZEN RIGHT NOW -- then Houston, we have a problem!");
        System.out.println("ProcessManager must have the write to stdin before the reader threads have started!");

        if (OS.isWindows())
            pm = new ProcessManager("cmd", "/c", "type", textfile);
        else
            pm = new ProcessManager("cat", textfile);

        pm.setStdinLines(hugeInput());
        pm.setEcho(false);
        pm.execute();
    }

    private List<String> hugeInput() {
        List<String> l = new ArrayList<String>();

        for (int i = 0; i < 50000; i++)
            l.add("line number " + i + "here!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        return l;
    }
    private static String textfile;
}
