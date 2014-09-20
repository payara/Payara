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

package com.sun.enterprise.tools.verifier.tests.webservices;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids: 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription:  Verify that the schemaLocation in webservices.xml matches the schema file 
 *                      requirement. 
 *
 *        All webservices deployment descriptors must indicate the
 *        webservices schema by using the J2EE namespace:
 *
 *        http://java.sun.com/xml/ns/j2ee
 *
 *       and by indicating the version of the schema by using the version
 *       element as shown below:
 *
 *            <webservices xmlns="http://java.sun.com/xml/ns/j2ee"
 *              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *              xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee
 *                http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd"
 *              version="1.1">
 *              ...
 *            </webservices>
 *
 *
 *   schemaLocation should be:
 *                              xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee 
 *                              http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd">
 *
 *   A Web services deployment descriptor is located in a WAR at WEB-INF/webservices.xml.
 */

public class WSSchemaLocation extends WSTest implements WSCheck {
    String myValue = null;
    String[] reqSchemaLocation =
                    {"http://java.sun.com/xml/ns/j2ee http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd",
                     "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/javaee_web_services_1_2.xsd",
                     "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd"};
    int i;
    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String[] reqSchemaLocationSub1 =
                    {"http://java.sun.com/xml/ns/j2ee", "http://java.sun.com/xml/ns/javaee"};
        String[] reqSchemaLocationSub2 =
                    {"http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd",
                     "http://java.sun.com/xml/ns/javaee/javaee_web_services_1_2.xsd",
                     "http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd"};
        boolean rslt = false;
        String schemaVersion = getVerifierContext().getSchemaVersion();
        Document wsdoc=getVerifierContext().getWebServiceDocument();
        //with jax-ws it is not mandatory to define webservices.xml deployment descriptor
        if (wsdoc == null && schemaVersion.compareTo("1.1") > 0) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString(getClass().getName() + ".passed1",
                    "Webservices deployment descriptor is not defined for this archive"));
            return result;
        }

        try {
           if (wsdoc.getDocumentElement().hasAttributes()) {
               getNode(wsdoc);
               if ( myValue != null) {
                   for(i=0; i<reqSchemaLocation.length; i++) {
                       rslt = verifySchema(myValue, reqSchemaLocation[i],
                               reqSchemaLocationSub1[i], reqSchemaLocationSub2[i]);
                       if(rslt) break;
                   }
               } else {
                   rslt = true; // schemaLocation is optional
               }
           }
           if (rslt) {
              result.addGoodDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
              result.passed(smh.getLocalString (getClass().getName() + ".passed",
                          "The schemaLocation in the webservices.xml file for [{0}] matches the schema file requirement",
                           new Object[] {compName.toString()}));
            }
            else {
             result.addErrorDetails(smh.getLocalString ("tests.componentNameConstructor",
                                   "For [ {0} ]", new Object[] {compName.toString()}));
             result.failed(smh.getLocalString (getClass().getName() + ".failed",
               "The schemaLocation in the webservices.xml file for [{0}] does not match the schema file requirement",
                new Object[] {compName.toString()}));
            }
        }catch (Exception e) {
            //result.fail
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
               "The schemaLocation in the webservices.xml file for [{0}] does not match the schema file requirement",
                new Object[] {compName.toString()}));
            result.addErrorDetails(smh.getLocalString
               ("com.sun.enterprise.tools.verifier.tests.webservices.Error",
                "Error: Unexpected error occurred [ {0} ]",
                new Object[] {e.getMessage()}));
        }
        return result;
    }

    private boolean verifySchema(String nodeval, String reqSchemaLocation, 
                                 String reqSchemaLocationSub1, String reqSchemaLocationSub2){
      try {
           int off1 = reqSchemaLocation.indexOf("http", reqSchemaLocationSub1.length());
           int off2 = nodeval.indexOf("http",reqSchemaLocationSub1.length());
           // 1. separate into 2 substrings and verify
           if (  checkSubString(nodeval, 0,0,reqSchemaLocationSub1.length())
                 && checkSubString(nodeval, off1,off2,reqSchemaLocationSub2.length()) ) {
                    ; // ok so far  
           }
           else {
                // no need to go further,  strings are not correct
                return false; 
           }

           // 2. make sure that there is at least a space between the 2 substrings
           if ( reqSchemaLocation.length() > nodeval.length())
              return false;

          // 3. Make sure that there is only whitespace as separation between the two substrings
          // java whitespace is one of the following:
          /*
          It is a Unicode space character (SPACE_SEPARATOR, LINE_SEPARATOR, or
          PARAGRAPH_SEPARATOR) but is not also a non-breaking space ('\u00A0', '\u2007',
          '\u202F').

          It is '\u0009', HORIZONTAL TABULATION.
          It is '\u000A', LINE FEED.
          It is '\u000B', VERTICAL TABULATION.
          It is '\u000C', FORM FEED.
          It is '\u000D', CARRIAGE RETURN.
          It is '\u001C', FILE SEPARATOR.
          It is '\u001D', GROUP SEPARATOR.
          It is '\u001E', RECORD SEPARATOR.
          It is '\u001F', UNIT SEPARATOR.

          */

         for ( int i = reqSchemaLocationSub1.length(); i < off2; i++) {
            if ( !(Character.isWhitespace(nodeval.charAt(i))) ) {
              return false;
            } 
          }

    }catch (Exception e) {
            e.toString();
            e.printStackTrace();
    } 
    return true;
 }
    private boolean checkSubString(String str , int off1, int off2, int len) {
                if (reqSchemaLocation[i].regionMatches(off1, str, off2, len)) {
                        return true;
                }
                return false;
           }

  public void getNode(Node node) {

    String name = node.getNodeName(); 
    int myType = node.getNodeType();
    // type 1 = Element
    if ( ( myType == 1 ) & (name.equals("webservices") )) {
       Element e = (Element)node;
       myValue = e.getAttribute("xsi:schemaLocation");
       return;
    }
    if (node.hasChildNodes()) {
      Node firstChild = node.getFirstChild();
      getNode(firstChild);
    }
    Node nextNode = node.getNextSibling();
    if (nextNode != null) getNode(nextNode);
    return ;
  }
}

