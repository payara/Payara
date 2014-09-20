/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 * NameMapper.java
 *
 * Created on December 11, 2001, 9:51 AM
 */

package com.sun.jdo.spi.persistence.support.ejb.model.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdo.api.persistence.mapping.ejb.AbstractNameMapper;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationshipDescriptor;

/** This is a class which helps translate between the various names of the 
 * CMP (ejb name, abstract schema, abstract bean, concrete bean, local
 * interface, remote interface) and the persistence-capable class name.  It 
 * also has methods for translation of field names.  The basic entry point 
 * is ejb name or persistence-capable class name.  This is a subclass of 
 * the AbstractNameMapper and implements the methods based on DOL.  It 
 * also adds methods which are used during deployment time but not needed
 * during development time and therefore, not in the abstract superclass.
 *
 * @author Rochelle Raccah
 */
public abstract class NameMapper extends AbstractNameMapper
{
	private EjbBundleDescriptorImpl _bundleDescriptor;
	private Map _generatedRelToInverseRelMap;
	private Map _relToInverseGeneratedRelMap;

	/** Creates a new instance of NameMapper
	 * @param bundleDescriptor the EjbBundleDescriptor which defines the 
	 * universe of names for this application.
	 */
	protected NameMapper (EjbBundleDescriptorImpl bundleDescriptor)
	{
		_bundleDescriptor = bundleDescriptor;
		initGeneratedRelationshipMaps();
	}

	private void initGeneratedRelationshipMaps ()
	{
		EjbBundleDescriptorImpl bundleDescriptor = getBundleDescriptor();
		Set relationships = bundleDescriptor.getRelationships();

		_generatedRelToInverseRelMap = new HashMap();
		_relToInverseGeneratedRelMap = new HashMap();

		// during development time this code may attempt to get the 
		// iterator even with no relationships, so protect it by a 
		// null check
		if (relationships != null)
		{
			Iterator iterator = relationships.iterator();
			List generatedRels = new ArrayList();
			int counter = 0;

			// gather list of generated cmr fields by examining source and sink
			while (iterator.hasNext())
			{
				RelationshipDescriptor relationship =
					(RelationshipDescriptor)iterator.next();

				if (relationship.getSource().getCMRField() == null)
					generatedRels.add(relationship);

				if (relationship.getSink().getCMRField() == null)
					generatedRels.add(relationship);
			}

			// now update the maps to contain this info
			iterator = generatedRels.iterator();
			while (iterator.hasNext())
			{
				RelationshipDescriptor relationship = 
					(RelationshipDescriptor)iterator.next();
				RelationRoleDescriptor source = relationship.getSource();
				String sourceEjbName = source.getOwner().getName();
				String sourceCMRField = source.getCMRField();
				boolean sourceIsNull = (sourceCMRField == null);
				RelationRoleDescriptor sink = relationship.getSink();
				String sinkEjbName = sink.getOwner().getName();
				String ejbName = (sourceIsNull ? sourceEjbName : sinkEjbName);
				String otherEjbName = 
					(sourceIsNull ? sinkEjbName : sourceEjbName);
				List ejbField = Arrays.asList(new String[]{otherEjbName, 
					(sourceIsNull ? sink.getCMRField() : sourceCMRField)});
				PersistenceDescriptor pDescriptor = ((EjbCMPEntityDescriptor)
					bundleDescriptor.getEjbByName(ejbName)).
					getPersistenceDescriptor();
				List generatedField = null;
				String uniqueName = null;

				// make sure the user doesn't already have a field
				// with this name
				do
				{
					counter++;
					uniqueName = GENERATED_CMR_FIELD_PREFIX + counter;
				} while (hasField(pDescriptor, uniqueName));

				generatedField = 
					Arrays.asList(new String[]{ejbName, uniqueName});
				_generatedRelToInverseRelMap.put(generatedField, ejbField);
				_relToInverseGeneratedRelMap.put(ejbField, generatedField);
			}
		}
	}

	protected Map getGeneratedFieldsMap ()
	{
		return _generatedRelToInverseRelMap;
	}
	protected Map getInverseFieldsMap () { return _relToInverseGeneratedRelMap; }

	// isCMPField does not return true for relationships, so we use getTypeFor
	private boolean hasField (PersistenceDescriptor persistenceDescriptor, 
		String fieldName)
	{
		Class fieldType = null;

		try
		{
			fieldType = persistenceDescriptor.getTypeFor(fieldName);
		}
		catch (RuntimeException e)
		{
			// fieldType will be null - there is no such field
		}

		return (fieldType != null);
	}

	/** Gets the EjbBundleDescriptor which defines the universe of
	 * names for this application.
	 * @return the EjbBundleDescriptor which defines the universe of
	 * names for this application.
	 */
	public EjbBundleDescriptorImpl getBundleDescriptor ()
	{
		return _bundleDescriptor;
	}

	/** Gets the EjbCMPEntityDescriptor which represents the ejb  
	 * with the specified name.
	 * @param name the name of the ejb
	 * @return the EjbCMPEntityDescriptor which represents the ejb.
	 */
	abstract public EjbCMPEntityDescriptor getDescriptorForEjbName (
		String name);

	/** Get the type of key class of this ejb.
	 * @return the key class type, one of {@link #USER_DEFINED_KEY_CLASS}, 
	 * {@link #PRIMARY_KEY_FIELD}, or {@link #UNKNOWN_KEY_CLASS}
	 */
	public int getKeyClassTypeForEjbName (String name)
	{
		String keyClass = getKeyClassForEjbName(name);

		if (!"java.lang.Object".equals(keyClass))		// NOI18N
		{
			EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

			return ((descriptor.getPrimaryKeyFieldDesc() != null) ?
				PRIMARY_KEY_FIELD : USER_DEFINED_KEY_CLASS);
		}

		return UNKNOWN_KEY_CLASS;
	}

	/** Gets the name of the ejb which corresponds to the specified abstract 
	 * schema name.
	 * @param schemaName the name of the abstract schema
	 * @return the name of the ejb for the specified abstract schema
	 */
	abstract public String getEjbNameForAbstractSchema (String schemaName);

	/** Gets the name of the concrete bean class which corresponds to the 
	 * specified ejb.
	 * @param name the name of the ejb
	 * @return the name of the concrete bean for the specified ejb
	 */
	abstract public String getConcreteBeanClassForEjbName (String name);
}
