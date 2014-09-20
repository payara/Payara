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
 * JavaTypeHelper.java
 *
 * Created on January 22, 2002, 3:39 PM
 */

package com.sun.jdo.spi.persistence.utility;

import java.util.Map;
import java.util.HashMap;

/** This is a helper class which provides some basic java type convenience
 * methods: extraction of a package from a fully qualified class name,
 * extraction of a short (non-qualified) name from a fully qualified class 
 * name, and various wrapper and primitive type methods.
 *
 * @author Rochelle Raccah
 */
public class JavaTypeHelper
{
	/** Map of primitive to wrapper classes */
	private final static Map _primitiveToWrappers;

	/** Map of primitive names to primitive classes */
	private final static Map _primitiveNamesToPrimitives;

	/** Map of primitive names to wrapper names */
	private final static Map _primitiveNamesToWrapperNames;

	/** Map of wrapper classes to primitive names*/
	private final static Map _wrapperToPrimitiveNames;

	static
	{
		_primitiveToWrappers = new HashMap(9);
		_primitiveToWrappers.put(Boolean.TYPE, Boolean.class);
		_primitiveToWrappers.put(Byte.TYPE, Byte.class);
		_primitiveToWrappers.put(Character.TYPE, Character.class);
		_primitiveToWrappers.put(Double.TYPE, Double.class);
		_primitiveToWrappers.put(Float.TYPE, Float.class);
		_primitiveToWrappers.put(Integer.TYPE, Integer.class);
		_primitiveToWrappers.put(Long.TYPE, Long.class);
		_primitiveToWrappers.put(Short.TYPE, Short.class);
		_primitiveToWrappers.put(Void.TYPE, Void.class);

		_primitiveNamesToPrimitives = new HashMap(9);
		_primitiveNamesToPrimitives.put("boolean", Boolean.TYPE);	// NOI18N
		_primitiveNamesToPrimitives.put("byte", Byte.TYPE);			// NOI18N
		_primitiveNamesToPrimitives.put("char", Character.TYPE);	// NOI18N
		_primitiveNamesToPrimitives.put("double", Double.TYPE);		// NOI18N
		_primitiveNamesToPrimitives.put("float", Float.TYPE);		// NOI18N
		_primitiveNamesToPrimitives.put("int", Integer.TYPE);		// NOI18N
		_primitiveNamesToPrimitives.put("long", Long.TYPE);			// NOI18N
		_primitiveNamesToPrimitives.put("short", Short.TYPE);		// NOI18N
		_primitiveNamesToPrimitives.put("void", Void.TYPE);			// NOI18N

		_primitiveNamesToWrapperNames =  new HashMap(9);
		_primitiveNamesToWrapperNames.put("boolean", "Boolean");	// NOI18N
		_primitiveNamesToWrapperNames.put("byte", "Byte");			// NOI18N
		_primitiveNamesToWrapperNames.put("char", "Character");		// NOI18N
		_primitiveNamesToWrapperNames.put("double", "Double");		// NOI18N
		_primitiveNamesToWrapperNames.put("float", "Float");		// NOI18N
		_primitiveNamesToWrapperNames.put("int", "Integer");		// NOI18N
		_primitiveNamesToWrapperNames.put("long", "Long");			// NOI18N
		_primitiveNamesToWrapperNames.put("short", "Short");		// NOI18N
		_primitiveNamesToWrapperNames.put("void", "Void");			// NOI18N

		_wrapperToPrimitiveNames = new HashMap(9);
		_wrapperToPrimitiveNames.put(Boolean.class, "boolean");	// NOI18N
		_wrapperToPrimitiveNames.put(Byte.class, "byte");		// NOI18N
		_wrapperToPrimitiveNames.put(Character.class, "char");	// NOI18N
		_wrapperToPrimitiveNames.put(Double.class, "double");	// NOI18N
		_wrapperToPrimitiveNames.put(Float.class, "float");		// NOI18N
		_wrapperToPrimitiveNames.put(Integer.class, "int");		// NOI18N
		_wrapperToPrimitiveNames.put(Long.class, "long");		// NOI18N
		_wrapperToPrimitiveNames.put(Short.class, "short");		// NOI18N
		_wrapperToPrimitiveNames.put(Void.class, "void");		// NOI18N
	}

	/**  
	 * Returns the package portion of the specified class
	 * @param className the name of the class from which to extract the 
	 * package 
	 * @return package portion of the specified class
	 */   
	public static String getPackageName (final String className)
	{ 
		if (className != null)
		{
			final int index = className.lastIndexOf('.');

			return ((index != -1) ? 
				className.substring(0, index) : ""); // NOI18N
		}

		return null;
    }

	/**
	 * Returns the name of a class without the package name.  For example: if
	 * input = "java.lang.Object" , then output = "Object".
	 * @param className fully qualified classname
	 */
	public static String getShortClassName (final String className)
	{
		if (className != null)
		{
			final int index = className.lastIndexOf('.');

			return className.substring(index + 1);
		}
		return null;
	}

	// ================= primitive/wrapper class utilities ====================

	/** Returns the wrapper class associated with the supplied primitive class.
	 * @param primitive the primitive class to be used for lookup.
	 * @return the associated wrapper class.
	 */
	public static Class getWrapperClass (Class primitive)
	{
		return (Class)_primitiveToWrappers.get(primitive);
	}

	/** Returns the primitive class associated with the supplied primitive 
	 * type name.
	 * @param primitiveName the name of the primitive to be used for lookup.
	 * @return the associated primitive class.
	 */
	public static Class getPrimitiveClass (String primitiveName)
	{
		return (Class)_primitiveNamesToPrimitives.get(primitiveName);
	}

	/** Returns the name of the wrapper class associated with the supplied 
	 * primitive type name.
	 * @param primitiveName the name of the primitive to be used for lookup.
	 * @return the associated wrapper class name.
	 */
	public static String getWrapperName (String primitiveName)
	{
		return (String)_primitiveNamesToWrapperNames.get(primitiveName);
	}

	/** Returns the name of the primitive type associated with the supplied 
	 * wrapper class.
	 * @param wrapper the wrapper class to be used for lookup.
	 * @return the associated primitive type name.
	 */
	public static String getPrimitiveName (Class wrapper)
	{
		return (String)_wrapperToPrimitiveNames.get(wrapper);
	}

	/** Returns the Boolean wrapper object for true or false 
	 * corresponding to the supplied argument.  This is to provide a 
	 * convenience method for this conversion but to prevent calling the 
	 * Boolean constructor which has been determined to be unnecessary 
	 * and a performance problem.  JDK 1.4 provides such a method, but 
	 * some of our code still works with JDK 1.3.
	 * @param flag the primitive boolean object to be translated to a 
	 * Boolean wrapper.
	 * @return the associated true/false shared wrapper object
	 */
	public static Boolean valueOf (boolean flag)
	{
		return (flag ? Boolean.TRUE : Boolean.FALSE);
	}
}
