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
package fish.payara.samples.datagridencryption.sfsb;

import fish.payara.samples.ServerOperations;

import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@RunWith(Arquillian.class)
public class SfsbEncryptionTest {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "sfsb-passivation.war")
                .addPackage("fish.payara.samples.datagridencryption.sfsb")
                .addClass(ServerOperations.class);
    }

    @Test
    public void testStateRestoredAfterPassivation() {
        Client client = ClientBuilder.newClient();
        WebTarget endpoint1 = client.target(url + "TestEjb");
        WebTarget endpoint2 = client.target(url + "TestEjb/2");
        WebTarget endpoint3 = client.target(url + "TestEjb/Lookup");

        // First, poke endpoint1 twice to store some state
        Response response = endpoint1.request().get();
        Assert.assertEquals("apple,pear", response.readEntity(String.class));

        response = endpoint1.request().get();
        Assert.assertEquals("apple,pear,apple,pear", response.readEntity(String.class));

        // Next, poke endpoint2 three times to store some state
        endpoint2.request().get();
        endpoint2.request().get();
        response = endpoint2.request().get();
        Assert.assertEquals("bapple,care,bapple,care,bapple,care", response.readEntity(String.class));

        // Now force passivation by spamming lookup of 1200 EJBs
        endpoint3.request().get();
        endpoint3.request().get();

        // Check endpoint1  and endpoint2 have restored their state and added another set upon invocation
        response = endpoint1.request().get();
        Assert.assertEquals("apple,pear,apple,pear,apple,pear", response.readEntity(String.class));

        response = endpoint2.request().get();
        Assert.assertEquals("bapple,care,bapple,care,bapple,care,bapple,care", response.readEntity(String.class));
    }
}
