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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;

@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class GrpcTest {
    @ArquillianResource
    private URL baseUrl;

    @Deployment(testable = false)
    public static Archive<?> deploy() {

        final Archive<?> archive = PayaraTestShrinkWrap.getWebArchive() //
                // Add protobuf classes
                .addPackage(PayaraProto.class.getPackage()) //
                // Add source package
                .addPackage(PayaraService.class.getPackage()) //
                // Add test classes
                .addClasses(GrpcTest.class, GrpcClient.class)
                // Make sure context root and classloading is configured by GF descriptor
                .addAsWebInfResource("glassfish-web.xml");

        return archive;
    }

    @Test
    public void test_async_grpc_call_streams_correctly() throws InterruptedException, URISyntaxException {
        final GrpcClient client = new GrpcClient(baseUrl);
        client.communicate();
        assertNull(client.getError());
    }

    @Test
    public void test_non_grpc_call_skips_grpc_filtering() throws URISyntaxException {
        final WebTarget target = ClientBuilder.newClient()
                .target(baseUrl.toURI().resolve("/fish.payara.samples.grpc.PayaraService"));
        final Response response = target.request().get();

        final String body = response.readEntity(String.class);
        assertEquals("Hello World!", body);
    }
}
