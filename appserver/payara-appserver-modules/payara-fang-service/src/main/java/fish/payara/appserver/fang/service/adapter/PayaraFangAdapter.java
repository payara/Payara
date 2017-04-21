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

package fish.payara.appserver.fang.service.adapter;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SystemApplications;
import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
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
import org.glassfish.grizzly.http.io.OutputBuffer;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service
public final class PayaraFangAdapter extends HttpHandler implements Adapter {
    private PayaraFangAdapterState stateMsg = PayaraFangAdapterState.UNINITIALISED;
    private boolean isRegistered = false;
    private boolean appRegistered = false;
    private ResourceBundle bundle;
    private final Method[] allowedHttpMethods = {Method.GET, Method.POST, Method.HEAD, Method.DELETE, Method.PUT};

    private static PayaraFangEndpointDecider endpointDecider;
    
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
    PayaraFangConfiguration fangServiceConfiguration;
    
    private final static Logger logger = Logger.getLogger(PayaraFangAdapter.class.getName());
    private final static String RESOURCE_PACKAGE = "fish/payara/appserver/fang/adapter";
    private final CountDownLatch latch = new CountDownLatch(1);
    
    @PostConstruct
    public void postConstruct() {
        fangServiceConfiguration = habitat.getService(PayaraFangConfiguration.class);
        init();
    }
    
    private void init() {
        try {
            endpointDecider = new PayaraFangEndpointDecider(serverConfig, fangServiceConfiguration);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Payara Fang Console cannot initialise", ex);
        }
        
        if (appExistsInConfig() && (domain.getSystemApplicationReferencedFrom(env.getInstanceName(), 
                fangServiceConfiguration.getApplicationName()) != null)) {
            setStateMsg(PayaraFangAdapterState.NOT_LOADED);
            setAppRegistered(true);
        } else {
            setStateMsg(PayaraFangAdapterState.NOT_REGISTERED);
        }
    }
    
    public boolean appExistsInConfig() {
        return (getSystemApplicationConfig() != null);
    }
    
    public boolean appExistsInConfig(String contextRoot) {
        return (getSystemApplicationConfig(contextRoot) != null);
    }
    
    public Application getSystemApplicationConfig() {
        // First, check if there is an app registered for this server with the given application name
        Application application = domain.getSystemApplicationReferencedFrom(env.getInstanceName(), 
                fangServiceConfiguration.getApplicationName());
        
        // If the app hasn't been registered to the instance yet, the previous check will return null, so check for one 
        // with a matching context root instead (as these are also unique)
        if (application == null) {
            application = getApplicationWithMatchingContextRoot();
        }
        
        return application;
    }
    
    public Application getSystemApplicationConfig(String contextRoot) {      
        // check for an app with a matching context root
        Application application = getApplicationWithMatchingContextRoot(contextRoot);
        
        return application;
    }
    
    private Application getApplicationWithMatchingContextRoot() {
        Application application = null;
        String contextRoot = getContextRoot();

        SystemApplications systemApplications = domain.getSystemApplications();
        for (Application systemApplication : systemApplications.getApplications()) {
            if (systemApplication.getContextRoot().equals(contextRoot)) {
                application = systemApplication;
                break;
            }
        }
        
        return application;
    }
    
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
    
    public PayaraFangAdapterState getStateMsg() {
        return stateMsg;
    }
    
    public void setStateMsg(PayaraFangAdapterState msg) {
        stateMsg = msg;
        logger.log(Level.FINE, msg.toString());
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
                logger.log(Level.SEVERE, "Timed out processing a Payara Fang request");
                return;
            }
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, "Cannot process Payara Fang request");
            return;
        }
        
        logRequest(request);
        
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

        String serverVersion = Version.getFullVersion();

        if ("/testifbackendisready.html".equals(request.getRequestURI())) {
            // Replace state token
            String status = getStateMsg().getI18NKey();
            try {
                // Try to get a localized version of this key
                status = bundle.getString(status);
            } catch (MissingResourceException ex) {
                // Use the non-localized String version of the status
                status = getStateMsg().toString();
            }
            
            String wkey = PayaraFangAdapterState.WELCOME_TO.getI18NKey();
            
            try {
                // Try to get a localized version of this key
                serverVersion = bundle.getString(wkey) + " " + serverVersion + ".";
            } catch (MissingResourceException ex) {
                // Use the non-localized String version of the status
                serverVersion = PayaraFangAdapterState.WELCOME_TO.toString() + " " + serverVersion + ".";
            }
            
            status += "\n" + serverVersion;
            
            try {
                OutputBuffer ob = getOutputBuffer(response);
                byte[] bytes = (":::" + status).getBytes("UTF-8");
                response.setContentLength(bytes.length);
                ob.write(bytes, 0, bytes.length);
                ob.flush();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to serve resource: {0}. Cause: {1}", ex);
            }
        }       
        // TODO: Handle application not being there
    }
    
    private ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle("com.sun.enterprise.v3.admin.adapter.LocalStrings", locale);
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

    private void logRequest(Request request) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "PayaraFangAdapter's STATE IS: {0}", getStateMsg());
            logger.log(Level.FINE, "Current Thread: {0}", Thread.currentThread().getName());
            
            for (final String name : request.getParameterNames()) {
                final String values = Arrays.toString(request.getParameterValues(name));
                logger.log(Level.FINE, "Parameter name: {0} values: {1}", new Object[]{name, values});
            }
        }
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

        ClassLoader loader = PayaraFangAdapter.class.getClassLoader();

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
    
    private OutputBuffer getOutputBuffer(Response response) {
        response.setStatus(202);
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        return response.getOutputBuffer();
    }
    
    @Override
    public HttpHandler getHttpService() {
        return this;
    }

    @Override
    public String getContextRoot() {
        return endpointDecider.getContextRoot();
    }

    @Override
    public int getListenPort() {
        return endpointDecider.getListenPort();
    }

    @Override
    public InetAddress getListenAddress() {
        return endpointDecider.getListenAddress();
    }

    @Override
    public List<String> getVirtualServers() {
        return endpointDecider.getHosts();
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
