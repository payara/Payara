/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 */

package fish.payara.appserver.web.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GrizzlyConnectorTestManual {
    @ClassRule
    public static GrizzlyTestHarness harness = new GrizzlyTestHarness();

    private static HttpClient client;

    @BeforeClass
    public static void setupServer() throws LifecycleException {
        client = HttpClient.newHttpClient();
    }


    @Test
    public void pureGrizzlyStartup() throws IOException, InterruptedException {
        var hello = new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                response.setStatus(200);
                response.setContentType("text/plain");
                response.getOutputBuffer().write("Hello from Grizzly");
            }

            @Override
            public String getName() {
                return "hello"; // otherwise cannot be removed
            }
        };
        harness.grizzly.config.addHttpHandler(hello, "/pureGrizzlyStartup/");

        var response = get("/pureGrizzlyStartup");
        assertEquals(200, response.statusCode());
        assertEquals("Hello from Grizzly", response.body());
        // grizzly.config.removeHttpHandler(hello); // there's a bug in HttpHandlerChain.removeHttpHandler which would make this the root handler instead
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + harness.grizzly.port).resolve(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    //@Ignore("Running two catalina processes in single jvm breaks NamingContextListener")
    //@Test
    public void pureCatalinaStartup() throws LifecycleException, IOException, InterruptedException {
        var catalina = new GrizzlyTestHarness.Catalina();
        var coyote = new Connector();
        coyote.setPort(0);
        catalina.start(coyote);
        var ctx = catalina.addContext("ROOT");

        catalina.addServlet(ctx, new TestServlet1(), "");

        var response = getRoot(coyote.getLocalPort());
        assertEquals(200, response.statusCode());
        assertEquals("Hello from Servlet", response.body());

        catalina.stop();
    }

    private HttpResponse<String> getRoot(int port) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void bridgeStartup() throws LifecycleException, IOException, InterruptedException {
        var ctx = harness.addContext("ROOT", c -> c.addServlet(new TestServlet1(), ""));

        var response = getRoot(harness.grizzly.port);
        assertEquals(200, response.statusCode());
        assertEquals("Hello from Servlet", response.body());
        assertTrue(response.headers().firstValue("X-Powered-By").isPresent());

        harness.catalina.removeServletByMapping(ctx, "");
    }


}
