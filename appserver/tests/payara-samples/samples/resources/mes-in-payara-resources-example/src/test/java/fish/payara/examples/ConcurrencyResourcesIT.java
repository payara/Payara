/*
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.examples;

import fish.payara.samples.Libraries;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test REST using ManamgedExecutorService configured in payara-resources xml.
 *
 * @author Petr Aubrecht <aubrecht@asoftware.cz>
 */
@ExtendWith(ArquillianExtension.class)
public class ConcurrencyResourcesIT {

    private final static Logger logger = Logger.getLogger(ConcurrencyResourcesIT.class.getName());

    @ArquillianResource
    private URL base;

    private Client client;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "mes-in-payara-resources-example.war")
                .addPackage(PersonResource.class.getPackage())
                .addAsWebResource(new File("src/main/webapp/index.xhtml"))
                .addAsResource(new File("src/main/resources/META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/payara-resources.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/web.xml"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.hibernate:hibernate-entitymanager"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.hibernate:hibernate-core:5.6.9.Final"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.hibernate.common:hibernate-commons-annotations:5.1.2.Final"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("com.h2database:h2"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("net.bytebuddy:byte-buddy:1.12.9"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.jboss.spec.javax.transaction:jboss-transaction-api_1.2_spec:1.1.1.Final"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.jboss:jandex:2.4.2.Final"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.jboss.logging:jboss-logging:3.4.2.Final"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("antlr:antlr:2.7.7"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("com.fasterxml:classmate:1.5.1"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("javax.activation:javax.activation-api:1.2.0"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.glassfish.jaxb:jaxb-runtime:2.3.1"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.glassfish.jaxb:txw2:2.3.1"))
                .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("com.sun.xml.fastinfoset:FastInfoset:1.2.15"));
        System.out.println(war.toString(true));
        return war;
    }

    @BeforeEach
    public void setup() throws MalformedURLException {
        logger.info("setting client");
        this.client = ClientBuilder.newClient();
        List createdPeople = client
                .target(new URL(this.base, "resources/person/create-demo").toExternalForm())
                .request().accept(MediaType.APPLICATION_JSON_TYPE).post(null, List.class);
        System.out.println(createdPeople);
    }

    @AfterEach
    public void clean() throws MalformedURLException {
        logger.info("close client");
        if (this.client != null) {
            this.client.close();
        }
    }

    @Test
    @DisplayName("Test ManagedExecutorService loaded from payara-resources.xml")
    @RunAsClient
    public void testManagedExecutorServiceResource() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to submit rest {0}", new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "resources/person").toExternalForm());
        logger.info(target.getUri().toASCIIString());
        String message = target.request().accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertTrue(message.contains("\"lastName\":\"Doe\""));
        assertTrue(message.contains("\"name\":\"Jane\"}"));
        assertTrue(message.contains("\"name\":\"John\""));
    }

    @Test
    @DisplayName("Test ManagedScheduledExecutorService loaded from payara-resources.xml")
    @RunAsClient
    public void testManagedScheduledExecutorServiceResource() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to submit rest {0}", new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "resources/concurrent/mses").toExternalForm());
        logger.info(target.getUri().toASCIIString());
        String message = target.request().accept(MediaType.TEXT_PLAIN_TYPE).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertEquals("MSES PRESENT", message);
    }

    @Test
    @DisplayName("Test ManagedThreadFactory loaded from payara-resources.xml")
    @RunAsClient
    public void testManagedThreadFactoryResource() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to submit rest {0}", new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "resources/concurrent/mtf").toExternalForm());
        logger.info(target.getUri().toASCIIString());
        String message = target.request().accept(MediaType.TEXT_PLAIN_TYPE).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertEquals("MTF, priority 3", message);
    }

}
