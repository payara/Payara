/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client;

import org.glassfish.appclient.client.acc.UserError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Quinn
 */
public class CLIBootstrapTest {

    public CLIBootstrapTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        System.setProperty(CLIBootstrap.ENV_VAR_PROP_PREFIX + "AS_JAVA", "");
        System.setProperty(CLIBootstrap.ENV_VAR_PROP_PREFIX + "JAVA_HOME", "");
        System.setProperty(CLIBootstrap.ENV_VAR_PROP_PREFIX + "PATH",
                System.getenv("PATH"));
        System.setProperty(CLIBootstrap.ENV_VAR_PROP_PREFIX + "_AS_INSTALL",
                "/Users/Tim/asgroup/v3/H/publish/glassfish4/glassfish");
    }

    @After
    public void tearDown() {
    }

    @Ignore
    @Test
    public void testChooseJavaASJAVAAsCurrent() {
        runTest("AS_JAVA");
    }

    @Ignore
    @Test
    public void testChooseJavaJAVAHOMEAsCurrent() {
        runTest("JAVA_HOME");
    }


    @Ignore
    @Test
    public void testChooseJavaASJAVAAsBad() {
        runTestUsingBadLocation("AS_JAVA");
    }
    
    @Ignore
    @Test
    public void testChooseJAVAHOMEAsBad() {
        runTestUsingBadLocation("JAVA_HOME");
    }
    
    private void runTestUsingBadLocation(final String envVarName) {
        try {
            final CLIBootstrap boot = new CLIBootstrap();
            System.setProperty(CLIBootstrap.ENV_VAR_PROP_PREFIX + envVarName,
                        "shouldnotexistanywhere");
            CLIBootstrap.JavaInfo javaInfo = boot.initJava();
            
        } catch (UserError ex) {
            /*
             * We expect this exception because we tried to use a non-sensical
             * setting for the java location.
             */
        }
    }

    private void runTest(final String envVarName) {
        System.setProperty(CLIBootstrap.ENV_VAR_PROP_PREFIX + envVarName,
                       System.getProperty("java.home"));
        try {
            final CLIBootstrap boot = new CLIBootstrap();
            CLIBootstrap.JavaInfo javaInfo = boot.initJava();
            if (javaInfo == null) {
                fail("chooseJava found no match; expected to match on " + envVarName);
            }
            if ( ! javaInfo.toString().equals(envVarName)) {
                fail("Expected to choose " + envVarName + " but chose " + javaInfo.toString() + " instead");
            }
            if ( ! javaInfo.isValid()) {
                fail("Correctly chose " + envVarName + " but it should have been valid, derived as it was from PATH, but was not");
            }
        } catch (UserError ex) {
            fail(ex.getMessage());
        }
    }

}
