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

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.security.common.Role;

/**
 * Represents a method permission. A method permission can be associated to 
 * a role, be unchecked or excluded.
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class MethodPermission extends Descriptor {

    private static MethodPermission unchecked;
    private static MethodPermission excluded;
    private boolean isUnchecked = false;
    private boolean isExcluded = false;
    private Role role;
    
    /**
     * construct a new MethodPermission based on a security role
     * 
     * @param role the security role associated to the method permission
     */
    public MethodPermission(Role role) {
        this.role = role;
    }

    // We don't want uninitialized method permissins
    private MethodPermission() {
    }
    
    /**
     * @return an unchecked method permission. Methods associated with such a 
     * method permission can be invoked by anyone
     */
    public static synchronized MethodPermission getUncheckedMethodPermission() {
        if (unchecked==null) {
            unchecked = new MethodPermission();
            unchecked.isUnchecked=true;   
        }
        return unchecked;
    }
    
    /**
     * @return an ecluded method permission. Methods associated with such a 
     * method permission cannot be invoked by anyone.
     */
    public static synchronized MethodPermission getExcludedMethodPermission() {
        if (excluded==null) {
            excluded = new MethodPermission();
            excluded.isExcluded=true;   
        }
        return excluded;
    }    
    
    /**
     * @return true if the method permission is based on a security role
     */
    public boolean isRoleBased() {
        return role!=null;
    }
    
    /**
     * @return true if the method permission is unchecked
     */
    public boolean isUnchecked() {
        return isUnchecked;
    }
    
    /** 
     * @return true if the method permission is excluded
     */
    public boolean isExcluded() {
        return isExcluded;
    }
    
    /**
     * @return the security role associated with this method permission when
     * applicable (role based method permission)
     */
    public Role getRole() {
        return role;
    }
    
    // For Map storage
    public int hashCode() {
        if (role!=null)
            return role.hashCode();
        else
            return super.hashCode();
    }
    
    // for Map storage
    public boolean equals(Object other) {
	boolean ret = false;
	if(other instanceof MethodPermission) {
            MethodPermission o = (MethodPermission) other;
            if (isRoleBased()) {
	        ret = role.equals(o.getRole());
            } else {
                ret = (isExcluded == o.isExcluded()) && (isUnchecked == o.isUnchecked());
            }
	}	
	return ret;
    }    
    
    public void print(StringBuffer toStringBuffer) {
        if (isRoleBased()) {
            toStringBuffer.append(role.toString());
        } else {
            if (isExcluded) 
                toStringBuffer.append("excluded");
            else 
                toStringBuffer.append("unchecked");
        }
    }
}
        
