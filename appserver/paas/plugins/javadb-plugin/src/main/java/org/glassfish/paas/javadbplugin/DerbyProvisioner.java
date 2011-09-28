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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.apache.tools.ant.types.Path;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.glassfish.paas.orchestrator.provisioning.util.RemoteCommandExecutor;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.apache.tools.ant.taskdefs.SQLExec.OnError;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 * @author Shalini M
 */
@Service
public class DerbyProvisioner implements DatabaseProvisioner {

    @Inject
    private RemoteCommandExecutor remoteCommandExecutor;

    private String glassFishInstallDir;
    private String userName;
    private String keyPairLocation;

    public static final String DERBY_LOCAL_KEYPAIR_LOCATION = "DERBY_LOCAL_KEYPAIR_LOCATION";

    public static final String DATABASE_PROVIDER = "DATABASE_PROVIDER";
    public static final String GLASSFISH_DERBY = "GLASSFISH_DERBY";
    public static final String DERBY_GLASSFISH_INSTALL_LOCATION = "DERBY_GLASSFISH_INSTALL_LOCATION";
    public static final String DERBY_INSTANCE_USER_NAME = "DERBY_INSTANCE_USER_NAME";

    private static final String DERBY_USERNAME = "APP";
    private static final String DERBY_PASSWORD = "APP";

    private static Logger logger = Logger.getLogger(DerbyProvisioner.class.getName());

    public boolean handles(Properties metaData) {
        String value = (String) metaData.get(DATABASE_PROVIDER);
        if (value != null && value.equals(GLASSFISH_DERBY)) {
            return true;
        }
        return false;
    }

    public void initialize(Properties properties) {
        userName = (String) properties.get(DERBY_INSTANCE_USER_NAME);
        glassFishInstallDir = (String) properties.get(DERBY_GLASSFISH_INSTALL_LOCATION);
        keyPairLocation = (String) properties.get(DERBY_LOCAL_KEYPAIR_LOCATION);
    }

    public void startDatabase(String ipAddress) {
        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "start-database ";
        String args[] = new String[]{userName, ipAddress, keyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
        System.out.println("Database started : " + ipAddress);
    }

    public void stopDatabase(String ipAddress) {
        String command = glassFishInstallDir + File.separator + "glassfish" +
                File.separator + "bin" + File.separator + "asadmin " + "stop-database ";
        String args[] = new String[]{userName, ipAddress, keyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
        System.out.println("Database stopped : " + ipAddress);
    }

    public String getDefaultServiceName() {
        return "default-derby-db-service";
    }

    public String getVendorName() {
        return GLASSFISH_DERBY;
    }

    public Properties getDefaultConnectionProperties() {
        Properties properties = new Properties();
        properties.put(DatabaseProvisioner.USER, DERBY_USERNAME);
        properties.put(DatabaseProvisioner.PASSWORD, DERBY_PASSWORD);
        properties.put(DatabaseProvisioner.DATABASENAME, "sample-db");
        properties.put("CONNECTIONATTRIBUTES", ";create\\=true");
//        properties.put(DatabaseProvisioner.PORTNUMBER, "1527");
        properties.put(DatabaseProvisioner.RESOURCE_TYPE, "javax.sql.XADataSource");
        properties.put(DatabaseProvisioner.CLASSNAME, "org.apache.derby.jdbc.ClientXADataSource");
        return properties;
    }

    public void executeInitSql(Properties dbProps, String sqlFile) {
        try {
            Project project = new Project();
            project.init();
            SQLExec task = new SQLExec();
            SQLExec.OnError error = new SQLExec.OnError();
            error.setValue("continue");
            task.setDriver("org.apache.derby.jdbc.ClientDriver");
            String url = "jdbc:derby://" + dbProps.getProperty("serverName") + ":" +
                    dbProps.getProperty("port") + "/" +
                    dbProps.getProperty(DatabaseProvisioner.DATABASENAME) + ";create=true;";
            task.setUrl(url);
            task.setUserid(dbProps.getProperty(DatabaseProvisioner.USER));
            task.setPassword(dbProps.getProperty(DatabaseProvisioner.PASSWORD));
            task.setSrc(new File(sqlFile));
            task.setOnerror(error);
            String derbyClient = new File(System.getProperty("com.sun.aas.installRoot")).getParent() +
                    File.separator + "javadb" + File.separator + "lib" + File.separator + "derbyclient.jar";
            Path path = new Path(project, derbyClient);
            path.addJavaRuntime();
            task.setClasspath(path);
            task.setProject(project);
            task.setAutocommit(true);
            task.execute();
        } catch(Exception ex) {
            logger.log(Level.WARNING, "Init SQL execution failed with exception : " + ex);
        }
    }
}
