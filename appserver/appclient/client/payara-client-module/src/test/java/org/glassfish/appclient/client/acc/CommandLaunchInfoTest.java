/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc;


import java.io.File;
import java.net.URL;
import org.junit.Ignore;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.glassfish.appclient.client.acc.CommandLaunchInfo.ClientLaunchType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author tjquinn
 */
public class CommandLaunchInfoTest {

    private static final String FIRST_ACC_ARG = "first arg";
    private static final String SECOND_ACC_ARG = "second=arg";

    private static final String JAR_CLIENT_NAME = "there/myClient.jar";
    private static final String DIR_CLIENT_NAME = "here/myClient";
    private static final String USER_VALUE = "joe-the-user";
    private static final String PASSWORDFILE_PATH = "/topSecret.stuff";
    
    private static final List<String> expectedCommandArgs = Arrays.asList(FIRST_ACC_ARG, SECOND_ACC_ARG);

    public CommandLaunchInfoTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testA() throws Exception, UserError {

        final AgentArguments agentArgs = AgentArguments.newInstance(
                "mode=acscript" +
                ",client=jar=" + JAR_CLIENT_NAME +
                ",arg=-textauth" +
                ",arg=-user,arg=" + USER_VALUE);
        CommandLaunchInfo info = CommandLaunchInfo.newInstance(agentArgs);
        assertEquals("wrong client type", ClientLaunchType.JAR, info.getClientLaunchType());
        assertEquals("wrong client name", JAR_CLIENT_NAME, info.getClientName());

    }

    @Test
    public void testB() throws Exception, UserError {
        URL testFileURL = getClass().getResource(PASSWORDFILE_PATH);
        assertNotNull("test file URL came back null", testFileURL);
        File testFile = new File(testFileURL.toURI());
        final AgentArguments agentArgs = AgentArguments.newInstance(
                "mode=acscript" +
                ",client=dir=" + DIR_CLIENT_NAME +
                ",arg=-passwordfile,arg=" + testFile.getAbsolutePath() +
                ",arg=-noappinvoke");
        CommandLaunchInfo info = CommandLaunchInfo.newInstance(agentArgs);

        assertEquals("wrong client type", ClientLaunchType.DIR, info.getClientLaunchType());
        assertEquals("wrong client name", DIR_CLIENT_NAME, info.getClientName());
    }

}
