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
 * PersistenceElementCollection.java
 *
 * Created on March 6, 2000, 2:20 PM
 */

package com.sun.jdo.api.persistence.model.jdo.impl;

import java.util.*;
import java.beans.PropertyVetoException;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.jdo.*;

/** 
 *
 * @author raccah
 * @version %I%
 */
public class PersistenceElementCollection
{
	/** Owner of the collection. */
	private PersistenceElementImpl _owner;

	/** Elements of the collection. */
	private PersistenceElement[] _elements;

	/** Array template for typed returns */
	private Object[] _template;

	/** Property name. */
	private String _propertyName;

	/** Create new PersistenceElementCollection with no owner, property, or
	 * template.  This constructor should only be used for cloning and 
	 * archiving.
	 */
	public PersistenceElementCollection ()
	{
		this(null, null, null);
	}

	/** Creates new PersistenceElementCollection */
	public PersistenceElementCollection (PersistenceElementImpl owner,
		String propertyName, Object[] template)
	{
		_owner = owner;
		_propertyName = propertyName;
		_template = template;
	}

	/** Change the set of elements.
	 * @param elements the new elements
	 * @param action {@link com.sun.jdo.api.persistence.model.jdo.PersistenceElement.Impl#ADD},
	 * {@link com.sun.jdo.api.persistence.model.jdo.PersistenceElement.Impl#REMOVE}, or
	 * {@link com.sun.jdo.api.persistence.model.jdo.PersistenceElement.Impl#SET}
	 * @exception ModelException if impossible
	 */
	public void changeElements (PersistenceElement[] elements, int action)
		throws ModelException
	{
		changeElements(Arrays.asList(elements), action);
	}

	/** Change the set of elements.
	 * @param elements the new elements
	 * @param action {@link com.sun.jdo.api.persistence.model.jdo.PersistenceElement.Impl#ADD},
	 * {@link com.sun.jdo.api.persistence.model.jdo.PersistenceElement.Impl#REMOVE}, or
	 * {@link com.sun.jdo.api.persistence.model.jdo.PersistenceElement.Impl#SET}
	 * @exception ModelException if impossible
	 */
	public void changeElements (List elements, int action)
		throws ModelException
	{
		boolean changed = false;

		try
		{
			PersistenceElement[] oldElements = getElements();
			int oldLength = (oldElements == null) ? 0 : oldElements.length;
			int newLength = (elements == null) ? 0 : elements.size();
			List list = null;

			switch (action)
			{
				case PersistenceElement.Impl.SET:
					list = elements;
					changed = true;
					break;
				case PersistenceElement.Impl.ADD:
					if (newLength > 0)
					{
						list = ((oldLength == 0) ? new ArrayList() : 
							new ArrayList(Arrays.asList(oldElements)));
						list.addAll(elements);
						changed = true;
					}
					break;
				case PersistenceElement.Impl.REMOVE:
					if ((newLength > 0) && (oldLength > 0))
					{
						list = new ArrayList(Arrays.asList(oldElements));
						list.removeAll(elements);
						changed = true;
					}
					break;
			}
			if (changed)
			{
				try
				{
					_owner.fireVetoableChange(_propertyName, null, null);
					_elements = (PersistenceElement[])list.toArray(_template);
				}
				catch (PropertyVetoException e)
				{
					throw new ModelVetoException(e);
				}
			}
		}
		finally
		{
			if (changed)
				_owner.firePropertyChange(_propertyName, null, null);
		}
	}

	/** Returns the collection of elements maintained by this holder in the form
	 * of an array.
	 * @return the elements maintained by this collection
	 */
	public PersistenceElement[] getElements () { return _elements; }

	/** Returns the element with the supplied name from the collection of 
	 * elements maintained by this collection.
	 * @param name the name to match
	 * @return the element with the supplied name, <code>null</code> if none 
	 * exists
	 */
	public PersistenceElement getElement (String name)
	{
		PersistenceElement[] elements = getElements();
		int i, count = ((elements != null) ? elements.length : 0);
		
		for (i = 0; i < count; i++)
		{
			PersistenceElement element = elements[i];

			if (name.equals(element.getName()))
				return element;
		}

		return null;
	}

	//=============== extra methods needed for xml archiver ==============

	/** Returns the owner of this collection.  This method should only 
	 * be used internally and for cloning and archiving.
	 * @return the owner of this collection
	 */
	public PersistenceElementImpl getOwner () { return _owner; }

	/** Set the owner of this collection to the supplied implementation.  
	 * This method should only be used internally and for cloning and 
	 * archiving.
	 * @param owner the owner of this collection
	 */
	public void setOwner (PersistenceElementImpl owner)
	{
		_owner = owner;
	}

	/** Returns the template for the array of this collection.  This method 
	 * should only be used internally and for cloning and archiving.
	 * @return the typed template of this collection
	 */
	public Object[] getTemplate () { return _template; }

	/** Set the template for the array of this collection to the supplied 
	 * array.  This template is used so the array returned by getElements is
	 * properly typed.  This method should only be used internally and 
	 * for cloning and archiving.
	 * @param template the typed template of this collection
	 */
	public void setTemplate (Object[] template) { _template = template; }

	/** Returns the property name of this collection.  This method 
	 * should only be used internally and for cloning and archiving.
	 * @return the property name for this collection
	 */
	public String getPropertyName () { return _propertyName; }

	/** Set the property name of this collection to the supplied name.  
	 * This name is used to generate the correct property change event on 
	 * changes to the collection.  This method should only be used 
	 * internally and for cloning and archiving.
	 * @param propertyName the property name for this collection
	 */
	public void setPropertyName (String propertyName)
	{
		_propertyName = propertyName;
	}

	/** Set the collection of elements maintained by this holder to the 
	 * supplied array.  This method should only be used internally and for 
	 * cloning and archiving.
	 * @param elements the collection of elements maintained by this holder
	 */
	public void setElements (PersistenceElement[] elements)
	{
		_elements = elements;
	}
}
