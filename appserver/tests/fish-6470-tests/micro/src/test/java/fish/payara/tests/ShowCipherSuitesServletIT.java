package fish.payara.tests;

import com.gargoylesoftware.htmlunit.*;
import org.junit.*;

import java.io.*;

import static org.assertj.core.api.Assertions.*;

public class ShowCipherSuitesServletIT
{
  private static final String URL = "http://localhost:28080/micro/ss";
  private WebClient webClient = new WebClient();

  @Test
  public void testShowCipherSuiteServlet() throws IOException  {
    String text = ((TextPage) webClient.getPage(URL)).getContent();
    assertThat(text).contains("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA");
    assertThat(text).contains("TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384");
    assertThat(text).contains("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
  }
}
