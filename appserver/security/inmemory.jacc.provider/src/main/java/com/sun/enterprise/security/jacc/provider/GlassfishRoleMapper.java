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

package com.sun.enterprise.security.jacc.provider;

import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import java.security.Principal;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

import org.glassfish.internal.api.Globals;

/**
 * Glassfish role mapper 
 * NB: mapper only supports disjunctive 
 * (as apposed to conjunctive principal 2 role mappings. IOW, there 
 * is no way to require 2 or more principals to be in a Role.
 *
 * @author monzillo
 */
public class GlassfishRoleMapper implements JACCRoleMapper {

    private static final Logger defaultLogger =
            Logger.getLogger(GlassfishRoleMapper.class.getName());
    private final Logger logger;

    public GlassfishRoleMapper(Logger logger) {
        if (logger == null) {
            this.logger = defaultLogger;
        } else {
            this.logger = logger;
        }
    }

    private SecurityRoleMapper getInternalMapper(String pcid) {

        SecurityRoleMapperFactory factory = Globals.get(SecurityRoleMapperFactory.class);
                //SecurityRoleMapperFactoryMgr.getFactory();

        if (factory == null) {
            String msg = "RoleMapper.factory.lookup.failed";
            logger.log(Level.SEVERE, msg);
            throw new SecurityException(msg);
        }

        SecurityRoleMapper srm = factory.getRoleMapper(pcid);

        if (srm == null) {
            String msg = "RoleMapper.mapper.lookup.failed";
            logger.log(Level.SEVERE, msg);
            throw new SecurityException(msg);
        }
        return srm;
    }

    private Set<String> getDeclaredRoles(SecurityRoleMapper srm) {

        // default role mapping does not implement srm.getRoles() properly
        // until that is fixed, must throw UnsupportedOperation exception

        if (true) {
            String msg = "RoleMapper.unable.to.get.roles";
            logger.log(Level.SEVERE, msg);
            throw new UnsupportedOperationException(msg);
        }

        HashSet<String> roleNameSet = null;
        Iterator<String> it = srm.getRoles();
        while (it.hasNext()) {
            if (roleNameSet == null) {
                roleNameSet = new HashSet<String>();
            }
            roleNameSet.add(it.next());
        }
        return roleNameSet;
    }

    private Set<Principal> getPrincipalsInRole(SecurityRoleMapper srm,
            String roleName) throws SecurityException, UnsupportedOperationException {

        Map<String, Subject> roleMap = (Map) srm.getRoleToSubjectMapping();
        if (roleMap == null) {
            return null;
        }

        Subject s = roleMap.get(roleName);
        if (s == null) {
            return null;
        }

        return s.getPrincipals();
    }

    public boolean arePrincipalsInRole(SecurityRoleMapper srm,
            Principal[] principals, String roleName) throws SecurityException {

        if (principals == null || principals.length == 0) {
            return false;
        }

        Set<Principal> rolePrincipals = getPrincipalsInRole(srm, roleName);
        if (rolePrincipals == null || rolePrincipals.isEmpty()) {
            return false;
        }

        for (Principal p : principals) {
            if (rolePrincipals.contains(p)) {
                return true;
            }
        }
        return false;
    }

    // public methods follow
    public Set<String> getDeclaredRoles(String pcid) {
        return getDeclaredRoles(getInternalMapper(pcid));
    }

    public boolean isSubjectInRole(String pcid, Subject s, String roleName)
            throws SecurityException {
        return arePrincipalsInRole(pcid, toArray(s.getPrincipals()),roleName);
    }

    public boolean arePrincipalsInRole(String pcid, Principal[] principals,
            String roleName) throws SecurityException {
        return arePrincipalsInRole(getInternalMapper(pcid), principals, roleName);
    }

    public Set<String> getRolesOfSubject(String pcid, Subject s)
            throws SecurityException, UnsupportedOperationException {
        return getRolesOfPrincipals(pcid, toArray(s.getPrincipals()));
    }

    public Set<String> getRolesOfPrincipals(String pcid, Principal[] principals)
            throws SecurityException, UnsupportedOperationException {

        if ( principals.length == 0) {
            return null;
        }

        SecurityRoleMapper srm = getInternalMapper(pcid);
        Set<String> roleNames = getDeclaredRoles(srm);

        //Comment out for now to supress FindBugs warning, getDeclaredRoles(srm) always throw UnsupportedOperationException currently so roleNames cannot be null, when getDeclaredRoles is fixed we can uncomment this
        //if (roleNames == null) {
        //    return null;
        //}

        HashSet<String> roles = new HashSet<String>();
        Iterator<String> it = roleNames.iterator();
        while (it.hasNext()) {
            String roleName = it.next();
            Set<Principal> pSet = getPrincipalsInRole(srm, roleName);
            if (pSet != null) {
                for (Principal p : principals) {
                    if (pSet.contains(p)) {
                        roles.add(roleName);
                        break;
                    }
                }
            }
        }

        return roles;
    }

    public BitSet getRolesOfSubject(String pcid, String[] roles, Subject s)
            throws SecurityException, UnsupportedOperationException {
        return getRolesOfPrincipals(pcid, roles, toArray(s.getPrincipals()));
    }

    private Principal[] toArray(Set principals) {
        Principal[] list = new Principal[principals.size()];
        int i=0;
        for (Object obj: principals) {
            if (obj instanceof Principal) {
                list[i] = (Principal)obj;
            }
        }
        return list;
    }
    public BitSet getRolesOfPrincipals(String pcid, String[] roles, Principal[] principals)
            throws SecurityException, UnsupportedOperationException {
        if ( principals.length == 0 ||
                roles == null || roles.length == 0) {
            return null;
        }
        BitSet roleSet = new BitSet(roles.length);
        SecurityRoleMapper srm = getInternalMapper(pcid);
        for (int i = 0; i < roles.length; i++) {
            roleSet.set(i, arePrincipalsInRole(srm, principals, roles[i]));
        }
        return roleSet;
    }

    public Set<Principal> getPrincipalsInRole(String pcid, String roleName)
            throws SecurityException, UnsupportedOperationException {
        return getPrincipalsInRole(getInternalMapper(pcid), roleName);
    }
}
