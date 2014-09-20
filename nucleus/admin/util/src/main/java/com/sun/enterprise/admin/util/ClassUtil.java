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

package com.sun.enterprise.admin.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;


/*
	Used internally.
 */
final class ClassToClassMapping
{
	final Class	mSrc;
	final Class	mDest;

		public
	ClassToClassMapping( Class src, Class dest )
	{
		mSrc	= src;
		mDest	= dest;
	}
}


/*
	Various utilities used for classes.
 */
public final class ClassUtil
{
		private
	ClassUtil( )
	{
		// disallow instantiation
	}
	
	/*
		Test whether an Object is an array
		
		@param o	object to test
		@returns	true if the object is an array, false otherwise.
	 */
		public static boolean
	objectIsArray( Object o )
	{
		return( classIsArray( o.getClass() ) );
	}
	
	/*
		Test whether a Class is an array
		
		@param theClass		class to test
		@returns			true if the class is an array, false otherwise.
	 */
		public static boolean
	classIsArray( Class theClass )
	{
		return( classnameIsArray( theClass.getName() )  );
	}
	
	/*
		Test whether an Object is an array of primitive types
		
		@param o		object to test
		@returns		true if the object is an array, false otherwise.
	 */
		public static boolean
	objectIsPrimitiveArray( Object o )
	{
		return( getPrimitiveArrayTypeCode( o.getClass() ) != 0 );
	}
	
	/*
		Test whether a classname is an array
		
		@param classname	classname string
		@returns			true if the object is an array, false otherwise.
	 */
		public static boolean
	classnameIsArray( String classname )
	{
		return( classname.startsWith( "[" ) );
	}
	
	
	/*
		Test whether a classname is a primitive array
		
		@param classname	classname string
		@returns			true if the object is a primitive array, false otherwise.
	 */
		public static boolean
	classnameIsPrimitiveArray( String classname )
	{
		return( getPrimitiveArrayTypeCode( classname ) != 0 );
	}
	
	/*
		Return the primitive element type code for an array of primitive types.
		Same as getPrimitiveArrayTypeCode( theClass.getName() )
		
		@param classname	the Class object
		@returns			the element type code; otherwise (char)0
	 */
		public static char
	getPrimitiveArrayTypeCode( Class theClass )
	{
		char		typeCode	= 0;
		
		if ( classIsArray( theClass ) )
		{
			typeCode	= getPrimitiveArrayTypeCode( theClass.getName() );
		}
		
		return( typeCode );
	}
	
	/*
		Return the primitive element type code for an array of primitive types.
		
		@param classname	classname string
		@returns			the element type code; otherwise (char)0
	 */
		public static char
	getPrimitiveArrayTypeCode( String classname )
	{
		char		typeCode	= 0;
		
		final int		length		= classname.length();
		
		if ( classnameIsArray( classname ) &&
				classname.charAt( length - 2 ) == '[' )
		{
			typeCode	= classname.charAt( length - 1 );
			
			switch( typeCode )
			{
				default:	typeCode	= 0;	break;
				
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
		
		return( typeCode );
	}
	
 
	/*
		Get the classname for an array element.
		
		@param classname	classname string
		@returns			the classname for the array element
	 */
 		public static String
 	getArrayMemberClassName( String classname )
 	{
 		String	result	= null;
 		
 		if ( ! classnameIsArray( classname ) )
 		{
 			throw new IllegalArgumentException( "not an array" );
 		}
 		
 		final int classnameLength	= classname.length();
 		
 		
 		if ( classnameIsPrimitiveArray( classname ) )
 		{
 			final char	lastChar	= classname.charAt(classnameLength -1 );
 			
			switch( lastChar )
			{
				default: 	assert( false );
				
				// a simple type
				case 'Z': 	result	= "boolean";	break;
				case 'B': 	result	= "byte";	break;
				case 'C': 	result	= "char";	break;
				case 'S': 	result	= "short";	break;
				case 'I': 	result	= "int";	break;
				case 'J': 	result	= "long";	break;
				case 'F': 	result	= "float";	break;
				case 'D': 	result	= "double";	break;
			}
 		}
 		else
 		{
 			// strip leading "[L" and trailing ";"
 			result	= classname.substring( 2, classnameLength - 1 );
 		}
 		
 		return( result );
 	}
 	

	
		 
	/* 
		Class.forName does not work for primitive types, so we need to do it ourselves here.
	 */
	final static class ClassNameToClassMapping
	{
		String	mName;
		Class	mClass;

		ClassNameToClassMapping( String name, Class theClass )
		{
			mName	= name;
			mClass	= theClass;
		}
	}
	
	private static final ClassNameToClassMapping []	sPrimitiveNameToObjectClass =
		new ClassNameToClassMapping [] 
		{
			new ClassNameToClassMapping( "int", int.class ),
			new ClassNameToClassMapping( "long", long.class ),
			new ClassNameToClassMapping( "short", short.class ),
			new ClassNameToClassMapping( "byte", byte.class ),
			new ClassNameToClassMapping( "boolean", boolean.class ),
			new ClassNameToClassMapping( "float", float.class ),
			new ClassNameToClassMapping( "double", double.class ),
			new ClassNameToClassMapping( "char", char.class ),
			new ClassNameToClassMapping( "void", void.class ),
		};
	 
	/*
		Get a Class from a classname.  Class.forName does not work for primitive types;
		this methods returns the correct Class for any type.
		
		@param classname	classname string
		@returns			the classname for the array element
	 */
		public static Class
	getClassFromName( final String classname )
		throws ClassNotFoundException
	{
		Class	theClass	= null;
		
		if ( classname.startsWith( "[L" ))
		{
			// an array
			theClass	= Class.forName( classname );
		}
		else
		{
			final int numMappings	= Array.getLength( sPrimitiveNameToObjectClass );
			for( int i = 0; i < numMappings; ++i )
			{
				if ( sPrimitiveNameToObjectClass[ i ].mName.equals( classname ) )
				{
					theClass	= sPrimitiveNameToObjectClass[ i ].mClass;
					break;
				}
			}
			
			if ( theClass == null )
			{
				theClass	= Class.forName( classname );
			}
		}
		return( theClass );
	}
	
	
	private static final ClassToClassMapping []	sPrimitiveClassToObjectClass =
	 	new ClassToClassMapping [] 
		 {
		 	new ClassToClassMapping( int.class, Integer.class ),
		 	new ClassToClassMapping( long.class, Long.class ),
		 	new ClassToClassMapping( short.class, Short.class ),
		 	new ClassToClassMapping( byte.class, Byte.class ),
		 	new ClassToClassMapping( boolean.class, Boolean.class),
		 	new ClassToClassMapping( float.class, Float.class ),
		 	new ClassToClassMapping( double.class, Double.class ),
		 	new ClassToClassMapping( char.class, Character.class ),
		 };
	/* 
		Map primitive class Classes to Object forms eg int.class to Integer.class
		
		@param		theClass	the class to map
		@returns	the corresponding Object class or the original Class if not a primitive.
	 */
		public static Class
	primitiveClassToObjectClass( final Class theClass )
	{
		Class	result	= theClass;
		
		final int numMappings	= Array.getLength( sPrimitiveClassToObjectClass );
		for( int i = 0; i < numMappings; ++i )
		{
			final ClassToClassMapping	mapping	= sPrimitiveClassToObjectClass[ i ];
			
			if ( mapping.mSrc.equals( theClass ) )
			{
				result	= mapping.mDest;
				break;
			}
		}
			
		return( result );
	}
	
	/* 
		Test whether a class is a primitive class.
		
		@param		theClass	the class to test
		@returns	true if it's a primitive class, false otherwise.
	 */
		public static boolean
	isPrimitiveClass( final Class theClass )
	{
		boolean	isSimple	= false;
		
		final int numMappings	= Array.getLength( sPrimitiveClassToObjectClass );
		for( int i = 0; i < numMappings; ++i )
		{
			final ClassToClassMapping	mapping	= sPrimitiveClassToObjectClass[ i ];
			
			if ( mapping.mSrc.equals( theClass ) )
			{
				isSimple	= true;
				break;
			}
		}
			
		return( isSimple );
	}
	
	
		public static String
	primitiveLetterToClassName( final char primitive)
	{
		String	result	= "" + primitive;
		
		// see JavaDoc on Class.getName()
		switch( primitive )
		{
			case 'B':	result	= "byte";	break;
			case 'C':	result	= "char";	break;
			case 'D':	result	= "double";	break;
			case 'F':	result	= "float";	break;
			case 'I':	result	= "int";	break;
			case 'J':	result	= "long";	break;
			case 'S':	result	= "short";	break;
			case 'Z':	result	= "boolean";    break;
                        default:        result  = "unknown";    break;
		}
		
		return( result );
	}
	

	
	
		public static String []
	getTypes( final Object [] args )
	{
		if ( args == null )
			return( null );
			
		final int	numArgs	= Array.getLength( args );
		
		final String []	types	= new String [ numArgs ];
		
		for( int i = 0; i < numArgs; ++i )
		{
			types[ i ]	= args[ i ].getClass().getName();
		}
		
		return( types );
	}
	
	
		public static String
	getFriendlyClassname( Class theClass )
	{
		return( getFriendlyClassname( theClass.getName() ) );
	}
	
	/*
		Convert a Java class name string into a more user friendly string. Examples
		java.lang.String		=> String
		java.lang.<type>		=> <type>;
		[i						=> int[]
		[Lfoo.bar.ClassName;	=> foo.bar.ClassName[]
		
		The types thus correspond exactly to what a Java programmer would write, rather
		than the internal JVM representation.
		
		@param 		type
		@returns	a friendlier string representing the type
	 */
	final static String	javaLang	= "java.lang.";
		public static String
	getFriendlyClassname( String type )
	{
		String	result	= type;
		
		if ( type.startsWith( "[" ) )
		{
			// count how deep the array is
			int	depth	= 0;
			while ( type.charAt( depth ) == (int)'[' )
			{
				++depth;
			}
			
			// strip all the '[' characters
			result	= type.substring( depth, type.length() );
			
			if ( result.startsWith( "L" ) && result.endsWith( ";" ) )
			{
				result	= result.substring( 1, result.length() - 1 );
			}
			else if ( result.length() == 1 )
			{
				// a simple type
				switch( result.charAt( 0 ) )
				{
					case 'Z': 	result	= "boolean";	break;
					case 'B': 	result	= "byte";	break;
					case 'C': 	result	= "char";	break;
					case 'S': 	result	= "short";	break;
					case 'I': 	result	= "int";	break;
					case 'J': 	result	= "long";	break;
					case 'F': 	result	= "float";	break;
					case 'D': 	result	= "double";	break;
                                        default:        result  = "unknown";    break;
				}
			}
			
			StringBuilder resultBuf = new StringBuilder(result);
                        for( int i = 0; i < depth; ++i )
			{
				resultBuf.append("[]");
			}
                        result = resultBuf.toString();
		}
		
		if ( result.startsWith( javaLang ) )
		{
			result	= result.substring( javaLang.length(), result.length() );
		}
		
		return( result );
	}
	
		
	/*
	 */
		public static Class
	getArrayElementClass( final Class arrayClass )
	{
		final String	arrayClassName	= arrayClass.getName();
			
		if ( ! classnameIsArray( arrayClassName ) )
		{
			throw new IllegalArgumentException( "not an array" );
		}
		
		String	name	= arrayClassName;
		
		// strip leading "["
		name	= name.substring( 1, name.length() );
		
		if ( ! name.startsWith( "[" ) )
		{
			// element is not an array
			
			if ( name.startsWith( "L" ) )
			{
				// an Object class; strip leading "L" and trailing ";"
				name	= name.substring( 1, name.length() - 1);
			}
			else if ( name.length() == 1 )
			{
				// may be a primitive type
				name	= primitiveLetterToClassName( name.charAt( 0 ) );
			}
		}
		else
		{
			// element is an array; return it
		}
		
		Class	theClass	= null;
		try
		{
			theClass	= getClassFromName( name );
		}
		catch( ClassNotFoundException e )
		{
			assert( false );
		}
		
		return( theClass );
	}
	
		public static Class
	getInnerArrayElementClass( final Class arrayClass )
		throws ClassNotFoundException
	{
		Class	elementClass	= arrayClass;
		
		do
		{
			elementClass	= getArrayElementClass( elementClass );
		}
		while ( classIsArray( elementClass ) );
		
		return( elementClass );
	}
	
	


		private static Object
	instantiateObject( final String theString )
		throws Exception
	{
		Object	result	= null;
		
		try
		{
			result	= instantiateNumber( theString );
		}
		catch( NumberFormatException e )
		{
			result	= theString;
		}
		
		return( result );
	}
	
	/*
		Return true if caller signature is compatible with callee.
		
		@param callee	the signature of the method to be called
		@param caller	the signature of the argument list
	 */
		public static boolean
	signaturesAreCompatible( Class [] callee, Class [] argsSignature )
	{
		boolean	compatible	= false;
		
		if ( callee.length == argsSignature.length )
		{
			compatible	= true;
			
			for( int i = 0; i < callee.length; ++i )
			{
				if ( ! callee[ i ].isAssignableFrom( argsSignature[ i ] ) )
				{
					compatible	= false;
					break;
				}
			}
		}
		
		return( compatible );
	}
	
		public static Object
	instantiateObject( final Class theClass, final Object [] args )
		throws Exception
	{
		final Class []		signature	= new Class [ args.length ];
		
		for( int i = 0; i < signature.length; ++i )
		{
			signature[ i ]	= args[ i ].getClass();
		}
		
		Constructor	constructor	= null;
		try
		{
			// this will fail if a constructor takes an interface;
			// the code below will then find a compatible constructor
			constructor	= theClass.getConstructor( signature );
		}
		catch( NoSuchMethodException e )
		{
			final Constructor []	constructors	= theClass.getConstructors();
			
			int	numMatches	= 0;
			for( int i = 0; i < constructors.length; ++i )
			{
				final Constructor	tempConstructor	= constructors[ i ];
				
				final Class [] tempSignature	= tempConstructor.getParameterTypes();
				
				if ( signaturesAreCompatible( tempSignature, signature ) )
				{
					++numMatches;
					constructor	= tempConstructor;
					// keep going; there could be more than one valid match
				}
			}
			
			// to succeed, there must be exactly one match
			if ( numMatches != 1 )
			{
				throw e;
			}
		}
		
		Object	result	= null;
		try
		{
			result	= constructor.newInstance( args );
		}
		catch( java.lang.reflect.InvocationTargetException e )
		{
			// InvocationTargetException wraps the real cause
			final Throwable cause	= e.getCause();
			
			if ( cause instanceof Exception )
			{
				throw (Exception)cause;
			}
			else
			{
				// shouldn't happen, so we'll just rethrow it
				throw e;
			}
		}
		
		return( result );
	}
	
	
		public static Object
	instantiateObject( final Class theClass, final String theString )
		throws Exception
	{
		final Class []		signature	= new Class [] { String.class };
		final Constructor	constructor	= theClass.getConstructor( signature );
		
		Object	result	= null;
		try
		{
			result	= constructor.newInstance( new Object[] { theString } );
		}
		catch( java.lang.reflect.InvocationTargetException e )
		{
			// InvocationTargetException wraps the real cause
			Throwable cause	= e.getCause();
			
			if ( cause instanceof Exception )
			{
				throw (Exception)cause;
			}
			else
			{
				// shouldn't happen, so we'll just rethrow it
				throw e;
			}
		}
		
		return( result );
	}
	
	/*
		Don't get fancy here, simple precedence:
			Integer, Long	 if no decimal point, use Long if won't fit in an Integer
			Double			 if decimal point (for maximum precision)
	 */
		private static Object
	instantiateNumber( final String theString )
		throws Exception
	{
		Object	result	= null;
		
		if ( theString.indexOf( '.' ) >= 0 )
		{
			result	= instantiateObject( Double.class, theString );
		}
		else
		{
			try
			{
				result	= instantiateObject( Integer.class, theString );
			}
			catch( NumberFormatException e )
			{
				// perhaps it wouldn't fit; try it as a long
				result	= instantiateObject( Long.class, theString );
			}
		}
		return( result );
	}

	
	/*
		Given a Class and a String, create a new instance with a constructor that accept
		a String. Primitive types are instantiated as their equivalent Object forms.
		
		@param theClass		the class from which an instance should be instantiated
		@param theString	the string to be supplied to the constructor
	 */
		public static Object
	instantiateFromString( final Class theClass, final String theString )
		throws Exception
	{
		Object result	= null;
		
		// char and Character do not have a String constructor, so we must special-case it
		if ( theClass == Object.class )
		{
			// special case, apply rules to create an object
			result	= instantiateObject( theString );
		}
		else if ( theClass == Number.class )
		{
			// special case, apply rules to create a number
			result	= instantiateNumber( theString );
		}
		else if ( theClass == Character.class || theClass == char.class)
		{
			if ( theString.length() != 1 )
			{
				throw new IllegalArgumentException( "not a character: " + theString );
			}
			
			result	= Character.valueOf( theString.charAt( 0 ) );
		}
		else
		{
			
			final Class			objectClass	= primitiveClassToObjectClass( theClass );
			
			result	= instantiateObject( objectClass, theString );
		}
		
		return( result );
	}
	
	
	/*
		Given a Class, create a new instance with an empty constructor.
		Primitive types are instantiated as their equivalent Object forms.
		Any value is acceptable in the newly created object.
		
		@param theClass		the class from which an instance should be instantiated
	 */
		public static Object
	instantiateDefault( final Class inClass )
		throws Exception
	{
		Object result	= null;
		
		final Class			objectClass	= primitiveClassToObjectClass( inClass );
		
		if ( Number.class.isAssignableFrom( objectClass ) )
		{
			result	= instantiateFromString( objectClass, "0" );
		}
		else if ( objectClass == Boolean.class)
		{
			result	= Boolean.TRUE;
		}
		else if ( objectClass == Character.class)
		{
			result	= Character.valueOf('X');
		}
		else if ( classIsArray( objectClass ) )
		{
			result	= Array.newInstance( objectClass, 0 );
		}
		else if ( objectClass == Object.class )
		{
			result	= "anyObject";
		}
		else if ( objectClass == String.class )
		{
			result	= "";
		}
		else if ( objectClass == java.net.URL.class )
		{
			result	= new java.net.URL( "http://www.sun.com" );
		}
		else if ( objectClass == java.net.URI.class )
		{
			result	= new java.net.URI( "http://www.sun.com" );
		}
		else if ( classIsArray( inClass ) )
		{
			final int	dimensions	= 3;
			result	= Array.newInstance( getInnerArrayElementClass( inClass ), dimensions );
		}
		else
		{
			result	= objectClass.newInstance();
			//result	= InstantiateFromString( objectClass, "0" );
		}
		return( result );
	}
	
	
	/*
		We allow abbrevations of certain standard java types
		
		Turn "Integer" into "java.lang.Integer", etc.
	 */
	 final static String []	sJavaLangTypes =
	 	{ "Character", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double", "String", "Object"};
	 final static int	sNumBaseTypes	= Array.getLength( sJavaLangTypes );
	 
		public static String
	expandClassName( final String name )
	{
		String	fullName	= name;
		
		final int	numTypes	= sNumBaseTypes;
		for( int i = 0; i < numTypes; ++i )
		{
			if ( name.equals( sJavaLangTypes[ i ] ) )
			{
				fullName	= "java.lang." + name;
				break;
			}
		}
		
		if ( fullName == name )	// no match so far
		{
			if ( name.equals( "Number" ) )
			{
				fullName	= "java.lang." + name;
			}
			else if ( name.equals( "BigDecimal" ) || name.equals( "BigInteger" ) )
			{
				fullName	= "java.math." + name;
			}
			else if ( name.equals( "URL" ) || name.equals( "URI" ) )
			{
				fullName	= "java.net." + name;
			}
			else if ( name.equals( "Date" ) )
			{
				fullName	= "java.util." + name;
			}
			else if ( name.equals( "ObjectName" ) )
			{
				fullName	= "javax.management." + name;
			}
			
		}
		
		return( fullName );
	}
	
	

	/*
		Convert inner element.  Only works for arrays of Objects.  Example:
		
		mapActualElementClass( "[[[LObject;", "Long" ) =>[[[LLong;
	 */
		public static Class
	convertArrayClass( final Class arrayClass, final Class newInnerType )
		throws ClassNotFoundException
	{
		final String	arrayClassname	= arrayClass.getName();
		if ( ! arrayClassname.endsWith( ";" ) )
		{
			throw new IllegalArgumentException( "not an array of Object" );
		}
		
		final int innerNameBegin	= 1 + arrayClassname.indexOf( "L" );
		
		final String newClassName	= arrayClassname.substring( 0, innerNameBegin ) + newInnerType.getName() + ";";
		
		final Class	newClass	= getClassFromName( newClassName );
		
		return( newClass  );
	}
	
	
}

