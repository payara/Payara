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
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.samples.jaxws.security.ejb;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.SincePayara;
import fish.payara.samples.Unstable;
import fish.payara.samples.jaxws.security.JAXWSEndpointTest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible("JAX-WS is not supported on Micro")
@SincePayara("5.202")
@Category(Unstable.class)
// fails from two reasons:
// 1) bug in Payara - reproducer, needs investigation
// 2) grizzly SSL is incompatible with JDK8u242 and later
public class EJBEndpointTest extends JAXWSEndpointTest {

    private HttpsURLConnection serviceConnection;

    @Deployment
    public static WebArchive createDeployment() {
        return createBaseDeployment()
                .addPackage(EJBEndpointTest.class.getPackage())
                .addAsWebInfResource(new File(WEBAPP_SRC_ROOT, "WEB-INF/wsit-fish.payara.samples.jaxws.security.ejb.CalculatorService.xml"));

    }

    @Before
    public void setUp() throws MalformedURLException, KeyManagementException, NoSuchAlgorithmException {
        URL baseHttpsUrl = ServerOperations.toContainerHttps(baseUrl);
        URL rootUrl = new URL(baseHttpsUrl.getProtocol(), baseHttpsUrl.getHost(), baseHttpsUrl.getPort(), "");
        serviceUrl = new URL(rootUrl, "CalculatorService/CalculatorService");
        insecureSSLConfigurator.enableInsecureSSL();
    }

    @After
    public void cleanUp() {
        if (serviceConnection != null) {
            serviceConnection.disconnect();
            serviceConnection = null;
        }
        insecureSSLConfigurator.revertSSLConfiguration();
    }

    //@Test
    // TODO - uncomment after solving problem: "MustUnderstand headers:[{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd}Security] are not understood"
    //@RunAsClient
    public void testUnrestrictedSoapRequest() throws IOException, URISyntaxException {
        serviceConnection = sendSoapHttpRequest("request.xml");
        assertResponseOK(serviceConnection);
    }

    //@Test
    // TODO - uncomment after solving problem: "MustUnderstand headers:[{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd}Security] are not understood"
    //@RunAsClient
    public void testPermittedSoapRequest() throws IOException, URISyntaxException {
        serviceConnection = sendSoapHttpRequest("request-restricted.xml");
        assertResponseOK(serviceConnection);
    }

    //@Test
    // TODO - uncomment after solving problem: "MustUnderstand headers:[{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd}Security] are not understood"
    //@RunAsClient
    public void testSoapRequestWithIncorrectCredentials() throws IOException, URISyntaxException {
        serviceConnection = sendSoapHttpRequest("request-with-bad-password.xml");
        assertResponseFailedWithMessage(serviceConnection, "Authentication of Username Password Token Failed");

    }

    // passes on JDK 8u232 and older
    //@Test
    // TODO - uncomment after solving problem: "MustUnderstand headers:[{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd}Security] are not understood"
    //@RunAsClient
    public void testSoapRequestUserNotAllowedExecution() throws IOException, URISyntaxException {
        serviceConnection = sendSoapHttpRequest("request-not-allowed.xml");
        assertResponseFailedWithMessage(serviceConnection, "Client not authorized");
    }

}
