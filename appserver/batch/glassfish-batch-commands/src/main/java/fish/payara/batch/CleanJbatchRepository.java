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
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.batch.runtime.BatchRuntime;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

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

    @Param(optional = true)
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
        BatchRuntime.getJobOperator();
        
        try {
            String dataSourceName = batchRuntimeHelper.getDataSourceLookupName();
            InitialContext ctx = new InitialContext();
            Object object = ctx.lookup(dataSourceName);
            if (object instanceof DataSource) {
                DataSource datasource = (DataSource) object;
                try (Connection conn = datasource.getConnection()) {
                    String prefix = config.getTablePrefix();
                    String suffix = config.getTableSuffix();
                    
                    PreparedStatement deleteStatement = conn.prepareStatement("DELETE FROM " + prefix + "stepstatus" + suffix +
                            " WHERE id IN (SELECT seid.stepexecid "
                            + "FROM jobinstancedata jid, executioninstancedata eid, stepexecutioninstancedata seid "
                            + "WHERE jid.jobinstanceid = eid.jobinstanceid AND eid.jobexecid = seid.jobexecid "
                            + "AND jid.name = ? AND eid.endtime < DATEADD('DAY',?, NOW()) AND (eid.batchstatus = ? OR ? = 'ALL'))");
                    
                    deleteStatement.setString(1, jobname);
                    deleteStatement.setInt(2, -days);
                    deleteStatement.setString(3, status);
                    deleteStatement.setString(4, status);
                    deleteStatement.execute();

                    deleteStatement = conn.prepareStatement("DELETE FROM " + prefix + "stepexecutioninstancedata" + suffix +
                            " WHERE jobexecid IN (SELECT eid.jobexecid "
                            + "FROM jobinstancedata jid, executioninstancedata eid WHERE jid.jobinstanceid = eid.jobinstanceid "
                            + "AND jid.name = ? AND eid.endtime < DATEADD('DAY',?, NOW()) AND (eid.batchstatus = ? OR ? = 'ALL'))");
                    deleteStatement.setString(1, jobname);
                    deleteStatement.setInt(2, -days);
                    deleteStatement.setString(3, status);
                    deleteStatement.setString(4, status);
                    deleteStatement.execute();

                    deleteStatement = conn.prepareStatement("DELETE FROM " + prefix + "executioninstancedata" + suffix +
                            " WHERE jobinstanceid IN (SELECT jid.jobinstanceid "
                            + "FROM jobinstancedata jid, executioninstancedata eid WHERE jid.jobinstanceid = eid.jobinstanceid "
                            + "AND jid.name = ? AND eid.endtime < DATEADD('DAY',?, NOW()) AND (eid.batchstatus = ? OR ? = 'ALL'))");
                    deleteStatement.setString(1, jobname);
                    deleteStatement.setInt(2, -days);
                    deleteStatement.setString(3, status);
                    deleteStatement.setString(4, status);
                    deleteStatement.execute();

                    deleteStatement = conn.prepareStatement("DELETE FROM " + prefix + "jobstatus" + suffix
                            + " WHERE id NOT IN (SELECT DISTINCT jobinstanceid FROM executioninstancedata)");
                    deleteStatement.execute();

                    deleteStatement = conn.prepareStatement("DELETE FROM " + prefix + "jobinstancedata " + suffix
                            + " WHERE jobinstanceid NOT IN (SELECT DISTINCT jobinstanceid FROM executioninstancedata)");
                    deleteStatement.execute();
                } catch (SQLException ex) {
                    Logger.getLogger("fish.payara.batch").log(Level.SEVERE, "Error cleaning repository", ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("Error cleaning repository");
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

}
