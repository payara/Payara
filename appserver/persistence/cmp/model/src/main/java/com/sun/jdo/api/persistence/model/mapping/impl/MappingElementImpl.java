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
 * MappingElementImpl.java
 *
 * Created on March 24, 2000, 10:06 AM
 */

package com.sun.jdo.api.persistence.model.mapping.impl;

import java.beans.*;
import java.util.ResourceBundle;
import java.text.Collator;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.mapping.MappingElement;
import org.glassfish.persistence.common.I18NHelper;

/** 
 *
 * @author raccah
 * @version %I%
 */
public abstract class MappingElementImpl implements MappingElement
{
	/** I18N message handler */
	private static final ResourceBundle _messages = I18NHelper.loadBundle(
		"com.sun.jdo.api.persistence.model.Bundle",		// NOI18N
		MappingElementImpl.class.getClassLoader());

	/** Property change support */
	private PropertyChangeSupport _support;

	/** Vetoable change support */
	private transient VetoableChangeSupport _vetoableSupport;

	String _name;

	/** Create new MappingElementImpl with no corresponding name.  This 
	 * constructor should only be used for cloning and archiving.
	 */
	public MappingElementImpl ()
	{
		this(null);
	}

	/** Creates new MappingElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public MappingElementImpl (String name)
	{
		super();
		_name = name;
	}

	/** @return I18N message handler for this element
	 */
	protected static final ResourceBundle getMessages () { return _messages; }

	/** Overrides Object's <code>toString</code> method to return the name 
	 * of this mapping element.
	 * @return a string representation of the object
	 */
	public String toString () { return getName(); }

    /** Overrides Object's <code>equals</code> method by comparing the name of this mapping element
     * with the name of the argument obj. The method returns <code>false</code> if obj does not have
     * the same dynamic type as this mapping element.
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
    
    /** Overrides Object's <code>hashCode</code> method to return the hashCode of this mapping element's name.
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        return (getName()==null) ? 0 : getName().hashCode();
    }

	/** Fires property change event.
	 * @param name property name
	 * @param o old value
	 * @param n new value
	 */
	protected void firePropertyChange (String name, Object o, Object n)
	{
		if (_support != null)
			_support.firePropertyChange(name, o, n);
	}

	/** Fires vetoable change event.
	 * @param name property name
	 * @param o old value
	 * @param n new value
	 * @exception PropertyVetoException when the change is vetoed by a listener
	 */
	protected void fireVetoableChange (String name, Object o, Object n)
		throws PropertyVetoException
	{
		if (_vetoableSupport != null)
			_vetoableSupport.fireVetoableChange(name, o, n);
	}

	//================= implementation of MappingElement ================

	/** Add a property change listener.
	 * @param l the listener to add
	 */
	public synchronized void addPropertyChangeListener 
		(PropertyChangeListener l)
	{
		if (_support == null)
		{
			synchronized(this)
			{
				// new test under synchronized block
				if (_support == null)
					_support = new PropertyChangeSupport(this);
			}
		}

		_support.addPropertyChangeListener(l);
	}

	/** Remove a property change listener.
	 * @param l the listener to remove
	 */
	public void removePropertyChangeListener (PropertyChangeListener l)
	{
		if (_support != null)
			_support.removePropertyChangeListener(l);
	}

	/** Add a vetoable change listener.
	 * @param l the listener to add
	 */
	public synchronized void addVetoableChangeListener 
		(VetoableChangeListener l)
	{
		if (_vetoableSupport == null)
			_vetoableSupport = new VetoableChangeSupport(this);

		_vetoableSupport.addVetoableChangeListener(l);
	}

	/** Remove a vetoable change listener.
	 * @param l the listener to remove
	 */
	public synchronized void removeVetoableChangeListener (
		VetoableChangeListener l)
	{
		if (_vetoableSupport != null)
			_vetoableSupport.removeVetoableChangeListener(l);
	}

	/** Get the name of this mapping element.
	 * @return the name
	 */
	public String getName () { return _name; }

	/** Set the name of this mapping element.
	 * @param name the name
	 * @exception ModelException if impossible
	 */
	public void setName (String name) throws ModelException
	{
		String old = getName();
		
		try
		{
			fireVetoableChange(PROP_NAME, old, name);
			_name = name;
			firePropertyChange(PROP_NAME, old, name);
		}
		catch (PropertyVetoException e)
		{
			throw new ModelVetoException(e);
		}
	}

	//================= implementation of Comparable ================
    
	/** Compares this object with the specified object for order. Returns a negative integer, zero, 
     * or a positive integer as this object is less than, equal to, or greater than the specified object.
     * The specified object must be mapping element, meaning it must be an instance of class 
     * MappingElementImpl or any subclass. If not a ClassCastException is thrown.
     * The order of MappingElementImpl objects is defined by the order of their names.
     * Mapping elements without name are considered to be less than any named mapping element.
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, 
     * or greater than the specified object.
     * @exception ClassCastException - if the specified object is null or is not an instance of MappingElementImpl
     */
    public int compareTo(Object o)
    {
        // null is not allowed
        if (o == null)
            throw new ClassCastException();
        if (o == this)
            return 0;

        String thisName = getName();
        // the following statement throws a ClassCastException if o is not a MappingElementImpl
        String otherName = ((MappingElementImpl)o).getName();
        // if this does not have a name it should compare less than any named object
        if (thisName == null)
            return (otherName == null) ? 0 : -1;
        // if this is named and o does not have a name it should compare greater
        if (otherName == null)
            return 1;
        // now we know that this and o are named mapping elements => 
        // use locale-sensitive String comparison 
        int ret = Collator.getInstance().compare(thisName, otherName);
        // if both names are equal, both objects might have different types. 
        // If so order both objects by their type names (necessary to be consistent with equals)
        if ((ret == 0) && (getClass() != o.getClass()))
            ret = getClass().getName().compareTo(o.getClass().getName());
        return ret;
    }
    
}

