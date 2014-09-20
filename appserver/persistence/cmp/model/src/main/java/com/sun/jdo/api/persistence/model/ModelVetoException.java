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
 * ModelVetoException.java
 *
 * Created on August 23, 2000, 10:50 PM
 */

package com.sun.jdo.api.persistence.model;

import java.io.PrintStream;
import java.io.PrintWriter;

import com.sun.jdo.spi.persistence.utility.StringHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public class ModelVetoException extends ModelException
{
	/** This field holds the target if the
	 * ModelVetoException (Throwable target) constructor was
	 * used to instantiate the object
	 */
	private Throwable _target;

	/**
	 * Creates new <code>ModelVetoException</code> without detail message and 
	 * <code>null</code> as the target exception.
	 */
	public ModelVetoException ()
	{
	}

	/**
	 * Constructs an <code>ModelVetoException</code> with the specified 
	 * detail message and <code>null</code> as the target exception..
	 * @param msg the detail message.
	 */
	public ModelVetoException (String msg)
	{
		super(msg);
	}

	/**
	 * Constructs a ModelVetoException with a target exception.
	 */
	public ModelVetoException (Throwable target)
	{
		super();
		_target = target;
	}

	/**
	 * Constructs a ModelVetoException with a target exception
	 * and a detail message.
	 */
	public ModelVetoException (Throwable target, String s)
	{
		super(s);
		_target = target;
	}

	/**
	 * Get the thrown target exception.
	 */
	public Throwable getTargetException() { return _target; }

	/**
	* Returns the error message string of this throwable object.
	* @return the error message string of this <code>ModelVetoException</code>
	* object if it was created with an error message string, the error 
	* message of the target exception if it was not created a message 
	* but the target exception has a message, or <code>null</code> if 
	* neither has an error message.
	*
	*/
	public String getMessage()
	{
		String message = super.getMessage();

		if (StringHelper.isEmpty(message))
		{
			Throwable target = getTargetException();

			message	= target.getMessage();
		}

		return message;
	}

	/**
	 * Prints the stack trace of the thrown target exception.
	 * @see java.lang.System#err
	 */
	public void printStackTrace ()
	{
		printStackTrace(System.err);
	}

	/**
	 * Prints the stack trace of the thrown target exception to the specified
	 * print stream.
	 */
	public void printStackTrace (PrintStream ps)
	{
		synchronized (ps)
		{
			Throwable target = getTargetException();

			if (target != null)
			{
				ps.print(getClass() + ": ");			// NOI18N
				target.printStackTrace(ps);
			}
			else
				super.printStackTrace(ps);
		}
	}

	/**
	 * Prints the stack trace of the thrown target exception to the
	 * specified print writer.
	 */
	public void printStackTrace (PrintWriter pw)
	{
		synchronized (pw)
		{
			Throwable target = getTargetException();

			if (target != null)
			{
				pw.print(getClass() + ": ");			// NOI18N
				target.printStackTrace(pw);
			}
			else
				super.printStackTrace(pw);
		}
	}
}
