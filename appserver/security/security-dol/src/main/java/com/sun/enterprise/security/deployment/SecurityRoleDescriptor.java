/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

 package com.sun.enterprise.security.deployment;

import com.sun.enterprise.deployment.web.SecurityRole;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.security.common.Role;

/**
    * I am an abstract role..
    *
    *@author Danny Coward
    */
public class SecurityRoleDescriptor extends Descriptor implements SecurityRole {
    
    /**
    * Construct a SecurityRoleDescriptor from the given role name and description.
    */
    public SecurityRoleDescriptor(String name, String description) {
	super(name, description);
    }
    
    /**
    * Construct a SecurityRoleDescriptor from the given role object.
    */
    
    public SecurityRoleDescriptor(Role role) {
	super(role.getName(), role.getDescription());
    }
    
    /**
    * Default constructor.
    */
    public SecurityRoleDescriptor() {
    }
    
    /**
    * Equality on rolename.
    */
    
    public boolean equals(Object other) {
	if (other instanceof SecurityRoleDescriptor &&
	    this.getName().equals( ((SecurityRoleDescriptor) other).getName() )) {
		return true;
	}
	return false;
    }
    
    /**
    * My hashcode.
    */
    
    public int hashCode() {
	return this.getName().hashCode();
    }
    
    /**
    * Formatted string representing my state.
    */    
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("SecurityRole ");
	super.print(toStringBuffer);
    }

}
