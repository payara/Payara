/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.admin.rest.resources;



import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.api.ActionReport;

import javax.ws.rs.DELETE;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.glassfish.common.util.admin.RestSessionManager;

/**
 * Represents a session with GlassFish Rest service
 * @author Mitesh Meswani
 */
public class SessionResource {

    private final String sessionId;
    private final HttpHeaders requestHeaders;
    private final UriInfo uriInfo;

    RestSessionManager sessionManager;

    public SessionResource(RestSessionManager sessionManager, String sessionId, HttpHeaders requestHeaders, UriInfo uriInfo) {
        this.sessionManager = sessionManager;
        this.sessionId = sessionId;
        this.requestHeaders = requestHeaders;
        this.uriInfo = uriInfo;
    }

    @DELETE
    public Response delete() {
        Response.Status status;
        ActionReport.ExitCode exitCode;
        String message;
        if(!sessionManager.deleteSession(sessionId)) {
            status = Response.Status.BAD_REQUEST;
            exitCode = ActionReport.ExitCode.FAILURE;
            message = "Session with id " + sessionId + " does not exist";
        } else {
            status = Response.Status.OK;
            exitCode = ActionReport.ExitCode.SUCCESS;
            message = "Session with id " + sessionId + " deleted";
        }

        return Response.status(status).entity(ResourceUtil.getActionReportResult(exitCode, message, requestHeaders, uriInfo) ).build();

    }

}
