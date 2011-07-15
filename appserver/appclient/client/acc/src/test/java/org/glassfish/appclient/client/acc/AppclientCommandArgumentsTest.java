/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Properties;
import java.io.File;
import com.sun.enterprise.transaction.JavaEETransactionManagerSimplified;
import com.sun.logging.LogDomains;
import java.io.FileWriter;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.MissingResourceException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tjquinn
 */
public class AppclientCommandArgumentsTest {

    private static final String USER_VALUE = "joe-the-user";
    private static final String PASSWORDFILE_NAME = "topSecret.stuff";
    private static final String EXPECTED_TARGETSERVER_VALUE = "A:1234,B:5678";
    private static final String EXPECTED_PASSWORD_IN_PASSWORD_FILE = "mySecretPassword";

    public AppclientCommandArgumentsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testA() throws Exception, UserError {
        AppclientCommandArguments info = AppclientCommandArguments.newInstance(
                Arrays.asList("-textauth", "-user", USER_VALUE));

        assertEquals("text auth not set", true, info.isTextauth());
        assertEquals("noappinvoke incorrectly set", false, info.isNoappinvoke());
        assertEquals("user incorrectly set", USER_VALUE, info.getUser());
        /*
         * Make sure an unspecified argument is repoted as null.
         */
        assertEquals("mainclass wrong", null, info.getMainclass());

    }

    @Test
    public void testB() throws Exception, UserError {
        AppclientCommandArguments info = AppclientCommandArguments.newInstance(
                Arrays.asList("-mainclass", PASSWORDFILE_NAME, "-noappinvoke"));


        assertEquals("wrong main class", PASSWORDFILE_NAME, info.getMainclass());
        assertEquals("noappinvoke not set", true, info.isNoappinvoke());
        assertEquals("user should have been null", null, info.getUser());
        assertEquals("textauth found but should be absent", false, info.isTextauth());
    }

    @Ignore
    @Test
    public void invalidArgumentTest() throws Exception, UserError {
        try {
            AppclientCommandArguments.newInstance(
                Arrays.asList("-badarg"));
            fail("did not throw expected IllegalArgumentException due to an invalid arg");
        } catch (IllegalArgumentException e) {
            // no-op so test passes
        } catch (Exception e) {
            fail("expected IllegalArgumentException but got " + e.getClass().getName());
        }
    }

    @Test
    public void disallowMainclassAndName() throws Exception {
        try {
            AppclientCommandArguments.newInstance(
                    Arrays.asList("-mainclass","x.y.Main","-name","some-display-name"));
            fail("did not detect incorrect spec of mainclass and name");
        } catch (UserError e) {
            // suppress exception for a successful test
        }
    }

    @Test
    public void allowMultiValuedTargetServer() throws Exception, UserError {
        final AppclientCommandArguments cmdArgs = AppclientCommandArguments.newInstance(
                Arrays.asList("-targetserver","\"" + EXPECTED_TARGETSERVER_VALUE + "\""));
        assertEquals("did not process targetserver list correctly",
                EXPECTED_TARGETSERVER_VALUE,
                cmdArgs.getTargetServer());
    }

    @Test
    public void useTransactionLogString() {
        final Logger logger = LogDomains.getLogger(JavaEETransactionManagerSimplified.class,
                LogDomains.JTA_LOGGER);
        final String target = "enterprise_used_delegate_name";
        try {
            final String result = logger.getResourceBundle().getString(target);
            assertTrue("message key look-up failed", (result != null &&
                    ! target.equals(result)));
        } catch (MissingResourceException ex) {
            fail("could not find message key");
        }
    }
    
    @Test
    public void checkPasswordInFile() {
        final Properties props = new Properties();
        props.setProperty(AppclientCommandArguments.PASSWORD_FILE_PASSWORD_KEYWORD, 
                EXPECTED_PASSWORD_IN_PASSWORD_FILE);
        try {
            final AppclientCommandArguments cmdArgs = prepareWithPWFile(props);
            final char[] pwInObject = cmdArgs.getPassword();
            assertTrue("Password " + EXPECTED_PASSWORD_IN_PASSWORD_FILE + 
                    " in password file does not match password " + new String(pwInObject) + 
                    " returned from AppclientCommandArguments object", 
                    Arrays.equals(pwInObject, EXPECTED_PASSWORD_IN_PASSWORD_FILE.toCharArray()));
            
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    
    
    @Test
    public void checkErrorHandlingIfRequiredPasswordInPasswordFileIsMissing() {
        final Properties props = new Properties();
        props.setProperty("UNEXPECTED", EXPECTED_PASSWORD_IN_PASSWORD_FILE);
        try {
            final AppclientCommandArguments cmdArgs = prepareWithPWFile(props);
            fail("Missing password in password file NOT correctly detected and flagged");
        } catch (UserError ue) {
            /*
             * This is what we expect - a UserError complaining about the 
             * missing password value.
             */
            return;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private AppclientCommandArguments prepareWithPWFile(
            final Properties props) throws UserError, IOException {
        final File passwordFile = createTempPWFile(props);
        
        return AppclientCommandArguments.newInstance(
                Arrays.asList("-passwordfile","\"" + passwordFile.getAbsolutePath() + "\""));
    }
    
    private File createTempPWFile(final Properties props) throws IOException {
        final File tempFile = File.createTempFile("accpw", ".txt");
        props.store(new FileWriter(tempFile), "temp file for acc unit test");
        tempFile.deleteOnExit();
        return tempFile;
    }

    
}
