/*
 * Copyright (c) 2014, 2015 C2B2 Consulting Limited. All rights reserved.
 
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
import com.ibm.jbatch.spi.services.IBatchConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.JobInstance;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * 
 * Oracle Persistence Manager
 */

public class OraclePersistenceManager extends JBatchJDBCPersistenceManager implements OracleJDBCConstants{

	private static final String CLASSNAME = OraclePersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;
	
	// oracle create table strings
	protected Map<String, String> createOracleTableStrings;
	protected Map<String, String> createOracleIndexStrings;

	@Override
	protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
		Map<String, String> result = super.getSharedQueryMap(batchConfig);
	//	if (schema != null && schema.length() != 0) {
			result.put(
					MOST_RECENT_STEPS_FOR_JOB,
					"select A.* from "
							+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
							+ " A inner join "
							+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
							+ " B on A.jobexecid = B.jobexecid where B.jobinstanceid = ? order by A.stepexecid desc");
			result.put(
					JOB_OPERATOR_GET_JOB_EXECUTION,
					"select A.jobexecid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.jobinstanceid, A.batchstatus, A.exitstatus, B.name from "
							+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
							+ " A inner join "
							+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
							+ " B on A.jobinstanceid = B.jobinstanceid where jobexecid = ?");
			result.put(
					JOB_OPERATOR_GET_JOB_EXECUTIONS,
					"select A.jobexecid, A.jobinstanceid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.batchstatus, A.exitstatus, B.name from "
							+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
							+ " A inner join "
							+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
							+ " B ON A.jobinstanceid = B.jobinstanceid where A.jobinstanceid = ?");
			result.put(GET_JOB_STATUS_FROM_EXECUTIONS, "select A.obj from "
					+ tableNames.get(JOB_STATUS_TABLE_KEY) + " A inner join "
				    + tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
					+ " B on A.id = B.jobinstanceid where B.jobexecid = ?");
	//	}
		return result;
	}

	@Override
	public void init(IBatchConfig batchConfig)
			throws BatchContainerServiceException {
		logger.config("Entering CLASSNAME.init(), batchConfig =" + batchConfig);

		this.batchConfig = batchConfig;

		schema = batchConfig.getDatabaseConfigurationBean().getSchema();

		jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
		

		if (jndiName == null || jndiName.equals("")) {
			throw new BatchContainerServiceException(
					"JNDI name is not defined.");
		}

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
		
		logger.config("JNDI name = " + jndiName);


		try {
			if (!isOracleSchemaValid()) {
				setDefaultSchema();

			}
			checkOracleTables();

		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw new BatchContainerServiceException(e);
		}

		logger.config("Exiting CLASSNAME.init()");
	}

	/**
	 * Check the schema exists and if not we will use the default schema
	 * @return
	 * @throws SQLException
	 */
	private boolean isOracleSchemaValid() throws SQLException {

		logger.entering(CLASSNAME, "isOracleSchemaValid");
		boolean result = false;
		Connection conn = null;
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;

		try {
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
		logger.exiting(CLASSNAME, "isOracleSchemaValid", false);

		return result;

	}

	/**
	 * Verify the relevant JBatch tables exist.
	 * @throws SQLException
	 */
	private void checkOracleTables() throws SQLException {
		// put the create table strings into a hashmap
		createOracleTableStrings = setOracleTableMap(batchConfig);

		// put the create index strings into a hashmap
		createOracleIndexStrings = setOracleIndexMap(batchConfig);
		logger.entering(CLASSNAME, "checkOracleTables");

		createOracleTableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_CHECKPOINTDATA));

		// do same check for indexes, triggers and sequences
		if (!checkOracleIndexExists(CREATE_CHECKPOINTDATA_INDEX_KEY)) {
			executeStatement(createOracleIndexStrings
					.get(CREATE_CHECKPOINTDATA_INDEX));
		}

		createOracleTableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_JOBINSTANCEDATA));
		createOracleSequenceNotExists(JOBINSTANCEDATA_SEQ_KEY,
				createOracleTableStrings.get(CREATE_JOBINSTANCEDATA_SEQ));
		createOracleTriggerNotExists(JOBINSTANCEDATA_TRG_KEY,
				createOracleTableStrings.get(CREATE_JOBINSTANCEDATA_TRG));

		createOracleTableNotExists(
				tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_EXECUTIONINSTANCEDATA));
		createOracleSequenceNotExists(EXECUTIONINSTANCEDATA_SEQ_KEY,
				createOracleTableStrings.get(CREATE_EXECUTIONINSTANCEDATA_SEQ));
		createOracleTriggerNotExists(EXECUTIONINSTANCEDATA_TRG_KEY,
				createOracleTableStrings.get(CREATE_EXECUTIONINSTANCEDATA_TRG));

		createOracleTableNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_STEPINSTANCEDATA));
		createOracleSequenceNotExists(STEPINSTANCEDATA_SEQ_KEY,
				createOracleTableStrings.get(CREATE_STEPINSTANCEDATA_SEQ));
		createOracleTriggerNotExists(STEPINSTANCEDATA_TRG_KEY,
				createOracleTableStrings.get(CREATE_STEPINSTANCEDATA_TRG));

		createOracleTableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_JOBSTATUS));
		createOracleTableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkOracleTables");
	}

	/**
	 * Create the jbatch tables if they do not exist.
	 * @param tableName
	 * @param createTableStatement
	 * @throws SQLException
	 */
	protected void createOracleTableNotExists(String tableName,
			String createTableStatement) throws SQLException {
		logger.entering(CLASSNAME, "createOracleTableNotExists", new Object[] {
				tableName, createTableStatement });

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			String query = "SELECT lower(owner),lower(table_name) FROM all_tables where lower(owner) =  "
					+ "\'"
					+ schema.toLowerCase()
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

		logger.exiting(CLASSNAME, "createOracleTableNotExists");
	}

	/**
	 * Create the jbatch sequences if they do not exist
	 * @param sequencename
	 * @param seqstmt
	 * @throws SQLException
	 */
	public void createOracleSequenceNotExists(String sequencename,
			String seqstmt) throws SQLException {
		logger.entering(CLASSNAME, "createOracleSequenceNotExists");
		// check sequences that exist
		Connection conn = null;
		boolean seqexists = false;
		ResultSet results = null;
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			Statement stmt = conn.createStatement();

			results = stmt
					.executeQuery("select lower(sequence_name) from user_sequences where lower(sequence_name)="
							+ "\'" + sequencename.toLowerCase() + "\'");

			while (results.next()) {

				seqexists = true;
				break;

			}

			if (!seqexists) {
				// create the sequence

				ps = conn.prepareStatement(seqstmt);
				ps.executeUpdate();

			}
		} catch (SQLException e) {

			e.printStackTrace();
			throw e;
		} finally {
			cleanupConnection(conn, results, ps);
		}
		logger.exiting(CLASSNAME, "createOracleSequenceNotExists");

	}
    /**
     * Create the relevant jbatch triggers as required
     * @param triggername
     * @param trgstmt
     * @throws SQLException
     */
	public void createOracleTriggerNotExists(String triggername, String trgstmt)
			throws SQLException {
		logger.entering(CLASSNAME, "createOracleTriggerNotExists");
		Connection conn = null;
		boolean triggerexists = false;
		ResultSet results = null;
		Statement ps = null;
		try {
			conn = getConnection();
			Statement stmt = conn.createStatement();

			results = stmt
					.executeQuery("select lower(trigger_name) from user_triggers where lower(trigger_name)="
							+ "\'" + triggername.toLowerCase() + "\'");

			while (results.next()) {

				triggerexists = true;
				break;

			}

			if (!triggerexists) {
				// create the trigger

				ps = conn.createStatement();
                                ps.executeUpdate(trgstmt);

			}
		} catch (SQLException e) {

			e.printStackTrace();
			throw e;
		} finally {
			cleanupConnection(conn, results, null);
		}
		logger.exiting(CLASSNAME, "createOracleTriggerNotExists");

	}
    /**
     * Check indexes exist for the jbatch tables
     * @param indexname
     * @return
     * @throws SQLException
     */
	public boolean checkOracleIndexExists(String indexname) throws SQLException {
		logger.entering(CLASSNAME, "createOracleIndexNotExists");
		Connection conn = null;
		boolean indexexists = false;
		ResultSet results = null;
		try {
			conn = getConnection();
			Statement stmt = conn.createStatement();

			results = stmt
					.executeQuery("select lower(index_name) from user_indexes where lower(index_name)="
							+ "\'" + indexname.toLowerCase() + "\'");

			while (results.next()) {

				indexexists = true;
				break;

			}
		} catch (SQLException e) {

			e.printStackTrace();
			throw e;
		} finally {
			cleanupConnection(conn, results, null);
		}
		logger.exiting(CLASSNAME, "createOracleIndexNotExists");
		return indexexists;
	}
	
	/**
	 * Method invoked to insert the Oracle create table, trigger and sequence
	 * strings into a hashmap
	 **/

	protected Map<String, String> setOracleTableMap(IBatchConfig batchConfig) {
		createOracleTableStrings = new HashMap<>();
		createOracleTableStrings.put(CREATE_TABLE_CHECKPOINTDATA, "CREATE TABLE "
				+ tableNames.get(CHECKPOINT_TABLE_KEY)
				+ "(id VARCHAR(512),obj BLOB)");

		createOracleTableStrings
				.put(CREATE_TABLE_JOBINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ "(jobinstanceid NUMBER(19,0) PRIMARY KEY,name VARCHAR2(512), apptag VARCHAR(512))");
		createOracleTableStrings.put(CREATE_JOBINSTANCEDATA_SEQ,
				"CREATE SEQUENCE JOBINSTANCEDATA_SEQ");
		createOracleTableStrings
				.put(CREATE_JOBINSTANCEDATA_TRG,
						"CREATE OR REPLACE TRIGGER JOBINSTANCEDATA_TRG BEFORE INSERT ON "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " FOR EACH ROW BEGIN SELECT JOBINSTANCEDATA_SEQ.nextval INTO :new.jobinstanceid FROM dual; END;");

		createOracleTableStrings
				.put(CREATE_TABLE_EXECUTIONINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ "(jobexecid NUMBER(19,0) PRIMARY KEY,jobinstanceid	NUMBER(19,0),createtime	TIMESTAMP,starttime		TIMESTAMP,endtime		TIMESTAMP,updatetime	TIMESTAMP,parameters BLOB,batchstatus VARCHAR2(512),exitstatus VARCHAR2(512),CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ "(jobinstanceid))");
		createOracleTableStrings.put(CREATE_EXECUTIONINSTANCEDATA_SEQ,
				"CREATE SEQUENCE EXECUTIONINSTANCEDATA_SEQ");
		createOracleTableStrings
				.put(CREATE_EXECUTIONINSTANCEDATA_TRG,
						"CREATE OR REPLACE TRIGGER EXECUTIONINSTANCEDATA_TRG BEFORE INSERT ON "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " FOR EACH ROW BEGIN SELECT EXECUTIONINSTANCEDATA_SEQ.nextval INTO :new.jobexecid FROM dual;END;");

		createOracleTableStrings
				.put(CREATE_TABLE_STEPINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ "(stepexecid NUMBER(19,0) PRIMARY KEY, jobexecid NUMBER(19,0),batchstatus VARCHAR2(512),exitstatus VARCHAR2(512),stepname VARCHAR(512),readcount NUMBER(11, 0),writecount NUMBER(11, 0),commitcount NUMBER(11, 0),rollbackcount NUMBER(11, 0),readskipcount NUMBER(11, 0),processskipcount NUMBER(11, 0),filtercount NUMBER(11, 0),writeskipcount NUMBER(11, 0),startTime TIMESTAMP,endTime TIMESTAMP, persistentData BLOB, CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ "(jobexecid))");
		createOracleTableStrings.put(CREATE_STEPINSTANCEDATA_SEQ,
				"CREATE SEQUENCE STEPEXECUTIONINSTANCEDATA_SEQ");
		createOracleTableStrings
				.put(CREATE_STEPINSTANCEDATA_TRG,
						"CREATE OR REPLACE TRIGGER STEPEXECUTIONINSTANCEDATA_TRG BEFORE INSERT ON "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " FOR EACH ROW BEGIN SELECT STEPEXECUTIONINSTANCEDATA_SEQ.nextval INTO :new.stepexecid FROM dual;END;");

		createOracleTableStrings
				.put(CREATE_TABLE_JOBSTATUS,
						"CREATE TABLE "
								+ tableNames.get(JOB_STATUS_TABLE_KEY)
								+ "(id NUMBER(19,0) PRIMARY KEY,"
								+ "obj BLOB,"
								+ "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid) ON DELETE CASCADE)");

		createOracleTableStrings
				.put(CREATE_TABLE_STEPSTATUS,
						"CREATE TABLE "
								+ tableNames.get(STEP_STATUS_TABLE_KEY)
								+ "(id NUMBER(19,0) PRIMARY KEY,"
								+ "obj BLOB,"
								+ "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " (stepexecid) ON DELETE CASCADE)");
		return createOracleTableStrings;
	}

	/**
	 * Method invoked to insert the Oracle create index strings into a hashmap
	 **/

	protected Map<String, String> setOracleIndexMap(IBatchConfig batchConfig) {
		createOracleIndexStrings = new HashMap<>();
		createOracleIndexStrings.put(
				CREATE_CHECKPOINTDATA_INDEX,
				"create index chk_index on "
						+ tableNames.get(CHECKPOINT_TABLE_KEY) + "(id)");
		return createOracleIndexStrings;
	}
	

}
