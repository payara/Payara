/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.batch;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.Properties;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.batch.AbstractListCommandProxy;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author susan
 */
@Service(name = "list-batch-jobs-with-pagination")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.batch.jobs.with.pagination")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "list-batch-jobs-with-pagination",
            description = "List Batch Jobs With Pagination")
})
public class ListBatchJobsWithPagination extends AbstractListCommandProxy {

    @Param(name = "offset", optional = true, defaultValue = "0")
    String offSetValue;

    @Param(name = "numberOfJobs", optional = true, defaultValue = "1000")
    String numberOfJobs;

    @Override
    protected String getCommandName() {
        return "_ListBatchJobs";
    }

    @Override
    protected void fillParameterMap(ParameterMap parameterMap) {
        super.fillParameterMap(parameterMap);
        parameterMap.add("offset", offSetValue);
        parameterMap.add("numberOfJobs", numberOfJobs);
    }

    @Override
    protected void postInvoke(AdminCommandContext context, ActionReport subReport) {
        Properties subProperties = subReport.getExtraProperties();
        Properties extraProps = context.getActionReport().getExtraProperties();
        if (subProperties.get("listBatchJobs") != null) {
            extraProps.put("listBatchJobs", subProperties.get("listBatchJobs"));
        }
          if (subProperties.get("getJobCount") != null) {
            extraProps.put("getJobCount", subProperties.get("getJobCount"));
        }
    }

}
