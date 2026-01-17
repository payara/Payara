/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.ejb.http.client;

import javax.naming.NamingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import static jakarta.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.REDIRECTION;
import static jakarta.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;

/**
 * Default implementation for the {@link LookupDiscoveryService} that uses HEAD request at the root {@link URI} to
 * discover options and falls back to v0 at {@code /ejb} in case no options found.
 * 
 * @author Patrik Duditš
 * @author Jan Bernitt
 */
class LookupDiscoveryServiceImpl implements LookupDiscoveryService {

    static final String INVOKER_V1_REL = "https://payara.fish/ejb-http-invoker/v1";
    static final String INVOKER_V0_REL = "https://payara.fish/ejb-http-invoker/v0";

    @Override
    public LookupDiscoveryResponse discover(Client client, URI root) throws NamingException {
        try (Response optionResponse = client.target(root).request().head()) {
            Response.Status.Family responseFamily = optionResponse.getStatusInfo().getFamily();
            if (responseFamily == REDIRECTION) {
                URI resolvedRoot = root.resolve(optionResponse.getLocation());
                try (Response redirctedOptionResponse = followRedirect(client, resolvedRoot)) {
                    return createDiscovery(client, resolvedRoot, redirctedOptionResponse);
                }
            }
            return createDiscovery(client, root, optionResponse);
        }
    }

    private static LookupDiscoveryResponse createDiscovery(Client client, URI root, Response optionResponse)
            throws NamingException {
        Response.Status.Family responseFamily = optionResponse.getStatusInfo().getFamily();
        if (responseFamily == SUCCESSFUL) {
            return discoverLinks(client, root, optionResponse);
        }
        String reasonPhrase = optionResponse.getStatusInfo().getReasonPhrase();
        if (responseFamily == CLIENT_ERROR) {
            if (optionResponse.getStatusInfo().toEnum() == NOT_FOUND) {
                // 5.192 does not handle /, we may try the ejb endpoint
                return discoverV0(client, root);
            }
            throw new NamingException("Invoker is not available at <" + root + ">: " + reasonPhrase);
        }
        if (responseFamily == SERVER_ERROR) {
            throw new NamingException("Server is not available at <" + root + ">: " + reasonPhrase);
        }
        throw new NamingException("Unexpected status of invoker root resource <" + root + ">: " + reasonPhrase);
    }

    private static LookupDiscoveryResponse discoverV0(Client client, URI resolvedRoot) throws NamingException {
        WebTarget invokerServletTarget = client.target(resolvedRoot).path("ejb/");
        try (Response v0response = invokerServletTarget.request().head()) {
            if (v0response.getStatusInfo().toEnum() == OK) {
                return new LookupDiscoveryResponse(resolvedRoot, invokerServletTarget.path("lookup"));
            }
            throw new NamingException("Invoker V0 not found at <" + invokerServletTarget.getUri() + ">: "
                    + v0response.getStatusInfo().getReasonPhrase());
        }
    }

    private static Response followRedirect(Client client, URI root) throws NamingException {
        Response redirectedResponse = client.target(root).request().head();
        if (redirectedResponse.getStatusInfo().getFamily() == REDIRECTION) {
            throw new NamingException("Multiple redirects when finding root resource: <" + root + "> -> <" +
                    root + "> -> (not followed) <" + redirectedResponse.getLocation() + ">");
        }
        return redirectedResponse;
    }

    private static LookupDiscoveryResponse discoverLinks(Client client, URI resolvedRoot, Response optionResponse) throws NamingException {
        Link v0Link = optionResponse.getLink(INVOKER_V0_REL);
        Link v1Link = optionResponse.getLink(INVOKER_V1_REL);
        if (v0Link == null && v1Link == null) {
            return discoverV0(client, resolvedRoot);
        }
        return new LookupDiscoveryResponse(resolvedRoot,
                v0Link == null ? null : client.target(resolvedRoot.resolve(v0Link.getUri())),
                v1Link == null ? null : client.target(resolvedRoot.resolve(v1Link.getUri())));
    }
}
