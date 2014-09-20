/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectInputStream;
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
import org.glassfish.admin.mbeanserver.Util;
import org.glassfish.logging.annotation.LogMessageInfo;


    /**
     * Inner class for SSL support for JMX connection using RMI.
     */
    public final class SecureRMIClientSocketFactory
            extends SslRMIClientSocketFactory  {

        @LogMessageInfo(level="INFO", message="Creating a SecureRMIClientSocketFactory @ {0}with ssl config = {1}")
        private final static String creatingFactory = Util.LOG_PREFIX + "00022";
    
        @LogMessageInfo(level="INFO", message="Setting SSLParams @ {0}")
        private final static String settingSSLParams = Util.LOG_PREFIX + "00023";
    
        private InetAddress mAddress;
        private transient SSLParams sslParams;
        private transient Map socketMap = new HashMap<Integer, Socket>();
        private static final Logger  _logger = Util.JMX_LOGGER;

        public SecureRMIClientSocketFactory(final SSLParams sslParams,
                final InetAddress addr) {
            super();
            mAddress = addr;
            this.sslParams = sslParams;
            if(sslParams != null) {
                _logger.log(Level.INFO, 
                        creatingFactory, 
                        new Object[]{addr.getHostAddress(), sslParams.toString()});
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
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new IOException("Serialization not supported");
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            //debug( "MyRMIServerSocketFactory.createServerSocket(): " + mAddress + " : " + port );
            if(socketMap.containsKey(Integer.valueOf(port))) {
                return (Socket)socketMap.get(Integer.valueOf(port));
            }
           
            final int backlog = 5;

            SSLClientConfigurator sslCC = SSLClientConfigurator.getInstance();
            _logger.log(Level.INFO, settingSSLParams, sslParams);
            sslCC.setSSLParams(sslParams);
            SSLContext sslContext = sslCC.configure(sslParams);
            SSLSocket sslSocket =
                    (SSLSocket)sslContext.getSocketFactory().createSocket(mAddress, port);
            configureSSLSocket(sslSocket, sslCC);

            socketMap.put(Integer.valueOf(8686), sslSocket);
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
            String ecs[] = sslCC.getEnabledCipherSuites();
            if (ecs != null) {
                sslSocket.setEnabledCipherSuites(configureEnabledCiphers(sslSocket, ecs));
            }
            
            String ep[] = sslCC.getEnabledProtocols();
            if (ep != null) {
                sslSocket.setEnabledProtocols(configureEnabledProtocols(sslSocket, ep));
            }

            sslSocket.setUseClientMode(true);
        }

        /**
         * Return the list of allowed protocols.
         * @return String[] an array of supported protocols.
         */
        private static String[] configureEnabledProtocols(
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
        private static String[] configureEnabledCiphers(SSLSocket socket,
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
