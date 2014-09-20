/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jca.admingui.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


import java.util.StringTokenizer;
import org.glassfish.admingui.common.util.GuiUtil;


public class ConnectorsHandlers {

    /** Creates a new instance of ConnectorsHandler */
    public ConnectorsHandlers() {
    }


    /**
     *	<p> This handler creates a ConnectorConnection Pool to be used in the wizard
     */
    @Handler(id = "gf.getConnectorConnectionPoolWizard", input = {
        @HandlerInput(name = "fromStep2", type = Boolean.class),
        @HandlerInput(name = "fromStep1", type = Boolean.class),
        @HandlerInput(name = "attrMap", type = Map.class),
        @HandlerInput(name = "poolName", type = String.class),
        @HandlerInput(name = "resAdapter", type = String.class)
        }, output = {
        @HandlerOutput(name = "resultAdapter", type = String.class)
    })
    public static void getConnectorConnectionPoolWizard(HandlerContext handlerCtx) {
        Boolean fromStep2 = (Boolean) handlerCtx.getInputValue("fromStep2");
        Boolean fromStep1 = (Boolean) handlerCtx.getInputValue("fromStep1");
        if ((fromStep2 != null) && fromStep2) {
            //wizardMap is already in session map, we don't want to change anything.
            Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
            String resAdapter = (String) extra.get("resourceAdapterName");
            handlerCtx.setOutputValue("resultAdapter", resAdapter);
        } else if ((fromStep1 != null) && fromStep1) {
            //this is from Step 1 where the page is navigated when changing the dropdown of resource adapter.
            //since the dropdown is immediate, the wizardPoolExtra map is not updated yet, we need
            //to update it manually and also set the connection definition map according to this resource adapter.
            String resAdapter = (String) handlerCtx.getInputValue("resAdapter");
            if (resAdapter != null) {
                resAdapter = resAdapter.trim();
            }
            String poolName = (String) handlerCtx.getInputValue("poolName");
            if (poolName != null) {
                poolName = poolName.trim();
            }
            if (resAdapter != null && !(resAdapter.equals(""))) {               
                Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
                extra.put("resourceAdapterName", resAdapter);
                extra.put("name", poolName);
                handlerCtx.setOutputValue("resultAdapter", resAdapter);
            }
        } else {
            Map extra = new HashMap();
            Map attrMap = (Map) handlerCtx.getInputValue("attrMap");
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardMap", attrMap);
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolExtra", extra);
        }
    }

    /**
     *	<p> updates the wizard map
     */
    @Handler(id = "gf.updateConnectorConnectionPoolWizard",
        input = {
            @HandlerInput(name = "props", type = List.class),
            @HandlerInput(name = "currentAdapter", type = String.class),
            @HandlerInput(name = "currentDef", type = String.class),
            @HandlerInput(name = "hasConfidential", type = Boolean.class)})
    public static void updateConnectorConnectionPoolWizard(HandlerContext handlerCtx) {
        List<Map> props = (List<Map>) handlerCtx.getInputValue("props");
        Boolean hasConfidential =  (Boolean) handlerCtx.getInputValue("hasConfidential");
        if (props != null) {
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", props);
        } else {
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", new ArrayList());
        }
        handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("hasConfidential", hasConfidential);
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
        extra.put("previousDefinition", (String) handlerCtx.getInputValue("currentDef"));
        extra.put("previousResAdapter", (String) handlerCtx.getInputValue("currentAdapter"));
    }
    
    /**
     *	<p> updates the wizard map properties on step 2
     */
    @Handler(id = "updateConnectorConnectionPoolWizardStep2")
    public static void updateConnectorConnectionPoolWizardStep2(HandlerContext handlerCtx) {
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
        Map attrs = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardMap");

        String resAdapter = (String) extra.get("resourceAdapterName");
        String definition = (String) extra.get("connectiondefinitionname");
        String name = (String) extra.get("name");

        attrs.put("name", name);
        attrs.put("connectiondefinitionname", definition);
        attrs.put("resourceAdapterName", resAdapter);
    }
      
    /**
     *	<p> This handler creates a Admin Object Resource
     */
    @Handler(id = "gf.getAdminObjectResourceWizard", input = {
    @HandlerInput(name = "reload", type = Boolean.class),
    @HandlerInput(name = "attrMap", type = Map.class),
    @HandlerInput(name = "currentMap", type = Map.class)
    }, output = {
    @HandlerOutput(name = "valueMap", type = Map.class)
    })
    public static void getAdminObjectResourceWizard(HandlerContext handlerCtx) {
        Boolean reload = (Boolean) handlerCtx.getInputValue("reload");
        Map attrMap = (Map) handlerCtx.getInputValue("attrMap");
        Map currentMap = (Map) handlerCtx.getInputValue("currentMap");
        String name = null;
        String resAdapter = null;
        String resType = null;
        String className = null;
        if (attrMap == null) {
            attrMap = new HashMap();
        }
        if (((reload == null) || (!reload)) && (currentMap != null)) {
            name = (String) currentMap.get("name");
            resAdapter = (String) currentMap.get("resAdapter");
            resType = (String) currentMap.get("resType");
            className = (String) currentMap.get("className");
            attrMap.putAll(currentMap);
        } else {
            name = (String) attrMap.get("name");
            resAdapter = (String) attrMap.get("resAdapter");
            resType = (String) attrMap.get("resType");
            className = (String) attrMap.get("className");
        }
        if (resAdapter != null) {
            resAdapter = resAdapter.trim();
        }
        if (GuiUtil.isEmpty(resAdapter) && Boolean.parseBoolean((String)GuiUtil.getSessionValue("_jms_exist"))){
            resAdapter = "jmsra";
        }

        attrMap.put("name", name);
        attrMap.put("resType", resType);
        attrMap.put("resAdapter", resAdapter);
        attrMap.put("className", className);
        handlerCtx.setOutputValue("valueMap", attrMap);
    }
    
    /**
     *	<p> If the RAR is an embedded rar, we don't wan to show the .rar extension.
     *  <p> eg. myjca.ear containing myTest.rar  will be shown as myjca.ear#myTest
     */
    @Handler(id = "filterOutRarExtension",
        input = {
            @HandlerInput(name = "inList", type = java.util.List.class, required = true)},
        output = {
            @HandlerOutput(name = "convertedList", type = java.util.List.class)
    })
    public static void filterOutRarExtension(HandlerContext handlerCtx) {
        List<String> inList = (List) handlerCtx.getInputValue("inList");
        List<String> convertedList = new ArrayList();
        for(String one: inList){
            if( (one.indexOf("#") != -1) && one.endsWith(".rar")){
                convertedList.add( one.substring(0, one.length() - 4));
            }else{
                convertedList.add(one);
            }
        }
        handlerCtx.setOutputValue("convertedList", convertedList);
    }



   public static Map stringToMap(String str, String delimiter) {
        Map props = new HashMap();
         if ( str != null && delimiter != null) {
            StringTokenizer tokens = new StringTokenizer(str, delimiter);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken().trim();
                String values[] = token.split("=");
                if(values.length == 2) {
                    props.put(values[0], values[1]);
                }
            }
        }
        return props;
    }

    public static final String ADMINOBJECT_INTERFACES_KEY = "AdminObjectInterfacesKey";
    public static final String ADMINOBJECT_CLASSES_KEY = "AdminObjectClassesKey";
    public static final String CONNECTION_DEFINITION_NAMES_KEY = "ConnectionDefinitionNamesKey";
    public static final String MCF_CONFIG_PROPS_KEY = "McfConfigPropsKey";
    public static final String SYSTEM_CONNECTORS_KEY = "SystemConnectorsKey";
    public static final String ADMINOBJECT_CONFIG_PROPS_KEY = "AdminObjectConfigPropsKey";
    public static final String RESOURCE_ADAPTER_CONFIG_PROPS_KEY = "ResourceAdapterConfigPropsKey";


}
 
