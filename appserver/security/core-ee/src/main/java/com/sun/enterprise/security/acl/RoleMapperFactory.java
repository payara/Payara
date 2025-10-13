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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.acl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Jerome Dochez
 */
@Service
@Singleton
public class RoleMapperFactory implements SecurityRoleMapperFactory {

    private Map<String, String> CONTEXT_TO_APPNAME = new ConcurrentHashMap<>();
    private Map<String, SecurityRoleMapper> ROLEMAPPER = new ConcurrentHashMap<>();


    /**
     * Returns a RoleMapper corresponding to the AppName.
     *
     * @param The Application Name of this RoleMapper.
     *
     */
    @Override
    public SecurityRoleMapper getRoleMapper(String appName) {
        // If the appName is not appname but contextid for web apps then get the appname
        String contextId = appName;
        String appname = getAppNameForContext(appName);

        SecurityRoleMapper securityRoleMapper = null;

        if (appname != null)
            securityRoleMapper = getRoleMapper(appname, this);

        if (securityRoleMapper == null) {
            securityRoleMapper = getRoleMapper(contextId, this);
        }

        return securityRoleMapper;
    }

    @Override
    public String getAppNameForContext(String contextId) {
        return CONTEXT_TO_APPNAME.get(contextId);
    }

    @Override
    public void setAppNameForContext(String appName, String contextId) {
        CONTEXT_TO_APPNAME.put(contextId, appName);
    }

    @Override
    public void removeAppNameForContext(String contextId) {
        CONTEXT_TO_APPNAME.remove(contextId);
    }

    /**
     * Returns a RoleMapper corresponding to the AppName.
     *
     * @param appName Application Name of this RoleMapper.
     * @return SecurityRoleMapper for the application
     */
    public RoleMapper getRoleMapper(String appName, SecurityRoleMapperFactory fact) {
        return (RoleMapper) ROLEMAPPER.computeIfAbsent(appName, e -> new RoleMapper(appName));
    }

    /**
     * Set a RoleMapper for the application
     *
     * @param appName Application or module name
     * @param securityRoleMapper <I>SecurityRoleMapper</I> for the application or the module
     */
    @Override
    public void setRoleMapper(String appName, SecurityRoleMapper securityRoleMapper) {
        ROLEMAPPER.put(appName, securityRoleMapper);
    }

    /**
     * @param appName Application/module name.
     */
    @Override
    public void removeRoleMapper(String appName) {
        if (ROLEMAPPER.containsKey(appName)) {
            ROLEMAPPER.remove(appName);
        }
    }
}
