/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.deployment.runtime.common.wls;

import java.util.ArrayList;
import java.util.List;

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

/**
 * This is the in memory representation of the security-role-mapping information. 
 * 
 * <p>
 * Note that we are keeping just the literal Strings in this object. The implementation of Principal is 
 * not instantiated here. 
 * 
 * <p>
 * This is because 
 * 
 * 1) the dol should avoid loading any classes as the classloaders used for deployment and runtime can be different. 
 * 2) verifier uses this information and it has not access to the role-mapper on the server.
 *
 * @author Sudarsan Sridhar
 */
public class SecurityRoleAssignment extends RuntimeDescriptor {

    private static final long serialVersionUID = -8950600974064582673L;
    
    private String roleName; // mandatory element
    private List<String> principals = new ArrayList<>();
    private boolean externallyDefined;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String name) {
        roleName = name;
    }

    public List<String> getPrincipalNames() {
        return principals;
    }

    public void addPrincipalName(String principalName) {
        principals.add(principalName);
    }

    public boolean isExternallyDefined() {
        return externallyDefined;
    }

    public void setExternallyDefined() {
        externallyDefined = true;
    }
}
