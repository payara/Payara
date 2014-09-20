/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.embeddable.web.config;

import java.io.File;
import java.util.Set;

/**
 * Class used to configure SSL processing parameters
 * 
 * @author Rajiv Mordani
 */
public class SslConfig {

    private String keyStore;
    private String trustStore;
    private char[] keyPassword;
    private char[] trustPassword;
    private int timeoutMilliSeconds;
    private Set<SslType> algorithms;
    private String certNickname;

    /**
     * Create an instance of <tt>SslConfig</tt>.
     *
     * @param key the location of the keystore file
     * @param trust the location of the truststore file
     */
    public SslConfig(String key, String trust) {
        this.keyStore = key;
        this.trustStore = trust;
    }

    /**
     * Sets the location of the keystore file
     *
     * @param keyStore The location of the keystore file
     */
    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Sets the location of the truststore file
     *
     * @param trustStore The location of the truststore file
     */
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    /**
     * Sets the password of the keystore file
     *
     * @param keyPassword The password of the keystore file
     */
    public void setKeyPassword(char[] keyPassword) {
        this.keyPassword = keyPassword;
    }

    /**
     * Sets the password of the truststore file
     *
     * @param trustPassword The password of the truststore file
     */
    public void setTrustPassword(char[] trustPassword) {
        this.trustPassword = trustPassword;
    }

    /**
     * Sets the timeout within which there must be activity from the client
     *
     * @param timeoutMilliSeconds The timeout in milliseconds
     */
    public void setHandshakeTimeout(int timeoutMilliSeconds) {
        this.timeoutMilliSeconds = timeoutMilliSeconds;
    }

    /**
     * Sets the algorithm
     * 
     * @param algorithms
     */
    public void setAlgorithms(Set<SslType> algorithms) {
        this.algorithms = algorithms;
    }

    /**
     * Gets the location of the keystore file
     *
     * @return the location of the keystore file
     */
    public String getKeyStore() {
        return this.keyStore;
    }

    /**
     * Gets the truststore file location
     *
     * @return the location of the truststore file
     */
    public String getTrustStore() {
        return this.trustStore;
    }

    /**
     * Gets the password of the keystore file
     *
     * @return the password of the keystore file
     */
    public char[] getKeyPassword() {
        return this.keyPassword;
    }

    /**
     * Gets the password of the truststore file
     *
     * @return the password of the truststore file
     */
    public char[] getTrustPassword() {
        return this.trustPassword;
    }

    /**
     * Gets the timeout within which there must be activity from the client
     *
     * @return the timeout in milliseconds
     */
    public int getHandshakeTimeout() {
        return this.timeoutMilliSeconds;
    }

    /**
     * Sets the algorithm
     *
     * @return the algorithm
     */
    public Set<SslType> getAlgorithms() {
        return this.algorithms;
    }

    /**
     * Gets the nickname of the server certificate in the certificate database
     *
     * @return the certNickname 
     */
    public String getCertNickname() {
       return this.certNickname;
    }

    /**
     * Sets the certNickname
     *
     */
    public void setCertNickname(String value) {
        this.certNickname = value;
    }

}
