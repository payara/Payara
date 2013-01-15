/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.transport.tcp;

import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.enterprise.web.WebModule;
import com.sun.istack.NotNull;
import com.sun.xml.ws.api.server.Adapter;
import com.sun.xml.ws.transport.tcp.resources.MessagesMessages;
import com.sun.xml.ws.transport.tcp.util.WSTCPURI;
import com.sun.xml.ws.transport.tcp.server.TCPAdapter;
import com.sun.xml.ws.transport.tcp.server.WSTCPAdapterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.webservices.AdapterInvocationInfo;
import org.glassfish.webservices.EjbRuntimeEndpointInfo;
import org.glassfish.webservices.JAXWSAdapterRegistry;

/**
 * @author Alexey Stashok
 */
public final class WSTCPAdapterRegistryImpl implements WSTCPAdapterRegistry {
    private static final Logger logger = Logger.getLogger(
            com.sun.xml.ws.transport.tcp.util.TCPConstants.LoggingDomain + ".server");

    /**
     * Registry holds correspondents between service name and adapter
     */
    final Map<String, RegistryRecord> registry = new ConcurrentHashMap<String, RegistryRecord>();
    private static final WSTCPAdapterRegistryImpl instance = new WSTCPAdapterRegistryImpl();

    private WSTCPAdapterRegistryImpl() {
    }

    public static @NotNull WSTCPAdapterRegistryImpl getInstance() {
        return instance;
    }

    @Override
    public TCPAdapter getTarget(@NotNull final WSTCPURI requestURI) {
        // path should have format like "/context-root/url-pattern", where context-root and url-pattern could be /xxxx/yyyy/zzzz

        RegistryRecord record;
        // check if URI path is not empty
        if (requestURI.path.length() > 0 && !requestURI.path.equals("/")) {
            record = registry.get(requestURI.path);
        } else {
            record = registry.get("/");
        }

        if (record != null) {
            if (record.adapter == null) {
                try {
                    record.adapter = createWSAdapter(requestURI.path,
                            record.wsEndpointDescriptor);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, LogUtils.ADAPTER_REGISTERED, requestURI.path);
                    }
                } catch (Exception e) {
                    // This common exception is thrown from ejbEndPtInfo.prepareInvocation(true)
                    logger.log(Level.SEVERE, "WSTCPAdapterRegistryImpl. " +
                            MessagesMessages.WSTCP_0008_ERROR_TCP_ADAPTER_CREATE(
                            record.wsEndpointDescriptor.getWSServiceName()), e);
                }
            }
            return record.adapter;
        }

        return null;
    }


    public void registerEndpoint(@NotNull final String path,
            @NotNull final WSEndpointDescriptor wsEndpointDescriptor) {
        registry.put(path, new RegistryRecord(wsEndpointDescriptor));
    }

    public void deregisterEndpoint(@NotNull final String path) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogUtils.ADAPTER_DEREGISTERED, path);
        }
        registry.remove(path);
    }

    public WSEndpointDescriptor lookupEndpoint(@NotNull final String path) {
        RegistryRecord record = registry.get(path);
        return record != null ? record.wsEndpointDescriptor : null;
    }

    private TCPAdapter createWSAdapter(@NotNull final String wsPath,
            @NotNull final WSEndpointDescriptor wsEndpointDescriptor) throws Exception {
        if (wsEndpointDescriptor.isEJB()) {
            final EjbRuntimeEndpointInfo ejbEndPtInfo = (EjbRuntimeEndpointInfo) V3Module.getWSEjbEndpointRegistry().
                    getEjbWebServiceEndpoint(wsEndpointDescriptor.getURI(), "POST", null);
            final AdapterInvocationInfo adapterInfo =
                    (AdapterInvocationInfo) ejbEndPtInfo.prepareInvocation(true);

            return new Ejb109Adapter(wsEndpointDescriptor.getWSServiceName().toString(),
                    wsPath, adapterInfo.getAdapter().getEndpoint(),
                    new ServletFakeArtifactSet(wsEndpointDescriptor.getRequestURL(), wsEndpointDescriptor.getUrlPattern()),
                    ejbEndPtInfo, adapterInfo);
        } else {
            final String uri = wsEndpointDescriptor.getURI();
            final Adapter adapter =
                    JAXWSAdapterRegistry.getInstance().getAdapter(wsEndpointDescriptor.getContextRoot(), uri, uri);

            final WebModule webModule = AppServRegistry.getWebModule(wsEndpointDescriptor.getWSServiceEndpoint());
            final ComponentInvocation invocation = new WebComponentInvocation(webModule);

            return new Web109Adapter(wsEndpointDescriptor.getWSServiceName().toString(),
                wsPath,
                adapter.getEndpoint(),
                new ServletFakeArtifactSet(wsEndpointDescriptor.getRequestURL(), wsEndpointDescriptor.getUrlPattern()),
                invocation);
        }
    }

    protected static class RegistryRecord {
        public TCPAdapter adapter;
        public WSEndpointDescriptor wsEndpointDescriptor;

        public RegistryRecord(WSEndpointDescriptor wsEndpointDescriptor) {
            this.wsEndpointDescriptor = wsEndpointDescriptor;
        }
    }
}
