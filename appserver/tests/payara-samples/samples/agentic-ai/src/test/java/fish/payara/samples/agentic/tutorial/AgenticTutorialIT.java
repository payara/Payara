/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.agentic.tutorial;

import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URL;

/**
 * Verifies the tutorial agent end to end without a live LLM, using
 * {@link StubLargeLanguageModel}: the form is exposed, a tutorial is generated,
 * and a chat refinement produces a different result.
 */
@RunWith(PayaraArquillianTestRunner.class)
public class AgenticTutorialIT {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "agentic-ai.war")
                .addClass(RestApplication.class)
                .addClass(TutorialResource.class)
                .addClass(TutorialAgent.class)
                .addClass(TutorialRequest.class)
                .addClass(TutorialStore.class)
                .addClass(FormSpec.class)
                .addClass(FieldSpec.class)
                .addClass(CustomerFormSpec.class)
                .addClass(StubLargeLanguageModel.class)
                .addAsWebInfResource(
                        new StringAsset("<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                                + "version=\"4.0\" bean-discovery-mode=\"all\"/>"),
                        "beans.xml");
    }

    @Test
    public void exposesTheForm() {
        Response response = ClientBuilder.newClient().target(url + "api/form").request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertTrue(response.readEntity(String.class).contains("businessEmail"));
    }

    @Test
    public void refinesField() {
        ClientBuilder.newClient()
                .target(url + "api/tutorial/generate")
                .request(MediaType.TEXT_HTML)
                .post(Entity.json(""));

        Response response = ClientBuilder.newClient()
                .target(url + "api/tutorial/refine-field")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json("{\"fieldName\":\"businessEmail\",\"instruction\":\"make it friendlier\"}"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void generatesThenRefines() {
        String generated = ClientBuilder.newClient()
                .target(url + "api/tutorial/generate")
                .request(MediaType.TEXT_HTML)
                .post(Entity.json("")).readEntity(String.class);
        Assert.assertTrue("Expected generated marker in: " + generated,
                generated.contains("data-tutorial=\"generated\""));

        String refined = ClientBuilder.newClient()
                .target(url + "api/tutorial/refine")
                .request(MediaType.TEXT_HTML)
                .post(Entity.json("{\"instruction\":\"make the email field friendlier\"}"))
                .readEntity(String.class);
        Assert.assertTrue("Expected refined marker in: " + refined,
                refined.contains("data-tutorial=\"refined\""));
    }
}
