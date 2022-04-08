/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.samples.grpc;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.grpc.ejbcomponents.PropertiesBean;
import fish.payara.samples.grpc.ejbcomponents.StatelessEjb;
import io.grpc.Status;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RunWith(PayaraArquillianTestRunner.class)
public class GrpcEjbTest {

    @EJB
    PropertiesBean propertiesBean;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "grpc-ejb.war")
                .addClass(PayaraService.class)
                .addPackage(StatelessEjb.class.getPackage())
                .addPackages(true, PayaraArquillianTestRunner.class.getPackage())
                .addAsResource("Strings.properties")
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/payara-web.xml"));
    }

    @Test
    @InSequence(1)
    public void checkGrpcModuleInstalled() {
        FilenameFilter filter = (dir, name) -> name.matches("(grpc-support-.*[.]jar)");
        Assert.assertNotNull("gRPC Module must be in the modules directory for these tests.",
                new File(System.getProperty("com.sun.aas.installRoot") + File.separator + "modules").list(filter).length);

    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void grpcClientWithEjbSuccessfulTest() throws InterruptedException, MalformedURLException {
        URL myURL = new URL("http://localhost:8080/fish.payara.samples.grpc.PayaraService/communicate"); // URL for the deployed gRPC service
        final GrpcClient client = new GrpcClient(myURL);
        client.communicate("Hello test!");
        Assert.assertNull(client.getError());

        client.communicate("Second hello test!");
    }

    @Test
    @InSequence(3)
    public void testGrpcCallsWereSuccessful() throws IOException {
        Assert.assertTrue("The server logs should contain the message in the request ", checkServerLogs("Processing message: Hello test!"));
        Assert.assertEquals(2, propertiesBean.getCallCount());
    }

    @Test
    @RunAsClient
    @InSequence(4)
    public void testGrpcClientError() throws MalformedURLException, InterruptedException {
        URL myURL = new URL("http://localhost:8080/fish.payara.samples.grpc.PayaraService/communicate"); // URL for the deployed gRPC service
        final GrpcClient client = new GrpcClient(myURL);
        client.communicate("Error");
        Assert.assertEquals(Status.Code.PERMISSION_DENIED.toString(), client.getError().getMessage());
    }

    public boolean checkServerLogs(String message) throws IOException {
        List<String> log = viewLog();
        for (String line : log) {
            if (line.contains(message)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the contents of the server log
     */
    private List<String> viewLog() throws IOException {
        Path serverLog = ServerOperations.getDomainPath("logs/server.log");
        return Files.readAllLines(serverLog);
    }

}