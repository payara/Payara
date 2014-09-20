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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myapp.test;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import myapp.util.HtmlUtil;
import myapp.util.TablesUtil;

/**
 *
 * @author jagadish
 */
public class UserTxTest implements SimpleTest {

    Map<String, Boolean> resultsMap = new HashMap<String, Boolean>();

    public Map<String, Boolean> runTest(DataSource ds1, PrintWriter out) {
        try {
            if (testUserTxWithRollback(ds1, out)) {
                resultsMap.put("user-tx-rollback", true);
            }else{
                resultsMap.put("user-tx-rollback", false);
            }
        } catch (Exception e) {
            resultsMap.put("user-tx-rollback", false);
        }

        try {
            if (testUserTxWithCommit(ds1, out)) {
                resultsMap.put("user-tx-commit", true);
            }else{
                resultsMap.put("user-tx-commit", false);
            }
        } catch (Exception e) {
            resultsMap.put("user-tx-commit", false);
        }

        return resultsMap;

    }

    private boolean testUserTxWithRollback(DataSource ds1, PrintWriter out) throws SystemException {
        boolean result = false;
        Connection con = null;

        Statement stmt = null;
        ResultSet rs = null;

        String tableName = "user_tx_table_rollback_test";
        String content = "testUserTxWithRollback";
        String columnName = "message";
        TablesUtil.createTables(ds1, out, tableName, columnName);

        HtmlUtil.printHR(out);
        out.println("<h4> user-tx-rollback test </h4>");
        javax.transaction.UserTransaction ut = null;
        try {
            InitialContext ic = new InitialContext();
            ut = (javax.transaction.UserTransaction) ic.lookup("java:comp/UserTransaction");
            out.println("<br>Able to lookup UserTransaction");
            ut.begin();
            out.println("<br>");
            out.println("Started UserTransaction<br>");

            out.println("Trying to get connection ...<br>");

            out.println("ds value : " + ds1);
            con = ds1.getConnection();
            out.println("Got connection : " + con);
            stmt = con.createStatement();
            stmt.executeUpdate("insert into " + tableName + " values ('" + content + "')");
            out.println("<br>");

            out.println("Able to lookup datasource");
            out.println("<br>");
            out.println("Rolling back transaction<br>");
            ut.rollback();
            if (!TablesUtil.verifyTableContent(ds1, out, tableName, columnName, content)) {
                result = true;
            }

        } catch (Throwable e) {
            HtmlUtil.printException(e, out);
            result = false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }

            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }

            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }

            TablesUtil.deleteTables(ds1, out, tableName);
            HtmlUtil.printHR(out);
            return result;
        }
    }

    private boolean testUserTxWithCommit(DataSource ds1, PrintWriter out) throws SystemException,
            IllegalStateException, SecurityException {
        boolean result = false;
        Connection con = null;

        Statement stmt = null;
        ResultSet rs = null;

        String tableName = "user_tx_table_commit_test";
        String content = "testUserTxWithCommit";
        String columnName = "message";
        TablesUtil.createTables(ds1, out, tableName, columnName);

        HtmlUtil.printHR(out);
        out.println("<h4> user-tx-commit test </h4>");
        javax.transaction.UserTransaction ut = null;
        try {
            InitialContext ic = new InitialContext();
            ut = (javax.transaction.UserTransaction) ic.lookup("java:comp/UserTransaction");
            out.println("<br>Able to lookup UserTransaction");
            ut.begin();
            out.println("<br>");
            out.println("Started UserTransaction<br>");

            out.println("Trying to get connection ...<br>");

            out.println("ds value : " + ds1);
            con = ds1.getConnection();
            out.println("Got connection : " + con);
            stmt = con.createStatement();
            stmt.executeUpdate("insert into " + tableName + " values ('" + content + "')");
            out.println("<br>");

            out.println("Able to lookup datasource");
            out.println("<br>");
            ut.commit();
            result = TablesUtil.verifyTableContent(ds1, out, tableName, columnName, content);

        } catch (Throwable e) {
            HtmlUtil.printException(e, out);
            out.println("Rolling back transaction<br>");
            ut.rollback();
            result = false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }

            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }

            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }
            TablesUtil.deleteTables(ds1, out, tableName);
            HtmlUtil.printHR(out);
            return result;
        }
    }
}
