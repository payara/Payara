/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.paas.dbspecommon.DatabaseSPEBase;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import java.io.File;
import java.io.IOException;
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
    private static Logger logger = Logger.getLogger(DerbyPlugin.class.getName());

    public String getDefaultServiceName() {
        return "default-derby-db-service";
    }

    @Override
    public void executeInitSql(Properties dbProps, String sqlFile) {
        try {
            logger.log(Level.INFO, "Executing init-sql : " + sqlFile);
            String url = "jdbc:derby://" + dbProps.getProperty(HOST) + ":" +
                    dbProps.getProperty(PORT) + "/" +
                    dbProps.getProperty(DATABASENAME) + ";create=true";
            executeAntTask(dbProps, "org.apache.derby.jdbc.ClientDriver", url, sqlFile, true);
            logger.log(Level.INFO, "Completed executing init-sql : " + sqlFile);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Init SQL execution [ " + sqlFile + " ] failed with exception : " + ex);
        }
    }

    @Override
    public void createDatabase(Properties dbProps) {
        try {
            logger.log(Level.INFO, "Creating Database: " + dbProps.getProperty(DATABASENAME));
            String url = "jdbc:derby://" + dbProps.getProperty(HOST) + ":" +
                    dbProps.getProperty(PORT) + "/" +
                    dbProps.getProperty(DATABASENAME) + ";create=true";
            String sql = "VALUES(1)";
            executeAntTask(dbProps, "org.apache.derby.jdbc.ClientDriver", url, sql, false);
            logger.log(Level.INFO, "Created database : " + dbProps.getProperty(DATABASENAME));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Database creation failed with exception : " + ex);
        }
    }

    @Override
    protected Properties getServiceProperties(String ipAddress, String databaseName) {
        Properties defaultConnPoolProperties = getDefaultConnectionProperties();
        Properties serviceProperties = new Properties();
        serviceProperties.putAll(defaultConnPoolProperties);
        serviceProperties.put(HOST, ipAddress);
        serviceProperties.put(PORT, DERBY_PORT);
        if (databaseName != null && databaseName.trim().length() > 0) {
            serviceProperties.put(DATABASENAME, databaseName);
        }
        return serviceProperties;
    }

    protected void setDatabaseName(String databaseName) {
        derbyDatabaseName = databaseName;
    }

    protected String getDatabaseName() {
        return derbyDatabaseName;
    }

    public Properties getDefaultConnectionProperties() {
        Properties properties = new Properties();
        properties.put(USER, DERBY_USERNAME);
        properties.put(PASSWORD, DERBY_PASSWORD);
        properties.put(DATABASENAME, derbyDatabaseName);
        properties.put("CONNECTIONATTRIBUTES", ";create\\=true");
//        properties.put(DatabaseProvisioner.PORTNUMBER, DERBY_PORT);
        properties.put(RESOURCE_TYPE, "javax.sql.XADataSource");
        properties.put(CLASSNAME, "org.apache.derby.jdbc.ClientXADataSource");
        return properties;
    }

    public void startDatabase(VirtualMachine virtualMachine) {
        runAsadminCommand("start-database", virtualMachine);
    }

    public void stopDatabase(VirtualMachine virtualMachine) {
        runAsadminCommand("stop-database", virtualMachine);
    }

    public void runAsadminCommand(String commandName, VirtualMachine virtualMachine) {
        if (virtualMachine.getMachine() == null) {
            return;
        }
        String installDir = virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR);
        String[] args = {installDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin ", commandName};
        try {
            RuntimeContext.logger.info("Virtual Machine " + virtualMachine.getName() + " output : " +
                    virtualMachine.executeOn(args));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception during " + commandName + "  : " + e);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Exception during " + commandName + "  :" + e);
        }
    }
}
