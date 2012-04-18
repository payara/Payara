/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author  kedar
 */
public final class TokenValue implements Comparable {
    
    public final String token;
    public final String value;
    public final String preDelimiter;
    public final String postDelimiter;
    public final String delimitedToken;
    
    public static final String DEFAULT_DELIMITER = "%%%";
    
    /** Creates a new instance of TokenValue  - with default delimiter.
     *  Also note that if the value contains any '\' characters, then these
     *  are appended to by another '\' character to work around the Java
     *  byte code interpretation. Note that none of the arguments can be null.
     *  The value of delimiter is given by DEFAULT_DELIMITER.
     *  @param   token a String that is the name of the token in this TokenValue.
     *  @param   value a String that is the value of the token.
     *  @throws  IllegalArgumentException in case of null values.
     *  @see     #TokenValue(java.lang.String, java.lang.String, java.lang.String)
     *  @see	 #DEFAULT_DELIMITER
     *  */
    
    public TokenValue(String token, String value) {
        this(token, value, DEFAULT_DELIMITER);
    }
    
    public TokenValue(String token, String value, String delimiter) {
        this(token, value, delimiter, delimiter);
    }
    
    public TokenValue(String token, String value, String preDelimiter, String postDelimiter) {
        if (token == null || value == null || preDelimiter == null || postDelimiter == null) {
            throw new IllegalArgumentException("Null Argument");
        }
        this.token = token;
	/* Because of escaping process of a '\' by Java's bytecode
	 * interpreter in string literals */
        this.value = escapeBackslashes(value);
        this.preDelimiter = preDelimiter;
        this.postDelimiter = postDelimiter;
        this.delimitedToken = preDelimiter + token + postDelimiter;
    }
    
    public TokenValue(TokenValue other) {
        this.token = other.token;
        this.value = other.value;
        this.preDelimiter = other.preDelimiter;
        this.postDelimiter = other.postDelimiter;
        this.delimitedToken = other.delimitedToken;
    }
    
    @Override
    public int compareTo(Object other) {
        final TokenValue otherTokenValue = (TokenValue) other;
        return (this.token.compareTo(otherTokenValue.token));
    }

    @Override
    public boolean equals(Object other) {
        boolean same = false;
        if (other instanceof TokenValue) {
            same = delimitedToken.equals(((TokenValue)other).delimitedToken) &&
                   value.equals(((TokenValue)other).value);
        }
        return same;
    }
    
    @Override
    public int hashCode() {
        int result = 43;
        result = 17 * result + token.hashCode();
        result = 17 * result + preDelimiter.hashCode();
        result = 17 * result + postDelimiter.hashCode();
        result = 17 * result + value.hashCode();
        
        return ( result );
    }
    
    @Override
    public String toString() {
        return delimitedToken + "=" + value;
    }

    /** Just appends additional '\' characters in the passed string. */
    private String escapeBackslashes(String anyString) {
        final char BACK_SLASH = '\\';
        final StringBuffer escaped = new StringBuffer();
        for (int i = 0; i < anyString.length(); i++) {
            final char ch = anyString.charAt(i);
            escaped.append(ch);
            if (ch == BACK_SLASH) {
                escaped.append(BACK_SLASH);
            }
        }
        return escaped.toString();
    }
}
