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
package org.glassfish.paas.javadbplugin;

import com.sun.enterprise.util.OS;
import com.sun.logging.LogDomains;
import org.glassfish.paas.dbspecommon.DatabaseSPEBase;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 * @author Shalini M
 */
@Scoped(PerLookup.class)
@Service
public class DerbyPlugin extends DatabaseSPEBase {

    private String derbyDatabaseName = "sample-db";
    private static final String DERBY_USERNAME = "APP";
    private static final String DERBY_PASSWORD = "APP";
    // TODO :: grab the actual port.
    private static final String DERBY_PORT = "1527";
    private static Logger logger = LogDomains.getLogger(DerbyPlugin.class, LogDomains.PAAS_LOGGER);
    private static final MessageFormat ASADMIN_COMMAND = new MessageFormat(
            "{0}" + File.separator + "lib" + File.separator + "nadmin" +
                    (OS.isWindows() ? ".bat" : "")); // {0} must be install root.

    public String getDefaultServiceName() {
        return "default-derby-db-service";
    }

    @Override
    public void executeInitSql(Properties dbProps, String sqlFile) {
        try {
            logger.log(Level.INFO, "javadb.spe.init_sql.exec.start", sqlFile);
            String url = "jdbc:derby://" + dbProps.getProperty(HOST) + ":" +
                    dbProps.getProperty(PORT) + "/" +
                    dbProps.getProperty(DATABASENAME) + ";create=true";
            executeAntTask(dbProps, "org.apache.derby.jdbc.ClientDriver", url, sqlFile, true);
            logger.log(Level.INFO, "javadb.spe.init_sql.exec.stop", sqlFile);
        } catch (Exception ex) {
            Object[] args = new Object[] {sqlFile, ex};
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
            Object[] args = new Object[] {dbProps.getProperty(DATABASENAME), ex};
            logger.log(Level.WARNING, "javadb.spe.custom_db_creation.fail.ex", args);
        }
    }

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

    protected void setDatabaseName(String databaseName) {
        derbyDatabaseName = databaseName;
    }

    protected String getDatabaseName() {
        return derbyDatabaseName;
    }

    public void startDatabase(VirtualMachine virtualMachine) {
        runAsadminCommand("start-database", virtualMachine);
    }

    public void stopDatabase(VirtualMachine virtualMachine) {
        runAsadminCommand("stop-database", virtualMachine);
    }

    public void runAsadminCommand(String commandName, VirtualMachine virtualMachine) {
        String[] installDir = {virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR) +
                File.separator + "glassfish"};

        String[] args = {ASADMIN_COMMAND.format(installDir).toString(),
                commandName};
        try {
            String output = virtualMachine.executeOn(args);
            Object[] params = new Object[] {virtualMachine.getName(), output};
            logger.log(Level.INFO, "javadb.spe.asadmin_cmd_exec", params);
        } catch (IOException e) {
            Object[] params = new Object[] {commandName, e};
            logger.log(Level.WARNING, "javadb.spe.command_execution.fail.ex", params);
        } catch (InterruptedException e) {
            Object[] params = new Object[] {commandName, e};
            logger.log(Level.WARNING, "javadb.spe.command_execution.fail.ex", params);
        }
    }
}
