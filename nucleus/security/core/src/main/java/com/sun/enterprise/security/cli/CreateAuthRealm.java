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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.security.cli;

import static com.sun.enterprise.security.cli.CLIUtil.chooseConfig;
import static com.sun.enterprise.security.cli.CLIUtil.isRealmNew;
import static com.sun.enterprise.util.SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import static org.glassfish.api.ActionReport.ExitCode.WARNING;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;

import java.beans.PropertyVetoException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.SecurityConfigListener;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.util.LocalStringManagerImpl;
import javax.security.auth.login.Configuration;


/**
 * CLI command to create Authentication Realm
 *
 * Usage: create-auth-realm --classname realm_class [--terse=false]
 *        [--interactive=true] [--host localhost] [--port 4848|4849]
 *        [--secure | -s] [--user admin_user] [--passwordfile file_name]
 *        [--property (name=value)[:name=value]*]
 *        [--echo=false] [--target target(Default server)] auth_realm_name
 *
 * domain.xml element example
 * {@code
 * <auth-realm name="file"
 *   classname="com.sun.enterprise.security.auth.realm.file.FileRealm">
 *   <property name="file" value="${com.sun.aas.instanceRoot}/config/keyfile"/>
 *   <property name="jaas-context" value="fileRealm"/>
 * </auth-realm>
 * }
 *       Or
 * {@code
 * <auth-realm name="certificate"
 *   classname="com.sun.enterprise.security.auth.realm.certificate.CertificateRealm">
 * </auth-realm>
 * }
 *
 * @author Nandini Ektare
 */
@Service(name="create-auth-realm")
@PerLookup
@I18n("create.auth.realm")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({DAS,STANDALONE_INSTANCE, CONFIG})
public class CreateAuthRealm implements AdminCommand, AdminCommandSecurity.Preauthorization {

    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateAuthRealm.class);

    @Param(name = "classname")
    private String className;

    @Param(name = "authrealmname", primary = true)
    private String authRealmName;

    @Param(optional = true, name = "property", separator = ':')
    private Properties properties;

    @Param(name = "target", optional = true, defaultValue = DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Param(name = "login-module", optional = true)
    private String loginModule;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    // initialize the habitat in Util needed by Realm classes
    @Inject
    private Util util;

    @AccessRequired.NewChild(type = AuthRealm.class)
    private SecurityService securityService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            return false;
        }

        securityService = config.getSecurityService();

        return ensureRealmIsNew(context.getActionReport());
    }

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and the
     * values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        // No duplicate auth realms found. So add one.
        try {
            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {

                public Object run(SecurityService param) throws PropertyVetoException, TransactionFailure {
                    AuthRealm newAuthRealm = param.createChild(AuthRealm.class);
                    populateAuthRealmElement(newAuthRealm);
                    param.getAuthRealm().add(newAuthRealm);

                    // In case of cluster instances, this is required to
                    // avoid issues with the listener's callback method
                    SecurityConfigListener.authRealmCreated(config, newAuthRealm);

                    return newAuthRealm;
                }
            }, securityService);
            if (loginModule != null) {
                appendLoginModule(report);
                if (report.hasFailures()) {
                    report.setActionExitCode(WARNING);
                    return;
                }
            }
        } catch (TransactionFailure e) {
            report.setMessage(
                    localStrings.getLocalString(
                            "create.auth.realm.fail",
                            "Creation of Authrealm {0} failed", authRealmName) + "  " +
                    e.getLocalizedMessage());
            report.setActionExitCode(FAILURE);
            report.setFailureCause(e);
            return;
        }

        report.setActionExitCode(SUCCESS);
    }

    private void appendLoginModule(ActionReport mainReport) {
        ActionReport report = mainReport.addSubActionsReport();
        report.setActionDescription("Updating login config");

        String loginConfLocation = System.getProperty("java.security.auth.login.config");
        if (loginConfLocation == null) {
            report.appendMessage(localStrings.getLocalString("create.auth.realm.loginconf.undefined",
                    "JDK default login config is set. Cannot update"));
            report.setActionExitCode(FAILURE);
            return;
        }

        String jaasContext = properties.getProperty("jaas-context");
        if (jaasContext == null || jaasContext.isEmpty()) {
            report.appendMessage(localStrings.getLocalString("create.auth.realm.loginconf.nojaasctx",
                    "No JAAS context is defined for login module"));
            report.setActionExitCode(FAILURE);
            return;
        }

        try {
            new LoginContext(jaasContext);
            report.appendMessage(localStrings.getLocalString("create.auth.realm.loginconf.jaasctx.already.defined",
                    "JAAS context {0} is already configured", jaasContext));
            report.setActionExitCode(FAILURE);
            return;
        } catch (LoginException e) {
            // Login Context will throw when initialized with unknown jaas context, which is exactly what we need.
        }

        try (FileWriter fw = new FileWriter(loginConfLocation, true)) {
            fw.append("\n")
                .append(jaasContext).append(" {\n")
                .append("\t").append(loginModule).append(" required;\n")
                .append("};");

        } catch (IOException e) {
            report.appendMessage(localStrings.getLocalString("create.auth.realm.loginconf.write_failed",
                    "Failed to update login conf at {0}: {1}", loginConfLocation, e.getLocalizedMessage()));
            report.setFailureCause(e);
            report.setActionExitCode(FAILURE);
        }
        Configuration.getConfiguration().refresh();
    }

    private void populateAuthRealmElement(AuthRealm newAuthRealm) throws PropertyVetoException, TransactionFailure {
        newAuthRealm.setName(authRealmName);
        newAuthRealm.setClassname(className);

        if (properties != null) {
            for (Object propertyName : properties.keySet()) {
                Property newProperty = newAuthRealm.createChild(Property.class);
                newProperty.setName((String) propertyName);

                newProperty.setValue(properties.getProperty((String) propertyName));
                newAuthRealm.getProperty().add(newProperty);
            }
        }
    }

    private boolean ensureRealmIsNew(ActionReport report) {
        if (!isRealmNew(securityService, authRealmName)) {
            report.setMessage(localStrings.getLocalString(
                    "create.auth.realm.duplicatefound",
                    "Authrealm named {0} exists. Cannot add duplicate AuthRealm.",
                    authRealmName));

            report.setActionExitCode(FAILURE);
            return false;
        }

        return true;
    }
}
