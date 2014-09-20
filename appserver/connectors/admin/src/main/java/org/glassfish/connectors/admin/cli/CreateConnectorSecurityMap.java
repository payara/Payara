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
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.BackendPrincipal;
import org.glassfish.connectors.config.SecurityMap;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import static org.glassfish.connectors.admin.cli.CLIConstants.SM.*;

/**
 * Create Connector SecurityMap command
 */
@org.glassfish.api.admin.ExecuteOn(RuntimeType.ALL)
@Service(name=SM_CREATE_COMMAND_NAME)
@PerLookup
@I18n("create.connector.security.map")
public class CreateConnectorSecurityMap extends ConnectorSecurityMap implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateConnectorSecurityMap.class);

    @Param(optional = true, obsolete = true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(name = SM_POOL_NAME)
    private String poolName;

    @Param(name = SM_PRINCIPALS, optional = true)
    private List<String> principals;

    @Param(name = SM_USER_GROUPS, optional = true)
    private List<String> userGroups;

    @Param(name = SM_MAPPED_NAME)
    private String mappedusername;

    @Param(name=SM_MAPPED_PASSWORD, password = true, optional = true)
    private String mappedpassword;

    @Param(name = SM_MAP_NAME, primary = true)
    private String securityMapName;

    @Inject
    private Domain domain;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
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

        if (principals == null && userGroups == null) {
            report.setMessage
                    (localStrings.getLocalString("create.connector.security.map.noPrincipalsOrGroupsMap",
                    "Either the principal or the user group has to be specified while creating a security map." +
                            " Both cannot be null."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (principals != null && userGroups != null) {
            report.setMessage(localStrings.getLocalString("create.connector.security.map.specifyPrincipalsOrGroupsMap",
                    "A work-security-map can have either (any number of) group mapping or (any number of) principals" +
                            " mapping but not both. Specify --principals or --usergroups."));
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

        if (doesMapNameExist(poolName, securityMapName, ccPools)) {
            report.setMessage(localStrings.getLocalString("create.connector.security.map.duplicate",
                    "A security map named {0} already exists for connector connection pool {1}. Please give a" +
                            " different map name.",
                    securityMapName, poolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        //get all the security maps for this pool.....
        List<SecurityMap> maps = getAllSecurityMapsForPool(poolName, ccPools);

        if (principals != null) {
            for (String principal : principals) {
                if (isPrincipalExisting(principal, maps)) {
                    report.setMessage(localStrings.getLocalString("create.connector.security.map.principal_exists",
                            "The principal {0} already exists in connector connection pool {1}. Please give a " +
                                    "different principal name.",
                            principal, poolName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }
        if (userGroups != null) {
            for (String userGroup : userGroups) {
                if (isUserGroupExisting(userGroup, maps)) {
                    report.setMessage(localStrings.getLocalString("create.connector.security.map.usergroup_exists",
                            "The user-group {0} already exists in connector connection pool {1}. Please give a" +
                                    " different user-group name.",
                            userGroup, poolName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        ConnectorConnectionPool connPool = null;
        for (ConnectorConnectionPool ccp : ccPools) {
            if (ccp.getName().equals(poolName)) {
                connPool = ccp;
            }
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<ConnectorConnectionPool>() {

                public Object run(ConnectorConnectionPool ccp) throws PropertyVetoException, TransactionFailure {

                    List<SecurityMap> securityMaps = ccp.getSecurityMap();

                    SecurityMap newResource = ccp.createChild(SecurityMap.class);
                    newResource.setName(securityMapName);

                    if (principals != null) {
                        for (String p : principals) {
                            newResource.getPrincipal().add(p);
                        }
                    }

                    if (userGroups != null) {
                        for (String u : userGroups) {
                            newResource.getUserGroup().add(u);
                        }
                    }

                    BackendPrincipal backendPrincipal = newResource.createChild(BackendPrincipal.class);
                    backendPrincipal.setUserName(mappedusername);
                    if (mappedpassword != null && !mappedpassword.isEmpty()) {
                        backendPrincipal.setPassword(mappedpassword);
                    }
                    newResource.setBackendPrincipal(backendPrincipal);
                    securityMaps.add(newResource);
                    return newResource;
                }
            }, connPool);

        } catch (TransactionFailure tfe) {
            Object params[] = {securityMapName, poolName};
            report.setMessage(localStrings.getLocalString("create.connector.security.map.fail",
                    "Unable to create connector security map {0} for connector connection pool {1} ", params) +
                    " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
            return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
