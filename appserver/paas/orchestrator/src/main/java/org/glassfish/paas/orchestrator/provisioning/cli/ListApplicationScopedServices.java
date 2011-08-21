package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

@org.jvnet.hk2.annotations.Service(name = "_list-application-scoped-services")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
public class ListApplicationScopedServices implements AdminCommand{

    @Param(name="appname", optional=false, primary=true)
    private String appName;

    @Inject
    private Domain domain;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

/*
        //old code that assumes that <applicatino-scoped-service> is within <application>
        Applications applications = domain.getApplications();
        if (applications != null) {
            Application app = applications.getApplication(appName);
            if (app != null) {
                Services services = app.getExtensionByType(Services.class);
                for(org.glassfish.paas.orchestrator.config.Service service : services.getServices()){
                    report.getTopMessagePart().addChild().setMessage(service.getServiceName());
                }
            }else{
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed in the server");
                return;
            }
        }else{
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No such application [" + appName + "] is deployed in the server");
            return;
        }
*/

        Services services = domain.getExtensionByType(Services.class);
        if (services != null) {
            for(Service service : services.getServices()){
                if(service instanceof ApplicationScopedService){
                    if(((ApplicationScopedService)service).getApplicationName().equals(appName)){
                        report.getTopMessagePart().addChild().setMessage(service.getServiceName());
                    }
                }
            }
        }else{
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No such application [" + appName + "] is deployed in the server");
            return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
