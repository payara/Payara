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

package com.sun.enterprise.web.util;

/**
 * Parse a Cookie: header into individual tokens according to RFC 2109.
 */
public class CookieTokenizer
{
    /**
     * Upper bound on the number of cookie tokens to accept.  The limit is
     * based on the 4 different attributes (4.3.4) and the 20 cookie minimum
     * (6.3) given in RFC 2109 multiplied by 2 to accomodate the 2 tokens in
     * each name=value pair ("JSESSIONID=1234" is 2 tokens).
     */
    private static final int MAX_COOKIE_TOKENS = 4 * 20 * 2;

    /**
     * Array of cookie tokens.  Even indices contain name tokens while odd
     * indices contain value tokens (or null).
     */
    private String tokens[] = new String[MAX_COOKIE_TOKENS];

    /**
     * Number of cookie tokens currently in the tokens[] array.
     */
    private int numTokens = 0;

    /**
     * Parse a name=value pair from the Cookie: header.
     *
     * @param cookies The Cookie: header to parse
     * @param beginIndex The index in cookies to begin parsing from, inclusive
     */
    private int parseNameValue(String cookies, int beginIndex) {
        int length = cookies.length();
        int index = beginIndex;

        while (index < length) {
            switch (cookies.charAt(index)) {
            case ';':
            case ',':
                // Found end of name token without value
                tokens[numTokens] = cookies.substring(beginIndex, index).trim();
                if (tokens[numTokens].length() > 0) {
                    numTokens++;
                    tokens[numTokens] = null;
                    numTokens++;
                }
                return index + 1;

            case '=':
                // Found end of name token with value
                tokens[numTokens] = cookies.substring(beginIndex, index).trim();
                numTokens++;
                return parseValue(cookies, index + 1);

            case '"':
                // Skip past quoted span
                do index++; while (cookies.charAt(index) != '"');
                break;
            default:
                break;
            }

            index++;
        }

        if (index > beginIndex) {
            // Found end of name token without value
            tokens[numTokens] = cookies.substring(beginIndex, index).trim();
            if (tokens[numTokens].length() > 0) {
                numTokens++;
                tokens[numTokens] = null;
                numTokens++;
            }
        }

        return index;
    }

    /**
     * Parse the name=value tokens from a Cookie: header.
     *
     * @param cookies The Cookie: header to parse
     */
    public int tokenize(String cookies) {
        numTokens = 0;

        if (cookies != null) {
            try {
                // Advance through cookies, parsing name=value pairs
                int length = cookies.length();
                int index = 0;
                while (index < length)
                    index = parseNameValue(cookies, index);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                // Filled up the tokens[] array
            }
            catch (IndexOutOfBoundsException e) {
                // Walked off the end of the cookies header
            }
        }

        return numTokens;
    }

    /**
     * Return the number of cookie tokens parsed from the Cookie: header.
     */
    public int getNumTokens() {
        return numTokens;
    }

    /**
     * Returns a given cookie token from the Cookie: header.
     *
     * @param index The index of the cookie token to return
     */
    public String tokenAt(int index) {
        return tokens[index];
    }

    /**
     * Parse the value token from a name=value pair.
     *
     * @param cookies The Cookie: header to parse
     * @param beginIndex The index in cookies to begin parsing from, inclusive
     */
    private int parseValue(String cookies, int beginIndex) {
        int length = cookies.length();
        int index = beginIndex;

        while (index < length) {
            switch (cookies.charAt(index)) {
            case ';':
            case ',':
                // Found end of value token
                tokens[numTokens] = cookies.substring(beginIndex, index).trim();
                numTokens++;
                return index + 1;

            case '"':
                // Skip past quoted span
                do index++; while (cookies.charAt(index) != '"');
                break;
            default:
                break;
            }

            index++;
        }

        // Found end of value token
        tokens[numTokens] = cookies.substring(beginIndex, index).trim();
        numTokens++;

        return index;
    }
}
