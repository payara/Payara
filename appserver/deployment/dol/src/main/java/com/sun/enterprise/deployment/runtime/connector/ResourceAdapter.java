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
 *	This generated bean class ResourceAdapter matches the DTD element resource-adapter
 *
 *	Generated on Mon May 13 13:36:49 PDT 2002
 */

package com.sun.enterprise.deployment.runtime.connector;

import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

/**
 * This class was based on the schema2beans generated one modified
 * to remove its dependencies on schema2beans libraries.
 
 * @author  Jerome Dochez
 * @version 
 */
public class ResourceAdapter extends RuntimeDescriptor
{
    static public final String DESCRIPTION = "Description";	// NOI18N
    static public final String PROPERTY = "PropertyElement";	// NOI18N
    
    static public final String JNDI_NAME = "JndiName";
    static public final String MAX_POOL_SIZE = "MaxPoolSize";
    static public final String STEADY_POOL_SIZE = "SteadyPoolSize";
    static public final String MAX_WAIT_TIME_IN_MILLIS = "MaxWaitTimeInMillis";
    static public final String IDLE_TIMEOUT_IN_SECONDS = "IdleTimeoutInSeconds";
    
    // This attribute is an array, possibly empty
    public void setPropertyElement(int index, NameValuePairDescriptor value)
    {
	this.setValue(PROPERTY, index, value);
    }
    
    //
    public boolean isPropertyElement(int index)
    {
	NameValuePairDescriptor ret = (NameValuePairDescriptor)this.getValue(PROPERTY, index);
	return ret != null;
    }
    
    // This attribute is an array, possibly empty
    public void setPropertyElement(NameValuePairDescriptor[] values)
    {
	this.setValues(PROPERTY, values);
    }
    
    //
    public NameValuePairDescriptor[] getPropertyElement()
    {
	return (NameValuePairDescriptor[])this.getValues(PROPERTY);
    }
    
    // Return the number of properties
    public int sizePropertyElement()
    {
	return this.size(PROPERTY);
    }
    
    // Add a new element returning its index in the list
    public int addPropertyElement(NameValuePairDescriptor value)
    {
	return this.addValue(PROPERTY, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removePropertyElement(NameValuePairDescriptor value)
    {
	return this.removeValue(PROPERTY, value);
    }
    
    //
    // Remove an element using its index
    //
    public void removePropertyElement(int index)
    {
	this.removeValue(PROPERTY, index);
    }
    
    // This method verifies that the mandatory properties are set
    public boolean verify()
    {
	return true;
    }
}
