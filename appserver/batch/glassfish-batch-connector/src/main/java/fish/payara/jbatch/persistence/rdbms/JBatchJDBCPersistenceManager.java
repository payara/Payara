/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jbatch.persistence.rdbms;

import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.impl.PartitionedStepBuilder;
import com.ibm.jbatch.container.jobinstance.JobInstanceImpl;
import com.ibm.jbatch.container.jobinstance.JobOperatorJobExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.util.TCCLObjectInputStream;
import com.ibm.jbatch.spi.services.IBatchConfig;
import fish.payara.notification.requesttracing.RequestTraceSpanLog;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_PREFIX_PROPERTY;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_SUFFIX_PROPERTY;
import static org.glassfish.internal.api.Globals.getDefaultHabitat;

/**
 *
 * Base Persistence Manager Class. All Persistence Managers extend this base
 * persistence manager class
 */
public class JBatchJDBCPersistenceManager implements
        IPersistenceManagerService, JDBCQueryConstants, OracleJDBCConstants {

    private static final String CLASSNAME = JBatchJDBCPersistenceManager.class.getName();

    private static final Logger logger = Logger.getLogger(CLASSNAME);

    private IBatchConfig batchConfig = null;

    protected DataSource dataSource = null;
    protected String jndiName = null;
    protected String prefix = null;
    protected String suffix = null;

    protected String schema = "";

    // shared table name and query strings
    protected Map<String, String> tableNames;
    protected Map<String, String> queryStrings;

    // h2 create table strings
    protected Map<String, String> createH2Strings;

    protected RequestTracingService requestTracing;
    
    private static final String JAVA_EE_MODE = "Java EE mode, getting connection from data source";
    private static final String NULL_TAGGED = "<null>";

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl
	 * #init(com.ibm.jbatch.container.IBatchConfig)
     */
    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {

        logger.log(Level.CONFIG, "Entering CLASSNAME.init(), batchConfig ={0}", batchConfig);

        this.batchConfig = batchConfig;

        schema = batchConfig.getDatabaseConfigurationBean().getSchema();
        jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
        prefix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_PREFIX_PROPERTY, "");
        suffix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_SUFFIX_PROPERTY, "");

        logger.log(Level.CONFIG, "JNDI name = {0}", jndiName);

        if (jndiName == null || jndiName.equals("")) {
            throw new BatchContainerServiceException("JNDI name is not defined.");
        }

        try {
            dataSource = (DataSource) new InitialContext().lookup(jndiName);
        } catch (NamingException e) {
            logger.severe(
                    "Lookup failed for JNDI name: " + jndiName + ". "
                    + " One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
            throw new BatchContainerServiceException(e);
        }

        // Load the table names and queries shared between different database types
        tableNames = getSharedTableMap();

        try {
            queryStrings = getSharedQueryMap(batchConfig);
        } catch (SQLException e1) {
            throw new BatchContainerServiceException(e1);
        }

        try {
            if (!isSchemaValid()) {
                setDefaultSchema();
            }
            checkTables();

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

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl
     * #getCheckpointData
     * (com.ibm.ws.batch.container.checkpoint.CheckpointDataKey)
     */
    @Override
    public CheckpointData getCheckpointData(CheckpointDataKey key) {
        logger.entering(CLASSNAME, "getCheckpointData", key == null ? NULL_TAGGED : key);

        CheckpointData checkpointData = key == null ? null : queryCheckpointData(key.getCommaSeparatedKey());

        logger.exiting(CLASSNAME, "getCheckpointData", checkpointData == null ? NULL_TAGGED : checkpointData);
        return checkpointData;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl
     * #updateCheckpointData
     * (com.ibm.ws.batch.container.checkpoint.CheckpointDataKey,
     * com.ibm.ws.batch.container.checkpoint.CheckpointData)
     */
    @Override
    public void updateCheckpointData(CheckpointDataKey key, CheckpointData value) {
        logger.entering(CLASSNAME, "updateCheckpointData", new Object[]{key, value});
        
        CheckpointData data = queryCheckpointData(key.getCommaSeparatedKey());
        if (data != null) {
            updateCheckpointData(key.getCommaSeparatedKey(), value);
        } else {
            createCheckpointData(key, value);
        }

        logger.exiting(CLASSNAME, "updateCheckpointData");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl
     * #createCheckpointData
     * (com.ibm.ws.batch.container.checkpoint.CheckpointDataKey,
     * com.ibm.ws.batch.container.checkpoint.CheckpointData)
     */
    @Override
    public void createCheckpointData(CheckpointDataKey key, CheckpointData value) {
        logger.entering(CLASSNAME, "createCheckpointData", new Object[]{key, value});

        insertCheckpointData(key.getCommaSeparatedKey(), value);

        logger.exiting(CLASSNAME, "createCheckpointData");
    }

    /**
     * Set the default schema to the username obtained from the connection based
     * on the datasource
     *
     * @return the name of the default database schema
     * @throws SQLException
     */
    public String setDefaultSchema() throws SQLException {
        logger.finest("Entering setDefaultSchema");

        logger.finest(JAVA_EE_MODE);
        try (Connection connection = dataSource.getConnection()) {

            DatabaseMetaData dbmd = connection.getMetaData();
            schema = dbmd.getUserName();
            String databaseProductName = dbmd.getDatabaseProductName().toLowerCase();
            if (databaseProductName.contains("mysql")) {
                schema = "test";
            } else if (databaseProductName.contains("postgres")) {
                schema = "public";
            } else if (databaseProductName.contains("h2")) {
                schema = "PUBLIC";
            }

        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw e;
        }

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
    protected boolean isSchemaValid() throws SQLException {

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
    protected void checkTables() throws SQLException {
        setCreateH2StringsMap(tableNames);
        createTableIfNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_CHECKPOINTDATA));

        createTableIfNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_JOBINSTANCEDATA));

        createTableIfNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
                createH2Strings
                        .get(H2_CREATE_TABLE_EXECUTIONINSTANCEDATA));
        createTableIfNotExists(
                tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_STEPINSTANCEDATA));
        createTableIfNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_JOBSTATUS));
        createTableIfNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
                createH2Strings.get(H2_CREATE_TABLE_STEPSTATUS));

    }

    public void createTables(DataSource dataSource, BatchRuntimeConfiguration batchRuntimeConfiguration) {
        this.dataSource = dataSource;
        prefix = batchRuntimeConfiguration.getTablePrefix();
        suffix = batchRuntimeConfiguration.getTableSuffix();
        schema = batchRuntimeConfiguration.getSchemaName();
        tableNames = getSharedTableMap();

        try {
            if (!isSchemaValid()) {
                setDefaultSchema();
            }
            checkTables();
        } catch (SQLException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
    }

    /**
     * Create the jbatch tables if they do not exist
     *
     * @param tableName
     * @param createTableStatement
     * @throws java.sql.SQLException
     */
    protected void createTableIfNotExists(String tableName, String createTableStatement) throws SQLException {
        logger.entering(CLASSNAME, "createTableIfNotExists", new Object[]{tableName, createTableStatement});

        try (Connection connection = getConnection()) {
            if (!checkIfTableExists(dataSource, tableName, schema)) {
                logger.log(INFO, "{0} table does not exists. Trying to create it.", tableName);
                try (PreparedStatement statement = connection.prepareStatement(createTableStatement)) {
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw e;
        } finally {
            logger.exiting(CLASSNAME, "createTableIfNotExists");
        }
    }

    /**
     *
     * Note: H2 has a configuration setting DATABASE_TO_UPPER which is set to
     * true per default. So any table name is converted to upper case which is
     * why you need to query for the table in upper case (or set
     * DATABASE_TO_UPPER to false).
     *
     * @param dSource
     * @param tableName
     * @param schemaName
     */
    public boolean checkIfTableExists(DataSource dSource, String tableName, String schemaName) {
        boolean result = true;
        dataSource = dSource;

        try (Connection connection = dataSource.getConnection()) {
            schema = schemaName;

            if (!isSchemaValid()) {
                setDefaultSchema();
            }

            try (ResultSet resultSet = connection.getMetaData().getTables(null, schema, tableName.toUpperCase(), null)) {
                if (!resultSet.next()) {
                    result = false;
                }
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
        }

        return result;
    }

    /**
     * Retrieve the number of rows in the resultset. This method is used to
     * check if a table exists in a given schema
     */
    public int getTableRowCount(ResultSet resultSet) throws SQLException {
        int rowcount = 0;

        try {
            if (resultSet.last()) {
                rowcount = resultSet.getRow();
                resultSet.beforeFirst();
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw e;
        }

        return rowcount;
    }

    /**
     * Executes the provided SQL statement
     *
     * @param statement
     * @throws SQLException
     */
    protected void executeStatement(String statement) throws SQLException {
        logger.entering(CLASSNAME, "executeStatement", statement);

        try (Connection connection = getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw e;
        }

        logger.exiting(CLASSNAME, "executeStatement");
    }

    /**
     * Get a connection from the datasource
     *
     * @return the database connection and sets the schema
     *
     * @throws SQLException
     */
    protected Connection getConnection() throws SQLException {
        logger.log(Level.FINEST, "Entering: {0}.getConnection", CLASSNAME);

        logger.finest(JAVA_EE_MODE);
        Connection connection = dataSource.getConnection();
        logger.log(Level.FINEST, "autocommit={0}", connection.getAutoCommit());

        setSchemaOnConnection(connection);

        logger.log(Level.FINEST, "Exiting: {0}.getConnection() with connection ={1}", new Object[]{CLASSNAME, connection});
        return connection;
    }

    /**
     * @return the database connection. The schema is set to whatever default is
     * used by the datasource.
     * @throws SQLException
     */
    protected Connection getConnectionToDefaultSchema() throws SQLException {
        logger.finest("Entering getConnectionToDefaultSchema");
        Connection connection = null;

        logger.finest(JAVA_EE_MODE);
        try {
            connection = dataSource.getConnection();

        } catch (SQLException e) {
            logException("FAILED GETTING DATABASE CONNECTION", e);
            throw new PersistenceException(e);
        }
        logger.log(Level.FINEST, "autocommit={0}", connection.getAutoCommit());

        logger.log(Level.FINEST, "Exiting from getConnectionToDefaultSchema, conn= {0}", connection);
        return connection;
    }

    protected void logException(String msg, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        logger.log(SEVERE, "{0}; Exception stack trace: {1}", new Object[]{msg, sw});
    }

    /**
     * Set the schema to the default schema or the schema defined at batch
     * configuration time
     *
     * @param connection
     * @throws SQLException
     */
    protected void setSchemaOnConnection(Connection connection) throws SQLException {
        logger.log(Level.FINEST, "Entering {0}.setSchemaOnConnection()", CLASSNAME);

        if (!(connection.getMetaData().getDatabaseProductName().contains("Oracle"))) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SET SCHEMA " + schema)) {
                preparedStatement.executeUpdate();
            } finally {
                logger.log(Level.FINEST, "Exiting {0}.setSchemaOnConnection()", CLASSNAME);
            }
        }
    }

    /**
     * select data from DB table
     *
     * @param key - the IPersistenceDataKey object
     * @return List of serializable objects store in the DB table
     *
     * Ex. select id, obj from tablename where id = ?
     */
    protected CheckpointData queryCheckpointData(Object key) {
        logger.entering(CLASSNAME, "queryCheckpointData", new Object[]{key, SELECT_CHECKPOINTDATA});

        CheckpointData data = null;

        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(queryStrings.get(SELECT_CHECKPOINTDATA))) {
                statement.setObject(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        data = (CheckpointData) deserializeObject(resultSet.getBytes("obj"));
                    }
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }

        logger.exiting(CLASSNAME, "queryCheckpointData");
        return data;
    }

    /**
     * Insert data to DB table
     *
     * @param key - the IPersistenceDataKey object
     * @param value - serializable object to store
     *
     * Ex. insert into tablename values(?, ?)
     */
    protected <T> void insertCheckpointData(Object key, T value) {
        logger.entering(CLASSNAME, "insertCheckpointData", new Object[]{key, value});

        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(queryStrings.get(INSERT_CHECKPOINTDATA))) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (ObjectOutputStream oout = new ObjectOutputStream(baos)) {
                    oout.writeObject(value);

                    statement.setObject(1, key);
                    statement.setBytes(2, baos.toByteArray());
                    statement.executeUpdate();
                }

            }
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }

        logger.exiting(CLASSNAME, "insertCheckpointData");
    }

    /**
     * update data in DB table
     *
     * @param value - serializable object to store
     * @param key - the IPersistenceDataKey object
     * @param query - SQL statement to execute.
     *
     * Ex. update tablename set obj = ? where id = ?
     */
    protected void updateCheckpointData(Object key, CheckpointData value) {
        logger.entering(CLASSNAME, "updateCheckpointData", new Object[]{key,
            value});

        try (Connection conn = getConnection();
                PreparedStatement statement
                = conn.prepareStatement(queryStrings.get(UPDATE_CHECKPOINTDATA));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(baos)) {

            oout.writeObject(value);

            byte[] b = baos.toByteArray();

            statement.setBytes(1, b);
            statement.setObject(2, key);
            statement.executeUpdate();

        } catch (SQLException | IOException e) {
            logger.severe(e.getLocalizedMessage());
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "updateCheckpointData");
    }

    /**
     * closes connection, result set and statement
     *
     * @param conn - connection object to close
     * @param rs - result set object to close
     * @param statement - statement object to close
     */
    protected void cleanupConnection(Connection conn, ResultSet rs,
            PreparedStatement statement) {

        logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Entering",
                new Object[]{conn, rs == null ? NULL_TAGGED : rs,
                    statement == null ? NULL_TAGGED : statement});

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new PersistenceException(e);
		        }
		    }
		}
		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Exiting");
	}

	/**
	 * closes connection and statement
	 *
     * @param conn - connection object to close
     * @param statement - statement object to close
     */
    protected void cleanupConnection(Connection conn,
            PreparedStatement statement) {

        logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Entering",
                new Object[]{conn, statement});

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new PersistenceException(e);
                }
            }
        }
        logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Exiting");
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String jobName, String appTag) {

        int count;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOBOPERATOR_GET_JOB_INSTANCE_COUNT))) {
            statement.setString(1, jobName);
            statement.setString(2, appTag);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                count = rs.getInt("jobinstancecount");
            }

        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw new PersistenceException(e);
        }
        return count;
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String jobName) {

        int count;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(SELECT_JOBINSTANCEDATA_COUNT))) {
            statement.setString(1, jobName);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                count = rs.getInt("jobinstancecount");
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return count;
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String jobName,
            String appTag, int start, int count) {
        List<Long> data = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOBOPERATOR_GET_JOB_INSTANCE_IDS))) {
            statement.setObject(1, jobName);
            statement.setObject(2, appTag);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("jobinstanceid");
                    data.add(id);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        if (!data.isEmpty()) {
            try {
                return data.subList(start, start + count);
            } catch (IndexOutOfBoundsException oobEx) {
                return data.subList(start, data.size());
            }
        } else {
            return data;
        }
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String jobName, int start,
            int count) {

        List<Long> data = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(SELECT_JOBINSTANCEDATA_IDS))) {

            statement.setObject(1, jobName);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("jobinstanceid");
                    data.add(id);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        if (!data.isEmpty()) {
            try {
                return data.subList(start, start + count);
            } catch (IndexOutOfBoundsException oobEx) {
                return data.subList(start, data.size());
            }
        } else {
            return data;
        }
    }

    @Override
    public Map<Long, String> jobOperatorGetExternalJobInstanceData() {

        Map<Long, String> data = new HashMap<>();

        final String filter = "not like '"
                + PartitionedStepBuilder.JOB_ID_SEPARATOR + "%'";

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA) + filter)) {
            // Filter out 'subjob' parallel execution entries which start with
            // the special character
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("jobinstanceid");
                    String name = rs.getString("name");
                    data.put(id, name);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return data;
    }

    @Override
    public Timestamp jobOperatorQueryJobExecutionTimestamp(long key,
            TimestampType timestampType) {

        Timestamp createTimestamp = null;
        Timestamp endTimestamp = null;
        Timestamp updateTimestamp = null;
        Timestamp startTimestamp = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP))) {
            statement.setObject(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    createTimestamp = rs.getTimestamp(1);
                    endTimestamp = rs.getTimestamp(2);
                    updateTimestamp = rs.getTimestamp(3);
                    startTimestamp = rs.getTimestamp(4);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        switch (timestampType) {
            case CREATE:
                return createTimestamp;
            case END:
                return endTimestamp;
            case LAST_UPDATED:
                return updateTimestamp;
            case STARTED:
                return startTimestamp;
            default:
                throw new IllegalArgumentException("Unexpected enum value.");
        }
    }

    @Override
    public String jobOperatorQueryJobExecutionBatchStatus(long key) {

        String status = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS))) {
            statement.setLong(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    status = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return status;
    }

    @Override
    public String jobOperatorQueryJobExecutionExitStatus(long key) {

        String status = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS))) {
            statement.setLong(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    status = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return status;
    }

    @Override
    public long jobOperatorQueryJobExecutionJobInstanceId(long executionID)
            throws NoSuchJobExecutionException {

        long jobinstanceID = 0;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID))) {
            statement.setLong(1, executionID);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    jobinstanceID = rs.getLong("jobinstanceid");
                } else {
                    String msg = "Did not find job instance associated with executionID ="
                            + executionID;
                    logger.fine(msg);
                    throw new NoSuchJobExecutionException(msg);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return jobinstanceID;
    }

    @Override
    public Properties getParameters(long executionId)
            throws NoSuchJobExecutionException {
        Properties props = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings.get(GET_PARAMETERS))) {
            statement.setLong(1, executionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    // get the object based data
                    byte[] buf = rs.getBytes("parameters");
                    props = (Properties) deserializeObject(buf);
                } else {
                    String msg = "Did not find table entry for executionID ="
                            + executionId;
                    logger.fine(msg);
                    throw new NoSuchJobExecutionException(msg);
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }

        return props;

    }

    @Override
    public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(long instanceId) {

        Map<String, StepExecution> data = new HashMap<>();

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(MOST_RECENT_STEPS_FOR_JOB))) {
            statement.setLong(1, instanceId);
            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    String stepname = null;

                    stepname = rs.getString("stepname");
                    if (data.containsKey(stepname)) {
                        continue;
                    } else {
                        long jobexecid = rs.getLong("jobexecid");
                        long stepexecid = rs.getLong("stepexecid");
                        String batchstatus = rs.getString("batchstatus");
                        String exitstatus = rs.getString("exitstatus");
                        long readCount = rs.getLong("readcount");
                        long writeCount = rs.getLong("writecount");
                        long commitCount = rs.getLong("commitcount");
                        long rollbackCount = rs.getLong("rollbackcount");
                        long readSkipCount = rs.getLong("readskipcount");
                        long processSkipCount = rs.getLong("processskipcount");
                        long filterCount = rs.getLong("filtercount");
                        long writeSkipCount = rs.getLong("writeSkipCount");
                        Timestamp startTS = rs.getTimestamp("startTime");
                        Timestamp endTS = rs.getTimestamp("endTime");

                        // get the object based data
                        Serializable persistentData = null;
                        byte[] pDataBytes = rs.getBytes("persistentData");
                        if (pDataBytes != null) {
                            try (ObjectInputStream objectIn
                                    = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes))) {
                                persistentData = (Serializable) objectIn.readObject();
                            }
                        }

                        StepExecutionImpl stepEx = new StepExecutionImpl(jobexecid, stepexecid);
                        stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                        stepEx.setExitStatus(exitstatus);
                        stepEx.setStepName(stepname);
                        stepEx.setReadCount(readCount);
                        stepEx.setWriteCount(writeCount);
                        stepEx.setCommitCount(commitCount);
                        stepEx.setRollbackCount(rollbackCount);
                        stepEx.setReadSkipCount(readSkipCount);
                        stepEx.setProcessSkipCount(processSkipCount);
                        stepEx.setFilterCount(filterCount);
                        stepEx.setWriteSkipCount(writeSkipCount);
                        stepEx.setStartTime(startTS);
                        stepEx.setEndTime(endTS);
                        stepEx.setPersistentUserData(persistentData);

                        data.put(stepname, stepEx);
                    }
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }

        return data;
    }

    @Override
    public List<StepExecution> getStepExecutionsForJobExecution(long execid) {

        List<StepExecution> data = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(STEP_EXECUTIONS_FOR_JOB_EXECUTION))) {
            statement.setLong(1, execid);
            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    long jobexecid = rs.getLong("jobexecid");
                    long stepexecid = rs.getLong("stepexecid");
                    String stepname = rs.getString("stepname");
                    String batchstatus = rs.getString("batchstatus");
                    String exitstatus = rs.getString("exitstatus");
                    long readCount = rs.getLong("readcount");
                    long writeCount = rs.getLong("writecount");
                    long commitCount = rs.getLong("commitcount");
                    long rollbackCount = rs.getLong("rollbackcount");
                    long readSkipCount = rs.getLong("readskipcount");
                    long processSkipCount = rs.getLong("processskipcount");
                    long filterCount = rs.getLong("filtercount");
                    long writeSkipCount = rs.getLong("writeSkipCount");
                    Timestamp startTS = rs.getTimestamp("startTime");
                    Timestamp endTS = rs.getTimestamp("endTime");
                    // get the object based data
                    Serializable persistentData = null;
                    byte[] pDataBytes = rs.getBytes("persistentData");
                    if (pDataBytes != null) {
                        try (ObjectInputStream objectIn
                                = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes))) {
                            persistentData = (Serializable) objectIn.readObject();
                        }
                    }

                    StepExecutionImpl stepEx = new StepExecutionImpl(jobexecid, stepexecid);
                    stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                    stepEx.setExitStatus(exitstatus);
                    stepEx.setStepName(stepname);
                    stepEx.setReadCount(readCount);
                    stepEx.setWriteCount(writeCount);
                    stepEx.setCommitCount(commitCount);
                    stepEx.setRollbackCount(rollbackCount);
                    stepEx.setReadSkipCount(readSkipCount);
                    stepEx.setProcessSkipCount(processSkipCount);
                    stepEx.setFilterCount(filterCount);
                    stepEx.setWriteSkipCount(writeSkipCount);
                    stepEx.setStartTime(startTS);
                    stepEx.setEndTime(endTS);
                    stepEx.setPersistentUserData(persistentData);

                    logger.log(Level.FINE, "BatchStatus: {0} | StepName: {1} | JobExecID: {2} | StepExecID: {3}",
                            new Object[]{batchstatus, stepname, jobexecid, stepexecid});

                    data.add(stepEx);
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }

        return data;
    }

    @Override
    public StepExecution getStepExecutionByStepExecutionId(long stepExecId) {

        StepExecutionImpl stepEx = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(STEP_EXECUTIONS_BY_STEP_ID))) {
            statement.setLong(1, stepExecId);
            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    long jobexecid = rs.getLong("jobexecid");
                    long stepexecid = rs.getLong("stepexecid");
                    String stepname = rs.getString("stepname");
                    String batchstatus = rs.getString("batchstatus");
                    String exitstatus = rs.getString("exitstatus");
                    long readCount = rs.getLong("readcount");
                    long writeCount = rs.getLong("writecount");
                    long commitCount = rs.getLong("commitcount");
                    long rollbackCount = rs.getLong("rollbackcount");
                    long readSkipCount = rs.getLong("readskipcount");
                    long processSkipCount = rs.getLong("processskipcount");
                    long filterCount = rs.getLong("filtercount");
                    long writeSkipCount = rs.getLong("writeSkipCount");
                    Timestamp startTS = rs.getTimestamp("startTime");
                    Timestamp endTS = rs.getTimestamp("endTime");
                    // get the object based data
                    Serializable persistentData = null;
                    byte[] pDataBytes = rs.getBytes("persistentData");
                    if (pDataBytes != null) {
                        try (ObjectInputStream objectIn
                                = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes))) {
                            persistentData = (Serializable) objectIn.readObject();
                        }
                    }

                    stepEx = new StepExecutionImpl(jobexecid, stepexecid);
                    stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                    stepEx.setExitStatus(exitstatus);
                    stepEx.setStepName(stepname);
                    stepEx.setReadCount(readCount);
                    stepEx.setWriteCount(writeCount);
                    stepEx.setCommitCount(commitCount);
                    stepEx.setRollbackCount(rollbackCount);
                    stepEx.setReadSkipCount(readSkipCount);
                    stepEx.setProcessSkipCount(processSkipCount);
                    stepEx.setFilterCount(filterCount);
                    stepEx.setWriteSkipCount(writeSkipCount);
                    stepEx.setStartTime(startTS);
                    stepEx.setEndTime(endTS);
                    stepEx.setPersistentUserData(persistentData);

                    logger.log(Level.FINE, "stepExecution BatchStatus: {0} StepName: {1}", new Object[]{batchstatus, stepname});
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }

        return stepEx;
    }

    @Override
    public void updateBatchStatusOnly(long key, BatchStatus batchStatus,
            Timestamp updates) {
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(UPDATE_BATCH_STATUS_ONLY))) {
            statement.setString(1, batchStatus.name());
            statement.setTimestamp(2, updates);
            statement.setLong(3, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void updateWithFinalExecutionStatusesAndTimestamps(long key,
            BatchStatus batchStatus, String exitStatus, Timestamp updates) {
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(UPDATE_FINAL_STATUS_AND_TIMESTAMP))) {
            statement.setString(1, batchStatus.name());
            statement.setString(2, exitStatus);
            statement.setTimestamp(3, updates);
            statement.setTimestamp(4, updates);
            statement.setLong(5, key);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void markJobStarted(long key, Timestamp startTS) {

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(MARK_JOB_STARTED))) {
            statement.setString(1, BatchStatus.STARTED.name());
            statement.setTimestamp(2, startTS);
            statement.setTimestamp(3, startTS);
            statement.setLong(4, key);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public IJobExecution jobOperatorGetJobExecution(long jobExecutionId) {
        IJobExecution jobEx;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_GET_JOB_EXECUTION))) {
            statement.setLong(1, jobExecutionId);
            try (ResultSet rs = statement.executeQuery()) {
                jobEx = (rs.next()) ? readJobExecutionRecord(rs) : null;
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }

        return jobEx;
    }

    @Override
    public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
        List<IJobExecution> data = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_GET_JOB_EXECUTIONS))) {
            statement.setLong(1, jobInstanceId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    data.add(readJobExecutionRecord(rs));
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
        return data;
    }

    protected IJobExecution readJobExecutionRecord(ResultSet rs)
            throws SQLException, IOException, ClassNotFoundException {
        if (rs == null) {
            return null;
        }

        JobOperatorJobExecution retMe = new JobOperatorJobExecution(
                rs.getLong("jobexecid"), rs.getLong("jobinstanceid"));

        retMe.setCreateTime(rs.getTimestamp("createtime"));
        retMe.setStartTime(rs.getTimestamp("starttime"));
        retMe.setEndTime(rs.getTimestamp("endtime"));
        retMe.setLastUpdateTime(rs.getTimestamp("updatetime"));

        retMe.setJobParameters((Properties) deserializeObject(rs
                .getBytes("parameters")));

        retMe.setBatchStatus(rs.getString("batchstatus"));
        retMe.setExitStatus(rs.getString("exitstatus"));
        retMe.setJobName(rs.getString("name"));

        return retMe;
    }

    @Override
    public Set<Long> jobOperatorGetRunningExecutions(String jobName) {
        Set<Long> executionIds = new HashSet<>();

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_OPERATOR_GET_RUNNING_EXECUTIONS))) {
            statement.setString(1, BatchStatus.STARTED.name());
            statement.setString(2, BatchStatus.STARTING.name());
            statement.setString(3, BatchStatus.STOPPING.name());
            statement.setString(4, jobName);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    executionIds.add(rs.getLong("jobexecid"));
                }
            }

        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return executionIds;
    }

    @Override
    public String getJobCurrentTag(long jobInstanceId) {
        String apptag = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(SELECT_JOBINSTANCEDATA_APPTAG))) {
            statement.setLong(1, jobInstanceId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    apptag = rs.getString(APPTAG);
                }
            }

        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return apptag;
    }

    @Override
    public void purge(String apptag) {

        logger.entering(CLASSNAME, "purge", apptag);
        String deleteJobs = queryStrings.get(DELETE_JOBS);
        String deleteJobExecutions = queryStrings.get(DELETE_JOB_EXECUTIONS);
        String deleteStepExecutions = queryStrings.get(DELETE_STEP_EXECUTIONS);

        try (Connection conn = getConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(deleteStepExecutions)) {
                statement.setString(1, apptag);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = conn.prepareStatement(deleteJobExecutions)) {
                statement.setString(1, apptag);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = conn.prepareStatement(deleteJobs)) {
                statement.setString(1, apptag);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "purge");

    }

    @Override
    public JobStatus getJobStatusFromExecution(long executionId) {

        JobStatus retVal = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(GET_JOB_STATUS_FROM_EXECUTIONS))) {
            statement.setLong(1, executionId);
            try (ResultSet rs = statement.executeQuery()) {
                byte[] buf = null;
                if (rs.next()) {
                    buf = rs.getBytes("obj");
                }
                retVal = (JobStatus) deserializeObject(buf);
            }
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "executeQuery");
        return retVal;
    }

    @Override
    public long getJobInstanceIdByExecutionId(long executionId) throws NoSuchJobExecutionException {
        long instanceId = 0;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(JOB_INSTANCE_ID_BY_EXECUTION_ID))) {
            statement.setObject(1, executionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    instanceId = rs.getLong("jobinstanceid");
                } else {
                    String msg = "Did not find job instance associated with executionID ="
                            + executionId;
                    logger.fine(msg);
                    throw new NoSuchJobExecutionException(msg);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return instanceId;
    }

    /**
     * This method is used to serialized an object saved into a table BLOB
     * field.
     *
     * @param theObject the object to be serialized
     * @return a object byte array
     * @throws IOException
     */
    protected byte[] serializeObject(Serializable theObject) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(baos)) {
            oout.writeObject(theObject);
            return baos.toByteArray();
        }
    }

    /**
     * This method is used to de-serialized a table BLOB field to its original
     * object form.
     *
     * @param buffer the byte array save a BLOB
     * @return the object saved as byte array
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected Serializable deserializeObject(byte[] buffer) throws IOException,
            ClassNotFoundException {
        Serializable theObject = null;
        if (buffer != null) {
            try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer))) {
                theObject = (Serializable) objectIn.readObject();
            }
        }
        return theObject;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#
	 * createJobInstance(java.lang.String, java.lang.String, java.lang.String,
	 * java.util.Properties)
     */
    @Override
    public JobInstance createSubJobInstance(String name, String apptag) {
        JobInstanceImpl jobInstance = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(
                        queryStrings.get(CREATE_SUB_JOB_INSTANCE),
                        new String[]{"JOBINSTANCEID"})) {
            statement.setString(1, name);
            statement.setString(2, apptag);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long jobInstanceID = rs.getLong(1);
                    jobInstance = new JobInstanceImpl(jobInstanceID);
                    jobInstance.setJobName(name);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return jobInstance;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#
	 * createJobInstance(java.lang.String, java.lang.String, java.lang.String,
	 * java.util.Properties)
     */
    @Override
    public JobInstance createJobInstance(String name, String apptag,
            String jobXml) {
        JobInstanceImpl jobInstance = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(
                        queryStrings.get(CREATE_JOB_INSTANCE),
                        new String[]{"JOBINSTANCEID"})) {
            statement.setString(1, name);
            statement.setString(2, apptag);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long jobInstanceID = rs.getLong(1);
                    jobInstance = new JobInstanceImpl(jobInstanceID, jobXml);
                    jobInstance.setJobName(name);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        return jobInstance;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#
	 * createJobExecution(com.ibm.jbatch.container.jsl.JobNavigator,
	 * jakarta.batch.runtime.JobInstance, java.util.Properties,
	 * com.ibm.jbatch.container.context.impl.JobContextImpl)
     */
    @Override
    public RuntimeJobExecution createJobExecution(JobInstance jobInstance,
            Properties jobParameters, BatchStatus batchStatus) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long newExecutionId = createRuntimeJobExecutionEntry(jobInstance,
                jobParameters, batchStatus, now);
        RuntimeJobExecution jobExecution = new RuntimeJobExecution(jobInstance,
                newExecutionId);
        jobExecution.setBatchStatus(batchStatus.name());
        jobExecution.setCreateTime(now);
        jobExecution.setLastUpdateTime(now);

        if (requestTracing != null && requestTracing.isRequestTracingEnabled()
                && requestTracing.isTraceInProgress()) {
            RequestTraceSpanLog spanLog = constructJBatchExecutionSpanLog(jobExecution);
            requestTracing.addSpanLog(spanLog);
        }

        return jobExecution;
    }

    protected long createRuntimeJobExecutionEntry(JobInstance jobInstance,
            Properties jobParameters, BatchStatus batchStatus,
            Timestamp timestamp) {
        long newJobExecutionId = 0L;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(
                        queryStrings.get(CREATE_JOB_EXECUTION_ENTRY),
                        new String[]{"JOBEXECID"})) {
            statement.setLong(1, jobInstance.getInstanceId());
            statement.setTimestamp(2, timestamp);
            statement.setTimestamp(3, timestamp);
            statement.setString(4, batchStatus.name());
            statement.setObject(5, serializeObject(jobParameters));
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    newJobExecutionId = rs.getLong(1);
                }
            }
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }

        return newJobExecutionId;
    }

    @Override
    public RuntimeFlowInSplitExecution createFlowInSplitExecution(
            JobInstance jobInstance, BatchStatus batchStatus) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long newExecutionId = createRuntimeJobExecutionEntry(jobInstance, null,
                batchStatus, now);
        RuntimeFlowInSplitExecution flowExecution = new RuntimeFlowInSplitExecution(
                jobInstance, newExecutionId);
        flowExecution.setBatchStatus(batchStatus.name());
        flowExecution.setCreateTime(now);
        flowExecution.setLastUpdateTime(now);
        return flowExecution;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#
	 * createStepExecution(long,
	 * com.ibm.jbatch.container.context.impl.StepContextImpl)
     */
    @Override
    public StepExecutionImpl createStepExecution(long rootJobExecId,
            StepContextImpl stepContext) {
        StepExecutionImpl stepExecution = null;
        String batchStatus = stepContext.getBatchStatus() == null ? BatchStatus.STARTING
                .name() : stepContext.getBatchStatus().name();
        String exitStatus = stepContext.getExitStatus();
        String stepName = stepContext.getStepName();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "batchStatus: {0} | stepName: {1}", new Object[]{batchStatus, stepName});
        }
        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;
        Timestamp startTime = stepContext.getStartTimeTS();
        Timestamp endTime = stepContext.getEndTimeTS();

        Metric[] metrics = stepContext.getMetrics();
        for (Metric metric : metrics) {
            switch (metric.getType()) {
                case READ_COUNT:
                    readCount = metric.getValue();
                    break;
                case WRITE_COUNT:
                    writeCount = metric.getValue();
                    break;
                case PROCESS_SKIP_COUNT:
                    processSkipCount = metric.getValue();
                    break;
                case COMMIT_COUNT:
                    commitCount = metric.getValue();
                    break;
                case ROLLBACK_COUNT:
                    rollbackCount = metric.getValue();
                    break;
                case READ_SKIP_COUNT:
                    readSkipCount = metric.getValue();
                    break;
                case FILTER_COUNT:
                    filterCount = metric.getValue();
                    break;
                case WRITE_SKIP_COUNT:
                    writeSkipCount = metric.getValue();
                    break;
                default:
                    break;
            }
        }
        Serializable persistentData = stepContext.getPersistentUserData();

        stepExecution = createStepExecution(rootJobExecId, batchStatus,
                exitStatus, stepName, readCount, writeCount, commitCount,
                rollbackCount, readSkipCount, processSkipCount, filterCount,
                writeSkipCount, startTime, endTime, persistentData);

        return stepExecution;
    }

    protected StepExecutionImpl createStepExecution(long rootJobExecId,
            String batchStatus, String exitStatus, String stepName,
            long readCount, long writeCount, long commitCount,
            long rollbackCount, long readSkipCount, long processSkipCount,
            long filterCount, long writeSkipCount, Timestamp startTime,
            Timestamp endTime, Serializable persistentData) {

        logger.entering(CLASSNAME, "createStepExecution", new Object[]{
            rootJobExecId, batchStatus,
            exitStatus == null ? NULL_TAGGED : exitStatus, stepName,
            readCount, writeCount, commitCount, rollbackCount,
            readSkipCount, processSkipCount, filterCount, writeSkipCount,
            startTime == null ? NULL_TAGGED : startTime,
            endTime == null ? NULL_TAGGED : endTime,
            persistentData == null ? NULL_TAGGED : persistentData});

        StepExecutionImpl stepExecution = null;
        String query = queryStrings.get(CREATE_STEP_EXECUTION);

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(query,
                        new String[]{"STEPEXECID"})) {
            statement.setLong(1, rootJobExecId);
            statement.setString(2, batchStatus);
            statement.setString(3, exitStatus);
            statement.setString(4, stepName);
            statement.setLong(5, readCount);
            statement.setLong(6, writeCount);
            statement.setLong(7, commitCount);
            statement.setLong(8, rollbackCount);
            statement.setLong(9, readSkipCount);
            statement.setLong(10, processSkipCount);
            statement.setLong(11, filterCount);
            statement.setLong(12, writeSkipCount);
            statement.setTimestamp(13, startTime);
            statement.setTimestamp(14, endTime);
            statement.setObject(15, serializeObject(persistentData));

            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    long stepExecutionId = rs.getLong(1);
                    stepExecution = new StepExecutionImpl(rootJobExecId,
                            stepExecutionId);
                    stepExecution.setStepName(stepName);
                }
            }
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "createStepExecution");

        return stepExecution;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#
	 * updateStepExecution
	 * (com.ibm.jbatch.container.context.impl.StepContextImpl)
     */
    @Override
    public void updateStepExecution(StepContextImpl stepContext) {

        Metric[] metrics = stepContext.getMetrics();

        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;

        for (Metric metric : metrics) {
            switch (metric.getType()) {
                case READ_COUNT:
                    readCount = metric.getValue();
                    break;
                case WRITE_COUNT:
                    writeCount = metric.getValue();
                    break;
                case PROCESS_SKIP_COUNT:
                    processSkipCount = metric.getValue();
                    break;
                case COMMIT_COUNT:
                    commitCount = metric.getValue();
                    break;
                case ROLLBACK_COUNT:
                    rollbackCount = metric.getValue();
                    break;
                case READ_SKIP_COUNT:
                    readSkipCount = metric.getValue();
                    break;
                case FILTER_COUNT:
                    filterCount = metric.getValue();
                    break;
                case WRITE_SKIP_COUNT:
                    writeSkipCount = metric.getValue();
                    break;
                default:
                    break;
            }
        }

        updateStepExecutionWithMetrics(stepContext, readCount, writeCount,
                commitCount, rollbackCount, readSkipCount, processSkipCount,
                filterCount, writeSkipCount);
    }

    /**
     * Obviously would be nice if the code writing this special format were in
     * the same place as this code reading it.
     *
     * Assumes format like:
     *
     * JOBINSTANCEDATA (jobinstanceid name, ...)
     *
     * 1197,"partitionMetrics","NOTSET" 1198,":1197:step1:0","NOTSET"
     * 1199,":1197:step1:1","NOTSET" 1200,":1197:step2:0","NOTSET"
     *
     * @param rootJobExecutionId JobExecution id of the top-level job
     * @param stepName Step name of the top-level stepName
     */
    protected String getPartitionLevelJobInstanceWildCard(
            long rootJobExecutionId, String stepName) {

        long jobInstanceId = getJobInstanceIdByExecutionId(rootJobExecutionId);

        StringBuilder sb = new StringBuilder(":");
        sb.append(Long.toString(jobInstanceId));
        sb.append(":");
        sb.append(stepName);
        sb.append(":%");

        return sb.toString();
    }

    @Override
    public void updateWithFinalPartitionAggregateStepExecution(
            long rootJobExecutionId, StepContextImpl stepContext) {

        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION))) {
            statement.setString(
                    1,
                    getPartitionLevelJobInstanceWildCard(rootJobExecutionId,
                            stepContext.getStepName()));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    readCount = rs.getLong("readcount");
                    writeCount = rs.getLong("writecount");
                    commitCount = rs.getLong("commitcount");
                    rollbackCount = rs.getLong("rollbackcount");
                    readSkipCount = rs.getLong("readskipcount");
                    processSkipCount = rs.getLong("processskipcount");
                    filterCount = rs.getLong("filtercount");
                    writeSkipCount = rs.getLong("writeSkipCount");
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }

        updateStepExecutionWithMetrics(stepContext, readCount, writeCount,
                commitCount, rollbackCount, readSkipCount, processSkipCount,
                filterCount, writeSkipCount);
    }

    protected void updateStepExecutionWithMetrics(StepContextImpl stepContext,
            long readCount, long writeCount, long commitCount,
            long rollbackCount, long readSkipCount, long processSkipCount,
            long filterCount, long writeSkipCount) {

        long stepExecutionId = stepContext.getInternalStepExecutionId();
        String batchStatus = stepContext.getBatchStatus() == null ? BatchStatus.STARTING
                .name() : stepContext.getBatchStatus().name();
        String exitStatus = stepContext.getExitStatus();
        String stepName = stepContext.getStepName();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "batchStatus: {0} | stepName: {1} | stepExecID: {2}", new Object[]{batchStatus, stepName, stepContext.getStepExecutionId()});
        }

        Timestamp startTime = stepContext.getStartTimeTS();
        Timestamp endTime = stepContext.getEndTimeTS();

        Serializable persistentData = stepContext.getPersistentUserData();

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER,
                    "About to update StepExecution with: ",
                    new Object[]{stepExecutionId, batchStatus,
                        exitStatus == null ? NULL_TAGGED : exitStatus,
                        stepName, readCount, writeCount, commitCount,
                        rollbackCount, readSkipCount, processSkipCount,
                        filterCount, writeSkipCount,
                        startTime == null ? NULL_TAGGED : startTime,
                        endTime == null ? NULL_TAGGED : endTime,
                        persistentData == null ? NULL_TAGGED : persistentData});
        }

        String query = queryStrings.get(UPDATE_STEP_EXECUTION_WITH_METRICS);

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, batchStatus);
            statement.setString(2, exitStatus);
            statement.setString(3, stepName);
            statement.setLong(4, readCount);
            statement.setLong(5, writeCount);
            statement.setLong(6, commitCount);
            statement.setLong(7, rollbackCount);
            statement.setLong(8, readSkipCount);
            statement.setLong(9, processSkipCount);
            statement.setLong(10, filterCount);
            statement.setLong(11, writeSkipCount);
            statement.setTimestamp(12, startTime);
            statement.setTimestamp(13, endTime);
            statement.setObject(14, serializeObject(persistentData));
            statement.setLong(15, stepExecutionId);
            statement.executeUpdate();
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#createJobStatus
	 * (long)
     */
    @Override
    public JobStatus createJobStatus(long jobInstanceId) {
        logger.entering(CLASSNAME, "createJobStatus", jobInstanceId);
        JobStatus jobStatus = new JobStatus(jobInstanceId);
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(CREATE_JOBSTATUS))) {
            statement.setLong(1, jobInstanceId);
            statement.setBytes(2, serializeObject(jobStatus));
            statement.executeUpdate();

        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "createJobStatus");
        return jobStatus;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#getJobStatus
	 * (long)
     */
    @Override
    public JobStatus getJobStatus(long instanceId) {
        logger.entering(CLASSNAME, "getJobStatus", instanceId);
        String query = queryStrings.get(GET_JOB_STATUS);
        JobStatus jobStatus = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setLong(1, instanceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    jobStatus = (JobStatus) deserializeObject(rs.getBytes(1));
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "getJobStatus", jobStatus);
        return jobStatus;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#updateJobStatus
	 * (long, com.ibm.jbatch.container.status.JobStatus)
     */
    @Override
    public void updateJobStatus(long instanceId, JobStatus jobStatus) {
        logger.entering(CLASSNAME, "updateJobStatus", new Object[]{
            instanceId, jobStatus});
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Updating Job Status to: {0}", jobStatus.getBatchStatus());
        }
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(UPDATE_JOBSTATUS))) {
            statement.setBytes(1, serializeObject(jobStatus));
            statement.setLong(2, instanceId);
            statement.executeUpdate();
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "updateJobStatus");
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#createStepStatus
	 * (long)
     */
    @Override
    public StepStatus createStepStatus(long stepExecId) {
        logger.entering(CLASSNAME, "createStepStatus", stepExecId);
        StepStatus stepStatus = new StepStatus(stepExecId);
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(CREATE_STEP_STATUS))) {
            statement.setLong(1, stepExecId);
            statement.setBytes(2, serializeObject(stepStatus));
            statement.executeUpdate();
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "createStepStatus");
        return stepStatus;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#getStepStatus
	 * (long, java.lang.String)
     */
    @Override
    public StepStatus getStepStatus(long instanceId, String stepName) {
        logger.entering(CLASSNAME, "getStepStatus", new Object[]{instanceId,
            stepName});
        String query = queryStrings.get(GET_STEP_STATUS);
        StepStatus stepStatus = null;

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setLong(1, instanceId);
            statement.setString(2, stepName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    stepStatus = (StepStatus) deserializeObject(rs.getBytes(1));
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "getStepStatus",
                stepStatus == null ? NULL_TAGGED : stepStatus);
        return stepStatus;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#updateStepStatus
	 * (long, com.ibm.jbatch.container.status.StepStatus)
     */
    @Override
    public void updateStepStatus(long stepExecutionId, StepStatus stepStatus) {
        logger.entering(CLASSNAME, "updateStepStatus", new Object[]{
            stepExecutionId, stepStatus});

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Updating StepStatus to: {0}", stepStatus.getBatchStatus());
        }
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(queryStrings
                        .get(UPDATE_STEP_STATUS))) {
            statement.setBytes(1, serializeObject(stepStatus));
            statement.setLong(2, stepExecutionId);
            statement.executeUpdate();
        } catch (SQLException | IOException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "updateStepStatus");
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.jbatch.container.services.IPersistenceManagerService#getTagName
	 * (long)
     */
    @Override
    public String getTagName(long jobExecutionId) {
        logger.entering(CLASSNAME, "getTagName", jobExecutionId);
        String apptag = null;
        String query = queryStrings.get(GET_TAGNAME);
        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setLong(1, jobExecutionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    apptag = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "getTagName");
        return apptag;
    }

    @Override
    public long getMostRecentExecutionId(long jobInstanceId) {
        logger.entering(CLASSNAME, "getMostRecentExecutionId", jobInstanceId);
        long mostRecentId = -1;
        String query = queryStrings.get(GET_MOST_RECENT_EXECUTION_ID);

        try (Connection conn = getConnection();
                PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setLong(1, jobInstanceId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    mostRecentId = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        logger.exiting(CLASSNAME, "getMostRecentExecutionId");
        return mostRecentId;
    }

    @Override
    public void shutdown() throws BatchContainerServiceException {

    }

    /**
     * Method invoked to insert the table key strings into a hashmap and to add
     * the prefix and suffix to the table names
     *
     */
    protected Map<String, String> getSharedTableMap() {
        Map<String, String> result = new HashMap<>(6);
        result.put(JOB_INSTANCE_TABLE_KEY, prefix + "JOBINSTANCEDATA" + suffix);
        result.put(EXECUTION_INSTANCE_TABLE_KEY, prefix + "EXECUTIONINSTANCEDATA" + suffix);
        result.put(STEP_EXECUTION_INSTANCE_TABLE_KEY, prefix + "STEPEXECUTIONINSTANCEDATA" + suffix);
        result.put(JOB_STATUS_TABLE_KEY, prefix + "JOBSTATUS" + suffix);
        result.put(STEP_STATUS_TABLE_KEY, prefix + "STEPSTATUS" + suffix);
        result.put(CHECKPOINT_TABLE_KEY, prefix + "CHECKPOINTDATA" + suffix);

        return result;
    }

    /**
     * Method invoked to insert the query strings used by all database types
     * into a hashmap
     *
     * @param batchConfig
     * @return
     * @throws SQLException
     *
     */
    protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
        queryStrings = new HashMap<>();

        queryStrings.put(Q_SET_SCHEMA, "SET SCHEMA ");

        queryStrings.put(
                SELECT_CHECKPOINTDATA,
                "select id, obj from "
                + tableNames.get(CHECKPOINT_TABLE_KEY)
                + " where id = ?");

        queryStrings.put(
                INSERT_CHECKPOINTDATA,
                "insert into "
                + tableNames.get(CHECKPOINT_TABLE_KEY)
                + " values(?, ?)");

        queryStrings.put(
                UPDATE_CHECKPOINTDATA,
                "update "
                + tableNames.get(CHECKPOINT_TABLE_KEY)
                + " set obj = ? where id = ?");

        queryStrings.put(
                LOCK_CHECKPOINTDATA,
                "lock table "
                + tableNames.get(CHECKPOINT_TABLE_KEY)
                + " in exclusive mode");

        queryStrings.put(JOBOPERATOR_GET_JOB_INSTANCE_COUNT,
                "select count(jobinstanceid) as jobinstancecount from "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " where name = ? and apptag = ?");

        queryStrings.put(SELECT_JOBINSTANCEDATA_COUNT,
                "select count(jobinstanceid) as jobinstancecount from "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " where name = ?");
        queryStrings
                .put(JOBOPERATOR_GET_JOB_INSTANCE_IDS,
                        "select jobinstanceid from "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + " where name = ? and apptag = ? order by jobinstanceid desc");
        queryStrings.put(
                SELECT_JOBINSTANCEDATA_IDS,
                "select jobinstanceid from "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " where name = ? order by jobinstanceid desc");
        queryStrings.put(
                JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA,
                "select distinct jobinstanceid, name from "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " where name ");
        queryStrings.put(JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP,
                "select createtime, endtime, updatetime, starttime from "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings.put(
                JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS,
                "select batchstatus from "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings.put(
                JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS,
                "select exitstatus from "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings.put(
                JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID,
                "select jobinstanceid from "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings.put(
                GET_PARAMETERS,
                "select parameters from "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings
                .put(MOST_RECENT_STEPS_FOR_JOB,
                        "select A.* from "
                        + tableNames
                                .get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                        + " as A inner join "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " as B on A.jobexecid = B.jobexecid where B.jobinstanceid = ? order by A.stepexecid desc");
        queryStrings.put(STEP_EXECUTIONS_FOR_JOB_EXECUTION, "select * from "
                + tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings.put(STEP_EXECUTIONS_BY_STEP_ID, "select * from "
                + tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                + " where stepexecid = ?");
        queryStrings
                .put(UPDATE_BATCH_STATUS_ONLY,
                        "update "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " set batchstatus = ?, updatetime = ? where jobexecid = ?");
        queryStrings
                .put(UPDATE_FINAL_STATUS_AND_TIMESTAMP,
                        "update "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " set batchstatus = ?, exitstatus = ?, endtime = ?, updatetime = ? where jobexecid = ?");
        queryStrings
                .put(MARK_JOB_STARTED,
                        "update "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " set batchstatus = ?, starttime = ?, updatetime = ? where jobexecid = ?");
        queryStrings
                .put(JOB_OPERATOR_GET_JOB_EXECUTION,
                        "select A.jobexecid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.jobinstanceid, A.batchstatus, A.exitstatus, B.name from "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " as A inner join "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + " as B on A.jobinstanceid = B.jobinstanceid where jobexecid = ?");
        queryStrings
                .put(JOB_OPERATOR_GET_JOB_EXECUTIONS,
                        "select A.jobexecid, A.jobinstanceid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.batchstatus, A.exitstatus, B.name from "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " as A inner join "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + " as B ON A.jobinstanceid = B.jobinstanceid where A.jobinstanceid = ?");
        queryStrings
                .put(JOB_OPERATOR_GET_RUNNING_EXECUTIONS,
                        "SELECT A.jobexecid FROM "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " A INNER JOIN "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + " B ON A.jobinstanceid = B.jobinstanceid WHERE A.batchstatus IN (?,?,?) AND B.name = ?");
        queryStrings.put(SELECT_JOBINSTANCEDATA_APPTAG, "select apptag from "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " where jobinstanceid = ?");
        queryStrings.put(DELETE_JOBS,
                "DELETE FROM " + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " WHERE apptag = ?");
        String deleteJobExecutions = "DELETE FROM "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY) + " "
                + "WHERE jobexecid IN (" + "SELECT B.jobexecid FROM "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY) + " A INNER JOIN "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY) + " B "
                + "ON A.jobinstanceid = B.jobinstanceid "
                + "WHERE A.apptag = ?)";
        queryStrings.put(DELETE_JOB_EXECUTIONS, deleteJobExecutions);
        String deleteStepExecutions = "DELETE FROM "
                + tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY) + " "
                + "WHERE stepexecid IN (" + "SELECT C.stepexecid FROM "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY) + " A INNER JOIN "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY) + " B "
                + "ON A.jobinstanceid = B.jobinstanceid INNER JOIN "
                + tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY) + " C "
                + "ON B.jobexecid = C.jobexecid " + "WHERE A.apptag = ?)";
        queryStrings.put(DELETE_STEP_EXECUTIONS, deleteStepExecutions);
        queryStrings.put(GET_JOB_STATUS_FROM_EXECUTIONS, "select A.obj from "
                + tableNames.get(JOB_STATUS_TABLE_KEY) + " as A inner join "
                + "" + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " as B on A.id = B.jobinstanceid where B.jobexecid = ?");
        queryStrings.put(
                JOB_INSTANCE_ID_BY_EXECUTION_ID,
                "select jobinstanceid from "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " where jobexecid = ?");
        queryStrings.put(CREATE_SUB_JOB_INSTANCE,
                "INSERT INTO " + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " (name, apptag) VALUES(?, ?)");
        queryStrings.put(CREATE_JOB_INSTANCE,
                "INSERT INTO " + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " (name, apptag) VALUES(?, ?)");
        queryStrings
                .put(CREATE_JOB_EXECUTION_ENTRY,
                        "INSERT INTO "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " (jobinstanceid, createtime, updatetime, batchstatus, parameters) VALUES(?, ?, ?, ?, ?)");
        queryStrings
                .put(CREATE_STEP_EXECUTION,
                        "INSERT INTO "
                        + tableNames
                                .get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                        + " (jobexecid, batchstatus, exitstatus, stepname, readcount,"
                        + "writecount, commitcount, rollbackcount, readskipcount, processskipcount, filtercount, writeskipcount, starttime,"
                        + "endtime, persistentdata) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        queryStrings
                .put(UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION,
                        "select SUM(STEPEX.readcount) readcount, SUM(STEPEX.writecount) writecount, SUM(STEPEX.commitcount) commitcount,  SUM(STEPEX.rollbackcount) rollbackcount,"
                        + " SUM(STEPEX.readskipcount) readskipcount, SUM(STEPEX.processskipcount) processskipcount, SUM(STEPEX.filtercount) filtercount, SUM(STEPEX.writeSkipCount) writeSkipCount"
                        + " from "
                        + tableNames
                                .get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                        + " STEPEX inner join "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + " JOBEX"
                        + " on STEPEX.jobexecid = JOBEX.jobexecid"
                        + " where JOBEX.jobinstanceid IN"
                        + " (select jobinstanceid from "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + " where name like ?)");
        queryStrings
                .put(UPDATE_STEP_EXECUTION_WITH_METRICS,
                        "UPDATE "
                        + tableNames
                                .get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                        + " SET batchstatus = ?, exitstatus = ?, stepname = ?,  readcount = ?,"
                        + "writecount = ?, commitcount = ?, rollbackcount = ?, readskipcount = ?, processskipcount = ?, filtercount = ?, writeskipcount = ?,"
                        + " starttime = ?, endtime = ?, persistentdata = ? WHERE stepexecid = ?");
        queryStrings.put(CREATE_JOBSTATUS,
                "INSERT INTO " + tableNames.get(JOB_STATUS_TABLE_KEY)
                + " (id, obj) VALUES(?, ?)");
        queryStrings.put(GET_JOB_STATUS,
                "SELECT obj FROM " + tableNames.get(JOB_STATUS_TABLE_KEY)
                + " WHERE id = ?");
        queryStrings.put(UPDATE_JOBSTATUS,
                "UPDATE " + tableNames.get(JOB_STATUS_TABLE_KEY)
                + " SET obj = ? WHERE id = ?");
        queryStrings.put(CREATE_STEP_STATUS,
                "INSERT INTO " + tableNames.get(STEP_STATUS_TABLE_KEY)
                + " (id, obj) VALUES(?, ?)");
        queryStrings.put(
                GET_STEP_STATUS,
                "SELECT obj FROM " + tableNames.get(STEP_STATUS_TABLE_KEY)
                + " WHERE id IN (" + "SELECT B.stepexecid FROM "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " A INNER JOIN "
                + tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
                + " B ON A.jobexecid = B.jobexecid "
                + "WHERE A.jobinstanceid = ? and B.stepname = ?)");
        queryStrings.put(UPDATE_STEP_STATUS,
                "UPDATE " + tableNames.get(STEP_STATUS_TABLE_KEY)
                + " SET obj = ? WHERE id = ?");
        queryStrings.put(
                GET_TAGNAME,
                "SELECT A.apptag FROM "
                + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                + " A INNER JOIN "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " B ON A.jobinstanceid = B.jobinstanceid"
                + " WHERE B.jobexecid = ?");
        queryStrings.put(GET_MOST_RECENT_EXECUTION_ID, "SELECT jobexecid FROM "
                + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                + " WHERE jobinstanceid = ? ORDER BY createtime DESC");
        return queryStrings;
    }

    /**
     * Method invoked to insert the H2 create table strings into a hashmap
     *
     * @param batchConfig
     * @return
     */
    private Map<String, String> setCreateH2StringsMap(Map<String, String> tableNames) {
        createH2Strings = new HashMap<>();
        createH2Strings.put(H2_CREATE_TABLE_CHECKPOINTDATA,
                "CREATE TABLE " + tableNames.get(CHECKPOINT_TABLE_KEY)
                + "(id VARCHAR(512),obj BLOB)");
        createH2Strings
                .put(H2_CREATE_TABLE_JOBINSTANCEDATA,
                        "CREATE TABLE "
                        + tableNames.get(JOB_INSTANCE_TABLE_KEY)
                        + "("
                        + "jobinstanceid BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"//CONSTRAINT JOBINSTANCE_PK
                        + "name VARCHAR(512)," + "apptag VARCHAR(512))");

        createH2Strings
                .put(H2_CREATE_TABLE_EXECUTIONINSTANCEDATA,
                        "CREATE TABLE "
                        + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
                        + "("
                        + "jobexecid BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"//CONSTRAINT JOBEXECUTION_PK
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
                        + "stepexecid BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"//CONSTRAINT STEPEXECUTION_PK
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

    private RequestTraceSpanLog constructJBatchExecutionSpanLog(RuntimeJobExecution jobExecution) {
        RequestTraceSpanLog spanLog = new RequestTraceSpanLog("jBatchExecutionContextEvent");

        try {
            spanLog.addLogEntry("Execution ID", Long.toString(jobExecution.getExecutionId()));
            spanLog.addLogEntry("Job ID", Long.toString(jobExecution.getInstanceId()));
            spanLog.addLogEntry("Job Name", jobExecution.getJobInstance().getJobName());
            spanLog.addLogEntry("Batch Status", jobExecution.getJobOperatorJobExecution().getBatchStatus().toString());

            if (jobExecution.getJobParameters() != null) {
                spanLog.addLogEntry("Job Parameters", jobExecution.getJobParameters().toString());
            } else {
                spanLog.addLogEntry("Job Parameters", "null");
            }
        } catch (NullPointerException e) {
            logger.log(Level.INFO, "NullPointerException when creating request tracing JBatchExecutionContextEvent");
        }

        return spanLog;
    }
}
