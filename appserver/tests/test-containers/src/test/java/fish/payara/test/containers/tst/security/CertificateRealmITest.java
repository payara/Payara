/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tst.security;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;

import fish.payara.test.containers.tools.container.PayaraServerFiles;
import fish.payara.test.containers.tools.env.TestConfiguration;
import fish.payara.test.containers.tools.junit.DockerITest;
import fish.payara.test.containers.tools.security.KeyStoreManager;
import fish.payara.test.containers.tools.security.KeyStoreType;
import fish.payara.test.containers.tst.security.war.servlets.PublicServlet;

import io.github.zforgo.arquillian.junit5.ArquillianExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Note: use <code>-Djavax.net.debug=all</code> from the command line to watch whole fun under it
 * all :-)
 *
 * @author David Matejcek
 * @author Arjan Tijms
 */
@ExtendWith(ArquillianExtension.class)
public class CertificateRealmITest extends DockerITest {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRealmITest.class);
    private static final String KS_PASSWORD = "changeit";

    @ArquillianResource
    private static URL base;
    private static URL baseHttps;

    private static WebClient webClient;
    private static KeyStoreManager clientKeyStore;


    @BeforeAll
    public static void initEnvironment() throws Exception {
        LOG.info("initEnvironment()");

        final KeyPair clientKeyPair = createKeyPair();
        final X509Certificate clientCertificate = createClientCertificate(clientKeyPair);
        clientKeyStore = createClientKeyStore(clientKeyPair.getPrivate(), clientCertificate);
        final PayaraServerFiles payaraFiles = new PayaraServerFiles(getTestConfiguration().getPayaraDirectory());
        payaraFiles.getTrustStore().putTrusted("TestCert", clientCertificate);

    }


    @BeforeEach
    public void init() throws Exception {
        if (webClient != null) {
            return;
        }
        webClient = new WebClient();
        baseHttps = getDockerEnvironment().getPayaraContainer().getHttpsUrl();
        LOG.debug("Using client key store from: {}", clientKeyStore);

        // Client -> Server : the key store's private keys and certificates are used to sign
        // and sent a reply to the server
        try (ByteArrayInputStream is = new ByteArrayInputStream(clientKeyStore.toBytes())) {
            webClient.getOptions().setSSLClientCertificate(is, KS_PASSWORD, clientKeyStore.getKeyStoreType().name());
        }
        webClient.getOptions().setTimeout(0);
    }


    @Test
    public void testPublicServlet() throws Exception {
        final TextPage page = webClient.getPage(new URL(baseHttps, "public"));
        final String outputContent = page.getContent();
        LOG.info("outputContent: " + outputContent);
        assertNotNull(outputContent, "output");
        final String[] lines = outputContent.split("\n");
        assertEquals(4, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertEquals("principal name", "principal=null", lines[0]);
        assertEquals("request.isUserInRole(payara-role-CN)=false", lines[1], "isInRole");
        assertEquals("request.isUserInRole(payara-role-email)=false", lines[2], "isInRole");
        assertEquals("request.isUserInRole(payara-role-another)=false", lines[3], "isInRole");
    }


    @Test
    public void testCNServlet() throws Exception {
        final TextPage page = webClient.getPage(new URL(baseHttps, "cn"));
        final String outputContent = page.getContent();
        LOG.info("outputContent: " + outputContent);
        assertNotNull("output", outputContent);
        final String[] lines = outputContent.split("\n");
        assertEquals(4, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertEquals("principal=Payara The Fish", lines[0], "principal name");
        assertEquals("request.isUserInRole(payara-role-CN)=true", lines[1], "isInRole");
        assertEquals("request.isUserInRole(payara-role-email)=true", lines[2], "isInRole");
        assertEquals("request.isUserInRole(payara-role-another)=false", lines[3], "isInRole");
    }


    @Test
    public void testEmailGroupServlet() throws Exception {
        final TextPage page = webClient.getPage(new URL(baseHttps, "emailgroup"));
        final String outputContent = page.getContent();
        LOG.info("outputContent: " + outputContent);
        assertNotNull("output", outputContent);
        final String[] lines = outputContent.split("\n");
        assertEquals(4, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertEquals("principal=Payara The Fish", lines[0], "principal name");
        assertEquals("request.isUserInRole(payara-role-CN)=true", lines[1], "isInRole");
        assertEquals("request.isUserInRole(payara-role-email)=true", lines[2], "isInRole");
        assertEquals("request.isUserInRole(payara-role-another)=false", lines[3], "isInRole");
    }


    @Test
    public void testInaccessibleNonExistingServlet() throws Exception {
        try {
            final String response = webClient.getPage(new URL(baseHttps, "inaccessible"));
            fail("Exception expected, but received response: \n" + response);
        } catch (FailingHttpStatusCodeException e) {
            assertThat("Exception message", e.getMessage(), startsWith("403 Forbidden"));
        }
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws Exception {
        LOG.info("createDeployment()");

        final TestConfiguration cfg = getTestConfiguration();
        final File webInfDir = new File(cfg.getBuildDirectory() + "/classes/WEB-INF");
        final WebArchive war = ShrinkWrap.create(WebArchive.class)
            .addPackage(PublicServlet.class.getPackage()) //
            .addAsWebInfResource(new File(webInfDir, "web.xml")) //
            .addAsWebInfResource(new File(webInfDir, "glassfish-web.xml")) //
            .addAsWebInfResource(new File(webInfDir, "payara-web.xml")) //
        ;

        LOG.info(war.toString(true));
        return war;
    }



    @AfterAll
    public static void cleanup() throws IOException {
        if (webClient != null) {
            webClient.getCookieManager().clearCookies();
            webClient.close();
        }
    }


    private static KeyPair createKeyPair() {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }


    private static X509Certificate createClientCertificate(final KeyPair keys) {
        try {
            final X500Name dn = new X500Name(
                "CN=Payara The Fish, OU=payara-ou, O=payara-o, C=ID, EMAILADDRESS=payara@payara.fish");
            final Instant now = Instant.now();
            final Provider provider = new BouncyCastleProvider();
            final JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
            signerBuilder.setProvider(provider);
            final ContentSigner signer = signerBuilder.build(keys.getPrivate());
            final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder( //
                dn, BigInteger.ONE, Date.from(now), Date.from(now.plus(1, ChronoUnit.DAYS)), //
                dn, SubjectPublicKeyInfo.getInstance(keys.getPublic().getEncoded()));
            return new JcaX509CertificateConverter().setProvider(provider)
                .getCertificate(certificateBuilder.build(signer));
        } catch (final CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }


    private static KeyStoreManager createClientKeyStore(final PrivateKey privateKey,
        final X509Certificate certificate) {
        final KeyStoreManager manager = new KeyStoreManager(KeyStoreType.PKCS12, KS_PASSWORD);
        manager.putEntry("client", new PrivateKeyEntry(privateKey, new Certificate[] {certificate}),
            new PasswordProtection(KS_PASSWORD.toCharArray()));
        return manager;
    }
}
