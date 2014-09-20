/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.common.util;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

import javax.faces.context.FacesContext;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * This class is intended to replace, eventually, RestUtil. Whether or not that happens depends in large part on how the
 * new console is built. In the meantime, though, there is a need for REST-related utilities that behave in a manner not
 * supported by the current methods in RestUtil, so these need to be written. Rather than place these new methods beside
 * the existing ones, we will, for now, separate the two to maintain ease in identifying old vs new methods, as well as
 * helping to ensure that these methods remain purely REST-focused, free from the encroachment of view-related libraries
 * (see the pervasiveness of JSFTemplating types in RestUtil).
 *
 * @author jdlee
 */
public class RestUtil2 {

    @Handler(id = "rest.list",
            input = {
                    @HandlerInput(name="endpoint", type=String.class, required=true),
                    @HandlerInput(name="attrs", type=Map.class, required=false)},
            output = {
                    @HandlerOutput(name="result", type=List.class)})
    public static void sendRequestToCollectionResource(HandlerContext handlerCtx) {
        //Map<String, Object> attrs = (Map<String, Object>) handlerCtx.getInputValue("attrs");
        String endpoint = fixEndpoint((String) handlerCtx.getInputValue("endpoint"));

        Response resp = RestUtil.getJerseyClient().target(endpoint)
                .request(RestUtil.RESPONSE_TYPE)
                .cookie(new Cookie(RestUtil.REST_TOKEN_COOKIE, RestUtil.getRestToken()))
                .get(Response.class);
        if (!isSuccess(resp.getStatus())) {
            throw new RuntimeException(resp.readEntity(String.class));
        }

        List list = resp.readEntity(List.class);
        handlerCtx.setOutputValue("result", list);
    }

    @Handler(id = "rest.get",
            input = {
                    @HandlerInput(name="endpoint", type=String.class, required=true),
                    @HandlerInput(name="attrs", type=Map.class, required=false)},
            output = {
                    @HandlerOutput(name="result", type=Map.class)})
    public static void sendGetRequestToItemResource(HandlerContext handlerCtx) {
        Map<String, Object> attrs = (Map<String, Object>) handlerCtx.getInputValue("attrs");
        String endpoint = fixEndpoint((String) handlerCtx.getInputValue("endpoint"));

        Response resp = RestUtil.targetWithQueryParams(RestUtil.getJerseyClient().target(endpoint),
                RestUtil.buildMultivalueMap(attrs))
                .request(RestUtil.RESPONSE_TYPE)
                .cookie(new Cookie(RestUtil.REST_TOKEN_COOKIE, RestUtil.getRestToken()))
                .get(Response.class);
//        Response resp = makeRequest(endpoint, attrs).get(Response.class);

        Map map = resp.readEntity(Map.class);
        handlerCtx.setOutputValue("result", map);
    }

    @Handler(id = "rest.post",
            input = {
                    @HandlerInput(name="endpoint", type=String.class, required=true),
                    @HandlerInput(name="attrs", type=Map.class, required=false)},
            output = {
                    @HandlerOutput(name="result", type=String.class)})
    public static void sendPostRequest(HandlerContext handlerCtx) {
        Map<String, Object> attrs = (Map<String, Object>) handlerCtx.getInputValue("attrs");
        String endpoint = fixEndpoint((String) handlerCtx.getInputValue("endpoint"));

//        Response resp = makeRequest(endpoint, null).post(Response.class, attrs);

        Response resp = RestUtil.getJerseyClient().target(endpoint)
                .request(RestUtil.RESPONSE_TYPE)
                .cookie(new Cookie(RestUtil.REST_TOKEN_COOKIE, RestUtil.getRestToken()))
                .post(Entity.entity(attrs, MediaType.APPLICATION_JSON_TYPE), Response.class);

        if (!isSuccess(resp.getStatus())) {
            GuiUtil.getLogger().log(
                    Level.SEVERE,
                    GuiUtil.getCommonMessage("LOG_UPDATE_ENTITY_FAILED", new Object[]{endpoint, attrs}));
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.error.checkLog"));
            return;
        }

        handlerCtx.setOutputValue("result", endpoint);
    }

    private static String fixEndpoint(String endpoint) {
        if (endpoint.startsWith("/")) {
            endpoint = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("REST_URL") + endpoint;
        }
        return endpoint;
    }

//    private static Builder makeRequest(String endpoint, Map<String, Object> queryParams) {
//        Target resource = RestUtil.getJerseyClient().target(endpoint);
//        if (queryParams != null) {
//            resource =  resource.queryParams(RestUtil.buildMultivalueMap(queryParams));
//        }
//        return resource
//                .type(MediaType.APPLICATION_JSON)
//                .cookie(new Cookie(RestUtil.REST_TOKEN_COOKIE, RestUtil.getRestToken()))
//                .accept(RestUtil.RESPONSE_TYPE);
//    }

    protected static boolean isSuccess(int status) {
        return (status >= 200) && (status <= 299);
    }
}
