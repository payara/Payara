/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;


/**
 * @author rajeshwar patil
 */
public class InputReader {

    /**
     * Construct a InputReader from a string.
     *
     * @param reader     A reader.
     */
    public InputReader(Reader reader) {
        this.reader = reader.markSupported() ? 
        		reader : new BufferedReader(reader);
        this.useLastChar = false;
        this.index = 0;
    }


    /**
     * Construct a InputReader from a string.
     *
     * @param s     A source string.
     */
    public InputReader(String s) {
        this(new StringReader(s));
    }


    /**
     * Back up one character.
     */
    public void back() throws InputException {
        if (useLastChar || index <= 0) {
            throw new InputException("Stepping back two steps is not supported");
        }
        index -= 1;
        useLastChar = true;
    }


    /**
    * Get the next character in the source string.
     *
     * @return The next character, or 0 if past the end of the source string.
     */
    public char next() throws InputException {
        if (this.useLastChar) {
        	this.useLastChar = false;
            if (this.lastChar != 0) {
            	this.index += 1;
            }
            return this.lastChar;
        } 
        int c;
        try {
            c = this.reader.read();
        } catch (IOException exc) {
            throw new InputException(exc);
        }

        if (c <= 0) { // End of stream
        	this.lastChar = 0;
            return 0;
        } 
    	this.index += 1;
    	this.lastChar = (char) c;
        return this.lastChar;
    }


    /**
     * Resturns InputException to signal a syntax error.
     *
     * @param message The error message.
     * @return  A InputException object, suitable for throwing
     */
    public InputException error(String message) {
        return new InputException(message + toString());
    }


    /**
     * Get the next char in the string, skipping whitespace.
     * @throws InputException
     * @return  A character, or 0 if there are no more characters.
     */
    public char nextNonSpace() throws InputException {
        for (;;) {
            char c = next();
            if (c == 0 || c > ' ') {
                return c;
            }
        }
    }


    /**
     * Get the next n characters.
     *
     * @param n     The number of characters to take.
     * @return      A string of n characters.
     * @throws InputException
     *   Substring bounds error if there are not
     *   n characters remaining in the source string.
     */
    public String next(int n) throws InputException {
        if (n == 0) {
            return "";
        }

        char[] buffer = new char[n];
        int pos = 0;

        if (this.useLastChar) {
            this.useLastChar = false;
            buffer[0] = this.lastChar;
            pos = 1;
        } 

        try {
            int len;
            while ((pos < n) && ((len = reader.read(buffer, pos, n - pos)) != -1)) {
                pos += len;
            }
        } catch (IOException exc) {
            throw new InputException(exc);
        }
        this.index += pos;

        if (pos < n) {
            throw error("Substring bounds error");
        }

        this.lastChar = buffer[n - 1];
        return new String(buffer);
    }


    /**
     * Determine if the source string still contains characters that next()
     * can consume.
     * @return true if not yet at the end of the source.
     */
    public boolean more() throws InputException {
        char nextChar = next();
        if (nextChar == 0) {
            return false;
        } 
        back();
        return true;
    }


    private int index;
    private Reader reader;
    private char lastChar;
    private boolean useLastChar;

}
