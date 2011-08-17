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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.amx.core;

import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.base.Pathnames;

/**
    Constants and regex related to pathnames.
    <p>
    Wildcarding is basic: a '*" means "0 or more characters" (a '*' is converted to
    '.*' for regex purposes).
 * @see Pathnames
 * @see PathnameParser
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class PathnameConstants
{
    private PathnameConstants() {}
    
    /** delimiter between parts of a path */
    public static final char SEPARATOR = '/';
    
    /**
        Wildcard charcter, the '*' (not a regex expression).
        Usage is similar to usage in a shell, the '*' means "zero or more
    */
    public static final String MATCH_ZERO_OR_MORE = "*";

    /** subscript left character, subscripts must be a character pair for grammar reasons */
    public static final char SUBSCRIPT_LEFT = '[';
    /** subscript right character, subscripts must be a character pair for grammar reasons */
    public static final char SUBSCRIPT_RIGHT = ']';
    
    /**
        The characters legal to use as the type portion of a pathname,
        expressed as regex compatible string, but without enclosing square brackets.
    */
    public static final String LEGAL_CHAR_FOR_TYPE = "$a-zA-Z0-9._-";
    
    /** Regex pattern for one legal character (in square braces). */
    public static final String LEGAL_CHAR_FOR_TYPE_PATTERN = "[**" + LEGAL_CHAR_FOR_TYPE + "]";
    
    /** Regex pattern for one legal character (in square braces), wildcard allowed */
   // public static final String LEGAL_CHAR_FOR_TYPE_WILD_PATTERN = "[" + LEGAL_CHAR_FOR_TYPE + "*]";
    
     /** regex pattern denoting a legal type, grouping () surrounding it */
    public static final String LEGAL_TYPE_PATTERN = "(" + LEGAL_CHAR_FOR_TYPE_PATTERN + LEGAL_CHAR_FOR_TYPE_PATTERN + "*)";
    
     /** regex pattern denoting a legal type, with wildcards, grouping () surrounding it */
   // public static String LEGAL_TYPE_WILD_PATTERN = "(" + LEGAL_CHAR_FOR_TYPE_WILD_PATTERN + "*)";

    /**
        The characters legal to use as a name.  A name may be zero length, and it may include
        the {@link #SEPARATOR} character. However, it may not include the right square brace, because
        that character terminates a subscript.
        JMX ObjectNames might have additional restrictions.
    */
    public static final String LEGAL_CHAR_FOR_NAME = "^" + SUBSCRIPT_RIGHT;

    /** Regex pattern for one legal name character (in square braces). */
    public static final String LEGAL_CHAR_FOR_NAME_PATTERN = "[" + LEGAL_CHAR_FOR_NAME + "]";

    /** Regex pattern for one legal name character (in square braces). */
   // public static final String LEGAL_CHAR_FOR_NAME_WILD_PATTERN = "[" + LEGAL_CHAR_FOR_NAME + "*]";

     /** regex pattern denoting a legal name */
    public static final String LEGAL_NAME_PATTERN = LEGAL_CHAR_FOR_NAME_PATTERN + "*";

     /** regex pattern denoting a legal name, with wildcards */
   // public static final String LEGAL_NAME_WILD_PATTERN = LEGAL_CHAR_FOR_NAME_WILD_PATTERN + "*";
    
}













