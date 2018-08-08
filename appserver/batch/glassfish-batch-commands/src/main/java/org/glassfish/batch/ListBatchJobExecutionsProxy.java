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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.glassfish.batch;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.Properties;
import static org.glassfish.batch.BatchConstants.LIST_BATCH_JOBS_EXECUTIONS;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
@Service(name = "list-batch-job-executions")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.batch.job.executions")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-batch-job-executions",
                description = "List Batch Job Executions")
})
public class ListBatchJobExecutionsProxy
    extends AbstractListCommandProxy {

    @Param(name = "executionid", shortName = "x", optional = true)
    String executionId;

    @Param(primary = true, optional = true)
    String instanceId;

    @Override
    protected String getCommandName() {
        return "_ListBatchJobExecutions";
    }

    @Override
    protected boolean preInvoke(AdminCommandContext ctx, ActionReport subReport) {
        if (executionId != null && !isLongNumber(executionId)) {
            subReport.setMessage("execution ID must be a number");
            return false;
        }
        if (instanceId != null && !isLongNumber(instanceId)) {
            subReport.setMessage("instance ID must be a number");
            return false;
        }
        return true;
    }

    protected void fillParameterMap(ParameterMap parameterMap) {
        super.fillParameterMap(parameterMap);
        if (executionId != null)
            parameterMap.add("executionid", executionId);
        if (instanceId != null)
            parameterMap.add("DEFAULT", instanceId);
    }

    protected void postInvoke(AdminCommandContext context, ActionReport subReport) {
        Properties subProperties = subReport.getExtraProperties();
        Properties extraProps = context.getActionReport().getExtraProperties();
        if (subProperties.get(LIST_BATCH_JOBS_EXECUTIONS) != null)
            extraProps.put(LIST_BATCH_JOBS_EXECUTIONS, subProperties.get(LIST_BATCH_JOBS_EXECUTIONS));
    }

}
