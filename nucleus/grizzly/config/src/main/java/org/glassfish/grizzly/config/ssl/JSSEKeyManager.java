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

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

/**
 * X509KeyManager which allows selection of a specific keypair and certificate chain (identified by their keystore alias
 * name) to be used by the server to authenticate itself to SSL clients.
 *
 * @author Jan Luehe
 */
public final class JSSEKeyManager extends X509ExtendedKeyManager {
    private final X509KeyManager delegate;
    private final String serverKeyAlias;

    /**
     * Constructor.
     *
     * @param mgr The X509KeyManager used as a delegate
     * @param serverKeyAlias The alias name of the server's key pair and supporting certificate chain
     */
    public JSSEKeyManager(X509KeyManager mgr, String serverKeyAlias) {
        delegate = mgr;
        this.serverKeyAlias = serverKeyAlias;
    }

    /**
     * Choose an alias to authenticate the client side of an SSLEngine connection given the public key type and the list
     * of certificate issuer authorities recognized by the peer (if any).
     *
     * @return The alias name for the desired key, or null if there are no matches
     */
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return delegate.chooseClientAlias(keyType, issuers, null);
    }

    /**
     * Choose an alias to authenticate the server side of an SSLEngine connection given the public key type and the list
     * of certificate issuer authorities recognized by the peer (if any).
     *
     * @return Alias name for the desired key
     */
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return serverKeyAlias;
    }

    /**
     * Choose an alias to authenticate the client side of a secure socket, given the public key type and the list of
     * certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType The key algorithm type name(s), ordered with the most-preferred key type first
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are
     * used
     * @param socket The socket to be used for this connection. This parameter can be null, in which case this method
     * will return the most generic alias to use
     *
     * @return The alias name for the desired key, or null if there are no matches
     */
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return delegate.chooseClientAlias(keyType, issuers, socket);
    }

    /**
     * Returns this key manager's server key alias that was provided in the constructor.
     *
     * @param keyType The key algorithm type name (ignored)
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are
     * used (ignored)
     * @param socket The socket to be used for this connection. This parameter can be null, in which case this method
     * will return the most generic alias to use (ignored)
     *
     * @return Alias name for the desired key
     */
    public String chooseServerAlias(String keyType, Principal[] issuers,
        Socket socket) {
        return serverKeyAlias;
    }

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias The alias name
     *
     * @return Certificate chain (ordered with the user's certificate first and the root certificate authority last), or
     *         null if the alias can't be found
     */
    public X509Certificate[] getCertificateChain(String alias) {
        return delegate.getCertificateChain(alias);
    }

    /**
     * Get the matching aliases for authenticating the client side of a secure socket, given the public key type and the
     * list of certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType The key algorithm type name
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are
     * used
     *
     * @return Array of the matching alias names, or null if there were no matches
     */
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate.getClientAliases(keyType, issuers);
    }

    /**
     * Get the matching aliases for authenticating the server side of a secure socket, given the public key type and the
     * list of certificate issuer authorities recognized by the peer (if any).
     *
     * @param keyType The key algorithm type name
     * @param issuers The list of acceptable CA issuer subject names, or null if it does not matter which issuers are
     * used
     *
     * @return Array of the matching alias names, or null if there were no matches
     */
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate.getServerAliases(keyType, issuers);
    }

    /**
     * Returns the key associated with the given alias.
     *
     * @param alias The alias name
     *
     * @return The requested key, or null if the alias can't be found
     */
    public PrivateKey getPrivateKey(String alias) {
        return delegate.getPrivateKey(alias);
    }
}
