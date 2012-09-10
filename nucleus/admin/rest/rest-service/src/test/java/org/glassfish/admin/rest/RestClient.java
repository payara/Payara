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
package org.glassfish.admin.rest;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.admin.rest.provider.RestModelProvider;
import org.glassfish.admin.rest.readers.RestModelReader;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientFactory;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.jettison.JettisonBinder;
import org.glassfish.jersey.media.multipart.MultiPartClientBinder;

/**
 * This class wraps the Client returned by JerseyClientFactory. Using this class allows us to encapsulate many of the
 * client configuration concerns, such as registering the <code>CsrfProtectionFilter</code>.
 * @author jdlee
 */
public class RestClient implements Client {
    protected Client realClient;

    public RestClient() {
        this(new HashMap<String, String>());
    }

    /**
     * Create the client, as well as registering a <code>ClientRequestFilter</code> that adds the specified headers to
     * each request.
     * @param headers
     */
    public RestClient(final Map<String, String> headers) {
        realClient = JerseyClientFactory.newClient(new ClientConfig().
                binders(new MultiPartClientBinder(), new JettisonBinder()));
        realClient.configuration().register(new CsrfProtectionFilter());
        realClient.configuration().register(RestModelReader.class);
        realClient.configuration().register(RestModelProvider.class);
        realClient.configuration().register(new ClientRequestFilter() {

            @Override
            public void filter(ClientRequestContext rc) throws IOException {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    rc.getHeaders().add(entry.getKey(), entry.getValue());
                }
            }

        });
    }

    @Override
    public void close() {
        realClient.close();
    }

    @Override
    public Configuration configuration() {
        return realClient.configuration();
    }

    @Override
    public WebTarget target(String uri) throws IllegalArgumentException, NullPointerException {
        return realClient.target(uri);
    }

    @Override
    public WebTarget target(URI uri) throws NullPointerException {
        return realClient.target(uri);
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) throws NullPointerException {
        return realClient.target(uriBuilder);
    }

    @Override
    public WebTarget target(Link link) throws NullPointerException {
        return realClient.target(link);
    }

    @Override
    public Builder invocation(Link link) throws NullPointerException {
        return realClient.invocation(link);
    }
}
