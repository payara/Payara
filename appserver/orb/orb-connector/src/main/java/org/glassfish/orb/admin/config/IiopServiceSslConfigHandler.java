package org.glassfish.orb.admin.config;

import com.sun.enterprise.admin.commands.CreateSsl;
import com.sun.enterprise.admin.commands.DeleteSsl;
import com.sun.enterprise.admin.commands.SslConfigHandler;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.config.serverbeans.SslClientConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.grizzly.config.dom.Ssl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * SSL configuration handler for iiop-service.
 * @author Jerome Dochez
 */
@Service(name="iiop-service")
public class IiopServiceSslConfigHandler implements SslConfigHandler {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateSsl.class);

    @Override
    public void create(final CreateSsl command, ActionReport report) {
        IiopService iiopSvc = command.config.getExtensionByType(IiopService.class);
        if (iiopSvc.getSslClientConfig() != null) {
            report.setMessage(
                localStrings.getLocalString(
                    "create.ssl.iiopsvc.alreadyExists", "IIOP Service " +
                        "already has been configured with SSL configuration."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<IiopService>() {
                        public Object run(IiopService param)
                                throws PropertyVetoException, TransactionFailure {
                            SslClientConfig newSslClientCfg =
                                    param.createChild(SslClientConfig.class);
                            Ssl newSsl = newSslClientCfg.createChild(Ssl.class);
                            command.populateSslElement(newSsl);
                            newSslClientCfg.setSsl(newSsl);
                            param.setSslClientConfig(newSslClientCfg);
                            return newSsl;
                        }
                    }, iiopSvc);

        } catch (TransactionFailure e) {
            command.reportError(report, e);
        }
        command.reportSuccess(report);
    }

    @Override
    public void delete(DeleteSsl command, ActionReport report) {
        if (command.config.getExtensionByType(IiopService.class).getSslClientConfig() == null) {
            report.setMessage(localStrings.getLocalString(
                    "delete.ssl.element.doesnotexistforiiop",
                    "Ssl element does not exist for IIOP service"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<IiopService>() {
                    public Object run(IiopService param)
                            throws PropertyVetoException {
                        param.setSslClientConfig(null);
                        return null;
                    }
                }, command.config.getExtensionByType(IiopService.class));
        } catch (TransactionFailure e) {
            command.reportError(report, e);
        }
    }
}
