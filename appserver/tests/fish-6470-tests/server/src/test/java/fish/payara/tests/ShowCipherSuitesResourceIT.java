package fish.payara.tests;

import io.restassured.http.*;
import org.apache.http.*;
import org.junit.*;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;

public class ShowCipherSuitesResourceIT
{
  private static final String  URL = "http://localhost:18080/server/ss/api";

  @Test
  public void TestShowCipherSuitesResource()
  {
    String response =
      given().contentType(ContentType.TEXT).when().get(URL).then().assertThat()
        .statusCode(HttpStatus.SC_OK).extract().asString();
    assertThat(response).contains("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA");
    assertThat(response).contains("TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384");
    assertThat(response).contains("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
  }
}
