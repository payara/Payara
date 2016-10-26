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
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;

/**
 * A persistence manager that is used if an incorrect datasource was configured and the datasource could not be found
 * @author steve
 */
class NullPersistenceManager implements IPersistenceManagerService {

    private final String datasourceName;
    private final String message;
    
    NullPersistenceManager(String datasourceName) {
        this(datasourceName,"Unable to find JBatch configured DataSource {0}");
    }
    
    NullPersistenceManager(String datasourceName, String message) {
        this.datasourceName = datasourceName;
        this.message = message;        
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String string) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return 0;
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String string, String string1) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return 0;
    }

    @Override
    public Map<Long, String> jobOperatorGetExternalJobInstanceData() {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String string, int i, int i1) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String string, String string1, int i, int i1) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public Timestamp jobOperatorQueryJobExecutionTimestamp(long l, TimestampType tt) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public String jobOperatorQueryJobExecutionBatchStatus(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public String jobOperatorQueryJobExecutionExitStatus(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public long jobOperatorQueryJobExecutionJobInstanceId(long l) throws NoSuchJobExecutionException {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return 0;
    }

    @Override
    public List<StepExecution> getStepExecutionsForJobExecution(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void updateBatchStatusOnly(long l, BatchStatus bs, Timestamp tmstmp) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public void markJobStarted(long l, Timestamp tmstmp) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public void updateWithFinalExecutionStatusesAndTimestamps(long l, BatchStatus bs, String string, Timestamp tmstmp) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public IJobExecution jobOperatorGetJobExecution(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public Properties getParameters(long l) throws NoSuchJobExecutionException {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public List<IJobExecution> jobOperatorGetJobExecutions(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public Set<Long> jobOperatorGetRunningExecutions(String string) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public String getJobCurrentTag(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void purge(String string) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public JobStatus getJobStatusFromExecution(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public long getJobInstanceIdByExecutionId(long l) throws NoSuchJobExecutionException {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return 0;
    }

    @Override
    public JobInstance createJobInstance(String string, String string1, String string2) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public RuntimeJobExecution createJobExecution(JobInstance ji, Properties prprts, BatchStatus bs) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public StepExecutionImpl createStepExecution(long l, StepContextImpl sci) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void updateStepExecution(StepContextImpl sci) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public void updateWithFinalPartitionAggregateStepExecution(long l, StepContextImpl sci) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public JobStatus createJobStatus(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public JobStatus getJobStatus(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void updateJobStatus(long l, JobStatus js) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public StepStatus createStepStatus(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public StepStatus getStepStatus(long l, String string) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void updateStepStatus(long l, StepStatus ss) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public String getTagName(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void updateCheckpointData(CheckpointDataKey cdk, CheckpointData cd) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public CheckpointData getCheckpointData(CheckpointDataKey cdk) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void createCheckpointData(CheckpointDataKey cdk, CheckpointData cd) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
    }

    @Override
    public long getMostRecentExecutionId(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return 0;
    }

    @Override
    public JobInstance createSubJobInstance(String string, String string1) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public RuntimeFlowInSplitExecution createFlowInSplitExecution(JobInstance ji, BatchStatus bs) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public StepExecution getStepExecutionByStepExecutionId(long l) {
        Logger.getLogger(BatchRuntimeHelper.class.getName()).log(Level.WARNING, message, datasourceName);
        return null;
    }

    @Override
    public void init(IBatchConfig ibc) {
    }

    @Override
    public void shutdown() {
    }
    
}
