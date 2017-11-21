/*
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 * 
 */

package fish.payara.jbatch.persistence.rdbms;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_PREFIX_PROPERTY;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_SUFFIX_PROPERTY;
import static org.glassfish.internal.api.Globals.getDefaultHabitat;

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

import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ibm.jbatch.container.context.impl.MetricImpl;
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

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;

/**
 * 
 * Base Persistence Manager Class. All Persistence Managers extend this base
 * persistence manager class
 */

public class JBatchJDBCPersistenceManager implements
		IPersistenceManagerService, JDBCQueryConstants, OracleJDBCConstants {

	private static final String CLASSNAME = JBatchJDBCPersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;

	protected DataSource dataSource = null;
	protected String jndiName = null;

	protected String schema = "";

	// shared table name and query strings

	protected Map<String, String> tableNames;
	protected Map<String, String> queryStrings;

	// derby create table strings
	protected Map<String, String> createDerbyStrings;

        protected RequestTracingService requestTracing;
        
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl
	 * #init(com.ibm.jbatch.container.IBatchConfig)
	 */
	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
            
	    logger.config("Entering CLASSNAME.init(), batchConfig =" + batchConfig);

        this.batchConfig = batchConfig;

        schema = batchConfig.getDatabaseConfigurationBean().getSchema();
        jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
        
        logger.config("JNDI name = " + jndiName);

        if (jndiName == null || jndiName.equals("")) {
            throw new BatchContainerServiceException("JNDI name is not defined.");
        }

        try {
            dataSource = (DataSource) new InitialContext().lookup(jndiName);
        } catch (NamingException e) {
            logger.severe(
                "Lookup failed for JNDI name: " + jndiName + ". " + 
                " One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
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
            if (!isDerbySchemaValid()) {
                setDefaultSchema();
            }
            checkDerbyTables();

        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
            throw new BatchContainerServiceException(e);
        }

        try {
            requestTracing = getDefaultHabitat().getService(RequestTracingService.class);
        } catch (NullPointerException ex) {
            logger.log(INFO,
                "Error retrieving Request Tracing service " + 
                "during initialisation of JBatchJDBCPersistenceManager - NullPointerException");
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
        logger.entering(CLASSNAME, "getCheckpointData", key == null ? "<null>" : key);
        
        tryObtainTableLock();
        
        CheckpointData checkpointData = queryCheckpointData(key.getCommaSeparatedKey());
        
        logger.exiting(CLASSNAME, "getCheckpointData", checkpointData == null ? "<null>" : checkpointData);
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
        logger.entering(CLASSNAME, "updateCheckpointData", new Object[] { key, value });
        
        tryObtainTableLock();
        
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
        logger.entering(CLASSNAME, "createCheckpointData", new Object[] { key, value });
        
        insertCheckpointData(key.getCommaSeparatedKey(), value);
        
        logger.exiting(CLASSNAME, "createCheckpointData");
    }

	/**
	 * Set the default schema to the username obtained from the connection based
	 * on the datasource
	 * */

	public String setDefaultSchema() throws SQLException {
		logger.finest("Entering setDefaultSchema");

		logger.finest("Java EE mode, getting connection from data source");
		try (Connection connection = dataSource.getConnection())  {

			DatabaseMetaData dbmd = connection.getMetaData();
			schema = dbmd.getUserName();
			if (dbmd.getDatabaseProductName().toLowerCase().contains("mysql")) {
				schema = "test";
			}

		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		}
		
		logger.finest("Exiting setDefaultSchema");
		return schema;
	}

	private boolean isDerby(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("derby");
    }
	
	/**
	 * Checks if the schema exists in the database. If not connect to the
	 * default schema
	 * 
	 * @return true if the schema exists, false otherwise.
	 * @throws SQLException
	 */
	protected boolean isDerbySchemaValid() throws SQLException {
		logger.entering(CLASSNAME, "isDerbySchemaValid");
		
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

		logger.exiting(CLASSNAME, "isDerbySchemaValid", false);
		return false;
	}

	/**
	 * Check if the derby jbatch tables exist, if not create them
	 **/
	private void checkDerbyTables() throws SQLException {
		setCreateDerbyStringsMap(batchConfig);
		createDerbyTableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createDerbyStrings.get(DERBY_CREATE_TABLE_CHECKPOINTDATA));

		createDerbyTableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createDerbyStrings.get(DERBY_CREATE_TABLE_JOBINSTANCEDATA));

		createDerbyTableNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createDerbyStrings
						.get(DERBY_CREATE_TABLE_EXECUTIONINSTANCEDATA));
		createDerbyTableNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createDerbyStrings.get(DERBY_CREATE_TABLE_STEPINSTANCEDATA));
		createDerbyTableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createDerbyStrings.get(DERBY_CREATE_TABLE_JOBSTATUS));
		createDerbyTableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createDerbyStrings.get(DERBY_CREATE_TABLE_STEPSTATUS));

	}

	/**
	 * Create the Derby tables
	 **/
	protected void createDerbyTableNotExists(String tableName, String createTableStatement) throws SQLException {
		logger.entering(CLASSNAME, "createIfNotExists", new Object[] { tableName, createTableStatement });
		
		try (Connection connection = getConnection()) {
			try (ResultSet resultSet = connection.getMetaData().getTables(null, schema, tableName, null)) {
    			if (!resultSet.next()) {
    				logger.log(INFO, tableName + " table does not exists. Trying to create it.");
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
	 * Retrieve the number of rows in the resultset. This method is used to
	 * check if a table exists in a given schema
	 **/

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
		logger.finest("Entering: " + CLASSNAME + ".getConnection");

		logger.finest("Java EE mode, getting connection from data source");
		Connection connection = dataSource.getConnection();
		logger.finest("autocommit=" + connection.getAutoCommit());

		setSchemaOnConnection(connection);

		logger.finest("Exiting: " + CLASSNAME + ".getConnection() with connection =" + connection);
		return connection;
	}

	/**
	 * @return the database connection. The schema is set to whatever default is
	 *         used by the datasource.
	 * @throws SQLException
	 */
	protected Connection getConnectionToDefaultSchema() throws SQLException {
		logger.finest("Entering getConnectionToDefaultSchema");
		Connection connection = null;

		logger.finest("Java EE mode, getting connection from data source");
		try {
			connection = dataSource.getConnection();

		} catch (SQLException e) {
			logException("FAILED GETTING DATABASE CONNECTION", e);
			throw new PersistenceException(e);
		}
		logger.finest("autocommit=" + connection.getAutoCommit());

		logger.finest("Exiting from getConnectionToDefaultSchema, conn= " + connection);
		return connection;
	}

	protected void logException(String msg, Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		logger.log(SEVERE, msg + "; Exception stack trace: " + sw);
	}

	/**
	 * Set the schema to the default schema or the schema defined at batch
	 * configuration time
	 * 
	 * @param connection
	 * @throws SQLException
	 */
	protected void setSchemaOnConnection(Connection connection) throws SQLException {
		logger.finest("Entering " + CLASSNAME + ".setSchemaOnConnection()");

		if (!(connection.getMetaData().getDatabaseProductName().contains("Oracle"))) {
			try (PreparedStatement preparedStatement = connection.prepareStatement(queryStrings.get(Q_SET_SCHEMA))) {
			    preparedStatement.setString(1, schema);
	            preparedStatement.executeUpdate();
			}
		}

		logger.finest("Exiting " + CLASSNAME + ".setSchemaOnConnection()");
	}

	/**
	 * select data from DB table
	 * 
	 * @param key
	 *            - the IPersistenceDataKey object
	 * @return List of serializable objects store in the DB table
	 * 
	 *         Ex. select id, obj from tablename where id = ?
	 */
	protected CheckpointData queryCheckpointData(Object key) {
		logger.entering(CLASSNAME, "queryCheckpointData", new Object[] { key, SELECT_CHECKPOINTDATA });
		
		CheckpointData data = null;
		
		try (Connection connection = getConnection()) {
		    try (PreparedStatement statement = connection.prepareStatement(queryStrings.get(SELECT_CHECKPOINTDATA))) {
    			statement.setObject(1, key);
    			ResultSet resultSet = statement.executeQuery();
    			if (resultSet.next()) {
    				data = (CheckpointData) deserializeObject(resultSet.getBytes("obj"));
    			}
		    }
		} catch (SQLException |IOException | ClassNotFoundException e) {
			throw new PersistenceException(e);
		} 
		
		logger.exiting(CLASSNAME, "queryCheckpointData");
		return data;
	}
	
   private void tryObtainTableLock() {
        
        try (Connection connection = getConnection()) {
            if (isDerby(connection)) {
                try (PreparedStatement statement = connection.prepareStatement(queryStrings.get(LOCK_CHECKPOINTDATA))) {
                   statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } 
    }

	/**
	 * Insert data to DB table
	 * 
	 * @param key
	 *            - the IPersistenceDataKey object
	 * @param value
	 *            - serializable object to store
	 * 
	 *            Ex. insert into tablename values(?, ?)
	 */
	protected <T> void insertCheckpointData(Object key, T value) {
		logger.entering(CLASSNAME, "insertCheckpointData", new Object[] { key, value });
		
		try (Connection connection = getConnection()) {
		    try (PreparedStatement statement = connection.prepareStatement(queryStrings.get(INSERT_CHECKPOINTDATA))) {
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();;
		        try (ObjectOutputStream oout = new ObjectOutputStream(baos)) {
		            oout.writeObject(value);
		            
		            statement.setObject(1, key);
		            statement.setBytes(2, baos.toByteArray());
		            statement.executeUpdate();
		        }
		        
		    }
		} catch (SQLException | IOException  e) {
			throw new PersistenceException(e);
		} 
		
		logger.exiting(CLASSNAME, "insertCheckpointData");
	}

	/**
	 * update data in DB table
	 * 
	 * @param value
	 *            - serializable object to store
	 * @param key
	 *            - the IPersistenceDataKey object
	 * @param query
	 *            - SQL statement to execute.
	 * 
	 *            Ex. update tablename set obj = ? where id = ?
	 */
	protected void updateCheckpointData(Object key, CheckpointData value) {
		logger.entering(CLASSNAME, "updateCheckpointData", new Object[] { key,
				value });
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(UPDATE_CHECKPOINTDATA));
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(value);

			b = baos.toByteArray();

			statement.setBytes(1, b);
			statement.setObject(2, key);
			statement.executeUpdate();
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw new PersistenceException(e);
		} catch (IOException e) {
			logger.severe(e.getLocalizedMessage());
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "updateCheckpointData");
	}

	/**
	 * closes connection, result set and statement
	 * 
	 * @param conn
	 *            - connection object to close
	 * @param rs
	 *            - result set object to close
	 * @param statement
	 *            - statement object to close
	 */
	protected void cleanupConnection(Connection conn, ResultSet rs,
			PreparedStatement statement) {

		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Entering",
				new Object[] { conn, rs == null ? "<null>" : rs,
						statement == null ? "<null>" : statement });

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
	 * @param conn
	 *            - connection object to close
	 * @param statement
	 *            - statement object to close
	 */
	protected void cleanupConnection(Connection conn,
			PreparedStatement statement) {

		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Entering",
				new Object[] { conn, statement });

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

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		int count;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOBOPERATOR_GET_JOB_INSTANCE_COUNT));
			statement.setString(1, jobName);
			statement.setString(2, appTag);
			rs = statement.executeQuery();
			rs.next();
			count = rs.getInt("jobinstancecount");

		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return count;
	}

	@Override
	public int jobOperatorGetJobInstanceCount(String jobName) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		int count;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(SELECT_JOBINSTANCEDATA_COUNT));
			statement.setString(1, jobName);
			rs = statement.executeQuery();
			rs.next();
			count = rs.getInt("jobinstancecount");

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return count;
	}

	@Override
	public List<Long> jobOperatorGetJobInstanceIds(String jobName,
			String appTag, int start, int count) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<Long> data = new ArrayList<Long>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOBOPERATOR_GET_JOB_INSTANCE_IDS));
			statement.setObject(1, jobName);
			statement.setObject(2, appTag);
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("jobinstanceid");
				data.add(id);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		if (data.size() > 0) {
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

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<Long> data = new ArrayList<Long>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(SELECT_JOBINSTANCEDATA_IDS));
			statement.setObject(1, jobName);
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("jobinstanceid");
				data.add(id);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		if (data.size() > 0) {
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

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		HashMap<Long, String> data = new HashMap<Long, String>();

		try {
			conn = getConnection();

			// Filter out 'subjob' parallel execution entries which start with
			// the special character
			final String filter = "not like '"
					+ PartitionedStepBuilder.JOB_ID_SEPARATOR + "%'";

			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA) + filter);
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("jobinstanceid");
				String name = rs.getString("name");
				data.put(id, name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return data;
	}

	@Override
	public Timestamp jobOperatorQueryJobExecutionTimestamp(long key,
			TimestampType timestampType) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp createTimestamp = null;
		Timestamp endTimestamp = null;
		Timestamp updateTimestamp = null;
		Timestamp startTimestamp = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP));
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				createTimestamp = rs.getTimestamp(1);
				endTimestamp = rs.getTimestamp(2);
				updateTimestamp = rs.getTimestamp(3);
				startTimestamp = rs.getTimestamp(4);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		if (timestampType.equals(TimestampType.CREATE)) {
			return createTimestamp;
		} else if (timestampType.equals(TimestampType.END)) {
			return endTimestamp;
		} else if (timestampType.equals(TimestampType.LAST_UPDATED)) {
			return updateTimestamp;
		} else if (timestampType.equals(TimestampType.STARTED)) {
			return startTimestamp;
		} else {
			throw new IllegalArgumentException("Unexpected enum value.");
		}
	}

	@Override
	public String jobOperatorQueryJobExecutionBatchStatus(long key) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String status = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS));
			statement.setLong(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				status = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return status;
	}

	@Override
	public String jobOperatorQueryJobExecutionExitStatus(long key) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String status = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS));
			statement.setLong(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				status = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return status;
	}

	@Override
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID)
			throws NoSuchJobExecutionException {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long jobinstanceID = 0;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID));
			statement.setLong(1, executionID);
			rs = statement.executeQuery();
			if (rs.next()) {
				jobinstanceID = rs.getLong("jobinstanceid");
			} else {
				String msg = "Did not find job instance associated with executionID ="
						+ executionID;
				logger.fine(msg);
				throw new NoSuchJobExecutionException(msg);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return jobinstanceID;
	}

	@Override
	public Properties getParameters(long executionId)
			throws NoSuchJobExecutionException {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Properties props = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings.get(GET_PARAMETERS));
			statement.setLong(1, executionId);
			rs = statement.executeQuery();

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

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
		}

		return props;

	}

	public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(
			long instanceId) {

		Map<String, StepExecution> data = new HashMap<String, StepExecution>();

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		long jobexecid = 0;
		long stepexecid = 0;
		String stepname = null;
		String batchstatus = null;
		String exitstatus = null;
		Exception ex = null;
		long readCount = 0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTS = null;
		Timestamp endTS = null;
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(MOST_RECENT_STEPS_FOR_JOB));
			statement.setLong(1, instanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				stepname = rs.getString("stepname");
				if (data.containsKey(stepname)) {
					continue;
				} else {

					jobexecid = rs.getLong("jobexecid");
					batchstatus = rs.getString("batchstatus");
					exitstatus = rs.getString("exitstatus");
					readCount = rs.getLong("readcount");
					writeCount = rs.getLong("writecount");
					commitCount = rs.getLong("commitcount");
					rollbackCount = rs.getLong("rollbackcount");
					readSkipCount = rs.getLong("readskipcount");
					processSkipCount = rs.getLong("processskipcount");
					filterCount = rs.getLong("filtercount");
					writeSkipCount = rs.getLong("writeSkipCount");
					startTS = rs.getTimestamp("startTime");
					endTS = rs.getTimestamp("endTime");
					// get the object based data
					Serializable persistentData = null;
					byte[] pDataBytes = rs.getBytes("persistentData");
					if (pDataBytes != null) {
						objectIn = new TCCLObjectInputStream(
								new ByteArrayInputStream(pDataBytes));
						persistentData = (Serializable) objectIn.readObject();
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

					data.put(stepname, stepEx);
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return data;
	}

	@Override
	public List<StepExecution> getStepExecutionsForJobExecution(long execid) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		long jobexecid = 0;
		long stepexecid = 0;
		String stepname = null;
		String batchstatus = null;
		String exitstatus = null;
		Exception ex = null;
		long readCount = 0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTS = null;
		Timestamp endTS = null;
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;

		List<StepExecution> data = new ArrayList<StepExecution>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(STEP_EXECUTIONS_FOR_JOB_EXECUTION));
			statement.setLong(1, execid);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobexecid = rs.getLong("jobexecid");
				stepexecid = rs.getLong("stepexecid");
				stepname = rs.getString("stepname");
				batchstatus = rs.getString("batchstatus");
				exitstatus = rs.getString("exitstatus");
				readCount = rs.getLong("readcount");
				writeCount = rs.getLong("writecount");
				commitCount = rs.getLong("commitcount");
				rollbackCount = rs.getLong("rollbackcount");
				readSkipCount = rs.getLong("readskipcount");
				processSkipCount = rs.getLong("processskipcount");
				filterCount = rs.getLong("filtercount");
				writeSkipCount = rs.getLong("writeSkipCount");
				startTS = rs.getTimestamp("startTime");
				endTS = rs.getTimestamp("endTime");
				// get the object based data
				Serializable persistentData = null;
				byte[] pDataBytes = rs.getBytes("persistentData");
				if (pDataBytes != null) {
					objectIn = new TCCLObjectInputStream(
							new ByteArrayInputStream(pDataBytes));
					persistentData = (Serializable) objectIn.readObject();
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

				logger.fine("BatchStatus: " + batchstatus + " | StepName: "
						+ stepname + " | JobExecID: " + jobexecid
						+ " | StepExecID: " + stepexecid);

				data.add(stepEx);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return data;
	}

	@Override
	public StepExecution getStepExecutionByStepExecutionId(long stepExecId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		long jobexecid = 0;
		long stepexecid = 0;
		String stepname = null;
		String batchstatus = null;
		String exitstatus = null;
		Exception ex = null;
		long readCount = 0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTS = null;
		Timestamp endTS = null;
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(STEP_EXECUTIONS_BY_STEP_ID));
			statement.setLong(1, stepExecId);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobexecid = rs.getLong("jobexecid");
				stepexecid = rs.getLong("stepexecid");
				stepname = rs.getString("stepname");
				batchstatus = rs.getString("batchstatus");
				exitstatus = rs.getString("exitstatus");
				readCount = rs.getLong("readcount");
				writeCount = rs.getLong("writecount");
				commitCount = rs.getLong("commitcount");
				rollbackCount = rs.getLong("rollbackcount");
				readSkipCount = rs.getLong("readskipcount");
				processSkipCount = rs.getLong("processskipcount");
				filterCount = rs.getLong("filtercount");
				writeSkipCount = rs.getLong("writeSkipCount");
				startTS = rs.getTimestamp("startTime");
				endTS = rs.getTimestamp("endTime");
				// get the object based data
				Serializable persistentData = null;
				byte[] pDataBytes = rs.getBytes("persistentData");
				if (pDataBytes != null) {
					objectIn = new TCCLObjectInputStream(
							new ByteArrayInputStream(pDataBytes));
					persistentData = (Serializable) objectIn.readObject();
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

				logger.fine("stepExecution BatchStatus: " + batchstatus
						+ " StepName: " + stepname);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return stepEx;
	}

	@Override
	public void updateBatchStatusOnly(long key, BatchStatus batchStatus,
			Timestamp updatets) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(UPDATE_BATCH_STATUS_ONLY));
			statement.setString(1, batchStatus.name());
			statement.setTimestamp(2, updatets);
			statement.setLong(3, key);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
	}

	@Override
	public void updateWithFinalExecutionStatusesAndTimestamps(long key,
			BatchStatus batchStatus, String exitStatus, Timestamp updatets) {
		// TODO Auto-generated methddod stub
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(UPDATE_FINAL_STATUS_AND_TIMESTAMP));

			statement.setString(1, batchStatus.name());
			statement.setString(2, exitStatus);
			statement.setTimestamp(3, updatets);
			statement.setTimestamp(4, updatets);
			statement.setLong(5, key);

			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}

	}

	public void markJobStarted(long key, Timestamp startTS) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(MARK_JOB_STARTED));

			statement.setString(1, BatchStatus.STARTED.name());
			statement.setTimestamp(2, startTS);
			statement.setTimestamp(3, startTS);
			statement.setLong(4, key);

			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
	}

	@Override
	public IJobExecution jobOperatorGetJobExecution(long jobExecutionId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		IJobExecution jobEx = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_GET_JOB_EXECUTION));
			statement.setLong(1, jobExecutionId);
			rs = statement.executeQuery();

			jobEx = (rs.next()) ? readJobExecutionRecord(rs) : null;
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
		}
		return jobEx;
	}

	@Override
	public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<IJobExecution> data = new ArrayList<IJobExecution>();
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_GET_JOB_EXECUTIONS));
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				data.add(readJobExecutionRecord(rs));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
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
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Set<Long> executionIds = new HashSet<Long>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_OPERATOR_GET_RUNNING_EXECUTIONS));
			statement.setString(1, BatchStatus.STARTED.name());
			statement.setString(2, BatchStatus.STARTING.name());
			statement.setString(3, BatchStatus.STOPPING.name());
			statement.setString(4, jobName);
			rs = statement.executeQuery();
			while (rs.next()) {
				executionIds.add(rs.getLong("jobexecid"));
			}

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return executionIds;
	}

	@Override
	public String getJobCurrentTag(long jobInstanceId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String apptag = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(SELECT_JOBINSTANCEDATA_APPTAG));
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				apptag = rs.getString(APPTAG);
			}

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return apptag;
	}

	@Override
	public void purge(String apptag) {

		logger.entering(CLASSNAME, "purge", apptag);
		String deleteJobs = queryStrings.get(DELETE_JOBS);
		String deleteJobExecutions = queryStrings.get(DELETE_JOB_EXECUTIONS);
		String deleteStepExecutions = queryStrings.get(DELETE_STEP_EXECUTIONS);

		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(deleteStepExecutions);
			statement.setString(1, apptag);
			statement.executeUpdate();

			statement = conn.prepareStatement(deleteJobExecutions);
			statement.setString(1, apptag);
			statement.executeUpdate();

			statement = conn.prepareStatement(deleteJobs);
			statement.setString(1, apptag);
			statement.executeUpdate();

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {

			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "purge");

	}

	@Override
	public JobStatus getJobStatusFromExecution(long executionId) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		JobStatus retVal = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(GET_JOB_STATUS_FROM_EXECUTIONS));
			statement.setLong(1, executionId);
			rs = statement.executeQuery();
			byte[] buf = null;
			if (rs.next()) {
				buf = rs.getBytes("obj");
			}
			retVal = (JobStatus) deserializeObject(buf);
		} catch (Exception e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "executeQuery");
		return retVal;
	}

	public long getJobInstanceIdByExecutionId(long executionId)
			throws NoSuchJobExecutionException {
		long instanceId = 0;

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(JOB_INSTANCE_ID_BY_EXECUTION_ID));
			statement.setObject(1, executionId);
			rs = statement.executeQuery();
			if (rs.next()) {
				instanceId = rs.getLong("jobinstanceid");
			} else {
				String msg = "Did not find job instance associated with executionID ="
						+ executionId;
				logger.fine(msg);
				throw new NoSuchJobExecutionException(msg);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return instanceId;
	}

	/**
	 * This method is used to serialized an object saved into a table BLOB
	 * field.
	 * 
	 * @param theObject
	 *            the object to be serialized
	 * @return a object byte array
	 * @throws IOException
	 */
	protected byte[] serializeObject(Serializable theObject) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(baos);
		oout.writeObject(theObject);
		byte[] data = baos.toByteArray();
		baos.close();
		oout.close();

		return data;
	}

	/**
	 * This method is used to de-serialized a table BLOB field to its original
	 * object form.
	 * 
	 * @param buffer
	 *            the byte array save a BLOB
	 * @return the object saved as byte array
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected Serializable deserializeObject(byte[] buffer) throws IOException,
			ClassNotFoundException {

		Serializable theObject = null;
		ObjectInputStream objectIn = null;

		if (buffer != null) {
			objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer));
			theObject = (Serializable) objectIn.readObject();
			objectIn.close();
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
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		JobInstanceImpl jobInstance = null;

		try {
			conn = getConnection();

			statement = conn.prepareStatement(
					queryStrings.get(CREATE_SUB_JOB_INSTANCE),
					new String[] { "JOBINSTANCEID" });

			statement.setString(1, name);
			statement.setString(2, apptag);
			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if (rs.next()) {
				long jobInstanceID = rs.getLong(1);
				jobInstance = new JobInstanceImpl(jobInstanceID);
				jobInstance.setJobName(name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
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
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		JobInstanceImpl jobInstance = null;

		try {
			conn = getConnection();

			statement = conn.prepareStatement(
					queryStrings.get(CREATE_JOB_INSTANCE),
					new String[] { "JOBINSTANCEID" });

			statement.setString(1, name);
			statement.setString(2, apptag);
			statement.executeUpdate();

			rs = statement.getGeneratedKeys();

			if (rs.next()) {
				long jobInstanceID = rs.getLong(1);
				jobInstance = new JobInstanceImpl(jobInstanceID, jobXml);
				jobInstance.setJobName(name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return jobInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#
	 * createJobExecution(com.ibm.jbatch.container.jsl.JobNavigator,
	 * javax.batch.runtime.JobInstance, java.util.Properties,
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
                
                if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
                    RequestEvent requestEvent = constructJBatchExecutionEvent(jobExecution);
                    requestTracing.traceRequestEvent(requestEvent);
                }
                
		return jobExecution;
	}

	protected long createRuntimeJobExecutionEntry(JobInstance jobInstance,
			Properties jobParameters, BatchStatus batchStatus,
			Timestamp timestamp) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long newJobExecutionId = 0L;
		try {
			conn = getConnection();

			statement = conn.prepareStatement(
					queryStrings.get(CREATE_JOB_EXECUTION_ENTRY),
					new String[] { "JOBEXECID" });

			statement.setLong(1, jobInstance.getInstanceId());
			statement.setTimestamp(2, timestamp);
			statement.setTimestamp(3, timestamp);
			statement.setString(4, batchStatus.name());
			statement.setObject(5, serializeObject(jobParameters));
			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if (rs.next()) {
				newJobExecutionId = rs.getLong(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
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
			logger.fine("batchStatus: " + batchStatus + " | stepName: "
					+ stepName);
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
		for (int i = 0; i < metrics.length; i++) {
			if (metrics[i].getType().equals(MetricImpl.MetricType.READ_COUNT)) {
				readCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.WRITE_COUNT)) {
				writeCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.PROCESS_SKIP_COUNT)) {
				processSkipCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.COMMIT_COUNT)) {
				commitCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.ROLLBACK_COUNT)) {
				rollbackCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.READ_SKIP_COUNT)) {
				readSkipCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.FILTER_COUNT)) {
				filterCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.WRITE_SKIP_COUNT)) {
				writeSkipCount = metrics[i].getValue();
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

		logger.entering(CLASSNAME, "createStepExecution", new Object[] {
				rootJobExecId, batchStatus,
				exitStatus == null ? "<null>" : exitStatus, stepName,
				readCount, writeCount, commitCount, rollbackCount,
				readSkipCount, processSkipCount, filterCount, writeSkipCount,
				startTime == null ? "<null>" : startTime,
				endTime == null ? "<null>" : endTime,
				persistentData == null ? "<null>" : persistentData });

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		StepExecutionImpl stepExecution = null;
		String query = queryStrings.get(CREATE_STEP_EXECUTION);

		try {
			conn = getConnection();

			statement = conn.prepareStatement(query,
					new String[] { "STEPEXECID" });

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
			rs = statement.getGeneratedKeys();
			if (rs.next()) {
				long stepExecutionId = rs.getLong(1);
				stepExecution = new StepExecutionImpl(rootJobExecId,
						stepExecutionId);
				stepExecution.setStepName(stepName);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
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

		for (int i = 0; i < metrics.length; i++) {
			if (metrics[i].getType().equals(MetricImpl.MetricType.READ_COUNT)) {
				readCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.WRITE_COUNT)) {
				writeCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.PROCESS_SKIP_COUNT)) {
				processSkipCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.COMMIT_COUNT)) {
				commitCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.ROLLBACK_COUNT)) {
				rollbackCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.READ_SKIP_COUNT)) {
				readSkipCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.FILTER_COUNT)) {
				filterCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(
					MetricImpl.MetricType.WRITE_SKIP_COUNT)) {
				writeSkipCount = metrics[i].getValue();
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
	 * @param rootJobExecutionId
	 *            JobExecution id of the top-level job
	 * @param stepName
	 *            Step name of the top-level stepName
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

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		long readCount = 0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION));

			statement.setString(
					1,
					getPartitionLevelJobInstanceWildCard(rootJobExecutionId,
							stepContext.getStepName()));
			rs = statement.executeQuery();
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
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
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
			logger.fine("batchStatus: " + batchStatus + " | stepName: "
					+ stepName + " | stepExecID: "
					+ stepContext.getStepExecutionId());
		}

		Timestamp startTime = stepContext.getStartTimeTS();
		Timestamp endTime = stepContext.getEndTimeTS();

		Serializable persistentData = stepContext.getPersistentUserData();

		if (logger.isLoggable(Level.FINER)) {
			logger.log(
					Level.FINER,
					"About to update StepExecution with: ",
					new Object[] { stepExecutionId, batchStatus,
							exitStatus == null ? "<null>" : exitStatus,
							stepName, readCount, writeCount, commitCount,
							rollbackCount, readSkipCount, processSkipCount,
							filterCount, writeSkipCount,
							startTime == null ? "<null>" : startTime,
							endTime == null ? "<null>" : endTime,
							persistentData == null ? "<null>" : persistentData });
		}

		Connection conn = null;
		PreparedStatement statement = null;
		String query = queryStrings.get(UPDATE_STEP_EXECUTION_WITH_METRICS);

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
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
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
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
		Connection conn = null;
		PreparedStatement statement = null;
		JobStatus jobStatus = new JobStatus(jobInstanceId);
		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(CREATE_JOBSTATUS));
			statement.setLong(1, jobInstanceId);
			statement.setBytes(2, serializeObject(jobStatus));
			statement.executeUpdate();

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
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
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		RuntimeJobExecution jobExecution = null;
		String query = queryStrings.get(GET_JOB_STATUS);
		JobStatus jobStatus = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, instanceId);
			rs = statement.executeQuery();
			if (rs.next()) {
				jobStatus = (JobStatus) deserializeObject(rs.getBytes(1));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
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
		logger.entering(CLASSNAME, "updateJobStatus", new Object[] {
				instanceId, jobStatus });
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Updating Job Status to: " + jobStatus.getBatchStatus());
		}
		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(UPDATE_JOBSTATUS));
			statement.setBytes(1, serializeObject(jobStatus));
			statement.setLong(2, instanceId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
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
		Connection conn = null;
		PreparedStatement statement = null;
		StepStatus stepStatus = new StepStatus(stepExecId);
		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(CREATE_STEP_STATUS));
			statement.setLong(1, stepExecId);
			statement.setBytes(2, serializeObject(stepStatus));
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
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
		logger.entering(CLASSNAME, "getStepStatus", new Object[] { instanceId,
				stepName });
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		RuntimeJobExecution jobExecution = null;
		String query = queryStrings.get(GET_STEP_STATUS);
		StepStatus stepStatus = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, instanceId);
			statement.setString(2, stepName);
			rs = statement.executeQuery();
			if (rs.next()) {
				stepStatus = (StepStatus) deserializeObject(rs.getBytes(1));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getStepStatus",
				stepStatus == null ? "<null>" : stepStatus);
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
		logger.entering(CLASSNAME, "updateStepStatus", new Object[] {
				stepExecutionId, stepStatus });

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Updating StepStatus to: "
					+ stepStatus.getBatchStatus());
		}
		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(UPDATE_STEP_STATUS));
			statement.setBytes(1, serializeObject(stepStatus));
			statement.setLong(2, stepExecutionId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
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
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String query = queryStrings.get(GET_TAGNAME);
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, jobExecutionId);
			rs = statement.executeQuery();
			if (rs.next()) {
				apptag = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getTagName");
		return apptag;
	}

	@Override
	public long getMostRecentExecutionId(long jobInstanceId) {
		logger.entering(CLASSNAME, "getMostRecentExecutionId", jobInstanceId);
		long mostRecentId = -1;
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String query = queryStrings.get(GET_MOST_RECENT_EXECUTION_ID);

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			if (rs.next()) {
				mostRecentId = rs.getLong(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getMostRecentExecutionId");
		return mostRecentId;
	}

	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}

	/**
	 * Method invoked to insert the table key strings into a hashmap and to add
	 * the prefix and suffix to the table names
	 **/

	protected Map<String, String> getSharedTableMap(IBatchConfig batchConfig) {
		String prefix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_PREFIX_PROPERTY, "");
		String suffix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_SUFFIX_PROPERTY, "");
		
		Map<String, String> result = new HashMap<String, String>(6);
		result.put(JOB_INSTANCE_TABLE_KEY, prefix + "JOBINSTANCEDATA" + suffix);
		result.put(EXECUTION_INSTANCE_TABLE_KEY, prefix	+ "EXECUTIONINSTANCEDATA" + suffix);
		result.put(STEP_EXECUTION_INSTANCE_TABLE_KEY, prefix + "STEPEXECUTIONINSTANCEDATA" + suffix);
		result.put(JOB_STATUS_TABLE_KEY, prefix + "JOBSTATUS" + suffix);
		result.put(STEP_STATUS_TABLE_KEY, prefix + "STEPSTATUS" + suffix);
		result.put(CHECKPOINT_TABLE_KEY, prefix + "CHECKPOINTDATA" + suffix);
		
		return result;
	}

	/**
	 * Method invoked to insert the query strings used by all database types
	 * into a hashmap
	 * @throws SQLException 
	 **/

	protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
		queryStrings = new HashMap<>();
		
		queryStrings.put(
	        Q_SET_SCHEMA, 
	        "SET SCHEMA ?");
		
		queryStrings.put(
	        SELECT_CHECKPOINTDATA, 
	        "select id, obj from " + 
	            tableNames.get(CHECKPOINT_TABLE_KEY) + 
            " where id = ?");
		
		queryStrings.put(
	        INSERT_CHECKPOINTDATA,
			"insert into " + 
		        tableNames.get(CHECKPOINT_TABLE_KEY) + 
	        " values(?, ?)");
		
		queryStrings.put(
	        UPDATE_CHECKPOINTDATA,
			"update " + 
		        tableNames.get(CHECKPOINT_TABLE_KEY) + 
	        " set obj = ? where id = ?");
		
		queryStrings.put(
            LOCK_CHECKPOINTDATA,
            "lock table " + 
                tableNames.get(CHECKPOINT_TABLE_KEY) + 
            " in exclusive mode");
	
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
	 * Method invoked to insert the Derby create table strings into a hashmap
	 **/

	protected Map<String, String> setCreateDerbyStringsMap(
			IBatchConfig batchConfig) {
		createDerbyStrings = new HashMap<>();
		createDerbyStrings.put(DERBY_CREATE_TABLE_CHECKPOINTDATA,
				"CREATE TABLE " + tableNames.get(CHECKPOINT_TABLE_KEY)
						+ "(id VARCHAR(512),obj BLOB)");
		createDerbyStrings
				.put(DERBY_CREATE_TABLE_JOBINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ "("
								+ "jobinstanceid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBINSTANCE_PK PRIMARY KEY,"
								+ "name VARCHAR(512)," + "apptag VARCHAR(512))");

		createDerbyStrings
				.put(DERBY_CREATE_TABLE_EXECUTIONINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ "("
								+ "jobexecid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBEXECUTION_PK PRIMARY KEY,"
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

		createDerbyStrings
				.put(DERBY_CREATE_TABLE_STEPINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ "("
								+ "stepexecid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT STEPEXECUTION_PK PRIMARY KEY,"
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

		createDerbyStrings
				.put(DERBY_CREATE_TABLE_JOBSTATUS,
						"CREATE TABLE "
								+ tableNames.get(JOB_STATUS_TABLE_KEY)
								+ "("
								+ "id BIGINT CONSTRAINT JOBSTATUS_PK PRIMARY KEY,"
								+ "obj BLOB,"
								+ "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid) ON DELETE CASCADE)");

		createDerbyStrings
				.put(DERBY_CREATE_TABLE_STEPSTATUS,
						"CREATE TABLE "
								+ tableNames.get(STEP_STATUS_TABLE_KEY)
								+ "("
								+ "id BIGINT CONSTRAINT STEPSTATUS_PK PRIMARY KEY,"
								+ "obj BLOB,"
								+ "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ "(stepexecid) ON DELETE CASCADE)");

		return createDerbyStrings;

	}
        
    private RequestEvent constructJBatchExecutionEvent(RuntimeJobExecution jobExecution) {
        RequestEvent requestEvent = new RequestEvent("JBatchExecutionContextEvent");
        
        try {
            requestEvent.addProperty("Execution ID", Long.toString(jobExecution.getExecutionId()));
            requestEvent.addProperty("Job ID", Long.toString(jobExecution.getInstanceId()));
            requestEvent.addProperty("Job Name", jobExecution.getJobInstance().getJobName());
            requestEvent.addProperty("Batch Status", jobExecution.getJobOperatorJobExecution().getBatchStatus().toString());

            if (jobExecution.getJobParameters() != null) {
                requestEvent.addProperty("Job Parameters", jobExecution.getJobParameters().toString());
            } else {
                requestEvent.addProperty("Job Parameters", "null");
            } 
        } catch (NullPointerException e) {
            logger.log(Level.INFO, "NullPointerException when creating request tracing JBatchExecutionContextEvent");
        }                   
        
        return requestEvent;
    }
}
