/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.admin.AccessRequired;
import org.glassfish.tests.utils.NucleusTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.glassfish.tests.utils.NucleusTestUtils.nadmin;
import static org.glassfish.tests.utils.NucleusTestUtils.nadminWithOutput;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author sanjeeb.sahoo@oracle.com
 */
@Test
public class OSGiCommandsTest {

    public void basicOsgiCmd() {
        assertTrue(nadmin("osgi", "lb"));
    }

    private List<String> runCmd(String... cmd) throws Exception {
        NucleusTestUtils.NadminReturn value = nadminWithOutput(cmd);
        if (!value.returnValue) {
            throw new Exception("Cmd failed: \n" + value.outAndErr);
        }
        List<String> output = new ArrayList<String>();
        for (String line : value.out.split("\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("nadmin") || line.startsWith("Command")) continue;
            output.add(line);
        }
        return output;
    }

    private String newCmdSession() throws Exception {
        List<String> value = runCmd("osgi", "--session", "new");
        if (value.size() != 1) {
            throw new Exception("Unexpected output: \n " + value);
        }
        return value.get(0);
    }

    private Set<String> listCmdSessions() throws Exception {
        List<String> sessions = runCmd("osgi", "--session", "list");
        return new HashSet<String>(sessions);
    }

    /**
     * Tests functionality of session handling of osgi command.
     * It creates sessions, lists them, executes commands against each session and finally stops them.
     * @throws Exception
     */
    public void osgiCmdSession() throws Exception {
        // Create some sessions
        Set<String> sessions = new HashSet<String>();
        for (int i = 0 ; i < 3; ++i) {
            sessions.add(newCmdSession());
        }

        // Let's list them to make sure list operation works.
        final Set<String> actual = listCmdSessions();
        assertEquals("listed sessions do not match with created sessions", sessions, actual);

        // Let's set the same variable in each command session with a different value and make sure the variables
        // are scoped to sessions.
        for (String sessionId : sessions) {
            runCmd("osgi", "--session", "execute", "--session-id", sessionId, "var=" + sessionId);
        }
        for (String sessionId : sessions) {
            String value = runCmd("osgi", "--session", "execute", "--session-id", sessionId, "echo $var").get(0);
            assertEquals(sessionId, value);
        }

        // Let's stop all sessions.
        for (String sessionId : sessions) {
            runCmd("osgi", "--session", "stop", "--session-id", sessionId);
        }
        sessions = listCmdSessions();
        assertTrue("Not all sessions closed properly: " + sessions, sessions.isEmpty());
    }

    /**
     * Test osgi-shell command which is a local command. It takes a file as input. The file contains
     * a list of shell commands to be executed.
     * @throws IOException
     */
    public void osgiShell() throws IOException {
        File cmdFile = File.createTempFile("osgi-commands", ".txt");
        cmdFile.deleteOnExit();
        PrintStream ps = new PrintStream(new FileOutputStream(cmdFile));
        try {
            ps.println("help");
            ps.println("lb");
            NucleusTestUtils.NadminReturn value = nadminWithOutput("osgi-shell", "--file", cmdFile.getAbsolutePath());
            assertTrue(value.out.contains("System Bundle"));
        } finally {
            try {
                ps.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
