/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

import com.sun.logging.LogDomains;
import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import org.glassfish.ejb.spi.WSEjbEndpointRegistry;
import org.glassfish.ejb.api.EjbEndpointFacade;

import javax.xml.ws.WebServiceException;
import org.glassfish.internal.api.Globals;


/**
 * This class acts as a registry of all the webservice EJB end points
 * enabled in this application server.
 *
 * @author  Bhakti Mehta
 */
@Service
@Scoped(Singleton.class)
public class WebServiceEjbEndpointRegistry implements WSEjbEndpointRegistry {
    
    
    private Logger logger = LogDomains.getLogger(this.getClass(),LogDomains.WEBSERVICES_LOGGER);

    private ResourceBundle rb = logger.getResourceBundle()   ;


    // Ejb service endpoint info.  
    private Hashtable webServiceEjbEndpoints = new Hashtable();

    // Derived set of all ejb web service related context roots.  Used
    // to optimize the check that determines whether an HTTP request is 
    // for an ejb.  NOTE that ejb endpoints may share the same context
    // root, but that context root must not be used by any web application.  
    // So if the context root portion of the request is in this set, we know
    // the call is for an ejb.
    private Set ejbContextRoots = new HashSet();
    

    // This keeps the list for each service
    private HashMap adapterListMap = new HashMap();


    public void registerEndpoint(WebServiceEndpoint webserviceEndpoint,
                                  EjbEndpointFacade ejbContainer,
                                  Object servant, Class tieClass)  {
        String ctxtRoot = null;
        String uri = null;
        EjbRuntimeEndpointInfo endpoint = createEjbEndpointInfo(webserviceEndpoint, ejbContainer,servant,tieClass);
        synchronized(webServiceEjbEndpoints) {
            String uriRaw = endpoint.getEndpointAddressUri();
            if (uriRaw != null ) {
                uri = (uriRaw.charAt(0)=='/') ? uriRaw.substring(1) : uriRaw;
                if (webServiceEjbEndpoints.containsKey(uri)) {
                    logger.log(Level.SEVERE,
                            format(rb.getString("enterprise.webservice.duplicateService"),
                                    uri));
                }
                webServiceEjbEndpoints.put(uri, endpoint);
                regenerateEjbContextRoots();
                ctxtRoot = getContextRootForUri(uri);
                if(adapterListMap.get(uri) == null) {
                    ServletAdapterList list = new ServletAdapterList();
                    adapterListMap.put(uri, list);
                }
            } else throw new WebServiceException(rb.getString("ejb.endpointuri.error"));
        }


        // notify monitoring layers that a new endpoint is being created.
        WebServiceEngineImpl engine = WebServiceEngineImpl.getInstance();
        if (endpoint.getEndpoint().getWebService().getMappingFileUri()!=null) {
            engine.createHandler((com.sun.xml.rpc.spi.runtime.SystemHandlerDelegate)null, endpoint.getEndpoint());
        } else {
            engine.createHandler(endpoint.getEndpoint());
            try {
                endpoint.initRuntimeInfo((ServletAdapterList)adapterListMap.get(uri));
            } catch (Exception e) {
                logger.log(Level.WARNING,
                       "Unexpected error in EJB WebService endpoint post processing", e);
            }
        }
    }


    public void unregisterEndpoint(String endpointAddressUri) {

        EjbRuntimeEndpointInfo endpoint = null;

        synchronized(webServiceEjbEndpoints) {
            String uriRaw = endpointAddressUri;
            String uri = (uriRaw.charAt(0)=='/') ? uriRaw.substring(1) : uriRaw;
            
            ServletAdapterList list = (ServletAdapterList)adapterListMap.get(uri);
            if (list != null) {
            	//bug12540102: remove only the data related to the endpoint that is unregistered
            	//since we are using the uri in the adapterListMap 
                for (ServletAdapter x :list)  {
                		x.getEndpoint().dispose();

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

    public EjbRuntimeEndpointInfo getEjbWebServiceEndpoint
        (String uriRaw, String method, String query) {
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
                    Collection values = webServiceEjbEndpoints.values();
                    for(Iterator iter = values.iterator(); iter.hasNext();) {
                        EjbRuntimeEndpointInfo next = (EjbRuntimeEndpointInfo)
                            iter.next();

                        if( next.getEndpoint().matchesEjbPublishRequest
                            (uri, query)) {
                            endpoint = next;
                            break;
                        }       
                    }
                }
            } else {
                // In this case the uri must match exactly to be an ejb web
                // service invocation, so do a direct table lookup.
                endpoint = (EjbRuntimeEndpointInfo)
                    webServiceEjbEndpoints.get(uri);
            }
        }
        return endpoint;
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
            Set contextRoots = new HashSet();
            for(Iterator iter = webServiceEjbEndpoints.keySet().iterator();
                iter.hasNext();) {
                String uri = (String) iter.next();
                String contextRoot = getContextRootForUri(uri);
                if( (contextRoot != null) && !contextRoot.equals("") ) {
                    contextRoots.add(contextRoot);
                }
            }
            ejbContextRoots = contextRoots;
        }
    }

    private String format(String key, String ... values){
        return MessageFormat.format(key,values);
    }
    
}
