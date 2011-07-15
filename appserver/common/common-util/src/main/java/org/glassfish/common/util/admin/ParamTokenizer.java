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

package org.glassfish.common.util.admin;

import java.util.NoSuchElementException;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Separate parameters to admin commands into tokens.
 * Mostly used for parameters that take a list of values
 * separated by colons.
 *
 * @author Bill Shannon
 */
public class ParamTokenizer {
    protected int currentPosition;
    protected int maxPosition;
    protected String str;
    protected char delimiter;
    protected StringBuilder token = new StringBuilder();

    public static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ParamTokenizer.class);

    /**
     * Construct a tokenizer for the specified string.
     *
     * @param   str            a string to be parsed.
     */
    public ParamTokenizer(String str, char delimiter) {
        currentPosition = 0;
        this.str = str;
        this.delimiter = delimiter;
        maxPosition = str.length();
    }

    /**
     * Test if there are more tokens available from this tokenizer's string.
     *
     * @return  <code>true</code> if there are more tokens available from this
     *          tokenizer's string; <code>false</code> otherwise.
     */
    public boolean hasMoreTokens() {
        return (currentPosition < maxPosition);
    }

    /**
     * Return the next token from this tokenizer.
     *
     * @return     the next token from this tokenizer.
     * @exception  NoSuchElementException  if there are no more tokens in this
     *               tokenizer's string.
     */
    public String nextToken() throws NoSuchElementException {
        if (currentPosition >= maxPosition) {
            throw new NoSuchElementException(
                localStrings.getLocalString("NoMoreTokens", "No more tokens"));
        }
        return scanToken(false);
    }

    /**
     * Return the next token from this tokenizer.
     * Keep escapes and quotes intact.
     *
     * @return     the next token from this tokenizer.
     * @exception  NoSuchElementException  if there are no more tokens in this
     *               tokenizer's string.
     */
    public String nextTokenKeepEscapes() throws NoSuchElementException {
        if (currentPosition >= maxPosition) {
            throw new NoSuchElementException(
                localStrings.getLocalString("NoMoreTokens", "No more tokens"));
        }
        return scanToken(true);
    }

    /**
     * Return the next token starting at the current position.
     */
    protected String scanToken(boolean keep) throws IllegalArgumentException {
        while (currentPosition < maxPosition) {
            char c = str.charAt(currentPosition++);
            if (c == '"' || c == '\'') {
                if (keep)
                    token.append(c);
                char quote = c;
                while (currentPosition < maxPosition) {
                    c = str.charAt(currentPosition++);
                    if (c == '\\' && quote == '"') {
                        if (currentPosition >= maxPosition)
                            throw new IllegalArgumentException(
                                localStrings.getLocalString("EscapeAtEOL",
                                    "Escape at EOL"));
                        c = str.charAt(currentPosition++);
                        if (keep)
                            token.append('\\');
                    } else if (c == quote) {
                        break;
                    }
                    token.append(c);
                }
                if (c != quote)
                    throw new IllegalArgumentException(
                        localStrings.getLocalString("UnbalancedQuotes",
                            "Unbalanced quotes"));
                if (keep)
                    token.append(c);
            } else if (c == delimiter) {
                break;
            } else if (c == '\\') {
                if (currentPosition >= maxPosition)
                    throw new IllegalArgumentException(
                        localStrings.getLocalString("EscapeAtEOL",
                            "Escape at EOL"));
                c = str.charAt(currentPosition++);
                if (keep)
                    token.append('\\');
                token.append(c);
            } else {
                token.append(c);
            }
        }
        String s = token.toString();
        token.setLength(0);
        return s;
    }
}
