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
package fish.payara.samples.agentic.quickstart;

import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.URL;

/**
 * Verifies the agent workflow runs end to end without a live LLM: the
 * application supplies {@link StubLargeLanguageModel}, so the runtime drives the
 * {@code @Trigger}/{@code @Decision}/{@code @Action}/{@code @Outcome} phases and
 * returns the stub's deterministic answer.
 */
@RunWith(PayaraArquillianTestRunner.class)
public class AgenticQuickstartIT {

    @ArquillianResource
    private URL url;

    /**
     * Deploys the module's own built WAR (with {@link StubLargeLanguageModel}
     * merged in) rather than reassembling it from {@code addClass(...)}.
     * <p>
     * This matters on the embedded container, which boots the server in the test
     * JVM: the {@code @Agent}'s phase methods are package-private, and Weld can
     * only override them in its client proxy when {@code QuestionAgent} is loaded
     * by the same classloader as that proxy — the webapp classloader. But the
     * module also compiles {@code QuestionAgent} into {@code target/classes},
     * which is on the in-JVM system classpath, and GlassFish web classloaders
     * delegate to the parent first: Weld would load the agent from the system
     * classpath, its proxy could not override the package-private {@code @Action},
     * and the injected {@code LargeLanguageModel} would be null at runtime.
     * <p>
     * A {@code glassfish-web.xml} with {@code delegate="false"} makes the webapp
     * classloader resolve application classes child-first, so the agent is loaded
     * from {@code WEB-INF/classes} alongside its Weld proxy. On the out-of-process
     * containers the agent isn't on the server classpath at all, so this is a
     * harmless no-op there. The stub is added directly because its methods are
     * public, so it is safe on the classpath.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(ZipImporter.class, "agentic-ai-quickstart.war")
                .importFrom(new File("target", "agentic-ai-quickstart.war"))
                .as(WebArchive.class)
                .addClass(StubLargeLanguageModel.class)
                .addAsWebInfResource(
                        new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<glassfish-web-app>\n"
                                + "    <class-loader delegate=\"false\"/>\n"
                                + "</glassfish-web-app>\n"),
                        "glassfish-web.xml");
    }

    @Test
    public void answersWithTheStubbedModel() {
        Response response = ClientBuilder.newClient()
                .target(url + "api/ask")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json("{\"question\":\"What is Jakarta EE?\"}"));

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = response.readEntity(String.class);
        Assert.assertTrue("Expected the stubbed answer in: " + body,
                body.contains(StubLargeLanguageModel.CANNED));
    }

    @Test
    public void terminatesOnBlankQuestion() {
        Response response = ClientBuilder.newClient()
                .target(url + "api/ask")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json("{\"question\":\"\"}"));

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = response.readEntity(String.class);
        // @Decision returned Result(false, ...): @Action never ran, so no stubbed answer.
        Assert.assertFalse("Workflow should have terminated before @Action: " + body,
                body.contains(StubLargeLanguageModel.CANNED));
    }
}
