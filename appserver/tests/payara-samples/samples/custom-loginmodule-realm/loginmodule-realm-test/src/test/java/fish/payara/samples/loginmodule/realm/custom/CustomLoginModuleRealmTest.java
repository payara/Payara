/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2022 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.loginmodule.realm.custom;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.CredentialsProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@NotMicroCompatible
@RunWith(PayaraArquillianTestRunner.class)
public class CustomLoginModuleRealmTest {
    private static final String WEBAPP_SOURCE = "src/main/webapp";

    @ArquillianResource
    private URL base;

    @Deployment
    public static WebArchive createDeployment() throws MalformedURLException {
        File implJar = new File( "../loginmodule-realm-impl/target/loginmodule-realm-impl.jar");
        assertTrue(implJar.exists());

        return PayaraTestShrinkWrap.getWebArchive()
                .addClasses(TestServlet.class)
                .addPackages(true, "org.apache.http.client")
                .addAsResource(implJar)
                .addAsWebInfResource(new File(WEBAPP_SOURCE, "WEB-INF/web.xml"));
    }

    @Test
    @InSequence(1)
    public void serverSetup() throws IOException {
        Path serverPathToRealm = Paths.get("../tests/loginmodule-realm-impl.jar");
        serverPathToRealm.getParent().toFile().mkdir();
        try (InputStream strm = getClass().getClassLoader().getResourceAsStream("loginmodule-realm-impl.jar")) {
            Files.copy(strm, serverPathToRealm, StandardCopyOption.REPLACE_EXISTING);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("delete-auth-realm");
        cmd.add("custom");
        CliCommands.payaraGlassFish(cmd);

        cmd.clear();
        cmd.add("create-auth-realm");
        cmd.add("--login-module");
        cmd.add("fish.payara.samples.loginmodule.realm.custom.CustomLoginModule");
        cmd.add("--classname");
        cmd.add("fish.payara.samples.loginmodule.realm.custom.CustomRealm");
        cmd.add("--property");
        cmd.add("jaas-context=customRealm:realmJarPath='" + serverPathToRealm.toAbsolutePath().normalize() + "'");
        cmd.add("custom");
        CliCommands.payaraGlassFish(cmd);
    }

    @InSequence(2)
    @RunAsClient
    public void testAuthenticationWithCorrectUser() throws IOException {
        try (WebClient webClient = new WebClient()) {
            System.out.println("\n\nRequesting: " + base + "testServlet");

            CredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
            ((DefaultCredentialsProvider) credentialsProvider).addCredentials("realmUser", "realmPassword");

            webClient.setCredentialsProvider(credentialsProvider);
            TextPage page = webClient.getPage(base + "testServlet");

            System.out.println(page.getContent());

            assertTrue("my GET", page.getContent().contains("This is a test servlet"));

            webClient.getCookieManager().clearCookies();
        }
    }
}
