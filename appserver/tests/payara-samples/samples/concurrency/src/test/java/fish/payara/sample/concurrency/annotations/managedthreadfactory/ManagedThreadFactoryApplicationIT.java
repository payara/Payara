/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.sample.concurrency.annotations.managedthreadfactory;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ArquillianExtension.class)
public class ManagedThreadFactoryApplicationIT {
    private final static Logger logger = Logger.getLogger(ManagedThreadFactoryApplicationIT.class.getName());

    @ArquillianResource
    private URL base;

    private Client client;

    @Deployment
    public static Archive<?> createDeployment() {
        //Creating Jar ejb module
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb-jar.jar")
                .addClasses(ManagedThreadFactoryEJB.class, ManagedThreadFactoryEJBFromConfig.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("ejb-jar2.xml", "META-INF/ejb-jar.xml");
        System.out.println(ejbJar.toString(true));
        //Creating web module
        WebArchive webWar = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(ManagedThreadFactoryApplication.class, ManagedThreadFactoryEJBRest.class);
        System.out.println(webWar.toString(true));
        //Creating EAR
        EnterpriseArchive ear = ShrinkWrap
                .create(EnterpriseArchive.class, "test-app.ear").setApplicationXML("application.xml")
                .addAsModule(ejbJar).addAsModule(webWar);
        System.out.println(ear.toString(true));
        return ear;
    }

    @BeforeEach
    public void setup() {
        logger.info("setting client");
        this.client = ClientBuilder.newClient();
    }

    @AfterEach
    public void clean() {
        logger.info("close client");
        if (this.client != null) {
            this.client.close();
        }
    }

    @Test
    @DisplayName("testing ManagedThreadFactory tag from Application EAR")
    @RunAsClient
    public void testManagedThreadFactoryFromApplicationConfig() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to submit xml config ManagedThreadFactory {0}", new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "xml/application").toExternalForm());
        String message = target.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertTrue(message.contains("Counting numbers total"));
        assertTrue(message.contains("125000250000"));
    }

    @Test
    @DisplayName("testing ManagedThreadFactory tag from EJB config")
    @RunAsClient
    public void testManagedThreadFactoryFromEJBConfig() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to submit xml config ManagedThreadFactory {0}", new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "xml/ejbconfig").toExternalForm());
        String message = target.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertTrue(message.contains("Counting numbers total"));
        assertTrue(message.contains("125000250000"));
    }

}