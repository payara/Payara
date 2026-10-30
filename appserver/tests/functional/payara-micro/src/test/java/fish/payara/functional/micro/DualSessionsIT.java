/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.functional.micro;

import com.microsoft.playwright.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import org.jboss.arquillian.container.test.api.Deployment;

import fish.payara.samples.PayaraArquillianTestRunner;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PayaraArquillianTestRunner.class)
public class DualSessionsIT {

    public static WebArchive createClusterJsp() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "clusterjsp.war")
                .addAsWebInfResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/WEB-INF/payara-web.xml"))
                .addAsWebInfResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/WEB-INF/web.xml"))
                .addAsWebResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/HaJsp.jsp"))
                .addAsWebResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/ClearSession.jsp"))
                .addAsManifestResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/MANIFEST.MF"));
        return archive;
    }

    // Deployment with testable = false as otherwise, Arquillian would add its own libraries to the war
    // including the library arquillian-jakarta-servlet-protocol.jar, preventing clusterjsp to work as intended
    @Deployment(name = "microInstance1", order = 1, testable = false)
    @TargetsContainer("micro1")
    public static WebArchive microInstance1() {
        return createClusterJsp();
    }

    @ArquillianResource
    @OperateOnDeployment("microInstance1")
    private URL deployment1URL;

    @Deployment(name = "microInstance2", order = 2, testable = false)
    @TargetsContainer("micro2")
    public static WebArchive microInstance2() {
        return createClusterJsp();
    }

    @ArquillianResource
    @OperateOnDeployment("microInstance2")
    private URL deployment2URL;


    @Test
    @RunAsClient
    public void addAndReadAttributes() throws MalformedURLException, InterruptedException {
        try (Page page = ClusterHAJSPPage.openNewPage(deployment1URL.toString())) {
            ClusterHAJSPPage.enterNameAttribute(page, "attribute1");
            ClusterHAJSPPage.enterValueAttribute(page, "value1");
            ClusterHAJSPPage.addSessionData(page);
            String results = ClusterHAJSPPage.readDataHttpSession(page);
            assertThat(results, containsString("attribute1 = value1"));
            System.out.println("attribute1 found in instance1");

            ClusterHAJSPPage.openNewUrl(page, deployment2URL.toString());
            ClusterHAJSPPage.enterNameAttribute(page, "attribute2");
            ClusterHAJSPPage.enterValueAttribute(page, "value2");
            ClusterHAJSPPage.addSessionData(page);
            String results2 = ClusterHAJSPPage.readDataHttpSession(page);
            assertThat(results2, containsString("attribute1 = value1"));
            System.out.println("attribute1 found in instance2");

            ClusterHAJSPPage.openNewUrl(page, deployment1URL.toString());
            ClusterHAJSPPage.reloadPage(page);
            String results3 = ClusterHAJSPPage.readDataHttpSession(page);
            assertThat(results3, containsString("attribute1 = value1"));
            System.out.println("attribute1 found in instance1");
            assertThat(results3, containsString("attribute2 = value2"));
            System.out.println("attribute2 found in instance1");
        }
    }

}
