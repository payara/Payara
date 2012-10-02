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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
This utility class contains two types of methods:
<ul>
<li>Methods to cast Collection/List/Set/Map/etc to a more strongly-typed
generic type;</li>
<li>Methods to verify the types of elements within the above.</li>
</ul>
<p>
Due to the way in which generic types are implemented in JDK 1.5, coupled
with the fact that both generic and non-generic code need to coexist,
there exist a variety of cases in which casts cannot be avoided.  However,
performing such cast generates compiler warnings which cannot be eliminated,
and which thus produce clutter which makes it hard to recognize
other warnings during compilation.
<p>
The casting methods here localize the aforementioned compiler warnings to this
file thus allowing code elsewhere to compile "cleanly" (eg without warnings).
<p>
Clients should use the casting routines <b>only when there is
no other appropriate solution</b>.
For example, consider a caller of non-generic code method getStuff():
<pre>Map getStuff()</pre>
The javadoc for getStuff() specifies that the keys and
values of the Map are java.lang.String.
The caller would like to declare:
<pre>
final Map&lt;String,String> m = getStuff();
</pre>
But this will generate a compiler warning.  To avoid this compiler warning,
the code should be written as follows:
<pre>
final Map&lt;String,String> m = TypeCast.asMap( getStuff() );
</pre>
If there is any doubt as to the correct contents of a
Collection/List/Set/Map, use the appropriate {@link #checkCollection},
{@link #checkMap}, {@link #checkList} method.
<p>
Due to the way generics are implemented, an explicit call is needed with
a specific class in order to do so; this is why the as() methods do
not already perform that check. Following the above example, we would write:
<code>TypeCast.checkCompatible(m, String.class, String.class)</code>
<p>
Naturally checking the keys and values of the Map is far more expensive
than a simple cast, but if the contents are unclear, {@link #checkMap}
is strongly advised over {@link #asMap}.  The same holds true for the
Collection, Set, and List variants of these methods.
Most casts can be handled appropriately through the appropriate use
of generic types.
 */
public final class TypeCast
{
    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkCollection} instead
    if there is any doubt.
    @param o the Object, which must be a {@link Collection}
    @return Collection<T>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> Collection<T> asCollection(final Object o)
    {
        return (Collection<T>) Collection.class.cast(o);
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkCollection} instead if there is
    any doubt.
    @return Collection<T extends Serializable>
     */
    public static <T extends Serializable> Collection<T> asSerializableCollection(final Object c)
    {
        final Collection<T> result = asCollection(c);
        checkSerializable(result);
        return result;
    }

    /**
    The caller should take appropriate care that the type of keys/values
    is correct, and may want to call {@link #checkMap} instead if there is
    any doubt.
    @return Map<K,V>
    @return Map<K extends Serializable,V extends Serializable>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <K, V> Map<K, V> asMap(final Object m)
    {
        return (Map<K, V>) Map.class.cast(m);
    }

    /**
    The caller should take appropriate care that the type of keys/values
    is correct, and may want to call {@link #checkSerializable} instead if there is
    any doubt.
    @return Map<K extends Serializable,V extends Serializable>
     */
    public static <K extends Serializable, V extends Serializable> Map<K, V> asSerializableMap(final Object m)
    {
        final Map<K, V> result = asMap(m);
        checkSerializable(result);

        return result;
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkMap} instead if there is
    any doubt.
    @return Hashtable<K,V>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <K, V> Hashtable<K, V> asHashtable(final Object o)
    {
        return (Hashtable<K, V>) Hashtable.class.cast(o);
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkList} instead if there is
    any doubt.
    @return List<T>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> List<T> asList(final Object list)
    {
        return (List<T>) List.class.cast(list);
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call{@link #checkList} instead if there is
    any doubt.
    @return List<T extends Serializable>
     */
    public static <T extends Serializable> List<T> asSerializableList(final Object list)
    {
        final List<T> result = asList(list);
        checkSerializable(result);
        return result;
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkSet} instead if there is
    any doubt.
    @return Set<T>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> Set<T> asSet(final Object s)
    {
        return (Set<T>) Set.class.cast(s);
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkSet} instead if there is
    any doubt.
    @return Set<T>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> SortedSet<T> asSortedSet(final Object s)
    {
        return (SortedSet<T>) Set.class.cast(s);
    }

    /**
    The caller should take appropriate care that the type of element
    is correct, and may want to call {@link #checkSet} instead if there is
    any doubt.
    @return Set<T extends Serializable>
     */
    public static <T extends Serializable> Set<T> asSerializableSet(final Object s)
    {
        final Set<T> result = asSet(s);
        checkSerializable(result);
        return result;
    }

    /**
    The caller should take appropriate care that the type is correct.
    @return Class<T>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> Class<T> asClass(final Class<?> c)
    {
        return (Class<T>) Class.class.cast(c);
    }

    /**
    The caller should take appropriate care that the type is correct.
    @return Class<T>
     */
    @SuppressWarnings("unchecked")  // inherent/unavoidable for this method
    public static <T> T[] asArray(final Object o)
    {
        return (T[]) Object[].class.cast(o);
    }

    /**
    Verify that all elements implement java.io.Serializable
    @throws ClassCastException
     */
    public static void checkSerializable(final Object[] a)
    {
        for (final Object o : a)
        {
            checkSerializable(o);
        }
    }

    /**
    Verify that all elements implement java.io.Serializable
    @throws ClassCastException
     */
    public static void checkSerializableElements(final Collection<?> l)
    {
        for (final Object o : l)
        {
            checkSerializable(o);
        }
    }

    /**
    Verify that all elements implement java.io.Serializable
    @throws ClassCastException
     */
    public static Collection<Serializable> checkSerializable(final Collection<?> l)
    {
        checkSerializable(l, true);
        return asCollection(l);
    }

    /**
    Verify that all elements implement java.io.Serializable
    @param l the Collection
    @param collectionItself if true, the Collection itself is additionally checked,
    if false only the elements are checked.
    @throws ClassCastException
     */
    public static Collection<Serializable> checkSerializable(final Collection<?> l, boolean collectionItself)
    {
        if (collectionItself)
        {
            checkSerializable((Object) l);
        }

        checkSerializableElements(l);

        return asCollection(l);
    }

    /**
    Verify that the Map itself, and all keys and values
    implement java.io.Serializable
    @throws ClassCastException
     */
    public static Map<Serializable, Serializable> checkSerializable(final Map<?, ?> m)
    {
        checkSerializable((Object) m);

        // the key set used by HashMap isn't Serializable; apparently
        // HashMap serializes itself properly, but not by serializing
        // the key set directly. So if the Map is Serializable, we
        // can't necessarily constrain the key set and value set to be so,
        // since it's up to the Map to serialize properly.
        checkSerializable(m.keySet(), false);
        checkSerializable(m.values(), false);

        return asMap(m);
    }

    /**
    Verify that the Object implements java.io.Serializable.
    @throws ClassCastException
     */
    public static Serializable checkSerializable(final Object o)
    {
        if ((o != null) && !(o instanceof Serializable))
        {
            throw new ClassCastException("Object not Serializable, class = " + o.getClass().getName());
        }

        return Serializable.class.cast(o);
    }

    //-------------
    /**
    Verify that the elements are all assignable to an object of the
    specified class.
    @param theClass the Class which the element must extend
    @param c
    @throws ClassCastException
     */
    public static <T> Collection<T> checkCollection(final Collection<?> c, final Class<T> theClass)
    {
        if (c != null)
        {
            for (final Object o : c)
            {
                checkObject(o, theClass);
            }
        }

        return asCollection(c);
    }

    /**
    Verify that the elements are all assignable to an object of the
    specified class.
    @param l the list
    @param theClass the Class which the element must extend
    @throws ClassCastException
     */
    public static <T> List<T> checkList(final List<?> l, final Class<T> theClass)
    {
        if (l != null)
        {
            for (final Object o : l)
            {
                checkObject(o, theClass);
            }
        }

        return asList(l);
    }

    /**
    Verify that the elements are all assignable to an object of the
    specified class.
    @param s
    @param theClass the Class which the element must extend
    @throws ClassCastException
     */
    public static <T> Set<T> checkSet(final Set<?> s, final Class<T> theClass)
    {
        if (s != null)
        {
            for (final Object o : s)
            {
                checkObject(o, theClass);
            }
        }

        return asSet(s);
    }

    /**
    Verify that the elements are all assignable to an object of the
    specified class.
    @param m
    @param keyClass the Class which keys must extend
    @param valueClass the Class which values must extend
    @throws ClassCastException
     */
    public static <K, V> Map<K, V> checkMap(
            final Map<?, ?> m,
            final Class<K> keyClass,
            final Class<V> valueClass)
    {
        if (m != null)
        {
            checkSet(m.keySet(), keyClass);
            checkCollection(m.values(), valueClass);
        }

        return asMap(m);
    }

    /**
    Verify that the Object is assignable to an object of the
    specified class.
    @param theClass the Class
    @param o the Object
    @throws ClassCastException
     */
    @SuppressWarnings("unchecked")
    public static <T> T checkObject(final Object o, final Class<T> theClass)
    {
        if (o != null && !theClass.isAssignableFrom(o.getClass()))
        {
            throw new ClassCastException("Object of class " + o.getClass().getName() +
                                         " not assignment compatible with: " + theClass.getName());
        }
        return (T) o;
    }

    /**
    Verify that the elements are all assignable to an object of the
    specified class.
    @param theClass the Class which the element must extend
    @param a the Array of elements
    @throws ClassCastException
     */
    public static <T> void checkArray(final Object[] a, final Class<T> theClass)
    {
        for (final Object o : a)
        {
            checkObject(o, theClass);
        }
    }

    /**
    Create a checked Collection<String>, first verifying that all elements
    are in fact String.
    @param c the Collection
    @throws ClassCastException
     */
    public static Collection<String> checkedStringCollection(final Collection<?> c)
    {
        return checkedCollection(c, String.class);
    }

    /**
    Create a checked Set<String>, first verifying that all elements
    are in fact String.
    @param s the Set
    @throws ClassCastException
     */
    public static Set<String> checkedStringSet(final Set<?> s)
    {
        return checkedSet(s, String.class);
    }

    /**
    Create a checked List<String>, first verifying that all elements
    are in fact String.
    @param l the List
    @throws ClassCastException
     */
    public static List<String> checkedStringList(final List<?> l)
    {
        return checkedList(l, String.class);
    }

    /**
    Create a checked Map<String,String>, first verifying that all keys
    and values are in fact String.
    @param m the Map
    @throws ClassCastException
     */
    public static Map<String, String> checkedStringMap(final Map<?, ?> m)
    {
        return checkedMap(m, String.class, String.class);
    }

    /**
    Create a checked Collection<String>, first verifying that all elements
    are in fact String.
    @param c the Collection
    @throws ClassCastException
     */
    public static <T> Collection<T> checkedCollection(final Collection<?> c, final Class<T> theClass)
    {
        final Collection<T> cc = checkCollection(c, theClass);
        return Collections.checkedCollection(cc, theClass);
    }

    /**
    Create a checked Set<String>, first verifying that all elements
    are in fact String.
    @param s the Set
    @throws ClassCastException
     */
    public static <T> Set<T> checkedSet(final Set<?> s, final Class<T> theClass)
    {
        final Set<T> cs = checkSet(s, theClass);
        return Collections.checkedSet(cs, theClass);
    }

    /**
    Create a checked List<String>, first verifying that all elements
    are in fact String.
    @param l the List
    @throws ClassCastException
     */
    public static <T> List<T> checkedList(final List<?> l, final Class<T> theClass)
    {
        final List<T> cl = checkList(l, theClass);
        return Collections.checkedList(cl, theClass);
    }

    /**
    Create a checked Map<String,String>, first verifying that all keys
    and values are in fact String.
    @param m the Map
    @throws ClassCastException
     */
    public static <K, V> Map<K, V> checkedMap(final Map<?, ?> m, final Class<K> keyClass, final Class<V> valueClass)
    {
        final Map<K, V> cm = checkMap(m, keyClass, valueClass);
        return Collections.checkedMap(cm, keyClass, valueClass);
    }

}
















