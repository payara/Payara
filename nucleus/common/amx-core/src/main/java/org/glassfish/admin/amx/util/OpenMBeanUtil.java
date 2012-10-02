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

package org.glassfish.admin.amx.util.jmx;

import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.InvalidOpenTypeException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.glassfish.admin.amx.util.TypeCast;
import org.glassfish.admin.amx.util.ArrayConversion;
import org.glassfish.admin.amx.util.IteratorUtil;

/**
Utilities dealing with OpenMBeans
 */
public final class OpenMBeanUtil
{
    private OpenMBeanUtil()
    {
    }

    private static volatile Map<Class<?>, SimpleType> SIMPLETYPES_MAP = null;

    private static Map<Class<?>, SimpleType> getSimpleTypesMap()
    {
        if (SIMPLETYPES_MAP == null)
        {
            final Map<Class<?>, SimpleType> m = new HashMap<Class<?>, SimpleType>();

            m.put(Byte.class, SimpleType.BYTE);
            m.put(Short.class, SimpleType.SHORT);
            m.put(Integer.class, SimpleType.INTEGER);
            m.put(Long.class, SimpleType.LONG);
            m.put(BigInteger.class, SimpleType.BIGINTEGER);
            m.put(BigDecimal.class, SimpleType.BIGDECIMAL);
            m.put(Float.class, SimpleType.FLOAT);
            m.put(Double.class, SimpleType.DOUBLE);

            m.put(Character.class, SimpleType.CHARACTER);
            m.put(Boolean.class, SimpleType.BOOLEAN);
            m.put(String.class, SimpleType.STRING);
            m.put(Date.class, SimpleType.DATE);
            m.put(Void.class, SimpleType.VOID);

            m.put(ObjectName.class, SimpleType.OBJECTNAME);

            SIMPLETYPES_MAP = m;
        }

        return (SIMPLETYPES_MAP);
    }

    /**
    Get the SimpleType for a class which can be so-represented.
     */
    static public SimpleType getSimpleType(final Class<?> c)
    {
        final SimpleType type = getSimpleTypesMap().get(c);

        return (type);

    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final OpenType[] EMPTY_OPENTYPES = new OpenType[0];

    /**
    Get any non-null array element from the array.
     */
    private static Object getAnyArrayElement(Object o)
    {
        Object result = null;

        final int length = Array.getLength(o);
        if (length != 0)
        {
            for (int i = 0; i < length; ++i)
            {
                final Object element = Array.get(o, 0);

                if (element != null)
                {
                    if (element.getClass().isArray())
                    {
                        result = getAnyArrayElement(element);
                        if (result != null)
                        {
                            break;
                        }
                    }
                    else
                    {
                        result = element;
                        break;
                    }
                }
            }
        }

        return (result);
    }

    private static int getArrayDimensions(final Class theClass)
    {
        final String classname = theClass.getName();

        int dim = 0;
        while (classname.charAt(dim) == '[')
        {
            ++dim;
        }

        return (dim);
    }

    /**
    Exists to avoid warnings from multiple call sites.

    JMX in JDK 1.5 does not have generic types for JMX, but does in JDK 1.6.
    This makes it impossible to use generics so long as we're compiling with 1.5.
    It also means we'll get warnings in JDK 1.6!
     */
    @SuppressWarnings("unchecked")  // no way to deal with them
    public static ArrayType newArrayType(final int numItems, final OpenType type)
            throws OpenDataException
    {
        return new ArrayType(numItems, type);
    }

    /**
    Get the OpenType of an Object, which must conform to OpenType requirements.
     */
    static public OpenType getOpenType(final Object o)
            throws InvalidOpenTypeException, OpenDataException
    {
        if (o == null)
        {
            // no OpenType for a null
            throw new IllegalArgumentException();
        }

        OpenType type = getSimpleType(o.getClass());

        if (type == null)
        {
            final Class<?> theClass = o.getClass();

            if (theClass.isArray())
            {
                final int dimensions = getArrayDimensions(theClass);
                final Class<?> elementClass = theClass.getComponentType();

                final SimpleType simpleType = getSimpleType(elementClass);
                if (simpleType != null)
                {
                    type = newArrayType(dimensions, simpleType);
                }
                else
                {
                    final Object element = getAnyArrayElement(o);

                    if (CompositeData.class.isAssignableFrom(elementClass))
                    {
                        if (element == null)
                        {
                            type = SimpleType.VOID;
                        }
                        else
                        {
                            type = newArrayType(dimensions, ((CompositeData) element).getCompositeType());
                        }
                    }
                    else if (TabularData.class.isAssignableFrom(elementClass))
                    {
                        if (element == null)
                        {
                            type = SimpleType.VOID;
                        }
                        else
                        {
                            type = newArrayType(dimensions, TabularData.class.cast(element).getTabularType());
                        }
                    }
                }

            }
            else if (o instanceof CompositeData)
            {
                type = ((CompositeData) o).getCompositeType();
            }
            else if (o instanceof TabularData)
            {
                type = ((TabularData) o).getTabularType();
            }
        }

        if (type == null)
        {
            throw new IllegalArgumentException(o.getClass().getName());
        }

        return (type);
    }

    /**
    Convert certain types and return a new Map:
    <ul>
    <li>Collection converts to an array</li>
    </ul>
    Nulls are not eliminated.
     */
    public static Map<String, Serializable> convertTypes(final Map<String, Serializable> orig)
    {
        final Map<String, Serializable> result = new HashMap<String, Serializable>();

        for (final Map.Entry<String, Serializable> me : orig.entrySet())
        {
            final String key = me.getKey();
            final Serializable value = me.getValue();

            if (value instanceof Collection)
            {
                final Collection<Serializable> c = TypeCast.asCollection(value);
                Object[] newValue = IteratorUtil.toArray(c.iterator());
                newValue = ArrayConversion.specializeArray(newValue);

                result.put(key, newValue);
            }
            else
            {
                result.put(key, value);
            }
        }


        return (result);
    }

    /**
    Create a CompositeType from a Map.  Each key in the map must be a String,
    and each value must be a type consistent with OpenTypes.

    @param typeName	the arbitrary name of the OpenType to be used
    @param description	the arbitrary description of the OpenType to be used
    @param map	a Map keyed by String, whose values may not be null
     */
    public static CompositeType mapToCompositeType(
            final String typeName,
            final String description,
            final Map<String, ?> map,
            CompositeTypeFromNameCallback callback)
            throws OpenDataException
    {
        final String[] itemNames = new String[map.keySet().size()];
        map.keySet().toArray(itemNames);

        final String[] itemDescriptions = new String[itemNames.length];
        final OpenType[] itemTypes = new OpenType[itemNames.length];

        for (int i = 0; i < itemNames.length; ++i)
        {
            final String name = itemNames[i];
            final Object value = map.get(name);

            itemDescriptions[i] = "value " + name;
            if (value == null)
            {
                // force nulls to type String
                itemTypes[i] = callback.getOpenTypeFromName(name);
            }
            else
            {
                itemTypes[i] = getOpenType(value);
            }
        }

        final CompositeType type = new CompositeType(
                typeName,
                description,
                itemNames,
                itemDescriptions,
                itemTypes);

        return (type);
    }

    /**
    Create a CompositeData from a Map.  Each key in the map must be a String,
    and each value must be a type consistent with OpenTypes.

    @param typeName	the arbitrary name of the OpenType to be used
    @param description	the arbitrary description of the OpenType to be used
    @param map	a Map keyed by String, whose values may not be null
     */
    public static CompositeData mapToCompositeData(
            final String typeName,
            final String description,
            final Map<String, Object> map)
            throws OpenDataException
    {
        final CompositeType type = mapToCompositeType(typeName, description, map, null);

        return (new CompositeDataSupport(type, map));
    }

    /**
    Convert a CompositeData to a Map.
     */
    public static Map<String, Serializable> compositeDataToMap(final CompositeData data)
    {
        final Map<String, Serializable> map = new HashMap<String, Serializable>();
        final CompositeType type = data.getCompositeType();
        final Set<String> keySet = TypeCast.asSet(type.keySet());

        for (String name : keySet)
        {
            map.put(name, (Serializable) data.get(name));
        }

        return (map);
    }

    /**
    Get a CompositeType describing a CompositeData which has no elements.
     */
    public static OpenType getStackTraceElementOpenType()
            throws OpenDataException
    {
        final String[] itemNames = new String[]
        {
            "ClassName",
            "FileName",
            "LineNumber",
            "IsNativeMethod",
        };

        final String[] descriptions = new String[]
        {
            "ClassName",
            "FileName",
            "LineNumber",
            "IsNativeMethod",
        };

        final OpenType[] openTypes = new OpenType[itemNames.length];
        openTypes[ 0] = SimpleType.STRING;
        openTypes[ 1] = SimpleType.STRING;
        openTypes[ 2] = SimpleType.INTEGER;
        openTypes[ 3] = SimpleType.BOOLEAN;

        return (new CompositeType(
                StackTraceElement.class.getName(),
                "StackTraceElement composite type",
                itemNames,
                descriptions,
                openTypes));
    }

    /**
    Get a CompositeType describing a CompositeData which has no elements.
     */
    public static OpenType getThrowableOpenType(final Throwable t)
            throws OpenDataException
    {
        final String[] itemNames = new String[]
        {
            "Message",
            "Cause",
            "StackTrace",
        };

        final String[] descriptions = new String[]
        {
            "The message from the Throwable",
            "The cause (if any) from the Throwable",
            "The stack trace from the Throwable",
        };

        final OpenType[] openTypes = new OpenType[itemNames.length];

        openTypes[ 0] = SimpleType.STRING;
        openTypes[ 1] = t.getCause() == null ? SimpleType.VOID : getThrowableOpenType(t.getCause());
        openTypes[ 2] = newArrayType(t.getStackTrace().length,
                getStackTraceElementOpenType());


        return (new CompositeType(
                t.getClass().getName(),
                "Throwable composite type",
                itemNames,
                descriptions,
                openTypes));
    }

}






