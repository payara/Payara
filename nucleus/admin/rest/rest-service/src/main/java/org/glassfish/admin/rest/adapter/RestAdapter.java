/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.adapter;



import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import com.sun.logging.LogDomains;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.LazyJerseyInterface;
import org.glassfish.admin.rest.ResourceUtil;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.SessionManager;
import org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider;
import org.glassfish.admin.rest.provider.ActionReportResultJsonProvider;
import org.glassfish.admin.rest.provider.ActionReportResultXmlProvider;
import org.glassfish.admin.rest.provider.BaseProvider;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.PostStartup;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;

/**
 * Adapter for REST interface
 * @author Rajeshwar Patil, Ludovic Champenois
 */
public abstract class RestAdapter extends HttpHandler implements Adapter, PostStartup, PostConstruct {

    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(RestService.class);

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile AdminService as;

    @Inject
    Habitat habitat;

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    CountDownLatch latch = new CountDownLatch(1);

    @Inject
    ServerContext sc;

    @Inject
    ServerEnvironment serverEnvironment;

    @Inject
    SessionManager sessionManager;

    protected abstract Set<Class<?>> getResourcesConfig();
    private volatile LazyJerseyInterface lazyJerseyInterface =null;
    private static final Logger logger = LogDomains.getLogger(RestAdapter.class, LogDomains.ADMIN_LOGGER);
    private volatile HttpHandler adapter = null;
    private boolean isRegistered = false;
    private AdminEndpointDecider epd = null;

    protected RestAdapter() {
        setAllowEncodedSlash(true);
    }

    @Override
    public void postConstruct() {
        epd = new AdminEndpointDecider(config, logger);
        //        events.register(this);
        /*
        try {
            exposeContext();
        } catch (EndpointRegistrationException ex) {
            Logger.getLogger(RestAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }
        */

        latch.countDown();
    }

    @Override
    public HttpHandler getHttpService() {
        return this;
    }

    @Override
    public void service(Request req, Response res) {
        LogHelper.getDefaultLogger().finer("Rest adapter !");
        LogHelper.getDefaultLogger().log(Level.FINER, "Received resource request: {0}", req.getRequestURI());

        try {
            res.setCharacterEncoding(Constants.ENCODING);
            if (latch.await(20L, TimeUnit.SECONDS)) {
                if(serverEnvironment.isInstance()) {
                    if(!Method.GET.equals(req.getMethod())) {
                        String msg = localStrings.getLocalString("rest.resource.only.GET.on.instance", "Only GET requests are allowed on an instance that is not DAS.");
                        reportError(req, res, HttpURLConnection.HTTP_FORBIDDEN, msg);
                        return;
                    }
                }

                AdminAccessController.Access access = authenticate(req);
                if (access == AdminAccessController.Access.FULL) {
                    exposeContext();
                    //delegate to adapter managed by Jersey.
                    adapter.service(req, res);
                } else { // Access != FULL
                    String msg;
                    int status;
                    if(access == AdminAccessController.Access.NONE) {
                        status = HttpURLConnection.HTTP_UNAUTHORIZED;
                        msg = localStrings.getLocalString("rest.adapter.auth.userpassword", 
                                "Invalid user name or password");
                        res.setHeader("WWW-Authenticate", "BASIC");
                    } else {
                        assert access == AdminAccessController.Access.FORBIDDEN;
                        status = HttpURLConnection.HTTP_FORBIDDEN;
                        msg = localStrings.getLocalString("rest.adapter.auth.forbidden", 
                                "Remote access not allowed. If you desire remote access, please turn on secure admin");
                    }
                    reportError(req, res, status, msg);
                }
            } else { // !latch.await(...)
                String msg = localStrings.getLocalString("rest.adapter.server.wait", 
                        "Server cannot process this command at this time, please wait");
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg);
            }
        } catch(InterruptedException e) {
                String msg = localStrings.getLocalString("rest.adapter.server.wait", 
                        "Server cannot process this command at this time, please wait");
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
        } catch(IOException e) {
                String msg = localStrings.getLocalString("rest.adapter.server.ioexception", 
                        "REST: IO Exception "+e.getLocalizedMessage());
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
        } catch(LoginException e) {
            String msg = localStrings.getLocalString("rest.adapter.auth.error", "Error authenticating");
            reportError(req, res, HttpURLConnection.HTTP_UNAUTHORIZED, msg); //authentication error
        } catch (Exception e) {
            String msg = localStrings.getLocalString("rest.adapter.server.exception", 
                    "An error occurred while processing the request. Please see the server logs for details.");
            reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
        }
    }
    
    /**
     * Authenticate given request
     * @return Access as determined by authentication process.
     *         If authentication succeeds against local password or rest token FULL access is granted
     *         else the access is as returned by admin authenticator
     * @see ResourceUtil.authenticateViaAdminRealm(Habitat, Request)
     *
     */
    private AdminAccessController.Access authenticate(Request req) throws LoginException, IOException {
        AdminAccessController.Access access = AdminAccessController.Access.FULL;
        boolean authenticated = authenticateViaLocalPassword(req);
        if (!authenticated) {
            authenticated = authenticateViaRestToken(req);
            if (!authenticated) {
                access = ResourceUtil.authenticateViaAdminRealm(habitat, req, req.getRemoteHost());
            }
        }
        return access;
    }

    private boolean authenticateViaRestToken(Request req) {
        boolean authenticated = false;
        Cookie[] cookies = req.getCookies();
        String restToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("gfresttoken".equals(cookie.getName())) {
                    restToken = cookie.getValue();
                }
            }
        }
        
        if (restToken == null) {
            restToken = req.getHeader("X-Auth-Token");
        }

        if(restToken != null) {
            authenticated  = sessionManager.authenticate(restToken, req);
        }
        return authenticated;
    }

    private boolean authenticateViaLocalPassword(Request req) {
        Cookie[] cookies = req.getCookies();
        boolean authenticated = false;
        String uid = RestService.getRestUID();
        if (uid != null) {
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("gfrestuid")) {
                        if (cookie.getValue().equals(uid)) {
                            authenticated = true;
                            break;
                        }
                    }
                }
            }
        }
        return authenticated;
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
        return epd.getAsadminHosts();
    }

    private String getAcceptedMimeType(Request req) {
        String type = null;
        String requestURI = req.getRequestURI();
        Set<String> acceptableTypes = new HashSet<String>(3);
        acceptableTypes.add("html");
        acceptableTypes.add("xml");
        acceptableTypes.add("json");

        // first we look at the command extension (ie list-applications.[json | html | mf]
        if (requestURI.indexOf('.')!=-1) {
            type = requestURI.substring(requestURI.indexOf('.')+1);
        } else {
            String userAgent = req.getHeader("User-Agent");
            if (userAgent != null) {
                String accept = req.getHeader("Accept");
                if (accept != null) {
                    if (accept.indexOf("html") != -1) {//html is possible so get it...
                        return "html";
                    }
                    StringTokenizer st = new StringTokenizer(accept, ",");
                    while (st.hasMoreElements()) {
                        String scheme=st.nextToken();
                        scheme = scheme.substring(scheme.indexOf('/')+1);
                        if (acceptableTypes.contains(scheme)) {
                            type = scheme;
                            break;
                        }
                    }
                }
            }
        }

        return type;
    }

    /*
     * dynamically load the class that contains all references to Jersey APIs
     * so that Jersey is not loaded when the RestAdapter is loaded at boot time
     * gain a few 100millis at GlassFish startyp time
     */
    protected LazyJerseyInterface getLazyJersey() {
        if (lazyJerseyInterface != null) {
            return lazyJerseyInterface;
        }
        synchronized (HttpHandler.class) {
            if (lazyJerseyInterface == null) {
               try {
                    Class<?> lazyInitClass = Class.forName("org.glassfish.admin.rest.LazyJerseyInit");
                    lazyJerseyInterface = (LazyJerseyInterface) lazyInitClass.newInstance();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE,
                            "Error trying to call org.glassfish.admin.rest.LazyJerseyInit via instrospection: ", ex);
                }
            }
        }
        return lazyJerseyInterface;

    }

    private void exposeContext() throws EndpointRegistrationException {
        //Use double checked locking to lazily initialize adapter
        if (adapter == null) {
            synchronized (HttpHandler.class) {
                if (adapter == null) {
                    String context = getContextRoot();
                    logger.log(Level.FINE, "Exposing rest resource context root: {0}", context);
                    if ((context != null) && (!"".equals(context))) {
                        Set<Class<?>> classes = getResourcesConfig();
                        adapter = lazyJerseyInterface.exposeContext(classes, sc, habitat);
                        logger.log(Level.INFO, "rest.rest_interface_initialized", context);
                    }
                }
            }
        }
    }

    private void reportError(Request req, Response res, int statusCode, String msg) {
        try {
            // TODO: There's a lot of arm waving and flailing here.  I'd like this to be cleaner, but I don't
            // have time at the moment.  jdlee 8/11/10
            RestActionReporter report = new RestActionReporter(); //getClientActionReport(req);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setActionDescription("Error");
            report.setMessage(msg);
            BaseProvider<ActionReportResult> provider;
            String type = getAcceptedMimeType(req);
            if ("xml".equals(type)) {
                res.setContentType("application/xml");
                provider = new ActionReportResultXmlProvider();
            } else if ("json".equals(type)) {
                res.setContentType("application/json");
                provider = new ActionReportResultJsonProvider();
            } else {
                res.setContentType("text/html");
                provider = new ActionReportResultHtmlProvider();
            }
            res.setStatus(statusCode);
            res.getOutputStream().write(provider.getContent(new ActionReportResult(report)).getBytes());
            res.getOutputStream().flush();
            res.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
