/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package test.web.jruby.helloworld;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;
import org.testng.Assert;

import java.io.*;
import java.net.*;
import java.util.*;

public class JRubyTestNG {

    private static final String TEST_NAME =
        "jruby-say-hello";

    private static final String EXPECTED_RESPONSE =
        "Welcome aboard";
    
    static String result = "";

    private String contextRoot = "helloworld";
    String host=System.getProperty("http.host");
    String port=System.getProperty("http.port");
    //String fileName="/tmp/JRuby_deploy.output";
    BufferedReader in = null;

    /*
     *If two asserts are mentioned in one method, then last assert is taken in
     *to account.
     *Each method can act as one test within one test suite
     */

    @BeforeTest
    @Test(groups ={ "pulse"} )
    public void deployTest() throws Exception {
       boolean result=true;
       File dir1 = new File (".");
       String fileName = dir1.getCanonicalPath() + "/deploy.output";
       //System.out.println("Deployment output file: " + fileName );
       try {
           in = new BufferedReader(new FileReader(fileName));
       } catch (FileNotFoundException e) {
           System.out.println("Could not open file " + fileName + " for reading ");
       }

       if(in != null) {
           String line;
           String testLine = null;        
           try {
              while (( line = in.readLine() )  != null ) {
                //System.out.println("The line read is: " + line);
            	if(line.indexOf("Command deploy failed")!=-1){
                  result=false;
             	  testLine = line;
                  Assert.fail("Deployment failed.");
           	  //System.out.println(testLine);
                } } 
           }catch(Exception e){
              e.printStackTrace();
              throw new Exception(e);
         }
       }
    }

    //@Parameters({ "hest", "port", "contextroot" })
    @Test(groups ={ "functional"} ) // test method
    //public void webtest(String host, String port, String contextroot) throws Exception{
    public void webtest() throws Exception{
        
        try{

        String testurl = "http://" + host  + ":" + port + "/"+ contextRoot+"/";
        URL url = new URL(testurl);
        //echo("Connecting to: " + url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();
        

        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        String line = null;
        boolean result=false;
        String testLine = null;        
        while ((line = input.readLine()) != null) {
              //System.out.println(line);
            if(line.indexOf(EXPECTED_RESPONSE)!=-1){
                result=true;
             testLine = line;
            }
          
        }        
                
        Assert.assertEquals(result, true,"Unexpected HTML");
               
        
        }catch(Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }

    }
    
    @Test(groups ={ "functional"} ) // test method
    public void controllerTest() throws Exception{
        try{

        String testurl = "http://" + host  + ":" + port + "/"+ contextRoot
                +"/say/hello";
        URL url = new URL(testurl);
        //echo("Connecting to: " + url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();
        

        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        String line = null;
        boolean result=false;
        String testLine = null;        
        while ((line = input.readLine()) != null) {
              //System.out.println(line);
            if(line.indexOf("Say#hello")!=-1){
                result=true;
             testLine = line;
            }
          
        }        
                
        Assert.assertEquals(result, true,"Unexpected HTML");
               
        
        }catch(Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
        
    }
    
    public static void echo(String msg) {
        System.out.println(msg);
    }

}
