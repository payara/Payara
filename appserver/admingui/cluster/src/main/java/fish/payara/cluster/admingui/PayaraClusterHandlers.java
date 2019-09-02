package fish.payara.cluster.admingui;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import fish.payara.api.admin.config.NameGenerator;
import org.glassfish.admingui.common.util.GuiUtil;

public class PayaraClusterHandlers {

    @Handler(id = "py.generateInstanceNameIfRequired",
            input = {
                    @HandlerInput(name = "name", type = String.class, required = true),
                    @HandlerInput(name = "autoname", type = Boolean.class, required = true)
            },
            output = {
                    @HandlerOutput(name = "instanceName", type = String.class)})
    public static void generateInstanceNameIfRequired(HandlerContext handlerCtx) {
        String instanceName = (String) handlerCtx.getInputValue("name");
        Boolean autoname = (Boolean) handlerCtx.getInputValue("autoname");

        if (GuiUtil.isEmpty(instanceName)) {
            if (autoname) {
                instanceName = NameGenerator.generateName();
            } else {
                GuiUtil.prepareAlert("error", "No instance name provided, and Auto Name not enabled", null);
                handlerCtx.getFacesContext().renderResponse();
            }
        }

        handlerCtx.setOutputValue("instanceName", instanceName);
    }

}
