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
package com.sun.enterprise.v3.services.impl;

import java.net.InetAddress;
import java.util.Collection;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.grizzly.http.server.HttpHandler;

/**
 * Abstraction represents an endpoint, which could be registered on {@link NetworkProxy}.
 * 
 * @author Alexey Stashok
 */
public abstract class Endpoint {

    /**
     * Creates <tt>Endpoint</tt> based on the passed {@link Adapter} descriptor.
     * @param {@link Adapter}
     * @return {@link Endpoint}, which can be registered on {@link NetworkProxy}.
     */
    public static Endpoint createEndpoint(final Adapter adapter) {
        return new AdapterEndpoint(adapter);
    }
    
    /**
     * @return the {@link InetAddress} on which this endpoint is listening
     */
    public abstract InetAddress getAddress();

    /**
     * Returns the listener port for this endpoint
     * @return listener port
     */
    public abstract int getPort();

    /**
     * Returns the context root for this endpoint
     * @return context root
     */
    public abstract String getContextRoot();

    /**
     * Get the underlying Grizzly {@link HttpHandler}.
     * 
     * @return the underlying Grizzly {@link HttpHandler}.
     */
    public abstract HttpHandler getEndpointHandler();

    /**
     * Returns the virtual servers supported by this endpoint
     * @return List&lt;String&gt; the virtual server list supported by the endpoint
     */
    public abstract Collection<String> getVirtualServers();

    /**
     * Return the {@link ApplicationContainer} endpoint belongs to.
     * @return the {@link ApplicationContainer} endpoint belongs to. 
     */
    public abstract ApplicationContainer getContainer();

    
    /**
     * {@link Adapter} based <tt>Endpoint</tt> implementation.
     */
    private static class AdapterEndpoint extends Endpoint {
        private final Adapter adapter;

        public AdapterEndpoint(final Adapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public InetAddress getAddress() {
            return adapter.getListenAddress();
        }

        @Override
        public int getPort() {
            return adapter.getListenPort();
        }

        @Override
        public String getContextRoot() {
            return adapter.getContextRoot();
        }

        @Override
        public HttpHandler getEndpointHandler() {
            return adapter.getHttpService();
        }

        @Override
        public Collection<String> getVirtualServers() {
            return adapter.getVirtualServers();
        }

        @Override
        public ApplicationContainer getContainer() {
            return null;
        }
    }
}
