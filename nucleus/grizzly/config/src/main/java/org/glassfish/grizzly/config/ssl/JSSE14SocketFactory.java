/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPathParameters;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.glassfish.grizzly.http.util.StringManager;

/**
 * SSL server socket factory. It _requires_ a valid RSA key and JSSE.
 *
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Stefan Freyr Stefansson
 * @author EKR -- renamed to JSSESocketFactory
 * @author Jan Luehe
 */
public class JSSE14SocketFactory extends JSSESocketFactory {
    private static final StringManager sm = StringManager.getManager(
            JSSE14SocketFactory.class.getPackage().getName(),
            JSSE14SocketFactory.class.getClassLoader());

    public JSSE14SocketFactory() {
    }

    /**
     * Reads the keystore and initializes the SSL socket factory.
     */
    public void init() throws IOException {
        try {
            clientAuthNeed = Boolean.valueOf((String) attributes.get("clientAuthNeed"));
            clientAuthWant = Boolean.valueOf((String) attributes.get("clientAuthWant"));
            
            // SSL protocol variant (e.g., TLS, SSL v3, etc.)
            String protocol = (String) attributes.get("protocol");
            if (protocol == null) {
                protocol = defaultProtocol;
            }
            // Certificate encoding algorithm (e.g., SunX509)
            String algorithm = (String) attributes.get("algorithm");
            if (algorithm == null) {
                algorithm = defaultAlgorithm;
            }
            // Create and init SSLContext
            /* SJSAS 6439313
            SSLContext context = SSLContext.getInstance(protocol);
             */
            // START SJSAS 6439313
            context = SSLContext.getInstance(protocol);
            // END SJSAS 6439313 
            // Configure SSL session timeout and cache size
            configureSSLSessionContext(context.getServerSessionContext());
            String trustAlgorithm = (String) attributes.get("truststoreAlgorithm");
            if (trustAlgorithm == null) {
                trustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            }
            context.init(getKeyManagers(algorithm,
                (String) attributes.get("keyAlias")),
                getTrustManagers(trustAlgorithm),
                new SecureRandom());
            // create proxy
            sslProxy = context.getServerSocketFactory();
            // Determine which cipher suites to enable
            String requestedCiphers = (String) attributes.get("ciphers");
            if (requestedCiphers != null) {
                enabledCiphers = getEnabledCiphers(requestedCiphers,
                    sslProxy.getSupportedCipherSuites());
            }
            // Check the SSL config is ok
            checkConfig();

        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Gets the initialized key managers.
     */
    protected KeyManager[] getKeyManagers(String algorithm,
        String keyAlias)
        throws Exception {
        KeyManager[] kms;
        String keystorePass = getKeystorePassword();
        KeyStore ks = getKeystore(keystorePass);
        if (keyAlias != null && !ks.isKeyEntry(keyAlias)) {
            throw new IOException(sm.getString("jsse.alias_no_key_entry", keyAlias));
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, keystorePass.toCharArray());
        kms = kmf.getKeyManagers();
        if (keyAlias != null) {
            for (int i = 0; i < kms.length; i++) {
                kms[i] = new JSSEKeyManager((X509KeyManager) kms[i], keyAlias);
            }
        }
        return kms;
    }

    /**
     * Gets the initialized trust managers.
     */
    protected TrustManager[] getTrustManagers(String algorithm) throws Exception {
        String crlFile = (String) attributes.get("crlFile");
        TrustManager[] tms = null;
        KeyStore trustStore = getTrustStore();
        if (trustStore != null) {
            if (crlFile == null) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(trustStore);
                tms = tmf.getTrustManagers();
            } else {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                CertPathParameters params = getParameters(algorithm, crlFile, trustStore);
                ManagerFactoryParameters mfp = new CertPathTrustManagerParameters(params);
                tmf.init(mfp);
                tms = tmf.getTrustManagers();
            }
        }
        return tms;
    }

    /**
     * Return the initialization parameters for the TrustManager. Currently, only the default <code>PKIX</code> is
     * supported.
     *
     * @param algorithm The algorithm to get parameters for.
     * @param crlf The path to the CRL file.
     * @param trustStore The configured TrustStore.
     *
     * @return The parameters including the CRLs and TrustStore.
     */
    protected CertPathParameters getParameters(String algorithm, String crlf, KeyStore trustStore) throws Exception {
        CertPathParameters params;
        if ("PKIX".equalsIgnoreCase(algorithm)) {
            PKIXBuilderParameters xparams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
            Collection crls = getCRLs(crlf);
            CertStoreParameters csp = new CollectionCertStoreParameters(crls);
            CertStore store = CertStore.getInstance("Collection", csp);
            xparams.addCertStore(store);
            xparams.setRevocationEnabled(true);
            String trustLength = (String) attributes.get("trustMaxCertLength");
            if (trustLength != null) {
                try {
                    xparams.setMaxPathLength(Integer.parseInt(trustLength));
                } catch (Exception ex) {
                    logger.warning("Bad maxCertLength: " + trustLength);
                }
            }
            params = xparams;
        } else {
            throw new CRLException("CRLs not supported for type: " + algorithm);
        }
        return params;
    }

    /**
     * Load the collection of CRLs.
     */
    protected Collection<? extends CRL> getCRLs(String crlf) throws IOException, CRLException, CertificateException {
        File crlFile = new File(crlf);
        if (!crlFile.isAbsolute()) {
            crlFile = new File(System.getProperty("catalina.base"), crlf);
        }
        Collection<? extends CRL> crls = null;
        InputStream is = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            is = new FileInputStream(crlFile);
            crls = cf.generateCRLs(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
        return crls;
    }

    protected void setEnabledProtocols(SSLServerSocket socket, String[] protocols) {
        if (protocols != null) {
            socket.setEnabledProtocols(protocols);
        }
    }

    protected String[] getEnabledProtocols(SSLServerSocket socket, String requestedProtocols) {
        String[] supportedProtocols = socket.getSupportedProtocols();
        String[] enabledProtocols = null;
        if (requestedProtocols != null) {
            List<String> vec = null;
            String protocol = requestedProtocols;
            int index = requestedProtocols.indexOf(',');
            if (index != -1) {
                int fromIndex = 0;
                while (index != -1) {
                    protocol = requestedProtocols.substring(fromIndex, index).trim();
                    if (supportedProtocols != null && protocol.length() > 0) {
                        /*
                         * Check to see if the requested protocol is among the
                         * supported protocols, i.e., may be enabled
                         */
                        for (String supportedProtocol : supportedProtocols) {
                            if (supportedProtocol.equals(protocol)) {
                                if (vec == null) {
                                    vec = new ArrayList<String>();
                                }
                                vec.add(protocol);
                                break;
                            }
                        }
                    }
                    fromIndex = index + 1;
                    index = requestedProtocols.indexOf(',', fromIndex);
                } // while
                protocol = requestedProtocols.substring(fromIndex);
            }
            
            assert protocol != null;
            
            protocol = protocol.trim();
            if (protocol.length() > 0 && supportedProtocols != null) {
                /*
                 * Check to see if the requested protocol is among the
                 * supported protocols, i.e., may be enabled
                 */
                for (String supportedProtocol : supportedProtocols) {
                    if (supportedProtocol.equals(protocol)) {
                        if (vec == null) {
                            vec = new ArrayList<String>();
                        }
                        vec.add(protocol);
                        break;
                    }
                }
            }
            if (vec != null) {
                enabledProtocols = vec.toArray(new String[vec.size()]);
            }
        }
        return enabledProtocols;
    }
    /**
    * Configures the given SSLSessionContext.
    *
    * @param sslSessionCtxt The SSLSessionContext to configure
    */

    private void configureSSLSessionContext(SSLSessionContext sslSessionCtxt) {
        String attrValue = (String) attributes.get("sslSessionTimeout");
        if (attrValue != null) {
            sslSessionCtxt.setSessionTimeout(
                Integer.parseInt(attrValue));
        }
        attrValue = (String) attributes.get("ssl3SessionTimeout");
        if (attrValue != null) {
            sslSessionCtxt.setSessionTimeout(
                Integer.parseInt(attrValue));
        }
        attrValue = (String) attributes.get("sslSessionCacheSize");
        if (attrValue != null) {
            sslSessionCtxt.setSessionCacheSize(
                Integer.parseInt(attrValue));
        }
    }

    /**
     * Checks that the certificate is compatible with the enabled cipher suites. If we don't check now, the JIoEndpoint
     * can enter a nasty logging loop. See bug 45528.
     */
    private void checkConfig() throws IOException {
        // Create an unbound server socket
        ServerSocket socket = sslProxy.createServerSocket();
        initServerSocket(socket);
        try {
            // Set the timeout to 1ms as all we care about is if it throws an
            // SSLException on accept. 
            socket.setSoTimeout(1);
            socket.accept();
            // Will never get here - no client can connect to an unbound port
        } catch (SSLException ssle) {
            // SSL configuration is invalid. Possibly cert doesn't match ciphers
            IOException ioe = new IOException(sm.getString("jsse.invalid_ssl_conf", ssle.getMessage()));
            ioe.initCause(ssle);
            throw ioe;
        } catch (Exception e) {
            /*
             * Possible ways of getting here
             * socket.accept() throws a SecurityException
             * socket.setSoTimeout() throws a SocketException
             * socket.accept() throws some other exception (after a JDK change)
             *      In these cases the test won't work so carry on - essentially
             *      the behaviour before this patch
             * socket.accept() throws a SocketTimeoutException
             *      In this case all is well so carry on
             */
        } finally {
            // Should be open here but just in case
            if (!socket.isClosed()) {
                socket.close();
            }
        }

    }
}
