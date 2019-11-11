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

import fish.payara.test.containers.tools.container.MySQLContainerConfiguration;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.junit.DockerITest;
import fish.payara.test.containers.tst.jdbc.war.DataSourceDefinitionBean;
import fish.payara.test.containers.tst.jdbc.war.JdbcDsName;

import io.github.zforgo.arquillian.junit5.ArquillianExtension;

import java.io.File;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.persistence.CleanupUsingScript;
import org.jboss.arquillian.persistence.TestExecutionPhase;
import org.jboss.arquillian.persistence.UsingDataSet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
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
@ExtendWith(ArquillianExtension.class)
@CleanupUsingScript(phase = TestExecutionPhase.BEFORE, value = {"dbCleanup.sql"})
@UsingDataSet(value = "basicDbStatus.json")
public class JdbcPoolITest extends DockerITest {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcPoolITest.class);

//    @ArquillianResource(MysqlDataSource.class)
//    private DataSource ds;
//
//    @ArquillianResource
//    private DataSourceDefinitionBean bean;
//
//    @ArquillianResource
//    private ProtocolMetaData metadata;
//
//    @ArquillianResource
//    private Deployer deployer;
//
//    @ArquillianResource
//    private InitialContext context;

    @Deployment(testable = false)
    public static WebArchive getArchiveToDeploy() throws Exception {
        LOG.info("getArchiveToDeploy()");

        initEnvironment();

        final WebArchive war = ShrinkWrap.create(WebArchive.class) //
            .addPackage(DataSourceDefinitionBean.class.getPackage()) //
//            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
// TODO: otestovat jako priklad s @Datasourcedefinition anotaci v jinem testu
//            .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml") //
//                .resolve("mysql:mysql-connector-java").withoutTransitivity().asFile())
//            .addAsWebInfResource(new File(webInfDir, "web.xml")) //
//            .addAsWebInfResource(new File(webInfDir, "glassfish-web.xml")) //
//            .addAsWebInfResource(new File(webInfDir, "payara-web.xml")) //
        ;

        LOG.info(war.toString(true));
        return war;
    }

//    @BeforeAll
    public static void initEnvironment() throws Exception {
        LOG.info("initEnvironment()");
        final DockerEnvironment environment = DockerEnvironment.getInstance();
        final PayaraServerContainer payara = environment.getPayaraContainer();
        final MySQLContainer<?> mysql = environment.getMySqlcontainer();
        final MySQLContainerConfiguration mysqlCfg = environment.getConfiguration().getMySQLServerConfiguration();
        PayaraServerContainerConfiguration payaraConfiguration = environment.getConfiguration().getPayaraServerConfiguration();

        final File driverFile = Maven.resolver().loadPomFromFile("pom.xml").resolve("mysql:mysql-connector-java")
            .withoutTransitivity().asSingleFile();
        final File targetFile = new File(payaraConfiguration.getPayaraDomainLibDirectory(), driverFile.getName());
        FileUtils.copyFile(driverFile, targetFile);
        // file is ignored by classloaders until server restart
        payara.asAdmin("restart-domain");

        final String poolNameBase = "domain-pool-";
        int counter = 1;
        for (String jndiName : new String[] {JdbcDsName.JDBC_DS_3, JdbcDsName.JDBC_DS_4}) {
            final String poolName = poolNameBase + counter++;
            payara.asAdmin("create-jdbc-connection-pool", "--ping", "--restype", "javax.sql.DataSource", //
                "--datasourceclassname", "com.mysql.cj.jdbc.MysqlDataSource", //
                "--steadypoolsize", "5", "--maxpoolsize", "20", //
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

        payara.asAdmin("list-jdbc-connection-pools");

//    runSqlCommand("ALTER DATABASE " + DB_NAME + " CHARACTER SET utf8;");
//    runSqlScript("dbSchema.sql");
    }


    @Test
    public void testSomething() throws Exception {
        WebTarget target = getAnonymousBasicWebTarget();
        // FIXME: must use proper context
        Builder builder = target.path("ds").path("versions").request();
        try (Response response = builder.get()) {
            assertEquals(Status.OK, response.getStatusInfo().toEnum(), "response.status");
            assertTrue(response.hasEntity(), "response.hasEntity");

            final String stringEntity = response.readEntity(String.class);
            LOG.info("stringEntity: \n{}", stringEntity);
            assertEquals("Table 'testdb.JEEVersion' doesn't exist", stringEntity, "response.text");
        }

        // FIXME: in-container test
//        assertAll( //
//            () -> assertNotNull(ds, "ds"), //
//            () -> assertNotNull(bean, "bean"), //
//            () -> assertNotNull(metadata, "metadata"), //
//            () -> assertNotNull(deployer, "deployer"), //
//            () -> assertNotNull(context, "context") //
//        );
    }
//
//
//    private static int getJdbcPort(final MySQLContainer<?> mysql) {
//        final String jdbcUrl = mysql.getJdbcUrl();
//        final URI url = URI.create(jdbcUrl.substring(5));
//        return url.getPort();
//    }
}
