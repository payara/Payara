/*
 * Copyright (c) 2014, 2016 Payara Foundation. All rights reserved.
 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */

package fish.payara.jbatch.persistence.rdbms;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.jobinstance.JobInstanceImpl;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.spi.services.IBatchConfig;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * PostgreSQL Persistence Manager
 */

public class PostgresPersistenceManager extends JBatchJDBCPersistenceManager
		implements PostgresJDBCConstants {

	private static final String CLASSNAME = PostgresPersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;

	// postgres create table strings
	protected Map<String, String> createPostgresStrings;

	@Override
	protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
		Map<String, String> result = super.getSharedQueryMap(batchConfig);
		if(schema.equals("") || schema.length() == 0){
			schema = setDefaultSchema();
		}
	
	    result.put(Q_SET_SCHEMA, "set search_path to " + schema);
	
		return result;
	}
	
    @Override
    protected void setSchemaOnConnection(Connection connection) throws SQLException {
            PreparedStatement ps = null;
            ps = connection.prepareStatement(queryStrings.get(Q_SET_SCHEMA));
            ps.executeUpdate();
            ps.close();
    }


	@Override
	public void init(IBatchConfig batchConfig)
			throws BatchContainerServiceException {
		logger.config("Entering CLASSNAME.init(), batchConfig =" + batchConfig);

		this.batchConfig = batchConfig;

		schema = batchConfig.getDatabaseConfigurationBean().getSchema();

		jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
		
		try {
			Context ctx = new InitialContext();
			dataSource = (DataSource) ctx.lookup(jndiName);

		} catch (NamingException e) {
			logger.severe("Lookup failed for JNDI name: "
					+ jndiName
					+ ".  One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
			throw new BatchContainerServiceException(e);
		}

		// Load the table names and queries shared between different database
		// types

		tableNames = getSharedTableMap(batchConfig);

		try {
			queryStrings = getSharedQueryMap(batchConfig);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			throw new BatchContainerServiceException(e1);
		}
		// put the create table strings into a hashmap
		// createTableStrings = setCreateTableMap(batchConfig);

		createPostgresStrings = setCreatePostgresStringsMap(batchConfig);

		logger.config("JNDI name = " + jndiName);

		if (jndiName == null || jndiName.equals("")) {
			throw new BatchContainerServiceException(
					"JNDI name is not defined.");
		}



		try {
			if (!isPostgresSchemaValid()) {
				setDefaultSchema();
			}
			checkPostgresTables();

		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw new BatchContainerServiceException(e);
		}

		logger.config("Exiting CLASSNAME.init()");
	}

	/**
	 * Check if the schema is valid. If not use the default schema
	 * 
	 * @return
	 * @throws SQLException
	 */
	private boolean isPostgresSchemaValid() throws SQLException {

		boolean result = false;
		Connection conn = null;
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;

		try {
			logger.entering(CLASSNAME, "isPostgresSchemaValid");
			conn = getConnectionToDefaultSchema();
			dbmd = conn.getMetaData();
			rs = dbmd.getSchemas();

			while (rs.next()) {

				String schemaname = rs.getString("TABLE_SCHEM");
				if (schema.equalsIgnoreCase(schemaname)) {
					logger.exiting(CLASSNAME, "isSchemaValid", true);
					return true;
				}
			}
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		} finally {
			cleanupConnection(conn, rs, null);
		}

		logger.exiting(CLASSNAME, "isPostgresSchemaValid", false);

		return result;

	}

	/**
	 * Check the JBatch Tables exist in the relevant schema
	 * 
	 * @throws SQLException
	 */
	private void checkPostgresTables() throws SQLException {
		logger.entering(CLASSNAME, "checkPostgresTables Postgres");

		createPostgresTableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createPostgresStrings.get(POSTGRES_CREATE_TABLE_CHECKPOINTDATA));

		createPostgresTableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createPostgresStrings
						.get(POSTGRES_CREATE_TABLE_JOBINSTANCEDATA));

		createPostgresTableNotExists(
				tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createPostgresStrings
						.get(POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA));

		createPostgresTableNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createPostgresStrings
						.get(POSTGRES_CREATE_TABLE_STEPINSTANCEDATA));

		createPostgresTableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createPostgresStrings.get(POSTGRES_CREATE_TABLE_JOBSTATUS));
		createPostgresTableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createPostgresStrings.get(POSTGRES_CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkAllTables Postgres");
	}

	/**
	 * Create Postgres tables if they do not exist
	 * 
	 * @param tableName
	 * @param createTableStatement
	 * @throws SQLException
	 */
	protected void createPostgresTableNotExists(String tableName,
			String createTableStatement) throws SQLException {
		logger.entering(CLASSNAME, "createPostgresTableNotExists",
				new Object[] { tableName, createTableStatement });

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			String query = "select lower(table_schema),lower(table_name) FROM information_schema.tables where lower(table_schema)= "
					+ "\'"
					+ schema
					+ "\'"
					+ " and lower(table_name)= "
					+ "\'"
					+ tableName.toLowerCase() + "\'";
			rs = stmt.executeQuery(query);

			int rowcount = getTableRowCount(rs);

			// Create table if it does not exist
			if (rowcount == 0) {
				if (!rs.next()) {
					logger.log(Level.INFO, tableName
							+ " table does not exists. Trying to create it.");
					ps = conn.prepareStatement(createTableStatement);
					ps.executeUpdate();
				}
			}
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		} finally {
			cleanupConnection(conn, ps);
		}

		logger.exiting(CLASSNAME, "createPostgresTableNotExists");
	}

	/**
	 * Method invoked to insert the Postgres create table strings into a hashmap
	 **/

	protected Map<String, String> setCreatePostgresStringsMap(
			IBatchConfig batchConfig) {
		createPostgresStrings = new HashMap<>();
		createPostgresStrings.put(POSTGRES_CREATE_TABLE_CHECKPOINTDATA,
				"CREATE TABLE " + tableNames.get(CHECKPOINT_TABLE_KEY)
						+ "(id character varying (512),obj bytea)");

		createPostgresStrings
				.put(POSTGRES_CREATE_TABLE_JOBINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ "(jobinstanceid serial not null PRIMARY KEY,name character varying (512),apptag VARCHAR(512))");

		createPostgresStrings.put(POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA,
				"CREATE TABLE " + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ "(jobexecid serial not null PRIMARY KEY,"
						+ "jobinstanceid bigint not null REFERENCES "
						+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ "(jobinstanceid)," + "createtime timestamp,"
						+ "starttime timestamp," + "endtime	timestamp,"
						+ "updatetime timestamp," + "parameters bytea,"
						+ "batchstatus character varying (512),"
						+ "exitstatus character varying (512))");

		createPostgresStrings.put(
				POSTGRES_CREATE_TABLE_STEPINSTANCEDATA,
				"CREATE TABLE "
						+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ "(stepexecid	serial not null PRIMARY KEY,"
						+ "jobexecid	bigint not null REFERENCES "
						+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ "(jobexecid),"
						+ "batchstatus character varying (512),"
						+ "exitstatus	character varying (512),"
						+ "stepname	character varying (512),"
						+ "readcount	integer," + "writecount	integer,"
						+ "commitcount integer," + "rollbackcount integer,"
						+ "readskipcount integer,"
						+ "processskipcount integer," + "filtercount integer,"
						+ "writeskipcount integer," + "startTime timestamp,"
						+ "endTime timestamp," + "persistentData	bytea)");

		createPostgresStrings.put(
				POSTGRES_CREATE_TABLE_JOBSTATUS,
				"CREATE TABLE " + tableNames.get(JOB_STATUS_TABLE_KEY)
						+ "(id	bigint not null REFERENCES "
						+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " (jobinstanceid),obj	bytea)");

		createPostgresStrings.put(
				POSTGRES_CREATE_TABLE_STEPSTATUS,
				"CREATE TABLE " + tableNames.get(STEP_STATUS_TABLE_KEY)
						+ "(id	bigint not null REFERENCES "
						+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ " (stepexecid), " + "obj bytea)");

		return createPostgresStrings;
	}

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
					statement.RETURN_GENERATED_KEYS);

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
					statement.RETURN_GENERATED_KEYS);

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

	@Override
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
					statement.RETURN_GENERATED_KEYS);

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
					statement.RETURN_GENERATED_KEYS);

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
        
        @Override
        public void markJobStarted(long key, Timestamp startTS) {
            
                logger.entering(CLASSNAME, "markJobStarted",
                                    new Object[] {key, startTS});
                
                final int retryMax = Integer.getInteger(P_MJS_RETRY_MAX, MJS_RETRY_MAX_DEFAULT);
                final int retryDelay = Integer.getInteger(P_MJS_RETRY_DELAY, MJS_RETRY_DELAY_DEFAULT);
                
                logger.log(Level.FINER,P_MJS_RETRY_MAX + 
                             " = {0}" + ", " + P_MJS_RETRY_DELAY + " = {1} ms", 
                            new Object[]{retryMax, retryDelay});
            
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(queryStrings
					.get(MARK_JOB_STARTED));

			statement.setString(1, BatchStatus.STARTED.name());
			statement.setTimestamp(2, startTS);
			statement.setTimestamp(3, startTS);
			statement.setLong(4, key);
                        
                        // Postgres use of Multi Version Concurrency (MVCC) means that
                        // blocking does not occur (particularly a problem in
                        // createStepExecution()).
                        // The below will check that the row has been commited by the 
                        // initiating thread by retrying the update until at least 1 row 
                        // is updated.
                                            
                        int retryCount = 0; 
                        while ( (statement.executeUpdate() < 1) && (retryCount++ <= retryMax) ) {
                                sleep(retryDelay);
                        }                       
                        logger.log(Level.FINER, "Marking job as started required {0} retries", retryCount);
                        
                        if (retryCount >= retryMax) {
                            logger.log(Level.WARNING, "Failed to mark job as started after {0} attempts", retryCount);
                        }

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
                logger.exiting(CLASSNAME, "markJobStarted");
        }
        
        private static void sleep(int duration){

                try {
                        Thread.sleep(duration);
                } catch(InterruptedException ie) {
                        logger.warning("Thread interrupted");
                        Thread.currentThread().interrupt();
                }
        }
}
