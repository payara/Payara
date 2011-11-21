package org.glassfish.elasticity.engine.commands;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.elasticity.config.serverbeans.ElasticService;
import org.glassfish.elasticity.config.serverbeans.ElasticServices;
import org.glassfish.elasticity.config.serverbeans.Actions;
import org.glassfish.elasticity.config.serverbeans.LogAction;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;

import java.beans.PropertyVetoException;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 11/21/11
 */
@Service(name="delete-log-action")
@I18n("delete.log.action")
@Scoped(PerLookup.class)
public class DeleteLogActionCommand implements AdminCommand{

    @Inject
    Domain domain;

   @Inject
   ElasticServices elasticServices;

   @Param(name="name", primary = true)
    String name;

   @Param(name="service")
   String servicename;

    @Override
   public void execute(AdminCommandContext context) {
       ActionReport report = context.getActionReport();
       Logger logger= context.logger;

       if (elasticServices == null)   {
           //service doesn't exist
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

       if (elasticService.getActions().getLogAction(name) != null) {
            try {
                deleteActionElement(name);
           } catch(TransactionFailure e) {
               logger.warning("failed.to.delete.action " + name);
               report.setActionExitCode(ActionReport.ExitCode.FAILURE);
               report.setMessage(e.getMessage());
            }
        } else {
           logger.warning("failed.to.delete.action " + name);
           report.setActionExitCode(ActionReport.ExitCode.FAILURE);
           report.setMessage("action.not.found "+ name);

       }
   }
        public void deleteActionElement(final String name) throws TransactionFailure {
       ConfigSupport.apply(new SingleConfigCode() {
           @Override
           public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
               // get the transaction
               Transaction t = Transaction.getTransaction(param);
               if (t != null) {
                   ElasticService elasticService = elasticServices.getElasticService(servicename);
                   if (elasticService != null) {
                       Actions writeableAction = elasticService.getActions();
                       if (writeableAction != null) {
                           writeableAction = t.enroll(writeableAction);
                           LogAction action = writeableAction.getLogAction(name);
                           writeableAction.getLogAction().remove(action);
                       }
                       //nothing to delete

                   }
               }
               return Boolean.TRUE;
           }

       }, domain);
   }

}
