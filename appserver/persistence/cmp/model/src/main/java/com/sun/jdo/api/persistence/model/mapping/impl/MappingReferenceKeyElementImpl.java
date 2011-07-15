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
 * MappingReferenceKeyElementImpl.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping.impl;

import java.util.ArrayList;
import java.util.ListIterator;
import java.beans.PropertyVetoException;

import org.netbeans.modules.dbschema.*;
import org.netbeans.modules.dbschema.util.NameUtil;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.mapping.MappingTableElement;
import com.sun.jdo.api.persistence.model.mapping.MappingReferenceKeyElement;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public class MappingReferenceKeyElementImpl extends MappingMemberElementImpl
	implements MappingReferenceKeyElement
{
	private ArrayList _referencingKey;	// array of column names
	private MappingTableElement _table;

	/** Create new MappingReferenceKeyElementImpl with no corresponding name.   
	 * This constructor should only be used for cloning and archiving.
	 */
	public MappingReferenceKeyElementImpl ()
	{
		this((String)null);
	}

	/** Creates new MappingReferenceKeyElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public MappingReferenceKeyElementImpl (String name)
	{
		super(name, null);
	}

	/** Creates new MappingReferenceKeyElementImpl with a corresponding 
	 * mapping table. 
	 * @param table mapping table element to be used with this key.
	 */
	public MappingReferenceKeyElementImpl (MappingTableElement table)
		throws ModelException
	{
		super(table.getName(), table.getDeclaringClass());
		setTableInternal(table);
	}

	/** Get the name of this element.
	 * @return the name
	 */
	public String getKeyName () { return getName(); }

	/** Set the name of this element.
	 * @param name the name
	 * @throws ModelException if impossible
	 */
	public void setKeyName (String name) throws ModelException
	{
		setName(name.toString());
	}

	//======================= table handling ===========================

	/** Returns the mapping table element for this referencing key.
	 * @return the meta data table for this referencing key
	 */
	public MappingTableElement getTable () { return _table; }
	
	/** Set the mapping table for this referencing key to the supplied table.
	 * @param table mapping table element to be used with this key.
	 * @exception ModelException if impossible
	 */
	public void setTable (MappingTableElement table) throws ModelException
	{
		MappingTableElement old = getTable();

		try
		{
			fireVetoableChange(PROP_TABLE, old, table);
			setTableInternal(table);
			firePropertyChange(PROP_TABLE, old, table);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Set the mapping table for this referencing key to the supplied table 
	 * without firing any property change events.
	 * @param table mapping table element to be used with this key.
	 * @exception ModelException if impossible
	 */
	private void setTableInternal (MappingTableElement table)
		throws ModelException
	{
		if (table == null)
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.element.null_argument"));					// NOI18N
		}

		_table = table;

		if (null == getDeclaringClass())
			_declaringClass = table.getDeclaringClass();

		if (null == getName())
			_name = table.getName();
	}

	/** Get the declaring table.  This method is provided as part of 
	 * the implementation of the ReferenceKey interface but should only 
	 * be used when a ReferenceKey object is used or by the runtime. 
	 * @return the table that owns this reference key element, or 
	 * <code>null</code> if the element is not attached to any table
	 */
	public TableElement getDeclaringTable ()
	{
		ArrayList locals = getReferencingKey();

		if ((locals != null) && (locals.size() > 0))
		{
			String absoluteName = NameUtil.getAbsoluteMemberName(
				getDeclaringClass().getDatabaseRoot(), 
				locals.get(0).toString());

			return TableElement.forName(NameUtil.getTableName(absoluteName));
		}

		return null;
	}

	/** Set the mapping table for this referencing key to the mapping table 
	 * based on the name of the supplied table.  This method is provided as 
	 * part of the implementation of the ReferenceKey interface but should 
	 * only be used when a ReferenceKey object is used or by the runtime.
	 * @param tableElement mapping table element to be used with this key.
	 */
	public void setDeclaringTable (TableElement tableElement)
	{
		throw new UnsupportedOperationException();
	}

	/** Get the referenced table of the reference key.  This method is 
	 * provided as part of the implementation of the ReferenceKey interface
	 * but should only be used when a ReferenceKey object is used or by 
	 * the runtime.
	 * @return the referenced table
	 */
	public TableElement getReferencedTable ()
	{
		ColumnPairElement[] columnPairs = getColumnPairs();

		if ((columnPairs != null) && (columnPairs.length > 0))
			return columnPairs[0].getReferencedColumn().getDeclaringTable();

		return null;
	}

	//======================= column handling ===========================

	/** Returns the list of key column names in this referencing key.  
	 * This method is private since API users should call the 
	 * <code>getColumnPairNames</code> method.
	 * @return the names of the columns in this referencing key
	 */
	private ArrayList getReferencingKey ()
	{
		if (_referencingKey == null)
			_referencingKey = new ArrayList();

		return _referencingKey;
	}

	/** Returns the list of relative column pair names in this referencing key. 
	 * @return the names of the column pairs in this referencing key
	 */
	public ArrayList getColumnPairNames ()
	{
		ArrayList locals = getReferencingKey();
		ArrayList foreigns = getTable().getKey();
		int i, count = ((locals != null) ? locals.size() : 0);
		ArrayList pairs = new ArrayList();

		for (i = 0; i < count; i++)
			pairs.add(locals.get(i) + ";" + foreigns.get(i));	// NOI18N

		return pairs;
	}

	/** Convenience method which takes a pair and returns its index.
	 * @param searchPairName the relative name of the column pair for 
	 * which to look
	 * @return the index of the column pair or -1 if not found
	 */
	private int getIndexOfColumnPair (String searchPairName)
	{
		ArrayList myPairs = getColumnPairNames();
		int count = ((myPairs != null) ? myPairs.size() : 0);

		if (count > 0)
		{
			int i;

			for (i = 0; i < count; i++)
			{
				if (myPairs.get(i).equals(searchPairName))
					return i;
			}
		}

		return -1;
	}

	/** Adds a column to the list of key columns in this referencing key.
	 * This method is only called privately from addColumnPairs and assumes
	 * that the column is not <code>null</code>.
	 * @param column column element to be added
	 * @exception ModelException if impossible
	 */
	private void addKeyColumn (ColumnElement column) throws ModelException
	{
		ArrayList referencingKey = getReferencingKey();
		String columnName = NameUtil.getRelativeMemberName(
			column.getName().getFullName());

		try
		{
			fireVetoableChange(PROP_KEY_COLUMNS, null, null);
			referencingKey.add(columnName);
			firePropertyChange(PROP_KEY_COLUMNS, null, null);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Get all local columns in this reference key.  This method is 
	 * provided as part of the implementation of the ReferenceKey interface
	 * but should only be used when a ReferenceKey object is used or by 
	 * the runtime.
	 * @return the columns
	 */
	public ColumnElement[] getLocalColumns ()
	{
		ColumnPairElement[] columnPairs = getColumnPairs();
		int i, count = ((columnPairs != null) ? columnPairs.length : 0);
		ColumnElement[] columns = new ColumnElement[count];

		for (i = 0; i < count ; i++)
			columns[i] = columnPairs[i].getLocalColumn();

		return columns;
	}

	/** Get all referenced columns in this reference key.  This method is 
	 * provided as part of the implementation of the ReferenceKey interface
	 * but should only be used when a ReferenceKey object is used or by 
	 * the runtime.
	 * @return the columns
	 */
	public ColumnElement[] getReferencedColumns ()
	{
		ColumnPairElement[] columnPairs = getColumnPairs();
		int i, count = ((columnPairs != null) ? columnPairs.length : 0);
		ColumnElement[] columns = new ColumnElement[count];

		for (i = 0; i < count ; i++)
			columns[i] = columnPairs[i].getReferencedColumn();

		return columns;
	}

	/** Remove a column pair from the holder.  This method can be used to 
	 * remove a pair by name when it cannot be resolved to an actual pair.
	 * @param pairName the relative name of the column pair to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPair (String pairName) throws ModelException
	{
		ArrayList pairNames = new ArrayList(1);

		pairNames.add(pairName);
		removeColumnPairs(pairNames);
	}

	/** Remove some column pairs from the holder.  This method can be used to 
	 * remove pairs by name when they cannot be resolved to actual pairs.
	 * @param pairNames the relative names of the column pairs to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPairs (ArrayList pairNames) throws ModelException
	{
		ArrayList refKey = getReferencingKey();
		ArrayList key = getTable().getKey();
		int i, count = ((pairNames != null) ? pairNames.size() : 0);

		for (i = 0; i < count ; i++)
		{
			String pairName = (String)pairNames.get(i);
			int index = getIndexOfColumnPair(pairName);

			if (pairName != null)
			{
				try
				{
					Object remove1 = null, remove2 = null;

					fireVetoableChange(PROP_KEY_COLUMNS, null, null);

					remove1 = key.remove(index);
					remove2 = refKey.remove(index);

					if ((remove1 == null) || (remove2 == null))
					{
						// if only 1 failed, put the other one back
						if (remove1 != null)
							key.add(index, remove1);
						else if (remove2 != null)
							refKey.add(index, remove2);

						throw new ModelException(I18NHelper.getMessage(
							getMessages(), 
							"mapping.element.element_not_removed", 		// NOI18N
							pairName));
					}

					firePropertyChange(PROP_KEY_COLUMNS, null, null);
				}
				catch (PropertyVetoException e)
				{
					throw new ModelVetoException(e);
				}
			}
		}
	}

	//============= implementation of ColumnPairElementHolder ===============

	/** Add a new column pair to the holder.
	 * @param pair the pair to add
	 * @throws ModelException if impossible
	 */
	public void addColumnPair (ColumnPairElement pair) throws ModelException
	{
		addColumnPairs(new ColumnPairElement[]{pair});
	}

	/** Add some new column pairs to the holder.
	 * @param pairs the column pairs to add
	 * @throws ModelException if impossible
	 */
	public void addColumnPairs (ColumnPairElement[] pairs) throws ModelException
	{
		MappingTableElementImpl table = (MappingTableElementImpl)getTable();
		int i, count = ((pairs != null) ? pairs.length : 0);

		for (i = 0; i < count; i++)
		{
			ColumnPairElement pair = (ColumnPairElement)pairs[i];

			if (pair != null)
			{
				// check if entire pair matches
				// OK only add it if it has not been added before.
				if (getIndexOfColumnPair(NameUtil.getRelativeMemberName(
					pair.getName().getFullName())) == -1)
				{
					table.addKeyColumnInternal(pair.getReferencedColumn());
					addKeyColumn(pair.getLocalColumn());
				}
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
	}

	/** Remove a column pair from the holder.
	 * @param pair the column pair to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPair (ColumnPairElement pair) throws ModelException
	{
		removeColumnPairs(new ColumnPairElement[]{pair});
	}

	/** Remove some column pairs from the holder.
	 * @param pairs the column pairs to remove
	 * @throws ModelException if impossible
	 */
	public void removeColumnPairs (ColumnPairElement[] pairs)
		throws ModelException
	{
		ArrayList pairNames = new ArrayList();
		int i, count = ((pairs != null) ? pairs.length : 0);

		for (i = 0; i < count ; i++)
		{
			ColumnPairElement pair = (ColumnPairElement)pairs[i];

			pairNames.add(NameUtil.getRelativeMemberName(
				pair.getName().getFullName()));
		}

		removeColumnPairs(pairNames);
	}

	/** Set the column pairs for this holder.
	 * Previous column pairs are removed.
	 * @param pairs the new column pairs
	 * @throws ModelException if impossible
	 */
	public void setColumnPairs (ColumnPairElement[] pairs) throws ModelException
	{
		removeColumnPairs(getColumnPairNames());	// remove the old ones
		addColumnPairs(pairs);						// add the new ones
	}

	/** Get all column pairs in this holder.
	 * @return the column pairs
	 */
	public ColumnPairElement[] getColumnPairs ()
	{
		ArrayList pairNames = getColumnPairNames();
		TableElement table = getDeclaringTable();
		int i, count = ((pairNames != null) ? pairNames.size() : 0);
		ColumnPairElement[] pairs = new ColumnPairElement[count];
		String databaseRoot = getDeclaringClass().getDatabaseRoot();

		for (i = 0; i < count; i++)
		{
			String absoluteName = NameUtil.getAbsoluteMemberName(
				databaseRoot, (String)pairNames.get(i));

			pairs[i] = (ColumnPairElement)table.getMember(
				DBIdentifier.create(absoluteName));
		}

		return pairs;
	}

	/** Find a column pair by name.
	 * @param name the name of the column pair for which to look
	 * @return the column pair or <code>null</code> if not found
	 */
	public ColumnPairElement getColumnPair (DBIdentifier name)
	{
		ColumnPairElement[] myPairs = getColumnPairs();
		int count = ((myPairs != null) ? myPairs.length : 0);
		String databaseRoot = getDeclaringClass().getDatabaseRoot();

		if (count > 0)
		{
			String absoluteTableName = NameUtil.getAbsoluteTableName(
				databaseRoot, getTable().getName());
			ColumnPairElement searchPair = (ColumnPairElement)
				TableElement.forName(absoluteTableName).getMember(name);
			int i;

			for (i = 0; i < count; i++)
			{
				if (myPairs[i].equals(searchPair))
					return searchPair;
			}
		}

		return null;
	}

	//=============== extra set methods needed for xml archiver ==============

	/** Set the list of of key column names in this referencing key.  This 
	 * method should only be used internally and for cloning and archiving.
	 * @param referencingKey the list of names of the columns in this 
	 * referencing key
	 */
	public void setReferencingKey (ArrayList referencingKey)
	{
		_referencingKey = referencingKey;
	}

	//============== extra methods for Boston -> Pilsen conversion ============

	/** Boston to Pilsen conversion.
	 * This method converts the name of this MappingReferenceKeyElement and 
	 * the absolute column names stored in _referencingKey to relative names.
	 */
	protected void stripSchemaName ()
	{
		// handle _name (use getRelativeTableName since the name is derived 
		// from the name of the participating table)
		_name = NameUtil.getRelativeTableName(_name);

		// handle _referencingKey
		if (_referencingKey != null)
		{
			// Use ListIterator here, because I want to replace the value 
			// stored in the ArrayList.  The ListIterator returned by 
			// ArrayList.listIterator() supports the set method.
			ListIterator i = _referencingKey.listIterator(); 

			while (i.hasNext())
				i.set(NameUtil.getRelativeMemberName((String)i.next()));
		}
	}
}
