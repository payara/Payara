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
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.admin.AdminAdapter;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import com.sun.logging.LogDomains;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import javax.security.auth.login.LoginException;

import org.glassfish.admin.rest.LazyJerseyInterface;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.SessionManager;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.RestrictTo;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider;
import org.glassfish.admin.rest.provider.ActionReportResultJsonProvider;
import org.glassfish.admin.rest.provider.ActionReportResultXmlProvider;
import org.glassfish.admin.rest.provider.BaseProvider;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.ServerContext;
import java.util.logging.Level;

/**
 * Adapter for REST interface
 * @author Rajeshwar Patil, Ludovic Champenois
 */
public abstract class RestAdapter extends HttpHandler implements Adapter, PostConstruct, EventListener {

    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(RestAdapter.class);

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile AdminService as;

    @Inject
    Events events;

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

    private volatile LazyJerseyInterface lazyJerseyInterface =null;

    private static final Logger logger = LogDomains.getLogger(RestAdapter.class, LogDomains.ADMIN_LOGGER);

    private Map<Integer, String> httpStatus = new HashMap<Integer, String>() {{
        put(404, "Resource not found");
        put(500, "A server error occurred. Please check the server logs.");
    }};

    protected RestAdapter() {
        setAllowEncodedSlash(true);
    }


    @Override
    public void postConstruct() {
        epd = new AdminEndpointDecider(config, logger);
        events.register(this);
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
            if (!latch.await(20L, TimeUnit.SECONDS)) {
                String msg = localStrings.getLocalString("rest.adapter.server.wait",
                        "Server cannot process this command at this time, please wait");
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg);
                return;
            } else {

                if(serverEnvironment.isInstance()) {
                    if(!Method.GET.equals(req.getMethod())) {
                        String msg = localStrings.getLocalString("rest.resource.only.GET.on.instance", "Only GET requests are allowed on an instance that is not DAS.");
                        reportError(req, res, HttpURLConnection.HTTP_FORBIDDEN, msg);
                        return;
                    }
                }

                if (!authenticate(req)) {
                    //Could not authenticate throw error
                    String msg = localStrings.getLocalString("rest.adapter.auth.userpassword", "Invalid user name or password");
                    res.setHeader("WWW-Authenticate", "BASIC");
                    reportError(req, res, HttpURLConnection.HTTP_UNAUTHORIZED, msg);
                    return;
                }

                //Use double checked locking to lazily initialize adapter
                if (adapter == null) {
                    synchronized(HttpHandler.class) {
                        if(adapter == null) {
                            exposeContext();  //Initializes adapter
                        }
                    }

                }

                //delegate to adapter managed by Jersey.
                ((HttpHandler)adapter).service(req, res);
                int status = res.getStatus();
                if (status < 200 || status > 299) {
                    String message = httpStatus.get(status);
                    if (message == null) {
                        // i18n
                        message = "Request returned " + status;
                    }

//                    reportError(req, res, status, message);
                }
            }
        } catch(InterruptedException e) {
                String msg = localStrings.getLocalString("rest.adapter.server.wait",
                        "Server cannot process this command at this time, please wait");
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
                return;
        } catch(IOException e) {
                String msg = localStrings.getLocalString("rest.adapter.server.ioexception",
                        "REST: IO Exception "+e.getLocalizedMessage());
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
                return;
        } catch(LoginException e) {
            String msg = localStrings.getLocalString("rest.adapter.auth.error", "Error authenticating");
            reportError(req, res, HttpURLConnection.HTTP_UNAUTHORIZED, msg); //authentication error
            return;
        } catch (Exception e) {
            StringWriter result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            String msg = localStrings.getLocalString("rest.adapter.server.exception",
                    "REST:  Exception " + result.toString());
            reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
            return;
        }
    }

    private boolean authenticate(Request req) throws LoginException, IOException {
        boolean authenticated = authenticateViaAnonymousUser(req);

        if (!authenticated) {
	    authenticated = authenticateViaLocalPassword(req);
	    if (!authenticated) {
		authenticated = authenticateViaRestToken(req);
		if (!authenticated) {
		    authenticated = authenticateViaAdminRealm(req);
		}
	    }
	}

        return authenticated;
    }

    /**
     *	<p> This method should return <code>true</code> if there is an
     *	    <em>anonymous user</em>.  It should also set an attribute called
     *	    "<code>restUser</code>" on the <code>Request</code>
     *	    containing the username of the anonymous user.  If the anonymous
     *	    user is not valid, then this method should return
     *	    <code>false</code>.</p>
     *
     *	<p> The <em>anonymous user</em> exists when there is only 1 admin user,
     *	    and that admin user's password is set to the empty string ("").  In
     *	    this case, the user should not be prompted for a username &amp;
     *	    password, but instead access should be automatically granted.</p>
     */
    private boolean authenticateViaAnonymousUser(Request req) {
// FIXME: Implement according to JavaDoc above...
	/*
	if (anonymousUser) {
	    String anonUser =
	    req.setAttribute("restUser", anonUser);
	    return true;
	}
	*/
	return false;
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


    private boolean authenticateViaAdminRealm(Request req) throws LoginException, IOException  {
        String[] up = AdminAdapter.getUserPassword(req);
        String user = up[0];
        String password = up.length > 1 ? up[1] : "";
        AdminAccessController authenticator = habitat.getByContract(AdminAccessController.class);
        if (authenticator != null) {
            return authenticator.loginAsAdmin(user, password, as.getAuthRealmName(), req.getRemoteHost(),
                    getAuthRelatedHeaders(req), req.getUserPrincipal()) != AdminAccessController.Access.NONE;
        }
        return true;   //if the authenticator is not available, allow all access - per Jerome
    }

    /**
     * Extract authentication related headers from Grizzly request.
     * This headers enables us to authenticate a request coming from DAS without a password.
     * The headers will be present if secured admin is not turned on and a request is sent from DAS to an instance.
     * @param req
     * @return Authentication related headers
     */
    private Map<String, String> getAuthRelatedHeaders(Request req) {
        Map<String, String> authRelatedHeaders = Collections.EMPTY_MAP;
        String adminIndicatorHeader = req.getHeader(SecureAdmin.Util.ADMIN_INDICATOR_HEADER_NAME);
        if(adminIndicatorHeader != null) {
            authRelatedHeaders = new HashMap<String, String>(1);
            authRelatedHeaders.put(SecureAdmin.Util.ADMIN_INDICATOR_HEADER_NAME, adminIndicatorHeader);
        }
        return authRelatedHeaders;
    }


    /**
     * Finish the response and recycle the request/response tokens. Base on
     * the connection header, the underlying socket transport will be closed
     */
//    @Override
//    public void afterService(Request req, Response res) throws Exception {
//
//    }


    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Adapter.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireAdapterEvent(String type, Object data) {

    }


    @Override
    public void event(@RestrictTo(EventTypes.SERVER_READY_NAME) Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            latch.countDown();
            logger.fine("Ready to receive REST resource requests");
        }
        //the count-down does not start if any other event is received
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



    protected abstract Set<Class<?>> getResourcesConfig();

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

//    private ActionReport getClientActionReport(Request req) {
//        ActionReport report=null;
//        String requestURI = req.getRequestURI();
//        String acceptedMimeType = getAcceptedMimeType(req);
//        report = habitat.getComponent(ActionReport.class, acceptedMimeType);
//
//        if (report==null) {
//            // get the default one.
//            report = habitat.getComponent(ActionReport.class, "html");
//        }
//        report.setActionDescription("REST");
//        return report;
//    }

    /*
     * dynamically load the class that contains all references to Jersey APIs
     * so that Jersey is not loaded when the RestAdapter is loaded at boot time
     * gain a few 100millis at GlassFish startyp time
+     */
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

    private void exposeContext()
            throws EndpointRegistrationException {
        String context = getContextRoot();
        logger.log(Level.FINE, "Exposing rest resource context root: {0}", context);
        if ((context != null) || (!"".equals(context))) {
            Set<Class<?>> classes = getResourcesConfig();
            adapter = lazyJerseyInterface.exposeContext(classes, sc, habitat);
//            ((HttpHandler) adapter).setResourcesContextPath(context);

            logger.log(Level.INFO, "rest.rest_interface_initialized", context);
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

    private volatile HttpHandler adapter = null;
    private boolean isRegistered = false;
    private AdminEndpointDecider epd = null;
}
