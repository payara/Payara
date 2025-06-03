package fish.payara.samples.programatic;

import fish.payara.micro.BootstrapException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class StartPayaraMicroWithNoClusterTest
{
  private PayaraMicroServer server;
  private static final File WAR_FILE = createWar();

  @Before
  public void before() throws Exception {
    server = PayaraMicroServer.newInstance();
  }

  @After
  public void after() throws BootstrapException {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  @Test
  public void serverStartWithNoclusterFalseShouldSucceed() {
    assertNotNull(server);
    server.start("--autobindhttp", "--deploy", WAR_FILE.getAbsolutePath());
  }

  @Test
  public void serverStartWithNoClusterTrueShouldFail() {
    assertNotNull(server);
    server.start("--autobindhttp", "--nocluster", "--deploy", WAR_FILE.getAbsolutePath());
  }

  private static File createWar() {
    WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
            .addClass(DataGridTest.class)
            .addAsLibraries(
                    Maven.resolver().resolve("org.apache.commons:commons-lang3:3.12.0")
                            .withTransitivity().asFile()
            );
      File warFile = null;
      try {
          warFile = File.createTempFile(DataGridTest.class.getSimpleName(), "test.war");
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      warFile.deleteOnExit();
    war.as(ZipExporter.class).exportTo(warFile, true);
    return warFile;
  }
}
