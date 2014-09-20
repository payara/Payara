/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

import com.sun.appserv.ProxyHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.catalina.connector.Constants;

/**
 * Default ProxyHandler implementation.
 */
public class ProxyHandlerImpl extends ProxyHandler {

    /**
     * Gets the SSL client certificate chain with which the client
     * had authenticated itself to the SSL offloader, and which the
     * SSL offloader has added as a custom request header on the
     * given request.
     *
     * @param request The request from which to retrieve the SSL client
     * certificate chain
     *
     * @return Array of java.security.cert.X509Certificate instances
     * representing the SSL client certificate chain, or null if this
     * information is not available from the given request
     *
     * @throws CertificateException if the certificate chain retrieved
     * from the request header cannot be parsed
     */
    public X509Certificate[] getSSLClientCertificateChain(
                        HttpServletRequest request)
            throws CertificateException {

        X509Certificate[] certs = null;

        String clientCert = request.getHeader(Constants.PROXY_AUTH_CERT);
        if (clientCert != null) {
            clientCert = clientCert.replaceAll("% d% a", "\n");
            clientCert = "-----BEGIN CERTIFICATE-----\n" + clientCert
                         + "\n-----END CERTIFICATE-----";
            byte[] certBytes = clientCert.getBytes(Charset.defaultCharset());
            ByteArrayInputStream bais = new ByteArrayInputStream(certBytes);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certs = new X509Certificate[1];
            certs[0] = (X509Certificate) cf.generateCertificate(bais);
        }

        return certs;
    }

    /**
     * Returns the SSL keysize with which the original client request that
     * was intercepted by the SSL offloader has been protected, and which
     * the SSL offloader has added as a custom request header on the
     * given request.
     *
     * @param request The request from which to retrieve the SSL key
     * size
     *
     * @return SSL keysize, or -1 if this information is not available from
     * the given request
     */
    public int getSSLKeysize(HttpServletRequest request) {

        int keySize = -1;

        String header = request.getHeader(Constants.PROXY_KEYSIZE);
        if (header != null) {
            keySize = Integer.parseInt(header);
        }

        return keySize;   
    }

    /**
     * Gets the Internet Protocol (IP) source port of the client request that
     * was intercepted by the proxy server.
     *
     * @param request The request from which to retrieve the IP source port
     * of the original client request
     *
     * @return IP source port of the original client request, or null if this
     * information is not available from the given request
     */
    public String getRemoteAddress(HttpServletRequest request) {
        return request.getHeader(Constants.PROXY_IP);
    }
}
