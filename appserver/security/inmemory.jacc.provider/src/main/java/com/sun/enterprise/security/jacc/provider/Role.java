/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.jacc.provider;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Set;

/**
 *
 * @author monzillo
 */
public class Role {

    String roleName;
    Permissions permissions;
    Set<Principal> principals;
    private boolean isAnyAuthenticatedUserRole = false;

    public Role(String name) {
        roleName = name;
    }

    /**
     * NB: Class Overrides equals and hashCode Methods such that 2 Roles are
     * equal simply based on having a common name.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        Role other = (o == null || !(o instanceof Role) ? null : (Role) o);
        return (o == null ? false : getName().equals(other.getName()));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.roleName != null ? this.roleName.hashCode() : 0);
        return hash;
    }

    public String getName() {
        return roleName;
    }

    void addPermission(Permission p) {
        if (permissions == null) {
            permissions = new Permissions();
        }
        permissions.add(p);
    }

    void addPermissions(PermissionCollection pc) {
        if (permissions == null) {
            permissions = new Permissions();
        }
        for (Enumeration<Permission> e = pc.elements();
                e.hasMoreElements();) {
            permissions.add(e.nextElement());
        }
    }

    Permissions getPermissions() {
        return permissions;
    }

    void setPrincipals(Set<Principal> pSet) {
        if (pSet != null) {
            principals = pSet;
        }
    }

    boolean implies(Permission p) {
        boolean rvalue = false;
        if (permissions != null) {
            rvalue = permissions.implies(p);
        }
        return rvalue;
    }

    void determineAnyAuthenticatedUserRole() {
        isAnyAuthenticatedUserRole = false;
        // If no princiapls are present then any authenticated user is possible
        if ((principals == null) || principals.isEmpty()) {
        	isAnyAuthenticatedUserRole = true;
        }
    }

    boolean isAnyAuthenticatedUserRole() {
        return isAnyAuthenticatedUserRole;
    }

    boolean isPrincipalInRole(Principal p) {
        if (isAnyAuthenticatedUserRole && (p != null)) {
            return true;
        }

        boolean rvalue = false;
        if (principals != null) {
            rvalue = principals.contains(p);
        }
        return rvalue;
    }

    boolean arePrincipalsInRole(Principal subject[]) {
        if (subject == null || subject.length == 0) {
            return false;
        }
        if (isAnyAuthenticatedUserRole) {
            return true;
        }
        if (principals == null || principals.isEmpty()) {
            return false;
        }

        boolean rvalue = false;
        for (Principal p : subject) {
            if (principals.contains(p)) {
                rvalue = true;
                break;
            }
        }
        return rvalue;
    }
}
