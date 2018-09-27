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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.cli.optional;

import com.sun.enterprise.admin.cli.*;
import static com.sun.enterprise.admin.cli.optional.DBType.DERBY;
import static com.sun.enterprise.admin.cli.optional.DBType.H2;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import static com.sun.enterprise.util.SystemPropertyConstants.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

/**
 * This is an abstract class to be inherited by StartDatabaseCommand and
 * StopDatabaseCommand. This classes prepares the variables that are used to to
 * invoke DBControl. It also contains a pingDatabase method that is used by both
 * start/stop database command.
 *
 * @author <a href="mailto:jane.young@sun.com">Jane Young</a>
 * @author Bill Shannon
 */
public abstract class DatabaseCommand extends CLICommand {

    protected static final String DB_TYPE_DEFAULT = "h2";
    protected static final String DB_HOST_DEFAULT = "0.0.0.0";
    protected final static String DB_USER = "dbuser";
    //protected final static String DB_PASSWORD   = "dbpassword";
    protected final static String DB_PASSWORDFILE = "dbpasswordfile";

    @Param(name = "dbtype", optional = true, defaultValue = DB_TYPE_DEFAULT)
    protected String dbType;

    @Param(name = "dbhost", optional = true, defaultValue = DB_HOST_DEFAULT)
    protected String dbHost;

    @Param(name = "dbport", optional = true)
    protected String dbPort;

    protected File dbLocation;
    protected File sJavaHome;
    protected File sInstallRoot;
    protected final ClassPathBuilder sClasspath = new ClassPathBuilder();
    protected final ClassPathBuilder sDatabaseClasspath = new ClassPathBuilder();

    private static final LocalStringsImpl strings
            = new LocalStringsImpl(DatabaseCommand.class);

    protected DBManager dbManager;
    
    /**
     * Prepare variables to invoke start/ping database command.
     */
    protected void prepareProcessExecutor() throws Exception {
        sInstallRoot = new File(getSystemProperty(INSTALL_ROOT_PROPERTY));
        if (dbType == null) {
            dbType = DB_TYPE_DEFAULT;
        } else {
            checkIfDBTypeIsValid(dbType);
        }
        if (dbHost == null) {
            dbHost = DB_HOST_DEFAULT;
        }
        if (dbPort == null) {
            if(dbType.equals(H2.getValue())){
                dbPort = H2.getPort();
            } else if(dbType.equals(DERBY.getValue())){
                dbPort = DERBY.getPort();
            }
        } else {
            checkIfPortIsValid(dbPort);
        }
        sJavaHome = new File(getSystemProperty(JAVA_ROOT_PROPERTY));
        dbManager = DBManagerFactory.getDBManager(DBType.valueOf(dbType.toUpperCase()));
        dbLocation = new File(getSystemProperty(dbManager.getRootProperty()));
        try {
            dbManager.checkIfDbInstalled(dbLocation);
        } catch (CommandException ce) {
            logger.info(strings.get("DatabaseNotInstalled", dbLocation));
            throw ce;
        }
        sClasspath.add(new File(sInstallRoot, "lib/asadmin/cli-optional.jar"));
        dbManager.buildDatabaseClasspath(dbLocation, sDatabaseClasspath);
    }

    /**
     * Check if database port is valid. DB does not check this so need to add
     * code to check the port number.
     */
    private void checkIfPortIsValid(final String port)
            throws CommandValidationException {
        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new CommandValidationException(
                    strings.get("InvalidPortNumber", port));
        }
    }
    
    /**
     * Check if database type is valid.
     */
    private void checkIfDBTypeIsValid(final String dbType)
            throws CommandValidationException {
        try {
            DBType.valueOf(dbType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException(
                    strings.get("InvalidDBType", dbType));
        }
    }

    /**
     * Defines the command to ping the database.
     *
     * @param bRedirect
     * @return
     */
    protected String[] pingDatabaseCmd(boolean bRedirect) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(getJavaExe().toString());
        cmd.add("-Djava.library.path=" + sInstallRoot + File.separator + "lib");
        cmd.addAll(dbManager.getSystemProperty());
        cmd.add("-cp");
        cmd.add(sClasspath + File.pathSeparator + sDatabaseClasspath);
        cmd.add(dbManager.getDBControl().getName());
        cmd.add("ping");
        cmd.add(dbHost);
        cmd.add(dbPort);
        cmd.add(Boolean.toString(bRedirect));
        return cmd.toArray(new String[cmd.size()]);
    }

    /**
     * Computes the java executable location from {@link #sJavaHome}.
     *
     * @return
     */
    protected final File getJavaExe() {
        return new File(new File(sJavaHome, "bin"), "java");
    }
}
