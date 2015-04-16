/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Provider;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;

import static org.glassfish.admin.rest.utils.Util.decode;


/**
 * @author Ludovic Champenois
 */
public abstract class LeafResource extends AbstractResource {

    protected LeafContent entity;
    protected Dom parent;
    protected String tagName;

    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(LeafResource.class);

    /** Creates a new instance of xxxResource */
    public LeafResource() {
    }

    public void setEntity(LeafContent p) {
        entity = p;
    }

    public LeafContent getEntity() {
        return entity;
    }

    public void setParentAndTagName(Dom parent, String tagName) {
        this.parent = parent;
        this.tagName = tagName;
        entity = new LeafContent();
        entity.name = tagName;//parent.leafElements(tagName);
        entity.value = parent.leafElement(tagName);


    }

    @GET
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5", MediaType.APPLICATION_FORM_URLENCODED+";qs=0.5"})
    public ActionReportResult get(@QueryParam("expandLevel") @DefaultValue("1") int expandLevel) {
        if (getEntity() == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return buildActionReportResult();
    }

    @POST //create
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5", MediaType.APPLICATION_FORM_URLENCODED+";qs=0.5"})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    public ActionReportResult create(HashMap<String, String> data) {
        //hack-1 : support delete method for html
        //Currently, browsers do not support delete method. For html media,
        //delete operations can be supported through POST. Redirect html
        //client POST request for delete operation to DELETE method.
        if ((data.containsKey("operation")) &&
                (data.get("operation").equals("__deleteoperation"))) {
            data.remove("operation");
            return delete(data);
        }
        return null;
        //TODO

////        String postCommand = getPostCommand();
////        final Map<String, String> payload = processData(data, postCommand);
////
////        return runCommand(postCommand, payload, "rest.resource.create.message",
////            "\"{0}\" created successfully.", "rest.resource.post.forbidden","POST on \"{0}\" is forbidden.");
    }

    @DELETE //delete
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5", MediaType.APPLICATION_FORM_URLENCODED+";qs=0.5"})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    public ActionReportResult delete(HashMap<String, String> data) {
        ResourceUtil.addQueryString(uriInfo.getQueryParameters(), data);

        return null;//TODOTODO


    }

    @OPTIONS
    @Produces({MediaType.APPLICATION_JSON+";qs=0.5", "text/html", MediaType.APPLICATION_XML+";qs=0.5"})
    public ActionReportResult options() {
        return buildActionReportResult();
    }

    protected ActionReportResult buildActionReportResult() {
        RestActionReporter ar = new RestActionReporter();
        final String typeKey = (decode(getName()));
        ar.setActionDescription(typeKey);
        ar.getExtraProperties().put("entityLeaf", getEntity());

        OptionsResult optionsResult = new OptionsResult(Util.getResourceName(uriInfo));
        Map<String, MethodMetaData> mmd = getMethodMetaData();
        optionsResult.putMethodMetaData("GET", mmd.get("GET"));
        optionsResult.putMethodMetaData("POST", mmd.get("POST"));

        ResourceUtil.addMethodMetaData(ar, mmd);
        ActionReportResult r= new ActionReportResult(ar, optionsResult);
        r.setLeafContent(entity);
        return r;
    }

    protected Map<String, MethodMetaData> getMethodMetaData() {
        Map<String, MethodMetaData> mmd = new TreeMap<String, MethodMetaData>();
        //GET meta data
        mmd.put("GET", new MethodMetaData());

        return mmd;
    }



    protected String getName() {
        return Util.getResourceName(uriInfo);
    }


    public static class LeafContent {

        public String name;
        public String value;
    }


}
