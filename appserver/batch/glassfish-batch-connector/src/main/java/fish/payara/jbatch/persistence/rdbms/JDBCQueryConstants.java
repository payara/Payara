/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.jbatch.persistence.rdbms;

/**
 * Interface for the query keys and table keys
 * 
 * @author steve
 */
public interface JDBCQueryConstants {

	public final static String JOB_INSTANCE_TABLE_KEY = "JOB_INSTANCE_TABLE_KEY";
	public final static String EXECUTION_INSTANCE_TABLE_KEY = "EXECUTION_INSTANCE_TABLE_KEY";
	public final static String STEP_EXECUTION_INSTANCE_TABLE_KEY = "STEP_EXECUTION_INSTANCE_TABLE_KEY";
	public final static String JOB_STATUS_TABLE_KEY = "JOB_STATUS_TABLE_KEY";
	public final static String STEP_STATUS_TABLE_KEY = "STEP_STATUS_TABLE_KEY";
	public final static String CHECKPOINT_TABLE_KEY = "CHECKPOINT_TABLE_KEY";
	public final static String Q_SET_SCHEMA = "Q_SET_SCHEMA";
	public final static String SELECT_CHECKPOINTDATA = "SELECT_CHECKPOINTDATA";
	public final static String INSERT_CHECKPOINTDATA = "INSERT_CHECKPOINTDATA";
	public final static String UPDATE_CHECKPOINTDATA = "UPDATE_CHECKPOINTDATA";
	public final static String JOBOPERATOR_GET_JOB_INSTANCE_COUNT = "JOBOPERATOR_GET_JOB_INSTANCE_COUNT";
	public final static String SELECT_JOBINSTANCEDATA_COUNT = "SELECT_JOBINSTANCEDATA_COUNT";
	public final static String JOBOPERATOR_GET_JOB_INSTANCE_IDS = "JOBOPERATOR_GET_JOB_INSTANCE_IDS";
	public final static String SELECT_JOBINSTANCEDATA_IDS = "SELECT_JOBINSTANCEDATA_IDS";
	public final static String JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA = "JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA";
	public final static String JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP = "JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP";
	public final static String JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS = "JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS";
	public final static String JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS = "JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS";
	public final static String JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID = "JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID";
	public final static String GET_PARAMETERS = "GET_PARAMETERS";
	public final static String MOST_RECENT_STEPS_FOR_JOB = "MOST_RECENT_STEPS_FOR_JOB";
	public final static String STEP_EXECUTIONS_FOR_JOB_EXECUTION = "STEP_EXECUTIONS_FOR_JOB_EXECUTION";
	public final static String STEP_EXECUTIONS_BY_STEP_ID = "STEP_EXECUTIONS_BY_STEP_ID";
	public final static String UPDATE_BATCH_STATUS_ONLY = "UPDATE_BATCH_STATUS_ONLY";
	public final static String UPDATE_FINAL_STATUS_AND_TIMESTAMP = "UPDATE_FINAL_STATUS_AND_TIMESTAMP";
	public final static String MARK_JOB_STARTED = "MARK_JOB_STARTED";
	public final static String JOB_OPERATOR_GET_JOB_EXECUTION = "JOB_OPERATOR_GET_JOB_EXECUTION";
	public final static String JOB_OPERATOR_GET_JOB_EXECUTIONS = "JOB_OPERATOR_GET_JOB_EXECUTIONS";
	public final static String JOB_OPERATOR_GET_RUNNING_EXECUTIONS = "JOB_OPERATOR_GET_RUNNING_EXECUTIONS";
	public final static String SELECT_JOBINSTANCEDATA_APPTAG = "SELECT_JOBINSTANCEDATA_APPTAG";
	public final static String DELETE_JOBS = "DELETE_JOBS";
	public final static String DELETE_JOB_EXECUTIONS = "DELETE_JOB_EXECUTIONS";
	public final static String DELETE_STEP_EXECUTIONS = "DELETE_STEP_EXECUTIONS";
	public final static String GET_JOB_STATUS_FROM_EXECUTIONS = "GET_JOB_STATUS_FROM_EXECUTIONS";
	public final static String JOB_INSTANCE_ID_BY_EXECUTION_ID = "JOB_INSTANCE_ID_BY_EXECUTION_ID";
	public final static String CREATE_SUB_JOB_INSTANCE = "CREATE_SUB_JOB_INSTANCE";
	public final static String CREATE_JOB_INSTANCE = "CREATE_JOB_INSTANCE";
	public final static String CREATE_JOB_EXECUTION_ENTRY = "CREATE_JOB_EXECUTION_ENTRY";
	public final static String CREATE_STEP_EXECUTION = "CREATE_STEP_EXECUTION";
	public final static String UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION = "UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION";
	public final static String UPDATE_STEP_EXECUTION_WITH_METRICS = "UPDATE_STEP_EXECUTION_WITH_METRICS";
	public final static String CREATE_JOBSTATUS = "CREATE_JOBSTATUS";
	public final static String GET_JOB_STATUS = "GET_JOB_STATUS";
	public final static String UPDATE_JOBSTATUS = "UPDATE_JOBSTATUS";
	public final static String CREATE_STEP_STATUS = "CREATE_STEP_STATUS";
	public final static String GET_STEP_STATUS = "GET_STEP_STATUS";
	public final static String UPDATE_STEP_STATUS = "UPDATE_STEP_STATUS";
	public final static String GET_TAGNAME = "GET_TAGNAME";
	public final static String GET_MOST_RECENT_EXECUTION_ID = "GET_MOST_RECENT_EXECUTION_ID";
	
	
	//oracle constants
	
	public static final String CREATE_TABLE_JOBSTATUS = "CREATE_TABLE_JOBSTATUS";
	public static final String CREATE_TABLE_STEPSTATUS = "CREATE_TABLE_STEPSTATUS";
	public static final String CREATE_TABLE_CHECKPOINTDATA = "CREATE_TABLE_CHECKPOINTDATA";
	public static final String CREATE_TABLE_JOBINSTANCEDATA = "CREATE_TABLE_JOBINSTANCEDATA";
	public static final String CREATE_JOBINSTANCEDATA_SEQ = "CREATE_JOBINSTANCEDATA_SEQ";
	public static final String JOBINSTANCEDATA_SEQ_KEY = "JOBINSTANCEDATA_SEQ";
	public static final String CREATE_JOBINSTANCEDATA_TRG = "CREATE_JOBINSTANCEDATA_TRG";
	public static final String JOBINSTANCEDATA_TRG_KEY = "JOBINSTANCEDATA_TRG";
	
	
	public static final String CREATE_TABLE_EXECUTIONINSTANCEDATA = "CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String CREATE_EXECUTIONINSTANCEDATA_SEQ = "CREATE_EXECUTIONINSTANCEDATA_SEQ";
	public static final String EXECUTIONINSTANCEDATA_SEQ_KEY = "EXECUTIONINSTANCEDATA_SEQ";
	public static final String CREATE_EXECUTIONINSTANCEDATA_TRG = "CREATE_EXECUTIONINSTANCEDATA_TRG";
	public static final String EXECUTIONINSTANCEDATA_TRG_KEY = "EXECUTIONINSTANCEDATA_TRG";
	
	public static final String CREATE_TABLE_STEPINSTANCEDATA = "CREATE_TABLE_STEPINSTANCEDATA";
	public static final String CREATE_STEPINSTANCEDATA_SEQ = "CREATE_STEPINSTANCEDATA_SEQ";
	public static final String STEPINSTANCEDATA_SEQ_KEY = "STEPEXECUTIONINSTANCEDATA_SEQ";
	public static final String CREATE_STEPINSTANCEDATA_TRG = "CREATE_STEPINSTANCEDATA_TRG";
	public static final String STEPINSTANCEDATA_TRG_KEY = "STEPEXECUTIONINSTANCEDATA_TRG";
	
	public static final String CREATE_CHECKPOINTDATA_INDEX = "CREATE_CHECKPOINTDATA_INDEX";
	public static final String CREATE_CHECKPOINTDATA_INDEX_KEY = "chk_index";
	
	// postgres constants

	public static final String POSTGRES_CREATE_TABLE_CHECKPOINTDATA = "POSTGRES_CREATE_TABLE_CHECKPOINTDATA";
	public static final String POSTGRES_CREATE_TABLE_JOBINSTANCEDATA = "POSTGRES_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA = "POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String POSTGRES_CREATE_TABLE_STEPINSTANCEDATA = "POSTGRES_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String POSTGRES_CREATE_TABLE_JOBSTATUS = "POSTGRES_CREATE_TABLE_JOBSTATUS";
	public static final String POSTGRES_CREATE_TABLE_STEPSTATUS = "POSTGRES_CREATE_TABLE_STEPSTATUS";
	
	
	// db2 constants
	
	public static final String DB2_CREATE_TABLE_CHECKPOINTDATA = "DB2__CREATE_TABLE_CHECKPOINTDATA";
	public static final String DB2_CREATE_TABLE_JOBINSTANCEDATA = "DB2_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA = "DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String DB2_CREATE_TABLE_STEPINSTANCEDATA = "DB2_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String DB2_CREATE_TABLE_JOBSTATUS = "DB2_CREATE_TABLE_JOBSTATUS";
	public static final String DB2_CREATE_TABLE_STEPSTATUS = "DB2_CREATE_TABLE_STEPSTATUS";
	public static final String CREATE_DB2_CHECKPOINTDATA_INDEX_KEY = "chk_index";
	
	// derby constants
	public static final String DERBY_CREATE_TABLE_CHECKPOINTDATA = "DB2_CREATE_TABLE_CHECKPOINTDATA";
	public static final String DERBY_CREATE_TABLE_JOBINSTANCEDATA = "DB2_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String DERBY_CREATE_TABLE_EXECUTIONINSTANCEDATA = "DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String DERBY_CREATE_TABLE_STEPINSTANCEDATA = "DB2_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String DERBY_CREATE_TABLE_JOBSTATUS = "DB2_CREATE_TABLE_JOBSTATUS";
	public static final String DERBY_CREATE_TABLE_STEPSTATUS = "DB2_CREATE_TABLE_STEPSTATUS";
	
	// mysql constants
	public static final String MYSQL_CREATE_TABLE_CHECKPOINTDATA = "MYSQL_CREATE_TABLE_CHECKPOINTDATA";
	public static final String MYSQL_CREATE_TABLE_JOBINSTANCEDATA = "MYSQL_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String MYSQL_CREATE_TABLE_EXECUTIONINSTANCEDATA = "MYSQL_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String MYSQL_CREATE_TABLE_STEPINSTANCEDATA = "MYSQL_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String MYSQL_CREATE_TABLE_JOBSTATUS = "MYSQL_CREATE_TABLE_JOBSTATUS";
	public static final String MYSQL_CREATE_TABLE_STEPSTATUS = "MYSQL_CREATE_TABLE_STEPSTATUS";
	
	public final String APPTAG = "apptag";

}
