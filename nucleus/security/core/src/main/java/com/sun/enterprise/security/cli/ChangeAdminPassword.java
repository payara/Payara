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
import java.lang.annotation.Annotation;
import java.util.Enumeration;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.List;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.internal.api.Target;

/**
 * Change Admin Password Command
 *
 * Usage: change-admin-password [--user admin_user] [--terse=false] 
 *        [--echo=false] [--host localhost] [--port 4848|4849] 
 *        [--secure | -s]
 *
 * @author Nandini Ektare
 */

@Service(name="change-admin-password")
@PerLookup
@I18n("change.admin.password")
@ExecuteOn({RuntimeType.ALL})
public class ChangeAdminPassword implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(ChangeAdminPassword.class);    

    @Param(name="password", password=true)
    private String oldpassword;
   
    @Param(name="newpassword", password=true)
    private String newpassword;

    @Param(name="username", primary=true)
    private String userName;
        
    @Inject
    private Configs configs;
    @Inject
    private Domain domain;

    @Inject
    private RealmsManager realmsManager;
    
    @Inject
    private AdminService adminService;
    
    private SecureAdmin secureAdmin = null;
    
    private Config config;
    
    @AccessRequired.To("update")
    private AuthRealm fileAuthRealm;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        //Issue 17513 Fix - Check for null passwords if secureadmin is enabled
        secureAdmin = domain.getSecureAdmin();
        if (SecureAdmin.Util.isEnabled(secureAdmin)) {
            if ((newpassword == null) || (newpassword.isEmpty())) {
                report.setMessage(localStrings.getLocalString(
                        "null_empty_password","The new password is null or empty"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return false;
            }           
        }
        final List <Config> configList = configs.getConfig();
        config = configList.get(0);

        SecurityService securityService = config.getSecurityService();
       
        fileAuthRealm = null;        
        for (AuthRealm authRealm : securityService.getAuthRealm()) {            
            if (authRealm.getName().equals(adminService.getAuthRealmName())) {                
                fileAuthRealm = authRealm;            
                break;
            }
        }        

        if (fileAuthRealm == null) {
            report.setMessage(localStrings.getLocalString(
                "change.admin.password.adminrealmnotfound", "Server " +
                "Error: There is no admin realm to perform this operation"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;                                            
        }
        
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
                    "change.admin.password.adminrealmnotsupported",
                    "Configured admin realm is not supported."));
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
                localStrings.getLocalString(
                    "change.admin.password.keyfilenotfound",
                    "There is no physical file associated with admin realm"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;                                            
        }
                            
        // We have the right impl so let's get to updating existing user 
        FileRealm fr = null;
        try {
            realmsManager.createRealms(config);
            fr = (FileRealm) realmsManager.getFromLoadedRealms(config.getName(), fileAuthRealm.getName());
            if (fr == null) {
                throw new NoSuchRealmException(fileAuthRealm.getName());
            }
        }  catch(NoSuchRealmException e) {
            report.setMessage(
                localStrings.getLocalString(
                    "change.admin.password.realmnotsupported",
                    "Configured admin realm does not exist.") +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        //now updating admin user password
        try {
            Enumeration en = fr.getGroupNames(userName);            
            int size = 0;
            while (en.hasMoreElements()) { 
                size++; 
                en.nextElement(); 
            }            
            String[] groups = new String[size];            
            en = fr.getGroupNames(userName);            
            for (int i = 0; i < size; i++) {
                groups[i] = (String) en.nextElement();
            }
            fr.updateUser(userName,userName, newpassword.toCharArray(), groups);
            fr.persist();
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);            
        } catch (Exception e) {
            report.setMessage(
                localStrings.getLocalString(
                    "change.admin.password.userupdatefailed",
                    "Password change failed for user named {0}", userName) +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }        
    }
 

  
}
