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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

// Portions Copyright [2016-2022] [Payara Foundation]

package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.impl.misc.ORBUtility;
import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.corba.ee.spi.orb.ORB;
import com.sun.corba.ee.spi.transport.Acceptor;
import com.sun.corba.ee.spi.transport.ORBSocketFactory;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.security.integration.AppClientSSL;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.enterprise.iiop.api.IIOPSSLUtil;
import org.glassfish.enterprise.iiop.util.IIOPUtils;
import org.glassfish.enterprise.iiop.util.NotServerException;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.internal.api.Globals;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;
import org.glassfish.security.common.CipherInfo;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.grizzly.config.dom.Ssl.*;


/**
 * This is socket factory used to create either plain sockets or SSL
 * sockets based on the target's policies and the client policies.
 *
 * @author Vivek Nagar
 * @author Shing Wai Chan
 */
public class IIOPSSLSocketFactory implements ORBSocketFactory {

    private static final Logger _logger = LogDomains.getLogger(
            IIOPSSLSocketFactory.class, LogDomains.CORBA_LOGGER);

    private static final String SSL_MUTUALAUTH = "SSL_MUTUALAUTH";
    private static final String PERSISTENT_SSL = "PERSISTENT_SSL";

    private static final int BACKLOG = 50;

    private static final String SO_KEEPALIVE = "fish.payara.SO_KEEPALIVE";
    // Deprecated as of 5.191
    private static final String SO_KEEPALIVE_DEPRECATED = "fish.payara.SOKeepAlive";



    //private static SecureRandom sr = null;

    /* this is stored for the Server side of SSL Connections.
     * Note: There will be only a port per iiop listener and a corresponding
     * ctx for that port
     */
    /*
     * @todo provide an interface to the admin, so that whenever a iiop-listener
     * is added / removed, we modify the hashtable,
     */
    private Map<Integer, SSLInfo> portToSSLInfo = new ConcurrentHashMap<>();

    /* this is stored for the client side of SSL Connections.
     * Note: There will be only 1 ctx for the client side, as we will reuse the
     * ctx for all SSL connections
     */
    private SSLInfo clientSslInfo = null;

    private ORB orb;

    /**
     * Constructs an <code>IIOPSSLSocketFactory</code>
     */
    public IIOPSSLSocketFactory() {
        try {
            ProcessEnvironment penv = null;
            ProcessType processType = null;
            boolean notServerOrACC = Globals.getDefaultHabitat() == null;
            if (!notServerOrACC) {
                penv = Globals.get(ProcessEnvironment.class);
                processType = penv.getProcessType();
            }
            //if (Switch.getSwitch().getContainerType() == Switch.EJBWEB_CONTAINER) {
            if ((processType != null) && (processType.isServer())) {
                //this is the EJB container
                Config conf = Globals.getDefaultHabitat().getService(Config.class,
                        ServerEnvironment.DEFAULT_INSTANCE_NAME);
                IiopService iiopBean = conf.getExtensionByType(IiopService.class);
                List<IiopListener> iiopListeners = iiopBean.getIiopListener();
                for (IiopListener listener : iiopListeners) {
                    Ssl ssl = listener.getSsl();
                    SSLInfo sslInfo;
                    boolean securityEnabled = Boolean.valueOf(listener.getSecurityEnabled());

                    if (securityEnabled) {
                        if (ssl != null) {
                            boolean tlsEnabled12 = Boolean.valueOf(ssl.getTls12Enabled());
                            boolean tlsEnabled13 = Boolean.valueOf(ssl.getTls13Enabled());

                            sslInfo = init(ssl.getCertNickname(), ssl.getSsl3TlsCiphers(), tlsEnabled12, tlsEnabled13);
                        } else {
                            sslInfo = getDefaultSslInfo();
                        }
                        portToSSLInfo.put(new Integer(listener.getPort()), sslInfo);
                    }
                }

                if (iiopBean.getSslClientConfig() != null && iiopBean.getSslClientConfig().getSsl() != null) {
                    Ssl outboundSsl = iiopBean.getSslClientConfig().getSsl();
                    if (outboundSsl != null) {
                        boolean tlsEnabled12 = Boolean.valueOf(outboundSsl.getTls12Enabled());
                        boolean tlsEnabled13 = Boolean.valueOf(outboundSsl.getTls13Enabled());
                        clientSslInfo = init(outboundSsl.getCertNickname(),
                                outboundSsl.getSsl3TlsCiphers(),
                                tlsEnabled12,
                                tlsEnabled13
                        );

                    }
                }
                if (clientSslInfo == null) {
                    clientSslInfo = getDefaultSslInfo();
                }
            } else {
                if (processType == ProcessType.ACC) {
                    IIOPSSLUtil sslUtil = Globals.getDefaultHabitat().getService(IIOPSSLUtil.class);
                    AppClientSSL clientSsl = (AppClientSSL) sslUtil.getAppClientSSL();
                    if (clientSsl != null) {
                        clientSslInfo = init(clientSsl.getCertNickname(), clientSsl.getSsl3TlsCiphers(),
                                clientSsl.getTls12Enabled(), clientSsl.getTls13Enabled());
                    } else { // include case keystore, truststore jvm option
                        clientSslInfo = getDefaultSslInfo();
                    }
                } else {
                    clientSslInfo = getDefaultSslInfo();
                }
            }
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "iiop.init_exception", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Return a default SSLInfo object.
     */
    private SSLInfo getDefaultSslInfo() throws Exception {
        return init(null, null,true, true);
    }

    /**
     * serveralias/clientalias cannot be set at the same time.
     * this method encapsulates the common code for both the client side and
     * server side to create a SSLContext
     * it is called once for each serveralias and once for each clientalias
     */
    private SSLInfo init(String alias, String ssl3TlsCiphers, boolean tlsEnabled12, boolean tlsEnabled13) throws Exception {

        String protocol;
        if (tlsEnabled13) {
            protocol = TLS13;
        } else if (tlsEnabled12) {
            protocol = TLS12;
        } else { // default
            protocol = SSL;
        }

        String[] ssl3TlsCipherArr = null;
        if (tlsEnabled12 || tlsEnabled13) {
            ssl3TlsCipherArr = getEnabledCipherSuites(
                    ssl3TlsCiphers, tlsEnabled12, tlsEnabled13);
        }
        SSLContext ctx = SSLContext.getInstance(protocol);
        if (Globals.getDefaultHabitat() != null) {
            IIOPSSLUtil sslUtil = Globals.getDefaultHabitat().getService(IIOPSSLUtil.class);
            KeyManager[] mgrs = sslUtil.getKeyManagers(alias);
            ctx.init(mgrs, sslUtil.getTrustManagers(), sslUtil.getInitializedSecureRandom());
        } else {
            //do nothing
            //ctx.init(mgrs, sslUtil.getTrustManagers(), sslUtil.getInitializedSecureRandom());
        }

        SSLInfo newInfo = new SSLInfo(ctx, ssl3TlsCipherArr);
        if (tlsEnabled12) {
            newInfo.addProtocol(TLS12);
        }
        if (tlsEnabled13) {
            newInfo.addProtocol(TLS13);
        }
        return newInfo;
    }

    //----- implements com.sun.corba.ee.spi.transport.ORBSocketFactory -----

    public void setORB(ORB orb) {
        this.orb = orb;
    }

    /**
     * Create a server socket on the specified InetSocketAddress  based on the
     * type of the server socket (SSL, SSL_MUTUALAUTH, PERSISTENT_SSL or CLEAR_TEXT).
     *
     * @param type              type of socket to create.
     * @param inetSocketAddress the InetSocketAddress
     * @return the server socket on the specified InetSocketAddress
     * @throws IOException if an I/O error occurs during server socket
     *                     creation
     */
    public ServerSocket createServerSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Creating server socket for type =" + type
                    + " inetSocketAddress =" + inetSocketAddress);
        }

	if(type.equals(SSL_MUTUALAUTH) || type.equals(SSL) ||
		type.equals(PERSISTENT_SSL)) {
	    return createSSLServerSocket(type, inetSocketAddress);
	} else {
            ServerSocket serverSocket = null;
            if (orb.getORBData().acceptorSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocket = serverSocketChannel.socket();
            } else {
                serverSocket = new ServerSocket();
            }

            serverSocket.bind(inetSocketAddress);
            return serverSocket;

        }
    }

    /**
     * Create a client socket for the specified InetSocketAddress. Creates an SSL
     * socket if the type specified is SSL or SSL_MUTUALAUTH.
     *
     * @param type
     * @param inetSocketAddress
     * @return the socket.
     */
    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        try {
            String host = inetSocketAddress.getHostName();
            int port = inetSocketAddress.getPort();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "createSocket(" + type + ", " + host + ", " + port + ")");
            }
            if (type.equals(SSL) || type.equals(SSL_MUTUALAUTH)) {
                return createSSLSocket(host, port);
            } else {
                Socket socket = null;
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Creating CLEAR_TEXT socket for:" + port);
		}

                if (orb.getORBData().connectionSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
                    SocketChannel socketChannel = ORBUtility.openSocketChannel(inetSocketAddress);
                    socket = socketChannel.socket();
                } else {
                    socket = new Socket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                }

                // Enable SO_KEEPALIVE if required
                enableSOKeepAliveAsRequired(socket);
                // Disable Nagle's algorithm (i.e. always send immediately).
                socket.setTcpNoDelay(true);
                return socket;
            }
        } catch (Exception ex) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Exception creating socket", ex);
            }
            throw new RuntimeException(ex);
        }
    }

    public void setAcceptedSocketOptions(Acceptor acceptor, ServerSocket serverSocket, Socket socket) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "setAcceptedSocketOptions: " + acceptor
                    + " " + serverSocket + " " + socket);
        }

        try {
            // Disable Nagle's algorithm (i.e., always send immediately).
            socket.setTcpNoDelay(true);

            // Enable SO_KEEPALIVE if required
            enableSOKeepAliveAsRequired(socket);
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    //----- END implements com.sun.corba.ee.spi.transport.ORBSocketFactory -----

    /**
     * Create an SSL server socket at the specified InetSocketAddress. If the type
     * is SSL_MUTUALAUTH then SSL client authentication is requested.
     */
    private ServerSocket createSSLServerSocket(String type,
            InetSocketAddress inetSocketAddress) throws IOException {

        if (inetSocketAddress == null) {
            throw new IOException(getFormatMessage("iiop.invalid_sslserverport", new Object[]{null}));
        }
        int port = inetSocketAddress.getPort();
        Integer iport = Integer.valueOf(port);
        SSLInfo sslInfo = (SSLInfo) portToSSLInfo.get(iport);
        if (sslInfo == null) {
            throw new IOException(getFormatMessage("iiop.invalid_sslserverport", new Object[]{iport}));
        }
        SSLServerSocketFactory ssf = sslInfo.getContext().getServerSocketFactory();
        String[] ciphers = null;

        String cs[] = null;

        if (_logger.isLoggable(Level.FINE)) {
            cs = ssf.getSupportedCipherSuites();
            for (int i = 0; i < cs.length; ++i) {
                _logger.log(Level.FINE, "Cipher Suite: " + cs[i]);
            }
        }
        ServerSocket ss = null;
        try {
            // bugfix for 6349541
            // specify the ip address to bind to, 50 is the default used
            // by the ssf implementation when only the port is specified
            ss = ssf.createServerSocket(port, BACKLOG, inetSocketAddress.getAddress());
            if (ciphers != null) {
                ((SSLServerSocket) ss).setEnabledCipherSuites(ciphers);
            }

            if (sslInfo.allowedProtocols.size() > 0) {
                // filter protocols against socket supported
                ArrayList<String> socketSupported = new ArrayList<>(
                        Arrays.asList(((SSLServerSocket) ss).getSupportedProtocols()));
                ArrayList<String> allowedProtocols = new ArrayList<>();
                for (String protocolName : sslInfo.allowedProtocols) {
                    if (socketSupported.contains(protocolName)) {
                        allowedProtocols.add(protocolName);
                    }
                }
                if (allowedProtocols.size() > 0) {
                    ((SSLServerSocket) ss).setEnabledProtocols(
                            allowedProtocols.toArray(new String[allowedProtocols.size()]));
                }
            }
        } catch (IOException e) {
            _logger.log(Level.SEVERE, "iiop.createsocket_exception", new Object[]{type, String.valueOf(port)});
            _logger.log(Level.SEVERE, "", e);
            throw e;
        }

        try {
            if (type.equals(SSL_MUTUALAUTH)) {
                _logger.log(Level.FINE, "Setting Mutual auth");
                ((SSLServerSocket) ss).setNeedClientAuth(true);
            }
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "iiop.cipher_exception", e);
            throw new IOException(e.getMessage());
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Created server socket:" + ss);
        }
        return ss;
    }

    /**
     * Create an SSL socket at the specified host and port.
     *
     * @param host
     * @param port
     * @return the socket.
     */
    private Socket createSSLSocket(String host, int port) throws IOException {
        SSLSocket socket = null;
        SSLSocketFactory factory = null;
        try {
            // get socketfactory+sanity check
            // clientSslInfo is never null
            factory = clientSslInfo.getContext().getSocketFactory();

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Creating SSL Socket for host:" + host + " port:" + port);
            }
            String[] clientCiphers = null;

            socket = (SSLSocket) factory.createSocket(host, port);
            if (clientCiphers != null) {
                socket.setEnabledCipherSuites(clientCiphers);
            }

            // Enable SO_KEEPALIVE if required
            enableSOKeepAliveAsRequired(socket);

        } catch (Exception e) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "iiop.createsocket_exception", new Object[]{host, String.valueOf(port)});
                _logger.log(Level.FINE, "", e);
            }
            IOException e2 = new IOException("Error opening SSL socket to host=" + host + " port=" + port);
            e2.initCause(e);
            throw e2;
        }
        return socket;
    }

    /**
     * This API return an array of String listing the enabled cipher suites.
     * Input is the cipherSuiteStr from xml which a space separated list
     * ciphers with a prefix '+' indicating enabled, '-' indicating disabled.
     * If no cipher is enabled, then it returns an empty array.
     * If no cipher is specified, then all are enabled and it returns null.
     *
     * @param cipherSuiteStr cipherSuiteStr from xml
     * @param tlsEnabled12
     * @param tlsEnabled13
     * @return an array of enabled Ciphers
     */
    private String[] getEnabledCipherSuites(String cipherSuiteStr, boolean tlsEnabled12, boolean tlsEnabled13) {
        String[] cipherArr = null;
        if (cipherSuiteStr != null && cipherSuiteStr.length() > 0) {
            ArrayList cipherList = new ArrayList();
            StringTokenizer tokens = new StringTokenizer(cipherSuiteStr, ",");
            while (tokens.hasMoreTokens()) {
                String cipherAction = tokens.nextToken();
                if (cipherAction.startsWith("+")) {
                    String cipher = cipherAction.substring(1);
                    CipherInfo cipherInfo = CipherInfo.getCipherInfo(cipher);
                    if (cipherInfo != null && isValidProtocolCipher(cipherInfo, tlsEnabled12, tlsEnabled13)) {
                        cipherList.add(cipherInfo.getCipherName());
                    } else {
                        throw new IllegalStateException(getFormatMessage("iiop.unknown_cipher",
                                new Object[]{cipher}));
                    }
                } else if (cipherAction.startsWith("-")) {
                    String cipher = cipherAction.substring(1);
                    CipherInfo cipherInfo = CipherInfo.getCipherInfo(cipher);
                    if (cipherInfo == null || !isValidProtocolCipher(cipherInfo, tlsEnabled12, tlsEnabled13)) {
                        throw new IllegalStateException(getFormatMessage("iiop.unknown_cipher",
                                new Object[]{cipher}));
                    }
                } else if (cipherAction.trim().length() > 0) {
                    throw new IllegalStateException(getFormatMessage("iiop.invalid_cipheraction",
                            new Object[]{cipherAction}));
                }
            }

            cipherArr = (String[]) cipherList.toArray(new String[cipherList.size()]);
        }
        return cipherArr;
    }


    /**
     * Check whether given cipherInfo belongs to given protocol.
     *
     * @param cipherInfo
     * @param tlsEnabled12
     * @param tlsEnabled13
     */
    private boolean isValidProtocolCipher(CipherInfo cipherInfo, boolean tlsEnabled12, boolean tlsEnabled13) {
        return (tlsEnabled12 && cipherInfo.isTLS() ||
                tlsEnabled13 && cipherInfo.isTLS());
    }

    /**
     * Checks whether SO_KEEPALIVE should be enabled on a Socket and enables it if it should be. Checks for the
     * presence of a property on the IIOP listener and globally, preferring the value set in the listener.
     *
     * @param socket The socket to potentially enable SO_KEEPALIVE on
     * @throws SocketException If there was an error enabling SO_KEEPALIVE on the socket.
     */
    private void enableSOKeepAliveAsRequired(Socket socket) throws SocketException {
        boolean shouldSet = false;

        try {
            // Try to get the IIOP Service as this does a check for if we are a server or not (save checking twice)
            IiopService iiopService = IIOPUtils.getInstance().getIiopService();

            // For each listener, find one with a matching port
            for (IiopListener iiopListener : IIOPUtils.getInstance().getIiopService().getIiopListener()) {
                if (Integer.valueOf(iiopListener.getPort()) == socket.getLocalPort()) {
                    // Check for the property globally before checking on the specific listener, giving precedence to the
                    // new property
                    if ((System.getProperty(SO_KEEPALIVE) == null && Boolean.getBoolean(SO_KEEPALIVE_DEPRECATED))
                            || Boolean.getBoolean(SO_KEEPALIVE)) {
                        // Check if the property has been set on the listener
                        if (soKeepAlivePropertyPresentOnIiopListener(iiopListener)) {
                            // Check if we should override the global value
                            if (soKeepAlivePropertyEnabledOnIiopListener(iiopListener)) {
                                shouldSet = true;
                            }
                        } else {
                            // If the property wasn't set on the listener, go with the global setting
                            shouldSet = true;
                        }
                        break;
                    } else {
                        // If it wasn't set globally, just check if it's set and enabled on the listener
                        if (soKeepAlivePropertyPresentOnIiopListener(iiopListener)
                                && soKeepAlivePropertyEnabledOnIiopListener(iiopListener)) {
                            shouldSet = true;
                        }
                        break;
                    }
                }
            }
        } catch (NotServerException notServerException) {
            // Enable or disable SO_KEEPALIVE for the socket as required
            if (Boolean.getBoolean(SO_KEEPALIVE) && !socket.getKeepAlive()) {
                shouldSet = true;
            }
        }

        if (shouldSet) {
            _logger.log(Level.FINER, "Enabling SO_KEEPALIVE");
            socket.setKeepAlive(true);
        }
    }

    /**
     * Shorthand method that simply returns true if either the new or old SO_KEEPALIVE property is present on the
     * listener.
     * @param iiopListener The IIOP listener to check if the SO_KEEPALIVE property is set on
     * @return True if the SO_KEEPALIVE property is present on the IIOP listener
     */
    private boolean soKeepAlivePropertyPresentOnIiopListener(IiopListener iiopListener) {
        boolean soKeepAlivePropertyPresentOnListener = false;

        if (iiopListener.getPropertyValue(SO_KEEPALIVE) != null
                || iiopListener.getPropertyValue(SO_KEEPALIVE_DEPRECATED) != null) {
            soKeepAlivePropertyPresentOnListener = true;
        }

        return soKeepAlivePropertyPresentOnListener;
    }

    /**
     * Helper method that checks if either the deprecated or new SO_KEEPALIVE property is enabled on an IIOP
     * listener, giving precedence to the new property if both are present.
     * @param iiopListener The IIOP listener to check if the SO_KEEPALIVE property is set on
     * @return True if the SO_KEEPALIVE property is enabled on the IIOP listener
     */
    private boolean soKeepAlivePropertyEnabledOnIiopListener(IiopListener iiopListener) {
        boolean soKeepAliveEnabledOnListener = false;

        // If the new property isn't present and the deprecated one is set to true, or if the new property is set to
        // true, then register SO_KEEPALIVE as enabled on the listener
        if ((iiopListener.getPropertyValue(SO_KEEPALIVE) == null
                && Boolean.valueOf(iiopListener.getPropertyValue(SO_KEEPALIVE_DEPRECATED)))
                || Boolean.valueOf(iiopListener.getPropertyValue(SO_KEEPALIVE))) {
            soKeepAliveEnabledOnListener = true;
        }

        return soKeepAliveEnabledOnListener;
    }

    /**
     * This API get the format string from resource bundle of _logger.
     *
     * @param key    the key of the message
     * @param params the parameter array of Object
     * @return the format String for _logger
     */
    private String getFormatMessage(String key, Object[] params) {
        return MessageFormat.format(_logger.getResourceBundle().getString(key), params);
    }

    class SSLInfo {
        private final SSLContext ctx;
        private String[] ssl3TlsCiphers = null;
        private ArrayList<String> allowedProtocols;

        SSLInfo(SSLContext ctx, String[] ssl3TlsCiphers) {
            this.ctx = ctx;
            this.ssl3TlsCiphers = ssl3TlsCiphers;
            allowedProtocols = new ArrayList<>();
        }

        SSLContext getContext() {
            return ctx;
        }

        void addProtocol(String protocol) {
            allowedProtocols.add(protocol);
        }

        String[] getSsl3TlsCiphers() {
            return ssl3TlsCiphers;
        }

    }
}
