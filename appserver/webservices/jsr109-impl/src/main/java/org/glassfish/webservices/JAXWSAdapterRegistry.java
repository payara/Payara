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

import com.sun.xml.ws.api.server.Adapter;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Registry of JAXWS Adapter of endpoints.
 */
public class JAXWSAdapterRegistry {
    
    private static JAXWSAdapterRegistry registry = null;
    private final Map<String, ContextAdapter> store;

    private static final Logger logger = LogUtils.getLogger();
    
    /** Creates a new instance of JAXWSServletUtil */
    private JAXWSAdapterRegistry() {
        store = Collections.synchronizedMap(new HashMap<String, ContextAdapter>());
    }
    
    public static synchronized JAXWSAdapterRegistry getInstance() {
        if(registry == null)
            registry = new JAXWSAdapterRegistry();
        return registry;
    }
    
    public void addAdapter(String contextRoot, String urlPattern, 
            Adapter info) {
        if (contextRoot == null)
            contextRoot = "";
        synchronized (store) {
            ContextAdapter contextRtInfo = store.get(contextRoot);
            if(contextRtInfo == null) {
                contextRtInfo = new ContextAdapter();
            }
            contextRtInfo.addAdapter(urlPattern, info);
            store.put(contextRoot, contextRtInfo);
        }
    }
    
     public Adapter getAdapter(String contextRoot,
             String path, String urlPattern ) {
            ContextAdapter serviceInfo = store.get(contextRoot);
            if(serviceInfo == null) {
                return null;
            }
            return serviceInfo.getAdapter(path, urlPattern);
     }
  
     public void removeAdapter(String contextRoot) {
         if(contextRoot == null)
             contextRoot = "";
         synchronized (store) {
            ContextAdapter serviceInfo = store.get(contextRoot);
            if(serviceInfo == null) {
                return ;
            }
            store.remove(contextRoot);
         }
     }
     
    static class ContextAdapter {

        final Map<String, Adapter> fixedUrlPatternEndpoints;
        final List<Adapter> pathUrlPatternEndpoints;

        ContextAdapter() {

            fixedUrlPatternEndpoints = Collections.synchronizedMap(new HashMap<String, Adapter>());
            pathUrlPatternEndpoints = Collections.synchronizedList(new ArrayList<Adapter>());
        }

        void addAdapter(String urlPattern, Adapter info) {
            if (urlPattern.indexOf("*.") != -1) {
                // cannot deal with implicit mapping right now
                logger.log(Level.SEVERE, LogUtils.ENTERPRISE_WEBSERVICE_IMPLICIT_MAPPING_NOT_SUPPORTED);
            } else if (urlPattern.endsWith("/*")) {
                pathUrlPatternEndpoints.add(info);
            } else {
                synchronized (fixedUrlPatternEndpoints) {
                    if (fixedUrlPatternEndpoints.containsKey(urlPattern)) {
                        logger.log(Level.SEVERE, LogUtils.ENTERPRISE_WEBSERVICE_DUPLICATE_SERVICE, urlPattern);
                    }
                    fixedUrlPatternEndpoints.put(urlPattern, info);
                }
            }
        }

        Adapter getAdapter(String path, String urlPattern) {
            Adapter result = fixedUrlPatternEndpoints.get(path);
            if (result == null) {                
                // This loop is unnecessary.Essentially what it is doing to always
                // return the first element from pathUrlPatternEndpoints
                // TO DO clean up after SCF required
                synchronized (pathUrlPatternEndpoints) {
                    for (Iterator<Adapter> iter = pathUrlPatternEndpoints.iterator(); iter.hasNext();) {
                        Adapter candidate = iter.next();
                        if (path.startsWith(getValidPathForEndpoint(urlPattern))) {
                            result = candidate;
                            break;
                        }
                    }
                }
            }
            return result;
        }

         private String getValidPathForEndpoint(String s) {
            if (s.endsWith("/*")) {
                return s.substring(0, s.length() - 2);
            } else {
                return s;
            }
        }
    }
}
