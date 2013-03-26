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

package org.glassfish.webservices;

import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import com.sun.enterprise.deployment.WebServiceEndpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import org.glassfish.ejb.spi.WSEjbEndpointRegistry;
import org.glassfish.ejb.api.EjbEndpointFacade;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;

/**
 * This class acts as a registry of all the webservice EJB end points
 * enabled in this application server.
 *
 * @author  Bhakti Mehta
 */
@Service
@Singleton
public class WebServiceEjbEndpointRegistry implements WSEjbEndpointRegistry {
    
    
    private static final Logger logger = LogUtils.getLogger();

    // Ejb service endpoint info.  
    private final Map<String, EjbRuntimeEndpointInfo> webServiceEjbEndpoints = new ConcurrentHashMap<String, EjbRuntimeEndpointInfo>();

    // Derived set of all ejb web service related context roots.  Used
    // to optimize the check that determines whether an HTTP request is 
    // for an ejb.  NOTE that ejb endpoints may share the same context
    // root, but that context root must not be used by any web application.  
    // So if the context root portion of the request is in this set, we know
    // the call is for an ejb.
    private Set<String> ejbContextRoots = new HashSet<String>();
    

    // This keeps the list for each service
    private final Map<String, ServletAdapterList> adapterListMap = new HashMap<String, ServletAdapterList>();


    @Override
    public void registerEndpoint(WebServiceEndpoint webserviceEndpoint,
                                  EjbEndpointFacade ejbContainer,
                                  Object servant, Class tieClass)  {
        String uri = null;
        EjbRuntimeEndpointInfo endpoint = createEjbEndpointInfo(webserviceEndpoint, ejbContainer,servant,tieClass);
        synchronized(webServiceEjbEndpoints) {
            String uriRaw = endpoint.getEndpointAddressUri();
            if (uriRaw != null ) {
                uri = (uriRaw.charAt(0)=='/') ? uriRaw.substring(1) : uriRaw;
                if (webServiceEjbEndpoints.containsKey(uri)) {
                    logger.log(Level.SEVERE, LogUtils.ENTERPRISE_WEBSERVICE_DUPLICATE_SERVICE, uri);
                }
                webServiceEjbEndpoints.put(uri, endpoint);
                regenerateEjbContextRoots();
                if(adapterListMap.get(uri) == null) {
                    ServletAdapterList list = new ServletAdapterList();
                    adapterListMap.put(uri, list);
                }
            } else throw new WebServiceException(logger.getResourceBundle().getString(LogUtils.EJB_ENDPOINTURI_ERROR));
        }


        // notify monitoring layers that a new endpoint is being created.
        WebServiceEngineImpl engine = WebServiceEngineImpl.getInstance();
        if (endpoint.getEndpoint().getWebService().getMappingFileUri()!=null) {
            engine.createHandler((com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate)null, endpoint.getEndpoint());
        } else {
            engine.createHandler(endpoint.getEndpoint());
            try {
                endpoint.initRuntimeInfo(adapterListMap.get(uri));
            } catch (Exception e) {
                logger.log(Level.WARNING,LogUtils.EJB_POSTPROCESSING_ERROR, e);
            }
        }
    }


    @Override
    public void unregisterEndpoint(String endpointAddressUri) {

        EjbRuntimeEndpointInfo endpoint = null;

        synchronized(webServiceEjbEndpoints) {
            String uriRaw = endpointAddressUri;
            String uri = (uriRaw.charAt(0)=='/') ? uriRaw.substring(1) : uriRaw;
            
            ServletAdapterList list = adapterListMap.get(uri);
            if (list != null) {
            	//bug12540102: remove only the data related to the endpoint that is unregistered
            	//since we are using the uri in the adapterListMap 
                for (ServletAdapter x :list)  {
                	x.getEndpoint().dispose();
                        for (Handler handler : x.getEndpoint().getBinding().getHandlerChain()) {
                        try {
                            WebServiceContractImpl wscImpl = WebServiceContractImpl.getInstance();
                            InjectionManager injManager = wscImpl.getInjectionManager();
                            injManager.destroyManagedObject(handler);
                        } catch (InjectionException e) {
                            logger.log(Level.WARNING, LogUtils.DESTORY_ON_HANDLER_FAILED,
                                    new Object[]{handler.getClass(), x.getEndpoint().getServiceName(), e.getMessage()});
                            continue;
                        }
                    }

                }
                //Fix for issue 9523
                adapterListMap.remove(uri);

            }
            endpoint = (EjbRuntimeEndpointInfo) webServiceEjbEndpoints.remove(uri);
            regenerateEjbContextRoots();
        }

        if (endpoint==null) {
            return;
        }

        // notify the monitoring layers that an endpoint is destroyed
        WebServiceEngineImpl engine = WebServiceEngineImpl.getInstance();

        engine.removeHandler(endpoint.getEndpoint());

    }
    
    /**
     * Creates a new EjbRuntimeEndpointInfo instance depending on the type
     * and version of the web service implementation.
     * @param   
     */
  public EjbRuntimeEndpointInfo createEjbEndpointInfo(WebServiceEndpoint webServiceEndpoint,
                                  EjbEndpointFacade ejbContainer,
                                  Object servant, Class tieClass) {
        EjbRuntimeEndpointInfo info = null;
        if (webServiceEndpoint.getWebService().hasMappingFile()) {
            info = new Ejb2RuntimeEndpointInfo(webServiceEndpoint, ejbContainer, servant, tieClass);
        } else {
            info = new EjbRuntimeEndpointInfo(webServiceEndpoint, ejbContainer, servant);
        }

        return info;
    }

    public EjbRuntimeEndpointInfo getEjbWebServiceEndpoint(String uriRaw, String method, String query) {
        EjbRuntimeEndpointInfo endpoint = null;

        if (uriRaw==null || uriRaw.length()==0) {
            return null;
        }

        // Strip off any leading slash.
        String uri = (uriRaw.charAt(0) == '/') ? uriRaw.substring(1) : uriRaw;

        synchronized(webServiceEjbEndpoints) {

            if( method.equals("GET") ) {
                // First check for a context root match so we avoid iterating
                // through all ejb endpoints.  This logic will be used for
                // all HTTP GETs, so it's important to reduce the overhead in
                // the likely most common case that the request is for a web
                // component.
                String contextRoot = getContextRootForUri(uri);
                if( ejbContextRoots.contains(contextRoot) ) {
                    // Now check for a match with a specific ejb endpoint.
                    for (EjbRuntimeEndpointInfo next: webServiceEjbEndpoints.values()) {
                        if( next.getEndpoint().matchesEjbPublishRequest(uri, query)) {
                            endpoint = next;
                            break;
                        }
                    }
                }
            }
        }
        return endpoint != null ? endpoint : webServiceEjbEndpoints.get(uri);
    }

    public Collection getEjbWebServiceEndpoints() {
        return webServiceEjbEndpoints.entrySet();
    }

    private String getContextRootForUri(String uri) {
        StringTokenizer tokenizer = new StringTokenizer(uri, "/");
        if (tokenizer.hasMoreTokens()) {
            return tokenizer.nextToken();
        } else {
            return null;
        }
    }

    private void regenerateEjbContextRoots() {
        synchronized(webServiceEjbEndpoints) {
            Set<String> contextRoots = new HashSet<String>();
            for (String uri : webServiceEjbEndpoints.keySet()) {
                String contextRoot = getContextRootForUri(uri);
                if( (contextRoot != null) && !contextRoot.equals("") ) {
                    contextRoots.add(contextRoot);
                }
            }
            ejbContextRoots = contextRoots;
        }
    }
}
