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

package org.glassfish.admin.amx.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import org.glassfish.admin.amx.util.jmx.JMXUtil;

/**
Escapes/unescapes strings
 */
public final class StringUtil
{
    private StringUtil()
    {
    }

    public final static char QUOTE_CHAR = '\"';

    public final static String QUOTE = "" + QUOTE_CHAR;

    /**
    Line separator as returned by System.getProperty()
     */
    public final static String LS = System.getProperty("line.separator", "\n");

    public static String quote(Object o)
    {
        return (quote(o, QUOTE_CHAR));
    }

    public static String quote(Object o, char leftHandChar)
    {
        final String s = o == null ? "null" : toString(o);

        char leftChar = leftHandChar;
        char rightChar = leftHandChar;

        if (leftHandChar == '(')
        {
            rightChar = ')';
        }
        else if (leftHandChar == '{')
        {
            rightChar = '}';
        }
        else if (leftHandChar == '[')
        {
            rightChar = ']';
        }
        else if (leftHandChar == '<')
        {
            rightChar = '>';
        }
        else
        {
            // same char on both left and right
        }

        final String out = leftChar + s + rightChar;

        return (out);
    }

    public static String toHexString(byte theByte)
    {
        String result = Integer.toHexString(((int) theByte) & 0x000000FF);
        if (result.length() == 1)
        {
            result = "0" + result;
        }
        return (result);
    }

    public static String toHexString(byte[] bytes)
    {
        return (toHexString(bytes, null));
    }

    public static String toHexString(byte[] bytes, String delim)
    {
        final StringBuffer buf = new StringBuffer();

        if (bytes.length == 0)
        {
            // nothing
        }
        else if (delim == null || delim.length() == 0)
        {
            for (int i = 0; i < bytes.length; ++i)
            {
                buf.append(toHexString(bytes[i]));
            }
        }
        else
        {
            for (int i = 0; i < bytes.length; ++i)
            {
                buf.append(toHexString(bytes[i])).append(delim);
            }

            // remove trailing delim
            buf.setLength(buf.length() - 1);
        }

        return (buf.toString());
    }

    public static String stripSuffix(
            final String s,
            final String suffix)
    {
        String result = s;

        if (s.endsWith(suffix))
        {
            result = s.substring(0, s.length() - suffix.length());
        }

        return (result);
    }

    public static String replaceSuffix(
            final String s,
            final String fromSuffix,
            final String toSuffix)
    {
        if (!s.endsWith(fromSuffix))
        {
            throw new IllegalArgumentException(fromSuffix);
        }

        return (stripSuffix(s, fromSuffix) + toSuffix);
    }

    public static String stripPrefix(
            final String s,
            final String prefix)
    {
        String result = s;

        if (s.startsWith(prefix))
        {
            result = s.substring(prefix.length(), s.length());
        }

        return (result);
    }

    public static String stripPrefixAndSuffix(
            final String s,
            final String prefix,
            final String suffix)
    {
        return stripPrefix(stripSuffix(s, suffix), prefix);
    }

    public static String upperCaseFirstLetter(final String s)
    {
        String result = s;

        if (s.length() >= 1)
        {
            result = s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1, s.length());
        }

        return (result);
    }

    public static String toString(final Object o)
    {
        String s;

        if (o instanceof String)
        {
            s = (String) o;
        }
        else if (o instanceof Throwable)
        {
            s = ExceptionUtil.toString((Throwable) o);
        }
        /*
        else if ( o instanceof ObjectName )
        {
        s   = JMXUtil.toString( (ObjectName)o );
        }
         */
        else if (o instanceof Attribute)
        {
            final Attribute a = (Attribute) o;
            s = a.getName() + "=" + toString(a.getValue());
        }
        else if (o instanceof AttributeList)
        {
            final Map<String, Object> items = JMXUtil.attributeListToValueMap((AttributeList) o);
            s = "{" + MapUtil.toString(items) + "}";
        }
        else if (o instanceof byte[])
        {
            final byte[] b = byte[].class.cast(o);
            s = "byte[] of length " + b.length;
        }
        else if (o == null)
        {
            s = "null";
        }
        else if (o instanceof Object[])
        {
            s = toString(", ", (Object[]) o);
        }
        else
        {
            s = "" + o;
        }

        return s;
    }

    public static String toString(final String[] args)
    {
        return toString(", ", args);
    }

    public static String toString(final String delim, final String... args)
    {
        return toString(delim, (Object[]) args);
    }

    /**
    Turn an array (or varargs) set of Objects into a String
    using the specified delimiter.
     */
    public static String toString(final String delim, final Object... args)
    {
        String result;

        if (args == null)
        {
            result = "" + null;
        }
        else if (args.length == 0)
        {
            result = "";
        }
        else if (args.length == 1)
        {
            result = toString(args[ 0]);
        }
        else
        {
            final StringBuilder builder = new StringBuilder();

            for (int i = 0; i < args.length - 1; ++i)
            {
                builder.append(toString(args[i]));
                builder.append(delim);
            }
            builder.append(toString(args[args.length - 1]));

            result = builder.toString();
        }


        return result;
    }

    /**
    @return the prefix found, or null if not found
     */
    public static String getPrefix(
            final Set<String> prefixes,
            final String s)
    {
        String result = null;
        for (final String prefix : prefixes)
        {
            if (s.startsWith(prefix))
            {
                result = prefix;
                break;
            }
        }
        return result;
    }

    /**
    @return the String after stripping the prefix
    @throws IllegalArgumentException if no prefix found
     */
    public static String findAndStripPrefix(
            final Set<String> prefixes,
            final String s)
    {
        final String prefix = getPrefix(prefixes, s);
        if (prefix == null)
        {
            throw new IllegalArgumentException(s);
        }

        return stripPrefix(s, prefix);
    }

    private static String NEWLINE_STR = null;

    public static String NEWLINE()
    {
        if (NEWLINE_STR == null)
        {
            NEWLINE_STR = System.getProperty("line.separator");
        }
        return NEWLINE_STR;
    }

    private static double micros(final long nanos)
    {
        return (double) nanos / 1000;
    }

    private static double millis(final long nanos)
    {
        return (double) nanos / (1000 * 1000);
    }

    private static double seconds(final long nanos)
    {
        return (double) nanos / (1000 * 1000 * (long) 1000);
    }

    /**
    @param nanos    elapsed nanoseconds
    @return a String describing the elapsed duration in seconds
     */
    public static String getSecondsString(final long nanos)
    {
        return getTimingString(nanos, TimeUnit.SECONDS);
    }

    /**
    @param nanos    elapsed nanoseconds
    @return a String describing the elapsed duration in seconds
     */
    public static String getMillisString(final long nanos)
    {
        return getTimingString(nanos, TimeUnit.MILLISECONDS);
    }

    private static final String NANOS_FORMAT = "%d ns";

    private static final String MICROS_FORMAT = "%.1f micros";

    private static final String MILLIS_FORMAT = "%.1f ms";

    private static final String SECONDS_FORMAT = "%.3f sec";

    public static String getTimingString(
            final long nanos,
            final TimeUnit timeUnit)
    {
        String result = null;

        if (timeUnit == TimeUnit.NANOSECONDS)
        {
            result = String.format(NANOS_FORMAT, nanos);
        }
        else if (timeUnit == TimeUnit.MICROSECONDS)
        {
            result = String.format(MICROS_FORMAT, micros(nanos));
        }
        else if (timeUnit == TimeUnit.MILLISECONDS)
        {
            result = String.format(MILLIS_FORMAT, millis(nanos));
        }
        else if (timeUnit == TimeUnit.SECONDS)
        {
            result = String.format(SECONDS_FORMAT, seconds(nanos));
        }

        return result;
    }

    /**
    Get a String representing the specified number of nanoseconds, choosing
    the appropriate units based on magnitude of the value.

    @param nanos    elapsed nanoseconds
    @return a String describing the elapsed duration
     */
    public static String getTimingString(final long nanos)
    {
        String runTimeString;
        final long MICROSECOND = 1000;
        final long MILLISECOND = 1000 * MICROSECOND;
        if (nanos < 10 * MICROSECOND)
        {
            runTimeString = nanos + " nanoseconds";
        }
        else if (nanos < 10 * MILLISECOND)
        {
            runTimeString = (nanos / MICROSECOND) + " microseconds";
        }
        else
        {
            runTimeString = (nanos / MILLISECOND) + " milliseconds";
        }

        return runTimeString;
    }

    /**
    @return String[]
     */
    public static <T> String[] toStringArray(final Collection<T> c)
    {
        final String[] strings = new String[c.size()];

        int i = 0;
        for (final Object o : c)
        {
            strings[i] = toString(o);
            ++i;
        }

        return (strings);
    }

    public static String[] toStringArray(final Object[] items)
    {
        final String[] strings = new String[items.length];

        int i = 0;
        for (final Object o : items)
        {
            strings[i] = toString(o);
            ++i;
        }

        return (strings);
    }

    /**
    @return a String
     */
    public static String toString(
            final Collection c,
            final String delim)
    {
        final String[] strings = toStringArray(c);
        //Arrays.sort( strings );

        return StringUtil.toString(delim, (Object[]) strings);
    }

    public static String toString(final Collection c)
    {
        return toString(c, ", ");
    }

    /**
    Convert ObjectName into a Set of String.  The resulting
    strings are more readable than just a simple toString() on the ObjectName;
    they are sorted and output in preferential order.
     */
    public static List<String> objectNamesToStrings(final Collection<ObjectName> objectNames)
    {
        // sorting doesn't work on returned array, so convert to Strings first,then sort
        final List<String> result = new ArrayList<String>();

        for (final ObjectName objectName : objectNames)
        {
            result.add("" + objectName);
        }

        return (result);
    }

    /**
    Convert a Set of ObjectName into a Set of String
     */
    public static String[] objectNamesToStrings(final ObjectName[] objectNames)
    {
        final String[] strings = new String[objectNames.length];

        for (int i = 0; i < strings.length; ++i)
        {
            strings[i] = "" + objectNames[i];
        }

        return (strings);
    }
    

    public static String toLines(final List<String> items)
    {
        final StringBuilder buf = new StringBuilder();
        for (final String item : items)
        {
            buf.append( toString(item) );
            buf.append(StringUtil.LS);
        }
        return buf.toString();
    }

}
















