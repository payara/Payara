/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2014-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobInstance;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_PREFIX_PROPERTY;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_SUFFIX_PROPERTY;

/**
 * PostgreSQL Persistence Manager
 */

public class PostgresPersistenceManager extends JBatchJDBCPersistenceManager
		implements PostgresJDBCConstants {

	private static final String CLASSNAME = PostgresPersistenceManager.class
			.getName();

	private static final Logger logger = Logger.getLogger(CLASSNAME);

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
		try (PreparedStatement preparedStatement = connection.prepareStatement("set search_path to " + schema)) {
			preparedStatement.executeUpdate();
		} finally {
			logger.log(Level.FINEST, "Exiting {0}.setSchemaOnConnection()", CLASSNAME);
		}
	}

	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		logger.log(Level.CONFIG, "Entering CLASSNAME.init(), batchConfig ={0}", batchConfig);

		this.batchConfig = batchConfig;

		schema = batchConfig.getDatabaseConfigurationBean().getSchema();
		jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
                prefix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_PREFIX_PROPERTY, "");
	        suffix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_SUFFIX_PROPERTY, "");

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

		tableNames = getSharedTableMap();

		try {
			queryStrings = getSharedQueryMap(batchConfig);
		} catch (SQLException e1) {
			throw new BatchContainerServiceException(e1);
		}


		logger.log(Level.CONFIG, "JNDI name = {0}", jndiName);

		if (jndiName == null || jndiName.equals("")) {
			throw new BatchContainerServiceException(
					"JNDI name is not defined.");
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

		logger.config("Exiting CLASSNAME.init()");
	}

	/**
	 * Check if the schema is valid. If not use the default schema
	 *
	 * @return
	 * @throws SQLException
	 */
        @Override
	protected boolean isSchemaValid() throws SQLException {

		boolean result = false;

		try (Connection conn = getConnectionToDefaultSchema()) {
			logger.entering(CLASSNAME, "isPostgresSchemaValid");
			DatabaseMetaData dbmd = conn.getMetaData();
			try (ResultSet rs = dbmd.getSchemas()) {
				while (rs.next()) {
					String schemaname = rs.getString("TABLE_SCHEM");
					if (schema.equalsIgnoreCase(schemaname)) {
						logger.exiting(CLASSNAME, "isSchemaValid", true);
						return true;
					}
				}
			}
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		}

		logger.exiting(CLASSNAME, "isPostgresSchemaValid", false);

		return result;

	}

	/**
	 * Check the JBatch Tables exist in the relevant schema
	 *
	 * @throws SQLException
	 */
        @Override
	protected void checkTables () throws SQLException {
		logger.entering(CLASSNAME, "checkPostgresTables Postgres");
                setCreatePostgresStringsMap(tableNames);
		createTableIfNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createPostgresStrings.get(POSTGRES_CREATE_TABLE_CHECKPOINTDATA));

		createTableIfNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createPostgresStrings
						.get(POSTGRES_CREATE_TABLE_JOBINSTANCEDATA));

		createTableIfNotExists(
				tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createPostgresStrings
						.get(POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA));

		createTableIfNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createPostgresStrings
						.get(POSTGRES_CREATE_TABLE_STEPINSTANCEDATA));

		createTableIfNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createPostgresStrings.get(POSTGRES_CREATE_TABLE_JOBSTATUS));
		createTableIfNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createPostgresStrings.get(POSTGRES_CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkAllTables Postgres");
	}

        @Override
        public boolean checkIfTableExists(DataSource dSource, String tableName, String schemaName) {
                dataSource = dSource;

                boolean result = true;

                try (Connection connection = dataSource.getConnection()) {
                    schema = schemaName;

                    if (!isSchemaValid()) {
                        setDefaultSchema();
                    }

                    try(Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_READ_ONLY)) {
						String query = "select lower(table_schema),lower(table_name) FROM information_schema.tables where lower(table_schema)= "
								+ "\'"
								+ schema
								+ "\'"
								+ " and lower(table_name)= "
								+ "\'"
								+ tableName.toLowerCase() + "\'";

						try (ResultSet resultSet = statement.executeQuery(query)) {
							int rowcount = getTableRowCount(resultSet);
							if (rowcount == 0 && !resultSet.next()) {
									result = false;
							}
						}

					}
                } catch (SQLException ex) {
                    logger.severe(ex.getLocalizedMessage());
                }

                return result;
        }

	/**
	 * Method invoked to insert the Postgres create table strings into a hashmap
	 **/

	private Map<String, String> setCreatePostgresStringsMap(Map<String, String> tableNames) {
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
		JobInstanceImpl jobInstance = null;

		try (Connection conn = getConnection();
			 PreparedStatement statement = conn.prepareStatement(
								queryStrings.get(CREATE_SUB_JOB_INSTANCE),
								Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, name);
			statement.setString(2, apptag);
			statement.executeUpdate();
			try(ResultSet rs = statement.getGeneratedKeys()) {
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
								Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, name);
			statement.setString(2, apptag);
			statement.executeUpdate();
			try(ResultSet rs = statement.getGeneratedKeys()) {
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

	@Override
	protected long createRuntimeJobExecutionEntry(JobInstance jobInstance,
			Properties jobParameters, BatchStatus batchStatus,
			Timestamp timestamp) {
		long newJobExecutionId = 0L;
		try (
			Connection conn = getConnection();
			PreparedStatement statement = conn.prepareStatement(
								queryStrings.get(CREATE_JOB_EXECUTION_ENTRY),
								Statement.RETURN_GENERATED_KEYS);

				) {
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

		StepExecutionImpl stepExecution = null;
		String query = queryStrings.get(CREATE_STEP_EXECUTION);

		try (Connection conn = getConnection();
			 PreparedStatement statement = conn.prepareStatement(query,
								Statement.RETURN_GENERATED_KEYS)) {
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

			try(ResultSet rs = statement.getGeneratedKeys()) {
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

        @Override
        public void markJobStarted(long key, Timestamp startTS) {

			logger.entering(CLASSNAME, "markJobStarted",
								new Object[] {key, startTS});

			final int retryMax = Integer.getInteger(P_MJS_RETRY_MAX, MJS_RETRY_MAX_DEFAULT);
			final int retryDelay = Integer.getInteger(P_MJS_RETRY_DELAY, MJS_RETRY_DELAY_DEFAULT);

			logger.log(Level.FINER,P_MJS_RETRY_MAX +
						 " = {0}" + ", " + P_MJS_RETRY_DELAY + " = {1} ms",
						new Object[]{retryMax, retryDelay});

			try (Connection conn = getConnection();
				 PreparedStatement statement = conn.prepareStatement(queryStrings
						.get(MARK_JOB_STARTED))) {
				statement.setString(1, BatchStatus.STARTED.name());
				statement.setTimestamp(2, startTS);
				statement.setTimestamp(3, startTS);
				statement.setLong(4, key);

				// Postgres use of Multi Version Concurrency (MVCC) means that
				// blocking does not occur (particularly a problem in
				// createStepExecution()).
				// The below will check that the row has been committed by the
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
