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
 * PersistenceMemberElementImpl.java
 *
 * Created on March 2, 2000, 5:17 PM
 */

package com.sun.jdo.api.persistence.model.jdo.impl;

import java.beans.PropertyVetoException;

import com.sun.jdo.api.persistence.model.jdo.PersistenceMemberElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceClassElement;

/** 
 *
 * @author raccah
 * @version %I%
 */
public abstract class PersistenceMemberElementImpl 
	extends PersistenceElementImpl implements PersistenceMemberElement.Impl
{
	/** Create new PersistenceMemberElementImpl with no corresponding name.   
	 * This constructor should only be used for cloning and archiving.
	 */
	public PersistenceMemberElementImpl ()
	{
		this(null);
	}

	/** Creates new PersistenceMemberElementImpl with the corresponding name 
	 * @param name the name of the element
	 */
	public PersistenceMemberElementImpl (String name)
	{
		super(name);
	}

	/** Fires property change event.  This method overrides that of 
	 * PersistenceElementImpl to update the PersistenceClassElementImpl's  
	 * modified status.
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
		PersistenceClassElement classElement = 
			((PersistenceMemberElement)_element).getDeclaringClass();

		super.firePropertyChange(name, o, n);

		if ((classElement != null) && !noChange)
			classElement.setModified(true);
	}

	/** Fires vetoable change event.  This method overrides that of 
	 * PersistenceElementImpl to give listeners a chance to block 
	 * changes on the persistence class element modified status.
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
		PersistenceClassElement classElement = 
			((PersistenceMemberElement)_element).getDeclaringClass();

		super.fireVetoableChange(name, o, n);

		if ((classElement != null) && !noChange)
		{
			((PersistenceElementImpl)classElement.getImpl()).
				fireVetoableChange(PROP_MODIFIED, Boolean.FALSE, Boolean.TRUE);
		}
	}
}
