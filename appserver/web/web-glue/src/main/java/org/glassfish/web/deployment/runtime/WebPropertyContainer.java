/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.runtime;

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

/**
* Interface for all web property containers
*
* @author Jerome Dochez
*/
public class WebPropertyContainer extends RuntimeDescriptor {
    
    static public final String NAME = "Name";	// NOI18N
    static public final String VALUE = "Value";	// NOI18N
    static public final String PROPERTY = "WebProperty";	// NOI18N

    // copy constructor
    public WebPropertyContainer(WebPropertyContainer other) 
    {
	super(other);
    }

    // constructor
    public WebPropertyContainer() 
    {
	super();
    }

    // This attribute is an array, possibly empty
    public void setWebProperty(int index, WebProperty value)
    {
	this.setValue(PROPERTY, index, value);
    }
    
    //
    public WebProperty getWebProperty(int index)
    {
	return (WebProperty)this.getValue(PROPERTY, index);
    }
    
    // This attribute is an array, possibly empty
    public void setWebProperty(WebProperty[] value)
    {
	this.setValue(PROPERTY, value);
    }
    
    //
    public WebProperty[] getWebProperty()
    {
	WebProperty[] props = (WebProperty[])this.getValues(PROPERTY);
        if (props==null) {
            return new WebProperty[0];
        } else {
            return props;
        }
    }
    
    // Return the number of properties
    public int sizeWebProperty()
    {
	return this.size(PROPERTY);
    }
    
    // Add a new element returning its index in the list
    public int addWebProperty(WebProperty value)
    {
	return this.addValue(PROPERTY, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeWebProperty(WebProperty value)
    {
	return this.removeValue(PROPERTY, value);
    }
    
    // This method verifies that the mandatory properties are set
    public boolean verify()
    {
	return true;
    }    
}
