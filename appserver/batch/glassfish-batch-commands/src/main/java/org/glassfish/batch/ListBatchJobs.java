/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package org.glassfish.batch;

import com.ibm.jbatch.spi.TaggedJobExecution;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.jbatch.persistence.rdbms.DB2PersistenceManager;
import fish.payara.jbatch.persistence.rdbms.JBatchJDBCPersistenceManager;
import fish.payara.jbatch.persistence.rdbms.MySqlPersistenceManager;
import fish.payara.jbatch.persistence.rdbms.OraclePersistenceManager;
import fish.payara.jbatch.persistence.rdbms.PostgresPersistenceManager;
import fish.payara.jbatch.persistence.rdbms.SQLServerPersistenceManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import jakarta.batch.operations.*;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import jakarta.validation.constraints.Min;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import static org.glassfish.batch.BatchConstants.LIST_BATCH_JOBS;
import static org.glassfish.batch.BatchConstants.LIST_JOBS_COUNT;
import static org.glassfish.batch.BatchConstants.SIMPLE_MODE;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
@Service(name = "_ListBatchJobs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("_ListBatchJobs")
@ExecuteOn(value = {RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "_ListBatchJobs",
            description = "_List Batch Jobs")
})
public class ListBatchJobs extends AbstractLongListCommand {


    private static final String JOB_NAME = "jobName";

    private static final String APP_NAME = "appName";

    private static final String INSTANCE_COUNT = "instanceCount";

    private static final String INSTANCE_ID = "instanceId";

    private static final String EXECUTION_ID = "executionId";

    private static final String BATCH_STATUS = "batchStatus";

    private static final String EXIT_STATUS = "exitStatus";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    @Param(primary = true, optional = true)
    String jobName;
    
    @Min(value = 0, message = "Offset value needs to be greater than 0")
    @Param(name = "offset", optional = true, defaultValue = "0")
    String offSetValue;

    @Min(value = 0, message = "Limit value needs to be greater than 0")
    @Param(name = "limit", optional = true, defaultValue = "2000")
    String limitValue;

    @Inject
    BatchRuntimeHelper batchRuntimeHelper;

    @Inject
    BatchRuntimeConfiguration batchRuntimeConfiguration;

    private DataSource dataSource;
    private String jobInstanceTableKey;
    private String queryToGetUniqueJobNames;
    
    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps)
            throws Exception {

        String dataSourceName = batchRuntimeHelper.getDataSourceLookupName();
        InitialContext ctx = new InitialContext();
        Object object = ctx.lookup(dataSourceName);
        //check whether the referenced JNDI entry is a DataSource
        if (object instanceof DataSource) {
            dataSource = DataSource.class.cast(object);
            String prefix = batchRuntimeConfiguration.getTablePrefix();
            String suffix = batchRuntimeConfiguration.getTableSuffix();
            createTables();
            jobInstanceTableKey = prefix + "JOBINSTANCEDATA" + suffix;
            if (checkIfTableExists(jobInstanceTableKey)) {
                queryToGetUniqueJobNames = "SELECT DISTINCT name FROM " + jobInstanceTableKey;

                ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());

                if (isSimpleMode()) {
                    setToSimpleMode(extraProps, columnFormatter);
                } else {
                    setToLongMode(extraProps, columnFormatter);
                }

                context.getActionReport().setMessage(columnFormatter.toString());
            } else {
                context.getActionReport().failure(logger, jobInstanceTableKey + " table doesn't exists");
            }
        }
    }

    private void setToSimpleMode(Properties extraProps, ColumnFormatter columnFormatter) {
        extraProps.put(SIMPLE_MODE, true);
        Map<String, Integer> jobsInstanceCount = new HashMap<>();

        if (jobName != null) {
            jobsInstanceCount.put(jobName, getJobInstanceCount(jobName));
        } else {
            // Get names of all available Jobs
            List<String> jobNames = executeQuery(queryToGetUniqueJobNames, "name");

            for (String job_Name : jobNames) {
                jobsInstanceCount.put(job_Name, getJobInstanceCount(job_Name));
            }
        }
        extraProps.put(LIST_BATCH_JOBS, findSimpleJobInfo(jobsInstanceCount, columnFormatter));
    }
    
    private void setToLongMode(Properties extraProps, ColumnFormatter columnFormatter) {
        extraProps.put(SIMPLE_MODE, false);
        Map<String, Object> map = new HashMap<>();
        map.put("allJobsCount", getAllJobInstanceCount());
        extraProps.put(LIST_JOBS_COUNT, map);
        List<Map<String, Object>> jobExecutions = new ArrayList<>();
        extraProps.put(LIST_BATCH_JOBS, jobExecutions);
        Map<String, Integer> jobsInstanceCount = new HashMap<>();

        if (Arrays.asList(getOutputHeaders()).contains(INSTANCE_COUNT)) {
            // Get names of all available Jobs
            List<String> jobNames = executeQuery(queryToGetUniqueJobNames, "name");

            for (String job_Name : jobNames) {
                jobsInstanceCount.put(job_Name, getJobInstanceCount(job_Name));
            }
        }

        List<Long> jobInstanceIDs = getJobInstanceIDs();
        JobOperator jobOperator = AbstractListCommand.getJobOperatorFromBatchRuntime();

        for (Long jobExecution : jobInstanceIDs) {
            try {
                if (glassFishBatchSecurityHelper.isVisibleToThisInstance(((TaggedJobExecution) jobOperator.getJobExecution(jobExecution)).getTagName())) {
                    jobExecutions.add(handleJob(jobOperator.getJobExecution(jobExecution), columnFormatter, jobsInstanceCount));
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Exception while getting jobExecution details: " + ex);
                logger.log(Level.FINE, "Exception while getting jobExecution details ", ex);
            }
        }
    }

    private boolean checkIfTableExists(String tableName) {
        boolean result = true;
        String schema = batchRuntimeConfiguration.getSchemaName();
        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName();
            if (database.contains("H2")) {
                JBatchJDBCPersistenceManager jBatchJDBCPersistenceManager = new JBatchJDBCPersistenceManager();
                result = jBatchJDBCPersistenceManager.checkIfTableExists(dataSource, tableName, schema);
            } else if (database.contains("MySQL")) {
                MySqlPersistenceManager mySqlPersistenceManager = new MySqlPersistenceManager();
                result = mySqlPersistenceManager.checkIfTableExists(dataSource, tableName, schema);
            } else if (database.contains("Oracle")) {
                OraclePersistenceManager oraclePersistenceManager = new OraclePersistenceManager();
                result = oraclePersistenceManager.checkIfTableExists(dataSource, tableName, schema);
            } else if (database.contains("PostgreSQL")) {
                PostgresPersistenceManager postgresPersistenceManager = new PostgresPersistenceManager();
                result = postgresPersistenceManager.checkIfTableExists(dataSource, tableName, schema);
            } else if (database.contains("DB2")) {
                DB2PersistenceManager dB2PersistenceManager = new DB2PersistenceManager();
                result = dB2PersistenceManager.checkIfTableExists(dataSource, tableName, schema);
            } else if (database.contains("Microsoft SQL Server")) {
                SQLServerPersistenceManager sQLServerPersistenceManager = new SQLServerPersistenceManager();
                result = sQLServerPersistenceManager.checkIfTableExists(dataSource, tableName, schema);
            }
        } catch (SQLException e) {
            logger.severe(e.getLocalizedMessage());
        }

        return result;
    }
    
    private void createTables(){
          try (Connection connection = dataSource.getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName();
            
            if (database.contains("H2")) {
                JBatchJDBCPersistenceManager jBatchJDBCPersistenceManager = new JBatchJDBCPersistenceManager();
                jBatchJDBCPersistenceManager.createTables(dataSource, batchRuntimeConfiguration);
            } else if (database.contains("MySQL")) {
                MySqlPersistenceManager mySqlPersistenceManager = new MySqlPersistenceManager();
                mySqlPersistenceManager.createTables(dataSource, batchRuntimeConfiguration);
            } else if (database.contains("Oracle")) {
                OraclePersistenceManager oraclePersistenceManager = new OraclePersistenceManager();
                oraclePersistenceManager.createTables(dataSource, batchRuntimeConfiguration);
            } else if (database.contains("PostgreSQL")) {
                PostgresPersistenceManager postgresPersistenceManager = new PostgresPersistenceManager();
                postgresPersistenceManager.createTables(dataSource, batchRuntimeConfiguration);
            } else if (database.contains("DB2")) {
                DB2PersistenceManager dB2PersistenceManager = new DB2PersistenceManager();
                dB2PersistenceManager.createTables(dataSource, batchRuntimeConfiguration);
            } else if (database.contains("Microsoft SQL Server")) {
                SQLServerPersistenceManager sQLServerPersistenceManager = new SQLServerPersistenceManager();
                sQLServerPersistenceManager.createTables(dataSource, batchRuntimeConfiguration);
            }
        } catch (SQLException ex) {
             logger.severe(ex.getLocalizedMessage());
        }
    }

    private int getJobInstanceCount(String jobName) {
        int jobInstanceCount = 0;
        
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(jobinstanceid) AS jobinstancecount FROM " + jobInstanceTableKey + " WHERE NAME =?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, jobName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    resultSet.next();
                    jobInstanceCount = resultSet.getInt("jobinstancecount");
                }
            }
        } catch (SQLException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        
        return jobInstanceCount;
    }
    
       private int getAllJobInstanceCount() {
        int allJobInstanceCount = 0;
        
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(jobinstanceid) AS jobinstancecount FROM " + jobInstanceTableKey;
            try (PreparedStatement prepareStatement = connection.prepareStatement(query)) {              
                try (ResultSet resultSet = prepareStatement.executeQuery()) {
                    resultSet.next();
                    allJobInstanceCount = resultSet.getInt("jobinstancecount");
                }
            }
        } catch (SQLException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        
        return allJobInstanceCount;
    }
    
    private List<Long> getJobInstanceIDs() {
        List<Long> result = new ArrayList<>();
                
        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName();
            String query;

            if (database.contains("Derby") || database.contains("DB2")) {
                if (jobName != null) {
                    query = "SELECT jobinstanceid "
                            + "FROM JOBINSTANCEDATA "
                            + "WHERE NAME = ? "
                            + "ORDER BY jobinstanceid "
                            + "DESC FETCH FIRST ? ROWS ONLY";
                    result = queryToGetInstanceIDs(query, jobName, limitValue);
                } else {
                    query = "SELECT jobinstanceid "
                            + "FROM JOBINSTANCEDATA "
                            + "ORDER BY jobinstanceid DESC "
                            + "OFFSET ? "
                            + "ROWS FETCH FIRST ? ROWS ONLY";
                    result = queryToGetInstanceIDs(query, offSetValue, limitValue);
                }


            } else if (database.contains("Oracle")) {
                if (jobName != null) {
                    query = "SELECT jobinstanceid "
                            + "FROM ("
                            + "SELECT jobinstanceid "
                            + "FROM " + jobInstanceTableKey
                            + " WHERE NAME =?"
                            + ") "
                            + "WHERE ROWNUM <=?"
                            + " ORDER BY jobinstanceid DESC";
                    result = queryToGetInstanceIDs(query, jobName, limitValue);
                } else {
                    int offset = Integer.parseInt(offSetValue);       
                    int limit = Integer.parseInt(limitValue);
                    int rowSize = offset + limit;
                    query = "SELECT jobinstanceid "
                            + "FROM ("
                            + "SELECT ROWNUM numofrows, tables.* "
                            + "FROM("
                            + "SELECT jobinstanceid "
                            + "FROM " + jobInstanceTableKey
                            + " ORDER BY jobinstanceid DESC"
                            + ") tables "
                            + "WHERE ROWNUM <=?"
                            + ") "
                            + "WHERE numofrows >?";
                    result = queryToGetInstanceIDs(query, String.valueOf(rowSize), offSetValue);
                }                  
            } else if (database.contains("Microsoft SQL Server")) {
                if (jobName != null) {
                    query = "SELECT TOP ? "
                            + "jobinstanceid FROM " + jobInstanceTableKey
                            + " WHERE NAME =? "
                            + "ORDER BY jobinstanceid DESC";
                    result = queryToGetInstanceIDs(query, limitValue, jobName);
                } else {
                    query = "SELECT TOP ? jobinstanceid "
                            + "FROM ("
                            + "SELECT jobinstanceid, ROW_NUMBER() "
                            + "OVER ("
                            + "ORDER BY jobinstanceid DESC"
                            + ") as RowNum "
                            + "FROM " + jobInstanceTableKey
                            + ") AS MyDerivedTable "
                            + "WHERE MyDerivedTable.RowNum > ?";
                    result = queryToGetInstanceIDs(query, limitValue, offSetValue);
                }
            } else {
                if (jobName != null) {
                    query = "SELECT jobinstanceid "
                            + "FROM " + jobInstanceTableKey
                            + " WHERE NAME = ? "
                            + " ORDER BY jobinstanceid "
                            + "DESC LIMIT ?";
                    result = queryToGetInstanceIDs(query, jobName, limitValue);
                } else {
                    query = "SELECT jobinstanceid "
                            + "FROM " + jobInstanceTableKey
                            + " ORDER BY jobinstanceid "
                            + "DESC LIMIT ?"
                            + " OFFSET ?";
                    result = queryToGetInstanceIDs(query, limitValue, offSetValue);
                }
            }           
        } catch (SQLException ex) {
             logger.severe(ex.getLocalizedMessage());
        }

        return result;
    }

    @Override
    protected final String[] getAllHeaders() {
        return new String[]{
            JOB_NAME, APP_NAME, INSTANCE_COUNT, INSTANCE_ID, EXECUTION_ID, BATCH_STATUS,
            START_TIME, END_TIME, EXIT_STATUS
        };
    }

    @Override
    protected final String[] getDefaultHeaders() {
        return new String[]{JOB_NAME, INSTANCE_COUNT};
    }

    private boolean isSimpleMode() {

        for (String outputHeader : getOutputHeaders()) {
            if (!JOB_NAME.equals(outputHeader) && !INSTANCE_COUNT.equals(outputHeader)) {
                return false;
            }
        }
        return getOutputHeaders().length == 2;
    }

    private Map<String, Integer> findSimpleJobInfo(Map<String, Integer> jobToInstanceCountMap, ColumnFormatter columnFormatter) {
        for (Map.Entry<String, Integer> e : jobToInstanceCountMap.entrySet()) {
            columnFormatter.addRow(new Object[]{e.getKey(), e.getValue()});
        }
        return jobToInstanceCountMap;
    }

    private Map<String, Object> handleJob(JobExecution jobExecution, ColumnFormatter columnFormatter, Map<String, Integer>  jobInstanceCount)
            throws JobSecurityException, NoSuchJobException, NoSuchJobExecutionException {
        Map<String, Object> jobInfo = new HashMap<>();

        String[] columnFormatterData = new String[getOutputHeaders().length];
        JobOperator jobOperator = AbstractListCommand.getJobOperatorFromBatchRuntime();
        for (int index = 0; index < getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case JOB_NAME:
                    data = "" + jobExecution.getJobName();
                    break;
                case APP_NAME:
                    try {
                        String appName = "" + ((TaggedJobExecution) jobExecution).getTagName();
                        int semi = appName.indexOf(':');
                        data = appName.substring(semi + 1);
                    } catch (Exception ex) {
                        logger.log(Level.FINE, "Error while calling ((TaggedJobExecution) je).getTagName() ", ex);
                        data = ex.toString();
                    }
                    break;
                case INSTANCE_COUNT:
                    data = jobInstanceCount.size() > 0 ? jobInstanceCount.get(jobExecution.getJobName()) : "";
                    break;
                case INSTANCE_ID:
                    data = jobOperator.getJobInstance(jobExecution.getExecutionId()).getInstanceId();
                    break;
                case EXECUTION_ID:
                    data = jobExecution.getExecutionId();
                    break;
                case BATCH_STATUS:
                    data = jobExecution.getBatchStatus() != null ? jobExecution.getBatchStatus() : "";
                    break;
                case EXIT_STATUS:
                    data = jobExecution.getExitStatus() != null ? jobExecution.getExitStatus() : "";
                    break;
                case START_TIME:
                    if (jobExecution.getStartTime() != null) {
                        data = jobExecution.getStartTime().getTime();
                        columnFormatterData[index] = jobExecution.getStartTime().toString();
                    } else {
                        data = "";
                    }
                    break;
                case END_TIME:
                    if (jobExecution.getEndTime() != null) {
                        data = jobExecution.getEndTime().getTime();
                        columnFormatterData[index] = jobExecution.getEndTime().toString();
                    } else {
                        data = "";
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            if (columnFormatterData[index] == null) {
                columnFormatterData[index] = data.toString();
            }
        }
        columnFormatter.addRow(columnFormatterData);

        return jobInfo;
    }
    
    
     private List<Long> queryToGetInstanceIDs(String query, String parameter1, String parameter2) {
        List<Long> result = new ArrayList<>();
        String columnID = "jobinstanceid";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, Integer.parseInt(parameter1));
                preparedStatement.setInt(2, Integer.parseInt(parameter2));

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(resultSet.getLong(columnID));
                    }
                }
            }
        } catch (SQLException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        return result;
    }
      
    private List<String> executeQuery(String query, String columnID) {
        List<String> result = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        result.add(resultSet.getString(columnID));
                    }
                }
            }
        } catch (SQLException ex) {
            logger.severe(ex.getLocalizedMessage());
        }
        
        return result;
    }
}
