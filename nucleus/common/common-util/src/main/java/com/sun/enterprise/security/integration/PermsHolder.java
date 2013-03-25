/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.integration;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;


public class PermsHolder {


    /**
     * The PermissionCollection for each CodeSource 
     */
    private ConcurrentHashMap<String, PermissionCollection> loaderPC =
        new ConcurrentHashMap<String, PermissionCollection>();

    
    /**
     * EE permissions for  a module
     */
    private PermissionCollection eePermissionCollection = null;

    /**
     * declared permissions in  a module
     */
    private PermissionCollection declaredPermissionCollection = null;

    /**
     * EE restriction list
     */
    private PermissionCollection restrictPermissionCollection = null;

    
    public PermsHolder() {
        
    }

    public PermsHolder(PermissionCollection eePC, 
            PermissionCollection declPC,
            PermissionCollection restrictPC) {
     
        setEEPermissions(eePC);
        setDeclaredPermissions(declPC);
        setRestrictPermissions(restrictPC);
    }

    public void setEEPermissions(PermissionCollection eePc) {        
        eePermissionCollection = eePc;
    }


    public void setDeclaredPermissions(PermissionCollection declaredPc) {        
        declaredPermissionCollection = declaredPc;
    }
    
    public void setRestrictPermissions(PermissionCollection restrictPC) {
        restrictPermissionCollection = restrictPC;
    }
    public PermissionCollection getCachedPerms(CodeSource codesource) {

        if (codesource == null)
            return null;
        
        String codeUrl = codesource.getLocation().toString();
        
        return loaderPC.get(codeUrl);
    }
    
    public PermissionCollection getPermissions(CodeSource codesource, 
            PermissionCollection parentPC ) {

        String codeUrl = codesource.getLocation().toString();
        PermissionCollection cachedPermissons = loaderPC.get(codeUrl);

        if (cachedPermissons != null)
            return cachedPermissons;        
        else 
            cachedPermissons = new Permissions();
        
        PermissionCollection pc = parentPC;

        if (pc != null) {
            Enumeration<Permission> perms =  pc.elements();
            while (perms.hasMoreElements()) {
                Permission p = perms.nextElement();
                cachedPermissons.add(p);
            }
        }
        
            
        if (declaredPermissionCollection != null) {
            Enumeration<Permission> dperms =  this.declaredPermissionCollection.elements();
            while (dperms.hasMoreElements()) {
                Permission p = dperms.nextElement();
                cachedPermissons.add(p);
            }
        }
        
        if (eePermissionCollection != null) {
            Enumeration<Permission> eeperms =  eePermissionCollection.elements();
            while (eeperms.hasMoreElements()) {
                Permission p = eeperms.nextElement();
                cachedPermissons.add(p);
            }
            
        }
 
        PermissionCollection tmpPc = loaderPC.putIfAbsent(codeUrl, cachedPermissons);                
        if (tmpPc != null) {
            cachedPermissons = tmpPc;
        }

        return cachedPermissons;
        
    }
}
