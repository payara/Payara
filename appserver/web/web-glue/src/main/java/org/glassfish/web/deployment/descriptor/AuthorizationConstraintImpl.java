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

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.SecurityRoleDescriptor;
import com.sun.enterprise.deployment.web.AuthorizationConstraint;
import com.sun.enterprise.deployment.web.SecurityRole;
import org.glassfish.deployment.common.Descriptor;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * This descriptor represents an authorization contraint on a security 
 * constraint in a web application.
 *
 * @author Danny Coward
 */

public class AuthorizationConstraintImpl extends Descriptor implements
		AuthorizationConstraint {
    private Set<SecurityRole> securityRoles;
    
    /**
     * Default constructor that creates an AuthorizationConstraint 
     * with no roles.
     */
    public AuthorizationConstraintImpl() {
    }
    
    /**
     * Copy constructor.
     */
    public AuthorizationConstraintImpl(AuthorizationConstraintImpl other) {
	this.securityRoles = new HashSet<SecurityRole>(other.getSecurityRoleSet());
    }
    
    /**
     * Return the set of roles.
     */
    private Set<SecurityRole> getSecurityRoleSet() {
	if (this.securityRoles == null) {
	    this.securityRoles = new HashSet<SecurityRole>();
	}
	return this.securityRoles;
    }

    /** 
     * Return the security roles involved in this constraint. The 
     * enumeration is empty if there are none.
     * @return the enumeration of security roles in this constraint.
     */
    public Enumeration getSecurityRoles() {
	if (this.securityRoles == null) {
	    this.securityRoles = new HashSet<SecurityRole>();
	}
	return (new Vector<SecurityRole>(this.getSecurityRoleSet())).elements();
    }
    
    /**
     * Adds a role to the authorization constraint.
     * @param the role to be added.
     */
    public void addSecurityRole(SecurityRole securityRole) {
	this.getSecurityRoleSet().add(securityRole);
    }
    
    /**
     * Adds a role to the authorization constraint
     * @param the role name to be added
     */ 
    public void addSecurityRole(String roleName) {
        SecurityRoleDescriptor sr = new SecurityRoleDescriptor();
        sr.setName(roleName);
        addSecurityRole(sr);
    }
    
    /**
     * Removes the given role from the autrhorization constraint.
     * @param the role to be removed.
     */
    public void removeSecurityRole(SecurityRole securityRole) {
	this.getSecurityRoleSet().remove(securityRole);
    }

    /**
     * Prints a formatted representation of this object.
     */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("AuthorizationConstraint ");
	super.print(toStringBuffer);
	toStringBuffer.append(" securityRoles ").append(this.securityRoles);
    }
    
}
