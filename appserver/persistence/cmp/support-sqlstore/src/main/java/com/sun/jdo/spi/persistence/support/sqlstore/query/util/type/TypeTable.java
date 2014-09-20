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

/*
 * TypeTable.java
 *
 * Created on March 8, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.util.type;

import java.util.*;
import java.math.*;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;

/**
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 * @version 0.1
 */
public class TypeTable
{
    /**
     * Represents the type of null
     */
    public static final NullType nullType = new NullType();

    /**
     * Represents the internal error type.
     */
    public static final ErrorType errorType = new ErrorType();

    /**
     * Represents the type boolean.
     */
    public BooleanType booleanType;

    /**
     * Represents the type char.
     */
    public IntegralType charType;

    /**
     * Represents the type byte.
     */
    public IntegralType byteType;

    /**
     * Represents the type short.
     */
    public IntegralType shortType;

    /**
     * Represents the type int.
     */
    public IntegralType intType;

    /**
     * Represents the type long.
     */
    public IntegralType longType;

    /**
     * Represents the type float.
     */
    public FloatingPointType floatType;

    /**
     * Represents the type double.
     */
    public FloatingPointType doubleType;

    /**
     * Represents the type java.lang.String.
     */
    public StringType stringType;

    /**
     * Represents the type java.math.BigDecimal
     */
    public MathType bigDecimalType;

    /**
     * Represents the type java.math.BigInteger
     */
    public MathType bigIntegerType;

    /**
     * The model used to access class meta info
     */
    protected Model model;

    /**
     * Store class loader for Class.forName lookup
     */
    protected ClassLoader classLoader;

    /**
     * The list of actual known types.
     */
    protected Map types = new HashMap();

    /** Map of TypeTable instances. Key is a classLoader. */
    private static Map typeTables = new HashMap();

    /** */
    public static TypeTable getInstance(ClassLoader classLoader)
    {
        synchronized (typeTables) {
            TypeTable typeTable = (TypeTable)typeTables.get(classLoader);
            if (typeTable == null) {
                typeTable = new TypeTable(classLoader);
                typeTables.put(classLoader, typeTable);
            }
            return typeTable;
        }
    }

    /** */
    public static void removeInstance(ClassLoader classLoader)
    {
        synchronized (typeTables) {
            typeTables.remove(classLoader);
        }
    }

    /**
     *
     */
    private TypeTable(ClassLoader classLoader)
    {
        // init model
        // JQLC is only used at runtime => use runtime model
        this.model = Model.RUNTIME;

        this.classLoader = classLoader;

        booleanType = new BooleanType();
        types.put(booleanType.getName(), booleanType);
        charType = new IntegralType("char", char.class, FieldTypeEnumeration.CHARACTER_PRIMITIVE); //NOI18N
        types.put(charType.getName(), charType);
        byteType = new IntegralType("byte", byte.class, FieldTypeEnumeration.BYTE_PRIMITIVE); //NOI18N
        types.put(byteType.getName(), byteType);
        shortType = new IntegralType("short", short.class, FieldTypeEnumeration.SHORT_PRIMITIVE); //NOI18N
        types.put(shortType.getName(), shortType);
        intType = new IntegralType("int", int.class, FieldTypeEnumeration.INTEGER_PRIMITIVE); //NOI18N
        types.put(intType.getName(), intType);
        longType = new IntegralType("long", long.class, FieldTypeEnumeration.LONG_PRIMITIVE); //NOI18N
        types.put(longType.getName(), longType);
        floatType = new FloatingPointType("float",  float.class, FieldTypeEnumeration.FLOAT_PRIMITIVE); //NOI18N
        types.put(floatType.getName(), floatType);
        doubleType = new FloatingPointType("double", double.class, FieldTypeEnumeration.DOUBLE_PRIMITIVE); //NOI18N
        types.put(doubleType.getName(), doubleType);

        stringType = new StringType(this);
        types.put(stringType.getName(), stringType);

        WrapperClassType booleanClassType =
            new WrapperClassType("java.lang.Boolean", Boolean.class, FieldTypeEnumeration.BOOLEAN, booleanType, this); //NOI18N
        types.put(booleanClassType.getName(), booleanClassType);
        NumericWrapperClassType byteClassType =
            new NumericWrapperClassType("java.lang.Byte", Byte.class, FieldTypeEnumeration.BYTE, byteType, this); //NOI18N
        types.put(byteClassType.getName(), byteClassType);
        NumericWrapperClassType shortClassType =
            new NumericWrapperClassType("java.lang.Short", Short.class, FieldTypeEnumeration.SHORT, shortType, this); //NOI18N
        types.put(shortClassType.getName(), shortClassType);
        NumericWrapperClassType intClassType =
            new NumericWrapperClassType("java.lang.Integer", Integer.class, FieldTypeEnumeration.INTEGER, intType, this); //NOI18N
        types.put(intClassType.getName(), intClassType);
        NumericWrapperClassType longClassType =
            new NumericWrapperClassType("java.lang.Long", Long.class, FieldTypeEnumeration.LONG, longType, this); //NOI18N
        types.put(longClassType.getName(), longClassType);
        NumericWrapperClassType charClassType =
            new NumericWrapperClassType("java.lang.Character", Character.class, FieldTypeEnumeration.CHARACTER, charType, this); //NOI18N
        types.put(charClassType.getName(), charClassType);
        NumericWrapperClassType floatClassType =
            new NumericWrapperClassType("java.lang.Float", Float.class, FieldTypeEnumeration.FLOAT, floatType, this); //NOI18N
        types.put(floatClassType.getName(), floatClassType);
        NumericWrapperClassType doubleClassType =
            new NumericWrapperClassType("java.lang.Double", Double.class, FieldTypeEnumeration.DOUBLE, doubleType, this); //NOI18N
        types.put(doubleClassType.getName(), doubleClassType);

        booleanType.setWrapper(booleanClassType);
        byteType.setWrapper(byteClassType);
        shortType.setWrapper(shortClassType);
        intType.setWrapper(intClassType);
        longType.setWrapper(longClassType);
        charType.setWrapper(charClassType);
        floatType.setWrapper(floatClassType);
        doubleType.setWrapper(doubleClassType);

        bigDecimalType = new MathType("java.math.BigDecimal", BigDecimal.class, FieldTypeEnumeration.BIGDECIMAL, this); //NOI18N
        types.put(bigDecimalType.getName(), bigDecimalType);
        bigIntegerType = new MathType("java.math.BigInteger", BigInteger.class, FieldTypeEnumeration.BIGINTEGER, this); //NOI18N
        types.put(bigIntegerType.getName(), bigIntegerType);

        // Date types
        DateType dateType = new DateType("java.util.Date", java.util.Date.class, FieldTypeEnumeration.UTIL_DATE, this);
        types.put(dateType.getName(), dateType);
        DateType sqldateType = new DateType("java.sql.Date", java.sql.Date.class, FieldTypeEnumeration.SQL_DATE, this);
        types.put(sqldateType.getName(), sqldateType);
        DateType sqlTimeType = new DateType("java.sql.Time", java.sql.Time.class, FieldTypeEnumeration.SQL_TIME, this);
        types.put(sqlTimeType.getName(), sqlTimeType);
        DateType sqlTimestampType = new DateType("java.sql.Timestamp", java.sql.Timestamp.class, FieldTypeEnumeration.SQL_TIMESTAMP, this);
        types.put(sqlTimestampType.getName(), sqlTimestampType);
    }

    /**
     * Checks for the type with the specified name.
     * First the internal type table is checked.
     * If the type is not found it checks Class.forName.
     * If the type is found the internal type table is updated
     * (optimization for further access).
     * If the type is neither in the type table nor found by forName
     * null is returned and the type table is not changed.
     * Otherwise the Type representation of the type is returned.
     * @param  name the name of the type to be checked.
     * @return the Type object representing the type with the
     *         specified name or null when the type was not found.
     */
    public Type checkType(String name)
    {
        synchronized(types)
        {
            Type result = (Type)types.get(name);
            if (result == null)
            {
                // type not found => check repository
                try
                {
                    Class clazz = Class.forName(name, true, classLoader);
                    result = new ClassType(name, clazz, this);
                    types.put(name, result);
                }
                catch (ClassNotFoundException ex)
                {
                    // unknown class -> error message?
                }
            }
            return result;
        }
    }

    /**
     * Checks for the type with the specified name.
     * First the internal type table is checked.
     * If the type is not found it checks Class.forName.
     * If the type is found the internal type table is updated
     * (optimization for further access).
     * If the type is neither in the type table nor found by forName
     * null is returned and the type table is not changed.
     * Otherwise the Type representation of the type is returned.
     * @param  clazz the name of the type to be checked.
     * @return the Type object representing the type with the
     *         specified name or null when the type was not found.
     */
    public Type checkType(Class clazz)
    {
        if (clazz == null)
            return null;
        String name = clazz.getName();
        synchronized (types)
        {
            Type result = (Type)types.get(name);

            if (result == null)
            {
                // type not found
                result = new ClassType(name, clazz, this);
                types.put(name, result);
            }
            return result;
        }
    }

    /**
     * Implements binary numeric promotion as defined in the Java Language Specification section 5.6.2
     */
    public Type binaryNumericPromotion(Type left, Type right)
    {
        if ((left instanceof NumericType) && (right instanceof NumericType))
        {
            if (left.equals(doubleType) || right.equals(doubleType))
                return doubleType;
            else if (left.equals(floatType) || right.equals(floatType))
                return floatType;
            else if (left.equals(longType) || right.equals(longType))
                return longType;
            else
                return intType;
        }
        else
        {
            return errorType;
        }
    }

    /**
     * Implements unray numeric promotion as defined in the Java Language Specification section 5.6.1
     */
    public Type unaryNumericPromotion(Type type)
    {
        if (type instanceof NumericType)
        {
            if (type.equals(byteType) || type.equals(shortType) || type.equals(charType))
            {
                return intType;
            }
            else
            {
                return type;
            }
        }
        else
        {
            return errorType;
        }
    }

    /**
     * Returns true if type is a NumericType or compatible to java.lang.Number
     */
    public boolean isNumberType(Type type)
    {
        Type numberType = checkType("java.lang.Number"); //NOI18N
        Type characterType = checkType("java.lang.Character"); //NOI18N
        return (type instanceof NumericType) ||
               (type.isCompatibleWith(numberType)) ||
               (type.isCompatibleWith(characterType));
    }

    /**
     * Returns true if type is an integral type or a Java wrapper class
     * type wrapping an integral type.
     */
    public boolean isIntegralType(Type type)
    {
        if (type instanceof IntegralType)
            return true;
        else if (type instanceof NumericWrapperClassType)
            return ((NumericWrapperClassType)type).getPrimitiveType() instanceof IntegralType;
        return false;
    }

    /**
     * Returns true if type is a floating point type or a Java wrapper
     * class type wrapping a floating point integral type.
     */
    public boolean isFloatingPointType(Type type)
    {
        if (type instanceof FloatingPointType)
            return true;
        else if (type instanceof NumericWrapperClassType)
            return ((NumericWrapperClassType)type).getPrimitiveType() instanceof FloatingPointType;
        return false;
    }

    /**
     * Returns true if type is double or java.lang.Double
     */
    public boolean isDoubleType(Type type)
    {
        return (type.equals(doubleType) || type.equals(doubleType.getWrapper()));
    }

    /**
     * Returns true if type is int or java.lang.Integer
     */
    public boolean isIntType(Type type)
    {
        return (type.equals(intType) || type.equals(intType.getWrapper()));
    }

    /**
     * Returns true if type is char or java.lang.Character
     */
    public boolean isCharType(Type type)
    {
        return (type.equals(charType) || type.equals(charType.getWrapper()));
    }

    /**
     * Returns true if type is boolean or java.lang.Boolean
     */
    public boolean isBooleanType(Type type)
    {
        return (type.equals(booleanType) || type.equals(booleanType.getWrapper()));
    }

    /**
     * Returns true if type denotes a pertsistence capable class
     * Note, it returns false for non ClassType values, especially for
     * NullType and ErrorType.
     */
    public boolean isPersistenceCapableType(Type type)
    {
        return ((type instanceof ClassType) &&
                ((ClassType)type).isPersistenceCapable());
    }

    /**
     * Returns true if type denotes a collection type.
     * Note, it returns false for non ClassType values, especially for
     * NullType and ErrorType.
     */
    public boolean isCollectionType(Type type)
    {
        Type collectionType = checkType("java.util.Collection"); //NOI18N
        return (type instanceof ClassType) && type.isCompatibleWith(collectionType);
    }

    /**
     * Returns true if type denotes a collection type.
     * Note, it returns false for non ClassType values, especially for
     * NullType and ErrorType.
     */
    public boolean isJavaLangMathType(Type type)
    {
        Type mathType = checkType("java.lang.Math"); //NOI18N
        return (type instanceof ClassType) && type.isCompatibleWith(mathType);
    }

    /**
     * Return JDO QL return type for Sum function for a given type.
     * @param type is a number data type
     */
    public Type getSumReturnType(Type type) {
        if (isFloatingPointType(type)) {
            return doubleType.getWrapper();
        } else if (bigDecimalType.equals(type)) {
            return bigDecimalType;
        } else if (bigIntegerType.equals(type)) {
            return bigIntegerType;
        } else if (isNumberType(type)) {
            return longType.getWrapper();
        } else {
            return type;
        }
    }

    /**
     * Return JDO QL return type for Avg function for a given type.
     * @param type is a number data type
     */
    public Type getAvgReturnType(Type type) {
        if (bigDecimalType.equals(type)) {
            return bigDecimalType;
        } else if (bigIntegerType.equals(type)) {
            return bigIntegerType;
        } else if (isNumberType(type)) {
            return doubleType.getWrapper();
        } else {
            return type;
        }
    }

    /**
     * Return JDO QL return type for Min/Max function for a given type.
     * @param type is an orderable data type
     */
    public Type getMinMaxReturnType(Type type) {
        if (isFloatingPointType(type)) {
            return doubleType.getWrapper();
        } else if (isCharType(type)) {
            return charType.getWrapper();
        } else if (bigDecimalType.equals(type)) {
            return bigDecimalType;
        } else if (bigIntegerType.equals(type)) {
            return bigIntegerType;
        } else if (isNumberType(type)) {
            return longType.getWrapper();
        } else {
            return type;
        }
    }
}
