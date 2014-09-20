/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.connectors.config.GroupMap;
import org.glassfish.connectors.config.PrincipalMap;
import org.glassfish.connectors.config.WorkSecurityMap;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import static org.glassfish.connectors.admin.cli.CLIConstants.DESCRIPTION;
import static org.glassfish.connectors.admin.cli.CLIConstants.WSM.*;

/**
 * Create Connector Work Security Map
 *
 */
@ExecuteOn(RuntimeType.ALL)
@Service(name="create-connector-work-security-map")
@PerLookup
@I18n("create.connector.work.security.map")
public class CreateConnectorWorkSecurityMap implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateConnectorWorkSecurityMap.class);

    @Param(name=WSM_RA_NAME)
    private String raName;

    @Param(name=WSM_PRINCIPALS_MAP, optional=true)
    private Properties principalsMap;

    @Param(name = WSM_GROUPS_MAP, optional=true)
    private Properties groupsMap;

    @Param(name=DESCRIPTION, optional=true)
    private String description;

    @Param(name= WSM_MAP_NAME, primary=true)
    private String mapName;

    @Inject
    private Domain domain;

    @Inject
    private Applications applications;
    

    //TODO common code replicated in ConnectorWorkSecurityMapManager
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (mapName == null) {
            report.setMessage(localStrings.getLocalString(
                    "create.connector.work.security.map.noMapName",
                    "No mapname defined for connector work security map."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (raName == null) {
            report.setMessage(localStrings.getLocalString(
                    "create.connector.work.security.map.noRaName",
                    "No raname defined for connector work security map."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (principalsMap == null && groupsMap == null) {
            report.setMessage(localStrings.getLocalString(
                    "create.connector.work.security.map.noMap",
                    "No principalsmap or groupsmap defined for connector work security map."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (principalsMap != null && groupsMap != null) {
            report.setMessage(localStrings.getLocalString(
                    "create.connector.work.security.map.specifyPrincipalsOrGroupsMap",
                    "A work-security-map can have either (any number of) group mapping  " +
                    "or (any number of) principals mapping but not both. Specify" +
                    "--principalsmap or --groupsmap."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // ensure we don't already have one of this name
        if (hasDuplicate(domain.getResources(), report)) return;

        //TODO ASR : need similar validation while creating app-scoped-resource of w-s-m
        String appName = raName;
        if (!ConnectorsUtil.isStandAloneRA(raName)) {
            appName = ConnectorsUtil.getApplicationNameOfEmbeddedRar(raName);

            Application application = applications.getApplication(appName);
            if(application != null){

                //embedded RAR
                String resourceAdapterName = ConnectorsUtil.getRarNameFromApplication(raName);
                Module module = application.getModule(resourceAdapterName);
                if(module != null){
                    Resources msr = module.getResources();
                    if(msr != null){
                        if(hasDuplicate(msr, report)) return;
                    }
                }
            }
        }else{
            //standalone RAR
            Application application = applications.getApplication(appName);
            if(application != null){
                Resources appScopedResources = application.getResources();
                if(appScopedResources != null){
                    if(hasDuplicate(appScopedResources, report)) return;
                }
            }
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Resources>() {

                public Object run(Resources param) throws PropertyVetoException,
                        TransactionFailure {

                    WorkSecurityMap workSecurityMap =
                            param.createChild(WorkSecurityMap.class);
                    workSecurityMap.setName(mapName);
                    workSecurityMap.setResourceAdapterName(raName);

                    if (principalsMap != null) {
                        for (Map.Entry e : principalsMap.entrySet()) {
                            PrincipalMap principalMap = workSecurityMap.createChild(PrincipalMap.class);
                            principalMap.setEisPrincipal((String)e.getKey());
                            principalMap.setMappedPrincipal((String)e.getValue());
                            workSecurityMap.getPrincipalMap().add(principalMap);
                        }
                    } else if (groupsMap != null) {
                        for (Map.Entry e : groupsMap.entrySet()) {
                            GroupMap groupMap = workSecurityMap.createChild(GroupMap.class);
                            groupMap.setEisGroup((String)e.getKey());
                            groupMap.setMappedGroup((String)e.getValue());
                            workSecurityMap.getGroupMap().add(groupMap);
                        }
                    } else {
                        // no mapping
                    }

                    param.getResources().add(workSecurityMap);
                    return workSecurityMap;
                }
            }, domain.getResources());
            
        } catch (TransactionFailure tfe) {
            Logger.getLogger(CreateConnectorWorkSecurityMap.class.getName()).log(Level.SEVERE,
                    "create-connector-work-security-map failed", tfe);
            report.setMessage(localStrings.getLocalString(
                    "create.connector.work.security.map.fail",
                    "Unable to create connector work security map {0}.", mapName) +
                    " " + tfe.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
            return;
        }
        
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private boolean hasDuplicate(Resources resources, ActionReport report) {
        for (Resource resource : resources.getResources()) {
            if (resource instanceof WorkSecurityMap) {
                if (((WorkSecurityMap) resource).getName().equals(mapName) &&
                    ((WorkSecurityMap) resource).getResourceAdapterName().equals(raName)){
                    report.setMessage(localStrings.getLocalString(
                            "create.connector.work.security.map.duplicate",
                            "A connector work security map named {0} for resource adapter {1} already exists.",
                            mapName, raName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return true;
                }
            }
        }
        return false;
    }
}
