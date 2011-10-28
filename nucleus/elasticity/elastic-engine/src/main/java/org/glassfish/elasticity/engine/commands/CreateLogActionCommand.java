package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.elasticity.config.serverbeans.*;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import com.sun.enterprise.config.serverbeans.Domain;

import java.beans.PropertyVetoException;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/24/11
 */
@Service(name="create-log-action")
@I18n("creaet.log.action")
@Scoped(PerLookup.class)
public class CreateLogActionCommand implements AdminCommand {
    @Inject
    ElasticServices elasticServices;

    @Inject
    Domain domain;

    @Param(name="service")
    String servicename;

    @Param(name="name", primary = true)
    String name;

    @Param(name="level", optional = true)
    String level;


    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        if (elasticServices == null)   {
            // elastic service doesn't exist
            String msg = Strings.get("elasticity.not.found", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        ElasticService elasticService= elasticServices.getElasticService(servicename);
        if (elasticService == null) {
            //service doesn't exist
            String msg = Strings.get("noSuchService", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        // find the metrics gatherer and then add it - should be an hk2 service(?)
        try {
            createLogActionElement(name);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.create.log.action " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }
    }
        public void createLogActionElement(final String alertName) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t != null) {
                    ElasticService elasticService = elasticServices.getElasticService(servicename);
                    if (elasticService != null) {
                        ElasticService writeableService = t.enroll(elasticService);
                        Actions writeableAction = elasticService.getActions();
                        if (writeableAction == null)
                            writeableAction = writeableService.createChild(Actions.class);
                        else
                            writeableAction = t.enroll(writeableAction);

                        LogAction writeableLog = writeableAction.createChild(LogAction.class);
                        if (name != null)
                            writeableLog.setName(name);
                        if (level != null)
                            writeableLog.setLogLevel(level);

                        writeableAction.getLogAction().add(writeableLog);
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
