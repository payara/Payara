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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]
package org.glassfish.web.deployment.descriptor;

import static java.util.Collections.enumeration;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.deployment.common.Descriptor;

import com.sun.enterprise.deployment.SecurityRoleDescriptor;
import com.sun.enterprise.deployment.web.AuthorizationConstraint;
import com.sun.enterprise.deployment.web.SecurityRole;

/**
 * This descriptor represents an authorization constraint on a security constraint in a web application.
 *
 * @author Danny Coward
 */
public class AuthorizationConstraintImpl extends Descriptor implements AuthorizationConstraint {

    private static final long serialVersionUID = -9221963350304406520L;

    private Set<SecurityRole> securityRoles;

    /**
     * Default constructor that creates an AuthorizationConstraint with no roles.
     */
    public AuthorizationConstraintImpl() {
    }

    /**
     * Copy constructor.
     */
    public AuthorizationConstraintImpl(AuthorizationConstraintImpl other) {
        securityRoles = new HashSet<SecurityRole>(other.getSecurityRoleSet());
    }

    /**
     * Return the set of roles.
     */
    private Set<SecurityRole> getSecurityRoleSet() {
        if (securityRoles == null) {
            securityRoles = new HashSet<>();
        }

        return securityRoles;
    }

    /**
     * Return the security roles involved in this constraint. The enumeration is empty if there are none.
     *
     * @return the enumeration of security roles in this constraint.
     */
    public Enumeration<SecurityRole> getSecurityRoles() {
        return enumeration(new ArrayList<>(getSecurityRoleSet()));
    }

    /**
     * Adds a role to the authorization constraint.
     *
     * @param the role to be added.
     */
    public void addSecurityRole(SecurityRole securityRole) {
        getSecurityRoleSet().add(securityRole);
    }

    /**
     * Adds a role to the authorization constraint
     *
     * @param the role name to be added
     */
    public void addSecurityRole(String roleName) {
        addSecurityRole(new SecurityRoleDescriptor(roleName));
    }

    /**
     * Removes the given role from the autrhorization constraint.
     *
     * @param the role to be removed.
     */
    public void removeSecurityRole(SecurityRole securityRole) {
        getSecurityRoleSet().remove(securityRole);
    }

    /**
     * Prints a formatted representation of this object.
     */
    public void print(StringBuilder toStringBuilder) {
        toStringBuilder.append("AuthorizationConstraint ");
        super.print(toStringBuilder);
        toStringBuilder.append(" securityRoles ").append(securityRoles);
    }

}
