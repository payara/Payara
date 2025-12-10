/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.resource.fish6479;

import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * bug reproducer for FISH-6479 and FISH-5981
 */
@RunWith(Arquillian.class)
public class Slf4jTest {

    @Deployment
    public static WebArchive createDeployment() {
        var war = ShrinkWrap.create(WebArchive.class, "slf4jtest.war")
                .addClasses(Slf4jResource.class, JaxrsApp.class)
                .addAsResource("simplelogger.properties")
                .addAsLibraries(
                        Maven.resolver()
                                .resolve("org.slf4j:slf4j-api:1.7.36",
                                        "org.slf4j:slf4j-simple:1.7.36")
                                .withTransitivity()
                                .asFile()
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @ArquillianResource
    URI base;

    @Test
    public void testWarnLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("warn").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "WARN");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Warn message ***"))
                .findAny()
                .isPresent());
    }

    @Test
    public void testInfoLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("info").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "INFO");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Info message ***"))
                .findAny()
                .isPresent());
    }

    @Test
    public void testFatalLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("fatal").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "ERROR");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Fatal message ***"))
                .findAny()
                .isPresent());
    }

    @Test
    public void testDebugLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("debug").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "DEBUG");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Debug message ***"))
                .findAny()
                .isPresent());
    }

    private List<String> findLogMessages(String logFileName, String loggerName, String level) throws Exception {
        List<String> logMessages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(logFileName));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(loggerName) && line.contains(level)) {
                logMessages.add(line);
            }
        }
        reader.close();
        return logMessages;
    }
}
