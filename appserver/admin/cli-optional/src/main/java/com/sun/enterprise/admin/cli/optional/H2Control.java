/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class uses Java reflection to invoke H2 NetworkServerControl class. This
 * class is used to start/stop/ping h2 database. The reason for creating this
 * class instead of directly invoking NetworkServerControl from the
 * StartDatabaseCommand class is so that a separate JVM is launched when
 * starting the database and the control is return to CLI.
 */
public final class H2Control extends DBControl {

    public static final String DB_LOG_FILENAME = "h2.log";

    public static final String JDBC_DRIVER = "org.h2.Driver";

    public H2Control(final String dbCommand, final String dbHost, final String dbPort,
            final String redirect, final String dbHome, final String dbUser, final String dbPassword) {
        super(dbCommand, dbHost, dbPort, redirect, dbHome, dbUser, dbPassword);

        // Do not set h2.system.home if dbHome is empty
        if (getDbHome() != null && getDbHome().length() > 0) {
            System.setProperty("h2.system.home", getDbHome());
        }
        // Set the property to not overwrite log file
        System.setProperty("h2.infolog.append", "true");
    }

    public H2Control(final String dbCommand, final String dbHost, final String dbPort) {
        this(dbCommand, dbHost, dbPort, "true", null, null, null);
    }

    public H2Control(final String dbCommand, final String dbHost, final String dbPort, final String redirect) {
        this(dbCommand, dbHost, dbPort, redirect, null, null, null);
    }

    public H2Control(final String dbCommand, final String dbHost, final String dbPort, final String redirect, final String dbHome) {
        this(dbCommand, dbHost, dbPort, redirect, dbHome, null, null);
    }

    public H2Control(final String dbCommand, final String dbHost, final String dbPort, final String redirect, final String dbUser, final String dbPassword) {
        this(dbCommand, dbHost, dbPort, redirect, null, dbUser, dbPassword);
    }

    /**
     * This method invokes the H2's org.h2.tools.Server to start/stop/ping/sysinfo the database.
     */
    private void invokeServer() {
        try {
            Class serverClass = Class.forName("org.h2.tools.Server");
            String dbPassword = getDbPassword() == null ? "changeit" : getDbPassword();
            String url = "tcp://localhost:" + getDbPort();
            if (null != getDbCommand()) 
                switch (getDbCommand()) {
                    case "start": {
                        Method createTcpServer = serverClass.getDeclaredMethod("createTcpServer", 
                                new Class[]{String[].class});
                        Object[] paramObj = new Object[]{new String[]{"-tcpPort", getDbPort(), "-tcpPassword", dbPassword, "-ifNotExists"}};
                        Object server = createTcpServer.invoke(serverClass, paramObj);
                        serverClass.getDeclaredMethod("start").invoke(server);
                        System.out.println(serverClass.getDeclaredMethod("getStatus").invoke(server));
                        break;
                    }
                    case "ping":
                        Class.forName(JDBC_DRIVER);
                        try {
                            DriverManager
                                    .getConnection(String.format("jdbc:h2:%s/mem:management_db_%s", url, getDbPort()), "", dbPassword)
                                    .close();
                        } catch (SQLException sqle) {
                            Runtime.getRuntime().exit(2);
                        }
                        break;
                    case "sysinfo": {
                        Method createTcpServer = serverClass.getDeclaredMethod("createTcpServer", 
                                new Class[]{String[].class});
                        Object[] paramObj = new Object[]{new String[]{"-tcpPort", getDbPort(), "-tcpAllowOthers"}};
                        Object server = createTcpServer.invoke(serverClass, paramObj);
                        System.out.println(serverClass.getDeclaredMethod("getURL").invoke(server));
                        break;
                    }
                    case "shutdown": {
                        Method shutdownTcpServer = serverClass.getDeclaredMethod("shutdownTcpServer", 
                                new Class[]{String.class, String.class, boolean.class, boolean.class});
                        Object[] paramObj = new Object[]{url, dbPassword, true, true};
                        shutdownTcpServer.invoke(serverClass, paramObj);
                        break;
                    }
                default:
                    break;
            }
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
            System.out.println("H2Control <h2 command> <h2 host> <h2 port> <h2 home> <redirect output>");
            System.exit(1);
        }

        H2Control h2Control = null;
        if (args.length == 3) {
            h2Control = new H2Control(args[0], args[1], args[2]);
        } else if (args.length == 4) {
            h2Control = new H2Control(args[0], args[1], args[2], args[3]);
        } else if (args.length == 5) {
            h2Control = new H2Control(args[0], args[1], args[2], args[3], args[4]);
        } else if (args.length > 5) {
            h2Control = new H2Control(args[0], args[1], args[2], args[3], args[4], args[5]);
        }

        if (h2Control != null) {
            h2Control.invokeServer();
        }
    }
}
