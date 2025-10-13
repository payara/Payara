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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
//Portions Copyright [2018-2019] [Payara Foundation and/or affiliates]

package org.glassfish.admin.amx.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class MapUtil
{
    private MapUtil()
    {
        // disallow instantiation
    }

    public static <K, V> V getWithDefault(final Map<K, V> m, final K key, V defaultValue)
    {
        return m.getOrDefault(key, defaultValue);
    }

    public static Object[] getKeyObjects(final Map<?, ?> m)
    {
        final Object[] keys = new Object[m.keySet().size()];
        m.keySet().toArray(keys);
        return keys;
    }

    public static String[] getKeyStrings(final Map<?, ?> m)
    {
        return (SetUtil.toSortedStringArray(m.keySet()));
    }

    /** assignable to any map type, keeps things short on the rhs */
    public static <K, V> Map<K, V> newMap()
    {
        return new HashMap<K, V>();
    }

    /**
    Create a new Map consisting of a single key/value pair.
     */
    public static <V> Map<String, V> newMap(final String key, final V value) {
        final Map<String, V> map = new HashMap<>();

        map.put(key, value);

        return (map);
    }

    /**
     * Create a new Map combined all entries of two other maps.
     * If a key is is both maps then the value in map2 will be used.
     */
    public static <K, V> Map<K, V> newMap(final Map<K, V> m1, final Map<K, V> m2)
    {
        final Map<K, V> newMap = new HashMap<>();

        if (m1 != null)
        {
            newMap.putAll(m1);
        }
        if (m2 != null)
        {
            newMap.putAll(m2);
        }

        return (newMap);
    }

    public static <K, V> Map<K, V> toMap(final Object[] params, final Class<K> keyClass, final Class<V> valueClass)
    {
        final Map<K, V> map = new HashMap<>();

        for (int i = 0; i < params.length; i += 2)
        {
            final Object key = params[i];
            if (key == null)
            {
                throw new IllegalArgumentException();
            }

            if (!keyClass.isAssignableFrom(key.getClass()))
            {
                throw new IllegalArgumentException("Key of class " + key.getClass().getName() + " not assignable to " + keyClass.getName());
            }

            final Object value = params[i + 1];
            if (value != null && !valueClass.isAssignableFrom(value.getClass()))
            {
                throw new IllegalArgumentException("Value of class " + value.getClass().getName() + " not assignable to " + valueClass.getName());
            }

            map.put(keyClass.cast(key), valueClass.cast(value));
        }
        return map;
    }

    /**
    Create a new Map and insert the specified mappings as found in 'mappings'.
    The even-numbered entries are the keys, and the odd-numbered entries are
    the values.
     */
    public static <T> Map<T, T> newMap(final T... mappings)
    {
        if ((mappings.length % 2) != 0)
        {
            throw new IllegalArgumentException("mappings must have even length");
        }

        final Map<T, T> m = new HashMap<>();

        for (int i = 0; i < mappings.length; i += 2)
        {
            m.put(mappings[i], mappings[i + 1]);
        }

        return (m);
    }

    /**
    Create a new Map and insert the specified mappings as found in 'mappings'.
    The even-numbered entries are the keys, and the odd-numbered entries are
    the values.
     */
    public static Map<String, String> newMap(final String... mappings)
    {
        if ((mappings.length % 2) != 0)
        {
            throw new IllegalArgumentException("mappings must have even length");
        }

        final Map<String, String> map = new HashMap<>();

        for (int i = 0; i < mappings.length; i += 2)
        {
            map.put(mappings[i], mappings[i + 1]);
        }

        return (map);
    }

    /**
    Remove all entries keyed by 'keys'
     */
    public static <T> void removeAll(final Map<T, ?> map, final T[] keys) {
        for (T key : keys) {
            map.remove(key);
        }
    }

    public static boolean mapsEqual(final Map<?, ?> map1, final Map<?, ?> map2) {
        if (map1 == map2)
        {
            return (true);
        }

        boolean equal = false;

        if (map1.size() == map2.size() &&
            map1.keySet().equals(map2.keySet()))
        {
            equal = true;

            for (final Map.Entry<?,?> me : map1.entrySet())
            {
                final Object key = me.getKey();
                final Object value1 = me.getValue();
                final Object value2 = map2.get(key);

                if (!CompareUtil.objectsEqual(value1, value2))
                {
                    equal = false;
                    break;
                }
            }
        }

        return (equal);
    }

    /**
     * Creates a new map with all the old key/value pairs unless the value
     * was null
     * @param map the old map to convert
     * @return a new map with no null values
     */
    public static <K, V> Map<K, V> newMapNoNullValues(final Map<K, V> map) {
        final Map<K, V> result = new HashMap<>();

        for (final Map.Entry<K, V> me : map.entrySet())
        {
            final V value = me.getValue();

            if (value != null)
            {
                result.put(me.getKey(), value);
            }
        }

        return (result);
    }

    public static String toString(final Map<?, ?> m)
    {
        return (toString(m, ","));
    }

    public static String toString(final Map<?, ?> map, final String separator)
    {
        if (map == null)
        {
            return ("null");
        }

        final StringBuilder buf = new StringBuilder();

        final String[] keyStrings = getKeyStrings(map);
        for (final String key : keyStrings)
        {
            final Object value = map.get(key);

            buf.append(key);
            buf.append("=");
            buf.append(StringUtil.toString(value));
            buf.append(separator);
        }
        if (buf.length() != 0)
        {
            // strip trailing separator
            buf.setLength(buf.length() - separator.length());
        }

        return (buf.toString());
    }

    /**
     * Gets all keys which map to a null value
     */
    public static <K> Set<K> getNullValueKeys(final Map<K, ?> map) {
        final Set<K> s = new HashSet<>();

        for (final Map.Entry<K, ?> me : map.entrySet())
        {
            if (me.getValue() == null)
            {
                s.add(me.getKey());
            }
        }
        return (s);
    }

    public static <K, V> Map<K, V> toMap(final Properties props, final Class<K> kClass, final Class<V> vClass)
    {
        return TypeCast.checkMap(props, kClass, vClass);
    }

    /**
     * Removes all keys in a set from a map
     * @param map the map to remove keys from
     * @param keySet a set of keys to remove
     */
    public static <K> void removeAll(final Map<K, ?> map, final Set<K> keySet) {
        for (final K key : keySet)
        {
            map.remove(key);
        }
    }

    /**
    @return true if non-null Map and all keys and values are of type java.lang.String
     */
    public static boolean isAllStrings(final Map<?, ?> map) {
        return map != null && CollectionUtil.isAllStrings(map.keySet()) &&
               CollectionUtil.isAllStrings(map.values());
    }

    /**
    Convert an arbitrary Map to one whose keys and values
    are both of type String.
     */
    public static Map<String, String> toStringStringMap(final Map<?, ?> map)
    {
        if (map == null)
        {
            return null;
        }

        Map<String, String> result;

        if (isAllStrings(map))
        {
            result = TypeCast.asMap(map);
        }
        else
        {
            result = new HashMap<>();

            for (final Map.Entry<?, ?> me : map.entrySet())
            {
                final Object key = me.getKey();
                final Object value = me.getValue();

                if ((key instanceof String) && (value instanceof String))
                {
                    result.put((String) key, (String) value);
                }
                else
                {
                    result.put("" + key, "" + value);
                }
            }
        }

        return result;
    }

}
