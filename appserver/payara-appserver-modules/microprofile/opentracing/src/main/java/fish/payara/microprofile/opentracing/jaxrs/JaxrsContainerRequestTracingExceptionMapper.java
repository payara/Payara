/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.opentracing.jaxrs;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;

/**
 * ExceptionMapper that catches all Exceptions. We need this because we need to add details about any exceptions to the
 * active span - if this isn't here, we don't go back through the container filter.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Priority(Integer.MAX_VALUE)
public class JaxrsContainerRequestTracingExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        StatusType status = Response.Status.INTERNAL_SERVER_ERROR;
        URI location = null;

        // Get the status and location if available
        if (exception instanceof WebApplicationException) {
            Response response = ((WebApplicationException) exception).getResponse();
            location = response.getLocation();
            status = response.getStatusInfo();
        }

        Response.ResponseBuilder responseBuilder = Response.status(status);
        if (location != null) {
            responseBuilder.location(location);
        }
        // If the status is a server error, attach it as an entity, otherwise just return a response
        if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
            responseBuilder.entity(exception);
        }
        return responseBuilder.build();
    }

}
