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

package com.sun.enterprise.security.acl;

import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.deployment.common.SecurityRoleMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;


import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 *
 * @author  Jerome Dochez
 */
@Service
@Singleton
public class RoleMapperFactory implements SecurityRoleMapperFactory {
    private  Map CONTEXT_TO_APPNAME = new HashMap();
    private  Hashtable ROLEMAPPER = new Hashtable();
    
    /** Creates a new instance of RoleMapperFactory */
    public RoleMapperFactory() {
    }
    
    /** Returns a RoleMapper corresponding to the AppName.
     * @param  The Application Name of this RoleMapper.
     *
     */
    public SecurityRoleMapper getRoleMapper(String appName) {
        // if the appName is not appname but contextid for
        // web apps then get the appname
        String contextId = appName;
        String appname = getAppNameForContext(appName);
        SecurityRoleMapper srm = null;
        if(appname != null)
            srm = getRoleMapper(appname, this);
        if(srm == null){
            srm = getRoleMapper(contextId, this);
        }
        return srm;
    }
     
    /**
     * remove the RoleMapping associated with this application
     * @param the application name for this RoleMapper
     
    public void removeRoleMapper(String appName) {
        RoleMapper.removeRoleMapper(appName);
    }*/
    
    /**
     * Sets a new RoleMapper for a particular Application
     * @param the application name
     * @param the new role mapper
     
    public void setRoleMapper(String appName, SecurityRoleMapper rmap) {
        RoleMapper.setRoleMapper(appName, rmap);
    }*/
    
    public String getAppNameForContext(String contextId) {
        return (String)CONTEXT_TO_APPNAME.get(contextId);
    }
    
    public void setAppNameForContext(String appName, String contextId) {
        CONTEXT_TO_APPNAME.put(contextId, appName); 
    }
    
    public void removeAppNameForContext(String contextId) {
        CONTEXT_TO_APPNAME.remove(contextId);
    }
    
     /** Returns a RoleMapper corresponding to the AppName.
     * @param appName Application Name of this RoleMapper.
     * @return SecurityRoleMapper for the application
     */
    public RoleMapper getRoleMapper(String appName, SecurityRoleMapperFactory fact) {
        RoleMapper r = (RoleMapper) ROLEMAPPER.get(appName);
        if (r == null) {
            r = new RoleMapper(appName);
            ROLEMAPPER.put(appName, r);
        }
        return r;
    }
    
    /** Set a RoleMapper for the application
     * @param appName Application or module name
     * @param rmap <I>SecurityRoleMapper</I> for the application or the module
     */
    public  void setRoleMapper(String appName, SecurityRoleMapper rmap) {
            ROLEMAPPER.put(appName, rmap);
    }

    /**
     * @param appName Application/module name.
     */
    public void removeRoleMapper(String appName) {
        if (ROLEMAPPER.containsKey(appName)) {
                ROLEMAPPER.remove(appName);
        }
    }
}
