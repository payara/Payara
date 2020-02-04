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

import fish.payara.test.containers.tools.container.AsadminCommandException;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerFiles;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.env.TestConfiguration;
import fish.payara.test.containers.tools.junit.DockerITestExtension;
import fish.payara.test.containers.tools.security.KeyStoreManager;
import fish.payara.test.containers.tools.security.KeyStoreType;
import fish.payara.test.containers.tst.security.war.servlets.PublicServlet;

import io.github.zforgo.arquillian.junit5.ArquillianExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Note: use <code>-Djavax.net.debug=all</code> from the command line to watch whole fun under it
 * all!
 *
 * @author David Matejcek
 */
@ExtendWith(DockerITestExtension.class)
@ExtendWith(ArquillianExtension.class)
public class CertificateRealmITest {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRealmITest.class);
    private static final String KS_PASSWORD = "changeit";
    private static final String DN_CLIENT = "CN=Payara The Fish, OU=payara-ou, O=payara-o, C=ID,"
        + " EMAILADDRESS=payara@payara.fish, DC=, DC=Payara1, DC=Payara2";
    private static final int RESPONSE_LINE_COUNT = 6;

    @ArquillianResource
    private static URL base;
    private static URL baseHttps;

    private static TestConfiguration testConfiguration;
    private static KeyStoreManager clientKeyStore;
    private static KeyStoreManager clientTrustStore;
    private static File clientTrustStoreFile;
    private static final AtomicInteger ix = new AtomicInteger();

    private WebClient webClient;


    @Deployment(testable = false)
    public static WebArchive createDeployment() throws Exception {
        LOG.info("createDeployment()");

        testConfiguration = TestConfiguration.getInstance();
        final File webInfDir = testConfiguration.getClassDirectory().toPath()
            .resolve(Paths.get("security", "war", "servlets", "WEB-INF")).toFile();
        final WebArchive war = ShrinkWrap.create(WebArchive.class) //
            .addPackage(PublicServlet.class.getPackage()) //
            .addAsWebInfResource(new File(webInfDir, "web.xml")) //
            // glassfish-web.xml must be ignored by the server, because of payara-web.xml
            .addAsWebInfResource(new File(webInfDir, "glassfish-web.xml")) //
            .addAsWebInfResource(new File(webInfDir, "payara-web.xml")) //
        ;

        LOG.info(war.toString(true));
        return war;
    }


    @BeforeAll
    public static void initEnvironment() throws Exception {
        LOG.info("initEnvironment()");

        final KeyPair clientKeyPair = createKeyPair();
        final X509Certificate clientCertificate = createClientCertificate(clientKeyPair);
        clientKeyStore = createClientKeyStore(clientKeyPair.getPrivate(), clientCertificate);

        final PayaraServerFiles payaraFiles = new PayaraServerFiles(testConfiguration.getPayaraDirectory());
        final KeyStoreManager payaraTrustStore = payaraFiles.getTrustStore();
        payaraTrustStore.putTrusted("TestCert", clientCertificate);
        payaraTrustStore.save(payaraFiles.getTrustStoreFile());

        DockerEnvironment.getInstance().getPayaraContainer().asLocalAdmin("restart-domain");
        DockerEnvironment.getInstance().getPayaraContainer().asAdmin("list-applications", "--subcomponents");
    }


    @BeforeEach
    public void init() throws Exception {
        if (baseHttps == null) {
            final DockerEnvironment dockerEnvironment = DockerEnvironment.getInstance();
            baseHttps = new URL(dockerEnvironment.getPayaraContainer().getHttpsUrl(), base.getPath());
            LOG.debug("Using baseHttps={} and client key store from: {}", baseHttps, clientKeyStore);

            final X509Certificate[] serverCertificateChain = getServerCertificateChain();
            LOG.debug("Received server certificates: \n{}", (Object) serverCertificateChain);
            assertThat("Received server certificate chain", serverCertificateChain.length, IsEqual.equalTo(1));

            clientTrustStore = createClientTrustStore(serverCertificateChain[0]);
            clientTrustStoreFile = File.createTempFile("trust-store", clientTrustStore.getKeyStoreType().name());
            clientTrustStore.save(clientTrustStoreFile);
        }

        assertNotNull(clientTrustStoreFile, "Client trust store is not initialized.");
        this.webClient = new WebClient();
        LOG.debug("Using baseHttps={} and client key store from: {}", baseHttps, clientKeyStore);

        // Client -> Server: the key store's private keys and certificates are used to sign
        // and send a reply to the server
        try (ByteArrayInputStream is = new ByteArrayInputStream(clientKeyStore.toBytes())) {
            webClient.getOptions().setSSLClientCertificate(is, KS_PASSWORD, clientKeyStore.getKeyStoreType().name());
        }
        this.webClient.getOptions().setSSLTrustStore(clientTrustStoreFile.toURI().toURL(), KS_PASSWORD,
            clientTrustStore.getKeyStoreType().name());
        this.webClient.getOptions().setTimeout(10000);
    }


    /**
     * Roles are not even mapped for this context.
     *
     * @throws Exception
     */
    @Test
    public void publicServletWithDefaults() throws Exception {
        final TextPage page = webClient.getPage(new URL(baseHttps, "public"));
        final String outputContent = page.getContent();
        LOG.info("Servlet response: \n{}", outputContent);
        assertNotNull(outputContent, "output");
        final String[] lines = outputContent.split("\n");
        assertEquals(RESPONSE_LINE_COUNT, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertAll( //
            () -> assertEquals("principal=null", lines[0], "principal name"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-cn)=false", lines[1], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-dn)=false", lines[2], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-dc)=false", lines[3], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-email)=false", lines[4], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-another)=false", lines[5], "isInRole") //
        );
    }


    /**
     * Shall pass only with dn-parts-used-for-groups=EMAILADDRESS,...
     *
     * @throws Exception
     */
    @Test
    public void cnServletWithDefaultPrincipal() throws Exception {
        final TextPage page = webClient.getPage(new URL(baseHttps, "cn"));
        final String outputContent = page.getContent();
        LOG.info("Servlet response: \n{}", outputContent);
        assertNotNull("output", outputContent);
        final String[] lines = outputContent.split("\n");
        assertEquals(RESPONSE_LINE_COUNT, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertAll( //
            () -> assertThat("principal name", lines[0], startsWith("principal=")),
            () -> assertThat("principal name", lines[0].substring("principal=".length()), principalName()),
            () -> assertEquals("request.isUserInRole(payara-role-principal-cn)=false", lines[1], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-dn)=true", lines[2], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-dc)=false", lines[3], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-email)=false", lines[4], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-another)=false", lines[5], "isInRole") //
        );
    }


    /**
     * Shall pass only with dn-parts-used-for-groups=EMAILADDRESS and
     * common-name-as-principal-name=true
     *
     * @throws Exception
     */
    @Test
    public void cnServletWithCnPrincipalAndEmailRole() throws Exception {
        setRealmProperties("EMAILADDRESS", true);
        final TextPage page = webClient.getPage(new URL(baseHttps, "cn"));
        final String outputContent = page.getContent();
        LOG.info("Servlet response: \n{}", outputContent);
        assertNotNull("output", outputContent);
        final String[] lines = outputContent.split("\n");
        assertEquals(RESPONSE_LINE_COUNT, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertAll( //
            () -> assertEquals("principal=Payara The Fish", lines[0], "principal name"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-cn)=true", lines[1], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-dn)=false", lines[2], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-dc)=false", lines[3], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-email)=true", lines[4], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-another)=false", lines[5], "isInRole") //
        );
    }


    /**
     * Shall pass only with dn-parts-used-for-groups=EMAILADDRESS,DC and
     * common-name-as-principal-name=true.
     * <p>
     * Note that DC can have multiple values.
     *
     * @throws Exception
     */
    @Test
    public void cnServletWithCnPrincipalAndEmailAndDcRole() throws Exception {
        setRealmProperties("EMAILADDRESS,DC", true);
        final TextPage page = webClient.getPage(new URL(baseHttps, "cn"));
        final String outputContent = page.getContent();
        LOG.info("Servlet response: \n{}", outputContent);
        assertNotNull("output", outputContent);
        final String[] lines = outputContent.split("\n");
        assertEquals(RESPONSE_LINE_COUNT, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertAll( //
            () -> assertEquals("principal=Payara The Fish", lines[0], "principal name"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-cn)=true", lines[1], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-dn)=false", lines[2], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-dc)=true", lines[3], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-email)=true", lines[4], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-another)=false", lines[5], "isInRole") //
        );
    }

    /**
     * Shall pass only with dn-parts-used-for-groups=EMAILADDRESS,...
     * <p>
     * ST is used as group - but it is not mapped to role. It should not be a problem.
     *
     * @throws Exception
     */
    @Test
    public void emailGroupServletWithDefaultPrincipalAndEmailRole() throws Exception {
        setRealmProperties("EMAILADDRESS,ST", false);
        final TextPage page = webClient.getPage(new URL(baseHttps, "emailgroup"));
        final String outputContent = page.getContent();
        LOG.info("Servlet response: \n{}", outputContent);
        assertNotNull("output", outputContent);
        final String[] lines = outputContent.split("\n");
        assertEquals(RESPONSE_LINE_COUNT, lines.length, () -> "outputContent.lines.count: \n" + outputContent);
        assertAll( //
            () -> assertThat("principal name", lines[0], startsWith("principal=")),
            () -> assertThat("principal name", lines[0].substring("principal=".length()), principalName()),
            () -> assertEquals("request.isUserInRole(payara-role-principal-cn)=false", lines[1], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-principal-dn)=true", lines[2], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-dc)=false", lines[3], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-email)=true", lines[4], "isInRole"),
            () -> assertEquals("request.isUserInRole(payara-role-another)=false", lines[5], "isInRole") //
        );
    }


    /**
     * Shall pass only without dn-parts-used-for-groups set
     *
     * @throws Exception
     */
    @Test
    public void emailGroupServletWithDefaultPrincipal() throws Exception {
        try {
            final TextPage page = webClient.getPage(new URL(baseHttps, "emailgroup"));
            fail("Exception expected, but received response: \n" + page.getContent());
        } catch (FailingHttpStatusCodeException e) {
            assertThat("Exception message", e.getMessage(), startsWith("403 Forbidden"));
        }
    }



    /**
     * This url even does not exist, but it is secured. 403 have priority over 404.
     *
     * @throws Exception
     */
    @Test
    public void inaccessibleNonExistingServlet() throws Exception {
        try {
            final TextPage response = webClient.getPage(new URL(baseHttps, "inaccessible"));
            fail("Exception expected, but received response: \n" + response.getContent());
        } catch (FailingHttpStatusCodeException e) {
            assertThat("Exception message", e.getMessage(), startsWith("403 Forbidden"));
        }
    }


    /**
     * Cleanup after each test
     *
     * @throws Exception
     */
    @AfterEach
    public void reset() throws Exception {
        // allow docker to flush logs
        Thread.yield();
        LOG.debug("resetRealm()");
        setRealmProperties("", false);
        if (webClient != null) {
            webClient.close();
        }
        Thread.yield();
    }


    @AfterAll
    public static void deleteTempFiles() throws Exception {
        if (clientTrustStoreFile != null) {
            clientTrustStoreFile.delete();
        }
    }


    private static void setRealmProperties(final String dnParts, final boolean cnAsPrincipal)
        throws AsadminCommandException {
        LOG.trace("setRealmProperties(dnParts={}, cnAsPrincipal={})", dnParts, cnAsPrincipal);
        final PayaraServerContainer payara = DockerEnvironment.getInstance().getPayaraContainer();
        final String prefix = "configs.config.server-config.security-service.auth-realm.certificate.property.";
        payara.asAdmin("set", prefix + "dn-parts-used-for-groups=" + dnParts);
        payara.asAdmin("set", prefix + "common-name-as-principal-name=" + cnAsPrincipal);
        // HACK for CUSTCOM-106 - only first change is reflected, following are ignored.
        // this line creates always a new property, so realm will be reconfigured.
        payara.asAdmin("set", prefix + "_hack" + ix.getAndIncrement() + "=" + (cnAsPrincipal ? "true" : "false"));
        Thread.yield();
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
            final Instant now = Instant.now();
            final Provider provider = new BouncyCastleProvider();
            final JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
            signerBuilder.setProvider(provider);
            final ContentSigner signer = signerBuilder.build(keys.getPrivate());
            final X500Name x500Name = new X500Name(DN_CLIENT);
            final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder( //
                x500Name, BigInteger.ONE, Date.from(now), Date.from(now.plus(1, ChronoUnit.DAYS)), //
                x500Name, SubjectPublicKeyInfo.getInstance(keys.getPublic().getEncoded()));
            return new JcaX509CertificateConverter().setProvider(provider)
                .getCertificate(certificateBuilder.build(signer));
        } catch (final CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }


    private static KeyStoreManager createClientKeyStore(final PrivateKey privateKey,
        final X509Certificate certificate) {
        LOG.debug("createClientKeyStore(privateKey=\n{}\n, certificate=\n{}\n)", privateKey, certificate);
        final KeyStoreManager manager = new KeyStoreManager(KeyStoreType.PKCS12, KS_PASSWORD);
        manager.putEntry("client", new PrivateKeyEntry(privateKey, new Certificate[] {certificate}),
            new PasswordProtection(KS_PASSWORD.toCharArray()));
        return manager;
    }


    private static KeyStoreManager createClientTrustStore(final X509Certificate certificate) {
        LOG.debug("createClientTrustStore(certificates=\n{}\n)", certificate);
        final KeyStoreManager manager = new KeyStoreManager(KeyStoreType.PKCS12, KS_PASSWORD);
        manager.putTrusted("localhost", certificate);
        return manager;
    }


    private static X509Certificate[] getServerCertificateChain() throws IOException, GeneralSecurityException {
        final X509TrustManager trustAllCerts = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }


            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // trust true
            }


            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // trust true
            }
        };

        final SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] {trustAllCerts}, new SecureRandom());
        final HttpsURLConnection conn = (HttpsURLConnection) baseHttps.openConnection();
        try {
            // all host names accepted
            conn.setHostnameVerifier((hostname, session) -> true);
            conn.setSSLSocketFactory(sc.getSocketFactory());
            conn.connect();
            return (X509Certificate[]) conn.getServerCertificates();
        } finally {
            conn.disconnect();
        }
    }


    private static PrincipalNameMatcher principalName() {
        return new PrincipalNameMatcher();
    }

    private static final class PrincipalNameMatcher extends TypeSafeMatcher<String> {

        private final Matcher<String[]> matcher = arrayContainingInAnyOrder(DN_CLIENT.split(", "));


        @Override
        public boolean matchesSafely(final String item) {
            if (item == null || item.length() == 0) {
                return false;
            }

            final String[] fields = item.split(",");
            return matcher.matches(fields);
        }


        @Override
        public void describeTo(final Description description) {
            description.appendText("principal name ");
            description.appendValue(DN_CLIENT);
        }
    }
}
