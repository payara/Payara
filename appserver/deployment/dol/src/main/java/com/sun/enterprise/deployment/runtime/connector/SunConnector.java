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
 *	This generated bean class SunConnector matches the DTD element sun-connector
 *
 *	Generated on Mon May 13 13:36:49 PDT 2002
 *
 *	This class matches the root element of the DTD,
 *	and is the root of the following bean graph:
 *
 *	  ResourceAdapter
 *	    [attr: JndiName CDATA #REQUIRED ]
 *	    [attr: MaxPoolSize CDATA 32]
 *	    [attr: SteadyPoolSize CDATA 4]
 *	    [attr: MaxWaitTimeInMillis CDATA 10000]
 *	    [attr: IdleTimeoutInSeconds CDATA 1000]
 *	         Description? - String
 *	         PropertyElement[0,n] - Boolean
 *	           [attr: Name CDATA #REQUIRED ]
 *	           [attr: Value CDATA #REQUIRED ]
 *	  RoleMap?
 *	    [attr: MapId CDATA #REQUIRED ]
 *	         Description? - String
 *	         MapElement[0,n]
 *	                Principal[1,n]
 *	                  [attr: UserName CDATA #REQUIRED ]
 *	                       Description? - String
 *	                BackendPrincipal - Boolean
 *	                  [attr: UserName CDATA #REQUIRED ]
 *	                  [attr: Password CDATA #REQUIRED ]
 *	                  [attr: Credential CDATA #REQUIRED ]
 *
 */

package com.sun.enterprise.deployment.runtime.connector;

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

/**
 * This class was based on the schema2beans generated one modified
 * to remove its dependencies on schema2beans libraries.
 
 * @author  Jerome Dochez
 * @version 
 */
public class SunConnector extends RuntimeDescriptor
{
    
    static public final String RESOURCE_ADAPTER = "ResourceAdapter";	// NOI18N
    static public final String ROLE_MAP = "RoleMap";	// NOI18N

    
    // This attribute is mandatory
    public void setResourceAdapter(ResourceAdapter value)
    {
	this.setValue(RESOURCE_ADAPTER, value);
    }
    
    //
    public ResourceAdapter getResourceAdapter()
    {
	return (ResourceAdapter)this.getValue(RESOURCE_ADAPTER);
    }
    
    // This attribute is optional
    public void setRoleMap(RoleMap value)
    {
	this.setValue(ROLE_MAP, value);
    }
    
    //
    public RoleMap getRoleMap()
    {
	return (RoleMap)this.getValue(ROLE_MAP);
    }
    
    // This method verifies that the mandatory properties are set
    public boolean verify()
    {
	return true;
    }
}
