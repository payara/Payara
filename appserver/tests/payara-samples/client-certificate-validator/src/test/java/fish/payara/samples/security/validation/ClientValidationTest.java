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

package fish.payara.samples.security.validation;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SecurityUtils;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.SincePayara;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omnifaces.utils.security.Certificates;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author James Hillyard
 */

@Ignore
@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2021.8")
public class ClientValidationTest {

    private static String certPath;

    private static final String CERTIFICATE_ALIAS = "omnikey";
    private static final String LOCALHOST_URL = "https://localhost:8181/security/secure/hello";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "security.war")
                .addPackage(ClientValidationTest.class.getPackage())
                .addPackages(true, "org.bouncycastle")
                .addPackages(true, "com.gargoylesoftware")
                .addPackages(true, "net.sourceforge.htmlunit")
                .addPackages(true, "org.eclipse")
                .addPackages(true, PayaraArquillianTestRunner.class.getPackage())
                .addClasses(ServerOperations.class, SecurityUtils.class, Certificates.class)
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/beans.xml"));
    }

    @Test
    @InSequence(1)
    public void generateCertsInTrustStore() throws IOException {
        if (ServerOperations.isServer()) {
            certPath = ServerOperations.generateClientKeyStore(true, true, CERTIFICATE_ALIAS);
        }
    }

    @Test
    @InSequence(2)
    public void validationFailTest() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        SSLSocketFactory sslSocketFactory = getSslSocketFactory(certPath, kmf, CERTIFICATE_ALIAS);
        assertEquals(401, callEndpoint(sslSocketFactory));
        assertTrue(checkForAPIValidationFailure());

    }

    private static int callEndpoint(SSLSocketFactory sslSocketFactory) throws IOException {
        URL url = new URL(LOCALHOST_URL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslSocketFactory);
        return connection.getResponseCode();
    }

    private static SSLSocketFactory getSslSocketFactory(String certPath, KeyManagerFactory kmf, String alias) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        String keystorePassword = "changeit";

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(certPath), keystorePassword.toCharArray());
        kmf.init(keyStore, keystorePassword.toCharArray());

        KeyManager[] keyManagers = kmf.getKeyManagers();

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[]{new MyKeyManager((X509ExtendedKeyManager) keyManagers[0], alias)}, null, null);
        return ctx.getSocketFactory();
    }

    /**
     * @return true if the correct warning is found in the logs
     * @throws IOException
     */
    public boolean checkForAPIValidationFailure() throws IOException {
        List<String> log = viewLog();
        for (String line : log) {
            if (line.contains("Certificate Validation Failed via API")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the contents of the server log
     */
    private List<String> viewLog() throws IOException {
        Path serverLog = ServerOperations.getDomainPath("logs/server.log");
        return Files.readAllLines(serverLog);
    }


}
