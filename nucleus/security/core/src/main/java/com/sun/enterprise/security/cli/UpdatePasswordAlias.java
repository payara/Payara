/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;

/**
 * Update Password Alias Command
 *
 * Usage: update-password-alias [--terse=false] [--echo=false]
 *        [--interactive=true] [--host localhost] [--port 4848|4849]
 *        [--secure | -s] [--user admin_user] [--passwordfile file_name] aliasname
 *
 * Result of the command is that:
 * the entry of the form: aliasname=<password-encrypted-with-masterpassword> in
 * <domain-dir>/<domain-name>/config/domain-passwords file gets updated with the
 * new alias password
 *
 * domain.xml example entry is:
 * <provider-config class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule"
 *                  provider-id="XWS_ClientProvider" provider-type="client">
 *      <property name="password" value="${ALIAS=myalias}/>
 * </provider-config>
 *
 * @author Nandini Ektare
 */

@Service(name="update-password-alias")
@PerLookup
@I18n("update.password.alias")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.DOMAIN})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="update-password-alias", 
        description="update-password-alias")
})
@AccessRequired(resource="domain/passwordAliases/passwordAlias/$aliasName", action="update")
public class UpdatePasswordAlias implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreatePasswordAlias.class);

    @Param(name="aliasname", primary=true)
    private String aliasName;

    @Param(name="aliaspassword", password=true)
    private String aliasPassword;

    @Inject
    private DomainScopedPasswordAliasStore domainPasswordAliasStore;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        try {
            if ( ! domainPasswordAliasStore.containsKey(aliasName)) {
                report.setMessage(localStrings.getLocalString(
                    "update.password.alias.notfound",
                    "Password alias for the alias {0} does not exist.",
                    aliasName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            domainPasswordAliasStore.put(aliasName, aliasPassword.toCharArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            report.setMessage(localStrings.getLocalString(
                "update.password.alias.fail",
                "Update of Password Alias {0} failed", aliasName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(ex);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage(localStrings.getLocalString(
            "update.password.alias.success",
            "Encrypted password for the alias {0} updated successfully",
            aliasName));
    }
}
