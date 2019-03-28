/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.appserver.microprofile.service.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import static fish.payara.appserver.microprofile.service.MicroProfileService.DEFAULT_GROUP_NAME;
import static fish.payara.appserver.microprofile.service.security.MicroProfileAuthModule.DEFAULT_USER_NAME;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import javax.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author Gaurav Gupta
 */
public abstract class SetMicroProfileConfigurationCommand implements AdminCommand {

    @Param(optional = true, defaultValue = "server-config")
    protected String target;

    @Param(optional = true)
    protected Boolean enabled;

    @Param(optional = true)
    protected String endpoint;

    @Param(optional = true, alias = "applicationname")
    protected String applicationName;

    @Param(optional = true, alias = "securityenabled")
    protected Boolean securityEnabled;

    @Param(optional = true)
    protected Boolean dynamic;

    @Param(optional = true, alias = "virtualservers")
    protected String virtualServers;

    @Inject
    protected Domain domain;

    @Inject
    protected Target targetUtil;

    @Inject
    ServiceLocator habitat;

    @Inject
    ServerEnvironment serverEnv;

    @Inject
    CommandRunner commandRunner;

    protected boolean messageSecurityProviderExists(
            ActionReport subActionReport,
            Subject subject,
            Class<? extends ServerAuthModule> samClass) {

        boolean exists = false;

        CommandInvocation invocation
                = commandRunner.getCommandInvocation("list-message-security-providers",
                        subActionReport, subject, false);

        ParameterMap parameters = new ParameterMap();
        parameters.add("layer", "HttpServlet");

        invocation.parameters(parameters).execute();

        String authModuleName = samClass.getSimpleName();
        for (MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(authModuleName)) {
                exists = true;
                break;
            }
        }

        return exists;
    }

    protected void createRequiredMessageSecurityProvider(
            ActionReport subActionReport,
            Subject subject,
            Class<? extends ServerAuthModule> samClass) {

        CommandInvocation invocation
                = commandRunner.getCommandInvocation("create-message-security-provider",
                        subActionReport, subject, false);

        ParameterMap parameters = new ParameterMap();
        parameters.add("classname", samClass.getName());
        parameters.add("isdefaultprovider", "false");
        parameters.add("layer", "HttpServlet");
        parameters.add("providertype", "server");
        parameters.add("target", target);
        parameters.add("requestauthsource", "sender");
        parameters.add("DEFAULT", samClass.getSimpleName());

        invocation.parameters(parameters).execute();
    }

    protected boolean defaultMicroProfileUserExists(ActionReport subActionReport, Subject subject) {
        boolean exists = false;

        CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "list-file-users",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("authrealmname", "file");

        invocation.parameters(parameters).execute();

        for (MessagePart message : subActionReport.getTopMessagePart().getChildren()) {
            if (message.getMessage().equals(DEFAULT_USER_NAME)) {
                exists = true;
                break;
            }
        }

        return exists;
    }

    protected void createDefaultMicroProfileUser(ActionReport subActionReport, Subject subject) {
        CommandInvocation invocation
                = commandRunner.getCommandInvocation(
                        "create-file-user",
                        subActionReport,
                        subject,
                        false
                );

        ParameterMap parameters = new ParameterMap();
        parameters.add("groups", DEFAULT_GROUP_NAME);
        parameters.add("userpassword", "microprofile");
        parameters.add("target", target);
        parameters.add("authrealmname", "file");
        parameters.add("DEFAULT", DEFAULT_USER_NAME);

        invocation.parameters(parameters).execute();
    }

}
