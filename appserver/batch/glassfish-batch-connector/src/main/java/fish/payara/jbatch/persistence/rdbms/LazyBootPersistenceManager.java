/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.spi.services.IBatchConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;

/**
 * As the persistence manager is required at boot of the JBatch infrastructure
 * previously we had to query the database to determine the correct subclass to use
 * at boot which hurts server boot time. This class prevents that by proxying calls 
 * through itself and only accessing the database when strictly necessay
 * @author steve
 */
public class LazyBootPersistenceManager implements IPersistenceManagerService {
    
    private IPersistenceManagerService lazyProxy;
    private IBatchConfig ibc;

    @Override
    public int jobOperatorGetJobInstanceCount(String string) {
       return lazyProxy.jobOperatorGetJobInstanceCount(string);
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String string, String string1) {
        return lazyProxy.jobOperatorGetJobInstanceCount(string, string1);
    }

    @Override
    public Map<Long, String> jobOperatorGetExternalJobInstanceData() {
        return lazyProxy.jobOperatorGetExternalJobInstanceData();
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String string, int i, int i1) {
        return lazyProxy.jobOperatorGetJobInstanceIds(string, i, i1);
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String string, String string1, int i, int i1) {
        return lazyProxy.jobOperatorGetJobInstanceIds(string, string1, i, i1);
    }

    @Override
    public Timestamp jobOperatorQueryJobExecutionTimestamp(long l, TimestampType tt) {
        return lazyProxy.jobOperatorQueryJobExecutionTimestamp(l, tt);
    }

    @Override
    public String jobOperatorQueryJobExecutionBatchStatus(long l) {
        return lazyProxy.jobOperatorQueryJobExecutionBatchStatus(l);
     }

    @Override
    public String jobOperatorQueryJobExecutionExitStatus(long l) {
        return lazyProxy.jobOperatorQueryJobExecutionExitStatus(l);
    }

    @Override
    public long jobOperatorQueryJobExecutionJobInstanceId(long l) throws NoSuchJobExecutionException {
        return lazyProxy.jobOperatorQueryJobExecutionJobInstanceId(l);
    }

    @Override
    public List<StepExecution> getStepExecutionsForJobExecution(long l) {
        return lazyProxy.getStepExecutionsForJobExecution(l);
    }

    @Override
    public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(long l) {
        return lazyProxy.getMostRecentStepExecutionsForJobInstance(l) ;
    }

    @Override
    public void updateBatchStatusOnly(long l, BatchStatus bs, Timestamp tmstmp) {
        lazyProxy.updateBatchStatusOnly(l, bs, tmstmp)  ;
    }

    @Override
    public void markJobStarted(long l, Timestamp tmstmp) {
        lazyProxy.markJobStarted(l, tmstmp);
    }

    @Override
    public void updateWithFinalExecutionStatusesAndTimestamps(long l, BatchStatus bs, String string, Timestamp tmstmp) {
        lazyProxy.updateWithFinalExecutionStatusesAndTimestamps(l, bs, string, tmstmp);
    }

    @Override
    public IJobExecution jobOperatorGetJobExecution(long l) {
        return lazyProxy.jobOperatorGetJobExecution(l);
    }

    @Override
    public Properties getParameters(long l) throws NoSuchJobExecutionException {
        return lazyProxy.getParameters(l);
    }

    @Override
    public List<IJobExecution> jobOperatorGetJobExecutions(long l) {
        return lazyProxy.jobOperatorGetJobExecutions(l);
    }

    @Override
    public Set<Long> jobOperatorGetRunningExecutions(String string) {
        return lazyProxy.jobOperatorGetRunningExecutions(string);
    }

    @Override
    public String getJobCurrentTag(long l) {
        return lazyProxy.getJobCurrentTag(l);
    }

    @Override
    public void purge(String string) {
        lazyProxy.purge(string);
    }

    @Override
    public JobStatus getJobStatusFromExecution(long l) {
        return lazyProxy.getJobStatusFromExecution(l);
    }

    @Override
    public long getJobInstanceIdByExecutionId(long l) throws NoSuchJobExecutionException {
        return lazyProxy.getJobInstanceIdByExecutionId(l);
    }

    @Override
    public JobInstance createJobInstance(String string, String string1, String string2) {
        return lazyProxy.createJobInstance(string, string1, string2);
    }

    @Override
    public RuntimeJobExecution createJobExecution(JobInstance ji, Properties prprts, BatchStatus bs) {
        return lazyProxy.createJobExecution(ji, prprts, bs);
    }

    @Override
    public StepExecutionImpl createStepExecution(long l, StepContextImpl sci) {
        return lazyProxy.createStepExecution(l, sci);
    }

    @Override
    public void updateStepExecution(StepContextImpl sci) {
        lazyProxy.updateStepExecution(sci);
    }

    @Override
    public void updateWithFinalPartitionAggregateStepExecution(long l, StepContextImpl sci) {
        lazyProxy.updateWithFinalPartitionAggregateStepExecution(l, sci);
    }

    @Override
    public JobStatus createJobStatus(long l) {
        return lazyProxy.createJobStatus(l);
    }

    @Override
    public JobStatus getJobStatus(long l) {
        return lazyProxy.getJobStatus(l);
    }

    @Override
    public void updateJobStatus(long l, JobStatus js) {
        lazyProxy.updateJobStatus(l, js);
    }

    @Override
    public StepStatus createStepStatus(long l) {
        return lazyProxy.createStepStatus(l);
    }

    @Override
    public StepStatus getStepStatus(long l, String string) {
        return lazyProxy.getStepStatus(l, string);
    }

    @Override
    public void updateStepStatus(long l, StepStatus ss) {
        lazyProxy.updateStepStatus(l, ss);
    }

    @Override
    public String getTagName(long l) {
        return lazyProxy.getTagName(l);
    }

    @Override
    public void updateCheckpointData(CheckpointDataKey cdk, CheckpointData cd) {
        lazyProxy.updateCheckpointData(cdk, cd);
    }

    @Override
    public CheckpointData getCheckpointData(CheckpointDataKey cdk) {
        return lazyProxy.getCheckpointData(cdk);
    }

    @Override
    public void createCheckpointData(CheckpointDataKey cdk, CheckpointData cd) {
        lazyProxy.createCheckpointData(cdk, cd);
    }

    @Override
    public long getMostRecentExecutionId(long l) {
        return lazyProxy.getMostRecentExecutionId(l);
    }

    @Override
    public JobInstance createSubJobInstance(String string, String string1) {
        return lazyProxy.createSubJobInstance(string, string1);
    }

    @Override
    public RuntimeFlowInSplitExecution createFlowInSplitExecution(JobInstance ji, BatchStatus bs) {
        return lazyProxy.createFlowInSplitExecution(ji, bs);
    }

    @Override
    public StepExecution getStepExecutionByStepExecutionId(long l) {
        return lazyProxy.getStepExecutionByStepExecutionId(l);
    }

    @Override
    public void init(IBatchConfig ibc) {
        this.ibc = ibc;
        internalInit(ibc);
 }

    private void internalInit(IBatchConfig ibc1) {
        try {
            // this is the default
            String dataSourceName = ibc1.getDatabaseConfigurationBean().getJndiName();
            InitialContext ctx = new InitialContext();
            Object object = ctx.lookup(dataSourceName);
            //check whether the referenced JNDI entry is a DataSource
            if (object instanceof DataSource) {
                Connection conn = null;
                try {
                    DataSource ds = DataSource.class.cast(object);
                    conn = ds.getConnection();
                    String database = conn.getMetaData().getDatabaseProductName();
                    if (database.contains("Derby")) {
                        lazyProxy = new JBatchJDBCPersistenceManager();
                        lazyProxy.init(ibc1);
                    } else if (database.contains("MySQL")) {
                        lazyProxy = new MySqlPersistenceManager();
                        lazyProxy.init(ibc1);
                    } else if (database.contains("Oracle")) {
                        lazyProxy = new OraclePersistenceManager();
                        lazyProxy.init(ibc1);
                    } else if (database.contains("PostgreSQL")) {
                        lazyProxy = new PostgresPersistenceManager();
                        lazyProxy.init(ibc1);
                    } else if (database.contains("DB2")) {
                        lazyProxy = new DB2PersistenceManager();
                        lazyProxy.init(ibc1);
                    } else if (database.contains("Microsoft SQL Server")) {
                        lazyProxy = new SQLServerPersistenceManager();
                        lazyProxy.init(ibc1);
                    } else {
                        lazyProxy = new NullPersistenceManager(ibc.getDatabaseConfigurationBean().getJndiName(), "{0} Datasource database type is not recognised as supported it's type is " + database);
                    }
                }catch (SQLException ex) {
                    Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.SEVERE, "Failed to get connecion to determine database type", ex);
                    lazyProxy = new NullPersistenceManager(dataSourceName,"{0} is not configured correctly. JBatch could not get a connection to the database");
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.SEVERE, "Failed to close connection", ex);
                        }
                    }
                }
            } else {
                lazyProxy = new NullPersistenceManager(dataSourceName,"Configured JNDI Name {0} for JBatch Datasource is NOT a datasource");
            }
        }catch (NamingException ex) {
            Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, "Unable to find JBatch configured DataSource", ex);
            lazyProxy = new NullPersistenceManager(ibc.getDatabaseConfigurationBean().getJndiName());
        }
    }

    @Override
    public void shutdown() {
        lazyProxy.shutdown();
    }
}
