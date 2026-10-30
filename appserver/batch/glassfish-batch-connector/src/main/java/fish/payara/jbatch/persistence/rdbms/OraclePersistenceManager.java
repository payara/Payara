/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.CHECKPOINT_TABLE_KEY;
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.EXECUTION_INSTANCE_TABLE_KEY;
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.JOB_INSTANCE_TABLE_KEY;
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.JOB_STATUS_TABLE_KEY;
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.STEP_EXECUTION_INSTANCE_TABLE_KEY;
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.STEP_STATUS_TABLE_KEY;

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
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_PREFIX_PROPERTY;
import static org.glassfish.batch.spi.impl.BatchRuntimeHelper.PAYARA_TABLE_SUFFIX_PROPERTY;

/**
 *
 * Oracle Persistence Manager
 */

public class OraclePersistenceManager extends JBatchJDBCPersistenceManager implements OracleJDBCConstants{

	private static final String CLASSNAME = OraclePersistenceManager.class
			.getName();

	private static final Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;

	// oracle create table strings
	protected Map<String, String> createOracleTableStrings;
	protected Map<String, String> createOracleIndexStrings;

        protected Map<String, String> oracleObjectNames;

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
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		logger.log(Level.CONFIG, "Entering CLASSNAME.init(), batchConfig ={0}", batchConfig);

		this.batchConfig = batchConfig;

                schema = batchConfig.getDatabaseConfigurationBean().getSchema();
                jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
                prefix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_PREFIX_PROPERTY, "");
                suffix = batchConfig.getConfigProperties().getProperty(PAYARA_TABLE_SUFFIX_PROPERTY, "");


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

		tableNames = getSharedTableMap();
                oracleObjectNames = getOracleObjectsMap();

		try {
			queryStrings = getSharedQueryMap(batchConfig);
		} catch (SQLException e1) {
			throw new BatchContainerServiceException(e1);
		}

		logger.log(Level.CONFIG, "JNDI name = {0}", jndiName);


		try {
			if (!isSchemaValid()) {
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
        @Override
	protected boolean isSchemaValid() throws SQLException {

		logger.entering(CLASSNAME, "isOracleSchemaValid");
		boolean result = false;

		try (Connection conn = getConnectionToDefaultSchema()) {
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
		logger.exiting(CLASSNAME, "isOracleSchemaValid", false);

		return result;

	}

	/**
	 * Verify the relevant JBatch tables exist.
	 * @throws SQLException
	 */
	private void checkOracleTables() throws SQLException {
		 setOracleTableMap();
                 setOracleIndexMap();
		logger.entering(CLASSNAME, "checkOracleTables");

		createOracleTableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_CHECKPOINTDATA));

                // do same check for indexes, triggers and sequences
                // for triggers and indexes, also check for old names that did not include a prefix/suffix for backward compatibility
                if (!checkOracleIndexExists(oracleObjectNames.get(CREATE_CHECKPOINTDATA_INDEX_KEY),
                                            CREATE_CHECKPOINTDATA_INDEX_KEY,
                                            tableNames.get(CHECKPOINT_TABLE_KEY))) {
                    executeStatement(createOracleIndexStrings
				.get(CREATE_CHECKPOINTDATA_INDEX));
                }

		createOracleTableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_JOBINSTANCEDATA));
                createTableIfNotExists(oracleObjectNames.get(JOBINSTANCEDATA_SEQ_KEY),
                                createOracleTableStrings.get(CREATE_JOBINSTANCEDATA_SEQ));
                createOracleTriggerNotExists(oracleObjectNames.get(JOBINSTANCEDATA_TRG_KEY),
                                createOracleTableStrings.get(CREATE_JOBINSTANCEDATA_TRG),
                                DEFAULT_JOBINSTANCEDATA_TRG_KEY,
                                tableNames.get(JOB_INSTANCE_TABLE_KEY));


		createOracleTableNotExists(
				tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_EXECUTIONINSTANCEDATA));
                createTableIfNotExists(oracleObjectNames.get(EXECUTIONINSTANCEDATA_SEQ_KEY),
                                createOracleTableStrings.get(CREATE_EXECUTIONINSTANCEDATA_SEQ));
                createOracleTriggerNotExists(oracleObjectNames.get(EXECUTIONINSTANCEDATA_TRG_KEY),
                                createOracleTableStrings.get(CREATE_EXECUTIONINSTANCEDATA_TRG),
                                DEFAULT_EXECUTIONINSTANCEDATA_TRG_KEY,
                                tableNames.get(EXECUTION_INSTANCE_TABLE_KEY));


		createOracleTableNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_STEPINSTANCEDATA));
                createTableIfNotExists(oracleObjectNames.get(STEPINSTANCEDATA_SEQ_KEY),
                                createOracleTableStrings.get(CREATE_STEPINSTANCEDATA_SEQ));
                createOracleTriggerNotExists(oracleObjectNames.get(STEPINSTANCEDATA_TRG_KEY),
                                createOracleTableStrings.get(CREATE_STEPINSTANCEDATA_TRG),
                                DEFAULT_STEPINSTANCEDATA_TRG_KEY,
                                tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY));

		createOracleTableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_JOBSTATUS));
		createOracleTableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createOracleTableStrings.get(CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkOracleTables");
	}

        @Override
        public void createTables(DataSource dataSource, BatchRuntimeConfiguration batchRuntimeConfiguration){
            this.dataSource = dataSource;
            prefix = batchRuntimeConfiguration.getTablePrefix();
            suffix = batchRuntimeConfiguration.getTableSuffix();
            schema = batchRuntimeConfiguration.getSchemaName();
            tableNames = getSharedTableMap();
            oracleObjectNames = getOracleObjectsMap();

            try {
                if (!isSchemaValid()) {
                    setDefaultSchema();
                }
                checkOracleTables();
            } catch (SQLException ex) {
                logger.severe(ex.getLocalizedMessage());
            }
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

		try (Connection conn = getConnection();
			 Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
								ResultSet.CONCUR_READ_ONLY)) {
			String query = "SELECT lower(owner),lower(table_name) FROM all_tables where lower(owner) =  "
					+ "\'"
					+ schema.toLowerCase()
					+ "\'"
					+ " and lower(table_name)= "
					+ "\'"
					+ tableName.toLowerCase() + "\'";

			try (ResultSet rs = stmt.executeQuery(query)) {
				int rowcount = getTableRowCount(rs);
				// Create table if it does not exist
				if (rowcount == 0 && !rs.next()) {
						logger.log(Level.INFO, "{0} table does not exists. Trying to create it.", tableName);
						try (PreparedStatement ps = conn.prepareStatement(createTableStatement)) {
							ps.executeUpdate();
						}
				}
			}

		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw e;
		}

		logger.exiting(CLASSNAME, "createOracleTableNotExists");
	}

        @Override
        public boolean checkIfTableExists(DataSource dSource, String tableName, String schemaName) {
                dataSource = dSource;

                boolean result = false;

                try (Connection connection = dataSource.getConnection()) {
                    schema = schemaName;

                    if (!isSchemaValid()) {
                        setDefaultSchema();
                    }

                    String query = "select lower(sequence_name) from user_sequences where lower(sequence_name)="
                            + "\'" + tableName.toLowerCase() + "\'";

					try (Statement statement = connection.createStatement();
						 ResultSet resultSet = statement.executeQuery(query)) {
						while (resultSet.next()) {
							result = true;
							break;
						}
					}
                } catch (SQLException ex) {
                    logger.severe(ex.getLocalizedMessage());
                }

                return result;
        }
    /**
     * Create the relevant jbatch triggers as required
     * @param triggername
     * @param trgstmt
     * @throws SQLException
     */
	public void createOracleTriggerNotExists(String triggername, String trgstmt,
                    String defaultTriggername, String tablename) throws SQLException {

		logger.entering(CLASSNAME, "createOracleTableNotExists", new Object[] {
				triggername, trgstmt, defaultTriggername, tablename });
		boolean triggerexists = false;
		try (Connection conn = getConnection();
			 Statement stmt = conn.createStatement()) {

			String query = "select lower(trigger_name) from user_triggers "
						 + " where lower(table_owner)=\'" + schema.toLowerCase()+ "\'"
						 +   " and lower(trigger_name) in ("
								 + "\'" + triggername.toLowerCase() + "\',"
								 + "\'" + defaultTriggername.toLowerCase() + "\')"
						+    " and lower(table_name)=\'" + tablename.toLowerCase() + "\'";
			try (ResultSet results = stmt.executeQuery(query)) {
				triggerexists = results.next();
			}

			if (!triggerexists) {
				// create the trigger
				try (Statement ps = conn.createStatement()) {
					ps.executeUpdate(trgstmt);
				}
			}
		} catch (SQLException e) {
			throw e;
		}
		logger.exiting(CLASSNAME, "createOracleTriggerNotExists");

	}
    /**
     * Check indexes exist for the jbatch tables
     * @param indexname
     * @return
     * @throws SQLException
     */
	public boolean checkOracleIndexExists(String indexname, String defaultIndexname, String tablename) throws SQLException {
		logger.entering(CLASSNAME, "createOracleIndexNotExists",new Object[] {
				indexname, defaultIndexname, tablename });
		boolean indexexists = false;
		try (Connection conn = getConnection();
			 Statement stmt = conn.createStatement()) {
			String query = "select lower(index_name) from user_indexes"
						 + " where lower(table_owner)=\'" + schema.toLowerCase()+ "\'"
						 +   " and lower(index_name) in ("
								 + "\'" + indexname.toLowerCase() + "\',"
								 + "\'" + defaultIndexname.toLowerCase() + "\')"
						 +   " and lower(table_name)=\'" + tablename.toLowerCase() + "\'";

			try (ResultSet results = stmt.executeQuery(query)) {
				indexexists = results.next();
			}
		} catch (SQLException e) {
			throw e;
		}
		logger.exiting(CLASSNAME, "createOracleIndexNotExists");
		return indexexists;
	}

	/**
	 * Method invoked to insert the Oracle create table, trigger and sequence
	 * strings into a hashmap
	 **/

	private Map<String, String> setOracleTableMap() {
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
				"CREATE SEQUENCE " + oracleObjectNames.get(JOBINSTANCEDATA_SEQ_KEY));
		createOracleTableStrings
				.put(CREATE_JOBINSTANCEDATA_TRG,
						"CREATE OR REPLACE TRIGGER " + oracleObjectNames.get(JOBINSTANCEDATA_TRG_KEY)
                                                                + " BEFORE INSERT ON "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " FOR EACH ROW BEGIN SELECT "
                                                                + oracleObjectNames.get(JOBINSTANCEDATA_SEQ_KEY)
                                                                + ".nextval INTO :new.jobinstanceid FROM dual; END;");

		createOracleTableStrings
				.put(CREATE_TABLE_EXECUTIONINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ "(jobexecid NUMBER(19,0) PRIMARY KEY, jobinstanceid NUMBER(19,0), "
                                                                + "createtime TIMESTAMP, starttime TIMESTAMP, endtime TIMESTAMP, updatetime TIMESTAMP, "
                                                                + "parameters BLOB, batchstatus VARCHAR2(512), exitstatus VARCHAR2(512),"
                                                                + "CONSTRAINT " + prefix + "JOBINST_JOBEXEC_FK" + suffix + " FOREIGN KEY (jobinstanceid) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ "(jobinstanceid))");
		createOracleTableStrings.put(CREATE_EXECUTIONINSTANCEDATA_SEQ,
                                "CREATE SEQUENCE " + oracleObjectNames.get(EXECUTIONINSTANCEDATA_SEQ_KEY));
		createOracleTableStrings
				.put(CREATE_EXECUTIONINSTANCEDATA_TRG,
						"CREATE OR REPLACE TRIGGER " + oracleObjectNames.get(EXECUTIONINSTANCEDATA_TRG_KEY)
                                                                + " BEFORE INSERT ON "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ " FOR EACH ROW BEGIN SELECT "
                                                                + oracleObjectNames.get(EXECUTIONINSTANCEDATA_SEQ_KEY)
                                                                + ".nextval INTO :new.jobexecid FROM dual;END;");

		createOracleTableStrings
				.put(CREATE_TABLE_STEPINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ "(stepexecid NUMBER(19,0) PRIMARY KEY, jobexecid NUMBER(19,0),"
                                                                + "batchstatus VARCHAR2(512), exitstatus VARCHAR2(512), stepname VARCHAR(512), "
                                                                + "readcount NUMBER(11, 0), writecount NUMBER(11, 0), commitcount NUMBER(11, 0),rollbackcount NUMBER(11, 0), "
                                                                + "readskipcount NUMBER(11, 0), processskipcount NUMBER(11, 0), filtercount NUMBER(11, 0), writeskipcount NUMBER(11, 0), "
                                                                + "startTime TIMESTAMP, endTime TIMESTAMP, persistentData BLOB, "
                                                                + "CONSTRAINT " + prefix + "JOBEXEC_STEPEXEC_FK" + suffix + " FOREIGN KEY (jobexecid) REFERENCES "
								+ tableNames.get(EXECUTION_INSTANCE_TABLE_KEY)
								+ "(jobexecid))");
		createOracleTableStrings.put(CREATE_STEPINSTANCEDATA_SEQ,
                                "CREATE SEQUENCE " + oracleObjectNames.get(STEPINSTANCEDATA_SEQ_KEY));
		createOracleTableStrings
				.put(CREATE_STEPINSTANCEDATA_TRG,
						"CREATE OR REPLACE TRIGGER " + oracleObjectNames.get(STEPINSTANCEDATA_TRG_KEY)
                                                                + " BEFORE INSERT ON "
								+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " FOR EACH ROW BEGIN SELECT "
                                                                + oracleObjectNames.get(STEPINSTANCEDATA_SEQ_KEY)
                                                                + ".nextval INTO :new.stepexecid FROM dual;END;");

		createOracleTableStrings
				.put(CREATE_TABLE_JOBSTATUS,
						"CREATE TABLE "
								+ tableNames.get(JOB_STATUS_TABLE_KEY)
								+ "(id NUMBER(19,0) PRIMARY KEY,"
								+ "obj BLOB,"
								+ "CONSTRAINT " + prefix + "JOBSTATUS_JOBINST_FK" + suffix + " FOREIGN KEY (id) REFERENCES "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid) ON DELETE CASCADE)");

		createOracleTableStrings
				.put(CREATE_TABLE_STEPSTATUS,
						"CREATE TABLE "
								+ tableNames.get(STEP_STATUS_TABLE_KEY)
								+ "(id NUMBER(19,0) PRIMARY KEY,"
								+ "obj BLOB,"
								+ "CONSTRAINT " + prefix + "STEPSTATUS_STEPEXEC_FK" + suffix + " FOREIGN KEY (id) REFERENCES "
								+ tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY)
								+ " (stepexecid) ON DELETE CASCADE)");
		return createOracleTableStrings;
	}

	/**
	 * Method invoked to insert the Oracle create index strings into a hashmap
	 **/

	private Map<String, String> setOracleIndexMap() {
		createOracleIndexStrings = new HashMap<>();
		createOracleIndexStrings.put(
				CREATE_CHECKPOINTDATA_INDEX,
				"create index " + oracleObjectNames.get(CREATE_CHECKPOINTDATA_INDEX_KEY)
						+ " on " + tableNames.get(CHECKPOINT_TABLE_KEY) + "(id)");
		return createOracleIndexStrings;
	}

        protected Map<String, String> getOracleObjectsMap() {
		Map<String, String> result = new HashMap<>(7);

		result.put(JOBINSTANCEDATA_SEQ_KEY, prefix + JOBINSTANCEDATA_SEQ_KEY + suffix);
		result.put(EXECUTIONINSTANCEDATA_SEQ_KEY, prefix + EXECUTIONINSTANCEDATA_SEQ_KEY + suffix);
		result.put(STEPINSTANCEDATA_SEQ_KEY, prefix + STEPINSTANCEDATA_SEQ_KEY + suffix);

                result.put(JOBINSTANCEDATA_TRG_KEY, prefix + JOBINSTANCEDATA_TRG_KEY + suffix);
		result.put(EXECUTIONINSTANCEDATA_TRG_KEY, prefix + EXECUTIONINSTANCEDATA_TRG_KEY + suffix);
		result.put(STEPINSTANCEDATA_TRG_KEY, prefix + STEPINSTANCEDATA_TRG_KEY + suffix);

                result.put(CREATE_CHECKPOINTDATA_INDEX_KEY, prefix + CREATE_CHECKPOINTDATA_INDEX_KEY + suffix);

		return result;
	}

}
