/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2019-2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.distributions.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author David Matejcek
 */
@Testcontainers
public class PayaraServerFullTest {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraServerFullTest.class);

    @Container
    private static final PayaraContainer CONTAINER = new PayaraContainer("payara/server-full") //
        .withExposedPorts(4848, 8080);


    @BeforeAll
    public static void initLogging() {
        CONTAINER.followOutput(new Slf4jLogConsumer(LOG));
    }

    @Test
    public void testStartedServerEndpoints() throws Exception {
        assertTrue(CONTAINER.isRunning(), "server is running");
        assertAll( //
            () -> assertNotNull(CONTAINER.getMappedPort(4848), "admin port"), //
            () -> assertNotNull(CONTAINER.getMappedPort(8080), "http port") //
        );
        final URL welcomePageUrl = CONTAINER.getHttpUrl(8080);
        assertNotNull(welcomePageUrl, "welcome page url");
        final String welcomePage = getPageContent(welcomePageUrl);
        assertThat("welcome page", welcomePage, containsString("Hello from Payara - your server is now running!"));

        final URL adminPageUrl = CONTAINER.getHttpsUrl(4848);
        assertNotNull(adminPageUrl, "admin page url");
        try {
            getPageContent(adminPageUrl);
            fail("The certificate was accepted despite it is not trusted!");
        } catch (final SSLHandshakeException e) {
            // note: the message may be localized or changed, update it appropriately (or find
            // another suitable check)
            assertThat("e.message", e.getMessage(),
                containsString("unable to find valid certification path to requested target"));
        }
    }

    @Test
    public void testOpenSslIsRemoved() throws IOException, InterruptedException {
        String openSslVersionOutput = CONTAINER.execInContainer("/bin/sh","-c","openssl","version").getStderr();
        assertTrue(openSslVersionOutput.contains("openssl: not found"));
    }


    private String getPageContent(final URL url) throws IOException {
        final URLConnection conn = url.openConnection();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
