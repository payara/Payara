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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
Utilities for working with sets using JDK 1.5 generics.
 */
public final class SetUtil
{
    private SetUtil()
    {
        // disallow instantiation
    }

    public static <T> T getSingleton(final Set<T> s)
    {
        if (s.size() != 1)
        {
            throw new IllegalArgumentException(s.toString());
        }
        return (s.iterator().next());
    }

    public static <T> void addArray(
            final Set<T> set,
            final T[] array)
    {
        for (final T item : array)
        {
            set.add(item);
        }
    }
    
    
    public static <T> Set<T> newSet()
    {
        return new HashSet<T>();
    }
    

    public static <T> Set<T> newSet(final Collection<T> c)
    {
        final HashSet<T> set = new HashSet<T>();

        set.addAll(c);

        return (set);
    }

    /**
    Create a new Set with one member.
     */
    public static <T> Set<T> newSet(final T item)
    {
        final Set<T> set = new HashSet<T>();
        set.add(item);

        return (set);
    }

    /**
    Create a new Set containing all members of another.
    The returned Set is always a HashSet.
     */
    public static <T> HashSet<T> copySet(final Set<? extends T> s1)
    {
        final HashSet<T> set = new HashSet<T>();

        set.addAll(s1);

        return (set);
    }

    public static <T> Set<? extends T> newSet(
            final T m1,
            final T m2)
    {
        final HashSet<T> set = new HashSet<T>();

        set.add(m1);
        set.add(m2);

        return (set);
    }

    /*
    public static <T> Set<T>
    newSet(
    final T m1,
    final T m2,
    final T m3 )
    {
    final HashSet<T>	set	= new HashSet<T>();

    set.add( m1 );
    set.add( m2 );
    set.add( m3 );

    return( set );
    }
     */
    public static <T> Set<T> newSet(
            final T m1,
            final T m2,
            final T m3,
            final T m4)
    {
        final HashSet<T> set = new HashSet<T>();

        set.add(m1);
        set.add(m2);
        set.add(m3);
        set.add(m4);

        return (set);
    }

    /**
    Create a new Set containing all array elements.
     */
    public static <T> Set<T> newSet(final T[] objects)
    {
        return (newSet(objects, 0, objects.length));
    }

    public static <T, TT extends T> Set<T> newSet(final Set<T> s1, final Set<TT> s2)
    {
        final Set<T> both = new HashSet<T>();
        both.addAll(s1);
        both.addAll(s2);

        return both;
    }

    /**
    Create a new Set containing all array elements.
     */
    public static <T> Set<T> newSet(
            final T[] objects,
            final int startIndex,
            final int numItems)
    {
        final Set<T> set = new HashSet<T>();

        for (int i = 0; i < numItems; ++i)
        {
            set.add(objects[startIndex + i]);
        }

        return (set);
    }

    /**
    Convert a Set to a String[]
     */
    public static String[] toStringArray(final Set<?> s)
    {
        final String[] strings = new String[s.size()];

        int i = 0;
        for (final Object o : s)
        {
            strings[i] = "" + o;
            ++i;
        }

        return (strings);
    }

    public static String[] toSortedStringArray(final Set<?> s)
    {
        final String[] strings = toStringArray(s);

        Arrays.sort(strings);

        return (strings);
    }

    public static Set<String> newStringSet(final String... args)
    {
        return newUnmodifiableSet(args);
    }

    public static <T> Set<T> newUnmodifiableSet(final T... args)
    {
        final Set<T> set = new HashSet<T>();

        for (final T s : args)
        {
            set.add(s);
        }
        return set;
    }

    public static Set<String> newUnmodifiableStringSet(final String... args)
    {
        return Collections.unmodifiableSet(newStringSet(args));
    }

    /*
    public static Set<String>
    newStringSet( final Object... args)
    {
    final Set<String>   set   = new HashSet<String>();

    for( final Object o : args )
    {
    set.add( o == null ? null : "" + o );
    }
    return set;
    }
     */
    public static <T> Set<T> newTypedSet(final T... args)
    {
        final Set<T> set = new HashSet<T>();

        for (final T o : args)
        {
            set.add(o);
        }
        return set;
    }

    /**
    Create a new Set with one member.  Additional items
    may be added.
     */
    public static <T> Set<T> newSingletonSet(final T m1)
    {
        final Set<T> set = new HashSet<T>();

        set.add(m1);

        return (set);
    }

    /**
    Return a new Set of all items in both set1 and set2.
     */
    public static <T> Set<T> intersectSets(
            final Set<T> set1,
            final Set<T> set2)
    {
        final Set<T> result = newSet(set1);
        result.retainAll(set2);

        return (result);
    }

    /**
    Return a new Set of all items in set1 not in set2.
     */
    public static <T> Set<T> removeSet(
            final Set<T> set1,
            final Set<T> set2)
    {
        final Set<T> result = new HashSet<T>();
        result.addAll(set1);
        result.removeAll(set2);

        return (result);
    }

    /**
    Return a new Set of all items not common to both sets.
     */
    public static <T> Set<T> newNotCommonSet(
            final Set<T> set1,
            final Set<T> set2)
    {
        final Set<T> result = newSet(set1, set2);
        final Set<T> common = intersectSets(set1, set2);

        result.removeAll(common);

        return (result);
    }

    public static String findIgnoreCase(final Set<String> candidates, final String target)
    {
        String match = null;
        // case-insensitive search
        for (final String candidate : candidates)
        {
            if (candidate.equalsIgnoreCase(target))
            {
                match = candidate;
                break;
            }
        }
        return match;
    }

}























