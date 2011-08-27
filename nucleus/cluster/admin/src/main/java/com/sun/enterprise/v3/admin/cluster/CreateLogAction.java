package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.StringUtils;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/26/11
 */
@Service(name = "create-log-action")
@I18n("create.log.action")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
public class CreateLogAction implements AdminCommand {
    @Inject
     ElasticServices elasticServices;

     @Inject
     Domain domain;

     @Param(name="name", primary = true)
      String name;

    @Param(name="service")
    String servicename;

    @Param(name="loglevel", optional = true)
    String loglevel;

    @Override
        public void execute(AdminCommandContext context) {
            ActionReport report = context.getActionReport();
            Logger logger= context.logger;

            ElasticService elasticService= elasticServices.getElasticService(servicename);
            if (elasticService == null) {
                //node doesn't exist
                String msg = Strings.get("noSuchService", name);
                logger.warning(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }

            try {
                createLogActionElement(name);
            } catch(TransactionFailure e) {
                logger.warning("failed.to.create.action " + name);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(e.getMessage());
            }
        }

    void createLogActionElement(final String actionName) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t!=null) {
                    ElasticServices elasticServices = ((Domain)param).getElasticServices();
                    ElasticService elasticService = elasticServices.getElasticService(servicename);
                    ElasticService writeableService = t.enroll(elasticService);
                    Actions writeableActions =  elasticService.getActions();
                    if (writeableActions == null)
                        writeableActions =writeableService.createChild(Actions.class);
                    else
                         writeableActions = t.enroll(writeableActions);

                    LogAction writeableLogAction = writeableActions.createChild(LogAction.class);
                     if (name != null)
                        writeableLogAction.setName(name);
                    if (loglevel != null)
                        writeableLogAction.setLogLevel(loglevel);

                    writeableActions.getLogAction().add(writeableLogAction);
                    writeableService.setActions(writeableActions);
                }
                return Boolean.TRUE;
            }

        }, domain);
    }

}
