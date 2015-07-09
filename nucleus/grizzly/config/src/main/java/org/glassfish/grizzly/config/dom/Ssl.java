/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.dom;

import javax.validation.constraints.Pattern;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * Define SSL processing parameters
 */
@Configured
public interface Ssl extends ConfigBeanProxy, PropertyBag {
    boolean ALLOW_LAZY_INIT = true;
    boolean CLIENT_AUTH_ENABLED = false;
    boolean SSL2_ENABLED = false;
    boolean SSL3_ENABLED = false;
    boolean TLS_ENABLED = true;
    boolean TLS11_ENABLED = true;
    boolean TLS12_ENABLED = true;
    boolean TLS_ROLLBACK_ENABLED = true;
    boolean RENEGOTIATE_ON_CLIENT_AUTH_WANT = true;
    int MAX_CERT_LENGTH = 5;
    int DEFAULT_SSL_INACTIVITY_TIMEOUT = 30;
    String CLIENT_AUTH_PATTERN = "(|need|want)";
    String STORE_TYPE_PATTERN = "(JKS|NSS)";
    String PASSWORD_PROVIDER = "plain";
    String SSL2_CIPHERS_PATTERN =
            "((\\+|\\-)(rc2|rc2export|rc4|rc4export|idea|des|desede3)(\\s*,\\s*(\\+|\\-)(rc2|rc2export|rc4|rc4export|idea|des|desede3))*)*";
    long HANDSHAKE_TIMEOUT_MILLIS = -1;

    /**
     * Nickname of the server certificate in the certificate database or the PKCS#11 token. In the certificate, the name
     * format is token name:nickname. Including the token name: part of the name in this attribute is optional.
     */
    @Attribute
    String getCertNickname();

    void setCertNickname(String value);

    /**
     * Determines whether SSL3 client authentication is performed on every request, independent of ACL-based access
     * control.
     */
    @Attribute(defaultValue = "" + CLIENT_AUTH_ENABLED, dataType = Boolean.class)
    String getClientAuthEnabled();

    void setClientAuthEnabled(String value);

    /**
     * Determines if if the engine will request (want) or require (need) client authentication. Valid values:  want,
     * need, or left blank
     */
    @Attribute(dataType = String.class, defaultValue = "")
    @Pattern(regexp = CLIENT_AUTH_PATTERN)
    String getClientAuth();

    void setClientAuth(String value);

    @Attribute
    String getCrlFile();

    void setCrlFile(String crlFile);

    @Attribute
    String getKeyAlgorithm();

    void setKeyAlgorithm(String algorithm);

    /**
     * type of the keystore file
     */
    @Attribute(dataType = String.class)
    @Pattern(regexp = STORE_TYPE_PATTERN)
    String getKeyStoreType();

    void setKeyStoreType(String type);

    @Attribute(defaultValue= PASSWORD_PROVIDER)
    String getKeyStorePasswordProvider();

    void setKeyStorePasswordProvider(String provider);

    /**
     * password of the keystore file
     */
    @Attribute
    String getKeyStorePassword();

    void setKeyStorePassword(String password);

    /**
     * Location of the keystore file
     */
    @Attribute
    String getKeyStore();

    void setKeyStore(String location);

    @Attribute
    String getClassname();

    void setClassname(String value);

    /**
     * A comma-separated list of the SSL2 ciphers used, with the prefix + to enable or - to disable, for example +rc4.
     * Allowed values are rc4, rc4export, rc2, rc2export, idea, des, desede3. If no value is specified, all supported
     * ciphers are assumed to be enabled. NOT Used in PE
     */
    @Attribute
    @Pattern(regexp = SSL2_CIPHERS_PATTERN)
    String getSsl2Ciphers();

    void setSsl2Ciphers(String value);

    /**
     * Determines whether SSL2 is enabled. NOT Used in PE. SSL2 is not supported by either iiop or web-services. When
     * this element is used as a child of the iiop-listener element then the only allowed value for this attribute is
     * "false".
     */
    @Attribute(defaultValue = "" + SSL2_ENABLED, dataType = Boolean.class)
    String getSsl2Enabled();

    void setSsl2Enabled(String value);

    /**
     * Determines whether SSL3 is enabled. If both SSL2 and SSL3 are enabled for a virtual server, the server tries SSL3
     * encryption first. If that fails, the server tries SSL2 encryption.
     */
    @Attribute(defaultValue = "" + SSL3_ENABLED, dataType = Boolean.class)
    String getSsl3Enabled();

    void setSsl3Enabled(String value);

    /**
     * A comma-separated list of the SSL3 ciphers used, with the prefix + to enable or - to disable, for example
     * +SSL_RSA_WITH_RC4_128_MD5. Allowed SSL3/TLS values are those that are supported by the JVM for the given security
     * provider and security service configuration. If no value is specified, all supported ciphers are assumed to be
     * enabled.
     */
    @Attribute
    String getSsl3TlsCiphers();

    void setSsl3TlsCiphers(String value);

    /**
     * Determines whether TLS is enabled.
     */
    @Attribute(defaultValue = "" + TLS_ENABLED, dataType = Boolean.class)
    String getTlsEnabled();

    void setTlsEnabled(String value);

    /**
     * Determines whether TLS 1.1 is enabled.
     */
    @Attribute(defaultValue = "" + TLS11_ENABLED, dataType = Boolean.class)
    String getTls11Enabled();

    void setTls11Enabled(String value);

    /**
     * Determines whether TLS 1.2 is enabled.
     */
    @Attribute(defaultValue = "" + TLS12_ENABLED, dataType = Boolean.class)
    String getTls12Enabled();

    void setTls12Enabled(String value);

    /**
     * Determines whether TLS rollback is enabled. TLS rollback should be enabled for Microsoft Internet Explorer 5.0
     * and 5.5. NOT Used in PE
     */
    @Attribute(defaultValue = "" + TLS_ROLLBACK_ENABLED, dataType = Boolean.class)
    String getTlsRollbackEnabled();

    void setTlsRollbackEnabled(String value);

    @Attribute
    String getTrustAlgorithm();

    void setTrustAlgorithm(String algorithm);

    @Attribute(dataType = Integer.class, defaultValue = "" + MAX_CERT_LENGTH)
    String getTrustMaxCertLength();

    void setTrustMaxCertLength(String maxLength);

    @Attribute
    String getTrustStore();

    void setTrustStore(String location);

    /**
     * type of the truststore file
     */
    @Attribute(dataType = String.class)
    @Pattern(regexp = STORE_TYPE_PATTERN)
    String getTrustStoreType();

    void setTrustStoreType(String type);

    @Attribute(defaultValue= PASSWORD_PROVIDER)
    String getTrustStorePasswordProvider();

    void setTrustStorePasswordProvider(String provider);

    /**
     * password of the truststore file
     */
    @Attribute
    String getTrustStorePassword();

    void setTrustStorePassword(String password);

    /**
     * Does SSL configuration allow implementation to initialize it lazily way
     */
    @Attribute(defaultValue = "" + ALLOW_LAZY_INIT, dataType = Boolean.class)
    String getAllowLazyInit();

    void setAllowLazyInit(String value);

    /**
     * @return the timeout within which there must be activity from the client.
     *  Defaults to {@value #DEFAULT_SSL_INACTIVITY_TIMEOUT} seconds.
     */
    @Attribute(defaultValue = "" + DEFAULT_SSL_INACTIVITY_TIMEOUT, dataType = Integer.class)
    String getSSLInactivityTimeout();

    void setSSLInactivityTimeout(int handshakeTimeout);


    /**
     * <p>
     * Determines whether or not ssl session renegotiation will occur if
     * client-auth is set to want.  This may be set to <code>false</code> under
     * the assumption that if a certificate wasn't available during the initial
     * handshake, it won't be available during a renegotiation.
     * </p>
     *
     * <p>
     * This configuration option defaults to <code>true</code>.
     * </p>
     * @return <code>true</code> if ssl session renegotiation will occur if
     *  client-auth is want.
     *
     * @since 2.1.2
     */
    @Attribute(defaultValue = "" + RENEGOTIATE_ON_CLIENT_AUTH_WANT, dataType = Boolean.class)
    String getRenegotiateOnClientAuthWant();


    /**
     * @since 2.1.2
     */
    void setRenegotiateOnClientAuthWant(boolean renegotiateClientAuthWant);
    
    /**
     * Handshake mode
     */
    @Attribute(defaultValue="" + HANDSHAKE_TIMEOUT_MILLIS, dataType = Long.class)
    String getHandshakeTimeoutMillis();

    void setHandshakeTimeoutMillis(String timeoutMillis);

}
