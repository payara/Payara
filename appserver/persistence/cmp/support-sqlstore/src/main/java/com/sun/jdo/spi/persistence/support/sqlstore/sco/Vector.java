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
 * sco.Vector.java
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sco;

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.api.persistence.support.JDOUnsupportedOptionException;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable;
import com.sun.jdo.spi.persistence.support.sqlstore.StateManager;
import com.sun.jdo.spi.persistence.support.sqlstore.SCOCollection;
import org.glassfish.persistence.common.I18NHelper;


/**
 * A mutable 2nd class object date.
 * @author Marina Vatkina
 * @version 1.0
 * @see java.util.Vector
 */
public class Vector
    extends java.util.Vector
    implements SCOCollection
{

    private transient PersistenceCapable owner;

    private transient String fieldName;

    private transient 	Class elementType;
 
    private transient boolean allowNulls;

    private transient java.util.Vector added = new java.util.Vector();

    private transient java.util.Vector removed = new java.util.Vector();

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
                             "com.sun.jdo.spi.persistence.support.sqlstore.impl.Bundle", // NOI18N
                             Vector.class.getClassLoader());

    private final static ResourceBundle messages1 = I18NHelper.loadBundle(
                             "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
                             Vector.class.getClassLoader());

    /**
     * Constructs an empty vector so that its internal data array
     * has size <tt>10</tt> and its standard capacity increment is
     * zero.  Assigns owning object and field name
     * @param owner the owning object
     * @param fieldName the owning field name
     * @param elementType the element types allowed
     * @param allowNulls true if nulls are allowed
     */
    public Vector(Object owner, String fieldName, Class elementType, boolean allowNulls)
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
     * Constructs an empty vector with the specified initial capacity and
     * with its capacity increment equal to zero. Assigns owning object and field name
     *
     * @param owner 		the owning object
     * @param fieldName 	the owning field name
     * @param elementType 	the element types allowed 
     * @param allowNulls 	true if nulls are allowed 
     * @param initialCapacity   the initial capacity of the vector.
     * @exception IllegalArgumentException if the specified initial capacity
     *               is negative
     */
    public Vector(Object owner, String fieldName,
			Class elementType, boolean allowNulls, int initialCapacity)
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

    /** ------------------Public Methods----------------*/

    /**
     * Sets the component at the specified <code>index</code> of this
     * vector to be the specified object. The previous component at that
     * position is discarded.<p> 
     *
     * @param      obj     what the component is to be set to.
     * @param      index   the specified index.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see java.util.Vector
     */  
    public synchronized void setElementAt(Object obj, int index) {

	throwUnsupportedOption();

	if (obj == null)
	{
		if (allowNulls == false)
		{
			throw new JDOUserException(I18NHelper.getMessage(messages,
				"sco.nulls_not_allowed")); // NOI18N
		}
		// It is actualy remove
		this.removeElementAt(index);
	}

	if (elementType == null || elementType.isAssignableFrom(obj.getClass()))
	{
		// Mark the field as dirty
		StateManager stateManager = this.makeDirty();

		Object o = super.elementAt(index);
		super.setElementAt(obj, index);

		if (added.remove(o) == false)
			removed.add(o);

		if (removed.remove(obj) == false)
			added.add(obj);

		// Apply updates
		this.applyUpdates(stateManager, true);

	} else {
		throw new JDOUserException(I18NHelper.getMessage(messages,
                                "sco.classcastexception", elementType.getName()), // NOI18N
				 new ClassCastException(), new Object[] {obj});
	}

    }

 
    /**
     * Deletes the component at the specified index. 
     *
     * @param      index   the index of the object to remove.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see java.util.Vector
     */  
    public synchronized void removeElementAt(int index) {

	throwUnsupportedOption();

	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

	Object obj = super.elementAt(index);
        super.removeElementAt(index);
	if (added.remove(obj) == false)
		removed.add(obj);

	// Apply updates
	this.applyUpdates(stateManager, true);

    }

    /**
     * Inserts the specified object as a component in this vector at the
     * specified <code>index</code>.
     * 
     * @param      obj     the component to insert.
     * @param      index   where to insert the new component.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see java.util.Vector
     */  
    public synchronized void insertElementAt(Object obj, int index) {
	if (allowNulls == false && obj == null)
        {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

        if (elementType == null || elementType.isAssignableFrom(obj.getClass()))
        { 
		// Mark the field as dirty
		StateManager stateManager = this.makeDirty();

                super.insertElementAt(obj, index);
        	if (removed.remove(obj) == false)
			added.add(obj);

		// Apply updates
		this.applyUpdates(stateManager, true);

	} else {
		throw new JDOUserException(I18NHelper.getMessage(messages,
                                "sco.classcastexception", elementType.getName()), // NOI18N
				 new ClassCastException(), new Object[] {obj});
	}

    } 
 
    /**
     * Adds the specified component to the end of this vector,
     * increasing its size by one. 
     *   
     * @param   obj   the component to be added.
     * @see java.util.Vector 
     */  
    public synchronized void addElement(Object obj) {
	if (allowNulls == false && obj == null)
        {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

        if (elementType == null || elementType.isAssignableFrom(obj.getClass()))
        { 
		// Mark the field as dirty
		StateManager stateManager = this.makeDirty();

                super.addElement(obj);
        	if (removed.remove(obj) == false)
			added.add(obj); 

		// Apply updates
		this.applyUpdates(stateManager, true);

	} else {
		throw new JDOUserException(I18NHelper.getMessage(messages,
                                "sco.classcastexception", elementType.getName()), // NOI18N
				 new ClassCastException(), new Object[] {obj});
	}

    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument
     * from this vector. 
     *   
     * @param   obj   the component to be removed.
     * @return  <code>true</code> if the argument was a component of this
     *          vector; <code>false</code> otherwise.
     * @see java.util.Vector
     */   
    public synchronized boolean removeElement(Object obj) {
	
        // Because java.util.Vector.removeElement(Object) calls internally removeElementAt(int)
        // which is not supported, we cannot rely on jdk. We need to process remove here.

        // Mark the field as dirty
        StateManager stateManager = this.makeDirty();

        int i = super.indexOf(obj);
        if (i > -1) {
                super.removeElementAt(i);

                if (added.remove(obj) == false)
                        removed.add(obj);

                // Apply updates
                this.applyUpdates(stateManager, true);
                return true;
        }
        return false;

    }

    /**
     * Removes all components from this vector and sets its size to zero.<p>
     *
     * @see java.util.Vector
     */ 
    public synchronized void removeAllElements() {
	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

	for (Iterator iter = super.iterator(); iter.hasNext();) {
		Object o = iter.next();
		if (added.remove(o) == false)
			removed.add(o);
	}
	added.clear();

	super.removeAllElements();

	// Apply updates
	this.applyUpdates(stateManager, true);

    }



    /**
     * Replaces the element at the specified position in this Vector with the
     * specified element.
     *   
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * @exception ArrayIndexOutOfBoundsException index out of range
     *            (index &lt; 0 || index &gt;= size()).
     * @exception IllegalArgumentException fromIndex &gt; toIndex.
     * @see java.util.Vector 
     */  
    public synchronized Object set(int index, Object element) {

	throwUnsupportedOption();

	if (element == null)
        {
		if (allowNulls == false)
                {
                        throw new JDOUserException(I18NHelper.getMessage(messages,
                                "sco.nulls_not_allowed")); // NOI18N
                }
                // It is actualy remove
                return this.remove(index); 
        }
        if (elementType == null || elementType.isAssignableFrom(element.getClass()))
        { 
		// Mark the field as dirty
		StateManager stateManager = this.makeDirty();

		Object o = super.set(index, element);

                if (added.remove(o) == false)
                        removed.add(o);

                if (removed.remove(element) == false)
                        added.add(element);

		// Apply updates
		this.applyUpdates(stateManager, true);

        	return o;
	} else {
		throw new JDOUserException(I18NHelper.getMessage(messages,
                                "sco.classcastexception", elementType.getName()), // NOI18N
				 new ClassCastException(), new Object[] {element});
	}

    } 


    /**
     * Appends the specified element to the end of this Vector.
     *   
     * @param o element to be appended to this Vector.
     * @return true (as per the general contract of Collection.add).
     * @see java.util.Vector
     */  
    public synchronized boolean add(Object o) {
	if (allowNulls == false && o == null)
        {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

	if (elementType == null || elementType.isAssignableFrom(o.getClass()))
        {
		// Mark the field as dirty
		StateManager stateManager = this.makeDirty();

                if (removed.remove(o) == false)
			added.add(o);

        	boolean modified = super.add(o);

		// Apply updates
		this.applyUpdates(stateManager, modified);

        	return modified;

	} else {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                                "sco.classcastexception", elementType.getName()), // NOI18N
				 new ClassCastException(), new Object[] {o});
	}

    }    

    /**
     * Removes the first occurrence of the specified element in this Vector
     * If the Vector does not contain the element, it is unchanged.  
     *
     * @param o element to be removed from this Vector, if present.
     * @return true if the Vector contained the specified element.
     * @see java.util.Vector 
     */   
    public boolean remove(Object o) {
        return this.removeElement(o);
    }

    /**
     * Inserts the specified element at the specified position in this Vector.
     *   
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * @exception ArrayIndexOutOfBoundsException index is out of range
     *            (index &lt; 0 || index &gt; size()).
     * @see java.util.Vector
     */  
    public void add(int index, Object element) {
        this.insertElementAt(element, index);
    }

    /**
     * Removes the element at the specified position in this Vector.
     * shifts any subsequent elements to the left (subtracts one from their
     * indices).  Returns the element that was removed from the Vector.
     *   
     * @param index the index of the element to removed.
     * @exception ArrayIndexOutOfBoundsException index out of range (index
     *            &lt; 0 || index &gt;= size()).
     * @see java.util.Vector 
     */   
    public synchronized Object remove(int index) {

	throwUnsupportedOption();

	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

        Object obj = super.remove(index);

        if (added.remove(obj) == false)
		removed.add(obj);

	// Apply updates
	this.applyUpdates(stateManager, true);

        return obj;
    } 

    /**
     * Removes all of the elements from this Vector.  The Vector will
     * be empty after this call returns (unless it throws an exception).
     *   
     * @see java.util.Vector
     */ 
    public void clear() {
        this.removeAllElements();
    }

    /**
     * Appends all of the elements in the specified Collection to the end of
     * this Vector, in the order that they are returned by the specified
     * Collection's Iterator.  
     *   
     * @param c elements to be inserted into this Vector.
     * @see java.util.Vector
     */  
    public synchronized boolean addAll(Collection c) {
	if (allowNulls == false && c.contains(null))
        {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

	java.util.Vector errc = new java.util.Vector();
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

	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

        removed.removeAll(c);
	added.addAll(c);

	boolean modified = super.addAll(c);

	// Apply updates
	this.applyUpdates(stateManager, modified);

	return modified;
    }

    /**
     * Removes from this Vector all of its elements that are contained in the
     * specified Collection.
     *
     * @return true if this Vector changed as a result of the call.
     * @see java.util.Vector 
     */   
    public synchronized boolean removeAll(Collection c) {
        boolean modified = false;
	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

        Iterator e = c.iterator();
        while (e.hasNext()) {
                Object o = e.next();
                if(super.contains(o)) {
                        removeInternal(o);
                        if (added.remove(o) == false)
                                removed.add(o);
                        modified = true;
                }
        }

	// Apply updates
	this.applyUpdates(stateManager, modified);

        return modified;
    } 

    /**
     * Inserts all of the elements in in the specified Collection into this
     * Vector at the specified position.  Shifts the element currently at
     * that position (if any) and any subsequent elements to the right
     * (increases their indices).  The new elements will appear in the Vector
     * in the order that they are returned by the specified Collection's
     * iterator.
     *   
     * @param index index at which to insert first element
     *              from the specified collection.
     * @param c elements to be inserted into this Vector.
     * @exception ArrayIndexOutOfBoundsException index out of range (index
     *            &lt; 0 || index &gt; size()).
     * @see java.util.Vector  
     */   
    public synchronized boolean addAll(int index, Collection c) {
	if (allowNulls == false && c.contains(null))
        {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "sco.nulls_not_allowed")); // NOI18N
        }

	java.util.Vector errc = new java.util.Vector();  
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

	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

        removed.removeAll(c); 
        added.addAll(c); 

        boolean modified = super.addAll(index, c);

	// Apply updates
	this.applyUpdates(stateManager, modified);

        return modified;
    }    

    /**
     * Retains only the elements in this Vector that are contained in the
     * specified Collection.  
     *   
     * @return true if this Vector changed as a result of the call.
     * @see java.util.Vector   
     */    
    public synchronized boolean retainAll(Collection c)  {
	boolean modified = false;
        java.util.Vector v = new java.util.Vector();

	// Mark the field as dirty
	StateManager stateManager = this.makeDirty();

        for (Iterator iter = super.iterator(); iter.hasNext();)
        {
                Object o = iter.next();
                if (!c.contains(o))
                {
                        v.add(o);
                        if (added.remove(o) == false)
                                removed.add(o);

                        modified = true;
                }
        }

        // Now remove the rest (stored in "v")
        for (Iterator iter = v.iterator(); iter.hasNext();)
        {
                        removeInternal(iter.next());
        }

	// Apply updates
	this.applyUpdates(stateManager, modified);


        return modified;
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
        Vector obj = (Vector)super.clone();
        obj.unsetOwner();

        return obj;
    }

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
        added.clear();
        removed.clear();
    }


	public void markDeferred()
	{
	}

	public boolean isDeferred()
	{
		return false;
	}

	public void applyDeferredUpdates(Collection c)
	{
		super.addAll(c);
	}

    /**
     * Adds an object to the list without recording changes
     */
    public void addInternal(Object o)
    {
        super.addElement(o);
    }

 
    /** 
     * Adds a Collection to the list without recording changes 
     */ 
    public void addAllInternal(Collection c) 
    { 
        super.addAll(c); 
    }

    /**
     * @inheritDoc
     */
    public void addToBaseCollection(Object o)
    {
        super.add(o);
    }

    /**
     * Removes from this collection without recording changes
     */ 
    public void removeAllInternal(Collection c) 
    { 
        super.removeAll(c); 
	}

    /**
     * Returns added collection
     * 
     * @return added	collection of added elements
     */ 
    public Collection getAdded()
    {
        return (Collection)added;
    }    

    /** 
     * Returns removed collection 
     *  
     * @return removed	collection of removed elements 
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
	//Cannot call super.clear() as it internally calls removeAllElements()
	// which causes marking field as dirty.

	int s = super.size() - 1;

	// Need to loop backwards to avoid resetting size of the collection
	for (int i = s; i > -1; i--)
	{
		super.removeElementAt(i);
	}
		
	this.reset();
    }


    /**
     * Removes an element without notifing the owner
     */  
    public void removeInternal(Object o)
    {
        int i = super.indexOf(o);
        super.remove(i);
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
	if (owner != null)
	{
                StateManager stateManager = owner.jdoGetStateManager();
                if (stateManager != null)
                {
                        stateManager.makeDirty(fieldName);
                }
 
                return stateManager;
	}
	return null;
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
     * Throw JDOUnsupportedOptionException
     */  
    private void throwUnsupportedOption()
    {
        // Index operation that changes the underline collection
        // is not supported in 2_beta. Bug 4370474
        throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                                "sco.not_supported")); //NOI18N
    }

    /**
     * Set the owner if this instance is not owned.
     * @see SCOCollection#setOwner
     * @param owner the new owner.
     * @param fieldName the new field name.
     * @param elementType the new element type as Class, or null if type
     * is not to be checked.
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
