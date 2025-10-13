/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]

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
import jakarta.validation.constraints.Min;
import static org.glassfish.batch.BatchConstants.LIST_BATCH_JOBS;
import static org.glassfish.batch.BatchConstants.LIST_JOBS_COUNT;
import static org.glassfish.batch.BatchConstants.SIMPLE_MODE;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
@Service(name = "list-batch-jobs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.batch.jobs")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-batch-jobs",
                description = "List Batch Jobs")
})
public class ListBatchJobsProxy
    extends AbstractListCommandProxy {

    @Param(primary = true, optional = true)
    String jobName;

    @Min(value = 0, message = "Offset value needs to be greater than 0")
    @Param(name = "offset", optional = true, defaultValue = "0")
    String offSetValue;

    @Min(value = 0, message = "Limit value needs to be greater than 0")
    @Param(name = "limit", optional = true, defaultValue = "2000")
    String limitValue;


    @Override
    protected String getCommandName() {
        return "_ListBatchJobs";
    }

    protected void fillParameterMap(ParameterMap parameterMap) {
        super.fillParameterMap(parameterMap);
        if (jobName != null) {
            parameterMap.add("DEFAULT", jobName);
        }
        parameterMap.add("offset", offSetValue);
        parameterMap.add("limit", limitValue);
    }


    protected void postInvoke(AdminCommandContext context, ActionReport subReport) {
        Properties subProperties = subReport.getExtraProperties();
        Properties extraProps = context.getActionReport().getExtraProperties();
        if (subProperties.get(SIMPLE_MODE) != null) {
            extraProps.put(SIMPLE_MODE, subProperties.get(SIMPLE_MODE));
        }
        if (subProperties.get(LIST_BATCH_JOBS) != null) {
            extraProps.put(LIST_BATCH_JOBS, subProperties.get(LIST_BATCH_JOBS));
        }
        if (subProperties.get(LIST_JOBS_COUNT) != null) {
            extraProps.put(LIST_JOBS_COUNT, subProperties.get(LIST_JOBS_COUNT));
        }
    }
}
