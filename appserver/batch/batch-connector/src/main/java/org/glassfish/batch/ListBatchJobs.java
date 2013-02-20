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

import com.sun.enterprise.util.ColumnFormatter;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import java.util.*;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 *
 */
@Service(name="list-batch-jobs")
@PerLookup
public class ListBatchJobs
    extends AbstractListCommand {

    private static final String JOB_NAME = "jobname";

    private static final String INSTANCE_COUNT = "instancecount";

    private static final String INSTANCE_ID = "instanceid";

    private static final String EXECUTION_ID = "executionid";

    private static final String BATCH_STATUS = "batchstatus";

    private static final String EXIT_STATUS = "exitstatus";

    private static final String START_TIME = "starttime";

    private static final String END_TIME = "endtime";

    private static final String JOB_PARAMETERS = "jobparameters";

    @Param(name = "jobname", optional = true)
    String jobName;

    @Param(name = "instanceid", optional = true)
    String instanceId;

    @Param(name = "executionid", optional = true)
    String executionId;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps) {

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        if (isSimpleMode()) {
            extraProps.put("simple-mode", true);
            extraProps.put("list-batch-jobs", findSimpleJobInfo(columnFormatter));
        } else {
            extraProps.put("simple-mode", false);
            for (JobExecution je : findJobExecutions()) {
                handleJob(je, columnFormatter);
            }
        }
        context.getActionReport().setMessage(columnFormatter.toString());
    }

    @Override
    protected final String[] getSupportedHeaders() {
        return new String[] {
                JOB_NAME, INSTANCE_COUNT, INSTANCE_ID, EXECUTION_ID, BATCH_STATUS,
                START_TIME, END_TIME, EXIT_STATUS, JOB_PARAMETERS
        };
    }

    @Override
    protected final String[] getTerseHeaders() {
        return new String[] {JOB_NAME, INSTANCE_COUNT};
    }

    @Override
    protected String[] getLongHeaders() {
        return getSupportedHeaders();
    }

    private boolean isSimpleMode() {
        for (String h : getOutputHeaders()) {
            if (!JOB_NAME.equals(h) && !INSTANCE_COUNT.equals(h)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Integer> findSimpleJobInfo(ColumnFormatter columnFormatter) {
        Map<String, Integer> jobToInstanceCountMap = new HashMap<String, Integer>();
        Set<String> jobNames = new HashSet<String>();
        if (jobName != null)
            jobNames.add(jobName);
        else
            jobNames = BatchRuntime.getJobOperator().getJobNames();
        if (jobNames != null) {
            for (String jName : jobNames) {
                int size = getOutputHeaders().length;
                String[] data = new String[size];
                int instCount = BatchRuntime.getJobOperator().getJobInstanceCount(jName);
                for (int index =0; index<size; index++) {
                    switch (getOutputHeaders()[index]) {
                        case JOB_NAME:
                            data[index] = jName;
                            break;
                        case INSTANCE_COUNT:
                            data[index] = "" + instCount;
                            break;
                        default:
                            throw new InternalError("Cannot handle " + getOutputHeaders()[index] + " in simple mode");
                    }
                }
                columnFormatter.addRow(data);
                jobToInstanceCountMap.put(jName, instCount);
            }
        }

        return jobToInstanceCountMap;
    }
    private List<JobExecution> findJobExecutions() {
        List<JobExecution> jobExecutions = new ArrayList<JobExecution>();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        if (executionId != null) {
            JobExecution jobExecution = jobOperator.getJobExecution(Long.valueOf(executionId));
            if (jobExecution != null)
                jobExecutions.add(jobExecution);
        } else if (instanceId != null) {
            jobExecutions.addAll(getJobExecutionForInstance(Long.valueOf(instanceId)));
        } else if (jobName != null) {
            List<JobInstance> exe = jobOperator.getJobInstances(jobName, 0, Integer.MAX_VALUE - 1);
            if (exe != null) {
                for (JobInstance ji : exe) {
                    jobExecutions.addAll(jobOperator.getExecutions(ji));
                }
            }
        } else {
            Set<String> jobNames = jobOperator.getJobNames();
            if (jobNames != null) {
                for (String jn : jobOperator.getJobNames()) {
                    List<JobInstance> exe = jobOperator.getJobInstances(jn, 0, Integer.MAX_VALUE - 1);
                    if (exe != null) {
                        for (JobInstance ji : exe) {
                            jobExecutions.addAll(jobOperator.getExecutions(ji));
                        }
                    }
                }
            }
        }

        return jobExecutions;
    }

    private static List<JobExecution> getJobExecutionForInstance(long instId) {
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
        List<JobExecution> jobExecutionList = BatchRuntime.getJobOperator().getJobExecutions(jobInstance);
        return jobExecutionList == null
                ? new ArrayList<JobExecution>() : jobExecutionList;
    }

    private Map<String, Object> handleJob(JobExecution je, ColumnFormatter columnFormatter) {
        Map<String, Object> jobInfo = new HashMap<String, Object>();

        int jobParamIndex = -1;
        StringTokenizer st = new StringTokenizer("", "");
        String[] cfData = new String[getOutputHeaders().length];
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        for (int index=0; index<getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case JOB_NAME:
                    data = jobOperator.getJobInstance(je.getInstanceId()).getJobName();
                    break;
                case INSTANCE_COUNT:
                    data = jobOperator.getJobInstanceCount(jobOperator.getJobInstance(je.getInstanceId()).getJobName());
                    break;
                case INSTANCE_ID:
                    data = je.getInstanceId();
                    break;
                case EXECUTION_ID:
                    data = je.getExecutionId();
                    break;
                case BATCH_STATUS:
                    data = je.getBatchStatus();
                    break;
                case EXIT_STATUS:
                    data = je.getExitStatus();
                    break;
                case START_TIME:
                    data = je.getStartTime();
                    break;
                case END_TIME:
                    data = je.getEndTime();
                    break;
                case JOB_PARAMETERS:
                    data = je.getJobParameters() == null ? new Properties() : je.getJobParameters();
                    jobParamIndex = index;
                    ColumnFormatter cf = new ColumnFormatter(new String[] {"KEY", "VALUE"});
                    for (Map.Entry e : ((Properties) data).entrySet())
                        cf.addRow(new String[] {e.getKey().toString(), e.getValue().toString()});
                    st = new StringTokenizer(cf.toString(), "\n");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            cfData[index] = (jobParamIndex != index)
                    ? data.toString()
                    : (st.hasMoreTokens()) ? st.nextToken() : "";
        }
        columnFormatter.addRow(cfData);

        cfData = new String[getOutputHeaders().length];
        for (int i=0; i<getOutputHeaders().length; i++)
            cfData[i] = "";
        while (st.hasMoreTokens()) {
            cfData[jobParamIndex] = st.nextToken();
            columnFormatter.addRow(cfData);
        }

        return jobInfo;
    }
}
