/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.test.containers.tst.security;

import fish.payara.test.containers.tools.container.AsadminCommandExecutor;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.env.TestConfiguration;
import fish.payara.test.containers.tools.junit.DockerITestExtension;
import fish.payara.test.containers.tools.junit.WaitForExecutable;
import fish.payara.test.containers.tools.rs.RestClientCache;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.Container.ExecResult;

import static fish.payara.test.containers.tools.container.TestablePayaraPort.DAS_ADMIN_PORT;
import static fish.payara.test.containers.tools.container.TestablePayaraPort.DAS_HTTPS_PORT;
import static fish.payara.test.containers.tools.container.TestablePayaraPort.DAS_HTTP_PORT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Matejcek
 * <pre>
 * mvn clean install -Ptest-containers -pl :test-containers -Ddocker.payara.version=4.1.2.191.17 -Ppayara4 -Dit.test=MetricsSecurityITest
 * </pre>
 */
// PAYARA-3515
@ExtendWith(DockerITestExtension.class)
public class MetricsSecurityITest {

    private static final String USER_NAME = "test";
    // must be same as in passwordfile-user.txt
    private static final String USER_PASSWORD = "admin";
    private static final String RESP_TEXT_SNIPPET = "cpu_system";
    private static final String RESP_JSON_SNIPPET = "cpu.systemLoadAverage";

    private static final TestConfiguration TEST_CFG = TestConfiguration.getInstance();
    private static final RestClientCache RS_CLIENTS = new RestClientCache(createSslContext());

    private static PayaraServerContainer payara;

    @BeforeAll
    public static void backupOfOriginalDomain() throws Exception {
        payara = DockerEnvironment.getInstance().getPayaraContainer();
        payara.asLocalAdmin("stop-domain", TEST_CFG.getPayaraDomainName());
        payara.asLocalAdmin("backup-domain", TEST_CFG.getPayaraDomainName());
    }


    private static SSLContext createSslContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[]{new NaiveTrustManager()}, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Could not initialize SSL context.", e);
        }
    }


    @AfterAll
    public static void cleanupAfterTest() throws Exception {
        RS_CLIENTS.close();
        payara.asLocalAdmin("stop-domain", TEST_CFG.getPayaraDomainName());
        payara.asLocalAdmin("restore-domain", TEST_CFG.getPayaraDomainName());
        payara.asLocalAdmin("start-domain", TEST_CFG.getPayaraDomainName());
    }


    @Test
    public void testProtectedMetrics() throws Throwable {
        payara.asLocalAdmin("start-domain", TEST_CFG.getPayaraDomainName());
        payara.asAdmin("set", "configs.config.server-config.microprofile-metrics-configuration.enabled=true");
        payara.asAdmin("set", "configs.config.server-config.microprofile-metrics-configuration.security-enabled=true");

        new AsadminCommandExecutor(payara, "--terse", "--user", "admin",
            "--passwordfile=" + payara.getConfiguration().getPasswordFileForUserInDocker().getAbsolutePath())
                .exec("create-file-user", "--groups=microprofile", "--target=server-config", "--authrealmname=file",
                    USER_NAME);

        final ExecResult resultA = payara.execInContainer("curl", "-s", "-G", "--insecure", "--verbose",
            "https://localhost:" + DAS_ADMIN_PORT + "/");
        assertAll("resultA.stdErr: \n" + resultA.getStderr(),
            () -> assertThat("resultA.exitCode", resultA.getExitCode(), equalTo(0)),
            () -> assertThat("resultA.stdOut", resultA.getStdout(), containsString("The Admin Console")) //
        );

        final ExecResult result1 = payara.execInContainer("curl", "-s", "-G", "--http1.1", "--verbose",
            "http://localhost:" + DAS_HTTP_PORT + "/");
        assertAll("stdErr: \n" + result1.getStderr(),
            () -> assertThat("result1.exitCode", result1.getExitCode(), equalTo(0)),
            () -> assertThat("result1.stdOut", result1.getStdout(), containsString("The document root folder"
                + " for this server is the docroot subdirectory of this server's domain directory.")) //
        );

        WaitForExecutable.waitFor(() -> assertAll(this::checkAnonymousCurlRequest, this::checkAuthorizedCurlRequest,
            this::checkAuthorizedJerseyTextRequest, this::checkAuthorizedJerseyJsonRequest), 60000L);
    }


    private void checkAnonymousCurlRequest() throws Exception {
        final ExecResult result = payara.execInContainer("curl", "-s", "-G", "--http1.1", "--insecure", "--verbose",
            "https://localhost:" + DAS_HTTPS_PORT + "/metrics/base");
        assertAll("anonymous.curl.stdErr: \n" + result.getStderr(),
            () -> assertThat("anonymous.curl.exitCode", result.getExitCode(), equalTo(0)), //
            () -> assertThat("anonymous.curl.stdOut", result.getStdout(),
                containsString("HTTP Status 401 - Unauthorized")) //
        );
    }

    private void checkAuthorizedCurlRequest() throws Exception {
        final ExecResult result = payara.execInContainer("curl", "-s", "-G", "--http1.1", "--insecure", "--verbose",
            "--anyauth",
            "--user", USER_NAME + ":" + USER_PASSWORD, "https://localhost:" + DAS_HTTPS_PORT + "/metrics/base");
        assertAll("auth.curl.stdErr: \n" + result.getStderr(),
            () -> assertThat("auth.curl.exitCode", result.getExitCode(), equalTo(0)), //
            () -> assertThat("auth.curl.stdOut", result.getStdout(), containsString(RESP_TEXT_SNIPPET)) //
        );
    }


    private void checkAuthorizedJerseyTextRequest() throws Exception {
        // default header was a problem for Payara until CUSTCOM-254
        final String responseBody = doAuthorizedJerseyRequest(null);
        assertThat("auth.jersey.text.entity", responseBody, containsString(RESP_TEXT_SNIPPET));

    }


    private void checkAuthorizedJerseyJsonRequest() throws Exception {
        final String responseBody = doAuthorizedJerseyRequest(MediaType.APPLICATION_JSON_TYPE);
        assertThat("auth.jersey.json.entity", responseBody, containsString(RESP_JSON_SNIPPET));
    }


    private String doAuthorizedJerseyRequest(final MediaType acceptHeader) throws Exception {
        final WebTarget target = RS_CLIENTS
            .getNonPreemptiveClient(true, USER_NAME, USER_PASSWORD)
            .target(payara.getHttpsUrl().toURI()).path("metrics").path("base");
        final Builder requestBuilder = target.request();
        if (acceptHeader != null) {
            requestBuilder.accept(acceptHeader);
        }
        try (Response response = requestBuilder.get()) {
            assertEquals(Status.OK, response.getStatusInfo().toEnum(), "auth.jersey.status");
            assertTrue(response.hasEntity(), "auth.jersey.hasEntity");
            return response.readEntity(String.class);
        }
    }


    private static final class NaiveTrustManager implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }


        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
            // accept everything
        }


        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) {
            // accept everything
        }
    }
}
