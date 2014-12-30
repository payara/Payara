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
 * MySQL Persistence Manager
 */

public class MySqlPersistenceManager extends JBatchJDBCPersistenceManager {

	private static final String CLASSNAME = MySqlPersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;
	
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
		
		createMySQLStrings = setCreateMySQLStringsMap(batchConfig);

		logger.config("JNDI name = " + jndiName);

		if (jndiName == null || jndiName.equals("")) {
			throw new BatchContainerServiceException(
					"JNDI name is not defined.");
		}
		Context ctx = null;
		try {
			ctx = new InitialContext();
			dataSource = (DataSource) ctx.lookup(jndiName);

		} catch (NamingException e) {
			logger.severe("Lookup failed for JNDI name: "
					+ jndiName
					+ ".  One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
			throw new BatchContainerServiceException(e);
		}

		try {
			if (!isMySQLSchemaValid()) {
				setDefaultSchema();
			}
			checkMySQLTables();

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
	private boolean isMySQLSchemaValid() throws SQLException {

		logger.entering(CLASSNAME, "isMySQLSchemaValid");
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
					logger.exiting(CLASSNAME, "isMySQLSchemaValid", true);
					return true;
				}
			}
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		} finally {
			cleanupConnection(conn, rs, null);
		}
		logger.exiting(CLASSNAME, "isMySQLSchemaValid", false);

		return result;

	}

	/**
	 * Verify the relevant JBatch tables exist.
	 * @throws SQLException
	 */
	private void checkMySQLTables() throws SQLException {
		
		logger.entering(CLASSNAME, "checkMySQLTables");

		createMySQLTableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createMySQLStrings.get(MYSQL_CREATE_TABLE_CHECKPOINTDATA));


		createMySQLTableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createMySQLStrings.get(MYSQL_CREATE_TABLE_JOBINSTANCEDATA));
		
		createMySQLTableNotExists(
				tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createMySQLStrings.get(MYSQL_CREATE_TABLE_EXECUTIONINSTANCEDATA));
		
		createMySQLTableNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createMySQLStrings.get(MYSQL_CREATE_TABLE_STEPINSTANCEDATA));
		
		createMySQLTableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createMySQLStrings.get(MYSQL_CREATE_TABLE_JOBSTATUS));
		createMySQLTableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createMySQLStrings.get(MYSQL_CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkMySQLTables");
	}

	/**
	 * Create the jbatch tables if they do not exist.
	 * @param tableName
	 * @param createTableStatement
	 * @throws SQLException
	 */
	protected void createMySQLTableNotExists(String tableName,
			String createTableStatement) throws SQLException {
		logger.entering(CLASSNAME, "createMySQLTableNotExists",
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

		logger.exiting(CLASSNAME, "createMySQLTableNotExists");
	}


	
	@Override
    protected Map<String, String> getQueryMap(IBatchConfig batchConfig) {
        Map<String, String> result = super.getQueryMap(batchConfig);
        if (schema != null && schema.length() != 0) {
            result.put(Q_SET_SCHEMA, "USE " + schema);
        }
        return result;
    }

    @Override
    protected void setSchemaOnConnection(Connection connection) throws SQLException {
            PreparedStatement ps = null;
            ps = connection.prepareStatement(queryStrings.get(Q_SET_SCHEMA));
            ps.executeUpdate();
            ps.close();
    }
    
    //TODO create methods to check if MySQ tables exist on start up and if not create them.
    
    

}