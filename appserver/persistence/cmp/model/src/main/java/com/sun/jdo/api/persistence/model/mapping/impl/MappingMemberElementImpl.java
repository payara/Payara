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
 * MappingMemberElementImpl.java
 *
 * Created on May 23, 2000, 12:41 AM
 */

package com.sun.jdo.api.persistence.model.mapping.impl;

import java.beans.PropertyVetoException;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.ModelVetoException;
import com.sun.jdo.api.persistence.model.mapping.*;

/** 
 *
 * @author Mark Munro
 * @author Rochelle Raccah
 * @version %I%
 */
public abstract class MappingMemberElementImpl extends MappingElementImpl
	implements MappingMemberElement
{
	/** the class to which this element belongs */
	MappingClassElement _declaringClass;

	/** Create new MappingMemberElementImpl with no corresponding name or 
	 * declaring class.  This constructor should only be used for cloning and 
	 * archiving.
	 */
	public MappingMemberElementImpl () 
	{
		this(null, null);
	}

	/** Create new MappingMemberElementImpl with the corresponding name and 
	 * declaring class.
	 * @param name the name of the element
	 * @param declaringClass the class to attach to
	 */
	public MappingMemberElementImpl (String name, 
		MappingClassElement declaringClass) 
	{
		super(name);
		_declaringClass = declaringClass;
	}

	/** Get the declaring class.
	 * @return the class that owns this member element, or <code>null</code>
	 * if the element is not attached to any class
	 */
	public MappingClassElement getDeclaringClass () { return _declaringClass; }

	/** Overrides MappingElementImpl's <code>equals</code> method to add  
	 * comparison of the name of the declaring class this mapping element.
	 * The method returns <code>false</code> if obj does not have a declaring 
	 * class with the same name as this mapping element.
	 * @return <code>true</code> if this object is the same as the obj argument;
	 * <code>false</code> otherwise.
	 * @param obj the reference object with which to compare.
	 */
	public boolean equals (Object obj)
	{
		if (super.equals(obj) && (obj instanceof MappingMemberElement))
		{
			MappingClassElement declaringClass = getDeclaringClass();
			MappingClassElement objDeclaringClass = 
				((MappingMemberElement)obj).getDeclaringClass();

			return ((declaringClass == null) ? (objDeclaringClass == null) :
				declaringClass.equals(objDeclaringClass));
		}

		return false;
	}

	/** Overrides MappingElementImpl's <code>hashCode</code> method to add 
	 * the hashCode of this mapping element's declaring class.
	 * @return a hash code value for this object.
	 */
	public int hashCode ()
	{
		MappingClassElement declaringClass = getDeclaringClass();

		return (super.hashCode() + 
			((declaringClass == null) ? 0 : declaringClass.hashCode()));
	}

	/** Fires property change event.  This method overrides that of 
	 * MappingElementImpl to update the MappingClassElementImpl's modified 
	 * status.
	 * @param name property name
	 * @param o old value
	 * @param n new value
	 */
	protected final void firePropertyChange (String name, Object o, Object n)
	{
		// even though o == null and n == null will signify a change, that 
		// is consistent with PropertyChangeSupport's behavior and is 
		// necessary for this to work
		boolean noChange = ((o != null) && (n != null) && o.equals(n));
		MappingClassElement classElement = getDeclaringClass();

		super.firePropertyChange(name, o, n);

		if ((classElement != null) && !noChange)
			classElement.setModified(true);
	}

	/** Fires vetoable change event.  This method overrides that of 
	 * MappingElementImpl to give listeners a chance to block 
	 * changes on the mapping class element modified status.
	 * @param name property name
	 * @param o old value
	 * @param n new value
	 * @exception PropertyVetoException when the change is vetoed by a listener
	 */
	protected final void fireVetoableChange (String name, Object o, Object n)
		throws PropertyVetoException
	{
		// even though o == null and n == null will signify a change, that 
		// is consistent with PropertyChangeSupport's behavior and is 
		// necessary for this to work
		boolean noChange = ((o != null) && (n != null) && o.equals(n));
		MappingClassElement classElement = getDeclaringClass();

		super.fireVetoableChange(name, o, n);

		if ((classElement != null) && !noChange)
		{
			((MappingClassElementImpl)classElement).fireVetoableChange(
				PROP_MODIFIED, Boolean.FALSE, Boolean.TRUE);
		}
	}

	//=============== extra set methods needed for xml archiver ==============

	/** Set the declaring class of this mapping member.  This method should 
	 * only be used internally and for cloning and archiving.
	 * @param declaringClass the declaring class of this mapping member
	 */
	public void setDeclaringClass (MappingClassElement declaringClass)
	{
		_declaringClass = declaringClass;
	}
}
