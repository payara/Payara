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

package fish.payara.samples.setuptests;

import fish.payara.samples.CliCommands;
import fish.payara.samples.SecurityUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 *
 * @Author James Hillyard
 */
@RunWith(Arquillian.class)
public class MultipleKeystoresConfigurationTest {

    @Test
    public void createAdditionalKeystore() {
        KeyPair clientKeyPair = SecurityUtils.generateRandomRSAKeys();
        X509Certificate clientCertificate = SecurityUtils.createSelfSignedCertificate(clientKeyPair);
        String path = SecurityUtils.createTempJKSKeyStore(clientKeyPair.getPrivate(), clientCertificate);

        //Used so the path is correct when run in the cli command
        path = path.replace("\\", "\\\\");
        path = path.replace(":", "\\:");

        CliCommands.payaraGlassFish("create-jvm-options", "\"-Dfish.payara.ssl.additionalKeyStores="+path+"\"");
    }

    @Test
    public void createNewNetworkListener() {
        CliCommands.payaraGlassFish("create-protocol", "--securityenabled=true", "--target=server-config", "wibbles-protocol");
        CliCommands.payaraGlassFish("create-http", "--defaultVirtualServer=server", "--target=server-config", "wibbles-protocol");
        CliCommands.payaraGlassFish("create-network-listener", "--address=0.0.0.0", "--listenerport=8282", "--protocol=wibbles-protocol", "wibbles");
        CliCommands.payaraGlassFish("set", "configs.config.server-config.network-config.protocols.protocol.wibbles-protocol.ssl.cert-nickname=omnikey");
    }

}
