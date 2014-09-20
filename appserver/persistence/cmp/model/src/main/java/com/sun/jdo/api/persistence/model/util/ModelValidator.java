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
 * ModelValidator.java
 *
 * Created on September 22, 2000, 12:49 PM
 */

package com.sun.jdo.api.persistence.model.util;

import java.util.*;
import java.lang.reflect.Modifier;

import org.netbeans.modules.dbschema.*;
import org.netbeans.modules.dbschema.util.NameUtil;
import org.netbeans.modules.dbschema.util.SQLTypeUtil;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.api.persistence.model.mapping.*;
import com.sun.jdo.spi.persistence.utility.*;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author Rochelle Raccah
 * @version %I%
 */
public class ModelValidator
{
	/** This field holds the model object used for validation */
	private Model _model;

	/** This field holds the name of the class being validated */
	private String _className;

	/** This field holds the class loader used to load class 
	 * being validated (if available).
	 */
	private ClassLoader _classLoader;

	/** I18N message handler */
	private ResourceBundle _messages;

	public ModelValidator (Model model, String className, ResourceBundle bundle)
	{
		this(model, className, null, bundle);
	}

 	/** Create a new model validator object.
	 * @param model model object used for validation
	 * @param className the name of the class being validated
	 */
	public ModelValidator (Model model, String className, 
		ClassLoader classLoader, ResourceBundle bundle)
	{
		_model = model;
		_className = className;
		_classLoader = classLoader;
		_messages = bundle;
	}

	/**
	 * Get the model object used for validation.
	 * @return the model object used for validation
	 */
	public Model getModel () { return _model; }

	/**
	 * Get the name of the class being validated.
	 * @return the name of the class being validated
	 */
	public String getClassName () { return _className; }

	/**
	 * Get the class loader used to load the class being validated.
	 * @return the class loader of the class being validated
	 */
	public ClassLoader getClassLoader () { return _classLoader; }

	/** @return I18N message handler for this element
	 */
	protected ResourceBundle getMessages () { return _messages; }

	/** Main method used for parsing the combination of java (or class) 
	 * information and mapping/jdo information by running through a subset 
	 * of the full validation check and aborting (and returning 
	 * <code>false</code> at the first error or warning.
	 * @return <code>true</code> if no errors or warnings occur, 
	 * <code>false</code> otherwise.
	 * @see #getBasicValidationList
	 */
	public boolean parseCheck ()
	{
		Iterator iterator = getBasicValidationList().iterator();

		try
		{
			while (iterator.hasNext())
				((ValidationComponent)iterator.next()).validate();
		}
		catch (ModelValidationException e)
		{
			LogHelperModel.getLogger().log(Logger.FINER, 
				"model.parse_error", e);	// NOI18N

			return false;
		}

		return true;
	}

	/** Main method used for validating the combination of java (or class) 
	 * information and mapping/jdo information by running through the full 
	 * validation check and returning a collection of 
	 * ModelValidationExceptions containing any errors or warnings encountered.
	 * @return a collection of ModelValidationExceptions containing any 
	 * errors or warnings encountered.  If no errors or warnings were
	 * encountered, the collection will be empty, not <code>null</code>.
	 * @see #getFullValidationList
	 */
	public Collection fullValidationCheck ()
	{
		ArrayList list = new ArrayList();
		Iterator iterator = getFullValidationList().iterator();

		while (iterator.hasNext())
		{
			try
			{
				((ValidationComponent)iterator.next()).validate();
			}
			catch (ModelValidationException e)
			{
				list.add(e);
			}
		}

		return Collections.unmodifiableCollection(list);
	}

	// ================ Validation list construction methods ===============

	/** Computes and returns a collection of ValidationComponents 
	 * representing the tests to be performed during parse.
	 * @return a collection of ValidationComponents representing the 
	 * tests to be performed during parse.
	 * @see #getDatabaseValidationList
	 * @see #getFieldsValidationList
	 * @see #getFullValidationList
	 */
	public Collection getBasicValidationList ()
	{
		ArrayList list = new ArrayList();
		String className = getClassName();

		list.add(createClassExistenceComponent(className));
		list.add(createClassPersistenceComponent(className));

		list.addAll(getDatabaseValidationList());
		list.addAll(getFieldsValidationList());

		return Collections.unmodifiableCollection(list);
	}
	
	/** Computes and returns a collection of ValidationComponents 
	 * representing the tests to be performed during validation.  These 
	 * include all those in the basic list plus those which check 
	 * cardinality and the related classes in more detail.
	 * @return a collection of ValidationComponents representing the 
	 * tests to be performed during validation.
	 * @see #getRelatedClassValidationList
	 * @see #getBasicValidationList
	 */
	public Collection getFullValidationList ()
	{
		ArrayList list = new ArrayList(getBasicValidationList());
		String className = getClassName();
		PersistenceClassElement persistenceClass = 
			getPersistenceClass(className);

		if (persistenceClass != null)
		{
			PersistenceFieldElement[] fields = persistenceClass.getFields();
			int i, count = ((fields != null) ? fields.length : 0);

			list.add(createSerializableClassComponent(className));
			list.add(createKeyClassComponent(persistenceClass.getKeyClass()));
			list.add(createClassMappingComponent(persistenceClass));
			list.add(createKeyColumnMappingComponent(persistenceClass));

			for (i = 0; i < count; i++)
			{
				PersistenceFieldElement field = fields[i];

				list.add(createFieldCardinalityComponent(field));
				list.add(createFieldMappingComponent(field));
				list.add(createFieldBlobMappingComponent(field));
				list.addAll(getRelatedClassValidationList(field));
			}
		}

		return Collections.unmodifiableCollection(list);
	}

	// ============= Validation list construction suppport methods ============

	/** Computes and returns a collection of ValidationComponents 
	 * representing the database tests to be performed.
	 * @return a collection of ValidationComponents representing the 
	 * database tests to be performed.
	 */
	private Collection getDatabaseValidationList ()
	{
		ArrayList list = new ArrayList();
		String className = getClassName();
		MappingClassElement mappingClass = getMappingClass(className);

		if (mappingClass != null)
		{
			ArrayList tables = mappingClass.getTables();
			int i, count = ((tables != null) ? tables.size() : 0);
			MappingTableElement primaryTable = null;
			Iterator iterator = null;

			list.add(createSchemaExistenceComponent(className));

			for (i = 0; i < count; i++)
			{
				MappingTableElement nextTable = 
					(MappingTableElement)tables.get(i);

				list.add(createTableExistenceComponent(nextTable.getTable()));

				if (i == 0)
				{
					primaryTable = nextTable;
					list.add(createPrimaryTableComponent(primaryTable));
				}
				else
				{
					MappingReferenceKeyElement referenceKey = 
						findReferenceKey(primaryTable, nextTable);

					if (referenceKey != null)
					{
						iterator = referenceKey.getColumnPairNames().iterator();
						while (iterator.hasNext())
						{
							list.add(createColumnExistenceComponent(
								(String)iterator.next()));
						}
					}
				}
			}

			list.add(createVersionConsistencyComponent(mappingClass));

			iterator = mappingClass.getFields().iterator();
			while (iterator.hasNext())
			{
				MappingFieldElement nextField = 
					(MappingFieldElement)iterator.next();
				ArrayList allColumns = new ArrayList();
				Iterator columnIterator = null;

				if (isRelationship(nextField))
				{
					allColumns.addAll(((MappingRelationshipElement)nextField).
						getAssociatedColumns());
				}

				allColumns.addAll(nextField.getColumns());

				columnIterator = allColumns.iterator();
				while (columnIterator.hasNext())
				{
					list.add(createColumnExistenceComponent(
						(String)columnIterator.next(), nextField));
				}
			}
		}

		return list;
	}

	/** Computes and returns a collection of ValidationComponents 
	 * representing the field and relationship tests to be performed.
	 * @return a collection of ValidationComponents representing the 
	 * field and relationship tests to be performed.
	 */
	private Collection getFieldsValidationList ()
	{
		ArrayList list = new ArrayList();
		Model model = getModel();
		String className = getClassName();
		PersistenceClassElement persistenceClass = 
			getPersistenceClass(className);

		if (persistenceClass != null)
		{
			PersistenceFieldElement[] fields = persistenceClass.getFields();
			int i, count = ((fields != null) ? fields.length : 0);
			Iterator iterator = 
				getMappingClass(className).getFields().iterator();

			for (i = 0; i < count; i++)
			{
				PersistenceFieldElement field = fields[i];

				list.add(createFieldExistenceComponent(field));

				// even though this is really the validation step, we 
				// only want to add the others if the field exists
				if (model.hasField(className, field.getName()))
				{
					list.add(createFieldPersistenceComponent(field));
					list.add(createFieldPersistenceTypeComponent(field));
					list.add(createFieldConsistencyComponent(field));

					if (isLegalRelationship(field))
					{
						RelationshipElement rel = (RelationshipElement)field;

						/* user modifiable collection class not yet supported
						list.add(createCollectionClassComponent(rel));*/
						list.add(createElementClassComponent(rel));
						list.add(createRelatedClassMatchesComponent(rel));
					}
				}
			}

			while (iterator.hasNext())
			{
				MappingFieldElement field = 
					(MappingFieldElement)iterator.next();
				String fieldName = field.getName();

				// only check this if it is not in the jdo model
				if (persistenceClass.getField(fieldName) == null)
				{
					list.add(createFieldExistenceComponent(field));

					// even though this is really the validation step, we 
					// only want to add the others if the field exists
					if (model.hasField(className, fieldName))
						list.add(createFieldConsistencyComponent(field));
				}

				if (!isRelationship(field))
					list.add(createColumnOverlapComponent(field));

				// preliminary fix for CR6239630
				if (Boolean.getBoolean("AllowManagedFieldsInDefaultFetchGroup")) // NOI18N
		 		{
                                    // Do nothing - AllowManagedFieldsInDefaultFetchGroup: 
                                    // disabled single model validation test; 
                                    // may use checked read/write access to managed fields
				}
				else
				{
					list.add(createFieldDefaultFetchGroupComponent(field));
				}
			}
		}

		return list;
	}

	/** Computes and returns a collection of ValidationComponents 
	 * representing the related class tests to be performed.  Right now, 
	 * these are only included as part of full validation, as they may 
	 * be somewhat time intensive since they compute information about 
	 * other classes as well as this class.
	 * @return a collection of ValidationComponents representing the 
	 * related class tests to be performed.
	 */
	private Collection getRelatedClassValidationList (
		PersistenceFieldElement field)
	{
		String relatedClass = getRelatedClass(field);
		ArrayList list = new ArrayList();

		// even though this is really already included in the validation 
		// step, we only want to add the extra steps if the field exists
		if ((relatedClass != null) && 
			getModel().hasField(getClassName(), field.getName()))
		{
			MappingClassElement relatedClassElement = 
				getMappingClass(relatedClass);

			list.add(createClassExistenceComponent(relatedClass, field));
			list.add(createClassPersistenceComponent(relatedClass, field));
			list.add(createSchemaExistenceComponent(relatedClass, field));
			list.add(createRelatedSchemaMatchesComponent(relatedClass, field));

			if (relatedClassElement != null)
			{
				ArrayList tables = relatedClassElement.getTables();
				MappingTableElement primaryTable = null;
				boolean hasTables = ((tables != null) && (tables.size() > 0));

				if (hasTables)
				{
					primaryTable = (MappingTableElement)tables.get(0);
					list.add(createTableExistenceComponent(
						primaryTable.getTable(), field));
				}

				if (isRelationship(field))
				{
					RelationshipElement relElement = (RelationshipElement)field;
					Object rel = getMappingClass(getClassName()).
						getField(field.getName());

					list.add(createInverseFieldComponent(relElement));
					list.add(createInverseMappingComponent(relElement));

					// verify that the columns from the primary table 
					// of the related class are actually from that table
					// since it could have been changed
					if ((rel != null) && isRelationship(rel))
					{
						MappingRelationshipElement relationship = 
							(MappingRelationshipElement)rel;
						ArrayList columns = 
							relationship.getAssociatedColumns();
						Iterator iterator = null;

						if ((columns == null) || (columns.size() == 0))
							columns = relationship.getColumns();

						if (columns != null)
						{
							List tableNames = new ArrayList();

							if (hasTables)
							{
								Iterator tableIterator = tables.iterator();

								while (tableIterator.hasNext())
								{
									tableNames.add(((MappingTableElement)
										tableIterator.next()).getName());
								}
							}

							iterator = columns.iterator();

							while (iterator.hasNext())
							{
								list.add(createRelatedTableMatchesComponent(
									relatedClass, field, tableNames, 
									(String)iterator.next()));
							}
						}
					}
				}
			}
		}

		return list;
	}

	// ================ Validation Component inner classes ===============

	/** Create a validation component which can check whether the class exists.
	 * @param className the class whose existence is being checked
	 * @param relatedField the relationship field whose class is being checked,
	 * may be <code>null</code> in which case we are probably checking the 
	 * same class as the validator is checking overall
	 * @return the validation component
	 */
	protected ValidationComponent createClassExistenceComponent (
		final String className, final PersistenceFieldElement relatedField)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if ((className == null) || 
					!getModel().hasClass(className, getClassLoader()))
				{
					throw constructClassException(className, relatedField, 
						"util.validation.class_not_found");		//NOI18N
				}
			}
		};
	}

	/** Create a validation component which can check whether the class exists.
	 * @param className the class whose existence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createClassExistenceComponent (
		final String className)
	{
		return createClassExistenceComponent(className, null);
	}

	/** Create a validation component which can check the class persistence.
	 * @param className the class whose persistence is being checked
	 * @param relatedField the relationship field whose class is being checked,
	 * may be <code>null</code> in which case we are probably checking the 
	 * same class as the validator is checking overall
	 * @return the validation component
	 */
	protected ValidationComponent createClassPersistenceComponent (
		final String className, final PersistenceFieldElement relatedField)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				Model model = getModel();
	
				if ((className != null) && 
					 model.hasClass(className, getClassLoader()))
				{
					String key = null;

					if (!isPersistent(className))
						key = "util.validation.class_not_persistence_capable";//NOI18N
					else if (!model.isPersistenceCapableAllowed(className))
						key = "util.validation.class_not_allowed";//NOI18N

					if (key != null)
					{
						throw constructClassException(
							className, relatedField, key);
					}
				}
			}
		};
	}

	/** Create a validation component which can check the class persistence.
	 * @param className the class whose persistence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createClassPersistenceComponent (
		final String className)
	{
		return createClassPersistenceComponent(className, null);
	}

	/** Create a validation component which can check whether the field exists.
	 * @param fieldName the field whose existence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldExistenceComponent (
		final String fieldName)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (!getModel().hasField(getClassName(), fieldName))
				{
					throw constructFieldException(fieldName, 
						"util.validation.field_not_found");			//NOI18N
				}
			}
		};
	}

	/** Create a validation component which can check whether the field exists.
	 * @param field the field whose existence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldExistenceComponent (Object field)
	{
		return createFieldExistenceComponent(field.toString());
	}

	/** Create a validation component which can check whether the field is
	 * persistent.
	 * @param field the field whose persistence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldPersistenceComponent (
		final PersistenceFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				boolean isPersistent = (PersistenceFieldElement.PERSISTENT == 
					field.getPersistenceType());
				String fieldName = field.getName();

				if (isPersistent && 
					!isPersistentAllowed(getClassName(), fieldName))
				{
					throw constructFieldException(fieldName, 
						"util.validation.field_persistent_not_allowed");//NOI18N
				}
			}
		};
	}

	/** Create a validation component which can check whether the field is
	 * consistent (field in both models or relationship in both).
	 * @param field the field whose consistency is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldConsistencyComponent (
		final PersistenceFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String fieldName = field.getName();
				String className = getClassName();
				boolean isLegallyPersistent = 
					isPersistentAllowed(className, fieldName);

				if (isLegallyPersistent)
				{
					MappingClassElement mappingClass = 
						getMappingClass(className);
					MappingFieldElement mappingElement = 
						((mappingClass != null) ? 
						mappingClass.getField(fieldName) : null);

					if (mappingElement != null)
					{
						boolean jdoIsRelationship = isLegalRelationship(field);

						if (jdoIsRelationship != isRelationship(mappingElement))
						{
							throw constructFieldException(fieldName, 
								"util.validation.field_type_inconsistent");	//NOI18N
						}
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the field is
	 * consistent (if in mapping model but not jdo, it is a problem).
	 * @param field the field whose consistency is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldConsistencyComponent (
		final MappingFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (field != null)
				{
					String fieldName = field.getName();
					PersistenceClassElement persistenceClass = 
						getPersistenceClass(getClassName());
					PersistenceFieldElement persistenceElement = 
						((persistenceClass != null) ? 
						persistenceClass.getField(fieldName) : null);

					if (persistenceElement == null)
					{
						throw constructFieldException(fieldName, 
							"util.validation.field_model_inconsistent");//NOI18N
					}
				}
			}
		};
	}

	/** Create a validation component which can check the persistence type 
	 * of the field (whether it is a relationship or not).
	 * @param field the field whose persistence type is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldPersistenceTypeComponent (
		final PersistenceFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String fieldName = field.getName();
				String className = getClassName();
				boolean isLegallyPersistent = 
					isPersistentAllowed(className, fieldName);

				if (isLegallyPersistent)
				{
					boolean isRelationship = isRelationship(field);
					boolean mustBeRelationship = shouldBeRelationship(field);

					if (isRelationship && !mustBeRelationship)
					{
						throw constructFieldException(fieldName, 
							"util.validation.field_relationship_not_allowed");//NOI18N
					}
					else if (!isRelationship && mustBeRelationship)
					{
						throw constructFieldException(fieldName, 
							"util.validation.field_type_not_allowed");	//NOI18N
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the cardinality 
	 * bounds are semantically valid given the relationship field type.
	 * @param field the relationship whose cardinality bounds are being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldCardinalityComponent (
		final PersistenceFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (isLegalRelationship(field))
				{
					RelationshipElement relationship =
						(RelationshipElement)field;
					String fieldName = field.getName();
					boolean nonCollectionRelationship = 
						!isCollection(getClassName(), fieldName);
					int upperBound = (nonCollectionRelationship ?
						1 : relationship.getUpperBound());
					int lowerBound = relationship.getLowerBound();
					MappingRelationshipElement mapping = null;

					if ((lowerBound < 0) || (upperBound <= 0) || 
						(lowerBound > upperBound))
					{
						throw constructFieldException(fieldName, 
							"util.validation.cardinality_invalid");	//NOI18N
					}

					// now check specific lower bound requirements imposed
					// by the mapping
					mapping = getMappingRelationship(relationship);
					if (nonCollectionRelationship && (lowerBound != 1) && 
						(mapping != null) && !isJoin(mapping))
					{
						// If the non-collection relationship field is exactly
						// mapped to a FK, we need to check the nullability
						// of the columns. If there are any non-nullable
						// columns the lower bound must be 1.
						ForeignKeyElement fk = getMatchingFK(mapping);

						if ((fk != null) && hasNonNullableColumn(fk))
						{
							throw constructFieldException(fieldName,
								"util.validation.lower_bound_invalid"); //NOI18N
						}
					}
				}
			}

			/** Returns <code>true</code> if the specified FK has at least
			 * one non-nullable column. Please note that the caller is
			 * responsible for passing a non-null fk argument.
			 */
			private boolean hasNonNullableColumn (ForeignKeyElement fk)
			{
				ColumnElement[] localColumns = fk.getLocalColumns();
				int count = ((localColumns != null) ? localColumns.length : 0);
				
				for (int i = 0; i < count; i++)
				{
					if (!localColumns[i].isNullable())
						return true;
				}

				return false;
			}

			/** Checks whether the specified relationship is exactly mapped
			 * to a FK. If yes, the method returns the matching FK. If not, 
			 * it returns <code>null</code>. Please note that the caller is
			 * responsible for passing a non-null mapping argument.
			 */
 			private ForeignKeyElement getMatchingFK (
				MappingRelationshipElement mapping)
			{
				MappingClassElement mappingClass = mapping.
					getDeclaringClass();
				String databaseRoot = getSchemaForClass(getClassName());
				List pairNames = mapping.getColumns();
				List tables = mappingClass.getTables();
					
				if (tables != null)
				{
					for (Iterator i = tables.iterator(); i.hasNext();)
					{
						String tableName = ((MappingTableElement)i.next()).
							getName();
						TableElement table = getTable(tableName, databaseRoot);
						ForeignKeyElement fk = getMatchingFK(pairNames, table);
						
						if (fk != null)
							return fk;
					}
				}

				return null;
			}

			/** Checks whether the specified TableElement has a FK that
			 * exactly matches the list of column pair names. 
			 * @return the matching FK if it exactly matches the list 
			 * of column pairs; <code>null</code> otherwise. 
			 */
			private ForeignKeyElement getMatchingFK (List pairNames, 
				TableElement table)
			{
				ForeignKeyElement[] foreignKeys = (table != null) ? 
					table.getForeignKeys() : null;
				int count = ((foreignKeys != null) ? foreignKeys.length : 0);

				for (int i = 0; i < count; i++)
				{
					if (matchesFK(pairNames, foreignKeys[i]))
						return foreignKeys[i];
				}

				return null;
			}

			/** Returns <code>true</code> if the specified list of column
			 * pair names matches exactly the specified FK. 
			 */
			private boolean matchesFK (List pairNames, 
				ForeignKeyElement foreignKey)
			{
				ColumnPairElement[] fkPairs = foreignKey.getColumnPairs();
				int fkCount = ((fkPairs != null) ? fkPairs.length : 0);
				int count = ((pairNames != null) ? pairNames.size() : 0);

				// First check whether the list of fk column pairs has the 
				// same size than the specified list of columns.
				if (fkCount == count) 
				{
					// Now check whether each fk column is included in the
					// specified list of columns.
					for (int i = 0; i < fkCount; i++)
					{
						String fkPairName = NameUtil.getRelativeMemberName(
							fkPairs[i].getName().getFullName());

						if (!pairNames.contains(fkPairName))
							return false;
					}

					return true;
				}

				return false;
			}
		};
	}

	/** Create a validation component which can check whether the field is  
	 * unmapped.
	 * @param field the field whose mapping is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldMappingComponent (
		final PersistenceFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String fieldName = field.getName();
				MappingClassElement mappingClass = 
					getMappingClass(getClassName());
 
				if ((mappingClass != null) && 
					(mappingClass.getTables().size() > 0))
				{
					MappingFieldElement mappingField = 
						mappingClass.getField(fieldName);

					if ((mappingField == null) || 
						(mappingField.getColumns().size() == 0))
					{
						throw constructFieldException(
							ModelValidationException.WARNING, fieldName, 
							"util.validation.field_not_mapped");	//NOI18N
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the field is  
	 * mapped to a blob type and if so, whether it is a key field or belongs 
	 * to the default fetch group.  Note that it's somewhat important to check
	 * for the key field first because if a field is key, its fetch group  
	 * value in the model is ignored.
	 * @param field the field whose mapping/key field/fetch group consistency 
	 * is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldBlobMappingComponent (
		final PersistenceFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String className = getClassName();
				String fieldName = field.getName();
				MappingClassElement mappingClass = getMappingClass(className);
				MappingFieldElement mappingField = ((mappingClass != null) ? 
					mappingClass.getField(fieldName) : null);

				if (mappingField != null)
				{
					boolean isKey = field.isKey();

 					if (isKey || (MappingFieldElement.GROUP_DEFAULT == 
						mappingField.getFetchGroup()))
					{
						if (isMappedToBlob(mappingField, 
							getSchemaForClass(className)))
						{
							throw constructFieldException(fieldName, (isKey ? 
								"util.validation.field_key_field_not_allowed" : //NOI18N
								"util.validation.field_fetch_group_not_allowed")); // NOI18N
						}
					}
				}
			}
			private boolean isMappedToBlob (MappingFieldElement mappingField, 
				String schema)
			{
				if (mappingField instanceof MappingRelationshipElement)
				{
					return isMappedToBlob(
						(MappingRelationshipElement)mappingField, schema);
				}
				else
				{
					Iterator iterator = mappingField.getColumns().iterator();

					while (iterator.hasNext())
					{
						String absoluteName = NameUtil.getAbsoluteMemberName(
							schema, (String)iterator.next());
						TableElement table = TableElement.forName(
							NameUtil.getTableName(absoluteName));
						ColumnElement columnElement = ((table != null) ?
							(ColumnElement)table.getMember(
							DBIdentifier.create(absoluteName)) : null);

						if (isMappedToBlob(columnElement))
							return true;
					}
				}

				return false;
			}
			private boolean isMappedToBlob (MappingRelationshipElement rel, 
				String schema)
			{
				Iterator iterator = rel.getColumns().iterator();

				while (iterator.hasNext())
				{
					ColumnPairElement pair = 
						getPair((String)iterator.next(), schema);

					if (isMappedToBlob(pair))
						return true;
				}

				// now check join columns
				iterator = rel.getAssociatedColumns().iterator();
				while (iterator.hasNext())
				{
					ColumnPairElement pair = 
						getPair((String)iterator.next(), schema);

					if (isMappedToBlob(pair))
						return true;
				}

				return false;
			}
			private boolean isMappedToBlob (ColumnPairElement pair)
			{
				return ((pair == null) ? false : 
					isMappedToBlob(pair.getLocalColumn()) && 
					isMappedToBlob(pair.getReferencedColumn()));
			}
			private boolean isMappedToBlob (ColumnElement column)
			{
				return ((column != null) && 
					SQLTypeUtil.isBlob(column.getType()));
			}
		};
	}

	/** Create a validation component which can check whether the collection 
	 * class is valid given the relationship field type.
	 * @param field the relationship whose collection class is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createCollectionClassComponent (
		final RelationshipElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String className = getClassName();
				String fieldName = field.getName();

				if (isCollection(className, fieldName))
				{
					Model model = getModel();
					String collectionClass = field.getCollectionClass();
					String fieldType = model.getFieldType(className, fieldName);
					boolean missingCollectionClass = 
						StringHelper.isEmpty(collectionClass);

					if (!missingCollectionClass && 
						!model.getSupportedCollectionClasses(fieldType).
						contains(collectionClass))
					{
						throw constructFieldException(fieldName, 
							"util.validation.collection_class_invalid");//NOI18N
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the 
	 * relationship is mapped to columns even though the element class is null.
	 * @param field the relationship whose element class is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createElementClassComponent (
		final RelationshipElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String className = getClassName();
				String fieldName = field.getName();

				if (isCollection(className, fieldName))
				{
					String elementClass = field.getElementClass();

					if (StringHelper.isEmpty(elementClass))
					{
						MappingClassElement mappingClass = 
							getMappingClass(className);
						MappingFieldElement mappingElement = 
							((mappingClass != null) ? 
							mappingClass.getField(fieldName) : null);

						if ((mappingElement != null) && 
							(mappingElement.getColumns().size() > 0))
						{
							throw constructFieldException(fieldName, 
								"util.validation.element_class_not_found");//NOI18N
						}
					}
				}
			}
		};
	}

	/** Create a validation component which checks whether the rules for 
	 * version consistency are followed.  This includes:
	 * <ul>
	 * <li> There must be exactly one version field defined.
	 * <li> The version field must not be a relationship.
	 * <li> The version field must not be a key field.
	 * <li> The version field must be of java type (primitive) long.
	 * <li> The version field must be in the default fetch group.
	 * <li> The version field must be mapped to exactly 1 column from the
	 * primary table.
	 * <li> The column to which the version field is mapped must be of a 
	 * numeric type and non-nullable.
	 * <li> The column to which the version field is mapped must not be a PK or
	 * FK column.
	 * </ul>
	 * @param mappingClass the mapping class element whose consistency is being
	 * checked
	 * @return the validation component
	 */
	protected ValidationComponent createVersionConsistencyComponent (
		final MappingClassElement mappingClass)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				// only bother to check for classes with version consistency
				if (MappingClassElement.VERSION_CONSISTENCY == 
					mappingClass.getConsistencyLevel())
				{
					MappingFieldElement versionField =
						 validateVersionFieldExistence();
					String className = mappingClass.getName();
					String fieldName = versionField.getName();
					String columnName = null;
					ColumnElement column = null;

					if (versionField instanceof MappingRelationshipElement)
					{
						throw constructFieldException(fieldName, 
							"util.validation.version_field_relationship_not_allowed");//NOI18N
					}
					else if (MappingFieldElement.GROUP_DEFAULT != 
						versionField.getFetchGroup()) // must be in DFG
					{
						throw constructFieldException(fieldName, 
							"util.validation.version_field_fetch_group_invalid");//NOI18N
					}

					validatePersistenceFieldAttributes(className, fieldName);
					columnName = validateVersionFieldMapping(versionField);
					column = validateTableMatch(className, fieldName, columnName);
					validateColumnAttributes(className, fieldName, column);
				}
			}
			/** Helper method validating the existence of the exactly one 
			 * version field.
			 */
			private MappingFieldElement validateVersionFieldExistence () 
				throws ModelValidationException
			{
				List versionFields = mappingClass.getVersionFields();

				// must have exactly 1 version field (for this release)
				if (versionFields.size() != 1)
				{
					throw constructClassException(mappingClass.getName(), 
						null, "util.validation.version_field_cardinality");	//NOI18N
				}

				return (MappingFieldElement)versionFields.get(0);
			}
			/** Helper method validating the attributes of the field in the  
			 * jdo model which corresponds to the version field.
			 */
			private void validatePersistenceFieldAttributes (String className, 
				String fieldName) throws ModelValidationException
			{
				Class fieldType = JavaTypeHelper.getPrimitiveClass(
					getModel().getFieldType(className, fieldName));
				String keyName = null;

				// must not be a key field
				if (getPersistenceClass(className).getField(fieldName).isKey())
					keyName = "util.validation.version_field_key_field_not_allowed";//NOI18N
				else if (Long.TYPE != fieldType)	// must be type long
					keyName = "util.validation.version_field_type_not_allowed";//NOI18N

				if (keyName != null)
					throw constructFieldException(fieldName, keyName);
			}
			/** Helper method validating the column name of the  
			 * version field mapping.
			 */
			private String validateVersionFieldMapping (
				MappingFieldElement versionField) 
				throws ModelValidationException
			{
				List columns = versionField.getColumns();

				// must be mapped to exactly 1 column (for this release)
				if (columns.size() != 1)
				{
					throw constructFieldException(versionField.getName(), 
						"util.validation.version_field_not_mapped");	//NOI18N
				}

				return (String)columns.get(0);
			}
			/** Helper method validating the column mapping of the version 
			 * field is from the primary table.
			 */
			private ColumnElement validateTableMatch (String className, 
				String fieldName, String columnName) 
				throws ModelValidationException
			{
				String schema = getSchemaForClass(className);
				String absoluteName = 
					NameUtil.getAbsoluteMemberName(schema, columnName);
				TableElement table = 
					TableElement.forName(NameUtil.getTableName(absoluteName));
				String primaryName = ((MappingTableElement)mappingClass.
					getTables().get(0)).getName();
				TableElement pTable = getTable(primaryName, schema);

				// column must be from the PT
				if (table != pTable)
				{
					throw new ModelValidationException(
						getModel().getField(className, fieldName), 
						I18NHelper.getMessage(getMessages(), 
						"util.validation.version_field_table_mismatch", //NOI18N
						new Object[]{columnName, fieldName, className}));
				}

				return ((table != null) ? (ColumnElement)table.getMember(
					DBIdentifier.create(absoluteName)) : null);
			}
			/** Helper method validating the attributes of the column of the  
			 * version field mapping.
			 */
			private void validateColumnAttributes (String className, 
				String fieldName, ColumnElement column)
				throws ModelValidationException
			{
				String keyName = null;

				// column must be numeric type and non-nullable
				if (column.isNullable() || !column.isNumericType())
					keyName = "util.validation.version_field_column_type_invalid";		// NOI18N
				else	// column must be non-PK and non-FK column
				{
					TableElement table = column.getDeclaringTable();
					UniqueKeyElement[] uks = table.getUniqueKeys();
					ForeignKeyElement[] fks = table.getForeignKeys();
					int i, count = ((uks != null) ? uks.length : 0);

					for (i = 0; i < count; i++)
					{
						UniqueKeyElement uk = uks[i];

						if (uk.isPrimaryKey() && Arrays.asList(
							uk.getColumns()).contains(column))
						{
							keyName = "util.validation.version_field_column_pk_invalid";		// NOI18N
							break;
						}
					}

					count = ((fks != null) ? fks.length : 0);
					for (i = 0; i < count; i++)
					{
						ForeignKeyElement fk = fks[i];

						if (Arrays.asList(fk.getLocalColumns()).
							contains(column))
						{
							keyName = "util.validation.version_field_column_fk_invalid";		// NOI18N
							break;
						}
					}
				}

				if (keyName != null)
				{
					throw new ModelValidationException(
						getModel().getField(className, fieldName), 
						I18NHelper.getMessage(getMessages(), keyName, 
						new Object[]{column.getName(), fieldName, className}));
				}
			}
		};
	}

	/** Create a validation component which can check whether the inverse of 
	 * the inverse of the relationship is the relationship itself.
	 * @param field the relationship whose inverse relationship is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createInverseFieldComponent (
		final RelationshipElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				Model model = getModel();
				RelationshipElement inverse = 
					field.getInverseRelationship(model);
				RelationshipElement inverseInverse = ((inverse != null) ? 
					inverse.getInverseRelationship(model) : null);

				if ((inverse != null) && 
					(!field.equals(inverseInverse) || (inverseInverse == null)))
				{
					String fieldName = field.getName();

					throw new ModelValidationException(
						model.getField(getClassName(), fieldName), 
						I18NHelper.getMessage(getMessages(), 
						"util.validation.inverse_field_invalid", //NOI18N
						new Object[]{fieldName, inverse.getName()}));
				}
			}
		};
	}

	/** Create a validation component which can check whether the inverse of 
	 * the relationship belongs to the related class (type or element class 
	 * depending on cardinality).
	 * @param field the relationship whose inverse relationship is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createRelatedClassMatchesComponent (
		final RelationshipElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String inverseName = 
					field.getInverseRelationshipName();

				if (!StringHelper.isEmpty(inverseName))
				{
					Model model = getModel();
					RelationshipElement inverse = 
						field.getInverseRelationship(model);

					if (inverse == null) // no such field in that related class
					{
						String relatedClass = getRelatedClass(field);
						String fieldName = field.getName();
						String key = ((relatedClass != null) ? 
							"util.validation.related_class_mismatch" : //NOI18N
							"util.validation.related_class_not_found");//NOI18N
						Object[] args = ((relatedClass != null) ? 
							new Object[]{fieldName, inverseName, relatedClass}
							: new Object[]{fieldName, inverseName});
							
						throw new ModelValidationException(
							model.getField(getClassName(), fieldName), 
							I18NHelper.getMessage(getMessages(), key, args));
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the mapping of 
	 * the relationship and the mapping of its inverse are inverses of each 
	 * other.
	 * @param field the relationship whose inverse relationship is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createInverseMappingComponent (
		final RelationshipElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				Model model = getModel();
				RelationshipElement inverse = 
					field.getInverseRelationship(model);

				if ((inverse != null) && !isInverseMapping(field, inverse))
				{
					String fieldName = field.getName();

					throw new ModelValidationException(
						model.getField(getClassName(), fieldName), 
						I18NHelper.getMessage(getMessages(), 
						"util.validation.inverse_mapping_mismatch", //NOI18N
						new Object[]{fieldName, inverse.getName()}));
				}
			}
			private boolean hasMappingRows (MappingRelationshipElement field2)
			{
				if (field2 != null)
				{
					ArrayList columns = field2.getColumns();
					
					return ((columns != null) && !columns.isEmpty());
				}

				return false;
			}
			private boolean isInverseMapping (RelationshipElement jdoField1,
				RelationshipElement jdoField2)
			{
				MappingRelationshipElement field1 = 
					getMappingRelationship(jdoField1);
				MappingRelationshipElement field2 = 
					getMappingRelationship(jdoField2);
				boolean field1HasMapping = hasMappingRows(field1);
				boolean field2HasMapping = hasMappingRows(field2);

				// if both have rows, they must be exact inverses
				if (field1HasMapping && field2HasMapping)
				{
					boolean field1IsJoin = isJoin(field1);

					if (field1IsJoin == isJoin(field2))
					{
						ArrayList pairs1 = field1.getColumns();
						ArrayList pairs2 = field2.getColumns();

						return ((!field1IsJoin) ? isInverse(pairs1, pairs2) :
							(isInverse(pairs1, 
							field2.getAssociatedColumns()) && 
							isInverse(field1.getAssociatedColumns(), pairs2)));
					}

					return false;
				}

				// if neither have rows that's fine
				return (field1HasMapping == field2HasMapping);
			}
			private boolean isInverse (ArrayList pairs1, ArrayList pairs2)
			{
				int i, size1 = pairs1.size(), size2 = pairs2.size();

				if (size1 == size2)
				{
					for (i = 0; i < size1; i++)
					{
						String nextPair = (String)pairs1.get(i);
						String inversePair = (String)pairs2.get(i);
						int semicolonIndex1 = nextPair.indexOf(';');
						int semicolonIndex2 = inversePair.indexOf(';');

						if (((semicolonIndex1 == -1) || (semicolonIndex2 == -1))
							|| (!nextPair.substring(0, semicolonIndex1).equals(
							inversePair.substring(semicolonIndex2 + 1)) || 
							!nextPair.substring(semicolonIndex1 + 1).equals(
							inversePair.substring(0, semicolonIndex2))))
						{
							return false;
						}
					}

					return true;
				}

				return false;
			}
		};
	}

	/** Create a validation component which can check whether the field is
	 * part of a managed (multiple fields to same column) group and in an 
	 * illegal fetch group.  If the field is in one of these groups, it is 
	 * not allowed to be in the default fetch group.
	 * @param field the field whose fetch group is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createFieldDefaultFetchGroupComponent (
		final MappingFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (field != null)
				{
					String fieldName = field.getName();
					PersistenceClassElement persistenceClass = 
						getPersistenceClass(getClassName());
					PersistenceFieldElement pElement = 
						((persistenceClass != null) ? 
						persistenceClass.getField(fieldName) : null);

					if ((pElement != null) && !pElement.isKey() && 
						(MappingFieldElement.GROUP_DEFAULT == 
						field.getFetchGroup()))
					{
						MappingClassElement mappingClass = 
							field.getDeclaringClass();
						boolean isVersionField = 
							((MappingClassElement.VERSION_CONSISTENCY == 
								mappingClass.getConsistencyLevel()) &&
								field.isVersion());
						Iterator iterator = mappingClass.getFields().iterator();
						String exceptionKey = (!isVersionField ?
							"util.validation.field_fetch_group_invalid"://NOI18N
							"util.validation.version_field_column_invalid");//NOI18N

						/* rules: 
						 *	primitive, primitive -> error if exact match of 
						 *		columns
						 *	primitive, relationship OR
						 *	relationship, primitive -> error if non-collection
						 *		relationship and none of the relationship's 
						 *		columns are PK columns and any are present in 
						 *		the primitive's list
						 *	relationship, relationship -> error if exact 
						 *		match of mapping (local, join, associated), 
						 *		but order is not important
						 */
						while (iterator.hasNext())
						{
							MappingFieldElement testField =
								(MappingFieldElement)iterator.next();

							if (isManaged(field, testField) || 
								isManaged(testField, field))
							{
								throw constructFieldException(
									fieldName, exceptionKey);
							}
							else if (!testField.equals(field) && isExactMatch(
								field, testField))
							{
								throw constructFieldException(
									fieldName, exceptionKey);
							}
						}
					}
				}
			}
			private boolean isManaged (MappingFieldElement primField, 
				MappingFieldElement relField)				
			{
				String className = getClassName();

				if (!isRelationship(primField) && isRelationship(relField) && 
					!isCollection(className, relField.getName()))
				{
					ArrayList columns = primField.getColumns();
					Iterator iterator = relField.getColumns().iterator();
					String databaseRoot = getSchemaForClass(className);

					while (iterator.hasNext())
					{
						if (!testColumn(getLocalColumn((String)iterator.next(), 
							databaseRoot), columns))
						{
							return true;
						}
					}
				}

				return false;
			}
			private boolean testColumn (ColumnElement column, 
				ArrayList masterList)
			{
				if ((column != null) && !isPrimaryKeyColumn(column))
				{
					return !masterList.contains(NameUtil.
						getRelativeMemberName(column.getName().getFullName()));
				}

				return true;
			}
			private ColumnElement getLocalColumn (String pairName, 
				String databaseRoot)
			{
				ColumnPairElement pair = getPair(pairName, databaseRoot);

				return ((pair != null) ? pair.getLocalColumn() : null);
			}
			private boolean isPrimaryKeyColumn (ColumnElement column)
			{
				if (column != null)
				{
					KeyElement key = column.getDeclaringTable().getPrimaryKey();

					return ((key != null) && 
						(key.getColumn(column.getName()) != null));
				}

				return false;
			}
			private boolean isExactMatch (ArrayList columns1, 
				ArrayList columns2)
			{
				int count = columns1.size();

				if ((count > 0) && (count == columns2.size()))
					return getDifference(columns1, columns2).isEmpty();

				return false;
			}
			private boolean isExactMatch (MappingFieldElement field1, 
				MappingFieldElement field2)
			{
				boolean field1IsRel = isRelationship(field1);
				boolean match = false;

				// both primitives, or both relationships
				if (field1IsRel == isRelationship(field2))
				{
					match = isExactMatch(field1.getColumns(), 
						field2.getColumns());

					if (match && field1IsRel)
					{
						MappingRelationshipElement rel1 =
							(MappingRelationshipElement)field1;
						MappingRelationshipElement rel2 =
							(MappingRelationshipElement)field2;
						boolean field1IsJoin = isJoin(rel1);

						// both join relationships or both direct
						if (field1IsJoin == isJoin(rel2))
						{
							if (field1IsJoin)
							{
								match = isExactMatch(
									rel1.getAssociatedColumns(), 
									rel2.getAssociatedColumns());
							}
						}
						else
							match = false;
					}
				}
				
				return match;
			}
		};
	}

	/** Create a validation component which can check whether the schema of   
	 * the related class matches that of the class we are checking.
	 * @param relatedClass the class whose schema is being checked
	 * @param relatedField the relationship field whose schema is being 
	 * compared
	 * @return the validation component
	 */
	protected ValidationComponent createRelatedSchemaMatchesComponent (
		final String relatedClass, final PersistenceFieldElement relatedField)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (relatedClass != null)
				{
					String className = getClassName();
					String mySchema = getSchemaForClass(className);
					String relatedSchema = getSchemaForClass(relatedClass);

					if ((mySchema != null) && (relatedSchema != null) && 
						!(relatedSchema.equals(mySchema)))
					{
						String fieldName = relatedField.getName();

						throw new ModelValidationException(
							getModel().getField(className, fieldName), 
							I18NHelper.getMessage(getMessages(), 
							"util.validation.schema_mismatch", //NOI18N
							new Object[]{className, relatedClass, fieldName}));
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether any of 
	 * the supplied tables of the related class (which includes primary 
	 * and secondary tables) contains the table of the column stored in 
	 * the relationship definition.
	 * @param relatedClass the class whose table is being checked
	 * @param relatedField the relationship field whose table is being compared
	 * @param tableNames the list of names of the tables we expect the 
	 * column to match
	 * @param pairName the name of the pair whose reference column is to 
	 * be checked
	 * @return the validation component
	 */
	protected ValidationComponent createRelatedTableMatchesComponent (
		final String relatedClass, final PersistenceFieldElement relatedField,
		final List tableNames, final String pairName)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				ColumnPairElement pair = getPair(pairName, 
					getSchemaForClass(relatedClass));

				if (pair != null)
				{
					ColumnElement column = pair.getReferencedColumn();

					if (!matchesTable(tableNames, column))
					{
						String fieldName = relatedField.getName();

						throw new ModelValidationException(
							getModel().getField(getClassName(), fieldName), 
							I18NHelper.getMessage(getMessages(), 
							getKey(
							"util.validation.table_mismatch", //NOI18N
							relatedField),
							new Object[]{column.getName().getFullName(),
							fieldName, relatedClass}));
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the schema of   
	 * the given class exists.
	 * @param className the class whose mapped schema's existence is 
	 * being checked
	 * @return the validation component
	 */
	protected ValidationComponent createSchemaExistenceComponent (
		final String className)
	{
		return createSchemaExistenceComponent(className, null);
	}

	/** Create a validation component which can check whether the schema of   
	 * the given class exists.
	 * @param className the class whose mapped schema's existence is 
	 * being checked
	 * @param relatedField the relationship field whose class' 
	 * mapped schema is being checked, may be <code>null</code> in which 
	 * case we are probably checking the same class as the validator is 
	 * checking overall
	 * @return the validation component
	 */
	protected ValidationComponent createSchemaExistenceComponent (
		final String className, final PersistenceFieldElement relatedField)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String schemaName = getSchemaForClass(className);

				if ((schemaName != null) && 
					(SchemaElement.forName(schemaName) == null))
				{
					Object[] args = (relatedField == null) ? 
						new Object[]{schemaName, className} : 
						new Object[]{schemaName, className, relatedField};

					throw new ModelValidationException(
						ModelValidationException.WARNING, 
						getOffendingObject(relatedField),
						I18NHelper.getMessage(getMessages(), getKey(
							"util.validation.schema_not_found", //NOI18N
							relatedField), args));
				}
			}
		};
	}

	/** Create a validation component which can check whether the 
	 * class is mapped to tables even though the schema is null or the 
	 * class is mapped to a primary table without a primary key.
	 * @param primaryTable the primary table for the class
	 * @return the validation component
	 */
	protected ValidationComponent createPrimaryTableComponent (
		final MappingTableElement primaryTable)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (primaryTable != null)
				{
					String className = getClassName();
					String schemaName = getSchemaForClass(className);

					if (schemaName == null)
					{
						throw constructClassException(className, null, 
							"util.validation.schema_not_set");		//NOI18N
					}
					else
					{
						String tableName = primaryTable.getName();
						TableElement table = getTable(tableName, schemaName);

						if ((table != null) && (table.getPrimaryKey() == null))
						{ 
							throw new ModelValidationException(
								getOffendingObject(null),
								I18NHelper.getMessage(getMessages(), 
									"util.validation.table_no_primarykey", //NOI18N
									new Object[]{tableName, className}));
						}
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the given table 
	 * exists.
	 * @param tableName the table whose existence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createTableExistenceComponent (
		final String tableName)
	{
		return createTableExistenceComponent(tableName, null);
	}

	/** Create a validation component which can check whether the given table 
	 * exists.
	 * @param tableName the table whose existence is being checked
	 * @param relatedField the relationship field whose class' 
	 * table is being checked, may be <code>null</code> in which 
	 * case we are probably checking the same class as the validator is 
	 * checking overall
	 * @return the validation component
	 */
	protected ValidationComponent createTableExistenceComponent (
		final String tableName, final PersistenceFieldElement relatedField)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (tableName != null)
				{
					String className = getClassName();
					boolean noRelated = (relatedField == null);
					TableElement table = getTable(tableName,
						getSchemaForClass((noRelated ? className : 
						getRelatedClass(relatedField))));

					if (table == null)
					{
						Object[] args = noRelated ? 
							new Object[]{tableName, className} : 
							new Object[]{tableName, relatedField};

						throw new ModelValidationException(
							ModelValidationException.WARNING, 
							getOffendingObject(relatedField),
							I18NHelper.getMessage(getMessages(), getKey(
								"util.validation.table_not_found", //NOI18N
								relatedField), args));
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the given column 
	 * exists.
	 * @param columnName the column whose existence is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createColumnExistenceComponent (
		final String columnName)
	{
		return createColumnExistenceComponent(columnName, null);
	}

	/** Create a validation component which can check whether the given  
	 * column or column pair exists.
	 * @param columnName the column or pair whose existence is being checked
	 * @param relatedField the field whose class' column is being checked, 
	 * may be <code>null</code> in which case we are probably checking the 
	 * same secondary table setup
	 * @return the validation component
	 */
	protected ValidationComponent createColumnExistenceComponent (
		final String columnName, final MappingFieldElement relatedField)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				if (columnName != null)
				{
					String className = getClassName();
					String absoluteName = NameUtil.getAbsoluteMemberName(
						getSchemaForClass(className), columnName);
					TableElement table = TableElement.forName(
						NameUtil.getTableName(absoluteName));
					boolean foundTable = (table != null);
					DBMemberElement columnElement = ((foundTable) ? 
						table.getMember(DBIdentifier.create(absoluteName)) :
						null);
					boolean noRelated = (relatedField == null);

					if (foundTable)
					{
						boolean isRelationship = 
							(!noRelated && isRelationship(relatedField));
						boolean noColumn = (columnElement == null);

						if (!isRelationship && noColumn)
						{
							Object[] args = (noRelated) ? 
								new Object[]{columnName, className} : 
								new Object[]{columnName, relatedField, 
								className};

							throw new ModelValidationException(
								ModelValidationException.WARNING, 
								getOffendingObject(relatedField),
								I18NHelper.getMessage(getMessages(), getKey(
									"util.validation.column_not_found", //NOI18N
									relatedField), args));
						}
						else if (isRelationship && 
							(noColumn || !isPairComplete(columnElement)))
						{
							throw new ModelValidationException(
								ModelValidationException.WARNING, 
								getOffendingObject(relatedField),
								I18NHelper.getMessage(getMessages(), 
									"util.validation.column_invalid", //NOI18N
									new Object[]{columnName, relatedField, 
									className}));
						}
					}
				}
			}
			private boolean isPairComplete (DBMemberElement member)
			{
				return ((member instanceof ColumnPairElement) &&  
					(((ColumnPairElement)member).getLocalColumn() != null) && 
					(((ColumnPairElement)member).getReferencedColumn() 
					!= null));
			}
		};
	}

	/** Create a validation component which can check whether the field is   
	 * one of a set mapped to overlapping columns
	 * @param field the field whose column mapping is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createColumnOverlapComponent (
		final MappingFieldElement field)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				MappingClassElement mappingClass = field.getDeclaringClass();
				Iterator iterator = mappingClass.getFields().iterator();
				ArrayList myColumns = field.getColumns();

				while (iterator.hasNext())
				{
					MappingFieldElement testField =
						(MappingFieldElement)iterator.next();

					if (!testField.equals(field) && !isRelationship(testField) 
						&& isPartialMatch(myColumns, testField.getColumns()))
					{
						String fieldName = field.getName();

						throw new ModelValidationException(getModel().getField(
							getClassName(), fieldName), 
							I18NHelper.getMessage(getMessages(), 
							"util.validation.field_mapping_invalid", //NOI18N
							new Object[]{fieldName, testField.getName()}));
					}
				}
			}
			private boolean isPartialMatch (ArrayList columns1, 
				ArrayList columns2)
			{
				int count = columns1.size();

				if (count > 0)
				{
					ArrayList difference = getDifference(columns1, columns2);

					return (!difference.isEmpty() && 
						(columns2.size() != difference.size()));
				}

				return false;
			}
		};
	}
	
	/** Create a validation component which can check whether the key class
	 * of the persistence capable class is valid. This includes:
	 * <ul>
	 * <li> The key class must be public.
	 * <li> The key class must implement Serializable.
	 * <li> If the key class is an inner class, it must be static.
	 * <li> The key class must have a public constructor, which might be 
	 * the default constructor or a no-arg constructor.
	 * <li> The field types of all non-static fields in the key class must be 
	 * of valid types.
	 * <li> All serializable non-static fields in the key class must be public.
	 * <li> The names of the non-static fields in the key class must include the
	 * names of the primary key fields in the JDO class, and the types of the 
	 * common fields must be identical
	 * <li> The key class must redefine equals and hashCode.
	 * </ul>
	 */
 	protected ValidationComponent createKeyClassComponent (
		final String className)
	{
		return new ValidationComponent ()
		{
			/** The class element of the key class */
			private Object keyClass;

			/** The fully qualified name of the key class */
			private String keyClassName;

			public void validate () throws ModelValidationException
			{
				// checks the key class name
				keyClassName = validateKeyClassName(className);
				// initilialize keyClass field 
				keyClass = getModel().getClass(keyClassName, getClassLoader());
				validateClass();
				validateConstructor();
				validateFields();
				validateMethods();
			}

			/** Helper method validating the key class itself: 
			 * public, serializable, static.
			 */
			private void validateClass () throws ModelValidationException
			{
				Model model = getModel();
				int modifiers = model.getModifiersForClass(keyClassName);
				boolean hasKeyClassName = !StringHelper.isEmpty(keyClassName);
				boolean isInnerClass = 
					(hasKeyClassName && (keyClassName.indexOf('$') != -1));
				String pcClassName = getClassName();

				// check for key class existence
				if (keyClass == null)
				{
					throw new ModelValidationException(
						ModelValidationException.WARNING,
						model.getClass(pcClassName),
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_missing", //NOI18N
						keyClassName, pcClassName));
				}

				// check for public class modifier
				if (!Modifier.isPublic(modifiers))
				{
					throw new ModelValidationException(keyClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_public", //NOI18N
						keyClassName, pcClassName));
				}
				
				// check for Serializable
				/* This check is disabled because of Boston backward 
				   compatibility. In Boston there was no requirement for a 
				   key class being serializable, thus key classes from pc 
				   classes mapped with boston are not serializable.

				if (!model.implementsInterface(keyClass, 
					"java.io.Serializable")) //NOI18N
				{
					throw new ModelValidationException(keyClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_serializable", //NOI18N
						keyClassName, pcClassName));
				}
				*/

				// if inner class it must be static
				if (isInnerClass && !Modifier.isStatic(modifiers))
				{
					throw new ModelValidationException(keyClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_static", //NOI18N
						keyClassName, pcClassName));
				}
			}

			/** Helper method validating the fields of the key class.
			 */
			private void validateFields () throws ModelValidationException
			{
				String pcClassName = getClassName();
				Model model = getModel();
				// check for valid typed public non-static fields
				List keyClassFieldNames = model.getAllFields(keyClassName);
				Map keyFields = getKeyFields();

				for (Iterator i = keyClassFieldNames.iterator(); i.hasNext();)
				{
					String keyClassFieldName = (String)i.next();
					Object keyClassField = 
						getKeyClassField(keyClassName, keyClassFieldName);
					int keyClassFieldModifiers = 
						model.getModifiers(keyClassField);
					String keyClassFieldType = model.getType(keyClassField);
					Object keyField = keyFields.get(keyClassFieldName);

					if (Modifier.isStatic(keyClassFieldModifiers))
						// we are not interested in static fields
						continue;

					if (!model.isValidKeyType(keyClassName, keyClassFieldName))
					{
						throw new ModelValidationException(keyClassField,
							I18NHelper.getMessage(getMessages(), 
							"util.validation.key_field_type_invalid", //NOI18N
							keyClassFieldName, keyClassName));
					}
					
					if (!Modifier.isPublic(keyClassFieldModifiers))
					{
						throw new ModelValidationException(keyClassField,
							I18NHelper.getMessage(getMessages(), 
							"util.validation.key_field_public", //NOI18N
							keyClassFieldName, keyClassName));
					}

					if (keyField == null)
						continue;
					
					if (!keyClassFieldType.equals(model.getType(keyField)))
					{
						throw new ModelValidationException(keyClassField,
							I18NHelper.getMessage(getMessages(), 
							"util.validation.key_field_type_mismatch", //NOI18N
							keyClassFieldName, keyClassName, pcClassName));
					}

					// remove handled keyField from the list of keyFields
					keyFields.remove(keyClassFieldName);
				}

				// check whether there are any unhandled key fields
				if (!keyFields.isEmpty())
				{
					Object pcClass = model.getClass(pcClassName);
					String fieldNames = StringHelper.arrayToSeparatedList(
						new ArrayList(keyFields.keySet()));

					throw new ModelValidationException(pcClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_field_missing", //NOI18N
						pcClassName, keyClassName, fieldNames));
				}
			}

			/** Helper method validating the key class constructors.
			 */
			private void validateConstructor () throws ModelValidationException
			{
				// no constructor or no arg constructor
				Model model = getModel();
				boolean hasConstr = model.hasConstructor(keyClassName);
				Object noArgConstr = 
					model.getConstructor(keyClassName, Model.NO_ARGS);
				int modifiers = model.getModifiers(noArgConstr);

				if (hasConstr && 
					((noArgConstr == null) || !Modifier.isPublic(modifiers)))
				{
					throw new ModelValidationException(keyClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_constructor", //NOI18N
						keyClassName, getClassName()));
				}
			}

			/** Helper method validating the key class methods.
			 */
			private void validateMethods () throws ModelValidationException
			{
				Model model = getModel();
				Object equalsMethod = getNonObjectMethod(keyClassName, 
					"equals", Model.getEqualsArgs()); //NOI18N
				Object hashCodeMethod = getNonObjectMethod(keyClassName, 
					"hashCode", Model.NO_ARGS); //NOI18N

				// check equals method
				if (!matchesMethod(equalsMethod, Modifier.PUBLIC,
					0, "boolean")) //NOI18N
				{
					throw new ModelValidationException(keyClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_equals", //NOI18N
						keyClassName, getClassName()));
				}
				
				// check hashCode method
				if (!matchesMethod(hashCodeMethod, Modifier.PUBLIC,
					0, "int")) //NOI18N
				{
					throw new ModelValidationException(keyClass,
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_hashcode", //NOI18N
						keyClassName, getClassName()));
				}
			}
			
			/** Helper method validating the name of the key class.
			 */
			private String validateKeyClassName (String keyClassName) 
				throws ModelValidationException
			{
				String pcClassName = getClassName();
				Model model = getModel();
				boolean hasKeyClassName = !StringHelper.isEmpty(keyClassName);
				boolean hasPrefix;
				String nameSuffix;
				boolean isOIDNameSuffix;

				// check for existence of key class name
				if (!hasKeyClassName)
				{
					throw new ModelValidationException(
						ModelValidationException.WARNING,
						model.getClass(pcClassName),
						I18NHelper.getMessage(getMessages(), 
						"util.validation.key_class_unset", //NOI18N
						pcClassName));
				}

				keyClassName = keyClassName.trim();
				hasPrefix = keyClassName.startsWith(pcClassName);
				nameSuffix = (hasPrefix ? 
				   keyClassName.substring(pcClassName.length()) : keyClassName);
				isOIDNameSuffix = 
					(nameSuffix.equalsIgnoreCase(".OID") || // NOI18N
					 nameSuffix.equalsIgnoreCase("$OID")); // NOI18N

				if (!hasPrefix || 
					(!nameSuffix.equalsIgnoreCase("Key") && 	// NOI18N
					 !isOIDNameSuffix))
				{
					Object pcClass = getModel().getClass(pcClassName);
					throw new ModelValidationException(pcClass,
						I18NHelper.getMessage(getMessages(),
						"util.validation.key_class_invalid", //NOI18N
						keyClassName, pcClassName));
				}
				if (isOIDNameSuffix)
				{
					StringBuffer buf = new StringBuffer(keyClassName);
					buf.setCharAt(keyClassName.length() - 4, '$');
					return buf.toString();  
				}
				return keyClassName;
			}

			// helper method which returns a field object from the 
			// given class or one of its superclasses
			private Object getKeyClassField (String keyClassName, 
				String keyClassFieldName)
			{
				Model model = getModel();
				Object keyClassField = 
					model.getField(keyClassName, keyClassFieldName);

				if (keyClassField == null)	// this is an inherited field
				{
					keyClassField = model.getInheritedField(
						keyClassName, keyClassFieldName);
				}

				return keyClassField;
			}

			/** Helper method returning the key fields of the pc class as a map.
			 */
			private Map getKeyFields ()
			{
				Model model = getModel();
				String pcClassName = getClassName();
				PersistenceClassElement pce = 
					model.getPersistenceClass(pcClassName);
				PersistenceFieldElement[] fields = pce.getFields();
				Map keyFields = new HashMap();

				if (fields != null)
				{
					for (int i = 0; i < fields.length; i++)
					{
						PersistenceFieldElement pfe = fields[i];
						if (pfe.isKey())
						{
							String name = pfe.getName();
							keyFields.put(name, 
								model.getField(pcClassName, name));
						}
					}
				}
				
				return keyFields;
			}
			// helper method which returns a method object from the 
			// given class or one of its superclasses provided it
			// is not java.lang.Object
			private Object getNonObjectMethod (String className, 
				String methodName, String[] argTypeNames)
			{
				Model model = getModel();
				Object method = 
					model.getMethod(className, methodName, argTypeNames);

				if (method == null)	// look for an inherited method
				{
					method = model.getInheritedMethod(
						className, methodName, argTypeNames);

					if ((method != null) && model.getDeclaringClass(method).
						equals("java.lang.Object"))		// NOI18N
					{
						method = null;
					}
				}

				return method;
			}
		};
	}

	/** Create a validation component which can check that the persistence 
	 * capable class implement methods readObject and writeObject, if the class 
	 * implements the intreface java.io.Serializable
	 * @param className the class whose methods are checked
	 * @return the validation component
	 */
	protected ValidationComponent createSerializableClassComponent (
		final String className)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				Model model = getModel();
				Object pcClass = null;

				if (className == null)
					return;
				pcClass = model.getClass(className);
				if (pcClass == null)
					return;

				if (model.implementsInterface(pcClass, "java.io.Serializable")) //NOI18N
				{
					// check readObject method
					Object readMethod = model.getMethod(className, 
						"readObject", Model.getReadObjectArgs()); //NOI18N

					if (!matchesMethod(readMethod, Modifier.PRIVATE,
						Modifier.SYNCHRONIZED, "void")) // NOI18N
					{
						throw new ModelValidationException(pcClass,
							I18NHelper.getMessage(getMessages(),
							"util.validation.class_readobject", //NOI18N
							className));
					}
					
					// check writeObject method
					Object writeMethod = model.getMethod(className, 
						"writeObject", Model.getWriteObjectArgs()); //NOI18N

					if (!matchesMethod(writeMethod, Modifier.PRIVATE,
						Modifier.SYNCHRONIZED, "void")) // NOI18N
					{
						throw new ModelValidationException(pcClass,
							I18NHelper.getMessage(getMessages(),
							"util.validation.class_writeobject", //NOI18N
							className));
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the class is  
	 * unmapped.
	 * @param persistenceClass the class whose mapping is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createClassMappingComponent (
		final PersistenceClassElement persistenceClass)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				PersistenceFieldElement[] fields = persistenceClass.getFields();
				String className = getClassName();

				if ((fields == null) || fields.length == 0)
				{
					throw constructClassException(
						ModelValidationException.WARNING, className, null, 
						"util.validation.class_no_fields");		//NOI18N
				}
				else	// has fields, check for primary table
				{
					MappingClassElement mappingClass = 
						getMappingClass(className);

					if ((mappingClass == null) || 
						(mappingClass.getTables().size() == 0))
					{
						throw constructClassException(
							ModelValidationException.WARNING, className, null, 
							"util.validation.class_not_mapped");		//NOI18N
					}
				}
			}
		};
	}

	/** Create a validation component which can check whether the class   
	 * contains field mappings for all primary key columns.
	 * @param persistenceClass the class whose mapping is being checked
	 * @return the validation component
	 */
	protected ValidationComponent createKeyColumnMappingComponent (
		final PersistenceClassElement persistenceClass)
	{
		return new ValidationComponent ()
		{
			public void validate () throws ModelValidationException
			{
				String className = getClassName();
				MappingClassElement mappingClass = getMappingClass(className);

				if (mappingClass != null)
				{
					List tables = mappingClass.getTables();

					if (tables.size() > 0)
					{
						String tableName = 
							((MappingTableElement)tables.get(0)).getName();
						TableElement table = getTable(tableName, 
							getSchemaForClass(className));
						List columns = getUnmappedColumnNames(
							((table != null) ? table.getPrimaryKey() : null), 
							mappingClass);

						if ((columns != null) && (columns.size() > 0))
						{
							throw new ModelValidationException(
								ModelValidationException.WARNING, 
								getOffendingObject(null),
								I18NHelper.getMessage(getMessages(), 
								"util.validation.class_key_column_missing", //NOI18N
								className, tableName, 
								StringHelper.arrayToSeparatedList(columns)));
						}
					}
				}
			}
			private List getUnmappedColumnNames (KeyElement primaryKey, 
				MappingClassElement mappingClass)
			{
				List unmappedColumns = null;

				if (primaryKey != null)	// check if primary table has a pk
				{
					ColumnElement[] columns = primaryKey.getColumns();
					int count = ((columns != null) ? columns.length : 0);

					// all columns in the pk should be mapped to key fields
					if (count > 0)
					{
						List mappingFields = mappingClass.getFields();
						Iterator iterator = mappingFields.iterator();

						unmappedColumns = getRelativeColumnNames(columns);

						while (iterator.hasNext())
						{
							MappingFieldElement field = 
								(MappingFieldElement)iterator.next();

							if (isKeyField(field))
								unmappedColumns.removeAll(field.getColumns());
						}
					}
				}

				return unmappedColumns;
			}
			private List getRelativeColumnNames (ColumnElement[] columns)
			{
				int i, count = ((columns != null) ? columns.length : 0);
				List columnNames = new ArrayList(count);
								
				for (i = 0; i < count; i++)
				{
					columnNames.add(NameUtil.getRelativeMemberName(
						columns[i].getName().getFullName()));
				}

				return columnNames;
			}
			private boolean isKeyField (MappingFieldElement field)
			{
				PersistenceFieldElement persistenceField = 
					persistenceClass.getField(field.getName());

				return ((persistenceField != null) && persistenceField.isKey());
			}
		};
	}

	//========== Convenience methods for exception construction ============

	/** Computes the offending object to be used in the construction of a 
	 * ModelValidationException as follows: if a non-null field is supplied, 
	 * the corresponding org.openide.src.FieldElement is returned, otherwise, 
	 * corresponding org.openide.src.ClassElement is returned.
	 * @param field the field object which caused the problem - may be 
	 * <code>null</code>
	 * @return the offending object
	 */
	private Object getOffendingObject (Object field)
	{
		return ((field == null) ? 
			getModel().getClass(getClassName(), getClassLoader()) :
			getModel().getField(getClassName(), field.toString()));
	}

	/** Computes the key for the i18n string to be used in the construction 
	 * of a ModelValidationException as follows: if a non-null field is 
	 * supplied, "_related" is appending to the supplied key base, otherwise, 
	 * the key base is returned as is.
	 * @param keyBase the base key to be used for the i18n string
	 * @param field the field object which caused the problem - may be 
	 * <code>null</code>
	 * @return the key
	 */
	private String getKey (String keyBase, Object field)
	{
		return ((field == null) ? keyBase : (keyBase + "_related"));	//NOI18N
	}

	/** Computes the arguments for the i18n string to be used in the 
	 * construction of a ModelValidationException as follows: if a 
	 * non-null field is supplied, an array containing the supplied className 
	 * and field is returned, otherwise, an array containing only the supplied
	 * className is returned.
	 * @param className the name of the class which caused the problem
	 * @param field the field object which caused the problem - may be 
	 * <code>null</code>
	 * @return the argument array
	 */
	private Object[] getArguments (String className, Object field)
	{
		return ((field == null) ? new Object[]{className} : 
			new Object[]{className, field});
	}

	/** Constructs a ModelValidationException for class validation tests 
	 * using the supplied class name, related field, and key base.
	 * @param className the name of the class which caused the problem
	 * @param field the field object which caused the problem - may be 
	 * <code>null</code>
	 * @param keyBase the base key to be used for the i18n string
	 * @return the ModelValidationException
	 * @see #getOffendingObject
	 * @see #getKey
	 * @see #getArguments
	 * @see #constructFieldException
	 */
	private ModelValidationException constructClassException (String className, 
		Object relatedField, String keyBase)
	{
		return constructClassException(ModelValidationException.ERROR, 
			className, relatedField, keyBase);
	}

	/** Constructs a ModelValidationException for class validation tests 
	 * using the supplied class name, related field, and key base.
	 * @param errorType the type of error -- one of 
	 * {@link ModelValidationException#ERROR} or 
	 * {@link ModelValidationException#WARNING}.
	 * @param className the name of the class which caused the problem
	 * @param field the field object which caused the problem - may be 
	 * <code>null</code>
	 * @param keyBase the base key to be used for the i18n string
	 * @return the ModelValidationException
	 * @see #getOffendingObject
	 * @see #getKey
	 * @see #getArguments
	 * @see #constructFieldException
	 */
	private ModelValidationException constructClassException (int errorType,
		String className, Object relatedField, String keyBase)
	{
		return new ModelValidationException(errorType, 
			getOffendingObject(relatedField), I18NHelper.getMessage(
			getMessages(), getKey(keyBase, relatedField), 
			getArguments(className, relatedField)));
	}

	/** Constructs a ModelValidationException for field validation tests 
	 * using the supplied field name and key.
	 * @param fieldName the name of the field which caused the problem
	 * @param keyBase the base key to be used for the i18n string
	 * @return the ModelValidationException
	 * @see #constructClassException
	 */
	private ModelValidationException constructFieldException (String fieldName, 
		String key)
	{
		return constructFieldException(ModelValidationException.ERROR, 
			fieldName, key);
	}

	/** Constructs a ModelValidationException for field validation tests 
	 * using the supplied field name and key.
	 * @param errorType the type of error -- one of 
	 * {@link ModelValidationException#ERROR} or 
	 * {@link ModelValidationException#WARNING}.
	 * @param fieldName the name of the field which caused the problem
	 * @param keyBase the base key to be used for the i18n string
	 * @return the ModelValidationException
	 * @see #constructClassException
	 */
	private ModelValidationException constructFieldException (int errorType,
		String fieldName, String key)
	{
		return new ModelValidationException(errorType, 
			getModel().getField(getClassName(), fieldName), 
			I18NHelper.getMessage(getMessages(), key, fieldName));
	}

	//=============== Misc. private convenience methods  ================

	/** Checks whether the specified method element exists and if so whether it
	 * has the expected modifiers and the expected return type.
	 * @param method the method element to be checked
	 * @param expectedModifiers the modifiers the method should have
	 * @param optionalModifiers additional modifiers the method might have
	 * @param expectedReturnType the return type the method should have
	 * @return <code>true</code> if the method matches, 
	 * <code>false</code> otherwise.
	 */
	private boolean matchesMethod (final Object method, 
		final int expectedModifiers, final int optionalModifiers, 
		final String expectedReturnType)
	{
		boolean matches = false;  

		if (method != null)
		{
			Model model = getModel();
			int modifiers = model.getModifiers(method);

			matches = (((modifiers == expectedModifiers) || 
				(modifiers == (expectedModifiers | optionalModifiers))) &&
				expectedReturnType.equals(model.getType(method)));
		}

		return matches;
	}

	/** Check if the table of the column matches one of the list of tables.
	 * @param tableNames A list of table names in which to check for a match
	 * @param column A ColumnElement object to be checked
	 * @return <code>true</code> if the column belongs to a table found 
	 * in the supplied list of table names, <code>false</code> otherwise 
	 */
	private boolean matchesTable (List tableNames, ColumnElement column)
	{	
		return ((column == null) ? true : tableNames.contains(
			column.getDeclaringTable().getName().getName()));
	}

	private boolean isRelationship (Object field)
	{
		return ((field instanceof RelationshipElement) || 
			(field instanceof MappingRelationshipElement));
	}

	private boolean shouldBeRelationship (PersistenceFieldElement field)
	{
		Model model = getModel();
		String fieldType = model.getFieldType(getClassName(), field.getName());

		return (isPersistent(fieldType) || model.isCollection(fieldType));
	}

	private boolean isLegalRelationship (PersistenceFieldElement field)
	{
		return (isRelationship(field) ? shouldBeRelationship(field) : false);
	}

	private boolean isCollection (String className, String fieldName)
	{
		Model model = getModel();

		return model.isCollection(model.getFieldType(className, fieldName));
	}

	private String getRelatedClass (PersistenceFieldElement field)
	{
		if (isLegalRelationship(field))
			return getModel().getRelatedClass((RelationshipElement)field);

		return null;
	}

	private String getSchemaForClass (String className)
	{
		MappingClassElement mappingClass = getMappingClass(className);
		String schema = ((mappingClass != null) ? 
			mappingClass.getDatabaseRoot() : null);

		return (StringHelper.isEmpty(schema) ? null : schema.trim());
	}

	private MappingRelationshipElement getMappingRelationship (
		RelationshipElement jdoElement)
	{
		MappingRelationshipElement mappingElement = null;

		if (jdoElement != null)
		{
			MappingClassElement mappingClass = getMappingClass(
				jdoElement.getDeclaringClass().getName());

			if (mappingClass != null)
			{
				MappingFieldElement fieldElement =
					mappingClass.getField(jdoElement.getName());

				if (isRelationship(fieldElement))
					mappingElement = (MappingRelationshipElement)fieldElement;
			}
		}

		return mappingElement;
	}

	private boolean isJoin (MappingRelationshipElement field)
	{
		if (field != null)
		{
			ArrayList columns = field.getAssociatedColumns();
			
			return ((columns != null) && !columns.isEmpty());
		}
		
		return false;
	}

	private MappingReferenceKeyElement findReferenceKey (
		MappingTableElement primaryTable, MappingTableElement secondaryTable)
	{
		if ((primaryTable != null) && (secondaryTable != null))
		{
			Iterator iterator = primaryTable.getReferencingKeys().iterator();

			while (iterator.hasNext())
			{
				MappingReferenceKeyElement testKey = 
					(MappingReferenceKeyElement)iterator.next();

				if (testKey.getTable().equals(secondaryTable))
					return testKey;
			}
		}

		return null;
	}

	private TableElement getTable (String tableName, String databaseRoot)
	{
		String absoluteName = NameUtil.getAbsoluteTableName(databaseRoot,
			tableName);
		return TableElement.forName(absoluteName);
	}

	private ColumnPairElement getPair (String pairName, String databaseRoot)
	{
		String absoluteName = NameUtil.getAbsoluteMemberName(
			databaseRoot, pairName);
		TableElement tableElement = TableElement.forName(
			NameUtil.getTableName(absoluteName));
		DBMemberElement pair = ((tableElement == null) ? null :
			tableElement.getMember(DBIdentifier.create(absoluteName)));

		return ((pair instanceof ColumnPairElement) ? 
			((ColumnPairElement)pair) : null);
	}

	private ArrayList getDifference (ArrayList columns1, ArrayList columns2)
	{
		ArrayList differenceColumns = new ArrayList(columns2);

		differenceColumns.removeAll(columns1);

		return differenceColumns;
	}

	/**
	 * Convenience method to call Model.getMappingClass.
	 */
	private MappingClassElement getMappingClass (String className)
	{
		return getModel().getMappingClass(className, getClassLoader());
	}

	/**
	 * Convenience method to call Model.getPersistenceClass.
	 */
	private PersistenceClassElement getPersistenceClass (String className)
	{
		return getModel().getPersistenceClass(className, getClassLoader());
	}

	/**
	 * Convenience method to call Model.isPersistent
	 */
	private boolean isPersistent (String className)
	{
		return getModel().isPersistent(className, getClassLoader());
	}

	/**
	 * Convenience method to call Model.isPersistentAllowed
	 */
	private boolean isPersistentAllowed (String className, String fieldName)
	{
		return getModel().isPersistentAllowed(className, getClassLoader(), 
			fieldName);
	}
	// ================== Validation component support =================

	/** Abstraction of component tests for validation.
	 */
	static abstract class ValidationComponent
	{
		/** Constructs a new ValidationComponent
		 */
		public ValidationComponent ()
		{
		}
		/** Method which validates this component
		 * @exception ModelValidationException when the validation fails.
		 */
		public abstract void validate () throws ModelValidationException;
	}
}
