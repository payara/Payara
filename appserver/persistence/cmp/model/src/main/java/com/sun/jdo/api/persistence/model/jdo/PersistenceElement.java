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
 * PersistenceElement.java
 *
 * Created on February 28, 2000, 3:37 PM
 */

package com.sun.jdo.api.persistence.model.jdo;

import java.util.ResourceBundle;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.text.Collator;

import com.sun.jdo.api.persistence.model.ModelException;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public abstract class PersistenceElement extends Object
	implements PersistenceElementProperties, Comparable
{
	/** I18N message handler */
	private static final ResourceBundle _messages = I18NHelper.loadBundle(
		"com.sun.jdo.api.persistence.model.Bundle",		// NOI18N
		PersistenceElement.class.getClassLoader());

	/** Implementation */
	Impl _impl;

	/** Create new PersistenceElement with no implementation. 
	 * This constructor should only be used for cloning and archiving.
	 */
	public PersistenceElement ()
	{
		this(null);
	}

	/** Create new PersistenceElement with the provided implementation. The 
	 * implementation is responsible for storing all properties of the object.
	 * @param impl the implementation to use
	 */
	protected PersistenceElement (PersistenceElement.Impl impl)
	{
		setImpl(impl);
	}

	/** @return implemetation factory for this element
	 */
	public final Impl getImpl () { return _impl; }

	/** @return I18N message handler for this element
	 */
	protected static final ResourceBundle getMessages () { return _messages; }

	/** Add a property change listener.
	 * @param l the listener to add
	 * @see PersistenceElementProperties
	 */
	public final void addPropertyChangeListener (PropertyChangeListener l)
	{
		getImpl().addPropertyChangeListener(l);
	}

	/** Remove a property change listener.
	 * @param l the listener to remove
	 * @see PersistenceElementProperties
	 */
	public final void removePropertyChangeListener (PropertyChangeListener l)
	{
		getImpl().removePropertyChangeListener(l);
	}

	/** Add a vetoable change listener.
	 * @param l the listener to add
	 * @see PersistenceElementProperties
	 */
	public final void addVetoableChangeListener (VetoableChangeListener l)
	{
		getImpl().addVetoableChangeListener(l);
	}

	/** Remove a vetoable change listener.
	 * @param l the listener to remove
	 * @see PersistenceElementProperties
	 */
	public final void removeVetoableChangeListener (VetoableChangeListener l)
	{
		getImpl().removeVetoableChangeListener(l);
	}

	/** Get the name of this persistence element.
	 * @return the name
	 */
	public String getName() { return getImpl().getName(); }

	/** Set the name of this persistence element.
	 * @param name the name
	 * @exception ModelException if impossible
	 */
	public void setName (String name) throws ModelException
	{
		getImpl().setName(name);
	}
	
	/** Overrides Object's <code>toString</code> method to return the name 
	 * of this persistence element.
	 * @return a string representation of the object
	 */
	public String toString () { return getName(); }

    /** Overrides Object's <code>equals</code> method by comparing the name of this persistence element
     * with the name of the argument obj. The method returns <code>false</code> if obj does not have 
     * the same dynamic type as this persistence element.
	 * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
     * @param obj the reference object with which to compare.
     */
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (obj == this)
            return true;

        // check for the right class and then do the name check by calling compareTo.
        return (getClass() == obj.getClass()) && (compareTo(obj) == 0);
    }
    
    /** Overrides Object's <code>hashCode</code> method to return the hashCode of this persistence element's name.
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        return (getName()==null) ? 0 : getName().hashCode();
    }

	//================= implementation of Comparable ================
    
	/** Compares this object with the specified object for order. Returns a negative integer, zero, 
     * or a positive integer as this object is less than, equal to, or greater than the specified object.
     * The specified object must be persistence element, meaning it must be an instance of class 
     * PersistenceElement or any subclass. If not a ClassCastException is thrown.
     * The order of PersistenceElement objects is defined by the order of their names.
     * Persistence elements without name are considered to be less than any named persistence element.
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, 
     * or greater than the specified object.
     * @exception ClassCastException - if the specified object is null or is not an instance of PersistenceElement
     */
    public int compareTo(Object o)
    {
        // null is not allowed
        if (o == null)
            throw new ClassCastException();
        if (o == this)
            return 0;

        String thisName = getName();
        // the following statement throws a ClassCastException if o is not a PersistenceElement
        String otherName = ((PersistenceElement)o).getName();
        // if this does not have a name it should compare less than any named object
        if (thisName == null)
            return (otherName == null) ? 0 : -1;
        // if this is named and o does not have a name it should compare greater
        if (otherName == null)
            return 1;
        // now we know that this and o are named persistence elements => 
        // use locale-sensitive String comparison 
        int ret = Collator.getInstance().compare(thisName, otherName);
        // if both names are equal, both objects might have different types. 
        // If so order both objects by their type names (necessary to be consistent with equals)
        if ((ret == 0) && (getClass() != o.getClass()))
            ret = getClass().getName().compareTo(o.getClass().getName());
        return ret;
    }
    
	//=============== extra set methods needed for xml archiver ==============

	/** Set the implementation factory of this persistence element.  
	 * This method should only be used internally and for cloning 
	 * and archiving.
	 * @param impl the implementation to use
	 */
	public void setImpl (PersistenceElement.Impl impl)
	{
		_impl = impl;

		if (_impl != null)
			getImpl().attachToElement(this);
	}

	/** Pluggable implementation of the storage of element properties.
	 * @see PersistenceElement#PersistenceElement
	 */
	public interface Impl
	{
		/** Add some items. */
		public static final int ADD = 1;
		/** Remove some items. */
		public static final int REMOVE = -1;
		/** Set some items, replacing the old ones. */
		public static final int SET = 0;
		
		/** Called to attach the implementation to a specific
		 * element. Will be called in the element's constructor.
		 * Allows implementors of this interface to store a reference to the
		 * holder class, useful for implementing the property change listeners.
		 *
		 * @param element the element to attach to
		 */
		public void attachToElement (PersistenceElement element);

		/** Add a property change listener.
		 * @param l the listener to add
		 */
		public void addPropertyChangeListener (PropertyChangeListener l);

		/** Remove a property change listener.
		 * @param l the listener to remove
		 */
		public void removePropertyChangeListener (PropertyChangeListener l);

		/** Add a vetoable change listener.
		 * @param l the listener to add
		 */
		public void addVetoableChangeListener (VetoableChangeListener l);

		/** Remove a vetoable change listener.
		 * @param l the listener to remove
		 */
		public void removeVetoableChangeListener (VetoableChangeListener l);

		/** Get the name of this persistence element.
		 * @return the name
		 */
		public String getName ();

		/** Set the name of this persistence element.
		 * @param name the name
		 * @exception ModelException if impossible
		 */
		public void setName (String name) throws ModelException;
	}
}
