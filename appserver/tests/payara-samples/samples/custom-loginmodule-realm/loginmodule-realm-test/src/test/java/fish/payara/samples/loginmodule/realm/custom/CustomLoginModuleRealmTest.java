/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.loginmodule.realm.custom;

import static fish.payara.samples.ServerOperations.addMavenJarsToContainerLibFolder;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;

import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import static fish.payara.samples.ServerOperations.*;

@NotMicroCompatible
@RunWith(PayaraArquillianTestRunner.class)
public class CustomLoginModuleRealmTest {

    private static final String WEBAPP_SOURCE = "src/main/webapp";

    private WebClient webClient;

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {

        addMavenJarsToContainerLibFolder("pom.xml", "fish.payara.samples:loginmodule-realm-impl");
        restartContainer(getPayaraDomainFromServer());

        List<String> cmd = new ArrayList<>();

        cmd.add("delete-auth-realm");
        cmd.add("custom");
        CliCommands.payaraGlassFish(cmd);

        cmd.clear();

        cmd.add("create-auth-realm");

        cmd.add("--login-module");
        cmd.add(CustomLoginModule.class.getName());

        cmd.add("--classname");
        cmd.add(CustomRealm.class.getName());

        cmd.add("--property");
        cmd.add("jaas-context=customRealm");

        cmd.add("custom");

        CliCommands.payaraGlassFish(cmd);

        restartContainer(getPayaraDomainFromServer());

        return ShrinkWrap.create(WebArchive.class)
                .addClass(TestServlet.class)
                .addAsWebInfResource(new File(WEBAPP_SOURCE, "WEB-INF/web.xml"));
    }

    @Before
    public void setUp() throws InterruptedException {
        webClient = new WebClient();
    }

    @Test
    @RunAsClient
    public void testAuthenticationWithCorrectUser() throws FailingHttpStatusCodeException, MalformedURLException, IOException {

        System.out.println("\n\nRequesting: " + (base + "testServlet"));

        DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
        credentialsProvider.addCredentials("realmUser", "realmPassword");

        webClient.setCredentialsProvider(credentialsProvider);
        TextPage page = webClient.getPage(base + "testServlet");

        System.out.println(page.getContent());

        assertTrue("my GET", page.getContent().contains("This is a test servlet"));

        assertTrue("User doesn't have the corrrect role", page.getContent().contains("web user has role \"realmGroup\": true"));
    }

    @After
    public void cleanUp() {
        webClient.getCookieManager().clearCookies();
        webClient.close();
    }
}
