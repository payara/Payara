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

package org.glassfish.deployment.admin;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.Collection;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.deployment.common.Artifacts;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.versioning.VersioningSyntaxException;
import org.glassfish.deployment.versioning.VersioningUtils;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;

/**
 *
 * @author tjquinn
 */
@Service(name="get-client-stubs")
@I18n("get.client.stubs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="get-client-stubs", 
        description="Get Client Stubs",
        params={
            @RestParam(name="appname", value="$parent")
        })
})
public class GetClientStubsCommand implements AdminCommand, AdminCommandSecurity.Preauthorization {

    private final static String APPNAME = "appname";

    private final static LocalStringManager localStrings =
            new LocalStringManagerImpl(GetClientStubsCommand.class);

    @Inject
    private Applications apps;

    @Param(name = APPNAME, optional=false)
    private String appname = null;

    @Param(primary=true)
    private String localDir;
    
    @AccessRequired.To("read")
    private Application matchingApp = null;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        for (Application app : apps.getApplications()) {
            if (app.getName().equals(appname)) {
                matchingApp = app;
                return true;
            }
        }
        context.getActionReport().setMessage(localStrings.getLocalString(
            getClass(),
            "get-client-stubs.noSuchApp",
            "Application {0} was not found",
            new Object[] {appname}));
        return false;
    }
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();
        Collection<Artifacts.FullAndPartURIs> artifactInfo = DeploymentUtils.downloadableArtifacts(matchingApp).getArtifacts();

        try {
            VersioningUtils.checkIdentifier(appname);
        } catch (VersioningSyntaxException ex) {
            report.failure(logger,ex.getMessage());
            return;
        }

        if (artifactInfo.size() == 0) {
            report.setMessage(localStrings.getLocalString(
                getClass(),
                "get-client-stubs.noStubApp",
                "there are no files to retrieve for application {0}",
                new Object[] {appname}));
            return;
        }

        try {
            DeployCommand.retrieveArtifacts(context, matchingApp, localDir);
        } catch (Exception e) {
            report.setFailureCause(e);
            report.failure(logger, localStrings.getLocalString(
                    getClass(),
                    "get-client-stubs.errorPrepDownloadedFiles",
                    "Error preparing for download"), e);
        }
    }
}
