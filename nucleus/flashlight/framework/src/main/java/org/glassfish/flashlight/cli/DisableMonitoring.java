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

package org.glassfish.flashlight.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.config.support.*;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import org.glassfish.api.monitoring.ContainerMonitoring;
import org.glassfish.hk2.api.PerLookup;

import javax.inject.Inject;

/**
 * @author Sreenivas Munnangi (3.0)
 * @author Byron Nevins (3.1+)
 */
@Service(name="disable-monitoring")
@PerLookup
@I18n("disable.monitoring")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="disable-monitoring", 
        description="disable-monitoring")
})
public class DisableMonitoring implements AdminCommand, AdminCommandSecurity.Preauthorization {

    // do NOT inject this.  We may need it for a different config tha ours.
    private MonitoringService ms;

    @Inject
    private Target targetService;

    @Param(name="target", optional=true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    // list of modules separated by comma
    @Param(optional=true)
    private String modules;

    final private LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(DisableMonitoring.class);

    @AccessRequired.To("update")
    private Config targetConfig = null;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        try {
            targetConfig = targetService.getConfig(target);
            if (targetConfig != null) {
                ms = targetConfig.getMonitoringService();
                return true;
            }
            fail(report, "unknown.target", "Could not find target {0}", target);
            return false;
        }
        catch (Exception e) {
            fail(report, "target.service.exception",
                    "Encountered exception trying to locate the MonitoringService element "
                    + "in the target ({0}) configuration: {1}", target, e.getMessage());
            return false;
        }
    }
    
    private void fail(final ActionReport report, final String messageKey,
            final String fallbackMessageText, final Object... args) {
        report.setMessage(localStrings.getLocalString(messageKey, fallbackMessageText, args));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
    }
    
    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();

        try {
            if ((modules == null) || (modules.length() < 1)) {
                // check if it is already false
                boolean enabled = Boolean.parseBoolean(ms.getMonitoringEnabled());
                // set overall monitoring-enabled to false
                if (enabled) {
                    MonitoringConfig.setMonitoringEnabled(ms, "false", report);
                } else {
                    report.setMessage(
                        localStrings.getLocalString("disable.monitoring.alreadyfalse",
                        "monitoring-enabled is already set to false"));
                }
            } else {
                // for each module set monitoring level to OFF
                String[] strArr = modules.split(":");
                for (String moduleName: strArr) {
                    if (moduleName.length() > 0) {
                        MonitoringConfig.setMonitoringLevel(ms, moduleName, ContainerMonitoring.LEVEL_OFF, report);
                    }
                }
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("disable.monitoring.exception",
                "Encountered exception during disabling monitoring {0}", e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

}
