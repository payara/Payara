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
// Portions Copyright [2018-2022] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.auth.realm.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.SharedSecureRandom;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * Custom socket factory for ldaps (SSL).
 *
 * The comparator only works in JDK 1.6 onwards. Due to a bug in JDK 1.6 compare method invocation fails with a
 * classcast exception. The caller is trying to pass java.lang.String when it should have passed javax.net.SocketFactory
 * 
 * @see com.sun.enterprise.security.auth.realm.ldap.LDAPRealm
 *
 */
public class CustomSocketFactory extends SocketFactory implements Comparator<SocketFactory> {
    private SocketFactory socketFactory;

    public static final String SSL = "SSL";
    protected static final Logger LOGGER = SecurityLoggerInfo.getLogger();
    protected static final StringManager sm = StringManager.getManager(CustomSocketFactory.class);
    private static final CustomSocketFactory customSocketFactory = new CustomSocketFactory();

    public CustomSocketFactory() {
        SSLUtils sslUtils = Globals.getDefaultHabitat().getService(SSLUtils.class);
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance(SSL);
            sc.init(sslUtils.getKeyManagers(), sslUtils.getTrustManagers(), SharedSecureRandom.get());
            socketFactory = sc.getSocketFactory();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, SecurityLoggerInfo.securityExceptionError, ex);
        }
    }

    /**
     * Overriding createSocket() to fix issue FISH-6567 when having connectTimeout for Ldap Connections.
     * The cause of the issue is that new implementation from blocking mechanism during creation of socket connections
     * is setting a positive value for the parameter connectTimeout instead of using -1 (as previous versions did)
     * to control the creation. For more information about the changes please check
     * @see <a href="https://github.com/openjdk/jdk/pull/6568">jdk connection timeout pr</a>
     * @return Socket instance with default ssl context
     * @throws IOException
     */
    @Override
    public Socket createSocket() throws IOException {
        return socketFactory.createSocket();
    }

    /**
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
     */
    @Override
    public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
        return socketFactory.createSocket(arg0, arg1);
    }

    /**
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
     */
    @Override
    public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
        return socketFactory.createSocket(arg0, arg1);
    }

    /**
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
     */
    @Override
    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException {
        return socketFactory.createSocket(arg0, arg1, arg2, arg3);
    }

    /**
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)
     */
    @Override
    public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
        return socketFactory.createSocket(arg0, arg1, arg2, arg3);
    }

    @Override
    public int compare(SocketFactory s1, SocketFactory s2) {
        return s1.getClass().toString().compareTo(s2.getClass().toString());
    }

    public static SocketFactory getDefault() {
        return customSocketFactory;
    }

}
