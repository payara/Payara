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
 * PersistenceClassElementImpl.java
 *
 * Created on March 2, 2000, 5:33 PM
 */

package com.sun.jdo.api.persistence.model.jdo.impl;

import java.beans.PropertyVetoException;

import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public class PersistenceClassElementImpl extends PersistenceElementImpl 
	implements PersistenceClassElement.Impl
{
	/** Flag used to keep track of changes to this class element. */
	private boolean _isModified;

	/** Object identity type of the class element. */
	private int _objectIdentityType;

	/** Primary key class of the class element. */
	private String _keyClass;

	/** Fields of the class element. */
	private PersistenceElementCollection _fields;

	/** Concurrency groups of the class element. */
	private PersistenceElementCollection _groups;

	/** Create new PersistenceClassElementImpl with no corresponding name.   
	 * This constructor should only be used for cloning and archiving.
	 */
	public PersistenceClassElementImpl ()
	{
		this(null);
	}

	/** Creates new PersistenceClassElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public PersistenceClassElementImpl (String name)
	{
		super(name);

		if (name != null)
			_keyClass = name + ".Oid";				// NOI18N

		_objectIdentityType = PersistenceClassElement.APPLICATION_IDENTITY;
		_fields = new PersistenceElementCollection(this, PROP_FIELDS, 
			new PersistenceFieldElement[0]);
		_groups = new PersistenceElementCollection(this, PROP_GROUPS, 
			new ConcurrencyGroupElement[0]);
	}

	/** Fires property change event.  This method overrides that of 
	 * PersistenceElementImpl to update the persistence class element's  
	 * modified status.
	 * @param name property name
	 * @param o old value
	 * @param n new value
	 */
	protected final void firePropertyChange (String name, Object o, Object n)
	{
		// even though o == null and n == null will signify a change, that 
		// is consistent with PropertyChangeSupport's behavior and is 
		// necessary for this to work
		boolean noChange = ((o != null) && (n != null) && o.equals(n));

		super.firePropertyChange(name, o, n);

		if (!(PROP_MODIFIED.equals(name)) && !noChange)
			setModified(true);
	}

	/** Fires vetoable change event.  This method overrides that of 
	 * PersistenceElementImpl to give listeners a chance to block 
	 * changes on the persistence class element modified status.
	 * @param name property name
	 * @param o old value
	 * @param n new value
	 * @exception PropertyVetoException when the change is vetoed by a listener
	 */
	protected final void fireVetoableChange (String name, Object o, Object n)
		throws PropertyVetoException
	{
		// even though o == null and n == null will signify a change, that 
		// is consistent with PropertyChangeSupport's behavior and is 
		// necessary for this to work
		boolean noChange = ((o != null) && (n != null) && o.equals(n));

		super.fireVetoableChange(name, o, n);

		if (!(PROP_MODIFIED.equals(name)) && !noChange)
			fireVetoableChange(PROP_MODIFIED, Boolean.FALSE, Boolean.TRUE);
	}

	/** Gets the modified flag for this persistence class.
	 * @return <code>true</code> if there have been (property) changes to this 
	 * class, <code>false</code> otherwise.
	 */
	public boolean isModified () { return _isModified; }

	/** Set the modified flag for this persistence class to flag.  This is  
	 * usually set to <code>true</code> by property changes and 
	 * <code>false</code> after a save.
	 * @param flag if <code>true</code>, this class is marked as modified;
	 * if <code>false</code>, it is marked as unmodified.
	 */
	public void setModified (boolean flag)
	{
		boolean oldFlag = isModified();

		if (flag != oldFlag)
		{
			_isModified = flag;
			firePropertyChange(PROP_MODIFIED, JavaTypeHelper.valueOf(oldFlag), 
				JavaTypeHelper.valueOf(flag));
		}
	}

	/** Get the object identity type of this class element.
	 * @return the object identity type, one of
	 * {@link PersistenceClassElement#APPLICATION_IDENTITY}, 
	 * {@link PersistenceClassElement#DATABASE_IDENTITY}, or
	 * {@link PersistenceClassElement#UNMANAGED_IDENTITY}.  The default is
	 * APPLICATION_IDENTITY.
	 */
	public int getObjectIdentityType ()
	{
		return _objectIdentityType;
	}

	/** Set the object identity type of this class element.
	 * @param type - an integer indicating the object identity type, one of:
	 * {@link PersistenceClassElement#APPLICATION_IDENTITY}, 
	 * {@link PersistenceClassElement#DATABASE_IDENTITY}, or
	 * {@link PersistenceClassElement#UNMANAGED_IDENTITY}
	 * @exception ModelException if impossible
	 */
	public void setObjectIdentityType (int type) throws ModelException
	{
		Integer old = new Integer(getObjectIdentityType());
		Integer newType = new Integer(type);

		try
		{
			fireVetoableChange(PROP_IDENTITY, old, newType);
			_objectIdentityType = type;
			firePropertyChange(PROP_IDENTITY, old, newType);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
 	}


	/** Get the fully qualified name of the primary key class for this class
	 * element.  This value is only used if <code>getObjectIdentityType</code>
	 * returns <code>APPLICATION_IDENTITY</code>
	 * @return the fully qualified key class name, <code>null</code> if the
	 * identity type is not managed by the application
	 * @see #setObjectIdentityType
	 * @see PersistenceClassElement#APPLICATION_IDENTITY
	 *
	 */
	public String getKeyClass ()
	{
		return ((PersistenceClassElement.APPLICATION_IDENTITY == 
			getObjectIdentityType()) ? _keyClass : null);
	}

	/** Set the primary key class for this class element.
	 * @param name - the fully qualified name which represents the primary key
	 * class.  This value is only used if <code>getObjectIdentityType</code>
	 * returns <code>APPLICATION_IDENTITY</code>
	 * @exception ModelException if impossible
	 * @see #setObjectIdentityType
	 * @see PersistenceClassElement#APPLICATION_IDENTITY
	 */
	public void setKeyClass (String name) throws ModelException
	{
		String old = getKeyClass();
		
		try
		{
			fireVetoableChange(PROP_KEY_CLASS, old, name);
			_keyClass = name;
			firePropertyChange(PROP_KEY_CLASS, old, name);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	//================== Fields ===============================

	/** Change the set of fields.
	 * @param fields the new fields
	 * @param action {@link #ADD}, {@link #REMOVE}, or {@link #SET}
	 * @exception ModelException if impossible
	 */
	public void changeFields (PersistenceFieldElement[] fields, int action)
		throws ModelException
	{
		_fields.changeElements(fields, action);
	}

	/** Get all fields.
	 * @return the fields
	 */
	public PersistenceFieldElement[] getFields ()
	{
		return (PersistenceFieldElement[])_fields.getElements();
	}

	/** Find a field by name.
	 * @param name the name to match
	 * @return the field, or <code>null</code> if it does not exist
	 */
	public PersistenceFieldElement getField (String name)
	{
		return (PersistenceFieldElement)_fields.getElement(name);
	}

	//================== ConcurrencyGroups ===============================

	/** Change the set of concurrency groups.
	 * @param groups the new concurrency groups
	 * @param action {@link #ADD}, {@link #REMOVE}, or {@link #SET}
	 * @exception ModelException if impossible
	 */
	public void changeConcurrencyGroups (ConcurrencyGroupElement[] groups,
		int action) throws ModelException
	{
		_groups.changeElements(groups, action);
	}

	/** Get all concurrency groups.
	 * @return the concurrency groups
	 */
	public ConcurrencyGroupElement[] getConcurrencyGroups ()
	{
		return (ConcurrencyGroupElement[])_groups.getElements();
	}

	/** Find a concurrency group by name.
	 * @param name the name to match
	 * @return the concurrency group, or <code>null</code> if it does not exist
	 */
	public ConcurrencyGroupElement getConcurrencyGroup (String name)
	{
		return (ConcurrencyGroupElement)_groups.getElement(name);
	}

	//=============== extra methods needed for xml archiver ==============

	/** Returns the field collection of this class element.  This method 
	 * should only be used internally and for cloning and archiving.
	 * @return the field collection of this class element
	 */
	public PersistenceElementCollection getFieldCollection ()
	{
		return _fields;
	}

	/** Set the field collection of this class element to the supplied 
	 * collection.  This method should only be used internally and for 
	 * cloning and archiving.
	 * @param collection the field collection of this class element
	 */
	public void setFieldCollection (PersistenceElementCollection collection)
	{
		_fields = collection;
	}

	/** Returns the concurrency group collection of this class element.  
	 * This method should only be used internally and for cloning and 
	 * archiving.
	 * @return the concurrency group collection of this class element
	 */
	public PersistenceElementCollection getGroupCollection ()
	{
		return _groups;
	}

	/** Set the concurrency group collection of this class element to the 
	 * supplied collection.  This method should only be used internally 
	 * and for cloning and archiving.
	 * @param collection the concurrency group collection of this class element
	 */
	public void setGroupCollection (PersistenceElementCollection collection)
	{
		_groups = collection;
	}
}
