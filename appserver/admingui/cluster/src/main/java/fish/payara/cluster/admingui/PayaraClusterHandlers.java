/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.cluster.admingui;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import fish.payara.api.admin.config.NameGenerator;
import org.glassfish.admingui.common.util.GuiUtil;

public class PayaraClusterHandlers {

    @Handler(id = "py.generateAutoNameIfRequired",
            input = {
                @HandlerInput(name = "name", type = String.class, required = true),
                @HandlerInput(name = "autoname", type = Boolean.class, required = true),
                @HandlerInput(name = "emptyErrorMsg", type = String.class, required = true)
            },
            output = {
                @HandlerOutput(name = "instanceName", type = String.class)})
    public static void generateAutoNameIfRequired(HandlerContext handlerCtx) {
        String instanceName = (String) handlerCtx.getInputValue("name");
        Boolean autoname = (Boolean) handlerCtx.getInputValue("autoname");
        String emptyErrorMsg = (String) handlerCtx.getInputValue("emptyErrorMsg");

        if (GuiUtil.isEmpty(instanceName)) {
            if (autoname) {
                instanceName = NameGenerator.generateName();
            } else {
                GuiUtil.prepareAlert("error", emptyErrorMsg, null);
                handlerCtx.getFacesContext().renderResponse();
            }
        }

        handlerCtx.setOutputValue("instanceName", instanceName);
    }

}
