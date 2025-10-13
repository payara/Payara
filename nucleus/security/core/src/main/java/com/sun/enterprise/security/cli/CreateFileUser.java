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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.cli;

import com.sun.enterprise.config.serverbeans.AdminService;
import java.util.List;
import java.util.ArrayList;

import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.SystemPropertyConstants;

import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;

import java.beans.PropertyVetoException;
import java.io.File;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Create File User Command
 * Usage: create-file-user [--terse=false] [--echo=false] [--interactive=true] 
 *        [--host localhost] [--port 4848|4849] [--secure | -s] 
 *        [--user admin_user] [--userpassword admin_passwd] 
 *        [--passwordfile file_name] [--groups user_groups[:user_groups]*] 
 *        [--authrealmname authrealm_name] [--target target(Default server)] 
 *        username 
 *
 * @author Nandini Ektare
 */
@Service(name = "create-file-user")
@PerLookup
@I18n("create.file.user")
@ExecuteOn({ RuntimeType.INSTANCE, RuntimeType.DAS })
@TargetType({ DAS, STANDALONE_INSTANCE, CLUSTER, CONFIG, CommandTarget.DEPLOYMENT_GROUP })
@RestEndpoints({
        @RestEndpoint(configBean = AuthRealm.class, opType = RestEndpoint.OpType.POST, path = "create-user", description = "Create", params = {
                @RestParam(name = "authrealmname", value = "$parent") }) })
public class CreateFileUser implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateFileUser.class);

    @Param(name = "groups", optional = true, separator = ':')
    private List<String> groups = new ArrayList<String>(0); // by default, an empty list is better than a null

    // TODO: this is still a String, need to convert to char[]
    @Param(name = "userpassword", password = true)
    private String userpassword;

    @Param(name = "authrealmname", optional = true)
    private String authRealmName;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Param(name = "username", primary = true)
    private String userName;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    @Inject
    private RealmsManager realmsManager;

    @Inject
    private AdminService adminService;

    private SecureAdmin secureAdmin;

    @AccessRequired.To("update")
    private AuthRealm fileAuthRealm;

    private SecurityService securityService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            // command is executed on all instances and remote instances may not have the config
            // however this is to be expected so do not show spurious error.
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
            return false;
        }
        
        securityService = config.getSecurityService();
        fileAuthRealm = CLIUtil.findRealm(securityService, authRealmName);
        if (fileAuthRealm == null) {
            ActionReport report = context.getActionReport();
            report.setMessage(
                    localStrings.getLocalString("create.file.user.filerealmnotfound", "File realm {0} does not exist", authRealmName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        /*
         * The realm might have been defaulted, so capture the actual name.
         */
        authRealmName = fileAuthRealm.getName();
        return true;
    }

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and the
     * values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        // Get FileRealm class name, match it with what is expected.
        String fileRealmClassName = fileAuthRealm.getClassname();

        // Report error if provided impl is not the one expected
        if (fileRealmClassName != null && !fileRealmClassName.equals("com.sun.enterprise.security.auth.realm.file.FileRealm")) {
            report.setMessage(localStrings.getLocalString("create.file.user.realmnotsupported",
                    "Configured file realm {0} is not supported.", fileRealmClassName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        // Ensure we have the file associated with the authrealm
        String keyFile = null;
        for (Property fileProp : fileAuthRealm.getProperty()) {
            if (fileProp.getName().equals("file"))
                keyFile = fileProp.getValue();
        }
        
        final String kf = keyFile;
        if (keyFile == null) {
            report.setMessage(localStrings.getLocalString("create.file.user.keyfilenotfound",
                    "There is no physical file associated with this file realm {0} ", authRealmName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        boolean exists = (new File(kf)).exists();
        if (!exists) {
            report.setMessage(localStrings.getLocalString("file.realm.keyfilenonexistent",
                    "The specified physical file {0} associated with the file realm {1} does not exist.",
                    new Object[] { kf, authRealmName }));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        // Now get all inputs ready. userid and groups are straightforward but
        // password is tricky. It is stored in the file passwordfile passed
        // through the CLI options. It is stored under the name
        // AS_ADMIN_USERPASSWORD. Fetch it from there.
        final String password = userpassword; // fetchPassword(report);
        if (password == null) {
            report.setMessage(
                    localStrings.getLocalString("create.file.user.keyfilenotreadable",
                            "Password for user {0} " + "has to be specified in --userpassword option or supplied "
                                    + "through AS_ADMIN_USERPASSWORD property in the file specified " + "in --passwordfile option",
                            userName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // Issue 17525 Fix - Check for null passwords for admin-realm if secureadmin is enabled
        secureAdmin = domain.getSecureAdmin();
        if ((SecureAdmin.Util.isEnabled(secureAdmin)) && (authRealmName.equals(adminService.getAuthRealmName()))) {
            if (password.isEmpty()) {
                report.setMessage(localStrings.getLocalString("null_empty_password", "The admin user password is null or empty"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        
        // now adding user
        try {
            // even though create-file-user is not an update to the security-service
            // do we need to make it transactional by referncing the securityservice
            // hypothetically ?.
            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {

                public Object run(SecurityService param) throws PropertyVetoException, TransactionFailure {
                    try {
                        realmsManager.createRealms(config);
                        // If the (shared) keyfile is updated by an external process, load the users first
                        refreshRealm(config.getName(), authRealmName);
                        
                        FileRealm fileRealm = (FileRealm) realmsManager.getFromLoadedRealms(config.getName(), authRealmName);
                        CreateFileUser.handleAdminGroup(authRealmName, groups);
                        String[] groups1 = groups.toArray(new String[groups.size()]);
                        fileRealm.addUser(userName, password.toCharArray(), groups1);
                        fileRealm.persist();
                        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    } catch (Exception e) {
                        String localalizedErrorMsg = (e.getLocalizedMessage() == null) ? "" : e.getLocalizedMessage();
                        report.setMessage(localStrings.getLocalString("create.file.user.useraddfailed",
                                "Adding User {0} to the file realm {1} failed", userName, authRealmName) + "  " + localalizedErrorMsg);
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setFailureCause(e);
                    }
                    return null;
                }
            }, securityService);

        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("create.file.user.useraddfailed", "Adding User {0} to the file realm {1} failed",
                    userName, authRealmName) + "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    public static void refreshRealm(String configName, String realmName) {
        if (realmName != null && realmName.length() > 0) {
            try {
                Realm realm = Realm.getInstance(configName, realmName);

                if (realm != null) {
                    realm.refresh(configName);
                }
            } catch (NoSuchRealmException | BadRealmException nre) {
                // _logger.fine("Realm: "+realmName+" is not configured");
            }
        }
    }

    static void handleAdminGroup(String authRealmName, List<String> groups) {
        String adminRealm = "admin-realm"; // this should be a constant defined at a central place -- the name of realm for admin
        String adminGroup = "asadmin"; // this should be a constant defined at a central place -- fixed name of admin group
        
        if (adminRealm.equals(authRealmName) && groups != null) {
            groups.clear(); // basically, we are ignoring the group specified on command line when it's admin realm
            groups.add(adminGroup);
        }
    }
}
