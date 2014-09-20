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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.connector;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;


/**
 * This socket factory holds secure socket factory parameters. Besides the usual
 * configuration mechanism based on setting JavaBeans properties, this
 * component may also be configured by passing a series of attributes set
 * with calls to <code>setAttribute()</code>.  The following attribute
 * names are recognized, with default values in square brackets:
 * <ul>
 * <li><strong>algorithm</strong> - Certificate encoding algorithm
 *     to use. [SunX509]</li>
 * <li><strong>clientAuth</strong> - Require client authentication if
 *     set to <code>true</code>. [false]</li>
 * <li><strong>keystoreFile</strong> - Pathname to the Key Store file to be
 *     loaded.  This must be an absolute path, or a relative path that
 *     is resolved against the "catalina.base" system property.
 *     ["./keystore" in the user home directory]</li>
 * <li><strong>keystorePass</strong> - Password for the Key Store file to be
 *     loaded. ["changeit"]</li>
 * <li><strong>keystoreType</strong> - Type of the Key Store file to be
 *     loaded. ["JKS"]</li>
 * <li><strong>protocol</strong> - SSL protocol to use. [TLS]</li>
 * </ul>
 *
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Craig McClanahan
 */

public class CoyoteServerSocketFactory
    implements org.apache.catalina.net.ServerSocketFactory {

    private String algorithm = null;
    private boolean clientAuth = false;
    private String keystoreFile =
        System.getProperty("user.home") + File.separator + ".keystore";
    private String randomFile =
        System.getProperty("user.home") + File.separator + "random.pem";
    private String rootFile =
        System.getProperty("user.home") + File.separator + "root.pem";
    private String keystorePass = "changeit";
    private String keystoreType = "JKS";
    private String protocol = "TLS";
    private String protocols;
    private String sslImplementation = null;
    private String cipherSuites;
    private String keyAlias;

    // ------------------------------------------------------------- Properties

    /**
     * Gets the certificate encoding algorithm to be used.
     *
     * @return Certificate encoding algorithm
     */
    public String getAlgorithm() {
        return (this.algorithm);
    }

    /**
     * Sets the certificate encoding algorithm to be used.
     *
     * @param algorithm Certificate encoding algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Provides information about whether client authentication is enforced.
     *
     * @return true if client authentication is enforced, false otherwise
     */
    public boolean getClientAuth() {
        return (this.clientAuth);
    }

    /**
     * Sets the requirement of client authentication.
     *
     * @param clientAuth true if client authentication is enforced, false
     * otherwise
     */
    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    /**
     * Gets the pathname to the keystore file.
     *
     * @return Pathname to the keystore file
     */
    public String getKeystoreFile() {
        return (this.keystoreFile);
    }

    /**
     * Sets the pathname to the keystore file.
     *
     * @param keystoreFile Pathname to the keystore file
     */
    public void setKeystoreFile(String keystoreFile) {
      
        File file = new File(keystoreFile);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            keystoreFile);
        this.keystoreFile = file.getAbsolutePath();
    }

    /**
     * Gets the pathname to the random file.
     *
     * @return Pathname to the random file
     */
    public String getRandomFile() {
        return (this.randomFile);
    }

    /**
     * Sets the pathname to the random file.
     *
     * @param randomFile Pathname to the random file
     */
    public void setRandomFile(String randomFile) {
      
        File file = new File(randomFile);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            randomFile);
        this.randomFile = file.getAbsolutePath();
    }

    /**
     * Gets the pathname to the root list.
     *
     * @return Pathname to the root list
     */
    public String getRootFile() {
        return (this.rootFile);
    }

    /**
     * Sets the pathname to the root list.
     *
     * @param rootFile Pathname to the root list
     */
    public void setRootFile(String rootFile) {
      
        File file = new File(rootFile);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            rootFile);
        this.rootFile = file.getAbsolutePath();
    }
     
    /**
     * Gets the keystore password.
     *
     * @return Keystore password
     */
    public String getKeystorePass() {
        return (this.keystorePass);
    }

    /**
     * Sets the keystore password.
     *
     * @param keystorePass Keystore password
     */
    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }

    /**
     * Gets the keystore type.
     *
     * @return Keystore type
     */
    public String getKeystoreType() {
        return (this.keystoreType);
    }

    /**
     * Sets the keystore type.
     *
     * @param keystoreType Keystore type
     */
    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    /**
     * Gets the SSL protocol variant to be used.
     *
     * @return SSL protocol variant
     */
    public String getProtocol() {
        return (this.protocol);
    }

    /**
     * Sets the SSL protocol variant to be used.
     *
     * @param protocol SSL protocol variant
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the SSL protocol variants to be enabled.
     *
     * @return Comma-separated list of SSL protocol variants
     */
    public String getProtocols() {
        return this.protocols;
    }

    /**
     * Sets the SSL protocol variants to be enabled.
     *
     * @param protocols Comma-separated list of SSL protocol variants
     */
    public void setProtocols(String protocols) {
        this.protocols = protocols;
    }

    /**
     * Gets the name of the SSL implementation to be used.
     *
     * @return SSL implementation name
     */
    public String getSSLImplementation() {
        return (this.sslImplementation);
    }

    /**
     * Sets the name of the SSL implementation to be used.
     *
     * @param sslImplementation SSL implementation name
     */
    public void setSSLImplementation(String sslImplementation) {
        this.sslImplementation = sslImplementation;
    }

    /**
     * Gets the alias name of the keypair and supporting certificate chain
     * used by the server to authenticate itself to SSL clients.
     *
     * @return The alias name of the keypair and supporting certificate chain
     */
    public String getKeyAlias() {
        return this.keyAlias;
    }

    /**
     * Sets the alias name of the keypair and supporting certificate chain
     * used by the server to authenticate itself to SSL clients.
     *
     * @param alias The alias name of the keypair and supporting certificate
     * chain
     */
    public void setKeyAlias(String alias) {
        this.keyAlias = alias;
    }

    /**
     * Gets the list of SSL cipher suites that are to be enabled
     *
     * @return Comma-separated list of SSL cipher suites, or null if all
     * cipher suites supported by the underlying SSL implementation are being
     * enabled
     */
    public String getCiphers() {
	return this.cipherSuites;
    }

    /**
     * Sets the SSL cipher suites that are to be enabled.
     *
     * Only those SSL cipher suites that are actually supported by
     * the underlying SSL implementation will be enabled.
     *
     * @param ciphers Comma-separated list of SSL cipher suites
     */
    public void setCiphers(String ciphers) {
	this.cipherSuites = ciphers;
    }


    // --------------------------------------------------------- Public Methods


    public ServerSocket createSocket(int port) {
        return (null);
    }


    public ServerSocket createSocket(int port, int backlog) {
        return (null);
    }


    public ServerSocket createSocket(int port, int backlog,
                                     InetAddress ifAddress) {
        return (null);
    }


}
