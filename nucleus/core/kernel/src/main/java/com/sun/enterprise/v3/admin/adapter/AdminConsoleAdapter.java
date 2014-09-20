/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin.adapter;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.v3.admin.AdminConsoleConfigUpgrade;
import java.beans.PropertyVetoException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.RestrictTo;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.io.OutputBuffer;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.kernel.KernelLoggerInfo;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 * An HK-2 Service that provides the functionality so that admin console access
 * is handled properly. The general contract of this adapter is as follows: <ol>
 * <li>This adapter is *always* installed as a Grizzly adapter for a particular
 * URL designated as admin URL in domain.xml. This translates to context-root of
 * admin console application. </li> <li>When the control comes to the adapter for
 * the first time, user is asked to confirm if downloading the application is OK.
 * In that case, the admin console application is downloaded and expanded. While
 * the download and installation is happening, all the clients or browser
 * refreshes get a status message. No push from the server side is attempted
 * (yet). After the application is "installed", ApplicationLoaderService is
 * contacted, so that the application is loaded by the containers. This
 * application is available as a
 * <code> system-application </code> and is persisted as such in the domain.xml.
 * </li> <li>Even after this application is available, we don't load it on server
 * startup by default. It is always loaded
 * <code> on demand </code>. Hence, this adapter will always be available to
 * find out if application is loaded and load it in the container(s) if it is
 * not. If the application is already loaded, it simply exits. </li> </ol>
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352; (km@dev.java.net)
 * @author Ken Paulsen (kenpaulsen@dev.java.net)
 * @author Siraj Ghaffar (sirajg@dev.java.net)
 * @since GlassFish V3 (March 2008)
 */
@Service
public final class AdminConsoleAdapter extends HttpHandler implements Adapter, PostConstruct, EventListener {

    @Inject
    ServerEnvironmentImpl env;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    AdminService adminService;
    private String contextRoot;
    private File warFile;    // GF Admin Console War File Location
    private AdapterState stateMsg = AdapterState.UNINITIAZED;
    private boolean installing = false;
    private boolean isOK = false;  // FIXME: initialize this with previous user choice
    private AdminConsoleConfigUpgrade adminConsoleConfigUpgrade = null;
    private final CountDownLatch latch = new CountDownLatch(1);
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
    
    AdminEndpointDecider epd;
    private static final Logger logger = KernelLoggerInfo.getLogger();
    private String statusHtml;
    private String initHtml;
    private boolean isRegistered = false;
    private ResourceBundle bundle;
    //don't change the following without changing the html pages
    private static final String MYURL_TOKEN = "%%%MYURL%%%";
    private static final String STATUS_TOKEN = "%%%STATUS%%%";
    private static final String REDIRECT_TOKEN = "%%%LOCATION%%%";
    private static final String RESOURCE_PACKAGE = "com/sun/enterprise/v3/admin/adapter";
    private static final String INSTALL_ROOT = "com.sun.aas.installRoot";
    static final String ADMIN_APP_NAME = ServerEnvironmentImpl.DEFAULT_ADMIN_CONSOLE_APP_NAME;
    private boolean isRestStarted = false;
    private boolean isRestBeingStarted = false;

    /**
     * Constructor.
     */
    public AdminConsoleAdapter() throws IOException {
        initHtml = Utils.packageResource2String("downloadgui.html");
        statusHtml = Utils.packageResource2String("status.html");
    }

    /**
     *
     */
    @Override
    public String getContextRoot() {
        return epd.getGuiContextRoot(); //default is /admin
    }

    @Override
    public final HttpHandler getHttpService() {
        return this;
    }

    /**
     *
     */
    @Override
    public void service(Request req, Response res) {

        bundle = getResourceBundle(req.getLocale());

        Method method = req.getMethod();
        if (!checkHttpMethodAllowed(method)) {
            res.setStatus(java.net.HttpURLConnection.HTTP_BAD_METHOD,
                    method.getMethodString() + " " + bundle.getString("http.bad.method"));
            res.setHeader("Allow", getAllowedHttpMethodsAsString());
            return;
        }
        if (!env.isDas()) {
            sendStatusNotDAS(req, res);
            return;
        }

        //This is needed to support the case where user update to 3.1 from previous release, and didn't run the upgrade tool.
        if (adminConsoleConfigUpgrade == null) {
            adminConsoleConfigUpgrade = habitat.getService(AdminConsoleConfigUpgrade.class);
        }

        try {
            if (!latch.await(100L, TimeUnit.SECONDS)) {
                // todo : better error reporting.
                logger.log(Level.SEVERE, KernelLoggerInfo.consoleRequestTimeout);
                return;
            }
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, KernelLoggerInfo.consoleCannotProcess);
            return;
        }
        logRequest(req);
        if (isResourceRequest(req)) {
            try {
                handleResourceRequest(req, res);
            } catch (IOException ioe) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, KernelLoggerInfo.consoleResourceError,
                            new Object[]{req.getRequestURI(), ioe.toString()});
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            ioe.toString(),
                            ioe);
                }
            }
            return;
        }
        res.setContentType("text/html; charset=UTF-8");

        // simple get request use via javascript to give back the console status (starting with :::)
        // as a simple string.
        // see usage in status.html


        String serverVersion = Version.getFullVersion();

        if ("/testifbackendisready.html".equals(req.getRequestURI())) {

            // Replace state token
            String status = getStateMsg().getI18NKey();
            try {
                // Try to get a localized version of this key
                status = bundle.getString(status);
            } catch (MissingResourceException ex) {
                // Use the non-localized String version of the status
                status = getStateMsg().toString();
            }
            String wkey = AdapterState.WELCOME_TO.getI18NKey();
            try {
                // Try to get a localized version of this key
                serverVersion = bundle.getString(wkey) + " " + serverVersion + ".";
            } catch (MissingResourceException ex) {
                // Use the non-localized String version of the status
                serverVersion = AdapterState.WELCOME_TO.toString() + " " + serverVersion + ".";
            }
            status += "\n" + serverVersion;
            try {
                OutputBuffer ob = getOutputBuffer(res);

                byte[] bytes = (":::" + status).getBytes("UTF-8");
                res.setContentLength(bytes.length);
                ob.write(bytes, 0, bytes.length);
                ob.flush();

            } catch (IOException ex) {
                logger.log(Level.SEVERE, KernelLoggerInfo.consoleResourceError, ex);
            }


            return;
        }
        if (isApplicationLoaded()) {
            // Let this pass to the admin console (do nothing)
            handleLoadedState();
        } else {
            // if the admin console is not loaded, and someone use the REST access,
            //browsers also request the favicon icon... Since we do not want to load
            // the admin gui just to return a non existing icon,
            //we just return without loading the entire console...
            if ("/favicon.ico".equals(req.getRequestURI())) {
                return;
            }
            if (!isRestStarted) {
                forceRestModuleLoad(req);
            }
	    synchronized(this) {
		if (isInstalling()) {
		    sendStatusPage(req, res);
		} else {
                    if (isApplicationLoaded()) {
			// Double check here that it is not yet loaded (not
			// likely, but possible)
			handleLoadedState();
		    }else {
                        loadConsole();
			sendStatusPage(req, res);
		    }
		}
	    }

        }

    }

    void loadConsole() {
        try {
            // We have permission and now we should install
            // (or load) the application.
            setInstalling(true);
            startThread();  // Thread must set installing false
        } catch (Exception ex) {
            // Ensure we haven't crashed with the installing
            // flag set to true (not likely).
            setInstalling(false);
            throw new RuntimeException(
                    "Unable to install Admin Console!", ex);
        }
    }
    
    /**
     * @param req the Request
     * @return <code>true</code> if the request is for a resource with a known content
     *  type otherwise <code>false</code>.
     */
    private boolean isResourceRequest(Request req) {
        return (getContentType(req.getRequestURI()) != null);
    }

    /**
     * All that needs to happen for the REST module to be initialized is a request
     * of some sort.  Here, we don't care about the response, so we make the request
     * then close the stream and move on.
     */
    private void forceRestModuleLoad(final Request req) {
        if (isRestBeingStarted==true){
            return;
        }
        isRestBeingStarted = true;
        Thread thread = new Thread("Force REST Module Load Thread") {
            @Override
            public void run() {
                initRest();
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    private String getContentType(String resource) {

        if (resource == null || resource.length() == 0) {
            return null;
        }
        // this may need to be expanded upon the future, in which case, the
        // current implementation may not be worth maintaining
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

    private void handleResourceRequest(Request req, Response res)
            throws IOException {

        String resourcePath = RESOURCE_PACKAGE + req.getRequestURI();

        ClassLoader loader = AdminConsoleAdapter.class.getClassLoader();

        InputStream in = null;
        try {
            in = loader.getResourceAsStream(resourcePath);
            if (in == null) {
                logger.log(Level.WARNING, KernelLoggerInfo.consoleResourceNotFound, resourcePath);
                return;
            }
            byte[] buf = new byte[512];
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            for (int i = in.read(buf); i != -1; i = in.read(buf)) {
                baos.write(buf, 0, i);
            }
            String contentType = getContentType(resourcePath);
            if (contentType != null) {
                res.setContentType(contentType);
            }
            res.setContentLength(baos.size());
            OutputStream out = res.getOutputStream();
            baos.writeTo(out);
            out.flush();

        } finally {
            if (in != null) {
                in.close();
            }
        }

    }

    boolean isApplicationLoaded() {
        return (stateMsg == AdapterState.APPLICATION_LOADED);
    }

    /**
     *
     */
    boolean isInstalling() {
        return installing;
    }

    /**
     *
     */
    void setInstalling(boolean flag) {
        installing = flag;
    }

    /**
     * Checks whether this adapter has been registered as a network endpoint.
     */
    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    /**
     * Marks this adapter as having been registered or unregistered as a
     * network endpoint
     */
    @Override
    public void setRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    /**
     * <p> This method sets the current state.</p>
     */
    void setStateMsg(AdapterState msg) {
        stateMsg = msg;
        logger.log(Level.FINE, msg.toString());
    }

    /**
     * <p> This method returns the current state, which will be one of the
     * valid values defined by {@link AdapterState}.</p>
     */
    AdapterState getStateMsg() {
        return stateMsg;
    }

    /**
     *
     */
    @Override
    public void postConstruct() {
        events.register(this);
        //set up the environment properly
        init();
    }

    /**
     *
     */
    @Override
    public void event(@RestrictTo(EventTypes.SERVER_READY_NAME) Event event) {
        latch.countDown();
        if (logger != null) {
            logger.log(Level.FINE, "AdminConsoleAdapter is ready.");
        }
    }

    /**
     *
     */
    private void init() {
        Property locProp = adminService.getProperty(ServerTags.ADMIN_CONSOLE_DOWNLOAD_LOCATION);
        if (locProp == null || locProp.getValue() == null || locProp.getValue().equals("")) {
            String iRoot = System.getProperty(INSTALL_ROOT) + "/lib/install/applications/admingui.war";
            warFile = new File(iRoot.replace('/', File.separatorChar));
            writeAdminServiceProp(ServerTags.ADMIN_CONSOLE_DOWNLOAD_LOCATION, "${" + INSTALL_ROOT + "}/lib/install/applications/admingui.war");
        } else {
            //For any non-absolute path, we start from the installation, ie glassfish4
            //eg, v3 prelude upgrade, where the location property was "glassfish/lib..."
            String locValue = locProp.getValue();
            warFile = new File(locValue);
            if (!warFile.isAbsolute()) {
                File tmp = new File(System.getProperty(INSTALL_ROOT), "..");
                warFile = new File(tmp, locValue);
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Admin Console download location: {0}", warFile.getAbsolutePath());
        }
        
        initState();
        
        try {
            epd = new AdminEndpointDecider(serverConfig);
            contextRoot = epd.getGuiContextRoot();
        } catch (Exception ex) {
            logger.log(Level.INFO, KernelLoggerInfo.consoleCannotInitialize, ex);
            return;
        }
    }
    
    void initRest() {
        InputStream is = null;
        try {
            NetworkListener nl = domain.getServerNamed("server").getConfig().getNetworkConfig()
                    .getNetworkListener("admin-listener");
            SecureAdmin secureAdmin = habitat.getService(SecureAdmin.class);

            URL url = new URL(
                    (SecureAdmin.Util.isEnabled(secureAdmin) ? "https" : "http"),
                    nl.getAddress(),
                    Integer.parseInt(nl.getPort()),
                    "/management/domain");
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            isRestStarted = true;
        } catch (Exception ex) {
           logger.log(Level.FINE, null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex1) {
                    logger.log(Level.FINE, null, ex1);
                }
            }
        }
    }
    

    /**
     *
     */
    private void initState() {
        // It is a given that the application is NOT loaded to begin with
        if (appExistsInConfig()) {
            isOK = true; // FIXME: I don't think this is good enough
            setStateMsg(AdapterState.APPLICATION_INSTALLED_BUT_NOT_LOADED);
        } else if (new File(warFile.getParentFile(), ADMIN_APP_NAME).exists() || warFile.exists()) {
            // The exploded dir, or the .war exists... mark as downloded
            if (logger.isLoggable(Level.FINE)) {
                setStateMsg(AdapterState.DOWNLOADED);
            }
            isOK = true;
        } else {
            setStateMsg(AdapterState.APPLICATION_NOT_INSTALLED);
        }
    }

    /**
     *
     */
    private boolean appExistsInConfig() {
        return (getConfig() != null);
    }

    /**
     *
     */
    Application getConfig() {
        //no application-ref logic here -- that's on purpose for now
        Application app = domain.getSystemApplicationReferencedFrom(env.getInstanceName(), ADMIN_APP_NAME);

        return app;
    }

    /**
     *
     */
    private void logRequest(Request req) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "AdminConsoleAdapter''s STATE IS: {0}", getStateMsg());
            logger.log(Level.FINE, "Current Thread: {0}", Thread.currentThread().getName());
            for (final String name : req.getParameterNames()) {
                final String values = Arrays.toString(req.getParameterValues(name));
                logger.log(Level.FINE, "Parameter name: {0} values: {1}", new Object[]{name, values});
            }
        }
    }

    /**
     *
     */
    enum InteractionResult {

        OK,
        CANCEL,
        FIRST_TIMER;
    }

    /**
     * <p> Determines if the user has permission.</p>
     */
    private boolean hasPermission(InteractionResult ir) {
        //do this quickly as this is going to block the grizzly worker thread!
        //check for returning user?
        if (ir == InteractionResult.OK) {
            isOK = true;
        }
        return isOK;
    }

    /**
     *
     */
    private void startThread() {
        new InstallerThread(this, habitat, domain, env, contextRoot, epd.getGuiHosts()).start();
    }

    /**
     *
     */
    /*
    private synchronized InteractionResult getUserInteractionResult(GrizzlyRequest req) {
        if (req.getParameter(OK_PARAM) != null) {
            proxyHost = req.getParameter(PROXY_HOST_PARAM);
            if ((proxyHost != null) && !proxyHost.equals("")) {
                String ps = req.getParameter(PROXY_PORT_PARAM);
                try {
                    proxyPort = Integer.parseInt(ps);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                            "The specified proxy port (" + ps
                                    + ") must be a valid port integer!", nfe);
                }
            }
// FIXME: I need to "remember" this answer in a persistent way!! Or it will popup this message EVERY time after the server restarts.
            setStateMsg(AdapterState.PERMISSION_GRANTED);
            isOK = true;
            return InteractionResult.OK;
        } else if (req.getParameter(CANCEL_PARAM) != null) {
            // Canceled
// FIXME: I need to "remember" this answer in a persistent way!! Or it will popup this message EVERY time after the server restarts.
            setStateMsg(AdapterState.CANCELED);
            isOK = false;
            return InteractionResult.CANCEL;
        }

        // This is a first-timer
        return InteractionResult.FIRST_TIMER;
    }
     *
     */
    private OutputBuffer getOutputBuffer(Response res) {
        res.setStatus(202);
        res.setContentType("text/html");
        res.setCharacterEncoding("UTF-8");
        return res.getOutputBuffer();
    }

    /**
     *
     */
    private void sendStatusPage(Request req, Response res) {
        byte[] bytes;
        try {
            OutputBuffer ob = getOutputBuffer(res);
            // Replace locale specific Strings
            String localHtml = replaceTokens(statusHtml, bundle);

            // Replace state token
            String status = getStateMsg().getI18NKey();
            try {
                // Try to get a localized version of this key
                status = bundle.getString(status);
            } catch (MissingResourceException ex) {
                // Use the non-localized String version of the status
                status = getStateMsg().toString();
            }
            String locationUrl = req.getScheme()
                    + "://" + req.getServerName()
                    + ':' + req.getServerPort() + "/login.jsf";
            localHtml = localHtml.replace(REDIRECT_TOKEN, locationUrl);
            bytes = localHtml.replace(STATUS_TOKEN, status).getBytes("UTF-8");
            res.setContentLength(bytes.length);
            ob.write(bytes, 0, bytes.length);
            ob.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     */
    private void sendStatusNotDAS(Request req, Response res) {
        byte[] bytes;
        try {
            String html = Utils.packageResource2String("statusNotDAS.html");
            OutputBuffer ob = getOutputBuffer(res);
            // Replace locale specific Strings
            String localHtml = replaceTokens(html, bundle);

            bytes = localHtml.getBytes("UTF-8");
            res.setContentLength(bytes.length);
            ob.write(bytes, 0, bytes.length);
            ob.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * <p> This method returns the resource bundle for localized Strings used
     * by the AdminConsoleAdapter.</p>
     *
     * @param    locale    The Locale to be used.
     */
    private ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(
                "com.sun.enterprise.v3.admin.adapter.LocalStrings", locale);
    }

    /**
     * <p> This method replaces all tokens in text with values from the given
     * <code>ResourceBundle</code>.  A token starts and ends with 3
     * percent (%) characters.  The value between the percent characters
     * will be used as the key to the given <code>ResourceBundle</code>.
     * If a key does not exist in the bundle, no substitution will take
     * place for that token.</p>
     *
     * @return The same text except with substituted tokens when available.
     * @param    text    The text containing tokens to be replaced.
     * @param    bundle    The <code>ResourceBundle</code> with keys for the value
     */
    private String replaceTokens(String text, ResourceBundle bundle) {
        int start = 0, end = 0;
        String newString = null;
        StringBuilder buf = new StringBuilder("");

        while (start != -1) {
            // Find start of token
            start = text.indexOf("%%%", end);
            if (start != -1) {
                // First copy the stuff before the start
                buf.append(text.substring(end, start));

                // Move past the %%%
                start += 3;

                // Find end of token
                end = text.indexOf("%%%", start);
                if (end != -1) {
                    try {
                        // Copy the token value to the buffer
                        buf.append(
                                bundle.getString(text.substring(start, end)));
                    } catch (MissingResourceException ex) {
                        // Unable to find the resource, so we don't do anything
                        buf.append("%%%").append(text.substring(start, end)).append("%%%");
                    }

                    // Move past the %%%
                    end += 3;
                } else {
                    // Add back the %%% because we didn't find a matching end
                    buf.append("%%%");

                    // Reset end so we can copy the remainder of the text
                    end = start;
                }
            }
        }

        // Copy the remainder of the text
        buf.append(text.substring(end));

        // Return the new String
        return buf.toString();
    }

    public AdminService getAdminService() {
        return adminService;
    }

    private void writeAdminServiceProp(final String propName, final String propValue) {
        try {
            ConfigSupport.apply(new SingleConfigCode<AdminService>() {

                @Override
                public Object run(AdminService adminService) throws PropertyVetoException, TransactionFailure {
                    Property newProp = adminService.createChild(Property.class);
                    adminService.getProperty().add(newProp);
                    newProp.setName(propName);
                    newProp.setValue(propValue);
                    return newProp;
                }
            }, adminService);
        } catch (Exception ex) {
            logger.log(Level.WARNING, KernelLoggerInfo.consoleCannotWriteProperty, 
                    new Object[] {propName, propValue, ex});
        }
    }

    /**
     *
     */
    private void handleLoadedState() {
//System.out.println(" Handle Loaded State!!");
        // do nothing
        statusHtml = null;
        initHtml = null;
    }

    @Override
    public int getListenPort() {
        return epd.getListenPort();
    }

    @Override
    public InetAddress getListenAddress() {
        return epd.getListenAddress();
    }

    @Override
    public List<String> getVirtualServers() {
        return epd.getGuiHosts();
    }
//    enum HttpMethod {
//        OPTIONS ("OPTIONS"),
//        GET ("GET"),
//        HEAD ("HEAD"),
//        POST ("POST"),
//        PUT ("PUT"),
//        DELETE ("DELETE"),
//        TRACE ("TRACE"),
//        CONNECT ("CONNECT");
//
//        private String method;
//
//        HttpMethod(String method) {
//            this.method = method;
//        }
//
//        static HttpMethod getHttpMethod(String httpMethod) {
//            for (HttpMethod hh: HttpMethod.values()) {
//                if (hh.method.equalsIgnoreCase(httpMethod)) {
//                    return hh;
//                }
//            }
//            return null;
//        }
//
//        String method() {
//            return method;
//        }
//    }
    private Method[] allowedHttpMethods = {Method.GET, Method.POST, Method.HEAD,
        Method.DELETE, Method.PUT};

    private boolean checkHttpMethodAllowed(Method method) {
        for (Method hh : allowedHttpMethods) {
            if (hh.equals(method)) {
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
}
