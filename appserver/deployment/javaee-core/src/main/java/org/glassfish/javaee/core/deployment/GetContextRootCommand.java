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

package org.glassfish.javaee.core.deployment;

import org.glassfish.api.Param;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.DeploymentProperties;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;


/**
 * Get context root command
 */
@Service(name="_get-context-root")
@ExecuteOn(value={RuntimeType.DAS})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=com.sun.enterprise.config.serverbeans.Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="get-context-root", 
        description="Get Context Root",
        params={
            @RestParam(name="appname", value="$parent")
        })
})
public class GetContextRootCommand implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(GetContextRootCommand.class);

    @Param(primary=true)
    private String modulename = null;

    @Param(optional=true)
    private String appname = null;

    @Inject
    ApplicationRegistry appRegistry;

    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();

        ApplicationInfo appInfo = appRegistry.get(appname);
        if (appInfo != null) {
            Application app = appInfo.getMetaData(Application.class);
            if (app != null) {
                // strip the version suffix (delimited by colon), if present
                int versionSuffix = modulename.indexOf(':');
                String versionLessModuleName = versionSuffix > 0 ? modulename.substring(0, versionSuffix) : modulename;
                BundleDescriptor bundleDesc = app.getModuleByUri(versionLessModuleName);
                if (bundleDesc != null &&
                    bundleDesc instanceof WebBundleDescriptor) {
                    String contextRoot = ((WebBundleDescriptor)bundleDesc).getContextRoot();
                    part.addProperty(DeploymentProperties.CONTEXT_ROOT, contextRoot);
                    part.setMessage(contextRoot);
                }
            }
        }
    }
}
