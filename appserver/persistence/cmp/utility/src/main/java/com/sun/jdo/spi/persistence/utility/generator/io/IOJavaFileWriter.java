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
 * IOJavaFileWriter.java
 *
 * Created on November 14, 2001, 5:19 PM
 */

package com.sun.jdo.spi.persistence.utility.generator.io;

import java.io.*;
import java.util.*;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.generator.JavaFileWriter;
import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriter;

/** 
 * This implementation of the {@link JavaFileWriter} interface is based on 
 * {@link java.io.File} and simple {@link java.lang.StringBuffer} "println" 
 * type statements.
 *<p>
 * Use this interface in conjunction with one or more {@link JavaClassWriter} 
 * instances to describe the class(es) in a java file.
 *
 * @author raccah
 */
public class IOJavaFileWriter implements JavaFileWriter
{
	/** I18N message handler */
	private static final ResourceBundle _messages = 
		I18NHelper.loadBundle(IOJavaFileWriter.class);

	private File _file;
	private String _packageBlock;
	private List _importStatements = new ArrayList();
	private List _classes = new ArrayList();

	/** Creates a new instance of IOJavaFileWriter.
	 * @param file The file object which will be used at save time. 
	 */
	public IOJavaFileWriter (File file)
	{
		_file = file;
	}
	
	/** @return I18N message handler for this element
	 */
	protected static final ResourceBundle getMessages () { return _messages; }

	/** Sets the package for this file.  Note that the package name format 
	 * must be package style (that is - it can contain . but not / or $).
	 * @param packageName The name of the package for this source file.
	 * @param comments The comments shown just above the package statement.
	 * The comments are passed as an array so the line separators can be added
	 * by the implementation.  Note that not all implementations will choose
	 * to make use of this comment.
	 */	
	public void setPackage (final String packageName, final String[] comments) 
	{
		final FormattedWriter writerHelper =  new FormattedWriter();

		writerHelper.writeComments(comments);
		if (packageName != null  &&  packageName.length() > 0)
		{
			writerHelper.writeln("package " + packageName + ';');	// NOI18N
			writerHelper.writeln();
		}

		_packageBlock = writerHelper.toString();
	}

	/** Adds an import statement for this source file.
	 * @param importName Name of the class or package (including the *) to be
	 * imported.  This string should not contain "import" or the ;
	 * @param comments The comments shown just above the import statement.
	 * The comments are passed as an array so the line separators can be added
	 * by the implementation.  Note that not all implementations will choose
	 * to make use of this comment.
	 */	
	public void addImport (final String importName, final String[] comments)
	{
		final FormattedWriter writerHelper =  new FormattedWriter();

		writerHelper.writeComments(comments);
		if (importName != null && importName.length() > 0)
			writerHelper.writeln("import " + importName + ';');		// NOI18N

		_importStatements.add(writerHelper.toString());
	}

	/** Adds a class to this source file.
	 * @param classWriter The definition of the class.
	 */	
	public void addClass (final JavaClassWriter classWriter)
	{
		if (classWriter != null)
			_classes.add(classWriter);
	}

	/** Saves the file by writing out the source contents to whatever 
	 * file (or alternate representation) was specified (usually by the 
	 * constructor of the implementation class.
	 * @throws IOException If the file cannot be saved.
	 */	
	public void save () throws IOException
	{
		if (_file != null)
		{
			final File directory = _file.getParentFile();
			final FileWriter fileWriter;

			if (directory != null)
			{
				if (!directory.exists() && !directory.mkdirs())
				{
					throw new IOException(I18NHelper.getMessage(getMessages(), 
						"utility.unable_create_destination_directory",	// NOI18N
						directory.getPath()));
				}
			}
			
			fileWriter = new FileWriter(_file);

			try
			{
				fileWriter.write(toString());
			}
			finally
			{
				fileWriter.close();
			}
		}
	}

	/** Returns a string representation of this object.
	 * @return The string representation of the generated file.
	 */	
	public String toString ()
	{
		final FormattedWriter writerHelper =  new FormattedWriter();

		// package block
		writerHelper.writeln();
		if (_packageBlock != null)
			writerHelper.write(_packageBlock);

		writerHelper.writeList(_importStatements);		// imports
		writerHelper.writeList(_classes);				// classes

		return writerHelper.toString();
	}
}
