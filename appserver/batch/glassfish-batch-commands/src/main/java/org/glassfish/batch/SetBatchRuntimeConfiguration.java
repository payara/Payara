/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016] [Payara Foundation and/or its affiliates]
package org.glassfish.batch;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;


import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import org.glassfish.batch.spi.impl.GlassFishBatchValidationException;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 *
 */
@Service(name = "set-batch-runtime-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.batch.runtime.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.POST,
                path = "set-batch-runtime-configuration",
                description = "Set Batch Runtime Configuration")
})
public class SetBatchRuntimeConfiguration
    implements AdminCommand {

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Logger logger;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Inject
    protected Target targetUtil;

    @Param(name = "dataSourceLookupName", shortName = "d", optional = true)
    private String dataSourceLookupName;

    @Param(name = "executorServiceLookupName", shortName = "x", optional = true)
    private String executorServiceLookupName;
    
    @Param(name = "schemaName", shortName = "n", optional = true)
    private String schemaName;
    
    @Param(name = "tablePrefix", optional = true)
    private String tablePrefix;
    
    @Param(name = "tableSuffix", optional = true)
    private String tableSuffix;   
    
    public static final int MAX_TABLE_LENGTH=26;

    @Override
    public void execute(final AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (dataSourceLookupName == null && executorServiceLookupName == null) {
            actionReport.setMessage("Either dataSourceLookupName or executorServiceLookupName must be specified.");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            Config config = targetUtil.getConfig(target);

            BatchRuntimeConfiguration batchRuntimeConfiguration = config.getExtensionByType(BatchRuntimeConfiguration.class);
            if (batchRuntimeConfiguration != null) {
                ConfigSupport.apply(new SingleConfigCode<BatchRuntimeConfiguration>() {
                    @Override
                    public Object run(final BatchRuntimeConfiguration batchRuntimeConfigurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        boolean encounteredError = false;
                        int tableprefixlength=0;
                        int tablesuffixlength=0;
                        if (dataSourceLookupName != null ) {
                            try {
                                validateDataSourceLookupName(context, target, dataSourceLookupName);
                                batchRuntimeConfigurationProxy.setDataSourceLookupName(dataSourceLookupName);
                                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                            } catch (GlassFishBatchValidationException ex) {
                                logger.log(Level.WARNING, ex.getMessage());
                                actionReport.setMessage(dataSourceLookupName + " is not mapped to a DataSource");
                                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                throw new GlassFishBatchValidationException(dataSourceLookupName + " is not mapped to a DataSource");
                            }
                        }
                        if (executorServiceLookupName != null && !encounteredError) {
                            try {
                                validateExecutorServiceLookupName(context, target, executorServiceLookupName);
                                batchRuntimeConfigurationProxy.setExecutorServiceLookupName(executorServiceLookupName);
                                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                            } catch (GlassFishBatchValidationException ex) {
                                logger.log(Level.WARNING, ex.getMessage());
                                actionReport.setMessage("No executor service bound to name = " + executorServiceLookupName);
                                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                throw new GlassFishBatchValidationException("No executor service bound to name = " + executorServiceLookupName);
                            }
                        }
                        if (schemaName != null && !encounteredError) {
                            batchRuntimeConfigurationProxy.setSchemaName(schemaName);
                            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        }
                        
                        
                        if (tablePrefix != null && !encounteredError) {
                        	tableprefixlength= tablePrefix.length();
                            batchRuntimeConfigurationProxy.setTablePrefix(tablePrefix);
                            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        }  
                        
                        if (tableSuffix != null && !encounteredError) {
                        	tablesuffixlength= tableSuffix.length();
                            batchRuntimeConfigurationProxy.setTableSuffix(tableSuffix);
                            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        }
                        if(targetUtil.isThisDAS() && isOracle(dataSourceLookupName)){
                        	if(tablesuffixlength + tableprefixlength + MAX_TABLE_LENGTH > 30 ){
                        		actionReport.setMessage("The table name cannot be greater than 30 characters in Oracle, please amend the table prefix or suffix size "); 
                        		actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        		throw new GlassFishBatchValidationException("The table name cannot be greater than 30 characters in Oracle, please amend the table prefix or suffix size ");
                  
                        	}
                        }
                        return null;
                    }
                }, batchRuntimeConfiguration);
            }

        } catch (TransactionFailure txfEx) {
            logger.log(Level.WARNING, "Exception during command ", txfEx);
            actionReport.setMessage(txfEx.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }

    public void validateDataSourceLookupName(AdminCommandContext context, String targetName, String dsLookupName) {
        try {
            CommandRunner runner = serviceLocator.getService(CommandRunner.class);
            ActionReport subReport = context.getActionReport().addSubActionsReport();
            CommandRunner.CommandInvocation inv = runner.getCommandInvocation("list-jdbc-resources", subReport, context.getSubject());

            ParameterMap params = new ParameterMap();
            params.add("target", targetName);
            inv.parameters(params);
            inv.execute();

            Properties props = subReport.getExtraProperties();
            if (props != null) {
                if (props.get("jdbcResources") != null) {
                    List<HashMap<String, String>> map = (List<HashMap<String, String>>) props.get("jdbcResources");
                    for (HashMap<String, String> e : map) {
                        if (e.get("name").equals(dsLookupName)) {
                            return;
                        }
                    }
                }
            }
            throw new GlassFishBatchValidationException("No DataSource mapped to " + dsLookupName);
        } catch (Exception ex) {
            throw new GlassFishBatchValidationException("Exception during validation: ", ex);
        }
    }

    public void validateExecutorServiceLookupName(AdminCommandContext context, String targetName, String exeLookupName) {
        if ("concurrent/__defaultManagedExecutorService".equals(exeLookupName)) {
            return;
        }
        try {
            CommandRunner runner = serviceLocator.getService(CommandRunner.class);
            ActionReport subReport = context.getActionReport().addSubActionsReport();
            CommandRunner.CommandInvocation inv = runner.getCommandInvocation("list-managed-executor-services", subReport, context.getSubject());

            ParameterMap params = new ParameterMap();
            params.add("target", targetName);
            inv.parameters(params);
            inv.execute();

            Properties props = subReport.getExtraProperties();
            if (props != null) {
                if (props.get("managedExecutorServices") != null) {
                    List<HashMap<String, String>> map = (List<HashMap<String, String>>) props.get("managedExecutorServices");
                    for (HashMap<String, String> e : map) {
                        if (e.get("name").equals(exeLookupName)) {
                            return;
                        }
                    }
                }
            }
            throw new GlassFishBatchValidationException("No ExecutorService mapped to " + exeLookupName);
        } catch (Exception ex) {
            throw new GlassFishBatchValidationException("Exception during validation: ", ex);
        }
    }
    
    private boolean isOracle(String datasource){
    	boolean result=false;
        // this only works when targeting the DAS otherwise it is not run locally
    	try{
            InitialContext ctx = new InitialContext();
            Object object = ctx.lookup(datasource);

            if (object instanceof DataSource) {
                Connection conn = null;
                try {
                    DataSource ds = DataSource.class.cast(object);
                    conn = ds.getConnection();
                    String database = conn.getMetaData().getDatabaseProductName();

                    if (database.contains("Oracle")) {
                        result = true;
                    }
                } catch (SQLException ex) {
                    // ignore this
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            // ignore this
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            // ignore this naming exception as this has already been checked
        }
        
        return result;
       
    }
}
