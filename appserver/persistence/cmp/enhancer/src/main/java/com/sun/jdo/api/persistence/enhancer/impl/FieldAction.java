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

package com.sun.jdo.api.persistence.enhancer.impl;

import java.util.Map;
import java.util.Enumeration;

import com.sun.jdo.api.persistence.enhancer.classfile.ClassFile;
import com.sun.jdo.api.persistence.enhancer.classfile.ClassField;
import com.sun.jdo.api.persistence.enhancer.classfile.VMConstants;
import com.sun.jdo.api.persistence.enhancer.classfile.Descriptor;

import com.sun.jdo.api.persistence.enhancer.util.Support;
import com.sun.jdo.api.persistence.enhancer.util.InternalError;

//@olsen: added import
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;


//@olsen: cosmetics
//@olsen: moved: this class -> package impl
//@olsen: subst: [iI]Persistent -> [pP]ersistenceCapable
//@olsen: subst: jdo/ -> com/sun/forte4j/persistence/internal/
//@olsen: subst: /* ... */ -> // ...
//@olsen: subst: isKnownPersistent -> JDOMetaData.isSecondClassObjectType
//@olsen: subst: FilterEnv -> Environment
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: Hashtable -> Map, HashMap
//@olsen: subst: absolut jdo types and names -> constants from JDOMetaData
//@olsen: subst: theClass, classAction -> ca
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, affirm()
//@olsen: removed: proprietary support for FieldNote
//@olsen: removed: proprietary support for IndexableField
//@olsen: removed: support for IgnoreTransientField, AddedTransientField
//@olsen: removed: old, disabled ODI code

//^olsen: clean-up fieldTypeInfo
//^olsen: remove proprietary support for ClassInfo

/**
 * FieldAction contains the annotation related information specific
 * to a single field of a class.
 */
final class FieldAction
    extends Support
    implements VMConstants {

    /* The field which we contain information about */
    //@olsen: made final
    private ClassField theField;

    /* The parent ClassAction of this FieldAction */
    //@olsen: made final
    private ClassAction ca;

    /* Central repository for the options and classes */
    //@olsen: added association
    //@olsen: made final
    private final Environment env;

    /* true if this persistent field is primary key. */
    //@olsen: added field
    private boolean fieldIsPrimaryKey;

    /* true if this persistent field's value is a second class object. */
    //@olsen: added field
    private boolean fieldIsMutableSCO;

    /* true if this is a non-static, non-final, non-transient field
     * and the declared type appears to be perisistence-capable.
     * This is not valid until the check method runs */
    private boolean fieldIsPersistent;

    /* zero for a non-array - otherwise, the number of array dimensions in
     * the type of the field. */
    private int nFieldArrayDims;

    /* Name of class or interface type, or base type of array */
    private String fieldClassName;

    /* Information about the type of the field.  Used to determine how
     * to initialize, flush, clear, etc. */
    private FieldTypeInfo fieldTypeInfo;

    /* The persistent field index for this method */
//@olsen: disabled feature
/*
    private int fieldIndex = -1;
*/

    /**
     * Constructor.
     */
    //@olsen: added parameter 'env' for association
    FieldAction(ClassAction ca,
                ClassField field,
                Environment env) {
        this.ca = ca;
        this.theField = field;
        this.env = env;
    }

    // package accessors

    /**
     * Get the VM type descriptor field string for the field type
     */
    String typeDescriptor() {
        return theField.signature().asString();
    }

    /**
     * Get the VM type name field string for the field type
     * This is the same as the type descriptor except when it is
     * a non-array class - in this case, the leading 'L' and trailing
     * ';' need to be removed.
     */
    String typeName() {
        String typeDesc = typeDescriptor();
        if (typeDesc.charAt(0) == 'L')
            return typeDesc.substring(1, typeDesc.length() - 1);
        return typeDesc;
    }

    /**
     * Get the field index for this field.
     * The index must have previously been set.
     */
//@olsen: disabled feature
/*
    int index() {
        if (fieldIndex < 0)
            throw new InternalError("The field index has not yet been set");
        return fieldIndex;
    }
*/

    /**
     * Set the field index for this field.
     */
//@olsen: disabled feature
/*
    void setIndex(int idx) {
        fieldIndex = idx;
    }
*/

    /**
     * Is this persistent field primary key?
     */
    //@olsen: added method
    boolean isPrimaryKey() {
        return fieldIsPrimaryKey;
    }

    /**
     * Is this persistent field's value is a second class object?
     */
    //@olsen: added method
    boolean isMutableSCO() {
        return fieldIsMutableSCO;
    }

    /**
     * Is this field one which is stored persistently?  This can only
     * be true for non-static, non-final fields.
     */
    boolean isPersistent() {
        return fieldIsPersistent;
    }

    /**
     * Return the name of the field
     */
    String fieldName() {
        return theField.name().asString();
    }

    /**
     * Is the field a synthetic field?
     * This is a java 1.1'ism for nested classes
     */
    boolean isSynthetic() {
        return theField.attributes().findAttribute("Synthetic") != null;//NOI18N
    }

    /**
     * Return the name of the static method on class Field which
     * will create a Field of the appropriate type.
     */
    String createMethod() {
        return fieldTypeInfo.fieldCreateMethod;
    }

    /**
     * Return the type signature of the static method on class Field which
     * will create a Field of the appropriate type.
     */
    String createMethodSig() {
        return fieldTypeInfo.fieldCreateMethodSig;
    }

    /**
     * Return the name of the static method on class GenericObject which
     * will set the field value.
     */
    String setMethod() {
        return fieldTypeInfo.fieldSetMethod;
    }

    /**
     * Return the type signature of the static method on class GenericObject
     * which will set the field value.
     */
    String setMethodSig() {
        return fieldTypeInfo.fieldSetMethodSig;
    }

    /**
     * Return the type of arg expected by the set method.
     */
    int setMethodArg() {
        return fieldTypeInfo.fieldSetArgType;
    }

    /**
     * Return the name of the static method on class GenericObject which
     * will get the field value.
     */
    String getMethod() {
        return fieldTypeInfo.fieldGetMethod;
    }

    /**
     * Return the type signature of the static method on class GenericObject
     * which will get the field value.
     */
    String getMethodSig() {
        return fieldTypeInfo.fieldGetMethodSig;
    }

    /**
     * Return the return type of the get method.
     */
    int getMethodReturn() {
        return fieldTypeInfo.fieldGetReturnType;
    }

    /**
     * For references fields, return the base type class name if a class
     * or interface, else null.
     */
    String fieldClassName() {
        return fieldClassName;
    }

    /**
     * For array fields, return the number of dimensions in the array type
     * else 0.
     */
    int nDims () {
        return nFieldArrayDims;
    }

    /**
     * Examine the field to decide what actions are required
     */
    void check() {
        //@olsen: improved control flow
        //@olsen: dropped code computing persistence information;
        //        used JDO meta data instead

        String sig = theField.signature().asString();
        fieldTypeInfo = FieldTypeInfo.determineFieldType(sig, env);

        final String className = ca.className();
        final String userClass = ca.userClassName();
        final String fieldName = theField.name().asString();
        final String fullFieldName = userFieldName();

        //@olsen: added shortcut
        final JDOMetaData jdoMetaData = env.getJDOMetaData();

        //@olsen: use JDO meta data to decide whether a field is persistent
        //@olsen: subst: fieldShouldBeTransient -> !fieldShouldBePersistent
        final boolean fieldShouldBePersistent
            = jdoMetaData.isPersistentField(className, fieldName);
        //@olsen: added println() for debugging
        if (false) {
            System.out.println("FieldAction.check(): field "//NOI18N
                               + className + "/" + fieldName//NOI18N
                               + " should be persistent = "//NOI18N
                               + fieldShouldBePersistent);
        }

        //@olsen: initialized property from JDO meta data
        fieldIsPrimaryKey
            = jdoMetaData.isPrimaryKeyField(className, fieldName);
        //@olsen: added println() for debugging
        if (false) {
            System.out.println("FieldAction.check(): field "//NOI18N
                               + className + "/" + fieldName//NOI18N
                               + " is primary key = "//NOI18N
                               + fieldIsPrimaryKey);
        }

        //@olsen: initialized property from JDO meta data
        fieldIsMutableSCO
            = jdoMetaData.isMutableSecondClassObjectType(typeName());
        //@olsen: added println() for debugging
        if (false) {
            System.out.println("FieldAction.check(): field "//NOI18N
                               + className + "/" + fieldName//NOI18N
                               + " is mutable SCO = "//NOI18N
                               + fieldIsMutableSCO);
        }

        nFieldArrayDims = 0;
        while (sig.charAt(nFieldArrayDims) == '[')
            nFieldArrayDims++;

        // If the base type is a class type, compute the class name
        if (sig.charAt(nFieldArrayDims) == 'L')
            fieldClassName = sig.substring(nFieldArrayDims+1, sig.length()-1);

        // check for transient field
        if (!fieldShouldBePersistent) {
            // done with transient field
            return;
        }

        //@olsen: dropped code ...

        // check for static field
        affirm(!theField.isStatic(),
               ("The field " + fullFieldName//NOI18N
                + " is a static field which cannot be made persistent."));//NOI18N

        // check for final field
        affirm(!theField.isFinal(),
               ("The field " + fullFieldName +//NOI18N
                " is a final field which cannot be made persistent."));//NOI18N

        // check for target type
        affirm((fieldClassName == null
                || jdoMetaData.isSecondClassObjectType(fieldClassName)
                || jdoMetaData.isPersistenceCapableClass(fieldClassName)),
               ("The field " + fullFieldName//NOI18N
                + " cannot be made persistent because of a non-primitive, "//NOI18N
                + " non-sco, or non-pc target type " + fieldClassName));//NOI18N

        fieldIsPersistent = true;
    }

    /**
     * Retarget class references according to the class name mapping
     * table.
     */
//@olsen: disabled feature
/*
    void retarget(Map classTranslations) {
        if (fieldClassName != null) {
            String mapTo = (String)classTranslations.get(fieldClassName);
            if (mapTo != null)
                fieldClassName = mapTo;
        }
    }
*/

    /**
     * Return a user consumable field name
     */
    String userFieldName() {
        return ca.userClassName() + "." + theField.name().asString();//NOI18N
    }

    /**
     * Return a user consumable signature
     */
    private String userSig(String vmSig) {
        // Stub: just return vm sig for now
        return Descriptor.userFieldSig(vmSig);
    }
}


class FieldTypeInfo
    extends Support
    implements VMConstants {

    /* Name and type signature of the Field.create method */
    String fieldCreateMethod;
    String fieldCreateMethodSig;

    /* Name and type signature of the GenericObject.get method */
    String fieldGetMethod;
    String fieldGetMethodSig;
    int fieldGetReturnType;

    /* Name and type signature of the GenericObject.set method */
    String fieldSetMethod;
    String fieldSetMethodSig;
    int fieldSetArgType;

    // constructor

    private FieldTypeInfo(String createName, String createSig,
                          String setName, String setSig, int argType,
                          String getName, String getSig, int returnType) {
        fieldCreateMethod = createName;
        fieldCreateMethodSig = createSig;
        fieldGetMethod = getName;
        fieldGetMethodSig = getSig;
        fieldGetReturnType = returnType;
        fieldSetMethod = setName;
        fieldSetMethodSig = setSig;
        fieldSetArgType = argType;
    }

    static private FieldTypeInfo byteInfo =
    new FieldTypeInfo("createByte", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setByteField", "(IBLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_BYTE,//NOI18N
                      "getByteField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)B", T_BYTE);//NOI18N

    static private FieldTypeInfo charInfo =
    new FieldTypeInfo("createChar", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setCharField", "(ICLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_CHAR,//NOI18N
                      "getCharField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)C", T_CHAR);//NOI18N

    static private FieldTypeInfo shortInfo =
    new FieldTypeInfo("createShort", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setShortField", "(ISLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_SHORT,//NOI18N
                      "getShortField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)S", T_SHORT);//NOI18N

    static private FieldTypeInfo intInfo =
    new FieldTypeInfo("createInt", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setIntField", "(IILcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_INT,//NOI18N
                      "getIntField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)I", T_INT);//NOI18N

    static private FieldTypeInfo longInfo =
    new FieldTypeInfo("createLong", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setLongField", "(IJLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_LONG,//NOI18N
                      "getLongField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)J", T_LONG);//NOI18N

    static private FieldTypeInfo floatInfo =
    new FieldTypeInfo("createFloat", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setFloatField", "(IFLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_FLOAT,//NOI18N
                      "getFloatField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)F", T_FLOAT);//NOI18N

    static private FieldTypeInfo doubleInfo =
    new FieldTypeInfo("createDouble", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setDoubleField", "(IDLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_DOUBLE,//NOI18N
                      "getDoubleField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)D", T_DOUBLE);//NOI18N

    static private FieldTypeInfo booleanInfo =
    new FieldTypeInfo("createBoolean", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setBooleanField", "(IZLcom/sun/forte4j/persistence/internal/ClassInfo;)V", T_BOOLEAN,//NOI18N
                      "getBooleanField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Z", T_BOOLEAN);

    static private FieldTypeInfo classInfo =
    new FieldTypeInfo("createClass", "(Ljava/lang/String;Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setClassField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getClassField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

//@olsen: disabled feature
/*
    //@olsen: don't distinguish between class and interface types
    static private FieldTypeInfo interfaceInfo =
    new FieldTypeInfo("createInterface", "(Ljava/lang/String;Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",
                      "setInterfaceField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,
                      "getInterfaceField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);
*/

    static private FieldTypeInfo stringInfo =
    new FieldTypeInfo("createString", "(Ljava/lang/String;)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setStringField", "(ILjava/lang/String;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_STRING,//NOI18N
                      "getStringField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/String;", TC_STRING);//NOI18N

    static private FieldTypeInfo byteArrayInfo =
    new FieldTypeInfo("createByteArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo charArrayInfo =
    new FieldTypeInfo("createCharArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo shortArrayInfo =
    new FieldTypeInfo("createShortArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo intArrayInfo =
    new FieldTypeInfo("createIntArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo longArrayInfo =
    new FieldTypeInfo("createLongArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo floatArrayInfo =
    new FieldTypeInfo("createFloatArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo doubleArrayInfo =
    new FieldTypeInfo("createDoubleArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo booleanArrayInfo =
    new FieldTypeInfo("createBooleanArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo classArrayInfo =
    new FieldTypeInfo("createClassArray", "(Ljava/lang/String;Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo interfaceArrayInfo =
    new FieldTypeInfo("createInterfaceArray", "(Ljava/lang/String;Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N

    static private FieldTypeInfo stringArrayInfo =
    new FieldTypeInfo("createStringArray", "(Ljava/lang/String;I)Lcom/sun/forte4j/persistence/internal/Field;",//NOI18N
                      "setArrayField", "(ILjava/lang/Object;Lcom/sun/forte4j/persistence/internal/ClassInfo;)V", TC_OBJECT,//NOI18N
                      "getArrayField", "(ILcom/sun/forte4j/persistence/internal/ClassInfo;)Ljava/lang/Object;", TC_OBJECT);//NOI18N


    static FieldTypeInfo determineFieldType(String sig,
                                            Environment env) {
        switch (sig.charAt(0)) {
        case 'B': // byte
            return byteInfo;
        case 'C': // char
            return charInfo;
        case 'D': // double
            return doubleInfo;
        case 'F': // float
            return floatInfo;
        case 'I': // int
            return intInfo;
        case 'J': // long
            return longInfo;
        case 'S': // short
            return shortInfo;
        case 'Z': // boolean
            return booleanInfo;
        case 'L': // class or interface
            if (sig.equals("Ljava/lang/String;"))//NOI18N
                return stringInfo;
            {
//@olsen: disabled feature
//@olsen: don't distinguish between class and interface types
//@olsen: don't read-in classes here!
/*
                ClassControl cc
                    = env.findClass(sig.substring(1, sig.length()-1));
                // Don't sweat it if we don't find the class - it's the
                // responsibility of the caller to check that
                if (cc != null && cc.classFile().isInterface())
                    return interfaceInfo;
*/
                return classInfo;
            }
        case '[': // array
            int baseTypeIndex = findArrayBaseType(sig);
            switch (sig.charAt(baseTypeIndex)) {
            case 'B': // byte
                return byteArrayInfo;
            case 'C': // char
                return charArrayInfo;
            case 'D': // double
                return doubleArrayInfo;
            case 'F': // float
                return floatArrayInfo;
            case 'I': // int
                return intArrayInfo;
            case 'J': // long
                return longArrayInfo;
            case 'S': // short
                return shortArrayInfo;
            case 'Z': // boolean
                return booleanArrayInfo;
            case 'L': // class or interface
                if (sig.substring(baseTypeIndex).equals("Ljava/lang/String;"))//NOI18N
                    return stringArrayInfo;
                {
//@olsen: disabled feature
//@olsen: don't distinguish between class and interface types
//@olsen: don't read-in classes here!
/*
                    ClassControl cc
                        = env.findClass(sig.substring(baseTypeIndex+1,
                                                      sig.length()-1));
                    // Don't sweat it if we don't find the class - it's the
                    // responsibility of the caller to check that
                    if (cc != null && cc.classFile().isInterface())
                        return interfaceArrayInfo;
*/
                    return classArrayInfo;
                }
            default:
                throw new InternalError("Missing case");//NOI18N
            }

        default:
            throw new InternalError("Missing case");//NOI18N
        }
    }

    private static int findArrayBaseType(String sig) {
        int idx = 0;
        while (sig.charAt(idx) == '[')
            idx++;
        return idx;
    }
}

