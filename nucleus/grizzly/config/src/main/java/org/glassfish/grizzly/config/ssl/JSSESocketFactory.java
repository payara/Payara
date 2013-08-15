/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import org.glassfish.grizzly.config.GrizzlyConfig;

import org.glassfish.grizzly.http.util.StringManager;

/**
 * SSL server socket factory. It _requires_ a valid RSA key and JSSE.
 *
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Stefan Freyr Stefansson
 * @author EKR -- renamed to JSSESocketFactory
 */
public abstract class JSSESocketFactory extends ServerSocketFactory {
    private static final StringManager sm = StringManager.getManager(
            JSSESocketFactory.class.getPackage().getName(),
            JSSESocketFactory.class.getClassLoader());
    public final static String defaultProtocol = "TLS";
    public final static String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
    final static boolean defaultClientAuth = false;
    private static final String defaultKeyPass = "changeit";
    protected static final Logger logger = GrizzlyConfig.logger();
    protected boolean initialized;
    protected boolean clientAuthNeed = false;
    protected boolean clientAuthWant = false;
    protected SSLServerSocketFactory sslProxy = null;
    protected String[] enabledCiphers;

    public JSSESocketFactory() {
    }

    @Override
    public ServerSocket createSocket(int port) throws IOException {
        if (!initialized) {
            init();
        }
        ServerSocket socket = sslProxy.createServerSocket(port);
        initServerSocket(socket);
        return socket;
    }

    @Override
    public ServerSocket createSocket(int port, int backlog) throws IOException {
        if (!initialized) {
            init();
        }
        ServerSocket socket = sslProxy.createServerSocket(port, backlog);
        initServerSocket(socket);
        return socket;
    }

    @Override
    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        if (!initialized) {
            init();
        }
        ServerSocket socket = sslProxy.createServerSocket(port, backlog, ifAddress);
        initServerSocket(socket);
        return socket;
    }

    @Override
    public Socket acceptSocket(ServerSocket socket) throws IOException {
        Socket asock;
        try {
            asock = socket.accept();
            assert asock instanceof SSLSocket;
            
            if(clientAuthNeed) {
                ((SSLSocket) asock).setNeedClientAuth(clientAuthNeed);
            } else {
                ((SSLSocket) asock).setWantClientAuth(clientAuthWant);
            }
        } catch (SSLException e) {
            throw new SocketException("SSL handshake error" + e.toString());
        }
        return asock;
    }

    @Override
    public void handshake(Socket sock) throws IOException {
        if (!(sock instanceof SSLSocket)) {
            throw new IllegalArgumentException("The Socket has to be SSLSocket");
        }
        
        ((SSLSocket) sock).startHandshake();
    }

    /**
     * Determines the SSL cipher suites to be enabled.
     *
     * @param requestedCiphers Comma-separated list of requested ciphers
     * @param supportedCiphers Array of supported ciphers
     *
     * @return Array of SSL cipher suites to be enabled, or null if none of the requested ciphers are supported
     */
    protected String[] getEnabledCiphers(String requestedCiphers, String[] supportedCiphers) {
        String[] enabled = null;
        if (requestedCiphers != null) {
            List<String> vec = null;
            String cipher = requestedCiphers;
            int index = requestedCiphers.indexOf(',');
            if (index != -1) {
                int fromIndex = 0;
                while (index != -1) {
                    cipher = requestedCiphers.substring(fromIndex, index).trim();
                    if (cipher.length() > 0) {
                        /*
                         * Check to see if the requested cipher is among the
                         * supported ciphers, i.e., may be enabled
                         */
                        for (int i = 0; supportedCiphers != null && i < supportedCiphers.length; i++) {
                            if (supportedCiphers[i].equals(cipher)) {
                                if (vec == null) {
                                    vec = new ArrayList<String>();
                                }
                                vec.add(cipher);
                                break;
                            }
                        }
                    }
                    fromIndex = index + 1;
                    index = requestedCiphers.indexOf(',', fromIndex);
                } // while
                cipher = requestedCiphers.substring(fromIndex);
            }
            
            assert cipher != null;
            
            cipher = cipher.trim();
            if (cipher.length() > 0) {
                /*
                 * Check to see if the requested cipher is among the
                 * supported ciphers, i.e., may be enabled
                 */
                for (int i = 0; supportedCiphers != null
                    && i < supportedCiphers.length; i++) {
                    if (supportedCiphers[i].equals(cipher)) {
                        if (vec == null) {
                            vec = new ArrayList<String>();
                        }
                        vec.add(cipher);
                        break;
                    }
                }
            }
            if (vec != null) {
                enabled = vec.toArray(new String[vec.size()]);
            }
        }
        return enabled;
    }

    /**
     * Gets the SSL server's keystore password.
     */
    protected String getKeystorePassword() {
        String keyPass = (String) attributes.get("keypass");
        if (keyPass == null) {
            keyPass = defaultKeyPass;
        }
        String keystorePass = (String) attributes.get("keystorePass");
        if (keystorePass == null) {
            keystorePass = keyPass;
        }
        return keystorePass;
    }

    /**
     * Gets the SSL server's keystore.
     */
    protected KeyStore getKeystore(String pass) throws IOException {
        String keystoreFile = (String) attributes.get("keystore");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Keystore file= {0}", keystoreFile);
        }
        String keystoreType = (String) attributes.get("keystoreType");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Keystore type= {0}", keystoreType);
        }
        return getStore(keystoreType, keystoreFile, pass);
    }
    /*
    * Gets the SSL server's truststore password.
    */

    protected String getTruststorePassword() {
        String truststorePassword = (String) attributes.get("truststorePass");
        if (truststorePassword == null) {
            truststorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
            if (truststorePassword == null) {
                truststorePassword = getKeystorePassword();
            }
        }
        return truststorePassword;
    }

    /**
     * Gets the SSL server's truststore.
     */
    protected KeyStore getTrustStore() throws IOException {
        KeyStore ts = null;
        String truststore = (String) attributes.get("truststore");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Truststore file= {0}", truststore);
        }
        String truststoreType = (String) attributes.get("truststoreType");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Truststore type= {0}", truststoreType);
        }
        String truststorePassword = getTruststorePassword();
        if (truststore != null && truststorePassword != null) {
            ts = getStore(truststoreType, truststore, truststorePassword);
        }
        return ts;
    }

    /**
     * Gets the key- or truststore with the specified type, path, and password.
     */
    private KeyStore getStore(String type, String path, String pass) throws IOException {
        KeyStore ks = null;
        InputStream istream = null;
        try {
            ks = KeyStore.getInstance(type);
            if (!("PKCS11".equalsIgnoreCase(type) ||
                "".equalsIgnoreCase(path))) {
                File keyStoreFile = new File(path);
                if (!keyStoreFile.isAbsolute()) {
                    keyStoreFile = new File(System.getProperty("catalina.base"),
                        path);
                }
                istream = new FileInputStream(keyStoreFile);
            }
            ks.load(istream, pass.toCharArray());
        } catch (FileNotFoundException fnfe) {
            logger.log(Level.SEVERE, sm.getString("jsse.keystore_load_failed", type, path, fnfe.getMessage()), fnfe);
            throw fnfe;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, sm.getString("jsse.keystore_load_failed", type, path, ioe.getMessage()), ioe);
            throw ioe;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, sm.getString("jsse.keystore_load_failed", type, path, ex.getMessage()), ex);
            throw new IOException(sm.getString("jsse.keystore_load_failed", type, path, ex.getMessage()));
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }
        return ks;
    }

    /**
     * Reads the keystore and initializes the SSL socket factory.
     *
     * Place holder method to initialize the KeyStore, etc.
     */
    @Override
    public abstract void init() throws IOException;

    /**
     * Determines the SSL protocol variants to be enabled.
     *
     * @param socket The socket to get supported list from.
     * @param requestedProtocols Comma-separated list of requested SSL protocol variants
     *
     * @return Array of SSL protocol variants to be enabled, or null if none of the requested protocol variants are
     *         supported
     */
    abstract protected String[] getEnabledProtocols(SSLServerSocket socket, String requestedProtocols);

    /**
     * Set the SSL protocol variants to be enabled.
     *
     * @param socket the SSLServerSocket.
     * @param protocols the protocols to use.
     */
    abstract protected void setEnabledProtocols(SSLServerSocket socket, String[] protocols);

    /**
     * Configures the given SSL server socket with the requested cipher suites, protocol versions, and need for client
     * authentication
     */
    protected void initServerSocket(ServerSocket ssocket) {
        if (!(ssocket instanceof SSLServerSocket)) {
            throw new IllegalArgumentException("The ServerSocket has to be SSLServerSocket");
        }
        
        SSLServerSocket socket = (SSLServerSocket) ssocket;
        if (attributes.get("ciphers") != null) {
            socket.setEnabledCipherSuites(enabledCiphers);
        }
        String requestedProtocols = (String) attributes.get("protocols");
        setEnabledProtocols(socket, getEnabledProtocols(socket,
            requestedProtocols));
        // we don't know if client auth is needed -
        // after parsing the request we may re-handshake
        if(clientAuthNeed) {
            socket.setNeedClientAuth(clientAuthNeed);
        } else {
            socket.setWantClientAuth(clientAuthWant);
        }
    }
}
