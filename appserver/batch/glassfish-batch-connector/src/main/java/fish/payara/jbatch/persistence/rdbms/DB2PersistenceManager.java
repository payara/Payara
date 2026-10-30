/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
 * DB2 Persistence Manager
 */
public class DB2PersistenceManager extends JBatchJDBCPersistenceManager implements DB2JDBCConstants {

	private static final String CLASSNAME = JBatchJDBCPersistenceManager.class.getName();

	private static final Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;

	// db2 create table strings
	protected Map<String, String> createDB2Strings;

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
			// TODO Auto-generated catch block
			throw new BatchContainerServiceException(e1);
		}

		logger.log(Level.CONFIG, "JNDI name = {0}", jndiName);

		if (jndiName == null || jndiName.equals("")) {
			throw new BatchContainerServiceException("JNDI name is not defined.");
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
	 * Check the schema exists
	 * @return
	 * @throws SQLException
	 */
        @Override
	protected boolean isSchemaValid() throws SQLException {

		boolean result = false;

		logger.entering(CLASSNAME, "isDB2SchemaValid");
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
		logger.exiting(CLASSNAME, "isDB2SchemaValid", false);

		return result;

	}

	/**
	 * Check the relevant db2 tables exist in the relevant schema
	 * @throws SQLException
	 */
        @Override
	protected void checkTables() throws SQLException {

		logger.entering(CLASSNAME, "checkDB2Tables");
                setCreateDB2StringsMap(tableNames);
		createTableIfNotExists(tableNames.get(CHECKPOINT_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_CHECKPOINTDATA));

		createTableIfNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_JOBINSTANCEDATA));

		createTableIfNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA));

		createTableIfNotExists(
				tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_STEPINSTANCEDATA));

		createTableIfNotExists(tableNames.get(JOB_STATUS_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_JOBSTATUS));

		createTableIfNotExists(tableNames.get(STEP_STATUS_TABLE_KEY),
				createDB2Strings.get(DB2_CREATE_TABLE_STEPSTATUS));

		logger.exiting(CLASSNAME, "checkDB2Tables");
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

					try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_READ_ONLY)) {

						String query = "select name from sysibm.systables where name ="
								+ "\'" + tableName.toUpperCase() + "\'" + "and type = 'T'";

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
         * Set the schema to the default schema or the schema defined at batch
         * configuration time
         *
         * @param connection
         * @throws SQLException
         */
        @Override
        protected void setSchemaOnConnection(Connection connection) throws SQLException {
            logger.log(Level.FINEST, "Entering {0}.setSchemaOnConnection()", CLASSNAME);

            try (PreparedStatement preparedStatement = connection.prepareStatement("SET SCHEMA ?")) {
                preparedStatement.setString(1, schema);
                preparedStatement.executeUpdate();
            } finally {
                logger.log(Level.FINEST, "Exiting {0}.setSchemaOnConnection()", CLASSNAME);
            }
        }

    @Override
        protected Map<String, String> getSharedQueryMap(IBatchConfig batchConfig) throws SQLException {
            queryStrings = super.getSharedQueryMap(batchConfig);
            
            queryStrings.put(Q_SET_SCHEMA, "SET SCHEMA ?");
            return queryStrings;
        }

	private Map<String, String> setCreateDB2StringsMap(Map<String, String> tableNames) {
		createDB2Strings = new HashMap<>();
		createDB2Strings.put(DB2_CREATE_TABLE_CHECKPOINTDATA, "CREATE TABLE "
				+ tableNames.get(CHECKPOINT_TABLE_KEY)
				+ " (id VARCHAR(512),obj BLOB)");
		createDB2Strings
				.put(DB2_CREATE_TABLE_JOBINSTANCEDATA,
						"CREATE TABLE "
								+ tableNames.get(JOB_INSTANCE_TABLE_KEY)
								+ " (jobinstanceid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBINSTANCE_PK PRIMARY KEY,"
                                                                        + "name VARCHAR(512), apptag VARCHAR(512))");

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
