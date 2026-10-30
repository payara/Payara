/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.batch;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.batch.runtime.BatchRuntime;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cleans the records of job executions from the repository.
 *
 * @author jonathan coustick
 * @since 5.25.0
 */
@Service(name = "clean-jbatch-repository")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value = {RuntimeType.INSTANCE})
public class CleanJbatchRepository implements AdminCommand {

    @Param(acceptableValues = "ALL,COMPLETED", defaultValue = "COMPLETED", optional = true)
    String status;

    @Param(optional = true, defaultValue = "1")
    int days;

    @Param(name = "jobname", primary = true, optional = false)
    String jobname;

    @Inject
    BatchRuntimeHelper batchRuntimeHelper;
    
    @Inject
    BatchRuntimeConfiguration config;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        
        //Initialises databases if they don't already exist, ignore result
        try {
            BatchRuntime.getJobOperator();
        } catch (ServiceConfigurationError error) {
            report.setMessage("Could not get JobOperator. Check if the Batch DataSource is configured properly and Check if the Database is up and running");
            report.setFailureCause(error);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (days < 1) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("The value for parameter --day must be 1 or higher.");

            return;
        }

        try {
            String dataSourceName = batchRuntimeHelper.getDataSourceLookupName();
            InitialContext ctx = new InitialContext();
            Object object = ctx.lookup(dataSourceName);
            if (object instanceof DataSource) {
                DataSource datasource = (DataSource) object;
                Feedback feedback = new Feedback();
                try (Connection conn = datasource.getConnection()) {
                    try {
                        boolean valid = conn.isValid(0);
                        if (!valid) {
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            report.setMessage("Database not accessible" );
                            return;
                        }
                    } catch (SQLException ex) {
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("Database not accessible" );
                        report.setFailureCause(ex);
                        return;
                    }
                    cleanTables(conn, feedback);
                } catch (SQLException ex) {
                    Logger.getLogger("fish.payara.batch").log(Level.SEVERE, "Error cleaning repository with table " + feedback.tableToClean, ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("Error cleaning repository" );
                    report.setFailureCause(ex);
                }

            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Invalid data source type for JBatch");
            }

        } catch (NamingException ex) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to get data source for JBatch");
            report.setFailureCause(ex);
        }
    }

    private void cleanTables(Connection connection, Feedback feedback) throws SQLException {
        try {
            connection.setAutoCommit(false);

            Timestamp threshold = determineEndTime();
            String prefix = config.getTablePrefix();
            String suffix = config.getTableSuffix();

            String tableStepStatus = prefix + "STEPSTATUS" + suffix;
            String tableJobInstanceData = prefix + "JOBINSTANCEDATA" + suffix;
            String tableExecutionInstanceData = prefix + "EXECUTIONINSTANCEDATA" + suffix;
            String tableStepExecutionInstanceData = prefix + "STEPEXECUTIONINSTANCEDATA" + suffix;
            String tableJobStatus = prefix + "JOBSTATUS" + suffix;

            String statusCheck1 = "";
            String statusCheck2 = "";
            if (status.equals("COMPLETED")) {
                statusCheck1 = " AND eid.batchstatus = 'COMPLETED'";
                statusCheck2 = " AND batchstatus = 'COMPLETED'";
                // This is not supported by IBM DB2
                // (eid.batchstatus = ? OR ? = 'ALL')
            }

            feedback.tableToClean = tableStepStatus;
            String sql = "DELETE FROM " + tableStepStatus +
                    " WHERE id IN (SELECT seid.stepexecid "
                    + "FROM " + tableJobInstanceData + " jid, " + tableExecutionInstanceData + " eid, " + tableStepExecutionInstanceData + " seid "
                    + "WHERE jid.jobinstanceid = eid.jobinstanceid AND eid.jobexecid = seid.jobexecid "
                    + "AND jid.name = ? AND eid.endtime < ? " + statusCheck1 + " )";
            PreparedStatement deleteStatement = connection.prepareStatement(sql);

            deleteStatement.setString(1, jobname);
            deleteStatement.setTimestamp(2, threshold);
            deleteStatement.execute();

            feedback.tableToClean = tableStepExecutionInstanceData;
            sql = "DELETE FROM " + tableStepExecutionInstanceData +
                    " WHERE jobexecid IN (SELECT eid.jobexecid "
                    + "FROM " + tableJobInstanceData + " jid, " + tableExecutionInstanceData + " eid WHERE jid.jobinstanceid = eid.jobinstanceid "
                    + "AND jid.name = ? AND eid.endtime < ? " + statusCheck1 + " )";
            deleteStatement = connection.prepareStatement(sql);

            deleteStatement.setString(1, jobname);
            deleteStatement.setTimestamp(2, threshold);
            deleteStatement.execute();

            feedback.tableToClean = tableExecutionInstanceData;
            sql = "DELETE FROM " + tableExecutionInstanceData +
                    " WHERE jobinstanceid IN (SELECT jid.jobinstanceid "
                    + "FROM " + tableJobInstanceData + " jid WHERE jid.name = ?) "
                    + "AND endtime < ? " + statusCheck2;
            deleteStatement = connection.prepareStatement(sql);

            deleteStatement.setString(1, jobname);
            deleteStatement.setTimestamp(2, threshold);
            deleteStatement.execute();

            feedback.tableToClean = tableJobStatus;
            deleteStatement = connection.prepareStatement("DELETE FROM " + tableJobStatus
                    + " WHERE id NOT IN (SELECT DISTINCT jobinstanceid FROM " + tableExecutionInstanceData + ")");
            deleteStatement.execute();

            feedback.tableToClean = tableJobInstanceData;
            deleteStatement = connection.prepareStatement("DELETE FROM " + tableJobInstanceData
                    + " WHERE jobinstanceid NOT IN (SELECT DISTINCT jobinstanceid FROM " + tableExecutionInstanceData + ")");
            deleteStatement.execute();

            connection.commit();
        } finally {
            connection.rollback();
        }
    }

    private Timestamp determineEndTime() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return java.sql.Timestamp.valueOf(threshold);
    }

    private static class Feedback {
        String tableToClean;
    }

}
