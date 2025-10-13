/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
//Portions Copyright [2018-2019] [Payara Foundation and/or affiliates]
package org.glassfish.admin.amx.util.stringifier;

import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import org.glassfish.admin.amx.util.StringUtil;

import javax.security.auth.x500.X500Principal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Stringifies an {@link X509Certificate}.
 */
public final class X509CertificateStringifier implements Stringifier {

    public final static X509CertificateStringifier DEFAULT = new X509CertificateStringifier();

    private static byte[] getFingerprint(byte[] signature, String alg) {
        byte[] result = null;

        try {
            final MessageDigest md = MessageDigest.getInstance(alg);

            result = md.digest(signature);
        } catch (NoSuchAlgorithmException e) {
            result = signature;
            e.printStackTrace();
        }

        return (result);
    }

    /**
     * Static variant when direct call will suffice.
     */
    public static String stringify(final X509Certificate cert) {
        final StringBuilder buf = new StringBuilder();
        final String NL = "\n";

        buf.append("Issuer: ").append(cert.getIssuerDN().getName()).append(NL);
        buf.append("Issued to: ").append(cert.getSubjectDN().getName()).append(NL);
        buf.append("Version: ").append(cert.getVersion()).append(NL);
        buf.append("Not valid before: ").append(cert.getNotBefore()).append(NL);
        buf.append("Not valid after: ").append(cert.getNotAfter()).append(NL);
        buf.append("Serial number: ").append(cert.getSerialNumber()).append(NL);
        buf.append("Signature algorithm: ").append(cert.getSigAlgName()).append(NL);
        buf.append("Signature algorithm OID: ").append(cert.getSigAlgOID()).append(NL);

        buf.append("Signature fingerprint (MD5): ");
        byte[] fingerprint = getFingerprint(cert.getSignature(), "MD5");
        buf.append(StringUtil.toHexString(fingerprint, ":")).append(NL);

        buf.append("Signature fingerprint (SHA1): ");
        fingerprint = getFingerprint(cert.getSignature(), "SHA1");
        buf.append(StringUtil.toHexString(fingerprint, ":")).append(NL);

        return (buf.toString());
    }

    @Override
    public String stringify(Object object) {
        return (stringify((X509Certificate) object));
    }
}
