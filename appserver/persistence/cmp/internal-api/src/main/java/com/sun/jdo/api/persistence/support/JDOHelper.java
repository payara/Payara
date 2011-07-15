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
 * JDOHelper.java
 *
 * Created on April 18, 2000
 */
 
package com.sun.jdo.api.persistence.support;

import java.util.ResourceBundle;
import java.util.Collection;

import org.glassfish.persistence.common.I18NHelper;

/**
 * @author Martin Zaun
 * @version 0.1
 */

/**
 * An utility class for querying <code>PersistenceCapable</code> objects.
 */
public class JDOHelper
{
    /**
     * Returns the associated PersistenceManager of an object if there is one.
     *
     * For transactional and persistent objects, their associated
     * <code>PersistenceManager</code> is returned.
     * For transient objects, <code>null</code> is returned.
     *
     * @param   obj     an <code>Object</code>
     * @return          the PersistenceManager associated with the object;
     *                  <code>null</code> otherwise
     * @see PersistenceCapable#jdoGetPersistenceManager()
     */
    static public PersistenceManager getPersistenceManager(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoGetPersistenceManager();
        return null;
    }
    
    /**
     * Explicitly marks a field of an object as dirty if the object is
     * persistent and transactional.
     *
     * Normally, PersistenceCapable classes are able to detect changes made
     * to their fields.  However, if a reference to an Array is given to a
     * method outside the class, and the Array is modified, then the
     * persistent object is not aware of the change.  This method allows the
     * application to notify the object that a change was made to a field.
     * For transient objects, this method does nothing.
     *
     * @param   obj             an object
     * @param   fieldName       the name of the object's field to be marked
     *                          dirty
     * @see PersistenceCapable#jdoMakeDirty(String fieldName)
     */
    static public void makeDirty(Object obj, String fieldName)
    {
        if (obj instanceof PersistenceCapable)
            ((PersistenceCapable)obj).jdoMakeDirty(fieldName);
    }
    
    /**
     * Returns a copy of the JDO identity associated with an object.
     *
     * Persistent objects of PersistenceCapable classes have a JDO identity
     * managed by the PersistenceManager.  This method returns a copy of the
     * ObjectId that represents the JDO identity of a persistent object.
     * For transient objects, <code>null</code> is returned.
     *<P>
     * The ObjectId may be serialized and later restored, and used with
     * a PersistenceManager from the same JDO implementation to locate a
     * persistent object with the same data store identity.
     * If the JDO identity is managed by the application, then the
     * ObjectId may be used with a PersistenceManager from any JDO
     * implementation that supports the PersistenceCapable class.
     * If the JDO identity is not managed by the application or the
     * data store, then the ObjectId returned is only valid within the
     * current transaction.
     *
     * @param   obj     an <code>Object</code>
     * @return          a copy of the ObjectId of a persistent object;
     *                  <code>null</code> if the object is transient
     * @see PersistenceCapable#jdoGetObjectId()
     * @see PersistenceManager#getObjectId(Object obj)
     * @see PersistenceManager#getObjectById(Object oid)
     */
    static public Object getObjectId(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoGetObjectId();
        return null;
    }
    
    /**
     * Tests whether an object is dirty.
     *
     * If the object have been modified, deleted, or newly 
     * made persistent in the current transaction, <code>true</code> is returned.
     * For transient objects, <code>false</code> is returned.
     *
     * @param   obj     an <code>Object</code>
     * @return          <code>true</code> if the object has been modified
     *                  in the current transaction; <code>false</code>
     *                  otherwise.
     * @see PersistenceCapable#jdoIsDirty()
     * @see PersistenceCapable#jdoMakeDirty(String fieldName)
     */
    static public boolean isDirty(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoIsDirty();
        return false;
    }

    /**
     * Tests whether an object is transactional.
     *
     * For objects that respect transaction boundaries, <code>true</code> is
     * returned.
     * These objects include transient objects made transactional as a
     * result of being the target of a makeTransactional method call; newly
     * made persistent or deleted persistent objects; persistent objects
     * read in data store transactions; and persistent objects modified in
     * optimistic transactions.
     * For non-transactional objects, <code>false</code> is returned.
     *
     * @param   obj     an <code>Object</code>
     * @return          <code>true</code> if the object is transactional;
     *                  <code>false</code> otherwise.
     * @see PersistenceCapable#jdoIsTransactional()
     */
    static public boolean isTransactional(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoIsTransactional();
        return false;
    }

    /**
     * Tests whether an object is persistent.
     *
     * For objects whose state is stored in the data store, <code>true</code>
     * is returned.
     * For transient objects, <code>false</code> is returned.
     *
     * @param   obj     an <code>Object</code>
     * @return          <code>true</code> if the object is persistent;
     *                  <code>false</code> otherwise.
     * @see PersistenceCapable#jdoIsPersistent()
     * @see PersistenceManager#makePersistent(Object obj)
     */
    static public boolean isPersistent(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoIsPersistent();
        return false;
    }

    /**
     * Tests whether the object has been newly made persistent.
     *
     * For objects that have been made persistent in the current transaction, 
     * <code>true</code> is returned.
     * For transient or objects, <code>false</code> is returned.
     *
     * @param   obj     an <code>Object</code>
     * @return          <code>true</code> if the object was made persistent
     *                  in the current transaction;
     *                  <code>false</code> otherwise.
     * @see PersistenceCapable#jdoIsNew()
     * @see PersistenceManager#makePersistent(Object obj)
     */
    static public boolean isNew(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoIsNew();
        return false;
    }

    /**
     * Tests whether the object has been deleted.
     *
     * For objects that have been deleted in the current transaction,
     * <code>true</code> is returned.
     * For transient objects, <code>false</code> is returned.
     *
     * @param   obj     an <code>Object</code>
     * @return          <code>true</code> if the object was deleted in the
     *                  current transaction;
     *                  <code>false</code> otherwise.
     * @see PersistenceCapable#jdoIsDeleted()
     * @see PersistenceManager#deletePersistent(Object obj)
     */
    static public boolean isDeleted(Object obj)
    {
        if (obj instanceof PersistenceCapable)
            return ((PersistenceCapable)obj).jdoIsDeleted();    
        return false;
    }

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
                                "com.sun.jdo.spi.persistence.support.sqlstore.impl.Bundle", // NOI18N
                                JDOHelper.class.getClassLoader());

    /**
     * Help string
     */
    static final String null_instance = "null"; //NOI18N

    /**
     * Prints the object.
     * If object is not NULL and is not deleted, calls toString() method
     * on the object.
     * Does not allow to access fields of the deleted object
     */
    static public String printObject(Object o) {
        if (o==null)
                return null_instance;
        else if (isDeleted(o))
                return I18NHelper.getMessage(messages, "jdohelper.deleted_instance", //NOI18N
				o.getClass().getName());
        else
                return o.toString();
  }

   /** Returns the class loader for the class of the object.
   * If object is an instance of the java.util.Collection or
   * an Array it is recursively checked for the class loader
   * of its elements.
   *
   * @param obj the object to get the class loader for
   * @return the class loader that loaded the class or interface 
   * represented by this object.
   */
   static private ClassLoader getObjectClassLoader(Object obj) {
	Class clazz = obj.getClass();

	if (obj instanceof Collection) {
	    return getCollectionClassLoader((Collection)obj);
	} else if (clazz.isArray()) {
	    return getArrayClassLoader((Object[])obj);
	} else {
	    return clazz.getClassLoader();
	}
   }

   /** Returns the class loader for the elements of the collection.
   * If element is itself an instance of the java.util.Collection or
   * an Array it is recursively checked for its class loader.
   *
   * @param col collection of objects to get the class loader for
   * @return the class loader that loaded the class or interface 
   * represented by its elements.
   */
   static private ClassLoader getCollectionClassLoader (Collection col) {
	Object[] arr = col.toArray();
	return getArrayClassLoader(arr);
   }

   /** Returns the first not null class loader for the elements of the 
   * object array.
   * If element is itself an instance of the java.util.Collection or
   * an Array it is recursively checked for its class loader.
   *
   * @param arr array of objects to get the class loader for
   * @return the class loader that loaded the class or interface 
   * represented by its elements.
   */
   static private ClassLoader getArrayClassLoader (Object[] arr) {
	ClassLoader cl = null;

	for (int i = 0; i < arr.length; i++) {
	    cl = getObjectClassLoader(arr[i]);
	    if (cl != null) {
		break;
	    }
	}
	return cl;
   }
}
