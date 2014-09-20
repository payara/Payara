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

package com.sun.enterprise.security.cli;

import com.sun.enterprise.config.serverbeans.AdminService;
import java.util.List;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.File;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

/**
 * Update File User Command
 *
 * Usage: update-file-user [--terse=false] [--echo=false] [--interactive=true] 
 *   [--host localhost] [--port 4848|4849] [--secure | -s] [--user admin_user]
 *   [--passwordfile file_name] [--userpassword admin_passwd] 
 *   [--groups user_groups[:user_groups]*] [--authrealmname authrealm_name] 
 *   [--target target(Default server)] username
 *
 * @author Nandini Ektare
 */

@Service(name="update-file-user")
@PerLookup
@I18n("update.file.user")
@ExecuteOn({RuntimeType.ALL})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=AuthRealm.class,
        opType=RestEndpoint.OpType.POST, 
        path="update-user", 
        description="Update Users",
        params={
            @RestParam(name="authrealmname", value="$parent")
        })
})
public class UpdateFileUser implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(UpdateFileUser.class);    

    @Param(name="groups", optional=true, separator=':')
    private List<String> groups = null;

    // @Param(name="userpasswordfile", optional=true)
    // String passwordFile;

    @Param(name="userpassword", optional=true, password=true)
    private String userpassword;

    @Param(name="authrealmname", optional=true)
    private String authRealmName;
    
    @Param(name = "target", optional = true, defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Param(name="username", primary=true)
    private String userName;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;
    
    @Inject
    private RealmsManager realmsManager;
    
    @Inject
    private AdminService adminService;
    
    private SecureAdmin secureAdmin = null;

    @AccessRequired.To("update")
    private AuthRealm fileAuthRealm;
    
    private SecurityService securityService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            return false;
        }
        securityService = config.getSecurityService();
        fileAuthRealm = CLIUtil.findRealm(securityService, authRealmName);
        if (fileAuthRealm == null) {
            final ActionReport report = context.getActionReport();
            report.setMessage(localStrings.getLocalString(
                "update.file.user.filerealmnotfound",
                "File realm {0} does not exist", 
                authRealmName));
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
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        // Get FileRealm class name, match it with what is expected.
        String fileRealmClassName = fileAuthRealm.getClassname();
        
        // Report error if provided impl is not the one expected
        if (fileRealmClassName != null && 
            !fileRealmClassName.equals(
                "com.sun.enterprise.security.auth.realm.file.FileRealm")) {
            report.setMessage(
                localStrings.getLocalString(
                    "update.file.user.realmnotsupported",
                    "Configured file realm {0} is not supported.",
                    fileRealmClassName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;                
        }

        // ensure we have the file associated with the authrealm
        String keyFile = null;
        for (Property fileProp : fileAuthRealm.getProperty()) {
            if (fileProp.getName().equals("file"))
                keyFile = fileProp.getValue();
        }
        if (keyFile == null) {
            report.setMessage(
                localStrings.getLocalString("update.file.user.keyfilenotfound",
                "There is no physical file associated with file realm {0}", 
                authRealmName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;                                            
        }
        boolean exists = (new File(keyFile)).exists();
        if (!exists) {
            report.setMessage(
                localStrings.getLocalString("file.realm.keyfilenonexistent",
                "The specified physical file {0} associated with the file realm {1} does not exist.",
                new Object[]{keyFile, authRealmName}));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        // Now get all inputs ready. userid and groups are straightforward but
        // password is tricky. It is stored in the file passwordfile passed 
        // through the CLI options. It is stored under the name 
        // AS_ADMIN_USERPASSWORD. Fetch it from there.
        String password = userpassword; // fetchPassword(report);
        if (password == null && groups == null) {
            report.setMessage(localStrings.getLocalString(
                "update.file.user.keyfilenotreadable", "None of password or groups have been specified for update,"
              + "Password for user {0} has to be specified"
              + "through AS_ADMIN_USERPASSWORD property in the file specified " 
              + "in --passwordfile option", userName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        //Issue 17525 Fix - Check for null passwords for admin-realm if secureadmin is enabled
        if (password != null) {
            secureAdmin = domain.getSecureAdmin();
            if ((SecureAdmin.Util.isEnabled(secureAdmin))
                    && (adminService.getAuthRealmName().equals(authRealmName))) {
                if ((password.isEmpty())) {
                    report.setMessage(localStrings.getLocalString(
                            "null_empty_password", "The admin user password is empty"));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        //even though update-file-user is not an update to the security-service
        //do we need to make it transactional by referncing the securityservice
        //hypothetically ?.
        //TODO: check and enclose the code below inside ConfigSupport.apply(...)
        FileRealm fr = null;
        try {
            realmsManager.createRealms(config);
            fr = (FileRealm) realmsManager.getFromLoadedRealms(config.getName(),authRealmName);
            if (fr == null) {
                throw new NoSuchRealmException(authRealmName);
            }
        } catch(NoSuchRealmException e) {
            report.setMessage(
                localStrings.getLocalString(
                    "update.file.user.realmnotsupported",
                    "Configured file realm {0} does not exist.", authRealmName) +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        //now updating user
        try {
            CreateFileUser.handleAdminGroup(authRealmName, groups);
            String[] groups1 = (groups == null) ? null: groups.toArray(new String[groups.size()]);
            fr.updateUser(userName, userName, password, groups1);
            fr.persist();
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setMessage(
                localStrings.getLocalString("update.file.user.userupdatefailed",
                "Updating user {0} in file realm {1} failed", 
                userName, authRealmName) + "  " + e.getLocalizedMessage() );
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }        
    }
        
    /* private String fetchPassword(ActionReport report) {
        String password = null;
        if (userpassword != null && passwordFile != null)
            return password;
        if (userpassword != null) 
            password = userpassword;
        if (passwordFile != null) {
            File passwdFile = new File(passwordFile);
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(passwdFile));
                Properties prop = new Properties();
                prop.load(is);            
                for (Enumeration e=prop.propertyNames(); e.hasMoreElements();) {
                    String entry = (String)e.nextElement();
                    if (entry.equals("AS_ADMIN_USERPASSWORD")) {                    
                        password = prop.getProperty(entry);
                        break;
                    }
                }
            } catch(Exception e) {
                report.setFailureCause(e);
            } finally {
                try {
                    if (is != null) 
                        is.close();
                } catch(final Exception ignore){}
            }        
        } 
        return password;
    } */
}
