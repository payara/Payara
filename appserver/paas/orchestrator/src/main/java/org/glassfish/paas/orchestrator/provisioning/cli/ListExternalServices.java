package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.ExternalService;
import org.glassfish.paas.orchestrator.config.Services;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

@Service(name = "list-external-services")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
public class ListExternalServices implements AdminCommand{

    @Inject
    private Domain domain;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart messagePart = report.getTopMessagePart();
        Services services = domain.getExtensionByType(Services.class);
        for(org.glassfish.paas.orchestrator.config.Service service : services.getServices()){
            if(service instanceof ExternalService){
                ActionReport.MessagePart part = messagePart.addChild();
                part.setMessage(service.getServiceName());
            }
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
