/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
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

    private static final int DEFAULT_OFFSET_INCREMENT = 20;
    private static final String OFFSET = "offset";
    private static final String NUMBER_FORMAT_EXCEPTION_MESSAGE = "Value isn't a valid integer ";
    private static final String RESULT = "result";

    @Handler(id = "py.addToOffSetValue",
            input = {
                @HandlerInput(name = "offset", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void addToOffSetValue(HandlerContext handlerCtx) {

        String offsetValue = (String) handlerCtx.getInputValue(OFFSET);
        int result = 0;

        try {
            result = Integer.parseInt(offsetValue) + DEFAULT_OFFSET_INCREMENT;
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info(NUMBER_FORMAT_EXCEPTION_MESSAGE + ex);
        }

        handlerCtx.setOutputValue(RESULT, result);
    }

    @Handler(id = "py.subtractFromOffSetValue",
            input = {
                @HandlerInput(name = "offset", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void subtractFromOffSetValue(HandlerContext handlerCtx) {

        String offsetValue = (String) handlerCtx.getInputValue(OFFSET);
        int result = 0;

        try {
            result = Integer.parseInt(offsetValue);
            if (result >= DEFAULT_OFFSET_INCREMENT) {
                result = result - DEFAULT_OFFSET_INCREMENT;
            } else {
                result = 0;
            }
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info(NUMBER_FORMAT_EXCEPTION_MESSAGE + ex);
        }

        handlerCtx.setOutputValue(RESULT, result);
    }

    @Handler(id = "py.getPageNumber",
            input = {
                @HandlerInput(name = "offset", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void getPageNumber(HandlerContext handlerCtx) {

        String offsetValue = (String) handlerCtx.getInputValue(OFFSET);
        int result = 0;

        try {
            int offSet = Integer.parseInt(offsetValue);
            result = (offSet + DEFAULT_OFFSET_INCREMENT) / DEFAULT_OFFSET_INCREMENT;
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info(NUMBER_FORMAT_EXCEPTION_MESSAGE + ex);
        }

        handlerCtx.setOutputValue(RESULT, result);
    }

    @Handler(id = "py.getSpecifiedPageNumber",
            input = {
                @HandlerInput(name = "pageNumber", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = String.class)})
    public static void getSpecifiedPageNumber(HandlerContext handlerCtx) {

        String pageNumberValue = (String) handlerCtx.getInputValue("pageNumber");
        int result = 0;

        try {
            int pageNumber = Integer.parseInt(pageNumberValue);
            if (pageNumber > 0) {
                result = (pageNumber * DEFAULT_OFFSET_INCREMENT) - DEFAULT_OFFSET_INCREMENT;
            }
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info(NUMBER_FORMAT_EXCEPTION_MESSAGE + ex);
        }

        handlerCtx.setOutputValue(RESULT, result);
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
                result = (jobCount + DEFAULT_OFFSET_INCREMENT - 1) / DEFAULT_OFFSET_INCREMENT;
            }
        } catch (NumberFormatException ex) {
            GuiUtil.getLogger().info(NUMBER_FORMAT_EXCEPTION_MESSAGE + ex);
        }

        handlerCtx.setOutputValue(RESULT, result);
    }

}
