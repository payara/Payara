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
 * ModelValidationException.java
 *
 * Created on September 22, 2000, 1:05 PM
 */

package com.sun.jdo.api.persistence.model.util;

import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.model.ModelException;
import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.StringHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public class ModelValidationException extends ModelException
{
	/** Constant representing an error. */
	public static final int ERROR = 0;

	/** Constant representing a warning. */
	public static final int WARNING = 1;

	/** I18N message handler */
	private static final ResourceBundle _messages = I18NHelper.loadBundle(
		"com.sun.jdo.api.persistence.model.Bundle",		// NOI18N
		ModelValidationException.class.getClassLoader());

	/** This field holds the type -- one of {@link #ERROR} or {@link #WARNING}
	 */
	private int _type;

	/** This field holds the offending object -- the one being validated 
	 * when the problem occurred
	 */
	private Object _offendingObject;

	/** @return I18N message handler for this element
	 */
	protected static final ResourceBundle getMessages ()
	{
		return _messages;
	}

	/**
	 * Creates new <code>ModelValidationException</code> of type {@link #ERROR} 
	 * without a detail message and with <code>null</code> as the 
	 * offending object.
	 */
	public ModelValidationException ()
	{
	}

	/**
	 * Constructs a <code>ModelValidationException</code> of type 
	 * {@link #ERROR} with the specified detail message and 
	 * <code>null</code> as the offending object.
	 * @param msg the detail message.
	 */
	public ModelValidationException (String msg)
	{
		super(msg);
	}

	/**
	 * Constructs a <code>ModelValidationException</code> of type 
	 * {@link #ERROR} with the specified offending object and no 
	 * detail message.
	 * @param offendingObject the offending object.
	 */
	public ModelValidationException (Object offendingObject)
	{
		super();
		_offendingObject = offendingObject;
	}

	/**
	 * Constructs a <code>ModelValidationException</code> of type 
	 * {@link #ERROR} with the specified detail message and offending 
	 * object.
	 * @param offendingObject the offending object.
	 * @param msg the detail message.
	 */
	public ModelValidationException (Object offendingObject, String msg)
	{
		this(ERROR, offendingObject, msg);
	}

	/**
	 * Constructs a <code>ModelValidationException</code> of the specified 
	 * type  with the specified detail message and offending object.
	 * @param errorType the type -- one of {@link #ERROR} or {@link #WARNING}.
	 * @param offendingObject the offending object.
	 * @param msg the detail message.
	 */
	public ModelValidationException (int errorType, Object offendingObject, 
		String msg)
	{
		super(msg);
		_type = errorType;
		_offendingObject = offendingObject;
	}

	/**
	 * Get the offending object -- the one being validated when the problem 
	 * occurred.
	 */
	public Object getOffendingObject () { return _offendingObject; }

	/**
	 * Get the type -- one of {@link #ERROR} or {@link #WARNING}.
	 */
	public int getType () { return _type; }

	/**
	* Returns the error message string of this throwable object.
	* @return the error message string of this 
	* <code>ModelValidationException</code>, prepended with the warning string 
	* if the type is {@link #WARNING}
	*
	*/
	public String getMessage ()
	{
		String message = super.getMessage();

		if ((WARNING == getType()) && !StringHelper.isEmpty(message))
		{
			message	= I18NHelper.getMessage(getMessages(), 
				"util.validation.warning") + message;			//NOI18N
		}

		return message;
	}
}
