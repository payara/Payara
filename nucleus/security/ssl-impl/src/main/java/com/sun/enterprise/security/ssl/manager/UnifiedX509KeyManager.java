/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ssl.manager;

import java.net.Socket;
import java.util.ArrayList;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509KeyManager;

/**
 * This class combines an array of X509KeyManagers into one.
 * @author Shing Wai Chan
 **/
public class UnifiedX509KeyManager implements X509KeyManager /* extends X509ExtendedKeyManager*/ {
    private X509KeyManager[] mgrs = null;
    private String[] tokenNames = null;

    /**
     * @param mgrs  
     * @param tokenNames Array of tokenNames with order corresponding to mgrs
     */
    public UnifiedX509KeyManager(X509KeyManager[] mgrs, String[] tokenNames) {
        if (mgrs == null || tokenNames == null) {
            throw new IllegalArgumentException("Null array of X509KeyManagers or tokenNames");
        }
        if (mgrs.length != tokenNames.length) {
            throw new IllegalArgumentException("Size of X509KeyManagers array and tokenNames array do not match.");
        }
        this.mgrs = mgrs;
        this.tokenNames = tokenNames;
    }

    // ---------- implements X509KeyManager ----------
    public String chooseClientAlias(String[] keyType, Principal[] issuers,
            Socket socket) {
        String alias = null;
        for (int i = 0; i < mgrs.length; i++) {
            alias = mgrs[i].chooseClientAlias(keyType, issuers, socket);
            if (alias != null) {
                break;
            }
        }
        return alias;
    }

    public String chooseServerAlias(String keyType, Principal[] issuers,
            Socket socket) {
        String alias = null;
        for (int i = 0; i < mgrs.length; i++) {
            alias = mgrs[i].chooseServerAlias(keyType, issuers, socket);
            if (alias != null) {
                break;
            }
        }
        return alias;
    }

    public X509Certificate[] getCertificateChain(String alias) {
        X509Certificate[] chain = null;
        for (int i = 0; i < mgrs.length; i++) {
            chain = mgrs[i].getCertificateChain(alias);
            if (chain != null) {
                break;
            }
        }
        return chain;
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
        ArrayList clientAliases = new ArrayList();
        for (int i = 0; i < mgrs.length; i++) {
            String[] clAliases = mgrs[i].getClientAliases(keyType, issuers);
            if (clAliases != null && clAliases.length > 0) {
                for (int j = 0; j < clAliases.length; j++) {
                    clientAliases.add(clAliases[j]);
                }
            }
        }

        return (clientAliases.size() == 0) ? null :
            (String[])clientAliases.toArray(new String[clientAliases.size()]);
    }

    public PrivateKey getPrivateKey(String alias) {
        PrivateKey privKey  = null;
        for (int i = 0; i < mgrs.length; i++) {
            privKey = mgrs[i].getPrivateKey(alias);
            if (privKey != null) {
                break;
            }
        }
        return privKey;
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
        ArrayList serverAliases = new ArrayList();
        for (int i = 0; i < mgrs.length; i++) {
            String[] serAliases = mgrs[i].getServerAliases(keyType, issuers);
            if (serAliases != null && serAliases.length > 0) {
                for (int j = 0; j < serAliases.length; j++) {
                    serverAliases.add(serAliases[j]);
                }
            }
        }

        return (serverAliases.size() == 0) ? null :
            (String[])serverAliases.toArray(new String[serverAliases.size()]);
    }

    // ---------- end of implements X509KeyManager ----------

    public X509KeyManager[] getX509KeyManagers() {
        X509KeyManager[] kmgrs = new X509KeyManager[mgrs.length];
        System.arraycopy(mgrs, 0, kmgrs, 0, mgrs.length);
        return kmgrs;
    }

    public String[] getTokenNames() {
        String[] tokens = new String[tokenNames.length];
        System.arraycopy(tokenNames, 0, tokens, 0, tokenNames.length);
        return tokens;
    }


    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return chooseClientAlias(keyType, issuers, null);
    }

    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return chooseServerAlias(keyType, issuers,null);
    }
}
