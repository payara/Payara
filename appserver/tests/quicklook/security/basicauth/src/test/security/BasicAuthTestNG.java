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

package test.security.basicauth;

import java.lang.*;
import java.io.*;
import java.net.*;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * A simple Web BASIC auth test.
 *
 */
@Test (groups={"pulse"})
public class BasicAuthTestNG {
    
    
    private static final String TEST_NAME =
        "security-basicauth-web";

    private String strContextRoot="basicauth";

    static String result = "";
    String host=System.getProperty("http.host");
    String portS=System.getProperty("http.port");
    int port = new Integer(portS).intValue();

    String testName;
        
    /**
     * Must be invoked with (host,port) args.
     * Nothing else is parameterized, this is intended as
     * throwaway after the SQE web test framework exists.
     * User/authorization info is hardcoded and must match
     * the values in descriptors and build.xml.
     *
     */
    @Test (groups = {"pulse"})
    public void testAuthRoleMappedUser() throws Exception {

       // System.out.println("Host ["+host+"] port ("+port+")");

        // GET with a user who maps directly to role
        testName="BASIC auth: Role Mapped User, testuser3";
        //log(testName);
        try {

            String result="RESULT: principal: testuser3";
            goGet(host, port, result,
                  "Authorization: Basic dGVzdHVzZXIzOnNlY3JldA==\n");
            Assert.assertTrue(true, testName);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            Assert.assertFalse(true, testName);
        }
    }
    
    @Test (groups = {"pulse"})
    public void testAuthGroupMappedUser() {
        
        // GET with a user who maps through group
        testName="BASIC auth: Group mapped user, testuser42";
        //log(testName);
        try {

            String result="RESULT: principal: testuser42";
            goGet(host, port, result,
                  "Authorization: Basic dGVzdHVzZXI0MjpzZWNyZXQ=\n");
            Assert.assertTrue(true, testName);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            Assert.assertFalse(true, testName);
        }
    }
    
    @Test (groups = {"pulse"})
    public void testAuthNotAuthorizedUser() {

        // GET with a valid user who is not authorized
        testName="BASIC auth: Not authorized user, testuser42";
        //log(testName);
        try {

            String result="HTTP/1.1 403";
            goGet(host, port, result,
                  "Authorization: Basic ajJlZTpqMmVl\n");
            Assert.assertTrue(true, testName);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            Assert.assertFalse(true, testName);
        }
    }
    
    @Test (groups = {"pulse"}) 
    public void testAuthNotValidPassword() {

        // GET with a valid user,bad password
        testName="BASIC auth: Valid user and invalid password";
        //log(testName);
        try {

            String result="HTTP/1.1 401";
            goGet(host, port, result,
                  "Authorization: Basic ajJlZTo=\n");
            Assert.assertTrue(true, testName);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            Assert.assertFalse(true, testName);
        }

    }

    /**
     * Connect to host:port and issue GET with given auth info.
     * This is hardcoded to expect the output that is generated
     * by the Test.jsp used in this test case.
     *
     */
    private static void goGet(String host, int port,
                              String result, String auth)
         throws Exception
    {
        Socket s = new Socket(host, port);
        OutputStream os = s.getOutputStream();

        os.write("GET /basicauth/Test.jsp HTTP/1.0\n".getBytes());
        os.write(auth.getBytes());
        os.write("\n".getBytes());
        
        InputStream is = s.getInputStream();
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        String line = null;

        while ((line = bis.readLine()) != null) {
            if (line.indexOf(result) != -1) {
                //System.out.println("  Found: "+line);
                s.close();
                return;
            }
        }

        s.close();
        throw new Exception("String not found: "+result);
    }
    
    private void log(String mesg) {
        System.out.println(mesg);
    }
  
}
