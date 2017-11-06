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
 * Interface for common MySQL JDBC Constants
 * @author dwinters
 *
 */
public interface MySQLJDBCConstants {
	
	public static final String MYSQL_CREATE_TABLE_CHECKPOINTDATA = "MYSQL_CREATE_TABLE_CHECKPOINTDATA";
	public static final String MYSQL_CREATE_TABLE_JOBINSTANCEDATA = "MYSQL_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String MYSQL_CREATE_TABLE_EXECUTIONINSTANCEDATA = "MYSQL_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String MYSQL_CREATE_TABLE_STEPINSTANCEDATA = "MYSQL_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String MYSQL_CREATE_TABLE_JOBSTATUS = "MYSQL_CREATE_TABLE_JOBSTATUS";
	public static final String MYSQL_CREATE_TABLE_STEPSTATUS = "MYSQL_CREATE_TABLE_STEPSTATUS";
	public static final String MYSQL_DEFAULT_DATABASE="test";

}
