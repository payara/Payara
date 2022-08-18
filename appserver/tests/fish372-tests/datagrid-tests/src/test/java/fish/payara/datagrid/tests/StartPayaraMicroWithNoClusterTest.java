package fish.payara.datagrid.tests;

import fish.payara.micro.*;
import org.junit.*;

import javax.ws.rs.client.*;

public class StartPayaraMicroWithNoClusterTest
{
  private Client client;
   private static final String URL = "http://localhost:%d/test/dg/api";

  @Before
  public void before()
  {
    client = ClientBuilder.newClient();
  }

  @After
  public void after()
  {
    if (client != null)
    {
      client.close();
      client = null;
    }
  }

  @Test
  public void a1() throws BootstrapException
  {
    /*WebTarget webTarget = client.target(String.format(URL, payaraMicroRuntime.getLocalDescriptor().getAdminPort()));
    Response response = webTarget.request().accept(MediaType.TEXT_PLAIN).get();
    assertNotNull(response);
    assertEquals(HttpStatus.SC_OK, response.getStatus());
    assertTrue(response.readEntity(String.class).contains("distributed cache"));*/
  }

  /*@Test
  public void serverStartWithNoclusterFalseShouldSucceed()
  {
    System.out.println ("##### StartPayaraMicroWithNoClusterTest.serverStartWithNoclusterFalseShouldSucceed(): starting");
    assertNotNull(server);
    System.out.println ("##### StartPayaraMicroWithNoClusterTest.serverStartWithNoclusterFalseShouldSucceed(): server okay");
    server.start("--autobindhttp", "--deploy", "/home/nicolas/Payara/appserver/tests/fish372-tests/datagrid-tests/target/datagrid-tests.war");
    System.out.println ("##### StartPayaraMicroWithNoClusterTest.serverStartWithNoclusterFalseShouldSucceed(): server started");
    WebTarget webTarget = client.target(String.format(URL, server.getHttpPort()));
    Response response = webTarget.request().accept(MediaType.TEXT_PLAIN).get();
    assertNotNull(response);
    assertEquals(HttpStatus.SC_OK, response.getStatus());
    assertTrue(response.readEntity(String.class).contains("distributed cache"));
  }

  @Test
  public void serverStartWithNoClusterTrueShouldFail()
  {
    assertNotNull(server);
    server.start("--autobindhttp", "--nocluster", "--deploy", "target/test.war");
  }*/
}
