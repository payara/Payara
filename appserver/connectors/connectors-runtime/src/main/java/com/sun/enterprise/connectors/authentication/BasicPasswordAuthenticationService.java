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

package com.sun.enterprise.connectors.authentication;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.ejb.api.EJBInvocation;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.ejb.EJBContext;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class does the functionality of security mapping of the
 * principal and userGroup to the backendPrincipal.
 *
 * @author Srikanth P
 */
public class BasicPasswordAuthenticationService
        implements AuthenticationService {

    private String rarName_;
    private PoolInfo poolInfo_;
    ConnectorRegistry connectorRegistry_ = ConnectorRegistry.getInstance();
    static Logger _logger = LogDomains.getLogger(BasicPasswordAuthenticationService.class, LogDomains.RSR_LOGGER);
    private Object containerContext = null;
    private SecurityRoleMapperFactory securityRoleMapperFactory;

    /**
     * Constructor
     *
     * @param rarName  Name of the rar
     * @param poolInfo Name of the pool.
     */
    public BasicPasswordAuthenticationService(String rarName, PoolInfo poolInfo) {
        rarName_ = rarName;
        poolInfo_ = poolInfo;
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Constructor:BasicPasswordAuthenticationService");
        }
    }

    /**
     * Maps the principal to the backendPrincipal
     *
     * @param callerPrincipal Name of the principal to be mapped.
     * @return Mapped Backendprincipal
     */
    public Principal mapPrincipal(Principal callerPrincipal, Set principalSet) {

        // If no security maps are associated with this pool, return empty
        RuntimeSecurityMap runtimeSecurityMap =
                connectorRegistry_.getRuntimeSecurityMap(poolInfo_);
        if (runtimeSecurityMap == null) {
            return null;
        }

        String principalName = callerPrincipal.getName();

        // Create a list of Group Names from group Set
        List<String> groupNames = new ArrayList<String>();
        Iterator iter = principalSet.iterator();
        while (iter.hasNext()) {
            Principal p = (Principal) iter.next();
            // remove the caller principal (calling user) from the Set.
            if (p.equals(callerPrincipal)) {
                continue;
            }
            String groupName = p.getName();
            groupNames.add(groupName);
        }

        // if webmodule get roles from WebBundle Descriptor
        if (isContainerContextAWebModuleObject()) {
            String roleName = getRoleName(callerPrincipal);
            return doMap(principalName, groupNames, roleName, runtimeSecurityMap);
        } else {
            return doMap(principalName, groupNames, null, runtimeSecurityMap);
        }
    }

    /**
     * Performs the actual mapping of the principal/userGroup to the
     * backendPrincipal by checking at the connector registry for all the
     * existing mapping. If a map is found the backendPrincipal is
     * returned else null is returned .
     */
    private Principal doMap(String principalName, List groupNames,
            String roleName, RuntimeSecurityMap runtimeSecurityMap) {

        // Policy: 
        // user_1, user_2, ... user_n
        // group_1/role_1, group_2/role_2, ... group_n/role_n
        // user contains * 
        // role/group contains *

        HashMap userNameSecurityMap = (HashMap) runtimeSecurityMap.getUserMap();
        HashMap groupNameSecurityMap = (HashMap) runtimeSecurityMap.getGroupMap();

        // Check if caller's user-name is preset in the User Map
        if (userNameSecurityMap.containsKey(principalName)) {
            return (Principal) userNameSecurityMap.get(principalName);
        }

        // Check if caller's role is present in the Group Map
        if (isContainerContextAWebModuleObject() && roleName != null) {
            if (groupNameSecurityMap.containsKey(roleName)) {
                return (Principal) groupNameSecurityMap.get(roleName);
            }
        }

        // If ejb, use isCallerInRole  
        if (isContainerContextAEJBContainerObject() && roleName == null) {
            ComponentInvocation componentInvocation =
                    ConnectorRuntime.getRuntime().getInvocationManager().getCurrentInvocation();
            EJBInvocation ejbInvocation = (EJBInvocation) componentInvocation;
            EJBContext ejbcontext = ejbInvocation.getEJBContext();
            Set<Map.Entry> s = (Set<Map.Entry>) groupNameSecurityMap.entrySet();
            Iterator i = s.iterator();
            while(i.hasNext()) {
                Map.Entry mapEntry = (Map.Entry) i.next();
                String key = (String) mapEntry.getKey();
                Principal entry = (Principal) mapEntry.getValue();

                boolean isInRole = false;
                try {
                    isInRole = ejbcontext.isCallerInRole(key);
                } catch (Exception ex) {
                    if(_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "BasicPasswordAuthentication::caller not in role " + key);
                    }
                }
                if (isInRole) {
                    return entry;
                }
            }
       }

        // Check if caller's group(s) is/are present in the Group Map
        for (int j = 0; j < groupNames.size(); j++) {
            String groupName = (String) groupNames.get(j);
            if (groupNameSecurityMap.containsKey(groupName)) {
                return (Principal) groupNameSecurityMap.get(groupName);
            }
        }

        // Check if user name is * in Security Map
        if (userNameSecurityMap.containsKey(ConnectorConstants.SECURITYMAPMETACHAR)) {
            return (Principal) userNameSecurityMap.get(ConnectorConstants.SECURITYMAPMETACHAR);
        }

        // Check if role/group name is * in Security Map
        if (groupNameSecurityMap.containsKey(ConnectorConstants.SECURITYMAPMETACHAR)) {
            return (Principal) groupNameSecurityMap.get(ConnectorConstants.SECURITYMAPMETACHAR);
        }

        return null;
    }

    private String getRoleName(Principal callerPrincipal) {

        String roleName = null;

        WebBundleDescriptor wbd = (WebBundleDescriptor) getComponentEnvManager().getCurrentJndiNameEnvironment();

        SecurityRoleMapperFactory securityRoleMapperFactory = getSecurityRoleMapperFactory();
        SecurityRoleMapper securityRoleMapper =
                securityRoleMapperFactory.getRoleMapper(wbd.getModuleID());

        Map<String, Subject> map = securityRoleMapper.getRoleToSubjectMapping();
        for (Map.Entry<String, Subject> entry : map.entrySet()) {
            roleName = entry.getKey();
            Subject subject = entry.getValue();
            Set principalSet = subject.getPrincipals();
            if (principalSet.contains(callerPrincipal)) {
                return roleName;
            }
        }
        return "";
    }

    private ComponentEnvManager getComponentEnvManager() {
        return ConnectorRuntime.getRuntime().getComponentEnvManager();
    }

    private ComponentInvocation getCurrentComponentInvocation() {
        return ConnectorRuntime.getRuntime().getInvocationManager().getCurrentInvocation();
    }

    private ComponentInvocation.ComponentInvocationType getCurrentComponentType() {
        return getCurrentComponentInvocation().getInvocationType();
    }

    private boolean isContainerContextAWebModuleObject() {
        return ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION.equals(getCurrentComponentType());
    }

    //TODO V3 use this instead of isContainerContextAContainerObject
    private boolean isContainerContextAEJBContainerObject() {
        return ComponentInvocation.ComponentInvocationType.EJB_INVOCATION.equals(getCurrentComponentType());
    }

    public SecurityRoleMapperFactory getSecurityRoleMapperFactory() {
        return ConnectorRuntime.getRuntime().getSecurityRoleMapperFactory();
    }
}
