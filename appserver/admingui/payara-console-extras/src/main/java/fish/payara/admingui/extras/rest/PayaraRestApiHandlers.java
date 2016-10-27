/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.admingui.extras.rest;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;

/**
 * A class containing Payara specific handler methods for the REST API
 * @author Andrew Pielage
 */
public class PayaraRestApiHandlers
{
    /**
     * Gets information about the instances current registered to the Hazelcast cluster.
     * @param handlerCtx 
     */
    @Handler(id = "py.getHazelcastInstances",
            input = {
                @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
                @HandlerInput(name = "type", type = String.class)},
            output = {
                @HandlerOutput(name = "result", type = java.util.List.class)
    })
    public static void getHazelcastClusterMembers(HandlerContext handlerCtx) {
        String parentEndpoint = (String) handlerCtx.getInputValue("parentEndpoint");
        String type = (String) handlerCtx.getInputValue("type");
        String endpoint;
        
        // Check for trailing slashes
        endpoint = parentEndpoint.endsWith("/") ? parentEndpoint + "list-hazelcast-cluster-members" : parentEndpoint 
                + "/" + "list-hazelcast-cluster-members";
        
        try {       
            // Set a target type if one has been provided
            if (type != null) {
                if (type.equals("micro")) {
                    endpoint += "?type=micro";
                } else if (type.equals("server")) {
                    endpoint += "?type=server";
                }
            }
                
            // Run the list-hazelcast-cluster-members command using the Rest endpoint
            Map responseMap = RestUtil.restRequest(endpoint, null, "GET", handlerCtx, false, true);
            Map data = (Map) responseMap.get("data");
           
            // Extract the information from the Map and place it in a List for representation in the dataTable
            List<Map> instances = new ArrayList<>();
            if (data != null) {
                Map extraProperties = (Map) data.get("extraProperties");
                if (extraProperties != null) {
                    try {
                        instances = (List<Map>) extraProperties.get("members");
                        if (instances == null) {
                            // Re-initialise to empty if members is not found
                            instances = new ArrayList<>();
                        }
                    } catch (ClassCastException ex) {
                        // This exception should only be caught if Hazelcast is not enabled, as the command returns a 
                        // String instead of a List. In such a case, re-initialise to an empty List
                        instances = new ArrayList<>();
                    } 
                }
            }
            
            handlerCtx.setOutputValue("result", instances);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
}
