/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

import com.sun.enterprise.util.i18n.StringManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.admin.CommandException;

/**
 *
 * @author jGauravGupta
 */
public abstract class DBControl {

    private final String dbHost;
    private final String dbPort;
    private final String dbUser;
    private final String dbPassword;
    private final String dbHome;
    private final String dbCommand;
    private boolean redirect;

    protected DBControl(final String dbCommand, final String dbHost, final String dbPort,
            final String redirect, final String dbHome, final String dbUser, final String dbPassword) {
        this.dbCommand = dbCommand;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.redirect = Boolean.parseBoolean(redirect);
        this.dbHome = dbHome;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;

        if (this.redirect) {
            try {
                String dbLog;
                if (this.dbHome == null) {
                    // if dbHome is null then redirect the output to a temporary file
                    // which gets deleted after the jvm exists.
                    dbLog = createTempLogFile();
                } else {
                    dbLog = createDBLog(this.dbHome);
                }

                // Redirect stdout and stderr to a file
                try (PrintStream printStream = new PrintStream(new FileOutputStream(dbLog, true), false, StandardCharsets.UTF_8)) {
                    System.setOut(printStream);
                    System.setErr(printStream);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                // Exit with an error code of 2
                Runtime.getRuntime().exit(2);
            }
        }
    }

    /**
     * create a db.log file that stdout/stderr will redirect to. dbhome is the
     * h2.system.home directory where derb.log gets created. if user specified
     * --dbhome options, ${db}.log will be created there.
     *
     * @param dbHome
     * @return
     * @throws java.lang.Exception
     */
    private String createDBLog(final String dbHome) throws Exception {
        // dbHome must exist and  have write permission
        final File fDBHome = new File(dbHome);
        String dbLogFilePath;
        final StringManager localManager = StringManager.getManager(this.getClass());
        if (fDBHome.isDirectory() && fDBHome.canWrite()) {
            final File fDBLog = new File(dbHome, getLogFileName());
            dbLogFilePath = fDBLog.toString();
            // if the file exists, check if it is writeable
            if (fDBLog.exists() && !fDBLog.canWrite()) {
                System.out.println(localManager.getString("UnableToAccessDatabaseLog", getLogFileName(), dbLogFilePath));
                System.out.println(localManager.getString("ContinueStartingDatabase"));
                // if exist but not able to write then create a temporary
                // log file and persist on starting the database
                dbLogFilePath = createTempLogFile();
            } else if (!fDBLog.exists()) {
                // Create the log file
                if (!fDBLog.createNewFile()) {
                    System.out.println(localManager.getString("UnableToCreateDatabaseLog", getLogFileName(), dbLogFilePath));
                }
            }
        } else {
            System.out.println(localManager.getString("InvalidDBDirectory", dbHome));
            System.out.println(localManager.getString("ContinueStartingDatabase"));
            // if directory does not exist then create a temporary log file
            // and persist on starting the database
            dbLogFilePath = createTempLogFile();
        }
        return dbLogFilePath;
    }

    /**
     * creates a temporary log file.
     *
     * @return
     * @throws org.glassfish.api.admin.CommandException
     */
    private String createTempLogFile() throws CommandException {
        String tempFileName = "";
        try {
            final File fTemp = File.createTempFile("foo", null);
            FileUtils.deleteOnExit(fTemp);
            tempFileName = fTemp.toString();
        } catch (IOException ioe) {
            final StringManager localManager = StringManager.getManager(this.getClass());
            throw new CommandException(localManager.getString("UnableToAccessDatabaseLog", tempFileName));
        }
        return tempFileName;
    }

    abstract String getLogFileName();

    /**
     * @return the dbHost
     */
    public String getDbHost() {
        return dbHost;
    }

    /**
     * @return the dbPort
     */
    public String getDbPort() {
        return dbPort;
    }

    /**
     * @return the dbUser
     */
    public String getDbUser() {
        return dbUser;
    }

    /**
     * @return the dbPassword
     */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * @return the dbHome
     */
    public String getDbHome() {
        return dbHome;
    }

    /**
     * @return the dbCommand
     */
    public String getDbCommand() {
        return dbCommand;
    }

    /**
     * @return the redirect
     */
    public boolean isRedirect() {
        return redirect;
    }
}
