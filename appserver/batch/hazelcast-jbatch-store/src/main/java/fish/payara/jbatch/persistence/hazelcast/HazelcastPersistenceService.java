/*
    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2014 C2B2 Consulting Limited and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.jbatch.persistence.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.jobinstance.JobInstanceImpl;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author steve
 */
public class HazelcastPersistenceService implements IPersistenceManagerService{
    
    public final static String JOB_INSTANCE_MAP = "JobInstanceMap";
    public final static String CHECKPOINTMAP = "CheckpointMap";
    private IMap jobInstanceMap;
    private IMap checkpointMAP;
    private HazelcastInstance theInstance;
    private IdGenerator jobInstanceIdGenerator;
    private IdGenerator checkpointIdGenerator;

    @Override
    public int jobOperatorGetJobInstanceCount(String jobName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String jobName, String appTag) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<Long, String> jobOperatorGetExternalJobInstanceData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String jobName, int start, int count) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String jobName, String appTag, int start, int count) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Timestamp jobOperatorQueryJobExecutionTimestamp(long key, TimestampType timetype) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String jobOperatorQueryJobExecutionBatchStatus(long key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String jobOperatorQueryJobExecutionExitStatus(long key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long jobOperatorQueryJobExecutionJobInstanceId(long executionID) throws NoSuchJobExecutionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<StepExecution> getStepExecutionsForJobExecution(long execid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(long instanceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateBatchStatusOnly(long executionId, BatchStatus batchStatus, Timestamp timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void markJobStarted(long key, Timestamp startTS) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateWithFinalExecutionStatusesAndTimestamps(long key, BatchStatus batchStatus, String exitStatus, Timestamp updatets) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IJobExecution jobOperatorGetJobExecution(long jobExecutionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Long> jobOperatorGetRunningExecutions(String jobName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getJobCurrentTag(long jobInstanceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void purge(String apptag) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JobStatus getJobStatusFromExecution(long executionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getJobInstanceIdByExecutionId(long executionId) throws NoSuchJobExecutionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JobInstance createJobInstance(String name, String apptag, String jobXml) {
        long id = jobInstanceIdGenerator.newId();
        JobInstanceImpl result = new JobInstanceImpl(id);
        return null;
    }

    @Override
    public RuntimeJobExecution createJobExecution(JobInstance jobInstance, Properties jobParameters, BatchStatus batchStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StepExecutionImpl createStepExecution(long jobExecId, StepContextImpl stepContext) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateStepExecution(StepContextImpl stepContext) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateWithFinalPartitionAggregateStepExecution(long rootJobExecutionId, StepContextImpl stepContext) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JobStatus createJobStatus(long jobInstanceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JobStatus getJobStatus(long instanceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateJobStatus(long instanceId, JobStatus jobStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StepStatus createStepStatus(long stepExecId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StepStatus getStepStatus(long instanceId, String stepName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateStepStatus(long stepExecutionId, StepStatus stepStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTagName(long jobExecutionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateCheckpointData(CheckpointDataKey key, CheckpointData value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CheckpointData getCheckpointData(CheckpointDataKey key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createCheckpointData(CheckpointDataKey key, CheckpointData value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getMostRecentExecutionId(long jobInstanceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JobInstance createSubJobInstance(String name, String apptag) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RuntimeFlowInSplitExecution createFlowInSplitExecution(JobInstance jobInstance, BatchStatus batchStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StepExecution getStepExecutionByStepExecutionId(long stepExecId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void init(IBatchConfig batchConfig) {
        try {
            String hazelcastJNDIName = batchConfig.getDatabaseConfigurationBean().getJndiName();
            InitialContext ctx = new InitialContext();
            theInstance = HazelcastInstance.class.cast(ctx.lookup(hazelcastJNDIName));
            jobInstanceMap = theInstance.getMap(JOB_INSTANCE_MAP);
            checkpointMAP = theInstance.getMap(CHECKPOINTMAP);
            jobInstanceIdGenerator = theInstance.getIdGenerator(JOB_INSTANCE_MAP+"ID");
            checkpointIdGenerator = theInstance.getIdGenerator(CHECKPOINTMAP + "ID");
        } catch (NamingException ex) {
            Logger.getLogger(HazelcastPersistenceService.class.getName()).log(Level.SEVERE, "Unable to find the Hazelcast Instance for JBatch Persistence", ex);
        }
    }

    @Override
    public void shutdown() {
    }
    
}
