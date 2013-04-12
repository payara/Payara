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

package org.glassfish.grizzly.extras.addons;

import java.io.IOException;
import org.glassfish.grizzly.config.ConfigAwareElement;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketFilter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.grizzly.ContextMapper;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Websocket service.
 *
 * @author Alexey Stashok
 */
@Service(name="websocket")
@ContractsProvided({WebSocketAddOnProvider.class, AddOn.class})
public class WebSocketAddOnProvider extends WebSocketAddOn implements ConfigAwareElement<Http> {

    private Mapper mapper;
    
    @Override
    public void configure(ServiceLocator habitat,
            NetworkListener networkListener, Http configuration) {
        mapper = getMapper(habitat, networkListener);
        
        setTimeoutInSeconds(Long.parseLong(configuration.getWebsocketsTimeoutSeconds()));
    }

    @Override
    protected WebSocketFilter createWebSocketFilter() {
        return new GlassfishWebSocketFilter(mapper, getTimeoutInSeconds());
    }

    private static Mapper getMapper(final ServiceLocator habitat,
            final NetworkListener listener) {
        
        final int port;
        try {
            port = Integer.parseInt(listener.getPort());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Port number is not integer");
        }
        
        for (Mapper m : habitat.<Mapper>getAllServices(Mapper.class)) {
            if (m.getPort() == port &&
                    m instanceof ContextMapper) {
                ContextMapper cm = (ContextMapper) m;
                if (listener.getName().equals(cm.getId())) {
                    return m;
                }
            }
        }
        
        return null;
    }
    
    private static class GlassfishWebSocketFilter extends WebSocketFilter {
        private final Mapper mapper;
        
        public GlassfishWebSocketFilter(final Mapper mapper,
                long wsTimeoutInSeconds) {
            super(wsTimeoutInSeconds);
            this.mapper = mapper;
        }

        @Override
        protected boolean doServerUpgrade(final FilterChainContext ctx,
                final HttpContent requestContent) throws IOException {
            return !WebSocketEngine.getEngine().upgrade(
                    ctx, requestContent, mapper);
        }
    }
}
