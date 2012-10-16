/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package test.osgi.hello;

import com.sun.appserv.test.AdminBaseDevTest;
import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.*;
import org.testng.Assert;
import java.io.*;
import java.lang.String;
import java.lang.System;
import java.net.*;
import java.util.*;


public class HelloOSGITestNG extends AdminBaseDevTest{

    @Override
    protected String getTestDescription() {
        return "OSGI simple Test";
    }

    final String tn = "OSGI";
    private static final String TEST_NAME = "osgi-webapp-test";
    static String BASEDIR = System.getProperty("BASEDIR");
    public boolean retStatus = false;
    final String cname = "osgi";
    final String flag = "true";
    final String options = "UriScheme=webBundle:Bundle-SymbolicName=bar:Import-Package=javax.servlet;javax.servlet.http:Web-ContextPath=/osgitest";

    private String strContextRoot="osgitest";

    static String result = "";
    String host=System.getProperty("http.host");
    String port=System.getProperty("http.port");
           

    @Test(groups ={ "osgi"} ) // test method
    public void simpleOSGIDeployTest() throws Exception{

        // deploy web application.
        File webapp = new File(BASEDIR+"/dist/basicosgi", "osgitest.war");
        retStatus = report(tn + "deploy", asadmin("deploy", "--type", cname, "--properties", options, webapp.getAbsolutePath()));
        Assert.assertEquals(retStatus, true, "App deployment failed ...");

    }

    @Test(groups ={ "osgi"},dependsOnMethods = { "simpleOSGIDeployTest" } ) // test method
     public void simpleJSPTestPage() throws Exception{

        try{
         Thread.currentThread().sleep(5000);

        String testurl = "http://" + host  + ":" + port + "/"+ strContextRoot + "/hello.jsp";
        URL url = new URL(testurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();
        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        String line = null;
        boolean result=false;
        String testLine = null;
	    String EXPECTED_RESPONSE ="JSP Test Page";
        while ((line = input.readLine()) != null) {
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

    @Test(groups ={ "osgi"},dependsOnMethods = { "simpleJSPTestPage" }  ) // test method
    public void simpleServletTest() throws Exception{
         try{
        String testurl = "http://" + host  + ":" + port + "/"+ strContextRoot + "/simpleservlet";
        URL url = new URL(testurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();
        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        String line = null;
        boolean result=false;
        String testLine = null;
        while ((line = input.readLine()) != null) {
            if(line.indexOf("Sample Application Servlet")!=-1){
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


    @Test(groups ={ "osgi"},dependsOnMethods = { "simpleServletTest" } ) // test method
    public void simpleOSGIUnDeployTest() throws Exception{
        // undeploy web application.
         retStatus = report(tn + "undeploy", asadmin("undeploy","osgitest"));
         Assert.assertEquals(retStatus, true, "App Undeployment failed ...");;

    }

}
