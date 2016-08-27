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
 */

package fish.payara.jbatch.persistence.rdbms;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * 
 * DB2 Persistence Manager
 */

public class DB2PersistenceManager extends JBatchJDBCPersistenceManager implements DB2JDBCConstants {

	private static final String CLASSNAME = JBatchJDBCPersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;
	
	// db2 create table strings
	protected Map<String, String> createDB2Strings;

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
	//	createTableStrings = setCreateTableMap(batchConfig);

		createDB2Strings = setCreateDB2StringsMap(batchConfig);

		logger.config("JNDI name = " + jndiName);

		if (jndiName == null || jndiName.equals("")) {
			throw new BatchContainerServiceException(
					"JNDI name is not defined.");
		}

		
		try {
			if (!isDB2SchemaValid()) {
				setDefaultSchema();
			}
			checkDB2Tables();

		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw new BatchContainerServiceException(e);
		}

		logger.config("Exiting CLASSNAME.init()");
	}
    
	/**
	 * Check the schema exists
	 * @return
	 * @throws SQLException
	 */
	private boolean isDB2SchemaValid() throws SQLException {

		boolean result = false;
		Connection conn = null;
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;

		logger.entering(CLASSNAME, "isDB2SchemaValid");
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
		logger.exiting(CLASSNAME, "isDB2SchemaValid", false);

		return result;

	}
    
	/**
	 * Check the relevant db2 tables exist in the relevant schema
	 * @throws SQLException
	 */
	private void checkDB2Tables() throws SQLException {

		logger.entering(CLASSNAME, "checkDB2Tables");

		createDB2TableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_CHECKPOINTDATA));

		createDB2TableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_JOBINSTANCEDATA));

		createDB2TableNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA));

		createDB2TableNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_STEPINSTANCEDATA));

		createDB2TableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_JOBSTATUS));

		createDB2TableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkDB2Tables");
	}

	/**
	 * Create the jbatch tables if they do not exist
	 * @param tableName
	 * @param createTableStatement
	 * @throws SQLException
	 */
	protected void createDB2TableNotExists(String tableName,
			String createTableStatement) throws SQLException {
		logger.entering(CLASSNAME, "createDB2TableNotExists", new Object[] {
				tableName, createTableStatement });
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			String query = "select name from sysibm.systables where name ="
					+ "\'" + tableName.toUpperCase() + "\'" + "and type = 'T'";
			;
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
			cleanupConnection(conn, rs, ps);
		}

		logger.exiting(CLASSNAME, "createDB2TableNotExists");
	}
	
	/**
	 * Method invoked to insert the DB2 create table strings into a hashmap
	 **/

	protected Map<String, String> setCreateDB2StringsMap(
			IBatchConfig batchConfig) {
		createDB2Strings = new HashMap<>();
		createDB2Strings.put(DB2_CREATE_TABLE_CHECKPOINTDATA, "CREATE TABLE "
				+ tableNames.get(CHECKPOINT_TABLE_KEY)
				+ " (id VARCHAR(512),obj BLOB)");
		createDB2Strings
				.put(DB2_CREATE_TABLE_JOBINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBINSTANCE_PK PRIMARY KEY,name VARCHAR(512), apptag VARCHAR(512))");

		createDB2Strings
				.put(DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " (jobexecid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBEXECUTION_PK PRIMARY KEY,"
								+ "jobinstanceid BIGINT,"
								+ "createtime	TIMESTAMP,"
								+ "starttime	TIMESTAMP,"
								+ "endtime	TIMESTAMP,"
								+ "updatetime	TIMESTAMP,"
								+ "parameters	BLOB,"
								+ "batchstatus VARCHAR(512),"
								+ "exitstatus		VARCHAR(512),"
								+ "CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid))");

		createDB2Strings
				.put(DB2_CREATE_TABLE_STEPINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " (stepexecid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT STEPEXECUTION_PK PRIMARY KEY,"
								+ "jobexecid	BIGINT,"
								+ "batchstatus VARCHAR(512),"
								+ "exitstatus	VARCHAR(512),"
								+ "stepname	VARCHAR(512),"
								+ "readcount	INTEGER,"
								+ "writecount	INTEGER,"
								+ "commitcount INTEGER,"
								+ "rollbackcount INTEGER,"
								+ "readskipcount	INTEGER,"
								+ "processskipcount INTEGER,"
								+ "filtercount INTEGER,"
								+ "writeskipcount	INTEGER,"
								+ "startTime TIMESTAMP,"
								+ "endTime TIMESTAMP,"
								+ "persistentData	BLOB,"
								+ "CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " (jobexecid))");

		createDB2Strings
				.put(DB2_CREATE_TABLE_JOBSTATUS,
						"CREATE TABLE "
								+ tableNames.get(JOB_STATUS_TABLE_KEY)
								+ " (id BIGINT CONSTRAINT JOBSTATUS_PK PRIMARY KEY NOT NULL,obj BLOB,"
								+ "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid) ON DELETE CASCADE)");

		createDB2Strings
				.put(DB2_CREATE_TABLE_STEPSTATUS,
						"CREATE TABLE "
								+ tableNames.get(STEP_STATUS_TABLE_KEY)
								+ "(id BIGINT CONSTRAINT STEPSTATUS_PK PRIMARY KEY NOT NULL,"
								+ "obj BLOB,"
								+ "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES "
								+ tableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " (stepexecid) ON DELETE CASCADE)");

		createDB2Strings.put(
				CREATE_DB2_CHECKPOINTDATA_INDEX_KEY,
				"CREATE INDEX CHK_INDEX ON "
						+ tableNames.get(CHECKPOINT_TABLE_KEY) + "(id)");

		return createDB2Strings;
	}


}
