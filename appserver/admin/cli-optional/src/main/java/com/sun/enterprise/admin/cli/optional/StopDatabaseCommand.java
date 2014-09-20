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

package com.sun.enterprise.admin.cli.optional;

import java.io.File;
import java.util.HashMap;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.admin.cli.CLIProcessExecutor;
import com.sun.enterprise.admin.cli.CLIUtil;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.OS;

/**
 *  stop-database command
 *  This command class will invoke DerbyControl to stop
 *  the database.
 *
 *  @author <a href="mailto:jane.young@sun.com">Jane Young</a>
 *  @author Bill Shannon
 */
@Service(name = "stop-database")
@PerLookup
public final class StopDatabaseCommand extends DatabaseCommand {

    @Param(name = "dbuser", optional = true)
    private String dbUser;

    private File dbPasswordFile;
    private String dbPassword;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(StopDatabaseCommand.class);

    /**
     * Defines the command to stop the derby database.
     * Note that when using Darwin (Mac), the property,
     * "-Dderby.storage.fileSyncTransactionLog=True" is defined.
     */
    public String[] stopDatabaseCmd() throws Exception {
        passwords = new HashMap<String, String>();
        String passwordfile = this.getOption(ProgramOptions.PASSWORDFILE);
        if (passwordfile != null) {
            dbPasswordFile = new File(passwordfile);
            dbPasswordFile = SmartFile.sanitize(dbPasswordFile);
        }
        if (dbPasswordFile != null) {
            passwords =
                CLIUtil.readPasswordFileOptions(dbPasswordFile.getPath(), true);
            dbPassword = passwords.get(Environment.getPrefix() + "DBPASSWORD");
        }
        if (dbUser == null && dbPassword == null) {
            if (OS.isDarwin()) {
                return new String[]{
                            sJavaHome + File.separator + "bin" + File.separator + "java",
                            "-Djava.library.path=" + sInstallRoot + File.separator + "lib",
                            "-Dderby.storage.fileSyncTransactionLog=True",
                            "-cp",
                            sClasspath + File.pathSeparator + sDatabaseClasspath,
                            "com.sun.enterprise.admin.cli.optional.DerbyControl",
                            "shutdown",
                            dbHost, dbPort, "false"
                        };
            }
            return new String[]{
                        sJavaHome + File.separator + "bin" + File.separator + "java",
                        "-Djava.library.path=" + sInstallRoot + File.separator + "lib",
                        "-cp",
                        sClasspath + File.pathSeparator + sDatabaseClasspath,
                        "com.sun.enterprise.admin.cli.optional.DerbyControl",
                        "shutdown",
                        dbHost, dbPort, "false"
                    };
        } else {
            if (OS.isDarwin()) {
                return new String[]{
                            sJavaHome + File.separator + "bin" + File.separator + "java",
                            "-Djava.library.path=" + sInstallRoot + File.separator + "lib",
                            "-Dderby.storage.fileSyncTransactionLog=True",
                            "-cp",
                            sClasspath + File.pathSeparator + sDatabaseClasspath,
                            "com.sun.enterprise.admin.cli.optional.DerbyControl",
                            "shutdown",
                            dbHost, dbPort, "false", dbUser, dbPassword
                        };
            }
            return new String[]{
                        sJavaHome + File.separator + "bin" + File.separator + "java",
                        "-Djava.library.path=" + sInstallRoot + File.separator + "lib",
                        "-cp",
                        sClasspath + File.pathSeparator + sDatabaseClasspath,
                        "com.sun.enterprise.admin.cli.optional.DerbyControl",
                        "shutdown",
                        dbHost, dbPort, "false", dbUser, dbPassword
                    };
        }
    }

    /**
     *  Method that Executes the command
     *  @throws CommandException
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {
        try {
            prepareProcessExecutor();
            CLIProcessExecutor cpe = new CLIProcessExecutor();
            cpe.execute("pingDatabaseCmd", pingDatabaseCmd(false), true);
            if (cpe.exitValue() > 0) {
                // if ping is unsuccesfull then database is not up and running
                throw new CommandException(
                    strings.get("StopDatabaseStatus", dbHost, dbPort));
            } else if (cpe.exitValue() < 0) {
                // Something terribly wrong!
                throw new CommandException(
                    strings.get("UnableToStopDatabase", "derby.log"));
            } else {
                // database is running so go ahead and stop the database
                cpe.execute("stopDatabaseCmd", stopDatabaseCmd(), true);
                if (cpe.exitValue() > 0) {
                    throw new CommandException(
                        strings.get("UnableToStopDatabase", "derby.log"));
                }
            }
        } catch (Exception e) {
            throw new CommandException(
                strings.get("UnableToStopDatabase", "derby.log"), e);
        }
        return 0;
    }
}
