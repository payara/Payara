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

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.connectors.config.ResourceAdapterConfig;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.connectors.admin.cli.CLIConstants.*;
import static org.glassfish.connectors.admin.cli.CLIConstants.RAC.*;
import static org.glassfish.resources.admin.cli.ResourceConstants.RESOURCE_ADAPTER_CONFIG_NAME;
import static org.glassfish.resources.admin.cli.ResourceConstants.THREAD_POOL_IDS;

/**
 * Create RA Config Command
 *
 */
@ExecuteOn(RuntimeType.ALL)
@Service(name=RAC_CREATE_RAC_COMMAND)
@PerLookup
@I18n("create.resource.adapter.config")
public class CreateResourceAdapterConfig implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateResourceAdapterConfig.class);

    @Param(name=RAC_RA_NAME, primary=true)
    private String raName;

    @Param(name=PROPERTY, optional=true, separator=':')
    private Properties properties;

    @Param(name=TARGET, optional=true, obsolete = true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(name=RAC_THREAD_POOL_ID, optional=true, alias="threadPoolIds")
    private String threadPoolIds;

    @Param(name=OBJECT_TYPE, defaultValue="user", optional=true)
    private String objectType;

    @Inject
    private Applications applications;

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

        HashMap attrList = new HashMap();
        attrList.put(RESOURCE_ADAPTER_CONFIG_NAME, raName);
        //attrList.put("name", name);
        attrList.put(THREAD_POOL_IDS, threadPoolIds);
        attrList.put(ServerTags.OBJECT_TYPE, objectType);

        ResourceStatus rs;

        //TODO ASR : need similar validation while creating app-scoped-resource of resource-adapter-config
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

        ResourceAdapterConfigManager resAdapterConfigMgr = new ResourceAdapterConfigManager();
        try {
            rs = resAdapterConfigMgr.create(domain.getResources(), attrList, properties, target);
        } catch (Exception ex) {
            Logger.getLogger(CreateResourceAdapterConfig.class.getName()).log(
                    Level.SEVERE,
                    "Unable to create resource adapter config for " + raName, ex);
            String def = "Resource adapter config: {0} could not be created, reason: {1}";
            report.setMessage(localStrings.getLocalString("create.resource.adapter.config.fail",
                    def, raName) + " " + ex.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(ex);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if (rs.getMessage() != null) {
                report.setMessage(rs.getMessage());
            } else {
                 report.setMessage(localStrings.getLocalString("create.resource.adapter.config.fail",
                    "Resource adapter config {0} creation failed", raName, ""));
            }
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        }
        report.setActionExitCode(ec);
    }

    private boolean hasDuplicate(Resources resources, ActionReport report) {
        if(ConnectorsUtil.getResourceByName(resources, ResourceAdapterConfig.class, raName) != null){
            String msg = localStrings.getLocalString("create.resource.adapter.config.duplicate",
                    "Resource adapter config already exists for RAR", raName);
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return true;
        }
        return false;
    }
}
