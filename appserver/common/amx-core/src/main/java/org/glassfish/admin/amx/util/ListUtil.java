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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Enumeration;
import java.util.Collections;

public final class ListUtil
{
    private ListUtil()
    {
        // disallow instantiation
    }

    /**
    Add all items in an array to a list.
     */
    public static <T> void addArray(
            final List<T> list,
            final T[] array)
    {
        for (int i = 0; i < array.length; ++i)
        {
            list.add(array[i]);
        }
    }

    public static <T> List<T> newList()
    {
        return new ArrayList<T>();
    }
    
    public static List<String> asStringList(final Object value)
    {
        List<String> values = null;

        if (value instanceof String)
        {
            values = Collections.singletonList((String) value);
        }
        else if (value instanceof String[])
        {
            values = ListUtil.newListFromArray((String[]) value);
        }
        else if (value instanceof List)
        {
            final List<String> checkedList = TypeCast.checkList(TypeCast.asList(value), String.class);
            values = new ArrayList<String>(checkedList);
        }
        else
        {
            throw new IllegalArgumentException("" + value);
        }

        return values;
    }

    /**
    Convert a List to a String[]
     */
    public static String[] toStringArray(final List<?> list)
    {
        final String[] names = new String[list.size()];

        int i = 0;
        for (final Object o : list)
        {
            names[i] = "" + o;
            ++i;
        }

        return (names);
    }

    /**
    Create a new List from a Collection
     */
    public static <T> List<T> newListFromCollection(final Collection<T> c)
    {
        final List<T> list = new ArrayList<T>();

        list.addAll(c);

        return (list);
    }

    public static <T> List<T> newList(final Enumeration<T> e)
    {
        final List<T> items = new ArrayList<T>();
        while (e.hasMoreElements())
        {
            items.add(e.nextElement());
        }
        return items;
    }

    /**
    Create a new List from a Collection
     */
    public static <T> List<? extends T> newListFromIterator(final Iterator<? extends T> iter)
    {
        final List<T> list = new ArrayList<T>();

        while (iter.hasNext())
        {
            list.add(iter.next());
        }

        return (list);
    }

    /**
    Create a new List with one member.
     */
    public static <T> List<T> newList(T m1)
    {
        final List<T> list = new ArrayList<T>();

        list.add(m1);

        return (list);
    }

    /**
    Create a new List with two members.
     */
    public static <T> List<T> newList(
            final T m1,
            final T m2)
    {
        final List<T> list = new ArrayList<T>();

        list.add(m1);
        list.add(m2);

        return (list);
    }

    /**
    Create a new List with three members.
     */
    public static <T> List<T> newList(
            final T m1,
            final T m2,
            final T m3)
    {
        final List<T> list = new ArrayList<T>();

        list.add(m1);
        list.add(m2);
        list.add(m3);

        return (list);
    }

    /**
    Create a new List with four members.
     */
    public static <T> List<T> newList(
            final T m1,
            final T m2,
            final T m3,
            final T m4)
    {
        final List<T> list = new ArrayList<T>();

        list.add(m1);
        list.add(m2);
        list.add(m3);
        list.add(m4);

        return (list);
    }

    /**
    Create a new List with four members.
     */
    public static <T> List<T> newList(
            final T m1,
            final T m2,
            final T m3,
            final T m4,
            final T m5)
    {
        final List<T> list = new ArrayList<T>();

        list.add(m1);
        list.add(m2);
        list.add(m3);
        list.add(m4);
        list.add(m5);

        return (list);
    }

    public static <T> List<T> newListFromArray(final T[] items)
    {
        final List<T> list = new ArrayList<T>();

        for (int i = 0; i < items.length; ++i)
        {
            list.add(items[i]);
        }

        return (list);
    }
    
    
    /**
    Return a new List in reverse order. Because the List is new,
    it works on any list, modifiable or not.
     */
    public static <T> List<T> reverse(final List<T> list)
    {
        final int numItems = list.size();
        final List<T> result = new ArrayList<T>(numItems);

        for (int i = 0; i < numItems; ++i)
        {
            result.add(list.get(numItems - i - 1));
        }

        return (result);
    }

}

