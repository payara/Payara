/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.omnifaces.utils.security.Certificates;
import static java.math.BigInteger.ONE;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Provides functionalitites of {@link ServerOperations} depending on external optional utilities.
 *
 * @author David Matejcek
 */
public class SecurityUtils {
    private static final Logger logger = Logger.getLogger(SecurityUtils.class.getName());
    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    public static X509Certificate createSelfSignedCertificate(KeyPair keys) {
        try {
            return new JcaX509CertificateConverter()
                    .setProvider(PROVIDER)
                    .getCertificate(
                            new X509v3CertificateBuilder(
                                    new X500Name("CN=lfoo, OU=bar, O=kaz, L=zak, ST=lak, C=UK"),
                                    ONE,
                                    Date.from(now()),
                                    Date.from(now().plus(1, DAYS)),
                                    new X500Name("CN=lfoo, OU=bar, O=kaz, L=zak, ST=lak, C=UK"),
                                    SubjectPublicKeyInfo.getInstance(keys.getPublic().getEncoded()))
                                    .build(
                                            new JcaContentSignerBuilder("SHA256WithRSA")
                                                    .setProvider(PROVIDER)
                                                    .build(keys.getPrivate())));
        } catch (CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static X509Certificate createExpiredSelfSignedCertificate(KeyPair keys) {
        try {
            return new JcaX509CertificateConverter()
                    .setProvider(PROVIDER)
                    .getCertificate(
                            new X509v3CertificateBuilder(
                                    new X500Name("CN=lfoo, OU=bar, O=kaz, L=zak, ST=lak, C=UK"),
                                    ONE,
                                    Date.from(now().minus(2, DAYS)),
                                    Date.from(now().minus(1, DAYS)),
                                    new X500Name("CN=lfoo, OU=bar, O=kaz, L=zak, ST=lak, C=UK"),
                                    SubjectPublicKeyInfo.getInstance(keys.getPublic().getEncoded()))
                                    .build(
                                            new JcaContentSignerBuilder("SHA256WithRSA")
                                                    .setProvider(PROVIDER)
                                                    .build(keys.getPrivate())));
        } catch (CertificateException | OperatorCreationException e) {
            throw new IllegalStateException(e);
        }
    }

    static URL getHostFromCertificate(X509Certificate[] serverCertificateChain, URL existingURL) {
        try {
            URL httpsUrl = new URL(
                    existingURL.getProtocol(),
                    Certificates.getHostFromCertificate(serverCertificateChain),
                    existingURL.getPort(),
                    existingURL.getFile()
            );

            logger.info("Changing base URL from " + existingURL + " into " + httpsUrl + "\n");
            return httpsUrl;

        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failure creating HTTPS URL", e);
        }
    }


    public static KeyPair generateRandomRSAKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER);
            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String createTempJKSKeyStore(PrivateKey privateKey, X509Certificate clientCertificate) {
        return Certificates.createTempJKSKeyStore(privateKey, clientCertificate);
    }

    public static String createTempJKSTrustStore(X509Certificate[] serverCertificateChain) {
        return Certificates.createTempJKSTrustStore(serverCertificateChain);
    }

    /**
     * @return never null, but can be an empty array
     */
    public static X509Certificate[] getCertificateChainFromServer(String host, int port) {
        final X509Certificate[] chain = Certificates.getCertificateChainFromServer(host, port);
        if (chain == null) {
            return new X509Certificate[0];
        }
        return chain;
    }
}
