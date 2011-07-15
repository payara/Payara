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
 * MappingTableElementImpl.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping.impl;

import java.util.*;
import java.beans.PropertyVetoException;

import org.netbeans.modules.dbschema.TableElement;
import org.netbeans.modules.dbschema.ColumnElement;
import org.netbeans.modules.dbschema.util.NameUtil;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.mapping.*;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public class MappingTableElementImpl extends MappingMemberElementImpl
	implements MappingTableElement
{
	private ArrayList _key;	// array of column names
	//@olsen: made transient to prevent from serializing into mapping files
	private transient ArrayList _keyObjects;	// array of ColumnElement (for runtime)
	private ArrayList _referencingKeys;	// array of MappingReferenceKeyElement
	private String _table;
	//@olsen: made transient to prevent from serializing into mapping files
	private transient TableElement _tableObject;	// for runtime

	/** Create new MappingTableElementImpl with no corresponding name or 
	 * declaring class.  This constructor should only be used for cloning and 
	 * archiving.
	 */
	public MappingTableElementImpl () 
	{
		this((String)null, null);
	}

	/** Create new MappingTableElementImpl with the corresponding name and 
	 * declaring class.
	 * @param name the name of the element
	 * @param declaringClass the class to attach to
	 */
	public MappingTableElementImpl (String name,
		MappingClassElement declaringClass) 
	{
		super(name, declaringClass);
	}

	/** Creates new MappingTableElementImpl with a corresponding 
	 * table and declaring class. 
	 * @param table table element to be used by the mapping table.
	 * @param declaringClass the class to attach to
	 */
	public MappingTableElementImpl (TableElement table, 
		MappingClassElement declaringClass) throws ModelException
	{
		this(table.toString(), declaringClass);

		// don't use setTable so as not to fire property change events
		_table = getName();
	}

	//======================= table handling ===========================

	/** Returns the name of the table element used by this mapping table.
	 * @return the table name for this mapping table
	 */
	public String getTable () { return _table; }

	/** Set the table element for this mapping table to the supplied table.
	 * @param table table element to be used by the mapping table.
	 * @exception ModelException if impossible
	 */
	public void setTable (TableElement table) throws ModelException
	{
		String old = getTable();
		String newName = table.toString();

		try
		{
			fireVetoableChange(PROP_TABLE, old, newName);
			_table = newName;
			firePropertyChange(PROP_TABLE, old, newName);
			setName(_table);

			// sync up runtime's object too: force next
			// access to getTableObject to recompute it
			_tableObject = null;
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Override method in MappingElementImpl to set the _table variable 
	 * if necessary (used for unarchiving).
	 * @param name the name
	 * @exception ModelException if impossible
	 */
	public void setName (String name) throws ModelException
	{
		super.setName(name);

		if (getTable() == null)
			_table = name;
	}

	/** Returns true if the table element used by this mapping table is equal
	 * to the supplied table.
	 * @return <code>true</code> if table elements are equal,
	 * <code>false</code> otherwise.
	 */
	public boolean isEqual (TableElement table)
	{
		return ((table != null) ? getTable().equals(table.toString()) : false);
	}

	//===================== primary key handling ===========================

	/** Returns the list of column names in the primary key for this 
	 * mapping table.
	 * @return the names of the columns in the primary key for this mapping 
	 * table
	 */
	public ArrayList getKey ()
	{
		if (_key == null)
			_key = new ArrayList();

		return _key;
	}

	/** Adds a column to the primary key of columns in this mapping table.
	 * This method should only be used to manipulate the key columns of the 
	 * primary table.  The secondary table key columns should be manipulated 
	 * using MappingReferenceKeyElement methods for pairs.
	 * @param column column element to be added
	 * @exception ModelException if impossible
	 */
	public void addKeyColumn (ColumnElement column) throws ModelException
	{
		if (column != null)
		{
			String columnName = NameUtil.getRelativeMemberName(
				column.getName().getFullName());

			if (!getKey().contains(columnName))
				addKeyColumnInternal(column);
			else
			{
				// this part was blank -- do we want an error or skip here?
			}
		}
		else
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.element.null_argument"));				// NOI18N
		}
	}

	/** Adds a column to the primary key of columns in this mapping table.
	 * This method is used internally to manipulate primary key columns 
	 * that have passed the null and duplicate tests in addKeyColumn and 
	 * secondary table key columns when pairs are being set up and ignoring 
	 * duplicates is done at the pair level.
	 * @param column column element to be added
	 * @exception ModelException if impossible
	 */
	protected void addKeyColumnInternal (ColumnElement column) throws ModelException
	{
		ArrayList key = getKey();
		String columnName = NameUtil.getRelativeMemberName(
			column.getName().getFullName());

		try
		{
			fireVetoableChange(PROP_KEY_COLUMNS, null, null);
			key.add(columnName);
			firePropertyChange(PROP_KEY_COLUMNS, null, null);

			// sync up runtime's object list too
			//@olsen: rather clear objects instead of maintaining them
			//getKeyObjects().add(column);
			_keyObjects = null;
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Removes a column from the primary key of columns in this mapping table.
	 * This method should only be used to manipulate the key columns of the 
	 * primary table.  The secondary table key columns should be manipulated 
	 * using MappingReferenceKeyElement methods for pairs.
	 * @param columnName the relative name of the column to be removed
	 * @exception ModelException if impossible
	 */
	public void removeKeyColumn (String columnName) throws ModelException
	{
		if (columnName != null)
		{
			try
			{
				fireVetoableChange(PROP_KEY_COLUMNS, null, null);

				if (!getKey().remove(columnName))
				{
					throw new ModelException(
						I18NHelper.getMessage(getMessages(), 
						"mapping.element.element_not_removed",		// NOI18N
						columnName));
				}

				firePropertyChange(PROP_KEY_COLUMNS, null, null);

				// sync up runtime's object list too
				//@olsen: rather clear objects instead of maintaining them
				//getKeyObjects().remove(column);
				_keyObjects = null;
			}
			catch (PropertyVetoException e)
			{
				throw new ModelVetoException(e);
			}
		}
	}

	//===================== reference key handling ===========================

	/** Returns the list of keys (MappingReferenceKeyElements) for this
	 * mapping table. There will be keys for foreign keys and "fake" foreign
	 * keys.
	 * @return the reference key elements for this mapping table
	 */
	public ArrayList getReferencingKeys ()
	{
		if (_referencingKeys == null)
			_referencingKeys = new ArrayList();

		return _referencingKeys;
	}

	/** Adds a referencing key to the list of keys in this mapping table.
	 * @param referencingKey referencing key element to be added
	 * @exception ModelException if impossible
	 */
	public void addReferencingKey (MappingReferenceKeyElement referencingKey)
		throws ModelException
	{
		try
		{
			fireVetoableChange(PROP_REFERENCING_KEYS, null, null);
			getReferencingKeys().add(referencingKey);
			firePropertyChange(PROP_REFERENCING_KEYS, null, null);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Removes the referencing key for the supplied table element from list 
	 * of keys in this mapping table.
	 * @param table mapping table element for which to remove referencing keys
	 * @exception ModelException if impossible
	 */
	public void removeReference (MappingTableElement table)
		throws ModelException
	{
		if (table != null)
		{
			Iterator keyIterator = getReferencingKeys().iterator();

			while (keyIterator.hasNext())
			{
				MappingReferenceKeyElement nextKey = 
					(MappingReferenceKeyElement)keyIterator.next();

				if (nextKey.getTable().equals(table))
				{
					try
					{
						fireVetoableChange(PROP_REFERENCING_KEYS, null, null);
						keyIterator.remove();
						firePropertyChange(PROP_REFERENCING_KEYS, null, null);
					}
					catch (PropertyVetoException e)
					{
						throw new ModelVetoException(e);
					}
				}
			}
		}
		else
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.element.null_argument"));					// NOI18N
		}
	}

	//============= extra object support for runtime ========================

	/** Returns the table element (TableElement) used by this mapping
	 * table.  This method should only be used by the runtime.
	 * @return the table element for this mapping table
	 */
	public TableElement getTableObject ()
	{
		if (_tableObject == null)
		{
			String absoluteTableName = NameUtil.getAbsoluteTableName(
				getDeclaringClass().getDatabaseRoot(), _table);

			_tableObject = TableElement.forName(absoluteTableName);
		}

		return _tableObject;
	}

	/** Returns the list of columns (ColumnElements) in the primary key for
	 * this mapping table.  This method should only be used by the runtime.
	 * @return the column elements in the primary key for this mapping table
	 */
	public ArrayList getKeyObjects ()
	{
		if (_keyObjects == null)
		{
			//@olsen: calculate the key objects based on
			//        the key names as stored in _key
			//_keyObjects = new ArrayList();
			_keyObjects = MappingClassElementImpl.toColumnObjects(
				getDeclaringClass().getDatabaseRoot(), getKey());
		}

		return _keyObjects;
	}

	//=============== extra set methods needed for xml archiver ==============

	/** Set the name of the table element used by this mapping table.  This 
	 * method should only be used internally and for cloning and archiving.
	 * @param table the table name for this mapping table
	 */
	public void setTable (String table)
	{
		_table = table;
	}

	/** Set the list of column names in the primary key for this mapping 
	 * table.  This method should only be used internally and for cloning 
	 * and archiving.
	 * @param key the list of names of the columns in the primary key for 
	 * this mapping table
	 */
	public void setKey (ArrayList key) { _key = key; }

	/** Set the list of keys (MappingReferenceKeyElements) for this mapping 
	 * table.  This method should only be used internally and for cloning 
	 * and archiving.
	 * @param referencingKeys the list of reference key elements for this 
	 * mapping table
	 */
	public void setReferencingKeys (ArrayList referencingKeys)
	{
		_referencingKeys = referencingKeys;
	}

	//=============== extra method for Boston -> Pilsen conversion ============

	/** Boston to Pilsen conversion.  This method converts the absolute db 
	 * element names to relative names.  This affects the name of the 
	 * MappingTableElement itself and the column names stored in _keys.
	 * The method is recursively called for all MappingReferenceKeyElements 
	 * attached to this MappingTableElement.
	 */
	protected void stripSchemaName ()
	{
		// handle _name
		_name = NameUtil.getRelativeTableName(_name);

		// handle _table
		_table = NameUtil.getRelativeTableName(_table);

		// handle _referencingKeys
		// call method stripSchemaName on the MappingReferenceKeyElementImpl 
		// objects
		if (_referencingKeys != null)
		{
			Iterator i = _referencingKeys.iterator(); 

			while (i.hasNext())
			{
				MappingReferenceKeyElementImpl refKey = 
					(MappingReferenceKeyElementImpl)i.next();

				refKey.stripSchemaName();
			}
		}
		
		// handle _key
		if (_key != null)
		{
			// Use ListIterator here, because I want to replace the value 
			// stored in the ArrayList.  The ListIterator returned by 
			// ArrayList.listIterator() supports the set method.
			ListIterator i = _key.listIterator(); 

			while (i.hasNext())
				i.set(NameUtil.getRelativeMemberName((String)i.next()));
		}
	}
}
