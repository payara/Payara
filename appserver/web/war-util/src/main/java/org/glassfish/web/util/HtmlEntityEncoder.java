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

package org.glassfish.web.util;

/**
 * This class encodes HTML display content for preventing XSS.
 */
public class HtmlEntityEncoder {
    /**
     * xssStrings:
     *     " => 34, % => 37, & => 38, ' => 39, ( => 40,
     *     ) => 41, + => 43, ; => 59, < => 60, > => 62
     */
    private static String[] xssStrings = {  //34-62
        "&quot;", null,
        null, "&#37;", "&amp;", "&#39;", "&#40;",
        "&#41;", null, "&#43;", null, null,
        null, null, null, null, null,
        null, null, null, null, null,
        null, null, null, "&#59;", "&lt;",
        null, "&gt;"
    };

    private static final int START = 34;
    private static final char DEFAULT_CHAR = ' ';

    public static String encodeXSS(Object obj) {
        if (obj == null) {
            return null;
        } else {
            return encodeXSS(obj.toString());
        }
    }

    /**
     * Encode
     * a) the following visible characters:
     *     " => 34, % => 37, & => 38, ' => 39, ( => 40,
     *     ) => 41, + => 43,
     *     ; => 59, < => 60,
     *     > => 62,
     * b) ignore control characters
     * c) ignore undefined characters
     */
    public static String encodeXSS(String s) {
        if (s == null) {
            return null;
        }

        int len = s.length();
        if (len == 0) {
            return s;
        }

        StringBuilder sb = null;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            int ind =(int)c - START;
            if (ind > -1 && ind < xssStrings.length && xssStrings[ind] != null) {
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(s.substring(0, i));
                }
                sb.append(xssStrings[ind]);
            } else if (32 <= c && c <= 126 || 128 <= c && c <= 255 || c == 9
                    || Character.isWhitespace(c)) {
                 if (sb != null) {
                     sb.append(c);
                 }
            } else if (Character.isISOControl(c)) { // skip
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(s.substring(0, i));
                }
                sb.append(DEFAULT_CHAR);
            } else if (Character.isHighSurrogate(c)) {
                boolean valid = false;
                if (i + 1 < len) {
                    char nextC = s.charAt(i + 1);
                    if (Character.isLowSurrogate(nextC)) {
                        valid = true;
                        if (sb != null) {
                            sb.append(c);
                            sb.append(nextC);
                        }
                    }
                }
                if (!valid) {
                    if (sb == null) {
                        sb = new StringBuilder(len);
                        sb.append(s.substring(0, i));
                    }
                    sb.append(DEFAULT_CHAR);
                }
                i++; // a pair
            } else if (Character.isDefined(c)) {
                if (sb != null) {
                    sb.append(c);
                }
            } else { // skip
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(s.substring(0, i));
                }
                sb.append(DEFAULT_CHAR);
            }
        }

        if (sb != null) {
            return sb.toString();
        } else {
            return s;
        }
    }
}
