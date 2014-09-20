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
 * TypeSupport.java
 *
 * Created on November 22, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import java.util.*;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper;

/** 
 * Helper class to support type info access.
 * A type info is statically an object, internally the helper uses the type name
 * as type info. The helper uses a model instance to access meta model info and 
 * uses a NameMapper to map EJB names to JDO names and vice versa.
 * 
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 */
public class TypeSupport
{
    /** Represents the internal error type. */
    public static final Object errorType = "error";
    
    /** Represents the primitive type boolean. */
    public static final Object booleanType = "boolean";

    /** Represents the primitive type byte. */
    public static final Object byteType = "byte";

    /** Represents the primitive type short. */
    public static final Object shortType = "short";

    /** Represents the primitive type char. */
    public static final Object charType = "char";

    /** Represents the primitive type int. */
    public static final Object intType = "int";

    /** Represents the primitive type long. */
    public static final Object longType = "long";

    /** Represents the primitive type float. */
    public static final Object floatType = "float";
    
    /** Represents the primitive type double. */
    public static final Object doubleType = "double";
    
    /** Represents the wrapper class type boolean. */
    public static final Object booleanClassType = "java.lang.Boolean";

    /** Represents the wrapper class type byte. */
    public static final Object byteClassType = "java.lang.Byte";

    /** Represents the wrapper class type short. */
    public static final Object shortClassType = "java.lang.Short";

    /** Represents the wrapper class type char. */
    public static final Object characterClassType = "java.lang.Character";

    /** Represents the wrapper class type int. */
    public static final Object integerClassType = "java.lang.Integer";

    /** Represents the wrapper class type long. */
    public static final Object longClassType = "java.lang.Long";

    /** Represents the wrapper class type float. */
    public static final Object floatClassType = "java.lang.Float";
    
    /** Represents the wrapper class type double. */
    public static final Object doubleClassType = "java.lang.Double";
    
    /** Represents the type java.lang.String. */
    public static final Object stringType = "java.lang.String";

    /** Represents the type java.math.BigDecimal. */
    public static final Object bigDecimalType = "java.math.BigDecimal";

    /** Represents the type java.math.BigInteger. */
    public static final Object bigIntegerType = "java.math.BigInteger";

    /** Set of names of numeric types. */
    protected static final Set numericTypes = new HashSet();

    /** Set of names of numeric wrapper classes. */
    protected static final Set numericWrapperTypes = new HashSet();

    /** Set of names of date and time types. */
    protected static final Set dateTimeTypes = new HashSet();

    /** Meta data access. */
    protected Model model;
    
    /** Name mapping EJB <-> JDO. */
    protected NameMapper nameMapper;
    
    /** I18N support. */
    protected final static ResourceBundle msgs = I18NHelper.loadBundle(
        TypeSupport.class);
    
    /** Inilialize static fields numericTypes numericWrapperTypes. */
    static 
    {
        numericTypes.add(byteType);
        numericTypes.add(shortType);
        numericTypes.add(charType);
        numericTypes.add(intType);
        numericTypes.add(longType);
        numericTypes.add(floatType);
        numericTypes.add(doubleType);
        
        numericWrapperTypes.add(byteClassType);
        numericWrapperTypes.add(shortClassType);
        numericWrapperTypes.add(characterClassType);
        numericWrapperTypes.add(integerClassType);
        numericWrapperTypes.add(longClassType);
        numericWrapperTypes.add(floatClassType);
        numericWrapperTypes.add(doubleClassType);

        dateTimeTypes.add("java.util.Date"); //NOI18N
        dateTimeTypes.add("java.sql.Date"); //NOI18N
        dateTimeTypes.add("java.sql.Time"); //NOI18N
        dateTimeTypes.add("java.sql.Timestamp"); //NOI18N
    }
    
    /** 
     * Creates a new TypeSupport using the specified model instance to 
     * access meta data and the specified nameMapper for EJB <-> JDO 
     * name mapping.
     */
    public TypeSupport(Model model, NameMapper nameMapper)
    {
        this.model = model;
        this.nameMapper = nameMapper;
    }

    /**
     * The method returns a type info by type name. 
     * If the type name denotes a class the name should be fully qualified. 
     * The method uses the type name as type info.
     */
    public Object getTypeInfo(String name)
    {
        return name;
    }
    
    /**
     * The method returns a type info by type name by class object.
     */
    public Object getTypeInfo(Class clazz)
    {
        return getTypeInfo(clazz.getName());
    }

    /** 
     * Returns <code>true</code> if type denotes the error type.
     */
    public static boolean isErrorType(Object type)
    {
        return type.equals(errorType);
    }

    /** 
     * Returns <code>true</code> if type is boolean or java.lang.Boolean
     */
    public static boolean isBooleanType(Object type)
    {
        return type.equals(booleanType) || 
               type.equals(booleanClassType);
    }

    /** 
     * Returns <code>true</code> if type is char or java.lang.Character
     */
    public static boolean isCharType(Object type)
    {
        return type.equals(charType) || 
               type.equals(characterClassType);
    }

    /** 
     * Returns <code>true</code> if type is int or java.lang.Integer
     */
    public static boolean isIntType(Object type)
    {
        return type.equals(intType) || 
               type.equals(integerClassType);
    }

    /** 
     * Returns <code>true</code> if type is double or java.lang.Double.
     */
    public static boolean isDoubleType(Object type)
    {
        return type.equals(doubleType) || 
               type.equals(doubleClassType);
    }

    /** 
     * Returns <code>true</code> if type is a primitive numeric type such as 
     * byte, int etc.
     */
    public static boolean isNumericType(Object type)
    {
        return numericTypes.contains(type);
    }

    /**
     * Returns <code>true</code> if type is a wrapper class of a primitive 
     * numeric type such as java.lang.Byte, java.lang.Integer etc.
     */
    public static boolean isNumericWrapperType(Object type)
    {
        return numericWrapperTypes.contains(type);
    }

    /**
     * Returns <code>true</code> if type is a NumerType, which means it is either
     * a numeric primitive or a numeric wrapper class.
     */
    public static boolean isNumberType(Object type)
    {
        return isNumericType(type) ||
               isNumericWrapperType(type) ||
               bigDecimalType.equals(type) ||
               bigIntegerType.equals(type);
    }

    /**
     * Returns <code>true</code> if type is a floating point type or
     * wrapper class of a floating point type.
     */
    public static boolean isFloatingPointType(Object type)
    {
        return doubleType.equals(type) ||
               doubleClassType.equals(type) ||
               floatType.equals(type) ||
               floatClassType.equals(type);
    }

    /** Returns <code>true</code> if type denotes java.lang.String. */
    public static boolean isStringType(Object type)
    {
        return type.equals(stringType);
    }

    /** Returns <code>true</code> if type is a collection type. */
    public boolean isCollectionType(Object type)
    {
        return model.isCollection((String)type);
    }
    
    /** Returns <code>true</code> if type is a date or time type */
    public boolean isDateTimeType(Object type)
    {
        return dateTimeTypes.contains(getTypeName(type));
    }
   
    /** Returns <code>true</code> if type is an orderable type */
    public boolean isOrderableType(Object type)
    {
        return isNumberType(type) || isDateTimeType(type) || isStringType(type);
    }
   
    /** 
     * Returns the type info for a primitive type. The method returns 
     * {@link #errorType} if the specified type is not a primitive type.
     */
    public static Object getPrimitiveType(Object type)
    {
        Object result = errorType;
        if (type.equals(booleanClassType))
            result = booleanType;
        else if (type.equals(integerClassType))
            result = intType;
        else if (type.equals(longClassType))
            result = longType;
        else if (type.equals(floatClassType))
            result = floatType;
        else if (type.equals(doubleClassType))
            result = doubleType;
        else if (type.equals(byteClassType))
            result = byteType;
        else if (type.equals(shortClassType))
            result = shortType;
        else if (type.equals(characterClassType))
            result = charType;
        return result;
    }

    /** 
     * Returns the type info for a wrapper class type. The method returns 
     * {@link #errorType} if the specified type is not a wrapper class type.
     */
    public static Object getWrapperType(Object type)
    {
        Object result = errorType;
        if (type.equals(booleanType))
            result = booleanClassType;
        else if (type.equals(intType))
            result = integerClassType;
        else if (type.equals(longType))
            result = longClassType;
        else if (type.equals(floatType))
            result = floatClassType;
        else if (type.equals(doubleType))
            result = doubleClassType;
        else if (type.equals(byteType))
            result = byteClassType;
        else if (type.equals(shortType))
            result = shortClassType;
        else if (type.equals(charType))
            result = characterClassType;
        return result;
    }

    /**
     * Implements binary numeric promotion as defined in the 
     * Java Language Specification section 5.6.2
     */
    public static Object binaryNumericPromotion(Object left, Object right)
    {
        if (isNumericType(left) && isNumericType(right)) {
            if (left.equals(doubleType) || right.equals(doubleType))
                return doubleType;
            else if (left.equals(floatType) || right.equals(floatType))
                return floatType;
            else if (left.equals(longType) || right.equals(longType))
                return longType;
            else
                return intType;
        }
        return errorType;
    }

    /**
     * Implements unray numeric promotion as defined in the 
     * Java Language Specification section 5.6.1
     */
    public static Object unaryNumericPromotion(Object type)
    {
        if (isNumericType(type)) {
            if (type.equals(byteType) || type.equals(shortType) || 
                type.equals(charType)) {
                return intType;
            }
            else {
                return type;
            }
        }
        return errorType;
    }

    /** 
     * Implements type compatibility. The method returns <code>true</code> 
     * if left is compatible with right. This is equivalent to 
     * rightClass.isAssignableFrom(leftClass). 
     * Note, the method does not support inheritance.
     */
    public boolean isCompatibleWith(Object left, Object right)
    {
        String leftTypeName = getTypeName(left);
        String rightTypeName = getTypeName(right);

        if (nameMapper.isLocalInterface(leftTypeName) && 
            nameMapper.isEjbName(rightTypeName))
            rightTypeName = nameMapper.getLocalInterfaceForEjbName(rightTypeName);
        else if (nameMapper.isRemoteInterface(leftTypeName) && 
            nameMapper.isEjbName(rightTypeName))
            rightTypeName = nameMapper.getRemoteInterfaceForEjbName(rightTypeName);
        else if (nameMapper.isLocalInterface(rightTypeName) && 
            nameMapper.isEjbName(leftTypeName))
            leftTypeName = nameMapper.getLocalInterfaceForEjbName(leftTypeName);
        else if (nameMapper.isRemoteInterface(rightTypeName) && 
            nameMapper.isEjbName(leftTypeName))
            leftTypeName = nameMapper.getRemoteInterfaceForEjbName(leftTypeName);

        // does not handle inheritance!
        return leftTypeName.equals(rightTypeName);
    }

    /** Returns the type name for a specified type info. */
    public static String getTypeName(Object type)
    {
        return (String)type;
    }
    
    /** Returns the typeInfo (the ejb name) for the specified abstract schema. */
    public Object getTypeInfoForAbstractSchema(String abstractSchema)
    {
        return nameMapper.getEjbNameForAbstractSchema(abstractSchema);
    }

    /** Returns the typeInfo (the ejb name) for the specified abstract schema. */
    public String getAbstractSchemaForTypeInfo(Object typeInfo)
    {
        String typeName = getTypeName(typeInfo);
        return nameMapper.isEjbName(typeName) ? 
            nameMapper.getAbstractSchemaForEjbName(typeName) :
            typeName;
    }

    /** Returns the type info for the type of the given field. */
    public Object getFieldType(Object typeInfo, String fieldName)
    {
        String typeName = getTypeName(typeInfo);
        if (!nameMapper.isEjbName(typeName)) {
            ErrorMsg.fatal(I18NHelper.getMessage(
                msgs, "ERR_EjbNameExpected", //NOI18N
                "TypeSupport.getFieldType", typeName)); //NOI18N
        }
        
        String fieldType = model.getFieldType(typeName, fieldName);
        // check for local or remote interface, map to ejb name
        if (nameMapper.isLocalInterface(fieldType)) {
            fieldType = nameMapper.getEjbNameForLocalInterface(
                typeName, fieldName, fieldType);
        }
        else if (nameMapper.isRemoteInterface(fieldType)) {

            fieldType = nameMapper.getEjbNameForRemoteInterface(
                typeName, fieldName, fieldType);
        }
        return getTypeInfo(fieldType);
    }
    
    /** 
     * Returns the field info for the specified field of the specified type. 
     * The field info is opaque for the caller. Methods {@link #isRelationship}
     * and {@link #getElementType} allow to get details for a given field info.
     */
    public Object getFieldInfo(Object typeInfo, String fieldName)
    {
        Object fieldInfo = null;
        String typeName = getTypeName(typeInfo);
        if (!nameMapper.isEjbName(typeName)) {
            ErrorMsg.fatal(I18NHelper.getMessage(
                msgs, "ERR__EjbNameExpected", //NOI18N
                "TypeSupport.getFieldInfo", typeName)); //NOI18N
        }
        String pcClassName = nameMapper.getPersistenceClassForEjbName(typeName);
        String pcFieldName = nameMapper.getPersistenceFieldForEjbField(
            typeName, fieldName);
        PersistenceClassElement pce = model.getPersistenceClass(pcClassName);
        if (pce != null) {
            fieldInfo = pce.getField(pcFieldName);
        }
        return fieldInfo;
    }
    
    /** 
     * Returns <code>true</code> if the specified field info denotes a 
     * relationship field. 
     */
    public boolean isRelationship(Object fieldInfo)
    {
        return (fieldInfo != null) && (fieldInfo instanceof RelationshipElement);
    }

    /** 
     * Returns the type info of the element type if the specified field info
     * denotes a collection relationship. Otherwise it returns <code>null</code>.
     */
    public Object getElementType(Object fieldInfo)
    {
        if ((fieldInfo != null) && (fieldInfo instanceof RelationshipElement)) {
            String elementClass = ((RelationshipElement)fieldInfo).getElementClass();
            return nameMapper.getEjbNameForPersistenceClass(elementClass);
        }
        else
            return null;
    }

    /** 
     * Gets the name of the persistence-capable class which corresponds to 
     * the specified typeInfo (assuming an ejb name). The method returs the
     * type name of the specified typeInfo, it the typeInfo does not denote
     * an ejb-name (e.g. a local or remote interface).
     */
    public String getPCForTypeInfo(Object typeInfo)
    {
        String typeName = getTypeName(typeInfo);
        String pcClassName = 
            nameMapper.getPersistenceClassForEjbName(typeName);
        return (pcClassName != null) ? pcClassName : typeName;
    }
    
    /** 
     * Returns <code>true</code> if the specified type info denotes an ejb name.
     */
    public boolean isEjbName(Object typeInfo)
    {
        return nameMapper.isEjbName(getTypeName(typeInfo));
    }
    
    /** 
     * Returns <code>true</code> if the specified type info denotes an ejb name 
     * or the name of a local interface or the name of a remote interface.
     */
    public boolean isEjbOrInterfaceName(Object typeInfo)
    {
        String typeName = getTypeName(typeInfo);
        return nameMapper.isEjbName(typeName) || 
               nameMapper.isLocalInterface(typeName) || 
               nameMapper.isRemoteInterface(typeName);
    }

    /**
     * Returns <code>true</code> if the specified type info denotes the
     * remote interface of the bean with the specified ejb name.
     */
    public boolean isRemoteInterfaceOfEjb(Object typeInfo, String ejbName)
    {
        String typeName = getTypeName(typeInfo);
        String remoteInterface = nameMapper.getRemoteInterfaceForEjbName(ejbName);
        return (remoteInterface != null) && remoteInterface.equals(typeName);
        
    }

    /**
     * Returns <code>true</code> if the specified type info denotes the
     * local interface of the bean with the specified ejb name.
     */
    public boolean isLocalInterfaceOfEjb(Object typeInfo, String ejbName)
    {
        String typeName = getTypeName(typeInfo);
        String localInterface = nameMapper.getLocalInterfaceForEjbName(ejbName);
        return (localInterface != null) && localInterface.equals(typeName);
    }

    /** 
     * Returns <code>true</code> if the specified type info denotes
     * a remote interface.
     */
    public boolean isRemoteInterface(Object typeInfo)
    {
        return nameMapper.isRemoteInterface(getTypeName(typeInfo));
    }

    /** 
     * Returns <code>true</code> if the specified type info denotes
     * a local interface.
     */
    public boolean isLocalInterface(Object typeInfo)
    {
        return nameMapper.isLocalInterface(getTypeName(typeInfo));
    }

    /** 
     * Returns <code>true</code> if the bean with the specified ejb name
     * has a remote interface.
     */
    public boolean hasRemoteInterface(Object typeInfo)
    {
        return nameMapper.getRemoteInterfaceForEjbName(
            getTypeName(typeInfo)) != null;
    }

    /** 
     * Returns <code>true</code> if the bean with the specified ejb name
     * has a local interface.
     */
    public boolean hasLocalInterface(Object typeInfo)
    {
        return nameMapper.getLocalInterfaceForEjbName(
            getTypeName(typeInfo)) != null;
    }

    /**
     * Return JDO QL return type for Sum function for a given type.
     * @param type is a number data type
     */
    public Object getSumReturnType(Object type) {
        if (isFloatingPointType(type)) {
            return doubleClassType;
        } else if (isNumericType(type) || isNumericWrapperType(type)) {
            return longClassType;
        } else {
            return type;
        }
    }

    /**
     * Return JDO QL return type for Avg function for a given type.
     * @param type is a number data type
     */
    public Object getAvgReturnType(Object type) {
        if (isNumericType(type) || isNumericWrapperType(type)) {
            return doubleClassType;
        } else {
            return type;
        }
    }

    /**
     * Return JDO QL return type for Min/Max function for a given type.
     * @param type is an orderable data type
     */
    public Object getMinMaxReturnType(Object type) {
        if (isFloatingPointType(type)) {
            return doubleClassType;
        } else if (isCharType(type)) {
            return characterClassType;
        } else if (isNumericType(type) || isNumericWrapperType(type)) {
            return longClassType;
        } else {
            return type;
        }
    }
}
