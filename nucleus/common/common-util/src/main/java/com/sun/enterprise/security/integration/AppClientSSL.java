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

package com.sun.enterprise.security.integration;

/**
 *
 * @author Kumar
 */
public class AppClientSSL {
    private boolean tlsEnabled = true;
    private boolean tlsRollbackEnabled = true;
    private String ssl3TlsCiphers;
    private boolean ssl3Enabled = true;
    private String certNickName;
    private boolean ssl2Enabled = false;
    private String ssl2Ciphers;
    private boolean clientAuthEnabled = false;
    
    /**
     * Gets the value of the certNickname property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getCertNickname(){
        return this.certNickName;
    }

    /**
     * Sets the value of the certNickname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCertNickname(String value) {
        this.certNickName = value;
    }

    /**
     * Gets the value of the ssl2Enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    public boolean getSsl2Enabled() {
        return this.ssl2Enabled;
    }

    /**
     * Sets the value of the ssl2Enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSsl2Enabled(String value) {
        this.ssl2Enabled = Boolean.parseBoolean(value);
    }
    public void setSsl2Enabled(boolean value) {
        this.ssl2Enabled = value;
    }

    /**
     * Gets the value of the ssl2Ciphers property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getSsl2Ciphers() {
        return ssl2Ciphers;
    }

    /**
     * Sets the value of the ssl2Ciphers property.
     * Values:  rc4, rc4export, rc2, rc2export, idea, des, desede3.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSsl2Ciphers(String value)  {
        this.ssl2Ciphers = value;
    }

    /**
     * Gets the value of the ssl3Enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    public boolean getSsl3Enabled() {
        return this.ssl3Enabled;
    }

    /**
     * Sets the value of the ssl3Enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSsl3Enabled(String value) {
        this.ssl3Enabled = Boolean.parseBoolean(value);
    }
    
    public void setSsl3Enabled(boolean value) {
        this.ssl3Enabled = value;
    }

    /**
     A comma-separated list of the SSL3 ciphers used, with the prefix + to enable or - to 
        disable, for example +SSL_RSA_WITH_RC4_128_MD5 . Allowed values are 
        SSL_RSA_WITH_RC4_128_MD5, SSL_RSA_WITH_3DES_EDE_CBC_SHA, SSL_RSA_WITH_DES_CBC_SHA, 
        SSL_RSA_EXPORT_WITH_RC4_40_MD5, SSL_RSA_WITH_NULL_MD5, SSL_RSA_WITH_RC4_128_SHA, and 
        SSL_RSA_WITH_NULL_SHA. Values available in previous releases are supported for backward 
        compatibility.
     */
    public String getSsl3TlsCiphers() {
        return ssl3TlsCiphers;
    }

    /**
     * Sets the value of the ssl3TlsCiphers property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSsl3TlsCiphers(String value) {
        this.ssl3TlsCiphers = value;
    }

    /**
     * Gets the value of the tlsEnabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    public boolean getTlsEnabled() {
        return this.tlsEnabled;
    }

    /**
     * Sets the value of the tlsEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTlsEnabled(String value) {
        this.tlsEnabled = Boolean.parseBoolean(value);
    }
    
    public void setTlsEnabled(boolean value) {
        this.tlsEnabled = value;
    }

    /**
     * Gets the value of the tlsRollbackEnabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    public boolean getTlsRollbackEnabled() {
        return this.tlsRollbackEnabled;
    }

    /**
     * Sets the value of the tlsRollbackEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTlsRollbackEnabled(String value) {
        this.tlsRollbackEnabled = Boolean.parseBoolean(value);
    }
    public void setTlsRollbackEnabled(boolean value) {
        this.tlsRollbackEnabled = value;
    }

    /**
     * Gets the value of the clientAuthEnabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    public boolean getClientAuthEnabled() {
        return this.clientAuthEnabled;
    }

    /**
     * Sets the value of the clientAuthEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setClientAuthEnabled(String value) {
        this.clientAuthEnabled = Boolean.parseBoolean(value);
    }
    public void setClientAuthEnabled(boolean value) {
        this.clientAuthEnabled = value;
    }

}
