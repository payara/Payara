package fish.payara.appserver.demo.module2.admin;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.internal.api.Target;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.demo.module2.DemoServiceConfiguration;
import java.beans.PropertyVetoException;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author srai
 */
@Service(name = "set-demo-service-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.EXCLUSIVE)
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-demo-service-configuration",
            description = "Set Demo Configuration")
})
public class SetDemoServiceConfiguration implements AdminCommand {

    @Param(name = "message")
    private String message;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Inject
    protected Target targetUtil;

    @Override
    public void execute(AdminCommandContext acc) {

        final ActionReport actionReport = acc.getActionReport();
        Config config = targetUtil.getConfig(target);
        DemoServiceConfiguration demoConfig = config.getExtensionByType(DemoServiceConfiguration.class);

        try {

            ConfigSupport.apply(new SingleConfigCode<DemoServiceConfiguration>() {

                @Override
                public Object run(final DemoServiceConfiguration demoConfigProxy) throws PropertyVetoException, TransactionFailure {
                    demoConfigProxy.setHelloWorldMessage(message);
                    actionReport.setMessage("Hello World Message set to " + message);
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return null;
                }

            }, demoConfig);
        } catch (TransactionFailure ex) {
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }

}
