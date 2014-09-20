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
 * RuntimeModel.java
 *
 * Created on March 10, 2000, 11:05 AM
 */

package com.sun.jdo.api.persistence.model;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.netbeans.modules.dbschema.SchemaElement;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.spi.persistence.utility.*;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public class RuntimeModel extends Model
{
	/** Extension of the class file, used to figure out the path for handling
	 * the mapping file.
	 */
	private static final String CLASS_EXTENSION = "class";			// NOI18N

	/** Constant which represents the prefix of the java package.
	 */
	private static final String JAVA_PACKAGE = "java.";				// NOI18N

	/** Constant which represents the serializable interface.
	 */
	private static final String SERIALIZABLE = "java.io.Serializable"; 	//NOI18N

	/** Map of class loader used to find classes and mapping information. Keys
	 * are fully qualified class names.
	 */
	private HashMap classLoaders = new HashMap();
	
	/** Creates a new RuntimeModel. This constructor should not be called 
	 * directly; instead, the static instance accesible from the Model class 
	 * should be used.
	 * @see Model#RUNTIME
	 */
	protected RuntimeModel ()
	{
		super();
	}

	/** Determines if the specified className represents an interface type.
	 * @param className the fully qualified name of the class to be checked 
	 * @return <code>true</code> if this class name represents an interface;
	 * <code>false</code> otherwise.
	 */
	public boolean isInterface (String className)
	{
		Class classElement = (Class)getClass(className);
	
		return ((classElement != null) ? classElement.isInterface() : false);
	}

	/** Returns the input stream with the supplied resource name found with 
	 * the supplied class name.
	 * NOTE, this implementation assumes the specified class loader is not null
	 * and needs not to be validated. Any validation is done by getMappingClass
	 * which is the only caller of this method.
	 * @param className the fully qualified name of the class which will 
	 * be used as a base to find the resource
	 * @param classLoader the class loader used to find mapping information
	 * @param resourceName the name of the resource to be found
	 * @return the input stream for the specified resource, <code>null</code> 
	 * if an error occurs or none exists
	 */
	protected BufferedInputStream getInputStreamForResource (String className, 
		ClassLoader classLoader, String resourceName)
	{
		InputStream is = ((className != null) ? 
                          classLoader.getResourceAsStream(resourceName) : null);

        BufferedInputStream rc = null;
        if (is != null && !(is instanceof BufferedInputStream)) {
            rc = new BufferedInputStream(is);
        } else {
            rc = (BufferedInputStream)is;
        }
        return rc;
	}

	/** Computes the class name (without package) for the supplied
	 * class name.
	 * @param className the fully qualified name of the class
	 * @return the class name (without package) for the supplied
	 * class name
	 */
	private String getShortClassName (String className)
	{
		 return JavaTypeHelper.getShortClassName(className);
	}

	/** Returns the name of the second to top (top excluding java.lang.Object) 
	 * superclass for the given class name.
	 * @param className the fully qualified name of the class to be checked
	 * @return the top non-Object superclass for className, 
	 * <code>className</code> if an error occurs or none exists
	 */
	protected String findPenultimateSuperclass (String className)
	{
		Class classElement = (Class)getClass(className);
		Class objectClass = java.lang.Object.class;
		Class testClass = null;

		if (classElement == null)
			return className;

		while ((testClass = classElement.getSuperclass()) != null)
		{
			if (testClass.equals(objectClass))
				break;

			classElement = testClass;
		}

		return classElement.getName();
	}

	/** Returns the name of the superclass for the given class name.
	 * @param className the fully qualified name of the class to be checked
	 * @return the superclass for className, <code>null</code> if an error 
	 * occurs or none exists
	 */
	protected String getSuperclass (String className)
	{
		Class classElement = (Class)getClass(className);

		if (classElement != null)
			classElement = classElement.getSuperclass();

		return ((classElement != null) ? classElement.getName() : null);
	}

	/** Returns the MappingClassElement created for the specified class name.
	 * This method looks up the class in the internal cache. If not present 
	 * it loads the corresponding xml file containing the mapping information. 
	 * @param className the fully qualified name of the mapping class
	 * @param classLoader the class loader used to find mapping information
	 * @return the MappingClassElement for className,
	 * <code>null</code> if an error occurs or none exists
	 * @see com.sun.jdo.api.persistence.model.mapping.impl.MappingClassElementImpl#forName(String, Model)
	 */
	public MappingClassElement getMappingClass (String className, 
		ClassLoader classLoader)
	{
		MappingClassElement mappingClass = null;

		// First check class loader. This has to be done before the super call!
		// Method Model.getMappingClass will check the MappingClassElement cache
		// and will find an entry in the case of a multiple class loader for the
		// same class name. So we have to check the multiple class loader first.
		classLoader = findClassLoader(className, classLoader);
		mappingClass = super.getMappingClass(className, classLoader);
		if ((mappingClass != null) && (classLoader != null))
		{
			// Lookup the SchemElement connected to mappingClass. This reads 
			// the .dbschema file using the specified classLoader and stores the
			// SchemaElement in the SchemaElement cache. Any subsequent 
			// SchemaElement.forName or TableElement.forName lookups will use 
			// the cached version.
			String databaseRoot = mappingClass.getDatabaseRoot();

			// An unmapped mapping class is allowed in which case the 
			// databaseRoot will be null (never mapped) or empty 
			// (mapped once, unmapped now), but if the databaseRoot is 
			// not null or empty and we can't find the schema, throw a 
			// RuntimeException to notify the user that something is wrong.
			if (!StringHelper.isEmpty(databaseRoot) && 
				(SchemaElement.forName(databaseRoot, classLoader) == null))
			{
				throw new RuntimeException(I18NHelper.getMessage(
					getMessages(), "dbschema.not_found", // NOI18N
					databaseRoot, className)); 
			}
		}
	
		return mappingClass;
	}

	/** Returns an unmodifiable copy of the ClassLoader cache.
	 * @return unmodifiable ClassLoader cache
	 */
	public Map getClassLoaderCache ()
	{
		return Collections.unmodifiableMap(classLoaders);
	}

	/** Removes the classes cached with the specified class loader from all
	 * caches. The method iterates the ClassLoader cache to find classes
	 * cached with the specified class loader. These classes are removed
	 * from the ClassLoader cache, the cache of MappingClassElements and
	 * the set of classes known to be non PC. The associated SchemaElements
	 * are removed from the SchemaElement cache. 
	 * @param classLoader used to determine the classes to be removed
	 */
	public void removeResourcesFromCaches (ClassLoader classLoader)
	{
		Collection classNames = new HashSet();

		synchronized(classLoaders)
		{
			for (Iterator i = classLoaders.entrySet().iterator(); i.hasNext();)
			{
				Map.Entry next = (Map.Entry)i.next();

				// check the cached class loader
				if (next.getValue() == classLoader)
				{
					// add className to the collection of classNames to be 
					// removed
					classNames.add(next.getKey());
					// remove this entry from the classLoaders cache
					i.remove();
				}
			}
		}

		removeResourcesFromCaches(classNames);
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
	protected BufferedOutputStream createFile (String className, String baseFileName, 
		String extension) throws IOException
	{
		char extensionCharacter = '.';
		File file = getFile(className, 
			baseFileName + extensionCharacter + extension);

		if (file == null)
		{
			Class classElement = (Class)getClass(className);

			if (classElement != null)
			{
				// need to find the path before the package name
				String path = classElement.getResource(
					getShortClassName(className) + extensionCharacter + 
					CLASS_EXTENSION).getFile();
				int index = path.lastIndexOf(extensionCharacter) + 1;
				
				file = new File(path.substring(0, index) + extension);
				file.createNewFile();
			}
		}
		return ((file != null)
                ? (new BufferedOutputStream(new FileOutputStream(file)))
                : null);
	}

	/** Deletes the file with the given file name which is parallel 
	 * to the supplied class.
	 * @param className the fully qualified name of the class
	 * @param fileName the name of the file
	 * @exception IOException if there is some error deleting the file
	 */
	protected void deleteFile (String className, String fileName)
		throws IOException
	{
		File file = getFile(className, fileName);

		if ((file != null) && file.exists())
			file.delete();
	}

	/** Returns a file with the given file name which is parallel to the 
	 * supplied class.
	 * @param className the fully qualified name of the class
	 * @param fileName the name of the file
	 * @return the file object for the specified resource, <code>null</code> 
	 * if an error occurs
	 * @exception IOException if there is some error getting the file
	 */
	protected File getFile (String className, String fileName)
		throws IOException
	{
		Class classElement = (Class)getClass(className);

		if (classElement != null)
		{
			// need to find the path before the package name
			URL path = classElement.getResource(fileName.substring(
				fileName.lastIndexOf(getShortClassName(className))));

			return ((path != null) ? (new File(path.getFile())) : null);
		}

		return null;
	}

	/** Returns the class element with the specified className.  The class is
	 * found using <code>Class.forName</code>.
	 * @param className the fully qualified name of the class to be checked 
	 * @param classLoader the class loader used to find mapping information
	 * @return the class element for the specified className, <code>null</code> 
	 * if an error occurs or none exists
	 */
	public Object getClass (String className, ClassLoader classLoader)
	{
		if (className == null) 
			return null;
		
		try
		{
			classLoader = findClassLoader(className, classLoader);
			return Class.forName(className, true, classLoader);
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}
	}
		
	/**
	 * This method returns the class loader used to find mapping information 
	 * for the specified className.  If the classLoader argument is not null, 
	 * the method updates the classLoaders cache and returns the specified 
	 * classLoader.  Otherwise it checks the cache for the specified className 
	 * and returns this class loader.  If there is no cached class loader it 
	 * returns the current class loader.
	 * @param className the fully qualified name of the class to be checked
	 * @param classLoader the class loader used to find mapping information
	 * @return the class loader used to find mapping information for the 
	 * specified className
	 * @exception IllegalArgumentException if there is class loader problem
	 */
	protected ClassLoader findClassLoader (String className, 
		ClassLoader classLoader) throws IllegalArgumentException
	{
		ClassLoader cached = null;

		if (className == null)
			return null;
		else if (className.startsWith(JAVA_PACKAGE) || isPrimitive(className))
			// Use current class loader for java packages or primitive types
			// - these classes cannot have the multiple class loader conflict
			// - these classes should not show up in the classLoaders map
			return getClass().getClassLoader();

		synchronized (classLoaders)
		{
			cached = (ClassLoader)classLoaders.get(className);

			if (classLoader == null)
			{
				// Case 1: specified class loader is null =>
				// return cached class loader if available, if not
				// take current class loader
				classLoader =
					(cached != null) ? cached : getClass().getClassLoader();
			}
			else if (cached == null)
			{
				// Case 2: specified class loader is NOT null AND
				// no class loader cached for the class name =>
				// put specified class loader in cache
				classLoaders.put(className, classLoader);
			}
			else if (classLoader != cached)
			{
				// Case 3: specified class loader is NOT null AND
				// cache contains class loader for this class name AND
				// both class loaders are not identical =>
				// pontential conflict
				Class clazz = null;
				Class cachedClazz  = null;

				try
				{
					String prop = ClassLoaderStrategy.getStrategy();

					// Load the class using specified and cached class loader.
					// NOTE, do not change the order of the next two lines, the
					// catch block relies on it!
					clazz = Class.forName(className, true, classLoader);
					cachedClazz = Class.forName(className, true, cached);

					if (clazz.getClassLoader() == cachedClazz.getClassLoader())
					{
						// Case 3a: both class loaders are the same =>
						// return it
						return cached;
					}
					else if (ClassLoaderStrategy.MULTIPLE_CLASS_LOADERS_IGNORE.equals(prop))
					{
						// Case 3b: both class loaders are different and
						// the system property is defined as ignore =>
						// ignore the specified class loader and return
						// the cached class loader
						return cached;
					}
					else if (ClassLoaderStrategy.MULTIPLE_CLASS_LOADERS_RELOAD.equals(prop))
					{
						// Case 3c: both class loaders are different and
						// the system property is defined as reload =>
						// discard the cached class loader and replace it
						// by the specified class loader
						removeResourcesFromCaches(cachedClazz.getClassLoader());
						classLoaders.put(className, classLoader);
						return classLoader;
					}
					else
					{
						// Case 3d: both class loaders are different and
						// the system property is defined as error or
						// any other value =>
						// throw exception
						throw new IllegalArgumentException(I18NHelper.getMessage(
							getMessages(), "classloader.multiple", // NOI18N
							className));
					}
				}
				catch (ClassNotFoundException ex)
				{
					// At least one of the class loader could not find the class.
					// Update the classLoader map, if the specified class loader
					// could find it, but the cached could not.
					if ((clazz != null) && (cachedClazz == null))
						classLoaders.put(className, classLoader);
				}
			}
		}

		return classLoader;
	}

	/** Determines if the specified class implements the specified interface. 
	 * Note, class element is a model specific class representation as returned 
	 * by a getClass call executed on the same model instance. This 
	 * implementation expects the class element being a reflection instance.
	 * @param classElement the class element to be checked
	 * @param interfaceName the fully qualified name of the interface to 
	 * be checked
	 * @return <code>true</code> if the class implements the interface; 
	 * <code>false</code> otherwise.
	 * @see #getClass
	 */
	public boolean implementsInterface (Object classElement, 
		String interfaceName)
	{
		Class interfaceClass = (Class)getClass(interfaceName);

		if ((classElement == null) || !(classElement instanceof Class) ||
			(interfaceClass == null))
			return false;
		
		return interfaceClass.isAssignableFrom((Class)classElement);
	}

	/** Determines if the class with the specified name declares a constructor.
	 * @param className the name of the class to be checked
	 * @return <code>true</code> if the class declares a constructor; 
	 * <code>false</code> otherwise.
	 * @see #getClass
	 */
	public boolean hasConstructor (final String className)
	{
		final Class classElement = (Class)getClass(className);

		if (classElement != null)
		{
			Boolean b = (Boolean)AccessController.doPrivileged(
				new PrivilegedAction()
			{
				public Object run ()
				{
					return JavaTypeHelper.valueOf(((Class)classElement).
						getDeclaredConstructors().length != 0);
				}
			});

			return b.booleanValue();
		}

		return false;
	}

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
	public Object getConstructor (final String className, String[] argTypeNames)
	{
		final Class classElement = (Class)getClass(className);

		if (classElement != null)
		{
			final Class[] argTypes = getTypesForNames(argTypeNames);

			return AccessController.doPrivileged(new PrivilegedAction()
			{
				public Object run ()
				{
					try
					{  
						return ((Class)classElement).getDeclaredConstructor(
							argTypes);
					}
					catch (NoSuchMethodException ex)
					{
						// constructor not found => return null
						return null;
					}
				}
			});
		}

		return null;
	}

	/** Returns the method element for the specified method name and argument 
	 * types in the class with the specified name. Types are specified as 
	 * type names for primitive type such as int, float or as fully qualified 
	 * class names.  Note, the method does not return inherited methods.
	 * @param className the name of the class which contains the method 
	 * to be checked
	 * @param methodName the name of the method to be checked
	 * @param argTypeNames the fully qualified names of the argument types
	 * @return the method element
	 * @see #getClass
	 */
	public Object getMethod (final String className, final String methodName,
		String[] argTypeNames)
	{
		final Class classElement = (Class)getClass(className);

		if (classElement != null)
		{
			final Class[] argTypes =  getTypesForNames(argTypeNames);

			return AccessController.doPrivileged(new PrivilegedAction()
			{
				public Object run ()
				{
					try
					{
						return classElement.getDeclaredMethod(
							methodName, argTypes);
					}
					catch (NoSuchMethodException ex)
					{
						// method not found => return null
						return null;
					}
				}
			});
		}

		return null;
	}

	/** Returns the string representation of type of the specified element. 
	 * If element denotes a field, it returns the type of the field. 
	 * If element denotes a method, it returns the return type of the method. 
	 * Note, element is either a field element as returned by getField, or a 
	 * method element as returned by getMethod executed on the same model 
	 * instance. This implementation expects the element being a reflection 
	 * instance.
	 * @param element the element to be checked
	 * @return the string representation of the type of the element
	 * @see #getField
	 * @see #getMethod
	 */
	public String getType (Object element)
	{
		return getNameForType(getTypeObject(element));
	}

	/** Returns a list of names of all the declared field elements in the 
	 * class with the specified name.
	 * @param className the fully qualified name of the class to be checked 
	 * @return the names of the field elements for the specified class
	 */
	public List getFields (String className)
	{
		List returnList = new ArrayList();
		final Class classElement = (Class)getClass(className);
		
		if (classElement != null)
		{
			Field[] fields = (Field[]) AccessController.doPrivileged(
				new PrivilegedAction()
			{
				public Object run ()
				{
					return classElement.getDeclaredFields();
				}
			});
			int i, count = fields.length;

			for (i = 0; i < count; i++)
				returnList.add(fields[i].getName());
		}

		return returnList;
	}

	/** Returns the field element for the specified fieldName in the class
	 * with the specified className.
	 * @param className the fully qualified name of the class which contains 
	 * the field to be checked 
	 * @param fieldName the name of the field to be checked 
	 * @return the field element for the specified fieldName
	 */
	public Object getField (String className, final String fieldName)
	{
		final Class classElement = (Class)getClass(className);

		if (classElement != null)
		{
			return AccessController.doPrivileged(new PrivilegedAction()
			{
				public Object run ()
				{
					try
					{
						return classElement.getDeclaredField(fieldName);
					}
					catch (NoSuchFieldException e)
					{
						// field not found => return null;
						return null;
					}
				}
			});
		}

		return null;
	}

	/** Determines if the specified field element has a serializable type. 
	 * A type is serializable if it is a primitive type, a class that implements
	 * java.io.Serializable or an interface that inherits from 
	 * java.io.Serializable. 
	 * Note, the field element is a model specific field representation as 
	 * returned by a getField call executed on the same model instance. This 
	 * implementation expects the field element being a reflection instance.
	 * @param fieldElement the field element to be checked
	 * @return <code>true</code> if the field element has a serializable type;
	 * <code>false</code> otherwise.
	 * @see #getField
	 */
	public boolean isSerializable (Object fieldElement)
	{
		Class type = getTypeObject(fieldElement);

		// check if the topmost element type is serializable
		while ((type != null) && type.isArray())
			type = type.getComponentType();

		return ((type != null) ? 
			(type.isPrimitive() || implementsInterface(type, SERIALIZABLE)) : 
			false);
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
	public boolean isArray (String className, String fieldName)
	{
		Object fieldElement = getField(className, fieldName);

		return ((fieldElement != null) ? 
			getTypeObject(fieldElement).isArray() : false);
	}

	/** Returns the string representation of declaring class of 
	 * the specified member element.  Note, the member element is 
	 * either a class element as returned by getClass, a field element 
	 * as returned by getField, a constructor element as returned by 
	 * getConstructor, or a method element as returned by getMethod 
	 * executed on the same model instance.  This implementation 
	 * expects the member element to be a reflection instance.
	 * @param memberElement the member element to be checked
	 * @return the string representation of the declaring class of 
	 * the specified memberElement
	 * @see #getClass
	 * @see #getField
	 * @see #getConstructor
	 * @see #getMethod
	 */
	public String getDeclaringClass (Object memberElement)
	{
		Class classElement = null;

		if ((memberElement != null) && (memberElement instanceof Member))
			classElement = ((Member)memberElement).getDeclaringClass();

		return ((classElement != null) ? classElement.getName() : null);
	}

	/** Returns the modifier mask for the specified member element.
	 * Note, the member element is either a class element as returned by 
	 * getClass, a field element as returned by getField, a constructor element 
	 * as returned by getConstructor, or a method element as returned by 
	 * getMethod executed on the same model instance.  This implementation 
	 * expects the member element to be a reflection instance.
	 * @param memberElement the member element to be checked
	 * @return the modifier mask for the specified memberElement
	 * @see java.lang.reflect.Modifier
	 * @see #getClass
	 * @see #getField
	 * @see #getConstructor
	 * @see #getMethod
	 */
	public int getModifiers (Object memberElement)
	{
		int modifiers = 0;
		
		if (memberElement != null) 
		{
			if (memberElement instanceof Class)
			{
				modifiers = ((Class)memberElement).getModifiers();
			}
			else if (memberElement instanceof Member)
			{
				modifiers = ((Member)memberElement).getModifiers();
			}
		}

		return modifiers;
	}

	/** Returns the Class type of the specified element. 
	 * If element denotes a field, it returns the type of the field. 
	 * If element denotes a method, it returns the return type of the method. 
	 * Note, element is either a field element as returned by getField, or a 
	 * method element as returned by getMethod executed on the same model 
	 * instance.
	 * @param element the element to be checked
	 * @return the Class type of the element
	 * @see #getField
	 * @see #getMethod
	 */
	protected Class getTypeObject (Object element)
	{
		Class type = null;

		if (element != null)
		{
			if (element instanceof Field)
				type = ((Field)element).getType();
			else if (element instanceof Method)
				type = ((Method)element).getReturnType();
		}

		return type;
	}

	private String getNameForType (Class type)
	{
		String typeName = null;

		if (type != null)
		{
			if (type.isArray())
			{
				typeName = getNameForType(
					type.getComponentType()) + "[]";	// NOI18N
			}
			else
				typeName = type.getName();
		}

		return typeName;
	}

	/** Converts the array of type names into an array of Class objects.
	 */
	private Class[] getTypesForNames (String[] typeNames)
	{
		Class[] classes = new Class[typeNames.length];

		for (int i = 0; i < classes.length; i++)
			classes[i] = getTypeForName(typeNames[i]);

		return classes;
	}

	/** Converts the specified type name into its corresponding java.lang.Class 
	 * representation.
	 */
	private Class getTypeForName (String typeName)
	{
		Class clazz = JavaTypeHelper.getPrimitiveClass(typeName);

		if (clazz == null)
			clazz = (Class)getClass(typeName);

		return clazz;
	}
}
