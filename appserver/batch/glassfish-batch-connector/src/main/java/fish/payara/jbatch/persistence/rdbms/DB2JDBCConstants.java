/*
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
 
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
 * Interface for common DB2 JDBC Constants
 * @author dwinters
 *
 */
public interface DB2JDBCConstants {
	
	public static final String DB2_CREATE_TABLE_CHECKPOINTDATA = "DB2__CREATE_TABLE_CHECKPOINTDATA";
	public static final String DB2_CREATE_TABLE_JOBINSTANCEDATA = "DB2_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA = "DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String DB2_CREATE_TABLE_STEPINSTANCEDATA = "DB2_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String DB2_CREATE_TABLE_JOBSTATUS = "DB2_CREATE_TABLE_JOBSTATUS";
	public static final String DB2_CREATE_TABLE_STEPSTATUS = "DB2_CREATE_TABLE_STEPSTATUS";
	public static final String CREATE_DB2_CHECKPOINTDATA_INDEX_KEY = "chk_index";

}
