/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2020-2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.distributions.docker;

import fish.payara.distributions.docker.war.TestServlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.containers.BindMode.READ_ONLY;

/**
 * Test for Payara Micro docker container basic functionality.
 *
 * @author David Matejcek
 */
@Testcontainers
public class PayaraMicroTest {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraMicroTest.class);
    private static final Class<TestServlet> SERVLET_CLASS = TestServlet.class;
    private static final File TC_DIR = new File(System.getProperty("testContainersWorkDir"));
    private static final File WAR_FILE = createWar();
    private static final String WEBAPP_CONTEXT = "/" + WAR_FILE.getName().substring(0, WAR_FILE.getName().length() - 4);

    @Container
    private final PayaraContainer container = new PayaraContainer("payara/micro") //
        .withExposedPorts(8080)
        .withLogConsumer(new Slf4jLogConsumer(LOG))
        .withFileSystemBind(WAR_FILE.getAbsolutePath(), "/opt/payara/deployments/" + WAR_FILE.getName(), READ_ONLY)
        .waitingFor(Wait.forLogMessage(".*Payara Micro.+ ready.+\\n", 1));

    private static File createWar() {
        try {
            final WebArchive war = ShrinkWrap.create(WebArchive.class).addClass(SERVLET_CLASS);
            TC_DIR.mkdirs();
            final File warFileOnHost = new File(TC_DIR, TestServlet.class.getSimpleName() + "WebApp.war");
            war.as(ZipExporter.class).exportTo(warFileOnHost, true);
            return warFileOnHost;
        } catch (Exception e) {
            return fail(e);
        }
    }


    /**
     * Tests if Micro can be started and it the war application has been deployed and works.
     *
     * @throws Exception
     */
    @Test
    public void testServlet() throws Exception {
        assertTrue(container.isRunning(), "server is running");
        assertAll( //
            () -> assertNotNull(container.getMappedPort(8080), "http port"), //
            () -> assertEquals(TestServlet.RESPONSE_TEXT, download()) //
        );
    }


    private String download() throws Exception {
        final URL url = new URL("http", container.getContainerIpAddress(), container.getMappedPort(8080), WEBAPP_CONTEXT);
        final Object object = url.getContent();
        if (object instanceof InputStream) {
            final InputStream input = (InputStream) object;
            try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
                return scanner.nextLine();
            } finally {
                input.close();
            }
        }
        return fail("Expected input stream, but received this: " + object.toString());
    }

    @Test
    public void testOpenSslIsRemoved() throws IOException, InterruptedException {
        String openSslVersionOutput = container.execInContainer("/bin/sh","-c","openssl","version").getStderr();
        assertTrue(openSslVersionOutput.contains("openssl: not found"));
    }
}
