/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) 2019-2026 Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import fish.payara.internal.notification.NotifierUtils;
import fish.payara.audit.AdminAuditConfiguration;
import fish.payara.audit.AdminAuditService;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
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
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
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

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "auditLevel", optional = true, acceptableValues = "MODIFIERS, ACCESSORS, INTERNAL")
    private String auditLevel;
    
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;
    private Config targetConfig;

    @Param(name = "enableNotifiers", alias = "enable-notifiers", optional = true)
    private List<String> enableNotifiers;

    @Param(name = "disableNotifiers", alias = "disable-notifiers", optional = true)
    private List<String> disableNotifiers;

    @Param(name = "setNotifiers", alias = "set-notifiers", optional = true)
    private List<String> setNotifiers;

    @Inject
    private ServiceLocator serviceLocator;

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
            final Set<String> notifierNames = NotifierUtils.getNotifierNames(serviceLocator);
            ConfigSupport.apply(new SingleConfigCode<AdminAuditConfiguration>() {
                @Override
                public Object run(AdminAuditConfiguration proxy) throws PropertyVetoException, TransactionFailure {
                    if (enabled != null) {
                        proxy.enabled(enabled.toString());
                    }

                    if (auditLevel != null) {
                        proxy.setAuditLevel(auditLevel);
                    }


                    List<String> notifiers = proxy.getNotifierList();
                    if (enableNotifiers != null) {
                        for (String notifier : enableNotifiers) {
                            if (notifierNames.contains(notifier)) {
                                if (!notifiers.contains(notifier)) {
                                    notifiers.add(notifier);
                                }
                            } else {
                                throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                        new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                            }
                        }
                    }
                    if (disableNotifiers != null) {
                        for (String notifier : disableNotifiers) {
                            if (notifierNames.contains(notifier)) {
                                notifiers.remove(notifier);
                            } else {
                                throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                        new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                            }
                        }
                    }
                    if (setNotifiers != null) {
                        notifiers.clear();
                        for (String notifier : setNotifiers) {
                            if (notifierNames.contains(notifier)) {
                                if (!notifiers.contains(notifier)) {
                                    notifiers.add(notifier);
                                }
                            } else {
                                throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                        new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                            }
                        }
                    }
                    return null;
                }
            }, configuration);

            if (dynamic != null && dynamic && target.equals("server-config")) {
                auditService.setEnabled(enabled);
                auditService.setAuditLevel(AuditLevel.valueOf(auditLevel));

                Set<String> notifiers = auditService.getEnabledNotifiers();
                if (enableNotifiers != null) {
                    enableNotifiers.forEach(notifiers::add);
                }
                if (disableNotifiers != null) {
                    disableNotifiers.forEach(notifiers::remove);
                }
                if (setNotifiers != null) {
                    notifiers.clear();
                    setNotifiers.forEach(notifiers::add);
                }
            }
        } catch (TransactionFailure ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            report.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

}
