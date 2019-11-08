package fish.payara.monitoring.admin;

import static org.glassfish.config.support.CommandTarget.DAS;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.deployment.autodeploy.AutoDeployer;
import org.glassfish.deployment.autodeploy.AutoDeploymentOperation;
import org.glassfish.deployment.autodeploy.AutoUndeploymentOperation;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;

@Service(name = "set-monitoring-console-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@TargetType({DAS})
public class SetMonitoringConsoleConfigurationCommand implements AdminCommand {

    private static final String MONITORING_CONSOLE_APP_NAME = "__monitoringconsole";
    private final static String GLASSFISH_LIB_INSTALL_APPLICATIONS = "glassfish/lib/install/applications";

    @Param(optional = true)
    private Boolean enabled;

    @Inject
    protected CommandRunner commandRunner;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Domain domain;

    @Override
    public void execute(AdminCommandContext context) {
        if (enabled != null) {
            if (enabled.booleanValue()) {
                deployMonitoringConsole(context.getActionReport());
            } else {
                undeployMonitoringConsole(context.getActionReport());
            }
        }
    }

    private void undeployMonitoringConsole(ActionReport report) {
        Path applications = getApplicationsPath();
        Path app = applications.resolve(MONITORING_CONSOLE_APP_NAME);

        AutoUndeploymentOperation command = AutoUndeploymentOperation.newInstance(
                serviceLocator,
                app.toFile(),
                MONITORING_CONSOLE_APP_NAME,
                SystemPropertyConstants.DAS_SERVER_NAME);
        AutoDeployer.AutodeploymentStatus deploymentStatus = command.run();

        report.setActionExitCode(deploymentStatus.getExitCode());

        if (deploymentStatus.getExitCode().equals(ActionReport.ExitCode.FAILURE)) {
            if (domain.getApplications().getApplication(MONITORING_CONSOLE_APP_NAME) == null) {
                report.appendMessage("\nMonitoring Console is not enabled on any target");
            } else {
                report.appendMessage("\nFailed to disable Monitoring Console - was it enabled on the specified target?");
            }
        }
    }

    private void deployMonitoringConsole(ActionReport report) {
        Path applications = getApplicationsPath();
        Path app = applications.resolve(MONITORING_CONSOLE_APP_NAME);

        AutoDeploymentOperation command = AutoDeploymentOperation.newInstance(
                serviceLocator,
                app.toFile(),
                null,
                SystemPropertyConstants.DAS_SERVER_NAME,
                "monitoring-console"
                );

        if (domain.getApplications().getApplication(MONITORING_CONSOLE_APP_NAME) == null) {
            AutoDeployer.AutodeploymentStatus deploymentStatus = command.run();
            report.setActionExitCode(deploymentStatus.getExitCode());
        } else {
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            report.setMessage("Monitoring Console is already deployed on at least one target");
        }
    }

    private static Path getApplicationsPath() {
        return Paths.get(System.getProperty(SystemPropertyConstants.PRODUCT_ROOT_PROPERTY)).resolve(GLASSFISH_LIB_INSTALL_APPLICATIONS);
    }

}
