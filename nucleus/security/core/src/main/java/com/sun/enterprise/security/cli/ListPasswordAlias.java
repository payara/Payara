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
import java.util.Iterator;
import org.glassfish.api.admin.AccessRequired;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;

/**
 * List Password Aliases Command
 *
 * Usage: list-password-aliases [--terse=false] [--echo=false]
 *        [--interactive=true] [--host localhost] [--port 4848|4849]
 *        [--secure | -s] [--user admin_user] [--passwordfile file_name]
 *
 * Result of the command is that:
 * <domain-dir>/<domain-name>/config/domain-passwords file gets appended with
 * the entry of the form: aliasname=<password encrypted with masterpassword>
 *
 * A user can use this aliased password now in setting passwords in domin.xml.
 * Benefit is it is in NON-CLEAR-TEXT
 *
 * domain.xml example entry is:
 * <provider-config class-name="com.sun.xml.wss.provider.ClientSecurityAuthModule"
 *                  provider-id="XWS_ClientProvider" provider-type="client">
 *      <property name="password" value="${ALIAS=myalias}/>
 * </provider-config>
 *
 * @author Nandini Ektare
 */

@Service(name="list-password-aliases")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.password.alias")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.DOMAIN})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-password-aliases", 
        description="list-password-aliases")
})
@AccessRequired(resource="domain/passwordAliases", action="read")
public class ListPasswordAlias implements AdminCommand {

    private final static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(ListPasswordAlias.class);

    @Inject
    private DomainScopedPasswordAliasStore domainPasswordAliasStore;


    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        try {
            final Iterator<String> it = domainPasswordAliasStore.keys();

            if (! it.hasNext()) {
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                report.setMessage(localStrings.getLocalString(
                    "list.password.alias.nothingtolist",
                    "Nothing to list"));
            }
            
            while (it.hasNext()) {
                ActionReport.MessagePart part =
                    report.getTopMessagePart().addChild();
                part.setMessage(it.next());
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            report.setMessage(localStrings.getLocalString(
               "list.password.alias.fail", "Listing of Password Alias failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(ex);
            return;
        }
    }
}
