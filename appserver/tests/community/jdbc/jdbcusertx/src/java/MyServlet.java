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

package myapp;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.util.Map;
import javax.annotation.Resource;
import myapp.test.SimpleTest;
import myapp.util.HtmlUtil;

public class MyServlet extends HttpServlet {

    @Resource(name = "jdbc/__default", mappedName = "jdbc/__default")
    DataSource ds1;

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Boolean pass = false;
        SimpleTest[] tests = null;
        StringBuffer buf = null;
        Boolean  notestcase = false;
        String testcase = request.getParameter("testcase");
        System.out.println("testcase="+testcase);
        if (testcase != null) {

          out.println("<html>");
          out.println("<head>");
          out.println("<title>Servlet MyServlet</title>");
          out.println("</head>");
          out.println("<body>");
          out.println("<h1>Servlet MyServlet at " + request.getContextPath() + "</h1>");

          buf = new StringBuffer();

          try {
	    if ("usertx".equals(testcase)) {
                 tests = initializeUserTxTest();
	    } else if ("noleak".equals(testcase)) {
                 tests = initializeLeakTest();
	    }
          } catch (Exception e) {
               HtmlUtil.printException(e, out);
          }

          try {
               buf.append("Test Name:Pass<br>");
               for (SimpleTest test : tests) {
                  Map<String, Boolean> map = test.runTest(ds1, out);
                   for (Map.Entry entry : map.entrySet()) {
                      buf.append(entry.getKey());
                      buf.append(":");
                      buf.append(entry.getValue());
                      buf.append("<br>");
                  }
               }
               out.println(buf.toString());
            } catch (Throwable e) {
               out.println("got outer excpetion");
               out.println(e);
               e.printStackTrace();
            } finally {
               out.println("</body>");
               out.println("</html>");
               out.close();
               out.flush();
           }
	}
    }

    private SimpleTest[] initializeUserTxTest() throws Exception {
        String[] tests = {
            "myapp.test.UserTxTest"
        };

        SimpleTest[] testInstances = new SimpleTest[tests.length];
        for (int i = 0; i < tests.length; i++) {
            Class testClass = Class.forName(tests[i]);
            Constructor c = testClass.getConstructor();
            testInstances[i] = (SimpleTest) c.newInstance();
        }
        return testInstances;
    }

    private SimpleTest[] initializeLeakTest() throws Exception {
        String[] tests = {
            "myapp.test.LeakTest"
        };

        SimpleTest[] testInstances = new SimpleTest[tests.length];
        for (int i = 0; i < tests.length; i++) {
            Class testClass = Class.forName(tests[i]);
            Constructor c = testClass.getConstructor();
            testInstances[i] = (SimpleTest) c.newInstance();
        }
        return testInstances;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
