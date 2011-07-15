/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.api.container;

import java.net.InetAddress;
import java.util.Collection;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.jvnet.hk2.annotations.Contract;

/**
 * RequestDispatcher is responsible for dispatching incoming requests.
 *
 * @author Jerome Dochez
 */
@Contract
public interface RequestDispatcher {
    /**
     * Registers a new endpoint (proxy implementation) for a particular context-root. All request coming with the
     * context root will be dispatched to the proxy instance passed in.
     *
     * @param contextRoot for the proxy
     * @param endpointAdapter servicing requests.
     */
    void registerEndpoint(String contextRoot, HttpHandler endpointAdapter, ApplicationContainer container)
        throws EndpointRegistrationException;

    /**
     * Registers a new endpoint (proxy implementation) for a particular context-root. All request coming with the
     * context root will be dispatched to the proxy instance passed in.
     *
     * @param contextRoot for the proxy
     * @param endpointAdapter servicing requests.
     * @param container
     * @param virtualServers comma separated list of the virtual servers
     */
    void registerEndpoint(String contextRoot, HttpHandler endpointAdapter, ApplicationContainer container,
        String virtualServers) throws EndpointRegistrationException;

    /**
     * Registers a new endpoint (proxy implementation) for a particular context-root. All request coming with the
     * context root will be dispatched to the proxy instance passed in.
     *
     * @param contextRoot for the proxy
     * @param endpointAdapter servicing requests.
     */
    void registerEndpoint(String contextRoot, Collection<String> vsServers, HttpHandler endpointAdapter,
        ApplicationContainer container) throws EndpointRegistrationException;

    /**
     * Registers a new endpoint for the given context root at the given port number.
     */
    void registerEndpoint(String contextRoot, InetAddress address, int port, Collection<String> vsServers,
        HttpHandler endpointAdapter, ApplicationContainer container) throws EndpointRegistrationException;

    /**
     * Removes the context root from our list of endpoints.
     */
    void unregisterEndpoint(String contextRoot) throws EndpointRegistrationException;

    /**
     * Removes the context root from our list of endpoints.
     */
    void unregisterEndpoint(String contextRoot, ApplicationContainer app) throws EndpointRegistrationException;
}
