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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.SecurityMap;

import org.glassfish.hk2.api.PerLookup;

import java.util.Collection;
import java.util.List;

/**
 * Create Connector SecurityMap command
 */
@PerLookup
@I18n("create.connector.security.map")
public class ConnectorSecurityMap {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ConnectorSecurityMap.class);

    boolean doesPoolNameExist(String poolName, Collection<ConnectorConnectionPool> ccPools) {
        //check if the poolname exists.If it does not then throw an exception.
        boolean doesPoolExist = false;
        if (ccPools != null) {
            for (ConnectorConnectionPool ccp : ccPools) {
                if (ccp.getName().equals(poolName)) {
                    doesPoolExist = true;
                }
            }
        }
        return doesPoolExist;
    }

    boolean doesMapNameExist(String poolName, String mapname, Collection<ConnectorConnectionPool> ccPools) {
        //check if the mapname exists for the given pool name..
        List<SecurityMap> maps = getAllSecurityMapsForPool(poolName, ccPools);

        boolean doesMapNameExist = false;
        if (maps != null) {
            for (SecurityMap sm : maps) {
                String name = sm.getName();
                if (name.equals(mapname)) {
                    doesMapNameExist = true;
                }
            }
        }
        return doesMapNameExist;
    }

    List<SecurityMap> getAllSecurityMapsForPool(String poolName, Collection<ConnectorConnectionPool> ccPools) {
         List<SecurityMap> securityMaps = null;
         for (ConnectorConnectionPool ccp : ccPools) {
            if (ccp.getName().equals(poolName)) {
                securityMaps = ccp.getSecurityMap();
                break;
            }
         }
         return securityMaps;
    }

    ConnectorConnectionPool getPool(String poolName, Collection<ConnectorConnectionPool> ccPools) {
         ConnectorConnectionPool pool = null;
         for (ConnectorConnectionPool ccp : ccPools) {
            if (ccp.getName().equals(poolName)) {
                pool = ccp;
                break;
            }
         }
         return pool;
    }

    SecurityMap getSecurityMap(String mapName, String poolName, Collection<ConnectorConnectionPool> ccPools) {
        List<SecurityMap> maps = getAllSecurityMapsForPool(poolName, ccPools);
        SecurityMap map = null;
        if (maps != null) {
            for (SecurityMap sm : maps) {
                if (sm.getName().equals(mapName)) {
                    map = sm;
                    break;
                }
            }
        }
        return map;
    }

    boolean isPrincipalExisting(String principal, List<SecurityMap> maps) {
        boolean exists = false;
        List<String> existingPrincipals = null;

        if (maps != null) {
            for (SecurityMap sm : maps) {
                existingPrincipals = sm.getPrincipal();
                if (existingPrincipals != null && principal != null) {
                    for (String ep : existingPrincipals) {
                        if (ep.equals(principal)) {
                            exists = true;
                            break;
                        }
                    }
                }
            }
        }
        return exists;
    }

    boolean isUserGroupExisting(String usergroup, List<SecurityMap> maps) {
        boolean exists = false;
        List<String> existingUserGroups = null;
        if (maps != null) {
            for (SecurityMap sm : maps) {
                existingUserGroups = sm.getUserGroup();
                if (existingUserGroups != null && usergroup != null) {
                    for (String eug : existingUserGroups) {
                        if (eug.equals(usergroup)) {
                            exists = true;
                            break;
                        }
                    }
                }
            }
        }
        return exists;
    }
}
