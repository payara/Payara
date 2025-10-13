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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
/*
 * JAXWSServletModule.java
 *
 * Created on June 19, 2007, 5:51 PM
 * @author Mike Grogan
 */

package org.glassfish.webservices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.server.BoundEndpoint;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import com.sun.xml.ws.transport.http.servlet.ServletModule;

/**
 * Implementation of JAX-WS ServletModule SPI used by WSIT WS-MetadataExchange.
 *
 * <p>
 * In the current JSR 109 design, each endpoint has a unique JAXWSContainer. On the other hand, the
 * requirements imposed by WSIT WS-MetadataExchange require that all endpoints sharing a context root
 * share a ServletMoule.
 *
 * <p>
 * Therefore, in general, multiple JAXWSContainers will share a JAXWSServletModule, so
 * JAXWSContainer must use a lookup in the static <code>JAXWSServletModule.modules</code> to find
 * its associated module.
 *
 */
public class JAXWSServletModule extends ServletModule {

    // Map of context-roots to JAXWSServletModules
    private final static Map<String, JAXWSServletModule> modules = new ConcurrentHashMap<>();

    // Map of uri->BoundEndpoint used to implement getBoundEndpoint.
    //
    // A Map is uses rather than a Set, so that when a new endpoint is redeployed at a given uri, the old
    // endpoint will be replaced by the new endpoint.
    // The values() method of the field is returned by <code>getBoundEndpoints</code>.
    private final Map<String, BoundEndpoint> endpoints = new ConcurrentHashMap<>();

    // The context-root for endpoints belonging to this module.
    private final String contextPath;

    public static JAXWSServletModule getServletModule(String contextPath) {
        return modules.computeIfAbsent(contextPath, JAXWSServletModule::new);
    }

    public static void destroy(String contextPath) {
        modules.remove(contextPath);
    }

    private JAXWSServletModule(String contextPath) {
        this.contextPath = contextPath;
    }

    public void addEndpoint(String uri, ServletAdapter adapter) {
        endpoints.put(uri, adapter);
    }

    @Override
    public @NotNull List<BoundEndpoint> getBoundEndpoints() {
        return new ArrayList<>(endpoints.values());
    }

    @Override
    public @NotNull String getContextPath() {
        return contextPath;
    }
}
