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
 * PersistenceManagerWrapper.java
 *
 * Created on January 16, 2001
 */

package com.sun.jdo.spi.persistence.support.sqlstore.impl;

import com.sun.jdo.api.persistence.support.*;
import org.glassfish.persistence.common.I18NHelper;

import java.util.Collection;
import java.util.Properties;
import java.util.ResourceBundle;

/** PersistenceManagerWrapper is ....
 */

public class PersistenceManagerWrapper implements PersistenceManager {

    // Previous  PersistenceManagerWrapper
    private PersistenceManagerWrapper prev = null;

    // Actual  PersistenceManager
    private PersistenceManagerImpl pm = null;

    // Boolean flag that allows to use this wrapper
    private boolean isValid = false;

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            PersistenceManagerWrapper.class.getClassLoader());

    // Constructed by  com.sun.jdo.spi.persistence.support.sqlstore.SQLPersistenceManagerFactory
    PersistenceManagerWrapper(PersistenceManagerImpl pm) {
        this.pm = pm;
        prev = pm.getCurrentWrapper();
        pm.pushCurrentWrapper(this);
        isValid = true;
    }

    /** A PersistenceManager instance can be used until it is closed.
     * @return if this PersistenceManager has been closed
     * @see #close()
     */
    public boolean isClosed() {
        if (isValid) {
            return pm.isClosed();
        } else {
            return true;
        }
    }

    /** A PersistenceManager instance can be used until it is closed.
     *
     * <P>This method closes the PersistenceManager, which if pooled, releases it
     * to the pool of available PersistenceManagers.
     */
    public void close() {
        if (isValid) {
            isValid = false;
            pm.popCurrentWrapper(prev);
            pm = null;
            prev = null;
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** There is exactly one Transaction associated with a PersistenceManager.
     * @return the Transaction associated with this
     * PersistenceManager.
     */
    public Transaction currentTransaction() {
        if (isValid) {
            return pm.currentTransaction();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Create a new Query with no elements.
     * @return a new Query instance with no elements.
     */
    public Query newQuery() {
        if (isValid) {
            return pm.newQuery();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Create a new Query using elements from another Query.  The other Query
     * must have been created by the same JDO implementation.  It might be active
     * in a different PersistenceManager or might have been serialized and
     * restored.
     * @return the new Query
     * @param compiled another Query from the same JDO implementation
     */
    public Query newQuery(Object compiled) {
        if (isValid) {
            return pm.newQuery(compiled);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Create a new Query specifying the Class of the results.
     * @param cls the Class of the results
     * @return the new Query
     */
    public Query newQuery(Class cls) {
        if (isValid) {
            return pm.newQuery(cls);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Create a new Query with the Class of the results and candidate Collection.
     * specified.
     * @param cls the Class of results
     * @param cln the Collection of candidate instances
     * @return the new Query
     */
    public Query newQuery(Class cls, Collection cln) {
        if (isValid) {
            return pm.newQuery(cls, cln);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Create a new Query with the Class of the results and Filter.
     * specified.
     * @param cls the Class of results
     * @param filter the Filter for candidate instances
     * @return the new Query
     */
    public Query newQuery(Class cls, String filter) {
        if (isValid) {
            return pm.newQuery(cls, filter);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Create a new Query with the Class of the results, candidate Collection,
     * and Filter.
     * @param cls the Class of results
     * @param cln the Collection of candidate instances
     * @param filter the Filter for candidate instances
     * @return the new Query
     */
    public Query newQuery(Class cls, Collection cln, String filter) {
        if (isValid) {
            return pm.newQuery(cls, cln, filter);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** The PersistenceManager may manage a collection of instances in the data
     * store based on the class of the instances.  This method returns a
     * Collection of instances in the data store that might be iterated or
     * given to a Query as the Collection of candidate instances.
     * @param persistenceCapableClass Class of instances
     * @param subclasses whether to include instances of subclasses
     * @return a Collection of instances
     * @see Query
     */
    public Collection getExtent(Class persistenceCapableClass, boolean subclasses) {
        if (isValid) {
            return pm.getExtent(persistenceCapableClass, subclasses);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** This method locates a persistent instance in the cache of instances
     * managed by this PersistenceManager.  If an instance with the same ObjectId
     * is found it is returned.  Otherwise, a new instance is created and
     * associated with the ObjectId.
     *
     * <P>If the instance does not exist in the data store, then this method will
     * not fail.  However, a request to access fields of the instance will
     * throw an exception.
     * @param oid an ObjectId
     * @return the PersistenceCapable instance with the specified
     * ObjectId
     */
    public Object getObjectById(Object oid) {
        if (isValid) {
            return pm.getObjectById(oid);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** This method locates a persistent instance in the cache of instances
     * managed by this <code>PersistenceManager</code>.
     * The <code>getObjectById</code> method attempts
     * to find an instance in the cache with the specified JDO identity.
     * The <code>oid</code> parameter object might have been returned by an earlier call
     * to <code>getObjectId</code> or might have been constructed by the application.
     * <P>If the <code>PersistenceManager</code> is unable to resolve the <code>oid</code> parameter
     * to an ObjectId instance, then it throws a <code>JDOUserException</code>.
     * <P>If the <code>validate</code> flag is <code>false</code>, and there is already an instance in the
     * cache with the same JDO identity as the <code>oid</code> parameter, then this method
     * returns it. There is no change made to the state of the returned
     * instance.
     * <P>If there is not an instance already in the cache with the same JDO
     * identity as the <code>oid</code> parameter, then this method creates an instance
     * with the specified JDO identity and returns it. If there is no
     * transaction in progress, the returned instance will be hollow or
     * persistent-nontransactional, at the choice of the implementation.
     * <P>If there is a transaction in progress, the returned instance will
     * be hollow, persistent-nontransactional, or persistent-clean, at the
     * choice of the implementation.
     * @see #getObjectId(Object pc)
     * @return the <code>PersistenceCapable</code> instance with the specified ObjectId
     * @param oid an ObjectId
     * @param validate if the existence of the instance is to be validated
     */  
    public Object getObjectById (Object oid, boolean validate) {
        if (isValid) {
            return pm.getObjectById(oid, validate);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** The ObjectId returned by this method represents the JDO identity of
     * the instance.  The ObjectId is a copy (clone) of the internal state
     * of the instance, and changing it does not affect the JDO identity of
     * the instance.
     * @param pc the PersistenceCapable instance
     * @return the ObjectId of the instance
     */
    public Object getObjectId(Object pc) {
        if (isValid) {
            return pm.getObjectId(pc);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** This method is used to get a PersistenceCapable instance
     * representing the same data store object as the parameter, that is valid
     * for this PersistenceManager.
     * @param pc a PersistenceCapable instance
     * @return the PersistenceCapable instance representing the
     * same data store object
     */
    public Object getTransactionalInstance(Object pc) {
        if (isValid) {
            return pm.getTransactionalInstance(pc);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Make the transient instance persistent in this PersistenceManager.
     * This method must be called in an active transaction.
     * The PersistenceManager assigns an ObjectId to the instance and
     * transitions it to persistent-new.
     * The instance will be managed in the Extent associated with its Class.
     * The instance will be put into the data store at commit.
     * @param pc a transient instance of a Class that implements
     * PersistenceCapable
     */
    public void makePersistent(Object pc) {
        if (isValid) {
            pm.makePersistent(pc);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Make an array of instances persistent.
     * @param pcs an array of transient instances
     * @see #makePersistent(Object pc)
     */
    public void makePersistent(Object[] pcs) {
        if (isValid) {
            pm.makePersistent(pcs);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Make a Collection of instances persistent.
     * @param pcs a Collection of transient instances
     * @see #makePersistent(Object pc)
     */
    public void makePersistent(Collection pcs) {
        if (isValid) {
            pm.makePersistent(pcs);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Delete the persistent instance from the data store.
     * This method must be called in an active transaction.
     * The data store object will be removed at commit.
     * Unlike makePersistent, which makes the closure of the instance persistent,
     * the closure of the instance is not deleted from the data store.
     * This method has no effect if the instance is already deleted in the
     * current transaction.
     * This method throws an exception if the instance is transient or is managed by another
     * PersistenceManager.
     *
     * @param pc a persistent instance
     */
    public void deletePersistent(Object pc) {
        if (isValid) {
            pm.deletePersistent(pc);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Delete an array of instances from the data store.
     * @param pcs a Collection of persistent instances
     * @see #deletePersistent(Object pc)
     */
    public void deletePersistent(Object[] pcs) {
        if (isValid) {
            pm.deletePersistent(pcs);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** Delete a Collection of instances from the data store.
     * @param pcs a Collection of persistent instances
     * @see #deletePersistent(Object pc)
     */
    public void deletePersistent(Collection pcs) {
        if (isValid) {
            pm.deletePersistent(pcs);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** This method returns the PersistenceManagerFactory used to create
     * this PersistenceManager.  It returns null if this instance was
     * created via a constructor.
     * @return the PersistenceManagerFactory that created
     * this PersistenceManager
     */
    public PersistenceManagerFactory getPersistenceManagerFactory() {
        if (isValid) {
            return pm.getPersistenceManagerFactory();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** The application can manage the PersistenceManager instances
     * more easily by having an application object associated with each
     * PersistenceManager instance.
     * @param o the user instance to be remembered by the PersistenceManager
     * @see #getUserObject
     */
    public void setUserObject(Object o) {
        if (isValid) {
            pm.setUserObject(o);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** The application can manage the PersistenceManager instances
     * more easily by having an application object associated with each
     * PersistenceManager instance.
     * @return the user object associated with this PersistenceManager
     * @see #setUserObject
     */
    public Object getUserObject() {
        if (isValid) {
            return pm.getUserObject();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /** The JDO vendor might store certain non-operational properties and
     * make those properties available to applications (for troubleshooting).
     *
     * <P>Standard properties include:
     * <li>VendorName</li>
     * <li>VersionNumber</li>
     * @return the Properties of this PersistenceManager
     */
    public Properties getProperties() {
        if (isValid) {
            return pm.getProperties();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /**
     * Returns the boolean value of the supersedeDeletedInstance flag
     * for this PersistenceManager. If set to true, deleted instances are
     * allowed to be replaced with persistent-new instances with the equal
     * Object Id.
     * @return      boolean supersedeDeletedInstance flag
     */
    public boolean getSupersedeDeletedInstance () {
        if (isValid) {
            return pm.getSupersedeDeletedInstance();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }


    /**
     * Sets the supersedeDeletedInstance flag for this PersistenceManager.
     * @param flag          boolean supersedeDeletedInstance flag
     */
    public void setSupersedeDeletedInstance (boolean flag) {
        if (isValid) {
            pm.setSupersedeDeletedInstance(flag);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /**
     * Returns the boolean value of the requireCopyObjectId flag
     * for this PersistenceManager. If set to false, the PersistenceManager
     * does not create a copy of an ObjectId for <code>PersistenceManager.getObjectId(Object pc)</code>
     * and <code>PersistenceManager.getObjectById(Object oid)</code> requests.
     *
     * @see PersistenceManager#getObjectId(Object pc)
     * @see PersistenceManager#getObjectById(Object oid)
     * @return      boolean requireCopyObjectId flag
     */
    public boolean getRequireCopyObjectId() {
        if (isValid) {
            return pm.getRequireCopyObjectId();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }
  
  
    /**
     * Sets the requireCopyObjectId flag for this PersistenceManager.
     * If set to false, the PersistenceManager will not create a copy of
     * an ObjectId for <code>PersistenceManager.getObjectId(Object pc)</code>
     * and <code>PersistenceManager.getObjectById(Object oid)</code> requests.
     *
     * @see PersistenceManager#getObjectId(Object pc)
     * @see PersistenceManager#getObjectById(Object oid)
     * @param flag          boolean requireCopyObjectId flag
     */
    public void setRequireCopyObjectId (boolean flag) {
        if (isValid) {
            pm.setRequireCopyObjectId(flag);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /**
     * Returns the boolean value of the requireTrackedSCO flag
     * for this PersistenceManager. If set to false, the PersistenceManager
     * will not create tracked SCO instances for 
     * new persistent instances at commit with retainValues set to true
     * and while retrieving data from a datastore.
     *   
     * @return      boolean requireTrackedSCO flag
     */  
    public boolean getRequireTrackedSCO() {
        if (isValid) {
            return pm.getRequireTrackedSCO();
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }

    /**
     * Sets the requireTrackedSCO flag for this PersistenceManager.
     * If set to false, the PersistenceManager will not create tracked
     * SCO instances for new persistent instances at commit with retainValues 
     * set to true and while retrieving data from a datastore.
     *
     * @param flag          boolean requireTrackedSCO flag
     */
    public void setRequireTrackedSCO (boolean flag) {
        if (isValid) {
            pm.setRequireTrackedSCO(flag);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }


    /** In order for the application to construct instance of the ObjectId class
     * it needs to know the class being used by the JDO implementation.
     * @param cls the PersistenceCapable Class
     * @return the Class of the ObjectId of the parameter
     */
    public Class getObjectIdClass(Class cls) {
        if (isValid) {
            return pm.getObjectIdClass(cls);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }


    /**
     * Returns a new Second Class Object instance of the type specified,
     * with the owner and field name to notify upon changes to the value
     * of any of its fields. If a collection class is created, then the
     * class does not restrict the element types, and allows nulls to be added as elements.
     *
     * @param type Class of the new SCO instance
     * @param owner the owner to notify upon changes
     * @param fieldName the field to notify upon changes
     * @return the object of the class type
     */
    public Object newSCOInstance(Class type, Object owner, String fieldName) {
        if (isValid) {
            return pm.newSCOInstance(type, owner, fieldName);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }


    /**
     * Returns a new Collection instance of the type specified, with the
     * owner and field name to notify upon changes to the value of any of its fields.
     * The collection class restricts the element types allowed to the elementType or
     * instances assignable to the elementType, and allows nulls to be added as
     * elements based on the setting of allowNulls. The Collection has an initial size
     * as specified by the initialSize parameter.
     *
     * @param type Class of the new SCO instance
     * @param owner the owner to notify upon changes
     * @param fieldName the field to notify upon changes
     * @param elementType the element types allowed
     * @param allowNulls true if allowed
     * @param initialSize initial size of the Collection
     * @return the object of the class type
     */
    public Object newCollectionInstance(Class type, Object owner, String fieldName,
                                        Class elementType, boolean allowNulls, int initialSize) {
        if (isValid) {
            return pm.newCollectionInstance(type, owner, fieldName, elementType, allowNulls, initialSize);
        } else {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerwrapper.invalidpm"));// NOI18N
        }
    }


    public PersistenceManager getPersistenceManager() {
        return (PersistenceManager) pm;
    }
}
