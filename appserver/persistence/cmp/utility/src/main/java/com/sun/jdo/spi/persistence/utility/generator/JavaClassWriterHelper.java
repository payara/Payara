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
 * JavaClassWriterHelper.java
 *
 * Created on December 03, 2001
 */

package com.sun.jdo.spi.persistence.utility.generator;

import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;

/*
 * This is the utility class for helper strings and type convertion.
 *
 * @author Marina Vatkina
 */
public class JavaClassWriterHelper extends JavaTypeHelper {

    public final static String javaExtension_             = ".java"; // NOI18N
    public final static String void_                      = "void"; // NOI18N
    public final static String boolean_                   = "boolean"; // NOI18N
    public final static String byte_                      = "byte"; // NOI18N
    public final static String byteArray_                 = "byte[]"; // NOI18N
    public final static String param_                     = "param"; // NOI18N
    public final static String param0_                    = "param0"; // NOI18N
    public final static String null_                      = "null"; // NOI18N
    public final static String home_                      = "home"; // NOI18N
    public final static String delim_                     = ";"; // NOI18N
    public final static String paramInitializer_          = "\"\" + "; // NOI18N
    public final static String paramSeparator_            = ", "; // NOI18N
    public final static String paramList_                 = ","; // NOI18N
    public final static String paramConcatenator_         = " + \", \" + "; // NOI18N
    public final static String space_                     = " "; // NOI18N
    public final static String none_                      = ""; // NOI18N
    public final static String escapedEmptyString_        = "\"\""; // NOI18N
    public final static String dot_                       = "."; // NOI18N
    public final static String parenleft_                 = "("; // NOI18N
    public final static String parenright_                = ")"; // NOI18N
    public final static String parenthesis_               = "()"; // NOI18N
    public final static String new_                       = "new"; // NOI18N
    public final static String endLine_                   = "\n"; // NOI18N
    public final static String true_                      = "true"; // NOI18N
    public final static String false_                     = "false"; // NOI18N
    public final static String Collection_                = "java.util.Collection"; // NOI18N
    public final static String Set_                       = "java.util.Set"; // NOI18N
    public final static String PersistenceCapable_        = "com.sun.jdo.api.persistence.support.PersistenceCapable"; // NOI18N
    public final static String brackets_                  = "[]"; // NOI18N
    public final static String get_                       = "get"; // NOI18N
    public final static String set_                       = "set"; // NOI18N
    public final static String Oid_                       = ".Oid"; // NOI18N
    public final static String Helper_                    = "_JDOHelper"; // NOI18N
    public final static String returnNull_                = "return null;"; // NOI18N
    public final static String fileName_                  = "fileName"; // NOI18N
    public final static String int_                       = "int"; // NOI18N
    public final static String String_                    = "java.lang.String"; // NOI18N
    public final static String Class_                     = "java.lang.Class"; // NOI18N
    public final static String Date_                      = "java.util.Date"; // NOI18N
    public final static String SqlDate_                   = "java.sql.Date"; // NOI18N
    public final static String SqlTime_                   = "java.sql.Time"; // NOI18N
    public final static String SqlTimestamp_              = "java.sql.Timestamp"; // NOI18N

    // This variable is used to construct both the type and method name
    // e.g. setObjectField(), so it should be kept in a short version.
    public final static String Object_                    = "Object"; // NOI18N

    public final static String[] super_                   = new String[] {"super();"}; // NOI18N

    // This String[] is used internally to replace "\t" with the element of this array
    // that represents the corresponding indentation in spaces. 
    private final static String[] indentation_              = new String[] {
        "    ", // NOI18N
        "        ", // NOI18N
        "            ", // NOI18N
        "                "}; // NOI18N

    /**
     * Converts method body into String array.
     * @param body as String with each substring separated by "\n"
     * @return method body as String array.
     */
    public static String[] getBodyAsStrings(String body) {
        StringTokenizer st = new StringTokenizer(body, endLine_);
        String[] rc = new String[st.countTokens()];
        int ii = 0;
        while(st.hasMoreTokens()) {
            String s = st.nextToken();
            int i = s.lastIndexOf('\t');
            if (i > -1)
                 rc[ii] = indentation_[i] + s.substring(i + 1);
            else
                 rc[ii] = s;

            ii++;
        }

        return rc;
    }

    /**
     * Returns java Object wrapper Class corresponding to
     * the primitive Class if the passed class represents a primitive.
     * If the parameter is of Object type, it is returned.
     * @param cls the primitive Class to find Object wrapper for.
     * @return Object type Class.
     */
    public static Class getWrapperType(Class cls) {
        Class rc = getWrapperClass(cls);
        if (rc == null) { // not a primitive
            rc = cls;
        }

        return rc;
    }

    /**
     * A helper method which generates an expression to wrap a primitive
     * datatype to its corresponding wrapper class.
     * @param exprType The class of the primitive type
     * @param expr The expression representing a primtive typevalue
     * @return A String containing the expression for wrapping the
     * primitive datatype in its corresponding wrapperclass
     */
    public static String getWrapperExpr(Class exprType, String expr) {

        StringBuffer wrapped = new StringBuffer();

        wrapped.append(new_);
        wrapped.append(space_);
        wrapped.append(getWrapperType(exprType).getName());
        wrapped.append(parenleft_);
        wrapped.append(expr);
        wrapped.append(parenright_);

        return wrapped.toString();
    }

    /** Returns the name of the method to access the value of the primitive type
     * of the wrapper class.
     * example: boolean.class is mapped to "booleanValue()".
     * @param primitiveType the class object of the primitive type.
     * @return the name of the method to access the primitive value of the wrapper
     */
    public static String getUnwrapMethodName(Class primitiveType)
    {
        return primitiveType.getName() + "Value()"; //NOI18N
    }

    /**
     * Returns name of the primitive type corresponding to
     * the Object wrapper Class passed as a parameter.
     * If the parameter is of primitive type, its name is
     * returned.
     * @param cls the Object wrapper Class to find name of
     * the primitive type for.
     * @return name of the primitive type as String.
     */
    public static String getPrimitiveType(Class cls) {
        String rc = getPrimitiveName(cls);
        if (rc == null) { // not an Object
            rc = cls.getName();
        }

        return rc;
    }

    /**
     * Returns exception type names as String[].
     * @param m the Method to identify exception types for.
     * @return exception type names as String[].
     */
    public static String[] getExceptionNames(Method m) {
        Class[] cls = m.getExceptionTypes();
        String[] rc = new String[cls.length];
        for (int ii = 0; ii < cls.length; ii++) {
            rc[ii] = cls[ii].getName();
        }
        return rc;
    }

    /**
     * Returns list of method parameter types in format
     * <code>type0[,type1[,...]]</code>
     * @param m the Method to identify list of method parameters for.
     * @return list of method parameter types as String
     */
    public static String getParameterTypesList(Method m) {
       if (m == null)
            return none_;
 
        StringBuffer buf = new StringBuffer();
        Class[] paramTypes = m.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0)
                buf.append(paramList_);
            buf.append(getTypeRepr(paramTypes[i]));
        }
        return buf.toString();

    }

    /**
     * Returns list of method parameters in format
     * <code>param0[, param1[,...]]</code>
     * @param m the Method to identify list of method parameters for.
     * @return list of method parameters as String
     */
    public static String getParametersList(Method m) {
        return getParametersListWithSeparator(m, paramSeparator_);
    }

    /**
     * Returns list of method parameters delimited by specified
     * separator
     * @param m the Method to identify list of method parameters for.
     * @param sep the separator to be used to delimit the parameter names
     * @return list of method parameters as String
     */
    public static String getParametersListWithSeparator(Method m, String sep) {
        int count = m.getParameterTypes().length;
        StringBuffer rc = new StringBuffer();

        for (int ii = 0; ii < count; ii++) {
            if (ii > 0)
                rc.append(sep);

            rc.append(param_ + ii);
        }
        return rc.toString();
    }

    /**
     * Adds fields to a class parsing the multi-String property.
     * @param prop String to use for field generation.
     * @param modifiers other field modifiers for these fields.
     * @param writer the Class writer.
     * @throws IOException if writer fails to add a field.
     */
    public static void addFields(String prop, int modifiers, JavaClassWriter writer)
                                                                 throws IOException{
        String[] v = getBodyAsStrings(prop);
        for (int i = 0; i < v.length; i++) {
            StringTokenizer st = new StringTokenizer(v[i], space_);
            String type = st.nextToken();
            String name = st.nextToken();
            StringBuffer value = new StringBuffer();
            while(st.hasMoreTokens())
               value.append(st.nextToken() + space_);

            int l = value.length();
            value.deleteCharAt(l - 1); // last space
            writer.addField(name, // name
               modifiers, // modifiers
               type, // type
               value.toString(), // value,
               null); // comments
        }
    }

    /**
     * Adds private fields to a class parsing the multi-String property.
     * @param prop String to use for field generation.
     * @param modifiers field modifiers for these fields.
     * @throws IOException if writer fails to add a field.
     */
    public static void addPrivateField(String prop, int modifiers, JavaClassWriter writer)
                                                                 throws IOException{
        addFields(prop, Modifier.PRIVATE + modifiers, writer);
    }


    /**  
     * Adds a private void no-args method to a class with this method body.
     * @param mname method name
     * @param body the method body as String[]
     * @throws IOException if writer fails to add a field.
     */
    public static void addGenericMethod(String mname,
                                 String[] body, JavaClassWriter writer)
                                 throws IOException{
        addGenericMethod(mname, void_, body, writer);
    }


    /**
     * Adds a private no-args method to a class with this method body
     * and return type.
     * @param mname method name
     * @param type return type of the method
     * @param body the method body as String[]
     * @throws IOException if writer fails to add a field.
     */
    public static void addGenericMethod(String mname, String type,
                                 String[] body, JavaClassWriter writer)
                                 throws IOException{
        addGenericMethod(mname, Modifier.PRIVATE, type, body, writer);
    }

    /**  
     * Adds a private void no-args method to a class with this method body
     * and modifiers.
     * @param mname method name
     * @param modifiers the method modifiers
     * @param body the method body as String[]
     * @throws IOException if writer fails to add a field.
     */  
    public static void addGenericMethod(String mname, int modifiers,
                                 String[] body, JavaClassWriter writer)
                                 throws IOException{
        addGenericMethod(mname, modifiers, void_, body, writer);
    }

    /**  
     * Adds a private void no-args method to a class with this method body,
     * modifiers, and return type.
     * @param mname method name
     * @param modifiers the method modifiers
     * @param type return type of the method
     * @param body the method body as String[]
     * @throws IOException if writer fails to add a field.
     */  
    public static void addGenericMethod(String mname, int modifiers,
                                 String type, String[] body, 
                                 JavaClassWriter writer)
                                 throws IOException{
        writer.addMethod(mname, // name
            modifiers, // modifiers
            type, // returnType
            null, // parameterNames
            null,// parameterTypes
            null,// exceptions
            body, // body
            null);// comments
    }

    /**
     * Adds a method to a class parsing the multi-String property.
     * @param m Method that describes one to be added to the bean.
     * @param mname method name if different from Method#getName(). This
     * will be true for finder/selector/create methods.
     * @param mtype return type of the method, that can differ from the
     * bean's local or remote interface.
     * @param body String to use for method body generation.
     * @throws IOException if writer fails to add a field.
     */
    public static void addGenericMethod(Method m, String mname,
                                 String mtype, String body,
                                 JavaClassWriter writer)
                                 throws IOException{

        Class[] types = m.getParameterTypes();
        int count = types.length;
        String[] parameterTypes = new String[count];
        String[] parameterNames = new String[count];
        for (int ii = 0; ii < count; ii++) {
            parameterTypes[ii] = getTypeRepr(types[ii]);
            parameterNames[ii] = param_ + ii;
        }

        String[] exceptionTypes = getExceptionNames(m);

        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers))
            modifiers -= Modifier.ABSTRACT;

        writer.addMethod(mname, // name
            modifiers, // modifiers
            mtype, // returnType
            parameterNames, // parameterNames
            parameterTypes,// parameterTypes
            exceptionTypes,// exceptions
            getBodyAsStrings(body), // body
            null);// comments
    }
 
    /**
     * Returns the String representation of the specified class instance.
     * An array is represented as the name of the element type followed by [].
     */  
    public static String getTypeRepr(Class clazz)
    {
        return (clazz.isArray() ?
                getTypeRepr(clazz.getComponentType()) + brackets_ :
                clazz.getName());
    }

}
