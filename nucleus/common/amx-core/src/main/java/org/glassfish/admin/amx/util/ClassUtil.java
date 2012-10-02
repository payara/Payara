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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

/*
Used internally.
 */
final class ClassToClassMapping
{
    final Class mSrc;

    final Class mDest;

    public ClassToClassMapping(Class src, Class dest)
    {
        mSrc = src;
        mDest = dest;
    }

}

/**
Provides a variety of useful utilities having to do with classes, types of
objects, etc.
 */
public final class ClassUtil
{
    private ClassUtil()
    {
        // disallow instantiation
    }

    public static boolean classIsAccessible(String name)
    {
        boolean accessible = false;

        try
        {
            accessible = getClassFromName(name) != null;
        }
        catch (ClassNotFoundException e)
        {
        }
        return (accessible);
    }

    public static boolean sigsEqual(
            final Class[] sig1,
            final Class[] sig2)
    {
        boolean equal = sig1.length == sig2.length;

        if (equal)
        {
            for (int i = 0; i < sig1.length; ++i)
            {
                if (sig1[i] != sig2[i])
                {
                    equal = false;
                    break;
                }
            }
        }

        return (equal);
    }

    /**
    Test whether an Object is an array

    @param o	object to test
    @return	true if the object is an array, false otherwise.
     */
    public static boolean objectIsArray(Object o)
    {
        return (classIsArray(o.getClass()));
    }

    /**
    Test whether a Class is an array

    @param theClass		class to test
    @return			true if the class is an array, false otherwise.
     */
    public static boolean classIsArray(Class theClass)
    {
        return (classnameIsArray(theClass.getName()));
    }

    /**
    Test whether an Object is an array of primitive types

    @param o		object to test
    @return		true if the object is an array, false otherwise.
     */
    public static boolean objectIsPrimitiveArray(Object o)
    {
        return (getPrimitiveArrayTypeCode(o.getClass()) != 0);
    }

    /**
    Test whether a classname is an array

    @param classname	classname string
    @return			true if the object is an array, false otherwise.
     */
    public static boolean classnameIsArray(String classname)
    {
        return (classname.startsWith("["));
    }

    /**
    Strip the package name.

    @param classname	classname string
    @return	the classname, without its package
     */
    public static String stripPackageName(String classname)
    {
        final int lastDot = classname.lastIndexOf(".");
        if (lastDot < 0)
        {
            return (classname);
        }

        return (classname.substring(lastDot + 1, classname.length()));
    }

    /**
    Test whether a classname is a primitive array

    @param classname	classname string
    @return			true if the object is a primitive array, false otherwise.
     */
    public static boolean classnameIsPrimitiveArray(String classname)
    {
        return (getPrimitiveArrayTypeCode(classname) != 0);
    }

    /**
    Return the primitive element type code for an array of primitive types.
    Same as getPrimitiveArrayTypeCode( theClass.getName() )

    @param theClass		the Class object
    @return				the element type code; otherwise (char)0
     */
    public static char getPrimitiveArrayTypeCode(Class theClass)
    {
        char typeCode = 0;

        if (classIsArray(theClass))
        {
            typeCode = getPrimitiveArrayTypeCode(theClass.getName());
        }

        return (typeCode);
    }

    /**
    Return the primitive element type code for an array of primitive types.

    @param classname	classname string
    @return			the element type code; otherwise (char)0
     */
    public static char getPrimitiveArrayTypeCode(String classname)
    {
        char typeCode = 0;

        final int length = classname.length();

        if (classnameIsArray(classname) &&
            classname.charAt(length - 2) == '[')
        {
            typeCode = classname.charAt(length - 1);

            switch (typeCode)
            {
                default:
                    typeCode = 0;
                    break;

                case 'Z':
                case 'B':
                case 'C':
                case 'S':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                    break;
            }
        }

        return (typeCode);
    }

    /**
    Get the classname for an array element.

    @param classname	classname string
    @return			the classname for the array element
     */
    public static String getArrayMemberClassName(String classname)
    {
        String result;

        if (!classnameIsArray(classname))
        {
            throw new IllegalArgumentException("not an array");
        }

        final int classnameLength = classname.length();


        if (classnameIsPrimitiveArray(classname))
        {
            final char lastChar = classname.charAt(classnameLength - 1);

            switch (lastChar)
            {
                default:
                    throw new RuntimeException("illegal primitive");

                // a simple type
                case 'Z':
                    result = "boolean";
                    break;
                case 'B':
                    result = "byte";
                    break;
                case 'C':
                    result = "char";
                    break;
                case 'S':
                    result = "short";
                    break;
                case 'I':
                    result = "int";
                    break;
                case 'J':
                    result = "long";
                    break;
                case 'F':
                    result = "float";
                    break;
                case 'D':
                    result = "double";
                    break;
            }
        }
        else
        {
            // strip leading "[L" and trailing ";"
            result = classname.substring(2, classnameLength - 1);
        }

        return (result);
    }

    /**
    Class.forName does not work for primitive types, so we need to do it ourselves here.
     */
    final static class ClassNameToClassMapping
    {
        String mName;

        Class mClass;

        ClassNameToClassMapping(String name, Class theClass)
        {
            mName = name;
            mClass = theClass;
        }

    }
    private static final ClassNameToClassMapping[] sPrimitiveNameToObjectClass =
            new ClassNameToClassMapping[]
    {
        new ClassNameToClassMapping("int", int.class),
        new ClassNameToClassMapping("long", long.class),
        new ClassNameToClassMapping("short", short.class),
        new ClassNameToClassMapping("byte", byte.class),
        new ClassNameToClassMapping("boolean", boolean.class),
        new ClassNameToClassMapping("float", float.class),
        new ClassNameToClassMapping("double", double.class),
        new ClassNameToClassMapping("char", char.class),
        new ClassNameToClassMapping("void", void.class),
    };

    public static Class<?> classForName(String name)
            throws ClassNotFoundException
    {
        Class<?> c;

        try
        {
            c = Class.forName(name);
        }
        catch (ClassNotFoundException e)
        {
            c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        }
        catch (NoClassDefFoundError e)
        {
            c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        }

        return (c);
    }

    /**
    Get a Class from a classname.  Class.forName does not work for primitive types;
    this methods returns the correct Class for any type.

    @param classname	classname string
    @return			the classname for the array element
     */
    public static Class<?> getClassFromName(final String classname)
            throws ClassNotFoundException
    {
        Class<?> theClass = null;

        if (classname.startsWith("[L"))
        {
            // an array
            theClass = classForName(classname);
        }
        else
        {
            final int numMappings = Array.getLength(sPrimitiveNameToObjectClass);
            for (int i = 0; i < numMappings; ++i)
            {
                if (sPrimitiveNameToObjectClass[i].mName.equals(classname))
                {
                    theClass = sPrimitiveNameToObjectClass[i].mClass;
                    break;
                }
            }

            if (theClass == null)
            {
                theClass = classForName(classname);
            }
        }
        return (theClass);
    }

    private static final ClassToClassMapping[] sPrimitiveClassToObjectClass =
            new ClassToClassMapping[]
    {
        new ClassToClassMapping(boolean.class, Boolean.class),
        new ClassToClassMapping(byte.class, Byte.class),
        new ClassToClassMapping(char.class, Character.class),
        new ClassToClassMapping(short.class, Short.class),
        new ClassToClassMapping(int.class, Integer.class),
        new ClassToClassMapping(long.class, Long.class),
        new ClassToClassMapping(float.class, Float.class),
        new ClassToClassMapping(double.class, Double.class),
    };

    /**
    Map primitive class Classes to Object forms eg int.class to Integer.class

    @param		theClass	the class to map
    @return	the corresponding Object class or the original Class if not a primitive.
     */
    public static Class primitiveClassToObjectClass(final Class theClass)
    {
        Class result = theClass;

        final int numMappings = Array.getLength(sPrimitiveClassToObjectClass);
        for (int i = 0; i < numMappings; ++i)
        {
            final ClassToClassMapping mapping = sPrimitiveClassToObjectClass[i];

            if (mapping.mSrc == theClass)
            {
                result = mapping.mDest;
                break;
            }
        }

        return (result);
    }

    /**
    Map primitive class Classes to Object forms eg int.class to Integer.class

    @param		theClass	the class to map
    @return	the corresponding Object class or the original Class if not a primitive.
     */
    public static Class objectClassToPrimitiveClass(final Class theClass)
    {
        Class result = theClass;

        final int numMappings = Array.getLength(sPrimitiveClassToObjectClass);
        for (int i = 0; i < numMappings; ++i)
        {
            final ClassToClassMapping mapping = sPrimitiveClassToObjectClass[i];

            if (mapping.mDest == theClass)
            {
                result = mapping.mSrc;
                break;
            }
        }

        return (result);
    }

    /**
    Test whether a class is a primitive class.

    @param		theClass	the class to test
    @return	true if it's a primitive class, false otherwise.
     */
    public static boolean isPrimitiveClass(final Class theClass)
    {
        boolean isSimple = false;

        final int numMappings = Array.getLength(sPrimitiveClassToObjectClass);
        for (int i = 0; i < numMappings; ++i)
        {
            final ClassToClassMapping mapping = sPrimitiveClassToObjectClass[i];

            if (mapping.mSrc.equals(theClass))
            {
                isSimple = true;
                break;
            }
        }

        return (isSimple);
    }

    /**
    Convert a primitive class letter to its corresponding class name.

    @param		primitive	the primitive character code
    @return		the corresponding classname
     */
    public static String primitiveLetterToClassName(final char primitive)
    {
        String result;

        // see JavaDoc on Class.getName()
        switch (primitive)
        {
            case 'B':
                result = "byte";
                break;
            case 'C':
                result = "char";
                break;
            case 'D':
                result = "double";
                break;
            case 'F':
                result = "float";
                break;
            case 'I':
                result = "int";
                break;
            case 'J':
                result = "long";
                break;
            case 'S':
                result = "short";
                break;
            case 'Z':
                result = "boolean";
                break;
            default:
                result = "" + primitive;
                break;
        }

        return result;
    }

    /**
    Return the corresponding classes for each element in an Object[]

    If an element is null, then its corresponding Class will also be null.

    @param		args	an array of objects.
    @return		an array of classes
     */
    public static String[] getTypes(final Object[] args)
    {
        if (args == null)
        {
            return (null);
        }

        final int numArgs = Array.getLength(args);

        final String[] types = new String[numArgs];

        for (int i = 0; i < numArgs; ++i)
        {
            if (args[i] == null)
            {
                types[i] = null;
            }
            else
            {
                types[i] = args[i].getClass().getName();
            }
        }

        return (types);
    }

    /**
    Return the corresponding classes for each classname.

    If an element is null, then its corresponding Class will also be null.

    @param		classnames	an array of classnames.
    @return		an array of classes
     */
    public static Class[] signatureFromClassnames(String[] classnames)
            throws ClassNotFoundException
    {
        if (classnames == null)
        {
            return (null);
        }

        final Class[] signature = new Class[classnames.length];

        for (int i = 0; i < signature.length; ++i)
        {
            signature[i] = getClassFromName(classnames[i]);
        }

        return (signature);
    }

    /**
    Return the corresponding classes for each classname.

    If an element is null, then its corresponding Class will also be null.

    @param		classes	an array of classnames.
    @return		an array of classes
     */
    public static String[] classnamesFromSignature(Class[] classes)
    {
        final String[] classnames = new String[classes.length];

        for (int i = 0; i < classnames.length; ++i)
        {
            classnames[i] = classes[i].getName();
        }

        return (classnames);
    }

    /**
    Get a "friendly" classname for a Class.
    <p>
    Calls getFriendlyClassname( theClass.getName() )

    @param		theClass	the class for which the name should be gotten
    @return		the "friendly" name
     */
    public static String getFriendlyClassname(Class theClass)
    {
        return (getFriendlyClassname(theClass.getName()));
    }

    /**
    Convert a Java class name string into a more user friendly string. Examples:
    <p>
    java.lang.String		=> String
    java.lang.<type>		=> <type>;
    [i						=> int[]
    [Lfoo.bar.ClassName;	=> foo.bar.ClassName[]
    <p>
    The names returned correspond exactly to what a Java programmer would write, rather
    than the internal JVM representation.

    @param 		type
    @return	a friendlier string representing the type
     */
    final static String javaLang = "java.lang.";

    public static String getFriendlyClassname(String type)
    {
        String result = type;

        if (type.startsWith("["))
        {
            // count how deep the array is
            int depth = 0;
            while (type.charAt(depth) == (int) '[')
            {
                ++depth;
            }

            // strip all the '[' characters
            result = type.substring(depth, type.length());

            if (result.startsWith("L") && result.endsWith(";"))
            {
                result = result.substring(1, result.length() - 1);
            }
            else if (result.length() == 1)
            {
                // a simple type
                switch (result.charAt(0))
                {
                    case 'Z':
                        result = "boolean";
                        break;
                    case 'B':
                        result = "byte";
                        break;
                    case 'C':
                        result = "char";
                        break;
                    case 'S':
                        result = "short";
                        break;
                    case 'I':
                        result = "int";
                        break;
                    case 'J':
                        result = "long";
                        break;
                    case 'F':
                        result = "float";
                        break;
                    case 'D':
                        result = "double";
                        break;
                    default:
                        result = "unknown";
                        break;
                }
            }

            StringBuilder sb = new StringBuilder(result);
            for (int i = 0; i < depth; ++i)
            {
                sb.append("[]");
            }
            result = sb.toString();
        }

        if (result.startsWith(javaLang))
        {
            result = result.substring(javaLang.length(), result.length());
        }

        return (result);
    }

    /*
     */
    public static int getArrayDimensions(final Class theClass)
    {
        final String classname = theClass.getName();

        int dim = 0;
        while (classname.charAt(dim) == '[')
        {
            ++dim;
        }

        return (dim);
    }

    /*
     */
    public static Class getArrayElementClass(final Class arrayClass)
    {
        final String arrayClassName = arrayClass.getName();

        if (!classnameIsArray(arrayClassName))
        {
            throw new IllegalArgumentException("not an array");
        }

        String name = arrayClassName;

        // strip leading "["
        name = name.substring(1, name.length());

        if (!name.startsWith("["))
        {
            // element is not an array

            if (name.startsWith("L"))
            {
                // an Object class; strip leading "L" and trailing ";"
                name = name.substring(1, name.length() - 1);
            }
            else if (name.length() == 1)
            {
                // may be a primitive type
                name = primitiveLetterToClassName(name.charAt(0));
            }
        }
        else
        {
            // element is an array; return it
        }

        Class theClass = null;
        try
        {
            theClass = getClassFromName(name);
        }
        catch (ClassNotFoundException e)
        {
            assert (false);
        }

        return (theClass);
    }

    public static Class getInnerArrayElementClass(final Class arrayClass)
            throws ClassNotFoundException
    {
        Class elementClass = arrayClass;

        do
        {
            elementClass = getArrayElementClass(elementClass);
        }
        while (classIsArray(elementClass));

        return (elementClass);
    }

    private static Object instantiateObject(final String theString)
            throws Exception
    {
        Object result;

        try
        {
            result = instantiateNumber(theString);
        }
        catch (NumberFormatException e)
        {
            result = theString;
        }

        return (result);
    }

    /**
    Return true if caller signature is compatible with callee.

    @param callee			the signature of the method to be called
    @param argsSignature	the signature of the argument list
     */
    public static boolean signaturesAreCompatible(final Class<?>[] callee, final Class<?>[] argsSignature)
    {
        boolean compatible = false;

        if (callee.length == argsSignature.length)
        {
            compatible = true;

            for (int i = 0; i < callee.length; ++i)
            {
                if (!callee[i].isAssignableFrom(argsSignature[i]))
                {
                    compatible = false;
                    break;
                }
            }
        }

        return (compatible);
    }

    /**
    Find all methods that match the name.
     */
    public static Method findMethod(
            final Class<?> theClass,
            final String methodName,
            final Class<?>[] sig)
    {
        Method m = null;
        try
        {
            m = theClass.getMethod(methodName, sig);
        }
        catch (NoSuchMethodException e)
        {
            // ok, doesn't exist
        }

        return (m);
    }

    /**
    Find all methods that match the name.
     */
    public static Set<Method> findMethods(
            final Method[] candidates,
            final String methodName)
    {
        final Set<Method> s = new HashSet<Method>();

        for (int methodIdx = 0; methodIdx < candidates.length; ++methodIdx)
        {
            final Method method = candidates[methodIdx];
            if (method.getName().equals(methodName))
            {
                s.add(method);
            }
        }

        return (s);
    }

    /**
    Create a new object of the specified class using a constructor
    that accepts the specified arguments.

    @param theClass	the Class of the desired Object
    @param args		the argument list for the constructor
     */
    public static <T> T instantiateObject(final Class<T> theClass, final Object[] args)
            throws Exception
    {
        final Class[] signature = new Class[args.length];

        for (int i = 0; i < signature.length; ++i)
        {
            signature[i] = args[i].getClass();
        }

        Constructor<T> constructor = null;
        try
        {
            // this will fail if a constructor takes an interface;
            // the code below will then find a compatible constructor
            constructor = theClass.getConstructor(signature);
        }
        catch (NoSuchMethodException e)
        {
            final Constructor<T>[] constructors = TypeCast.asArray(theClass.getConstructors());

            int numMatches = 0;
            for (int i = 0; i < constructors.length; ++i)
            {
                final Constructor<T> tempConstructor = constructors[i];

                final Class[] tempSignature = tempConstructor.getParameterTypes();

                if (signaturesAreCompatible(tempSignature, signature))
                {
                    ++numMatches;
                    constructor = tempConstructor;
                    // keep going; there could be more than one valid match
                }
            }

            // to succeed, there must be exactly one match
            if (numMatches != 1)
            {
                throw e;
            }
        }

        T result = null;
        try
        {
            result = constructor.newInstance(args);
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            // InvocationTargetException wraps the real cause
            final Throwable cause = e.getCause();

            if (cause instanceof Exception)
            {
                throw (Exception) cause;
            }
            else
            {
                // shouldn't happen, so we'll just rethrow it
                throw e;
            }
        }

        return (result);
    }

    /**
    Create a new object of the specified class using a String constructor.

    @param theClass		the Class of the desired Object
    @param theString	the string for a String constructor
    @return the resulting Object
     */
    public static <T> T instantiateObject(final Class<T> theClass, final String theString)
            throws Exception
    {
        final Class[] signature = new Class[]
        {
            String.class
        };
        final Constructor<T> constructor = theClass.getConstructor(signature);

        T result = null;
        try
        {
            result = constructor.newInstance(new Object[]
                    {
                        theString
                    });
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            // InvocationTargetException wraps the real cause
            Throwable cause = e.getCause();

            if (cause instanceof Exception)
            {
                throw (Exception) cause;
            }
            else
            {
                // shouldn't happen, so we'll just rethrow it
                throw e;
            }
        }

        return (result);
    }

    /**
    Create a new number based on a String.
    <p>
    Don't get fancy here, simple precedence:
    Integer, Long	 if no decimal point, use Long if won't fit in an Integer
    Double			 if decimal point (for maximum precision)

    @param theString	String representation of the number
    @return the resulting Object
     */
    private static Object instantiateNumber(final String theString)
            throws Exception
    {
        Object result;

        if (theString.indexOf('.') >= 0)
        {
            result = instantiateObject(Double.class, theString);
        }
        else
        {
            try
            {
                result = instantiateObject(Integer.class, theString);
            }
            catch (NumberFormatException e)
            {
                // perhaps it wouldn't fit; try it as a long
                result = instantiateObject(Long.class, theString);
            }
        }
        return result;
    }

    /**
    Given a Class and a String, create a new instance with a constructor that accept
    a String. Primitive types are instantiated as their equivalent Object forms.

    @param theClass		the class from which an instance should be instantiated
    @param theString	the string to be supplied to the constructor
    @return the resulting Object
     */
    public static Object instantiateFromString(final Class<?> theClass, final String theString)
            throws Exception
    {
        Object result;

        // char and Character do not have a String constructor, so we must special-case it
        if (theClass == Object.class)
        {
            // special case, apply rules to create an object
            result = instantiateObject(theString);
        }
        else if (theClass == Number.class)
        {
            // special case, apply rules to create a number
            result = instantiateNumber(theString);
        }
        else if (theClass == Character.class || theClass == char.class)
        {
            if (theString.length() != 1)
            {
                throw new IllegalArgumentException("not a character: " + theString);
            }

            result = Character.valueOf(theString.charAt(0));
        }
        else
        {
            final Class<?> objectClass = primitiveClassToObjectClass(theClass);

            result = instantiateObject(objectClass, theString);
        }

        return result;
    }

    /**
    Given a Class, create a new instance with an empty constructor.
    Primitive types are instantiated as their equivalent Object forms.
    Any value is acceptable in the newly created object.

    @param inClass		the class from which an instance should be instantiated
    @return the resulting Object
     */
    public static Object instantiateDefault(final Class<?> inClass)
            throws Exception
    {
        Object result;

        final Class<?> objectClass = primitiveClassToObjectClass(inClass);

        if (Number.class.isAssignableFrom(objectClass))
        {
            result = instantiateFromString(objectClass, "0");
        }
        else if (objectClass == Boolean.class)
        {
            result = Boolean.TRUE;
        }
        else if (objectClass == Character.class)
        {
            result = Character.valueOf('X');
        }
        else if (classIsArray(objectClass))
        {
            result = Array.newInstance(objectClass, 0);
        }
        else if (objectClass == Object.class)
        {
            result = "anyObject";
        }
        else if (objectClass == String.class)
        {
            result = "";
        }
        else if (objectClass == java.net.URL.class)
        {
            result = new java.net.URL("http://www.sun.com");
        }
        else if (objectClass == java.net.URI.class)
        {
            result = new java.net.URI("http://www.sun.com");
        }
        else if (classIsArray(inClass))
        {
            final int dimensions = 3;
            result = Array.newInstance(getInnerArrayElementClass(inClass), dimensions);
        }
        else
        {
            result = objectClass.newInstance();
            //result	= InstantiateFromString( objectClass, "0" );
        }
        return result;
    }

    final static String[] sJavaLangTypes =
    {
        "Character", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "String", "Object"
    };

    final static int sNumBaseTypes = Array.getLength(sJavaLangTypes);

    /**
    Expand an abbreviated classname into its true java name.

    Turn "Integer" into "java.lang.Integer", etc.
     */
    public static String expandClassName(final String name)
    {
        String fullName = name;

        final int numTypes = sNumBaseTypes;
        for (int i = 0; i < numTypes; ++i)
        {
            if (name.equals(sJavaLangTypes[i]))
            {
                fullName = "java.lang." + name;
                break;
            }
        }

        if (fullName == name)	// no match so far
        {
            if (name.equals("Number"))
            {
                fullName = "java.lang." + name;
            }
            else if (name.equals("BigDecimal") || name.equals("BigInteger"))
            {
                fullName = "java.math." + name;
            }
            else if (name.equals("URL") || name.equals("URI"))
            {
                fullName = "java.net." + name;
            }
            else if (name.equals("Date"))
            {
                fullName = "java.util." + name;
            }
            else if (name.equals("ObjectName"))
            {
                fullName = "javax.management." + name;
            }

        }

        return fullName;
    }

    /**
    Convert inner element to another type.  Only works for arrays of Objects.  Example:

    convertArrayClass( "[[[LObject;", "Long" ) =>[[[LLong;

    @param arrayClass
    @param newInnerType		the desired Class of the innermost element
     */
    public static Class convertArrayClass(final Class arrayClass, final Class newInnerType)
            throws ClassNotFoundException
    {
        final String arrayClassname = arrayClass.getName();
        if (!arrayClassname.endsWith(";"))
        {
            throw new IllegalArgumentException("not an array of Object");
        }

        final int innerNameBegin = 1 + arrayClassname.indexOf("L");

        final String newClassName = arrayClassname.substring(0, innerNameBegin) + newInnerType.getName() + ";";

        final Class newClass = getClassFromName(newClassName);

        return (newClass);
    }

    private static class MyClassLoader extends ClassLoader
    {
        MyClassLoader()
        {
            this(Thread.currentThread().getContextClassLoader());
        }

        MyClassLoader(ClassLoader cl)
        {
            super(cl);
        }

        @Override
        public Package[] getPackages()
        {
            return (super.getPackages());
        }

    }

    public static Package[] getPackages()
    {
        return AccessController.doPrivileged(new PrivilegedAction<MyClassLoader>() {
            @Override
            public MyClassLoader run() {
                return new MyClassLoader();
            }
        }).getPackages();
    }

    public static Package[] getPackages(final ClassLoader cl)
    {
        return AccessController.doPrivileged(new PrivilegedAction<MyClassLoader>() {
            @Override
            public MyClassLoader run() {
                return new MyClassLoader(cl);
            }
        }).getPackages();
    }

    /**
     */
    public static Object getFieldValue(
            final Class<?> theInterface,
            final String name)
    {
        Object value;

        try
        {
            final Field field = theInterface.getField(name);
            value = field.get(theInterface);
        }
        catch (Exception e)
        {
            value = null;
        }

        return value;
    }

    public static String stripPackagePrefix(final String classname)
    {
        final int index = classname.lastIndexOf(".");

        String result = classname;
        if (index > 0)
        {
            result = classname.substring(index + 1, classname.length());
        }

        return result;
    }

    ;

    public static String getPackagePrefix(final String classname)
    {
        final int index = classname.lastIndexOf(".");

        String result = classname;
        if (index > 0)
        {
            result = classname.substring(0, index);
        }

        return result;
    }

    ;
}







