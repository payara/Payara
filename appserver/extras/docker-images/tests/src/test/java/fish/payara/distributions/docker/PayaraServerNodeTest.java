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
import java.time.Duration;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

/**
 * @author David Matejcek
 */
@Testcontainers
public class PayaraServerNodeTest {

    private static final int DAS_ADMIN_PORT = 4848;
    private static final int NODE_HTTP_PORT = 28080;

    private static final Logger DASLOG = LoggerFactory.getLogger("DAS");
    private static final Logger NODELOG = LoggerFactory.getLogger("NODE");

    @Container
    private static final PayaraContainer DAS = new PayaraContainer("payara/server-full") //
        .withExposedPorts(4848, 8080);

    @Container
    private final PayaraContainer node = new PayaraContainer("payara/server-node").withExposedPorts(NODE_HTTP_PORT) //
        .withEnv("PAYARA_DAS_HOST", "host.testcontainers.internal")
        .withEnv("PAYARA_DAS_PORT", Integer.toString(DAS.getMappedPort(DAS_ADMIN_PORT)))
        .withEnv("DOCKER_CONTAINER_IP", "host.testcontainers.internal") //
        .withStartupTimeout(Duration.ofSeconds(60));

    @BeforeAll
    public static void initDAS() throws Exception {
        DAS.followOutput(new Slf4jLogConsumer(DASLOG));
        org.testcontainers.Testcontainers.exposeHostPorts(DAS.getMappedPort(4848));
    }


    @BeforeEach
    public void initNODE() {
        node.followOutput(new Slf4jLogConsumer(NODELOG));
    }


    @Test
    @Timeout(value = 30)
    public void testStartedServerEndpoints() throws Exception {
        assertTrue(node.isRunning(), "server is running");
        assertAll( //
            () -> assertNotNull(DAS.getMappedPort(4848), "DAS: admin port"), //
            () -> assertNotNull(node.getMappedPort(28080), "NODE: http port") //
        );
        final URL welcomePageUrl = node.getHttpUrl(28080);
        assertNotNull(welcomePageUrl, "welcome page url");
        final String welcomePage = getPageContent(welcomePageUrl);
        assertThat("welcome page of the node", welcomePage,
            containsString("Hello from Payara - your server is now running!"));
    }


    private String getPageContent(final URL url) throws IOException {
        final URLConnection conn = url.openConnection();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
