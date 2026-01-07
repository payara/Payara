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
package fish.payara.samples.usebundledjsf.primefaces;

import fish.payara.samples.Libraries;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.URI;

/**
 * Test that checks that a WAR embedded within an EAR can still use its bundled JSF implementation.
 * Bundling the JSF implementation within the EAR and expecting the WAR to be able to access it is out of scope.
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class UseBundledJsfEarTest {

    private final static String JSF_VERSION = "4.0.4";

    @ArquillianResource
    private URI uri;

    @Deployment
    public static EnterpriseArchive createDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "usebundledjsfprimefacesear.ear")
                .addAsModule(ShrinkWrap.create(WebArchive.class, "usebundledjsfprimefaces.war")
                        .addClasses(Resources.class, JSFVersion.class)
                        .addAsWebInfResource(new File("src/main/webapp/WEB-INF", "beans.xml"))
                        .addAsWebInfResource(new File("src/main/webapp/WEB-INF", "faces-config.xml"))
                        .addAsWebInfResource(new File("src/main/webapp/WEB-INF", "web.xml"))
                        .addAsWebInfResource(new File("src/main/webapp/WEB-INF", "payara-web.xml"))
                        .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.glassfish:jakarta.faces:" + JSF_VERSION))
                        .addAsLibraries(Libraries.resolveMavenCoordinatesToFiles("org.primefaces:primefaces:jar:jakarta:13.0.1")));
    }

    @Test
    @RunAsClient
    public void checkFacesContextImplementationVersion() {
        WebTarget target = ClientBuilder.newClient().target(uri).path("resources").path("jsf");
        Response response = target.request().get();

        String message = response.readEntity(String.class);

        System.out.println("FacesContext implementation version is: " + message);

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(JSF_VERSION, message);
    }
}