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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.web.SecurityRole;
import com.sun.enterprise.deployment.web.SecurityRoleReference;
import org.glassfish.security.common.Role;

/** 
 * Special kind of environment property that encapsulates the primitive roles 
 * as defined by the bean developer. The name of a primitive role will appear 
 * in the bean code, the value will be mapped to the name of a Role chosen by 
 * the application assembler which is referenced by the EjbBundle being 
 * assembled.
 * @author Danny Coward
 */

public class RoleReference extends EnvironmentProperty implements 
		SecurityRoleReference 
{
    /** 
     * Default constructor.
     */
    public RoleReference() {
    }

    /** 
     * Construct a role reference from the given name and description.
     */
    public RoleReference(String name, String description) {
	super(name, "", description);
    }
    
    /** 
     * Construct the role reference with the same name and rolename the same 
     * as the environment property value.
     * @param the environment property instance.
     */
    public RoleReference(EnvironmentProperty environmentProperty) {
	super(environmentProperty.getName(), 
		environmentProperty.getDescription(), "");
	this.setValue(environmentProperty.getValue());
    }
    
    /**
     * Set the value for the reference.
     * @param the role
     */
    void setRole(Role role) {
	super.setValue(role.getName());
    }
    
    /** 
     * Return the role object from this descriptor.
     * @return the role.
     */
    public Role getRole() {
	return new Role(super.getValue());
    }
    
    /** 
     * Return the rolename.
     * @return the role name.
     */
    public SecurityRole getSecurityRoleLink() {
	return new SecurityRoleDescriptor(super.getValue(), "");
    }
    
    /** 
     * Sets the rolename.
     * @param the rolename.
     */
    public void setSecurityRoleLink(SecurityRole securityRole) {
	super.setValue(securityRole.getName());
    }
    
    /** 
     * Return the coded name. 
     * @return the role name used in the bean code.
     */
    public String getRoleName() {
	return this.getName();
    }
    
    /** 
     * Sets the coded name.
     * @param the role name used in the bean code.
     */
    public void setRoleName(String rolename) {
	this.setName(rolename);
    }
    
    /**
     * Returns a formatted version of this object as a String.
     */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("Role-Ref-Env-Prop: ").append(super.getName()).append("@").append( 
	    this.getRole()).append("@").append(super.getDescription());
    }

}

