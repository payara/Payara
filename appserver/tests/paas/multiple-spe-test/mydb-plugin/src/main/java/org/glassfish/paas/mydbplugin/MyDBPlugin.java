/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.mydbplugin;

import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ProcessExecutor;
import com.sun.logging.LogDomains;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.paas.javadbplugin.DerbyPlugin;
import org.glassfish.virtualization.spi.VirtualMachine;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sandhya Kripalani K
  */
@PerLookup
@Service
public class MyDBPlugin extends DerbyPlugin {

    @Inject
    private ServerContext serverContext;

    private String derbyDatabaseName = "sample-db";
    private static final String DERBY_USERNAME = "APP";
    private static final String DERBY_PASSWORD = "APP";
    // TODO :: grab the actual port.
    private static final String DERBY_PORT = "1528";
    private static Logger logger = LogDomains.getLogger(MyDBPlugin.class, LogDomains.PAAS_LOGGER);

    public String getDefaultServiceName() {
        return "default-myderby-db-service";
    }

    /*@Override
    public void executeInitSql(Properties dbProps, String sqlFile) {
        try {
            logger.log(Level.INFO, "javadb.spe.init_sql.exec.start", sqlFile);
            String url = "jdbc:derby://" + dbProps.getProperty(HOST) + ":" +
                    dbProps.getProperty(PORT) + "/" +
                    dbProps.getProperty(DATABASENAME) + ";create=true";
            executeAntTask(dbProps, "org.apache.derby.jdbc.ClientDriver", url, sqlFile, true);
            logger.log(Level.INFO, "javadb.spe.init_sql.exec.stop", sqlFile);
        } catch (Exception ex) {
            Object[] args = new Object[]{sqlFile, ex};
            logger.log(Level.WARNING, "javadb.spe.init_sql.fail.ex", args);
        }
    }

    @Override
    public void createDatabase(Properties dbProps) {
        try {
            logger.log(Level.INFO, "javadb.spe.custom_db_creation.exec.start", dbProps.getProperty(DATABASENAME));
            String url = "jdbc:derby://" + dbProps.getProperty(HOST) + ":" +
                    dbProps.getProperty(PORT) + "/" +
                    dbProps.getProperty(DATABASENAME) + ";create=true";
            String sql = "VALUES(1)";
            executeAntTask(dbProps, "org.apache.derby.jdbc.ClientDriver", url, sql, false);
            logger.log(Level.INFO, "javadb.spe.custom_db_creation.exec.stop", dbProps.getProperty(DATABASENAME));
        } catch (Exception ex) {
            Object[] args = new Object[]{dbProps.getProperty(DATABASENAME), ex};
            logger.log(Level.WARNING, "javadb.spe.custom_db_creation.fail.ex", args);
        }
    } */

    @Override
    protected Properties getServiceProperties(String ipAddress) {
        Properties serviceProperties = new Properties();
        serviceProperties.put(USER, DERBY_USERNAME);
        serviceProperties.put(PASSWORD, DERBY_PASSWORD);
        serviceProperties.put(HOST, ipAddress);
        serviceProperties.put(PORT, DERBY_PORT);
        serviceProperties.put(DATABASENAME, getDatabaseName());
        serviceProperties.put("CONNECTIONATTRIBUTES", ";create=true");
        serviceProperties.put(RESOURCE_TYPE, "javax.sql.XADataSource");
        serviceProperties.put(CLASSNAME, "org.apache.derby.jdbc.ClientXADataSource");
        return serviceProperties;
    }

    /*protected void setDatabaseName(String databaseName) {
        derbyDatabaseName = databaseName;
    }

    protected String getDatabaseName() {
        return derbyDatabaseName;
    }*/

    public void startDatabase(VirtualMachine virtualMachine) {
        //Non native mode
        if (virtualMachine.getMachine() != null) {
            runAsadminCommand(virtualMachine, "start-database", "--dbport", "1528");
        } else { //Native mode
            start(virtualMachine, false);
        }
    }

    public void stopDatabase(VirtualMachine virtualMachine) {
        //Non native mode
        if (virtualMachine.getMachine() != null) {
            runAsadminCommand(virtualMachine, "stop-database", "--dbport", "1528");
        } else {   //Native mode
            stop(virtualMachine);
        }
    }

    public void start(VirtualMachine virtualMachine, boolean firstStart) {

        String[] startdbArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                File.separator + "bin" + File.separator + "asadmin" + (OS.isWindows() ? ".bat" : ""), "start-database", "--dbport", "1528"};
        ProcessExecutor startDatabase = new ProcessExecutor(startdbArgs);

        try {
            startDatabase.execute();
        } catch (ExecException e) {
            e.printStackTrace();
        }
    }

    public void stop(VirtualMachine virtualMachine) {

        String[] stopdbArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                File.separator + "bin" + File.separator + "asadmin" + (OS.isWindows() ? ".bat" : ""), "stop-database", "--dbport", "1528"};
        ProcessExecutor stopDatabase = new ProcessExecutor(stopdbArgs);

        try {
            stopDatabase.execute();
        } catch (ExecException e) {
            e.printStackTrace();
        }
    }

    public void runAsadminCommand(VirtualMachine virtualMachine, String... parameters) {
        if (virtualMachine.getMachine() == null) {
            return;
        }
        String installDir = virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR);
        List<String> args = new ArrayList<String>();
        String asadmin = installDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin ";
        args.add(asadmin);
        args.addAll(Arrays.asList(parameters));

        try {
            String output = virtualMachine.executeOn(args.toArray(new String[args.size()]));
            Object[] params = new Object[]{virtualMachine.getName(), output};
            logger.log(Level.INFO, "javadb.spe.asadmin_cmd_exec", params);
        } catch (IOException e) {
            Object[] params = new Object[]{parameters.toString(), e};
            logger.log(Level.WARNING, "javadb.spe.command_execution.fail.ex", params);
        } catch (InterruptedException e) {
            Object[] params = new Object[]{parameters.toString(), e};
            logger.log(Level.WARNING, "javadb.spe.command_execution.fail.ex", params);
        }
    }
}
