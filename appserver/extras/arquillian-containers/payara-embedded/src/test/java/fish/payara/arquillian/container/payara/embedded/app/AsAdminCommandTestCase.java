// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.embedded.app;

import static org.glassfish.embeddable.CommandResult.ExitStatus.SUCCESS;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.naming.InitialContext;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that Payara 'asadmin' commands can be executed against an Embedded Payara instance using
 * the CommandRunner resource. The CommandRunner is exercised by performing commands to create a
 * Payara JDBC connection pool and a JDBC resource. The JDBC connection pool is tested by using the
 * CommandRunner to perform a ping. The JDBC resource is verified by performing a JNDI lookup.
 *
 * @author magnus.smith
 */
@RunWith(Arquillian.class)
public class AsAdminCommandTestCase {

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        return create(WebArchive.class)
                   .addClasses(
                       NoInterfaceEJB.class, 
                       NameProvider.class)
                   .addAsWebInfResource(INSTANCE, "beans.xml");
    }

    @Resource(mappedName = "org.glassfish.embeddable.CommandRunner")
    private CommandRunner commandRunner;

    @Test
    public void shouldBeAbleToIssueAsAdminCommand() throws Exception {
        assertNotNull("Verify that the asadmin CommandRunner resource is available", commandRunner);

        CommandResult result = commandRunner.run("create-jdbc-connection-pool", "--datasourceclassname=org.apache.derby.jdbc.EmbeddedXADataSource",
                "--restype=javax.sql.XADataSource",
                "--property=portNumber=1527:password=APP:user=APP" + ":serverName=localhost:databaseName=my_database" + ":connectionAttributes=create\\=true",
                "my_derby_pool");

        assertEquals("Verify 'create-jdbc-connection-pool' asadmin command", SUCCESS, result.getExitStatus());

        result = commandRunner.run("create-jdbc-resource", "--connectionpoolid", "my_derby_pool", "jdbc/my_database");

        assertEquals("Verify 'create-jdbc-resource' asadmin command", SUCCESS, result.getExitStatus());

        result = commandRunner.run("ping-connection-pool", "my_derby_pool");

        assertEquals("Verify asadmin command 'ping-connection-pool'", SUCCESS, result.getExitStatus());

        assertNotNull(new InitialContext().lookup("jdbc/my_database"));
    }
}
