/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.portunif;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.net.ssl.SSLEngine;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.config.ConfigAwareElement;
import org.glassfish.grizzly.config.dom.HttpRedirect;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.ssl.SSLUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
 *
 * @author Alexey Stashok
 */
public class HttpRedirectFilter extends BaseFilter implements
        ConfigAwareElement {

    private Integer redirectPort;

    // default to true to retain compatibility with legacy redirect declarations.
    private Boolean secure;

    // ----------------------------------------- Methods from ConfigAwareElement
    /**
     * Configuration for &lt;http-redirect&gt;.
     *
     * @param configuration filter configuration
     */
    @Override
    public void configure(ServiceLocator habitat, NetworkListener networkListener,
            ConfigBeanProxy configuration) {
        
        if (configuration instanceof HttpRedirect) {
            final HttpRedirect httpRedirectConfig = (HttpRedirect) configuration;
            int port = Integer.parseInt(httpRedirectConfig.getPort());
            redirectPort = port != -1 ? port : null;
            secure = Boolean.parseBoolean(httpRedirectConfig.getSecure());
        } else {
            // Retained for backwards compatibility with legacy redirect declarations.
        }
    }

    // --------------------------------------------- Methods from Filter


    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final HttpContent httpContent = ctx.getMessage();

        final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();
        final URI requestURI;
        try {
            final String uri = request.getQueryString() == null ?
                    request.getRequestURI() :
                    request.getRequestURI() + "?" + request.getQueryString();
            requestURI = new URI(uri);
        } catch (URISyntaxException ignored) {
            return ctx.getStopAction();
        }

        final boolean redirectToSecure;
        if (secure != null) { // if secure is set - we use it
            redirectToSecure = secure;
        } else {  // if secure is not set - use secure settings opposite to the current request
            final SSLEngine sslEngine = SSLUtils.getSSLEngine(connection);
            redirectToSecure = sslEngine == null;
        }


        final StringBuilder hostPort = new StringBuilder();

        String hostHeader = request.getHeader("host");
        if (hostHeader == null) {
            String hostRequestURI = requestURI.getHost();

            if (hostRequestURI == null) {
                hostPort.append(request.getLocalHost());
            } else {
                hostPort.append(hostRequestURI);
            }

            hostPort.append(':');

            if (redirectPort == null) {
                int port = requestURI.getPort();
                if (port == -1) {
                    hostPort.append(request.getLocalPort());
                } else {
                    hostPort.append(port);
                }
            } else {
                hostPort.append(redirectPort);
            }

        } else if (redirectPort != null) { // if port is specified - cut it from host header
            final int colonIdx = hostHeader.indexOf(':');
            if (colonIdx != -1) {
                hostHeader = hostHeader.substring(0, colonIdx);
            }
            hostPort.append(hostHeader)
                    .append(':')
                    .append(redirectPort);
        } else {
            hostPort.append(hostHeader);
        }

        if (hostPort.length() > 0) {
            String path = requestURI.toString();
            
            assert path != null;

            final StringBuilder sb = new StringBuilder();
            sb.append((redirectToSecure ? "https://" : "http://"))
                    .append(hostPort)
                    .append(path);

            request.setSkipRemainder(true);
            final HttpResponsePacket response = HttpResponsePacket.builder(request)
                    .status(302)
                    .header("Location", sb.toString())
                    .contentLength(0)
                    .build();
            ctx.write(response);
        } else {
            connection.closeSilently();
        }
        
        return ctx.getStopAction();
    }
}
