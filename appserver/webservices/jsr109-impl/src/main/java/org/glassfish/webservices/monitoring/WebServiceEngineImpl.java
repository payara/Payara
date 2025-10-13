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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]

/*
 * TracingSystemHandlerFactory.java
 *
 * Created on August 12, 2004, 4:04 PM
 */

package org.glassfish.webservices.monitoring;

import static com.sun.enterprise.deployment.util.DOLUtils.ejbType;
import static java.util.logging.Level.SEVERE;
import static org.glassfish.webservices.LogUtils.EXCEPTION_CREATING_ENDPOINT;
import static org.glassfish.webservices.monitoring.EndpointImpl.NAME;
import static org.glassfish.webservices.monitoring.EndpointType.EJB_ENDPOINT;
import static org.glassfish.webservices.monitoring.EndpointType.SERVLET_ENDPOINT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.webservices.LogUtils;
import org.glassfish.webservices.SOAPMessageContext;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.util.DOLUtils;
import java.util.logging.Level;

/**
 * This class acts as a factory to create TracingSystemHandler instances. It also provides an API to register listeners
 * of SOAP messages.
 * <p>
 * <b>NOT THREAD SAFE: mutable instance variable: globalMessageListener</b>
 *
 * @author Jerome Dochez
 */
public final class WebServiceEngineImpl implements WebServiceEngine {

    private final Map<String, Endpoint> endpoints = new HashMap<>();
    private final List<EndpointLifecycleListener> lifecycleListeners = new ArrayList<>();
    private final List<AuthenticationListener> authListeners = new ArrayList<>();
    private volatile GlobalMessageListener globalMessageListener;

    static final ThreadLocal<ThreadLocalInfo> servletThreadLocal = new ThreadLocal<>();
    public static final Logger sLogger = LogUtils.getLogger();

    /** Creates a new instance of TracingSystemHandlerFactory */
    private WebServiceEngineImpl() {
        // this is normally a bad idiom (see Java Concurrency 3.2), but
        // this method is private, and can only be constructed in the static
        // constructor below *and* the listern maintains no reference to 'this.
        addAuthListener(new LogAuthenticationListener());
    }

    private static final WebServiceEngineImpl INSTANCE = new WebServiceEngineImpl();

    public static WebServiceEngineImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public Endpoint getEndpoint(String uri) {
        return endpoints.get(uri);
    }

    @Override
    public Iterator<Endpoint> getEndpoints() {
        return endpoints.values().iterator();
    }

    @Override
    public void addLifecycleListener(EndpointLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public void removeLifecycleListener(EndpointLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    @Override
    public void addAuthListener(AuthenticationListener listener) {
        authListeners.add(listener);
    }

    @Override
    public void removeAuthListener(AuthenticationListener listener) {
        authListeners.remove(listener);
    }

    @Override
    public GlobalMessageListener getGlobalMessageListener() {
        return globalMessageListener;
    }

    @Override
    public void setGlobalMessageListener(GlobalMessageListener listener) {
        globalMessageListener = listener;
    }
    
    public EndpointImpl createHandler(WebServiceEndpoint endpointDesc) {
        EndpointImpl endpointInfo = createEndpointInfo(endpointDesc);
        if (endpointInfo == null) {
            return null;
        }
        
        endpoints.put(endpointInfo.getEndpointSelector(), endpointInfo);

        // Notify listeners
        for (EndpointLifecycleListener listener : lifecycleListeners) {
            listener.endpointAdded(endpointInfo);
        }

        return endpointInfo;
    }
    
    public Collection<AuthenticationListener> getAuthListeners() {
        return authListeners;
    }

    public boolean hasGlobalMessageListener() {
        return globalMessageListener != null;
    }
    
    public void removeHandler(WebServiceEndpoint endpointDesc) {
        EndpointImpl endpoint = (EndpointImpl) endpointDesc.getExtraAttribute(NAME);
        if (endpoint == null) {
            return;
        }

        // Remove this endpoint from our list of endpoints
        endpoints.remove(endpoint.getEndpointSelector());

        // Notify listeners
        for (EndpointLifecycleListener listener : lifecycleListeners) {
            listener.endpointRemoved(endpoint);
        }

        // Forcing the cleaning so we don't have DOL objects staying alive because
        // some of our clients have not released the endpoint instance.
        endpoint.setDescriptor(null);
    }

    /**
     * Callback when a web service request entered the web service container before any processing is done.
     *
     * @param endpoint the Endpoint
     * @return a message ID to trace the request in the subsequent callbacks
     */
    public String preProcessRequest(Endpoint endpoint) {
        if (globalMessageListener == null) {
            return null;
        }

        return globalMessageListener.preProcessRequest(endpoint);
    }

    /**
     * Callback when a 2.0 web service request is received on the endpoint.
     *
     * @param messageID returned by preProcessRequest call
     * @param context the jaxws message trace, transport dependent.
     * @param info the transport info
     */
    public void processRequest(String messageID, SOAPMessageContext context, TransportInfo info) {
        if (globalMessageListener == null) {
            return;
        }

        globalMessageListener.processRequest(messageID, context, info);
    }

    /**
     * Callback when a 2.0 web service response is received on the endpoint.
     *
     * @param messageID returned by the preProcessRequest call
     * @param context jaxws message context
     */
    public void processResponse(String messageID, SOAPMessageContext context) {
        if (globalMessageListener == null) {
            return;
        }
        
        globalMessageListener.processResponse(messageID, context);
    }

    /**
     * Callback when a web service response has finished being processed by the container and was sent back to the client
     *
     * @param messageID returned by the preProcessRequest call
     * @param info
     */
    public void postProcessResponse(String messageID, TransportInfo info) {
        if (globalMessageListener == null) {
            return;
        }

        globalMessageListener.postProcessResponse(messageID, info);
    }

    public ThreadLocal<ThreadLocalInfo> getThreadLocal() {
        return servletThreadLocal;
    }

    private EndpointImpl createEndpointInfo(WebServiceEndpoint endpoint) {

        try {
            String endpointURL = endpoint.getEndpointAddressUri();
            EndpointType endpointType;
            ArchiveType moduleType = endpoint.getWebService().getWebServicesDescriptor().getModuleType();
            if (moduleType != null && moduleType.equals(ejbType())) {
                endpointType = EJB_ENDPOINT;
            } else {
                endpointType = SERVLET_ENDPOINT;
            }

            EndpointImpl endpointInfo = new JAXWSEndpointImpl(endpointURL, endpointType);
            endpointInfo.setDescriptor(endpoint);
            return endpointInfo;

        } catch (Exception e) {
            sLogger.log(SEVERE, EXCEPTION_CREATING_ENDPOINT, e);
        }
        return null;

    }
        
}
