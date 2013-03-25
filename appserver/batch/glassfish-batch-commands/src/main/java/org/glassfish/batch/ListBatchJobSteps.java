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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import java.util.*;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
@Service(name = "_ListBatchJobSteps")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("_ListBatchJobSteps")
@ExecuteOn(value = {RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "_ListBatchJobSteps",
                description = "_List Batch Job Steps")
})
public class ListBatchJobSteps
    extends AbstractLongListCommand {

    private static final String NAME = "stepName";

    private static final String STEP_ID = "stepId";

    private static final String BATCH_STATUS = "batchStatus";

    private static final String EXIT_STATUS = "exitStatus";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    private static final String STEP_METRICS = "stepMetrics";

    @Param(primary = true)
    String executionId;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps)
        throws Exception {

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        List<Map<String, Object>> jobExecutions = new ArrayList<>();
        extraProps.put("listBatchJobSteps", jobExecutions);
        for (StepExecution je : findStepExecutions()) {
            jobExecutions.add(handleJob(je, columnFormatter));
        }
        context.getActionReport().setMessage(columnFormatter.toString());
    }

    @Override
    protected final String[] getAllHeaders() {
        return new String[] {
                NAME, STEP_ID, START_TIME, END_TIME, BATCH_STATUS, EXIT_STATUS, STEP_METRICS
        };
    }

    @Override
    protected final String[] getDefaultHeaders() {
        return new String[] {NAME, STEP_ID, START_TIME, END_TIME, BATCH_STATUS, EXIT_STATUS};
    }

    private List<StepExecution> findStepExecutions()
        throws JobSecurityException, NoSuchJobExecutionException {
        List<StepExecution> stepExecutions = new ArrayList<>();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        List<StepExecution> jobExecution = jobOperator.getStepExecutions(Long.valueOf(executionId));
        if (jobExecution == null || jobExecution.size() == 0)
            throw new NoSuchJobExecutionException("No job execution exists for job execution id: " + executionId);
        stepExecutions.addAll(jobExecution);

        return stepExecutions;
    }

    private Map<String, Object> handleJob(StepExecution stepExecution, ColumnFormatter columnFormatter) {
        Map<String, Object> jobInfo = new HashMap<>();

        int stepMetricsIndex = -1;
        StringTokenizer st = new StringTokenizer("", "");
        String[] cfData = new String[getOutputHeaders().length];
        for (int index = 0; index < getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case NAME:
                    data = stepExecution.getStepName();
                    break;
                case STEP_ID:
                    data = stepExecution.getStepExecutionId();
                    break;
                case BATCH_STATUS:
                    data = stepExecution.getBatchStatus();
                    break;
                case EXIT_STATUS:
                    data = stepExecution.getExitStatus();
                    break;
                case START_TIME:
                    data = stepExecution.getStartTime().getTime();
                    cfData[index] = stepExecution.getStartTime().toString();
                    break;
                case END_TIME:
                    data = stepExecution.getEndTime().getTime();
                    cfData[index] = stepExecution.getEndTime().toString();
                    break;
                case STEP_METRICS:
                    stepMetricsIndex = index;
                    Map<String, Long> metricMap = new HashMap<>();
                    if (stepExecution.getMetrics() != null) {
                        ColumnFormatter cf = new ColumnFormatter(new String[]{"METRICNAME", "VALUE"});
                        for (Metric metric : stepExecution.getMetrics()) {
                            metricMap.put(metric.getType().name(), metric.getValue());
                            cf.addRow(new Object[] {metric.getType().name(), metric.getValue()});
                        }
                        st = new StringTokenizer(cf.toString(), "\n");
                    }
                    data = metricMap;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            cfData[index] = (stepMetricsIndex != index)
                    ? (cfData[index] == null ? data.toString() : cfData[index])
                    : (st.hasMoreTokens() ? st.nextToken() : "");
        }
        columnFormatter.addRow(cfData);

        cfData = new String[getOutputHeaders().length];
        for (int i = 0; i < getOutputHeaders().length; i++)
            cfData[i] = "";
        while (st.hasMoreTokens()) {
            cfData[stepMetricsIndex] = st.nextToken();
            columnFormatter.addRow(cfData);
        }

        return jobInfo;
    }

}
