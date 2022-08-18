package fish.payara.datagrid.tests;

import org.apache.http.*;
import org.junit.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import static org.junit.Assert.*;

public class PayaraMicroWithNoClusterIT
{
  private Client client;
  private static final String URL1 = "http://localhost:9090/dg/api/produce";
  private static final String URL2 = "http://localhost:9191/dg/api/consume";
  private static final String URL3 = "http://localhost:9090/dg/api/add";
  private static final String URL4 = "http://localhost:9090/dg/api/get?key=foo";
  private static final String URL5 = "http://localhost:9292/dg/api/add";

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
  public void testWithNoclusterFalseShouldSucceed()
  {
    WebTarget webTarget = client.target(URL1);
    Response response = webTarget.request().accept(MediaType.TEXT_PLAIN).get();
    assertNotNull(response);
    assertEquals(HttpStatus.SC_OK, response.getStatus());
    assertTrue(response.readEntity(String.class).contains("distributed cache"));
  }

  @Test
  public void testWithNoclusterTrueShouldFail()
  {
    WebTarget webTarget = client.target(URL2);
    Response response = webTarget.request().accept(MediaType.TEXT_PLAIN).get();
    assertNotNull(response);
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testWithNoHazelcastFalseShouldSucceed()
  {
    WebTarget webTarget = client.target(URL3).queryParam("key", "foo").queryParam("value", "bar");
    Response response = webTarget.request().accept(MediaType.TEXT_PLAIN).put(Entity.text(""));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
    webTarget = client.target(URL4).queryParam("key", "foo");
    response = webTarget.request().accept(MediaType.TEXT_PLAIN).get();
    assertNotNull(response);
    assertEquals(HttpStatus.SC_OK, response.getStatus());
    assertTrue(response.readEntity(String.class).contains("bar"));
  }

  @Test
  public void testWithNohazelcastTrueShouldFail()
  {
    WebTarget webTarget = client.target(URL5).queryParam("key", "foo").queryParam("value", "bar");
    Response response = webTarget.request().accept(MediaType.TEXT_PLAIN).put(Entity.text(""));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
  }
}
