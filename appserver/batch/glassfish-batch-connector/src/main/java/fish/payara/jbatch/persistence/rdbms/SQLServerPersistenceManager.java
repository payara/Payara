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

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;

import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_PREFIX_PROPERTY;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_SUFFIX_PROPERTY;

public class SQLServerPersistenceManager extends JBatchJDBCPersistenceManager implements SQLServerJDBCConstants {

	private static final String CLASSNAME = SQLServerPersistenceManager.class.getName();
	private static final Logger LOGGER = Logger.getLogger(CLASSNAME);

	// SQL Server create table strings
	protected Map<String, String> SQLServerCreateStrings;
	protected Map<String, String> schemaTableNames;

	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {

		LOGGER.entering(CLASSNAME, "init", batchConfig);

		schema = batchConfig.getDatabaseConfigurationBean().getSchema();
		jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
                prefix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_PREFIX_PROPERTY, "");
	        suffix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_SUFFIX_PROPERTY, "");

		if (null == jndiName || jndiName.isEmpty()) {
			throw new BatchContainerServiceException("JNDI name is not defined.");
		}

		Context ctx;
		try {
			ctx = new InitialContext();
			dataSource = (DataSource) ctx.lookup(jndiName);

		} catch (NamingException e) {
			LOGGER.log(Level.SEVERE,
                            "Lookup failed for JNDI name: {0}. "
                          + "One cause of this could be that the batch runtime "
                          + "is incorrectly configured to EE mode when it "
                          + "should be in SE mode.", jndiName);
			throw new BatchContainerServiceException(e);
		}

		// Load the table names and queries shared between different database types
		tableNames = getSharedTableMap();
                schemaTableNames = getSharedSchemaTableMap();

		try {
			queryStrings = getSQLServerSharedQueryMap(batchConfig);
		} catch (SQLException e1) {
			throw new BatchContainerServiceException(e1);
		}

		LOGGER.log(Level.CONFIG, "JNDI name = {0}", jndiName);

		try {
			if (!isSchemaValid()) {
				setDefaultSchema();
			}
			checkSQLServerTables();

		} catch (SQLException e) {
			LOGGER.severe(e.getLocalizedMessage());
			throw new BatchContainerServiceException(e);
		}

                LOGGER.exiting(CLASSNAME, "init");
	}

	/**
	 * Check the schema exists and if not we will use the default schema
	 * @return
	 * @throws SQLException
	 */
        @Override
	protected boolean isSchemaValid() throws SQLException {

		LOGGER.entering(CLASSNAME, "isSQLServerSchemaValid");
		boolean result = false;
		try (Connection conn = getConnectionToDefaultSchema();
			 PreparedStatement ps = conn.prepareStatement("SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE ?")) {
			ps.setString(1, schema);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					result = true;
				}
			}
		} catch (SQLException e) {
			LOGGER.severe(e.getLocalizedMessage());
			throw e;
		}
		LOGGER.exiting(CLASSNAME, "isSQLServerSchemaValid", result);
		return result;
	}

	/**
	 * Verify the relevant JBatch tables exist.
	 * @throws SQLException
	 */
	private void checkSQLServerTables() throws SQLException {

		LOGGER.entering(CLASSNAME, "checkSQLServerTables");
                setCreateSQLServerStringsMap();
		createTableIfNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				SQLServerCreateStrings.get(SQLSERVER_CREATE_TABLE_CHECKPOINTDATA));

		createTableIfNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				SQLServerCreateStrings.get(SQLSERVER_CREATE_TABLE_JOBINSTANCEDATA));

		createTableIfNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				SQLServerCreateStrings.get(SQLSERVER_CREATE_TABLE_EXECUTIONINSTANCEDATA));

		createTableIfNotExists(tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				SQLServerCreateStrings.get(SQLSERVER_CREATE_TABLE_STEPINSTANCEDATA));

		createTableIfNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				SQLServerCreateStrings.get(SQLSERVER_CREATE_TABLE_JOBSTATUS));

		createTableIfNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				SQLServerCreateStrings.get(SQLSERVER_CREATE_TABLE_STEPSTATUS));

		LOGGER.exiting(CLASSNAME, "checkSQLServerTables");
	}

        @Override
        public void createTables(DataSource dataSource, BatchRuntimeConfiguration batchRuntimeConfiguration){
			this.dataSource = dataSource;
			prefix = batchRuntimeConfiguration.getTablePrefix();
			suffix = batchRuntimeConfiguration.getTableSuffix();
			schema = batchRuntimeConfiguration.getSchemaName();
			tableNames = getSharedTableMap();
			schemaTableNames = getSharedSchemaTableMap();

			try {
				if (!isSchemaValid()) {
					setDefaultSchema();
				}
				checkSQLServerTables();
			} catch (SQLException ex) {
				LOGGER.severe(ex.getLocalizedMessage());
			}
         }
	/**
	 * Create the jbatch tables if they do not exist.
	 * @param tableName
	 * @param createTableStatement
	 * @throws SQLException
	 */
	protected void createSQLServerTableIfNotExist(String tableName,
			String createTableStatement) throws SQLException {

		LOGGER.entering(CLASSNAME, "createSQLServerTableIfNotExists",
				new Object[] { tableName, createTableStatement });


		try (Connection conn = getConnection();
			 PreparedStatement ps = conn.prepareStatement(
                          "SELECT table_schema, table_name FROM information_schema.tables "
                                + "WHERE table_schema LIKE ? AND table_name LIKE ?",
                          ResultSet.TYPE_SCROLL_INSENSITIVE,
                          ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, schema);
			ps.setString(2, tableName);

			try(ResultSet rs = ps.executeQuery()) {
				int rowcount = getTableRowCount(rs);
				// Create table if it does not exist
				if (rowcount == 0 && !rs.next()) {
						LOGGER.log(Level.INFO, "{0} table does not exists. Trying to create it.", tableName);
						try(PreparedStatement psCt = conn.prepareStatement(createTableStatement)) {
							psCt.executeUpdate();
						}
				}
			}

		} catch (SQLException e) {
			LOGGER.severe(e.getLocalizedMessage());
			throw e;
		}

		LOGGER.exiting(CLASSNAME, "createSQLServerTableIfNotExists");
	}

        @Override
        public boolean checkIfTableExists(DataSource dSource, String tableName, String schemaName) {
                dataSource = dSource;

                boolean result = true;

                try (Connection connection = dSource.getConnection()) {
                    schema = schemaName;

                    if (!isSchemaValid()) {
                        setDefaultSchema();
                    }

					try(PreparedStatement preparedStatement = connection.prepareStatement(
                          "SELECT table_schema, table_name FROM information_schema.tables "
                                + "WHERE table_schema LIKE ? AND table_name LIKE ?",
                          ResultSet.TYPE_SCROLL_INSENSITIVE,
                          ResultSet.CONCUR_READ_ONLY)) {

						preparedStatement.setString(1, schema);
						preparedStatement.setString(2, tableName);
						try(ResultSet resultSet = preparedStatement.executeQuery()) {
							int rowcount = getTableRowCount(resultSet);

							if (rowcount == 0 && !resultSet.next()) {
									result = false;
							}
						}

					}

                } catch (SQLException ex) {
                    LOGGER.severe(ex.getLocalizedMessage());
                }

                return result;
        }
	protected Map<String, String> getSharedSchemaTableMap() {
                String schemaPrefix;
                if(schema == null || schema.isEmpty()) {
                    schemaPrefix = "";
                } else {
                    schemaPrefix = schema + ".";
                }

		Map<String, String> result = new HashMap<>(6);
		result.put(JOB_INSTANCE_TABLE_KEY, schemaPrefix + prefix
                                + "JOBINSTANCEDATA" + suffix);
		result.put(EXECUTION_INSTANCE_TABLE_KEY, schemaPrefix + prefix
				+ "EXECUTIONINSTANCEDATA" + suffix);
		result.put(STEP_EXECUTION_INSTANCE_TABLE_KEY, schemaPrefix + prefix
				+ "STEPEXECUTIONINSTANCEDATA" + suffix);
		result.put(JOB_STATUS_TABLE_KEY, schemaPrefix + prefix
                                + "JOBSTATUS" + suffix);
		result.put(STEP_STATUS_TABLE_KEY, schemaPrefix + prefix
                                + "STEPSTATUS" + suffix);
		result.put(CHECKPOINT_TABLE_KEY, schemaPrefix + prefix
                                + "CHECKPOINTDATA" + suffix);
		return result;
	}

        @Override
        protected void setSchemaOnConnection(Connection connection){
            // SQL Server does not support setting default schema for session
        }
        
    @Override
    protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
            queryStrings = super.getSharedQueryMap(batchConfig);
            
            queryStrings.put(Q_SET_SCHEMA, "SET SCHEMA ?");
            return queryStrings;
    }

    /**
     * Method invoked to insert the MySql create table strings into a hashmap
     * @return SQLServerCreateStrings
     **/
	private Map<String, String> setCreateSQLServerStringsMap () {

		SQLServerCreateStrings = new HashMap<>();

		SQLServerCreateStrings.put(SQLSERVER_CREATE_TABLE_CHECKPOINTDATA, "CREATE TABLE "
                                                + schemaTableNames.get(CHECKPOINT_TABLE_KEY)
                                                + "("
                                                + "id VARCHAR(512),"
                                                + "obj VARBINARY(MAX))");

		SQLServerCreateStrings.put(SQLSERVER_CREATE_TABLE_JOBINSTANCEDATA,"CREATE TABLE "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ "("
                                                + "jobinstanceid BIGINT NOT NULL PRIMARY KEY IDENTITY(1,1),"
                                                + "name VARCHAR(512),"
                                                + "apptag VARCHAR(512))");

		SQLServerCreateStrings.put(SQLSERVER_CREATE_TABLE_EXECUTIONINSTANCEDATA,"CREATE TABLE "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ "("
						+ "jobexecid BIGINT NOT NULL PRIMARY KEY IDENTITY(1,1),"
						+ "jobinstanceid BIGINT,"
						+ "createtime DATETIME,"
						+ "starttime DATETIME,"
						+ "endtime DATETIME,"
						+ "updatetime DATETIME,"
						+ "parameters VARBINARY(MAX),"
						+ "batchstatus VARCHAR(512),"
						+ "exitstatus VARCHAR(512),"
						+ "CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ "(jobinstanceid))");

		SQLServerCreateStrings.put(SQLSERVER_CREATE_TABLE_STEPINSTANCEDATA,"CREATE TABLE "
						+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ "("
						+ "stepexecid BIGINT NOT NULL PRIMARY KEY IDENTITY(1,1),"
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
						+ "startTime DATETIME,"
						+ "endTime DATETIME,"
						+ "persistentData VARBINARY(MAX),"
						+ "CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ "(jobexecid))");

		SQLServerCreateStrings.put(SQLSERVER_CREATE_TABLE_JOBSTATUS,"CREATE TABLE "
						+ schemaTableNames.get(JOB_STATUS_TABLE_KEY)
						+ "("
						+ "id BIGINT NOT NULL PRIMARY KEY,"
						+ "obj VARBINARY(MAX),"
						+ "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " (jobinstanceid) ON DELETE CASCADE)");

		SQLServerCreateStrings.put(SQLSERVER_CREATE_TABLE_STEPSTATUS,"CREATE TABLE "
						+ schemaTableNames.get(STEP_STATUS_TABLE_KEY)
						+ "("
						+ "id BIGINT NOT NULL PRIMARY KEY,"
						+ "obj VARBINARY(MAX),"
						+ "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES "
						+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ "(stepexecid) ON DELETE CASCADE)");

		return SQLServerCreateStrings;
	}

        protected Map<String, String> getSQLServerSharedQueryMap(IBatchConfig batchConfig) throws SQLException {

                String schemaPrefix = batchConfig.getDatabaseConfigurationBean().getSchema();
                if(schemaPrefix != null && !schemaPrefix.isEmpty()) {
                    schemaPrefix += ".";
                }
		queryStrings = new HashMap<>();
		queryStrings.put(Q_SET_SCHEMA, "SET SCHEMA ?");
		queryStrings.put(SELECT_CHECKPOINTDATA, "select id, obj from "
				+ schemaTableNames.get(CHECKPOINT_TABLE_KEY) + " where id = ?");
		queryStrings.put(INSERT_CHECKPOINTDATA,
				"insert into " + schemaTableNames.get(CHECKPOINT_TABLE_KEY)
						+ " values(?, ?)");
		queryStrings.put(UPDATE_CHECKPOINTDATA,
				"update " + schemaTableNames.get(CHECKPOINT_TABLE_KEY)
						+ " set obj = ? where id = ?");
		queryStrings.put(JOBOPERATOR_GET_JOB_INSTANCE_COUNT,
				"select count(jobinstanceid) as jobinstancecount from "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " where name = ? and apptag = ?");
		queryStrings.put(SELECT_JOBINSTANCEDATA_COUNT,
				"select count(jobinstanceid) as jobinstancecount from "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " where name = ?");
		queryStrings
				.put(JOBOPERATOR_GET_JOB_INSTANCE_IDS,
						"select jobinstanceid from "
								+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " where name = ? and apptag = ? order by jobinstanceid desc");
		queryStrings.put(
				SELECT_JOBINSTANCEDATA_IDS,
				"select jobinstanceid from "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " where name = ? order by jobinstanceid desc");
		queryStrings.put(
				JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA,
				"select distinct jobinstanceid, name from "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " where name ");
		queryStrings.put(JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP,
				"select createtime, endtime, updatetime, starttime from "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " where jobexecid = ?");
		queryStrings.put(
				JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS,
				"select batchstatus from "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " where jobexecid = ?");
		queryStrings.put(
				JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS,
				"select exitstatus from "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " where jobexecid = ?");
		queryStrings.put(
				JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID,
				"select jobinstanceid from "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " where jobexecid = ?");
		queryStrings.put(
				GET_PARAMETERS,
				"select parameters from "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " where jobexecid = ?");
		queryStrings
				.put(MOST_RECENT_STEPS_FOR_JOB,
						"select A.* from "
								+ schemaTableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " as A inner join "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " as B on A.jobexecid = B.jobexecid where B.jobinstanceid = ? order by A.stepexecid desc");
		queryStrings.put(STEP_EXECUTIONS_FOR_JOB_EXECUTION, "select * from "
				+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
				+ " where jobexecid = ?");
		queryStrings.put(STEP_EXECUTIONS_BY_STEP_ID, "select * from "
				+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
				+ " where stepexecid = ?");
		queryStrings
				.put(UPDATE_BATCH_STATUS_ONLY,
						"update "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " set batchstatus = ?, updatetime = ? where jobexecid = ?");
		queryStrings
				.put(UPDATE_FINAL_STATUS_AND_TIMESTAMP,
						"update "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " set batchstatus = ?, exitstatus = ?, endtime = ?, updatetime = ? where jobexecid = ?");
		queryStrings
				.put(MARK_JOB_STARTED,
						"update "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " set batchstatus = ?, starttime = ?, updatetime = ? where jobexecid = ?");
		queryStrings
				.put(JOB_OPERATOR_GET_JOB_EXECUTION,
						"select A.jobexecid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.jobinstanceid, A.batchstatus, A.exitstatus, B.name from "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " as A inner join "
								+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " as B on A.jobinstanceid = B.jobinstanceid where jobexecid = ?");
		queryStrings
				.put(JOB_OPERATOR_GET_JOB_EXECUTIONS,
						"select A.jobexecid, A.jobinstanceid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.batchstatus, A.exitstatus, B.name from "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " as A inner join "
								+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " as B ON A.jobinstanceid = B.jobinstanceid where A.jobinstanceid = ?");
		queryStrings
				.put(JOB_OPERATOR_GET_RUNNING_EXECUTIONS,
						"SELECT A.jobexecid FROM "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " A INNER JOIN "
								+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " B ON A.jobinstanceid = B.jobinstanceid WHERE A.batchstatus IN (?,?,?) AND B.name = ?");
		queryStrings.put(SELECT_JOBINSTANCEDATA_APPTAG, "select apptag from "
				+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
				+ " where jobinstanceid = ?");
		queryStrings.put(DELETE_JOBS,
				"DELETE FROM " + schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " WHERE apptag = ?");
		String deleteJobExecutions = "DELETE FROM "
				+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY) + " "
				+ "WHERE jobexecid IN (" + "SELECT B.jobexecid FROM "
				+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY) + " A INNER JOIN "
				+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY) + " B "
				+ "ON A.jobinstanceid = B.jobinstanceid "
				+ "WHERE A.apptag = ?)";
		queryStrings.put(DELETE_JOB_EXECUTIONS, deleteJobExecutions);
		String deleteStepExecutions = "DELETE FROM "
				+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY) + " "
				+ "WHERE stepexecid IN (" + "SELECT C.stepexecid FROM "
				+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY) + " A INNER JOIN "
				+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY) + " B "
				+ "ON A.jobinstanceid = B.jobinstanceid INNER JOIN "
				+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY) + " C "
				+ "ON B.jobexecid = C.jobexecid " + "WHERE A.apptag = ?)";
		queryStrings.put(DELETE_STEP_EXECUTIONS, deleteStepExecutions);
		queryStrings.put(GET_JOB_STATUS_FROM_EXECUTIONS, "select A.obj from "
				+ schemaTableNames.get(JOB_STATUS_TABLE_KEY) + " as A inner join "
				+ "" + schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
				+ " as B on A.id = B.jobinstanceid where B.jobexecid = ?");
		queryStrings.put(
				JOB_INSTANCE_ID_BY_EXECUTION_ID,
				"select jobinstanceid from "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " where jobexecid = ?");
		queryStrings.put(CREATE_SUB_JOB_INSTANCE,
				"INSERT INTO " + schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " (name, apptag) VALUES(?, ?)");
		queryStrings.put(CREATE_JOB_INSTANCE,
				"INSERT INTO " + schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " (name, apptag) VALUES(?, ?)");
		queryStrings
				.put(CREATE_JOB_EXECUTION_ENTRY,
						"INSERT INTO "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid, createtime, updatetime, batchstatus, parameters) VALUES(?, ?, ?, ?, ?)");
		queryStrings
				.put(CREATE_STEP_EXECUTION,
						"INSERT INTO "
								+ schemaTableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " (jobexecid, batchstatus, exitstatus, stepname, readcount,"
								+ "writecount, commitcount, rollbackcount, readskipcount, processskipcount, filtercount, writeskipcount, starttime,"
								+ "endtime, persistentdata) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		queryStrings
				.put(UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION,
						"select SUM(STEPEX.readcount) readcount, SUM(STEPEX.writecount) writecount, SUM(STEPEX.commitcount) commitcount,  SUM(STEPEX.rollbackcount) rollbackcount,"
								+ " SUM(STEPEX.readskipcount) readskipcount, SUM(STEPEX.processskipcount) processskipcount, SUM(STEPEX.filtercount) filtercount, SUM(STEPEX.writeSkipCount) writeSkipCount"
								+ " from "
								+ schemaTableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " STEPEX inner join "
								+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " JOBEX"
								+ " on STEPEX.jobexecid = JOBEX.jobexecid"
								+ " where JOBEX.jobinstanceid IN"
								+ " (select jobinstanceid from "
								+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " where name like ?)");
		queryStrings
				.put(UPDATE_STEP_EXECUTION_WITH_METRICS,
						"UPDATE "
								+ schemaTableNames
										.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " SET batchstatus = ?, exitstatus = ?, stepname = ?,  readcount = ?,"
								+ "writecount = ?, commitcount = ?, rollbackcount = ?, readskipcount = ?, processskipcount = ?, filtercount = ?, writeskipcount = ?,"
								+ " starttime = ?, endtime = ?, persistentdata = ? WHERE stepexecid = ?");
		queryStrings.put(CREATE_JOBSTATUS,
				"INSERT INTO " + schemaTableNames.get(JOB_STATUS_TABLE_KEY)
						+ " (id, obj) VALUES(?, ?)");
		queryStrings.put(GET_JOB_STATUS,
				"SELECT obj FROM " + schemaTableNames.get(JOB_STATUS_TABLE_KEY)
						+ " WHERE id = ?");
		queryStrings.put(UPDATE_JOBSTATUS,
				"UPDATE " + schemaTableNames.get(JOB_STATUS_TABLE_KEY)
						+ " SET obj = ? WHERE id = ?");
		queryStrings.put(CREATE_STEP_STATUS,
				"INSERT INTO " + schemaTableNames.get(STEP_STATUS_TABLE_KEY)
						+ " (id, obj) VALUES(?, ?)");
		queryStrings.put(
				GET_STEP_STATUS,
				"SELECT obj FROM " + schemaTableNames.get(STEP_STATUS_TABLE_KEY)
						+ " WHERE id IN (" + "SELECT B.stepexecid FROM "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " A INNER JOIN "
						+ schemaTableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
						+ " B ON A.jobexecid = B.jobexecid "
						+ "WHERE A.jobinstanceid = ? and B.stepname = ?)");
		queryStrings.put(UPDATE_STEP_STATUS,
				"UPDATE " + schemaTableNames.get(STEP_STATUS_TABLE_KEY)
						+ " SET obj = ? WHERE id = ?");
		queryStrings.put(
				GET_TAGNAME,
				"SELECT A.apptag FROM "
						+ schemaTableNames.get(JOB_INSTANCE_TABLE_KEY)
						+ " A INNER JOIN "
						+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
						+ " B ON A.jobinstanceid = B.jobinstanceid"
						+ " WHERE B.jobexecid = ?");
		queryStrings.put(GET_MOST_RECENT_EXECUTION_ID, "SELECT jobexecid FROM "
				+ schemaTableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
				+ " WHERE jobinstanceid = ? ORDER BY createtime DESC");
		return queryStrings;
	}
}
