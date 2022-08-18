package fish.payara.tst;

import fish.payara.micro.*;
import org.jboss.shrinkwrap.api.*;
import org.jboss.shrinkwrap.api.spec.*;
import org.jboss.shrinkwrap.resolver.api.maven.*;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.*;

public class StartPayaraMicroWithNoClusterTest
{
  private PayaraMicroServer server;
  private static final WebArchive WAR_FILE = createWar();

  @Before
  public void before() throws Exception {
    server = PayaraMicroServer.newInstance();
  }

  @After
  public void after() throws BootstrapException
  {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  @Test
  public void serverStartWithNoclusterFalseShouldSucceed() {
    assertNotNull(server);
    server.start("--autobindhttp", "--deploy", "test.war");
  }

  @Test
  public void serverStartWithNoClusterTrueShouldFail() {
    assertNotNull(server);
    server.start("--autobindhttp", "--nocluster", "--deploy", "test.war");
  }

  private static WebArchive createWar() {
    return ShrinkWrap.create(WebArchive.class, "test.war")
      .addClass(DataGridTest.class)
      .addAsLibraries(
        Maven.resolver().resolve("org.apache.commons:commons-lang3:3.12.0")
          .withTransitivity().asFile());
  }
}
