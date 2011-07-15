/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package client;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.ws.*;
import service.web.example.calculator.*;
//import common.IncomeTaxDetails;
//import java.util.Hashtable;

public class Client extends HttpServlet {

//       @WebServiceRef(name="sun-web.serviceref/calculator") CalculatorService service;
       CalculatorService service = new CalculatorService();

       public void doGet(HttpServletRequest req, HttpServletResponse resp) 
		throws javax.servlet.ServletException {
           doPost(req, resp);
       }

       public void doPost(HttpServletRequest req, HttpServletResponse resp)
              throws javax.servlet.ServletException {
	    PrintWriter out=null;
            try {
                System.out.println(" Service is :" + service);
                resp.setContentType("text/html");
            	out = resp.getWriter();
                Calculator port = service.getCalculatorPort();
				IncomeTaxDetails itDetails = new IncomeTaxDetails();
				itDetails.setFirstName ( "bhavani");
				itDetails.setLastName ("s");
				itDetails.setAnnualIncome ( 400000);
				itDetails.setStatus ("salaried");

				long startTime = System.currentTimeMillis();
				long ret = 0;
				// Make 100 calls to see how much time it takes.
				//for(int i=0; i<1000; i++) {
					ret = port.calculateIncomeTax(itDetails
							, itDetails
							, itDetails
							, itDetails
							, itDetails
							, itDetails
							, itDetails
							, itDetails
							, itDetails
							, itDetails
							);
				//}
				long timeTaken = System.currentTimeMillis() - startTime;
				
                //int ret = port.add(1, 2);
		printSuccess("Your income tax is : Rs ", out,ret, timeTaken);
		startTime = System.currentTimeMillis();
		int k = port.add(505, 50);
				timeTaken = System.currentTimeMillis() - startTime;
		printSuccess("Sum of 505 and 50 is : ", out,k, timeTaken);

		startTime = System.currentTimeMillis();
		String hi = port.sayHi();
				timeTaken = System.currentTimeMillis() - startTime;
		printSuccess("Output from webservice : ", out, hi, timeTaken);

		startTime = System.currentTimeMillis();
		port.printHi();
				timeTaken = System.currentTimeMillis() - startTime;
		printSuccess("SUCCESS : ", out, "Webservice has successfully printed hi in server.log", timeTaken);

		startTime = System.currentTimeMillis();
		port.printHiToMe("JavaEEServiceEngine");
				timeTaken = System.currentTimeMillis() - startTime;
		printSuccess("SUCCESS : ", out, "Webservice has successfully printed hi to me in server.log", timeTaken);

            } catch(java.lang.Exception e) {
		//e.printStackTrace();
	    	printFailure(out, e.getMessage());
            } finally {
		if(out != null) {
                    out.flush();
                    out.close();
		}
	    }
       }

       public void printFailure(PrintWriter out, String errMsg) {
		if(out == null) return;
		out.println("<html>");
                out.println("<head>");
                out.println("<title>TestServlet</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<p>");
                out.println("Test FAILED: Error message - " + errMsg);
                out.println("</p>");
                out.println("</body>");
                out.println("</html>");
       }

       public void printSuccess(String message, PrintWriter out, long result, long timeTaken) {
		if(out == null) return;
                out.println("\n\n");
                out.println(message + result);
                out.println("Time taken to invoke the endpoint operation is  :  " + timeTaken + " milliseconds.");
       }

       public void printSuccess(String message, PrintWriter out, String result, long timeTaken) {
		if(out == null) return;
                out.println("\n\n");
                out.println(message + result);
                out.println("Time taken to invoke the endpoint operation is  :  " + timeTaken + " milliseconds.");
       }
}

