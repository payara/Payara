/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package org.glassfish.tests.embedded.web;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

/**
 * @author bhavanishankar@java.net
 */
@DataSourceDefinitions({
@DataSourceDefinition(name = "java:app/mysql/MySQLDataSource",
        className = "com.mysql.cj.jdbc.MysqlDataSource",
        portNumber = 3306,
        serverName = "localhost",
        databaseName = "testDB",
        user = "root",
        password = "abc123",
        properties = {"createDatabaseIfNotExist=true"}),
@DataSourceDefinition(name = "java:app/mysql/MySQLEmbeddedDataSource",
        className = "com.mysql.jdbc.Driver",
        url="jdbc:mysql:mxj://localhost:3336/testDB",
        user = "root",
        password = "abc123",
        properties = {"createDatabaseIfNotExist=true",
                "server.basedir=/tmp/testDB", "server.initialize-user=true"})
})
@WebServlet(name = "mySqlTestServlet", urlPatterns = "/mysqlTestServlet")
public class MySqlTestServlet extends HttpServlet {

    @Resource(mappedName = "java:app/mysql/MySQLDataSource")
    DataSource myDB;

    @Resource(mappedName = "java:app/mysql/MySQLEmbeddedDataSource")
    DataSource myEmbeddedDB;

    @PersistenceContext
    private EntityManager em;

    @Resource
    private UserTransaction utx;


    @Override
    protected void doGet(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse) throws ServletException, IOException {
        PrintWriter writer = httpServletResponse.getWriter();
        try {
            writer.println("DS = " + myDB);
            writer.println("EM = " + em);
            Connection connection = myEmbeddedDB.getConnection();
            writer.println("connection = " + connection);
            connection.close();

            if (!entryExists("BHAVANI-13-02")) {
                Person person = new Person("BHAVANI-13-02", "Bhavanishankar", "Engineer");
                utx.begin();
                em.persist(person);
                utx.commit();
                System.out.println("Persisted " + person);
            }
        } catch (Exception ex) {
            ex.printStackTrace(writer);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private boolean entryExists(String uuid) {
        return em.find(Person.class, uuid) != null;
    }

}
