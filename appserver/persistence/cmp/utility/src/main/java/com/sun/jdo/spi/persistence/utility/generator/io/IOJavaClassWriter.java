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
 * IOJavaClassWriter.java
 *
 * Created on November 12, 2001, 4:59 PM
 */

package com.sun.jdo.spi.persistence.utility.generator.io;

import java.util.*;
import java.lang.reflect.Modifier;

import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriter;

/**
 * This implementation of the {@link JavaClassWriter} interface is based on 
 * simple {@link java.lang.StringBuffer} "println" type statements.
 * <p>
 * The order of the generated code in this implementation depends on the 
 * initialization.  The default order is to accept fields and methods in any
 * order and generate all fields together and all methods together (by member 
 * category).  Specifying the ordered parameter in the constructor will lead 
 * to a slightly different order: generation of fields and methods 
 * interspersed among each other in exactly the order they were added (no 
 * member categories).
 *
 * @author raccah
 */
public final class IOJavaClassWriter implements JavaClassWriter
{
	private static final String FIELD = "FIELD";				// NOI18N
	private static final String INITIALIZER = "INITIALIZER";	// NOI18N
	private static final String CONSTRUCTOR = "CONSTRUCTOR";	// NOI18N
	private static final String METHOD = "METHOD";				// NOI18N
	private static final String INNER_CLASS = "INNER_CLASS";	// NOI18N
	private static final String MIXED = "MIXED";				// NOI18N
	private static final String COMMA_SEPARATOR = ", ";			// NOI18N

	private boolean _maintainCategories;
	private String _superclass;
	private String _classDeclarationBlock;
	private List _interfaces = new ArrayList();
	private Map _members = new HashMap();

	/** Creates a new instance of IOJavaClassWriter which maintains the 
	 * order of member input but groups them by category.
	 * @see #IOJavaClassWriter(boolean)
	 */	
	public IOJavaClassWriter ()
	{
		this(true);
	}

	/** Creates a new instance of IOJavaClassWriter in which the order of the 
	 * generated code depends on the specified flag.
	 * @param maintainCategories If <code>true</code>, the order of members is 
	 * preserved within category groups.  If <code>false</code>, the 
	 * generation of fields and methods will be interspersed among each other 
	 * in exactly the order they were added with no member categories.
	 */	
	public IOJavaClassWriter (boolean maintainCategories)
	{
		_maintainCategories = maintainCategories;
	}

	/** Sets the information for the class declaration including modifiers,
	 * name, and comments.  Note that the name must not be fully qualified.
	 * @param modifiers The modifier flags for this class.
	 * @param className The (non-qualified) name of this class.
	 * @param comments The comments shown just above the class declaration.
	 * The comments are passed as an array so the line separators can be added
	 * by the implementation.  Note that not all implementations will choose
	 * to make use of this comment.
	 * @see java.lang.reflect.Modifier
	 */	
	public void setClassDeclaration (final int modifiers, 
		final String className, final String[] comments)
	{
		final FormattedWriter writerHelper =  new FormattedWriter();
		final String modifierString = Modifier.toString(modifiers);

		writerHelper.writeComments(comments);

		writerHelper.writeln(modifierString + ((modifierString.length() > 0)
			? " " : "") + "class " + className);	// NOI18N

		_classDeclarationBlock = writerHelper.toString();
	}

	/** Sets the superclass of this class.  Note that the name format must 
	 * be package style (that is - it can contain . but not / or $).
	 * @param name The name of the superclass.
	 */	
	public void setSuperclass (final String name)
	{
		_superclass = name;
	}

	/** Adds an interface to the list of those implemented by this class.
	 * @param name The name of the interface.
	 */	
	public void addInterface (final String name)
	{
		if (!StringHelper.isEmpty(name))
			_interfaces.add(name);
	}

	/** Adds a field to the list of those declared by this class.  Note 
	 * that the type format must be package style (that is - it can contain
	 * . but not / or $).
	 * @param name The name of the field.
	 * @param modifiers The modifier flags for this field.
	 * @param type A string representing the type of this field.
	 * @param initialValue A string representing the initial value of
	 * this field.
	 * @param comments The comments shown just above the declaration of
	 * this field.  The comments are passed as an array so the line
	 * separators can be added by the implementation.  Note that not all
	 * implementations will choose to make use of this comment.
	 * @see java.lang.reflect.Modifier
	 */ 
	public void addField (final String name, final int modifiers, String type,
		final String initialValue, final String[] comments)
	{
		final FormattedWriter writerHelper = new FormattedWriter();
		final String fieldString = 
			Modifier.toString(modifiers) + ' ' + type + ' ' + name;

		writerHelper.writeComments(comments);
		writerHelper.writeln(fieldString + ((initialValue != null) ?
			(" = " + initialValue) : "") + ';');		// NOI18N

		getMemberList(FIELD).add(writerHelper.toString());
	}

	/** Adds an initializer to this class.
	 * @param isStatic True if this is a static initializer, false otherwise.
	 * @param body The implementation block of the initializer.  The body of
	 * the implementation is passed as an array so the line separators can
	 * be added by the implementation.
	 * @param comments The comments shown just above the initializer block.
	 * The comments are passed as an array so the line separators can be added
	 * by the implementation.  Note that not all implementations will choose
	 * to make use of this comment.
	 */	
	public void addInitializer (boolean isStatic, String[] body,
		String[] comments)
	{
		final FormattedWriter writerHelper = new FormattedWriter();
		final int n = (body != null ? body.length : 0);

		writerHelper.writeComments(comments);

		// header
		writerHelper.writeln(isStatic ? "static" : "");		// NOI18N
		writerHelper.writeln("{");							// NOI18N

		// implementation
		for (int i = 0; i < n; i++)
			writerHelper.writeln(1, body[i]);

		// end
		writerHelper.writeln("}");							// NOI18N

		getMemberList(INITIALIZER).add(writerHelper.toString());
	}

	/** Adds a constructor to this class.  Note that the type format in the 
	 * parameter type strings must be package style (that is - it can contain
	 * . but not / or $).
	 * @param name The name of the constructor - should be the same as the
	 * name of the class.
	 * @param modifiers The modifier flags for this constructor.
	 * @param parameterNames A list of parameter names.
	 * @param parameterTypes A list of parameter types.
	 * @param exceptions A list of exceptions.
	 * @param body The implementation block of the constructor.  The body of
	 * the implementation is passed as an array so the line separators can
	 * be added by the implementation.
	 * @param comments The comments shown just above the constructor.  The
	 * comments are passed as an array so the line separators can be added
	 * by the implementation.  Note that not all implementations will choose
	 * to make use of this comment.
	 * @see java.lang.reflect.Modifier
	 */	
	public void addConstructor (final String name, final int modifiers,
		final String[] parameterNames, final String[] parameterTypes,
		final String[] exceptions, final String[] body, final String[] comments)
	{
		addMethod(name, modifiers, null, parameterNames, parameterTypes, 
			exceptions, body, comments, getMemberList(CONSTRUCTOR));
	}

	/** Adds a method to this class.  Note that the type format in the 
	 * return type and parameter type strings must be package style 
	 * (that is - it can contain . but not / or $).
	 * @param name The name of the method.
	 * @param modifiers The modifier flags for this method.
	 * @param returnType A string representing the return type of this method.
	 * @param parameterNames A list of parameter names.
	 * @param parameterTypes A list of parameter types.
	 * @param exceptions A list of exceptions.
	 * @param body The implementation block of the method.  The body of
	 * the implementation is passed as an array so the line separators can
	 * be added by the implementation.
	 * @param comments The comments shown just above the method.  The
	 * comments are passed as an array so the line separators can be added
	 * by the implementation.  Note that not all implementations will choose
	 * to make use of this comment.
	 * @see java.lang.reflect.Modifier
	 */	
	public void addMethod (final String name, final int modifiers,
		final String returnType, final String[] parameterNames,
		final String[] parameterTypes, final String[] exceptions,
		final String[] body, final String[] comments)
	{
		addMethod(name, modifiers, returnType, parameterNames, parameterTypes, 
			exceptions, body, comments, getMemberList(METHOD));
	}

	/** Adds an inner class to this class.
	 * @param classWriter The definition of the inner class.
	 */	
	public void addClass (final JavaClassWriter classWriter)
	{
		if (classWriter != null)
			getMemberList(INNER_CLASS).add(classWriter);
	}

	/** Returns a string representation of this object.
	 * @return The string representation of the generated class.
	 */	
	public String toString ()
	{
		final FormattedWriter writerHelper =  new FormattedWriter();

		writeClassDeclaration(writerHelper);		// class declaration
		writeMembers(writerHelper);					// members
		writerHelper.writeln("}");		// NOI18N	// closing
		writerHelper.writeln();

		return writerHelper.toString();
	}

	private void addMethod (final String name, final int modifiers,
		final String returnType, final String[] parameterNames,
		final String[] parameterTypes, final String[] exceptions,
		final String[] body, final String[] comments, List methodList)
	{
		final String signature = createMethodSignature(name, modifiers, 
			returnType, parameterNames, parameterTypes, exceptions);
		final FormattedWriter writerHelper =  new FormattedWriter();
		final int n = (body != null ? body.length : 0);

		writerHelper.writeComments(comments);

		// signature==null if we have an instance initializer
		if (signature.length() > 0)
			writerHelper.writeln(signature);

		writerHelper.writeln("{");					// NOI18N

		// implementation
		for (int i = 0; i < n; i++)
			writerHelper.writeln(1, body[i]);

		// end
		writerHelper.writeln("}");					// NOI18N

		methodList.add(writerHelper.toString());
	}

	static private String createMethodSignature (final String name,
		final int modifiers, String returnType, final String[] parameterNames,
		final String[] parameterTypes, final String[] exceptions)
	{
		int i, count = (parameterNames != null ? parameterNames.length : 0);
		final FormattedWriter writerHelper =  new FormattedWriter();

		if (modifiers != 0)
			writerHelper.write(Modifier.toString(modifiers) + ' ');

		writerHelper.write(((returnType != null) ? 
			returnType + " " : "") + name);			// NOI18N

		// parameters
		writerHelper.write(" (");						// NOI18N

		for (i = 0; i < count; i++)
		{
			writeListElement(i, count, parameterTypes[i] + ' ' + 
				parameterNames[i], writerHelper);
		}
		writerHelper.write(")");						// NOI18N

		// exceptions
		count = (exceptions != null ? exceptions.length : 0);
		if (count > 0)
		{
			writerHelper.writeln();
			writerHelper.write(1, "throws ");				// NOI18N

			for (i = 0; i < count; i++)
				writeListElement(i, count, exceptions[i], writerHelper);
		}

		return writerHelper.toString();
	}

	static private void writeListElement (int i, int count, String string, 
		FormattedWriter writerHelper)
	{
		int indent = ((i == 0) ? 0 : 1);

		if (i == (count - 1))
			writerHelper.write(indent, string);	
		else
			writerHelper.writeln(indent, string + COMMA_SEPARATOR);
	}

	private List getMemberList (String memberType)
	{
		Object memberList = null;

		if (!_maintainCategories)
			memberType = MIXED;

		memberList = _members.get(memberType);
		if (memberList == null)
		{
			memberList = new ArrayList();

			_members.put(memberType, memberList);
		}

		return (List)memberList;
	}

	private void writeClassDeclaration (FormattedWriter writerHelper)
	{
		// class declaration block
		writerHelper.write(_classDeclarationBlock);

		// extends
		if (_superclass != null)
			writerHelper.writeln(1, "extends " + _superclass);	// NOI18N

		// implements
		if ((_interfaces != null) &&  (_interfaces.size() > 0))
		{
			writerHelper.write(1, "implements ");				// NOI18N
			writerHelper.write(StringHelper.arrayToSeparatedList(
				_interfaces, COMMA_SEPARATOR));
			writerHelper.writeln();
		}

		writerHelper.writeln("{");								// NOI18N
	}

	private void writeMembers (FormattedWriter writerHelper)
	{
		if (_maintainCategories)
		{
			writerHelper.writeList(1, getMemberList(FIELD));
			writerHelper.writeList(1, getMemberList(INITIALIZER));
			writerHelper.writeList(1, getMemberList(CONSTRUCTOR));
			writerHelper.writeList(1, getMemberList(METHOD));
			writerHelper.writeList(1, getMemberList(INNER_CLASS));
		}
		else
			writerHelper.writeList(1, getMemberList(MIXED), true);
	}
}
