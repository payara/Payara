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
package fish.payara.admin.servermgmt.cli;

import static com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.glassfish.api.admin.CommandValidationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the {@link PrintCertificateCommand} with default and BouncyCastle provider.
 * Verifies work with several file formats.
 *
 * @author David Matejcek
 */
@RunWith(Parameterized.class)
public class PrintCertificateCommandTest {

    private static final String ALIAS = "test";

    private static final String KEYSTORE_PASSWORD = "changeit";

    private static final File FILE_JKS = new File("target/pcct.jks");
    private static final File FILE_PKCS12 = new File("target/pcct.p12");
    private static final File FILE_JCEKS = new File("target/pcct.jceks");
    private static final File FILE_DER = new File("target/pcct.der");
    private static final File FILE_PEM = new File("target/pcct.pem");

    private static final PrintStream ORIGINAL_STDOUT = System.out;
    private static final String ORIGINAL_INSTALL_ROOT = System.getProperty(INSTALL_ROOT_PROPERTY);

    private ByteArrayOutputStream stdout;
    private int providerCount;

    private final PrintCertificateCommandMock command;


    @BeforeClass
    public static void initEnvironment() throws Exception {
        // prevents NPE in CLICommand
        if (ORIGINAL_INSTALL_ROOT == null) {
            System.setProperty(INSTALL_ROOT_PROPERTY, ".");
        }

        final KeyPair keyPair = createKeyPair();
        final X509Certificate certificate = createSelfSignedCertificate(keyPair);

        saveKeyStore(keyPair.getPrivate(), certificate, FILE_PKCS12, "PKCS12");
        saveKeyStore(keyPair.getPrivate(), certificate, FILE_JKS, "JKS");
        saveKeyStore(keyPair.getPrivate(), certificate, FILE_JCEKS, "JCEKS");
        saveDer(certificate);
        savePem(certificate);
    }


    @AfterClass
    public static void resetEnvironment() {
        if (ORIGINAL_INSTALL_ROOT == null) {
            System.clearProperty(INSTALL_ROOT_PROPERTY);
        } else {
            System.setProperty(INSTALL_ROOT_PROPERTY, ORIGINAL_INSTALL_ROOT);
        }
        System.setOut(ORIGINAL_STDOUT);
    }


    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
            {FILE_DER, null}, {FILE_PEM, null}, {FILE_PKCS12, ALIAS}, {FILE_JKS, ALIAS}, {FILE_JCEKS, ALIAS} //
        });
    }


    public PrintCertificateCommandTest(final File file, final String alias) {
        this.command = new PrintCertificateCommandMock();
        this.command.certificateAlias = alias;
        this.command.file = file.getAbsolutePath();
    }


    @Before
    public void init() {
        this.stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(this.stdout));
        this.providerCount = Security.getProviders().length;
    }


    @After
    public void cleanup() {
        // if the count of providers changed, we were successful with the registration of one more.
        if (Security.getProviders().length != this.providerCount) {
            Security.removeProvider(Security.getProviders()[0].getName());
        }
    }


    @Test
    public void testDefaultProvider() throws Exception {
        this.command.validate();
        this.command.executeCommand();
        final String[] output = this.stdout.toString().split("\n");

        final String dn = "UID=LDAP-Test,EMAILADDRESS=nobody@nowhere.space,CN=PrintCertificateCommandTest,"
            + "OU=Test Test\\, Test,O=Payara Foundation,L=Pilsen,C=CZ";
        assertEquals("Found Certificate:", output[0]);
        assertEquals("Subject:    " + dn, output[1]);
        assertThat(output[2], startsWith("Validity:   "));
        assertEquals("S/N:        1", output[3]);
        assertEquals("Version:    3", output[4]);
        assertEquals("Issuer:     " + dn, output[5]);
        assertEquals("Public Key: RSA, 2048 bits", output[6]);
        assertEquals("Sign. Alg.: SHA256withRSA (OID: 1.2.840.113549.1.1.11)", output[7]);
    }


    @Test
    public void testBCProvider() throws Exception {
        this.command.providerClass = BouncyCastleProvider.class.getName();
        this.command.validate();
        this.command.executeCommand();
        final String[] output = this.stdout.toString().split("\n");

        final String dn = "UID=LDAP-Test,EMAILADDRESS=nobody@nowhere.space,CN=PrintCertificateCommandTest,"
            + "OU=Test Test\\, Test,O=Payara Foundation,L=Pilsen,C=CZ";
        assertEquals("Found Certificate:", output[0]);
        assertEquals("Subject:    " + dn, output[1]);
        assertThat(output[2], startsWith("Validity:   "));
        assertEquals("S/N:        1", output[3]);
        assertEquals("Version:    3", output[4]);
        assertEquals("Issuer:     " + dn, output[5]);
        assertEquals("Public Key: RSA, 2048 bits", output[6]);
        assertEquals("Sign. Alg.: SHA256WITHRSA (OID: 1.2.840.113549.1.1.11)", output[7]);
    }


    private static void saveKeyStore(final PrivateKey key, final X509Certificate certificate, //
        final File keystoreFile, final String keystoreType) throws Exception {

        final KeyStore keystore = KeyStore.getInstance(keystoreType);
        keystore.load(null, null);
        keystore.setKeyEntry(ALIAS, key, "changeit".toCharArray(), new Certificate[] {certificate});
        try (final OutputStream os = new FileOutputStream(keystoreFile)) {
            keystore.store(os, "changeit".toCharArray());
        }
        System.out.println("File has been created: " + keystoreFile.getAbsolutePath());
    }


    private static void saveDer(final X509Certificate certificate) throws Exception {
        try (OutputStream os = new FileOutputStream(FILE_DER)) {
            os.write(certificate.getEncoded());
        }
    }


    private static void savePem(final X509Certificate certificate) throws IOException {
        try (JcaPEMWriter pw = new JcaPEMWriter(
            new OutputStreamWriter(new FileOutputStream(FILE_PEM), StandardCharsets.US_ASCII))) {
            pw.writeObject(certificate);
        }
    }


    private static KeyPair createKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }


    private static X509Certificate createSelfSignedCertificate(KeyPair keyPair)
        throws OperatorCreationException, CertificateException {

        final Instant now = LocalDate.of(2019, 8, 1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        final X500Name dn = new X500NameBuilder() //
            .addRDN(BCStyle.C, "CZ") //
            .addRDN(BCStyle.L, "Pilsen") //
            .addRDN(BCStyle.O, "Payara Foundation") //
            .addRDN(BCStyle.OU, "Test Test, Test") //
            .addRDN(BCStyle.CN, PrintCertificateCommandTest.class.getSimpleName()) //
            .addRDN(BCStyle.EmailAddress, "nobody@nowhere.space") //
            .addRDN(BCStyle.UID, "LDAP-Test") //
            .build();

        final ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dn, BigInteger.ONE,
            Date.from(now), Date.from(now.plus(Duration.ofDays(1))), dn, keyPair.getPublic());
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
    }

    /**
     * Mocks the getPassword method to avoid user interaction.
     */
    private class PrintCertificateCommandMock extends PrintCertificateCommand {

        @Override
        protected char[] getPassword(String paramname, String localizedPrompt, String localizedPromptConfirm,
            boolean create) throws CommandValidationException {
            return KEYSTORE_PASSWORD.toCharArray();
        }
    }
}
