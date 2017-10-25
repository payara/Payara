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

import java.lang.reflect.Method;

/**
 * This class uses Java reflection to invoke Derby NetworkServerControl class.
 * This class is used to start/stop/ping derby database. The reason for creating
 * this class instead of directly invoking NetworkServerControl from the
 * StartDatabaseCommand class is so that a separate JVM is launched when
 * starting the database and the control is return to CLI.
 *
 * @author <a href="mailto:jane.young@sun.com">Jane Young</a>
 * @version $Revision: 1.13 $
 */
public final class DerbyControl extends DBControl {

    public static final String DB_LOG_FILENAME = "derby.log";

    public DerbyControl(final String dbCommand, final String dbHost, final String dbPort,
            final String redirect, final String dbHome, final String dbUser, final String dbPassword) {
        super(dbCommand, dbHost, dbPort, redirect, dbHome, dbUser, dbPassword);

        // Do not set derby.system.home if dbHome is empty
        if (getDbHome() != null && getDbHome().length() > 0) {
            System.setProperty("derby.system.home", getDbHome());
        }
        // Set the property to not overwrite log file
        System.setProperty("derby.infolog.append", "true");
    }

    public DerbyControl(final String dbCommand, final String dbHost, final String dbPort) {
        this(dbCommand, dbHost, dbPort, "true", null, null, null);
    }

    public DerbyControl(final String dbCommand, final String dbHost, final String dbPort, final String redirect) {
        this(dbCommand, dbHost, dbPort, redirect, null, null, null);
    }

    public DerbyControl(final String dbCommand, final String dbHost, final String dbPort, final String redirect, final String dbHome) {
        this(dbCommand, dbHost, dbPort, redirect, dbHome, null, null);
    }

    public DerbyControl(final String dbCommand, final String dbHost, final String dbPort, final String redirect, final String dbUser, final String dbPassword) {
        this(dbCommand, dbHost, dbPort, redirect, null, dbUser, dbPassword);
    }

    /**
     * This method invokes the Derby's NetworkServerControl to start/stop/ping
     * the database.
     */
    private void invokeNetworkServerControl() {
        try {
            Class networkServer = Class.forName("org.apache.derby.drda.NetworkServerControl");
            Method networkServerMethod = networkServer.getDeclaredMethod("main",
                    new Class[]{String[].class});
            Object[] paramObj;
            if (getDbUser() == null && getDbPassword() == null) {
                paramObj = new Object[]{new String[]{getDbCommand(), "-h", getDbHost(), "-p", getDbPort()}};
            } else {
                paramObj = new Object[]{new String[]{getDbCommand(), "-h", getDbHost(), "-p", getDbPort(), "-user", getDbUser(), "-password", getDbPassword()}};
            }

            networkServerMethod.invoke(networkServer, paramObj);
        } catch (Throwable t) {
            t.printStackTrace();
            Runtime.getRuntime().exit(2);
        }
    }

    @Override
    public String getLogFileName() {
        return DB_LOG_FILENAME;
    }

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("paramters not specified.");
            System.out.println("DerbyControl <derby command> <derby host> <derby port> <derby home> <redirect output>");
            System.exit(1);
        }

        DerbyControl derbyControl = null;
        if (args.length == 3) {
            derbyControl = new DerbyControl(args[0], args[1], args[2]);
        } else if (args.length == 4) {
            derbyControl = new DerbyControl(args[0], args[1], args[2], args[3]);
        } else if (args.length == 5) {
            derbyControl = new DerbyControl(args[0], args[1], args[2], args[3], args[4]);
        } else if (args.length > 5) {
            derbyControl = new DerbyControl(args[0], args[1], args[2], args[3], args[4], args[5]);
        }
        if (derbyControl != null) {
            derbyControl.invokeNetworkServerControl();
        }
    }
}
