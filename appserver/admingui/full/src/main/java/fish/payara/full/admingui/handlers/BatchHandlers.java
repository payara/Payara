/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.full.admingui.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import org.glassfish.admingui.common.util.GuiUtil;

/**
 *
 * @author Susan Rai
 */
public class BatchHandlers {

    private static final int DEFAULT_OFFSET_VALUE = 20;

    @Handler(id = "py.addToOffSetValue",
            input = {
                @HandlerInput(name = "offset", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void addToOffSetValue(HandlerContext handlerCtx) {

        String offsetValue = (String) handlerCtx.getInputValue("offset");
        int result = 0;

        try {
            result = Integer.parseInt(offsetValue) + DEFAULT_OFFSET_VALUE;
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info("Value isn't a valid integer " + ex);
        }

        handlerCtx.setOutputValue("result", result);
    }

    @Handler(id = "py.subtractFromOffSetValue",
            input = {
                @HandlerInput(name = "offset", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void subtractFromOffSetValue(HandlerContext handlerCtx) {

        String offsetValue = (String) handlerCtx.getInputValue("offset");
        int result = 0;

        try {
            result = Integer.parseInt(offsetValue);
            if (result >= DEFAULT_OFFSET_VALUE) {
                result = result - DEFAULT_OFFSET_VALUE;
            } else {
                result = 0;
            }
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info("Value isn't a valid integer " + ex);
        }

        handlerCtx.setOutputValue("result", result);
    }

    @Handler(id = "py.getPageNumber",
            input = {
                @HandlerInput(name = "offset", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void getPageNumber(HandlerContext handlerCtx) {

        String offsetValue = (String) handlerCtx.getInputValue("offset");
        int result = 0;

        try {
            int offSet = Integer.parseInt(offsetValue);
            result = (offSet + DEFAULT_OFFSET_VALUE) / DEFAULT_OFFSET_VALUE;
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info("Value isn't a valid integer " + ex);
        }

        handlerCtx.setOutputValue("result", result);
    }

    @Handler(id = "py.goToSpecifiedPageNumber",
            input = {
                @HandlerInput(name = "pageNumber", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void goToSpecifiedPageNumber(HandlerContext handlerCtx) {

        String pageNumberValue = (String) handlerCtx.getInputValue("pageNumber");
        int result = 0;

        try {
            int pageNumber = Integer.parseInt(pageNumberValue);
            result = (pageNumber * DEFAULT_OFFSET_VALUE) - DEFAULT_OFFSET_VALUE;
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info("Value isn't a valid integer " + ex);
        }

        handlerCtx.setOutputValue("result", result);
    }

    @Handler(id = "py.getPageCount",
            input = {
                @HandlerInput(name = "jobCount", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void getPageCount(HandlerContext handlerCtx) {

        String jobCountValue = (String) handlerCtx.getInputValue("jobCount");
        int result = 1;

        try {
            int jobCount = Integer.parseInt(jobCountValue);
            if (jobCount > 0) {
                result = (jobCount + DEFAULT_OFFSET_VALUE - 1) / DEFAULT_OFFSET_VALUE;
            }
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info("Value isn't a valid integer " + ex);
        }

        handlerCtx.setOutputValue("result", result);
    }

}
