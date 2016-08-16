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
 * Interface for common Postgres JDBC Constants
 * @author dwinters
 *
 */

public interface PostgresJDBCConstants {

	public static final String POSTGRES_CREATE_TABLE_CHECKPOINTDATA = "POSTGRES_CREATE_TABLE_CHECKPOINTDATA";
	public static final String POSTGRES_CREATE_TABLE_JOBINSTANCEDATA = "POSTGRES_CREATE_TABLE_JOBINSTANCEDATA";
	public static final String POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA = "POSTGRES_CREATE_TABLE_EXECUTIONINSTANCEDATA";
	public static final String POSTGRES_CREATE_TABLE_STEPINSTANCEDATA = "POSTGRES_CREATE_TABLE_STEPINSTANCEDATA";
	public static final String POSTGRES_CREATE_TABLE_JOBSTATUS = "POSTGRES_CREATE_TABLE_JOBSTATUS";
	public static final String POSTGRES_CREATE_TABLE_STEPSTATUS = "POSTGRES_CREATE_TABLE_STEPSTATUS";
        
        // System properties
        public static final String P_MJS_RETRY_MAX = "fish.payara.jbatch.pg.mjsretrymax";
        public static final String P_MJS_RETRY_DELAY = "fish.payara.jbatch.pg.mjsretrydelay";
        
        // Default System property values
        public static final int MJS_RETRY_MAX_DEFAULT = 500;
        public static final int MJS_RETRY_DELAY_DEFAULT = 10;
}
