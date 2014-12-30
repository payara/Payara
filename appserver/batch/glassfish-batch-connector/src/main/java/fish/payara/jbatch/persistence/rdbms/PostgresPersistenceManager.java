/*
 * Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.
 
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

import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.Q_SET_SCHEMA;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * 
 * PostgreSQL Persistence Manager
 */

public class PostgresPersistenceManager extends JBatchJDBCPersistenceManager {

	private static final String CLASSNAME = PostgresPersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;

	@Override
	protected Map<String, String> getQueryMap(IBatchConfig batchConfig) {
		Map<String, String> result = super.getQueryMap(batchConfig);
		if (schema != null && schema.length() != 0) {
			result.put(Q_SET_SCHEMA, "set search_path to " + schema);
		}
		return result;
	}

	@Override
	protected void setSchemaOnConnection(Connection connection)
			throws SQLException {
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

		// Load the table names and queries shared between different database
		// types

		tableNames = getTableMap(batchConfig);

		queryStrings = getQueryMap(batchConfig);

		// put the create table strings into a hashmap
		createTableStrings = setCreateTableMap(batchConfig);

		createPostgresStrings = setCreatePostgresStringsMap(batchConfig);

		logger.config("JNDI name = " + jndiName);

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

}
