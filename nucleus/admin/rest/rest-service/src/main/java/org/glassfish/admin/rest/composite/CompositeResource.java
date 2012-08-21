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
package org.glassfish.admin.rest.composite;

import com.sun.enterprise.v3.common.ActionReporter;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestResource;
import org.glassfish.admin.rest.composite.metadata.DefaultsGenerator;
import org.glassfish.admin.rest.composite.metadata.RestResourceMetadata;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.security.services.common.SubjectUtil;
import org.jvnet.hk2.component.Habitat;

/**
 * This is the base class for all composite resources. It provides all of the basic configuration and utilities needed
 * by composites.  For top-level resources, the <code>@Path</code> and <code>@Service</code> annotations are still
 * required, though, in order for the resource to be located and configured properly.
 * @author jdlee
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class CompositeResource implements RestResource, DefaultsGenerator {

    @Context
    protected UriInfo uriInfo;
    @Inject
    protected Ref<Request> requestRef;
    @Inject
    protected Habitat habitat;
    protected Subject subject;
    private String authenticatedUser;
    protected CompositeUtil compositeUtil = CompositeUtil.instance();

    /**
     * This method will return the Subject associated with the current request.
     * @return
     */
    protected Subject getSubject() {
        if (subject == null) {
            Request req = requestRef.get();
            subject = (Subject) req.getAttribute(Constants.REQ_ATTR_SUBJECT);
        }
        
        return subject;
    }

    /**
     * This method will return the authenticated user associated with the current request.
     * @return
     */
    protected String getAuthenticatedUser() {
        if (authenticatedUser == null) {
            Subject s = getSubject();
            if (s != null) {
                List<String> list = SubjectUtil.getUsernamesFromSubject(s);
                if (list != null) {
                    authenticatedUser = list.get(0);
                }
            }
        }

        return authenticatedUser;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setRequestRef(Ref<Request> requestRef) {
        this.requestRef = requestRef;
    }

    public void setHabitat(Habitat habitat) {
        this.habitat = habitat;
    }

    @Override
    public String getDefaultValue(String propertyName) {
        return null;
    }


    /**
     * This method will handle any OPTIONS requests for composite resources. 
     * @return
     * @throws JSONException 
     */
    @OPTIONS
    public String options() throws JSONException {
        RestResourceMetadata rrmd = new RestResourceMetadata(this);
        return rrmd.toJson().toString(getFormattingIndentLevel());
    }

    /**
     * This method creates a sub-resource of the specified type. Since the JAX-RS does not allow for injection into
     * sub-resources (as it doesn't know or control the lifecycle of the object), this method performs a manual
     * "injection" of the various system objects the resource might need. If the requested Class can not be instantiated
     * (e.g., it does not have a no-arg public constructor), the system will throw a <code>WebApplicationException</code>
     * with an HTTP status code of 500 (internal server error).
     * 
     * @param clazz The Class of the desired sub-resource
     * @return
     */
    public <T> T getSubResource(Class<T> clazz) {
        try {
            T resource = clazz.newInstance();
            CompositeResource cr = (CompositeResource)resource;
            cr.setHabitat(habitat);
            cr.setRequestRef(requestRef);
            cr.setUriInfo(uriInfo);
            
            return resource;
        } catch (Exception ex) {
            throw new WebApplicationException(ex, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the current configured indenting value for the REST layer
     * @return
     */
    protected int getFormattingIndentLevel() {
        RestConfig rg = ResourceUtil.getRestConfig(habitat);
        if (rg == null) {
            return -1;
        } else {
            return Integer.parseInt(rg.getIndentLevel());
        }

    }

    /**
     * Execute an <code>AdminCommand</code> with no parameters
     * @param command
     * @return
     */
    protected ActionReporter executeCommand(String command) {
        return executeCommand(command, new ParameterMap());
    }

    /**
     * Execute an <code>AdminCommand</code> with the specified parameters.
     * @param command
     * @param parameters
     * @return
     */
    protected ActionReporter executeCommand(String command, ParameterMap parameters) {
        RestActionReporter ar = ResourceUtil.runCommand(command, parameters,
                Globals.getDefaultHabitat(), "", getSubject()); //TODO The last parameter is resultType and is not used. Refactor the called method to remove it
        if (ar.getActionExitCode().equals(ExitCode.FAILURE)) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).
                    entity(ar.getTopMessagePart().getMessage()).
                    build());
        }
        return ar;
    }

    /**
     * Every resource that returns a collection will need to return the URI for each item in the colleciton. This method
     * handles the creation of that URI, ensuring a correct and consistent URI pattern.
     * @param name
     * @return
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     */
    protected URI getChildItemUri(String name) throws IllegalArgumentException, UriBuilderException {
        return uriInfo.getBaseUriBuilder().path("id").path(name).build();
    }
}
