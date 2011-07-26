/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.provider;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Arrays;

import sun.net.www.ParseUtil;
import sun.security.util.Debug;
import sun.security.util.Password;


/**
 * A utility class for getting a KeyStore instance from policy information.
 * In addition, a supporting getInputStream method.
 *
 * @version 1.2
 */
public class PolicyUtil {

    // standard PKCS11 KeyStore type
    private static final String P11KEYSTORE = "PKCS11";

    // reserved word
    private static final String NONE = "NONE";

    /*
     * Fast path reading from file urls in order to avoid calling
     * FileURLConnection.connect() which can be quite slow the first time
     * it is called. We really should clean up FileURLConnection so that
     * this is not a problem but in the meantime this fix helps reduce
     * start up time noticeably for the new launcher. -- DAC
     */
    public static InputStream getInputStream(URL url) throws IOException {
	if ("file".equals(url.getProtocol())) {
	    String path = url.getFile().replace('/', File.separatorChar);
	    path = ParseUtil.decode(path);
	    return new FileInputStream(path);
	} else {
	    return url.openStream();
	}
    }

    /**
     * this is intended for use by policytool and the policy parser to
     * instantiate a KeyStore from the information in the GUI/policy file
     */
    public static KeyStore getKeyStore
		(URL policyUrl,			// URL of policy file
		String keyStoreName,		// input: keyStore URL
		String keyStoreType,		// input: keyStore type
		String keyStoreProvider,	// input: keyStore provider
		String storePassURL,		// input: keyStore password
		Debug debug)
	throws KeyStoreException, MalformedURLException, IOException,
		NoSuchProviderException, NoSuchAlgorithmException,
		java.security.cert.CertificateException {

        if (keyStoreName == null) {
	    throw new IllegalArgumentException("null KeyStore name");
        }

        char[] keyStorePassword = null;
        try {
            KeyStore ks;
            if (keyStoreType == null) {
                keyStoreType = KeyStore.getDefaultType();
            }

            if (P11KEYSTORE.equalsIgnoreCase(keyStoreType) &&
                !NONE.equals(keyStoreName)) {
                throw new IllegalArgumentException
                        ("Invalid value (" +
                        keyStoreName +
                        ") for keystore URL.  If the keystore type is \"" +
                        P11KEYSTORE +
                        "\", the keystore url must be \"" +
                        NONE +
                        "\"");
            }

            if (keyStoreProvider != null) {
                ks = KeyStore.getInstance(keyStoreType, keyStoreProvider);
            } else {
                ks = KeyStore.getInstance(keyStoreType);
            }

            if (storePassURL != null) {
                URL passURL;
                try {
                    passURL = new URL(storePassURL);
                    // absolute URL
                } catch (MalformedURLException e) {
                    // relative URL
		    if (policyUrl == null) {
			throw e;
		    }
                    passURL = new URL(policyUrl, storePassURL);
                }

                if (debug != null) {
                    debug.println("reading password"+passURL);
                }

                InputStream in = passURL.openStream();
                keyStorePassword = Password.readPassword(in);
                in.close();
            }

            if (NONE.equals(keyStoreName)) {
                ks.load(null, keyStorePassword);
                return ks;
            } else {
                /*
                 * location of keystore is specified as absolute URL in policy
                 * file, or is relative to URL of policy file
                 */
                URL keyStoreUrl = null;
                try {
                    keyStoreUrl = new URL(keyStoreName);
                    // absolute URL
                } catch (MalformedURLException e) {
                    // relative URL
		    if (policyUrl == null) {
			throw e;
		    }
                    keyStoreUrl = new URL(policyUrl, keyStoreName);
                }

                if (debug != null) {
                    debug.println("reading keystore"+keyStoreUrl);
                }

                InputStream inStream = null;
                try {
                    inStream = new BufferedInputStream(getInputStream(keyStoreUrl));
                    ks.load(inStream, keyStorePassword);
                } finally {
                    inStream.close();
                }

                return ks;
            }
        } finally {
            if (keyStorePassword != null) {
                Arrays.fill(keyStorePassword, ' ');
            }
        }
    }
} 
