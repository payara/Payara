/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.universal.xml.MiniXmlParserException;
import java.io.*;
import java.util.*;
import org.glassfish.api.admin.RuntimeType;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class GFLauncherTest {

    public GFLauncherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        ClassLoader cl = GFLauncherTest.class.getClassLoader();

        File asenv = new File(cl.getResource("config/asenv.bat").toURI());
        installDir = asenv.getParentFile().getParentFile();
        domainsDir = new File(installDir, "domains");
        assertTrue("domain1 -- domain.xml is missing!!",
                new File(domainsDir, "domain1/config/domain.xml").exists());
        assertTrue("domain2 -- domain.xml is missing!!",
                new File(domainsDir, "domain2/config/domain.xml").exists());
        assertTrue("domain3 -- domain.xml is missing!!",
                new File(domainsDir, "domain3/config/domain.xml").exists());
        assertTrue("baddomain -- domain.xml is missing!!",
                new File(domainsDir, "baddomain/config/domain.xml").exists());
        assertTrue("domainNoLog -- domain.xml is missing!!",
                new File(domainsDir, "domainNoLog/config/domain.xml").exists());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws GFLauncherException {
        launcher = GFLauncherFactory.getInstance(RuntimeType.DAS);
        info = launcher.getInfo();
        info.setInstallDir(installDir);
        launcher.setMode(GFLauncher.LaunchType.fake);
    }

    @After
    public void tearDown() {
    }


    /**
     * First Test -- Fake Launch the default domain in the default domain dir
     * Since we have more than 1 domain in there -- it should fail!
     */
    @Test(expected=GFLauncherException.class)
    public void test1() throws GFLauncherException, MiniXmlParserException {
        launcher.launch();
    }
    /**
     * Let's fake-launch domain1  -- which DOES have the jvm logging args
     */

    @Test
    public void test2() throws GFLauncherException, MiniXmlParserException {
        info.setDomainName("domain1");
        launcher.launch();
        List<String> cmdline = launcher.getCommandLine();

        assertTrue(cmdline.contains("-XX:+UnlockDiagnosticVMOptions"));
        // 0 --> java, 1 --> "-cp" 2 --> the classpath, 3 -->first arg
        assertEquals(cmdline.get(3), "-XX:+UnlockDiagnosticVMOptions");
        
        /* Too noisy, todo figure out how to get it into the test report
        System.out.println("COMMANDLINE:");
        for(String s : cmdline) {
            System.out.println(s);
        }
         */
    }

    /**
     * Let's fake-launch domain2 -- which does NOT have the jvm logging args
     */

    @Test
    public void test3() throws GFLauncherException, MiniXmlParserException {
        info.setDomainName("domain2");
        launcher.launch();
        List<String> cmdline = launcher.getCommandLine();
        assertFalse(cmdline.contains("-XX:+UnlockDiagnosticVMOptions"));

        /*
        System.out.println("COMMANDLINE:");
        for(String s : cmdline) {
            System.out.println(s);
        }
         */
    }

    /**
     * Let's fake-launch a domain that doesn't exist
     * it has an XML error in it.
     */
    @Test(expected=GFLauncherException.class)
    public void test4() throws GFLauncherException, MiniXmlParserException {
        info.setDomainName("NoSuchDomain");
        launcher.launch();
        List<String> cmdline = launcher.getCommandLine();

        System.out.println("COMMANDLINE:");
        for(String s : cmdline) {
            System.out.println(s);
        }
    }
    /**
     * Let's fake-launch baddomain
     * it has an XML error in it.
     */
    @Test(expected=GFLauncherException.class)
    public void test5() throws GFLauncherException, MiniXmlParserException {
        info.setDomainName("baddomain");
        launcher.launch();
        List<String> cmdline = launcher.getCommandLine();

        System.out.println("COMMANDLINE:");
        for(String s : cmdline) {
            System.out.println(s);
        }
    }

    /**
     * Test the logfilename handling -- log-service is in domain.xml like V2
     */
    @Test
    public void test6() throws GFLauncherException {
        info.setDomainName("domain1");
        launcher.launch();
        assertTrue(launcher.getLogFilename().endsWith("server.log"));
    }

    /**
     * Test the logfilename handling -- no log-service is in domain.xml
     */

    @Test
    public void test7() throws GFLauncherException {
        info.setDomainName("domainNoLog");
        launcher.launch();
        assertTrue(launcher.getLogFilename().endsWith("server.log"));
    }

    @Test
    public void testDropInterruptedCommands() throws GFLauncherException {
        info.setDomainName("domainNoLog");
        info.setDropInterruptedCommands(true);
        launcher.launch();
        assertTrue(launcher.getJvmOptions().contains("-Dorg.glassfish.job-manager.drop-interrupted-commands=true"));
    }

    //private static File domain1, domain2, domain3, domain4, domain5;
    private static File installDir;
    private static File domainsDir;
    private GFLauncher launcher;
    private GFLauncherInfo info;
}
