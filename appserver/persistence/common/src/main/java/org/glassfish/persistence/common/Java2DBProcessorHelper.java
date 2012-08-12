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

package org.glassfish.persistence.common;

import org.glassfish.persistence.common.database.DBVendorTypeHelper;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.ActionReport;
import com.sun.logging.LogDomains;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pramodg
 */
public class Java2DBProcessorHelper { 

    /** The logger */
    private final static Logger logger = LogDomains.getLogger(Java2DBProcessorHelper.class, LogDomains.PERSISTENCE_LOGGER);

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "org.glassfish.persistence.common.LogStrings", //NOI18N
         Java2DBProcessorHelper.class.getClassLoader());

    /**
     * Default DDL name prefix. Need to have something to avoid
     * generating hidden names when a suffix is added to an empty string.
     * E.g. <code>.dbschema</code> name can be difficult to find,
     * while <code>default.dbschema</code> will signal that the default
     * had been used.
     **/
    private final static String DEFAULT_NAME = "default"; // NOI18N

    /**
     * Key for storing and retrieving corresponding values.
     */
    private final static String APPLICATION_NAME = "org.glassfish.persistence.app_name_property"; // NOI18N

    /**
     * Key prefixes for storing and retrieving corresponding values.
     */
    private final static String PROCESSOR_TYPE = "org.glassfish.persistence.processor_type."; // NOI18N
    private final static String RESOURCE_JNDI_NAME = "org.glassfish.persistence.resource_jndi_name_property."; // NOI18N
    private final static String JDBC_FILE_LOCATION = "org.glassfish.persistence.jdbc_file_location_property."; // NOI18N
    private final static String CREATE_JDBC_FILE_NAME = "org.glassfish.persistence.create_jdbc_file_name_property."; // NOI18N
    private final static String DROP_JDBC_FILE_NAME = "org.glassfish.persistence.drop_jdbc_file_name_property."; // NOI18N
    private final static String CREATE_TABLE_VALUE = "org.glassfish.persistence.create_table_value_property."; // NOI18N
    private final static String DROP_TABLE_VALUE = "org.glassfish.persistence.drop_table_value_property."; // NOI18N
    
    private DeploymentContext ctx;
    private Properties deploymentContextProps;
    private ActionReport subReport;
    
    /**
     * True if this is instance is created for deploy
     */
    private  boolean deploy;


    private  Boolean cliCreateTables;
    private  Boolean cliDropAndCreateTables;
    private  Boolean cliDropTables;
    /**
     * Name with which the application is registered.
     */
    private String appRegisteredName;
    
    private String appDeployedLocation;
    private String appGeneratedLocation;

    /**
     * Creates a new instance of Java2DBProcessorHelper to be used to execute SQL 
     * statements only.
     * @param appName the name used for reporting purposes
     */
    public Java2DBProcessorHelper(String appName) {
        appRegisteredName = appName;
    }

    /**
     * Creates a new instance of Java2DBProcessorHelper.
     * Do not parse all the data until it's requested in the #init() call.
     * @param ctx the deployment context object.
     */
    public Java2DBProcessorHelper(DeploymentContext ctx) {
        this.ctx = ctx;

        OpsParams params = ctx.getCommandParameters(OpsParams.class);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> Origin: " + params.origin);
        }

        deploy = params.origin.isDeploy();

        deploymentContextProps = ctx.getModuleProps();
    }

    /**
     * Initializes the rest of the settings
     */
    public void init() {
        if (deploy) {
            // DeployCommandParameters are available only on deploy or deploy
            // part of redeploy
            DeployCommandParameters cliOverrides = 
                    ctx.getCommandParameters(DeployCommandParameters.class);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("---> cliOverrides " + cliOverrides);
            }

            cliCreateTables = cliOverrides.createtables;
            cliDropAndCreateTables = cliOverrides.dropandcreatetables;

            Application application = ctx.getModuleMetaData(Application.class);
            appRegisteredName = application.getRegistrationName();
            deploymentContextProps.setProperty(APPLICATION_NAME, appRegisteredName);

        } else {
            // UndeployCommandParameters are available only on undeploy or undeploy
            // part of redeploy. In the latter case, cliOverrides.droptables
            // is set from cliOverrides.dropandcreatetables passed to redeploy.
            UndeployCommandParameters cliOverrides = 
                    ctx.getCommandParameters(UndeployCommandParameters.class);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("---> cliOverrides " + cliOverrides);
            }

            cliDropTables = cliOverrides.droptables;
            appRegisteredName = deploymentContextProps.getProperty(APPLICATION_NAME);
        }

        try {
            appGeneratedLocation =
                ctx.getScratchDir("ejb").getCanonicalPath() + File.separator;
        } catch (Exception e) {
            throw new RuntimeException(
                I18NHelper.getMessage(messages,
                "Java2DBProcessorHelper.generatedlocation", //NOI18N
                appRegisteredName), e);
        }

        appDeployedLocation =
            ctx.getSource().getURI().getSchemeSpecificPart() + File.separator;

        ActionReport report = ctx.getActionReport();
        subReport = report.addSubActionsReport();
        subReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        
    }


    /**
     * Iterate over all "create" or "drop" ddl files and execute them.
     * Skip processing if the boolean argument is false.
     */
    public void createOrDropTablesInDB(boolean create, String type) {
        for (String key : deploymentContextProps.stringPropertyNames()) {
            if (key.startsWith(PROCESSOR_TYPE)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("---> key " + key);
                }

                if (!deploymentContextProps.getProperty(key).equals(type)) {
                    // These set of properties were created by a different processor type
                    continue;
                }

                String bundleName = key.substring(PROCESSOR_TYPE.length());
                String jndiName = deploymentContextProps.getProperty(RESOURCE_JNDI_NAME + bundleName);
                String fileName = null;
                if (create) {
                    if (getCreateTables(bundleName)) {
                        fileName = deploymentContextProps.getProperty(CREATE_JDBC_FILE_NAME + bundleName);
                    }
                } else {
                    if (getDropTables(bundleName)) {
                        fileName = deploymentContextProps.getProperty(DROP_JDBC_FILE_NAME + bundleName);
                    }
                } 
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("---> fileName " + fileName);
                    logger.fine("---> jndiName " + jndiName);
                }
                if (fileName == null) {
                        continue; // DDL execution is not required
                }

                File file = getDDLFile(getGeneratedLocation(bundleName) + fileName, true);
                if(file.exists()) {
                    executeDDLStatement(file, jndiName);
                } else {
                    logI18NWarnMessage(
                            (create)? "Java2DBProcessorHelper.cannotcreatetables" //NOI18N
                                    : "Java2DBProcessorHelper.cannotdroptables", //NOI18N
                            appRegisteredName, fileName, null);
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("<---");
                }
            }
        }
    }

       /**
        * Read the ddl file from the disk location.
        * @param fileName the string name of the file.
        * @param deploy true if this event results in creating tables.
        * @return the jdbc ddl file.
        */
    public File getDDLFile(String fileName, boolean deploy) {
        File file = null;        
        try {
            file = new File(fileName);   
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(I18NHelper.getMessage(messages,
                    ((deploy)? "Java2DBProcessorHelper.createfilename" //NOI18N
                    : "Java2DBProcessorHelper.dropfilename"), //NOI18N
                    file.getName()));
            }
        } catch (Exception e) {
            logI18NWarnMessage(
                 "Exception caught in Java2DBProcessorHelper.getDDLFile()", //NOI18N
                appRegisteredName, null, e);
        }
        return file;        
    }
    
    /**
     * Open a DDL file and execute each line as a SQL statement.
     * @param f the File object to use.
     * @param sql the Statement to use for execution.
     * @throws java.io.IOException if there is a problem with reading the file.
     */
    public void executeDDLs(File f, Statement sql)
            throws IOException {

        BufferedReader reader = null;
        StringBuffer warningBuf = new StringBuffer();

        try {
            reader = new BufferedReader(new FileReader(f));
            String s;
            while ((s = reader.readLine()) != null) {
                try {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(I18NHelper.getMessage(messages, 
                        "Java2DBProcessorHelper.executestatement", s)); //NOI18N
                    }
                    sql.execute(s);

                } catch(SQLException ex) {
                    String msg = getI18NMessage("Java2DBProcessorHelper.sqlexception", //NOI18N
                            s, null, ex);
                    logger.warning(msg);
                    warningBuf.append("\n\t").append(msg); // NOI18N
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch(IOException ex) {
                    // Ignore.
                }
            }
            if (warningBuf.length() > 0) {
                String warning = 
                        getI18NMessage("Java2DBProcessorHelper.tablewarning");
                warnUser(subReport, warning + warningBuf.toString());
            }
        }
    }

    public String getDeployedLocation() {
        return appDeployedLocation;
    }

    public String getAppRegisteredName() {
        return appRegisteredName;
    }

    /**
     * Returns createJdbcFileName
     */
    public String getCreateJdbcFileName(String bundleName) {
        return deploymentContextProps.getProperty(CREATE_JDBC_FILE_NAME + bundleName);
    }

    /**
     * Sets createJdbcFileName
     */
    public void setCreateJdbcFileName(String createJdbcFileName, String bundleName) {
        deploymentContextProps.setProperty(CREATE_JDBC_FILE_NAME + bundleName, createJdbcFileName);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + CREATE_JDBC_FILE_NAME + bundleName + " " + createJdbcFileName);
        }
    }

    /**
     * Returns dropJdbcFileName
     */
    public String getDropJdbcFileName(String bundleName) {
        return deploymentContextProps.getProperty(DROP_JDBC_FILE_NAME + bundleName);
    }

    /**
     * Sets dropJdbcFileName
     */
    public void setDropJdbcFileName(String dropJdbcFileName, String bundleName) {
        deploymentContextProps.setProperty(DROP_JDBC_FILE_NAME + bundleName, dropJdbcFileName);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + DROP_JDBC_FILE_NAME + bundleName + " " + dropJdbcFileName);
        }
    }

    /**
     * Returns jndiName
     */
    public String getJndiName(String bundleName) {
        return deploymentContextProps.getProperty(RESOURCE_JNDI_NAME + bundleName);
    }

    /**
     * Sets jndiName
     */
    public void setJndiName(String jndiName, String bundleName) {
        deploymentContextProps.setProperty(RESOURCE_JNDI_NAME + bundleName, jndiName);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + RESOURCE_JNDI_NAME + bundleName + " " + jndiName);
        }
    }

    /**
     * Sets this processor type
     */
    public void setProcessorType(String processorType, String bundleName) {
        deploymentContextProps.setProperty(PROCESSOR_TYPE + bundleName, processorType);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + PROCESSOR_TYPE + bundleName + " " + processorType);
        }
    }

    /**
     * Returns appGeneratedLocation or user defined value if the latter is specified
     */
    public String getGeneratedLocation(String bundleName) {
        String userFileLocation = deploymentContextProps.getProperty(JDBC_FILE_LOCATION + bundleName);
        return (userFileLocation != null)? userFileLocation : appGeneratedLocation;
    }

    /**
     * Sets the substitute for the internal location of the generated files
     */
    public void setGeneratedLocation(String generatedLocation, String bundleName) {
        deploymentContextProps.setProperty(JDBC_FILE_LOCATION + bundleName, generatedLocation);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + JDBC_FILE_LOCATION + bundleName + " " + generatedLocation);
        }
    }

    /**
     * @return true if cli overrides were set during deploy
     */
    public boolean hasDeployCliOverrides() {
        return (cliCreateTables != null || cliDropAndCreateTables != null);
    }

    /**
     * @return true if cli overrides were set during undeploy
     */
    public boolean hasUndeployCliOverrides() {
        return (cliDropTables != null);
    }

    /**
     * Create tables only on  deploy, and only if the CLI options cliCreateTables or
     * cliDropAndCreateTables are not set to false.
     * If those options are not set (null) the value is taken from the boolean parameter
     * provided by the caller.
     * @return true if tables are to be created.
     */
    public boolean getCreateTables(boolean param) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> param " + param);
            logger.fine("---> cliCreateTables " + cliCreateTables);
            logger.fine("---> cliDropAndCreateTables " + cliDropAndCreateTables);
        }

        return
                (cliCreateTables != null && cliCreateTables.equals(Boolean.TRUE))
                || (cliDropAndCreateTables != null && cliDropAndCreateTables.equals(Boolean.TRUE))
                || (cliCreateTables == null && cliDropAndCreateTables == null && param);

    }

    /**
     * Drop tables on undeploy and redeploy, if the corresponding CLI options 
     * cliDropAndCreateTables (for redeploy) or cliDropTables (for undeploy) are
     * not set to false.
     * If the corresponding option is not set the value is taken from the boolean parameter
     * provided by the caller.
     * @return true if the tables have to be dropped.
     */
    public boolean getDropTables(boolean param) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> param " + param);
            logger.fine("---> cliDropTables " + cliDropTables);
        }
        return
                (cliDropTables != null && cliDropTables.equals(Boolean.TRUE))
                || (cliDropTables == null && param);

    }

    /**
     * Calculate createTables value based on the parameter stored on deploy
     */
    public boolean getCreateTables(String bundleName) {
        return getCreateTables(Boolean.valueOf(deploymentContextProps.getProperty(CREATE_TABLE_VALUE + bundleName)));
    }

    /**
     * Store user defined value for create tables for future reference.
     */
    public void setCreateTablesValue(boolean createTablesValue, String bundleName) {
        deploymentContextProps.setProperty(CREATE_TABLE_VALUE + bundleName, ""+createTablesValue);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + CREATE_TABLE_VALUE + bundleName + " " + createTablesValue);
        }
    }

    /**
     * Calculate dropTables value based on the parameter stored on deploy
     */
    public boolean getDropTables(String bundleName) {
        return getDropTables(Boolean.valueOf(deploymentContextProps.getProperty(DROP_TABLE_VALUE + bundleName)));
    }

    /**
     * Store user defined value for drop tables for future reference.
     */
    public void setDropTablesValue(boolean dropTablesValue, String bundleName) {
        deploymentContextProps.setProperty(DROP_TABLE_VALUE + bundleName, ""+dropTablesValue);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("---> " + DROP_TABLE_VALUE + bundleName + " " + dropTablesValue);
        }
    }

    /**
     * Returns name prefix for DDL files extracted from the info instance by the
     * Sun-specific code.
     *
     * @param info the instance to use for the name generation.
     * @return name prefix as String.
     */
    public static String getDDLNamePrefix(Object info) {
        StringBuffer rc = new StringBuffer();

        if (info instanceof BundleDescriptor && !(info instanceof Application)) {
            BundleDescriptor bundle = (BundleDescriptor)info;
            rc.append(bundle.getApplication().getRegistrationName());

            Application application = bundle.getApplication();
            if (!application.isVirtual()) {
                String modulePath = bundle.getModuleDescriptor().getArchiveUri();
                int l = modulePath.length();

                // Remove ".jar" from the module's jar name.
                rc.append(DatabaseConstants.NAME_SEPARATOR).
                    append(modulePath.substring(0, l - 4));
            }

        } // no other option is available at this point.

        return (rc.length() == 0)? DEFAULT_NAME : rc.toString();
    }

    /**
     * Get the ddl files eventually executed
     * against the database. This method deals
     * with both create and drop ddl files.
     * @param fileName  the create or drop jdbc ddl file.
     * @param resourceName the jdbc resource name that would be used
     * to get a connection to the database.
     * @return true if the tables were successfully
     *    created/dropped from the database.
     */
    public boolean executeDDLStatement(File fileName, String resourceName) {
        boolean result = false;
        Connection conn = null;
        Statement sql = null;
        try {
            try {
                conn = getConnection(resourceName);
                sql = conn.createStatement();
                result = true;
            } catch (Exception ex) {
                cannotConnect(resourceName, ex);
            }

            if(result) {
                executeDDLs(fileName, sql);
            }
        } catch (IOException e) {
            fileIOError(appRegisteredName, e);
        } finally {
            if (sql != null) {
                try {
                    sql.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
            closeConn(conn);
        }
        return result;
    }

    /**
     * Get the DDL file and execute the statements.
     * @param fileNamePrefix the common prefix for the DDL file name
     * @param resourceName the jdbc resource name that would be used
     * to get a connection to the database.
     * @return true if the statements were successfully in the database.
     */
    public boolean executeDDLStatement(String fileNamePrefix, String resourceName) {
        File file = null;
        Connection conn = null;
        try {
            conn = getConnection(resourceName);
            DatabaseMetaData dbMetaData = conn.getMetaData();
            String vendorName = DBVendorTypeHelper.getDBType(
                    dbMetaData.getDatabaseProductName()).toLowerCase(Locale.ENGLISH);
            file = new File(fileNamePrefix + vendorName + DatabaseConstants.SQL_FILE_EXTENSION);
            logger.fine("===> File to use: " + file);
        } catch (IOException e) {
            fileIOError(appRegisteredName, e);
        } catch (Exception ex) {
            cannotConnect(resourceName, ex);
        } finally {
            closeConn(conn);
        }

        return executeDDLStatement(file, resourceName);
    }


    /** Get a Connection from the resource specified by the JNDI name
     * of a resource.
     * This connection is aquired from a non-transactional resource which does not
     * go through transaction enlistment/delistment.
     * The deployment processing is required to use only those connections.
     *
     * @param jndiName JNDI name of a resource for the connection.
     * @return a Connection.
     * @throws SQLException if can not get a Connection.
     */
    private Connection getConnection(String jndiName) throws Exception {
        // TODO - pass Habitat or ConnectorRuntime as an argument.
        // TODO - remove duplication with DeploymentHelper

        ServiceLocator habitat = Globals.getDefaultHabitat();
        ConnectorRuntime connectorRuntime = habitat.getService(ConnectorRuntime.class);
        DataSource ds = PersistenceHelper.lookupNonTxResource(connectorRuntime, ctx, jndiName);
        return ds.getConnection();
    }

    /**
     * Provide a warning message to the user about inability to connect to the
     * database.  The message is created from the cmpResource's JNDI name and
     * the exception.
     * @param connName the JNDI name for obtaining a connection
     * @param ex Exception which is cause for inability to connect.
     */
    private void cannotConnect(String connName, Throwable ex) {
        logI18NWarnMessage( "Java2DBProcessorHelper.cannotConnect",  
                connName,  null, ex);
    }
    
    /**
     * Provide a warning message to the user about inability to read a DDL file.
     */
    private void fileIOError(String regName, Throwable ex) {
        logI18NWarnMessage("Java2DBProcessorHelper.ioexception",  
                regName,  null, ex);
    }
    
    /**
     * Close the connection that was opened to the database
     * @param conn the database connection.
     */
    private void closeConn(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch(SQLException ex) {
                // Ignore.
            }
        }
    }
    
    /**
     * Provide a generic warning message to the user.
     */
    public void logI18NWarnMessage(
            String errorCode, String regName, 
            String fileName, Throwable ex) {
        String msg = getI18NMessage(errorCode, 
                regName, fileName, ex);
        logger.warning(msg);
        warnUser(subReport, msg);        
    }
    
    /**
     * Get the localized message for the error code.
     * @param errorCode 
     * @return i18ned message 
     */
    public String getI18NMessage(String errorCode) {
        return getI18NMessage(errorCode, null, null, null);
    }    

    /**
     * Get a generic localized message.
     */
    public String getI18NMessage(
            String errorCode, String regName, 
            String fileName, Throwable ex) {
        String msg = null;
        if(null != ex)
               msg = I18NHelper.getMessage(
                    messages, errorCode,  regName,  ex.toString());
        else if(null != fileName )
            msg = I18NHelper.getMessage(
                    messages, errorCode,  regName,  fileName); 
        else            
             msg = I18NHelper.getMessage(messages, errorCode);
        
        return msg;
    }

    /**
     * Provide a warning message to the user.  The message is appended to any
     * already-existing warning message text.
     * @param msg Message for user.
     */
    public static void warnUser(ActionReport report, String msg) {
        if (report != null) {
            StringBuffer sb = new StringBuffer();
            String s = report.getMessage();
            if (s != null) {
                sb.append(s);
            }
            sb.append("\n").append(msg); // NOI18N
            report.setMessage(sb.toString());
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
        }
    }

}
