/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.security.SecurityLoggerInfo;
import org.glassfish.grizzly.ssl.SSLSupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.util.logging.Level;
import javax.security.cert.X509Certificate;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 *
 * @author Sudarsan Sridhar
 */
public class GlassfishSSLSupport implements SSLSupport {

    private final static Logger logger = SecurityLoggerInfo.getLogger();
    
    /**
     * A mapping table to determine the number of effective bits in the key
     * when using a cipher suite containing the specified cipher name.  The
     * underlying data came from the TLS Specification (RFC 2246), Appendix C.
     */
    private static final CipherData ciphers[] = {
        new CipherData("_WITH_NULL_", 0),
        new CipherData("_WITH_IDEA_CBC_", 128),
        new CipherData("_WITH_RC2_CBC_40_", 40),
        new CipherData("_WITH_RC4_40_", 40),
        new CipherData("_WITH_RC4_128_", 128),
        new CipherData("_WITH_DES40_CBC_", 40),
        new CipherData("_WITH_DES_CBC_", 56),
        new CipherData("_WITH_3DES_EDE_CBC_", 168),
        new CipherData("_WITH_AES_128_", 128),
        new CipherData("_WITH_AES_256_", 256)
    };
    
    private final SSLSocket socket;
    private final SSLEngine engine;
    private SSLSession session;

    public GlassfishSSLSupport(SSLSocket socket) {
        this.socket = socket;
        this.engine = null;
        session = socket.getSession();
    }

    public GlassfishSSLSupport(SSLEngine engine) {
        this.socket = null;
        this.engine = engine;
        if(engine != null) {
            session = engine.getSession();
        }
    }

    public String getCipherSuite() throws IOException {
        if (session == null) {
            return null;
        }
        return session.getCipherSuite();
    }

    public Object[] getPeerCertificateChain() throws IOException {
        return getPeerCertificateChain(false);
    }

    public Object[] getPeerCertificateChain(boolean force) throws IOException {
        if (session == null) {
            return null;
        }
        X509Certificate[] certs = null;
        certs = session.getPeerCertificateChain();
        if (certs == null) {
            certs = new X509Certificate[0];
        }
        if (certs.length == 0 && force) {
            session.invalidate();
            handshake();

            if (socket == null) {
                session = engine.getSession();
            } else {
                session = socket.getSession();
            }
        }
        return getX509Certs();
    }

    public Integer getKeySize() throws IOException {
        if (session == null) {
            return null;
        }
        Integer keySize = (Integer) session.getValue(KEY_SIZE_KEY);
        if (keySize == null) {
            int size = 0;
            String cipherSuite = session.getCipherSuite();
            for (CipherData cipher : ciphers) {
                if (cipherSuite.contains(cipher.phrase)) {
                    size = cipher.keySize;
                    break;
                }
            }
            keySize = size;
            session.putValue(KEY_SIZE_KEY, keySize);
        }
        return keySize;
    }

    public String getSessionId() throws IOException {
        if (session == null) {
            return null;
        }
        byte[] sessionId = session.getId();
        if (sessionId == null) {
            return null;
        }
        StringBuilder Id = new StringBuilder();
        for (byte b : sessionId) {
            String digit = Integer.toHexString(b);
            if (digit.length() < 2) {
                Id.append('0');
            } else if (digit.length() > 2) {
                digit = digit.substring(digit.length() - 2);
            }
            Id.append(digit);
        }
        return Id.toString();
    }

    private void handshake() throws IOException {
        socket.setNeedClientAuth(true);
        socket.startHandshake();
    }

    private Object[] getX509Certs() {
        X509Certificate certs[] = null;
        try {
            certs = session.getPeerCertificateChain();
        } catch (Throwable ex) {
            // Get rid of the warning in the logs when no Client-Cert is
            // available
        }

        if (certs == null) {
            certs = new X509Certificate[0];
        }
        java.security.cert.X509Certificate[] x509Certs =
                new java.security.cert.X509Certificate[certs.length];
        for (int i = 0; i < x509Certs.length; i++) {
            try {
                byte buffer[] = certs[i].getEncoded();
                CertificateFactory cf =
                        CertificateFactory.getInstance("X.509");
                ByteArrayInputStream stream =
                        new ByteArrayInputStream(buffer);
                x509Certs[i] = (java.security.cert.X509Certificate) cf.generateCertificate(stream);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINE, "Cert #{0} = {1}", new Object[]{i, x509Certs[i]});
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, SecurityLoggerInfo.convertingCertError, new Object[] {certs[i], ex.toString()});
                return null;
            }
        }

        if (x509Certs.length < 1) {
            return null;
        }
        return x509Certs;
    }
}
