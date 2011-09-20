package org.glassfish.paas.gfplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.scopes.PerLookup;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;


@Service(name="_disable-glassfish-elastic-scaling")
@Supplemental(value="_stop-glassfish-service", ifFailure= FailurePolicy.Warn, on= Supplemental.Timing.Before)
@Scoped(PerLookup.class)
@ExecuteOn(value={RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
public class DisableGlassFishAutoScaling implements AdminCommand {

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

        commandRunner.getCommandInvocation("disable-auto-scaling", report).parameters(parameterMap).execute();
    }
}
