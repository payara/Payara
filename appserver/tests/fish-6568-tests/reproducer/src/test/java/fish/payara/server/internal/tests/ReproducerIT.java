package fish.payara.server.internal.tests;

import fish.payara.server.internal.tests.model.*;
import io.restassured.http.*;
import jakarta.ws.rs.core.*;
import lombok.extern.slf4j.*;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.*;
import org.testcontainers.containers.wait.strategy.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.*;

import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.*;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class ReproducerIT
{
  private static URI uri;

  @Container
  private static final GenericContainer<?> oracle =
    new GenericContainer<>("oracleinanutshell/oracle-xe-11g:latest")
      .withExposedPorts(1521, 5500)
      .withEnv("ORACLE_ALLOW_REMOTE", "true")
      .withEnv("ORACLE_DISABLE_ASYNCH_IO", "true")
      .withClasspathResourceMapping("scripts/oracle",
        "/docker-entrypoint-initdb.d", BindMode.READ_WRITE)
      .withLogConsumer(new Slf4jLogConsumer(log))
      .waitingFor(Wait.forLogMessage(".*SQL>.*", 1))
      .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS));

  @Container
  private static final GenericContainer<?> payara =
    new GenericContainer<>("payara/micro:5.2022.2-jdk11")
      .dependsOn(oracle)
      .withExposedPorts(8080)
      .withCopyFileToContainer(MountableFile.forHostPath(
        Paths.get("target/reproducer.war")
          .toAbsolutePath(), 511), "/opt/payara/deployments/test.war")
      .waitingFor(Wait.forLogMessage(".* Payara Micro .* ready in .*\\s", 1))
      .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS))
      .withCommand(
        "--noCluster --deploy /opt/payara/deployments/test.war --contextRoot /test");

  @BeforeAll
  public static void beforeAll()
  {
    uri = UriBuilder.fromUri("http://" + payara.getHost())
      .port(payara.getMappedPort(8080))
      .path("test").path("fish-6568").path("person")
      .build();
  }

  @AfterAll
  public static void afterAll()
  {
    uri = null;
  }

  @Test
  @Order(10)
  public void testOracleDatabaseIsRunning()
  {
    assertThat(oracle).isNotNull();
    assertThat(oracle.isRunning()).isTrue();
  }

  @Test
  @Order(12)
  public void testPayaraMicroIsRunning()
  {
    assertThat(payara).isNotNull();
    assertThat(payara.isRunning()).isTrue();
  }

  @Test
  @Order(15)
  public void testPayaraLogFileDoesNotContainSaxParserException()
  {
    assertThat(payara.getLogs()).doesNotContain(
      "org.xml.sax.SAXParseException");
  }

  @Test
  @Order(20)
  public void testCreatePersonShouldSucceed()
  {
    assertThat(given()
      .contentType(ContentType.JSON)
      .body(new Person("John", "Doe"))
      .when()
      .post(uri)
      .then()
      .assertThat().statusCode(202)
      .and()
      .extract().body().as(Person.class).getFirstName()).isEqualTo("John");
  }

  @Test
  @Order(30)
  public void testFindAllShouldSucceed()
  {
    assertThat(given()
      .contentType(ContentType.JSON)
      .when()
      .get(uri)
      .then()
      .assertThat().statusCode(200)
      .and()
      .extract().body().jsonPath().getList(".", Person.class).size()).isEqualTo(
      1);
  }
}
