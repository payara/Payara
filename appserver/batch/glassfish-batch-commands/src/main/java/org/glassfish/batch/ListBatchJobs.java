/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
@Service(name = "_ListBatchJobs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("_ListBatchJobs")
@ExecuteOn(value = {RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "_ListBatchJobs",
                description = "_List Batch Jobs")
})
public class ListBatchJobs
    extends AbstractLongListCommand {


    private static final String JOB_NAME = "jobName";

    private static final String APP_NAME = "appName";

    private static final String INSTANCE_COUNT = "instanceCount";

    private static final String INSTANCE_ID = "instanceId";

    private static final String EXECUTION_ID = "executionId";

    private static final String BATCH_STATUS = "batchStatus";

    private static final String EXIT_STATUS = "exitStatus";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    @Param(primary = true, optional = true)
    String jobName;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps)
        throws Exception {

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        if (isSimpleMode()) {
            extraProps.put("simpleMode", true);
            extraProps.put("listBatchJobs", findSimpleJobInfo(columnFormatter));
        } else {
            extraProps.put("simpleMode", false);
            List<Map<String, Object>> jobExecutions = new ArrayList<>();
            extraProps.put("listBatchJobs", jobExecutions);
            for (JobExecution je : findJobExecutions()) {
                try {
                    if (glassFishBatchSecurityHelper.isVisibleToThisInstance(((TaggedJobExecution) je).getTagName()))
                        jobExecutions.add(handleJob(je, columnFormatter));
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Exception while getting jobExecution details: " + ex);
                    logger.log(Level.FINE, "Exception while getting jobExecution details ", ex);
                }
            }
        }
        context.getActionReport().setMessage(columnFormatter.toString());
    }

    @Override
    protected final String[] getAllHeaders() {
        return new String[]{
                JOB_NAME, APP_NAME, INSTANCE_COUNT, INSTANCE_ID, EXECUTION_ID, BATCH_STATUS,
                START_TIME, END_TIME, EXIT_STATUS
        };
    }

    @Override
    protected final String[] getDefaultHeaders() {
        return new String[]{JOB_NAME, INSTANCE_COUNT};
    }

    private boolean isSimpleMode() {

        for (String h : getOutputHeaders()) {
            if (!JOB_NAME.equals(h) && !INSTANCE_COUNT.equals(h)) {
                return false;
            }
        }
        return getOutputHeaders().length == 2;
    }

    private Map<String, Integer> findSimpleJobInfo(ColumnFormatter columnFormatter)
        throws JobSecurityException, NoSuchJobException {

        Map<String, Integer> jobToInstanceCountMap = new HashMap<>();
        List<JobExecution> jobExecutions = findJobExecutions();
        for (JobExecution je : jobExecutions) {
            if (glassFishBatchSecurityHelper.isVisibleToThisInstance(((TaggedJobExecution) je).getTagName())) {
                String jobName = je.getJobName();
                int count = 0;
                if (jobToInstanceCountMap.containsKey(jobName)) {
                    count = jobToInstanceCountMap.get(jobName);
                }
                jobToInstanceCountMap.put(jobName, count+1);
            }
        }
        for (Map.Entry<String, Integer> e : jobToInstanceCountMap.entrySet())
            columnFormatter.addRow(new Object[] {e.getKey(), e.getValue()});
        return jobToInstanceCountMap;
    }

    private List<JobExecution> findJobExecutions()
        throws JobSecurityException, NoSuchJobException, NoSuchJobInstanceException {
        List<JobExecution> jobExecutions = new ArrayList<>();

        JobOperator jobOperator = AbstractListCommand.getJobOperatorFromBatchRuntime();
        if (jobName != null) {
            List<JobInstance> exe = jobOperator.getJobInstances(jobName, 0, Integer.MAX_VALUE - 1);
            if (exe != null) {
                for (JobInstance ji : exe) {
                    jobExecutions.addAll(jobOperator.getJobExecutions(ji));
                }
            }
        } else {
            Set<String> jobNames = jobOperator.getJobNames();
            if (jobNames != null) {
                for (String jn : jobOperator.getJobNames()) {
                    List<JobInstance> exe = jobOperator.getJobInstances(jn, 0, Integer.MAX_VALUE - 1);
                    if (exe != null) {
                        for (JobInstance ji : exe) {
                            jobExecutions.addAll(jobOperator.getJobExecutions(ji));
                        }
                    }
                }
            }
        }

        return jobExecutions;
    }

    private Map<String, Object> handleJob(JobExecution je, ColumnFormatter columnFormatter)
        throws  JobSecurityException, NoSuchJobException, NoSuchJobExecutionException {
        Map<String, Object> jobInfo = new HashMap<>();

        String[] cfData = new String[getOutputHeaders().length];
        JobOperator jobOperator = AbstractListCommand.getJobOperatorFromBatchRuntime();
        for (int index = 0; index < getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case JOB_NAME:
                    data = "" + je.getJobName();
                    break;
                case APP_NAME:
                    try {
                        String appName = "" + ((TaggedJobExecution) je).getTagName();
                        int semi = appName.indexOf(':');
                        data = appName.substring(semi+1);
                    } catch (Exception ex) {
                        logger.log(Level.FINE, "Error while calling ((TaggedJobExecution) je).getTagName() ", ex);
                        data = ex.toString();
                    }
                    break;
                case INSTANCE_COUNT:
                    data = jobOperator.getJobInstanceCount(je.getJobName());
                    break;
                case INSTANCE_ID:
                    data = jobOperator.getJobInstance(je.getExecutionId()).getInstanceId();
                    break;
                case EXECUTION_ID:
                    data = je.getExecutionId();
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
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            if (cfData[index] == null)
                cfData[index] = data.toString();
        }
        columnFormatter.addRow(cfData);

        return jobInfo;
    }
}
