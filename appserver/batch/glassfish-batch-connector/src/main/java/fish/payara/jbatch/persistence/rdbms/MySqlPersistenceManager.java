/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_PREFIX_PROPERTY;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_SUFFIX_PROPERTY;

/**
 *
 * MySQL Persistence Manager
 */

public class MySqlPersistenceManager extends JBatchJDBCPersistenceManager implements MySQLJDBCConstants{

	private static final String CLASSNAME = MySqlPersistenceManager.class
			.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;
	
	// mysql create table strings
	protected Map<String, String> createMySQLStrings;
	
	@Override
	public void init(IBatchConfig batchConfig)
			throws BatchContainerServiceException {
		logger.config("Entering CLASSNAME.init(), batchConfig =" + batchConfig);

		this.batchConfig = batchConfig;

		schema = batchConfig.getDatabaseConfigurationBean().getSchema();
		jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
                prefix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_PREFIX_PROPERTY, "");
	        suffix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_SUFFIX_PROPERTY, "");
		
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

		// Load the table names and queries shared between different database
		// types

		tableNames = getSharedTableMap();

		try {
			queryStrings = getSharedQueryMap(batchConfig);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			throw new BatchContainerServiceException(e1);
		}

		logger.config("JNDI name = " + jndiName);



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
	 * Check the schema exists and if not we will use the default schema
	 * @return
	 * @throws SQLException
	 */
        @Override
	protected boolean isSchemaValid() throws SQLException {

		logger.entering(CLASSNAME, "isMySQLSchemaValid");
		boolean result = false;
		Connection conn = null;
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;
                PreparedStatement ps = null;
		try {
			conn = getConnectionToDefaultSchema();
                        ps = conn.prepareStatement("SHOW DATABASES like ?");
                        ps.setString(1, schema);
			rs = ps.executeQuery();

			if (rs.next()) {
                            result = true;
			}
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		} finally {
			cleanupConnection(conn, rs, ps);
		}
		logger.exiting(CLASSNAME, "isMySQLSchemaValid", false);

		return result;

	}

	/**
	 * Verify the relevant JBatch tables exist.
         * 
         * @param tableNames
	 * @throws SQLException
	 */
        @Override
	protected void checkTables() throws SQLException {
		
		logger.entering(CLASSNAME, "checkMySQLTables");
                setCreateMySQLStringsMap(tableNames);

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
    protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
        Map<String, String> result = super.getSharedQueryMap(batchConfig);
        if(schema.equals("") || schema.length() == 0)
        {
        	schema = setDefaultSchema();
        }
       
        result.put(Q_SET_SCHEMA, "USE " + schema);
        
        return result;
    }

    @Override
    protected void setSchemaOnConnection(Connection connection) throws SQLException { 
        logger.log(Level.FINEST, "Entering {0}.setSchemaOnConnection()", CLASSNAME);
        try (PreparedStatement preparedStatement = connection.prepareStatement("USE " + schema)) {
            preparedStatement.executeUpdate();
        }
        logger.log(Level.FINEST, "Exiting {0}.setSchemaOnConnection()", CLASSNAME);
    }
    
    /**
	 * Method invoked to insert the MySql create table strings into a hashmap
	 **/

	private Map<String, String> setCreateMySQLStringsMap (Map<String, String> tableNames) {
		createMySQLStrings = new HashMap<>();
		
		createMySQLStrings.put(MYSQL_CREATE_TABLE_CHECKPOINTDATA, "CREATE TABLE "
				+ tableNames.get(CHECKPOINT_TABLE_KEY)
				+ " (id VARCHAR(512),obj BLOB)");
		
		createMySQLStrings.put(MYSQL_CREATE_TABLE_JOBINSTANCEDATA,"CREATE TABLE "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,name VARCHAR(512), apptag VARCHAR(512))");
		
		
		createMySQLStrings.put(MYSQL_CREATE_TABLE_EXECUTIONINSTANCEDATA,"CREATE TABLE "
						+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ "("
						+ "jobexecid BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,"
						+ "jobinstanceid BIGINT,"
						+ "createtime TIMESTAMP NULL,"
						+ "starttime TIMESTAMP NULL,"
						+ "endtime TIMESTAMP NULL,"
						+ "updatetime TIMESTAMP NULL,"
						+ "parameters BLOB,"
						+ "batchstatus VARCHAR(512),"
						+ "exitstatus VARCHAR(512),"
						+ "CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES "
						+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ "(jobinstanceid))");

		createMySQLStrings.put(MYSQL_CREATE_TABLE_STEPINSTANCEDATA,"CREATE TABLE "
						+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ "("
						+ "stepexecid BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,"
						+ "jobexecid BIGINT,"
						+ "batchstatus VARCHAR(512),"
						+ "exitstatus VARCHAR(512),"
						+ "stepname VARCHAR(512),"
						+ "readcount INT,"
						+ "writecount INT,"
						+ "commitcount INT,"
						+ "rollbackcount INT,"
						+ "readskipcount INT,"
						+ "processskipcount INT,"
						+ "filtercount INT,"
						+ "writeskipcount INT,"
						+ "startTime TIMESTAMP NULL,"
						+ "endTime TIMESTAMP NULL,"
						+ "persistentData BLOB,"
						+ "CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES "
						+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ "(jobexecid))");
		
		 


		createMySQLStrings.put(MYSQL_CREATE_TABLE_JOBSTATUS,"CREATE TABLE "
						+ tableNames.get(JOB_STATUS_TABLE_KEY)
						+ "("
						+ "id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,"
						+ "obj BLOB,"
						+ "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES "
						+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " (jobinstanceid) ON DELETE CASCADE)");

		createMySQLStrings.put(MYSQL_CREATE_TABLE_STEPSTATUS,"CREATE TABLE "
						+ tableNames.get(STEP_STATUS_TABLE_KEY)
						+ "("
						+ "id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,"
						+ "obj BLOB,"
						+ "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES "
						+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ "(stepexecid) ON DELETE CASCADE)");

		return createMySQLStrings;
	}
    

}