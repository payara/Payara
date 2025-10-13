/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
 * 
 * Portions Copyright [2018] [Payara Foundation and/or its affiliates]
 */

package org.glassfish.web.admingui.handlers;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.grizzly.config.GrizzlyListener;
import org.glassfish.grizzly.config.dom.NetworkListener;

/**
 *
 * @author Anissa Lam
 */
public class WebHandlers {

    @Handler(id="changeNetworkListenersInVS",
    input={
        @HandlerInput(name = "vsAttrs", type = Map.class, required = true),
        @HandlerInput(name = "listenerName", type = String.class, required = true),
        @HandlerInput(name = "addFlag", type = Boolean.class, required = true)},
        output={
            @HandlerOutput(name="result", type=Map.class)})
    public static void changeNetworkListenersInVS(HandlerContext handlerCtx){
        //get the virtual server and add this network listener to it.
        Map vsAttrs = (HashMap) handlerCtx.getInputValue("vsAttrs");
        String listenerName = (String) handlerCtx.getInputValue("listenerName");
        Boolean addFlag = (Boolean) handlerCtx.getInputValue("addFlag");
        String nwListeners = (String)vsAttrs.get("networkListeners");
        List<String> listeners = GuiUtil.parseStringList(nwListeners, ",");
        if (addFlag.equals(Boolean.TRUE)){
            if (! listeners.contains(listenerName)){
                listeners.add(listenerName);
            }
        }else {
            if (listeners.contains(listenerName)){
                listeners.remove(listenerName);
            }
        }
        String ll = GuiUtil.listToString(listeners, ",");
        vsAttrs.put("networkListeners", ll);
        handlerCtx.setOutputValue("result", vsAttrs);
    }

    @Handler(id="py.resolveDynamicPort",
        input={
            @HandlerInput(name = "listenerName", type = String.class, required = true),
            @HandlerInput(name = "configName", type = String.class, required = false, defaultValue = "server-config")
        }, output={
            @HandlerOutput(name="result", type=String.class)
        })
    public static void resolveDynamicPort(HandlerContext handlerCtx) {
        String listenerName = (String) handlerCtx.getInputValue("listenerName");
        String configName = (String) handlerCtx.getInputValue("configName");

        GrizzlyService grizzlyService = GuiUtil.getHabitat().getService(GrizzlyService.class);
        Domain domain = GuiUtil.getHabitat().getService(Domain.class);
        Config config = domain.getConfigNamed(configName);
        NetworkListener listener = config.getNetworkConfig().getNetworkListener(listenerName);

        // if DAS
        if (listener != null && configName.equals("server-config")) {
            handlerCtx.setOutputValue("result", grizzlyService.getRealPort(listener));
        } else if (listener != null) {
            handlerCtx.setOutputValue("result", listener.getPort());
        }
    }

}
