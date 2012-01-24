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

package org.glassfish.tests.paas.javaee_shared_service;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Shalini M
 */
public class DatabaseOperations {

    Map<String, Boolean> resultsMap = new HashMap<String, Boolean>();

    public void printDBDetails(DataSource ds, PrintWriter out) {
        Connection conn = null;
        Statement stmt = null;
        try {
            out.println("<br>");

            conn = ds.getConnection();
            DatabaseMetaData dmd = conn.getMetaData();
            out.println("Database : " + dmd.getDatabaseProductName() + "<br>");

        } catch (Exception e) {
            out.println("Exception : " + e.getMessage() + "<br>");
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e1) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e1) {
                }
            }
        }
    }

    public void updateAccessInfo(DataSource ds, String userName, PrintWriter out) {
        Connection conn = null;
        Statement stmt = null;

        String tableName = "DEMO_TABLE";
        try {
            out.println("<br>");
//            out.println("Trying to establish connection to bookstore database...<br>");

            if(ds != null) {
                conn = ds.getConnection();
                DatabaseMetaData dmd = conn.getMetaData();

                out.println("Successfully established connection to " + dmd.getDatabaseProductName() + " Version : " + dmd.getDatabaseProductVersion());

//                out.println("Database : " + dmd.getDatabaseProductName() + "<br>");
//                out.println("Product Version : " + dmd.getDatabaseProductVersion() + "<br>");
//                out.println("<br>");

                stmt = conn.createStatement();
                String query = "Select * from " + tableName  + " where Name = \'" + userName + "\'";
                ResultSet rs = stmt.executeQuery(query);
                boolean foundUser = false;
                while(rs.next()) {
                    String name = rs.getString("Name");
                    if(userName.equals(name.trim())) { // use trimmed name for comparison.
//                        out.println("Welcome back " + userName + "!!! You have accessed our database earlier<br>");
                        foundUser = true;
                        //row exists
                        Timestamp curDate = rs.getTimestamp("Current_Accessed");
                        Timestamp lastDate = rs.getTimestamp("Last_Accessed");
                        out.println("<br>");
//                        out.println("Retrieving your database access times <br>");
                        out.println("Hello " + userName + "!!! Your last access of this database is " +
                                (lastDate != null ? lastDate.toString() : curDate.toString()) + "<br>");
                        lastDate = curDate;
                        curDate = new Timestamp(System.currentTimeMillis());
                        out.println("<br>");
//                        out.println("Updating your database access times <br>");
                        PreparedStatement prep1 = conn.prepareStatement("update " + tableName +
                                " set Current_Accessed = ?, Last_Accessed = ?" +
                                " where Name = ?");
                        prep1.setTimestamp(1, curDate);
                        prep1.setTimestamp(2, lastDate);
                        prep1.setString(3, name); // use the name with space characters.
                        prep1.executeUpdate();
                        prep1.close();
//                        out.println("Database updated ! Thank you !<br>");
                    }
                }
                if(!foundUser) {
                    out.println("<br/>Hello " + userName + "!!! You are accessing our database for the first time <br>");
                    out.println("<br>");
                    PreparedStatement prep1 = conn.prepareStatement("INSERT INTO " + tableName + " values(?,?,?)");
                    prep1.setString(1, userName);
                    prep1.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    prep1.setTimestamp(2, null);
                    prep1.executeUpdate();
                    prep1.close();
//                    out.println("Added you to our database<br>");
                }
                rs.close();
//                out.println("<br>");
//                out.println("<h3> JDBC driver details </h3>");
//                out.println("Driver name : " + dmd.getDriverName() + "<br>");
//                out.println("Driver version : " + dmd.getDriverVersion() + "<br>");
//                out.println("Driver minor version : " + dmd.getDriverMinorVersion() + "<br>");
//                out.println("Driver major version : " + dmd.getDriverMajorVersion() + "<br>");
            }
        } catch (Exception e) {
            out.println("Exception : " + e.getMessage() + "<br>");
        } finally {
            if (stmt != null ) {
                try {stmt.close(); } catch( Exception e1) {
                }
            }
            if (conn != null ) {
                try {conn.close();} catch( Exception e1) {
                }
            }
        }
    }


    public boolean createTable(DataSource ds1, String tableName,
                               String createQuery, PrintWriter out) {
        boolean tableCreated = true;
        Connection con = null;
        Statement stmt = null;
        try {

            con = ds1.getConnection();
            stmt = con.createStatement();
            String selectQuery = "SELECT * FROM " + tableName;
            try {
                stmt.executeQuery(selectQuery);
            } catch(Exception ex) {
                //does not exist.
                tableCreated = false;
            }

            if (!tableCreated) {
                try {
                    System.out.println("Executing SQL command ["  + createQuery.toString() + "]");
                    stmt.executeUpdate(createQuery.toString());
                    tableCreated = true;
                } catch (Exception ex) {
                    ex.printStackTrace(); // print trace in case of error creating table.
                    tableCreated = false;
                }
            }
        } catch (Exception e) {
            tableCreated = false;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }

            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
        return tableCreated;
    }

    public boolean createBookStoreTable(DataSource ds, PrintWriter out) {
        String query = "create table  BOOKS_TABLE (Title char(100), " +
                "Authors char(100), Price char(16))";
        return createTable(ds, "BOOKS_TABLE", query, out);
    }

    public boolean createAccessInfoTable(DataSource ds, PrintWriter out) {
        String query = "create table DEMO_TABLE" +
                "(Name char(50) PRIMARY KEY NOT NULL, " +
                "Last_Accessed timestamp, Current_Accessed timestamp)";
        return createTable(ds, "DEMO_TABLE", query, out);
    }

    public boolean addBookToTable(DataSource ds,
                                  String title, String authors, String price) {
        if(title == null || authors == null || price == null) {
            return false;
        }
        String sql = "INSERT INTO BOOKS_TABLE values(\'" + title  + "\', \'" +
                authors + "\', \'" + price + "\')";
        return execute(ds, sql);
    }

    // format and write the entire books_table to the printwriter.
    public void printBooksTable(DataSource ds, PrintWriter out) {
        Connection con = null;
        Statement stmt = null;
        try {
            con = ds.getConnection();
            stmt = con.createStatement();
            String query = "SELECT * from BOOKS_TABLE";
            ResultSet rs = stmt.executeQuery(query);
            
            boolean printHeaders = true;

            while(rs.next()) {
                if(printHeaders) {
                    printHeaders = false;
                    out.println("<b>Here are the list of books available in our store:</b><br/>");
                    out.println("<table>");
                    out.println("<tr><td><b>Title</b></td>" +
                            "<td><b>Author(s)</b></td><td><b>Price</b></td></tr>");
                }
                out.println("<tr>");
                for (int i = 1; i <= 3; i++) {
                    out.println("<td>" + rs.getString(i) + "</td>");
                }
                out.println("</tr>");
            }
            
            if(!printHeaders) { // atleast one row was there.
                out.println("</table>");
            } else {
                out.println("<b>Currently there are no books in our store.</b>");
            }
        } catch (Exception e) {
            // ignore??
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }
    
    private boolean execute(DataSource ds1, String sql) {
        Connection con = null;
        Statement stmt = null;
        try {

            con = ds1.getConnection();
            stmt = con.createStatement();
            return stmt.execute(sql);
        } catch (Exception e) {
            return false;
            // ignore??
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
