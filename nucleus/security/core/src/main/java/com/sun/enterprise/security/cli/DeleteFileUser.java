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


import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.beans.PropertyVetoException;
import java.io.File;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 * Delete File User Command
 * Usage: delete-file-user [--terse=false] [--echo=false] [--interactive=true] 
 * [--host localhost] [--port 4848|4849] [--secure | -s] [--user admin_user]
 * [--passwordfile file_name] [--authrealmname authrealm_name] 
 * [--target target(Default server)] username
 *
 * @author Nandini Ektare
 */

@Service(name="delete-file-user")
@PerLookup
@I18n("delete.file.user")
@ExecuteOn({RuntimeType.ALL})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=AuthRealm.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-user", 
        description="Delete",
        params={
            @RestParam(name="authrealmname", value="$parent")
        })
})
public class DeleteFileUser implements /*UndoableCommand*/ AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(DeleteFileUser.class);    

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

    @AccessRequired.To("update")
    private AuthRealm fileAuthRealm;
    
    private SecurityService securityService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            // config can be null as this command executes on all instances
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
            return false;
        }
        securityService = config.getSecurityService();
        fileAuthRealm = CLIUtil.findRealm(securityService, authRealmName);
        if (fileAuthRealm == null) {
            final ActionReport report = context.getActionReport();
            report.setMessage(localStrings.getLocalString(
                "delete.file.user.filerealmnotfound",
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
                    "delete.file.user.realmnotsupported",
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
        final String kFile = keyFile;
        if (keyFile == null) {
            report.setMessage(
                localStrings.getLocalString("delete.file.user.keyfilenotfound",
                "There is no physical file associated with this file realm {0} ", 
                authRealmName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;                                            
        }
        boolean exists = (new File(kFile)).exists();
        if (!exists) {
            report.setMessage(
                localStrings.getLocalString("file.realm.keyfilenonexistent",
                "The specified physical file {0} associated with the file realm {1} does not exist.",
                new Object[]{kFile, authRealmName}));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
         //even though delete-file-user is not an update to the security-service
         //do we need to make it transactional by referncing the securityservice
         //hypothetically ?.
        try {
            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {
                public Object run(SecurityService param)
                        throws PropertyVetoException, TransactionFailure {
                    try {
                        realmsManager.createRealms(config);
                        final FileRealm fr = (FileRealm) realmsManager.getFromLoadedRealms(config.getName(),authRealmName);
                        fr.removeUser(userName);
                        fr.persist();
                        CreateFileUser.refreshRealm(config.getName(),authRealmName);
                        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    } catch (NoSuchUserException e) {
                        report.setMessage(
                                localStrings.getLocalString("delete.file.user.usernotfound",
                                "There is no such existing user {0} in the file realm {1}.",
                                userName, authRealmName) + "  " + e.getLocalizedMessage());
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setFailureCause(e);
                    } catch (BadRealmException e) {
                        report.setMessage(
                                localStrings.getLocalString(
                                "delete.file.user.realmcorrupted",
                                "Configured file realm {0} is corrupted.", authRealmName) + "  " + e.getLocalizedMessage());
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setFailureCause(e);
                   } catch (Exception e) {
                        e.printStackTrace();
                        report.setMessage(
                                localStrings.getLocalString("delete.file.user.userdeletefailed",
                                "Removing User {0} from file realm {1} failed",
                                userName, authRealmName) + "  " + e.getLocalizedMessage());
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setFailureCause(e);
                    }
                    return null;
                }
            }, securityService);
        } catch (Exception e) {
            report.setMessage(
                    localStrings.getLocalString("delete.file.user.userdeletefailed",
                    "Removing User {0} from file realm {1} failed",
                    userName, authRealmName) + "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
//    @Override
//    public ActionReport prepare(ParameterMap parameters) {
//        //TODO: is there a way to check if in a Cluster some
//        //instances are down
////        com.sun.enterprise.config.serverbeans.Cluster cluster = domain.getClusterNamed(target);
////        if (cluster!=null) {
////            List<Server> servers = cluster.getInstances();
////        }
//        final ActionReport report = new ActionReport();
//    }
//
//    @Override
//    public void undo(ParameterMap parameters) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
}
