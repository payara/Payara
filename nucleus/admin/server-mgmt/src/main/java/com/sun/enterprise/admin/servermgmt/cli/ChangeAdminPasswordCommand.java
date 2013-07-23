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

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.launcher.GFLauncher;
import com.sun.enterprise.admin.launcher.GFLauncherException;
import com.sun.enterprise.admin.launcher.GFLauncherFactory;
import com.sun.enterprise.admin.launcher.GFLauncherInfo;
import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import java.io.Console;
import org.jvnet.hk2.annotations.*;
import org.glassfish.api.admin.*;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.net.NetUtils;
import java.io.IOException;
import java.net.ConnectException;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.common.FileRealmHelper;

/**
 * The change-admin-password command.
 * The remote command implementation presents a different
 * interface (set of options) than the local command.
 * This special local implementation adapts the local
 * interface to the requirements of the remote command.
 * 
 * The remote command is different in that it accepts the user name as 
 * an operand.  This command accepts it via the --user parameter. If the --user
 * option isn't specified, this command prompts for the user name.
 * 
 * Another difference is that the local command will prompt for the old 
 * password only once.  The default behavior for @Param for passwords is to 
 * prompt for the password twice.  *
 *
 * @author Bill Shannon
 */
@Service(name = "change-admin-password")
@PerLookup
@I18n("change.admin.password")
public class ChangeAdminPasswordCommand extends LocalDomainCommand {
    private ParameterMap params;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(ChangeAdminPasswordCommand.class);

    @Param(name="domain_name", optional=true)
    private String userArgDomainName;
    
    @Param(password=true, optional=true)
    private String password;
    
    @Param(password=true, optional=true)
    private String newpassword;
    
    private SecureAdmin secureAdmin = null;

    
    
    /**
     * Require the user to actually type the passwords unless they are in
     * the file specified by the --passwordfile option.
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        setDomainName(userArgDomainName);
        super.validate();
        /*
         * If --user wasn't specified as a program option,
         * we treat it as a required option and prompt for it
         * if possible.
         */
        if (programOpts.getUser() == null) {
            // prompt for it (if interactive)
            Console cons = System.console();
            if (cons != null && programOpts.isInteractive()) {
                cons.printf("%s", strings.get("AdminUserDefaultPrompt",
                    SystemPropertyConstants.DEFAULT_ADMIN_USER));
                String val = cons.readLine();
                if (ok(val))
                    programOpts.setUser(val);
                else
                    programOpts.setUser(
                                    SystemPropertyConstants.DEFAULT_ADMIN_USER);
            } else {
                //logger.info(strings.get("AdminUserRequired"));
                throw new CommandValidationException(
                    strings.get("AdminUserRequired"));
            }
        }
        
        if (password == null) {
            // prompt for it (if interactive)
            password = getPassword("password", strings.get("AdminPassword"), 
                    null, false);
            if (password == null) {
                throw new CommandValidationException(strings.get("AdminPwRequired"));
            }
            programOpts.setPassword(password, ProgramOptions.PasswordLocation.USER);
        }

        if (newpassword == null) {
            // prompt for it (if interactive)
            newpassword = getPassword("newpassword", strings.get("change.admin.password.newpassword"), 
                    strings.get("change.admin.password.newpassword.again"), true);
            if (newpassword == null) {
                throw new CommandValidationException(strings.get("AdminNewPwRequired"));
            }
        }

        /*
         * Now that the user-supplied parameters have been validated,
         * we set the parameter values for the remote command.
         */
        params = new ParameterMap();
        params.set("DEFAULT", programOpts.getUser());
        params.set("password", password);
        params.set("newpassword", newpassword);
    }

    /**
     * Execute the remote command using the parameters we've collected.
     */
    @Override
    protected int executeCommand() throws CommandException {
        
         if(ok(domainDirParam) || ok(userArgDomainName)) {
          //If domaindir or domain arguments are provided,
           // do not attempt remote connection. Change password locally
           String domainDir = (ok(domainDirParam))?domainDirParam:getDomainsDir().getPath();
           String domainName = (ok(userArgDomainName))?userArgDomainName:getDomainName();
           return changeAdminPasswordLocally(domainDir,domainName);         
        
       } else {
           try {
                RemoteRestAdminCommand rac = new RemoteRestAdminCommand(name,
                    programOpts.getHost(), programOpts.getPort(),
                    programOpts.isSecure(), programOpts.getUser(),
                    programOpts.getPassword(), logger,false);
                rac.executeCommand(params);
                return SUCCESS;
           } catch(CommandException ce) {
               if ( ce.getCause() instanceof ConnectException) {
                   //Remote change failure - change password with default values of
                   // domaindir and domain name,if the --host option is not provided.
                   if(!isLocalHost(programOpts.getHost())) {
                       throw ce;
                   }
                   return changeAdminPasswordLocally(getDomainsDir().getPath(),
                           getDomainName());
                   
                   
               } else {
                   throw ce;
               }
           }
        }
       
        
    }

    private int changeAdminPasswordLocally(String domainDir, String domainName) throws CommandException {
        
        if(!isLocalHost(programOpts.getHost())) {
            throw new CommandException(strings.get("CannotExecuteLocally"));
        }  
        
        GFLauncher launcher = null;
        try {
            launcher = GFLauncherFactory.getInstance(RuntimeType.DAS);
            GFLauncherInfo info = launcher.getInfo();
            info.setDomainName(domainName);
            info.setDomainParentDir(domainDir);
            launcher.setup();
            
            //If secure admin is enabled and if new password is null
            //throw new exception
            if(launcher.isSecureAdminEnabled()) {
                if ((newpassword == null) || (newpassword.isEmpty())) {
                    throw new CommandException(strings.get("NullNewPassword"));
                }
            }

            String adminKeyFile = launcher.getAdminRealmKeyFile();

            if (adminKeyFile != null) {
                //This is a FileRealm, instantiate it.
                FileRealmHelper helper = new FileRealmHelper(adminKeyFile);

                //Authenticate the old password
                String[] groups = helper.authenticate(programOpts.getUser(), password.toCharArray());
                if (groups == null) {
                    throw new CommandException(strings.get("InvalidCredentials", programOpts.getUser()));
                }
                helper.updateUser(programOpts.getUser(), programOpts.getUser(), newpassword.toCharArray(), null);
                helper.persist();
                return SUCCESS;

            } else {
                //Cannot change password locally for non file realms
                throw new CommandException(strings.get("NotFileRealmCannotChangeLocally"));

            }

        } catch (MiniXmlParserException ex) {
            throw new CommandException(ex);
        } catch (GFLauncherException ex) {
            throw new CommandException(ex);
        } catch (IOException ex) {
            throw new CommandException(ex);
        }
    }
    
    private static boolean isLocalHost(String host) {        
        if(host != null && (NetUtils.isThisHostLocal(host) || NetUtils.isLocal(host))) {
            return true;          
        }
        return false;
    }
}
