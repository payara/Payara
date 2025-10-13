/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.digest.impl;

import static com.sun.enterprise.security.auth.digest.api.Constants.A2;
import static com.sun.enterprise.security.auth.digest.api.Constants.CNONCE;
import static com.sun.enterprise.security.auth.digest.api.Constants.DATA;
import static com.sun.enterprise.security.auth.digest.api.Constants.METHOD;
import static com.sun.enterprise.security.auth.digest.api.Constants.NONCE;
import static com.sun.enterprise.security.auth.digest.api.Constants.NONCE_COUNT;
import static com.sun.enterprise.security.auth.digest.api.Constants.QOP;
import static com.sun.enterprise.security.auth.digest.api.Constants.RESPONSE;
import static com.sun.enterprise.security.auth.digest.api.Constants.URI;
import static java.util.logging.Level.SEVERE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;

/**
 * HttpDigestParamGenerator consumes Authorization header from HttpServlet request and generates Digest parameter
 * objects to be used by Digest validators.
 * 
 * @author K.Venugopal@sun.com
 */
public final class HttpDigestParamGenerator extends DigestParameterGenerator {

    private StringTokenizer commaTokenizer;
    private String userName;
    private String realmName;
    private String nOnce;
    private String nc;
    private String cnonce;
    private String qop;
    private String uri;
    private String response;
    private String method;
    private byte[] entityBody;
    private String algorithm = "MD5";
    private DigestAlgorithmParameter secret;
    private DigestAlgorithmParameter key;

    public DigestAlgorithmParameter[] generateParameters(AlgorithmParameterSpec param) throws InvalidAlgorithmParameterException {
        ServletInputStream sis = null;

        HttpServletRequest request = null;
        if (param instanceof HttpAlgorithmParameterImpl) {
            request = ((HttpAlgorithmParameterImpl) param).getValue();
        } else {
            throw new InvalidAlgorithmParameterException(param.getClass().toString());
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            return null;
        }

        if (!authorization.startsWith("Digest ")) {
            return null;
        }

        authorization = authorization.substring(7).trim();

        commaTokenizer = new StringTokenizer(authorization, ",");
        method = request.getMethod();

        while (commaTokenizer.hasMoreTokens()) {
            String currentToken = commaTokenizer.nextToken();
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0) {
                return null;
            }

            String currentTokenName = currentToken.substring(0, equalSign).trim();
            String currentTokenValue = currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName)) {
                userName = removeQuotes(currentTokenValue);
            } else if ("realm".equals(currentTokenName)) {
                realmName = removeQuotes(currentTokenValue, true);
            } else if ("nonce".equals(currentTokenName)) {
                nOnce = removeQuotes(currentTokenValue);
            } else if ("nc".equals(currentTokenName)) {
                nc = currentTokenValue;
            } else if ("cnonce".equals(currentTokenName)) {
                cnonce = removeQuotes(currentTokenValue);
            } else if ("qop".equals(currentTokenName)) {
                qop = removeQuotes(currentTokenValue);
            } else if ("uri".equals(currentTokenName)) {
                uri = removeQuotes(currentTokenValue);
            } else if ("response".equals(currentTokenName)) {
                response = removeQuotes(currentTokenValue);
            }
        }

        if ((userName == null) || (realmName == null) || (nOnce == null) || (uri == null) || (response == null)) {
            return null;
        }
        if (qop == null) {
            qop = "auth";
        }
        if ("auth-int".equals(qop)) {
            try {
                sis = request.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while (true) {
                    byte[] data = new byte[1024];
                    int len = sis.read(data, 0, 1023);
                    if (len == -1) {
                        break;
                    }
                    bos.write(data, 0, len);
                }
                entityBody = bos.toByteArray();
            } catch (IOException ex) {
                Logger.getLogger("global").log(SEVERE, null, ex);
            } finally {
                try {
                    sis.close();
                } catch (IOException ex) {
                    Logger.getLogger("global").log(SEVERE, null, ex);
                }
            }
        }

        key = getA1();
        DigestAlgorithmParameter a2 = getA2();
        DigestAlgorithmParameterImpl p1 = new DigestAlgorithmParameterImpl(NONCE, nOnce.getBytes());
        DigestAlgorithmParameter[] list = null;
        
        if ("auth-int".equals(qop) || "auth".equals(qop)) {
            DigestAlgorithmParameterImpl p2 = new DigestAlgorithmParameterImpl(NONCE_COUNT, nc.getBytes());
            DigestAlgorithmParameterImpl p3 = new DigestAlgorithmParameterImpl(CNONCE, cnonce.getBytes());
            DigestAlgorithmParameterImpl p4 = new DigestAlgorithmParameterImpl(QOP, qop.getBytes());
            list = new DigestAlgorithmParameter[5];
            list[0] = p1;
            list[1] = p2;
            list[2] = p3;
            list[3] = p4;
            list[4] = (DigestAlgorithmParameter) a2;
        } else {
            list = new DigestAlgorithmParameter[2];
            list[0] = p1;
            list[1] = (DigestAlgorithmParameter) a2;
        }
        
        secret = new DigestAlgorithmParameterImpl(RESPONSE, response.getBytes());
        DigestAlgorithmParameter[] data = new DigestAlgorithmParameter[3];
        data[0] = new NestedDigestAlgoParamImpl(DATA, list);
        data[1] = secret;
        data[2] = (DigestAlgorithmParameter) key;

        return data;
    }

    protected DigestAlgorithmParameter getA1() {
        return new KeyDigestAlgoParamImpl(algorithm, userName, realmName);
    }

    protected DigestAlgorithmParameter getA2() {
        DigestAlgorithmParameterImpl p1 = new DigestAlgorithmParameterImpl(METHOD, method.getBytes());
        DigestAlgorithmParameterImpl p2 = new DigestAlgorithmParameterImpl(URI, uri.getBytes());

        if ("auth".equals(qop)) {
            DigestAlgorithmParameterImpl[] list = new DigestAlgorithmParameterImpl[2];
            list[0] = p1;
            list[1] = p2;
            NestedDigestAlgoParamImpl a2 = new NestedDigestAlgoParamImpl(algorithm, A2, list);
            return a2;
        }

        if ("auth-int".equals(qop)) {
            AlgorithmParameterSpec[] list = new AlgorithmParameterSpec[3];
            DigestAlgorithmParameterImpl p3 = new DigestAlgorithmParameterImpl("enity-body", algorithm, entityBody);
            list[0] = p1;
            list[1] = p2;
            list[2] = p3;
            NestedDigestAlgoParamImpl a2 = new NestedDigestAlgoParamImpl(algorithm, A2, list);
            return a2;
        }

        return null;
    }

    protected static String removeQuotes(String quotedString) {
        return removeQuotes(quotedString, false);
    }

    protected static String removeQuotes(String quotedString, boolean quotesRequired) {
        // support both quoted and non-quoted
        if (quotedString.length() > 0 && quotedString.charAt(0) != '"' && !quotesRequired) {
            return quotedString;
        }
        
        if (quotedString.length() > 2) {
            return quotedString.substring(1, quotedString.length() - 1);
        }
            
        return "";
    }
}
