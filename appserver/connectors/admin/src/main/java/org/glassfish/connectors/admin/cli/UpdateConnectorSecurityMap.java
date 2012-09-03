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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.connectors.config.BackendPrincipal;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.SecurityMap;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;

import javax.inject.Inject;

/**
 * Update Connector SecurityMap command
 */
@Service(name="update-connector-security-map")
@PerLookup
@I18n("update.connector.security.map")
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.POST, 
        path="update-connector-security-map", 
        description="update-connector-security-map")
})
public class UpdateConnectorSecurityMap extends ConnectorSecurityMap implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(UpdateConnectorSecurityMap.class);

    @Param(optional = true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Param(name="poolname")
    String poolName;

    @Param(name="addprincipals", optional=true)
    List<String> addPrincipals;

    @Param(name="addusergroups", optional=true)
    List<String> addUserGroups;

    @Param(name="removeprincipals", optional=true)
    List<String> removePrincipals;

    @Param(name="removeusergroups", optional=true)
    List<String> removeUserGroups;

    @Param(name="mappedusername", optional=true)
    String mappedusername;

    @Param(name="mappedpassword", password=true, optional=true)
    String mappedpassword;

    @Param(name="mapname", primary=true)
    String securityMapName;

    @Inject
    private Domain domain;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (securityMapName == null) {
            report.setMessage(localStrings.getLocalString("create.connector.security.map.noSecurityMapName",
                    "No security map name specified"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        Collection<ConnectorConnectionPool> ccPools =  domain.getResources().getResources(ConnectorConnectionPool.class);
        if (!doesPoolNameExist(poolName, ccPools)) {
            report.setMessage(localStrings.getLocalString("create.connector.security.map.noSuchPoolFound",
                    "Connector connection pool {0} does not exist. Please specify a valid pool name.", poolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!doesMapNameExist(poolName, securityMapName, ccPools)) {
            report.setMessage(localStrings.getLocalString("update.connector.security.map.map_does_not_exist",
                    "Security map {0} does not exist for connector connection pool {1}. Please give a valid map name.",
                    securityMapName, poolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        //get all the security maps for this pool.....
        List<SecurityMap> maps = getAllSecurityMapsForPool(poolName, ccPools);

        //check if addPrincipals and removePrincipals have the same value
        if (addPrincipals != null && removePrincipals != null) {
            for (String ap : addPrincipals) {
                for (String rp : removePrincipals) {
                    if (rp.equals(ap)) {
                        report.setMessage(localStrings.getLocalString(
                                "update.connector.security.map.same_principal_values",
                                "This value {0} is given in both --addprincipals and --removeprincipals. The same value cannot given for these options.",
                                ap));
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }

                }
            }
        }

        //check if addUserGroups and removeUserGroups have the same value
        if (addUserGroups != null && removeUserGroups != null) {
            for (String aug : addUserGroups) {
                for (String rug : removeUserGroups) {
                    if (rug.equals(aug)) {
                        report.setMessage(localStrings.getLocalString(
                                "update.connector.security.map.same_usergroup_values",
                                "This value {0} is given in both --addusergroups and --removeusergroups. The same value cannot given for these options.",
                                aug));
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }

                }
            }
        }

        // make sure that the principals to be added are not existing in any map ...
        if (addPrincipals != null) {
            for (String principal : addPrincipals) {
                if (isPrincipalExisting(principal, maps)) {
                    report.setMessage(localStrings.getLocalString("create.connector.security.map.principal_exists",
                            "The principal {0} already exists in connector connection pool {1}. Please give a different principal name.",
                            principal, poolName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }
        // make sure that the user groups to be added are not existing in any map ...
        if (addUserGroups != null) {
            for (String userGroup : addUserGroups) {
                if (isUserGroupExisting(userGroup, maps)) {
                    report.setMessage(localStrings.getLocalString("create.connector.security.map.usergroup_exists",
                            "The user-group {0} already exists in connector connection pool {1}. Please give a different user-group name.",
                            userGroup, poolName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        SecurityMap map = getSecurityMap(securityMapName, poolName, ccPools);
        final List<String> existingPrincipals = new ArrayList(map.getPrincipal());
        final List<String> existingUserGroups = new ArrayList(map.getUserGroup());

         if (existingPrincipals.isEmpty() && addPrincipals != null) {
            report.setMessage(localStrings.getLocalString("update.connector.security.map." +
                    "addPrincipalToExistingUserGroupsWorkSecurityMap",
                    "Failed to add principals to a security map with user groups."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (existingUserGroups.isEmpty() && addUserGroups != null) {
            report.setMessage(localStrings.getLocalString("update.connector.security.map." +
                    "addUserGroupsToExistingPrincipalsWorkSecurityMap",
                    "Failed to add user groups to a security map with principals."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        //check if there is any invalid principal in removePrincipals.
        if (removePrincipals != null) {
            boolean principalExists = true;
            String principal = null;
            for (String p : removePrincipals) {
                if (!existingPrincipals.contains(p)) {
                    principalExists = false;
                    principal = p;
                    break;
                }
            }
            if (!principalExists) {
                report.setMessage(localStrings.getLocalString("update.connector.security.map.principal_does_not_exists",
                        "The principal {0} that you want to delete does not exist in connector connection pool {1}. Please give a valid principal name.",
                        principal, poolName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        //check if there is any invalid usergroup in removeUserGroups.
        if (removeUserGroups != null) {
            boolean userGroupExists = true;
            String userGroup = null;
            for (String ug : removeUserGroups) {
                if (!existingUserGroups.contains(ug)) {
                    userGroupExists = false;
                    userGroup = ug;
                    break;
                }
            }
            if (!userGroupExists) {
                report.setMessage(localStrings.getLocalString("update.connector.security.map.usergroup_does_not_exists",
                        "The usergroup {0} that you want to delete does not exist in connector connection pool {1}. Please give a valid user-group name.",
                        userGroup, poolName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        //FIX : Bug 4914883.
        //The user should not delete all principals and usergroups in the map.
        // Atleast one principal or usergroup must exists.

        if (addPrincipals == null && addUserGroups == null) {
            boolean principalsEmpty = false;
            boolean userGroupsEmpty = false;

            if (removePrincipals == null && existingPrincipals.isEmpty()) {
                principalsEmpty = true;
            }
            if (removeUserGroups == null && existingUserGroups.isEmpty()) {
                userGroupsEmpty = true;
            }

            if ((removePrincipals != null) &&
                    (removePrincipals.size() == existingPrincipals.size())) {
                principalsEmpty = true;
            }

            if ((removeUserGroups != null) &&
                    (removeUserGroups.size() == existingUserGroups.size())) {
                userGroupsEmpty = true;
            }
            if (userGroupsEmpty && principalsEmpty) {
                report.setMessage(localStrings.getLocalString("update.connector.security.map.principals_usergroups_will_be_null",
                        "The values in your command will delete all principals and usergroups. You cannot delete all principals and usergroups. Atleast one of them must exist."));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        //add principals to the existingPrincipals arraylist.
        if (addPrincipals != null) {
            for (String principal : addPrincipals) {
                if (!existingPrincipals.contains(principal)) {
                    existingPrincipals.add(principal);
                } else {
                    report.setMessage(localStrings.getLocalString("create.connector.security.map.principal_exists",
                            "The principal {0} already exists in connector connection pool {1}. Please give a different principal name.",
                            principal, poolName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        //removing principals from existingPrincipals arraylist.
        if (removePrincipals != null) {
            for (String principal : removePrincipals) {
                existingPrincipals.remove(principal);
            }
        }

        //adding user-groups....
        if (addUserGroups != null) {
            for (String userGroup : addUserGroups) {
                if (!existingUserGroups.contains(userGroup)) {
                    existingUserGroups.add(userGroup);
                } else {
                    report.setMessage(localStrings.getLocalString("create.connector.security.map.usergroup_exists",
                            "The user-group {0} already exists in connector connection pool {1}. Please give a different user-group name.",
                            userGroup, poolName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        //removing user-groups....
        if (removeUserGroups != null) {
            for (String userGroup : removeUserGroups) {
                existingUserGroups.remove(userGroup);
            }
        }

        BackendPrincipal backendPrincipal = map.getBackendPrincipal();

        try {
            ConfigSupport.apply(new ConfigCode() {

                public Object run(ConfigBeanProxy... params) throws PropertyVetoException, TransactionFailure {
                    SecurityMap sm = (SecurityMap) params[0];
                    BackendPrincipal bp = (BackendPrincipal) params[1];
                    //setting the updated principal user-group arrays....
                    if (existingPrincipals != null) {
                        sm.getPrincipal().clear();
                        for (String principal : existingPrincipals) {
                            sm.getPrincipal().add(principal);
                        }
                    }
                    if (existingUserGroups != null) {
                        sm.getUserGroup().clear();
                        for (String userGroup : existingUserGroups) {
                            sm.getUserGroup().add(userGroup);
                        }
                    }

                    //updating the backend-principal.......
                    //get the backend principal for the given security map and pool...
                    if (mappedusername != null && !mappedusername.isEmpty()) {
                        bp.setUserName(mappedusername);
                    }
                    if (mappedpassword != null && !mappedpassword.isEmpty()) {
                        bp.setPassword(mappedpassword);
                    }

                    return sm;
                }
            }, map, backendPrincipal);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (TransactionFailure tfe) {
            Object params[] = {securityMapName, poolName};
            report.setMessage(localStrings.getLocalString("update.connector.security.map.fail",
                    "Unable to update security map {0} for connector connection pool {1}.", params) +
                    " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }        
    }   
}
