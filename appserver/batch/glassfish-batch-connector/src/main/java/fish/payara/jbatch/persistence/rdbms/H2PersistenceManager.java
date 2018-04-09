/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

package fish.payara.jbatch.persistence.rdbms;

import static java.util.logging.Level.INFO;
import static org.glassfish.internal.api.Globals.getDefaultHabitat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;

import fish.payara.nucleus.requesttracing.RequestTracingService;

/**
 * H2 Persistence Manager
 */
public class H2PersistenceManager extends JBatchJDBCPersistenceManager implements H2JDBCConstants {

    private static final String CLASSNAME = H2PersistenceManager.class.getName();

    private static final Logger logger = Logger.getLogger(CLASSNAME);

    private IBatchConfig batchConfig = null;

    // h2 create table strings
    protected Map<String, String> createH2Strings;

    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {

        logger.log(Level.CONFIG, "Entering CLASSNAME.init(), batchConfig ={0}", batchConfig);

        this.batchConfig = batchConfig;

        schema = batchConfig.getDatabaseConfigurationBean().getSchema();
        jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();

        logger.log(Level.CONFIG, "JNDI name = {0}", jndiName);

        if (jndiName == null || jndiName.equals("")) {
            throw new BatchContainerServiceException("JNDI name is not defined.");
        }

        try {
            dataSource = (DataSource) new InitialContext().lookup(jndiName);
        } catch (NamingException e) {
            logger.log(Level.SEVERE, "Lookup failed for JNDI name: {0}.  One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.", jndiName);
            throw new BatchContainerServiceException(e);
        }

        // Load the table names and queries shared between different database types
        tableNames = getSharedTableMap(batchConfig);

        try {
            queryStrings = getSharedQueryMap(batchConfig);
        } catch (SQLException e1) {
            throw new BatchContainerServiceException(e1);
        }

        try {
            if (!isH2SchemaValid()) {
                setDefaultSchema();
            }
            checkH2Tables();

        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw new BatchContainerServiceException(e);
        }

        try {
            requestTracing = getDefaultHabitat().getService(RequestTracingService.class);
        } catch (NullPointerException ex) {
            logger.log(INFO,
                    "Error retrieving Request Tracing service "
                    + "during initialisation of JBatchJDBCPersistenceManager - NullPointerException");
        }

        logger.config("Exiting CLASSNAME.init()");
    }

    /**
     * Set the default schema to the username obtained from the connection based
     * on the datasource
     *
     * @return 
     * @throws java.sql.SQLException
     */
    @Override
    public String setDefaultSchema() throws SQLException {
        logger.finest("Entering setDefaultSchema");
        schema = "PUBLIC";
        logger.finest("Exiting setDefaultSchema");
        return schema;
    }

    /**
     * Checks if the schema exists in the database. If not connect to the
     * default schema
     *
     * @return true if the schema exists, false otherwise.
     * @throws SQLException
     */
    protected boolean isH2SchemaValid() throws SQLException {
        logger.entering(CLASSNAME, "isH2SchemaValid");

        try (Connection connection = getConnectionToDefaultSchema()) {
            try (ResultSet rs = connection.getMetaData().getSchemas()) {
                while (rs.next()) {
                    if (schema.equalsIgnoreCase(rs.getString("TABLE_SCHEM"))) {
                        logger.exiting(CLASSNAME, "isSchemaValid", true);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw e;
        }

        logger.exiting(CLASSNAME, "isH2SchemaValid", false);
        return false;
    }

    /**
     * Check if the h2 jbatch tables exist, if not create them
     *
     */
    private void checkH2Tables() throws SQLException {
        setCreateH2StringsMap(batchConfig);
        createH2TableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_CHECKPOINTDATA));

        createH2TableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_JOBINSTANCEDATA));

        createH2TableNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
                createH2Strings
                        .get(H2_CREATE_TABLE_EXECUTIONINSTANCEDATA));
        createH2TableNotExists(
                tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_STEPINSTANCEDATA));
        createH2TableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_JOBSTATUS));
        createH2TableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_STEPSTATUS));

    }

    /**
     * Create the H2 tables
     * 
     * Note: H2 has a configuration setting DATABASE_TO_UPPER which is set to true per default. 
     * So any table name is converted to upper case which is why you need to query for the table in upper case (or set DATABASE_TO_UPPER to false).
     *
     * @param tableName
     * @param createTableStatement
     * @throws java.sql.SQLException
     */
    protected void createH2TableNotExists(String tableName, String createTableStatement) throws SQLException {
        logger.entering(CLASSNAME, "createIfNotExists", new Object[]{tableName, createTableStatement});
        
        try (Connection connection = getConnection()) {
            try (ResultSet resultSet = connection.getMetaData().getTables(null, schema, tableName.toUpperCase(), null)) {
                if (!resultSet.next()) {
                    logger.log(INFO, "{0} table does not exists. Trying to create it.", tableName);
                    try (PreparedStatement statement = connection.prepareStatement(createTableStatement)) {
                        statement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw e;
        }

        logger.exiting(CLASSNAME, "createIfNotExists");
    }

    /**
     * Set the schema to the default schema or the schema defined at batch
     * configuration time
     *
     * @param connection
     * @throws SQLException
     */
    @Override
    protected void setSchemaOnConnection(Connection connection) throws SQLException {
        logger.log(Level.FINEST, "Entering {0}.setSchemaOnConnection()", CLASSNAME);
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryStrings.get(Q_SET_SCHEMA) + schema)) {
            preparedStatement.executeUpdate();
        }
        logger.log(Level.FINEST, "Exiting {0}.setSchemaOnConnection()", CLASSNAME);
    }
   
    @Override
    protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
        queryStrings = super.getSharedQueryMap(batchConfig);

        queryStrings.put(Q_SET_SCHEMA, "SET SCHEMA ");
        return queryStrings;
    }

    /**
     * Method invoked to insert the H2 create table strings into a hashmap
     *
     * @param batchConfig
     * @return 
     */
    protected Map<String, String> setCreateH2StringsMap(IBatchConfig batchConfig) {
        createH2Strings = new HashMap<>();
        createH2Strings.put(H2_CREATE_TABLE_CHECKPOINTDATA,
                "CREATE TABLE " + tableNames.get(CHECKPOINT_TABLE_KEY)
                + "(id VARCHAR(512),obj BLOB)");
        createH2Strings
                .put(H2_CREATE_TABLE_JOBINSTANCEDATA,
                        "CREATE TABLE "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + "("
                        + "jobinstanceid BIGINT NOT NULL IDENTITY PRIMARY KEY,"//CONSTRAINT JOBINSTANCE_PK
                        + "name VARCHAR(512)," + "apptag VARCHAR(512))");

        createH2Strings
                .put(H2_CREATE_TABLE_EXECUTIONINSTANCEDATA,
                        "CREATE TABLE "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + "("
                        + "jobexecid BIGINT NOT NULL IDENTITY PRIMARY KEY,"//CONSTRAINT JOBEXECUTION_PK
                        + "jobinstanceid BIGINT,"
                        + "createtime TIMESTAMP,"
                        + "starttime TIMESTAMP,"
                        + "endtime TIMESTAMP,"
                        + "updatetime TIMESTAMP,"
                        + "parameters BLOB,"
                        + "batchstatus VARCHAR(512),"
                        + "exitstatus VARCHAR(512),"
                        + "CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + "(jobinstanceid))");

        createH2Strings
                .put(H2_CREATE_TABLE_STEPINSTANCEDATA,
                        "CREATE TABLE "
                        + tableNames
                                .get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                        + "("
                        + "stepexecid BIGINT NOT NULL IDENTITY PRIMARY KEY,"//CONSTRAINT STEPEXECUTION_PK
                        + "jobexecid BIGINT,"
                        + "batchstatus VARCHAR(512),"
                        + "exitstatus VARCHAR(512),"
                        + "stepname VARCHAR(512),"
                        + "readcount INTEGER,"
                        + "writecount INTEGER,"
                        + "commitcount INTEGER,"
                        + "rollbackcount INTEGER,"
                        + "readskipcount INTEGER,"
                        + "processskipcount INTEGER,"
                        + "filtercount INTEGER,"
                        + "writeskipcount INTEGER,"
                        + "startTime TIMESTAMP,"
                        + "endTime TIMESTAMP,"
                        + "persistentData BLOB,"
                        + "CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + "(jobexecid))");

        createH2Strings
                .put(H2_CREATE_TABLE_JOBSTATUS,
                        "CREATE TABLE "
                        + tableNames.get(JOB_STATUS_TABLE_KEY)
                        + "("
                        + "id BIGINT CONSTRAINT JOBSTATUS_PK PRIMARY KEY,"
                        + "obj BLOB,"
                        + "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + " (jobinstanceid) ON DELETE CASCADE)");

        createH2Strings
                .put(H2_CREATE_TABLE_STEPSTATUS,
                        "CREATE TABLE "
                        + tableNames.get(STEP_STATUS_TABLE_KEY)
                        + "("
                        + "id BIGINT CONSTRAINT STEPSTATUS_PK PRIMARY KEY,"
                        + "obj BLOB,"
                        + "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES "
                        + tableNames
                                .get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                        + "(stepexecid) ON DELETE CASCADE)");

        return createH2Strings;
    }
}
