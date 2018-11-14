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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Susan Rai
 */
public class PayaraPropertiesHandlers {
    
        
    /**
     * 
     * Only get Configuration Properties.
     *
     * @param handlerCtx
     */
    @Handler(id = "py.onlyGetConfigurationProps",
            input = {
                @HandlerInput(name = "props", type = List.class, required = true)},
            output = {
                @HandlerOutput(name = "modifiedProps", type = List.class)}
    )
    public static void onlyGetConfigurationProps(HandlerContext handlerCtx) {
        List<Map<String, String>> props = (List<Map<String, String>>) handlerCtx.getInputValue("props");
        List<Map<String, String>> modifiedProps = new ArrayList<>();

        if (props != null) {
            for (Map<String, String> prop : props) {
                if (!prop.values().toString().contains("payara.microprofile")) {
                    modifiedProps.add(prop);
                }
            }
        }

        handlerCtx.setOutputValue("modifiedProps", modifiedProps);
    }
    
    /**
     * 
     * Only get MicroProfile Configuration Properties.
     *
     * @param handlerCtx
     */
    @Handler(id = "py.onlyGetMicroProfileProps",
            input = {
                @HandlerInput(name = "props", type = List.class, required = true)},
            output = {
                @HandlerOutput(name = "modifiedProps", type = List.class)}
    )
    public static void onlyGetMicroProfileProps(HandlerContext handlerCtx) {
        List<Map<String, String>> props = (List<Map<String, String>>) handlerCtx.getInputValue("props");
        List<Map<String, String>> modifiedProps = new ArrayList<>();

        if (props != null) {
            for (Map<String, String> prop : props) {
                if (prop.values().toString().contains("payara.microprofile")) {
                    modifiedProps.add(prop);
                }
            }
        }

        handlerCtx.setOutputValue("modifiedProps", modifiedProps);
    }
    
       /**
     * 
     * Convert MicroProfile properties to properties
     *
     * @param handlerCtx
     */
    @Handler(id = "py.convertToConfigProperties",
            input = {
                @HandlerInput(name = "props", type = List.class, required = true)},
            output = {
                @HandlerOutput(name = "convertedProps", type = List.class)}
    )
    public static void convertToConfigProperties(HandlerContext handlerCtx) {
        List<Map<String, String>> props = (List<Map<String, String>>) handlerCtx.getInputValue("props");
        List<Map<String, String>> convertedProps = new ArrayList<>();

        if (props != null) {
            for (Map<String, String> prop : props) {
                for (String key : prop.keySet()) {
                    if (key.equals("name")) {
                        if (prop.get(key).toString().contains("payara.microprofile")) {
                            prop.put(key, prop.get(key).toString().replaceAll("payara.microprofile.", ""));
                            convertedProps.add(prop);
                        }
                    }
                }
            }
        }

        handlerCtx.setOutputValue("convertedProps", convertedProps);
    }
    
      
    /**
     * 
     * Convert properties into MicroProfile Properties
     *
     * @param handlerCtx
     */
    @Handler(id = "py.convertToMicroProfileProperties",
            input = {
                @HandlerInput(name = "props", type = List.class, required = true)},
            output = {
                @HandlerOutput(name = "convertedProps", type = List.class)}
    )
    public static void convertToMicroProfileProperties(HandlerContext handlerCtx) {
        List<Map<String, String>> props = (List<Map<String, String>>) handlerCtx.getInputValue("props");
        List<Map<String, String>> convertedProps = new ArrayList<>();

        if (props != null) {
            for (Map<String, String> prop : props) {
                for (String key : prop.keySet()) {
                    if (key.equals("name")) {
                        if (!prop.get(key).toString().contains("payara.microprofile.")) {
                            prop.put(key, "payara.microprofile." + prop.get(key).toString().replaceAll("\\s+", ""));
                        }
                    }
                }
                convertedProps.add(prop);
            }
        }

        handlerCtx.setOutputValue("convertedProps", convertedProps);
    }
    
    /**
     *
     * Merge MicroProfile properties with Config Properties
     *
     * @param handlerCtx
     */
    @Handler(id = "py.mergeMicroProfileProperties",
            input = {
                @HandlerInput(name = "newProps", type = List.class, required = true)
                ,
                @HandlerInput(name = "oldProps", type = List.class, required = true)},
            output = {
                @HandlerOutput(name = "modifiedProps", type = List.class)}
    )
    public static void mergeMicroProfileProperties(HandlerContext handlerCtx) {
        List<Map<String, String>> newProps = (List<Map<String, String>>) handlerCtx.getInputValue("newProps");
        List<Map<String, String>> oldProps = (List<Map<String, String>>) handlerCtx.getInputValue("oldProps");
        List<Map<String, String>> modifiedProps = new ArrayList<>();

        if (newProps != null) {
            for (Map<String, String> prop : newProps) {
                modifiedProps.add(prop);
            }
        }

        if (oldProps != null) {
            for (Map<String, String> prop : oldProps) {
                if (prop.values().toString().contains("payara.microprofile")) {
                    modifiedProps.add(prop);
                }
            }
        }

        handlerCtx.setOutputValue("modifiedProps", modifiedProps);
    } 
    
    /**
     *
     * Merge Config properties with MicroProfile Properties
     *
     * @param handlerCtx
     */
    @Handler(id = "py.mergeConfigProperties",
            input = {
                @HandlerInput(name = "newProps", type = List.class, required = true)
                ,
                @HandlerInput(name = "currentPros", type = List.class, required = true)
                ,
                @HandlerInput(name = "oldProps", type = List.class, required = true)},
            output = {
                @HandlerOutput(name = "modifiedProps", type = List.class)}
    )
    public static void mergeConfigProperties(HandlerContext handlerCtx) {
        List<Map<String, String>> newProps = (List<Map<String, String>>) handlerCtx.getInputValue("newProps");
        List<Map<String, String>> currentPros = (List<Map<String, String>>) handlerCtx.getInputValue("currentPros");
        List<Map<String, String>> oldProps = (List<Map<String, String>>) handlerCtx.getInputValue("oldProps");
        List<Map<String, String>> modifiedProps = new ArrayList<>();

        if (currentPros.size() > 0 && currentPros.size() > newProps.size()) {
            for (Map<String, String> prop : currentPros) {
                if (newProps.contains(prop)) {
                    modifiedProps.add(prop);
                }
            }
        } else {
            for (Map<String, String> prop : newProps) {
                modifiedProps.add(prop);
            }
        }

        if (oldProps != null) {
            for (Map<String, String> prop : oldProps) {
                if (!prop.values().toString().contains("payara.microprofile")
                        && !currentPros.contains(prop)) {
                    modifiedProps.add(prop);
                }
            }
        }

        handlerCtx.setOutputValue("modifiedProps", modifiedProps);
    } 
}
