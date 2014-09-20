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
 * FormattedWriter.java
 *
 * Created on November 14, 2001, 5:50 PM
 */

package com.sun.jdo.spi.persistence.utility.generator.io;

import java.util.*;


/**
 *
 * @author raccah
 */
class FormattedWriter
{
	static private final String lineSeparator =
		System.getProperty("line.separator");
	static private final String indent = "    ";	// NOI18N

	private StringBuffer _buffer;
	private int _initialIndents = 0;

	/** Creates new FormattedWriter */
	FormattedWriter ()
	{
	}

	private StringBuffer getBuffer ()
	{
		if (_buffer == null)
			_buffer = new StringBuffer();

		return _buffer;
	}

	/** Returns a string representation of the FormattedWriter.
	 * @return The string representation of the internal StringBuffer 
	 * used by this object.
	 */	
	public String toString () { return getBuffer().toString(); }

	void writeComments (final String[] comments)
	{
		final int n = (comments != null ? comments.length : 0);

		for (int i = 0; i < n; i++)
		{
			final String s = comments[i];

			writeln("// " + (s != null ? s : ""));	// NOI18N
		}
	}

	private void _write (final int indents, final String s)
	{
		final StringBuffer buffer = getBuffer();

		if (!s.equals(lineSeparator))
		{
			for (int i = 0; i < indents; i++)
				buffer.append(indent);
		}

		buffer.append(s);
	}

	void write (final int indents, final String s)
	{
		_write(indents + _initialIndents, s);
	}

	void write (final String s)
	{
		_write(0, s);
	}

	void writeln (final int indents, final String s)
	{
		if (_initialIndents > 0)
			_write(_initialIndents, "");	// NOI18N

		_write(indents, s + lineSeparator);
	}

	void writeln (final String s)
	{
		writeln(0, s);
	}

	void writeln ()
	{
		writeln(0, "");			// NOI18N
	}

	void writeList (final int indents, final List list, 
		final boolean addSeparator)
	{
		if ((list != null) && (list.size() > 0))
		{
			Iterator iterator = list.iterator();
	
			while (iterator.hasNext())
			{
				indent(indents, iterator.next().toString());

				if (addSeparator)
					writeln();
			}

			if (!addSeparator)
				writeln();
		}
	}

	void writeList (final int indents, final List list)
	{
		writeList(indents, list, false);
	}

	void writeList (final List list)
	{
		writeList(0, list);
	}

	private void indent (final int indents, final String s)
	{
		if (s.indexOf(lineSeparator) != -1)
		{
			StringTokenizer tokenizer =
				new StringTokenizer(s, lineSeparator, true);

			while (tokenizer.hasMoreTokens())
				write(indents, tokenizer.nextToken());
		}
		else
			write(indents, s);
	}
}
