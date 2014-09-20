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

package com.sun.jdo.api.persistence.enhancer.generator;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.jdo.api.persistence.enhancer.meta.ExtendedJDOMetaData;
import com.sun.jdo.api.persistence.enhancer.util.Assertion;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;
import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriterHelper;

/**
 *
 */
final class ImplHelper extends Assertion
{
    // string constants
    static final String[] COMMENT_ENHANCER_ADDED
    = null; //{ "added by enhancer" };
    static final String[] COMMENT_NOT_ENHANCER_ADDED
    = null; //{ "not added by enhancer" };

    static final String CLASSNAME_JDO_PERSISTENCE_CAPABLE
    = "com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable";
    static final String CLASSNAME_JDO_PERSISTENCE_MANAGER
    = "com.sun.jdo.api.persistence.support.PersistenceManager";
    static final String CLASSNAME_JDO_STATE_MANAGER
    = "com.sun.jdo.spi.persistence.support.sqlstore.StateManager";
    static final String CLASSNAME_JDO_SCO
    = "com.sun.jdo.spi.persistence.support.sqlstore.SCO";
    static final String CLASSNAME_JDO_FATAL_EXCEPTION
    = "com.sun.jdo.api.persistence.support.JDOFatalException";

    static final String FIELDNAME_JDO_FLAGS
    = "jdoFlags";
    static final String FIELDNAME_JDO_STATE_MANAGER
    = "jdoStateManager";
    static final String FIELDNAME_JDO_INHERITED_FIELD_COUNT
    = "jdoInheritedFieldCount";
    static final String FIELDNAME_JDO_FIELD_NAMES
    = "jdoFieldNames";
    static final String FIELDNAME_JDO_FIELD_TYPES
    = "jdoFieldTypes";
    static final String FIELDNAME_JDO_FIELD_FLAGS
    = "jdoFieldFlags";
    static final String METHODNAME_JDO_NEW_INSTANCE
    = "jdoNewInstance";
    static final String METHODNAME_JDO_SET_FIELD
    = "jdoSetField";
    static final String METHODNAME_JDO_GET_FIELD
    = "jdoGetField";
    static final String METHODNAME_JDO_GET_STATE_MANAGER
    = "jdoGetStateManager";
    static final String METHODNAME_JDO_SET_STATE_MANAGER
    = "jdoSetStateManager";
    static final String METHODNAME_JDO_GET_FLAGS
    = "jdoGetFlags";
    static final String METHODNAME_JDO_SET_FLAGS
    = "jdoSetFlags";
    static final String METHODNAME_JDO_GET_PERSISTENCE_MANAGER
    = "jdoGetPersistenceManager";
    static final String METHODNAME_JDO_CLEAR
    = "jdoClear";
    static final String METHODNAME_JDO_MAKE_DIRTY
    = "jdoMakeDirty";
    static final String METHODNAME_JDO_GET_OBJECT_ID
    = "jdoGetObjectId";
    static final String METHODNAME_JDO_IS_PERSISTENT
    = "jdoIsPersistent";
    static final String METHODNAME_JDO_IS_TRANSACTIONAL
    = "jdoIsTransactional";
    static final String METHODNAME_JDO_IS_NEW
    = "jdoIsNew";
    static final String METHODNAME_JDO_IS_DIRTY
    = "jdoIsDirty";
    static final String METHODNAME_JDO_IS_DELETED
    = "jdoIsDeleted";

    static private final HashMap typeNameConversion = new HashMap();
    static
    {
        typeNameConversion.put(int.class.getName(), "Int");
        typeNameConversion.put(long.class.getName(), "Long");
        typeNameConversion.put(byte.class.getName(), "Byte");
        typeNameConversion.put(char.class.getName(), "Char");
        typeNameConversion.put(boolean.class.getName(), "Boolean");
        typeNameConversion.put(short.class.getName(), "Short");
        typeNameConversion.put(float.class.getName(), "Float");
        typeNameConversion.put(double.class.getName(), "Double");
        typeNameConversion.put("String", "String");
    }

    static private boolean isPrimitiveClass(String className)
    {
        affirm(!className.equals("void"));      // NOI18N

        return JavaTypeHelper.getWrapperName(className) != null;
    }

    static private String getConvertedTypeName(String fieldType)
    {
        final String name = (String)typeNameConversion.get(fieldType);
        return (name != null ? name : JavaClassWriterHelper.Object_);
    }

    static private String getMethodNameGetField(String fieldType)
    {
        return JavaClassWriterHelper.get_ +
               getConvertedTypeName(fieldType) + "Field";
    }

    static private String getMethodNameSetField(String fieldType)
    {
        return JavaClassWriterHelper.set_ +
               getConvertedTypeName(fieldType) + "Field";
    }


    // Create bodies of methods.

    static String[] getJDOManagedFieldCountImpl(int fieldcount)
    {
        return new String[] {
            FIELDNAME_JDO_INHERITED_FIELD_COUNT + " + " + fieldcount + JavaClassWriterHelper.delim_
        };
    }

    static String[] getDefaultConstructorImpl()
    {
        return JavaClassWriterHelper.super_;
    }

    static String[] getCloneImpl(String className)
    {
        className = normalizeClassName(className);
        String[] bodies = new String[4];
        int i = 0;
        bodies[i++] = (new StringBuffer(className)).append(" clone = (")
                 .append(className).append(")super.clone();").toString();
        bodies[i++] = "clone.jdoFlags = (byte)0;";
        bodies[i++] = "clone.jdoStateManager = null;";
        bodies[i++] = "return clone;";
        return bodies;
    }

    static String[] getJDOConstructorSMImpl(String statemanager)
    {
        String[] bodies = new String[2];
        int i = 0;
        bodies[i++] = FIELDNAME_JDO_FLAGS + " = (byte)1; // == LOAD_REQUIRED";
        bodies[i++] = "this." + FIELDNAME_JDO_STATE_MANAGER
                 + " = " + statemanager + JavaClassWriterHelper.delim_;
        return bodies;
    }

    static String[] getJDONewInstanceImpl(String className,
                                      String statemanager)
    {
        className = getClassName(className);
        return new String[] { (new StringBuffer("return new "))
                  .append(className).append("(").append(statemanager)
                  .append(");").toString() };
    }

    static String[] getFieldDirectReadImpl(String fieldName,
                                       String fieldType,
                                       int fieldNumber)
    {
        normalizeClassName(fieldType);
        return new String[] {
            "// annotation: grant direct read access",
            "return " + fieldName + JavaClassWriterHelper.delim_
        };
    }

    static String[] getFieldMediateReadImpl(String fieldName,
                                        String fieldType,
                                        int fieldNumber)
    {
        normalizeClassName(fieldType);
        String[] bodies = new String[6];
        int i = 0;
        bodies[i++] = "// annotation: mediate read access";
        bodies[i++] = (new StringBuffer("final "))
                 .append(CLASSNAME_JDO_STATE_MANAGER)
                 .append(" stateManager = this.")
                 .append(FIELDNAME_JDO_STATE_MANAGER)
                 .append(JavaClassWriterHelper.delim_).toString();
        bodies[i++] = "if (stateManager != null) {";
        bodies[i++] = (new StringBuffer("    "))
                .append("stateManager.prepareGetField(").append(fieldNumber)
                .append(");").toString();
        bodies[i++] = "}";
        bodies[i++] = "return " + fieldName + JavaClassWriterHelper.delim_;
        return bodies;
    }

    static String[] getFieldCheckReadImpl(String fieldName,
                                      String fieldType,
                                      int fieldNumber)
    {
        String[] bodies = new String[5];
        int i = 0;
        normalizeClassName(fieldType);
        bodies[i++] = "// annotation: check read access";
        bodies[i++] = (new StringBuffer("if ("))
                 .append(FIELDNAME_JDO_FLAGS).append(" > 0) {").toString();
        bodies[i++] = "   " + FIELDNAME_JDO_STATE_MANAGER + ".loadForRead();";
        bodies[i++] = "}";
        bodies[i++] = "return " + fieldName + JavaClassWriterHelper.delim_;
        return bodies;
    }

    static String[] getFieldDirectWriteImpl(String fieldName,
                                           String fieldType,
                                           int fieldNumber,
                                           String newvalue)
    {
        normalizeClassName(fieldType);
        return new String[] {
            "// annotation: grant direct write access",
            (new StringBuffer("this."))
                 .append(fieldName).append(" = ").append(newvalue)
                 .append(JavaClassWriterHelper.delim_).toString()
        };
    }

    static String[] getFieldMediateWriteImpl(String fieldName,
                                         String fieldType,
                                         int fieldNumber,
                                         String newvalue)
    {
        String[] bodies = new String[7];
        int i = 0;
        fieldType = normalizeClassName(fieldType);
        bodies[i++] = "// annotation: mediate write access";
        bodies[i++] = (new StringBuffer("final "))
                 .append(CLASSNAME_JDO_STATE_MANAGER)
                 .append(" stateManager = this.")
                 .append(FIELDNAME_JDO_STATE_MANAGER)
                 .append(JavaClassWriterHelper.delim_).toString();
        bodies[i++] = "if (stateManager == null) {";
        bodies[i++] = (new StringBuffer("    this."))
                 .append(fieldName).append(" = ")
                 .append(newvalue).append(JavaClassWriterHelper.delim_)
                 .toString();
        bodies[i++] = "} else {";
        bodies[i++] = (new StringBuffer("    stateManager."))
                 .append(getMethodNameSetField(fieldType)).append('(')
                 .append(fieldNumber).append(", ").append(newvalue)
                 .append(");").toString();
        bodies[i++] = "}";
        return bodies;
    }

    static String[] getFieldCheckWriteImpl(String fieldName,
                                       String fieldType,
                                       int fieldNumber,
                                       String newvalue)
    {
        String[] bodies = new String[5];
        int i = 0;
        normalizeClassName(fieldType);
        bodies[i++] = "// annotation: check write access";
        bodies[i++] = "if (" + FIELDNAME_JDO_FLAGS + " != 0) {";
        bodies[i++] = "    " + FIELDNAME_JDO_STATE_MANAGER + ".loadForUpdate();";
        bodies[i++] = "}";
        bodies[i++] = (new StringBuffer("this.")).append(fieldName)
                 .append(" = ").append(newvalue)
                 .append(JavaClassWriterHelper.delim_).toString();
        return bodies;
    }

    static String[] getJDOClearImpl(String className, ExtendedJDOMetaData meta,
            String[] fieldNames, String[] fieldTypes) {
        final List impl = new ArrayList(20);
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldTypeClassPath = fieldTypes[i];
            String fieldType = normalizeClassName(fieldTypes[i]);
            String fieldName = fieldNames[i];
            if (meta.isKeyField(className, fieldName)) {
                continue;
            }
            String primClass = JavaTypeHelper.getWrapperName(fieldType);
            if (meta.isMutableSecondClassObjectType(fieldTypeClassPath)) {
                impl.add((new StringBuffer("if ("))
                        .append(fieldName).append(" instanceof ")
                        .append(CLASSNAME_JDO_SCO).append(") {")
                        .toString());
                impl.add((new StringBuffer("    (("))
                        .append(CLASSNAME_JDO_SCO).append(")")
                        .append(fieldName).append(").unsetOwner();")
                        .toString());
                impl.add("}");
                impl.add(fieldName + " = null;");
            } else if (primClass == null) {
                impl.add(fieldName + " = null;");
            } else if (JavaClassWriterHelper.boolean_.equals(fieldType)) {
                impl.add(fieldName + " = false;");
            } else {
                impl.add(fieldName + " = 0;");
            }
        }
        String[] strArr = new String[impl.size()];
        return (String[])impl.toArray(strArr);
    }

    static String[] getJDOGetFieldImpl(String fieldNumber,
                                   String[] fieldNames,
                                   String[] fieldTypes)
    {
        final List impl = new ArrayList(20);
        impl.add("switch (" + fieldNumber + ") {");
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldType = normalizeClassName(fieldTypes[i]);
            impl.add("case " + i + ':');
            String primClass = JavaTypeHelper.getWrapperName(fieldType);
            if (primClass == null) {
                impl.add("    return " + fieldNames[i] + ";");
            } else {
                impl.add((new StringBuffer("    return new "))
                          .append(primClass).append("(")
                          .append(fieldNames[i]).append(");").toString());
            }
        }
        impl.add("default:");
        impl.add("    throw new " + CLASSNAME_JDO_FATAL_EXCEPTION + "();");
        impl.add("}");
        String[] strArr = new String[impl.size()];
        return (String[])impl.toArray(strArr);
    }

    static String[] getJDOSetFieldImpl(String fieldNumber, String objName,
                                       String[] fieldNames,
                                       String[] fieldTypes)
    {
        final List impl = new ArrayList(20);
        impl.add("switch (" + fieldNumber + ") {");
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldType = normalizeClassName(fieldTypes[i]);
            impl.add("case " + i + ':');
            String primClass = JavaTypeHelper.getWrapperName(fieldType);
            if (primClass == null) {
                impl.add((new StringBuffer("    this."))
                          .append(fieldNames[i])
                          .append(" = (").append(fieldType)
                          .append(")").append(objName)
                          .append(";").toString());
            } else {
                impl.add((new StringBuffer("    this."))
                          .append(fieldNames[i])
                          .append(" = ((").append(primClass)
                          .append(")").append(objName).append(").")
                          .append(((String)typeNameConversion.get(fieldType)).toLowerCase())
                          .append("Value();").toString());
            }
            impl.add("    return;");
        }
        impl.add("default:");
        impl.add("    throw new " + CLASSNAME_JDO_FATAL_EXCEPTION + "();");
        impl.add("}");
        String[] strArr = new String[impl.size()];
        return (String[])impl.toArray(strArr);
    }

    // returnType = null means void
    private static String[] getJDOStateManagerDelegationImpl(
            String delegation, String returnType) {
        final List impl = new ArrayList(7);
        impl.add((new StringBuffer("final "))
                 .append(CLASSNAME_JDO_STATE_MANAGER)
                 .append(" stateManager = this.")
                 .append(FIELDNAME_JDO_STATE_MANAGER)
                 .append(JavaClassWriterHelper.delim_)
                 .toString());
        impl.add("if (stateManager != null) {");
        StringBuffer buf = new StringBuffer("    ");
        if (returnType != null) {
            buf.append("return ");
        }
        impl.add(((buf.append("stateManager."))
                  .append(delegation).append(";")).toString());
        impl.add("}");
        if (returnType != null) {
            impl.add((new StringBuffer("return ")).append(returnType)
                        .append(";").toString());
        };
        String[] strArr = new String[impl.size()];
        return (String[])impl.toArray(strArr);
    }

    static String[] getJDOStateManagerVoidDelegationImpl(String delegation)
    {
        return getJDOStateManagerDelegationImpl(delegation, null);
    }

    static String[] getJDOStateManagerObjectDelegationImpl(String delegation)
    {
        return getJDOStateManagerDelegationImpl(delegation,
                JavaClassWriterHelper.null_);
    }

    static String[] getJDOStateManagerBooleanDelegationImpl(String delegation)
    {
        return getJDOStateManagerDelegationImpl(delegation,
                JavaClassWriterHelper.false_);
    }

    static String[] getOidHashCodeImpl(String[] pknames,
                                   String[] pktypes,
                                   boolean  isRoot)
    {
        final List impl = new ArrayList(3);
        if (isRoot) {
            impl.add("int hash = 0;");
        } else {
            impl.add("int hash = super.hashCode();");
        }
        for (int i = 0; i < pknames.length; i++) {
            if (isPrimitiveClass(pktypes[i])) {
                if (pktypes[i].equals("boolean")) {
                    impl.add("hash += (" + pknames[i] + " ? 1 : 0);");
                } else {
                    impl.add("hash += (int)" + pknames[i] +
                            JavaClassWriterHelper.delim_);
                }
            } else {
                impl.add("hash += (this." + pknames[i]
                         + " != null ? this." + pknames[i]
                         + ".hashCode() : 0);");
            }
        }
        impl.add("return hash;");
        String[] strArr = new String[impl.size()];
        return (String[])impl.toArray(strArr);
    }

    static String[] getOidEqualsImpl(String   oidClassName,
                                 String[] pknames,
                                 String[] pktypes,
                                 String   pk,
                                 boolean  isRoot)
    {
        final List impl = new ArrayList(31);
        if (isRoot) {
            impl.add("if (" + pk + " == null || !this.getClass().equals("
                     + pk + ".getClass())) {");
        } else {
            impl.add("if (!super.equals(" + pk + ")) {");
        }
        impl.add("    return false;");
        impl.add("}");
        oidClassName = getClassName(oidClassName);
        impl.add(oidClassName + " oid = (" + oidClassName + ")" + pk +
                JavaClassWriterHelper.delim_);
        for (int i = 0; i < pknames.length; i++) {
            if (isPrimitiveClass(pktypes[i])) {
                impl.add("if (this." + pknames[i] + " != oid."
                         + pknames[i] + ") return false;");
            } else {
                impl.add("if (this." + pknames[i] + " != oid."
                         + pknames[i] + " && (this." + pknames[i]
                         + " == null || " + "!this." + pknames[i]
                         + ".equals(oid." + pknames[i]
                         + "))) return false;");
            }
        }
        impl.add("return true;");
        String[] strArr = new String[impl.size()];
        return (String[])impl.toArray(strArr);
    }


    //The following three methods are originated from NameHelper.
    /**
     * The input parameter can be a classPath with "/" or a valid
     * full className with package name.
     */
    static String normalizeClassName(String className)
    {
        if (className == null) {
            return null;
        }
        return className.replace('/', '.').replace('$', '.');
    }

    /**
     * The inner class must be represented by $ in className.
     * The input className can be a classPath with "/".
     */
    static String getPackageName(String className)
    {
        if (className == null) {
            return null;
        }
        return JavaTypeHelper.getPackageName(className.replace('/', '.'));
    }

    /**
     * The input parameter can be a classPath with "/" or a valid
     * full className with package name.
     */
    static String getClassName(String className) {
        return JavaTypeHelper.getShortClassName(normalizeClassName(className));
    }
}
