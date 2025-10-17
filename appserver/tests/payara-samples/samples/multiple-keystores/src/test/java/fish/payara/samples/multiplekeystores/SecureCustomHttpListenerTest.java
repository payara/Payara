/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples.multiplekeystores;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author James Hillyard
 */

@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class SecureCustomHttpListenerTest {
    private WebResponse webResponse;
    private static WebClient WEB_CLIENT;
    private static final String NEW_LISTENER_URL = "https://localhost:8282";
    private static final String HTTP_LISTENER_TWO_URL = "https://localhost:8181";


    @BeforeClass
    public static void setUp() {
        WEB_CLIENT = new WebClient();
        WEB_CLIENT.getOptions().setThrowExceptionOnFailingStatusCode(false);
        WEB_CLIENT.getOptions().setUseInsecureSSL(true);
    }

    @Test
    public void secureHttpListenerTwoWithDefaultCert() throws IOException {
        webResponse = WEB_CLIENT.getPage(HTTP_LISTENER_TWO_URL).getWebResponse();
        assertNotNull(webResponse);
        assertEquals("Status code should be 200", 200, webResponse.getStatusCode());
    }

    @Test
    public void secureNewHttpListenerWithAdditionalCert() throws IOException {
        webResponse = WEB_CLIENT.getPage(NEW_LISTENER_URL).getWebResponse();
        assertNotNull(webResponse);
        assertEquals("Status code should be 200", 200, webResponse.getStatusCode());
    }
}
