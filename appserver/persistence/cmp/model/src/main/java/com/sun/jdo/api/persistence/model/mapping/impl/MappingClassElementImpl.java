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
 * MappingClassElementImpl.java
 *
 * Created on March 3, 2000, 1:11 PM
 */

package com.sun.jdo.api.persistence.model.mapping.impl;

import java.util.*;
import java.beans.PropertyVetoException;

import org.netbeans.modules.dbschema.*;
import org.netbeans.modules.dbschema.util.NameUtil;

import com.sun.jdo.api.persistence.model.*;
import com.sun.jdo.api.persistence.model.mapping.*;
import com.sun.jdo.api.persistence.model.jdo.PersistenceClassElement;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public class MappingClassElementImpl extends MappingElementImpl
	implements MappingClassElement
{
	// used in properties bitmask, jeff will check if used
	public static final int CLONE_FIELDS = 1;
	public static final int CLONE_DEEP = 2;
	public static final int CLONE_MASK = 3;
	public static final int NAVIGABLE = 4;

	PersistenceClassElement _persistenceElement;

	private boolean _isModified;
	private int _properties;
	private ArrayList _tables;		// array of MappingTableElement
	private ArrayList _fields;		// array of MappingFieldElement

	/** The current version number.
	 * Note: Please increment this if there are any changes in the 
	 * mapping model that might cause incompatibilities to older versions.
	 */
	private static final int CURRENT_VERSION_NO = 5;

	/** Version number of this MappingClassElementImpl object.
	 * This number is set by the initilaizer of the declaration or 
	 * set by the archiver when reading a mapping file.
	 */
	private int versionNo = CURRENT_VERSION_NO;

	/** The database root for this MappingClassElement.
	 * The database root is the schema name of of all the db elements 
	 * attached to this MappingClassElement.
	 */
	private String _databaseRoot;

	/** Consistency Level of this MappingClassElement.
	 */
	private int _consistencyLevel;

	
	/*
	// possibly for EJB use later
	// private String EJBName;
	// which of these (one from desc, one from config)?
	// public Class finderClass;
	// private String finderClass;
	// private Class finderClassType;
	// public static final String DEFAULT_JAVA_FINDERCLASS ="not yet implemented";
	// end possibly for EJB use later
	*/

	// possibly for sequence/identity fields later
	// public ColumnElement uniqueIDCol;

	/** Create new MappingClassElementImpl with no corresponding persistence 
	 * element or name.  This constructor should only be used for cloning and 
	 * archiving.
	 */
	public MappingClassElementImpl ()
	{
		this((String)null);
	}

	/** Creates new MappingClassElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public MappingClassElementImpl (String name)
	{
		super(name);
		_consistencyLevel = NONE_CONSISTENCY;
		_properties = _properties | NAVIGABLE;
	}

	/** Creates new MappingClassElementImpl with a corresponding 
	 * PersistenceClassElement 
	 * @param element the persistence element with which to be associated
	 */
	public MappingClassElementImpl (PersistenceClassElement element)
	{
		this((element != null) ? element.getName() : null);
		setPersistenceElement(element);
	}

	/** Returns the version number of this MappingClassElement object.
	 * Please note, the returned version number reflects the version number at 
	 * the last save, NOT the version number of the memory representation.
	 * @return version number
	 */
	public int getVersionNumber () { return versionNo; }

	/** Set the version number of this MappingClassElement.
	 * @param version the new version number
	 */
	private void setVersionNumber (int version) { versionNo = version; }
	
	/** Returns true if the version number of this MappingClassElement object
	 * is older than the current version number of the archiving scheme.
	 * @see #getVersionNumber
	 * @return true if it is in need of updating, false otherwise
	 */
	public boolean hasOldVersionNumber ()
	{
		return (getVersionNumber() < CURRENT_VERSION_NO);
	}

	/** Returns the mapping class element associated with the class with the 
	 * given string name, using the given model object to look it up.
	 * @param name the fully qualified name of the desired class
	 * @param model the model object to be used to look it up
	 * @return mapping class element representing the desired class
	 */
	public static MappingClassElement forName (String name, Model model)
	{
		return model.getMappingClass(name);
	}

	/** Fires property change event.  This method overrides that of 
	 * MappingElementImpl to update the mapping class element's modified 
	 * status.
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
	 * MappingElementImpl to give listeners a chance to block 
	 * changes on the mapping class element modified status.
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

	/** @return persistence class element for this mapping class element
	 */
	public final PersistenceClassElement getPersistenceElement ()
	{
		return _persistenceElement;
	}

	/** Set the persistence class element for this mapping class element.
	 * @param element the persistence class element
	 */
	public void setPersistenceElement (PersistenceClassElement element)
	{
		_persistenceElement = element;
	}

	// TBD if needed
	/*public void clear()
	{
		Iterator fieldIterator = getFields().iterator();

		while (fieldIterator.hasNext())
			((MappingFieldElement)fieldIterator.next()).clear();

		_tables = new ArrayList();
	}
*/
	//================= implementation of MappingClassElement ================

	/** Gets the modified flag for this mapping class.
	 * @return <code>true</code> if there have been (property) changes to this 
	 * class, <code>false</code> otherwise.
	 */
	public boolean isModified () { return _isModified; }

	/** Set the modified flag for this mapping class to flag.  This is usually 
	 * set to <code>true</code> by property changes and <code>false</code> 
	 * after a save.
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

	/** Gets the consistency level of this mapping class.
	 * @return the consistency level, one of {@link #NONE_CONSISTENCY},
	 * {@link #CHECK_MODIFIED_AT_COMMIT_CONSISTENCY}, 
	 * {@link #CHECK_ALL_AT_COMMIT_CONSISTENCY}, 
	 * {@link #LOCK_WHEN_MODIFIED_CONSISTENCY}, 
	 * {@link #LOCK_WHEN_MODIFIED_CHECK_ALL_AT_COMMIT_CONSISTENCY}, 
	 * {@link #LOCK_WHEN_LOADED_CONSISTENCY}, or 
	 * {@link #VERSION_CONSISTENCY}.
	 * The default is {@link #NONE_CONSISTENCY}.
	 */
	public int getConsistencyLevel () { return _consistencyLevel; }

	/** Set the consistency level of this mapping class.
	 * @param level an integer indicating the consistency level, one of:
	 * {@link #NONE_CONSISTENCY},{@link #CHECK_MODIFIED_AT_COMMIT_CONSISTENCY}, 
	 * {@link #CHECK_ALL_AT_COMMIT_CONSISTENCY}, 
	 * {@link #LOCK_WHEN_MODIFIED_CONSISTENCY}, 
	 * {@link #LOCK_WHEN_MODIFIED_CHECK_ALL_AT_COMMIT_CONSISTENCY}, 
	 * {@link #LOCK_WHEN_LOADED_CONSISTENCY}, or 
	 * {@link #VERSION_CONSISTENCY}.
	 * @exception ModelException if impossible.
	 */
	public void setConsistencyLevel (int level)  throws ModelException
	{
		Integer old = new Integer(getConsistencyLevel());
		Integer newLevel = new Integer(level);

		try
		{
			fireVetoableChange(PROP_CONSISTENCY, old, newLevel);
			_consistencyLevel = level;
			firePropertyChange(PROP_CONSISTENCY, old, newLevel);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Returns the name of the SchemaElement which represents the 
	 * database used by the tables mapped to this mapping class.
	 * @return the name of the database root for this mapping class
	 */
	public String getDatabaseRoot () { return _databaseRoot; }

	/** Set the database root for this MappingClassElement.
	 * The root represents the database used by the tables mapped to 
	 * this mapping class.
	 * @param root the new database root
	 * @exception ModelException if impossible
	 */
	public void setDatabaseRoot (SchemaElement root) throws ModelException
	{
		String old = getDatabaseRoot();
		String newRoot = ((root != null) ? root.getName().getFullName() : null);
		
		try
		{
			fireVetoableChange(PROP_DATABASE_ROOT, old, newRoot);
			_databaseRoot = newRoot;
			firePropertyChange(PROP_DATABASE_ROOT, old, newRoot);
		}
 		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Returns the list of tables (MappingTableElements) used by this mapping 
	 * class.
	 * @return the meta data tables for this mapping class
	 */
	public ArrayList getTables ()
	{
		if (_tables == null)
			_tables = new ArrayList();

		return _tables;
	}

	/** Scans through this mapping class looking for a table whose
	 * name matches the name passed in.
	 * @param name name of the table to find.
	 * @return the meta data table whose name matches the name parameter
	 */
	public MappingTableElement getTable (String name)
	{
		Iterator tableIterator = getTables().iterator();

		while (tableIterator.hasNext())
		{
			MappingTableElement table = 
				(MappingTableElement)tableIterator.next();

			if (table.getName().equals(name))
				return table;
		}

		return null;
	}

	/** Convenience method which accepts a table element and attempts to add 
	 * it as either a primary or secondary table depending on the existing list
	 * of tables and the foreign keys for the table.
	 * @param table table element to be added as either a primary or secondary 
	 * table.
	 * @exception ModelException if impossible
	 */
	public void addTable (TableElement table) throws ModelException
	{
		if (table != null)
		{
			ArrayList tables = getTables();

			// If the table list is empty, this should be the primary table
			if (tables.isEmpty())
				setPrimaryTable(table);
			else
			{
				HashMap newSecondaryTables = new HashMap();
				Iterator iterator = tables.iterator();
				boolean found = false;

				// If this table has already been added just skip it and return
				while (iterator.hasNext())
					if (((MappingTableElement)iterator.next()).isEqual(table))
						return;

				// Add the table as a secondary table as long as there are
				// relevant fks setup. Otherwise, throw an exception
				iterator = tables.iterator();
				while (iterator.hasNext())
				{
					MappingTableElement mappingTable = 
						(MappingTableElement)iterator.next();
					String absoluteTableName = NameUtil.getAbsoluteTableName(
						_databaseRoot, mappingTable.getTable());
					ForeignKeyElement[] foreignKeys = TableElement.forName(
						absoluteTableName).getForeignKeys();
					int i, count = 
						((foreignKeys != null) ? foreignKeys.length : 0);

					for (i = 0; i < count; i++)
					{
						ForeignKeyElement fk = foreignKeys[i];

						if (table == fk.getReferencedTable())
						{
							// store it so it can be added after we finish
							// iterating the array (can't now because of 
							// concurrent modification restrictions)
							newSecondaryTables.put(mappingTable, fk);
							found = true;
						}
					}
				}

				if (found)	// add the secondary tables now
				{
					iterator = newSecondaryTables.keySet().iterator();
					
					while (iterator.hasNext())
					{		
						MappingTableElement mappingTable = 
							(MappingTableElement)iterator.next();
						MappingReferenceKeyElement refKey = 
							addSecondaryTable(mappingTable, table);

						refKey.addColumnPairs(((ForeignKeyElement)
							newSecondaryTables.get(mappingTable)).
							getColumnPairs());
					}

				}
				else
				{
					throw new ModelException(I18NHelper.getMessage(
						getMessages(), 
						"mapping.table.foreign_key_not_found", table)); // NOI18N
				}
			}
		}
		else
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.table.null_argument"));					// NOI18N
		}
	}

	/** Set the primary table for this mapping class to the supplied table.
	 * @param table table element to be used as the primary table.
	 * @exception ModelException if impossible
	 */
	public void setPrimaryTable (TableElement table) throws ModelException
	{
		ArrayList tables = getTables();

		if (!tables.isEmpty())
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.table.primary_table_defined", table));	// NOI18N
		}
		else
		{
			UniqueKeyElement key = table.getPrimaryKey();
			MappingTableElement mappingTable = 
				new MappingTableElementImpl(table, this);
			SchemaElement schema = table.getDeclaringSchema();
			String currentRoot = getDatabaseRoot();

			if (currentRoot == null)	// set database root
				setDatabaseRoot(schema);
			else if (!currentRoot.equals(schema.getName().getFullName()))
			{
				// if database root was set before, it must match
				throw new ModelException(I18NHelper.getMessage(
					getMessages(), "mapping.table.schema_mismatch",	// NOI18N
					table.toString(), currentRoot));
			}

			try
			{
				fireVetoableChange(PROP_TABLES, null, null);
				tables.add(mappingTable);
				firePropertyChange(PROP_TABLES, null, null);
			}
			catch (PropertyVetoException e)
			{
				throw new ModelVetoException(e);
			}

			//	If can't find a primary key, settle for first unique key.
			if (key == null)
			{
				UniqueKeyElement[] uniqueKeys = table.getUniqueKeys();

				if ((uniqueKeys != null) && (uniqueKeys.length > 0))
					key = uniqueKeys[0];
			}

			if (key == null)
			{
				//	This is a warning -- we can still use the table but we 
				//	cannot perform update operations on it.  Also the user  
				//	may define the key later.
			}
			else
			{
				ColumnElement[] columns = key.getColumns();
				int i, count = ((columns != null) ? columns.length : 0);

				for (i = 0; i < count; i++)
					mappingTable.addKeyColumn(columns[i]);
			}
		}
	}

	/** Adds a reference to the supplied table as a secondary table for this
	 * mapping class.  It creates a MappingReferenceKeyElement for the supplied
	 * primary/secondary table pair.
	 * @param parentTable mapping table element which should also be the primary
	 * table.
	 * @param table table element to be used as a secondary table.
	 * @exception ModelException if impossible
	 */
	public MappingReferenceKeyElement addSecondaryTable (MappingTableElement 
		parentTable, TableElement table) throws ModelException
	{
		ArrayList tables = getTables();

		if ((parentTable == null) || (table == null))
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.element.null_argument"));				// NOI18N
		}
		else if (!tables.contains(parentTable))
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.table.parent_table_not_found", 		// NOI18N
				parentTable.getTable()));
		}
		else
		{
			// Check the parent table's reference keys to make sure that this
			// secondary table has not already been added to this parent table.
			// If it has, throw an exception
			Iterator iterator = parentTable.getReferencingKeys().iterator();
			MappingTableElement mappingTable = 
				new MappingTableElementImpl(table, this);
			MappingReferenceKeyElement key = 
				new MappingReferenceKeyElementImpl(mappingTable);

			while (iterator.hasNext())
			{
				MappingTableElement compareTable = 
					((MappingReferenceKeyElement)iterator.next()).getTable();

				if (compareTable.isEqual(table))
				{
					throw new ModelException(I18NHelper.getMessage(
						getMessages(), 
						"mapping.table.secondary_table_defined", 	// NOI18N
						new Object[]{table, parentTable.getTable()}));
				}
			}

			try
			{
				fireVetoableChange(PROP_TABLES, null, null);
				parentTable.addReferencingKey(key);
				tables.add(mappingTable);
				firePropertyChange(PROP_TABLES, null, null);
			}
			catch (PropertyVetoException e)
			{
				throw new ModelVetoException(e);
			}

			return key;
		}
	}

	/** Removes the reference to the supplied table as a mapped table for this
	 * mapping class.  This works whether the table is the primary table or a
	 * secondary table.
	 * @param table mapping table element to be removed from this mapping class.
	 * @exception ModelException if impossible
	 */
	public void removeTable (MappingTableElement table) throws ModelException
	{
		if (table != null)
		{
			Collection tables = getTables();
			Iterator iterator = null;
			boolean found = false;

			try
			{
				fireVetoableChange(PROP_TABLES, null, null);
				found = tables.remove(table);
				firePropertyChange(PROP_TABLES, null, null);
			}
			catch (PropertyVetoException e)
			{
				throw new ModelVetoException(e);
			}

			// remove all references to this table
			iterator = tables.iterator();
			while (iterator.hasNext())
			{
				MappingTableElement nextTable =
					(MappingTableElement)iterator.next();

				nextTable.removeReference(table);
			}

			if (found)	// remove any fields mapped to that table
			{
				ArrayList fieldsToRemove = new ArrayList();

				iterator = getFields().iterator();
				while (iterator.hasNext())
				{
					MappingFieldElementImpl mappingField = 
						(MappingFieldElementImpl)iterator.next();
					
					if (mappingField.isMappedToTable(table))
						fieldsToRemove.add(mappingField);
				}

				iterator = fieldsToRemove.iterator();
				while (iterator.hasNext())
				{
					MappingFieldElement mappingField = 
						(MappingFieldElement)iterator.next();
					boolean versionField = mappingField.isVersion();

					removeField(mappingField);

					// if it is a version field, add back an unmapped
					// field which retains the version flag setting
					if (versionField)
					{
						mappingField = new MappingFieldElementImpl(
							mappingField.getName(), this);
						mappingField.setVersion(true);
						addField(mappingField);
					}
				}
			}
			else
			{
				throw new ModelException(I18NHelper.getMessage(getMessages(), 
					"mapping.element.element_not_removed", table));	// NOI18N
			}
		}
		else
		{
			throw new ModelException(I18NHelper.getMessage(getMessages(), 
				"mapping.element.null_argument"));				// NOI18N
		}
	}

	/** Returns the list of fields (MappingFieldElements) in this mapping 
	 * class.  This list includes both local and relationship fields.
	 * @return the mapping fields in this mapping class
	 */
	public ArrayList getFields ()
	{
		if (_fields == null)
			_fields = new ArrayList();

		return _fields;
	}

	/** Scans through this mapping class looking for a field whose
	 * name matches the name passed in.
	 * @param name name of the field to find.
	 * @return the mapping field whose name matches the name parameter
	 */
	public MappingFieldElement getField (String name)
	{
		Iterator fieldIterator = getFields().iterator();

		while (fieldIterator.hasNext())
		{
			MappingFieldElement field = 
				(MappingFieldElement)fieldIterator.next();

			if (name.equals(field.getName()))
				return field;
		}

		return null;
	}

	/** Adds a field to the list of fields in this mapping class.
	 * @param field field element to be added
	 * @exception ModelException if impossible
	 */
	public void addField (MappingFieldElement field) throws ModelException
	{
		ArrayList fields = getFields();

		if (!fields.contains(field))
		{
			try
			{
				fireVetoableChange(PROP_FIELDS, null, null);
				fields.add(field);
				firePropertyChange(PROP_FIELDS, null, null);
			}
			catch (PropertyVetoException e)
			{
				throw new ModelVetoException(e);
			}
		}
	}

	/** Removes a field from the list of fields in this mapping class.
	 * @param field field element to be removed
	 * @exception ModelException if impossible
	 */
	public void removeField (MappingFieldElement field) throws ModelException
	{
		try
		{
			fireVetoableChange(PROP_FIELDS, null, null);

			if (!getFields().remove(field))
			{
				throw new ModelException(I18NHelper.getMessage(getMessages(), 
					"mapping.element.element_not_removed", field));	// NOI18N
			}

			firePropertyChange(PROP_FIELDS, null, null);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	/** Returns the list of version fields (MappingFieldElements) in this 
	 * mapping class.  This list only includes fields if the consistency 
	 * level is {@link #VERSION_CONSISTENCY}.
	 * @return the version fields in this mapping class
	 */
	public List getVersionFields ()
	{
		List versionFields = new ArrayList();

		if (VERSION_CONSISTENCY == getConsistencyLevel())
		{
			Iterator iterator = getFields().iterator();

			while (iterator.hasNext())
			{
				MappingFieldElement fieldCandidate =
					(MappingFieldElement)iterator.next();

				if (fieldCandidate.isVersion())
					versionFields.add(fieldCandidate);
			}
		}

		return versionFields;
	}

	/** Gets the navigable flag for this mapping class.
	 * @return <code>true</code> if lazy initialization will be used, 
	 * <code>false</code> if access to a non-fetched field will result in an
	 * exception.  The default is <code>true</code>.
	 */
	public boolean isNavigable () { return ((_properties & NAVIGABLE) > 0); }

	/** Set the navigable flag for this mapping class to flag.
	 * @param flag if <code>true</code>, lazy initialization will be used;
	 * if <code>false</code>, access to a non-fetched field will result in an
	 * exception.
	 * @exception ModelException if impossible
	 */
	public void setNavigable (boolean flag) throws ModelException
	{
		Boolean old = JavaTypeHelper.valueOf(isNavigable());
		Boolean newFlag = JavaTypeHelper.valueOf(flag);

		try
		{
			fireVetoableChange(PROP_NAVIGABLE, old, newFlag);
			_properties = (flag) ? 
				(_properties | NAVIGABLE) : (_properties & ~NAVIGABLE);
			firePropertyChange(PROP_NAVIGABLE, old, newFlag);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	//============= extra object support for runtime ========================

	/** Accept an arraylist of column names and return an array list containing
	 * the corresponding column or column pair objects.
	 * @param schemaName the database root used to find the column objects
	 * @param columnNames array of column names.
	 * @return an array of corresponding column objects
	 * @see org.netbeans.modules.dbschema.TableElement#forName
	 * @see org.netbeans.modules.dbschema.TableElement#getMember
	 */
	protected static ArrayList toColumnObjects (String schemaName, 
		ArrayList columnNames)
	{
		Iterator iterator = columnNames.iterator();
		ArrayList objects = new ArrayList();

		while (iterator.hasNext())
		{
			String columnName = (String)iterator.next();
			String absoluteColumnName = 
				NameUtil.getAbsoluteMemberName(schemaName, columnName);
			final TableElement table =
				TableElement.forName(NameUtil.getTableName(absoluteColumnName));

			objects.add(table.getMember(
				DBIdentifier.create(absoluteColumnName)));
		}

		return objects;
	}

	//============= delegation to PersistenceClassElement ===========

	/** Get the fully qualified name of the primary key class for this class
	 * element.  This value is only used if <code>getObjectIdentityType</code>
	 * returns <code>APPLICATION_IDENTITY</code>
	 * @return the fully qualified key class name, <code>null</code> if the
	 * identity type is not managed by the application
	 * @see PersistenceClassElement#setObjectIdentityType
	 * @see PersistenceClassElement#APPLICATION_IDENTITY
	 *
	 */
	public String getKeyClass ()
	{
		return getPersistenceElement().getKeyClass();
	}

	//=============== extra set methods needed for xml archiver ==============

	/** Set the list of tables (MappingTableElements) used by this mapping 
	 * class.  This method should only be used internally and for cloning 
	 * and archiving.
	 * @param tables the list of meta data tables for this mapping class
	 */
	//@olsen: disabled method because not used by archiver
/*
	public void setTables (ArrayList tables) { _tables = tables; }
*/

	/** Set the list of fields (MappingFieldElements) in this mapping 
	 * class.  This method should only be used internally and for cloning 
	 * and archiving.
	 * @param fields the list of mapping fields in this mapping class
	 */
	public void setFields (ArrayList fields) { _fields = fields; }

	public int getProperties () { return _properties; }

	//======== to be used for reference in best guess implementation ==========
	// configure methods

	/**
	 * The following method attempts to map a field to a particular table.
	 * Only local fields are supported at present
	 * @return boolean to indicate whether the mapping worked
	 * @param fieldToMap The field to be mapped
	 * @param table The table the fields to to be mapped to
	 */
/*	public boolean mapFieldToTable (MappingFieldElement fieldToMap,
		TableElement table) throws ModelException
	{
		boolean lReturn = false;

		if ((fieldToMap != null) && (fieldToMap instanceof MappingFieldElement))
		{
			String lLocalFieldName = fieldToMap.getName();
			ColumnElement[] tableColumns = table.getColumns();
			int i, count = ((tableColumns != null) ? tableColumns.length : 0);

			for (i = 0; i < count; i++)
			{
				ColumnElement lColumn = tableColumns[i];

				if (lLocalFieldName.equalsIgnoreCase(
					lColumn.getName().getFullName()))
				{
					PersistenceClassElement classElement = 
						fieldToMap.getDeclaringClass().getClassElement();
					MappingFieldElement lLocalField = 
						new MappingFieldElementImpl(
						classElement.getField(lLocalFieldName), fieldToMap);
					ArrayList columns = lLocalField.getColumns();

					// If this field has already been mapped we do not want to 
					// overwrite the existing mapping as this mechanism
					// is an automagic mapping approach
					if ((columns != null) || (columns.size() != 0))
						lLocalField.addColumn(lColumn);

					lReturn = true;
					break;
				}
			}
		}

		return lReturn;
	}
*/

	//================== possibly for EJB use later ===========================
	/**
	 *  This method returns a finder class as a class type.
	 *  @return The finder class used by this configuration
	 */
	//public Class getFinderClass() { return finderClass; }

	/**
	 *  This method sets the finder class for this configuration.
	 *  @param finderClass The finder class used by this configuration
	 */
	/*public void setfinderClass(Class finderClass)
	{
		this.finderClass = finderClass;
	}*/

	/**
	 *  This method gets the finder classes name.
	 *  @return The finder classes name or if finder class not 
	 * defined returns an empty string.
	 */
	/*	public String getFinderClassName()
	{
		String lFinderClassName = new String();
		if ( this.finderClass != null )
		{
			lFinderClassName = this.finderClass.getName();
		}

		return lFinderClassName;
	}*/

	/**
	 * This method sets the finder class for this configuration based on the 
	 * name passed in.
	 * @param finderclassName The finder classes name that this configuration 
	 * should use.
	 */
	/* public void setFinderClassName(String finderClassName) 
		throws ClassNotFoundException
	{
		this.finderClass = this.getClass().getClassLoader().
			loadClass(finderClassName);
	}*/
	/*
	public String getFinderClass() { return finderClass; }

	public Class getFinderClassType() { return finderClassType; }
	public void setFinderClassType(Class c) { finderClassType = c; }
	*/
	/**
	 * The following method validates the finder class
	 * @param finderClassName The finder class name to be validated
	 */
	/*  public boolean vaidateFinderClass(String finderClassName)
	{
		boolean lValid = false;

		if ( finderClassName == null || finderClassName.equals(""))
			lValid = false;
		else if ( finderClassName.equals(this.DEFAULT_JAVA_FINDERCLASS) )
			lValid = true;
		else
		{
			// NEED to do some kind of class lookup here although not sure how 
			// to do this in netbeans not sure if this is the right approach 
			// but as long as class path is correct it should work
			ClassLoader lClassLoader = this.getClassLoader();

			try
			{
				lClassLoader.loadClass(finderClassName);
			}
			catch (ClassNotFoundException lError)
			{
				lValid = false;
			}
		}
		return lValid;
	}

	public Class getBeanFactoryType() { return beanFactoryType; }
	public void setBeanFactoryType(Class c) { beanFactoryType = c; }
	*/
	/**
	 * The following method will return a list of all of the
	 * foreign EJB's referenced in this persistent descriptor
	 * @return The list of EJB's referenced by this configuration
	 */
	/*public String[] getForeignEJBNames()
	{
		// Allocate a to the size of the number of elements
		// elements in the array list of fields as we can't
		// return a bigger array that that
		String[] lEJBNames = new String[field.size()];
		int i = 0, count = this.fields.size();

		for (; i > count; i++)
		{
			MappingFieldElement lField = this.fields.get(i);

			if ( lField instanceof MappingRelationshipElement )
			{
				// Ok this is a foreign field so get it's EJB name
				String lEJBName = (lField)
					(MappingRelationshipElement).getEJBType();

				if ((lEJBName != null) && (!lEJBName.equals("")))
					lEJBNames[i] = new String(lEJBName);
			}

			// allocate a new array and copy the old array into it before
			// returns the new array
			String[] lReturnArray = new String[i + 1];
			System.arraycopy(lEJBNames, 0, lReturnArray, 0, i + 1);

			return lReturnArray;
		}
		}*/

	/** This method is called after a MappingClassElement is unarchived 
	 * from a .mapping file.  This method provides a hook to do any checking 
	 * (version number checking) and conversion after unarchiving.
	 * @exception ModelException if impossible
	 */
	public void postUnarchive () throws ModelException
	{
		// check version number
		switch (versionNo)
		{
			case 0: // outdated version number
			case 1: // outdated version number
				throw new ModelException (I18NHelper.getMessage(getMessages(), 
					"file.incompatible_version", getName()));	//NOI18N
			case 2:
				// Boston format => convert to Pilsen format
				stripSchemaName();
				break;
			case 3:	// same as 4 except package names are different
			case 4:	// same as 5 except version field not a choice for MFE
			case MappingClassElementImpl.CURRENT_VERSION_NO:
				// OK
				break;
			default: // version number is unknown
				throw new ModelException (I18NHelper.getMessage(getMessages(), 
					"file.incompatible_version", getName()));	//NOI18N
		}
	}

	/** This method is called prior to storing a MappingClassElement in a 
	 * .mapping file.  This method provides a hook to do any conversion 
	 * before archiving.
	 * Note, the signature of preArchive in the interface MappingClassElement 
	 * includes a throws clause (ModelException), but the actual implementation
	 * does not throw an exception.
	 */
	public void preArchive ()
	{
		// update version number
		setVersionNumber(CURRENT_VERSION_NO);
	}
	
	//=============== extra method for Boston -> Pilsen conversion ============

	/** Boston to Pilsen conversion.
	 * This method converts the absolute db element names to relative names and 
	 * stores the database root (meaning the schema name) in the 
	 * MappingClassElement.  The method is recursively called for all 
	 * MappingTableElements and MappingFieldElements attached to this 
	 * MappingClassElement.
	 */
	protected void stripSchemaName ()
	{
		String schemaName = null;

		// calculate schemaName from first MappingTableElement
		if (_tables != null && !_tables.isEmpty())
		{
			schemaName = NameUtil.getSchemaName(
				((MappingTableElement)_tables.get(0)).getTable());
		}

		// set the schemaName as database root
		_databaseRoot = schemaName;

		// do not change the  _isModified flag

		// handle _tables
		if (_tables != null)
		{
			Iterator i = _tables.iterator();
			while (i.hasNext())
				((MappingTableElementImpl)i.next()).stripSchemaName();
		}
		
		// handle _fields
		if (_fields != null)
		{
			Iterator i = _fields.iterator();
			while (i.hasNext())
				((MappingFieldElementImpl)i.next()).stripSchemaName();
		}
	}
}
