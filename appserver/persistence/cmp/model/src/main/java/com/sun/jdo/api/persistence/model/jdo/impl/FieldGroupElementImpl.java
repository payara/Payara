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
 * FieldGroupElementImpl.java
 *
 * Created on March 2, 2000, 6:28 PM
 */
 
package com.sun.jdo.api.persistence.model.jdo.impl;

import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.jdo.FieldGroupElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceFieldElement;

/** 
 *
 * @author  raccah
 * @version %I%
 */
public class FieldGroupElementImpl extends PersistenceMemberElementImpl 
	implements FieldGroupElement.Impl
{
	/** Fields of the field group element. */
	private PersistenceElementCollection _fields;

	/** Create new FieldGroupElementImpl with no corresponding name.  This 
	 * constructor should only be used for cloning and archiving.
	 */
	public FieldGroupElementImpl ()
	{
		this(null);
	}

	/** Creates new FieldGroupElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public FieldGroupElementImpl (String name)
	{
		super(name);
		_fields = new PersistenceElementCollection(this, PROP_FIELDS, 
			new PersistenceFieldElement[0]);
	}

	/** Find a field by name.
	 * @param name the name to match
	 * @return the field, or <code>null</code> if it does not exist
	 */
	public PersistenceFieldElement getField (String name)
	{
		return (PersistenceFieldElement)_fields.getElement(name);
	}

	/** Get all fields.
	 * @return the fields
	 */
	public PersistenceFieldElement[] getFields ()
	{
		return (PersistenceFieldElement[])_fields.getElements();
	}


	/** Change the set of fields.
	 * @param fields the new fields
	 * @param action {@link #ADD}, {@link #REMOVE}, or {@link #SET}
	 * @exception ModelException if impossible
	 */
	public void changeFields (PersistenceFieldElement[] fields, int action)
		throws ModelException
	{
		_fields.changeElements(fields, action);
	}

	//=============== extra methods needed for xml archiver ==============

	/** Returns the field collection of this field group element.  This 
	 * method should only be used internally and for cloning and archiving.
	 * @return the field collection of this field group element
	 */
	public PersistenceElementCollection getCollection () { return _fields; }

	/** Set the field collection of this field group element to the supplied 
	 * collection.  This method should only be used internally and for 
	 * cloning and archiving.
	 * @param collection the field collection of this field group element
	 */
	public void setCollection (PersistenceElementCollection collection)
	{
		_fields = collection;
	}
}
