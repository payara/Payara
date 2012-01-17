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
package org.glassfish.paas.mysqldbplugin;

import com.sun.logging.LogDomains;
import org.glassfish.paas.dbspecommon.DatabaseSPEBase;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin for MySQL Database Service
 *
 * @author Shalini M
 */
@Scoped(PerLookup.class)
@Service
public class MySQLDBPlugin  extends DatabaseSPEBase {

    private String mysqlDatabaseName = "foo";
    private static final String MYSQL_USERNAME = "root";
    private static final String MYSQL_PASSWORD = "mysql";
    // TODO :: grab the actual port.
    private static final String MYSQL_PORT = "3306";
    private static Logger logger = LogDomains.getLogger(MySQLDBPlugin.class, LogDomains.PAAS_LOGGER);
    private static final String MYSQL_DRIVER_CLASSNAME = "com.mysql.jdbc.Driver";

    public String getDefaultServiceName() {
        return "default-mysql-db-service";
    }

    @Override
    public void executeInitSql(Properties dbProps, String sqlFile) {
        try {
            logger.log(Level.INFO, "mysqldb.spe.init_sql.exec.start", sqlFile);
            executeSql(dbProps, MYSQL_DRIVER_CLASSNAME, sqlFile);
            logger.log(Level.INFO, "mysqldb.spe.init_sql.exec.stop", sqlFile);
        } catch (Exception ex) {
            Object[] args = new Object[] {sqlFile, ex};
            logger.log(Level.WARNING, "mysqldb.spe.init_sql.fail.ex", args);
        }
    }

    @Override
    public void executeTearDownSql(Properties dbProps, String sqlFile) {
        try {
            logger.log(Level.INFO, "mysqldb.spe.tear_down_sql.exec.start", sqlFile);
            executeSql(dbProps, MYSQL_DRIVER_CLASSNAME, sqlFile);
            logger.log(Level.INFO, "mysqldb.spe.tear_down_sql.exec.stop", sqlFile);
        } catch (Exception ex) {
            Object[] args = new Object[] {sqlFile, ex};
            logger.log(Level.WARNING, "mysqldb.spe.tear_down_sql.fail.ex", args);
        }
    }

    @Override
    public void createDatabase(Properties dbProps) {
        try {
            logger.log(Level.INFO, "mysqldb.spe.custom_db_creation.exec.start",
                    dbProps.getProperty(DATABASENAME));
            String url = dbProps.getProperty(URL);
            String sql = "SELECT '1'";
            executeAntTask(dbProps, "com.mysql.jdbc.Driver", url, sql, false);
            logger.log(Level.INFO, "mysqldb.spe.custom_db_creation.exec.stop",
                    dbProps.getProperty(DATABASENAME));
        } catch (Exception ex) {
            Object[] args = new Object[] {dbProps.getProperty(DATABASENAME), ex};
            logger.log(Level.WARNING, "mysqldb.spe.custom_db_creation.fail.ex", args);
        }
    }

    @Override
    protected Properties getServiceProperties(String ipAddress) {
        Properties serviceProperties = new Properties();
        serviceProperties.put(USER, MYSQL_USERNAME);
        serviceProperties.put(PASSWORD, MYSQL_PASSWORD);
        serviceProperties.put(DATABASENAME, getDatabaseName());
        serviceProperties.put(HOST, ipAddress);
        serviceProperties.put(PORT, MYSQL_PORT);
        serviceProperties.put(URL, "jdbc:mysql://" + ipAddress + ":" +
                MYSQL_PORT + "/" + getDatabaseName() + "?createDatabaseIfNotExist=true");
        serviceProperties.put(RESOURCE_TYPE, "javax.sql.XADataSource");
        serviceProperties.put(CLASSNAME, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
        serviceProperties.put("createDatabaseIfNotExist", "true");
        return serviceProperties;
    }

    protected void setDatabaseName(String databaseName) {
        mysqlDatabaseName = databaseName;
    }

    protected String getDatabaseName() {
        return mysqlDatabaseName;
    }

    public void startDatabase(VirtualMachine virtualMachine) {
        //no-op
    }

    public void stopDatabase(VirtualMachine virtualMachine) {
        //no-op
    }
}
