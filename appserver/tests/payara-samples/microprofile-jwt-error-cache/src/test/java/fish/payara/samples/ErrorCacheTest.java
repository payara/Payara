/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.samples;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith(Arquillian.class)
public class ErrorCacheTest {

    @ArquillianResource
    private URL uri;

    private WireMockServer wireMockServer;
    private static final String BEARER_TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJPa3h5TXo3RWJVdk81QjNBTjVBVmdvWDdfRXd5ZFVRODFwaG1GckVWelBnIn0.eyJleHAiOjMzNDA2OTY2NzgsImlhdCI6MTc2Mjg1OTg3OCwianRpIjoib25ydHJvOjAzYmM4ZGQ4LTc1MzQtYjc1My1hYjY4LTVjZmM3OTA5OWMzZSIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6OTAyMS9yZWFsbXMvdGVzdCIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIyN2I5MWEwYy01Y2Q3LTRlMjYtOGE4Mi1lYTgwNjQ1YTlkY2MiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJ0ZXN0Iiwic2lkIjoiZTJhOWRlNjYtN2VlOC1lNDIxLTBiZTEtZWYzYTQxNzE1OTI2IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy10ZXN0IiwiYXJjaGl0ZWN0Iiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiZ3JvdXBzIjpbImRlZmF1bHQtcm9sZXMtdGVzdCIsImFyY2hpdGVjdCIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXSwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdCJ9.N6UWoAxuLAgITaw6Vj81aPR5otGPJscQSLsCohiRvFBJPnRegZJTHEIbhM90jdWJUuL1mmewERYKegsvBaazhPI6U5SW_gw4PF2O1zqLrIMmx7NRhkKsIDscefGkxJ_I81IDldxHlNDidOopLc4kk_rYsEvreAJ6Ssr0xEwU__ZYxYrOaSbgdPIqUmbxA0owUmdhotvA00D99cC-mblruf2HT-ZNA-j03nqYJM1rTHLUPbhyz5P8aXF_-3VJ7j6ghbW3RkwWhhklSsEYVaWpYtK9X-_uNKCfZ0rWKozTwR7xdJZR4ntv-XFGmNVtw0-X2nRwmHroAd6ab5X8FXq6wA";


    @Before
    public void setup() {
        wireMockServer = new WireMockServer(9021);
        WireMock.configureFor("localhost", 9021);
        wireMockServer.start();
        WireMock.stubFor(WireMock.get("/realms/test/protocol/openid-connect/certs").willReturn(WireMock.aResponse().withBody("{\"keys\":[{\"kid\":\"OkxyMz7EbUvO5B3AN5AVgoX7_EwydUQ81phmFrEVzPg\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"x5c\":[\"MIIClzCCAX8CBgGacpnatzANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTI1MTExMTExMDYxNloXDTM1MTExMTExMDc1NlowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM0TlZq2OhpBBduKLse7e4BgfFwFufrazj7MJ915KF9le6tMNkVIWBUjRyeF8HAkFyhq7iG5RTeY+tAAq17CgCCcmsKHk27+IZlBg4DmkJuH0Ewf0mSHNsfslB+6q/FALxQU4jdHorodQdUENsRP3qj7kITWD/z9u2Zdpf/4DLg+JDI18aFivaSiE4Yya4Wj5OlaZPUZzsbNMVT0DCkL/DgaWehpamdhXZckPc1jxmqKZUCksaKcxlo5DtT3t+X+caanOZi/xN4FNi7KPOPYeM+wc2QSB1GIiQ5oF4jf6ja51haVZzSUM4AVPYAXKYIK17j40Y964lLCQaM2FD8V9VcCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAYgckC4iwMDsxoNqv3yuqjoDO76U81bN2PBWJTBtZ6w7eVCTIazlSQmCTA0jjPc+jXs84nT71CSdV0/6CqoJe/OYIHlvdUC+7OIb/spagO88On4MiOXuV3Seowwve14i3Fzco9eqmwdQSwMmPXVSyiCEzX3w/EQAR6gjp6iaxXkErM8WHiYo+uKusl350stQL5zcePEuvbWdmRy0JkXfDK+AXb/aLaxSs5sCMQEOi7wjWaJoSHgQ9gx2etwjGnE1liLfPd7sRlDsPfEoHDB/rqm8c2ytYrEdt/Ez4zX4iZ9xZT2rmVfksw/BSmIUcHqSCvVBU8Kgv3Bqyae2grngWFQ==\"],\"x5t\":\"oTasT0SVdaFcm-ZIQiEpohLqF2c\",\"x5t#S256\":\"EOSRu81mD56F_uCJEcbjYlg1LIS6Up4vKOjO5C3R-fo\",\"n\":\"zROVmrY6GkEF24oux7t7gGB8XAW5-trOPswn3XkoX2V7q0w2RUhYFSNHJ4XwcCQXKGruIblFN5j60ACrXsKAIJyawoeTbv4hmUGDgOaQm4fQTB_SZIc2x-yUH7qr8UAvFBTiN0eiuh1B1QQ2xE_eqPuQhNYP_P27Zl2l__gMuD4kMjXxoWK9pKIThjJrhaPk6Vpk9RnOxs0xVPQMKQv8OBpZ6GlqZ2FdlyQ9zWPGaoplQKSxopzGWjkO1Pe35f5xpqc5mL_E3gU2Lso849h4z7BzZBIHUYiJDmgXiN_qNrnWFpVnNJQzgBU9gBcpggrXuPjRj3riUsJBozYUPxX1Vw\",\"e\":\"AQAB\"},{\"kid\":\"t0NkXkUKEYZO1YOeNd9KACmr-hRvjlj2ZhlWDOmzCIo\",\"kty\":\"RSA\",\"alg\":\"RSA-OAEP\",\"use\":\"enc\",\"x5c\":[\"MIIClzCCAX8CBgGacpnbEzANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTI1MTExMTExMDYxN1oXDTM1MTExMTExMDc1N1owDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOHmLHC7oSLE44qRPqTxG9PCGAtHWwNx2tvVlcvtBmAUfyYPRy7fbLVCXD6hOyKrNlNbnpgiLv23mHRXOXLZDT0cxgZgkj0jJ/j/F2boPmeE3EWJWk4F9niewMmB4QXfQd2ogem1byDRT8rWOx5Ynjf+XYHC1SeB/ex0Bzs7BhUKf5ymzE0s+MARB0RHh3aRqXOkZxVcUkGBI7qxIOH/bNMSB0a7a9vGz7yQS+bY19srAJTAW40DHtbu6Pd7Jn/YGod0Co4lgdYzj6VxcxyMDcKkF16Rwh/xsG9xmDmbr8myfvt5ta/8TYzfSNy3nCJGHIedTq+bQIBOAFuL4gOMoGMCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAJ0OFnW0aQ36gLs8swnBTMgE6kgK6R6uOTKy3Y8i/w7fPevH5MOPOs5wb2hmxFD9QMsK8gEsOLVwbAD56YKKRZNaDK/j4dQML2zDSlRVFiNjpYXbua8cMDz3j+kdDsSucATERmvb6U7qNaQM0TIMaEkc2PxE5DfSnv3L6K+iPuLPI4MladA4R/amjIiKxrT2Kd8mddx3mkzbX5hidVvGrwzmJsx8bBDjMDxkd24T6B/kfQpUWTL+3PGMsNwm8uj8W0WnizUbHnz2SdkxpEI057e7ppVqU4Us6zgrP9kW9mkkoepbN4F/HCwawJ0z7xBVQWL6aWG2BUpPhKozaLjBUxg==\"],\"x5t\":\"EG4i92hoafIFx5O0H4aKdQB2_cY\",\"x5t#S256\":\"-ASKs42Q3GZanc3xaExARLOwKK8aCPc0hcQsacMZNb0\",\"n\":\"4eYscLuhIsTjipE-pPEb08IYC0dbA3Ha29WVy-0GYBR_Jg9HLt9stUJcPqE7Iqs2U1uemCIu_beYdFc5ctkNPRzGBmCSPSMn-P8XZug-Z4TcRYlaTgX2eJ7AyYHhBd9B3aiB6bVvINFPytY7HlieN_5dgcLVJ4H97HQHOzsGFQp_nKbMTSz4wBEHREeHdpGpc6RnFVxSQYEjurEg4f9s0xIHRrtr28bPvJBL5tjX2ysAlMBbjQMe1u7o93smf9gah3QKjiWB1jOPpXFzHIwNwqQXXpHCH_Gwb3GYOZuvybJ--3m1r_xNjN9I3LecIkYch51Or5tAgE4AW4viA4ygYw\",\"e\":\"AQAB\"}]}")));
        WireMock.stubFor(WireMock.get("/realms/test").willReturn(WireMock.aResponse().withBody("{\"realm\":\"test\",\"public_key\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzROVmrY6GkEF24oux7t7gGB8XAW5+trOPswn3XkoX2V7q0w2RUhYFSNHJ4XwcCQXKGruIblFN5j60ACrXsKAIJyawoeTbv4hmUGDgOaQm4fQTB/SZIc2x+yUH7qr8UAvFBTiN0eiuh1B1QQ2xE/eqPuQhNYP/P27Zl2l//gMuD4kMjXxoWK9pKIThjJrhaPk6Vpk9RnOxs0xVPQMKQv8OBpZ6GlqZ2FdlyQ9zWPGaoplQKSxopzGWjkO1Pe35f5xpqc5mL/E3gU2Lso849h4z7BzZBIHUYiJDmgXiN/qNrnWFpVnNJQzgBU9gBcpggrXuPjRj3riUsJBozYUPxX1VwIDAQAB\",\"token-service\":\"http://localhost:9021/realms/test/protocol/openid-connect\",\"account-service\":\"http://localhost:9021/realms/test/account\",\"tokens-not-before\":0}")));
        WireMock.stubFor(WireMock.get("/realms/test/.well-known/openid-configuration").willReturn(WireMock.aResponse().withBody("{\"issuer\":\"http://localhost:9021/realms/test\",\"authorization_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/auth\",\"token_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/token\",\"introspection_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/token/introspect\",\"userinfo_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/userinfo\",\"end_session_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/logout\",\"frontchannel_logout_session_supported\":true,\"frontchannel_logout_supported\":true,\"jwks_uri\":\"http://localhost:9021/realms/test/protocol/openid-connect/certs\",\"check_session_iframe\":\"http://localhost:9021/realms/test/protocol/openid-connect/login-status-iframe.html\",\"grant_types_supported\":[\"authorization_code\",\"client_credentials\",\"implicit\",\"password\",\"refresh_token\",\"urn:ietf:params:oauth:grant-type:device_code\",\"urn:ietf:params:oauth:grant-type:token-exchange\",\"urn:ietf:params:oauth:grant-type:uma-ticket\",\"urn:openid:params:grant-type:ciba\"],\"acr_values_supported\":[\"0\",\"1\"],\"response_types_supported\":[\"code\",\"none\",\"id_token\",\"token\",\"id_token token\",\"code id_token\",\"code token\",\"code id_token token\"],\"subject_types_supported\":[\"public\",\"pairwise\"],\"prompt_values_supported\":[\"none\",\"login\",\"consent\"],\"id_token_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"id_token_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"id_token_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"userinfo_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"userinfo_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"userinfo_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"request_object_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"request_object_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"request_object_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\",\"query.jwt\",\"fragment.jwt\",\"form_post.jwt\",\"jwt\"],\"registration_endpoint\":\"http://localhost:9021/realms/test/clients-registrations/openid-connect\",\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"token_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"introspection_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"introspection_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"authorization_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"authorization_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"authorization_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"claims_supported\":[\"iss\",\"sub\",\"aud\",\"exp\",\"iat\",\"auth_time\",\"name\",\"given_name\",\"family_name\",\"preferred_username\",\"email\",\"acr\",\"azp\",\"nonce\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":true,\"scopes_supported\":[\"openid\",\"phone\",\"service_account\",\"profile\",\"microprofile-jwt\",\"organization\",\"roles\",\"email\",\"offline_access\",\"address\",\"basic\",\"web-origins\",\"acr\"],\"request_parameter_supported\":true,\"request_uri_parameter_supported\":true,\"require_request_uri_registration\":true,\"code_challenge_methods_supported\":[\"plain\",\"S256\"],\"tls_client_certificate_bound_access_tokens\":true,\"dpop_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"ES256\",\"RS256\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"revocation_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/revoke\",\"revocation_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"revocation_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"backchannel_logout_supported\":true,\"backchannel_logout_session_supported\":true,\"device_authorization_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/auth/device\",\"backchannel_token_delivery_modes_supported\":[\"poll\",\"ping\"],\"backchannel_authentication_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/ext/ciba/auth\",\"backchannel_authentication_request_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"ES256\",\"RS256\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"require_pushed_authorization_requests\":false,\"pushed_authorization_request_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/ext/par/request\",\"mtls_endpoint_aliases\":{\"token_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/token\",\"revocation_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/revoke\",\"introspection_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/token/introspect\",\"device_authorization_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/auth/device\",\"registration_endpoint\":\"http://localhost:9021/realms/test/clients-registrations/openid-connect\",\"userinfo_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/userinfo\",\"pushed_authorization_request_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/ext/par/request\",\"backchannel_authentication_endpoint\":\"http://localhost:9021/realms/test/protocol/openid-connect/ext/ciba/auth\"},\"authorization_response_iss_parameter_supported\":true}")));
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsManifestResource("microprofile-config.properties")
                .addAsResource("payara-mp-jwt.properties")
                .addClasses(ApplicationInit.class, Resource.class);
    }

    @Test
    @RunAsClient
    public void testAuthentication(){
        try(Client client = ClientBuilder.newClient()) {
            Assert.assertEquals("This is a public resource", client.target(uri + "resource/public").request().get(String.class));
            Assert.assertEquals(401, client.target(uri + "resource/protected").request().get().getStatus());
            Assert.assertEquals(200, client.target(uri + "resource/protected").request().header("Authorization", BEARER_TOKEN).get().getStatus());
            Assert.assertEquals("This is a protected resource", client.target(uri + "resource/protected").request().header("Authorization", BEARER_TOKEN).get(String.class));

            // Key should be cached for 5 seconds.
            wireMockServer.stop();
            Assert.assertThrows(ProcessingException.class, () -> client.target("http://localhost:9021/realms/test").request().get().getStatus());
            System.out.println("Sleeping for 5 seconds to expire key cache.");
            Thread.sleep(5000);
            // We should still be able to access for 5000ms
            Assert.assertEquals(200, client.target(uri + "resource/protected").request().header("Authorization", BEARER_TOKEN).get().getStatus());

            Thread.sleep(1000);
            Assert.assertEquals(200, client.target(uri + "resource/protected").request().header("Authorization", BEARER_TOKEN).get().getStatus());
            System.out.println("Sleeping for 5 seconds to expire cache error retention.");
            Thread.sleep(5000);
            // Key should be expired now
            Assert.assertEquals(401, client.target(uri + "resource/protected").request().header("Authorization", BEARER_TOKEN).get().getStatus());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
