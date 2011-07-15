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

package com.sun.enterprise.admin.util;

import java.util.NoSuchElementException;

public class QuotedStringTokenizer
{
    private final char[] ca;
    private String delimiters = "\t ";
    private final int numTokens;
    private int curToken = 0;
    private final CharIterator iterator;

    public QuotedStringTokenizer(String s)
    {
        this(s, null);
    }

    public QuotedStringTokenizer(String s, String delim)
    {
        if (null == s)
        {
            throw new IllegalArgumentException("null param");
        }
        ca = s.toCharArray();
        if (delim != null && delim.length() > 0)
        {
            delimiters = delim;
        }
        numTokens = _countTokens();
        iterator = new CharIterator(ca);
    }
    
    public int countTokens()
    {
        return numTokens;
    }

    public boolean hasMoreTokens()
    {
        return curToken < numTokens;
    }

    public String nextToken()
    {
        if (curToken == numTokens)
            throw new NoSuchElementException();
        final StringBuffer sb = new StringBuffer();
        boolean bQuote = false;
        boolean bEscaped = false;
        char c;
        while ((c = iterator.next()) != CharIterator.EOF)
        {
            boolean isDelimiter = isDelimiter(c);
            if (!isDelimiter && !bEscaped)
            {
                sb.append(c);
                if (c == '\"')
                    bQuote = !bQuote;
                char next = iterator.peekNext();
                if (next == CharIterator.EOF || (isDelimiter(next) && !bQuote))
                    break;
            }
            else if (bQuote || bEscaped)
            {
                sb.append(c);
            }
            if(c=='\\')
                bEscaped = !bEscaped;
            else
                bEscaped = false;
        }
        curToken++;
        return sb.toString();
    }

    boolean isDelimiter(char c)
    {
        return delimiters.indexOf(c) >= 0;
    }

    private int _countTokens()
    {
        int     tokens = 0;
        boolean bQuote = false;
        boolean bEscaped = false;
        final   CharIterator it = new CharIterator(ca);
        char    c;

        while ((c = it.next()) != CharIterator.EOF)
        {
            char next = it.peekNext();
            if (!isDelimiter(c) && !bEscaped)
            {
                if (c == '\"')
                    bQuote = !bQuote;
                if (next == CharIterator.EOF || (isDelimiter(next) && !bQuote))
                    tokens++;
            }
            else if (next == CharIterator.EOF && bQuote) //eg :- "\" "
                tokens++;
            if(c=='\\')
                bEscaped = !bEscaped;
            else
                bEscaped = false;
        }
        return tokens;
    }

    private static final class CharIterator
    {
        static final char EOF = '\uFFFF';

        private final char[] carr;
        private int index = 0;

        private CharIterator(char[] ca)
        {
            carr = ca;
        }

        char next()
        {
            if (index >= carr.length)
                return EOF;
            char c = carr[index];
            ++index;
            return c;
        }

        char peekNext()
        {
            if (index >= carr.length)
                return EOF;
            return carr[index];
        }
    }
}
