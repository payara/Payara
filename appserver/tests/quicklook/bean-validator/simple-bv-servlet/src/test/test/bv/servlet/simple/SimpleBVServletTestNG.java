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

package test.bv.servlet.simple;

import org.testng.annotations.*;
import org.testng.Assert;

import java.io.*;
import java.net.*;

public class SimpleBVServletTestNG {

    private static final String TEST_NAME =
        "bv-servlet-simple";
   
    private String strContextRoot="simple-bv-servlet";

    static String result = "";
    String host=System.getProperty("http.host");
    String port=System.getProperty("http.port");

    public SimpleBVServletTestNG() {
        result = null;
    }
    
    
           
    /*
     *If two asserts are mentioned in one method, then last assert is taken in
     *to account.
     *Each method can act as one test within one test suite
     */


    //@Parameters({ "host", "port", "contextroot" })
    @Test(groups ={ "pulse"} ) // test method
    //public void webtest(String host, String port, String contextroot) throws Exception{
    public void executeServlet() throws Exception{
        
        try{

            String testurl = "http://" + host + ":" + port + "/" + strContextRoot + "/test";
            //System.out.println("URL is: "+testurl);
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
            String[] regexesToFind = {
		"(?s)(?m).*Obtained ValidatorFactory: org.hibernate.validator.(internal.)*engine.ValidatorFactoryImpl.*",
                "(?s)(?m).*case1: No ConstraintViolations found.*",
                "(?s)(?m).*case2: caught IllegalArgumentException.*",
                "(?s)(?m).*case3: ConstraintViolation: message: may not be null propertyPath: listOfString.*",
                "(?s)(?m).*case3: ConstraintViolation: message: may not be null propertyPath: lastName.*",
                "(?s)(?m).*case3: ConstraintViolation: message: may not be null propertyPath: firstName.*",
                "(?s)(?m).*case4: No ConstraintViolations found.*"
            };
            final int len = regexesToFind.length;
            int i;
            Boolean regexesFound[] = new Boolean[len];

            StringBuilder rspContent = new StringBuilder();
            while ((line = input.readLine()) != null) {
                rspContent.append(line);
                rspContent.append("\n ");

                // for each line in the input, loop through each of the 
                // elements of regexesToFind.  At least one must match.
                boolean found = false;
                for (i = 0; i < len; i++) {
                    if (found = line.matches(regexesToFind[i])) {
                        regexesFound[i] = Boolean.TRUE;
                    }
                }
            }
            
            boolean foundMissingRegexMatch = false;
            String errorMessage = null;
            for (i = 0; i < len; i++) {
                if (null == regexesFound[i] ||
                    Boolean.FALSE == regexesFound[i]) {
                    foundMissingRegexMatch = true;
                    errorMessage = "Unable to find match for regex " + 
                            regexesToFind[i] + " in output from request to " + testurl;
                    System.out.println("Response content: ");
                    System.out.println(rspContent.toString());
                    break;
                }
            }
            Assert.assertTrue(!foundMissingRegexMatch, errorMessage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    public static void echo(String msg) {
        System.out.println(msg);
    }

}
