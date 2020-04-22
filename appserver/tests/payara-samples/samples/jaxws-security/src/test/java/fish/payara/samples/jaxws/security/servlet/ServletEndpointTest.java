/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.jaxws.security.servlet;

import fish.payara.samples.*;
import fish.payara.samples.jaxws.security.JAXWSEndpointTest;
import java.io.*;

import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;


/**
 * Test case for CUSTCOM-247 - Custom realm defined in web.xml isn't used for
 * SOAP services secured using WS security policy
 */
@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.202")
public class ServletEndpointTest extends JAXWSEndpointTest {

    @Deployment
    public static WebArchive createDeployment() {
        return createBaseDeployment()
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addPackage(ServletEndpointTest.class.getPackage())
                .addAsWebInfResource(new File(WEBAPP_SRC_ROOT, "WEB-INF/wsit-fish.payara.samples.jaxws.security.servlet.CalculatorService.xml"));
    }

    @Before
    public void setUp() throws MalformedURLException, KeyManagementException, NoSuchAlgorithmException {
        URL baseHttpsUrl = ServerOperations.toContainerHttps(baseUrl);
        serviceUrl = new URL(baseHttpsUrl, "CalculatorService");
        insecureSSLConfigurator.enableInsecureSSL();
    }

    @After
    public void cleanUp() {
        insecureSSLConfigurator.revertSSLConfiguration();
    }

    @Test
    @RunAsClient
    public void testUnrestrictedSoapRequest() throws IOException, URISyntaxException {
        HttpsURLConnection serviceConnection = sendSoapHttpRequest("request.xml");
        assertResponseOK(serviceConnection);
    }

    @Test
    @RunAsClient
    public void testPermittedSoapRequest() throws IOException, URISyntaxException {
        HttpsURLConnection serviceConnection = sendSoapHttpRequest("request-restricted.xml");
        assertResponseOK(serviceConnection);
    }

    @Test
    @RunAsClient
    public void testSoapRequestWithIncorrectCredentials() throws IOException, URISyntaxException {
        HttpsURLConnection serviceConnection = sendSoapHttpRequest("request-with-bad-password.xml");
        assertResponseFailedWithMessage(serviceConnection, "Authentication of Username Password Token Failed");
    }

    @Test
    @RunAsClient
    public void testSoapRequestUserNotAllowedExecution() throws IOException, URISyntaxException {
        HttpsURLConnection serviceConnection = sendSoapHttpRequest("request-not-allowed.xml");
        assertResponseFailedWithMessage(serviceConnection, "Caller was not permitted access");
    }

}
