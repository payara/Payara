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
    private static final String BEARER_TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJBWU5uWS1aZXhhQjYwaTdlbnlDR1VJNlBtN01wWExIZjYwWmd3b3Voa05jIn0.eyJleHAiOjMzMzg4Mzg5OTUsImlhdCI6MTc2MjM4NDU5NSwianRpIjoib25ydHJvOjFhNmQwNjQ3LTMzMWQtYzA4My01YWQ5LTYzYmU3ZWE4YzNmNSIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODI4Mi9yZWFsbXMvdGVzdCIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJkMzM2NGZkZC1hM2Q5LTRjNjEtOWU2My0xOGNhZjFmNTlkYTciLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJ0ZXN0Iiwic2lkIjoiMGU1ZTcxZTctNTgwOC1kOTI2LTgyZjEtMjJjMjljNTk2ZGNjIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy10ZXN0IiwiYXJjaGl0ZWN0Iiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiZ3JvdXBzIjpbImRlZmF1bHQtcm9sZXMtdGVzdCIsImFyY2hpdGVjdCIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXSwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdCJ9.Z_FiHJt00Fz-IeGO77H0xsPT7RS2kYwGr6ND0mRHC27xTbxoLmXY2WAm965pDFvwwKa-FEMvdJlHYWrx3eRZXiHTxKNW3qmSqDdn2uxIyxH3-_SwiSKnvNhEn2ar8qknGJ5bch8St0k0Lr6l-1thqdT68CmImBKWGp292USCihYT-FGe0IKNmCvKlOq6ccuFATtzIeMqIOxHPlbNzuHFs9RxuOr4Nc78BAVP35h6QY6TXUElsf1X-jR4rCf3JIybhb8fFvR-QuGPiNY1PUyef1c_Y9Hh6FUpcZvQER_gHD-NJ7t-vdRMiQfbBFbxDnKgoflIYVlMfczV_1UdLH0rLw";


    @Before
    public void setup() {
        wireMockServer = new WireMockServer(8282);
        WireMock.configureFor("localhost", 8282);
        wireMockServer.start();
        WireMock.stubFor(WireMock.get("/realms/test/protocol/openid-connect/certs").willReturn(WireMock.aResponse().withBody("{\"keys\":[{\"kid\":\"y3XbgHFpl-Yk_Ak7vo7HCtrFmRTMe2yjGl_vbZpN4Ec\",\"kty\":\"RSA\",\"alg\":\"RSA-OAEP\",\"use\":\"enc\",\"x5c\":[\"MIIClzCCAX8CBgGaVjGq3TANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTI1MTEwNTIyNDMwNloXDTM1MTEwNTIyNDQ0NlowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJtcSYy5ZNEY34Cf5nL8aPNNqjaA6SiullYLZbOlBq1hk2prK8xChUDlQIyk+3yA7G7aQwQ2BKPoU30yOt6Q751yhUjabuKt0y1FUFqL0o2ybILSyPJIalwlH/zCcTlNCVFB+GCOcxkdKCP37DfDKllVNNTG9ByZqxf+U1tLKmNRXNvet1l3+q/0HUdSmbEKNRHYWocHp3DpB2Dp+10UwAfE7hvp6MTSQrGiUt1SiAzF1t1Ns1VXVZ93ezX/Y7JYOMTgvi1GP/1aMKlq3xy+o3KulzVk04Mnyd+gGmguEg246OF4D13sFXvmRN6j8RhVKcQ3Z+CPZQw10oLkZtrKF3ECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEASfsQfaYrebi+OrHJ3WIWG18517eOApcYtFOGeQBe0cp7099seCTFtThwIxkXhAH/gyBY8n+kt3d0sKK32e4OA3ToOoIYdlChjH1BIqyHMeZLk7wfMayx3PzdMNzXqq650V9cHIXuSlm/9e0pzzZu9X9UGQ7Lu7IO4yVAKOkgfM9JU7NLr/PmVy1VBbkEuuYZ1zGAOkSsStVcOwnZONZ8DwjZyo4gDeHHoTdgzjprliKF0s1aLEQ0CUvqpIalt5Wb4T8ylSrPpRPFEg94m33yMm9Bx8wdl5zEPKc2F/Cyoe/kbyFWCvAdDrlkixX9P5wNO/q9jucol8NqJghyRyfHHw==\"],\"x5t\":\"e6JMm85VVYiUJpeSdJzAmzYMfS0\",\"x5t#S256\":\"m94GkzuFqPR5aFZbfsomBhiUBvwht8VuSkc4DHyjBAI\",\"n\":\"m1xJjLlk0RjfgJ_mcvxo802qNoDpKK6WVgtls6UGrWGTamsrzEKFQOVAjKT7fIDsbtpDBDYEo-hTfTI63pDvnXKFSNpu4q3TLUVQWovSjbJsgtLI8khqXCUf_MJxOU0JUUH4YI5zGR0oI_fsN8MqWVU01Mb0HJmrF_5TW0sqY1Fc2963WXf6r_QdR1KZsQo1EdhahwencOkHYOn7XRTAB8TuG-noxNJCsaJS3VKIDMXW3U2zVVdVn3d7Nf9jslg4xOC-LUY__VowqWrfHL6jcq6XNWTTgyfJ36AaaC4SDbjo4XgPXewVe-ZE3qPxGFUpxDdn4I9lDDXSguRm2soXcQ\",\"e\":\"AQAB\"},{\"kid\":\"AYNnY-ZexaB60i7enyCGUI6Pm7MpXLHf60ZgwouhkNc\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"x5c\":[\"MIIClzCCAX8CBgGaVjGqLzANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTI1MTEwNTIyNDMwNloXDTM1MTEwNTIyNDQ0NlowDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALH6PB9YFj84EflM0lK49B2MyimCd1PXxpZwoi8AuP7+ZUMqRD4mMuz+XCXXKyc4+UM+9tCC+QIeguO8cB4DRXoknutGhsItupcBorsD23r+nUD56M8J9i+J7m4l8YkcnjnXEMSRa/9BjcQbQjevBTxXlNtiKay3Zvs3FqgQO3m59Y3swvBBb1HuPy1fn/XIjOw5BPpS/uCmkK+aP0npKB9DfE9Fy0wix7n0QROPdgP3vakrhZPbJRNGPVF+68sEOmqJkjd3sUG706rQJ8rnHpiAKibxyx9916KtJBJ0S4AnX+xh7g0hhPK2E0/xXDxRJh9F+iY1AUCZAiU+1GMRMKUCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEARPHLp5xaWXqVhPLGTgiALPGOFdKDYlreXOC9UUJOcuRIposUgumzETHZNY3u6w9+GNObwgwg3qB5ptV2u7pV5Rm/xjdY8hrwkr+q0uAPFYnykLc59rrY6qfvDZ7DlMRnSOe1sF1FiPIEyxDduAWLfc9rU2neHc7J+dW4BZbAoX9QmM8FDD6QTZys9c+KtFn9KkCn/mn4uPIZyX3xKo7LdMUebfYDX5eulvyQIwwFBg0mzPCsPXGlSdHgfkJTvfe6vKDeX0H/DGZwD5+uY2JCMpYefyfdHyFQpG0aMGZ5pweZ93RJdi1gX+43l0GAMPFcD8vxibKvaFpI2tQZ7YMaFg==\"],\"x5t\":\"RKT4onMU66KxFOpF3RblgaliXOE\",\"x5t#S256\":\"NKw5qXo9pJ-x66BMj7N9SwSACCGdo4Ir2VWUs0DQm90\",\"n\":\"sfo8H1gWPzgR-UzSUrj0HYzKKYJ3U9fGlnCiLwC4_v5lQypEPiYy7P5cJdcrJzj5Qz720IL5Ah6C47xwHgNFeiSe60aGwi26lwGiuwPbev6dQPnozwn2L4nubiXxiRyeOdcQxJFr_0GNxBtCN68FPFeU22IprLdm-zcWqBA7ebn1jezC8EFvUe4_LV-f9ciM7DkE-lL-4KaQr5o_SekoH0N8T0XLTCLHufRBE492A_e9qSuFk9slE0Y9UX7rywQ6aomSN3exQbvTqtAnyucemIAqJvHLH33Xoq0kEnRLgCdf7GHuDSGE8rYTT_FcPFEmH0X6JjUBQJkCJT7UYxEwpQ\",\"e\":\"AQAB\"}]}")));
        WireMock.stubFor(WireMock.get("/realms/test").willReturn(WireMock.aResponse().withBody("{\"realm\":\"test\",\"public_key\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsfo8H1gWPzgR+UzSUrj0HYzKKYJ3U9fGlnCiLwC4/v5lQypEPiYy7P5cJdcrJzj5Qz720IL5Ah6C47xwHgNFeiSe60aGwi26lwGiuwPbev6dQPnozwn2L4nubiXxiRyeOdcQxJFr/0GNxBtCN68FPFeU22IprLdm+zcWqBA7ebn1jezC8EFvUe4/LV+f9ciM7DkE+lL+4KaQr5o/SekoH0N8T0XLTCLHufRBE492A/e9qSuFk9slE0Y9UX7rywQ6aomSN3exQbvTqtAnyucemIAqJvHLH33Xoq0kEnRLgCdf7GHuDSGE8rYTT/FcPFEmH0X6JjUBQJkCJT7UYxEwpQIDAQAB\",\"token-service\":\"http://localhost:8282/realms/test/protocol/openid-connect\",\"account-service\":\"http://localhost:8282/realms/test/account\",\"tokens-not-before\":0}")));
        WireMock.stubFor(WireMock.get("/realms/test/.well-known/openid-configuration").willReturn(WireMock.aResponse().withBody("{\"issuer\":\"http://localhost:8282/realms/test\",\"authorization_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/auth\",\"token_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/token\",\"introspection_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/token/introspect\",\"userinfo_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/userinfo\",\"end_session_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/logout\",\"frontchannel_logout_session_supported\":true,\"frontchannel_logout_supported\":true,\"jwks_uri\":\"http://localhost:8282/realms/test/protocol/openid-connect/certs\",\"check_session_iframe\":\"http://localhost:8282/realms/test/protocol/openid-connect/login-status-iframe.html\",\"grant_types_supported\":[\"authorization_code\",\"client_credentials\",\"implicit\",\"password\",\"refresh_token\",\"urn:ietf:params:oauth:grant-type:device_code\",\"urn:ietf:params:oauth:grant-type:token-exchange\",\"urn:ietf:params:oauth:grant-type:uma-ticket\",\"urn:openid:params:grant-type:ciba\"],\"acr_values_supported\":[\"0\",\"1\"],\"response_types_supported\":[\"code\",\"none\",\"id_token\",\"token\",\"id_token token\",\"code id_token\",\"code token\",\"code id_token token\"],\"subject_types_supported\":[\"public\",\"pairwise\"],\"prompt_values_supported\":[\"none\",\"login\",\"consent\"],\"id_token_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"id_token_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"id_token_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"userinfo_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"userinfo_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"userinfo_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"request_object_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\",\"none\"],\"request_object_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"request_object_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\",\"query.jwt\",\"fragment.jwt\",\"form_post.jwt\",\"jwt\"],\"registration_endpoint\":\"http://localhost:8282/realms/test/clients-registrations/openid-connect\",\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"token_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"introspection_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"introspection_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"authorization_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"authorization_encryption_alg_values_supported\":[\"ECDH-ES+A256KW\",\"ECDH-ES+A192KW\",\"ECDH-ES+A128KW\",\"RSA-OAEP\",\"RSA-OAEP-256\",\"RSA1_5\",\"ECDH-ES\"],\"authorization_encryption_enc_values_supported\":[\"A256GCM\",\"A192GCM\",\"A128GCM\",\"A128CBC-HS256\",\"A192CBC-HS384\",\"A256CBC-HS512\"],\"claims_supported\":[\"iss\",\"sub\",\"aud\",\"exp\",\"iat\",\"auth_time\",\"name\",\"given_name\",\"family_name\",\"preferred_username\",\"email\",\"acr\",\"azp\",\"nonce\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":true,\"scopes_supported\":[\"openid\",\"acr\",\"microprofile-jwt\",\"address\",\"basic\",\"service_account\",\"email\",\"organization\",\"phone\",\"offline_access\",\"profile\",\"web-origins\",\"roles\"],\"request_parameter_supported\":true,\"request_uri_parameter_supported\":true,\"require_request_uri_registration\":true,\"code_challenge_methods_supported\":[\"plain\",\"S256\"],\"tls_client_certificate_bound_access_tokens\":true,\"dpop_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"ES256\",\"RS256\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"revocation_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/revoke\",\"revocation_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"revocation_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"backchannel_logout_supported\":true,\"backchannel_logout_session_supported\":true,\"device_authorization_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/auth/device\",\"backchannel_token_delivery_modes_supported\":[\"poll\",\"ping\"],\"backchannel_authentication_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/ext/ciba/auth\",\"backchannel_authentication_request_signing_alg_values_supported\":[\"PS384\",\"RS384\",\"EdDSA\",\"ES384\",\"ES256\",\"RS256\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"require_pushed_authorization_requests\":false,\"pushed_authorization_request_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/ext/par/request\",\"mtls_endpoint_aliases\":{\"token_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/token\",\"revocation_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/revoke\",\"introspection_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/token/introspect\",\"device_authorization_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/auth/device\",\"registration_endpoint\":\"http://localhost:8282/realms/test/clients-registrations/openid-connect\",\"userinfo_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/userinfo\",\"pushed_authorization_request_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/ext/par/request\",\"backchannel_authentication_endpoint\":\"http://localhost:8282/realms/test/protocol/openid-connect/ext/ciba/auth\"},\"authorization_response_iss_parameter_supported\":true}")));
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
            Assert.assertThrows(ProcessingException.class, () -> client.target("http://localhost:8282/realms/test").request().get().getStatus());
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
