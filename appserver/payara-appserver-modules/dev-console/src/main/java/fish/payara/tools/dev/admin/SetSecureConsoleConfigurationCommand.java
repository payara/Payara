/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.tools.dev.admin;

import com.sun.enterprise.config.serverbeans.SecurityService;
import static fish.payara.tools.dev.admin.Constants.DEFAULT_GROUP_NAME;
import static fish.payara.tools.dev.admin.Constants.DEFAULT_USER_NAME;
import jakarta.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;

/**
 *
 * @author Gaurav Gupta
 */
public abstract class SetSecureConsoleConfigurationCommand implements AdminCommand {

    @Param(optional = true, defaultValue = "server-config")
    protected String target;
    
    @Inject
    protected CommandRunner commandRunner;
    
    @Inject
    private SecurityService securityService;

    @Param(optional = true, defaultValue = DEFAULT_GROUP_NAME)
    protected String roles;

    @Param(optional = true, alias = "securityenabled")
    protected Boolean securityEnabled;

    protected boolean defaultMicroprofileUserExists(ActionReport subActionReport, Subject subject) {
        CommandRunner.CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "list-file-users",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("authrealmname", securityService.getDefaultRealm());

        invocation.parameters(parameters).execute();

        for (ActionReport.MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(DEFAULT_USER_NAME)) {
                return true;
            }
        }

        return false;
    }

    protected void createDefaultMicroprofileUser(ActionReport subActionReport, Subject subject) {
        CommandRunner.CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "create-file-user",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("groups", roles.replace(',', ':'));
        parameters.add("userpassword", "mp");
        parameters.add("target", target);
        parameters.add("authrealmname", securityService.getDefaultRealm());
        parameters.add("DEFAULT", DEFAULT_USER_NAME);

        invocation.parameters(parameters).execute();
    }
}
