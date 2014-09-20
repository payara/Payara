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
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
 */

/*
 * ConnectorHandlers.java
 *
 * Created on Sept 1, 2006, 8:32 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 */
package org.glassfish.full.admingui.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.Set;


public class JndiHandlers {

    /** Creates a new instance of ConnectorsHandler */
    public JndiHandlers() {
    }



    @Handler(id="gf.getJndiResourceForCreate",
    input={
        @HandlerInput(name="resources", type=Map.class)},
    output={
        @HandlerOutput(name="result", type=List.class),
        @HandlerOutput(name="attrMap", type=Map.class),
        @HandlerOutput(name="classnameOption", type=String.class),
        @HandlerOutput(name="factoryMap", type=String.class)})
    public static void getJndiResourceForCreate(HandlerContext handlerCtx) {

        Map<String, String> emap = (Map<String, String>) handlerCtx.getInputValue("resources");
        List result = new ArrayList();
        result.addAll(emap.keySet());
        String factoryMap = getFactoryMap(emap);

        handlerCtx.setOutputValue("result",result);
        handlerCtx.setOutputValue("classnameOption", "predefine");
        Map attrMap = new HashMap();
        attrMap.put("predefinedClassname", Boolean.TRUE);
        handlerCtx.setOutputValue("attrMap", attrMap);
        handlerCtx.setOutputValue("factoryMap", factoryMap);
    }

    @Handler(id="gf.getJndiResourceAttrForEdit",
    input={
        @HandlerInput(name="resType", type=String.class),
        @HandlerInput(name="resources", type=Map.class)},
    output={
        @HandlerOutput(name="attrMap",      type=Map.class),
        @HandlerOutput(name="classnameOption",      type=String.class),
        @HandlerOutput(name="result",      type=List.class),
        @HandlerOutput(name="factoryMap", type=String.class)})

    public static void getJndiResourceAttrForEdit(HandlerContext handlerCtx) {
        List result = new ArrayList();
        Map<String, String> emap = (Map<String, String>) handlerCtx.getInputValue("resources");
        result.addAll(emap.keySet());
        String factoryMap = getFactoryMap(emap);
        handlerCtx.setOutputValue("factoryMap", factoryMap);

        String resType = (String) handlerCtx.getInputValue("resType");
        Map attrMap = new HashMap();
        if (emap.containsKey(resType)) {
            handlerCtx.setOutputValue("classnameOption", "predefine");
            attrMap.put("predefinedClassname", Boolean.TRUE);
            attrMap.put("classname", resType);

        } else {
            //Custom realm class
            handlerCtx.setOutputValue("classnameOption", "input");
            attrMap.put("predefinedClassname", Boolean.FALSE);
            attrMap.put("classnameInput", resType);

        }
        handlerCtx.setOutputValue("result", result);
        handlerCtx.setOutputValue("attrMap", attrMap);
    }

    private static String getFactoryMap(Map<String,String> emap){
        String factMap = null;
        StringBuilder factoryMap = new StringBuilder();
        if(emap != null) {
            String sep = "";
            for(Map.Entry<String,String> e : emap.entrySet()){
                    factoryMap.append(sep)
                            .append("\"")
                            .append(e.getKey())
                            .append("\": '")
                            .append(e.getValue())
                            .append("'");
                    sep = ",";
            }
        }
        factMap = "{" + factoryMap.toString() + "}";
        return factMap;
    }

        @Handler(id="updateJndiResourceAttrs",
        input={
                @HandlerInput(name="classnameOption",   type=String.class),
                @HandlerInput(name="attrMap",      type=Map.class)},
        output={
            @HandlerOutput(name="resType", type=String.class)
            } )
        public static void updateJndiResourceAttrs(HandlerContext handlerCtx){
            String option = (String) handlerCtx.getInputValue("classnameOption");
            Map<String,String> attrMap = (Map)handlerCtx.getInputValue("attrMap");
            String restype ;
            if (option.equals("predefine")) {
                restype = attrMap.get("classname");
            } else {
                restype = attrMap.get("classnameInput");
            }
            handlerCtx.setOutputValue("resType", restype);
        }
}
