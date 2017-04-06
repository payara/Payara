/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.fang.service.adapter;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.fang.service.PayaraFangService;
import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service
public final class PayaraFangAdapter extends HttpHandler implements Adapter {
    private PayaraFangAdapterState stateMsg = PayaraFangAdapterState.UNINITIALISED;
    private boolean isOK = false;
    private boolean isRegistered = false;
    private ResourceBundle bundle;
    private Method[] allowedHttpMethods = {Method.GET, Method.POST, Method.HEAD, Method.DELETE, Method.PUT};

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
    
    private final static Logger logger = Logger.getLogger(PayaraFangAdapter.class.getName());
    private final static String RESOURCE_PACKAGE = "fish/payara/appserver/fang/adapter";
    private final CountDownLatch latch = new CountDownLatch(1);
    
    @PostConstruct
    public void postConstruct() {
        init();
    }
    
    private void init() {
        if (appExistsInConfig()) {
            setStateMsg(PayaraFangAdapterState.NOT_LOADED);
        } else {
            setStateMsg(PayaraFangAdapterState.NOT_REGISTERED);
        }
        
        try {
            PayaraFangConfiguration fangServiceConfiguration = habitat.getService(PayaraFangConfiguration.class);
            endpointDecider = new PayaraFangEndpointDecider(serverConfig, fangServiceConfiguration);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Payara Fang Console cannot initialise", ex);
        }
    }
    
    private boolean appExistsInConfig() {
        return (getConfig() != null);
    }
    
    public Application getConfig() {
        return domain.getSystemApplicationReferencedFrom(env.getInstanceName(), PayaraFangService.FANG_APP_NAME);
    }
    
    private PayaraFangAdapterState getStateMsg() {
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
}
