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

package org.glassfish.admin.amx.util;

import java.util.regex.Pattern;

/**
Useful utilities for regex handling
 */
public final class RegexUtil
{
    private RegexUtil()
    {
        // disallow instantiation
    }

    private final static char BACKSLASH = '\\';

    /**
    These characters will be escaped by wildcardToJavaRegex()
     */
    public static final String REGEX_SPECIALS = BACKSLASH + "[]^$?+{}()|-!";

    /**
    Converts each String to a Pattern using wildcardToJavaRegex

    @param exprs	String[] of expressions
    @return	Pattern[], one for each String
     */
    public static Pattern[] exprsToPatterns(final String[] exprs)
    {
        return (exprsToPatterns(exprs, 0));
    }

    /**
    Converts each String to a Pattern using wildcardToJavaRegex, passing the flags.

    @param exprs	String[] of expressions
    @param flags	flags to pass to Pattern.compile
    @return	Pattern[], one for each String
     */
    public static Pattern[] exprsToPatterns(final String[] exprs, int flags)
    {
        final Pattern[] patterns = new Pattern[exprs.length];

        for (int i = 0; i < exprs.length; ++i)
        {
            patterns[i] = Pattern.compile(wildcardToJavaRegex(exprs[i]), flags);
        }
        return (patterns);
    }

    /**
    Supports the single wildcard "*".  There is no support for searching for
    a literal "*".

    Convert a string to a form suitable for passing to java.util.regex.
     */
    public static String wildcardToJavaRegex(String input)
    {
        String converted = input;

        if (input != null)
        {
            final int length = input.length();
            final StringBuffer buf = new StringBuffer();

            for (int i = 0; i < length; ++i)
            {
                final char theChar = input.charAt(i);

                if (theChar == '.')
                {
                    buf.append("[.]");
                }
                else if (theChar == '*')
                {
                    buf.append(".*");
                }
                else if (REGEX_SPECIALS.indexOf(theChar) >= 0)
                {
                    // '[' begins a set of characters
                    buf.append("" + BACKSLASH + theChar);
                }
                else
                {
                    buf.append(theChar);
                }
            }

            converted = buf.toString();

        }
        return (converted);
    }

}

