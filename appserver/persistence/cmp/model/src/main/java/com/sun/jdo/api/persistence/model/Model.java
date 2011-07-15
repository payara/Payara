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
 * Model.java
 *
 * Created on March 9, 2000, 6:19 PM
 */

package com.sun.jdo.api.persistence.model;

import java.io.*;
import java.util.*;
import java.lang.reflect.Modifier;

import org.netbeans.modules.dbschema.migration.archiver.XMLInputStream;
import org.netbeans.modules.dbschema.migration.archiver.XMLOutputStream;

import org.netbeans.modules.dbschema.SchemaElement;
import com.sun.jdo.api.persistence.model.util.LogHelperModel;
import com.sun.jdo.api.persistence.model.util.ModelValidator;
import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.api.persistence.model.jdo.impl.*;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.api.persistence.model.mapping.MappingFieldElement;
import com.sun.jdo.api.persistence.model.mapping.impl.MappingClassElementImpl;
import com.sun.jdo.spi.persistence.utility.*;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

/* TODO:
	1. think about moving illegal lists of static info out to a properties file
	2. think about throwing an exception or setting the declaring class/table
	on add member in both models (jdo and mapping).
	3. add an extra level (bridge pattern) for java elements (generic, member,
	class, field) so can use those instead of names
	4. javadoc: @exception vs. @throws
	5. review javadoc, try generating with -link to java.sun.com for Integer
	calls
	6. Move various ARGS constants to a better place, like an extra class.

 */

/**
 *
 * @author raccah
 * @version %I%
 */
public abstract class Model
{
	/** Default instance of the model for use at runtime. */
	public static final Model RUNTIME;

	/** Default instance of the model used by the enhancer. */
	public static final Model ENHANCER;

	/** Standard set of empty arguments (for comparison with hashCode method
	 * and no-arg constructor).
	 */
	public static final String[] NO_ARGS = new String[0];

	/** Map of mapping class elements which have been loaded.  Keys are fully
	 * qualified class names.
	 */
	private final Map _classes = new WeakValueHashMap();

	/** Set of fully qualified names of classes known to be 
	 * non persistence-capable. 
	 */
	private final Set _nonPCClasses = new WeakHashSet();

	/** List of illegal package name prefixes for superclasses of
	 * persistence capable classes.
	 */
	private static List _illegalPrefixes;

	/** List of illegal class names for superclasses of persistence capable
	 * classes.
	 */
	private static List _illegalClasses;

	/** List of class names for second class objects. */
	private static List _scoClasses;

	/** List of class names for mutable second class objects. */
	private static List _mutableScoClasses;

	/** List of class names for collections. */
	private static List _collectionClasses;

	/** I18N message base */
	public static final String messageBase = 
		"com.sun.jdo.api.persistence.model.Bundle"; // NOI18N

	/** I18N message handler */
	private static final ResourceBundle _messages = I18NHelper.loadBundle(
		Model.class);

	static
	{
		String prefixes[] =
			{"java.awt", "java.applet", "javax.swing", "javax.ejb"};// NOI18N
		String classes[] = {"java.lang.Throwable"};					// NOI18N
		String collectionClasses[] = { "java.util.Collection",		// NOI18N
			"java.util.AbstractCollection", 						// NOI18N
			//"java.util.List", "java.util.AbstractList",			// NOI18N
			"java.util.Set", "java.util.AbstractSet", 				// NOI18N
			//"java.util.ArrayList", "java.util.Vector", 			// NOI18N
			"java.util.HashSet", 									// NOI18N
			//"com.sun.jdo.spi.persistence.support.sqlstore.sco.ArrayList",	// NOI18N
			//"com.sun.jdo.spi.persistence.support.sqlstore.sco.Vector", // NOI18N
			"com.sun.jdo.spi.persistence.support.sqlstore.sco.HashSet"}; // NOI18N
		String mutableScoClasses[] = {"java.util.Date", 			// NOI18N
			"com.sun.jdo.spi.persistence.support.sqlstore.sco.Date", "java.sql.Date",// NOI18N
			"com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlDate", // NOI18N
			"java.sql.Time", "com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTime", // NOI18N
			"java.sql.Timestamp", 									// NOI18N
			"com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTimestamp"};		// NOI18N
		String scoClasses[] = {"java.lang.String", 					// NOI18N
			"java.lang.Character", "java.lang.Boolean", 			// NOI18N
			"java.lang.Long", "java.lang.Number", "java.lang.Byte", // NOI18N
			"java.lang.Short", "java.lang.Integer", "java.lang.Float", // NOI18N
			"java.lang.Double", "java.math.BigDecimal",				// NOI18N
			"java.math.BigInteger"};								// NOI18N

		_illegalPrefixes = Arrays.asList(prefixes);
		_illegalClasses = Arrays.asList(classes);
		_collectionClasses = Arrays.asList(collectionClasses);
		_mutableScoClasses = new ArrayList(Arrays.asList(mutableScoClasses));
		_mutableScoClasses.addAll(_collectionClasses);
		_scoClasses = new ArrayList(Arrays.asList(scoClasses));
		_scoClasses.addAll(_mutableScoClasses);

		// always load the runtime model
		RUNTIME = NewModel(null, "com.sun.jdo.api.persistence.model.RuntimeModel"); //NOI18N
		// always load the enhancer model
		ENHANCER = NewModel(null, "com.sun.jdo.api.persistence.model.EnhancerModel"); //NOI18N
	}

		/** Create a new Model of the requested type.  If the class definition
		 * exists in the class path of the environment, then this method will
		 * create a new instance of the Model.
		 * @param modelName the fully qualified name of the class to be 
		 * instantiated.
		 * @param testName the fully qualified name of the class to be tested 
		 * as a precondition to loading.
		 * @return a new instance of the requested class (which implements 
		 * Model).
		 */
		static protected Model NewModel (String testName, String modelName) {
			Class DynamicClass = null;
			Model model = null;
			try {
				if (testName != null)
					// try this class as a precondition to the real class to load
					Class.forName (testName); 
				DynamicClass = Class.forName (modelName);
				if (DynamicClass != null)
					model = (Model) DynamicClass.newInstance();
			}
			catch (Exception e) {
				// this is expected in the environment
			}
			return model;
		}
	/** @return I18N message handler for this element
	 */
	protected static final ResourceBundle getMessages ()
	{
		return _messages;
	}

	/** Returns the input stream with the supplied resource name found with
	 * the supplied class name.
	 * @param className the fully qualified name of the class which will be
	 * used as a base to find the resource
	 * @param classLoader the class loader used to find mapping information
	 * @param resourceName the name of the resource to be found
	 * @return the input stream for the specified resource, <code>null</code>
	 * if an error occurs or none exists
	 */
	abstract protected BufferedInputStream getInputStreamForResource (
		String className, ClassLoader classLoader, String resourceName);

	/** Determines if the specified className represents an interface type.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class name represents an interface;
	 * <code>false</code> otherwise.
	 */
 	abstract public boolean isInterface (String className);

	/** Determines if the specified className has a persistent superclass.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class name represents a class which
	 * has a persistent superclass (anywhere in the inheritance chain);
	 * <code>false</code> otherwise.
	 */
 	public boolean hasPersistentSuperclass (String className)
	{
		while ((className = getSuperclass(className)) != null)
		{
			if (isPersistent(className))
				return true;
		}

		return false;
	}

	/** Returns the name of the second to top (top excluding java.lang.Object)
	 * superclass for the given class name.
	 * @param className the fully qualified name of the class to be checked
	 * @return the top non-Object superclass for className,
	 * <code>className</code> if an error occurs or none exists
	 */
	abstract protected String findPenultimateSuperclass (String className);

	/** Returns the name of the superclass for the given class name.
	 * @param className the fully qualified name of the class to be checked
	 * @return thesuperclass for className, <code>null</code> if an error
	 * occurs or none exists
	 */
	abstract protected String getSuperclass (String className);

	/** Returns a PersistenceClassElement created from the specified class name.
	 * Since our implementation of the mapping model class includes the
	 * persistence class, this method finds the persistence class by extracting
	 * it from the mapping class for the supplied name.
	 * @param className the fully qualified name of the persistence capable
	 * class to be returned
	 * @return the PersistenceClassElement for className,
	 * <code>null</code> if an error occurs or none exists
	 * @see #getMappingClass
	 */
	public PersistenceClassElement getPersistenceClass (String className)
	{
		return getPersistenceClass(className, null);
	}

	/** Returns a PersistenceClassElement created from the specified class name.
	 * Since our implementation of the mapping model class includes the
	 * persistence class, this method finds the persistence class by extracting
	 * it from the mapping class for the supplied name.
	 * @param className the fully qualified name of the persistence capable
	 * class to be returned
	 * @param classLoader the class loader used to find mapping information
	 * @return the PersistenceClassElement for className,
	 * <code>null</code> if an error occurs or none exists
	 * @see #getMappingClass
	 */
	public PersistenceClassElement getPersistenceClass (String className, 
		ClassLoader classLoader)
	{
		return getPersistenceClass(getMappingClass(className, classLoader));
	}

	/** Returns a PersistenceClassElement created from the mapping class.
	 * @param mappingClass the mapping class element to which the persistence
	 * class is associated
	 * @return the PersistenceClassElement for mappingClass,
	 * <code>null</code> if an error occurs or none exists
	 * @see #getMappingClass
	 */
	protected PersistenceClassElement getPersistenceClass (
		MappingClassElement mappingClass)
	{
		return ((mappingClass == null) ? null :
			((MappingClassElementImpl)mappingClass).getPersistenceElement());
	}

	/** Returns the MappingClassElement created for the specified class name.
	 * This method looks up the class in the internal cache. If not present 
	 * it loads the corresponding xml file containing the mapping information. 
	 * @param className the fully qualified name of the mapping class
	 * @return the MappingClassElement for class,
	 * <code>null</code> if an error occurs or none exists
	 */
	public MappingClassElement getMappingClass (String className)
	{
		return getMappingClass(className, null);
	}

	/** Returns the MappingClassElement created for the specified class name.
	 * This method looks up the class in the internal cache. If not present 
	 * it loads the corresponding xml file containing the mapping information. 
	 * @param className the fully qualified name of the mapping class
	 * @param classLoader the class loader used to find mapping information
	 * @return the MappingClassElement for className,
	 * <code>null</code> if an error occurs or none exists
	 * @see MappingClassElementImpl#forName
	 */
	public MappingClassElement getMappingClass (String className, 
	   ClassLoader classLoader)
	{
		// This method synchronizes the access of the _classes cache, 
		// rather than using a synchronized map. This is for optimization only. 
		// Otherwise two parallel calls would read the mapping file twice, 
		// create two MCE instances and the second MCE instance would replace 
		// the first in the cache.
		// Any other access of _classes potentially needs to be synchronized 
		// using the same variable _classes (e.g. updateKeyForClass).
		synchronized (this._classes)
		{
			MappingClassElement mappingClass =
				(MappingClassElement)_classes.get(className);

			if (mappingClass == null)
			{
				// check whether the class is known to be non PC
				if (_nonPCClasses.contains(className))
					return null;

				try
				{
					InputStream stream = getInputStreamForResource(className,
						classLoader, getResourceNameWithExtension(className));

					if (stream != null)
					{
						// if the file is empty, the archiver prints an 
						// exception, so protect against that case and 
						// return null without updating either cache
						if (stream.available() > 0)
						{
							XMLInputStream xmlInput = new XMLInputStream(stream,
								getClass().getClassLoader());

							mappingClass = 
								(MappingClassElement)xmlInput.readObject();
							xmlInput.close();

							// postUnarchive performs version number checking 
							// and possible format conversions
							mappingClass.postUnarchive();

							// can't call updateKeyForClass here there are cases
							// when the mapping class name doesn't match the
							// classname (such as copy/paste, move etc.)
							_classes.put(className, mappingClass);

							// update the modified flags for the mapping and 
							// persistence classes since the xml archiver uses 
							// all the set methods
							mappingClass.setModified(false);
							getPersistenceClass(mappingClass).
								setModified(false);
						}
					}
					else
					{
						// stream is null, mapping file does not exist => 
						// class is not PC, so store the class name in the 
						// set of classes known to be non PC
						_nonPCClasses.add(className);
					}
				}
				catch (ModelException e)
				{
					// MBO: print reason to logger
					LogHelperModel.getLogger().log(Logger.WARNING, 
						e.getMessage());
					return null;
				}
				catch (Exception e)
				{
					// MBO: print reason to logger
					LogHelperModel.getLogger().log(Logger.WARNING, 
						I18NHelper.getMessage(getMessages(),
						"file.cannot_read", className, e.toString())); //NOI18N
				}	// will return null
			}

			return mappingClass;
		}
	}

	/** Returns an unmodifiable copy of the MappingClassElement cache.
	 * @return unmodifiable MappingClassElement cache
	 */
	public Map getMappingCache ()
	{
		return Collections.unmodifiableMap(_classes);
	}

	/** Returns an unmodifiable copy of the ClassLoader cache.
	 * This implementation returns null, but subclasses (such as RuntimeModel)
	 * can override this method if they support a class loader cache.
	 * @return unmodifiable ClassLoader cache
	 */
	public Map getClassLoaderCache ()
	{
		return null;
	}

	/** Removes the classes cached with the specified class loader from all
	 * caches. 
	 * This implementation does nothing, but subclasses (such as RuntimeModel)
	 * can override this method if they support a class loader cache.
	 * @param classLoader used to determine the classes to be removed
	 */
	public void removeResourcesFromCaches (ClassLoader classLoader)
	{
		// Do nothing in the top-level model.
	}
	
	/** Removes the specified classes from all caches. The specified
	 * collection includes the fully qualified class names of the classes
	 * to be removed. The method removes each class from the cache of
	 * MappingClassElements and the set of classes known to be non
	 * PC. Furthermore it removes the SchemaElement associated with this
	 * class from the SchemaElement cache. The next call getMappingClass
	 * will determine the status of the classes. 
	 * @param classNames a collection of fully qualified class names
	 */
	protected void removeResourcesFromCaches (Collection classNames)
	{
		if (classNames == null)
			return;

		synchronized (this._classes)
		{
			for (Iterator i = classNames.iterator(); i.hasNext();)
			{
				String className = (String)i.next();
				MappingClassElement mapping = 
					(MappingClassElement)_classes.get(className);

				// If the cache has a MappingClassElement with the specified
				// className, get its databaseRoot and remove the corresonding
				// SchemaElement from the SchemaElement cache. 
				if (mapping != null)
					SchemaElement.removeFromCache(mapping.getDatabaseRoot());
				
				// remove the corresponding MappingClassElement from cache
				_classes.remove(className);
				
				// remove the class from the set of classes known to be non PC
				_nonPCClasses.remove(className);
			}
		}
	}

	/** Removes the class with the supplied name from the cache of  
	 * classes known to be non PC. 
	 * The next call getMappingClass will determine the status of the class.
 	 * @param className the fully qualified name of the class
	 */
	public void removeFromCache (String className)
	{
		synchronized (this._classes)
		{
			// remove the class from the set of classes known to be non PC
			_nonPCClasses.remove(className);
		}
	}

	/** Stores the supplied MappingClassElement to an xml file, creating the
	 * file if necessary. The caller is responsible for updating the cache
	 * by calling updateKeyForClass, if necessary.
	 * @param mappingClass the mapping class to be saved
	 * @exception IOException if there is some error saving the class
	 * @see #createFile
	 */
	public void storeMappingClass (MappingClassElement mappingClass)
		throws IOException
	{
		if (mappingClass != null)
		{
			String className = mappingClass.getName();
			OutputStream stream = ((className == null) ? null :
				createFile(className, getFileName(className),
				MappingClassElement.MAPPING_EXTENSION));

			storeMappingClass(mappingClass, stream);
		}
	}

	/** Stores the supplied MappingClassElement to an xml file in the  
	 * specified output stream.  The caller is responsible for updating 
	 * the cache by calling updateKeyForClass, if necessary.
	 * @param mappingClass the mapping class to be saved
	 * @param stream the output stream 
	 * @exception IOException if there is some error saving the class
	 * @see #createFile
	 */
	public void storeMappingClass (MappingClassElement mappingClass,
		OutputStream stream) throws IOException
	{
		if (mappingClass != null)
		{
			String className = mappingClass.getName();

			if (stream != null)
			{
				XMLOutputStream xmlOutput = new XMLOutputStream(stream);

				try
				{
					mappingClass.preArchive();		// call pre archive hook
					xmlOutput.writeObject(mappingClass);

					// update modified flags for the mapping and persistence
					// classes after save
					mappingClass.setModified(false);
					getPersistenceClass(mappingClass).setModified(false);
				}
				catch (ModelException e)
				{
					// MBO: print reason to logger
					LogHelperModel.getLogger().log(Logger.WARNING, 
						e.getMessage());
				}
				finally
				{
					if (xmlOutput != null)
						xmlOutput.close();

					unlockFile(stream, className);
				}
				return;
			}

			throw new IOException(I18NHelper.getMessage(getMessages(),
				"file.cannot_save", className));				// NOI18N
		}
	}

	public void unlockFile (OutputStream stream, String className)
		throws IOException
	{
		unlockFile(className);

		if (stream != null)
			stream.close();
	}

	// overridden in DevelopmentModel
	public void lockFile (String className) throws IOException {}

	// overridden in DevelopmentModel
	public void unlockFile (String className) {}

	/** Stores the MappingClassElement for the specified class name to an xml
	 * file, creating the file if necessary.  The MappingClassElement must be
	 * present in the HashMap of classes known by the Model in order to stored.
	 * @param className the fully qualified name of the mapping class
	 * @exception IOException if there is some error saving the class
	 * @see #storeMappingClass
	 */
	public void storeMappingClass (String className) throws IOException
	{
		MappingClassElement mappingClass = null;

		synchronized (this._classes)
		{
			mappingClass = (MappingClassElement)_classes.get(className);
		}
		storeMappingClass(mappingClass);
	}

	/** Updates the key in the cache for the supplied MappingClassElement.
	 * @param mappingClass the mapping class to be put in the cache
	 * (the new name is extracted from this element).  The corresponding 
	 * handling of the files is automatically handled by the data object. 
	 * (use <code>null</code> to remove the old key but not replace it)
	 * @param oldName the fully qualified name of the old key for the mapping 
	 * class (use <code>null</code> to add the new key but not replace it)
	 */
	public void updateKeyForClass (MappingClassElement mappingClass,
		String oldName)
	{
		// need to synchronize _classes access here 
		// (for details see getMappingClass)
		synchronized (this._classes)
		{
			// remove the old key from the cache
			if (oldName != null)
				_classes.remove(oldName);

			// store the class under the new key in the cache
			if (mappingClass != null)
			{
				String className = mappingClass.getName();

				_classes.put(className, mappingClass);

				// ensure that the name of the mappingClass does not occur 
				// in the list of classes known to be non PC
				_nonPCClasses.remove(className);
			}
		}
	}

	/** Determines if the specified className represents a persistence capable
	 * class.  A class is persistence capable only if it is directly marked as
	 * such -- not by inheritance.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class name represents a persistence
	 * capable class; <code>false</code> otherwise.
	 */
	public boolean isPersistent (String className)
	{
		return isPersistent(className, (ClassLoader)null);
	}


	/** Determines if the specified className represents a persistence capable
	 * class.  A class is persistence capable only if it is directly marked as
	 * such -- not by inheritance.
	 * @param className the fully qualified name of the class to be checked
	 * @param classLoader the class loader used to find mapping information
	 * @return <code>true</code> if this class name represents a persistence
	 * capable class; <code>false</code> otherwise.
	 */
	public boolean isPersistent (String className, ClassLoader classLoader)
	{
		return (getPersistenceClass(className, classLoader) != null);
	}

	/** Determines if the specified className represents a legal candidate for
	 * becoming a persistence capable class.  A class may not become
	 * persistence capable if it is declared as static or abstract, an
	 * interface, a subclass of another persistence capable class
	 * (either direct or indirect), an exception subclass, or a subclass
	 * of ejb, swing, awt, or applet classes.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class name represents a legal
	 * candidate for becoming a persistence capable class;
	 * <code>false</code> otherwise.
	 * @see #getModifiersForClass
	 * @see #isInterface
	 * @see #findPenultimateSuperclass
	 */
	public boolean isPersistenceCapableAllowed (String className)
	{
		int modifier = getModifiersForClass(className);

		if (!Modifier.isStatic(modifier) && !Modifier.isAbstract(modifier) &&
			!isInterface(className) && !hasPersistentSuperclass(className))
		{
			String highestSuperclassName = findPenultimateSuperclass(className);
			Iterator iterator = _illegalPrefixes.iterator();

			while (iterator.hasNext())
			{
				String nextPrefix = iterator.next().toString();

				if (highestSuperclassName.startsWith(nextPrefix))
					return false;
			}

			iterator = _illegalClasses.iterator();
			while (iterator.hasNext())
			{
				String nextClass = iterator.next().toString();

				if (highestSuperclassName.equals(nextClass))
					return false;
			}

			return true;
		}

		return false;
	}

	/** Computes the mapping file resource name (with extension) for the 
	 * supplied class name by converting the package name to a resource name.
	 * @param className the fully qualified name of the class
	 * @return the mapping file resource name (with extension) for the supplied
	 * class name
	 * @see MappingClassElement#MAPPING_EXTENSION
	 */
	protected String getResourceNameWithExtension (String className)
	{
		return getResourceName(className) + "." + 					// NOI18N
			MappingClassElement.MAPPING_EXTENSION;
	}

	/** Computes the base resource name (without extension) for the supplied
	 * class name by converting the package name to a resource name.
	 * @param className the fully qualified name of the class
	 * @return the base resource name (without extension) for the supplied
	 * class name
	 */
	protected String getResourceName (String className)
	{
		return ((className != null) ?
			className.replace('.', '/') : null);
	}

	/** Computes the mapping file name (with extension) for the supplied
	 * class name by converting the package name to a path name.
	 * @param className the fully qualified name of the class
	 * @return the mapping file name (with extension) for the supplied
	 * class name
	 * @see #getFileName
	 * @see MappingClassElement#MAPPING_EXTENSION
	 */
	protected String getFileNameWithExtension (String className)
	{
		return getFileName(className) + "." + 						// NOI18N
			MappingClassElement.MAPPING_EXTENSION;
	}

	/** Computes the base file name (without extension) for the supplied
	 * class name by converting the package name to a path name.
	 * @param className the fully qualified name of the class
	 * @return the base file name (without extension) for the supplied
	 * class name
	 */
	protected String getFileName (String className)
	{
		return ((className != null) ?
			className.replace('.', File.separatorChar) : null);
	}

	/** Converts the class with the supplied name to or from persistence
	 * capable depending on the flag.
	 * @param className the fully qualified name of the class
	 * @param flag if <code>true</code>, convert this class to be
	 * persistence capable, if <code>false</code>, convert this class
	 * to be non-persistence capable
	 * @exception IOException if there is some error converting the class
	 */
	public void convertToPersistenceCapable (String className,
		boolean flag) throws IOException
	{
		boolean classIsPersistent = isPersistent(className);
		Exception conversionException = null;

		if (flag && !classIsPersistent &&
			isPersistenceCapableAllowed(className))
		{
			try
			{
				// this calls updateKeyForClass which updates 
				// the mapping cache and the set of classes known to be non PC
				createSkeletonMappingClass(className);
			}
			catch (Exception e)
			{
				// need to unconvert whatever partial conversion succeeded
				conversionException = e;
			}
		}

		if ((!flag && classIsPersistent) || (conversionException != null))
		{
			try
			{
				// delete the mapping file
				deleteFile(className, getFileNameWithExtension(className));

				synchronized (this._classes)
				{
					// remove the corresponding MappingClassElement from cache
					_classes.remove(className);
					
					// put the class in the set of classes known to be non PC
					_nonPCClasses.add(className);
				}
			}
			catch (Exception e)	// rethrow if not a problem during unconvert
			{
				if (conversionException == null)
					conversionException = e;
			}
		}

		if (conversionException != null)		// rethrow the exception
		{
			if (conversionException instanceof RuntimeException)
				throw (RuntimeException)conversionException;
			else if (conversionException instanceof IOException)
				throw (IOException)conversionException;
		}
	}

	/** Converts the class with the supplied name to persistence-capable,
	 * then convert its default fields and save it to the xml file.
	 * @param className the fully qualified name of the class
	 * @exception IOException if there is some error storing the class
	 */
	public void convertToPersistenceCapable (String className)
		throws IOException
	{
		convertToPersistenceCapable(className, true);
		convertDefaultFields(className);
		storeMappingClass(className);
	}

	/** Creates a PersistenceClassElement with the specified name, then wraps 
	 * it in a mapping class and stores it in the hash map of classes.  
	 * This is the first phase of converting a class to be persistence-capable.
	 * @param className the fully qualified name of the class
	 * @see #convertDefaultFields
	 * @see #updateKeyForClass
	 */
	private void createSkeletonMappingClass (String className)
	{
		PersistenceClassElement element = new PersistenceClassElement(
			new PersistenceClassElementImpl(className));

		updateKeyForClass(new MappingClassElementImpl(element), null);
	}

	/** Adds the default allowable persistent fields to the persistent class
	 * with the specified name.  This is the second phase of converting
	 * a class to be persistence-capable.
	 * @param className the fully qualified name of the class
	 * @see #createSkeletonMappingClass
	 * @see #convertFields
	 */
	public void convertDefaultFields (String className)
	{
		convertFields(className, getFields(className));
	}

	/** Adds the allowable persistent fields from the supplied list 
	 * to the persistent class with the specified name.
	 * @param className the fully qualified name of the class
	 * @param fields a list of (short) field names
	 * @see #convertDefaultFields
	 */
	public void convertFields (String className, List fields)
	{
		PersistenceClassElement element = getPersistenceClass(className);

		if (element != null)
		{
			Iterator iterator = fields.iterator();

			// iterate the list of fields and create corresponding
			// PersistenceFieldElements (& RelationshipElements)
			while (iterator.hasNext())
			{
				String fieldName = (String)iterator.next();

				if (isPersistentAllowed(className, fieldName) &&
					shouldBePersistent(className, fieldName))
				{
					addFieldElement(element, fieldName);
				}
			}

			/* comment out -- not supporting concurrency groups for beta
			// add everything to one concurrency group by default
			PersistenceFieldElement[] persistentFields = element.getFields();
			if ((persistentFields != null) && (persistentFields.length > 0))
			{
				String defaultGroupName = I18NHelper.getMessage(getMessages(),
					"jdo.concurrency_group.default");
				ConcurrencyGroupElement group = new ConcurrencyGroupElement(
					new ConcurrencyGroupElementImpl(defaultGroupName), element);

				try
				{
					group.addFields(persistentFields);
					element.addConcurrencyGroup(group);
				}
				catch (ModelException e)
				{}	// just don't add this group
			}*/
		}
	}

	/** Adds a PersistenceFieldElement for the specified field to the
	 * supplied PersistenceClassElement, creating a RelationshipElement if
	 * necessary.
	 * @param element the persistence class element to be used
	 * @param fieldName the name of the field to be added
	 */
	public boolean addFieldElement (PersistenceClassElement element,
		String fieldName)
	{
		String fieldType = getFieldType(element.getName(), fieldName);
		boolean isCollection = isCollection(fieldType);

		try
		{
			// check if should be relationship here
			if (isPersistent(fieldType) || isCollection)
			{
				RelationshipElement relationship = new RelationshipElement(
					new RelationshipElementImpl(fieldName), element);

				if (isCollection)
				{
					relationship.setCollectionClass(
						getDefaultCollectionClass(fieldType));
				}
				else	// set upper bound = 1 (jdo model should really do this)
					relationship.setUpperBound(1);

				element.addField(relationship);
			}
			else
			{
				element.addField(new PersistenceFieldElement(new
					PersistenceFieldElementImpl(fieldName), element));
			}

			return true;
		}
		catch (ModelException e)
		{}	// will return false

		return false;
	}

	/** Removes the specified PersistenceFieldElement from its declaring
	 * class.  This method is added so that there is a common way to do this
	 * which checks if the argument is a relationship element with an inverse
	 * (in which case the inverse should first be cleared).  This should
	 * really be handled by the jdo model directly, but the removeField method
	 * doesn't have access to the Model which is necessary for setting
	 * (or clearing) inverse relationships.
	 * @param element the persistence field element to be removed
	 * @exception ModelException if there is some error removing the field
	 */
	public void removeFieldElement (PersistenceFieldElement element)
		throws ModelException
	{
		if (element != null)
		{
			if (element instanceof RelationshipElement)
			{
				((RelationshipElement)element).setInverseRelationship(null,
					this);
			}

			element.getDeclaringClass().removeField(element);
		}
	}

	/** Gets the name of the related class for a relationship element.
	 * This method is added so that there is a common way to do this.  It
	 * checks if the argument is a collection relationship element (in which
	 * case the element class should be returned) or not (in which case the
	 * type should be returned).  This should really be handled by the jdo
	 * model directly, but it doesn't have access to the Model which is
	 * necessary for finding the field type and whether it is a collection or
	 * not.
	 * @param element the relationship element to be examined
	 * @return the name of the related class
	 */
	public String getRelatedClass (RelationshipElement element)
	{
		if (element != null)
		{
			String fieldType = getFieldType(
				element.getDeclaringClass().getName(), element.getName());
			String relatedClass =  (isCollection(fieldType) ?
				element.getElementClass() : fieldType);

			return (StringHelper.isEmpty(relatedClass) ? null : 
				relatedClass.trim());
		}

		return null;
	}

	/** Computes the list of names of the possible collection classes for the
	 * specified class.
	 * @param className the fully qualified name of the class to be checked
	 * @return an array of supported collection classes for the
	 * specified class name.
	 * @see #getFieldType
	 * @see #getDefaultCollectionClass
	 */
	public ArrayList getSupportedCollectionClasses (String className)
	{
		String supportedSet = "java.util.HashSet";		// NOI18N
	//	String supportedList = "java.util.ArrayList";	// NOI18N
	//	String supportedVector = "java.util.Vector";	// NOI18N
		ArrayList returnList = new ArrayList();

		// for dogwood, only support sets
		returnList.add(supportedSet);
	/*	if (className.indexOf("Collection") != -1)	// NOI18N
		{
			returnList.add(supportedSet);
			returnList.add(supportedList);
			returnList.add(supportedVector);
		}
		else if (className.indexOf("List") != -1)	// NOI18N
			returnList.add(supportedList);
		else if (className.indexOf("Set") != -1)	// NOI18N
			returnList.add(supportedSet);
		else if (supportedVector.equals(className))
			returnList.add(supportedVector);
	*/
		return returnList;
	}

	/** Returns the default collection class for the specified class.  If
	 * the specified class is an unspecified Collection type, the return
	 * will be HashSet.
	 * @param className the fully qualified name of the class to be checked
	 * @return the name of the default supported collection class for the
	 * specified class name.
	 * @see #getFieldType
	 * @see #getSupportedCollectionClasses
	 */
	public String getDefaultCollectionClass (String className)
	{
		String collectionClass = "java.util.HashSet";	// NOI18N

		// for dogwood, only support sets
	/*	if (className.indexOf("List") != -1)			// NOI18N
			collectionClass = "java.util.ArrayList";	// NOI18N
		else if ("java.util.Vector".equals(className))	// NOI18N
			collectionClass = className;
	*/
		return collectionClass;
	}

	/** Creates a file with the given base file name and extension
	 * parallel to the supplied class (if it does not yet exist).
	 * @param className the fully qualified name of the class
	 * @param baseFileName the name of the base file
	 * @param extension the file extension
	 * @return the output stream for the specified resource, <code>null</code>
	 * if an error occurs or none exists
	 * @exception IOException if there is some error creating the file
	 */
	abstract protected BufferedOutputStream createFile (String className,
		String baseFileName, String extension) throws IOException;

	/** Deletes the file with the given file name which is parallel
	 * to the supplied class.
	 * @param className the fully qualified name of the class
	 * @param fileName the name of the file
	 * @exception IOException if there is some error deleting the file
	 */
	abstract protected void deleteFile (String className, String fileName)
		throws IOException;

	/** Returns a list of names of all the declared field elements in the
	 * class with the specified name.
	 * @param className the fully qualified name of the class to be checked
	 * @return the names of the field elements for the specified class
	 */
	abstract public List getFields (String className);

	/** Returns a list of names of all the field elements in the
	 * class with the specified name.  This list includes the inherited 
	 * fields.
	 * @param className the fully qualified name of the class to be checked
	 * @return the names of the field elements for the specified class
	 */
	public List getAllFields (String className)
	{
		List returnList = new ArrayList();
		
		while (className != null)
		{
			returnList.addAll(getFields(className));
			className = getSuperclass(className);
		}

		return returnList;
	}

	/** Returns the class element with the specified className.
	 * @param className the fully qualified name of the class to be checked
	 * @return the class element for the specified className
	 */
	public Object getClass (String className)
	{
		return getClass(className, null);
	}

	/** Returns the class element with the specified className.
	 * @param className the fully qualified name of the class to be checked
	 * @param classLoader the class loader used to find mapping information
	 * @return the class element for the specified className
	 */
	abstract public Object getClass (String className, ClassLoader classLoader);

	/** Determines if a class with the specified className exists.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class name represents a valid
	 * class; <code>false</code> otherwise.
	 */
	public boolean hasClass (String className)
	{
		return hasClass(className, null);
	}

	/** Determines if a class with the specified className exists.
	 * @param className the fully qualified name of the class to be checked
	 * @param classLoader the class loader used to find mapping information
	 * @return <code>true</code> if this class name represents a valid
	 * class; <code>false</code> otherwise.
	 */
	public boolean hasClass (String className, ClassLoader classLoader)
	{
		return (getClass(className, classLoader) != null);
	}

	/** Determines if the specified class implements the specified interface. 
	 * Note, class element is a model specific class representation as returned 
	 * by a getClass call executed on the same model instance.
	 * @param classElement the class element to be checked
	 * @param interfaceName the fully qualified name of the interface to 
	 * be checked
	 * @return <code>true</code> if the class implements the interface; 
	 * <code>false</code> otherwise.
	 * @see #getClass
	 */
	abstract public boolean implementsInterface (Object classElement, 
		String interfaceName);

	/** Determines if the class with the specified name declares a constructor.
	 * @param className the name of the class to be checked
	 * @return <code>true</code> if the class declares a constructor; 
	 * <code>false</code> otherwise.
	 * @see #getClass
	 */
	abstract public boolean hasConstructor (String className);

	/** Returns the constructor element for the specified argument types 
	 * in the class with the specified name. Types are specified as type 
	 * names for primitive type such as int, float or as fully qualified 
	 * class names.
	 * @param className the name of the class which contains the constructor 
	 * to be checked
	 * @param argTypeNames the fully qualified names of the argument types
	 * @return the constructor element
	 * @see #getClass
	 */
	abstract public Object getConstructor (String className, 
		String[] argTypeNames);

	/** Returns the method element for the specified method name and argument 
	 * types in the class with the specified name. Types are specified as 
	 * type names for primitive type such as int, float or as fully qualified 
	 * class names.
	 * @param className the name of the class which contains the method 
	 * to be checked
	 * @param methodName the name of the method to be checked
	 * @param argTypeNames the fully qualified names of the argument types
	 * @return the method element
	 * @see #getClass
	 */
	abstract public Object getMethod (String className, String methodName, 
		String[] argTypeNames);

	/** Returns the inherited method element for the specified method 
	 * name and argument types in the class with the specified name.  
	 * Types are specified as type names for primitive type such as 
	 * int, float or as fully qualified class names.  Note that the class 
	 * with the specified className is not checked for this method, only
	 * superclasses are checked.
	 * @param className the name of the class which contains the method 
	 * to be checked
	 * @param methodName the name of the method to be checked
	 * @param argTypeNames the fully qualified names of the argument types
	 * @return the method element
	 * @see #getClass
	 */
	public Object getInheritedMethod (String className, String methodName, 
		String[] argTypeNames)
	{
		String superClass = getSuperclass(className);
		Object method = null;

		while ((superClass != null) && ((method = 
			getMethod(superClass, methodName, argTypeNames)) == null))
		{
			superClass = getSuperclass(superClass);
		}

		return method;
	}

	/** Returns the string representation of type of the specified element. 
	 * If element denotes a field, it returns the type of the field. 
	 * If element denotes a method, it returns the return type of the method. 
	 * Note, element is either a field element as returned by getField, or a 
	 * method element as returned by getMethod executed on the same model 
	 * instance.
	 * @param element the element to be checked
	 * @return the string representation of the type of the element
	 * @see #getField
	 * @see #getMethod
	 */
	abstract public String getType (Object element);

	/** Returns the field element for the specified fieldName in the class
	 * with the specified className.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return the field element for the specified fieldName
	 */
	abstract public Object getField (String className, String fieldName);

	/** Returns the inherited field element for the specified fieldName in 
	 * the class with the specified className.  Note that the class 
	 * with the specified className is not checked for this field, only
	 * superclasses are checked.
	 * @param className the fully qualified name of the class which contains
	 * a superclass with the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return the field element for the specified fieldName
	 */
	public Object getInheritedField (String className, String fieldName)
	{
		String superClass = getSuperclass(className);
		Object field = null;

		while ((superClass != null) && 
			((field = getField(superClass, fieldName)) == null))
		{
			superClass = getSuperclass(superClass);
		}

		return field;
	}

	/** Determines if a field with the specified fieldName exists in the class
	 * with the specified className.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a valid
	 * field; <code>false</code> otherwise.
	 */
	public boolean hasField (String className, String fieldName)
	{
		return (getField(className, fieldName) != null);
	}

	/** Returns the field type for the specified fieldName in the class
	 * with the specified className.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return the field type for the specified fieldName
	 */
	public String getFieldType (String className, String fieldName)
	{
		return getType(getField(className, fieldName));
	}

	/** Determines if the specified field element has a serializable type. 
	 * A type is serializable if it is a primitive type, a class that 
	 * implements java.io.Serializable or an interface that inherits from 
	 * java.io.Serializable.
	 * Note, the field element is a model specific field representation as 
	 * returned by a getField call executed on the same model instance.
	 * @param fieldElement the field element to be checked
	 * @return <code>true</code> if the field element has a serializable type;
	 * <code>false</code> otherwise.
	 * @see #getField
	 */
	abstract public boolean isSerializable (Object fieldElement);

	/** Determines if a field with the specified fieldName in the class
	 * with the specified className has a primitive type.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a primitive
	 * field; <code>false</code> otherwise.
	 * @see #getFieldType
	 */
	public boolean isPrimitive (String className, String fieldName)
	{
		return isPrimitive(getFieldType(className, fieldName));
	}

	/** Determines if the specified className represents a primitive type.
	 * @param className the fully qualified name of the class or type to be 
	 * checked
	 * @return <code>true</code> if this class represents a primitive; 
	 * <code>false</code> otherwise.
	 * @see #getFieldType
	 */
	protected boolean isPrimitive (String className)
	{
		return ((className != null) && 
			JavaTypeHelper.getPrimitiveClass(className) != null);
	}

	/** Determines if a field with the specified fieldName in the class
	 * with the specified className is an array.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a java array
	 * field; <code>false</code> otherwise.
	 * @see #getFieldType
	 */
	abstract public boolean isArray (String className, String fieldName);

	/** Determines if a field with the specified fieldName in the class
	 * with the specified className is a byte array.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a byte array
	 * field; <code>false</code> otherwise.
	 * @see #getFieldType
	 */
	public boolean isByteArray (String className, String fieldName)
	{
		return isByteArray(getFieldType(className, fieldName));
	}

	/** Determines if the specified className represents a byte array.
	 * @param className the fully qualified name of the class or type to be 
	 * checked
	 * @return <code>true</code> if this class represents a byte array; 
	 * <code>false</code> otherwise.
	 */
	protected boolean isByteArray (String className)
	{
		return ("byte[]".equals(className));		// NOI18N
	}

	/** Determines if the class name represents a collection.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class represents a collection;
	 * <code>false</code> otherwise.
	 * @see #getFieldType
	 */
	public boolean isCollection (String className)
	{
		return _collectionClasses.contains(className);
	}

	/** Determines if the specified className represents a second class object.
	 * For this release, the class name is checked against a list of supported
	 * second class objects since user-defined second class objects are not
	 * supported.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class represents a second class
	 * object; <code>false</code> otherwise.
	 * @see #isMutableSecondClassObject
	 * @see #isCollection
	 * @see #getFieldType
	 */
	public boolean isSecondClassObject (String className)
	{
		return _scoClasses.contains(className);
	}

	/** Determines if the specified className represents a mutable second class
	 * object.  For this release, the class name is checked against a list of
	 * supported mutable second class objects since user-defined second class
	 * objects are not supported.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if this class represents a mutable second
	 * class object; <code>false</code> otherwise.
	 * @see #isSecondClassObject
	 * @see #isCollection
	 * @see #getFieldType
	 */
	public boolean isMutableSecondClassObject (String className)
	{
		return _mutableScoClasses.contains(className);
	}

	/** Returns the string representation of declaring class of 
	 * the specified member element.  Note, the member element is 
	 * either a class element as returned by getClass, a field element 
	 * as returned by getField, a constructor element as returned by 
	 * getConstructor, or a method element as returned by getMethod 
	 * executed on the same model instance.
	 * @param memberElement the member element to be checked
	 * @return the string representation of the declaring class of 
	 * the specified memberElement
	 * @see #getClass
	 * @see #getField
	 * @see #getConstructor
	 * @see #getMethod
	 */
	abstract public String getDeclaringClass (Object memberElement);

	/** Returns the modifier mask for the specified member element.
	 * Note, the member element is either a class element as returned by 
	 * getClass, a field element as returned by getField, a constructor element 
	 * as returned by getConstructor, or a method element as returned by 
	 * getMethod executed on the same model instance.
	 * @param memberElement the member element to be checked
	 * @return the modifier mask for the specified memberElement
	 * @see java.lang.reflect.Modifier
	 * @see #getClass
	 * @see #getField
	 * @see #getConstructor
	 * @see #getMethod
	 */
	abstract public int getModifiers (Object memberElement);

	/** Returns the modifier mask for the specified className.
	 * @param className the fully qualified name of the class to be checked
	 * @return the modifier mask for the specified class
	 * @see java.lang.reflect.Modifier
	 */
	public int getModifiersForClass (String className)
	{
		return getModifiers(getClass(className));
	}

	/** Returns the modifier mask for the specified fieldName in the class
	 * with the specified className.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return the modifier mask for the specified field
	 * @see java.lang.reflect.Modifier
	 */
	protected int getModifiers (String className, String fieldName)
	{
		return getModifiers(getField(className, fieldName));
	}

	/** Returns <code>true</code> if the specified field can be made
	 * persistent, <code>false</code> otherwise.  This computation is based
	 * on the modifier and type of the field.  Fields which are non-final
	 * and non-static and are primitive, persistence capable, or second
	 * class objects and not arrays return <code>true</code>.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return whether the specified field can be made persistent
	 * @see #getModifiers(String,String)
	 * @see #isPrimitive
	 * @see #isArray
	 * @see #isPersistent
	 * @see #isSecondClassObject
	 * @see #shouldBePersistent
	 */
	public boolean isPersistentAllowed (String className, String fieldName)
	{
		return isPersistentAllowed(className, null, fieldName);
	}
	
	/** Returns <code>true</code> if the specified field can be made
	 * persistent, <code>false</code> otherwise.  This computation is based
	 * on the modifier and type of the field.  Fields which are non-final
	 * and non-static and are primitive, persistence capable, byte arrays, or 
	 * second class objects and not arrays return <code>true</code>.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param classLoader the class loader used to find mapping information
	 * @param fieldName the name of the field to be checked
	 * @return whether the specified field can be made persistent
	 * @see #getModifiers(String,String)
	 * @see #getFieldType
	 * @see #isPersistentTypeAllowed
	 * @see #shouldBePersistent
	 */
	public boolean isPersistentAllowed (String className, 
	   ClassLoader classLoader, String fieldName)
	{
		int modifier = getModifiers(className, fieldName);

		if (!Modifier.isStatic(modifier) && !Modifier.isFinal(modifier))
		{
			return isPersistentTypeAllowed(
				getFieldType(className, fieldName), classLoader);
		}

		return false;
	}

	/** Returns <code>true</code> if the a field of the specified class or 
	 * type can be made persistent, <code>false</code> otherwise.  Fields 
	 * which are primitive, persistence capable, byte arrays, or second
	 * class objects and not arrays return <code>true</code>.
	 * @param className the fully qualified name of the class or type to be 
	 * checked
	 * @param classLoader the class loader used to find mapping information
	 * @return <code>true</code> if this class represents a type which 
	 * can be made persistent; <code>false</code> otherwise.
	 * @see #isPrimitive
	 * @see #isByteArray
	 * @see #isPersistent
	 * @see #isSecondClassObject
	 */
	protected boolean isPersistentTypeAllowed (String className, 
		ClassLoader classLoader)
	{
		return (isPrimitive(className) || isSecondClassObject(className) || 
			isByteArray(className) || isPersistent(className, classLoader));
	}

	/** Returns <code>true</code> if the specified field should be made
	 * persistent (i.e. does it make sense), <code>false</code> otherwise.
	 * This computation is based solely on the modifier: those which are not
	 * volatile return <code>true</code>.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return whether the specified field should be made persistent
	 * see #getModifiers(String,String)
	 */
	public boolean shouldBePersistent (String className, String fieldName)
	{
		return !Modifier.isVolatile(getModifiers(className, fieldName));
	}

	/** Returns the PersistenceFieldElement with the supplied fieldName found
	 * in the supplied className.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return the PersistenceFieldElement for the specified field,
	 * <code>null</code> if an error occurs or none exists
	 */
	public PersistenceFieldElement getPersistenceField (String className,
		String fieldName)
	{
		return (hasField(className, fieldName) ? 
			getPersistenceFieldInternal(className, fieldName) : null);
	}

	/** Returns the PersistenceFieldElement with the supplied fieldName found
	 * in the supplied className.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return the PersistenceFieldElement for the specified field,
	 * <code>null</code> if an error occurs or none exists
	 */
	protected PersistenceFieldElement getPersistenceFieldInternal 
		(String className, String fieldName)
	{
		PersistenceClassElement classElement = getPersistenceClass(className);

		return ((classElement != null) ? 
			classElement.getField(fieldName) : null);
	}

	/** Determines if the specified className and fieldName pair represent a
	 * persistent field.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a persistent
	 * field; <code>false</code> otherwise.
	 */
	public boolean isPersistent (String className, String fieldName)
	{
		PersistenceFieldElement fieldElement =
			getPersistenceField(className, fieldName);

		if (fieldElement != null)
		{
			return (PersistenceFieldElement.PERSISTENT ==
				fieldElement.getPersistenceType());
		}

		return false;
	}

	/** Determines if the specified className and fieldName pair represent a
	 * key field.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a key field;
	 * <code>false</code> otherwise.
	 */
	public boolean isKey (String className, String fieldName)
	{
		if (hasField(className, fieldName))
		{
			PersistenceClassElement classElement =
				getPersistenceClass(className);

			if (classElement != null)
			{
				String keyClass = classElement.getKeyClass();

				if (keyClass != null)
					return hasField(keyClass, fieldName);
			}
		}

		return false;
	}

	/** Determines if the specified className and fieldName pair represent a
	 * field which has a type which is valid for key fields.  Valid key 
	 * field types include non-collection SCOs (wrappers, Date, Time, etc.) 
	 * and primitives.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a field 
	 * with a valid type for a key field; <code>false</code> otherwise.
	 */
	public boolean isValidKeyType (String className, String fieldName)
	{
		String fieldType = getFieldType(className, fieldName);

		if (fieldType == null)
			fieldType = getType(getInheritedField(className, fieldName));

		return (isPrimitive(fieldType) || 
			(isSecondClassObject(fieldType) && !isCollection(fieldType)));
	}

	/** Determines if the specified className and fieldName pair represent a
	 * field which is part of the default fetch group.
	 * @param className the fully qualified name of the class which contains
	 * the field to be checked
	 * @param fieldName the name of the field to be checked
	 * @return <code>true</code> if this field name represents a field in
	 * the default fetch group; <code>false</code> otherwise.
	 */
	public boolean isDefaultFetchGroup (String className, String fieldName)
	{
		MappingClassElement mappingClass = getMappingClass(className);

		try
		{
			return (MappingFieldElement.GROUP_DEFAULT ==
				mappingClass.getField(fieldName).getFetchGroup());
		}
		catch (Exception e)
		{}	// will return false

		return false;
	}

	/** Parses the combination of java (or class) information and mapping/jdo
	 * information by running through a subset of the full validation check
	 * and aborting (and returning <code>false</code> at the first error or
	 * warning.
	 * @param className the fully qualified name of the class to be checked
	 * @return <code>true</code> if no errors or warnings occur,
	 * <code>false</code> otherwise.
	 */
	public boolean parse (String className)
	{
		return new ModelValidator(this, className, getMessages()).parseCheck();
	}

	/** Validates the combination of java (or class) information and mapping/jdo
	 * information by running through the full validation check and returning
	 * a collection of ModelValidationExceptions containing any errors or
	 * warnings encountered.
	 * @param className the fully qualified name of the class to be checked
	 * @param bundle the overridden resource bundle file - specify
	 * <code>null</code> if the default one should be used
	 * @return a collection of ModelValidationExceptions containing any
	 * errors or warnings encountered.  If no errors or warnings were
	 * encountered, the collection will be empty, not <code>null</code>.
	 */
	public Collection validate (String className, ResourceBundle bundle)
	{
		return validate(className, null, bundle);
	}

	/** Validates the combination of java (or class) information and mapping/jdo
	 * information by running through the full validation check and returning
	 * a collection of ModelValidationExceptions containing any errors or
	 * warnings encountered.
	 * @param className the fully qualified name of the class to be checked
	 * @param classLoader the class loader used to find mapping information
	 * @param bundle the overridden resource bundle file - specify
	 * <code>null</code> if the default one should be used
	 * @return a collection of ModelValidationExceptions containing any
	 * errors or warnings encountered.  If no errors or warnings were
	 * encountered, the collection will be empty, not <code>null</code>.
	 */
	public Collection validate (String className, ClassLoader classLoader, 
		ResourceBundle bundle)
	{
		return new ModelValidator(this, className, classLoader, 
			((bundle == null) ? getMessages() : bundle)).fullValidationCheck();
	}

    /** Standard set of arguments for comparison with readObject method.
     */
    public static String[] getReadObjectArgs() {
        // Creating and returning a new array every time to prevent returning a mutable array
        return new String[] {"java.io.ObjectInputStream"}; //NOI18N
    }

    /** Standard set of arguments for comparison with equals method.
     */
    public static String[] getEqualsArgs() {
        // Creating and returning a new array every time to prevent returning a mutable array
        return new String[] {"java.lang.Object"}; //NOI18N
    }

	/** Standard set of arguments for comparison with writeObject method.
	 */
    public static String[] getWriteObjectArgs() {
        // Creating and returning a new array every time to prevent returning a mutable array 
        return new String[] {"java.io.ObjectOutputStream"}; //NOI18N
    }

}

