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

package fish.payara.samples.jaccperapp;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.junit.Test;
import org.junit.runner.RunWith;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This sample tests that we can install a custom JACC provider
 * using the Payara API.
 *
 * @author Arjan Tijms
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class InstallJaccProviderTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        String pomPath = System.getProperty("pomPath");
        System.out.println(pomPath);
        assertNotNull("System property pomPath", pomPath);
        MavenResolvedArtifact jaccLibrary = Maven.resolver()
             .loadPomFromFile(pomPath)
             .resolve("fish.payara.server.internal.tests:jacc-provider-repackaged:jar:jakartaee9:" + System.getProperty("payara.version"))
             .withTransitivity()
             .asSingleResolvedArtifact();
        WebArchive archive =
            create(WebArchive.class)
                .addClasses(
                    JaccInstaller.class,
                    LoggingTestPolicy.class,
                    ProtectedServlet.class,
                    TestAuthenticationMechanism.class,
                    TestIdentityStore.class
                ).addAsLibraries(jaccLibrary.asFile())
                ;

        System.out.println("************************************************************");
        System.out.println(archive.toString(true));
        System.out.println("************************************************************");

        return archive;
    }

    @Test
    @RunAsClient
    public void testAuthenticated() throws IOException {

        String response =
                newClient()
                     .target(
                         URI.create(new URL(base, "protected/servlet").toExternalForm()))
                     .queryParam("name", "test")
                     .queryParam("password", "secret")
                     .request(TEXT_PLAIN)
                     .get(String.class);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Response: \n\n" + response);
        System.out.println("-------------------------------------------------------------------------");

        assertTrue(
            response.contains("web user has role \"a\": true")
        );

        // If these permissions are logged, our custom JACC provider has been invoked when the container was
        // checking for access to our protected servlet.

        assertTrue(
            response.contains("\"jakarta.security.jacc.WebResourcePermission\" \"/protected/servlet\" \"GET\"")
        );

        assertTrue(
            response.contains("\"jakarta.security.jacc.WebRoleRefPermission\" \"fish.payara.samples.jaccperapp.ProtectedServlet\" \"a\"")
        );

    }

    @Test
    @RunAsClient
    public void testNotAuthenticated() throws IOException {

        String response =
                newClient()
                     .target(
                         URI.create(new URL(base, "protected/servlet").toExternalForm()))
                     .request(TEXT_PLAIN)
                     .get(String.class);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Response: \n\n" + response);
        System.out.println("-------------------------------------------------------------------------");

        assertFalse(response.contains("web user has role \"a\": true"));
    }

}
