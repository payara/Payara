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
package fish.payara.tests.functional.payararesourcesxml;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.URI;

/**
 * Tests that resources defined in payara-resources.xml and its deprecated predecessors are deployed correctly,
 * and that a deployment without any resource descriptor does not make the resource available.
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class PayaraResourcesXmlIT {

    private static final String EXPECTED_VALUE = "resource-works";

    @ArquillianResource
    private URI uri;

    @Deployment(name = "payara7Resources")
    public static WebArchive createPayara7ResourcesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "payara-resources.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "payara7-resources.xml"), "payara-resources.xml")
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Deployment(name = "payara6Resources")
    public static WebArchive createPayara6ResourcesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "payara6-resources.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "payara6-resources.xml"), "payara-resources.xml")
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Deployment(name = "payara5Resources")
    public static WebArchive createPayara5ResourcesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "payara5-resources.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "payara5-resources.xml"), "payara-resources.xml")
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Deployment(name = "oldPayaraResources")
    public static WebArchive createOldPayaraResourcesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "old-payara-resources.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "payara-resources_1_8.xml"), "payara-resources.xml")
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Deployment(name = "glassFishResources")
    public static WebArchive createGlassFishResourcesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "glassfish-resources.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "glassfish-resources.xml"))
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Deployment(name = "payaraOverGlassFish")
    public static WebArchive createPayara7OverGlassFishDeployment() {
        return ShrinkWrap.create(WebArchive.class, "payara-over-glassfish.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "payara-resources-both.xml"), "payara-resources.xml")
                .addAsWebInfResource(new File("src/test/WEB-INF", "glassfish-resources-both.xml"), "glassfish-resources.xml")
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Deployment(name = "noResources")
    public static WebArchive createNoResourcesDeployment() {
        return ShrinkWrap.create(WebArchive.class, "no-resources.war")
                .addClasses(RestApplication.class, ResourceLookupEndpoint.class)
                .addAsWebInfResource(new File("src/test/WEB-INF", "beans.xml"));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("payara7Resources")
    public void testPayara7ResourcesXml() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(EXPECTED_VALUE, response.readEntity(String.class));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("payara6Resources")
    public void testPayara6ResourcesXml() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(EXPECTED_VALUE, response.readEntity(String.class));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("payara5Resources")
    public void testPayara5ResourcesXml() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(EXPECTED_VALUE, response.readEntity(String.class));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("oldPayaraResources")
    public void testOldPayaraResourcesXml() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(EXPECTED_VALUE, response.readEntity(String.class));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("glassFishResources")
    public void testGlassFishResourcesXml() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(EXPECTED_VALUE, response.readEntity(String.class));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("payaraOverGlassFish")
    public void testPayara7TakesPrecedenceOverGlassFish() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("payara-resource-wins", response.readEntity(String.class));
    }

    @Test
    @RunAsClient
    @OperateOnDeployment("noResources")
    public void testNoResourcesXml() {
        Response response = ClientBuilder.newClient().target(uri).path("resources").path("resource").request().get();
        Assert.assertNotEquals(200, response.getStatus());
        Assert.assertNotEquals(EXPECTED_VALUE, response.readEntity(String.class));
    }
}
