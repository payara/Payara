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
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.samples.security.oauth2.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;

import fish.payara.samples.security.oauth2.testapp.Callback;
import fish.payara.samples.security.oauth2.testapp.Endpoint;
import fish.payara.samples.security.oauth2.testapp.SecuredPage;
import fish.payara.samples.security.oauth2.testapp.UnsecuredPage;

/**
 *
 * @author jonathan coustick
 */
@NotMicroCompatible
@RunWith(PayaraArquillianTestRunner.class)
public class OAuthTest {
    
    private WebClient webClient;
    
    @ArquillianResource
    private URL base;
    
    @Before
    public void init() {
        webClient = new WebClient();
        System.out.println("Set up new WebClient");
    }
    
    @Deployment
    public static WebArchive createDeployment() {
        
        // Create a war with the test app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "oauthtest.war")
                .addClass(Callback.class)
                .addClass(Endpoint.class)
                .addClass(SecuredPage.class)
                .addClass(UnsecuredPage.class).addAsWebInfResource("all-beans.xml", "beans.xml");
        
        // Print out directory contents
        System.out.println(war.toString(true));

        // Return Arquillian Test Archive for application server
        return war;
    }
    
    @Test
    @RunAsClient
    public void runOAuthTest() throws IOException {
        String result = ((TextPage) webClient.getPage(base + "Unsecured")).getContent();
        assertEquals("This is an unsecured web page", result);

        TextPage page= (TextPage) webClient.getPage(base + "Secured");
        assertEquals("/oauthtest/Callback", page.getUrl().getPath());
        assertNotEquals("null", page.getContent());
        
        try {
            webClient.getPage(base + "Secured");
            fail("Roles test failed");
        } catch (FailingHttpStatusCodeException e){
            System.out.println("Successfully forbidden from accessing page because of " + e.getStatusMessage());
        }
        
    }

}
