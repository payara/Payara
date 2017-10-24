/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
import org.glassfish.api.admin.CommandException;

/**
 *
 * @author jGauravGupta
 */
public abstract class DBControl {

    protected final String dbHost;
    protected final String dbPort;
    protected final String dbUser;
    protected final String dbPassword;
    protected final String dbHome;
    protected final String dbCommand;
    protected boolean redirect;

    public DBControl(final String dc, final String dht, final String dp,
            final String redirect, final String dhe, final String duser, final String dpwd) {
        this.dbCommand = dc;
        this.dbHost = dht;
        this.dbPort = dp;
        this.redirect = Boolean.parseBoolean(redirect);
        this.dbHome = dhe;
        this.dbUser = duser;
        this.dbPassword = dpwd;

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

                //redirect stdout and stderr to a file
                PrintStream printStream = new PrintStream(new FileOutputStream(dbLog, true), true);
                System.setOut(printStream);
                System.setErr(printStream);
            } catch (Throwable t) {
                t.printStackTrace();
                //exit with an error code of 2
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
        //dbHome must exist and  have write permission
        final File fDBHome = new File(dbHome);
        String dbLogFilePath;
        final StringManager lsm = StringManager.getManager(H2Control.class);
        if (fDBHome.isDirectory() && fDBHome.canWrite()) {
            final File fDBLog = new File(dbHome, getLogFileName());
            dbLogFilePath = fDBLog.toString();
            //if the file exists, check if it is writeable
            if (fDBLog.exists() && !fDBLog.canWrite()) {
                System.out.println(lsm.getString("UnableToAccessDatabaseLog", getLogFileName(), dbLogFilePath));
                System.out.println(lsm.getString("ContinueStartingDatabase"));
                //if exist but not able to write then create a temporary 
                //log file and persist on starting the database
                dbLogFilePath = createTempLogFile();
            } else if (!fDBLog.exists()) {
                //create log file
                if (!fDBLog.createNewFile()) {
                    System.out.println(lsm.getString("UnableToCreateDatabaseLog", getLogFileName(), dbLogFilePath));
                }
            }
        } else {
            System.out.println(lsm.getString("InvalidDBDirectory", dbHome));
            System.out.println(lsm.getString("ContinueStartingDatabase"));
            //if directory does not exist then create a temporary log file
            //and persist on starting the database
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
            fTemp.deleteOnExit();
            tempFileName = fTemp.toString();
        } catch (IOException ioe) {
            final StringManager lsm = StringManager.getManager(this.getClass());
            throw new CommandException(lsm.getString("UnableToAccessDatabaseLog", tempFileName));
        }
        return tempFileName;
    }

    abstract String getLogFileName();
}
