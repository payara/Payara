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

package org.glassfish.paas.mysqldbplugin;

import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.SQLExec.OnError;

/**
 * @author Shalini M
 */
@Service
public class MySQLDbProvisioner implements DatabaseProvisioner {

    @Inject
    private ClassLoaderHierarchy clh;

    private static final String MYSQL_USERNAME = "root";
    private static final String MYSQL_PASSWORD = "mysql";

    private String mysqlDatabaseName = "foo";

    private static Logger logger = Logger.getLogger(MySQLDbProvisioner.class.getName());

    public boolean handles(Properties metaData) {
        return true;
    }

    public void initialize(Properties properties) {
    }

    public void startDatabase(String ipAddress) {
    }

    public void stopDatabase(String ipAddress) {
    }

    public String getDefaultServiceName() {
        return "default-mysql-db-service";
    }

    public String getVendorName() {
        return "MYSQL";
    }

    public Properties getDefaultConnectionProperties() {
        Properties properties = new Properties();
        properties.put(DatabaseProvisioner.USER, MYSQL_USERNAME);
        properties.put(DatabaseProvisioner.PASSWORD, MYSQL_PASSWORD);
        properties.put(DatabaseProvisioner.DATABASENAME, mysqlDatabaseName);
        properties.put(DatabaseProvisioner.RESOURCE_TYPE, "javax.sql.XADataSource");
        properties.put(DatabaseProvisioner.CLASSNAME, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
        properties.put("createDatabaseIfNotExist", "true");
        return properties;
    }

    public void setDatabaseName(String databaseName) {
        mysqlDatabaseName = databaseName;
    }

    public String getDatabaseName() {
        return mysqlDatabaseName;
    }

    private void executeTask(Properties dbProps, String sqlFile) {
            Project project = new Project();
            project.init();
            SQLExec task = new SQLExec();
            SQLExec.OnError error = new SQLExec.OnError();
            error.setValue("continue");
            task.setDriver("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + dbProps.getProperty("serverName") +
                    ":" + dbProps.getProperty("port") + "/" +
                    dbProps.getProperty(DatabaseProvisioner.DATABASENAME) +
                    "?createDatabaseIfNotExist=true";
            task.setUrl(url);
            task.setUserid(dbProps.getProperty(DatabaseProvisioner.USER));
            task.setPassword(dbProps.getProperty(DatabaseProvisioner.PASSWORD));
            if(sqlFile == null) {
                task.addText("SELECT '1'");
            } else {
                task.setSrc(new File(sqlFile));
            }
            task.setOnerror(error);
            Path path = new Path(project, clh.getCommonClassPath());
            path.addJavaRuntime();
            task.setClasspath(path);
            task.setProject(project);
            task.setAutocommit(true);
            task.execute();
    }

    public void createDatabase(Properties dbProps) {
        try {
            System.out.println("Creating Database");
            executeTask(dbProps, null);
            System.out.println("Created database");
        } catch(Exception ex) {
            logger.log(Level.WARNING, "Database creation failed with exception : " + ex);
        }
    }

    public void executeInitSql(Properties dbProps, String sqlFile) {
        try {
            System.out.println("executing init-sql : " + sqlFile);
            executeTask(dbProps, sqlFile);
            System.out.println("Completed executing init-sql : " + sqlFile);
        } catch(Exception ex) {
            logger.log(Level.WARNING, "Init SQL execution [ " + sqlFile + " ] failed with exception : " + ex);
        }
    }
}
