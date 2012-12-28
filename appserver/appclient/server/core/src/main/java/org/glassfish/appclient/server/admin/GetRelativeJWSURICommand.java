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

package org.glassfish.appclient.server.admin;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.appclient.server.core.AppClientDeployer;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Returns the path part (not host or port) of the URI for launching
 * an app client using Java Web Start.
 * <p>
 * Used primarily from the admin console to support the Java Web Start
 * client launch feature.
 *
 * @author Tim Quinn
 */
@Service(name="_get-relative-jws-uri")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="_get-relative-jws-uri", 
        description="Get Relative JWS URI",
        params={
            @RestParam(name="appname", value="$parent")
        },
        useForAuthorization=true)
})
public class GetRelativeJWSURICommand implements AdminCommand {

    private static final String APPNAME_OPTION = "appname";
    private static final String MODULENAME_OPTION = "modulename";
    private static final String URI_PROPERTY_NAME = "relative-uri";

    private final static LocalStringManager localStrings =
            new LocalStringManagerImpl(GetRelativeJWSURICommand.class);

    @Param(name = APPNAME_OPTION, optional=false)
    public String appname;

    @Param(name = MODULENAME_OPTION, optional=false)
    public String modulename;

    @Inject
    private AppClientDeployer appClientDeployer;
    
    @Inject
    private Applications apps;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        
        final Application app = apps.getApplication(appname);
        if (app != null) {
            Module appClient = app.getModule(modulename);
            if (appClient == null) {
                appClient = app.getModule(modulename + ".jar");
            }
            if (appClient != null) {
                String result = appClient.getPropertyValue("jws.user.friendly.path");
                /*
                 * For stand-alone app clients the property is stored at the
                 * application level instead of the module level.
                 */
                if (result == null) {
                    result = app.getPropertyValue("jws.user.friendly.path");
                }
                if (result != null) {
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    report.getTopMessagePart().addProperty(URI_PROPERTY_NAME, result);
                    report.setMessage(result);
                    return;
                }
            }
        }
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(localStrings.getLocalString(
                this.getClass(),
                "getreljwsuri.appOrModuleNotFound",
                "Could not find application {0}, module {1}",
                new Object[] {appname, modulename}));
    }

}
