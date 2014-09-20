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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.glassfish.admin.mbeanserver.JMXSslConfigHolder;
import org.glassfish.admin.mbeanserver.Util;
import org.glassfish.grizzly.config.SSLConfigurator;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Inner class for SSL support for JMX connection using RMI.
 *
 * @author prasad
 */
public class SecureRMIServerSocketFactory
        extends SslRMIServerSocketFactory {

    private final InetAddress mAddress;
    private final ServiceLocator habitat;
    private final Ssl ssl;
    // The list of cipher suite
    private String[] enabledCipherSuites;
    private volatile Object enabledCipherSuitesLock;
    //the list of protocols
    private String[] enabledProtocols;
    private volatile Object enabledProtocolsLock;
    private final Object cipherSuitesSync = new Object();
    private final Object protocolsSync = new Object();
    private Map socketMap = new HashMap<Integer, Socket>();

    @LogMessageInfo(level="INFO", message="Creating a SecureRMIServerSocketFactory @ {0} with ssl config = {1}")
    private final static String creatingServerSocketFactory = Util.LOG_PREFIX + "00024";
    
    @LogMessageInfo(level="INFO", message="SSLServerSocket {0} and {1} created")
    private final static String createdServerSocket = Util.LOG_PREFIX + "00025";
    
    public SecureRMIServerSocketFactory(final ServiceLocator habitat,
            final Ssl sslConfig,
            final InetAddress addr) {
        mAddress = addr;
        this.habitat = habitat;
        ssl = sslConfig;
    
        Util.getLogger().log(Level.INFO, 
                creatingServerSocketFactory, 
                new Object[] {addr.getHostAddress(), ssl.toString()});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SecureRMIServerSocketFactory that = (SecureRMIServerSocketFactory) o;

        if (cipherSuitesSync != null ? !cipherSuitesSync.equals(that.cipherSuitesSync) : that.cipherSuitesSync != null)
            return false;
        if (!Arrays.equals(enabledCipherSuites, that.enabledCipherSuites))
            return false;
        if (!Arrays.equals(enabledProtocols, that.enabledProtocols))
            return false;
        if (habitat != null ? !habitat.equals(that.habitat) : that.habitat != null)
            return false;
        if (mAddress != null ? !mAddress.equals(that.mAddress) : that.mAddress != null)
            return false;
        if (protocolsSync != null ? !protocolsSync.equals(that.protocolsSync) : that.protocolsSync != null)
            return false;
        if (socketMap != null ? !socketMap.equals(that.socketMap) : that.socketMap != null)
            return false;
        if (ssl != null ? !ssl.equals(that.ssl) : that.ssl != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mAddress != null ? mAddress.hashCode() : 0);
        result = 31 * result + (habitat != null ? habitat.hashCode() : 0);
        result = 31 * result + (ssl != null ? ssl.hashCode() : 0);
        result = 31 * result + (enabledCipherSuites != null ? Arrays.hashCode(enabledCipherSuites) : 0);
        result = 31 * result + (enabledProtocols != null ? Arrays.hashCode(enabledProtocols) : 0);
        result = 31 * result + (cipherSuitesSync != null ? cipherSuitesSync.hashCode() : 0);
        result = 31 * result + (protocolsSync != null ? protocolsSync.hashCode() : 0);
        result = 31 * result + (socketMap != null ? socketMap.hashCode() : 0);
        return result;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        //debug( "MyRMIServerSocketFactory.createServerSocket(): " + mAddress + " : " + port );
        if (socketMap.containsKey(port)) {
            return (ServerSocket) socketMap.get(port);
        }

        final int backlog = 5;  // plenty
        // we use a custom class here. The reason is mentioned in the class.
        final JMXSslConfigHolder sslConfigHolder;
        try {
            sslConfigHolder = new JMXSslConfigHolder(habitat, ssl);
        } catch (SSLException ssle) {
            throw new IllegalStateException(ssle);
        }

        final SSLContext context = sslConfigHolder.getSslContext();
        ServerSocket sslSocket =
                context.getServerSocketFactory().
                createServerSocket(port, backlog, mAddress);
        if (!(sslSocket instanceof SSLServerSocket)) {
            throw new IllegalStateException("ServerSocketFactory returned non-secure server socket.");
        }
        configureSSLSocket((SSLServerSocket) sslSocket, sslConfigHolder);
    
        Util.getLogger().log(Level.INFO, createdServerSocket, 
                new Object[] {sslSocket.getLocalSocketAddress(), sslSocket.toString()});
        //sslSocket.startHandshake();
        //debug( "MyRMIServerSocketFactory.createServerSocket(): " + mAddress + " : " + port );
        socketMap.put(port, sslSocket);
        return sslSocket;
    }

    private void configureSSLSocket(SSLServerSocket sslSocket,
            SSLConfigurator sslConfigHolder) {
        if (sslConfigHolder.getEnabledCipherSuites() != null) {
            if (enabledCipherSuitesLock == null) {
                synchronized (cipherSuitesSync) {
                    if (enabledCipherSuitesLock == null) {
                        enabledCipherSuitesLock = new Object();
                        enabledCipherSuites = configureEnabledCiphers(sslSocket,
                                sslConfigHolder.getEnabledCipherSuites());
                    }
                }
            }

            sslSocket.setEnabledCipherSuites(enabledCipherSuites);
        }

        if (sslConfigHolder.getEnabledProtocols() != null) {
            if (enabledProtocolsLock == null) {
                synchronized (protocolsSync) {
                    if (enabledProtocolsLock == null) {
                        enabledProtocolsLock = new Object();
                        enabledProtocols = configureEnabledProtocols(sslSocket,
                                sslConfigHolder.getEnabledProtocols());
                    }
                }
            }
            sslSocket.setEnabledProtocols(enabledProtocols);
        }

        sslSocket.setUseClientMode(sslConfigHolder.isClientMode());
    }

    /**
     * Return the list of allowed protocol.
     *
     * @return String[] an array of supported protocols.
     */
    private final static String[] configureEnabledProtocols(
            SSLServerSocket socket, String[] requestedProtocols) {

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
    private final static String[] configureEnabledCiphers(SSLServerSocket socket,
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
