/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.rest.app.service;

import static fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication.APPLICATION_YAML;
import static java.util.logging.Level.WARNING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.OpenAPIBuildException;
import fish.payara.microprofile.openapi.impl.OpenApiService;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;

@Path("/")
public class OpenApiResource {

    private static final Logger LOGGER = Logger.getLogger(OpenApiResource.class.getName());

    @GET
    @Produces({ APPLICATION_YAML, APPLICATION_JSON })
    public Response getResponse(@Context HttpServletResponse response) throws IOException {

        // If the server is disabled, throw an error
        if (!OpenApiService.getInstance().isEnabled()) {
            response.sendError(FORBIDDEN.getStatusCode(), "OpenAPI Service is disabled.");
            return Response.status(FORBIDDEN).build();
        }

        // Get the OpenAPI document
        OpenAPI document = null;
        try {
            document = OpenApiService.getInstance().getDocument();
        } catch (OpenAPIBuildException ex) {
            LOGGER.log(WARNING, "OpenAPI document creation failed.", ex);
        }

        // If there are none, return an empty OpenAPI document
        if (document == null) {
            LOGGER.info("No OpenAPI document found.");
            return Response.status(Status.NOT_FOUND).entity(new OpenAPIImpl()).build();
        }

        // Return the document
        return Response.ok(document).build();
    }
}