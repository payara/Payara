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
 * Created on December 3, 2001, 5:09 PM
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import org.glassfish.ejb.deployment.descriptor.CMRFieldInfo;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;

/** This is a subclass of 
 * {@link com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper} (in 
 * the <code>com.sun.jdo.spi.persistence.support.ejb.model.util</code> 
 * package) which implements the abstract methods based on an IAS 
 * implementation.
 *
 * @author Rochelle Raccah
 */
public class NameMapper extends 
	com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper
{
	private static String EJB_NAME = "EJB_NAME"; // NOI18N
	private static String ABSTRACT_SCHEMA_NAME = "ABSTRACT_SCHEMA_NAME"; // NOI18N
	private static String PERSISTENCE_NAME = "PERSISTENCE_NAME"; // NOI18N
	private static String LOCAL_NAME = "LOCAL_NAME"; // NOI18N
	private static String REMOTE_NAME = "REMOTE_NAME"; // NOI18N

	private final boolean _expandPCNames;
	private Map _nameTypeToNameMap;

    /**
     * Signature with CVS keyword substitution for identifying the generated code
     */
    public static final String SIGNATURE = "$RCSfile: NameMapper.java,v $ $Revision: 1.2 $"; //NOI18N         
        
	/** Creates a new instance of NameMapper
	 * @param bundleDescriptor the IASEjbBundleDescriptor which defines the 
	 * universe of names for this application.
	 */
	public NameMapper (EjbBundleDescriptorImpl bundleDescriptor)
	{
		this(bundleDescriptor, true);
	}

	/** Creates a new instance of NameMapper
	 * @param bundleDescriptor the IASEjbBundleDescriptor which defines the 
	 * universe of names for this application.
	 * @param expandPersistenceClassNames flag to indicate whether 
	 * persistence class names should differ from bean names
	 */
	public NameMapper (EjbBundleDescriptorImpl bundleDescriptor,
		boolean expandPersistenceClassNames)
	{
		super(bundleDescriptor);
		_expandPCNames = expandPersistenceClassNames;
		initMap();
	}

	private void initMap ()
	{
		Iterator iterator = getBundleDescriptor().getEjbs().iterator();
		Map ejbMap = new HashMap();
		Map persistenceClassMap = new HashMap();
		Set localNames = new HashSet();
		Set remoteNames = new HashSet();
		Map abstractSchemaMap = new HashMap();

		_nameTypeToNameMap = new HashMap();

		while (iterator.hasNext())
		{
			Object next = iterator.next();

			if (next instanceof IASEjbCMPEntityDescriptor)
			{
				IASEjbCMPEntityDescriptor descriptor = 
					(IASEjbCMPEntityDescriptor)next;
				String ejbName = descriptor.getName();

				ejbMap.put(ejbName, descriptor);
				safePut(persistenceClassMap, 
					getPersistenceClassForDescriptor(descriptor), ejbName);
				safeAdd(localNames, descriptor.getLocalClassName());
				safeAdd(remoteNames, descriptor.getRemoteClassName());
				safePut(abstractSchemaMap, 
					descriptor.getAbstractSchemaName(), ejbName);
			}
		}
		_nameTypeToNameMap.put(EJB_NAME, ejbMap);
		_nameTypeToNameMap.put(PERSISTENCE_NAME, persistenceClassMap);
		_nameTypeToNameMap.put(LOCAL_NAME, localNames);
		_nameTypeToNameMap.put(REMOTE_NAME, remoteNames);
		_nameTypeToNameMap.put(ABSTRACT_SCHEMA_NAME, abstractSchemaMap);
	}

	// puts a key-value pair in a map as long as the key is not null
	private void safePut (Map map, Object key, Object value)
	{
		if ((key != null) && (map != null))
			map.put(key, value);
	}
	// puts a value in a set as long as the object is not null
	private void safeAdd (Set set, Object value)
	{
		if ((value != null) && (set != null))
			set.add(value);
	}

	private Map getMap () { return _nameTypeToNameMap; }

	/** Determines if the specified name represents an ejb.
	 * @param name the fully qualified name to be checked
	 * @return <code>true</code> if this name represents an ejb; 
	 * <code>false</code> otherwise.
	 */
	public boolean isEjbName (String name)
	{
		return mapContainsKey(EJB_NAME, name);
	}

	/** Gets the EjbCMPEntityDescriptor which represents the ejb  
	 * with the specified name.
	 * @param name the name of the ejb
	 * @return the EjbCMPEntityDescriptor which represents the ejb.
	 */
	public EjbCMPEntityDescriptor getDescriptorForEjbName (String name)
	{
		Map ejbMap = (Map)getMap().get(EJB_NAME);
		Object descriptor = ejbMap.get(name);

		return (((descriptor != null) && 
			(descriptor instanceof EjbCMPEntityDescriptor)) ? 
			(EjbCMPEntityDescriptor)descriptor : null);
	}

	private IASEjbCMPEntityDescriptor getIASDescriptorForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return (((descriptor != null) && 
			(descriptor instanceof IASEjbCMPEntityDescriptor)) ? 
			(IASEjbCMPEntityDescriptor)descriptor : null);
	}

	/** Gets the name of the abstract bean class which corresponds to the 
	 * specified ejb name.
	 * @param name the name of the ejb
	 * @return the name of the abstract bean for the specified ejb
	 */
	public String getAbstractBeanClassForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return ((descriptor != null) ? descriptor.getEjbClassName() : null);
	}

	/** Gets the name of the key class which corresponds to the specified 
	 * ejb name.
	 * @param name the name of the ejb
	 * @return the name of the key class for the ejb
	 */
	public String getKeyClassForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return ((descriptor != null) ? 
			descriptor.getPrimaryKeyClassName() : null);
	}

	/** Gets the name of the ejb which corresponds to the specified abstract 
	 * schema name.
	 * @param schemaName the name of the abstract schema
	 * @return the name of the ejb for the specified abstract schema
	 */
	public String getEjbNameForAbstractSchema (String schemaName)
	{
		Map abstractSchemaMap = (Map)getMap().get(ABSTRACT_SCHEMA_NAME);

		return (String)abstractSchemaMap.get(schemaName);
	}

	/** Gets the name of the abstract schema which corresponds to the 
	 * specified ejb.
	 * @param name the name of the ejb
	 * @return the name of the abstract schema for the specified ejb
	 */
	public String getAbstractSchemaForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return ((descriptor != null) ? 
			descriptor.getAbstractSchemaName() : null);
	}

	/** Gets the name of the concrete bean class which corresponds to the 
	 * specified ejb.
	 * @param name the name of the ejb
	 * @return the name of the concrete bean for the specified ejb
	 */
	public String getConcreteBeanClassForEjbName (String name)
	{
		IASEjbCMPEntityDescriptor descriptor = 
			getIASDescriptorForEjbName(name);

		return ((descriptor != null) ? getQualifiedName(
			getAbstractBeanClassForEjbName(name), 
			descriptor.getConcreteImplClassName()) : null);
	}

	private String getQualifiedName (String classNameWithPackage, 
		String classNameToQualify)
	{
		if (!StringHelper.isEmpty(classNameToQualify))
		{
			String packageName = 
				JavaTypeHelper.getPackageName(classNameToQualify);
			
			if (StringHelper.isEmpty(packageName))	// not already qualified
			{
				packageName = 
					JavaTypeHelper.getPackageName(classNameWithPackage);

				if (!StringHelper.isEmpty(packageName))
					return packageName + '.' + classNameToQualify;
			}
		}

		return classNameToQualify;
	}

	/** Gets the name of the ejb name which corresponds to the 
	 * specified persistence-capable class name.
	 * @param className the name of the persistence-capable
	 * @return the name of the ejb for the specified persistence-capable
	 */
	public String getEjbNameForPersistenceClass (String className)
	{
		Map pcMap = (Map)getMap().get(PERSISTENCE_NAME);

		return (String)pcMap.get(className);
	}

	/** Gets the name of the persistence-capable class which corresponds to 
	 * the specified ejb name.
	 * @param name the name of the ejb
	 * @return the name of the persistence-capable for the specified ejb
	 */
	public String getPersistenceClassForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return ((descriptor != null) ? 
			getPersistenceClassForDescriptor(descriptor) : null);
	}

	private String getPersistenceClassForDescriptor (
		EjbCMPEntityDescriptor descriptor)
	{
		String pcName = ((descriptor instanceof IASEjbCMPEntityDescriptor) ? 
			((IASEjbCMPEntityDescriptor)descriptor).getPcImplClassName() : 
			null);

		// use the package name, keep the ejb name
		if ((pcName != null) && !_expandPCNames)
		{
			pcName = JavaTypeHelper.getPackageName(pcName) + 
				'.' + descriptor.getName();
		}

		return pcName;
	}

	private boolean mapContainsKey (String stringIndex, String name)
	{
		Object mapObject = getMap().get(stringIndex);
		Set testSet = ((mapObject instanceof Set) ? (Set)mapObject : 
			((Map)mapObject).keySet());

		return ((name != null) ? testSet.contains(name) : false);
	}

	/** Determines if the specified name represents a local interface.
	 * @param name the fully qualified name to be checked
	 * @return <code>true</code> if this name represents a local interface; 
	 * <code>false</code> otherwise.
	 */
	public boolean isLocalInterface (String name)
	{
		return mapContainsKey(LOCAL_NAME, name);
	}

	/** Gets the name of the ejb which corresponds to the specified 
	 * local interface name.
	 * @param ejbName the name of the ejb which contains fieldName 
	 * from which to find relationship and therefore the local interface
	 * @param fieldName the name of the field in the ejb
	 * @param interfaceName the name of the local interface
	 * @return the name of the ejb for the specified local interface
	 */
	public String getEjbNameForLocalInterface (String ejbName, 
		String fieldName, String interfaceName)
	{
		EjbCMPEntityDescriptor descriptor = 
			getRelatedEjbDescriptor(ejbName, fieldName);

		return (((descriptor != null) && !StringHelper.isEmpty(interfaceName)
			&& interfaceName.equals(descriptor.getLocalClassName())) ? 
			descriptor.getName() : null);
	}

	/** Gets the name of the local interface which corresponds to the 
	 * specified ejb name.
	 * @param name the name of the ejb
	 * @return the name of the local interface for the specified ejb
	 */
	public String getLocalInterfaceForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return ((descriptor != null) ? descriptor.getLocalClassName() : null);
	}

	/** Determines if the specified name represents a remote interface.
	 * @param name the fully qualified name to be checked
	 * @return <code>true</code> if this name represents a remote interface; 
	 * <code>false</code> otherwise.
	 */
	public boolean isRemoteInterface (String name)
	{
		return mapContainsKey(REMOTE_NAME, name);
	}

	/** Gets the name of the ejb which corresponds to the specified 
	 * remote interface name.
	 * @param ejbName the name of the ejb which contains fieldName 
	 * from which to find relationship and therefore the remote interface
	 * @param fieldName the name of the field in the ejb
	 * @param interfaceName the name of the remote interface
	 * @return the name of the ejb for the specified remote interface
	 */
	public String getEjbNameForRemoteInterface (String ejbName, 
		String fieldName, String interfaceName)
	{
		EjbCMPEntityDescriptor descriptor = 
			getRelatedEjbDescriptor(ejbName, fieldName);

		return (((descriptor != null) && !StringHelper.isEmpty(interfaceName)
			&& interfaceName.equals(descriptor.getRemoteClassName())) ? 
			descriptor.getName() : null);
	}

	/** Gets the name of the remote interface which corresponds to the 
	 * specified ejb name.
	 * @param name the name of the ejb
	 * @return the name of the remote interface for the specified ejb
	 */
	public String getRemoteInterfaceForEjbName (String name)
	{
		EjbCMPEntityDescriptor descriptor = getDescriptorForEjbName(name);

		return ((descriptor != null) ? descriptor.getRemoteClassName() : null);
	}

	private EjbCMPEntityDescriptor getRelatedEjbDescriptor (
		String ejbName, String ejbFieldName)
	{
		EjbCMPEntityDescriptor descriptor = ((ejbName != null) ? 
			getDescriptorForEjbName(ejbName) : null);

		if (descriptor != null)
		{
			PersistenceDescriptor persistenceDescriptor =
				descriptor.getPersistenceDescriptor();
			CMRFieldInfo cmrf =
				persistenceDescriptor.getCMRFieldInfoByName(ejbFieldName);
			
			return cmrf.role.getPartner().getOwner();
		}

		return null;
	}

	/** Gets the name of the field in the ejb which corresponds to the 
	 * specified persistence-capable class name and field name pair.
	 * @param className the name of the persistence-capable
	 * @param fieldName the name of the field in the persistence-capable
	 * @return the name of the field in the ejb for the specified 
	 * persistence-capable field
	 */
	public String getEjbFieldForPersistenceField (String className, 
		String fieldName)
	{
		return fieldName;
	}

	/** Gets the name of the field in the persistence-capable class which 
	 * corresponds to the specified ejb name and field name pair.
	 * @param name the name of the ejb
	 * @param fieldName the name of the field in the ejb
	 * @return the name of the field in the persistence-capable for the 
	 * specified ejb field
	 */
	public String getPersistenceFieldForEjbField (String name, String fieldName)
	{
		return fieldName;
	}
}
