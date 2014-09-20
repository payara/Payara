/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ssl;

import java.io.IOException;
import java.security.KeyStore;
import java.util.logging.Level;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.glassfish.grizzly.config.ssl.JSSE14SocketFactory;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Sudarsan Sridhar
 */
public class GlassfishServerSocketFactory extends JSSE14SocketFactory {

    private SSLUtils sslUtils;

    @Override
    protected KeyManager[] getKeyManagers(String algorithm, String keyAlias) throws Exception {
        if (sslUtils == null) {
            initSSLUtils();
        }
        String keystoreFile = (String) attributes.get("keystore");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Keystore file= {0}", keystoreFile);
        }

        String keystoreType = (String) attributes.get("keystoreType");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Keystore type= {0}", keystoreType);
        }
        KeyManager[] kMgrs = sslUtils.getKeyManagers(algorithm);
        if (keyAlias != null && keyAlias.length() > 0 && kMgrs != null) {
            for (int i = 0; i < kMgrs.length; i++) {
                kMgrs[i] = new J2EEKeyManager((X509KeyManager) kMgrs[i], keyAlias);
            }
        }
        return kMgrs;
    }

    @Override
    protected KeyStore getTrustStore() throws IOException {
        if (sslUtils == null) {
            initSSLUtils();
        }
        return sslUtils.getTrustStore();
    }

    private void initSSLUtils() {
        if (sslUtils == null) {
            if (Globals.getDefaultHabitat() != null) {
                sslUtils = Globals.getDefaultHabitat().getService(SSLUtils.class);
            } else {
                sslUtils = new SSLUtils();
                sslUtils.postConstruct();
            }
        }
    }
}
