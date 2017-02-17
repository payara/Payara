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
 * PersistenceElementImpl.java
 *
 * Created on March 1, 2000, 5:01 PM
 */

package com.sun.jdo.api.persistence.model.jdo.impl;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.jdo.PersistenceElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceElementProperties;

import java.beans.*;

/* TODO:
	1. way to get to declaring class from here?
 */

/** 
 *
 * @author raccah
 * @version %I%
 */
public abstract class PersistenceElementImpl extends Object 
	implements PersistenceElement.Impl, PersistenceElementProperties
{
	/** Element */
	PersistenceElement _element;

	/** Property change support */
	private PropertyChangeSupport _support;

	/** Vetoable change support */
	private transient VetoableChangeSupport _vetoableSupport;

	/** Name of the element. */
	private String _name;

	/** Create new PersistenceElementImpl with no corresponding name.  This 
	 * constructor should only be used for cloning and archiving.
	 */
	public PersistenceElementImpl ()
	{
		this(null);
	}

	/** Creates new PersistenceElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public PersistenceElementImpl (String name)
	{
		super();
		_name = name;
	}

	/** Called to attach the implementation to a specific
	 * element. Will be called in the element's constructor.
	 * Allows implementors of this interface to store a reference to the
	 * holder class, useful for implementing the property change listeners.
	 *
	 * @param element the element to attach to
	 */
	public void attachToElement (PersistenceElement element)
	{
		_element = element;
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

	/** Add a property change listener.
	 * @param l the listener to add
	 */
	public synchronized void addPropertyChangeListener 
		(PropertyChangeListener l)
	{
		// new test under synchronized block
		if (_support == null)
			_support = new PropertyChangeSupport(_element);


		_support.addPropertyChangeListener(l);
	}

	/** Remove a property change listener.
	 * @param l the listener to remove
	 */
	public synchronized void removePropertyChangeListener (
		PropertyChangeListener l)
	{
		if (_support != null)
			_support.removePropertyChangeListener(l);
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

	/** Add a vetoable change listener.
	 * @param l the listener to add
	 */
	public synchronized void addVetoableChangeListener 
		(VetoableChangeListener l)
	{
		if (_vetoableSupport == null)
			_vetoableSupport = new VetoableChangeSupport(_element);

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

	/** Get the name of this persistence element.
	 * @return the name
	 */
	public String getName () { return _name; }

	/** Set the name of this persistence element.
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
}
