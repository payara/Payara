/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.jaxws.endpoint.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SincePayara;
import fish.payara.samples.jaxws.endpoint.JAXWSEndpointTest;

/**
 * @author Arjan Tijms
 */
@RunWith(PayaraArquillianTestRunner.class)
@FixMethodOrder(NAME_ASCENDING)
@SincePayara("5.193")
public class EJBEndpointTest extends JAXWSEndpointTest {

    @Deployment
    public static WebArchive createDeployment() {
        return createBaseDeployment()
                .addPackage(EJBEndpointTest.class.getPackage());
    }

    private Service jaxwsEndPointService;

    @Before
    public void setupClass() throws MalformedURLException {
        URL rootUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "");
        
        jaxwsEndPointService = Service.create(
            // The WSDL file used to create this service is fetched from the application we deployed
            // above using the createDeployment() method.
                
            new URL(rootUrl, "JAXWSEndPointImplementationService/JAXWSEndPointImplementation?wsdl"),
            new QName("http://ejb.endpoint.jaxws.samples.payara.fish/", "JAXWSEndPointImplementationService"));
    }

    @Test
    @RunAsClient
    public void test1RequestFromClient() throws MalformedURLException {
        assertEquals("Hi Payara!", jaxwsEndPointService
                .getPort(JAXWSEndPointInterface.class)
                .sayHi("Payara!"));
    }
    
    @Test
    // Runs on Server
    public void test2ServerCheck() throws MalformedURLException {
        assertTrue(isTraceMonitorTriggered());
    }
   
}
