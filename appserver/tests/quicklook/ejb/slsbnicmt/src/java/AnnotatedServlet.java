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

package slsbnicmt;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.ejb.EJB;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class AnnotatedServlet extends HttpServlet {
   
    @EJB
    private AnnotatedEJB simpleEJB;
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        boolean status = false;
        try {
            
            out.println("-------AnnotatedServlet--------");  
            out.println("AnntatedServlet at " + request.getContextPath ());

            String testcase = request.getParameter("tc");
            out.println("testcase = " + testcase);
            if (testcase != null) {

	      if ("EJBInject".equals(testcase)){

		out.println("Simple EJB:");
		out.println("@EJB Injection="+simpleEJB);
		String simpleEJBName = null;
		
		if (simpleEJB != null) {
		  simpleEJBName = simpleEJB.getName();
		  out.println("@EJB.getName()=" + simpleEJBName);
		}

		if (simpleEJB != null &&
		    "foo".equals(simpleEJBName)){
		  status = true;
		}

	      } else if ("JpaPersist".equals(testcase)){
		
		if (simpleEJB != null) {
		  out.println("Persist Entity");
		  status  = simpleEJB.persistEntity();
		}

	      } else if ("JpaRemove".equals(testcase)){

		if (simpleEJB != null) {
		  out.println("Verify Persisted Entity and Remove Entity");
		  status  = simpleEJB.removeEntity();
		}

	      } else if ("JpaVerify".equals(testcase)){

		if (simpleEJB != null) {
		  out.println("Verify Removed Enitity");
		  status  = simpleEJB.verifyRemove();
		}

	      } else {
		out.println("No such testcase");
	      }
	  }
        } catch (Exception ex ) {
            ex.printStackTrace();
            System.out.println("servlet test failed");
            throw new ServletException(ex);
        } finally { 
            if (status) 
	      out.println("Test:Pass");
            else
	      out.println("Test:Fail");
            out.close();
        }
    } 

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 


    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    public String getServletInfo() {
        return "AnnontatedServlet";
    }

    private Object lookupField(String name) {
        try {
            return new InitialContext().lookup("java:comp/env/" + getClass().getName() + "/" + name);
        } catch (NamingException e) {
            return null;
        }
    }

}



