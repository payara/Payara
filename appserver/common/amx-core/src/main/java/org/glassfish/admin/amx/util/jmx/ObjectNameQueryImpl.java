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

package org.glassfish.admin.amx.util.jmx;

import org.glassfish.admin.amx.util.EnumerationIterator;

import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

public class ObjectNameQueryImpl implements ObjectNameQuery
{
    public ObjectNameQueryImpl()
    {
    }

    /**
    Return true if one (or more) of the properties match the regular expressions
    for both name and value.   Return false if no property/value combinations match.

    A null pattern matches anything.
     */
    boolean match(Hashtable properties, Pattern propertyPattern, Pattern valuePattern)
    {
        final Iterator keys = new EnumerationIterator(properties.keys());
        boolean matches = false;

        while (keys.hasNext())
        {
            final String key = (String) keys.next();

            if (propertyPattern == null || propertyPattern.matcher(key).matches())
            {
                if (valuePattern == null)
                {
                    matches = true;
                    break;
                }

                // see if value matches
                final String value = (String) properties.get(key);

                if (valuePattern.matcher(value).matches())
                {
                    matches = true;
                    break;
                }
            }
        }

        return (matches);
    }

    /**
    Match all property/value expressions against the ObjectName.

    Return true if for each property/value regular expression pair there is at least one
    property within the ObjectName whose property name and value match their respective
    patterns.

    A null regex indicates "match anything".
     */
    boolean matchAll(ObjectName name,
                     Pattern[] propertyPatterns,
                     Pattern[] valuePatterns)
    {
        boolean matches = true;

        final Hashtable properties = name.getKeyPropertyList();

        for (int i = 0; i < propertyPatterns.length; ++i)
        {
            if (!match(properties, propertyPatterns[i], valuePatterns[i]))
            {
                matches = false;
                break;
            }
        }

        return (matches);
    }

    /**
    Match all property/value expressions against the ObjectName.

    Return true if there is at least one property/value regular expression pair that
    matches a property/value pair within the ObjectName.

    A null regex indicates "match anything".
     */
    boolean matchAny(ObjectName name,
                     Pattern[] propertyPatterns,
                     Pattern[] valuePatterns)
    {
        boolean matches = false;

        final Hashtable properties = name.getKeyPropertyList();

        for (int i = 0; i < propertyPatterns.length; ++i)
        {
            if (match(properties, propertyPatterns[i], valuePatterns[i]))
            {
                matches = true;
                break;
            }
        }

        return (matches);
    }

    Pattern[] createPatterns(final String[] patternStrings, int numItems)
    {
        final Pattern[] patterns = new Pattern[numItems];

        if (patternStrings == null)
        {
            for (int i = 0; i < numItems; ++i)
            {
                patterns[i] = null;
            }

            return (patterns);
        }


        for (int i = 0; i < numItems; ++i)
        {
            // consider null to match anything

            if (patternStrings[i] == null)
            {
                patterns[i] = null;
            }
            else
            {
                patterns[i] = Pattern.compile(patternStrings[i]);
            }
        }

        return (patterns);
    }

    private interface Matcher
    {
        boolean match(ObjectName name, Pattern[] names, Pattern[] values);

    }

    private class MatchAnyMatcher implements Matcher
    {
        public MatchAnyMatcher()
        {
        }

        public boolean match(ObjectName name, Pattern[] names, Pattern[] values)
        {
            return (matchAny(name, names, values));
        }

    }

    private class MatchAllMatcher implements Matcher
    {
        public MatchAllMatcher()
        {
        }

        public boolean match(ObjectName name, Pattern[] names, Pattern[] values)
        {
            return (matchAll(name, names, values));
        }

    }

    Set<ObjectName> matchEither(
            Matcher matcher,
            Set<ObjectName> startingSet,
            String[] regexNames,
            String[] regexValues)
    {
        if (regexNames == null && regexValues == null)
        {
            // both null => matches entire original set
            return (startingSet);
        }

        final Set<ObjectName> results = new HashSet<ObjectName>();

        int numMatches = 0;
        if (regexNames != null)
        {
            numMatches = regexNames.length;
        }
        else
        {
            numMatches = regexValues.length;
        }

        final Pattern[] namePatterns = createPatterns(regexNames, numMatches);
        final Pattern[] valuePatterns = createPatterns(regexValues, numMatches);

        for (final ObjectName name : startingSet)
        {
            if (matcher.match(name, namePatterns, valuePatterns))
            {
                results.add(name);
            }
        }

        return (results);
    }

    public Set<ObjectName> matchAll(Set<ObjectName> startingSet, String[] regexNames, String[] regexValues)
    {
        return (matchEither(new MatchAllMatcher(), startingSet, regexNames, regexValues));
    }

    public Set<ObjectName> matchAny(Set<ObjectName> startingSet, String[] regexNames, String[] regexValues)
    {
        return (matchEither(new MatchAnyMatcher(), startingSet, regexNames, regexValues));
    }

}






