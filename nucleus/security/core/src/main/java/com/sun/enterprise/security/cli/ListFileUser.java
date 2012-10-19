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

import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import java.util.Enumeration;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
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
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.File;
import org.glassfish.api.admin.ExecuteOn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

/**
 * List File Users Command
 * Usage: list-file-users [--terse=false] [--echo=false] [--interactive=true] 
 * [--host localhost] [--port 4848|4849] [--secure | -s] [--user admin_user] 
 * [--passwordfile file_name] [--authrealmname authrealm_name] 
 * [target(Default server)]
 * @author Nandini Ektare
 */

@Service(name="list-file-users")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.file.user")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,
    CommandTarget.CLUSTERED_INSTANCE,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=AuthRealm.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-users", 
        description="List Users",
        params={
            @RestParam(name="authrealmname", value="$parent")
        })
})
public class ListFileUser implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(ListFileUser.class);    

    @Param(name="authrealmname", optional=true)
    private String authRealmName;
    
    @Param(name = "target", primary=true, optional = true, defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    @Inject
    private RealmsManager realmsManager;

    @AccessRequired.To("read")
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
                "list.file.user.filerealmnotfound",
                "File realm {0} does not exist", authRealmName));
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
                    "list.file.user.realmnotsupported",
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
                localStrings.getLocalString("list.file.user.keyfilenotfound",
                "There is no physical file associated with this file realm {0} ", 
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
        // We have the right impl so let's try to remove one 
        FileRealm fr = null;
        try {
            realmsManager.createRealms(config);
            //account for updates to realms from outside this config sharing
            //same keyfile
            CreateFileUser.refreshRealm(config.getName(), authRealmName);
            fr = (FileRealm) realmsManager.getFromLoadedRealms(config.getName(),authRealmName);
            if (fr == null) {
                throw new NoSuchRealmException(authRealmName);
            }
        }  catch(NoSuchRealmException e) {
            report.setMessage(
                localStrings.getLocalString(
                    "list.file.user.realmnotsupported",
                    "Configured file realm {0} is not supported.", authRealmName) +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        try {
            Enumeration users = fr.getUserNames();
            List userList = new ArrayList();
            
            while (users.hasMoreElements()) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                String userName = (String) users.nextElement();
                part.setMessage(userName);
                Map userMap = new HashMap();
                userMap.put("name", userName);
                try {
                    userMap.put("groups", Collections.list(fr.getGroupNames(userName)));
                } catch (NoSuchUserException ex) {
                    // This should never be thrown since we just got the user name from the realm
                }
                userList.add(userMap);
            }
            Properties extraProperties = new Properties();
            extraProperties.put("users", userList);
            report.setExtraProperties(extraProperties);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (BadRealmException e) {
            report.setMessage(
                localStrings.getLocalString(
                    "list.file.user.realmcorrupted",
                    "Configured file realm {0} is corrupted.", authRealmName) +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }        
    }
}
