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

package org.glassfish.admin.mbeanserver.ssl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.rmi.ssl.SslRMIClientSocketFactory;


    /**
     * Inner class for SSL support for JMX connection using RMI.
     */
    public final class SecureRMIClientSocketFactory
            extends SslRMIClientSocketFactory  {

        private transient InetAddress mAddress;
        private  transient SSLParams sslParams;
        // The list of cipher suite
        private volatile String[] enabledCipherSuites = null;
        //the list of protocols
        private volatile String[] enabledProtocols = null;
        private transient Object cipherSuitesSync = new Object();
        private transient Object protocolsSync = new Object();
        private transient Map socketMap = new HashMap<Integer, Socket>();
        private transient Logger  _logger = Logger.getLogger(SecureRMIClientSocketFactory.class.getName());

        public SecureRMIClientSocketFactory(final SSLParams sslParams,
                final InetAddress addr) {
            super();
            mAddress = addr;
            this.sslParams = sslParams;
            if(sslParams != null) {
                _logger.log(Level.INFO, "Creating a SecureRMIClientSocketFactory @ " +
                    addr.getHostAddress() + "with ssl config = " + sslParams.toString());
            }
        }

        public SecureRMIClientSocketFactory() {

        }


        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SecureRMIClientSocketFactory) {
                 return(this.hashCode()==obj.hashCode());
            } else  {
                return false;
            }
        }

        @Override
        public int hashCode() {
             return mAddress.hashCode();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            //debug( "MyRMIServerSocketFactory.createServerSocket(): " + mAddress + " : " + port );
            if(socketMap.containsKey(new Integer(port))) {
                return (Socket)socketMap.get(new Integer(port));
            }
           /* final int backlog = 5;  // plenty
            // we use a custom class here. The reason is mentioned in the class.
            final JMXSslConfigHolder sslConfigHolder;
            try {
                sslConfigHolder = new JMXSslConfigHolder(ssl);
            } catch (SSLException ssle) {
                throw new IllegalStateException(ssle);
            }

            sslConfigHolder.configureSSL();
            final SSLContext context = sslConfigHolder.getSSLContext();

            SSLSocket sslSocket =
                    (SSLSocket) context.getSocketFactory().createSocket(mAddress, port);
            configureSSLSocket(sslSocket, sslConfigHolder);
            Util.getLogger().info("SSLSocket " +
                    sslSocket.getLocalSocketAddress() + "and" + sslSocket.toString()+ " created"); */
            final int backlog = 5;

            SSLClientConfigurator sslCC = SSLClientConfigurator.getInstance();
            _logger.log(Level.INFO, "Setting SSLParams @ " +
                     sslParams);
            sslCC.setSSLParams(sslParams);
            SSLContext sslContext = sslCC.configure(sslParams);
            SSLSocket sslSocket =
                    (SSLSocket)sslContext.getSocketFactory().createSocket(mAddress, port);
            configureSSLSocket(sslSocket, sslCC);

            //sslSocket.startHandshake();
            //debug( "MyRMIServerSocketFactory.createServerSocket(): " + mAddress + " : " + port );
            socketMap.put(new Integer(8686), sslSocket);
            return sslSocket;
        }

        /**********************************************************************
         *  Private Methods
         ********************************************************************** /

        /**
         * Configures the client socket with the enabled protocols and cipher suites.
         * @param sslSocket
         * @param sslCC
         */

        private void configureSSLSocket(SSLSocket sslSocket,
                SSLClientConfigurator sslCC) {
            if (sslCC.getEnabledCipherSuites() != null) {
                if (enabledCipherSuites == null) {
                    synchronized (cipherSuitesSync) {
                        if (enabledCipherSuites == null) {
                            enabledCipherSuites = configureEnabledCiphers(sslSocket,
                                    sslCC.getEnabledCipherSuites());
                        }
                    }
                }
                 sslSocket.setEnabledCipherSuites(enabledCipherSuites);
            }

            if (sslCC.getEnabledProtocols() != null) {
                if (enabledProtocols == null) {
                    synchronized (protocolsSync) {
                        if (enabledProtocols == null) {
                            enabledProtocols = configureEnabledProtocols(sslSocket,
                                    sslCC.getEnabledProtocols());
                        }
                    }
                }
                sslSocket.setEnabledProtocols(enabledProtocols);
            }

            sslSocket.setUseClientMode(true);
        }

        /**
         * Return the list of allowed protocol.
         * @return String[] an array of supported protocols.
         */
        private final static String[] configureEnabledProtocols(
                SSLSocket socket, String[] requestedProtocols) {

            String[] supportedProtocols = socket.getSupportedProtocols();
            String[] protocols = null;
            ArrayList<String> list = null;
            for (String supportedProtocol : supportedProtocols) {
                /*
                 * Check to see if the requested protocol is among the
                 * supported protocols, i.e., may be enabled
                 */
                for (String protocol : requestedProtocols) {
                    protocol = protocol.trim();
                    if (supportedProtocol.equals(protocol)) {
                        if (list == null) {
                            list = new ArrayList<String>();
                        }
                        list.add(protocol);
                        break;
                    }
                }
            }

            if (list != null) {
                protocols = list.toArray(new String[list.size()]);
            }

            return protocols;
        }

        /**
         * Determines the SSL cipher suites to be enabled.
         *
         * @return Array of SSL cipher suites to be enabled, or null if none of the
         * requested ciphers are supported
         */
        private final static String[] configureEnabledCiphers(SSLSocket socket,
                String[] requestedCiphers) {

            String[] supportedCiphers = socket.getSupportedCipherSuites();
            String[] ciphers = null;
            ArrayList<String> list = null;
            for (String supportedCipher : supportedCiphers) {
                /*
                 * Check to see if the requested protocol is among the
                 * supported protocols, i.e., may be enabled
                 */
                for (String cipher : requestedCiphers) {
                    cipher = cipher.trim();
                    if (supportedCipher.equals(cipher)) {
                        if (list == null) {
                            list = new ArrayList<String>();
                        }
                        list.add(cipher);
                        break;
                    }
                }
            }

            if (list != null) {
                ciphers = list.toArray(new String[list.size()]);
            }

            return ciphers;
        }
    }
