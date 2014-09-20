/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.ObjectName;
import org.glassfish.admin.amx.base.DomainRoot;
import static org.glassfish.admin.amx.core.PathnameConstants.*;
import static org.glassfish.external.amx.AMX.*;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
/**
Parses a pathname into parts.
<p>
The root part (leading "/") is not included in the parts list returned
by {@link #parts}.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class PathnameParser
{
    private static void debug(final Object o)
    {
        System.out.println("" + o);
    }

    private final char mDelim;

    private final char mNameLeft;

    private final char mNameRight;

    private final String mPath;

    private final List<PathPart> mParts;

    private final boolean mIsFullPath;

    public static final class PathPart
    {
        private final String mType,  mName;

        public PathPart(final String type, final String name)
        {
            mType = type;
            mName = name;
        }

        public String type()
        {
            return mType;
        }

        public String name()
        {
            return mName;
        }
        
        public boolean isWildType()
        {
            return mType.indexOf(MATCH_ZERO_OR_MORE) >= 0;
        }
        
        public boolean isWildName()
        {
            return mName != null && mName.indexOf(MATCH_ZERO_OR_MORE) >= 0;
        }

        @Override
        public String toString()
        {
            return pathPart(mType, mName);
        }

    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();

        buf.append(mPath).append(" as ").append(mParts.size()).append(" parts: ");
        buf.append("{");
        final String delim = ", ";
        for (final PathPart part : mParts)
        {
            buf.append(part.toString());
            buf.append(delim);
        }
        if (!mParts.isEmpty())
        {
            buf.setLength(buf.length() - delim.length());
        }
        buf.append("}");
        return buf.toString();
    }

    public PathnameParser(final String path)
    {
        this(path, SEPARATOR, SUBSCRIPT_LEFT, SUBSCRIPT_RIGHT);
    }

    public PathnameParser(
            final String path,
            final char delim,
            final char nameLeft,
            final char nameRight)
    {
        mPath = path;

        mDelim = delim;
        mNameLeft = nameLeft;
        mNameRight = nameRight;

        mParts = parse();

        mIsFullPath = path.startsWith(DomainRoot.PATH);
    }

    public boolean isFullPath()
    {
        return mIsFullPath;
    }
    
    public boolean isRoot()
    {
        return mParts.isEmpty();
    }
    
    /** return true if any part of the path includes a wildcard */
    public boolean isWild()
    {
        for( final PathPart part : mParts )
        {
            if ( part.isWildType() || part.isWildName() )
            {
                return true;
            }
        }
        return false;
    }

    public List<PathPart> parts()
    {
        return mParts;
    }

    public String type()
    {
        return mParts.get(mParts.size() - 1).type();
    }

    public String name()
    {
        return mParts.get(mParts.size() - 1).name();
    }

    public String parentPath()
    {
        if (mParts.isEmpty())
        {
            return null;
        }

        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < mParts.size() - 1; ++i)
        {
            final PathPart part = mParts.get(i);
            buf.append(part.toString());
            // don't want a trailing slash
            if (i < mParts.size() - 2)
            {
                buf.append(SEPARATOR);
            }
        }

        return DomainRoot.PATH + buf.toString();
    }

    /**
        This pattern finds a type and whatever follows.
        FIXME: how to support arbitrary delimiter or subscript?
     */
    private static final Pattern TYPE_SEARCH_PATTERN = Pattern.compile(LEGAL_TYPE_PATTERN + ".*");

    /**
        This pattern finds a name up to the terminating SUBSCRIPT_RIGHT.
     */
    private static final Pattern NAME_SEARCH_PATTERN = Pattern.compile("(" + LEGAL_NAME_PATTERN + ")" + SUBSCRIPT_RIGHT + ".*");

    /* a legal type, by itself */
    private static final Pattern LEGAL_TYPE_PATTERN_COMPILED = Pattern.compile( LEGAL_TYPE_PATTERN );

    /* a legal name, by itself */
    private static final Pattern LEGAL_NAME_PATTERN_COMPILED = Pattern.compile( LEGAL_NAME_PATTERN );

    private static boolean isValidType(final String type)
    {
        final Matcher matcher = LEGAL_TYPE_PATTERN_COMPILED.matcher(type);
        return matcher.matches();
    }
    private static boolean isValidName(final String type)
    {
        final Matcher matcher = LEGAL_NAME_PATTERN_COMPILED.matcher(type);
        return matcher.matches();
    }

    /**
     */
    private void parse(final String path, final List<PathPart> parts)
    {
        //debug( "PathnameParser: parsing: " + path );
        if (path == null || path.length() == 0)
        {
            throw new IllegalArgumentException(path);
        }
        String remaining = path;

        // strip the leading "/" for DomainRoot if present, to avoid having
        // to support a type of an empty string eg ""/foo/bar
        if (remaining.startsWith(DomainRoot.PATH))
        {
            remaining = remaining.substring(DomainRoot.PATH.length());
        }

        final Pattern typePattern = TYPE_SEARCH_PATTERN;

        // how to know whether to escape the name-left char if it can be any char?
        while (remaining.length() != 0)
        {
            Matcher matcher = typePattern.matcher(remaining);
            if (!matcher.matches())
            {
                throw new IllegalArgumentException("No match: " + remaining);
            }

            final String type = matcher.group(1);
            //debug( "PathnameParser, matched type: \"" + type + "\"" );

            char matchChar;
            if (type.length() < remaining.length())
            {
                matchChar = remaining.charAt(type.length());
                remaining = remaining.substring(type.length() + 1);
            }
            else
            {
                final PathPart part = new PathPart(type, null);
                parts.add(part);
                break;
            }
            //debug( "PathnameParser, match char: \"" + matchChar + "\"" );
            //debug( "PathnameParser, remaining: \"" + remaining + "\"" );

            String name = null;
            if (matchChar == mNameLeft)
            {
                // anything goes in a name, and we do NOT allow escaped SUBSCRIPT_RIGHT,
                // so just scarf up everything untilthe next SUBSCRIPT_RIGHT.
                final int idx = remaining.indexOf(mNameRight);
                if (idx < 0)
                {
                    throw new IllegalArgumentException(path);
                }
                name = remaining.substring(0, idx);
                remaining = remaining.substring(idx + 1);
                if (remaining.length() != 0 && remaining.charAt(0) == mDelim)
                {
                    remaining = remaining.substring(1);
                }
            }

            final PathPart part = new PathPart(type, name);
            parts.add(part);

        //debug( "PathnameParser, matched part: \"" + part + "\"" );
        }

    /*
    String s = "";
    for( final PathPart part : parts ){
    s = s + "{" + part + "}";
    }
    debug( "FINAL PARSE for : " + path + " = " + s);
     */
    }

    private List<PathPart> parse()
    {
        final List<PathPart> parts = new ArrayList<PathPart>();

        parse(mPath, parts);

        return parts;
    }

    private static void checkName(final String name)
    {
        if ( name != null && ! isValidName(name) )
        {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
    }

    private static void checkType(final String type)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("Illegal type: null");
        }

        if (type.indexOf(SUBSCRIPT_LEFT) >= 0 || type.indexOf(SUBSCRIPT_RIGHT) >= 0)
        {
            throw new IllegalArgumentException("Illegal type: " + type);
        }

        if (!isValidType(type))
        {
            throw new IllegalArgumentException("Illegal type: " + type);
        }
    }

    public static String pathPart(final String type, final String name)
    {
        checkName(name);

        final String namePart = (name == null) ? "" : SUBSCRIPT_LEFT + name + SUBSCRIPT_RIGHT;

        final String part = pathPart(type) + namePart;
        return part;
    }

    public static String pathPart(final String type)
    {
        checkType(type);
        return type;
    }

    public static String path(final String parentPath, final String type, final String name)
    {
        if (parentPath != null && parentPath.length() == 0 && !type.equals(Util.deduceType(DomainRoot.class)))
        {
            throw new IllegalArgumentException("parent path cannot be the empty string");
        }

        String path = (parentPath == null || parentPath.equals(DomainRoot.PATH)) ? DomainRoot.PATH : parentPath + SEPARATOR;

        path = path + pathPart(type, name);

        // make sure it can be parsed
        new PathnameParser(path);

        return path;
    }

    public static String parentPath(final ObjectName objectName)
    {
        return Util.unquoteIfNeeded(objectName.getKeyProperty(PARENT_PATH_KEY));
    }

}













