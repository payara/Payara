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

import java.lang.reflect.Array;

/**
Provides:
- utility to check for equality
 */
public final class ArrayUtil
{
    private ArrayUtil()
    {
        // disallow instantiation
    }

    public static boolean arraysEqual(final Object array1, final Object array2)
    {
        boolean equal = array1 == array2;

        if (equal)
        {
            // same object or both null
            return (true);
        }
        else if (array1 == null || array2 == null)
        {
            return (false);
        }


        if (array1.getClass() == array2.getClass() &&
            ClassUtil.objectIsArray(array1) &&
            Array.getLength(array1) == Array.getLength(array2))
        {
            equal = true;
            final int length = Array.getLength(array1);

            for (int i = 0; i < length; ++i)
            {
                final Object a1 = Array.get(array1, i);
                final Object a2 = Array.get(array2, i);

                if (a1 != a2)
                {
                    if (a1 == null || a2 == null)
                    {
                        equal = false;
                    }
                    else if (ClassUtil.objectIsArray(a1))
                    {
                        if (!arraysEqual(a1, a2))
                        {
                            equal = false;
                        }
                    }
                }

                if (!equal)
                {
                    break;
                }
            }
        }

        return (equal);
    }

    public static boolean arrayContainsNulls(final Object[] array)
    {
        boolean containsNulls = false;

        for (int i = 0; i < array.length; ++i)
        {
            if (array[i] == null)
            {
                containsNulls = true;
                break;
            }
        }

        return (containsNulls);
    }

    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> T[] newArray(final Class<T> theClass, int numItems)
    {
        return (T[]) (Array.newInstance(theClass, numItems));
    }

    /**
    Create a new array from the original.

    @param items		the original array
    @param startIndex	index of the first item
    @param numItems
    @return an array of the same type, containing numItems items
     */
    public static <T> T[] newArray(
            final T[] items,
            final int startIndex,
            final int numItems)
    {
        final Class<T> theClass = TypeCast.asClass(items.getClass().getComponentType());

        final T[] result = newArray(theClass, numItems);
        System.arraycopy(items, startIndex, result, 0, numItems);

        return (result);
    }

    /**
    Create a new array consisting of originals and new.

    @param items1		1st array
    @param items2		2nd array
    @return an array of the same type as items1, its elements first
     */
    public static <T> T[] newArray(
            final T[] items1,
            final T[] items2)
    {
        final Class<T> class1 = TypeCast.asClass(items1.getClass().getComponentType());
        final Class<T> class2 = TypeCast.asClass(items2.getClass().getComponentType());

        if (!class1.isAssignableFrom(class2))
        {
            throw new IllegalArgumentException();
        }

        final int length1 = Array.getLength(items1);
        final int length2 = Array.getLength(items2);
        final T[] result = newArray(class1, length1 + length2);
        System.arraycopy(items1, 0, result, 0, length1);
        System.arraycopy(items2, 0, result, length1, length2);

        return (result);
    }

    /**
    Create a new array consisting of an original array, and a single new item.

    @param items		an array
    @param item		an item to append
    @return an array of the same type as items1, its elements first
     */
    public static <T> T[] newArray(
            final T[] items,
            final T item)
    {
        final Class<T> theClass = TypeCast.asClass(items.getClass().getComponentType());
        final T[] result = newArray(theClass, items.length + 1);
        System.arraycopy(items, 0, result, 0, items.length);

        result[result.length - 1] = item;
        return (result);
    }

}

























