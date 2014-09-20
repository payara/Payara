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

package org.apache.catalina.authenticator;


import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.DigestEncoder;
import org.apache.catalina.LifecycleException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Map;
import java.util.StringTokenizer;




/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of HTTP DIGEST
 * Authentication (see RFC 2069).
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.6 $ $Date: 2007/04/17 21:33:22 $
 */

public class DigestAuthenticator
    extends AuthenticatorBase {

    // -------------------------------------------------------------- Constants

    /**
     * The MD5 helper object for this class.
     */
    protected static final DigestEncoder digestEncoder = new DigestEncoder();


    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "org.apache.catalina.authenticator.DigestAuthenticator/1.0";

    /**
     *  DIGEST implementation only supports auth quality of protection.
     */
    protected static final String QOP = "auth";

    /**
     * The default message digest algorithm to use if we cannot use
     * the requested one.
     */
    protected static final String DEFAULT_ALGORITHM = "MD5";

    private static final String EMPTY_STRING = "";


    // ----------------------------------------------------------- Constructors

    public DigestAuthenticator() {
        super();
    }


    // ----------------------------------------------------- Static Variables

    /**
     * The message digest algorithm to be used when generating session
     * identifiers. This must be an algorithm supported by the
     * <code>java.security.MessageDigest</code> class on your platform.
     */
    protected static volatile String algorithm = DEFAULT_ALGORITHM;

    /**
     * MD5 message digest provider.
     */
    protected volatile static MessageDigest messageDigest;

    // ----------------------------------------------------- Instance Variables

    /**
     * List of client nonce values currently being tracked
     
    protected Map<String,NonceInfo> cnonces;
    */

    /**
     * Maximum number of client nonces to keep in the cache. If not specified,
     * the default value of 1000 is used.
     */
    protected int cnonceCacheSize = 1000;


    /**
     * Private key.
     */
    protected String key = null;


    /**
     * How long server nonces are valid for in milliseconds. Defaults to 5
     * minutes.
     */
    protected long nonceValidity = 5 * 60 * 1000;


    /**
     * Opaque string.
     */
    protected String opaque;


    /**
     * Should the URI be validated as required by RFC2617? Can be disabled in
     * reverse proxies where the proxy has modified the URI.
     */
    protected boolean validateUri = true;



    
    // ------------------------------------------------------------- Properties

    /**
     * Return the message digest algorithm for this Manager.
     */
    public static String getAlgorithm() {
        return algorithm;
    }


    /**
     * Set the message digest algorithm for this Manager.
     *
     * @param alg The new message digest algorithm
     */
    public static synchronized void setAlgorithm(String alg) {
        algorithm = alg;
        // reset the messageDigest
        messageDigest = null;
    }

    /**
     * Return descriptive information about this Valve implementation.
     */
    @Override
    public String getInfo() {
        return (info);
    }

    public int getCnonceCacheSize() {
        return cnonceCacheSize;
    }


    public void setCnonceCacheSize(int cnonceCacheSize) {
        this.cnonceCacheSize = cnonceCacheSize;
    }


    public String getKey() {
        return key;
    }


    public void setKey(String key) {
        this.key = key;
    }


    public long getNonceValidity() {
        return nonceValidity;
    }


    public void setNonceValidity(long nonceValidity) {
        this.nonceValidity = nonceValidity;
    }


    public String getOpaque() {
        return opaque;
    }


    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }


    public boolean isValidateUri() {
        return validateUri;
    }


    public void setValidateUri(boolean validateUri) {
        this.validateUri = validateUri;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Authenticate the user making this request, based on the specified
     * login configuration.  Return <code>true</code> if any specified
     * constraint has been satisfied, or <code>false</code> if we have
     * created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication
     * should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config)
        throws IOException {

        // Have we already authenticated someone?
        Principal principal =
            ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        if (principal != null)
            return (true);

        // Validate any credentials already included with this request
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();
        String authorization = request.getAuthorization();
        DigestInfo digestInfo = new DigestInfo(getOpaque(), getNonceValidity(),
                getKey(), /*cnonces,*/ isValidateUri());

        if (authorization != null) {
            boolean validRequest = digestInfo.validate(hreq, authorization, config);
            if (validRequest) {
                principal = context.getRealm().authenticate(hreq);
                if (principal != null) {
                    String username = parseUsername(authorization);
                    register(request, response, principal,
                            Constants.DIGEST_METHOD,
                            username, null);
                    String ssoId = (String) request.getNote(
                            Constants.REQ_SSOID_NOTE);
                    if (ssoId != null) {
                        getSession(request, true);
                    }
                    return (true);
                }
            }
        }

        // Send an "unauthorized" response and an appropriate challenge

        // Next, generate a nOnce token (that is a token which is supposed
        // to be unique).
        String nonce = generateNonce(hreq);

        setAuthenticateHeader(hreq, hres, config, nonce,
                digestInfo.isNonceStale());
        hres.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        //      hres.flushBuffer();
        return (false);

    }


    // ------------------------------------------------------ Protected Methods




    /**
     * Parse the username from the specified authorization string.  If none
     * can be identified, return <code>null</code>
     *
     * @param authorization Authorization string to be parsed
     */
    protected String parseUsername(String authorization) {

        //System.out.println("Authorization token : " + authorization);
        // Validate the authorization credentials format
        if (authorization == null)
            return (null);
        if (!authorization.startsWith("Digest "))
            return (null);
        authorization = authorization.substring(7).trim();

        StringTokenizer commaTokenizer =
            new StringTokenizer(authorization, ",");

        while (commaTokenizer.hasMoreTokens()) {
            String currentToken = commaTokenizer.nextToken();
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0)
                return null;
            String currentTokenName =
                currentToken.substring(0, equalSign).trim();
            String currentTokenValue =
                currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName))
                return (removeQuotes(currentTokenValue));
        }

        return (null);

    }

    @Override
    protected String getAuthMethod() {
        return HttpServletRequest.DIGEST_AUTH;
    }

    /**
     * Removes the quotes on a string.
     */
    protected static String removeQuotes(String quotedString,
                                         boolean quotesRequired) {
        //support both quoted and non-quoted
        if (quotedString.length() > 0 && quotedString.charAt(0) != '"' &&
                !quotesRequired) {
            return quotedString;
        } else if (quotedString.length() > 2) {
            return quotedString.substring(1, quotedString.length() - 1);
        } else {
            return EMPTY_STRING;
        }
    }

    /**
     * Removes the quotes on a string.
     */
    protected static String removeQuotes(String quotedString) {
        return removeQuotes(quotedString, false);
    }


    /**
     * Generate a unique token. The token is generated according to the
     * following pattern. NOnceToken = Base64 ( MD5 ( client-IP ":"
     * time-stamp ":" private-key ) ).
     *
     * @param request HTTP Servlet request
     */
    protected String generateNonce(HttpServletRequest request) {

        long currentTime = System.currentTimeMillis();

        String ipTimeKey =
            request.getRemoteAddr() + ":" + currentTime + ":" + getKey();

        byte[] buffer = digest(ipTimeKey.getBytes(Charset.defaultCharset()));
        
        return currentTime + ":" + new String (digestEncoder.encode(buffer));
    }


    /**
     * Generates the WWW-Authenticate header.
     * <p>
     * The header MUST follow this template :
     * <pre>
     *      WWW-Authenticate    = "WWW-Authenticate" ":" "Digest"
     *                            digest-challenge
     *
     *      digest-challenge    = 1#( realm | [ domain ] | nOnce |
     *                  [ digest-opaque ] |[ stale ] | [ algorithm ] )
     *
     *      realm               = "realm" "=" realm-value
     *      realm-value         = quoted-string
     *      domain              = "domain" "=" <"> 1#URI <">
     *      nonce               = "nonce" "=" nonce-value
     *      nonce-value         = quoted-string
     *      opaque              = "opaque" "=" quoted-string
     *      stale               = "stale" "=" ( "true" | "false" )
     *      algorithm           = "algorithm" "=" ( "MD5" | token )
     * </pre>
     *
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response
     * @param config    Login configuration describing how authentication
     *              should be performed
     * @param nOnce nonce token
     */
    protected void setAuthenticateHeader(HttpServletRequest request,
                                         HttpServletResponse response,
                                         LoginConfig config,
                                         String nOnce,
                                         boolean isNonceStale) {

        // Get the realm name
        String realmName = config.getRealmName();
        if (realmName == null)
            realmName = REALM_NAME;

        String authenticateHeader;
        if (isNonceStale) {
            authenticateHeader = "Digest realm=\"" + realmName + "\", " +
            "qop=\"" + QOP + "\", nonce=\"" + nOnce + "\", " + "opaque=\"" +
            getOpaque() + "\", stale=true";
        } else {
            authenticateHeader = "Digest realm=\"" + realmName + "\", " +
            "qop=\"" + QOP + "\", nonce=\"" + nOnce + "\", " + "opaque=\"" +
            getOpaque() + "\"";
        }

        response.setHeader(AUTH_HEADER_NAME, authenticateHeader);

    }

    protected static synchronized MessageDigest getMessageDigest() {
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance(algorithm);
            } catch(NoSuchAlgorithmException e) {
                throw new IllegalStateException(
                        algorithm + " digest algorithm not available", e);
            }
        }

        return messageDigest;
    }

    protected static byte[] digest(byte[] data) {
        byte[] buffer = null;

        MessageDigest md = getMessageDigest();
        synchronized(md) {
            buffer = md.digest(data);
        }

        return buffer;
    }

        // ------------------------------------------------------- Lifecycle Methods

    @Override
    public synchronized void start() throws LifecycleException {
        super.start();

        // Generate a random secret key
        if (getKey() == null) {
            setKey(generateSessionId());
        }

        // Generate the opaque string the same way
        if (getOpaque() == null) {
            setOpaque(generateSessionId());
        }
    }

    private static class DigestInfo {

        private String opaque;
        private long nonceValidity;
        private String key;
        private boolean validateUri = true;

        private String userName = null;
        private String uri = null;
        private String response = null;
        private String nonce = null;
        private String nc = null;
        private String cnonce = null;
        private String realmName = null;
        private String qop = null;

        private boolean nonceStale = false;


        public DigestInfo(String opaque, long nonceValidity, String key,
               boolean validateUri) {
            this.opaque = opaque;
            this.nonceValidity = nonceValidity;
            this.key = key;
            this.validateUri = validateUri;
        }

        public boolean validate(HttpServletRequest request, String authorization,
                LoginConfig config) {
            // Validate the authorization credentials format
            if (authorization == null) {
                return false;
            }
            if (!authorization.startsWith("Digest ")) {
                return false;
            }
            authorization = authorization.substring(7).trim();

            // Bugzilla 37132: http://issues.apache.org/bugzilla/show_bug.cgi?id=37132
            String[] tokens = authorization.split(",(?=(?:[^\"]*\"[^\"]*\")+$)");

            String opaque_client = null;

            for (int i = 0; i < tokens.length; i++) {
                String currentToken = tokens[i];
                if (currentToken.length() == 0)
                    continue;

                int equalSign = currentToken.indexOf('=');
                if (equalSign < 0) {
                    return false;
                }
                String currentTokenName =
                    currentToken.substring(0, equalSign).trim();
                String currentTokenValue =
                    currentToken.substring(equalSign + 1).trim();
                if ("username".equals(currentTokenName))
                    userName = removeQuotes(currentTokenValue);
                if ("realm".equals(currentTokenName))
                    realmName = removeQuotes(currentTokenValue, true);
                if ("nonce".equals(currentTokenName))
                    nonce = removeQuotes(currentTokenValue);
                if ("nc".equals(currentTokenName))
                    nc = removeQuotes(currentTokenValue);
                if ("cnonce".equals(currentTokenName))
                    cnonce = removeQuotes(currentTokenValue);
                if ("qop".equals(currentTokenName))
                    qop = removeQuotes(currentTokenValue);
                if ("uri".equals(currentTokenName))
                    uri = removeQuotes(currentTokenValue);
                if ("response".equals(currentTokenName))
                    response = removeQuotes(currentTokenValue);
                if ("opaque".equals(currentTokenName))
                    opaque_client = removeQuotes(currentTokenValue);
            }

            if ( (userName == null) || (realmName == null) || (nonce == null)
                 || (uri == null) || (response == null) ) {
                return false;
            }

            // Validate the URI - should match the request line sent by client
            if (validateUri) {
                String uriQuery;
                String query = request.getQueryString();
                if (query == null) {
                    uriQuery = request.getRequestURI();
                } else {
                    uriQuery = request.getRequestURI() + "?" + query;
                }
                if (!uri.equals(uriQuery)) {
                    return false;
                }
            }

            // Validate the Realm name
            String lcRealm = config.getRealmName();
            if (lcRealm == null) {
                lcRealm = REALM_NAME;
            }
            if (!lcRealm.equals(realmName)) {
                return false;
            }

            // Validate the opaque string
            if (!this.opaque.equals(opaque_client)) {
                return false;
            }

            // Validate nonce
            int i = nonce.indexOf(":");
            if (i < 0 || (i + 1) == nonce.length()) {
                return false;
            }
            long nOnceTime;
            try {
                nOnceTime = Long.parseLong(nonce.substring(0, i));
            } catch (NumberFormatException nfe) {
                return false;
            }
            String md5clientIpTimeKey = nonce.substring(i + 1);
            long currentTime = System.currentTimeMillis();
            if ((currentTime - nOnceTime) > nonceValidity) {
                nonceStale = true;
                return false;
            }
            String serverIpTimeKey =
                request.getRemoteAddr() + ":" + nOnceTime + ":" + key;
            byte[] buffer = digest(serverIpTimeKey.getBytes(Charset.defaultCharset()));

            String md5ServerIpTimeKey = new String(digestEncoder.encode(buffer));
            if (!md5ServerIpTimeKey.equals(md5clientIpTimeKey)) {
                return false;
            }

            // Validate qop
            if (qop != null && !QOP.equals(qop)) {
                return false;
            }

            // Validate cnonce and nc
            // Check if presence of nc and nonce is consistent with presence of qop
            if (qop == null) {
                if (cnonce != null || nc != null) {
                    return false;
                }
            } else {
                if (cnonce == null || nc == null) {
                    return false;
                }
                if (nc.length() != 8) {
                    return false;
                }
                try {
                    Long.parseLong(nc, 16);
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }
            return true;
        }

        public boolean isNonceStale() {
            return nonceStale;
        }

    }
}
