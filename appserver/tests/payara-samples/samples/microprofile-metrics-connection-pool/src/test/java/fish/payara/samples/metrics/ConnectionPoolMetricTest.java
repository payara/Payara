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
package fish.payara.samples.metrics;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.samples.CliCommands;
import fish.payara.samples.ServerOperations;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertTrue;


@RunWith(Arquillian.class)
@RunAsClient
public class ConnectionPoolMetricTest {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource("web.xml")
                .addClasses(TestServlet.class);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Health Check and Connection Pool must be enabled for connection pool metrics
        CliCommands.payaraGlassFish("healthcheck-configure", "--enabled=true", "--target=server-config");
        CliCommands.payaraGlassFish("set-healthcheck-service-configuration", "--serviceName=connection-pool", "--enabled=true", "--unit=SECONDS", "--time=1", "--target=server-config");
        CliCommands.payaraGlassFish("restart-domain", ServerOperations.getDomainName());
        Thread.sleep(2000);
    }

    @Test
    @InSequence(1)
    public void testConnectionPoolMetric() throws Exception {
        try (WebClient client = new WebClient()) {
            TextPage page = client.getPage(url + "../metrics?scope=vendor");
            System.out.println(page.getContent());
            assertTrue("Expected \"H2Pool_freeConnection_total\" to be 0.0", page.getContent().contains("connection_pool_H2Pool_freeConnection_total{mp_scope=\"vendor\"} 0.0"));
            assertTrue("Expected \"H2Pool_totalConnection\" to be 0.0", page.getContent().contains("connection_pool_H2Pool_totalConnection{mp_scope=\"vendor\"} 0.0"));
            assertTrue("Expected \"H2Pool_usedConnection_total\" to be 0.0", page.getContent().contains("connection_pool_H2Pool_usedConnection_total{mp_scope=\"vendor\"} 0.0"));
        }
    }

    @Test
    @InSequence(2)
    public void testConnectionPoolMetricAfter() throws Exception {
        try (WebClient client = new WebClient()) {
            TextPage page = client.getPage(url);
            System.out.println(page.getContent());
            Thread.sleep(1000);
            page = client.getPage(url + "../metrics?scope=vendor");
            System.out.println(page.getContent());
            assertTrue("Expected \"H2Pool_freeConnection_total\" to be 7.0", page.getContent().contains("connection_pool_H2Pool_freeConnection_total{mp_scope=\"vendor\"} 7.0"));
            assertTrue("Expected \"H2Pool_totalConnection\" to be 8.0", page.getContent().contains("connection_pool_H2Pool_totalConnection{mp_scope=\"vendor\"} 8.0"));
            assertTrue("Expected \"H2Pool_usedConnection_total\" to be 1.0", page.getContent().contains("connection_pool_H2Pool_usedConnection_total{mp_scope=\"vendor\"} 1.0"));
        }
    }
}
