package org.glassfish.paas.gfplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.paas.gfplugin.GlassFishPlugin;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;


@Service(name="_create-glassfish-elastic-service")
@Supplemental(value="_create-glassfish-service", ifFailure= FailurePolicy.Warn, on= Supplemental.Timing.After)
@Scoped(PerLookup.class)
@ExecuteOn(value={RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
public class CreateGlassFishElasticService implements AdminCommand {

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Param(name="appname", optional=true)
    private String appName;

    @Inject
    CommandRunner commandRunner;

    @Inject
    GlassFishServiceUtil gfServiceUtil;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("name",serviceName);

        String min = gfServiceUtil.getProperty(serviceName, GlassFishPlugin.MIN_CLUSTER_PROPERTY_NAME);
        if(min != null){
            parameterMap.add("min", min);
        }
        String max = gfServiceUtil.getProperty(serviceName, GlassFishPlugin.MAX_CLUSTER_PROPERTY_NAME);
        if(max != null){
            parameterMap.add("max", max);
        }
        commandRunner.getCommandInvocation("_create-elastic-service", report).parameters(parameterMap).execute();
    }
}
