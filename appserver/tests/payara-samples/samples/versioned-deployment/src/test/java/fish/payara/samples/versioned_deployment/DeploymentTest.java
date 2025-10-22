/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.versioned_deployment;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class DeploymentTest {

    private static final String DEPLOYMENT_V1_NAME = "versioned-deployment:1.0.0";
    private static final String DEPLOYMENT_V2_NAME = "versioned-deployment:1.0.1";
    private static final String URI = "http://localhost:8080/versioned-deployment";
    private static final WebTarget target = ClientBuilder.newClient().target(URI);

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_V1_NAME, managed = false)
    public static WebArchive createV1Deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_V1_NAME + ".war")
                .addAsWebInfResource("payara-web.xml")
                .addClasses(TestServlet.class);
    }

    @Deployment(name = DEPLOYMENT_V2_NAME, managed = false)
    public static WebArchive createV2Deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_V2_NAME +".war")
                .addAsWebInfResource("payara-web.xml")
                .addClasses(TestServletV2.class);
    }


    @Test
    @InSequence(1)
    public void verifyVersion1Deployment() {
        deployer.deploy(DEPLOYMENT_V1_NAME);
        Assert.assertEquals("Hello from version 1.0.0", validateAndGetMessage());
    }

    @Test
    @InSequence(2)
    public void verifyNewDeployment() {
        //Verify application is still deployed
        Assert.assertEquals("Hello from version 1.0.0", validateAndGetMessage());
        deployer.deploy(DEPLOYMENT_V2_NAME);
        Assert.assertEquals("Hello from version 1.0.1", validateAndGetMessage());
    }

    @Test
    @InSequence(3)
    public void verifyOldUnDeployment() {
        deployer.undeploy(DEPLOYMENT_V1_NAME);
        Assert.assertEquals("Hello from version 1.0.1", validateAndGetMessage());
        deployer.undeploy(DEPLOYMENT_V2_NAME);
    }

    private String validateAndGetMessage() {
        Response response = target.request().get();

        Assert.assertEquals(200, response.getStatus());

        return response.readEntity(String.class).strip();

    }
}
