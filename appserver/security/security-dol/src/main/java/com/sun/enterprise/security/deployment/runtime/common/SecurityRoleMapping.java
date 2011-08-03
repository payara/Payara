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

package com.sun.enterprise.security.deployment.runtime.common;

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the in memory representation of the security-role-mapping
 * information.  Note that we are keeping just the literal Strings
 * in this object.  The implementation of Principal is not instantiated
 * here.  This is because 1) the dol should avoid loading any classes
 * as the classloaders used for deployment and runtime can be different.
 * 2) verifier uses this information and it has not access to the rolemaper
 * on the server.
 *
 * @author Jerome Dochez
 */
public class SecurityRoleMapping extends RuntimeDescriptor {

    private String roleName = null; //mandatory element
    private List<PrincipalNameDescriptor> principals = 
                        new ArrayList<PrincipalNameDescriptor>();
    private List<String> groups = new ArrayList<String>();

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String name) {
        roleName = name;
    }

    public List<PrincipalNameDescriptor> getPrincipalNames() {
        return principals; 
    }

    public void addPrincipalName(PrincipalNameDescriptor p) {
        principals.add(p);
    }

    public List<String> getGroupNames() {
        return groups;
    }

    public void addGroupName(String g) {
        groups.add(g);
    }

    /**
     *@deprecated 
     *This method needs to be removed once the custom principal is fully 
     *supported. We keep it for now for backward compatiblity in API.  Note
     *that this method only returns the name of the principals, not their
     *class-names.  Use with caution!
     */
    public String[] getPrincipalName() {
        String[] names = new String[principals.size()];
        for (int i = 0; i < principals.size(); i++) {
            names[i] = principals.get(i).getName();
        }
        return names;
    }
}
