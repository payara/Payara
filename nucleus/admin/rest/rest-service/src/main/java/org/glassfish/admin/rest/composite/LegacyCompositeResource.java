/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.OptionsCapable;
import org.glassfish.admin.rest.composite.metadata.DefaultsGenerator;
import org.glassfish.admin.rest.composite.metadata.RestResourceMetadata;
import org.glassfish.admin.rest.model.ResponseBody;
import org.glassfish.admin.rest.model.RestModelResponseBody;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.api.admin.ParameterMap;

/**
 * This is the base class for all legacy composite resources. It provides all of the basic configuration and utilities needed
 * by composites.  For top-level resources, the <code>@Path</code> and <code>@Service</code> annotations are still
 * required, though, in order for the resource to be located and configured properly.
 * @author jdlee
 */
public abstract class LegacyCompositeResource extends CompositeResource implements DefaultsGenerator, OptionsCapable {

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    @Override
    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    /**
     * This method will handle any OPTIONS requests for composite resources.
     * @return
     * @throws JSONException
     */
    @OPTIONS
    public String options() throws JSONException {
        RestResourceMetadata rrmd = new RestResourceMetadata(this);
        return rrmd.toJson().toString(Util.getFormattingIndentLevel());
    }

    @Override
    public Object getDefaultValue(String propertyName) {
        return null;
    }

    protected Response legacyCreated(String name, String message, RestModel model) {
        RestModelResponseBody rb = legacyResponseBody(RestModel.class);
        rb.setEntity(model);
        rb.addSuccess(message);
        return legacyCreated(getChildItemUri(name), rb);
    }
    protected Response legacyCreated(URI location, RestModelResponseBody responseBody) {
        return Response
                .status(Status.CREATED)
                .header("Location", location)
                .entity(responseBody).build();
    }

    protected Response legacyUpdated(String message, RestModel model) {
        RestModelResponseBody<RestModel> rb = legacyResponseBody(RestModel.class);
        rb.setEntity(model);
        rb.addSuccess(message);
        return legacyUpdated(rb);
    }
    protected Response legacyUpdated(ResponseBody responseBody) {
        return Response.ok().entity(responseBody).build();
    }

    protected Response legacyDeleted(String message) {
        return deleted(responseBody().addSuccess(message));
    }
    protected Response legacyDeleted(ResponseBody responseBody) {
        return Response.ok().entity(responseBody).build();
    }

    protected Response legacyAccepted(String command, ParameterMap parameters) {
        return legacyAccepted(command, parameters, null);
    }
    protected Response legacyAccepted(String command, ParameterMap parameters, URI childUri) {
        URI jobUri = launchDetachedCommand(command, parameters);
        ResponseBuilder rb = Response.status(Response.Status.ACCEPTED).header("Location", jobUri);
        if (childUri != null) {
            rb.header("X-Location", childUri);
        }
        return rb.build();
    }

    protected <T extends RestModel> RestModelResponseBody<T> legacyResponseBody(Class<T> modelIface) {
        return restModelResponseBody(modelIface);
    }
}
