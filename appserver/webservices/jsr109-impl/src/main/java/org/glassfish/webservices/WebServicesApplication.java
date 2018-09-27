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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package org.glassfish.webservices;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.glassfish.web.deployment.util.WebServerInfo;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.WebServicesDescriptor;

/**
 * This class implements the ApplicationContainer and will be used
 * to register endpoints to the grizzly ServletAdapter
 *
 * <p>
 * Thus when a request is received it is directed to our {@link EjbWebServiceServlet}
 * so that it can process the request
 *
 * @author Bhakti Mehta
 */

public class WebServicesApplication implements ApplicationContainer<Object> {

    private static final Logger logger = LogUtils.getLogger();

    private ArrayList<EjbEndpoint> ejbendpoints;
    private ServletHandler httpHandler;
    private final RequestDispatcher dispatcher;
    private DeploymentContext deploymentCtx;

    private ClassLoader cl;
    private Application app;
    private Set<String> publishedFiles;

    public WebServicesApplication(DeploymentContext context,  RequestDispatcher dispatcherString, Set<String> publishedFiles){
        this.deploymentCtx = context;
        this.dispatcher = dispatcherString;
        this.ejbendpoints = getEjbEndpoints();
        this.httpHandler = new EjbWSAdapter();
        this.publishedFiles = publishedFiles;
    }

    @Override
    public Object getDescriptor() {
        return null;
    }

    @Override
    public boolean start(ApplicationContext startupContext) throws Exception {

        cl = startupContext.getClassLoader();

        try {
           app = deploymentCtx.getModuleMetaData(Application.class);

            DeployCommandParameters commandParams = ((DeploymentContext)startupContext).getCommandParameters(DeployCommandParameters.class);
            String virtualServers = commandParams.virtualservers;
            Iterator<EjbEndpoint> iter = ejbendpoints.iterator();
            EjbEndpoint ejbendpoint = null;
            while(iter.hasNext()) {
                ejbendpoint = iter.next();
                String contextRoot = ejbendpoint.contextRoot;
                WebServerInfo wsi = new WsUtil().getWebServerInfoForDAS();
                URL rootURL = wsi.getWebServerRootURL(ejbendpoint.isSecure);
                dispatcher.registerEndpoint(contextRoot, httpHandler, this, virtualServers);
                //Fix for issue 13107490 and 17648
                if (wsi.getHttpVS() != null && wsi.getHttpVS().getPort()!=0) {
                    logger.log(Level.INFO, LogUtils.EJB_ENDPOINT_REGISTRATION,
                            new Object[] {app.getAppName(), rootURL + contextRoot});
                }
            }

        } catch (EndpointRegistrationException e) {
            logger.log(Level.SEVERE,  LogUtils.ENDPOINT_REGISTRATION_ERROR, e.toString());
        }
        return true;
    }


    private ArrayList<EjbEndpoint> getEjbEndpoints() {
        ejbendpoints = new ArrayList<EjbEndpoint>();

        Application app = deploymentCtx.getModuleMetaData(Application.class);

        Set<BundleDescriptor> bundles = app.getBundleDescriptors();
        for(BundleDescriptor bundle : bundles) {
            collectEjbEndpoints(bundle);
        }

        return ejbendpoints;
    }

    private void collectEjbEndpoints(BundleDescriptor bundleDesc) {
        WebServicesDescriptor wsDesc = bundleDesc.getWebServices();
        for (WebService ws : wsDesc.getWebServices()) {
            for (WebServiceEndpoint endpoint : ws.getEndpoints()) {
                //Only add for ejb based endpoints
                if (endpoint.implementedByEjbComponent()) {
                    ejbendpoints.add(new EjbEndpoint(endpoint.getEndpointAddressUri(), endpoint.isSecure()));
                }
            }
        }
    }

    @Override
    public boolean stop(ApplicationContext stopContext) {
        try {
            Iterator<EjbEndpoint> iter = ejbendpoints.iterator();
            String contextRoot;
            EjbEndpoint endpoint;
            while(iter.hasNext()) {
                endpoint = iter.next();
                contextRoot = endpoint.contextRoot;
                dispatcher.unregisterEndpoint(contextRoot);
            }
        } catch (EndpointRegistrationException e) {
            logger.log(Level.SEVERE,  LogUtils.ENDPOINT_UNREGISTRATION_ERROR ,e.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean suspend() {
        return false;
    }

    @Override
    public boolean resume() throws Exception {
        return false;
    }

    @Override
    public ClassLoader getClassLoader() {
        return cl;
    }

    Application getApplication() {
        return app;
    }

    static class EjbEndpoint {
        private final String contextRoot;

        private boolean isSecure;

        EjbEndpoint(String contextRoot,boolean secure){
            this.contextRoot = contextRoot;
            this.isSecure = secure;
        }
    }

    Set<String> getPublishedFiles() {
        return publishedFiles;
    }
}
