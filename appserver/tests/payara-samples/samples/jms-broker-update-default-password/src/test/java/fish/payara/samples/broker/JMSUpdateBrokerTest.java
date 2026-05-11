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

package fish.payara.samples.broker;

import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.arquillian.test.api.ArquillianResource;


@RunWith(PayaraArquillianTestRunner.class)
public class JMSUpdateBrokerTest {

    @ArquillianResource
    private URL baseUrl;

    private static final String logFilePath = System.getProperty("logFilePath");

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webapp = ShrinkWrap.create(WebArchive.class, "brokerInstanceUpdate.war")
                .addClasses(JAXRSConfiguration.class, JakartaEEResource.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        
        System.out.println(webapp.toString(true));
        return webapp;
    }

    @Test
    public void testUpdateBrokerPassword() throws IOException {
        try {
            Assert.assertTrue(checkMessage("The default broker instance for OpenMQ is using the default admin password".trim()));
        } catch (IOException e) {

        }
    }

    public boolean checkMessage(String logMessage) throws IOException {
        try {
            try {
                Thread.sleep(5000);
                BufferedReader reader = new BufferedReader(new FileReader(logFilePath));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().contains(logMessage)) {
                        return true;
                    }
                }
                return false;
            } catch (IOException e) {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
    }
}
