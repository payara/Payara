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

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import java.util.*;
import com.sun.enterprise.tools.verifier.tests.*;
import java.lang.reflect.*;

/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids:  JSR109_WS_43; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 *   @testDescription: A servlet must only be linked to by a single port-component. 
 */

public class ServletLinkedToOnePortCompCheck  extends WSTest implements WSCheck {

    /**
     * @param descriptor the WebServices  descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check (WebServiceEndpoint descriptor) {

	Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (descriptor.implementedByEjbComponent()) {
           result.addNaDetails(smh.getLocalString
                     ("tests.componentNameConstructor", "For [ {0} ]",
                      new Object[] {compName.toString()}));
           result.notApplicable(smh.getLocalString
                 (getClass().getName() + ".notapp",
                 "This is an EJB Service Endpoint"));
           return result;
        }

    
        if (isLinkedToSinglePortComp(getAllEndPointsInApp(descriptor),descriptor.getLinkName())) {
           // result.pass
           result.addGoodDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
           result.passed(smh.getLocalString
                   (getClass().getName() + ".passed",
           "The Servlet associated with this end-point is linked to by a single port-component."));

        }
        else {
          // result.fail
          result.addErrorDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
          result.failed(smh.getLocalString
                (getClass().getName() + ".failed",
                "The Servlet associated with this end-point is linked to by multiple port-components."));

        }

        return result;
    }

    Collection getAllEndPointsInApp(WebServiceEndpoint desc) {
       Collection allWebServices = desc.getWebService().getWebServicesDescriptor().getWebServices();
       Collection ret = new Vector();
       for (Iterator it = allWebServices.iterator(); it.hasNext();) {
           ret.addAll(((WebService)it.next()).getEndpoints());
       }

     return ret;
    }

    // the compLink here is either an ejb-link or a servlet-link
    boolean isLinkedToSinglePortComp(Collection endPoints, String compLink) {
       boolean single = true;
       boolean linkAlreadySeen = false;
       for (Iterator it = endPoints.iterator(); it.hasNext();) {
           String myCompLink = ((WebServiceEndpoint)it.next()).getLinkName();

           if (myCompLink.equals(compLink)) {
              if (!linkAlreadySeen) {
                 linkAlreadySeen = true;
              }
              else {
                 single = false;
                 break;
              }
           }
       }
     return single;
    }
 }

