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

package test.web.jsfinjection;
import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.*;
import org.testng.Assert;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Simple TestNG client for basic WAR containing JSF page with injectited values.
 * Client checks for two values: injected string and injected number.
 * If both values are as expected test passes.
 */
public class JSFInjectionTestNG {

    private static final String TEST_NAME =
        "simple-webapp-jsf-injection";
   
    private String strContextRoot="jsfinjection";

    static String result = "";
    String host=System.getProperty("http.host");
    String port=System.getProperty("http.port");
           
    /*
     *If two asserts are mentioned in one method, then last assert is taken in
     *to account.
     *Each method can act as one test within one test suite
     */


    //@Parameters({ "host", "port", "contextroot" })
    @Test(groups ={ "pulse"} ) // test method
    public void injectedValuesTestPage() throws Exception {
        
      try {

        String errorText = "";
        boolean testPass = false;

        String testUrl = "http://" + host  + ":" + port + "/"+ strContextRoot + "/jsfHello.jsf";
        //echo("URL is: " + testUrl);
        URL url = new URL(testUrl);
        //echo("Connecting to: " + url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();
        if ( responseCode != 200 ) {
          //echo("ERROR: http response code is " + responseCode);
          errorText = errorText + "ERROR: http response code is " + responseCode + ".\n";
        } else {
          //echo("Connected: " + responseCode);
        }

        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        String line = null;
        String line2 = null;
        String line3 = null;
        boolean result=false;
	String EXPECTED_RESPONSE = "Injected entry";
	String DIVIDER = "===";
        String PC = "Postconstruct";
        while ((line = input.readLine()) != null) {
          //echo ("LINE:"+line);
          if (line.indexOf(EXPECTED_RESPONSE)!= -1) {
            testPass = true;
            //echo("Received line: " + line);
          }
          if (line.indexOf(DIVIDER)!= -1) {
            line2 = line;
            //echo("Received line2: " + line2);
          }
          if (line.indexOf(PC)!= -1) {
            line3 = line;
            //echo("Received line3: " + line3);
          }
        }

        if (! testPass) {
          echo("ERROR: injection 1 not found");
          errorText = errorText + "ERROR: injection 1 not found\n";
        }

        if (line2 != null) {
          String [] injection2Array = line2.split(DIVIDER);
          String injectedNumber = injection2Array[1].trim();
          //echo("injectedNumber = " + injectedNumber);
          int num = Integer.parseInt(injectedNumber);
  
          if ( num < 0 ) {
            echo("ERROR: injection 2 is less than zero.");
            errorText = errorText + "ERROR: injection 2 is less than zero";
            testPass = false;
          } else {
            echo("Injection2 matched.");
          }
        } else {
          echo("ERROR: line with " + DIVIDER + " not found.");
          errorText = errorText + "ERROR: line with " + DIVIDER + " not found";
          testPass = false;
        }

        if (line3 != null) {
            if (line3.contains("true")) {
                echo("@PostConstruct worked");
            } else {
                echo("ERROR:@PostConstruct failed");
                errorText = errorText + "ERROR: @PostConstruct failed";
                testPass = false;
            } 
        } else {
          echo("ERROR: line with " + PC + " not found.");
          errorText = errorText + "ERROR: line with " + PC + " not found";
          testPass = false;
        }

        Assert.assertEquals(testPass, true, errorText);
        
      }catch(Exception e){
        echo("ERROR: caught exception!");
        e.printStackTrace();
        throw new Exception(e);
      }
    }

    public static void echo(String msg) {
      System.out.println(msg);
    }
}
