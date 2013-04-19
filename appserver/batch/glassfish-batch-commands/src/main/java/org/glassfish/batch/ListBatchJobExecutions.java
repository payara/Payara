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
package org.glassfish.batch;

import com.ibm.jbatch.spi.TaggedJobExecution;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.batch.operations.*;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import java.util.*;
import java.util.logging.Level;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
@Service(name = "_ListBatchJobExecutions")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("_ListBatchJobExecutions")
@ExecuteOn(value = {RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "_ListBatchJobExecutions",
                description = "_List Batch Job Executions")
})
public class ListBatchJobExecutions
    extends AbstractLongListCommand {

    private static final String JOB_NAME = "jobName";

    private static final String EXECUTION_ID = "executionId";

    private static final String BATCH_STATUS = "batchStatus";

    private static final String EXIT_STATUS = "exitStatus";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    private static final String JOB_PARAMETERS = "jobParameters";

    private static final String STEP_COUNT = "stepCount";

    @Param(name = "executionid", shortName = "x", optional = true)
    Long executionId;

    @Param(primary = true, optional = true)
    Long instanceId;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps)
        throws Exception {

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        List<Map<String, Object>> jobExecutions = new ArrayList<>();
        extraProps.put("listBatchJobExecutions", jobExecutions);
        if (executionId != null) {
            JobOperator jobOperator = BatchRuntime.getJobOperator();
            JobExecution je = jobOperator.getJobExecution(Long.valueOf(executionId));
            if (instanceId != null) {
                JobInstance ji = jobOperator.getJobInstance(Long.valueOf(executionId));
                if (ji.getInstanceId() != Long.valueOf(instanceId)) {
                    throw new RuntimeException("executionid " + executionId
                    + " is not associated with the specified instanceid (" + instanceId + ")"
                    + "; did you mean " + ji.getInstanceId() + " ?");
                }
            }
            try {
                if (glassFishBatchSecurityHelper.isVisibleToThisInstance(((TaggedJobExecution) je).getTagName()))
                    jobExecutions.add(handleJob(je, columnFormatter));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Exception while getting jobExecution details: " + ex);
                logger.log(Level.FINE, "Exception while getting jobExecution details: ", ex);
            }
        } else if (instanceId != null) {
            for (JobExecution je : getJobExecutionForInstance(Long.valueOf(instanceId))) {
                try {
                    if (glassFishBatchSecurityHelper.isVisibleToThisInstance(((TaggedJobExecution) je).getTagName()))
                        jobExecutions.add(handleJob(je, columnFormatter));
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Exception while getting jobExecution details: " + ex);
                    logger.log(Level.FINE, "Exception while getting jobExecution details: ", ex);
                }
            }
        } else {
            JobOperator jobOperator = BatchRuntime.getJobOperator();
            Set<String> jobNames = jobOperator.getJobNames();
            if (jobNames != null) {
                for (String jn : jobOperator.getJobNames()) {
                    List<JobInstance> exe = jobOperator.getJobInstances(jn, 0, Integer.MAX_VALUE - 1);
                    if (exe != null) {
                        for (JobInstance ji : exe) {
                            for (JobExecution je : jobOperator.getJobExecutions(ji)) {
                                try {
                                    if (glassFishBatchSecurityHelper.isVisibleToThisInstance(((TaggedJobExecution) je).getTagName()))
                                        jobExecutions.add(handleJob(jobOperator.getJobExecution(je.getExecutionId()), columnFormatter));
                                } catch (Exception ex) {
                                    logger.log(Level.WARNING, "Exception while getting jobExecution details: " + ex);
                                    logger.log(Level.FINE, "Exception while getting jobExecution details: ", ex);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (jobExecutions.size() > 0) {
            context.getActionReport().setMessage(columnFormatter.toString());
        } else {
            throw new RuntimeException("No Job Executions found");
        }

    }

    @Override
    protected final String[] getAllHeaders() {
        return new String[]{
                JOB_NAME, EXECUTION_ID, START_TIME, END_TIME, BATCH_STATUS, EXIT_STATUS, JOB_PARAMETERS, STEP_COUNT
        };
    }

    @Override
    protected final String[] getDefaultHeaders() {
        return new String[]{JOB_NAME, EXECUTION_ID, START_TIME, END_TIME, BATCH_STATUS, EXIT_STATUS};
    }

    private boolean isSimpleMode() {
        for (String h : getOutputHeaders()) {
            if (!JOB_NAME.equals(h)) {
                return false;
            }
        }
        return true;
    }

    private static List<JobExecution> getJobExecutionForInstance(long instId)
            throws JobSecurityException, NoSuchJobException, NoSuchJobInstanceException, NoSuchJobExecutionException {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobInstance jobInstance = null;
        for (String jn : jobOperator.getJobNames()) {
            List<JobInstance> exe = jobOperator.getJobInstances(jn, 0, Integer.MAX_VALUE - 1);
            if (exe != null) {
                for (JobInstance ji : exe) {
                    if (ji.getInstanceId() == instId) {
                        jobInstance = ji;
                        break;
                    }
                }
            }
        }

        List<JobExecution> jeList = new ArrayList<JobExecution>();

        if (jobInstance == null)
            throw new RuntimeException("No Job Executions found for instanceid = " + instId);

        List<JobExecution> lst = BatchRuntime.getJobOperator().getJobExecutions(jobInstance);
        if (lst != null) {
            for (JobExecution je : lst) {
                jeList.add(jobOperator.getJobExecution(je.getExecutionId()));
            }
        }

        return jeList;
    }

    private Map<String, Object> handleJob(JobExecution je, ColumnFormatter columnFormatter)
        throws JobSecurityException, NoSuchJobExecutionException {

        Map<String, Object> jobInfo = new HashMap<>();

        int jobParamIndex = -1;
        StringTokenizer st = new StringTokenizer("", "");
        String[] cfData = new String[getOutputHeaders().length];
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        for (int index = 0; index < getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case JOB_NAME:
                    data = " " + je.getJobName();
                    break;
                case EXECUTION_ID:
                    data = "" + je.getExecutionId();
                    break;
                case BATCH_STATUS:
                    data = je.getBatchStatus() != null ? je.getBatchStatus() : "";
                    break;
                case EXIT_STATUS:
                    data = je.getExitStatus() != null ? je.getExitStatus() : "";
                    break;
                case START_TIME:
                    if (je.getStartTime() != null) {
                        data = je.getStartTime().getTime();
                        cfData[index] = je.getStartTime().toString();
                    } else {
                        data = "";
                    }
                    break;
                case END_TIME:
                    if (je.getEndTime() != null) {
                        data = je.getEndTime().getTime();
                        cfData[index] = je.getEndTime().toString();
                    } else {
                        data = "";
                    }
                    break;
                case JOB_PARAMETERS:
                    data = je.getJobParameters() == null ? new Properties() : je.getJobParameters();
                    jobParamIndex = index;
                    ColumnFormatter cf = new ColumnFormatter(new String[]{"KEY", "VALUE"});
                    for (Map.Entry e : ((Properties) data).entrySet())
                        cf.addRow(new String[]{e.getKey().toString(), e.getValue().toString()});
                    st = new StringTokenizer(cf.toString(), "\n");
                    break;
                case STEP_COUNT:
                    long exeId = executionId == null ? je.getExecutionId() : Long.valueOf(executionId);
                    data = jobOperator.getStepExecutions(exeId) == null
                        ? 0 : jobOperator.getStepExecutions(exeId).size();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            cfData[index] = (jobParamIndex != index)
                    ? (cfData[index] == null ? data.toString() : cfData[index])
                    : (st.hasMoreTokens()) ? st.nextToken() : "";
        }
        columnFormatter.addRow(cfData);

        cfData = new String[getOutputHeaders().length];
        for (int i = 0; i < getOutputHeaders().length; i++)
            cfData[i] = "";
        while (st.hasMoreTokens()) {
            cfData[jobParamIndex] = st.nextToken();
            columnFormatter.addRow(cfData);
        }

        return jobInfo;
    }
}
