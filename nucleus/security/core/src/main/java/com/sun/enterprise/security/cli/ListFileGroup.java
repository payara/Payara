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
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

/**
 * List File GroupsCommand
 * Usage: list-file-groups [--terse={true|false}][ --echo={true|false} ]
 *        [ --interactive={true|false} ] [--host  host] [--port port]
 *        [--secure| -s ] [--user  admin_user] [--passwordfile filename]
 *        [--help] [--name username] [--authrealmname auth_realm_name] [ target]
 *
 * @author Nandini Ektare
 */

@Service(name="list-file-groups")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.file.group")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,
CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=SecurityService.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-file-groups", 
        description="list-file-groups")
})
public class ListFileGroup implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(ListFileGroup.class);

    @Param(name="authrealmname", optional=true)
    private String authRealmName;

    @Param(name="name", optional=true)
    private String fileUserName;

    @Param(name = "target", primary=true, optional = true, defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Configs configs;

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
                "list.file.group.filerealmnotfound",
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

        try {
            // Get all users of this file realm. If a username has
            // been passed in through the --name CLI option use that
            FileRealm fr = getFileRealm(securityService, fileAuthRealm, report);

            if (fr == null) {
                // the getFileRealm method would have filled
                // in the right cause of this situation
                return;
            }
            
            Enumeration groups = null;
            if (fileUserName != null) {
                groups = fr.getGroupNames(fileUserName);
            } else {
                groups = fr.getGroupNames();
            }

            report.getTopMessagePart().setMessage(localStrings.getLocalString(
                "list.file.group.success",
                "list-file-groups successful"));
            report.getTopMessagePart().setChildrenType("file-group");
            while (groups.hasMoreElements()) {
                final ActionReport.MessagePart part =
                     report.getTopMessagePart().addChild();
                part.setMessage((String) groups.nextElement());
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (BadRealmException e) {
            report.setMessage(
                localStrings.getLocalString(
                    "list.file.group.realmcorrupted",
                    "Configured file realm {0} is corrupted.", authRealmName) +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        } catch (NoSuchUserException e) {
            report.setMessage(
                localStrings.getLocalString(
                    "list.file.group.usernotfound",
                    "Specified file user {0} not found.", fileUserName) +
                "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private FileRealm getFileRealm(final SecurityService securityService, AuthRealm fileAuthRealm, ActionReport report) {
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
            return null;
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
            return null;
        }

        // We have the right impl so let's try to remove one
        FileRealm fr = null;
        try {
            realmsManager.createRealms(config);
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
        }
        return fr;
    }
}
