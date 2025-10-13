/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package org.apache.catalina.authenticator;

import static java.text.MessageFormat.format;
import static jakarta.servlet.http.HttpServletRequest.CLIENT_CERT_AUTH;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.catalina.Globals.CERTIFICATES_ATTR;
import static org.apache.catalina.Globals.SSL_CERTIFICATE_ATTR;
import static org.apache.catalina.LogFacade.CANNOT_AUTHENTICATE_WITH_CREDENTIALS;
import static org.apache.catalina.LogFacade.LOOK_UP_CERTIFICATE_INFO;
import static org.apache.catalina.LogFacade.NO_CERTIFICATE_INCLUDED_INFO;
import static org.apache.catalina.LogFacade.NO_CLIENT_CERTIFICATE_CHAIN;
import static org.apache.catalina.LogFacade.PRINCIPAL_BEEN_AUTHENTICATED_INFO;
import static org.apache.catalina.authenticator.Constants.CERT_METHOD;
import static org.apache.catalina.authenticator.Constants.REQ_SSOID_NOTE;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.deploy.LoginConfig;

/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of authentication that utilizes SSL certificates to identify
 * client users.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2007/04/17 21:33:22 $
 */
public class SSLAuthenticator extends AuthenticatorBase {

    // ------------------------------------------------------------- Properties

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info = "org.apache.catalina.authenticator.SSLAuthenticator/1.0";
 

    // --------------------------------------------------------- Public Methods

    /**
     * Authenticate the user by checking for the existence of a certificate chain, and optionally asking a trust manager to
     * validate that we trust this user.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean authenticate(HttpRequest request, HttpResponse response, LoginConfig config) throws IOException {
        // Have we already authenticated someone?
        Principal principal = ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        if (principal != null) {
            if (debug >= 1) {
                log(format(rb.getString(PRINCIPAL_BEEN_AUTHENTICATED_INFO), principal.getName()));
            }
            return true;
        }

        // Retrieve the certificate chain for this client
        HttpServletResponse httpServletResponse = (HttpServletResponse) response.getResponse();
        if (debug >= 1) {
            log(rb.getString(LOOK_UP_CERTIFICATE_INFO));
        }

        X509Certificate certs[] = (X509Certificate[]) request.getRequest().getAttribute(CERTIFICATES_ATTR);
        if (certs == null || certs.length < 1) {
            certs = (X509Certificate[]) request.getRequest().getAttribute(SSL_CERTIFICATE_ATTR);
        }
        
        if (certs == null || certs.length < 1) {
            if (debug >= 1) {
                log(rb.getString(NO_CERTIFICATE_INCLUDED_INFO));
            }
            
            httpServletResponse.sendError(SC_BAD_REQUEST);
            response.setDetailMessage(rb.getString(NO_CLIENT_CERTIFICATE_CHAIN));
            return false;
        }

        // Authenticate the specified certificate chain
        principal = context.getRealm().authenticate(certs);
        
        if (principal == null) {
            if (debug >= 1) {
                log("Realm.authenticate() returned false");
            }
            
            httpServletResponse.sendError(SC_UNAUTHORIZED);
            response.setDetailMessage(rb.getString(CANNOT_AUTHENTICATE_WITH_CREDENTIALS));
            return false;
        }

        // Cache the principal (if requested) and record this authentication
        register(request, response, principal, CERT_METHOD, null, null);
        
        String ssoId = (String) request.getNote(REQ_SSOID_NOTE);
        if (ssoId != null) {
            getSession(request, true);
        }

        return true;
    }
    
    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override
    public String getInfo() {
        return SSLAuthenticator.info;
    }

    @Override
    protected String getAuthMethod() {
        return CLIENT_CERT_AUTH;
    }
}
