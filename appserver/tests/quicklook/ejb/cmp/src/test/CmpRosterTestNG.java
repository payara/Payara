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
// Portions Copyright [2020] Payara Foundation and/or affiliates

package test;
import org.testng.annotations.*;
import org.testng.Assert;

import java.io.*;
import java.net.*;

/**
 * Simple TestNG client for basic WAR containing one JSP,one Servlet and one static
 *HTML resource.Each resources (HTML,JSP,Servlet) is invoked as a separate test.
 *
 */
public class CmpRosterTestNG {

    private static final String TEST_NAME = "Roster";
   
    private final String strContextRoot="roster";

    static String result = "";
    String host=System.getProperty("http.host");
    String port=System.getProperty("http.port");
           
    /*
     *If two asserts are mentioned in one method, then last assert is taken in
     *to account.
     *Each method can act as one test within one test suite
     */


    @Test(groups = {"pulse"}) // test method
    public void test() throws Exception {

        try {

            String testurl = "http://" + host + ":" + port + "/" + strContextRoot + "/" + TEST_NAME;

            URL url = new URL(testurl);
            //echo("Connecting to: " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            int responseCode = conn.getResponseCode();

            InputStream is = conn.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(is));

            String line = null;
            boolean result = false;
            String testLine = null;
            String EXPECTED_RESPONSE = "ROSTER-FINISHED-OK";
            while ((line = input.readLine()) != null) {
                // echo(line);
                if (line.contains(EXPECTED_RESPONSE)) {
                    testLine = line;
                    //echo(testLine);
                    result = true;
                    break;
                }
            }

            Assert.assertEquals(result, true, "Unexpected Results");

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    public static void echo(String msg) {
        System.out.println(msg);
    }

}
