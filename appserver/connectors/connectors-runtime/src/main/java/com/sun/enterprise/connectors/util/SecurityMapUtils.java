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

package com.sun.enterprise.connectors.util;

import org.glassfish.connectors.config.BackendPrincipal;
import org.glassfish.connectors.config.SecurityMap;
import com.sun.enterprise.connectors.authentication.ConnectorSecurityMap;
import com.sun.enterprise.connectors.authentication.EisBackendPrincipal;
import com.sun.enterprise.connectors.authentication.RuntimeSecurityMap;
import com.sun.enterprise.deployment.ResourcePrincipal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is class performs the task of adding/deleting and updating the
 * security maps to the connector registry.
 *
 * @author Srikanth P
 */

public class SecurityMapUtils {

    public static final String USERMAP = "usermap";
    public static final String GROUPMAP = "groupmap";

    /**
     * Updates the registry with the security map. If a security map already
     * exists it deletes that map completely before adding the mew security
     * map.
     *
     * @param securityMaps Array of securityMaps to be updated.
     * @return Hash Map containing 1 - 1 mappings of principal and
     *         Resource Principal
     */

    public static RuntimeSecurityMap processSecurityMaps(
            ConnectorSecurityMap[] securityMaps) {
        if (securityMaps == null || securityMaps.length == 0) {
            return new RuntimeSecurityMap();
        }

        HashMap userMap = new HashMap();
        HashMap groupMap = new HashMap();
        // Add user-backendPrincipal mappings to Map1
        for (ConnectorSecurityMap map : securityMaps) {
            ResourcePrincipal principal = generateResourcePrincipal(map);

            List<String> principalNames = map.getPrincipals();
            for (String principalName : principalNames) {
                userMap.put(principalName, principal);
            }

            List<String> groupNames = map.getUserGroups();
            for (String groupName : groupNames)
                groupMap.put(groupName, principal);
        }
        return new RuntimeSecurityMap(userMap, groupMap);
    }

    public static ConnectorSecurityMap[] getConnectorSecurityMaps(
            List<SecurityMap> securityMapList) {

        ConnectorSecurityMap[] maps = null;
        maps = new ConnectorSecurityMap[securityMapList.size()];
        for (int i = 0; i < securityMapList.size(); i++) {
            maps[i] = convertSecurityMapConfigBeanToSecurityMap(securityMapList.get(i));
        }
        return maps;
    }

    private static ConnectorSecurityMap convertSecurityMapConfigBeanToSecurityMap(
            SecurityMap securityMap) {

        String name = securityMap.getName();
        List<String> principalList = new ArrayList<String>();
        for(String p: securityMap.getPrincipal()){
            principalList.add(p);
        }

        List<String> userGroupList = new ArrayList<String>();
        for(String g : securityMap.getUserGroup()){
            userGroupList.add(g);
        }
        EisBackendPrincipal backendPrincipal = transformBackendPrincipal(securityMap
                .getBackendPrincipal());
        return new ConnectorSecurityMap(name, principalList, userGroupList, backendPrincipal);
    }

    /**
     * Creates the ResourcePrincipal object from the given securityMap
     *
     * @param securityMap SecurityMap
     * @return created ResourcePrincipal object
     */

    private static ResourcePrincipal generateResourcePrincipal(
            ConnectorSecurityMap securityMap) {

        EisBackendPrincipal backendPrincipal = securityMap.getBackendPrincipal();
        String userName = backendPrincipal.getUserName();
        String password = backendPrincipal.getPassword();
        return new ResourcePrincipal(userName, password);
    }

    private static EisBackendPrincipal transformBackendPrincipal(BackendPrincipal principal) {
        String userName = principal.getUserName();
        String password = principal.getPassword();
        return new EisBackendPrincipal(userName, password);
    }
}

