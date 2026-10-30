/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.programatic;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class DeployApplicationTest {

    private static final File WAR_FILE = createWar();
    private static final String WEBAPP_CONTEXT = "/" + WAR_FILE.getName().substring(0, WAR_FILE.getName().length() - 4);
    private static final String HOST_NAME = System.getProperty("payara.adminHost", "localhost");

    private PayaraMicroServer server;

    @Before
    public void startMicroInstance() {
        server = PayaraMicroServer.newInstance();
    }

    @Test
    public void deployApplicationInVanillaMode() throws Exception {
        assertNotNull(server);
        server.start("--autobindhttp", "--nocluster", "--deploy", WAR_FILE.getAbsolutePath());
        assertEquals(TestServlet.RESPONSE_TEXT, download(HOST_NAME, server.getHttpPort()));
    }

    @Test
    public void deployApplicationInVanillaModeWithHazelcast() throws Exception {
        assertNotNull(server);
        server.start("--autobindhttp", "--nohazelcast", "--deploy", WAR_FILE.getAbsolutePath());
        assertEquals(TestServlet.RESPONSE_TEXT, download(HOST_NAME, server.getHttpPort()));
    }

    @After
    public void stopMicroInstance() {
        if (server != null) {
            server.stop();
        }
    }

    private String download(String hostName, int hostPort) throws Exception {
        final URL url = new URL("http", hostName, hostPort, WEBAPP_CONTEXT);
        final Object object = url.getContent();
        if (object instanceof InputStream) {
            final InputStream input = (InputStream) object;
            try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
                return scanner.nextLine();
            } finally {
                input.close();
            }
        }
        throw new AssertionError("Expected input stream, but received this: " + object.toString());
    }

    private static File createWar() {
        try {
            final WebArchive war = ShrinkWrap.create(WebArchive.class).addClass(TestServlet.class);
            System.out.println(war);
            final File warFile = File.createTempFile(TestServlet.class.getSimpleName(), "WebApp.war");
            warFile.deleteOnExit();
            war.as(ZipExporter.class).exportTo(warFile, true);
            return warFile;
        } catch (Exception e) {
            throw new AssertionError("Failed to create war file", e);
        }
    }

}
