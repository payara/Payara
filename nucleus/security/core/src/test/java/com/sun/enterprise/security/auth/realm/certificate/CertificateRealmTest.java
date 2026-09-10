/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package com.sun.enterprise.security.auth.realm.certificate;

import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for CertificateRealm.
 */
public class CertificateRealmTest {

    private CertificateRealm realm;
    private Method getCertificateFromSubjectMethod;

    @Before
    public void setUp() throws Exception {
        realm = new CertificateRealm();
        
        // Access the private method via reflection for testing
        getCertificateFromSubjectMethod = CertificateRealm.class.getDeclaredMethod(
            "getCertificateFromSubject", Subject.class, X500Principal.class);
        getCertificateFromSubjectMethod.setAccessible(true);
    }

    /**
     * Test that getCertificateFromSubject correctly matches the certificate's 
     * SubjectX500Principal (not IssuerX500Principal).
     */
    @Test
    public void testGetCertificateFromSubject_MatchesSubjectPrincipal() throws Exception {
        // Create a test certificate
        X500Principal subjectPrincipal = new X500Principal("CN=Test User, OU=Engineering, O=Payara, C=US");
        X500Principal issuerPrincipal = new X500Principal("CN=Test CA, O=Payara, C=US");
        
        X509Certificate certificate = createSelfSignedCertificate(subjectPrincipal, issuerPrincipal);
        
        // Create a Subject with the certificate in public credentials
        Subject subject = new Subject();
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(certificate);
        subject.getPublicCredentials().add(certList);
        
        // Call the method - should match the SUBJECT principal
        X509Certificate result = (X509Certificate) getCertificateFromSubjectMethod.invoke(
            realm, subject, subjectPrincipal);
        
        assertNotNull("Certificate should be found when matching subject principal", result);
        assertEquals("Returned certificate should be the same instance", certificate, result);
    }

    /**
     * Test that getCertificateFromSubject does NOT match the certificate's 
     * IssuerX500Principal (this verifies the bug fix).
     */
    @Test
    public void testGetCertificateFromSubject_DoesNotMatchIssuerPrincipal() throws Exception {
        // Create a test certificate with different subject and issuer
        X500Principal subjectPrincipal = new X500Principal("CN=Test User, OU=Engineering, O=Payara, C=US");
        X500Principal issuerPrincipal = new X500Principal("CN=Test CA, O=Payara, C=US");
        
        X509Certificate certificate = createSelfSignedCertificate(subjectPrincipal, issuerPrincipal);
        
        // Create a Subject with the certificate in public credentials
        Subject subject = new Subject();
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(certificate);
        subject.getPublicCredentials().add(certList);
        
        // Call the method with the ISSUER principal - should NOT match
        X509Certificate result = (X509Certificate) getCertificateFromSubjectMethod.invoke(
            realm, subject, issuerPrincipal);
        
        assertNull("Certificate should NOT be found when matching issuer principal", result);
    }

    /**
     * Test that getCertificateFromSubject returns null when no certificate matches.
     */
    @Test
    public void testGetCertificateFromSubject_NoMatch() throws Exception {
        X500Principal subjectPrincipal = new X500Principal("CN=Test User, O=Payara, C=US");
        X500Principal differentPrincipal = new X500Principal("CN=Different User, O=Payara, C=US");
        
        X509Certificate certificate = createSelfSignedCertificate(subjectPrincipal, subjectPrincipal);
        
        // Create a Subject with the certificate
        Subject subject = new Subject();
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(certificate);
        subject.getPublicCredentials().add(certList);
        
        // Try to find with a different principal
        X509Certificate result = (X509Certificate) getCertificateFromSubjectMethod.invoke(
            realm, subject, differentPrincipal);
        
        assertNull("Certificate should not be found with non-matching principal", result);
    }

    /**
     * Test with multiple certificates in the subject.
     */
    @Test
    public void testGetCertificateFromSubject_MultipleCertificates() throws Exception {
        X500Principal principal1 = new X500Principal("CN=User1, O=Payara, C=US");
        X500Principal principal2 = new X500Principal("CN=User2, O=Payara, C=US");
        
        X509Certificate cert1 = createSelfSignedCertificate(principal1, principal1);
        X509Certificate cert2 = createSelfSignedCertificate(principal2, principal2);
        
        // Create a Subject with multiple certificates
        Subject subject = new Subject();
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert1);
        certList.add(cert2);
        subject.getPublicCredentials().add(certList);
        
        // Should find the matching certificate
        X509Certificate result = (X509Certificate) getCertificateFromSubjectMethod.invoke(
            realm, subject, principal2);
        
        assertNotNull("Certificate should be found", result);
        assertEquals("Should return the certificate with matching subject", cert2, result);
    }

    /**
     * Test with empty subject.
     */
    @Test
    public void testGetCertificateFromSubject_EmptySubject() throws Exception {
        Subject subject = new Subject();
        X500Principal principal = new X500Principal("CN=Test User, O=Payara, C=US");
        
        X509Certificate result = (X509Certificate) getCertificateFromSubjectMethod.invoke(
            realm, subject, principal);
        
        assertNull("Certificate should not be found in empty subject", result);
    }

    /**
     * Helper method to create a self-signed certificate for testing.
     */
    private X509Certificate createSelfSignedCertificate(
            X500Principal subjectPrincipal, X500Principal issuerPrincipal) throws Exception {
        
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        return new TestX509Certificate(subjectPrincipal, issuerPrincipal, keyPair.getPublic());
    }

    /**
     * Simple test implementation of X509Certificate for testing purposes.
     */
    private static class TestX509Certificate extends X509Certificate {
        private final X500Principal subjectPrincipal;
        private final X500Principal issuerPrincipal;
        private final PublicKey publicKey;

        public TestX509Certificate(X500Principal subjectPrincipal, X500Principal issuerPrincipal, PublicKey publicKey) {
            this.subjectPrincipal = subjectPrincipal;
            this.issuerPrincipal = issuerPrincipal;
            this.publicKey = publicKey;
        }

        @Override
        public X500Principal getSubjectX500Principal() {
            return subjectPrincipal;
        }

        @Override
        public X500Principal getIssuerX500Principal() {
            return issuerPrincipal;
        }

        @Override
        public PublicKey getPublicKey() {
            return publicKey;
        }

        @Override
        public void checkValidity() {}

        @Override
        public void checkValidity(Date date) {}

        @Override
        public int getVersion() { return 3; }

        @Override
        public BigInteger getSerialNumber() { return BigInteger.ONE; }

        @Override
        public Principal getIssuerDN() { return issuerPrincipal; }

        @Override
        public Principal getSubjectDN() { return subjectPrincipal; }

        @Override
        public Date getNotBefore() { return Date.from(Instant.now()); }

        @Override
        public Date getNotAfter() { return Date.from(Instant.now().plus(365, ChronoUnit.DAYS)); }

        @Override
        public byte[] getTBSCertificate() { return new byte[0]; }

        @Override
        public byte[] getSignature() { return new byte[0]; }

        @Override
        public String getSigAlgName() { return "SHA256withRSA"; }

        @Override
        public String getSigAlgOID() { return "1.2.840.113549.1.1.11"; }

        @Override
        public byte[] getSigAlgParams() { return null; }

        @Override
        public boolean[] getIssuerUniqueID() { return null; }

        @Override
        public boolean[] getSubjectUniqueID() { return null; }

        @Override
        public boolean[] getKeyUsage() { return null; }

        @Override
        public int getBasicConstraints() { return -1; }

        @Override
        public byte[] getEncoded() { return new byte[0]; }

        @Override
        public void verify(PublicKey key) {}

        @Override
        public void verify(PublicKey key, String sigProvider) {}

        @Override
        public String toString() {
            return "TestX509Certificate[subject=" + subjectPrincipal + ", issuer=" + issuerPrincipal + "]";
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() { return false; }

        @Override
        public java.util.Set<String> getCriticalExtensionOIDs() { return null; }

        @Override
        public java.util.Set<String> getNonCriticalExtensionOIDs() { return null; }

        @Override
        public byte[] getExtensionValue(String oid) { return null; }
    }
}
