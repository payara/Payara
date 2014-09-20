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

package org.glassfish.admin.mbeanserver.ssl;

import java.io.File;

/**
 * This class is a config holder for configuring SSL Sockets.
 * It comes with set of defaults as defined below
 * TrustAlgorithm = SunX509
 * keystore type = JKS
 * truststore type = JKS
 * protocol = TLS
 * ssl3 Enabled = true
 * tls Enabled= true
 * It also picks up the value of keystore, keystore password, truststore , trustore password from
 * system properties.
 *
 * Usage : This class can be used in any enviroment , where one wants to pass
 * in SSL defaults programatically as well as use a default set of configuration
 * without setting in values explicitly.
 * @author prasads@dev.java.net
 */
public class SSLParams {
    private File trustStore;
    private String trustStorePwd;
    private String trustStoreType = "JKS";
    private String trustAlgorithm = "SunX509";

    private String keyAlgorithm;
    private String keyStoreType = "JKS";
    private String keyStorePassword;
    private File keyStore;

    private String protocol = "TLS";

    private String[] enabledCiphers = new String[5];
    private String[] enabledProtocols = new String[5];

    private String trustMaxCertLength;
    private String certNickname;
    private String clientAuthEnabled;
    private String clientAuth;
    private String crlFile;
    private String ssl2Ciphers;
    private Boolean ssl2Enabled = false;
    private Boolean ssl3Enabled = true;
    private String ssl3TlsCiphers;
    private Boolean tlsEnabled=true;
    private Boolean tlsRollBackEnabled=false;




    public SSLParams( File truststore,  String trustStorePwd,  String trustStoreType ) {
        this.trustStore = truststore;
        this.trustStorePwd = trustStorePwd;
        this.trustStoreType = trustStoreType;
    }

    public SSLParams() {

    }

    public File getTrustStore() {
        if(trustStore != null ) {
            return trustStore;
        } else if(System.getProperty("javax.net.ssl.trustStore") != null) {
            return new File(System.getProperty("javax.net.ssl.trustStore"));
        } else {
            return null;
        }
    }

    public String getTrustStorePassword() {
        if(trustStorePwd != null ) {
            return trustStorePwd;
        } else if(System.getProperty("javax.net.ssl.trustStorePassword") != null) {
            return System.getProperty("javax.net.ssl.trustStorePassword");
        } else {
            return null;
        }
    }

    public String getTrustStoreType() {
        if(trustStoreType != null ) {
            return trustStoreType;
        } else if(System.getProperty("javax.net.ssl.trustStoreType") != null) {
            return System.getProperty("javax.net.ssl.trustStoreType");
        } else {
            return "JKS";
        }
    }

    String getTrustMaxCertLength() {
        if( trustMaxCertLength == null) return "5";
        return trustMaxCertLength;
    }


    public String getTrustAlgorithm() {
        return trustAlgorithm;
    }

    public void setTrustAlgorithm(String algorithm) {
        this.trustAlgorithm = algorithm;
    }

    public String[] getEnabledCiphers() {
        return enabledCiphers;
    }

    public void setEnabledCiphers(String[] enabledCiphers) {
        this.enabledCiphers = enabledCiphers;
    }

    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    public void setEnabledProtocols(String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    public void setTrustMaxCertLength(String maxLength) {
        trustMaxCertLength = maxLength;
    }

    public String getCertNickname() {
        return certNickname;
    }

    public void setCertNickname(String certNickname) {
        this.certNickname = certNickname;
    }

    /**
     * Determines whether SSL3 client authentication is performed on every request, independent of ACL-based access
     * control.
     */

    public String getClientAuthEnabled() {
        return clientAuthEnabled;
    }

    public void setClientAuthEnabled(String clientAuthEnabled) {
        this.clientAuthEnabled = clientAuthEnabled;
    }

    /**
     * Determines if if the engine will request (want) or require (need) client authentication. Valid values:  want,
     * need, or left blank
     */

    public String getClientAuth() {
        return clientAuth;
    }

    public void setClientAuth(String clientAuth) {
        this.clientAuth = clientAuth;
    }


    public String getCrlFile() {
        return crlFile;
    }

    public void setCrlFile(String crlFile) {
        this.crlFile = crlFile;
    }


    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String algorithm) {
        this.keyAlgorithm = algorithm;
    }

    /**
     * type of the keystore file
     */

    public String getKeyStoreType() {
        if(keyStoreType == null) {
            keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", "JKS");
        }
        return keyStoreType;
    }

    public void setKeyStoreType(String type) {
        this.keyStoreType = type;
    }


    public String getKeyStorePassword() {
        return keyStorePassword == null? System.getProperty("javax.net.ssl.keyStorePassword"):keyStorePassword;
    }

    public void setKeyStorePassword(String password) {
        this.keyStorePassword = password;
    }

    public File getKeyStore() {
        return keyStore == null ? new File(System.getProperty("javax.net.ssl.keyStore")) : keyStore ;
    }

    public void setKeyStore(String location) {
        keyStore = new File(location);
    }


    /**
     * A comma-separated list of the SSL2 ciphers used, with the prefix + to enable or - to disable, for example +rc4.
     * Allowed values are rc4, rc4export, rc2, rc2export, idea, des, desede3. If no value is specified, all supported
     * ciphers are assumed to be enabled. NOT Used in PE
     */

    public String getSsl2Ciphers() {
        return ssl2Ciphers;
    }

    public void setSsl2Ciphers(String ssl2Ciphers) {
        this.ssl2Ciphers = ssl2Ciphers;
    }

    /**
     * Determines whether SSL2 is enabled. NOT Used in PE. SSL2 is not supported by either iiop or web-services. When
     * this element is used as a child of the iiop-listener element then the only allowed value for this attribute is
     * "false".
     */
    public Boolean getSsl2Enabled() {
        return ssl2Enabled;
    }

    public void setSsl2Enabled(String ssl2Enabled) {
        this.ssl2Enabled = Boolean.parseBoolean(ssl2Enabled);
    }

    /**
     * Determines whether SSL3 is enabled. If both SSL2 and SSL3 are enabled for a virtual server, the server tries SSL3
     * encryption first. If that fails, the server tries SSL2 encryption.
     */
    public Boolean getSsl3Enabled() {
        return ssl3Enabled;
    }

    public void setSsl3Enabled(String ssl3Enabled) {
        this.ssl3Enabled = Boolean.parseBoolean(ssl3Enabled);
    }

    /**
     * A comma-separated list of the SSL3 ciphers used, with the prefix + to enable or - to disable, for example
     * +SSL_RSA_WITH_RC4_128_MD5. Allowed SSL3/TLS values are those that are supported by the JVM for the given security
     * provider and security service configuration. If no value is specified, all supported ciphers are assumed to be
     * enabled.
     */
    public String getSsl3TlsCiphers() {
        return ssl3TlsCiphers;
    }

    public void setSsl3TlsCiphers(String ssl3TlsCiphers) {
        this.ssl3TlsCiphers  = ssl3TlsCiphers;
    }

    /**
     * Determines whether TLS is enabled.
     */

    public Boolean getTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(String tlsEnabled) {
        this.tlsEnabled = Boolean.parseBoolean(tlsEnabled);
    }


    /**
     * Determines whether TLS rollback is enabled. TLS rollback should be enabled for Microsoft Internet Explorer 5.0
     * and 5.5. NOT Used in PE
     */

    public Boolean getTlsRollbackEnabled() {
        return tlsRollBackEnabled;
    }

    public void setTlsRollbackEnabled(String tlsRollBackEnabled) {
        this.tlsRollBackEnabled = Boolean.parseBoolean(tlsRollBackEnabled);
    }

}
