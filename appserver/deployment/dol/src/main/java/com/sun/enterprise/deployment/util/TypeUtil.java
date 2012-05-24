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

package com.sun.enterprise.deployment.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Datatype management utility methods
 */
public class TypeUtil {

    // map a decimal digit to its (real!) character
    private static final char[] digits = { 
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9' 
    };

    // Map of primitive class name and its associated Class object
    private static Hashtable primitiveClasses_;

    static {
        primitiveClasses_ = new Hashtable();
        primitiveClasses_.put(Character.TYPE.getName(), Character.TYPE);
        primitiveClasses_.put(Boolean.TYPE.getName(), Boolean.TYPE);
        primitiveClasses_.put(Byte.TYPE.getName(), Byte.TYPE);
        primitiveClasses_.put(Integer.TYPE.getName(), Integer.TYPE);
        primitiveClasses_.put(Long.TYPE.getName(), Long.TYPE);
        primitiveClasses_.put(Short.TYPE.getName(), Short.TYPE);
        primitiveClasses_.put(Float.TYPE.getName(), Float.TYPE);
        primitiveClasses_.put(Double.TYPE.getName(), Double.TYPE);
    }

    /** 
     * Place a character representation of src into the buffer.
     * No formatting (e.g. localization) is done.
     *
     * @param src - the integer to convert.  Must not be Integer.MIN_VALUE.
     * @param buf - the buf to put the result in
     * @param offset - the offset in buf to place the first digit
     * @return the number of bytes added to buf
     * @exception IllegalArgumentException if src is Integer.MIN_VALUE.
     */
    public static int intGetChars(
	int src,
	char buf[],
	int offset
    ) {
	int power = 1000000000;  // magnitude of highest digit this can handle
	int this_digit;
	boolean have_emitted = false;
	int init_offset = offset;

	// special case src is zero
	if (src == 0) {
	    buf[offset] = digits[0];
	    return 1;
	}
	else if (src < 0) {
	    if (src == Integer.MIN_VALUE)
		throw new IllegalArgumentException();
	    
	    // emit the negation sign and continue as if positive
	    buf[offset++] = '-';
	    src = Math.abs(src);
	}

	// iterate until there are no more digits to emit
	while (power > 0) {
	    this_digit = src / power;
	    if (this_digit != 0 || have_emitted) {
		// emit this digit
		have_emitted = true;
		buf[offset++] = digits[this_digit];
	    }
	    src = src % power;
	    power = power / 10;
	}
	return offset - init_offset;
    }


    // map a digit to its single byte character
    private static final byte[] charval = { 
	(byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5',(byte) '6',(byte) '7',(byte) '8',(byte) '9' 
    };

    /** 
     * Place a byte representation of src into the byte array buf.
     * No commas or any other formatting is done to the integer.
     * @param src - the integer to convert.  Must not be Integer.MIN_VALUE.
     * @param buf - the buf to put the result in
     * @param offset - the offset in buf to place the first digit
     * @return the number of bytes added to buf
     * @exception IllegalArgumentException if src is Integer.MIN_VALUE.
     */
    public static int intGetBytes(
	int src,
	byte buf[],
	int offset
    ) {
	int power = 1000000000;  // magnitude of highest digit this can handle
	int this_digit;
	boolean have_emitted = false;
	int init_offset = offset;

	// special case src is zero
	if (src == 0) {
	    buf[offset] = charval[0];
	    return 1;
	}
	else if (src < 0) {
	    if (src == Integer.MIN_VALUE)
		throw new IllegalArgumentException();
	    
	    // emit the negation sign and continue as if positive
	    buf[offset++] = (byte) '-';
	    src = Math.abs(src);
	}

	// iterate until there are no more digits to emit
	while (power > 0) {
	    this_digit = src / power;
	    if (this_digit != 0 || have_emitted) {
		// emit this digit
		have_emitted = true;
		buf[offset++] = charval[this_digit];
	    }
	    src = src % power;
	    power = power / 10;
	}
	return offset - init_offset;
    }


    /**
     * Work around a performance bug in String.hashCode() for strings longer
     * than sixteen characters, by calculating a (slower) hash on all the
     * characters in the string.  Not needed starting in the JDK 1.2 release.
     */
    public static int hashCode(String s)
    {
	int length = s.length();
	int h = 1;

	for (int i = 0; i < length; i++)
	    h = (h * 37) + (int) s.charAt(i);
	return h;
    }


    /**
     * Word-wrap a string into an array of strings.  Space is the only
     * separator character recognized.
     */
    public static String[] wordWrap(String msg, int widthInChars) {
	int width = widthInChars;
	int nextBreak =0;
	int lastBreak = 0;
	int length = msg.length();
	int lengthLeft = length;
	boolean breakFound = true;
	Vector v = new Vector();
	int nextNewline = msg.indexOf("\n");
	    
	while (lengthLeft > width || nextNewline != -1) {
	    // Find a convenient word break, always respecting explicit line
	    // breaks.
	    nextBreak = nextNewline;

	    // If no newline, look for a space.
	    if (nextBreak == -1 || 
		nextBreak <= lastBreak ||
		nextBreak > lastBreak + width) {
		nextBreak = msg.lastIndexOf(" ", lastBreak + width);
	    }

	    // No space, break it at the wrap width.
	    if (nextBreak == -1 || nextBreak <= lastBreak) {
		nextBreak = lastBreak + width - 1;
		breakFound = false;
		if (nextBreak > length) {
		    break;
		}
	    }

	    // Save the substring and adjust indexes.
	    String substr = msg.substring(lastBreak, nextBreak);
	    v.addElement(substr);
	    lengthLeft -= substr.length();

	    lastBreak = nextBreak;
	    if (breakFound) {
		++lastBreak;
	    }
	    breakFound = true;
	    nextNewline = msg.indexOf("\n", lastBreak);
	}

	v.addElement(msg.substring(lastBreak));
	String[] lines = new String[v.size()];
	v.copyInto(lines);
	return lines;
    }


    /**
     * Convert an array of strings to a single line with elements separated
     * by the given separator. Similar to Tcl's <code>join</code>.
     * @param from the array of strings to convert
     * @param separator the string to insert between each element
     */
    public static String arrayToString(String[] from, String separator) {
	StringBuffer sb = new StringBuffer(100);
	String sep = "";
	for (int i = 0; i < from.length; i++) {
	   sb.append(sep);
	   sb.append(from[i]);
	   sep = separator;
	}
	return sb.toString();
    }


    /**
     * Convert a string of delimited strings to an array of strings.
     * Similar to AWK's and Tcl's <code>split</code>.
     * @param from the string to convert
     * @param separator the delimiter
     */
    public static String[] stringToArray(String from, String separator) {
	if (from == null) {
	    return null;
	}
	if (separator == null) {
	    separator = " ";
	}
	StringTokenizer toks = new StringTokenizer(from, separator);
	String[] result = new String[toks.countTokens()];
	int i = 0;
	while (toks.hasMoreTokens()) {
	    result[i++] = toks.nextToken().trim();
	}
	return result;
    }


    /**
     * Truncate a float to the required number of significant digits.
     */
    public static String truncateFloat(float f, int digits) {
	double factor = Math.pow(10, digits);
	f = (float)(Math.round(f * factor) / factor);
	return Float.toString(f);
    }


    /**
     * Add commas to a number for "123,456.7" style formatting.
     * @deprecated Use standard java.* APIs which create the correct
     *	localized number format.
     */
    public static String addCommas(float f) {
	String floatStr = truncateFloat(f, 0);
	return addCommas(floatStr);
    }


    /**
     * Add commas to a number for "123,456.7" style formatting.
     * @deprecated Use standard java.* APIs which create the correct
     *	localized number format.
     */
    public static String addCommas(String numStr) {
	int dotIndex = numStr.lastIndexOf('.');
	String n;

	String fraction = "";
	if (dotIndex >= 0) {
	    fraction = numStr.substring(dotIndex);
	    n = numStr.substring(0, dotIndex);
	} else {
	    n = numStr;
	}

	String val = "";
	int lastIndex = 0;
	for (int i = n.length(); i > 0; i -= 3) {
	    String comma;
	    if (i > 3) {
		comma = ",";
	    } else {
		comma = "";
	    }
	    int start = Math.max(i - 3, 0);
	    val = comma + n.substring(start, i) + val;
	    lastIndex = start;
	}
	val = n.substring(0, lastIndex) + val + fraction;
	return val;
    }


    /**
     * Test if a class is a subclass of another.
     * @deprecated Use <em>sup.isAssignableFrom(sub)</em>
     */
    public static boolean isSubclassOf(Class sub, Class sup) {
	if (sub == sup) {
	    return true;
	}
	Class superclass = sub.getSuperclass();
	while (superclass != null && superclass != sup) {
	    superclass = superclass.getSuperclass();
	}
	return (superclass != null);
    }

    /**
     * Get all super-interfaces of a class, excluding the 
     * given base interface.
     * Returns a set of strings containing class names.
     */
    public static Set getSuperInterfaces(ClassLoader cl, String className, String baseClassName) throws ClassNotFoundException {
        Set allSuper = new HashSet();
        if( !className.equals(baseClassName) ) {
            Class theClass          = cl.loadClass(className);
            Class[] superInterfaces = theClass.getInterfaces();

            for(int superIndex = 0; superIndex < superInterfaces.length; superIndex++) {
                Class currentClass      = superInterfaces[superIndex];
                String currentClassName = currentClass.getName();
                if( !currentClassName.equals(baseClassName) ) {
                    allSuper.add(currentClassName);
                    allSuper.addAll(getSuperInterfaces(cl, currentClassName, baseClassName));
                }
            } // End for -- each super interface
        }
        return allSuper;
    }

    public static Method getMethod(Class declaringClass, ClassLoader loader,
                                   String name, String[] paramClassNames)
       throws Exception
    {

        Class[] parameterTypes=null;
        if (paramClassNames!=null) {       
            parameterTypes = new Class[paramClassNames.length];
            for(int pIndex = 0; pIndex < parameterTypes.length; pIndex++) {
                String next = paramClassNames[pIndex];
                if( primitiveClasses_.containsKey(next) ) {
                    parameterTypes[pIndex] = 
                        (Class) primitiveClasses_.get(next);
                } else {
                    parameterTypes[pIndex] = Class.forName(next, true, loader);
                }
            }
        }
        return declaringClass.getMethod(name, parameterTypes);
    }

    public static Method getDeclaredMethod(Class declaringClass, ClassLoader loader,
                                   String name, String[] paramClassNames)
       throws Exception
    {

        Class[] parameterTypes=null;
        if (paramClassNames!=null) {       
            parameterTypes = new Class[paramClassNames.length];
            for(int pIndex = 0; pIndex < parameterTypes.length; pIndex++) {
                String next = paramClassNames[pIndex];
                if( primitiveClasses_.containsKey(next) ) {
                    parameterTypes[pIndex] = 
                        (Class) primitiveClasses_.get(next);
                } else {
                    parameterTypes[pIndex] = Class.forName(next, true, loader);
                }
            }
        }
        return declaringClass.getDeclaredMethod(name, parameterTypes);
    }

    /**
     * Compares the signatures of two methods to see if they
     * have the same numer of parameters and same parameter types.
     *
     * Note that this equality check does NOT cover :
     * 1) declaring class 2) exceptions 3) method name 
     * 4) return type
     *
     */
    public static boolean sameParamTypes(Method m1, Method m2) {
        boolean same = false;

        Type[] gpm1 = m1.getGenericParameterTypes();
        Type[] gpm2 = m2.getGenericParameterTypes();

        if ((gpm1.length == gpm2.length)) {
            same = true;
            for (int i = 0; i < gpm1.length; i++) {
                if (!gpm1[i].equals(gpm2[i])) {
                    if (gpm1[i] instanceof TypeVariable || gpm2[i] instanceof TypeVariable) {
                        continue;
                    } else if(gpm1[i] instanceof ParameterizedType || gpm2[i] instanceof ParameterizedType) {

                        //See issue 15595 (ClassFormatError: Duplicate method name thrown in deployment)
                        //For ParameterizedType params, compare their non-generics parameter types.
                        same = m1.getParameterTypes()[i].equals(m2.getParameterTypes()[i]);
                        if(!same) {
                            break;
                        }
                    } else {
                        same = false;
                        break;
                    }
                }
            }
        }

        return same;
    }

    /**
     * Compares the signatures of two methods to see if they
     * have the same method name, parameters, and return type.  
     *
     * Note that this equality check does NOT cover :
     * 1) declaring class 2) exceptions
     *
     */
    public static boolean sameMethodSignature(Method m1, Method m2) {
        boolean same = false;
        
        if(m1.getName().equals(m2.getName())) {
            same = sameParamTypes(m1, m2) && sameReturnTypes(m1, m2);
        }

        return same;
    }

    /**
     * Compares the return types of 2 methods.
     * @param m1 method 1
     * @param m2 method 2
     * @return true if the return types of the 2 methods are the same, or if
     * one of them is instance of java.lang.reflect.TypeVariable.
     */
    private static boolean sameReturnTypes(Method m1, Method m2) {
        if(m1.getReturnType().equals(m2.getReturnType())) {
            return true;
        }
        
        Type grt1 = m1.getGenericReturnType();
        Type grt2 = m2.getGenericReturnType();

        if(grt1.equals(grt2)) {
            return true;
        }
        if(grt1 instanceof TypeVariable || grt2 instanceof TypeVariable) {
            return true;
        }

        return false;
    }

    /**
     * Convert a java beans setter method to its property name.
     */
    public static String setterMethodToPropertyName(String setterMethodName) {

        if( (setterMethodName == null) ||
            (setterMethodName.length() <= 3) ||
            !setterMethodName.startsWith("set") ) {
            throw new IllegalArgumentException("Invalid setter method name " +
                                               setterMethodName);
        } 

        return ( setterMethodName.substring(3, 4).toLowerCase(Locale.ENGLISH) +
                 setterMethodName.substring(4) );

    }

    /**
     * Convert a java beans property name to a setter method name
     */
    public static String propertyNameToSetterMethod(String propertyName) {

        if( (propertyName == null) ||
            (propertyName.length() == 0) ) {
            throw new IllegalArgumentException("Invalid property name " +
                                               propertyName);
        } 

        return ( "set" + propertyName.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                 propertyName.substring(1) );

    }
    
   /**
    * Convert String array of class names into array of Classes.
    */
    public static Class[] paramClassNamesToTypes(String[] paramClassNames, 
            ClassLoader loader) throws Exception {

        Class[] parameterTypes = null;
        if (paramClassNames != null) {
            parameterTypes = new Class[paramClassNames.length];
            for(int pIndex = 0; pIndex < parameterTypes.length; pIndex++) {
                String next = paramClassNames[pIndex];
                if( primitiveClasses_.containsKey(next) ) {
                    parameterTypes[pIndex] =
                        (Class) primitiveClasses_.get(next);
                } else {
                    parameterTypes[pIndex] = Class.forName(next, true, loader);
                }
            }
        }

        return parameterTypes;
    }
}
