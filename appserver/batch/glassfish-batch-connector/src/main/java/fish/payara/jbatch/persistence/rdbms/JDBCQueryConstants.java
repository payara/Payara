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

/**
 * Interface for the common JDBC Constants
 * 
 * @author steve
 */
public interface JDBCQueryConstants {

	public static final String JOB_INSTANCE_TABLE_KEY = "JOB_INSTANCE_TABLE_KEY";
	public static final String EXECUTION_INSTANCE_TABLE_KEY = "EXECUTION_INSTANCE_TABLE_KEY";
	public static final String STEP_EXECUTION_INSTANCE_TABLE_KEY = "STEP_EXECUTION_INSTANCE_TABLE_KEY";
	public static final String JOB_STATUS_TABLE_KEY = "JOB_STATUS_TABLE_KEY";
	public static final String STEP_STATUS_TABLE_KEY = "STEP_STATUS_TABLE_KEY";
	public static final String CHECKPOINT_TABLE_KEY = "CHECKPOINT_TABLE_KEY";
	public static final String Q_SET_SCHEMA = "Q_SET_SCHEMA";
	public static final String SELECT_CHECKPOINTDATA = "SELECT_CHECKPOINTDATA";
	public static final String INSERT_CHECKPOINTDATA = "INSERT_CHECKPOINTDATA";
	public static final String UPDATE_CHECKPOINTDATA = "UPDATE_CHECKPOINTDATA";
	public static final String LOCK_CHECKPOINTDATA = "LOCK_CHECKPOINTDATA";
	public static final String JOBOPERATOR_GET_JOB_INSTANCE_COUNT = "JOBOPERATOR_GET_JOB_INSTANCE_COUNT";
	public static final String SELECT_JOBINSTANCEDATA_COUNT = "SELECT_JOBINSTANCEDATA_COUNT";
	public static final String JOBOPERATOR_GET_JOB_INSTANCE_IDS = "JOBOPERATOR_GET_JOB_INSTANCE_IDS";
	public static final String SELECT_JOBINSTANCEDATA_IDS = "SELECT_JOBINSTANCEDATA_IDS";
	public static final String JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA = "JOB_OPERATOR_GET_EXTERNAL_JOB_INSTANCE_DATA";
	public static final String JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP = "JOB_OPERATOR_QUERY_JOB_EXECUTION_TIMESTAMP";
	public static final String JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS = "JOB_OPERATOR_QUERY_JOB_EXECUTION_BATCH_STATUS";
	public static final String JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS = "JOB_OPERATOR_QUERY_JOB_EXECUTION_EXIT_STATUS";
	public static final String JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID = "JOB_OPERATOR_QUERY_JOB_EXECUTION_JOB_ID";
	public static final String GET_PARAMETERS = "GET_PARAMETERS";
	public static final String MOST_RECENT_STEPS_FOR_JOB = "MOST_RECENT_STEPS_FOR_JOB";
	public static final String STEP_EXECUTIONS_FOR_JOB_EXECUTION = "STEP_EXECUTIONS_FOR_JOB_EXECUTION";
	public static final String STEP_EXECUTIONS_BY_STEP_ID = "STEP_EXECUTIONS_BY_STEP_ID";
	public static final String UPDATE_BATCH_STATUS_ONLY = "UPDATE_BATCH_STATUS_ONLY";
	public static final String UPDATE_FINAL_STATUS_AND_TIMESTAMP = "UPDATE_FINAL_STATUS_AND_TIMESTAMP";
	public static final String MARK_JOB_STARTED = "MARK_JOB_STARTED";
	public static final String JOB_OPERATOR_GET_JOB_EXECUTION = "JOB_OPERATOR_GET_JOB_EXECUTION";
	public static final String JOB_OPERATOR_GET_JOB_EXECUTIONS = "JOB_OPERATOR_GET_JOB_EXECUTIONS";
	public static final String JOB_OPERATOR_GET_RUNNING_EXECUTIONS = "JOB_OPERATOR_GET_RUNNING_EXECUTIONS";
	public static final String SELECT_JOBINSTANCEDATA_APPTAG = "SELECT_JOBINSTANCEDATA_APPTAG";
	public static final String DELETE_JOBS = "DELETE_JOBS";
	public static final String DELETE_JOB_EXECUTIONS = "DELETE_JOB_EXECUTIONS";
	public static final String DELETE_STEP_EXECUTIONS = "DELETE_STEP_EXECUTIONS";
	public static final String GET_JOB_STATUS_FROM_EXECUTIONS = "GET_JOB_STATUS_FROM_EXECUTIONS";
	public static final String JOB_INSTANCE_ID_BY_EXECUTION_ID = "JOB_INSTANCE_ID_BY_EXECUTION_ID";
	public static final String CREATE_SUB_JOB_INSTANCE = "CREATE_SUB_JOB_INSTANCE";
	public static final String CREATE_JOB_INSTANCE = "CREATE_JOB_INSTANCE";
	public static final String CREATE_JOB_EXECUTION_ENTRY = "CREATE_JOB_EXECUTION_ENTRY";
	public static final String CREATE_STEP_EXECUTION = "CREATE_STEP_EXECUTION";
	public static final String UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION = "UPDATE_WITH_FINAL_PARTITION_STEP_EXECUTION";
	public static final String UPDATE_STEP_EXECUTION_WITH_METRICS = "UPDATE_STEP_EXECUTION_WITH_METRICS";
	public static final String CREATE_JOBSTATUS = "CREATE_JOBSTATUS";
	public static final String GET_JOB_STATUS = "GET_JOB_STATUS";
	public static final String UPDATE_JOBSTATUS = "UPDATE_JOBSTATUS";
	public static final String CREATE_STEP_STATUS = "CREATE_STEP_STATUS";
	public static final String GET_STEP_STATUS = "GET_STEP_STATUS";
	public static final String UPDATE_STEP_STATUS = "UPDATE_STEP_STATUS";
	public static final String GET_TAGNAME = "GET_TAGNAME";
	public static final String GET_MOST_RECENT_EXECUTION_ID = "GET_MOST_RECENT_EXECUTION_ID";

        // H2 JDBC Constants
        public static final String H2_CREATE_TABLE_CHECKPOINTDATA = "H2_CREATE_TABLE_CHECKPOINTDATA";
	public static final String H2_CREATE_TABLE_JOBINSTANCEDATA = "H2_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String H2_CREATE_TABLE_EXECUTIONINSTANCEDATA = "H2_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String H2_CREATE_TABLE_STEPINSTANCEDATA = "H2_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String H2_CREATE_TABLE_JOBSTATUS = "H2_CREATE_TABLE_JOBSTATUS";
	public static final String H2_CREATE_TABLE_STEPSTATUS = "H2_CREATE_TABLE_STEPSTATUS";
        
	public final String APPTAG = "apptag";

}
