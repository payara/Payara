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

package com.sun.enterprise.security.auth.digest.impl;


import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.sun.enterprise.security.auth.digest.api.Constants.*;


/**
 * HttpDigestParamGenerator consumes Authorization header from HttpServlet
 * request and generates Digest parameter objects to be used by Digest validators.
 * @author K.Venugopal@sun.com
 */
public final class HttpDigestParamGenerator extends DigestParameterGenerator {

    private StringTokenizer commaTokenizer = null;
    private String userName = null;
    private String realmName = null;
    private String nOnce = null;
    private String nc = null;
    private String cnonce = null;
    private String qop = null;
    private String uri = null;
    private String response = null;
    private String method = null;
    private byte[] entityBody = null;
    private String algorithm = "MD5";
    private DigestAlgorithmParameter secret = null;
    private com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter key = null;

    
    public HttpDigestParamGenerator() {
    }


    public com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter[] generateParameters(AlgorithmParameterSpec param) throws InvalidAlgorithmParameterException {
        javax.servlet.ServletInputStream sis = null;

        javax.servlet.http.HttpServletRequest request = null;
        if (param instanceof com.sun.enterprise.security.auth.digest.impl.HttpAlgorithmParameterImpl) {
            request = ((com.sun.enterprise.security.auth.digest.impl.HttpAlgorithmParameterImpl) param).getValue();
        } else {
            throw new java.security.InvalidAlgorithmParameterException(param.getClass().toString());
        }
        java.lang.String authorization = request.getHeader("Authorization");
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
            java.lang.String currentToken = commaTokenizer.nextToken();
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0) {
                return null;
            }
            java.lang.String currentTokenName = currentToken.substring(0, equalSign).trim();
            java.lang.String currentTokenValue = currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName)) {
                userName = removeQuotes(currentTokenValue);
            }else if ("realm".equals(currentTokenName)) {
                realmName = removeQuotes(currentTokenValue, true);
            }else if ("nonce".equals(currentTokenName)) {
                nOnce = removeQuotes(currentTokenValue);
            }else if ("nc".equals(currentTokenName)) {
                nc = currentTokenValue;
            }else if ("cnonce".equals(currentTokenName)) {
                cnonce = removeQuotes(currentTokenValue);
            }else if ("qop".equals(currentTokenName)) {
                qop = removeQuotes(currentTokenValue);
            }else if ("uri".equals(currentTokenName)) {
                uri = removeQuotes(currentTokenValue);
            }else if ("response".equals(currentTokenName)) {
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
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
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
                Logger.getLogger("global").log(Level.SEVERE, null, ex);
            } finally {
                try {
                    sis.close();
                } catch (IOException ex) {
                    Logger.getLogger("global").log(Level.SEVERE, null, ex);
                }
            }
        }

        key = getA1();
        com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter a2 = getA2();
        com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl p1 = new com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl(NONCE, nOnce.getBytes());
        com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter[] list = null;
        if ("auth-int".equals(qop) || "auth".equals(qop)) {
            com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl p2 = new com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl(NONCE_COUNT, nc.getBytes());
            com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl p3 = new com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl(CNONCE, cnonce.getBytes());
            com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl p4 = new com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl(QOP, qop.getBytes());
            list = new com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter[5];
            list[0] = p1;
            list[1] = p2;
            list[2] = p3;
            list[3] = p4;
            list[4] = (com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter) a2;
        } else {
            list = new com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter[2];
            list[0] = p1;
            list[1] = (com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter) a2;
        }
        secret = new com.sun.enterprise.security.auth.digest.impl.DigestAlgorithmParameterImpl(RESPONSE, response.getBytes());
        com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter[] data = new com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter[3];
        data[0] = new com.sun.enterprise.security.auth.digest.impl.NestedDigestAlgoParamImpl(DATA, list);
        data[1] = secret;
        data[2] = (com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter) key;
        return data;
    }

    protected com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter getA1() {
        return new KeyDigestAlgoParamImpl(algorithm, userName, realmName);
    }

    protected com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter getA2() {
        DigestAlgorithmParameterImpl p1 = new DigestAlgorithmParameterImpl(METHOD, method.getBytes());
        DigestAlgorithmParameterImpl p2 = new DigestAlgorithmParameterImpl(URI, uri.getBytes());
        if ("auth".equals(qop)) {
            DigestAlgorithmParameterImpl[] list = new DigestAlgorithmParameterImpl[2];
            list[0] = p1;
            list[1] = p2;
            NestedDigestAlgoParamImpl a2 = new NestedDigestAlgoParamImpl(algorithm, A2, list);
            return a2;
        } else if ("auth-int".equals(qop)) {
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
        //support both quoted and non-quoted
        if (quotedString.length() > 0 && quotedString.charAt(0) != '"' && !quotesRequired) {
            return quotedString;
        } else if (quotedString.length() > 2) {
            return quotedString.substring(1, quotedString.length() - 1);
        } else {
            return "";
        }
    }
}
