/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples.microprofile.healthcheck;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.NotMicroCompatible;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * This sample tests that an EAR application display all parts
 * of a healthcheck that comes from an ear.
 *
 * @author Jonathan Coustick
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class HealthEndpointTest {

    @ArquillianResource
    private URL base;


    @Deployment(testable = false)
    public static EnterpriseArchive createDeployment() {
        EnterpriseArchive archive =
            create(EnterpriseArchive.class)
                 .addAsModule(create(WebArchive.class).addClasses(NewServlet.class, WarCheck1.class))
                .addAsModule(create(WebArchive.class).addClasses(JAXRSConfiguration.class, JakartaEE8Resource.class, SystemLivenessCheck.class));

        System.out.println("************************************************************");
        System.out.println(archive.toString(true));
        System.out.println("************************************************************");

        return archive;
    }


    @Test
    @RunAsClient
    public void testHealthcheckResponse() throws IOException {

        String response = newClient().target(URI.create(new URL(base, "mphealth-insecure").toExternalForm())).request(TEXT_PLAIN).get(String.class);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Response: \n\n" + response);
        System.out.println("-------------------------------------------------------------------------");

        JsonObject healthcheck = Json.createReader(new StringReader(response)).readObject();

        Assert.assertEquals("Wrong number of healthchecks", 2, healthcheck.getJsonArray("checks").size());
        Assert.assertEquals("Healthchecks should all be UP", "UP", healthcheck.getString("status"));
    }

}
