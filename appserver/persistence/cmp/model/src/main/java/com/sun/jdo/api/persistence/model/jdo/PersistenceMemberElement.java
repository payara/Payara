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
 * PersistenceMemberElement.java
 *
 * Created on February 29, 2000, 12:50 PM
 */

package com.sun.jdo.api.persistence.model.jdo;

/** 
 *
 * @author raccah
 * @version %I%
 */
public abstract class PersistenceMemberElement extends PersistenceElement
{
	/** the class to which this element belongs */
	private PersistenceClassElement _declaringClass;

	/** Create new PersistenceMemberElement with no implementation. 
	 * This constructor should only be used for cloning and archiving.
	 */
	public PersistenceMemberElement ()
	{
		this(null, null);
	}

	/** Create new PersistenceMemberElement with the provided implementation. The 
	 * implementation is responsible for storing all properties of the object.
	 * @param impl the implementation to use
	 * @param declaringClass the class to attach to
	 */
	protected PersistenceMemberElement (PersistenceMemberElement.Impl impl, 
		PersistenceClassElement declaringClass)
	{
		super(impl);
		_declaringClass = declaringClass;
	}

	/** @return the current implementation.
	 */
	final Impl getMemberImpl () { return (Impl)getImpl(); }

	/** Get the declaring class.
	 * @return the class that owns this member element, or <code>null</code>
	 * if the element is not attached to any class
	 */
	public PersistenceClassElement getDeclaringClass ()
	{
		return _declaringClass;
	}

	//=============== extra set methods needed for xml archiver ==============

	/** Set the declaring class of this member element.  This method should 
	 * only be used internally and for cloning and archiving.
	 * @param declaringClass the declaring class of this member element
	 */
	public void setDeclaringClass (PersistenceClassElement declaringClass)
	{
		_declaringClass = declaringClass;
	}

	/** Overrides PersistenceElement's <code>equals</code> method to add  
	 * comparison of the name of the declaring class this persistence element.
	 * The method returns <code>false</code> if obj does not have a declaring 
	 * class with the same name as this persistence element.
	 * @return <code>true</code> if this object is the same as the obj argument;
	 * <code>false</code> otherwise.
	 * @param obj the reference object with which to compare.
	 */
	public boolean equals (Object obj)
	{
		if (super.equals(obj) && (obj instanceof PersistenceMemberElement))
		{
			PersistenceClassElement declaringClass = getDeclaringClass();
			PersistenceClassElement objDeclaringClass = 
				((PersistenceMemberElement)obj).getDeclaringClass();

			return ((declaringClass == null) ? (objDeclaringClass == null) :
				declaringClass.equals(objDeclaringClass));
		}

		return false;
	}

	/** Overrides PersistenceElement's <code>hashCode</code> method to add 
	 * the hashCode of this persistence element's declaring class.
	 * @return a hash code value for this object.
	 */
	public int hashCode ()
	{
		PersistenceClassElement declaringClass = getDeclaringClass();

		return (super.hashCode() + 
			((declaringClass == null) ? 0 : declaringClass.hashCode()));
	}

	/** Pluggable implementation of member elements.
	 * @see PersistenceMemberElement
	 */
	public interface Impl extends PersistenceElement.Impl { }
}

