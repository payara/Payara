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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.apache.catalina.ssi;


/**
 * Parses an expression string to return the individual tokens. This is
 * patterned similar to the StreamTokenizer in the JDK but customized for SSI
 * conditional expression parsing.
 * 
 * @version $Revision: 1.2 $
 * @author Paul Speed
 */
public class ExpressionTokenizer {
    public static final int TOKEN_STRING = 0;
    public static final int TOKEN_AND = 1;
    public static final int TOKEN_OR = 2;
    public static final int TOKEN_NOT = 3;
    public static final int TOKEN_EQ = 4;
    public static final int TOKEN_NOT_EQ = 5;
    public static final int TOKEN_RBRACE = 6;
    public static final int TOKEN_LBRACE = 7;
    public static final int TOKEN_GE = 8;
    public static final int TOKEN_LE = 9;
    public static final int TOKEN_GT = 10;
    public static final int TOKEN_LT = 11;
    public static final int TOKEN_END = 12;
    private final char[] expr;
    private String tokenVal = null;
    private int index;
    private final int length;


    /**
     * Creates a new parser for the specified expression.
     */
    public ExpressionTokenizer(String expr) {
        this.expr = expr.trim().toCharArray();
        this.length = this.expr.length;
    }


    /**
     * Returns true if there are more tokens.
     */
    public boolean hasMoreTokens() {
        return index < length;
    }


    /**
     * Returns the current index for error reporting purposes.
     */
    public int getIndex() {
        return index;
    }


    protected boolean isMetaChar(char c) {
        return Character.isWhitespace(c) || c == '(' || c == ')' || c == '!'
                || c == '<' || c == '>' || c == '|' || c == '&' || c == '=';
    }


    /**
     * Returns the next token type and initializes any state variables
     * accordingly.
     */
    public int nextToken() {
        // Skip any leading white space
        while (index < length && Character.isWhitespace(expr[index]))
            index++;
        // Clear the current token val
        tokenVal = null;
        if (index == length) return TOKEN_END; // End of string
        int start = index;
        char currentChar = expr[index];
        char nextChar = (char)0;
        index++;
        if (index < length) nextChar = expr[index];
        // Check for a known token start
        switch (currentChar) {
            case '(' :
                return TOKEN_LBRACE;
            case ')' :
                return TOKEN_RBRACE;
            case '=' :
                return TOKEN_EQ;
            case '!' :
                if (nextChar == '=') {
                    index++;
                    return TOKEN_NOT_EQ;
                } else {
                    return TOKEN_NOT;
                }
            case '|' :
                if (nextChar == '|') {
                    index++;
                    return TOKEN_OR;
                }
                break;
            case '&' :
                if (nextChar == '&') {
                    index++;
                    return TOKEN_AND;
                }
                break;
            case '>' :
                if (nextChar == '=') {
                    index++;
                    return TOKEN_GE; // Greater than or equal
                } else {
                    return TOKEN_GT; // Greater than
                }
            case '<' :
                if (nextChar == '=') {
                    index++;
                    return TOKEN_LE; // Less than or equal
                } else {
                    return TOKEN_LT; // Less than
                }
            default :
                // Otherwise it's a string
                break;
        }
        int end = index;
        // If it's a quoted string then end is the next unescaped quote
        if (currentChar == '"' || currentChar == '\'') {
            char endChar = currentChar;
            boolean escaped = false;
            start++;
            for (; index < length; index++) {
                if (expr[index] == '\\' && !escaped) {
                    escaped = true;
                    continue;
                }
                if (expr[index] == endChar && !escaped) break;
                escaped = false;
            }
            end = index;
            index++; // Skip the end quote
        } else {
            // End is the next whitespace character
            for (; index < length; index++) {
                if (isMetaChar(expr[index])) break;
            }
            end = index;
        }
        // Extract the string from the array
        this.tokenVal = new String(expr, start, end - start);
        return TOKEN_STRING;
    }


    /**
     * Returns the String value of the token if it was type TOKEN_STRING.
     * Otherwise null is returned.
     */
    public String getTokenValue() {
        return tokenVal;
    }
}
