/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util;

import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.universal.GFBase64Decoder;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.callback.*;
import org.glassfish.common.util.admin.AdminAuthenticator.AuthenticatorType;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.LocalPassword;

/**
 * Handles callbacks for admin authentication other than user-provided
 * username and password, such as the local password, a limited-use token,
 * a ReST token.
 * <p>
 * Note that some of the information the callback handler stores is really for
 * the use of the admin LoginModule.  But because we don't control how the
 * login module is instantiated or initialized - but we do control that for the
 * callback handler - we can put that information here.  This callback handler
 * sets the info in the callback, which is then available to the LoginModule.
 * 
 * @author tjquinn
 */
public class AdminCallbackHandler implements CallbackHandler {

    public static final String COOKIE_REST_TOKEN = "gfresttoken";
    public static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";
    
    
    private static final Level PROGRESS_LEVEL = Level.FINE;
    
    private static final Logger logger = GenericAdminAuthenticator.ADMSEC_LOGGER;
    
    private final Request request;
    
    private Map<String,String> headers = null;
    
    private static final GFBase64Decoder decoder = new GFBase64Decoder();
    private static final String BASIC = "Basic ";

    private final Principal clientPrincipal;
    private final String originHost;
    
    private final PasswordAuthentication passwordAuthentication;
    
    private final String specialAdminIndicator;
    private final String token;
    private final String defaultAdminUsername;
    private final LocalPassword localPassword;
    private final ServiceLocator serviceLocator;
    
    public AdminCallbackHandler(
            final ServiceLocator serviceLocator,
            final Request request,
            final String alternateHostName,
            final String defaultAdminUsername,
            final LocalPassword localPassword) throws IOException {
        this.serviceLocator = serviceLocator;
        this.request = request;
        this.defaultAdminUsername = defaultAdminUsername;
        this.localPassword = localPassword;
        clientPrincipal = request.getUserPrincipal();
        originHost = alternateHostName != null ? alternateHostName : request.getRemoteHost();
        passwordAuthentication = basicAuth();
        specialAdminIndicator = specialAdminIndicator();
        token = token();
        
        
    }
    
    ServiceLocator getServiceLocator() {
        return serviceLocator;
    }
    
    private static Map<String,String> headers(final Request req) {
        final Map<String,String> result = new HashMap<String,String>();
        for (String headerName : req.getHeaderNames()) {
            result.put(headerName(headerName), req.getHeader(headerName));
        }
        return result;
    }
    
    
//    private List<String> headers(final String headerName) {
//        return headers.get(headerName);
//    }
    
    private static String headerName(final String headerName) {
        return headerName.toLowerCase(Locale.ENGLISH);
    }
    
    private synchronized Map<String,String> headers() {
        if (headers == null) {
            headers = headers(request);
        }
        return headers;
    }
    private String header(final String headerName) {
//        final List<String> matches = headers(headerName);
//        if (matches != null && matches.size() > 0) {
//            return matches.get(0);
//        }
//        return null;
        return headers().get(headerName(headerName));
    }
    
    private PasswordAuthentication basicAuth() throws IOException {
        final String authHeader = header("Authorization");
        if (authHeader == null) {
            logger.log(PROGRESS_LEVEL, "No Authorization header found; preparing default with username {0} and empty password", defaultAdminUsername);
            return new PasswordAuthentication(defaultAdminUsername, new char[0]);
        }
        
        String enc = authHeader.substring(BASIC.length());
        String dec = new String(decoder.decodeBuffer(enc));
        int i = dec.indexOf(':');
        if (i < 0) {
            logger.log(PROGRESS_LEVEL, "Authorization header contained no : to separate the username from the password; proceeding with an empty username and empty password");
            return new PasswordAuthentication("", new char[0]);
        }
        final char[] password = dec.substring(i + 1).toCharArray();
        String username = dec.substring(0, i);
        if (username.isEmpty() && ! localPassword.isLocalPassword(new String(password))) {
            logger.log(PROGRESS_LEVEL, "Authorization header contained no username and the password is not the local password, so continue with the default username {0}", defaultAdminUsername);
            username  = defaultAdminUsername;
        }
        logger.log(PROGRESS_LEVEL, "basicAuth processing returning PasswordAuthentication with username {0}", username);
        return new PasswordAuthentication(username, password);    
        
    }
    
    private String specialAdminIndicator() {
        return header(SecureAdmin.Util.ADMIN_INDICATOR_HEADER_NAME);
    }
    
    private String token() {
        return header(SecureAdmin.Util.ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME);
    }
    
    private String restToken() {
        final Cookie[] cookies = request.getCookies();
        String result = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_REST_TOKEN.equals(cookie.getName())) {
                    result = cookie.getValue();
                }
            }
        }
        
        if (result == null) {
            result = request.getHeader(HEADER_X_AUTH_TOKEN);
        }
        return result;
    }
    
    public String getRemoteHost() {
        return originHost;
    }
    
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback cb : callbacks) {
            if (cb instanceof NameCallback) {
                ((NameCallback) cb).setName(passwordAuthentication.getUserName());
            } else if (cb instanceof PasswordCallback) {
                ((PasswordCallback) cb).setPassword(passwordAuthentication.getPassword());
            } else if (cb instanceof TextInputCallback) {
                final TextInputCallback ticb = (TextInputCallback) cb;
                final String prompt = ticb.getPrompt();
                if (AuthenticatorType.ADMIN_INDICATOR.name().equals(prompt)) {
                    ticb.setText(specialAdminIndicator());
                } else if (AuthenticatorType.ADMIN_TOKEN.name().equals(prompt)) {
                    ticb.setText(token());
                } else if (AuthenticatorType.REMOTE_HOST.name().equals(prompt)) {
                    ticb.setText(remoteHost());
                } else if (AuthenticatorType.REST_TOKEN.name().equals(prompt)) {
                    ticb.setText(restToken());
                } else if (AuthenticatorType.REMOTE_ADDR.name().equals(prompt)) {
                    ticb.setText(remoteAddr());
                }
            } else if (cb instanceof AdminLoginModule.PrincipalCallback) {
                ((AdminLoginModule.PrincipalCallback) cb).setPrincipal(clientPrincipal);
            }
        }
    }
    
    PasswordAuthentication pw() {
        return passwordAuthentication;
    }
    
    Principal clientPrincipal() {
        return clientPrincipal;
    }
    
    String tkn() {
        return token;
    }
    
    String remoteHost() {
        return originHost;
    }
    
    String adminIndicator() {
        return specialAdminIndicator;
    }
    
    String remoteAddr() {
        return request.getRemoteAddr();
    }
    
}
