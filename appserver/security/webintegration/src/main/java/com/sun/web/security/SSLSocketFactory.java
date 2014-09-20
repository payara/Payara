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

package com.sun.web.security;

import java.io.*;
import java.net.*;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.security.ssl.SSLUtils;
//V3:Commented import com.sun.enterprise.ServerConfiguration;
//V3:Commented import com.sun.web.server.*;
//V3:Commented import com.sun.enterprise.server.J2EEServer;
import com.sun.enterprise.security.ssl.J2EEKeyManager;

import java.util.logging.*;
import com.sun.logging.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.X509KeyManager;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.SharedSecureRandom;


/**
 * SSL server socket factory. 
 *
 * @author Harish Prabandham
 * @author Vivek Nagar
 * @author Harpreet Singh
 */
// TODO: this should become a HK2 component
public class SSLSocketFactory implements org.apache.catalina.net.ServerSocketFactory {

    static Logger _logger=LogDomains.getLogger(SSLSocketFactory.class, LogDomains.WEB_LOGGER);

    private static final boolean clientAuth = false;

    private static LocalStringManagerImpl localStrings = 
	new LocalStringManagerImpl(SSLSocketFactory.class);

    private SSLContext context = null;
    private javax.net.ssl.SSLServerSocketFactory factory = null;
    private String cipherSuites[];
    
    private static KeyManager[] keyManagers = null;
    private static TrustManager[] trustManagers = null;

    //XXX initStoresAtStartup may call more than once, should clean up later
    //copied from SSLUtils : V3 to break dependency of this SSLUtils on this Class.
    private static boolean initialized = false;

    /**
     * Create the SSL socket factory. Initialize the key managers and
     * trust managers which are passed to the SSL context.
     */
    public SSLSocketFactory () {
	try {
	    if(keyManagers == null || trustManagers == null) {
		initStoresAtStartup();
	    }
	    context = SSLContext.getInstance("TLS");
	    context.init(keyManagers, trustManagers, SharedSecureRandom.get());

	    factory = context.getServerSocketFactory();
	    cipherSuites = factory.getSupportedCipherSuites();
	    
            for(int i=0; i < cipherSuites.length; ++i) {
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.log(Level.FINEST,"Suite: " + cipherSuites[i]);
                }
	    }
            
	} catch(Exception e) {
	  _logger.log(Level.SEVERE,
                      "web_security.excep_sslsockfact", e.getMessage());
	}
    }

    /**
     * Create the socket at the specified port.
     * @param port the port number.
     * @return the SSL server socket.
     */
    public ServerSocket createSocket (int port)
    throws IOException
    {
	SSLServerSocket socket = 
	    (SSLServerSocket) factory.createServerSocket(port);
	init(socket);
	return socket;
    }

    /**
     * Specify whether the server will require client authentication.
     * @param socket the SSL server socket.
     */
    private void init(SSLServerSocket socket) {
	// Some initialization goes here.....
	// socket.setEnabledCipherSuites(cipherSuites);
	socket.setNeedClientAuth(clientAuth);
    }

    /**
     * Create the socket at the specified port.
     * @param port the port number.
     * @return the SSL server socket.
     */
    public ServerSocket createSocket (int port, int backlog)
    throws IOException
    {
	SSLServerSocket socket = (SSLServerSocket)
	    factory.createServerSocket(port, backlog);
	init(socket);
	return socket;
    }

    /**
     * Create the socket at the specified port.
     * @param port the port number.
     * @return the SSL server socket.
     */
    public ServerSocket createSocket (int port, int backlog, InetAddress ifAddress)
    throws IOException
    {
	SSLServerSocket socket = (SSLServerSocket)
	    factory.createServerSocket(port, backlog, ifAddress);
	init(socket);
	return socket;
    }

    //V3: to break dependency of SSLUtils on this class
//    public static void setManagers(KeyManager[] kmgrs, TrustManager[] tmgrs) {
//        keyManagers = kmgrs;
//        trustManagers = tmgrs;
//    }
    //V3: Copied from SSLUtils to break dependency of SSLUtils on this class
     public static synchronized void initStoresAtStartup()
	throws Exception
    {
        if (initialized) {
            return;
        }
        ServiceLocator habitat = Globals.getDefaultHabitat();
        SSLUtils sslUtils = habitat.getService(SSLUtils.class);

        keyManagers = sslUtils.getKeyManagers();
        trustManagers = sslUtils.getTrustManagers();
	
        // Creating a default SSLContext and HttpsURLConnection for clients 
        // that use Https
        SSLContext ctx = SSLContext.getInstance("TLS");
        String keyAlias = System.getProperty(SSLUtils.HTTPS_OUTBOUND_KEY_ALIAS);
        KeyManager[] kMgrs = sslUtils.getKeyManagers();
        if (keyAlias != null && keyAlias.length() > 0 && kMgrs != null) {
            for (int i = 0; i < kMgrs.length; i++) {
                kMgrs[i] = new J2EEKeyManager((X509KeyManager)kMgrs[i], keyAlias);
            }
        }
	ctx.init(kMgrs, sslUtils.getTrustManagers(), null);
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        initialized = true;
    }
}
