/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.audit.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;

import fish.payara.audit.AuditLevel;
import fish.payara.audit.AdminAuditConfiguration;
import fish.payara.audit.AdminAuditService;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Sets the Admin Audit Configuration
 *
 * @author jonathan coustick
 * @since 5.192
 */
@Service(name = "set-admin-audit-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG,
    CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = AdminAuditConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-admin-audit-configuration",
            description = "Sets the Configuration for the Admin Audit Service")
})
public class SetAdminAuditConfiguration implements AdminCommand {

    private static final Logger LOGGER = Logger.getLogger(SetAdminAuditConfiguration.class.getPackage().toString());

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "enabled")
    private Boolean enabled;

    @Param(name = "auditLevel", optional = true, acceptableValues = "MODIFIERS, ACCESSORS, INTERNAL")
    private String auditLevel;
    
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;
    private Config targetConfig;

    @Inject
    private AdminAuditService auditService;

    @Inject
    private Target targetUtil;
    
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        
        targetConfig = targetUtil.getConfig(target);

        final AdminAuditConfiguration configuration = targetConfig.getExtensionByType(AdminAuditConfiguration.class);

        try {
            ConfigSupport.apply(new SingleConfigCode<AdminAuditConfiguration>() {
                @Override
                public Object run(AdminAuditConfiguration configurationProxy) throws PropertyVetoException, TransactionFailure {

                    configurationProxy.enabled(enabled.toString());
                    configurationProxy.setAuditLevel(auditLevel);
                    return null;
                }
            }, configuration);

            if (dynamic && target.equals("server-config")) {
                auditService.setEnabled(enabled);
                auditService.setAuditLevel(AuditLevel.valueOf(auditLevel));
            }
        } catch (TransactionFailure ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            report.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

}
