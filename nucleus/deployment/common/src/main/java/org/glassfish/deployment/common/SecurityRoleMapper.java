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
// Portions Copyright [2024] Payara Foundation and/or affiliates
// Portions Copyright [2024] Contributors to the Eclipse Foundation
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package org.glassfish.deployment.common;

import org.glassfish.security.common.Role;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This interface defines the protocol used by the DOL to access the role
 * mapping information of a Java EE application. This class is implemented by
 * other modules and their instanciation is done through the 
 * SecurityRoleMapperFactory class.
 *
 * @author Jerome Dochez
 */
public interface SecurityRoleMapper {
    
    /**
     * Set the role mapper application name
     * @param the app name
     */ 
    void setName(String name);
    
    /**
     * @return the role mapper application name
     */ 
    String getName();    
    
    /**
     * @return an iterator on all the assigned roles
     */
    Iterator<String> getRoles();
    
    /**
     * @rturns an enumeration of Principals assigned to the given role
     * @param The Role to which the principals are assigned to.
     */
    Enumeration<? extends Principal> getUsersAssignedTo(Role r);
    
    
    /**
     * Returns an enumeration of Groups assigned to the given role
     * @param The Role to which the groups are assigned to.
     */
    Enumeration<? extends Principal> getGroupsAssignedTo(Role r);
    
    /**
     * Assigns a Principal to the specified role.
     *
     * @param p The principal that needs to be assigned to the role.
     * @param r The Role the principal is being assigned to.
     * @param rdd The descriptor of the module calling assignRole.
     */
    void assignRole(Principal p, Role r, RootDeploymentDescriptor rdd);
    
    /**
     * Remove the given role-principal mapping
     * @param role, Role object
     * @param principal, the principal
     */    
    void unassignPrincipalFromRole(Role role, Principal principal);
    
    /**
     *  Remove all the role mapping information for this role
     * @param role, the role object
     */
    void unassignRole(Role role);
    
    /*
     * @Map a map of roles to the corresponding subjects
     */
    Map<String, Subject> getRoleToSubjectMapping();

    /**
     *
     * @return
     */
    Map<String, Set<String>> getGroupToRolesMapping();


    /**
     *
     * @return
     */
        Map<String, Set<String>> getCallerToRolesMapping();

    /**
     *
     * @return
     */
    boolean isDefaultPrincipalToRoleMapping();

    /**
     * Extracts the groups from the GlassFish specific and potential other unknown principals.
     *
     * @param subject container for finding groups, may be null
     * @return a list of (non-mapped) groups
     */
    Set<String> getGroups(Subject subject);

    /**
     *
     * @param subject
     * @return
     */
    Principal getCallerPrincipal(Subject subject);}
