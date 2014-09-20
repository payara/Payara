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

/*
 * PersistenceTestNG.java
 *
 * Created on March 18, 2008, 8:36 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test;

import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.*;
import org.testng.Assert;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.cookie.*;

import java.io.*;
import java.net.*;
import java.util.*;


public class PersistenceTestNG {
    
   private String strContextRoot="persistence";

    static String result = "";
    String m_host=System.getProperty("http.host");
    String m_port=System.getProperty("http.port"); 
    HttpClient httpclient = new HttpClient();
    
    
    //@Parameters({"host","port"})
    @BeforeMethod
    public void beforeTest(){
        m_host=System.getProperty("http.host");
        m_port=System.getProperty("http.port");
        
    }
    
    @Test(groups={"pulse"})
    public void testIndexPage() throws Exception{
        GetMethod method=null;
        System.out.println("Testing Index Page..");
        String comparedString="Hello-Java Persistence API Finder Test";
        try{
            System.out.println("Running method testRequestResponse");
            String testurl = "http://" + m_host  + ":" + m_port +
                    "/"+ strContextRoot + "/index.jsp";    
            
            System.out.println("running URL-->"+testurl);
            
            
            method = new GetMethod(testurl);
           
            
            int statusCode = httpclient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: " + method.getStatusLine());
            } else
                System.out.println("print status ok "+statusCode);
            
            // Read the response body.
            byte[] responseBody = method.getResponseBody();
            String responseHTML=new String(responseBody);
            System.out.println(responseHTML);
            
            boolean result=false;
            
            if(responseHTML.indexOf(comparedString)!=-1){
                result=true;                
            }
                        
            System.out.println("testIndexPage result :"+result);
            Assert.assertEquals(result,true);
            
        }catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } 
        finally {
            // Release the connection.
            method.releaseConnection();
        }
    }
    
    @Test(groups={"pulse"})
    public void testServletFormMethod() throws Exception{
        
        GetMethod method=null;
        System.out.println("Testing Customer Details Page..");
        String comparedString="Search Customer Information";
        try{
        
        String testurl = "http://" + m_host  + ":" + m_port +
                    "/"+ strContextRoot + "/details";
        
        System.out.println("URL is:"+testurl);
        method = new GetMethod(testurl);
           
            
            int statusCode = httpclient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: " + method.getStatusLine());
            } else
                System.out.println("print status ok "+statusCode);
        
        byte[] responseBody = method.getResponseBody();
            String responseHTML=new String(responseBody);
            //System.out.println(responseHTML);
            
            boolean result=false;
            
            if(responseHTML.indexOf(comparedString)!=-1){
                result=true;
                System.out.println(result);
            }
            System.out.println("testServletForm result :"+result);
            Assert.assertEquals(result,true);
        }catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } 
        finally {
            // Release the connection.
            method.releaseConnection();
        }
    }
    
    @Test(groups={"pulse"})
    public void testFinderMethod() throws Exception{
        
        GetMethod method=null;
        String comparedString="Alice_1";
        System.out.println("Testing Finder JPA method..");
        try{
            
            String testurl = "http://" + m_host  + ":" + m_port +
                    "/"+ strContextRoot + "/details?customer_nr=1";
            System.out.println("finding details of customer 1..");
            System.out.println("URL is:"+testurl);
            System.out.println("Response should be Alice_1");
            
            method = new GetMethod(testurl);
           
            
            int statusCode = httpclient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: " + method.getStatusLine());
            } else
                System.out.println("print status ok "+statusCode);  
            
            byte[] responseBody = method.getResponseBody();
            String responseHTML=new String(responseBody);
            System.out.println(responseHTML);
            
            boolean result=false;
            
            if(responseHTML.indexOf(comparedString)!=-1){
                result=true;
                System.out.println(result);
            }
            System.out.println("testFinderMethod result :"+result);
            Assert.assertEquals(result,true);
            
        }catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } 
        finally {
            // Release the connection.
            method.releaseConnection();
        }
    }
}
            
 /*           String name="testuser";
            String birthday="121212";
            System.out.println("URL is: "+testurl);
            GetMethod httpget=null;
            PostMethod post=null;
            httpget = new GetMethod(testurl);
            post=new PostMethod("http://localhost:8080/jsfastrologer/faces/greetings.jsp");

            
            NameValuePair[] mydata = {
                // new NameValuePair("loginID", itUser),
                // new NameValuePair("password", itPwd), Not working for editing of bug
                
                new NameValuePair("name",name),
                new NameValuePair("birthday",birthday)
            };
            
            post.setRequestBody(mydata);
            int statusCode = httpclient.executeMethod(post);
            System.out.println("print status ok "+statusCode);
             Assert.assertEquals(statusCode, 200);
            
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: " + post.getStatusLine());
            }
            post.getStatusLine();
        
        String response=post.getResponseBodyAsString();
        System.out.println(response);
            
            
        }catch(Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }
        
    }

}
    
    
    
}*/
