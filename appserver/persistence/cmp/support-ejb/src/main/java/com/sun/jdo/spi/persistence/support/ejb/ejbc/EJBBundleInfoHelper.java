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
 * EJBBundleInfoHelper.java
 *
 * Created on October 15, 2004, 1:51 PM
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.sun.jdo.api.persistence.mapping.ejb.AbstractNameMapper;
import com.sun.jdo.api.persistence.mapping.ejb.ConversionHelper;
import com.sun.jdo.api.persistence.mapping.ejb.EJBInfoHelper;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.spi.persistence.support.ejb.model.DeploymentDescriptorModel;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.DeploymentHelper;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationshipDescriptor;
import org.netbeans.modules.dbschema.SchemaElement;

/** This is a class which implements the EJBInfoHelper interface 
 * based on EjbBundleDescriptor and other DOL classes.
 *
 * @author Rochelle Raccah
 */
public class EJBBundleInfoHelper implements EJBInfoHelper {
	private static final char UNDERLINE = '_'; // NOI18N
	private static final char DOT = '.'; // NOI18N

	private final EjbBundleDescriptorImpl bundleDescriptor;
	private Collection availableSchemaNames;
	private NameMapper nameMapper;	// standard one
	private Model model;

	/** Creates a new instance of EJBBundleInfoHelper
	 * @param bundleDescriptor the EjbBundleDescriptor which defines the 
	 * universe of names for this application.
	 * @param availableSchemaNames a Collection of available schemas 
	 * in the application - used only during development
	 */
	public EJBBundleInfoHelper(EjbBundleDescriptorImpl bundleDescriptor,
			Collection availableSchemaNames) {
		this(bundleDescriptor, null, null, availableSchemaNames);
	}

	/** Creates a new instance of EJBBundleInfoHelper
	 * @param bundleDescriptor the EjbBundleDescriptor which defines the 
	 * universe of names for this application.
	 * @param nameMapper the NameMapper object to be used - allows 
	 * a client to supply its own mapper to the helper rather than 
	 * have the helper construct a new instance
	 * @param model the Model object to be used - allows 
	 * a client to supply its own mapper to the helper rather than 
	 * have the helper construct a new instance
	 * @param availableSchemaNames a Collection of available schemas 
	 * in the application - used only during development
	 */
	EJBBundleInfoHelper(EjbBundleDescriptorImpl bundleDescriptor,
			NameMapper nameMapper, Model model, 
			Collection availableSchemaNames) {
		this.bundleDescriptor = bundleDescriptor;
		this.nameMapper = nameMapper;
		this.model = model;
		this.availableSchemaNames = availableSchemaNames;
	}

	/** Gets the EjbBundleDescriptor which defines the universe of
	 * names for this application.
	 * @return the EjbBundleDescriptor which defines the universe of
	 * names for this application.
	 */
	private EjbBundleDescriptorImpl getBundleDescriptor() {
		return bundleDescriptor;
	}

	/** 
	 * @see EJBInfoHelper#getEjbJarDisplayName
	 */
	public String getEjbJarDisplayName() {
		return bundleDescriptor.getName();
	}

	/** Gets a collection of names of schemas defined in this
	 * ejb jar.  This implementation simply returns the list passed in
	 * the constructor or <code>null</code> if there was none supplied.
	 * @return a collection schema names
	 */
	public Collection getAvailableSchemaNames () {
		return availableSchemaNames;
	}

	/** Gets the name to use for schema generation.  This implementation 
	 * uses a combo of app name, module name, etc.
	 * @return the name to use for schema generation
	 */
	public String getSchemaNameToGenerate() {
		// make sure there is no '.' in schema name
		return DeploymentHelper.getDDLNamePrefix(
			getBundleDescriptor()).replace(DOT, UNDERLINE);
	}

	/** Gets the schema with the specified name, loading it if necessary.
	 * This implementation uses the class loader as the extra context 
	 * information used to load.
	 * @param schemaName the name of the schema to be loaded
	 * @return the schema object
	 */
	public SchemaElement getSchema(String schemaName) {
		return SchemaElement.forName(schemaName, getClassLoader());
	}

	/** 
	 * @see EJBInfoHelper#getEjbNames
	 */
	public Collection getEjbNames() {
		Iterator iterator = getBundleDescriptor().getEjbs().iterator();
		ArrayList returnList = new ArrayList();

		while (iterator.hasNext()) {
			EjbDescriptor ejb = (EjbDescriptor)iterator.next();

            if (ejb instanceof EjbCMPEntityDescriptor)
				returnList.add(ejb.getName());
		}
		
		return returnList;
	}

	/** 
	 * @see EJBInfoHelper#getFieldsForEjb
	 */
	public Collection getFieldsForEjb(String ejbName) {
		Iterator iterator = getModel().getFields(ejbName).iterator();
		ArrayList returnList = new ArrayList();

		while (iterator.hasNext())
			returnList.add(iterator.next());

		return returnList;
	}

	/** 
	 * @see EJBInfoHelper#getRelationshipsForEjb
	 */
	public Collection getRelationshipsForEjb(String ejbName) {
		Iterator iterator = getBundleDescriptor().getRelationships().iterator();
		ArrayList returnList = new ArrayList();

		// TODO: issue of usage of this - several iterations of this if
		// iterating all the bean - but, I think it can change, so can't 
		// cache it in a map (same comment applies to getEjbNames and 
		// getFieldsForEjb)
		while (iterator.hasNext()) {
			RelationshipDescriptor relD =
				(RelationshipDescriptor)iterator.next();
			RelationRoleDescriptor testRole = relD.getSource();
			String cmrField = null;

			if (ejbName.equals(testRole.getOwner().getName())) {
				cmrField = testRole.getCMRField();
				if (cmrField != null)
					returnList.add(cmrField);
			}

			testRole = relD.getSink();
			if (ejbName.equals(testRole.getOwner().getName())) {
				cmrField = testRole.getCMRField();
				if (cmrField != null)
					returnList.add(cmrField);
			}
		}

		return returnList;
	}

	/** Gets the class loader which corresponds to this ejb bundle.
	 * @return the class loader which corresponds to this ejb bundle
	 */
	public ClassLoader getClassLoader() {
		return bundleDescriptor.getClassLoader();
	}

	/** 
	 * @see EJBInfoHelper#getNameMapper
	 */
	public AbstractNameMapper getNameMapper() {
		return getNameMapperInternal();
	}

	/** 
	 * @see EJBInfoHelper#createUniqueNameMapper
	 */
	public AbstractNameMapper createUniqueNameMapper() {
		return new NameMapper(bundleDescriptor);
	}

	private NameMapper getNameMapperInternal() {
		if (nameMapper == null)
			nameMapper = new NameMapper(bundleDescriptor, false);

		return nameMapper;
	}

	/** 
	 * @see EJBInfoHelper#createConversionHelper
	 */
	public ConversionHelper createConversionHelper() {
		return new EjbConversionHelper(getNameMapperInternal());
	}

	/** 
	 * @see EJBInfoHelper#getModel
	 */
	public Model getModel() {
		if (model == null) {
			model = new DeploymentDescriptorModel(getNameMapperInternal(), 
				getClassLoader());
		}

		return model;
	}
}
