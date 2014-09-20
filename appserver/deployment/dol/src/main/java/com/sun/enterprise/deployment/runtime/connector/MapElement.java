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

/**
*	This generated bean class MapElement matches the DTD element map-element
*
*	Generated on Mon May 13 13:36:49 PDT 2002
*/

package com.sun.enterprise.deployment.runtime.connector;

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

/**
* This class was based on the schema2beans generated one modified
* to remove its dependencies on schema2beans libraries.

* @author  Jerome Dochez
* @version 
*/

public class MapElement extends RuntimeDescriptor
{
    
    static public final String PRINCIPAL = "Principal";	// NOI18N
    static public final String BACKEND_PRINCIPAL = "BackendPrincipal";	// NOI18N
    
    Principal backendPrincipal = null;
    
    // copy constructor
    public MapElement(MapElement other)
    {
	super(other);
    }

    // constructor
    public MapElement()
    {
	super();
    }

    // This attribute is an array containing at least one element
    public void setPrincipal(int index, Principal value)
    {
	this.setValue(PRINCIPAL, index, value);
    }
    
    //
    public Principal getPrincipal(int index)
    {
	return (Principal)this.getValue(PRINCIPAL, index);
    }
    
    // This attribute is an array containing at least one element
    public void setPrincipal(Principal[] value)
    {
	this.setValue(PRINCIPAL, value);
    }
    
    //
    public Principal[] getPrincipal()
    {
	return (Principal[])this.getValues(PRINCIPAL);
    }
    
    // Return the number of properties
    public int sizePrincipal()
    {
	return this.size(PRINCIPAL);
    }
    
    // Add a new element returning its index in the list
    public int addPrincipal(Principal value)
    {
	return this.addValue(PRINCIPAL, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removePrincipal(Principal value)
    {
	return this.removeValue(PRINCIPAL, value);
    }
    
    // This attribute is mandatory
    public void setBackendPrincipal(boolean value)
    {
	this.setValue(BACKEND_PRINCIPAL, Boolean.valueOf(value));
    }
    
    //
    public boolean isBackendPrincipal()
    {
	Boolean ret = (Boolean)this.getValue(BACKEND_PRINCIPAL);
	if (ret == null)
	    return false;
	return ret.booleanValue();
    }
    
    // This method verifies that the mandatory properties are set
    public boolean verify()
    {
	return true;
    }
}
