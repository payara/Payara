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

package com.sun.appserv;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import javax.servlet.http.HttpServletRequest;

/**
 * Abstract class allowing a backend appserver instance to retrieve information
 * about the original client request that was intercepted by an SSL
 * terminating proxy server (e.g., load balancer).
 * <p>
 * An implementation of this abstract class inspects a given request for
 * the custom request headers through which the proxy server communicates the
 * information about the original client request to the appserver instance,
 * and makes this information available to the appserver.
 * <p>
 * This allows the appserver to work with any number of 3rd party SSL
 * offloader implementations configured on the front-end web server, for 
 * which a corresponding ProxyHandler implementation has been configured
 * on the backend appserver.
 */
public abstract class ProxyHandler {

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
        return null;
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
        return -1;
    }

    /**
     * Gets the Internet Protocol (IP) address of the original client request
     * that was intercepted by the proxy server.
     *
     * @param request The request from which to retrieve the IP address of the
     * original client request
     *
     * @return IP address of the original client request, or null if this
     * information is not available from the given request
     */
    public String getRemoteAddress(HttpServletRequest request) {
        return null;
    }

}
