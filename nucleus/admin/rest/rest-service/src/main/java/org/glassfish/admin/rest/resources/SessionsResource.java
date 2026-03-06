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
 */

// Portions Copyright 2016-2026 Payara Foundation and/or affiliates

package org.glassfish.admin.rest.resources;

import java.util.HashMap;
import java.util.Optional;

import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.config.serverbeans.Domain;
import jakarta.inject.Inject;
import javax.security.auth.Subject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.common.util.admin.RestSessionManager;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.RemoteAdminAccessException;
import org.glassfish.jersey.internal.util.collection.Ref;

/**
 * Represents sessions with GlassFish Rest service
 * @author Mitesh Meswani
 */
@Path("/sessions")
public class SessionsResource extends AbstractResource {
    @Context
    RestSessionManager sessionManager;

    @Inject
    private Ref<Request> request;

    private static volatile DasConfig dasConfig = null;

    /**
     * Get a new session with GlassFish Rest service
     * If a request lands here when authentication has been turned on => it has been authenticated.
     * @return a new session with GlassFish Rest service
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response create(HashMap<String, String> data) {
        if (data == null) {
            data = new HashMap<String, String>();
        }
        Response.ResponseBuilder responseBuilder = Response.status(UNAUTHORIZED);
        RestActionReporter ar = new RestActionReporter();
        Request grizzlyRequest = request.get();

        // If the call flow reached here, the request has been authenticated by logic in RestAdapater
        // probably with an admin username and password.  The remoteHostName value
        // in the data object is the actual remote host of the end-user who is
        // using the console (or, conceivably, some other client).  We need to
        // authenticate here once again with that supplied remoteHostName to
        // make sure we enforce remote access rules correctly.
        String hostName = data.get("remoteHostName");
        boolean isAuthorized = false;
        boolean responseErrorStatusSet = false;
        Subject subject = null;
        try {
            subject = ResourceUtil.authenticateViaAdminRealm(locatorBridge.getRemoteLocator(), grizzlyRequest, hostName);
            isAuthorized = ResourceUtil.isAuthorized(locatorBridge.getRemoteLocator(), subject, "domain/rest-sessions/rest-session", "create");
        } catch (RemoteAdminAccessException e) {
            responseBuilder.status(FORBIDDEN);
            responseErrorStatusSet = true;
        } catch (Exception e) {
            ar.setMessage("Error while authenticating " + e);
        }

        if (isAuthorized) {
            responseBuilder.status(OK);

            // Check to see if the username has been set (anonymous user case)
            String username = (String) grizzlyRequest.getAttribute("restUser");
            if (username != null) {
                ar.getExtraProperties().put("username", username);
            }
            ar.getExtraProperties().put("token", sessionManager.createSession(grizzlyRequest.getRemoteAddr(), subject, chooseTimeout(getDASConfig(locatorBridge.getRemoteLocator()))));

        } else {
            if ( ! responseErrorStatusSet) {
                responseBuilder.status(UNAUTHORIZED);
            }
        }

        return responseBuilder.entity(new ActionReportResult(ar)).build();
    }

    private int chooseTimeout(final DasConfig dasConfig) {
        int inactiveSessionLifeTime = 30 /*mins*/;
        if (dasConfig != null) {
            inactiveSessionLifeTime = Integer.parseInt(dasConfig.getAdminSessionTimeoutInMinutes());
        }
        return inactiveSessionLifeTime;
    }

    @Path("{sessionId}/")
    public SessionResource getSessionResource(@PathParam("sessionId")String sessionId) {
        return new SessionResource(sessionManager, sessionId, requestHeaders, uriInfo);
    }

    private static DasConfig getDASConfig(ServiceLocator habitat) {
        if (dasConfig == null) {
            synchronized (SessionsResource.class) {
                if (dasConfig != null) {
                    return dasConfig;
                }

                Optional.ofNullable(habitat).map(
                        serviceLocator -> Globals.getDefaultBaseServiceLocator().getService(Domain.class)
                    ).map(
                            domain -> domain.getConfigNamed("server-config")
                    ).map(
                            Config::getAdminService
                    ).map(
                            AdminService::getDasConfig
                    ).ifPresent(config -> dasConfig = config);
            }
        }

        return dasConfig;
    }
}
