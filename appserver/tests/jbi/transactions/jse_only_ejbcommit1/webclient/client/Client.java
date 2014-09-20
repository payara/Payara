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

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.sql.DataSource;

import javax.xml.ws.*;
import endpoint.ejb.*;

public class Client extends HttpServlet {

       //@WebServiceRef(name="sun-web.serviceref/HelloEJBService")
       HelloEJBService service = new HelloEJBService();

       @Resource(mappedName="jdbc/__default") private DataSource ds;

       public void doGet(HttpServletRequest req, HttpServletResponse resp) 
		throws javax.servlet.ServletException {
           doPost(req, resp);
       }

       public void doPost(HttpServletRequest req, HttpServletResponse resp)
              throws javax.servlet.ServletException {
            UserTransaction ut = null;
            // Create Table with name CUSTOMER_cm1. This name will be used in the EJB
	    String tableName = "CUSTOMER_cm";
	    String[] names = {"Vikas", "VikasAwasthi"};
	    String[] emails= {"vikas@sun.com", "VikasA@sun.com"};
            try {
	        Connection con = ds.getConnection();
	        createTable(con, tableName);
                ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                ut.begin();

                System.out.println(" Service is :" + service);
		Hello port = service.getHelloEJBPort();
                
		String ret = port.sayHello("Appserver Tester !");
                System.out.println("Return value from webservice:"+ret);

		updateTable(con, tableName, names[1], emails[1]);
		if(ut.getStatus() != Status.STATUS_ACTIVE) {
                    ret += "FAILED";
		} else {
		    System.out.println("**** committing transaction");
		    ut.commit();
		    if(!isDataUpdated(con, tableName, names, emails)) {
                	ret += "FAILED";
                    }
		}
                PrintWriter out = resp.getWriter();
                resp.setContentType("text/html");
                out.println("<html>");
                out.println("<head>");
                out.println("<title>TestServlet</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<p>");
                out.println("So the RESULT OF EJB webservice IS :");
                out.println("</p>");
                out.println("[" + ret + "]");
                out.println("</body>");
                out.println("</html>");
		dropTable(con, tableName);
            } catch(Exception e) {
                e.printStackTrace();
	    }
       }

       // use this table in the EJB webservice
       private void createTable(Connection con, String tableName) throws Exception {
	    // autocommit is made true so that the table is created immediately
	    boolean autoCommit = con.getAutoCommit();
	    con.setAutoCommit(true);
	    System.out.println("**** auto commit = " + con.getAutoCommit());
            PreparedStatement pStmt =
            con.prepareStatement("CREATE TABLE "+tableName+" (NAME VARCHAR(30) NOT NULL PRIMARY KEY, EMAIL VARCHAR(30))");
            pStmt.executeUpdate();
	    con.setAutoCommit(autoCommit);
       }

       private void dropTable(Connection con, String tableName) throws Exception {
	    boolean autoCommit = con.getAutoCommit();
	    con.setAutoCommit(true);
            PreparedStatement pStmt = con.prepareStatement("DROP TABLE "+tableName);
            pStmt.executeUpdate();
	    con.setAutoCommit(autoCommit);
       }

       // Check whether the EJB webservice has updated the data in the table.
       private boolean isDataUpdated(Connection con, String tableName, String[] names, String[] emails) throws Exception {
	    PreparedStatement pStmt = con.prepareStatement("SELECT NAME, EMAIL FROM "+tableName);
	    ResultSet rs = pStmt.executeQuery();
	    int allDataCount = 0;
	    while(rs.next()) {
		String db_Name  = rs.getString(1);
		String db_Email = rs.getString(2);
		System.out.println("NAME="+db_Name+", EMAIL="+db_Email);
	      	for (int i=0; i < names.length; i++) 
		    if(db_Name.equals(names[i]) && db_Email.equals(emails[i]))
			allDataCount++;
	    }
	    rs.close();
	    return (allDataCount == names.length);
       }

       private void updateTable(Connection con, String tableName, String name, String email) 
		throws Exception {
	   PreparedStatement pStmt = 
		con.prepareStatement("INSERT INTO "+ tableName +" (NAME, EMAIL) VALUES(?,?)");
	   pStmt.setString(1, name);
	   pStmt.setString(2, email);
	   pStmt.executeUpdate();
       }
}
