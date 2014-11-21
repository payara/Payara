/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.jbatch;

import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.impl.PartitionedStepBuilder;
import com.ibm.jbatch.container.jobinstance.JobOperatorJobExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.util.TCCLObjectInputStream;
import com.ibm.jbatch.spi.DatabaseConfigurationBean;
import com.ibm.jbatch.spi.services.IBatchConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author steve
 */
public class JBatchJDBCPersistenceManager implements IPersistenceManagerService {

    protected DataSource dataSource;
    private final static Logger logger = Logger.getLogger(JBatchJDBCPersistenceManager.class.getName());
    private String schema;

    @Override
    public int jobOperatorGetJobInstanceCount(String jobName) {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        int count;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select count(jobinstanceid) as jobinstancecount from jobinstancedata where name = ?");
            statement.setString(1, jobName);
            rs = statement.executeQuery();
            rs.next();
            count = rs.getInt("jobinstancecount");

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }
        return count;
    }

    @Override
    public int jobOperatorGetJobInstanceCount(String jobName, String appTag) {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        int count;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select count(jobinstanceid) as jobinstancecount from jobinstancedata where name = ? and apptag = ?");
            statement.setString(1, jobName);
            statement.setString(2, appTag);
            rs = statement.executeQuery();
            rs.next();
            count = rs.getInt("jobinstancecount");

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }
        return count;
    }

    @Override
    public Map<Long, String> jobOperatorGetExternalJobInstanceData() {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        HashMap<Long, String> data = new HashMap<Long, String>();

        try {
            conn = getConnection();

            // Filter out 'subjob' parallel execution entries which start with the special character
            final String filter = "not like '" + PartitionedStepBuilder.JOB_ID_SEPARATOR + "%'";

            statement = conn.prepareStatement("select distinct jobinstanceid, name from jobinstancedata where name " + filter);
            rs = statement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("jobinstanceid");
                String name = rs.getString("name");
                data.put(id, name);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return data;
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String jobName, int start, int count) {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<Long> data = new ArrayList<Long>();

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select jobinstanceid from jobinstancedata where name = ? order by jobinstanceid desc");
            statement.setObject(1, jobName);
            rs = statement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("jobinstanceid");
                data.add(id);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        if (data.size() > 0) {
            try {
                return data.subList(start, start + count);
            } catch (IndexOutOfBoundsException oobEx) {
                return data.subList(start, data.size());
            }
        } else {
            return data;
        }
    }

    @Override
    public List<Long> jobOperatorGetJobInstanceIds(String jobName, String appTag, int start, int count) {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<Long> data = new ArrayList<Long>();

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select jobinstanceid from jobinstancedata where name = ? and apptag = ? order by jobinstanceid desc");
            statement.setObject(1, jobName);
            statement.setObject(2, appTag);
            rs = statement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("jobinstanceid");
                data.add(id);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        if (data.size() > 0) {
            try {
                return data.subList(start, start + count);
            } catch (IndexOutOfBoundsException oobEx) {
                return data.subList(start, data.size());
            }
        } else {
            return data;
        }
    }

    @Override
    public Timestamp jobOperatorQueryJobExecutionTimestamp(long key, TimestampType timestampType) {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        Timestamp createTimestamp = null;
        Timestamp endTimestamp = null;
        Timestamp updateTimestamp = null;
        Timestamp startTimestamp = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select createtime, endtime, updatetime, starttime from executioninstancedata where jobexecid = ?");
            statement.setObject(1, key);
            rs = statement.executeQuery();
            while (rs.next()) {
                createTimestamp = rs.getTimestamp(1);
                endTimestamp = rs.getTimestamp(2);
                updateTimestamp = rs.getTimestamp(3);
                startTimestamp = rs.getTimestamp(4);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        if (timestampType.equals(TimestampType.CREATE)) {
            return createTimestamp;
        } else if (timestampType.equals(TimestampType.END)) {
            return endTimestamp;
        } else if (timestampType.equals(TimestampType.LAST_UPDATED)) {
            return updateTimestamp;
        } else if (timestampType.equals(TimestampType.STARTED)) {
            return startTimestamp;
        } else {
            throw new IllegalArgumentException("Unexpected enum value.");
        }
    }

    @Override
    public String jobOperatorQueryJobExecutionBatchStatus(long key) {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        String status = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select batchstatus from executioninstancedata where jobexecid = ?");
            statement.setLong(1, key);
            rs = statement.executeQuery();
            while (rs.next()) {
                status = rs.getString(1);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return status;
    }

    @Override
    public String jobOperatorQueryJobExecutionExitStatus(long key) {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        String status = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select exitstatus from executioninstancedata where jobexecid = ?");
            statement.setLong(1, key);
            rs = statement.executeQuery();
            while (rs.next()) {
                status = rs.getString(1);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return status;
    }

    public long jobOperatorQueryJobExecutionJobInstanceId(long executionID) throws NoSuchJobExecutionException {

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        long jobinstanceID = 0;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select jobinstanceid from executioninstancedata where jobexecid = ?");
            statement.setLong(1, executionID);
            rs = statement.executeQuery();
            if (rs.next()) {
                jobinstanceID = rs.getLong("jobinstanceid");
            } else {
                String msg = "Did not find job instance associated with executionID =" + executionID;
                logger.fine(msg);
                throw new NoSuchJobExecutionException(msg);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return jobinstanceID;
    }

    @Override
    public List<StepExecution> getStepExecutionsForJobExecution(long execid) {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        long jobexecid = 0;
        long stepexecid = 0;
        String stepname = null;
        String batchstatus = null;
        String exitstatus = null;
        Exception ex = null;
        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;
        Timestamp startTS = null;
        Timestamp endTS = null;
        StepExecutionImpl stepEx = null;
        ObjectInputStream objectIn = null;

        List<StepExecution> data = new ArrayList<StepExecution>();

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select * from stepexecutioninstancedata where jobexecid = ?");
            statement.setLong(1, execid);
            rs = statement.executeQuery();
            while (rs.next()) {
                jobexecid = rs.getLong("jobexecid");
                stepexecid = rs.getLong("stepexecid");
                stepname = rs.getString("stepname");
                batchstatus = rs.getString("batchstatus");
                exitstatus = rs.getString("exitstatus");
                readCount = rs.getLong("readcount");
                writeCount = rs.getLong("writecount");
                commitCount = rs.getLong("commitcount");
                rollbackCount = rs.getLong("rollbackcount");
                readSkipCount = rs.getLong("readskipcount");
                processSkipCount = rs.getLong("processskipcount");
                filterCount = rs.getLong("filtercount");
                writeSkipCount = rs.getLong("writeSkipCount");
                startTS = rs.getTimestamp("startTime");
                endTS = rs.getTimestamp("endTime");
                // get the object based data
                Serializable persistentData = null;
                byte[] pDataBytes = rs.getBytes("persistentData");
                if (pDataBytes != null) {
                    objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
                    persistentData = (Serializable) objectIn.readObject();
                }

                stepEx = new StepExecutionImpl(jobexecid, stepexecid);

                stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                stepEx.setExitStatus(exitstatus);
                stepEx.setStepName(stepname);
                stepEx.setReadCount(readCount);
                stepEx.setWriteCount(writeCount);
                stepEx.setCommitCount(commitCount);
                stepEx.setRollbackCount(rollbackCount);
                stepEx.setReadSkipCount(readSkipCount);
                stepEx.setProcessSkipCount(processSkipCount);
                stepEx.setFilterCount(filterCount);
                stepEx.setWriteSkipCount(writeSkipCount);
                stepEx.setStartTime(startTS);
                stepEx.setEndTime(endTS);
                stepEx.setPersistentUserData(persistentData);

                logger.fine("BatchStatus: " + batchstatus + " | StepName: " + stepname + " | JobExecID: " + jobexecid + " | StepExecID: " + stepexecid);

                data.add(stepEx);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return data;
    }

    public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(long instanceId) {

        Map<String, StepExecution> data = new HashMap<String, StepExecution>();

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        long jobexecid = 0;
        long stepexecid = 0;
        String stepname = null;
        String batchstatus = null;
        String exitstatus = null;
        Exception ex = null;
        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;
        Timestamp startTS = null;
        Timestamp endTS = null;
        StepExecutionImpl stepEx = null;
        ObjectInputStream objectIn = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select A.* from stepexecutioninstancedata as A inner join executioninstancedata as B on A.jobexecid = B.jobexecid where B.jobinstanceid = ? order by A.stepexecid desc");
            statement.setLong(1, instanceId);
            rs = statement.executeQuery();
            while (rs.next()) {
                stepname = rs.getString("stepname");
                if (data.containsKey(stepname)) {
                    continue;
                } else {

                    jobexecid = rs.getLong("jobexecid");
                    batchstatus = rs.getString("batchstatus");
                    exitstatus = rs.getString("exitstatus");
                    readCount = rs.getLong("readcount");
                    writeCount = rs.getLong("writecount");
                    commitCount = rs.getLong("commitcount");
                    rollbackCount = rs.getLong("rollbackcount");
                    readSkipCount = rs.getLong("readskipcount");
                    processSkipCount = rs.getLong("processskipcount");
                    filterCount = rs.getLong("filtercount");
                    writeSkipCount = rs.getLong("writeSkipCount");
                    startTS = rs.getTimestamp("startTime");
                    endTS = rs.getTimestamp("endTime");
                    // get the object based data
                    Serializable persistentData = null;
                    byte[] pDataBytes = rs.getBytes("persistentData");
                    if (pDataBytes != null) {
                        objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
                        persistentData = (Serializable) objectIn.readObject();
                    }

                    stepEx = new StepExecutionImpl(jobexecid, stepexecid);

                    stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                    stepEx.setExitStatus(exitstatus);
                    stepEx.setStepName(stepname);
                    stepEx.setReadCount(readCount);
                    stepEx.setWriteCount(writeCount);
                    stepEx.setCommitCount(commitCount);
                    stepEx.setRollbackCount(rollbackCount);
                    stepEx.setReadSkipCount(readSkipCount);
                    stepEx.setProcessSkipCount(processSkipCount);
                    stepEx.setFilterCount(filterCount);
                    stepEx.setWriteSkipCount(writeSkipCount);
                    stepEx.setStartTime(startTS);
                    stepEx.setEndTime(endTS);
                    stepEx.setPersistentUserData(persistentData);

                    data.put(stepname, stepEx);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return data;
    }

    @Override
    public void updateBatchStatusOnly(long key, BatchStatus batchStatus, Timestamp updatets) {
        Connection conn = null;
        PreparedStatement statement = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oout = null;
        byte[] b;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("update executioninstancedata set batchstatus = ?, updatetime = ? where jobexecid = ?");
            statement.setString(1, batchStatus.name());
            statement.setTimestamp(2, updatets);
            statement.setLong(3, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenceException(e);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            if (oout != null) {
                try {
                    oout.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            cleanupConnection(conn, null, statement);
        }
    }

    public void markJobStarted(long key, Timestamp startTS) {
        Connection conn = null;
        PreparedStatement statement = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oout = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("update executioninstancedata set batchstatus = ?, starttime = ?, updatetime = ? where jobexecid = ?");

            statement.setString(1, BatchStatus.STARTED.name());
            statement.setTimestamp(2, startTS);
            statement.setTimestamp(3, startTS);
            statement.setLong(4, key);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenceException(e);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            if (oout != null) {
                try {
                    oout.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            cleanupConnection(conn, null, statement);
        }
    }

    @Override
    public void updateWithFinalExecutionStatusesAndTimestamps(long key,
            BatchStatus batchStatus, String exitStatus, Timestamp updatets) {
        // TODO Auto-generated methddod stub
        Connection conn = null;
        PreparedStatement statement = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oout = null;
        byte[] b;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("update executioninstancedata set batchstatus = ?, exitstatus = ?, endtime = ?, updatetime = ? where jobexecid = ?");

            statement.setString(1, batchStatus.name());
            statement.setString(2, exitStatus);
            statement.setTimestamp(3, updatets);
            statement.setTimestamp(4, updatets);
            statement.setLong(5, key);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenceException(e);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            if (oout != null) {
                try {
                    oout.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            cleanupConnection(conn, null, statement);
        }

    }

    @Override
    public IJobExecution jobOperatorGetJobExecution(long jobExecutionId) {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        IJobExecution jobEx = null;
        ObjectInputStream objectIn = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select A.jobexecid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.jobinstanceid, A.batchstatus, A.exitstatus, B.name from executioninstancedata as A inner join jobinstancedata as B on A.jobinstanceid = B.jobinstanceid where jobexecid = ?");
            statement.setLong(1, jobExecutionId);
            rs = statement.executeQuery();

            jobEx = (rs.next()) ? readJobExecutionRecord(rs) : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            cleanupConnection(conn, rs, statement);
        }
        return jobEx;
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        Properties props = null;
        ObjectInputStream objectIn = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select parameters from executioninstancedata where jobexecid = ?");
            statement.setLong(1, executionId);
            rs = statement.executeQuery();

            if (rs.next()) {
                // get the object based data
                byte[] buf = rs.getBytes("parameters");
                props = (Properties) deserializeObject(buf);
            } else {
                String msg = "Did not find table entry for executionID =" + executionId;
                logger.fine(msg);
                throw new NoSuchJobExecutionException(msg);
            }

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            cleanupConnection(conn, rs, statement);
        }

        return props;

    }

    @Override
    public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        List<IJobExecution> data = new ArrayList<IJobExecution>();
        ObjectInputStream objectIn = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select A.jobexecid, A.jobinstanceid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.batchstatus, A.exitstatus, B.name from executioninstancedata as A inner join jobinstancedata as B ON A.jobinstanceid = B.jobinstanceid where A.jobinstanceid = ?");
            statement.setLong(1, jobInstanceId);
            rs = statement.executeQuery();
            while (rs.next()) {
                data.add(readJobExecutionRecord(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    throw new PersistenceException(e);
                }
            }
            cleanupConnection(conn, rs, statement);
        }
        return data;
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        long jobexecid = 0;
        long stepexecid = 0;
        String stepname = null;
        String batchstatus = null;
        String exitstatus = null;
        Exception ex = null;
        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;
        Timestamp startTS = null;
        Timestamp endTS = null;
        StepExecutionImpl stepEx = null;
        ObjectInputStream objectIn = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select * from stepexecutioninstancedata where stepexecid = ?");
            statement.setLong(1, stepExecId);
            rs = statement.executeQuery();
            while (rs.next()) {
                jobexecid = rs.getLong("jobexecid");
                stepexecid = rs.getLong("stepexecid");
                stepname = rs.getString("stepname");
                batchstatus = rs.getString("batchstatus");
                exitstatus = rs.getString("exitstatus");
                readCount = rs.getLong("readcount");
                writeCount = rs.getLong("writecount");
                commitCount = rs.getLong("commitcount");
                rollbackCount = rs.getLong("rollbackcount");
                readSkipCount = rs.getLong("readskipcount");
                processSkipCount = rs.getLong("processskipcount");
                filterCount = rs.getLong("filtercount");
                writeSkipCount = rs.getLong("writeSkipCount");
                startTS = rs.getTimestamp("startTime");
                endTS = rs.getTimestamp("endTime");
                // get the object based data
                Serializable persistentData = null;
                byte[] pDataBytes = rs.getBytes("persistentData");
                if (pDataBytes != null) {
                    objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
                    persistentData = (Serializable) objectIn.readObject();
                }

                stepEx = new StepExecutionImpl(jobexecid, stepexecid);

                stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                stepEx.setExitStatus(exitstatus);
                stepEx.setStepName(stepname);
                stepEx.setReadCount(readCount);
                stepEx.setWriteCount(writeCount);
                stepEx.setCommitCount(commitCount);
                stepEx.setRollbackCount(rollbackCount);
                stepEx.setReadSkipCount(readSkipCount);
                stepEx.setProcessSkipCount(processSkipCount);
                stepEx.setFilterCount(filterCount);
                stepEx.setWriteSkipCount(writeSkipCount);
                stepEx.setStartTime(startTS);
                stepEx.setEndTime(endTS);
                stepEx.setPersistentUserData(persistentData);

                logger.fine("stepExecution BatchStatus: " + batchstatus + " StepName: " + stepname);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return stepEx;
    }

    @Override
    public void init(IBatchConfig batchConfig) {
        String jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
        schema = batchConfig.getDatabaseConfigurationBean().getSchema();

        logger.config("JNDI name = " + jndiName);

        if (jndiName == null || jndiName.equals("")) {
            throw new BatchContainerServiceException("JNDI name is not defined.");
        }

        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(jndiName);

        } catch (NamingException e) {
            logger.severe("Lookup failed for JNDI name: " + jndiName
                    + ".  One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
            throw new BatchContainerServiceException(e);
        }

    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the database connection and sets it to the default schema JBATCH
     * or the schema defined in batch-config.
     *
     * @throws SQLException
     */
    protected Connection getConnection() throws SQLException {
        Connection connection = null;

        logger.finest("J2EE mode, getting connection from data source");
        connection = dataSource.getConnection();
        logger.finest("autocommit=" + connection.getAutoCommit());
        setSchemaOnConnection(connection);
        return connection;
    }

    protected void setSchemaOnConnection(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ps = conn.prepareStatement("SET SCHEMA ?");
        ps.setString(1, schema);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * closes connection, result set and statement
     *
     * @param conn - connection object to close
     * @param rs - result set object to close
     * @param statement - statement object to close
     */
    protected void cleanupConnection(Connection conn, ResultSet rs, PreparedStatement statement) {

        logger.logp(Level.FINEST, this.getClass().getName(), "cleanupConnection", "Entering", new Object[]{conn, rs == null ? "<null>" : rs, statement == null ? "<null>" : statement});

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new PersistenceException(e);
                }
            }
        }
        logger.logp(Level.FINEST, this.getClass().getName(), "cleanupConnection", "Exiting");
    }

    /**
     * closes connection and statement
     *
     * @param conn - connection object to close
     * @param statement - statement object to close
     */
    protected void cleanupConnection(Connection conn, PreparedStatement statement) {

        logger.logp(Level.FINEST, this.getClass().getName(), "cleanupConnection", "Entering", new Object[]{conn, statement});

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new PersistenceException(e);
                }
            }
        }
        logger.logp(Level.FINEST, this.getClass().getName(), "cleanupConnection", "Exiting");
    }

    /**
     * This method is used to serialized an object saved into a table BLOB
     * field.
     *
     * @param theObject the object to be serialized
     * @return a object byte array
     * @throws IOException
     */
    protected byte[] serializeObject(Serializable theObject) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(baos);
        oout.writeObject(theObject);
        byte[] data = baos.toByteArray();
        baos.close();
        oout.close();

        return data;
    }

    /**
     * This method is used to de-serialized a table BLOB field to its original
     * object form.
     *
     * @param buffer the byte array save a BLOB
     * @return the object saved as byte array
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected Serializable deserializeObject(byte[] buffer) throws IOException, ClassNotFoundException {

        Serializable theObject = null;
        ObjectInputStream objectIn = null;

        if (buffer != null) {
            objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer));
            theObject = (Serializable) objectIn.readObject();
            objectIn.close();
        }
        return theObject;
    }

    protected IJobExecution readJobExecutionRecord(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        if (rs == null) {
            return null;
        }

        JobOperatorJobExecution retMe
                = new JobOperatorJobExecution(rs.getLong("jobexecid"), rs.getLong("jobinstanceid"));

        retMe.setCreateTime(rs.getTimestamp("createtime"));
        retMe.setStartTime(rs.getTimestamp("starttime"));
        retMe.setEndTime(rs.getTimestamp("endtime"));
        retMe.setLastUpdateTime(rs.getTimestamp("updatetime"));

        retMe.setJobParameters((Properties) deserializeObject(rs.getBytes("parameters")));

        retMe.setBatchStatus(rs.getString("batchstatus"));
        retMe.setExitStatus(rs.getString("exitstatus"));
        retMe.setJobName(rs.getString("name"));

        return retMe;
    }

}
