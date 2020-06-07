/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.jwtauth.jaxrs;

import static java.util.Arrays.stream;
import static javax.security.enterprise.AuthenticationStatus.NOT_DONE;
import static javax.security.enterprise.AuthenticationStatus.SEND_FAILURE;
import static javax.security.enterprise.AuthenticationStatus.SUCCESS;
import static javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters.withParams;
import static javax.ws.rs.Priorities.AUTHORIZATION;

import java.io.IOException;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

/**
 * This JAX-RS filter makes sure only callers with the given roles can access the
 * resource method(s) to which this filter is applied.
 *
 * <p>
 * Note that if pre-emptive authentication is not active and the caller is found to be
 * unauthenticated when reaching this filter, the installed authentication mechanism
 * will be invoked.
 *
 * @author Arjan Tijms
 */
@Priority(AUTHORIZATION)
public class RolesAllowedRequestFilter implements ContainerRequestFilter {

    private final SecurityContext securityContext;

    private final String[] rolesAllowed;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    RolesAllowedRequestFilter(HttpServletRequest request, HttpServletResponse response, String[] rolesAllowed) {
        this.request = request;
        this.response = response;
        this.rolesAllowed = rolesAllowed;
        this.securityContext = CDI.current().select(SecurityContext.class).get();
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (rolesAllowed.length > 0 && !isAuthenticated()) {

            AuthenticationStatus status =  securityContext.authenticate(request, response, withParams());

            // Authentication was not done at all (i.e. no credentials present) or
            // authentication failed (i.e. wrong credentials, credentials expired, etc)
            if (status == NOT_DONE || status == SEND_FAILURE) {
                throw new NotAuthorizedException(
                    "Authentication resulted in " + status,
                    Response.status(Response.Status.UNAUTHORIZED).build()
                );
            }

            if (status == SUCCESS && !isAuthenticated()) { // compensate for possible Soteria bug, need to investigate
                throw new NotAuthorizedException(
                    "Authentication not done (i.e. no JWT credential found)",
                    Response.status(Response.Status.UNAUTHORIZED).build()
                );
            }

        }

        boolean hasRole = stream(rolesAllowed).anyMatch(r -> requestContext.getSecurityContext().isUserInRole(r));

        if (!hasRole) {
            throw new ForbiddenException("Caller not in requested role");
        }

    }

    private boolean isAuthenticated() {
        return securityContext.getCallerPrincipal() != null;
    }
}