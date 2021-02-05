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
package fish.payara.test.containers.tst.jdbc;

import com.github.database.rider.core.api.connection.ConnectionHolder;
import com.github.database.rider.core.api.dataset.DataSetExecutor;
import com.github.database.rider.core.configuration.DBUnitConfig;
import com.github.database.rider.core.configuration.DataSetConfig;
import com.github.database.rider.core.dataset.DataSetExecutorImpl;
import com.github.database.rider.junit5.DBUnitExtension;
import com.mysql.cj.jdbc.MysqlDataSource;

import fish.payara.test.containers.tools.container.MySQLContainerConfiguration;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.junit.DockerITestExtension;
import fish.payara.test.containers.tst.jdbc.war.DataSourceDefinitionBean;
import fish.payara.test.containers.tst.jdbc.war.JdbcDsName;

import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Matejcek
 */
@ExtendWith(DockerITestExtension.class)
@ExtendWith(DBUnitExtension.class)
public class JdbcPoolITest {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcPoolITest.class);
    private static String applicationName;
    private static PayaraServerContainer payara;
    private static ConnectionHolder connectionHolder;
    private static DataSetExecutor dsExecutor;


    @BeforeAll
    public static void initEnvironment() throws Exception {
        LOG.info("initEnvironment()");
        final DockerEnvironment environment = DockerEnvironment.getInstance();
        payara = environment.getPayaraContainer();
        final MySQLContainer<?> mysql = environment.getMySqlcontainer();
        final MySQLContainerConfiguration mysqlCfg = environment.getConfiguration().getMySQLServerConfiguration();

        final MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setDatabaseName("testdb");
        dataSource.setServerName(mysql.getContainerIpAddress());
        dataSource.setPort(mysql.getMappedPort(mysqlCfg.getPort()));
        dataSource.setUser(mysqlCfg.getDbUser());
        dataSource.setPassword(mysqlCfg.getDbPassword());
        dataSource.setServerTimezone("UTC");
        dataSource.setNullCatalogMeansCurrent(true);
        dataSource.setUseInformationSchema(true);
        dataSource.setZeroDateTimeBehavior("CONVERT_TO_NULL");
        dataSource.setCharacterEncoding(StandardCharsets.UTF_8.name());

        connectionHolder = dataSource::getConnection;

        final DBUnitConfig dbUnitConfig = DBUnitConfig.fromGlobalConfig();
        dbUnitConfig.executorId("executor");
        dbUnitConfig.addDBUnitProperty("caseSensitiveTableNames", "true");
        dbUnitConfig.addDBUnitProperty("batchedStatements", "true");
        dbUnitConfig.addDBUnitProperty("qualifiedTableNames", "false");
        dbUnitConfig.addDBUnitProperty("schema", "testdb");
        dsExecutor = DataSetExecutorImpl.instance("executor", connectionHolder);
        dsExecutor.setDBUnitConfig(dbUnitConfig);

        final PayaraServerContainerConfiguration payaraConfiguration = environment.getConfiguration()
            .getPayaraServerConfiguration();
        final File driverFile = Maven.resolver().loadPomFromFile("pom.xml").resolve("mysql:mysql-connector-java")
            .withoutTransitivity().asSingleFile();
        final File targetFile = new File(payaraConfiguration.getPayaraDomainLibDirectory(), driverFile.getName());
        FileUtils.copyFile(driverFile, targetFile);
        // file is ignored by classloaders until server restart
        payara.asAdmin("restart-domain", payaraConfiguration.getPayaraDomainName());

        final String poolNameBase = "domain-pool-";
        int counter = 1;
        for (String jndiName : new String[] {JdbcDsName.JDBC_DS_3, JdbcDsName.JDBC_DS_4}) {
            final String poolName = poolNameBase + counter++;
            payara.asAdmin("create-jdbc-connection-pool", "--ping", "--restype", "javax.sql.DataSource", //
                "--datasourceclassname", MysqlDataSource.class.getName(), //
                "--steadypoolsize", "0", "--maxpoolsize", "20", //
                "--validationmethod", "auto-commit", //
                "--isconnectvalidatereq", "true", "--failconnection", "true", //
                "--property", "user=" + mysqlCfg.getDbUser() + ":password=" + mysqlCfg.getDbPassword() //
                    + ":DatabaseName=" + mysql.getDatabaseName() //
                    + ":ServerName=" + mysqlCfg.getHostName() + ":port=" + mysqlCfg.getPort() + ":useSSL=false" //
                    + ":zeroDateTimeBehavior=CONVERT_TO_NULL:useUnicode=true" //
                    + ":serverTimezone=UTC:characterEncoding=UTF-8" //
                    + ":useInformationSchema=true:nullCatalogMeansCurrent=true:nullNamePatternMatchesAll=false"
                , //
                poolName);
            payara.asAdmin("create-jdbc-resource", "--connectionpoolid", poolName, jndiName);
        }

        applicationName = payara.deploy("/", getArchiveToDeploy());
        payara.asAdmin("list-jdbc-connection-pools");
    }


    @AfterAll
    public static void cleanup() {
        payara.undeploy(applicationName);
    }


    @BeforeEach
    public void resetTables() {
        dsExecutor.executeScript("dbCleanup.sql");
        final DataSetConfig dataSet = new DataSetConfig("basicDbStatus.json");
        dsExecutor.createDataSet(dataSet);
    }


    private static WebArchive getArchiveToDeploy() throws Exception {
        LOG.info("getArchiveToDeploy()");
        final WebArchive war = ShrinkWrap.create(WebArchive.class) //
            .addPackage(DataSourceDefinitionBean.class.getPackage()) //
        ;
        LOG.info(war.toString(true));
        return war;
    }


    @Test
    public void testSomething() throws Exception {
        final WebTarget target = payara.getAnonymousBasicWebTarget();
        final Builder builder = target.path("ds").path("versions").request();
        try (Response response = builder.get()) {
            assertEquals(Status.OK, response.getStatusInfo().toEnum(), "response.status");
            assertTrue(response.hasEntity(), "response.hasEntity");

            final String stringEntity = response.readEntity(String.class);
            LOG.info("stringEntity: \n{}", stringEntity);
            assertEquals("[J2EE 1.2, J2EE 1.3, J2EE 1.4, Java EE 6, Java EE 7, Java EE 8, Jakarta EE 8, Jakarta EE 9]",
                stringEntity, "response.text");
        }
    }
}
