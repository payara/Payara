/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.net.URI;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.deployment.common.DeploymentUtils;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;

@Service(name="_mt-provision")
@org.glassfish.api.admin.ExecuteOn(value={RuntimeType.DAS})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
public class MTProvisionCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(optional=true)
    public File customizations = null;

    @Param
    public String tenant = null;

    @Param
    public String contextroot = null;

    @Param(primary=true)
    public String appname;

    @Inject
    Domain domain;

    @Inject
    Applications applications;

    @Inject
    Deployment deployment;

    @Inject
    ArchiveFactory archiveFactory;
    
    private Application app;
    private ApplicationRef appRef;

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        app = applications.getApplication(appname);
        if (app != null) {
            accessChecks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(app), "provision"));
            appRef = domain.getApplicationRefInTarget(appname, DeploymentUtils.DAS_TARGET_NAME);
            if (appRef != null) {
                accessChecks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(appRef), "provision"));
            }
        }
        return accessChecks;
    }
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(MTProvisionCommand.class);

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        final Logger logger = context.getLogger();

        if (app == null) {
            report.setMessage("Application " + appname + " needs to be deployed first before provisioned to tenant");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ReadableArchive archive = null;

        DeployCommandParameters commandParams = app.getDeployParameters(appRef);

        commandParams.contextroot = contextroot;
        commandParams.target = DeploymentUtils.DAS_TARGET_NAME;
        commandParams.name = DeploymentUtils.getInternalNameForTenant(appname, tenant);
        commandParams.enabled = Boolean.TRUE;
        commandParams.origin = DeployCommandParameters.Origin.mt_provision;

        try {
            URI uri = new URI(app.getLocation());
            File file = new File(uri);

            if (!file.exists()) {
                throw new Exception(localStrings.getLocalString("fnf", "File not found", file.getAbsolutePath()));
            }

            archive = archiveFactory.openArchive(file);

            ExtendedDeploymentContext deploymentContext =
                deployment.getBuilder(logger, commandParams, report).
                    source(archive).build();

            Properties appProps = deploymentContext.getAppProps();
            appProps.putAll(app.getDeployProperties());

            // some container code is accessing context root through
            // app props so we also need to override that
            if (contextroot!=null) {
                appProps.setProperty(ServerTags.CONTEXT_ROOT, contextroot);
            }

            deploymentContext.setModulePropsMap(app.getModulePropertiesMap());

            deploymentContext.setTenant(tenant, appname);

            expandCustomizationJar(deploymentContext.getTenantDir());
            deployment.deploy(deploymentContext);

            deployment.registerTenantWithAppInDomainXML(appname, deploymentContext);
        } catch(Throwable e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
            report.setFailureCause(e);
        } finally {
            try {
                if (archive != null) {
                    archive.close();
                }
            } catch(IOException e) {
                // ignore
            }
        }
    }

    private void expandCustomizationJar(File tenantDir) throws IOException {
        if (!tenantDir.exists() && !tenantDir.mkdirs()) {
             // TODO Handle this situation properly -- issue reported by findbugs
        }

        if (customizations == null) {
            return;
        }

        ReadableArchive cusArchive = null;
        WritableArchive expandedArchive = null;

        try {
            expandedArchive = archiveFactory.createArchive(tenantDir);
            cusArchive = archiveFactory.openArchive(customizations);
            DeploymentUtils.expand(cusArchive, expandedArchive);
        } finally {
            try {
                if (cusArchive != null) {
                    cusArchive.close();
                }
                if (expandedArchive != null) {
                    expandedArchive.close();
                }
            } catch(IOException e) {
                // ignore
            }
        }
    }
}
