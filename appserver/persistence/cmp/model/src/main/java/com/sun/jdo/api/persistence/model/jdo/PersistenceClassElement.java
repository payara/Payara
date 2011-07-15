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
 * PersistenceClassElement.java
 *
 * Created on February 28, 2000, 4:09 PM
 */

package com.sun.jdo.api.persistence.model.jdo;

import java.util.ArrayList;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import org.glassfish.persistence.common.I18NHelper;

/* TODO:
	1. throw ModelException on add duplicate field or concurrency group (will 
	 need I18N for message)
	2. review javadoc, links for get/set methods
 */

/** 
 *
 * @author raccah
 * @version %I%
 */
public class PersistenceClassElement extends PersistenceElement 
	implements FieldElementHolder
{
	/** Constant representing jdo identity managed by the application. */
	public static final int APPLICATION_IDENTITY = 0;

	/** Constant representing jdo identity managed by the database. */
	public static final int DATABASE_IDENTITY = 1;

	/** Constant representing unmanaged jdo identity. */
	public static final int UNMANAGED_IDENTITY = 2;
    
	/** Create new PersistenceClassElement with no implementation. 
	 * This constructor should only be used for cloning and archiving.
	 */
	public PersistenceClassElement ()
	{
		this(null);
	}

	/** Create new PersistenceClassElement with the provided implementation. The
	 * implementation is responsible for storing all properties of the object.
	 * @param impl the implementation to use
	 */
	public PersistenceClassElement (PersistenceClassElement.Impl impl)
	{
		super(impl);
	}

	/** Returns the persistence class element associated with the class with  
	 * the given string name, using the given model object to look it up.
	 * @param name the fully qualified name of the desired class
	 * @param model the model object to be used to look it up
	 * @return persistence class element representing the desired class
	 */
	public static PersistenceClassElement forName (String name, Model model)
	{
		return model.getPersistenceClass(name);
	}

	/** @return implemetation factory for this class
	 */
	final Impl getClassImpl () { return (Impl)getImpl(); }

	/** Get the package name of this class element.
	 * @return the package
	 * @see PersistenceElement#getName
	 */
	public String getPackage ()
	{
		String className = getName();
		int index = className.lastIndexOf('.');

		return ((index != -1) ? className.substring(0, index) : "");	// NOI18N
	}

	/** Gets the modified flag for this persistence class.
	 * @return <code>true</code> if there have been (property) changes to this 
	 * class, <code>false</code> otherwise.
	 */
	public boolean isModified () { return getClassImpl().isModified(); }

	/** Set the modified flag for this persistence class to flag.  This is  
	 * usually set to <code>true</code> by property changes and 
	 * <code>false</code> after a save.
	 * @param flag if <code>true</code>, this class is marked as modified;
	 * if <code>false</code>, it is marked as unmodified.
	 */
	public void setModified (boolean flag) { getClassImpl().setModified(flag); }

	/** Get the object identity type of this class element.
	 * @return the object identity type, one of {@link #APPLICATION_IDENTITY}, 
	 * {@link #DATABASE_IDENTITY}, or {@link #UNMANAGED_IDENTITY}
	 */
	public int getObjectIdentityType ()
	{
		return getClassImpl().getObjectIdentityType();
	}

	/** Set the object identity type of this class element.
	 * @param type - an integer indicating the object identity type, one of:
	 * {@link #APPLICATION_IDENTITY}, {@link #DATABASE_IDENTITY}, or 
	 * {@link #UNMANAGED_IDENTITY}
	 * @exception ModelException if impossible
	 */
	public void setObjectIdentityType (int type) throws ModelException
	{
		getClassImpl().setObjectIdentityType(type);
	}

	/** Get the fully qualified name of the primary key class for this class 
	 * element.  This value is only used if <code>getObjectIdentityType</code> 
	 * returns <code>APPLICATION_IDENTITY</code>
	 * @return the fully qualified key class name, <code>null</code> if the 
	 * identity type is not managed by the application
	 * @see #setObjectIdentityType
	 * @see #APPLICATION_IDENTITY
	 * 
	 */
	public String getKeyClass () { return getClassImpl().getKeyClass(); }

	/** Set the primary key class for this class element.  For now the key 
	 * class is required to be in the same package with the same name as this 
	 * class and the suffix "Key" (any capitalization), or an inner class of 
	 * this class with the name "OID" (any capitalization).
	 * @param name - the fully qualified name which represents the primary key 
	 * class.  This value is only used if <code>getObjectIdentityType</code> 
	 * returns <code>APPLICATION_IDENTITY</code>
	 * @exception ModelException if impossible
	 * @see #setObjectIdentityType
	 * @see #APPLICATION_IDENTITY
	 */
	public void setKeyClass (String name) throws ModelException
	{
		boolean hasValue = (name != null);

		if (hasValue)
			name = name.trim();

		if (hasValue && (name.length() > 0))
		{
			String className = getName();
			boolean hasPrefix = name.startsWith(className);
			String nameSuffix = ((hasPrefix) ? 
				name.substring(className.length()) : name);

			if (!hasPrefix || (!nameSuffix.equalsIgnoreCase("Key") && 	// NOI18N
				!nameSuffix.equalsIgnoreCase(".OID")					// NOI18N
				&& !nameSuffix.equalsIgnoreCase("$OID")))				// NOI18N
			{
				throw new ModelException(I18NHelper.getMessage(getMessages(),
					"jdo.class.key_class_invalid", 						// NOI18N
					new Object[]{name, className}));
			}
		}

		getClassImpl().setKeyClass(name);
	}

	/** Set the name of this persistence element.  This method overrides 
	 * the one in PersistenceElement in order to keep the 
	 * {@link #getKeyClass key class} in sync if possible.
	 * @param name the name
	 * @exception ModelException if impossible
	 */
	public void setName (String name) throws ModelException
	{
		String oldName = getName();

		super.setName(name);

		if (!StringHelper.isEmpty(name))
		{
			String oldKeyClass = getKeyClass();

			// a rename -- set the key class too
			if ((oldKeyClass != null) && oldKeyClass.startsWith(oldName))
				setKeyClass(name + oldKeyClass.substring(oldName.length()));
		}
	}

	//================== Fields ===============================
	// PersistenceFieldElement handling, implementation of FieldElementHolder

	/** Add the supplied field to the collection of fields maintained by this
	 * holder.
	 * @param field the field to be added
	 * @exception ModelException if impossible
	 */
	public void addField (PersistenceFieldElement field) 
		throws ModelException 
	{
		addFields(new PersistenceFieldElement[]{field});
	}

	/** Add the supplied fields to the collection of fields maintained by this
	 * holder.
	 * @param fields the array of fields to be added
	 * @exception ModelException if impossible
	 */
	public void addFields(PersistenceFieldElement[] fields)
		throws ModelException 
	{
		getClassImpl().changeFields(fields, Impl.ADD);
	}

	/** Remove the supplied field from the collection of fields maintained by
	 * this holder.
	 * @param field the field to be removed
	 * @exception ModelException if impossible
	 */
	public void removeField (PersistenceFieldElement field)
		throws ModelException 
	{
		removeFields(new PersistenceFieldElement[]{field});
	}

	/** Removed the supplied fields from the collection of fields maintained
	 * by this holder.
	 * @param fields the array of fields to be removed
	 * @exception ModelException if impossible
	 */
	public void removeFields (PersistenceFieldElement[] fields)
		throws ModelException 
	{
		int i, count = ((fields != null) ? fields.length : 0);

		// first remove the fields from this class
		getClassImpl().changeFields(fields, Impl.REMOVE);

		// now remove the fields from any concurrency groups
		for (i = 0; i < count; i++)
		{
			PersistenceFieldElement field = fields[i];
			ConcurrencyGroupElement[] groups = field.getConcurrencyGroups();
			int j, groupCount = ((groups != null) ? groups.length : 0);

			for (j = 0; j < groupCount; j++)
				groups[j].removeField(field);
		}	
	}

	/** Returns the collection of fields maintained by this holder in the form
	 * of an array.
	 * @return the fields maintained by this holder
	 */
	public PersistenceFieldElement[] getFields ()
	{
		return getClassImpl().getFields();
	}

	/** Sets the collection of fields maintained by this holder to the contents
	 * of the supplied array.
	 * @param fields the fields maintained by this holder
	 * @exception ModelException if impossible
	 */
	public void setFields (PersistenceFieldElement[] fields)
		throws ModelException 
	{
		getClassImpl().changeFields(fields, Impl.SET);
	}

	/** Returns the field with the supplied name from the collection of fields
	 * maintained by this holder.
	 * @param name the name of the field to be found
	 * @return the field with the supplied name, <code>null</code> if none 
	 * exists
	 */
	public PersistenceFieldElement getField (String name)
	{
		return getClassImpl().getField(name);
	}

	/** Tests whether the supplied field is in the collection of fields 
	 * maintained by this holder.
	 * @param field the field to be tested
	 */
	public boolean containsField (PersistenceFieldElement field)
	{
		return (getClassImpl().getField(field.getName()) != null);
	}

	//================== Relationships ===============================
	// convenience methods to access RelationshipElements

	/** Returns the subset of the collection of fields which are relationahips.
	 * @return the relationship fields maintained by this holder
	 * @see PersistenceClassElement#getFields
	 */
	public RelationshipElement[] getRelationships ()
	{
		PersistenceFieldElement[] fields = getFields();
		int i, count = ((fields != null) ? fields.length : 0);
		ArrayList relationships = new ArrayList(count);

		for (i = 0; i < count; i++)
		{
			PersistenceFieldElement field = fields[i];

			if (field instanceof RelationshipElement)
				relationships.add(field);
		}
		
		count = relationships.size();

		return ((RelationshipElement[])relationships.toArray(
			new RelationshipElement[count]));
	}

	/** Returns the relationship with the supplied name from the collection of 
	 * relationships maintained by this holder.
	 * @param name the name of the relationship to be found
	 * @return the relationship with the supplied name, <code>null</code> if 
	 * none exists
	 * @see PersistenceClassElement#getRelationships
	 * @see PersistenceClassElement#getField
	*/
	public RelationshipElement getRelationship (String name)
	{
		RelationshipElement[] relationships = getRelationships();
		int i, count = ((relationships != null) ? relationships.length : 0);
		
		for (i = 0; i < count; i++)
		{
			RelationshipElement relationship = relationships[i];

			if (name.equals(relationship.getName()))
				return relationship;
		}

		return null;
	}

	//================== ConcurrencyGroups ===============================
	// ConcurrencyGroupElement handling

	/** Add the supplied group to the collection of concurrency groups for this
	 * class.
	 * @param group the concurrency group to be added
	 * @exception ModelException if impossible
	 */
	public void addConcurrencyGroup (ConcurrencyGroupElement group)
		throws ModelException 
	{
		addConcurrencyGroups(new ConcurrencyGroupElement[]{group});
	}

	/** Add the supplied groups to the collection of concurrency groups for 
	 * this class.
	 * @param groups the array of concurrency groups to be added
	 * @exception ModelException if impossible
	 */
	public void addConcurrencyGroups (ConcurrencyGroupElement[] groups)
		throws ModelException 
	{
		getClassImpl().changeConcurrencyGroups(groups, Impl.ADD);
	}

	/** Remove the supplied group from the collection of concurrency groups for 
	 * this class.
	 * @param group the concurrency group to be removed
	 * @exception ModelException if impossible
	 */
	public void removeConcurrencyGroup (ConcurrencyGroupElement group)
		throws ModelException 
	{
		removeConcurrencyGroups(new ConcurrencyGroupElement[]{group});
	}

	/** Removed the supplied groups from the collection of concurrency groups 
	 * for this class.
	 * @param groups the array of concurrency groups to be removed
	 * @exception ModelException if impossible
	 */
	public void removeConcurrencyGroups (ConcurrencyGroupElement[] groups)
		throws ModelException 
	{
		getClassImpl().changeConcurrencyGroups(groups, Impl.REMOVE);
	}

	/** Returns the collection of fields groups by this class in the form
	 * of an array.
	 * @return the concurrency groups maintained by this class
	 */
	public ConcurrencyGroupElement[] getConcurrencyGroups ()
	{
		return getClassImpl().getConcurrencyGroups();
	}

	/** Sets the collection of concurrency groups maintained by this class to 
	 * the contents of the supplied array.
	 * @param groups the concurrency groups maintained by this holder
	 * @exception ModelException if impossible
	 */
	public void setConcurrencyGroups (ConcurrencyGroupElement[] groups)
		throws ModelException 
	{
		getClassImpl().changeConcurrencyGroups(groups, Impl.SET);
	}

	/** Returns the concurrency group with the supplied name from the 
	 * collection of groups maintained by this class.
	 * @param name the name of the concurrency group to be found
	 * @return the concurrency group with the supplied name, <code>null</code>
	 * if none exists
	 */
	public ConcurrencyGroupElement getConcurrencyGroup (String name)
	{
		return getClassImpl().getConcurrencyGroup(name);
	}

	/** Tests whether the supplied group is in the collection of groups 
	 * maintained by this class.
	 * @param group the concurrency group to be tested
	 */
	public boolean containsConcurrencyGroup (ConcurrencyGroupElement group)
	{
		return (getClassImpl().getConcurrencyGroup(group.getName()) != null);
	}

	// for later
	/* get/setFieldInheritanceFlag - This would allow a user to choose whether
	 * db inheritance is used (field persistence and behavior inherited from
	 * persistent superclass or not) or the list of the superclass fields could
	 * be configured for persistence in the subclass. 
	 * - applies only to fields and relationships
	 */

	/** Pluggable implementation of the storage of class element properties.
	 * @see PersistenceClassElement#PersistenceClassElement
	 */
	public interface Impl extends PersistenceElement.Impl
	{
		/** Gets the modified flag for this persistence class.
		 * @return <code>true</code> if there have been (property) changes to  
		 * this class, <code>false</code> otherwise.
		 */
		public boolean isModified ();

		/** Set the modified flag for this persistence class to flag.  This is  
		 * usually set to <code>true</code> by property changes and 
		 * <code>false</code> after a save.
		 * @param flag if <code>true</code>, this class is marked as modified;
		 * if <code>false</code>, it is marked as unmodified.
		 */
		public void setModified (boolean flag);

		/** Get the object identity type of this class element.
		 * @return the object identity type, one of 
		 * {@link #APPLICATION_IDENTITY}, {@link #DATABASE_IDENTITY}, or 
		 * {@link #UNMANAGED_IDENTITY}
		 */
		public int getObjectIdentityType ();

		/** Set the object identity type of this class element.
		 * @param type - an integer indicating the object identity type, one of:
		 * {@link #APPLICATION_IDENTITY}, {@link #DATABASE_IDENTITY}, or 
		 * {@link #UNMANAGED_IDENTITY}
		 * @exception ModelException if impossible
		 */
		public void setObjectIdentityType (int type) throws ModelException;

		/** Get the fully qualified name of the primary key class for this class 
		 * element.  This value is only used if 
		 * <code>getObjectIdentityType</code> returns 
		 * <code>APPLICATION_IDENTITY</code>
		 * @return the fully qualified key class name, <code>null</code> if the 
		 * identity type is not managed by the application
		 * @see #setObjectIdentityType
		 * @see #APPLICATION_IDENTITY
		 * 
		 */
		public String getKeyClass ();

		/** Set the primary key class for this class element.
		 * @param name - the fully qualified name which represents the primary  
		 * key class.  This value is only used if 
		 * <code>getObjectIdentityType</code> returns 
		 * <code>APPLICATION_IDENTITY</code>
		 * @exception ModelException if impossible
		 * @see #setObjectIdentityType
		 * @see #APPLICATION_IDENTITY
		 */
		public void setKeyClass (String name) throws ModelException;

		/** Change the set of fields.
		 * @param fields the new fields
		 * @param action {@link #ADD}, {@link #REMOVE}, or {@link #SET}
		 * @exception ModelException if impossible
		 */
		public void changeFields (PersistenceFieldElement[] fields, int action)
			throws ModelException;

		/** Get all fields.
		 * @return the fields
		 */
		public PersistenceFieldElement[] getFields ();

		/** Find a field by name.
		 * @param name the name to match
		 * @return the field, or <code>null</code> if it does not exist
		 */
		public PersistenceFieldElement getField (String name);

		/** Change the set of concurrency groups.
		 * @param groups the new concurrency groups
		 * @param action {@link #ADD}, {@link #REMOVE}, or {@link #SET}
		 * @exception ModelException if impossible
		 */
		public void changeConcurrencyGroups (ConcurrencyGroupElement[] groups, 
			int action) throws ModelException;

		/** Get all concurrency groups.
		 * @return the concurrency groups
		 */
		public ConcurrencyGroupElement[] getConcurrencyGroups ();

		/** Find a concurrency group by name.
		 * @param name the name to match
		 * @return the concurrency group, or <code>null</code> if it does not
		 * exist 
		 */
		public ConcurrencyGroupElement getConcurrencyGroup (String name);
	}
}
