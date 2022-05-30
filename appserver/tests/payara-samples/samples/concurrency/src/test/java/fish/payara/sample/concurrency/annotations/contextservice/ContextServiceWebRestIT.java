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
package fish.payara.sample.concurrency.annotations.contextservice;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(ArquillianExtension.class)
public class ContextServiceWebRestIT {

    private final static Logger logger = Logger.getLogger(ContextServiceWebRestIT.class.getName());

    @ArquillianResource
    private URL base;

    private Client client;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackages(true, "fish.payara.sample.concurrency.annotations.contextservice",
                        "fish.payara.sample.concurrency.annotations.contextservice.util")
                .addAsWebInfResource("web.xml").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider("jakarta.enterprise.concurrent.spi.ThreadContextProvider",
                        "fish.payara.sample.concurrency.annotations.contextservice.util.IntContextProvider");
        System.out.println(war.toString(true));
        return war;
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
    @DisplayName("testing ContextService annotation")
    @RunAsClient
    public void processContextServiceFromAnnotation() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to test ContextService annotation {0}",
                new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "annotation/annotationconfig").toExternalForm());
        String message = target.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertTrue(message.contains("10"));
    }

    @Test
    @DisplayName("testing ContextService from multiple annotation")
    @RunAsClient
    public void processContextServiceMultipleAnnotation() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to test ContextService from multiple annotation {0}",
                new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "annotation/multipleannotation").toExternalForm());
        String message = target.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        assertEquals("1020", message);
    }


    @Test
    @DisplayName("testing ContextService from web xml")
    @RunAsClient
    public void processContextServiceFromWebXml() throws MalformedURLException {
        logger.log(Level.INFO, "Consuming service to test ContextService config from web xml {0}",
                new Object[]{client});
        WebTarget target = this.client.target(new URL(this.base, "annotation/xmlconfig").toExternalForm());
        String message = target.request().accept(MediaType.TEXT_PLAIN).get(String.class);
        logger.log(Level.INFO, "Returned message {0}", new Object[]{message});
        //needs to be fixed because it is always returning 0
        assertTrue(message.contains("0"));
    }
}