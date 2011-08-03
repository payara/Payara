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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.enterprise.security.auth.login.common;

import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * This class holds the user certificate for the certificate realm and the
 * realm name. This credential is added as a public credential to the
 * JAAS subject.
 */

public class X509CertificateCredential {
    private X509Certificate[] certChain;
    private String realm;
    private String alias;

    /**
     * Construct a credential with the specified X509Certificate certificate
     * chain, realm name and alias.
     * @param the X509Certificate.
     * @param the alias for the certificate
     * @param the realm name. The only value supported for now is "certificate".
     */

    public X509CertificateCredential(X509Certificate[] certChain, 
				    String alias, String realm)
    {
	this.certChain = certChain;
	this.alias = alias;
	this.realm = realm;
    }

    /**
     * Return the alias for the certificate.
     * @return the alias. 
     */
    public String getAlias() {
	return alias;
    }

    /**
     * Return the realm name.
     * @return the realm name. Only value supported for now is "certificate".
     */
    public String getRealm() {
	return realm;
    }

    /**
     * Return the chain of certificates.
     * @return the chain of X509Certificates.
     */
    public X509Certificate[] getX509CertificateChain() {
	return certChain;
    }

    /**
     * Compare two instances of the credential and return true if they are
     * the same and false otherwise.
     * @return true if the instances are equal, false otherwise.
     */
    public boolean equals(Object o) {
	if(o instanceof X509CertificateCredential) {
	    X509CertificateCredential pc = (X509CertificateCredential) o;
	    if(pc.getRealm().equals(realm) && pc.getAlias().equals(alias)) {
		X509Certificate[] certs = pc.getX509CertificateChain();
		for(int i = 0; i < certs.length; i++) {
		    if(!certs[i].equals(certChain[i])) {
			return false;
		    }
		}
		return true;
	    }
	}
	return false;
    }

    /**
     * Return the hashCode computed from the certificate, realm and alias.
     * @return the hash code.
     */
    public int hashCode() {
	return Arrays.hashCode(certChain) + realm.hashCode() + ((alias != null)?alias.hashCode():0);
    }

    /**
     * String representation of the credential.
     */
    public String toString() {
	String s = "Realm=" + realm;
	s = s + " alias=" + alias;
        StringBuffer certChainStr = new StringBuffer("");
        for (int i=0; i < certChain.length; i++) {
            certChainStr.append(certChain[i].toString());
            certChainStr.append("\n");
        }
	s = s + " X509Certificate=" + certChainStr.toString();
	return s;
    }

}
