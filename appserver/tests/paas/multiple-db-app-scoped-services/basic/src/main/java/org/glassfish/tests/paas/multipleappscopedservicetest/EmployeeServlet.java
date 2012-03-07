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

package org.glassfish.tests.paas.multipleappscopedservicetest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.util.Enumeration;
import javax.annotation.Resource;


public final class EmployeeServlet extends HttpServlet {

    @Resource(mappedName = "java:app/jdbc/SalaryRes")
    private DataSource salDs = null;

    @Resource(mappedName = "java:app/jdbc/HrRes")
    private DataSource hrDs = null;

    /**
     * Respond to a GET request for the content produced by
     * this servlet.
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are producing
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        System.out.println("Servlet processing do get..");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();

        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Multiple App Scoped DB Service Test</title>");
        writer.println("</head>");
        writer.println("<body bgcolor=white>");

        writer.println("<table border=\"0\">");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println("<img height=\"200\" width=\"200\" src=\"images/numbers.jpg\">");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<h1>Multiple Application Scoped DB Service PaaS Application</h1>");
        writer.println("</td>");
        writer.println("</tr>");
        writer.println("</table>");

        writer.println("<table border=\"0\" width=\"100%\">");
        Enumeration names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            writer.println("<tr>");
            writer.println("  <th align=\"right\">" + name + ":</th>");
            writer.println("  <td>" + request.getHeader(name) + "</td>");
            writer.println("</tr>");
        }
        writer.println("</table>");
        if (hrDs != null && salDs != null) {
            Statement stmt1 = null;
	    PreparedStatement stmt2 = null;
            try {
                stmt1 = hrDs.getConnection().createStatement();

                DatabaseMetaData dbMetadata1 = stmt1.getConnection().getMetaData();
                String dbUrl1 = dbMetadata1.getURL();
                writer.println("DB URL : " + dbUrl1 + "\n");
                if (dbUrl1.indexOf("hr_database") == -1) {
                    throw new Exception("Custom Database [hr_database] is not created while provisioning.");
                }


                ResultSet rs1 = stmt1.executeQuery("SELECT emp_id, emp_name from HR");
		String salQuery = "SELECT emp_sal from SALARY WHERE emp_id = ? ";
		stmt2 = salDs.getConnection().prepareStatement(salQuery);
                
		DatabaseMetaData dbMetadata2 = stmt2.getConnection().getMetaData();
                String dbUrl2 = dbMetadata2.getURL();
                writer.println("DB URL : " + dbUrl2 + "\n");
                if (dbUrl2.indexOf("salary_database") == -1) {
                    throw new Exception("Custom Database [salary_database] is not created while provisioning.");
                }
                
		writer.println("<table border=\"1\" width=\"100%\">");
                writer.println("<tr>");
                writer.println("  <th align=\"left\" colspan=\"2\">" + "Employee Information retrieved" + "</th>");
                writer.println("</tr>");
                writer.println("<tr>");
                writer.println("<td>" + "Employee ID" + "</td>");
                writer.println("<td>" + "Employee Name" + "</td>");
		writer.println("<td>" + "Employee Salary" + "</td>");
                writer.println("</tr>");
                while (rs1.next()) {
                    writer.println("<tr>");
                    writer.println("  <td>" + rs1.getObject(1) + "</td>");
                    writer.println("  <td>" + rs1.getObject(2) + "</td>");
		    stmt2.setInt(1, (Integer) rs1.getObject(1));
		    ResultSet rs2 = stmt2.executeQuery();
		    while(rs2.next()) {
			writer.println("  <td>" + rs2.getObject(1) + "</td>");
                        writer.println("</tr>");
		    }	
                }
                writer.println("</table>");
            } catch (Exception ex) {
                ex.printStackTrace(writer);
            } finally {
                if (stmt1 != null) {
                    try {
                        stmt1.getConnection().close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (stmt2 != null) {
                    try {
                        stmt2.getConnection().close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
        }
        writer.println("</body>");
        writer.println("</html>");
    }

}

