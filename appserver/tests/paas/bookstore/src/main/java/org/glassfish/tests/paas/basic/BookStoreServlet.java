/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.paas.basic;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.String;
import java.util.Enumeration;


public final class BookStoreServlet extends HttpServlet {

    @Resource(mappedName = "jdbc/__bookstore")
    private DataSource ds = null;
    private boolean createdTables = false;

    /**
     * Respond to a GET request for the content produced by
     * this servlet.
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are producing
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void service(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        System.out.println("Servlet processing do get..");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();

        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Simple PaaS Enabled BookStore Application</title>");
        writeCSS(writer);
        writer.println("</head>");
        writer.println("<body bgcolor=white>");

        writer.println("<table border=\"0\">");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println("<img height=\"200\" width=\"200\" src=\"images/bookstore.gif\">");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<h1>Simple PaaS Enabled BookStore Application</h1>");
        writer.println("</td>");
        writer.println("</tr>");
        writer.println("</table>");

        writer.println("<table border=\"0\" width=\"100%\">");
        writer.println("<p>This application is served by <b>" +
                getServletContext().getServerInfo() + "</b> [" +
                System.getProperty("com.sun.aas.instanceName") + "]</p>");
        writer.println("Please wait while accessing the bookstore database.....");
        writer.println("</table>");
        if (ds != null) {
            DatabaseOperations operations = new DatabaseOperations();
            String userName = "World";//System.getenv("USER");

            operations.createAccessInfoTable(ds, writer);
            operations.createBookStoreTable(ds, writer);
            operations.updateAccessInfo(ds, userName, writer);

            operations.addBookToTable(ds, request.getParameter("title"),
                    request.getParameter("authors"), request.getParameter("price"));
            operations.printBooksTable(ds, writer);
            generateNewBookForm(writer);
            
        }

        writer.println("<p/><a href=\'BookStoreServlet\'>My Home</a>");
        writer.println("<p><font color=red>Thanks for using Oracle PaaS Solutions</font></p>");
        writer.println("</body>");
        writer.println("</html>");

    }

    private void writeCSS(PrintWriter out) {
        out.println("<style type=\"text/css\">"
                + "table {"
                + "width:90%;"
                + "border-top:1px solid #e5eff8;"
                + "border-right:1px solid #e5eff8;"
                + "margin:1em auto;"
                + "border-collapse:collapse;"
                + "}"
                + "td {"
                + "color:#678197;"
                + "border-bottom:1px solid #e5eff8;"
                + "border-left:1px solid #e5eff8;"
                + "padding:.3em 1em;"
                + "text-align:center;"
                + "}"
                + "</style>");
    }


    private void generateNewBookForm(PrintWriter out) {
        out.println("<form name=\'add_new_book\' method=\'GET\' action=\'BookStoreServlet\'>");
        out.println("<p/><b>Add a new book to the store:</b>");
        out.println("<table>");
        out.println("<tr><td>Title: </td><td><input type=text name=\'title\' size=30 " +
                "value=\'Developing PaaS Components\'></td></tr>");
        out.println("<tr><td>Author(s): </td><td><input type=text name=\'authors\' size=30 " +
                "value=\'Shalini M\'></td></tr>");
        out.println("<tr><td>Price:</td><td><input type=text name=\'price\' size=30 value=\'100$\'></td></tr>");
        out.println("<tr><td></td><td><input type=submit value=\'Add This Book\'></td></tr>");
        out.println("</table>");
        out.println("</form>");
    }
}

