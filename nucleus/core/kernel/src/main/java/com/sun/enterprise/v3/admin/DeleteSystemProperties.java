/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.config.serverbeans.Domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Delete System Properties Command
 * <p>
 * Removes one or more system properties of the domain, configuration, cluster, or server instance.
 * <p>
 * Usage: delete-system-properties [--target target(Default server)] property_name[:property_name]*
 */
@Service(name = "delete-system-properties")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE,
        CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE})
@I18n("delete.system.properties")
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.DELETE,
                path = "delete-system-properties", description = "Delete System Properties")
})
public class DeleteSystemProperties implements AdminCommand,
        AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteSystemProperties.class);

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "property_name", primary = true, separator = ':')
    List<String> propNames;

    @Inject
    DeleteSystemProperty deleteSystemProperty;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        deleteSystemProperty.target = this.target;
        return deleteSystemProperty.preAuthorization(context);
    }

    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        return deleteSystemProperty.getAccessChecks();
    }

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        List<String> deletedProps = new ArrayList<>();
        List<String> failedProps = new ArrayList<>();

        for (String propName : propNames) {
            deleteSystemProperty.propName = propName;
            deleteSystemProperty.target = this.target;
            // Required to initialize deleteSystemProperty.spb before execute(); not just an auth gate
            if (!deleteSystemProperty.preAuthorization(context)) {
                failedProps.add(propName);
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                continue;
            }
            deleteSystemProperty.execute(context);
            if (report.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
                failedProps.add(propName);
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            } else {
                deletedProps.add(propName);
            }
        }

        if (failedProps.isEmpty()) {
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage(localStrings.getLocalString("delete.sysprops.multiple.ok",
                    "System Properties {0} deleted from target {1}. Make sure you check their references.",
                    deletedProps.toString(), target));
        } else if (deletedProps.isEmpty()) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(localStrings.getLocalString("delete.sysprops.multiple.failed",
                    "Failed to delete the following system properties: {0}", failedProps.toString()));
        } else {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(localStrings.getLocalString("delete.sysprops.partial.failed",
                    "Deleted {0} but failed to delete the following system properties: {1}",
                    deletedProps.toString(), failedProps.toString()));
        }
    }
}