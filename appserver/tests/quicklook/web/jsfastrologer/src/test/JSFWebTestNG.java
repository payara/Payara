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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package test.jsf.astrologer;

import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.*;
import org.testng.Assert;

// import org.apache.commons.httpclient.*;
// import org.apache.commons.httpclient.methods.*;
// import org.apache.commons.httpclient.params.*;
// import org.apache.commons.httpclient.cookie.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class JSFWebTestNG {

    private static final String TEST_NAME = "jsf-webapp";

    private static final String EXPECTED_RESPONSE = "JSP Page Test";

    private String strContextRoot = "jsfastrologer";

    String m_host = "";
    String m_port = "";
    
    @BeforeMethod
    public void beforeTest() {
        m_host = System.getProperty("http.host");
        m_port = System.getProperty("http.port");
    }

    /*
     * If tw o asserts are mentioned in one method, then last assert is taken in to account. Each method
     * can act as one test within one test suite
     */

    @Test(groups = { "pulse" }) // test method
    public void jsfAppDeployedFirstPagetest() throws Exception {

        try {
            String testurl = "http://" + m_host + ":" + m_port + "/" + strContextRoot + "/faces/greetings.jsp";
            
            URL url = new URL(testurl);
            echo("Connecting to: " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            int responseCode = conn.getResponseCode();

            InputStream is = conn.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(is));

            String line = null;
            boolean result = false;
            String testLine = null;
            while ((line = input.readLine()) != null) {
                if (line.indexOf("Welcome to jAstrologer") != -1) {
                    result = true;
                    testLine = line;
                }
            }

            Assert.assertEquals(result, true, "Unexpected HTML");

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    @Test(groups = { "pulse" }) // test method
    public void jsfIndexPageBasicTest() throws Exception {
        try {
            String testurl = "http://" + m_host + ":" + m_port + "/" + strContextRoot + "/index.jsp";
            
            URL url = new URL(testurl);
            echo("Connecting to: " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            int responseCode = conn.getResponseCode();

            InputStream is = conn.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(is));

            String line = null;
            boolean result = false;
            String testLine = null;
            while ((line = input.readLine()) != null) {
                if (line.indexOf("JavaServer Faces Greetings Page") != -1) {
                    result = true;
                    testLine = line;
                    // System.out.println(testLine);
                }

            }

            Assert.assertEquals(result, true);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    public static void echo(String msg) {
        System.out.println(msg);
    }

}
