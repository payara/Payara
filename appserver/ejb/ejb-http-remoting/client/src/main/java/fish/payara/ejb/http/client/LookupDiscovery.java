/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

class LookupDiscovery {
    static final String INVOKER_V1_REL = "https://payara.fish/ejb-http-invoker/v1";
    static final String INVOKER_V0_REL = "https://payara.fish/ejb-http-invoker/v0";

    private final Map<String, Object> environment;
    private final URI root;
    private final Client client;

    LookupDiscovery(Map<String, Object> environment, Client client, URI root) {
        this.environment = environment;
        this.root = root;;
        this.client = client;
    }

    Discovery discover() throws NamingException {
        URI resolvedRoot = root;
        Response optionResponse = client.target(root).request().head();
        Response.Status.Family responseFamily = optionResponse.getStatusInfo().getFamily();
        String reasonPhrase = optionResponse.getStatusInfo().getReasonPhrase();
        if (responseFamily == REDIRECTION) {
            resolvedRoot = root.resolve(optionResponse.getLocation());
            optionResponse = followRedirect(resolvedRoot);
            responseFamily = optionResponse.getStatusInfo().getFamily();
        }
        if (responseFamily == SUCCESSFUL) {
            return discoverLinks(resolvedRoot, optionResponse);
        } else if (responseFamily == CLIENT_ERROR) {
            if (optionResponse.getStatusInfo().toEnum() == NOT_FOUND) {
                // 5.192 does not handle /, we may try the ejb endpoint
                return discoverV0(resolvedRoot);
            }
            throw new NamingException("Invoker is not available at <" + root + ">: " + reasonPhrase);
        } else if (responseFamily == SERVER_ERROR) {
            throw new NamingException("Server is not available at <" + root + ">: " + reasonPhrase);
        } else {
            throw new NamingException("Unexpected status of invoker root resource <" + root + ">: " + reasonPhrase);
        }

    }

    private Discovery discoverV0(URI resolvedRoot) throws NamingException {
        WebTarget invokerServletTarget = client.target(resolvedRoot).path("ejb/");
        Response v0response = invokerServletTarget.request().head();
        if (v0response.getStatusInfo().toEnum() == OK) {
            Discovery discovery = new Discovery(resolvedRoot);
            discovery.v0target = true;
            discovery.v0lookup = invokerServletTarget.path("lookup");
            return discovery;
        } else {
            throw new NamingException("Invoker V0 not found at <" + invokerServletTarget.getUri()
                    + ">: " + v0response.getStatusInfo().getReasonPhrase());
        }
    }

    private Response followRedirect(URI resolvedRoot) throws NamingException {
        Response redirectedResponse = client.target(resolvedRoot).request().head();
        if (redirectedResponse.getStatusInfo().getFamily() == REDIRECTION) {
            throw new NamingException("Multiple redirects when finding root resource: <" + root + "> -> <" +
                    resolvedRoot + "> -> (not followed) <" + redirectedResponse.getLocation() + ">");
        } else {
            return redirectedResponse;
        }
    }

    private Discovery discoverLinks(URI resolvedRoot, Response optionResponse) throws NamingException {
        Set<Link> links = optionResponse.getLinks();
        if (links.isEmpty()) {
            return discoverV0(resolvedRoot);
        }
        Discovery discovery = new Discovery(resolvedRoot);
        for (Link link : links) {
            if (INVOKER_V1_REL.equals(link.getRel())) {
                discovery.v1target = true;
                discovery.v1lookup = client.target(resolvedRoot.resolve(link.getUri()));
            }
            if (INVOKER_V0_REL.equals(link.getRel())) {
                discovery.v0target = true;
                discovery.v0lookup = client.target(resolvedRoot.resolve(link.getUri()));
            }
        }
        return discovery;
    }


    static class Discovery {
        private URI resolvedRoot;
        private boolean v0target;
        private WebTarget v0lookup;
        private boolean v1target;
        private WebTarget v1lookup;

        public Discovery(URI resolvedRoot) {
            this.resolvedRoot = resolvedRoot;
        }


        public URI getResolvedRoot() {
            return resolvedRoot;
        }

        public boolean isV0target() {
            return v0target;
        }

        public WebTarget getV0lookup() {
            return v0lookup;
        }

        public boolean isV1target() {
            return v1target;
        }

        public WebTarget getV1lookup() {
            return v1lookup;
        }
    }
}
