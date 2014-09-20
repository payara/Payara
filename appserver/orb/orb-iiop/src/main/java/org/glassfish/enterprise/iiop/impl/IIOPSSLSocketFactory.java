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

package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.impl.misc.ORBUtility;
import com.sun.corba.ee.spi.transport.Acceptor;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import com.sun.corba.ee.spi.orb.ORB;
import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.corba.ee.spi.transport.ORBSocketFactory;
import com.sun.enterprise.config.serverbeans.Config;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;
import org.glassfish.grizzly.config.dom.Ssl;
import com.sun.logging.LogDomains;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.CipherInfo;
import org.glassfish.enterprise.iiop.api.IIOPSSLUtil;
import com.sun.enterprise.security.integration.AppClientSSL;
import org.glassfish.api.admin.ServerEnvironment;


/**
 * This is socket factory used to create either plain sockets or SSL
 * sockets based on the target's policies and the client policies.
 * @author Vivek Nagar
 * @author Shing Wai Chan
 */
public class IIOPSSLSocketFactory  implements ORBSocketFactory
{ 
    private static final Logger _logger = LogDomains.getLogger(
        IIOPSSLSocketFactory.class, LogDomains.CORBA_LOGGER);

    private static final String TLS = "TLS";
    private static final String SSL3 = "SSLv3";
    private static final String SSL2 = "SSLv2";
    private static final String SSL = "SSL";
    private static final String SSL_MUTUALAUTH = "SSL_MUTUALAUTH";
    private static final String PERSISTENT_SSL = "PERSISTENT_SSL";

    private static final int BACKLOG = 50;

    //private static SecureRandom sr = null;

    /* this is stored for the Server side of SSL Connections.
     * Note: There will be only a port per iiop listener and a corresponding
     * ctx for that port
     */
    /*
     * @todo provide an interface to the admin, so that whenever a iiop-listener
     * is added / removed, we modify the hashtable,
     */ 
    private Map portToSSLInfo = new Hashtable();
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
            boolean notServerOrACC =  Globals.getDefaultHabitat() == null ? true : false;
            if (!notServerOrACC) {
                penv = Globals.get(ProcessEnvironment.class);
                processType = penv.getProcessType();
            }
            //if (Switch.getSwitch().getContainerType() == Switch.EJBWEB_CONTAINER) {
            if((processType != null) && (processType.isServer())) {
                //this is the EJB container
                        Config conf = Globals.getDefaultHabitat().getService(Config.class,
                        ServerEnvironment.DEFAULT_INSTANCE_NAME);
                IiopService iiopBean =conf.getExtensionByType(IiopService.class);
                List<IiopListener> iiopListeners = iiopBean.getIiopListener();
                for (IiopListener listener : iiopListeners) {
                    Ssl ssl = listener.getSsl(); 
                    SSLInfo sslInfo = null;
                    boolean securityEnabled = Boolean.valueOf(listener.getSecurityEnabled());
                   
                    if (securityEnabled) {
                        if (ssl != null) {
                            boolean ssl2Enabled = Boolean.valueOf(ssl.getSsl2Enabled());
                            boolean tlsEnabled = Boolean.valueOf(ssl.getTlsEnabled());
                            boolean ssl3Enabled = Boolean.valueOf(ssl.getSsl3Enabled());
                            sslInfo = init(ssl.getCertNickname(),
                                    ssl2Enabled, ssl.getSsl2Ciphers(),
                                    ssl3Enabled, ssl.getSsl3TlsCiphers(),
                                    tlsEnabled);
                        } else {
                            sslInfo = getDefaultSslInfo();
                        }
                        portToSSLInfo.put(
                            new Integer(listener.getPort()), sslInfo);
                    }
                }

                if (iiopBean.getSslClientConfig() != null &&
                        /*iiopBean.getSslClientConfig().isEnabled()*/
                        iiopBean.getSslClientConfig().getSsl() != null) {
                    Ssl outboundSsl = iiopBean.getSslClientConfig().getSsl();
                    if (outboundSsl != null) {
                        boolean ssl2Enabled = Boolean.valueOf(outboundSsl.getSsl2Enabled());
                        boolean ssl3Enabled = Boolean.valueOf(outboundSsl.getSsl3Enabled());
                        boolean tlsEnabled = Boolean.valueOf(outboundSsl.getTlsEnabled());
                        clientSslInfo = init(outboundSsl.getCertNickname(),
                            ssl2Enabled,
                            outboundSsl.getSsl2Ciphers(),
                            ssl3Enabled,
                            outboundSsl.getSsl3TlsCiphers(),
                            tlsEnabled);
                    }
                }
                if (clientSslInfo == null) {
                    clientSslInfo = getDefaultSslInfo();
                }
            } else {
                if ((processType != null) && (processType == ProcessType.ACC)) {
                    IIOPSSLUtil sslUtil = Globals.getDefaultHabitat().getService(IIOPSSLUtil.class);
                    AppClientSSL clientSsl = (AppClientSSL)sslUtil.getAppClientSSL();
                    if (clientSsl != null) {
                        clientSslInfo = init(clientSsl.getCertNickname(),
                                clientSsl.getSsl2Enabled(), clientSsl.getSsl2Ciphers(),
                                clientSsl.getSsl3Enabled(), clientSsl.getSsl3TlsCiphers(),
                                clientSsl.getTlsEnabled());
                    } else { // include case keystore, truststore jvm option

                        clientSslInfo = getDefaultSslInfo();
                    }
                } else {
                    clientSslInfo = getDefaultSslInfo();
                }
            }
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.init_exception",e);
            throw new IllegalStateException(e);
        }
    }
  
    /**
     * Return a default SSLInfo object.
     */
    private SSLInfo getDefaultSslInfo() throws Exception {
       return init(null, false, null, true, null, true);
    }

    /**
     * serveralias/clientalias cannot be set at the same time. 
     * this method encapsulates the common code for both the client side and
     * server side to create a SSLContext
     * it is called once for each serveralias and once for each clientalias 
     */
    private SSLInfo init(String alias,
            boolean ssl2Enabled, String ssl2Ciphers,
            boolean ssl3Enabled, String ssl3TlsCiphers,
            boolean tlsEnabled) throws Exception {

        String protocol;
        if (tlsEnabled) {
            protocol = TLS;
        } else if (ssl3Enabled) {
            protocol = SSL3;
        } else if (ssl2Enabled) {
            protocol = SSL2;
        } else { // default
            protocol = "SSL";
        }

        String[] ssl3TlsCipherArr = null;
        if (tlsEnabled || ssl3Enabled) {
            ssl3TlsCipherArr = getEnabledCipherSuites(ssl3TlsCiphers,
                    false, ssl3Enabled, tlsEnabled);
        }

        String[] ssl2CipherArr = null;
        if (ssl2Enabled) {
            ssl2CipherArr = getEnabledCipherSuites(ssl2Ciphers,
                    true, false, false);
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

        return new SSLInfo(ctx, ssl3TlsCipherArr, ssl2CipherArr);
    }

    //----- implements com.sun.corba.ee.spi.transport.ORBSocketFactory -----

    public void setORB(ORB orb) {
        this.orb = orb;
    }

    /**
     * Create a server socket on the specified InetSocketAddress  based on the
     * type of the server socket (SSL, SSL_MUTUALAUTH, PERSISTENT_SSL or CLEAR_TEXT).
     * @param type type of socket to create.
     * @param  inetSocketAddress the InetSocketAddress
     * @return the server socket on the specified InetSocketAddress
     * @exception IOException if an I/O error occurs during server socket
     * creation
     */
    public ServerSocket createServerSocket(String type,
            InetSocketAddress inetSocketAddress) throws IOException {

	if (_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE, "Creating server socket for type =" + type
                + " inetSocketAddress =" + inetSocketAddress);
	}

	if(type.equals(SSL_MUTUALAUTH) || type.equals(SSL) || 
		type.equals(PERSISTENT_SSL)) {
	    return createSSLServerSocket(type, inetSocketAddress);
	} else {
            ServerSocket serverSocket = null;
            if (orb.getORBData().acceptorSocketType().equals(
                    ORBConstants.SOCKETCHANNEL)) {
                ServerSocketChannel serverSocketChannel =
                        ServerSocketChannel.open();
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
     * @param type
     * @param inetSocketAddress
     * @return the socket.
     */
    public Socket createSocket(String type, InetSocketAddress inetSocketAddress)
            throws IOException {

	try {
	    String host = inetSocketAddress.getHostName();
	    int port = inetSocketAddress.getPort();
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, "createSocket(" + type + ", " + host + ", " +port + ")");
	    }
	    if (type.equals(SSL) || type.equals(SSL_MUTUALAUTH)) {
		return createSSLSocket(host, port);
	    } else {
                Socket socket = null;
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, "Creating CLEAR_TEXT socket for:" +port);
		}

                if (orb.getORBData().connectionSocketType().equals(
                        ORBConstants.SOCKETCHANNEL)) {
	            SocketChannel socketChannel = ORBUtility.openSocketChannel(inetSocketAddress);
	            socket = socketChannel.socket();
	        } else {
                    socket = new Socket(inetSocketAddress.getHostName(),
                        inetSocketAddress.getPort());
	        }

                // Disable Nagle's algorithm (i.e. always send immediately).
		socket.setTcpNoDelay(true);
                return socket;
	    }
	} catch ( Exception ex ) {
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Exception creating socket",ex);
	    }
	    throw new RuntimeException(ex);
	}
    }

    public void setAcceptedSocketOptions(Acceptor acceptor,
            ServerSocket serverSocket, Socket socket)  {
	if (_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE, "setAcceptedSocketOptions: " + acceptor
                    + " " + serverSocket + " " + socket);
	}
        // Disable Nagle's algorithm (i.e., always send immediately).
        try {
        socket.setTcpNoDelay(true);
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
            throw new IOException(getFormatMessage(
                "iiop.invalid_sslserverport",
                new Object[] { null }));
        }
        int port = inetSocketAddress.getPort();
        Integer iport = Integer.valueOf(port);
        SSLInfo sslInfo = (SSLInfo)portToSSLInfo.get(iport);
        if (sslInfo == null) {
            throw new IOException(getFormatMessage(
                "iiop.invalid_sslserverport",
                new Object[] { iport }));
        }
        SSLServerSocketFactory ssf = sslInfo.getContext().getServerSocketFactory();   
        String[] ssl3TlsCiphers = sslInfo.getSsl3TlsCiphers();
        String[] ssl2Ciphers = sslInfo.getSsl2Ciphers();
        String[] ciphers = null;
        if (ssl3TlsCiphers != null || ssl2Ciphers != null) {
            String[] socketCiphers = ssf.getDefaultCipherSuites();
            ciphers = mergeCiphers(socketCiphers, ssl3TlsCiphers, ssl2Ciphers);
        }

	String cs[] = null;

	if(_logger.isLoggable(Level.FINE)) {
	    cs = ssf.getSupportedCipherSuites();
	    for(int i=0; i < cs.length; ++i) {
		_logger.log(Level.FINE,"Cipher Suite: " + cs[i]);
	    }
	}
	ServerSocket ss = null;
        try{
            // bugfix for 6349541
            // specify the ip address to bind to, 50 is the default used
            // by the ssf implementation when only the port is specified
            ss = ssf.createServerSocket(port, BACKLOG, inetSocketAddress.getAddress());
            if (ciphers != null) {
                ((SSLServerSocket)ss).setEnabledCipherSuites(ciphers);
            }
        } catch(IOException e) {
            _logger.log(Level.SEVERE, "iiop.createsocket_exception",
                new Object[] { type, String.valueOf(port) });
            _logger.log(Level.SEVERE, "", e);
            throw e;
        }

	try {
	    if(type.equals(SSL_MUTUALAUTH)) {
		_logger.log(Level.FINE,"Setting Mutual auth");
		((SSLServerSocket)ss).setNeedClientAuth(true);
	    }
	} catch(Exception e) {
	    _logger.log(Level.SEVERE,"iiop.cipher_exception",e);
	    throw new IOException(e.getMessage());
	}
	if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,"Created server socket:" + ss);
	}
	return ss;
    }

    /**
     * Create an SSL socket at the specified host and port.
     * @param host
     * @param port
     * @return the socket.
     */
    private Socket createSSLSocket(String host, int port)
        throws IOException {
        SSLSocket socket = null;    
	SSLSocketFactory factory = null;
        try{
            // get socketfactory+sanity check
            // clientSslInfo is never null
            factory = clientSslInfo.getContext().getSocketFactory();

            if(_logger.isLoggable(Level.FINE)) {
                  _logger.log(Level.FINE,"Creating SSL Socket for host:" + host +" port:" + port);
            }
            String[] ssl3TlsCiphers = clientSslInfo.getSsl3TlsCiphers();
            String[] ssl2Ciphers = clientSslInfo.getSsl2Ciphers();
            String[] clientCiphers = null;
            if (ssl3TlsCiphers != null || ssl2Ciphers != null) {
                String[] socketCiphers = factory.getDefaultCipherSuites();
                clientCiphers = mergeCiphers(socketCiphers, ssl3TlsCiphers, ssl2Ciphers);
            }

            socket = (SSLSocket)factory.createSocket(host, port);
            if (clientCiphers != null) {
                socket.setEnabledCipherSuites(clientCiphers);
            }
        }catch(Exception e) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "iiop.createsocket_exception",
                new Object[] { host, String.valueOf(port) });
                _logger.log(Level.FINE, "", e);
            }
            IOException e2 = new IOException(
            "Error opening SSL socket to host="+host+" port="+port);
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
     * @param cipherSuiteStr cipherSuiteStr from xml
     * @param ssl2Enabled
     * @param ssl3Enabled
     * @param tlsEnabled
     * @return an array of enabled Ciphers
     */
    private String[] getEnabledCipherSuites(String cipherSuiteStr,
            boolean ssl2Enabled, boolean ssl3Enabled, boolean tlsEnabled) {
        String[] cipherArr = null;
        if (cipherSuiteStr != null && cipherSuiteStr.length() > 0) {
            ArrayList cipherList = new ArrayList();
            StringTokenizer tokens = new StringTokenizer(cipherSuiteStr, ",");
            while (tokens.hasMoreTokens()) {
                String cipherAction = tokens.nextToken();
                if (cipherAction.startsWith("+")) {
                    String cipher = cipherAction.substring(1);
                    CipherInfo cipherInfo = CipherInfo.getCipherInfo(cipher);
                    if (cipherInfo != null &&
                            isValidProtocolCipher(cipherInfo, ssl2Enabled,
                                ssl3Enabled, tlsEnabled)) {
                        cipherList.add(cipherInfo.getCipherName());
                    } else {
                        throw new IllegalStateException(getFormatMessage(
                            "iiop.unknown_cipher",
                            new Object[] { cipher }));
                    }
                } else if (cipherAction.startsWith("-")) {
                    String cipher = cipherAction.substring(1);
                    CipherInfo cipherInfo = CipherInfo.getCipherInfo(cipher);
                    if (cipherInfo == null ||
                            !isValidProtocolCipher(cipherInfo, ssl2Enabled,
                                ssl3Enabled, tlsEnabled)) {
                        throw new IllegalStateException(getFormatMessage(
                            "iiop.unknown_cipher",
                            new Object[] { cipher }));
                    }
                } else if (cipherAction.trim().length() > 0) {
                    throw new IllegalStateException(getFormatMessage(
                        "iiop.invalid_cipheraction",
                        new Object[] { cipherAction }));
                }
            }

            cipherArr = (String[])cipherList.toArray(
                    new String[cipherList.size()]);
        }
        return cipherArr;
    }

    /**
     * Return an array of merged ciphers.
     * @param enableCiphers  ciphers enabled by socket factory
     * @param ssl3TlsCiphers
     * @param ssl2Ciphers
     */
    private String[] mergeCiphers(String[] enableCiphers,
            String[] ssl3TlsCiphers, String[] ssl2Ciphers) {

        if (ssl3TlsCiphers == null && ssl2Ciphers == null) {
            return null;
        }

        int eSize = (enableCiphers != null)? enableCiphers.length : 0;

        if (_logger.isLoggable(Level.FINE)) {
            StringBuffer buf = new StringBuffer("Default socket ciphers: ");
            for (int i = 0; i < eSize; i++) {
                buf.append(enableCiphers[i] + ", ");
            }
            _logger.log(Level.FINE, buf.toString());
        }

        ArrayList cList = new ArrayList();
        if (ssl3TlsCiphers != null) {
            for (int i = 0; i < ssl3TlsCiphers.length; i++) {
                cList.add(ssl3TlsCiphers[i]);
            }
        } else {
            for (int i = 0; i < eSize; i++) {
                String cipher = enableCiphers[i];
                CipherInfo cInfo = CipherInfo.getCipherInfo(cipher);
                if (cInfo != null && (cInfo.isTLS() || cInfo.isSSL3())) {
                    cList.add(cipher);
                }
            }
        }
        
        if (ssl2Ciphers != null) {
            for (int i = 0; i < ssl2Ciphers.length; i++) {
                cList.add(ssl2Ciphers[i]);
            }
        } else {
            for (int i = 0; i < eSize; i++) {
                String cipher = enableCiphers[i];
                CipherInfo cInfo = CipherInfo.getCipherInfo(cipher);
                if (cInfo != null && cInfo.isSSL2()) {
                    cList.add(cipher);
                }
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Merged socket ciphers: " + cList);
        }

        return (String[])cList.toArray(new String[cList.size()]);
    }

    /**
     * Check whether given cipherInfo belongs to given protocol.
     * @param cipherInfo
     * @param ssl2Enabled
     * @param ssl3Enabled
     * @param tlsEnabled
     */
    private boolean isValidProtocolCipher(CipherInfo cipherInfo,
            boolean ssl2Enabled, boolean ssl3Enabled, boolean tlsEnabled) {
        return (tlsEnabled && cipherInfo.isTLS() ||
                ssl3Enabled && cipherInfo.isSSL3() ||
                ssl2Enabled && cipherInfo.isSSL2());
    }

    /**
     * This API get the format string from resource bundle of _logger.
     * @param key the key of the message
     * @param params the parameter array of Object
     * @return the format String for _logger
     */
    private String getFormatMessage(String key, Object[] params) {
        return MessageFormat.format(
            _logger.getResourceBundle().getString(key), params);
    }

    class SSLInfo {
        private SSLContext ctx;
        private String[] ssl3TlsCiphers = null;
        private String[] ssl2Ciphers = null;

        SSLInfo(SSLContext ctx, String[] ssl3TlsCiphers, String[] ssl2Ciphers) {
            this.ctx = ctx;
            this.ssl3TlsCiphers = ssl3TlsCiphers;
            this.ssl2Ciphers = ssl2Ciphers;
        }

        SSLContext getContext() {
            return ctx;
        }

        String[] getSsl3TlsCiphers() {
            return ssl3TlsCiphers;
        }

        String[] getSsl2Ciphers() {
            return ssl2Ciphers;
        }
    }
} 
