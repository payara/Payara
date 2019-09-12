/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.oidc.test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.security.oidc.client.Callback;
import fish.payara.security.oidc.client.SecuredPage;
import fish.payara.security.oidc.client.UnsecuredPage;
import fish.payara.security.oidc.server.ApplicationConfig;
import fish.payara.security.oidc.server.OidcProvider;
import java.io.IOException;
import java.net.URL;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author Gaurav Gupta
 * @author Jonathan
 */
public class OpenIdTestUtil {

    public static WebArchive createServerDeployment() {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "openid-server.war")
                .addClass(OidcProvider.class)
                .addClass(ApplicationConfig.class)
                .addAsResource("openid-configuration.json")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        System.out.println(war.toString(true));
        return war;
    }

    public static WebArchive createClientDeployment() {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "openid-client.war")
                .addClass(Callback.class)
                .addClass(SecuredPage.class)
                .addClass(UnsecuredPage.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        System.out.println(war.toString(true));
        return war;
    }

    public static void testOpenIdConnect(WebClient webClient, URL base) throws IOException {
        String result = ((TextPage) webClient.getPage(base + "Unsecured")).getContent();
        assertEquals("This is an unsecured web page", result);

        TextPage page = (TextPage) webClient.getPage(base + "Secured");
        assertEquals("/openid-client/Callback", page.getUrl().getPath());
        assertNotEquals("null", page.getContent());

        try {
            webClient.getPage(base + "Secured");
            fail("Roles test failed");
        } catch (FailingHttpStatusCodeException e) {
            System.out.println("Successfully forbidden from accessing page because of " + e.getStatusMessage());
        }

    }

}
