/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package org.glassfish.jdbc.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Properties;

/**
 * Create JDBC Connection Pool Command
 * 
 */
@ExecuteOn(RuntimeType.ALL)
@Service(name="create-jdbc-connection-pool")
@PerLookup
@I18n("create.jdbc.connection.pool")
public class CreateJdbcConnectionPool implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateJdbcConnectionPool.class);    

    @Param(name = "datasourceClassname",  optional=true)
    String datasourceclassname;

    @Param(optional=true, name = "resType",  acceptableValues="javax.sql.DataSource,javax.sql.XADataSource,javax.sql.ConnectionPoolDataSource,java.sql.Driver")
    String restype;

    @Param(name = "steadyPoolSize",  optional=true, defaultValue = "8")
    String steadypoolsize = "8";

    @Param(name = "maxPoolSize",  optional=true, defaultValue = "32")
    String maxpoolsize = "32";
    
    @Param(name="maxWait", alias = "maxWaitTimeInMillis",  optional=true, defaultValue = "60000")
    String maxwait = "60000";

    @Param(name="poolResize", alias = "poolResizeQuantity",  optional=true, defaultValue = "2")
    String poolresize = "2";
    
    @Param(name="idleTimeout", alias = "idleTimeoutInSeconds",  optional=true, defaultValue = "300")
    String idletimeout = "300";

    @Param(name = "initSql", optional=true)
    String initsql;
        
    @Param(name="isolationLevel", alias = "transactionIsolationLevel",  optional=true)
    String isolationlevel;
            
    @Param(name="isIsolationGuaranteed", alias = "isIsolationLevelGuaranteed",  optional=true, defaultValue="true")
    Boolean isisolationguaranteed;
                
    @Param(name="isConnectValidateReq", alias = "isConnectionValidationRequired",  optional=true, defaultValue="false")
    Boolean isconnectvalidatereq;
    
    @Param(name = "validationMethod", optional=true, alias = "connectionValidationMethod",  acceptableValues="auto-commit,meta-data,table,custom-validation", defaultValue = "table")
    String validationmethod = "table";
    
    @Param(name="validationTable", alias = "validationTableName",  optional=true)
    String validationtable;
    
    @Param(name="failConnection", alias = "failAllConnections",  optional=true, defaultValue="false")
    Boolean failconnection;
    
    @Param(name = "allowNonComponentCallers",  optional=true, defaultValue="false")
    Boolean allownoncomponentcallers;
    
    @Param(name = "nonTransactionalConnections",  optional=true, defaultValue="false")
    Boolean nontransactionalconnections;
    
    @Param(name="validateAtMostOncePeriod", alias = "validateAtmostOncePeriodInSeconds",  optional=true, defaultValue = "0")
    String validateatmostonceperiod = "0";
    
    @Param(name="leakTimeout", alias = "connectionLeakTimeoutInSeconds",  optional=true, defaultValue = "0")
    String leaktimeout = "0";
    
    @Param(name="leakReclaim", alias = "connectionLeakReclaim",  optional=true, defaultValue="false")
    Boolean leakreclaim;
    
    @Param(name="creationRetryAttempts", alias = "connectionCreationRetryAttempts",  optional=true, defaultValue = "0")
    String creationretryattempts = "0";
    
    @Param(name="creationRetryInterval", alias = "connectionCreationRetryIntervalInSeconds",  optional=true, defaultValue = "10")
    String creationretryinterval = "10";

    @Param(name = "sqlTraceListeners", optional=true)
    String sqltracelisteners;
    
    @Param(name="statementTimeout", alias = "statementTimeoutInSeconds",  optional=true, defaultValue = "-1")
    String statementtimeout = "-1";
    
    @Param(name="statementLeakTimeout", alias = "statementLeakTimeoutInSeconds",  optional=true, defaultValue = "0")
    String statementLeaktimeout = "0";

    @Param(name="statementLeakReclaim", alias = "statementLeakReclaim",  optional=true, defaultValue="false")
    Boolean statementLeakreclaim;

    @Param(name = "lazyConnectionEnlistment",  optional=true, defaultValue="false")
    Boolean lazyconnectionenlistment;
    
    @Param(name = "lazyConnectionAssociation",  optional=true, defaultValue="false")
    Boolean lazyconnectionassociation;
    
    @Param(name = "associateWithThread",  optional=true, defaultValue="false")
    Boolean associatewiththread;

    @Param(name = "driverClassname",  optional=true)
    String driverclassname;
    
    @Param(name = "matchConnections",  optional=true, defaultValue="false")
    Boolean matchconnections;
    
    @Param(name = "maxConnectionUsageCount",  optional=true, defaultValue = "0")
    String maxconnectionusagecount = "0";

    @Param(optional=true, defaultValue="false")
    Boolean ping;

    @Param(optional=true, defaultValue="true")
    Boolean pooling;

    @Param(optional=true, name = "statementCacheSize",  defaultValue="0")
    String statementcachesize;

    @Param(name = "validationClassname",  optional=true)
    String validationclassname;
    
    @Param(name = "wrapJdbcObjects",  optional=true, defaultValue="true")
    Boolean wrapjdbcobjects;
    
    @Param(name="description", optional=true)
    String description;
    
    @Param(name="property", optional=true, separator=':')
    Properties properties;

    @Param(optional=true, obsolete = true)
    String target = SystemPropertyConstants.DAS_SERVER_NAME;
    
    @Param(name="jdbc_connection_pool_id", alias = "name" /*Mapped to ResourceConstants.CONNECTION_POOL_NAME below */,  primary=true)
    String jdbc_connection_pool_id;

    @Inject
    Domain domain;

    @Inject
    CommandRunner commandRunner;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
       
        HashMap attrList = new HashMap();
        attrList.put(ResourceConstants.CONNECTION_POOL_NAME, jdbc_connection_pool_id);
        attrList.put(ResourceConstants.DATASOURCE_CLASS, datasourceclassname);
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(ResourceConstants.RES_TYPE, restype);
        attrList.put(ResourceConstants.STEADY_POOL_SIZE, steadypoolsize);
        attrList.put(ResourceConstants.MAX_POOL_SIZE, maxpoolsize);
        attrList.put(ResourceConstants.MAX_WAIT_TIME_IN_MILLIS, maxwait);
        attrList.put(ResourceConstants.POOL_SIZE_QUANTITY, poolresize);
        attrList.put(ResourceConstants.INIT_SQL, initsql);
        attrList.put(ResourceConstants.IDLE_TIME_OUT_IN_SECONDS, idletimeout);
        attrList.put(ResourceConstants.TRANS_ISOLATION_LEVEL, isolationlevel);
        attrList.put(ResourceConstants.IS_ISOLATION_LEVEL_GUARANTEED, isisolationguaranteed.toString());
        attrList.put(ResourceConstants.IS_CONNECTION_VALIDATION_REQUIRED, isconnectvalidatereq.toString());
        attrList.put(ResourceConstants.CONNECTION_VALIDATION_METHOD, validationmethod);
        attrList.put(ResourceConstants.VALIDATION_TABLE_NAME, validationtable);
        attrList.put(ResourceConstants.CONN_FAIL_ALL_CONNECTIONS, failconnection.toString());
        attrList.put(ResourceConstants.NON_TRANSACTIONAL_CONNECTIONS, nontransactionalconnections.toString());
        attrList.put(ResourceConstants.ALLOW_NON_COMPONENT_CALLERS, allownoncomponentcallers.toString());
        attrList.put(ResourceConstants.VALIDATE_ATMOST_ONCE_PERIOD_IN_SECONDS, validateatmostonceperiod);
        attrList.put(ResourceConstants.CONNECTION_LEAK_TIMEOUT_IN_SECONDS, leaktimeout);
        attrList.put(ResourceConstants.CONNECTION_LEAK_RECLAIM, leakreclaim.toString());
        attrList.put(ResourceConstants.CONNECTION_CREATION_RETRY_ATTEMPTS, creationretryattempts);
        attrList.put(ResourceConstants.CONNECTION_CREATION_RETRY_INTERVAL_IN_SECONDS, creationretryinterval);
        attrList.put(ResourceConstants.DRIVER_CLASSNAME, driverclassname);
        attrList.put(ResourceConstants.SQL_TRACE_LISTENERS, sqltracelisteners);
        attrList.put(ResourceConstants.STATEMENT_TIMEOUT_IN_SECONDS, statementtimeout);
        attrList.put(ResourceConstants.STATEMENT_LEAK_TIMEOUT_IN_SECONDS, statementLeaktimeout);
        attrList.put(ResourceConstants.STATEMENT_LEAK_RECLAIM, statementLeakreclaim.toString());
        attrList.put(ResourceConstants.STATEMENT_CACHE_SIZE, statementcachesize);
        attrList.put(ResourceConstants.LAZY_CONNECTION_ASSOCIATION, lazyconnectionassociation.toString());
        attrList.put(ResourceConstants.LAZY_CONNECTION_ENLISTMENT, lazyconnectionenlistment.toString());
        attrList.put(ResourceConstants.ASSOCIATE_WITH_THREAD, associatewiththread.toString());
        attrList.put(ResourceConstants.MATCH_CONNECTIONS, matchconnections.toString());
        attrList.put(ResourceConstants.MAX_CONNECTION_USAGE_COUNT, maxconnectionusagecount);
        attrList.put(ResourceConstants.PING, ping.toString());
        attrList.put(ResourceConstants.POOLING, pooling.toString());
        attrList.put(ResourceConstants.VALIDATION_CLASSNAME, validationclassname);
        attrList.put(ResourceConstants.WRAP_JDBC_OBJECTS, wrapjdbcobjects.toString());
        
        ResourceStatus rs;

        try {
            JDBCConnectionPoolManager connPoolMgr = new JDBCConnectionPoolManager();
            rs = connPoolMgr.create(domain.getResources(), attrList, properties, target);
        } catch(Exception e) {
            String actual = e.getMessage();
            String def = "JDBC connection pool: {0} could not be created, reason: {1}";
            report.setMessage(localStrings.getLocalString("create.jdbc.connection.pool.fail",
                    def, jdbc_connection_pool_id, actual));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        if (rs.getMessage() != null) {
                report.setMessage(rs.getMessage());
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if (rs.getMessage() == null) {
                 report.setMessage(localStrings.getLocalString("create.jdbc.connection.pool.fail",
                    "JDBC connection pool {0} creation failed", jdbc_connection_pool_id, ""));
            }
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        } else {
            //TODO only for DAS
            if ("true".equalsIgnoreCase(ping.toString())) {
                ActionReport subReport = report.addSubActionsReport();
                ParameterMap parameters = new ParameterMap();
                parameters.set("pool_name", jdbc_connection_pool_id);
                commandRunner.getCommandInvocation("ping-connection-pool", subReport, context.getSubject()).parameters(parameters).execute();
                if (ActionReport.ExitCode.FAILURE.equals(subReport.getActionExitCode())) {
                    subReport.setMessage(localStrings.getLocalString("ping.create.jdbc.connection.pool.fail",
                            "\nAttempting to ping during JDBC Connection Pool " +
                            "Creation : {0} - Failed.", jdbc_connection_pool_id));
                    subReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                } else {
                    subReport.setMessage(localStrings.getLocalString("ping.create.jdbc.connection.pool.success",
                            "\nAttempting to ping during JDBC Connection Pool " +
                            "Creation : {0} - Succeeded.", jdbc_connection_pool_id));
                }
            }
        }
        report.setActionExitCode(ec);
    }
}
