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

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;

import java.io.File;
import java.security.KeyStore;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.RSAKey;
import java.util.Collection;
import java.util.concurrent.Callable;

import javax.security.auth.x500.X500Principal;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Prints information about a certificate given in a file.
 * <p>
 * Uses RFC-2253 and the {@link CertificateRealm#OID_MAP} for the principal distinguished name.
 * The Subject from the output can be directly used for a role mapping when using client
 * certificate authentication, f.e.<br>
 * <code>&lt;principal-name&gt;CN=My Client,OU=Payara,O=Payara Foundation,L=Great Malvern,ST=Worcestershire,C=UK&lt;/principal-name&gt;</code>
 * <p>
 * <b>WARNING:</b> JKS and JCEKS may be removed from some JDKs, then this command may fail.
 *
 * @author David Matejcek
 */
@Service(name = "print-certificate")
@PerLookup
public class PrintCertificateCommand extends CLICommand {

    @Param(name = "file", primary = true)
    String file;

    @Param(name = "certificatealias", optional = true, defaultValue = "")
    String certificateAlias;

    @Param(name = "providerclass", optional = true)
    String providerClass;

    private Provider provider;

    private File derFile;

    private File keystoreFile;

    private char[] keystorePassword;

    @Override
    protected void validate() throws CommandException, CommandValidationException {
        super.validate();
        if (!ok(this.file)) {
            throw new CommandValidationException("The file with the certificate must be specified.");
        }
        final File sourceFile = new File(this.file);
        if (!sourceFile.canRead()) {
            throw new CommandValidationException(
                "The file '" + this.file + "' with the certificate must exist and must be readable.");
        }
        if (ok(this.providerClass)) {
            try {
                this.provider = (Provider) Class.forName(this.providerClass).newInstance();
            } catch (final ReflectiveOperationException e) {
                throw new CommandValidationException("The provider class was not found on classpath.", e);
            }
        }

        if (isDerEncodedFile()) {
            this.derFile = sourceFile;
        } else if (isKeystoreFile()) {
            this.keystoreFile = sourceFile;
            if (!ok(this.certificateAlias)) {
                throw new CommandValidationException("The certificate alias is mandatory for the keystore type.");
            }
            this.keystorePassword = getPassword("keystorePassword", "Keystore Password", null, false);
        } else {
            throw new CommandValidationException("The file type is not supported by this command.");
        }
    }


    @Override
    protected int executeCommand() throws CommandException {
        if (this.provider != null) {
            Security.insertProviderAt(this.provider, 1);
        }
        final X509Certificate certificate = getCertificate();
        System.out.println("Found Certificate:\n" + toPayaraFormattedString(certificate));
        return 0;
    }


    private String toPayaraFormattedString(final X509Certificate certificate) {
        final StringBuilder output = new StringBuilder(1024);
        output.append("Subject:    ").append(toString(certificate.getSubjectX500Principal()));
        output.append("\nValidity:   ").append(certificate.getNotBefore()).append(" - ").append(certificate.getNotAfter());
        output.append("\nS/N:        ").append(certificate.getSerialNumber());
        output.append("\nVersion:    ").append(certificate.getVersion());
        output.append("\nIssuer:     ").append(toString(certificate.getIssuerX500Principal()));
        output.append("\nPublic Key: ").append(toString(certificate.getPublicKey()));
        output.append("\nSign. Alg.: ").append(certificate.getSigAlgName()).append(" (OID: ").append(certificate.getSigAlgOID()).append(')');

        return output.toString();
    }


    private String toString(final X500Principal principal) {
        return principal.getName(X500Principal.RFC2253, CertificateRealm.OID_MAP);
    }


    private String toString(final PublicKey key) {
        if (key instanceof RSAKey) {
            final RSAKey rsaKey = (RSAKey) key;
            return key.getAlgorithm() + ", " + rsaKey.getModulus().bitLength() + " bits";
        }
        if (key instanceof DSAKey) {
            final DSAKey dsaKey = (DSAKey) key;
            if (dsaKey.getParams() != null) {
                return key.getAlgorithm() + ", " + dsaKey.getParams().getP().bitLength() + " bits";
            }
        }
        return key.getAlgorithm() + ", unresolved bit length.";
    }


    private boolean isDerEncodedFile() {
        return this.file.trim().matches(".*\\.(cer|cert|crt|der|pem)");
    }


    private boolean isKeystoreFile() {
        // PKCS #12: p12, pfx, pkcs12
        // Other Java Keystores: jks, jceks (both Oracle proprietary)
        return this.file.trim().matches(".*\\.(jks|jceks|pkcs12|pfx|p12)");
    }


    private String getKeystoreType() {
        final String ksFilename = this.keystoreFile.getName().toLowerCase();
        if (ksFilename.endsWith("jks")) {
            return "JKS";
        }
        if (ksFilename.endsWith("jceks")) {
            return "JCEKS";
        }
        if (ksFilename.endsWith("p12") || ksFilename.endsWith("pfx") || ksFilename.endsWith("pkcs12")) {
            return "PKCS12";
        }
        throw new IllegalStateException("Reached unreachable code, validation is incomplete!");
    }


    private X509Certificate getCertificate() throws CommandException {
        if (this.derFile != null) {
            return getCertificateFromDerFile();
        }
        if (this.keystoreFile != null) {
            return getCertificateFromKeystore();
        }
        throw new CommandException("Could not read the certificate from the provided file.");
    }


    private X509Certificate getCertificateFromDerFile() throws CommandException {
        try {
            final Callable<Certificate> supplier = () -> {
                final KeystoreManager manager = new KeystoreManager();
                final Collection<? extends Certificate> chain = manager.readPemCertificateChain(this.derFile);
                if (chain.isEmpty()) {
                    return null;
                }
                return chain.iterator().next();
            };
            return getX509Certificate(supplier);
        } catch (final Exception e) {
            throw new CommandException("Could not read the certificate from the provided file.", e);
        }
    }


    private X509Certificate getCertificateFromKeystore() throws CommandException {
        try {
            final Callable<Certificate> supplier = () -> {
                final KeystoreManager manager = new KeystoreManager();
                final String ksType = getKeystoreType();
                final KeyStore keystore = manager.openKeyStore(this.keystoreFile, ksType, this.keystorePassword);
                return keystore.getCertificate(this.certificateAlias);
            };
            return getX509Certificate(supplier);
        } catch (final Exception e) {
            throw new CommandException("Could not read the certificate from the provided keystore.", e);
        }
    }


    private static X509Certificate getX509Certificate(final Callable<Certificate> supplier) throws Exception {
        final Certificate certificate  = supplier.call();
        if (certificate instanceof X509Certificate) {
            return (X509Certificate) certificate;
        }
        throw new IllegalStateException("The certificate was found but it is not supported X509 certificate.");
    }
}
