/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.appserver.monitoring.rest.service.adapter;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SystemApplications;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.event.Events;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import fish.payara.appserver.monitoring.rest.service.configuration.RestMonitoringConfiguration;

/**
 * The adapter class for the Rest Monitoring application.
 * @author Andrew Pielage
 */
@Service
public final class RestMonitoringAdapter extends HttpHandler implements Adapter {
    private boolean isRegistered = false;
    private boolean appRegistered = false;
    private ResourceBundle bundle;
    private final Method[] allowedHttpMethods = {Method.GET, Method.POST, Method.HEAD, Method.DELETE, Method.PUT};

    private static RestMonitoringEndpointDecider endpointDecider;
    
    @Inject
    ServerEnvironmentImpl env;
    
    @Inject
    ApplicationRegistry appRegistry;
    
    @Inject
    Domain domain;
    
    @Inject
    ServiceLocator habitat;
    
    @Inject
    Events events;
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config serverConfig;
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    RestMonitoringConfiguration restMonitoringServiceConfiguration;
    
    private final static Logger logger = Logger.getLogger(RestMonitoringAdapter.class.getName());
    private final static String RESOURCE_PACKAGE = "fish/payara/appserver/monitoring/rest/service/adapter";
    private final CountDownLatch latch = new CountDownLatch(1);
    
    @PostConstruct
    public void postConstruct() {
        restMonitoringServiceConfiguration = habitat.getService(RestMonitoringConfiguration.class);
        init();
    }
    
    private void init() {
        try {
            endpointDecider = new RestMonitoringEndpointDecider(serverConfig, restMonitoringServiceConfiguration);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Rest Monitoring Console cannot initialise", ex);
        }
        
        // If the app exists in the domain.xml AND it's registered to this instance
        if (appExistsInConfig() && (domain.getSystemApplicationReferencedFrom(env.getInstanceName(), 
                restMonitoringServiceConfiguration.getApplicationName()) != null)) {
            setAppRegistered(true);
        }
    }
    
    public boolean appExistsInConfig() {
        return (getSystemApplicationConfig() != null);
    }
    
    public boolean appExistsInConfig(String contextRoot) {
        return (getSystemApplicationConfig(contextRoot) != null);
    }
    
    /**
     * Gets the application config for the system application with the matching name or context root (in that order).
     * @return The application config, or null if there is no matching application
     */
    public Application getSystemApplicationConfig() {
        // First, check if there is an app registered for this server with the given application name
        Application application = domain.getSystemApplicationReferencedFrom(env.getInstanceName(), 
                restMonitoringServiceConfiguration.getApplicationName());
        
        // If the app hasn't been registered to the instance yet, the previous check will return null, so check for one 
        // with a matching context root instead (as these are also unique and saves us creating an extra app entry)
        if (application == null) {
            application = getApplicationWithMatchingContextRoot(getContextRoot());
        }
        
        return application;
    }
    
    /**
     * Gets the application config for the system application with the matching context root. This method is used over the 
     * overloaded method if you want to skip trying to get the application config based on the application name, such as
     * if you've reconfigured the application.
     * @param contextRoot The context root of the application
     * @return The application config, or null if there is no matching application.
     */
    public Application getSystemApplicationConfig(String contextRoot) {
        Application application = getApplicationWithMatchingContextRoot(contextRoot);
        
        return application;
    }
    
    /**
     * Helper method that searches through all system applications for one with a matching context root.
     * @param contextRoot The context root fo the application.
     * @return The application config, or null if there are no applications with a matching context root.
     */
    private Application getApplicationWithMatchingContextRoot(String contextRoot) {
        Application application = null;

        SystemApplications systemApplications = domain.getSystemApplications();
        for (Application systemApplication : systemApplications.getApplications()) {
            if (systemApplication.getContextRoot().equals(contextRoot)) {
                application = systemApplication;
                break;
            }
        }
        
        return application;
    }
    
    @Override
    public void service(Request request, Response response) throws Exception {
        bundle = getResourceBundle(request.getLocale());
        Method method = request.getMethod();
        
        if (!checkHttpMethodAllowed(method)) {
            response.setStatus(java.net.HttpURLConnection.HTTP_BAD_METHOD,
                    method.getMethodString() + " " + bundle.getString("http.bad.method"));
            response.setHeader("Allow", getAllowedHttpMethodsAsString());
            return;
        }

        try {
            if (!latch.await(100L, TimeUnit.SECONDS)) {
                logger.log(Level.SEVERE, "Timed out processing a rest monitoring request");
                return;
            }
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, "Cannot process rest monitoring request");
            return;
        }
        
        if (isResourceRequest(request)) {
            try {
                handleResourceRequest(request, response);
            } catch (IOException ioe) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, "Unable to serve resource: {0}. Cause: {1}",
                            new Object[]{request.getRequestURI(), ioe.toString()});
                }
                
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, ioe.toString(), ioe);
                }
            }
            
            return;
        }
        
        response.setContentType("text/html; charset=UTF-8");
    }
    
    private ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(RestMonitoringAdapter.class.getPackage().getName() + ".LocalStrings", locale);
    }
    
    private boolean checkHttpMethodAllowed(Method method) {
        for (Method allowedMethod : allowedHttpMethods) {
            if (allowedMethod.equals(method)) {
                return true;
            }
        }
        return false;
    }
    
    private String getAllowedHttpMethodsAsString() {
        StringBuilder sb = new StringBuilder(allowedHttpMethods[0].getMethodString());
        for (int i = 1; i < allowedHttpMethods.length; i++) {
            sb.append(", ").append(allowedHttpMethods[i].getMethodString());
        }
        
        return sb.toString();
    }
    
    private boolean isResourceRequest(Request request) {
        return (getContentType(request.getRequestURI()) != null);
    }
    
    private String getContentType(String resource) {
        if (resource == null || resource.length() == 0) {
            return null;
        }
        
        if (resource.endsWith(".gif")) {
            return "image/gif";
        } else if (resource.endsWith(".jpg")) {
            return "image/jpeg";
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Unhandled content-type: {0}", resource);
            }
            
            return null;
        }
    }
    
    private void handleResourceRequest(Request request, Response response) throws IOException {
        String resourcePath = RESOURCE_PACKAGE + request.getRequestURI();

        ClassLoader loader = RestMonitoringAdapter.class.getClassLoader();

        try (InputStream inputStream = loader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.log(Level.WARNING, "Resource not found: {0}", resourcePath);
                return;
            }
            
            byte[] buffer = new byte[512];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(512);
            
            for (int i = inputStream.read(buffer); i != -1; i = inputStream.read(buffer)) {
                byteArrayOutputStream.write(buffer, 0, i);
            }
            
            String contentType = getContentType(resourcePath);
            
            if (contentType != null) {
                response.setContentType(contentType);
            }
            
            response.setContentLength(byteArrayOutputStream.size());
            OutputStream outputStream = response.getOutputStream();
            byteArrayOutputStream.writeTo(outputStream);
            outputStream.flush();
        }
    }
    
    @Override
    public HttpHandler getHttpService() {
        return this;
    }

    @Override
    public String getContextRoot() {
        return endpointDecider == null ? null : endpointDecider.getContextRoot();
    }

    @Override
    public int getListenPort() {
        return endpointDecider == null ? -1 : endpointDecider.getListenPort();
    }

    @Override
    public InetAddress getListenAddress() {
        return endpointDecider == null ? null : endpointDecider.getListenAddress();
    }

    @Override
    public List<String> getVirtualServers() {
        return endpointDecider == null ? Collections.emptyList() : endpointDecider.getHosts();
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public void setRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public boolean isAppRegistered() {
        return appRegistered;
    }
    
    public void setAppRegistered(boolean appRegistered) {
        this.appRegistered = true;
    }
    
    /**
     * Overloaded method that checks if an application with the provided context root has been registered to this instance.
     * @param contextRoot The context root to match.
     * @return True if an application has been registered to this instance.
     */
    public boolean isAppRegistered(String contextRoot) {
        boolean registered = false;
        Application application = getSystemApplicationConfig(contextRoot);
        
        // Check if we've found an application with a matching context root, and that it's registered to this instance
        if (application != null && (domain.getSystemApplicationReferencedFrom(env.getInstanceName(), 
                application.getName()) != null)) {
            registered = true;
        }
        
        return registered;
    }
}
