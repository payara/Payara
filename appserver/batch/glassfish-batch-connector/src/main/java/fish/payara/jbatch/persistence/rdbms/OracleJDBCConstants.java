/*
 * Copyright (c) 2014, 2016 Payara Foundation. All rights reserved.
 
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

/**
 * Interface for common Oracle JDBC Constants
 * @author dwinters
 *
 */

public interface OracleJDBCConstants {
	
		public static final String CREATE_TABLE_JOBSTATUS = "CREATE_TABLE_JOBSTATUS";
		public static final String CREATE_TABLE_STEPSTATUS = "CREATE_TABLE_STEPSTATUS";
		public static final String CREATE_TABLE_CHECKPOINTDATA = "CREATE_TABLE_CHECKPOINTDATA";
		public static final String CREATE_TABLE_JOBINSTANCEDATA = "CREATE_TABLE_JOBINSTANCEDATA";
		public static final String CREATE_JOBINSTANCEDATA_SEQ = "CREATE_JOBINSTANCEDATA_SEQ";
		public static final String JOBINSTANCEDATA_SEQ_KEY = "JOBINSTANCE_SEQ";
		public static final String CREATE_JOBINSTANCEDATA_TRG = "CREATE_JOBINSTANCEDATA_TRG";
		public static final String JOBINSTANCEDATA_TRG_KEY = "JOBINSTANCE_TRG";
		
		
		public static final String CREATE_TABLE_EXECUTIONINSTANCEDATA = "CREATE_TABLE_EXECUTIONINSTANCEDATA";
		public static final String CREATE_EXECUTIONINSTANCEDATA_SEQ = "CREATE_EXECUTIONINSTANCEDATA_SEQ";
		public static final String EXECUTIONINSTANCEDATA_SEQ_KEY = "EXECUTIONINSTANCE_SEQ";
		public static final String CREATE_EXECUTIONINSTANCEDATA_TRG = "CREATE_EXECUTIONINSTANCEDATA_TRG";
		public static final String EXECUTIONINSTANCEDATA_TRG_KEY = "EXECUTIONINSTANCE_TRG";
		
		public static final String CREATE_TABLE_STEPINSTANCEDATA = "CREATE_TABLE_STEPINSTANCEDATA";
		public static final String CREATE_STEPINSTANCEDATA_SEQ = "CREATE_STEPINSTANCEDATA_SEQ";
		public static final String STEPINSTANCEDATA_SEQ_KEY = "STEPEXECUTIONINSTANCE_SEQ";
		public static final String CREATE_STEPINSTANCEDATA_TRG = "CREATE_STEPINSTANCEDATA_TRG";
		public static final String STEPINSTANCEDATA_TRG_KEY = "STEPEXECUTIONINSTANCE_TRG";
		
		public static final String CREATE_CHECKPOINTDATA_INDEX = "CREATE_CHECKPOINTDATA_INDEX";
		public static final String CREATE_CHECKPOINTDATA_INDEX_KEY = "chk_index";
                
                // Previous default trigger names for backward compatibility
                public static final String DEFAULT_JOBINSTANCEDATA_TRG_KEY = "JOBINSTANCEDATA_TRG";
                public static final String DEFAULT_EXECUTIONINSTANCEDATA_TRG_KEY = "EXECUTIONINSTANCEDATA_TRG";
                public static final String DEFAULT_STEPINSTANCEDATA_TRG_KEY = "STEPEXECUTIONINSTANCEDATA_TRG";
}
