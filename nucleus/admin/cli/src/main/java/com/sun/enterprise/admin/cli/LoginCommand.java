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

package com.sun.enterprise.admin.cli;

import com.sun.appserv.management.client.prefs.LoginInfo;
import com.sun.appserv.management.client.prefs.LoginInfoStore;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;
import com.sun.enterprise.admin.cli.remote.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.Console;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.*;

/**
 * The asadmin login command.
 * Pretend to be a remote command so that program options are allowed.
 *
 * @author Nandini Ektare
 * @author Bill Shannon
 */
@Service(name = "login")
@PerLookup
public class LoginCommand extends CLICommand {

    private String adminUser = null;
    private String adminPassword = null;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(LoginCommand.class);

    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {

        // Step 1: Get admin username and password
        programOpts.setInteractive(true);       // force it
        adminUser = getAdminUser();
        programOpts.setUser(adminUser);
        adminPassword = SystemPropertyConstants.DEFAULT_ADMIN_PASSWORD;
        programOpts.setPassword(adminPassword,
            ProgramOptions.PasswordLocation.DEFAULT);
        boolean interactive = programOpts.isInteractive();      // save value
        programOpts.setInteractive(false);      // no more prompting allowed
 
        // Step 2: Invoke version command to validate the authentication info
        boolean tryAgain = false;
        do {
            switch (DASUtils.pingDASWithAuth(programOpts, env)) {
            case NONE:
                tryAgain = false;
                break;
            case AUTHENTICATION:
                if (tryAgain)   // already tried once
                    throw new CommandException(strings.get("InvalidCredentials",
                                                    programOpts.getUser()));
                tryAgain = true;

                // maybe we need a password?
                programOpts.setInteractive(interactive);
                adminPassword = getAdminPassword();
                programOpts.setPassword(adminPassword,
                    ProgramOptions.PasswordLocation.USER);
                programOpts.setInteractive(false);
                break;
            case CONNECTION:
                throw new CommandException(strings.get("ConnectException",
                    programOpts.getHost(), "" + programOpts.getPort()));
            case IO:
                throw new CommandException(strings.get("IOException",
                    programOpts.getHost(), "" + programOpts.getPort()));
            case UNKNOWN:
                throw new CommandException(strings.get("UnknownException",
                    programOpts.getHost(), "" + programOpts.getPort()));
            }
        } while (tryAgain);

        // Step 3: Save in <userhomedir>/.asadminpass the string 
        // asadmin://<adminuser>@<adminhost>:<adminport><encrypted adminpassword>
        saveLogin(programOpts.getHost(), programOpts.getPort(),
                    adminUser, adminPassword);
        return 0;
    }
 
    /**
     * Prompt for the admin user name.
     */
    private String getAdminUser() {
        Console cons = System.console();
        String user = null;
        String defuser = programOpts.getUser();
        if (defuser == null)
            defuser = SystemPropertyConstants.DEFAULT_ADMIN_USER;
        if (cons != null) {
            cons.printf("%s", strings.get("AdminUserPrompt", defuser));
            String val = cons.readLine();
            if (val != null && val.length() > 0)
                user = val;
            else
                user = defuser;
        }
        return user;
    }

    /**
     *  This methods prompts for the admin password. 
     *
     *  @return admin password
     *  @throws CommandValidationException if adminpassword can't be fetched 
     */
    private String getAdminPassword() {
        final String prompt = strings.get("AdminPasswordPrompt");

        return readPassword(prompt);
    }

    /*
     * Saves the login information to the login store. Usually this is the file
     * ".asadminpass" in user's home directory.
     */
    private void saveLogin(String host, final int port, 
                           final String user, final String passwd) {
        if (!ok(host))
            host = "localhost";
        // to avoid putting commas in the port number (e.g., "4,848")...
        String sport = Integer.toString(port);
        try {
            // By definition, the host name will default to "localhost" and 
            // entry is overwritten
            final LoginInfoStore store = LoginInfoStoreFactory.getStore(null);
            final LoginInfo login = new LoginInfo(host, port, user, passwd);
            if (store.exists(login.getHost(), login.getPort())) {
                // Let the user know that the user has chosen to overwrite the 
                // login information. This is non-interactive, on purpose
                logger.info(strings.get("OverwriteLoginMsgCreateDomain",
                                        login.getHost(), "" + login.getPort()));
            }
            store.store(login, true);
            logger.info(strings.get("LoginInfoStored", 
                user, login.getHost(), sport, store.getName()));
        } catch (final Exception e) {
            logger.warning(
                strings.get("LoginInfoNotStored", host, sport));
            printExceptionStackTrace(e);
        }
    }
}
