package fish.payara.admingui.support.handlers;

import java.util.logging.Logger;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import org.glassfish.internal.api.Globals;

public class SupportHandlers {

    private static Logger LOGGER = Logger.getLogger(SupportHandlers.class.getName());

    @Handler(
        id = "py.getSpecialAdminIndicator",
        output = {
            @HandlerOutput(name = "token", type = String.class)
        }
    )
    public static void getDeployedAppsInfo(HandlerContext handlerCtx) {

        // Get Domain xml
        SecureAdmin secureAdmin = Globals.getDefaultBaseServiceLocator().getService(SecureAdmin.class);
        if (secureAdmin == null) {
            LOGGER.warning("Unable to find Secure Admin configuration in Service Locator.");
            return;
        }

        // Get the special admin indicator
        String token = secureAdmin.getSpecialAdminIndicator();
        if (token == null) {
            LOGGER.warning("Unable to find special admin token from the secure administration configuration.");
        }

        handlerCtx.setOutputValue("token", token);
    }

}