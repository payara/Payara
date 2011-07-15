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
 * sco.HashSet.java
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable;
import com.sun.jdo.spi.persistence.support.sqlstore.StateManager;
import com.sun.jdo.spi.persistence.support.sqlstore.SCOCollection;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceManager;
import org.glassfish.persistence.common.I18NHelper;


/**
 * A mutable 2nd class object date.
 * @author Marina Vatkina
 * @version 1.0
 * @see java.util.HashSet
 */
public class HashSet
    extends java.util.HashSet
    implements SCOCollection
{

    private transient PersistenceCapable owner;

    private transient String fieldName;

    private transient Class elementType;

    private transient boolean allowNulls;

    private transient java.util.HashSet added = new java.util.HashSet();

    private transient java.util.HashSet removed = new java.util.HashSet();

    private transient boolean isDeferred;

    /**
     * I18N message handlers
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
                             "com.sun.jdo.spi.persistence.support.sqlstore.impl.Bundle", // NOI18N
                             HashSet.class.getClassLoader());

    private final static ResourceBundle messages1 = I18NHelper.loadBundle(
                             "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
                             HashSet.class.getClassLoader());


    /**
     * Creates a new empty <code>HashSet</code> object.
     * Assigns owning object and field name.
     *
     * @param owner the owning object
     * @param fieldName the owning field name
     * @param elementType the element types allowed
     * @param allowNulls true if nulls are allowed
     */
    public HashSet(Object owner, String fieldName, Class elementType, boolean allowNulls)
    {
        super();
    if (owner instanceof PersistenceCapable)
        {
                this.owner = (PersistenceCapable)owner;
            this.fieldName = fieldName;
        }
    this.elementType = elementType;
        this.allowNulls = allowNulls;
    }

    /**
     * Creates a new empty <code>HashSet</code> object that has
     * the specified initial capacity.Assigns owning object and field name.
     *
     * @param owner     the owning object
     * @param fieldName the owning field name
     * @param elementType the element types allowed
     * @param allowNulls true if nulls are allowed
     * @param      initialCapacity   the initial capacity of the hash map.
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero.
     * @see java.util.HashSet
     */
    public HashSet(Object owner, String fieldName,
                                Class elementType, boolean allowNulls,
                                int initialCapacity)
    {
    super(initialCapacity);
    if (owner instanceof PersistenceCapable)
        {
                this.owner = (PersistenceCapable)owner;
            this.fieldName = fieldName;
        }
    this.elementType = elementType;
        this.allowNulls = allowNulls;
    }

    // -------------------------Public Methods------------------

    /**
     * Adds the specified element to this set if it is not already
     * present.
     *
     * @param o element to be added to this set.
     * @return <tt>true</tt> if the set did not already contain the specified
     * element.
     * @see java.util.HashSet
     */
    public boolean add(Object o)
    {
        if (allowNulls == false && o == null)
        {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

        if (elementType != null && !elementType.isAssignableFrom(o.getClass()))
        {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                                     "sco.classcastexception", elementType.getName()), // NOI18N
                                       new ClassCastException(), new Object[] {o});
        }

        if (owner != null)
        {
            StateManager stateManager = owner.jdoGetStateManager();

            if (stateManager != null)
            {
                PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                pm.acquireShareLock();

                boolean modified = false;

                try
                {
                    pm.acquireFieldUpdateLock();
                    try
                    {
                       // Mark the field as dirty
                       stateManager.makeDirty(fieldName);

                       modified = super.add(o);

                       if (modified)
                       {
                           if (removed.remove(o) == false)
                           {
                               added.add(o);
                           }
                           stateManager.applyUpdates(fieldName, this);
                       }
                       return modified;
                    }
                    finally
                    {
                        pm.releaseFieldUpdateLock();
                    }

                }
                catch (JDOUserException e)
                {
                    Object[] failedObjects = e.getFailedObjectArray();

                    if (modified && (failedObjects != null))
                    {
                        //
                        // The failedObjects array may contain objects other
                        // than the one added. We iterate through it to find
                        // the one added and remove it from the collection.
                        //
                        for (int i = 0; i < failedObjects.length; i++)
                        {
                            Object failedObject = failedObjects[i];

                            if (failedObject == o)
                            {
                                super.remove(failedObject);
                                break;
                            }
                        }
                    }

                    throw e;
                }
                finally
                {
                    pm.releaseShareLock();
                }
            }
        }

        return super.add(o);
    }

    /**
     * Adds all of the elements in the specified collection to this collection
     *
     * @param c collection whose elements are to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     * call.
     * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
     *            not supported by this collection.
     *
     * @see java.util.AbstractCollection
     * @see java.util.HashSet
     */
    public boolean addAll(Collection c)
    {
        if (allowNulls == false && c.contains(null))
        {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

        ArrayList errc = new ArrayList();

        if (elementType != null)
        {
            // iterate the collection and make a list of wrong elements.
            Iterator i = c.iterator();
            while (i.hasNext())
            {
                Object o = i.next();
                if (!elementType.isAssignableFrom(o.getClass()))
                    errc.add(o);
            }
        }

        if (errc != null && errc.size() > 0)
        {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                                     "sco.classcastexception", elementType.getName()), // NOI18N
                                       new ClassCastException(), errc.toArray());
        }

        boolean modified = false;

        if (owner != null)
        {
            StateManager stateManager = owner.jdoGetStateManager();

            if (stateManager != null)
            {
                PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                pm.acquireShareLock();

                try
                {
                    pm.acquireFieldUpdateLock();
                    try
                    {
                        // Mark the field as dirty
                        stateManager.makeDirty(fieldName);

                        for (Iterator iter = c.iterator(); iter.hasNext();)
                        {
                            Object o = iter.next();
                            if (!super.contains(o))
                            {
                                if (removed.remove(o) == false)
                                {
                                    added.add(o);
                                }

                                super.add(o);
                                modified = true;
                            }
                        }

                        // Apply updates
                        if (modified)
                        {
                            stateManager.applyUpdates(fieldName, this);
                        }
                        return modified;
                    }
                    finally
                    {
                        pm.releaseFieldUpdateLock();
                    }
                }
                catch (JDOUserException e)
                {
                    Object[] failedObjects = e.getFailedObjectArray();

                    if (modified && (failedObjects != null))
                    {
                        for (int i = 0; i < failedObjects.length; i++)
                        {
                            super.remove(failedObjects[i]);
                        }
                    }

                    throw e;
                }
                finally
                {
                    pm.releaseShareLock();
                }
            }
        }

        return super.addAll(c);
    }

    /**
     * Removes the given element from this set if it is present.
     *
     * @param o object to be removed from this set, if present.
     * @return <tt>true</tt> if the set contained the specified element.
     * @see java.util.HashSet
     */
    public boolean remove(Object o)
    {
    // Mark the field as dirty

        if (owner != null)
        {
            StateManager stateManager = owner.jdoGetStateManager();

            if (stateManager != null)
            {
                PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                pm.acquireShareLock();

                try
                {
                    pm.acquireFieldUpdateLock();
                    try
                    {
                        stateManager.makeDirty(fieldName);

                        boolean modified = super.remove(o);

                        if (modified)
                        {
                            if (added.remove(o) == false)
                            {
                                removed.add(o);
                            }

                            stateManager.applyUpdates(fieldName, this);
                        }

                        return modified;
                    }
                    finally
                    {
                        pm.releaseFieldUpdateLock();
                    }
                }
                finally
                {
                    pm.releaseShareLock();
                }
            }
        }

        return super.remove(o);
    }


    /**
     * Removes from this collection all of its elements that are contained in
     * the specified collection (optional operation). <p>
     *
     *
     * @param c elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     * call.
     *
     * @throws    UnsupportedOperationException removeAll is not supported
     *            by this collection.
     *
     * @see java.util.HashSet
     * @see java.util.AbstractCollection
     */
    public boolean removeAll(Collection c)
    {
        // Mark the field as dirty

        if (owner != null)
        {
            StateManager stateManager = owner.jdoGetStateManager();

            if (stateManager != null)
            {
                PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                pm.acquireShareLock();

                try
                {
                    pm.acquireFieldUpdateLock();
                    try
                    {
                        stateManager.makeDirty(fieldName);

                        for (Iterator iter = c.iterator(); iter.hasNext();)
                        {
                            Object o = iter.next();
                            if (super.contains(o))
                            {
                                if (added.remove(o) == false)
                                {
                                    removed.add(o);
                                }
                            }
                        }

                        boolean modified = super.removeAll(c);

                        // Apply updates
                        if (modified)
                        {
                            stateManager.applyUpdates(fieldName, this);
                        }

                        return modified;
                    }
                    finally
                    {
                        pm.releaseFieldUpdateLock();
                    }
                }
                finally
                {
                    pm.releaseShareLock();
                }
            }
        }

        return super.removeAll(c);
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).
     *
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call.
     *
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
     *            is not supported by this collection.
     *
     * @see java.util.HashSet
     * @see java.util.AbstractCollection
     */
    public boolean retainAll(Collection c)
    {
        if (owner != null)
        {
            StateManager stateManager = owner.jdoGetStateManager();

            if (stateManager != null)
            {
                PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                pm.acquireShareLock();

                try
                {
                    pm.acquireFieldUpdateLock();
                    try
                    {
                        // Mark the field as dirty
                        stateManager.makeDirty(fieldName);

                        for (Iterator iter = super.iterator(); iter.hasNext();)
                        {
                            Object o = iter.next();
                            if (!c.contains(o))
                            {
                                if (added.remove(o) == false)
                                {
                                    removed.add(o);
                                }
                            }
                        }

                        boolean modified = super.retainAll(c);

                        // Apply updates

                        if (modified)
                        {
                            stateManager.applyUpdates(fieldName, this);
                        }

                        return modified;
                    }
                    finally
                    {
                        pm.releaseFieldUpdateLock();
                    }
                }
                finally
                {
                    pm.releaseShareLock();
                }
            }
        }

        return super.retainAll(c);
    }

    /**
     * Removes all of the elements from this set.
     * @see java.util.HashSet
     */
    public void clear()
    {
        if (owner != null)
        {
            StateManager stateManager = owner.jdoGetStateManager();

            if (stateManager != null)
            {
                PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                pm.acquireShareLock();

                try
                {
                    pm.acquireFieldUpdateLock();
                    try
                    {
                        // Mark the field as dirty
                        stateManager.makeDirty(fieldName);

                        removed.clear();
                        added.clear();

                        for (Iterator iter = super.iterator(); iter.hasNext();)
                        {
                            removed.add(iter.next());
                        }

                        super.clear();

                        // Apply updates
                        stateManager.applyUpdates(fieldName, this);
                        return;
                    }
                    finally
                    {
                        pm.releaseFieldUpdateLock();
                    }
                }
                finally
                {
                    pm.releaseShareLock();
                }
            }
        }

        super.clear();
    }

    /**
     * Creates and returns a copy of this object.
     *
     * <P>Mutable Second Class Objects are required to provide a public
     * clone method in order to allow for copying PersistenceCapable
     * objects. In contrast to Object.clone(), this method must not throw a
     * CloneNotSupportedException.
     */
    public Object clone()
    {
        HashSet obj = (HashSet) super.clone();

        // RESOLVE: check if added/removed should not be cleared
        // for a deferred collection, but applyDeferredUpdates logic
        // be used?
    obj.unsetOwner();

        return obj;
    }

    /**
     * Returns an iterator over the elements in this set.  The elements
     * are returned in no particular order.
     *
     * @return an Iterator over the elements in this set.
     * @see java.util.ConcurrentModificationException
     */
    public Iterator iterator() {
        return new SCOHashIterator(super.iterator(), this);
    }

    private class SCOHashIterator implements Iterator {
        Iterator _iterator = null;
        HashSet _caller = null;
        Object lastReturned = null;

        SCOHashIterator(Iterator it, HashSet cl) {
            _iterator = it;
            _caller = cl;
        }

        public boolean hasNext() {
            return _iterator.hasNext();
        }

        public Object next() {
            lastReturned = _iterator.next();
            return  lastReturned;
        }

        public void remove() {
            // Check if called twice.
            if (lastReturned == null)
                throw new IllegalStateException();

            if (_caller.owner != null) {
                // Mark the field as dirty
                StateManager stateManager = _caller.owner.jdoGetStateManager();

                if (stateManager != null) {
                    PersistenceManager pm = (PersistenceManager) stateManager.getPersistenceManagerInternal();

                    pm.acquireShareLock();

                    try {
                        pm.acquireFieldUpdateLock();
                        try
                        {
                            stateManager.makeDirty(_caller.fieldName);

                            _iterator.remove();

                            if (added.remove(lastReturned) == false) {
                                removed.add(lastReturned);
                            }

                            stateManager.applyUpdates(_caller.fieldName, _caller);

                        } finally {
                            pm.releaseFieldUpdateLock();
                        }
                    }
                    finally
                    {
                        pm.releaseShareLock();
                    }
                }
            } else {
                // No owner - regular HashSet operation.
                _iterator.remove();
            }
            lastReturned = null;
        }
    }

    //
    // The following internal methods should be called under an outer lock such
    // as fieldUpdateLock. There is no need to synchronize them.
    //

    /**
     * Creates and returns a copy of this object without resetting the owner and field value.
     *
     */
    public Object cloneInternal()
    {
        return super.clone();
    }

    /**
     * Cleans removed and added lists
     */
    public void reset()
    {
        // RESOLVE: do we need to synchronize this??
        if (added != null)
            added.clear();

        if (removed != null)
            removed.clear();
    }

    /**
     * Mark this HashSet as deferred.
     */
    public void markDeferred()
    {
        isDeferred = true;
    }

    /**
     * Return true is this HashSet is deferred, false otherwise.
     */
    public boolean isDeferred()
    {
        return isDeferred;
    }

    /**
     * If the HashSet is deferred, we first initialize the internal collection
     * with c and they apply any deferred updates specified by the added and
     * removed lists.
     */
    public void applyDeferredUpdates(Collection c)
    {
        if (!isDeferred)
        {
            // should throw an exception??
            return;
        }

        isDeferred = false;
        addAllInternal(c);

        addAllInternal(added);
        removeAllInternal(removed);
        added.clear();
        removed.clear();
    }

    /**
      * Adds an object to the list without recording changes if the HashSet is
      * not deferred. Otherwise, add o to the added list.
     */
    public void addInternal(Object o)
    {
        if (isDeferred)
        {
            if (removed.remove(o) == false)
            {
                added.add(o);
            }
        }
        else
        {
            super.add(o);
        }
    }


    /**
     * Adds a Collection to the list without recording changes if the HashSet is
     * not deferred. Otherwise, add o to the removed list.
     */
    public void addAllInternal(Collection c)
    {
        if (c == null)
        {
            return;
        }

        Iterator iter = c.iterator();

        while (iter.hasNext())
        {
            addInternal(iter.next());
        }
    }

    /**
     * @inheritDoc
     */
    public void addToBaseCollection(Object o)
    {
        super.add(o);
    }

    /*
     * Remove c from the list if the HashSet is not deferred.
     * Otherwise, add c to the removed list.
     */
    public void removeAllInternal(Collection c)
    {
        if (c == null)
        {
            return;
        }

        Iterator iter = c.iterator();

        while (iter.hasNext())
        {
            removeInternal(iter.next());
        }
    }

    /**
     * Returns added collection
     *
     * @return added    collection of added elements
     */
    public Collection getAdded()
    {
        return (Collection)added;
    }

    /**
     * Returns removed collection
     *
     * @return removed  collection of removed elements
     */
    public Collection getRemoved()
    {
        return (Collection)removed;
    }


    /**
     * Clears Collection without notifing the owner
     */
    public void clearInternal()
    {
        super.clear();
        this.reset();
    }

    /**
     * Removes an element without notifing the owner
     */
    public void removeInternal(Object o)
    {
        if (isDeferred)
        {
            if (added.remove(o) == false)
            {
                removed.add(o);
            }

        }
        else
        {
            super.remove(o);
        }
    }

    /**
     * Nullifies references to the owner Object and Field
     */
    public void unsetOwner()
    {
        this.owner = null;
        this.fieldName = null;
        this.elementType = null;
                added.clear();
                removed.clear();
    }

    /**
     * Returns the owner object of the SCO instance
     *
     * @return owner object
     */
    public Object getOwner()
    {
        return this.owner;
    }

    /**
     * Returns the field name
     *
     * @return field name as java.lang.String
     */
    public String getFieldName()
    {
        return this.fieldName;
    }

    /**
     * Marks object dirty
     */
    public StateManager makeDirty()
    {
        StateManager stateManager = owner.jdoGetStateManager();

        if (stateManager != null)
        {
            stateManager.makeDirty(fieldName);
        }

        return stateManager;
    }


    /**
     * Apply changes (can be a no-op)
     */
    public void applyUpdates(StateManager sm, boolean modified)
    {
         if (modified && sm != null)
        {
            sm.applyUpdates(fieldName, this);
        }
    }

    /**
     * Set the owner if this instance is not owned.
     * @see SCOCollection#setOwner
     * @param owner the new owner.
     * @param fieldName the new field name.
     * @param elementType the new element type as Class, or null if type
     * is not checked or not supported.
     */
    public void setOwner(Object owner, String fieldName, Class elementType) {

        if (this.owner != null) {
            throw new JDOUserException(I18NHelper.getMessage(
                messages1, "core.statemanager.anotherowner"), // NOI18N
                new Object[]{this.owner, this.fieldName});
        }
        if (owner instanceof PersistenceCapable) {
                this.owner = (PersistenceCapable)owner;
                this.fieldName = fieldName;
                this.elementType = elementType;
        }
    }
    }




