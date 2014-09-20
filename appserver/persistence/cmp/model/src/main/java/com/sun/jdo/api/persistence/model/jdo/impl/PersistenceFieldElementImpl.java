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
 * PersistenceFieldElementImpl.java
 *
 * Created on March 2, 2000, 6:16 PM
 */

package com.sun.jdo.api.persistence.model.jdo.impl;

import java.beans.PropertyVetoException;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.jdo.PersistenceFieldElement;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public class PersistenceFieldElementImpl extends PersistenceMemberElementImpl 
	implements PersistenceFieldElement.Impl
{
	/** Constant representing read sensitive. */
	private static final int READ_SENSITIVE = 1;

	/** Constant representing write sensitive. */
	private static final int WRITE_SENSITIVE = 2;

	/** Persistence type of the field element. */
	private int _persistenceType;

	/** Derived modifier of the field element. */
	private int _derivedModifier;

	/** Key field flag of the field element. */
	private boolean _isKey;

	/** Create new PersistenceFieldElementImpl with no corresponding name.   
	 * This constructor should only be used for cloning and archiving.
	 */
	public PersistenceFieldElementImpl ()
	{
		this(null);
	}

	/** Creates new PersistenceFieldElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public PersistenceFieldElementImpl (String name)
	{
		super(name);
		_persistenceType = PersistenceFieldElement.PERSISTENT;
	}

	/** Get the persistence type of this field element.
	 * @return the persistence type, one of 
	 * {@link PersistenceFieldElement#PERSISTENT} or
	 * {@link PersistenceFieldElement#DERIVED}.  The default is PERSISTENT.
	 */
	public int getPersistenceType() { return _persistenceType; }

	/** Set the persistence type of this field element.
	 * @param type - an integer indicating the persistence type, one of:
	 * {@link PersistenceFieldElement#PERSISTENT} or 
	 * {@link PersistenceFieldElement#DERIVED}
	 * @exception ModelException if impossible
	 */
	public void setPersistenceType (int type) throws ModelException
	{
		Integer old = new Integer(getPersistenceType());
		Integer newType = new Integer(type);
		
		try
		{
			fireVetoableChange(PROP_PERSISTENCE, old, newType);
			_persistenceType = type;
			firePropertyChange(PROP_PERSISTENCE, old, newType);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Determines whether this field element is read sensitive or not.
	 * This value is only used if <code>getPersistenceType</code> returns
	 * <code>DERIVED</code>
	 * @return <code>true</code> if the field is read sensitive,
	 * <code>false</code> if it is not or if the persistence type is not
	 * derived
	 * @see #isWriteSensitive
	 * @see #setPersistenceType
	 * @see PersistenceFieldElement#DERIVED
	 *
	 */
	public boolean isReadSensitive ()
	{
		return ((PersistenceFieldElement.DERIVED == getPersistenceType()) ? 
			((_derivedModifier & READ_SENSITIVE) != 0) : false);
	}
	
	/** Set whether this field element is read sensitive or not.
	 * @param flag - if <code>true</code> and this is a derived field, the
	 * field element is marked as read sensitive; otherwise, it is not
	 * This value is only used if <code>getPersistenceType</code> returns
	 * <code>DERIVED</code>
	 * @exception ModelException if impossible
	 * @see #setWriteSensitive
	 * @see #setPersistenceType
	 * @see PersistenceFieldElement#DERIVED
	 */
	public void setReadSensitive (boolean flag) throws ModelException
	{
		Boolean old = JavaTypeHelper.valueOf(isReadSensitive());
		Boolean newFlag = JavaTypeHelper.valueOf(flag);

		try
		{
			fireVetoableChange(PROP_SENSITIVITY, old, newFlag);

			if (flag)
				_derivedModifier |= READ_SENSITIVE;
			else
				_derivedModifier &= READ_SENSITIVE;

			firePropertyChange(PROP_SENSITIVITY, old, newFlag);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Determines whether this field element is write sensitive or not.
	 * This value is only used if <code>getPersistenceType</code> returns
	 * <code>DERIVED</code>
	 * @return <code>true</code> if the field is write sensitive,
	 * <code>false</code> if it is not or if the persistence type is not
	 * derived
	 * @see #isReadSensitive
	 * @see #setPersistenceType
	 * @see PersistenceFieldElement#DERIVED
	 *
	 */
	public boolean isWriteSensitive ()
	{
		return ((PersistenceFieldElement.DERIVED == getPersistenceType()) ?
			((_derivedModifier & WRITE_SENSITIVE) != 0) : false);
	}

	/** Set whether this field element is write sensitive or not.
	 * @param flag - if <code>true</code> and this is a derived field, the
	 * field element is marked as write sensitive; otherwise, it is not
	 * This value is only used if <code>getPersistenceType</code> returns
	 * <code>DERIVED</code>
	 * @exception ModelException if impossible
	 * @see #setReadSensitive
	 * @see #setPersistenceType
	 * @see PersistenceFieldElement#DERIVED
	 */
	public void setWriteSensitive (boolean flag) throws ModelException
	{
		Boolean old = JavaTypeHelper.valueOf(isWriteSensitive());
		Boolean newFlag = JavaTypeHelper.valueOf(flag);

		try
		{
			fireVetoableChange(PROP_SENSITIVITY, old, newFlag);

			if (flag)
				_derivedModifier |= WRITE_SENSITIVE;
			else
				_derivedModifier &= WRITE_SENSITIVE;

			firePropertyChange(PROP_SENSITIVITY, old, newFlag);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Determines whether this field element is a key field or not.  
	 * @return <code>true</code> if the field is a key field, 
	 * <code>false</code> otherwise
	 * @see com.sun.jdo.api.persistence.model.jdo.impl.PersistenceClassElementImpl#getKeyClass
	 */
	public boolean isKey () { return _isKey; }

	/** Set whether this field element is a key field or not.
	 * @param flag - if <code>true</code>, the field element is marked 
	 * as a key field; otherwise, it is not
	 * @exception ModelException if impossible
	 * @see com.sun.jdo.api.persistence.model.jdo.impl.PersistenceClassElementImpl#getKeyClass
	 */
	public void setKey (boolean flag) throws ModelException
	{
		Boolean old = JavaTypeHelper.valueOf(isKey());
		Boolean newFlag = JavaTypeHelper.valueOf(flag);

		try
		{
			fireVetoableChange(PROP_KEY_FIELD, old, newFlag);
			_isKey = flag;
			firePropertyChange(PROP_KEY_FIELD, old, newFlag);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}
}
