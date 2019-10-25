/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tst.jdbc.war;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;


/**
 * @author David Matejcek
 */
@DataSourceDefinition(name = JdbcDsName.JDBC_DS_1, //
    className = "com.mysql.cj.jdbc.MysqlDataSource", //
    serverName = "tc-mysql", //
    portNumber = 3306, //
    user = "mysql", //
    password = "mysqlpassword", //
    databaseName = "testdb", //
    properties = { //
        "useSSL=false", "useInformationSchema=true", "nullCatalogMeansCurrent=true", "nullNamePatternMatchesAll=false" //
    })
@DataSourceDefinition(name = JdbcDsName.JDBC_DS_2, //
    className = "com.mysql.cj.jdbc.MysqlDataSource", //
    serverName = "tc-mysql", //
    portNumber = 3306, //
    user = "mysql", //
    password = "mysqlpassword", //
    databaseName = "testdb", //
    properties = { //
        "useSSL=false", "useInformationSchema=true", "nullCatalogMeansCurrent=true", "nullNamePatternMatchesAll=false" //
    })
@Path("ds")
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DataSourceDefinitionBean {
    private static final Logger LOG = Logger.getLogger(DataSourceDefinitionBean.class.getName());

    @Resource(name = JdbcDsName.JDBC_DS_1)
    private DataSource dsa1;
    @Resource(name = JdbcDsName.JDBC_DS_2)
    private DataSource dsa2;

    @Resource(name = JdbcDsName.JDBC_DS_3)
    private DataSource dsd1;
    @Resource(name = JdbcDsName.JDBC_DS_4)
    private DataSource dsd2;

    @PostConstruct
    private void init() {
        LOG.severe("****************************************** HI!");
    }

    @GET
    @Path("/versions")
    public Response versions() throws Exception {
        LOG.severe("==================================== whoa!");
        try (Connection connection = dsa1.getConnection()) {
            LOG.severe("++++++++++++++++++++++++++++++++++++++++++++++CONNECTION DSA1: " + connection);
        }
        try (Connection connection = dsa2.getConnection()) {
            LOG.severe("++++++++++++++++++++++++++++++++++++++++++++++CONNECTION DSA2: " + connection);
        }
        try (Connection connection = dsd1.getConnection()) {
            LOG.severe("++++++++++++++++++++++++++++++++++++++++++++++CONNECTION DSD1: " + connection);
        }
        try (Connection connection = dsd2.getConnection()) {
            LOG.severe("++++++++++++++++++++++++++++++++++++++++++++++CONNECTION DSD2: " + connection);
        }
        try {
            final List<String> jeeVersions = getJeeVersions();
            return Response.ok().entity(jeeVersions).build();
        } catch (final Exception e) {
            return Response.ok().entity(e.getMessage()).build();
        }
    }


    private List<String> getJeeVersions() throws Exception {
        LOG.info("getJeeVersions()");
        try (Connection conn = dsd1.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select name from JEEVersion") //
        ) {

            final List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        }
    }
}
