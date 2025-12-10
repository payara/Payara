/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2022] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.microprofile.config.expression;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class PayaraExpressionConfigPropertiesIT {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "microprofile-config-expression.war")
            .addPackage(ConfigServlet.class.getPackage()) //
            .addAsManifestResource( //
                ConfigServlet.class.getResource("/META-INF/payara-expression-config.properties"),
                "payara-expression-config.properties");
    }

    @BeforeClass
    public static void createPasswordAlias() {
        CliCommands.payaraGlassFish("create-password-alias", "-W",
            PayaraExpressionConfigPropertiesIT.class.getResource("/passwordfile.txt").getFile(), "wibbles");

        // Deployment actually happens before @BeforeClass, and the PasswordAlias config source requires an application
        // refresh to update, so disable and enable it
        CliCommands.payaraGlassFish("set", "servers.server.server.application-ref.microprofile-config-expression.enabled=false");
        CliCommands.payaraGlassFish("set", "servers.server.server.application-ref.microprofile-config-expression.enabled=true");
    }

    @AfterClass
    public static void deletePasswordAlias() {
        CliCommands.payaraGlassFish("delete-password-alias", "wibbles");
    }

    @Test
    public void testAliasSubstitution() throws Exception {
        try (WebClient client = new WebClient()) {
            TextPage page = client.getPage(url + "ConfigServlet");
            System.out.println(page.getContent());

            assertTrue("Expected \"Normal Notation\" to give wobbles",
                page.getContent().contains("Normal Notation: wobbles"));
            assertTrue("Expected \"Substitution Notation\" to give wobbles",
                page.getContent().contains("Substitution Notation: wobbles"));
            assertTrue("Expected \"Password Alias from File\" to give wobbles",
                page.getContent().contains("Password Alias from File: wobbles"));
            assertTrue("Expected \"System Property Alias from File\" to give Tiddles!",
                page.getContent().contains("System Property Alias from File: Tiddles!"));
            assertTrue(
                "Expected \"Environment Variable Alias referencing System Property Alias from File\" to give Dobbles",
                page.getContent()
                    .contains("Environment Variable Alias referencing System Property Alias from File: Dobbles"));
            assertTrue(
                "Expected \"Environment Variable Alias and System Property Alias from File (same property)\" to give Bibbles and Bobbles",
                page.getContent().contains(
                    "Environment Variable Alias and System Property Alias from File (same property): Bibbles and Bobbles"));
        }
    }
}
