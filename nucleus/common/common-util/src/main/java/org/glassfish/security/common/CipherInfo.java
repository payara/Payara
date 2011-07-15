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

package org.glassfish.security.common;

import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * This class represents the information associated to ciphers.
 * It also maintains a HashMap from configName to CipherInfo.
 * @author Shing Wai Chan
 */
public class CipherInfo {
    private static final short SSL2 = 0x1;
    private static final short SSL3 = 0x2;
    private static final short TLS = 0x4;

    // The old names mapped to the standard names as existed
    private static final String[][] OLD_CIPHER_MAPPING = {
            // IWS 6.x or earlier
            {"rsa_null_md5"   , "SSL_RSA_WITH_NULL_MD5"},
            {"rsa_null_sha"   , "SSL_RSA_WITH_NULL_SHA"},
            {"rsa_rc4_40_md5" , "SSL_RSA_EXPORT_WITH_RC4_40_MD5"},
            {"rsa_rc4_128_md5", "SSL_RSA_WITH_RC4_128_MD5"},
            {"rsa_rc4_128_sha", "SSL_RSA_WITH_RC4_128_SHA"},
            {"rsa_3des_sha"   , "SSL_RSA_WITH_3DES_EDE_CBC_SHA"},
            {"fips_des_sha"   , "SSL_RSA_WITH_DES_CBC_SHA"},
            {"rsa_des_sha"    , "SSL_RSA_WITH_DES_CBC_SHA"},

            // backward compatible with AS 9.0 or earlier
            {"SSL_RSA_WITH_NULL_MD5", "SSL_RSA_WITH_NULL_MD5"},
            {"SSL_RSA_WITH_NULL_SHA", "SSL_RSA_WITH_NULL_SHA"}
        };

    private static Map ciphers = new HashMap();

    private String configName;
    private String cipherName;
    private short protocolVersion;


    static {
        int len = OLD_CIPHER_MAPPING.length;
        for(int i=0; i<len; i++) {
            String nonStdName = OLD_CIPHER_MAPPING[i][0];
            String stdName    = OLD_CIPHER_MAPPING[i][1];
            ciphers.put(nonStdName, 
                new CipherInfo(nonStdName, stdName, (short)(SSL3|TLS)) );
        }

        SSLServerSocketFactory factory = 
                (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        String[] supportedCiphers = factory.getDefaultCipherSuites();
        len = supportedCiphers.length;
        for(int i=0; i<len; i++) {
            String s = supportedCiphers[i];
            ciphers.put(s, new CipherInfo(s, s, (short)(SSL3|TLS)) );
        }
    }

    /**
     * @param configName  name used in domain.xml, sun-acc.xml
     * @param cipherName  name that may depends on backend
     * @param protocolVersion
     */
    private CipherInfo(String configName, String cipherName, 
            short protocolVersion) {
        this.configName = configName;
        this.cipherName = cipherName;
        this.protocolVersion = protocolVersion;
    }

    public static CipherInfo getCipherInfo(String configName) {
        return (CipherInfo)ciphers.get(configName);
    }

    public String getConfigName() {
        return configName;
    }

    public String getCipherName() {
        return cipherName;
    }

    public boolean isSSL2() {
        return (protocolVersion & SSL2) == SSL2;
    } 

    public boolean isSSL3() {
        return (protocolVersion & SSL3) == SSL3;
    } 

    public boolean isTLS() {
        return (protocolVersion & TLS) == TLS;
    } 
}
