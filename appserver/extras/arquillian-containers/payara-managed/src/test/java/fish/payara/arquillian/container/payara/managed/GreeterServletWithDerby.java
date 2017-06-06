/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.managed;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Simple servlet for testing deployment with enabled derby database.
 *
 * @author <a href="http://community.jboss.org/people/aslak">Aslak Knutsen</a>
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@WebServlet("/Greeter")
public class GreeterServletWithDerby extends HttpServlet {

    private static final String GET_LOG_ARCHIVE_MODE_QUERY =
        "VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('derby.storage.logArchiveMode')";

    private static final long serialVersionUID = 8249673615048070666L;

    private static final Logger logger = Logger.getLogger(GreeterServletWithDerby.class.getName());

    @EJB
    private Greeter greeter;

    @Resource(name = "jdbc/__default")
    private DataSource dataSource;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // test the DataSource and thus the working DB connection with an internal Derby query
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(GET_LOG_ARCHIVE_MODE_QUERY);

            rs.next();
            final PrintWriter writer = resp.getWriter();
            if (!rs.getBoolean(1)) {
                writer.append(this.greeter.greet());
            } else {
                writer.append("Something terrible happened! No greetings! derby.storage.logArchiveMode is set to TRUE");
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed closing resource!", e);
        }

        try {
            if (rs != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed closing resource!", e);
        }

        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed closing resource!", e);
        }
    }
}
