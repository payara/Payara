package fish.payara.cluster.admingui.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import org.glassfish.admingui.common.util.GuiUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.glassfish.admingui.common.util.RestUtil.buildChildEntityList;

public class PayaraRestApiHandlers {

    @Handler(id = "py.getNodesList",
            input = {
                    @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                    @HandlerInput(name = "childType", type = String.class, required = true),
                    @HandlerInput(name = "skipList", type = List.class, required = false),
                    @HandlerInput(name = "includeList", type = List.class, required = false),
                    @HandlerInput(name = "id", type = String.class, defaultValue = "name")},
            output = {
                    @HandlerOutput(name = "result", type = List.class)
            })
    public static void getNodesList(HandlerContext handlerCtx) {
        try {
            List<Map<String, Object>> allNodes = buildChildEntityList(
                    (String)handlerCtx.getInputValue("parentEndpoint"),
                    (String)handlerCtx.getInputValue("childType"),
                    (List)handlerCtx.getInputValue("skipList"),
                    (List)handlerCtx.getInputValue("includeList"),
                    (String)handlerCtx.getInputValue("id"));

            List<Map<String, Object>> nonHiddenNodes = new ArrayList<>();
            for (Map<String, Object> node : allNodes) {
                if (!node.get("type").equals("HIDDEN")) {
                    nonHiddenNodes.add(node);
                }
            }

            handlerCtx.setOutputValue("result", nonHiddenNodes);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    @Handler(id = "py.getNodesListNavTree",
            input = {
                    @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                    @HandlerInput(name = "childType", type = String.class, required = true),
                    @HandlerInput(name = "skipList", type = List.class, required = false),
                    @HandlerInput(name = "includeList", type = List.class, required = false),
                    @HandlerInput(name = "id", type = String.class, defaultValue = "name")},
            output = {
                    @HandlerOutput(name = "result", type = Map.class)
            })
    public static void getNodesListNavTree(HandlerContext handlerCtx) {
        try {
            String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
            List<Map<String, Object>> allNodes = buildChildEntityList(
                    parentEndpoint,
                    (String)handlerCtx.getInputValue("childType"),
                    (List)handlerCtx.getInputValue("skipList"),
                    (List)handlerCtx.getInputValue("includeList"),
                    (String)handlerCtx.getInputValue("id"));

            Map<String, String> nonHiddenNodes = new HashMap<>();
            for (Map<String, Object> node : allNodes) {
                if (!node.get("type").equals("HIDDEN")) {
                    String name = (String) node.get("name");
                    nonHiddenNodes.put(name, parentEndpoint + name);
                }
            }

            handlerCtx.setOutputValue("result", nonHiddenNodes);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    @Handler(id = "py.getNodeNamesList",
            input = {
                    @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                    @HandlerInput(name = "childType", type = String.class, required = true),
                    @HandlerInput(name = "skipList", type = List.class, required = false),
                    @HandlerInput(name = "includeList", type = List.class, required = false),
                    @HandlerInput(name = "id", type = String.class, defaultValue = "name")},
            output = {
                    @HandlerOutput(name = "result", type = List.class)
            })
    public static void getNodeNamesList(HandlerContext handlerCtx) {
        try {
            String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
            List<Map<String, Object>> allNodes = buildChildEntityList(
                    parentEndpoint,
                    (String)handlerCtx.getInputValue("childType"),
                    (List)handlerCtx.getInputValue("skipList"),
                    (List)handlerCtx.getInputValue("includeList"),
                    (String)handlerCtx.getInputValue("id"));

            List<String> nonHiddenNodes = new ArrayList<>();
            for (Map<String, Object> node : allNodes) {
                if (!node.get("type").equals("HIDDEN")) {
                    String name = (String) node.get("name");
                    nonHiddenNodes.add(name);
                }
            }

            handlerCtx.setOutputValue("result", nonHiddenNodes);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
}
