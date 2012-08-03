/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import javax.ws.rs.core.MediaType;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.component.Habitat;

import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestConfigChangeListener;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider;
import org.glassfish.admin.rest.provider.ActionReportResultJsonProvider;
import org.glassfish.admin.rest.provider.ActionReportResultXmlProvider;
import org.glassfish.admin.rest.provider.BaseProvider;
import org.glassfish.admin.rest.resources.ReloadResource;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.admin.restconnector.ProxiedRestAdapter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.common.util.admin.RestSessionManager;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.media.json.JsonJaxbBinder;
import org.glassfish.jersey.media.multipart.MultiPartBinder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.CsrfProtectionFilter;
import org.glassfish.jersey.server.filter.UriConnegFilter;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import com.sun.logging.LogDomains;

/**
 * Adapter for REST interface
 * @author Rajeshwar Patil, Ludovic Champenois
 */
public abstract class RestAdapter extends HttpHandler implements ProxiedRestAdapter, PostConstruct {
    protected static final String COOKIE_REST_TOKEN = "gfresttoken";
    protected static final String COOKIE_GF_REST_UID = "gfrestuid";
    protected static final String HEADER_ACCEPT = "Accept";
    protected static final String HEADER_USER_AGENT = "User-Agent";
    protected static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";
    protected static final String HEADER_AUTHENTICATE = "WWW-Authenticate";

    protected final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(RestService.class);

    @Inject
    protected Habitat habitat;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    private CountDownLatch latch = new CountDownLatch(1);

    @Inject
    private ServerContext sc;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Inject
    private RestSessionManager sessionManager;

    @Inject @Optional
    private AdminAccessController adminAuthenticator;

    private static final Logger logger = LogDomains.getLogger(RestAdapter.class, LogDomains.ADMIN_LOGGER);
    private volatile HttpHandler adapter = null;
    private boolean isRegistered = false;
    private AdminEndpointDecider epd = null;

    protected RestAdapter() {
        setAllowEncodedSlash(true);
    }

    protected AbstractBinder getJsonBinder() {
        return new JsonJaxbBinder();
    }

    @Override
    public void postConstruct() {
        epd = new AdminEndpointDecider(config, logger);
        latch.countDown();
    }

    protected abstract String getContextRoot();
    protected abstract Set<Class<?>> getResourceClasses();

    @Override
    public HttpHandler getHttpService() {
        return this;
    }

    @Override
    public void service(Request req, Response res) {
        logger.log(Level.FINER, "Received resource request: {0}", req.getRequestURI());

        try {
            res.setCharacterEncoding(Constants.ENCODING);
            if (latch.await(20L, TimeUnit.SECONDS)) {
                if(serverEnvironment.isInstance()) {
                    if(!Method.GET.equals(req.getMethod())) {
                        reportError(req, res, HttpURLConnection.HTTP_FORBIDDEN,
                                localStrings.getLocalString("rest.resource.only.GET.on.instance",
                                "Only GET requests are allowed on an instance that is not DAS."));
                        return;
                    }
                }

                AdminAccessController.Access access = null;
                if (adminAuthenticator != null) {
                    final Subject subject = adminAuthenticator.loginAsAdmin(req);
                    req.setAttribute(Constants.REQ_ATTR_SUBJECT, subject);
                    access = adminAuthenticator.chooseAccess(subject, req.getRemoteHost());
                }

                if (access == null || access.isOK()) {
                    String context = getContextRoot();
                    logger.log(Level.FINE, "Exposing rest resource context root: {0}", context);
                    if ((context != null) && (!"".equals(context)) && (adapter == null)) {
                        adapter = exposeContext(getResourceClasses(), sc, habitat);
                        logger.log(Level.INFO, "rest.rest_interface_initialized", context);
                    }
                    //delegate to adapter managed by Jersey.
                    adapter.service(req, res);
                } else { // Access != FULL
                    String msg;
                    int status;
                    if(access == AdminAccessController.Access.NONE) {
                        status = HttpURLConnection.HTTP_UNAUTHORIZED;
                        msg = localStrings.getLocalString("rest.adapter.auth.userpassword",
                                "Invalid user name or password");
                        res.setHeader(HEADER_AUTHENTICATE, "BASIC");
                    } else {
                        assert access == AdminAccessController.Access.FORBIDDEN;
                        status = HttpURLConnection.HTTP_FORBIDDEN;
                        msg = localStrings.getLocalString("rest.adapter.auth.forbidden",
                                "Remote access not allowed. If you desire remote access, please turn on secure admin");
                    }
                    reportError(req, res, status, msg);
                }
            } else { // !latch.await(...)
                reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE,
                        localStrings.getLocalString("rest.adapter.server.wait",
                        "Server cannot process this command at this time, please wait"));
            }
        } catch (InterruptedException e) {
            reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE,
                    localStrings.getLocalString("rest.adapter.server.wait",
                    "Server cannot process this command at this time, please wait")); //service unavailable
        } catch (IOException e) {
            reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE,
                    localStrings.getLocalString("rest.adapter.server.ioexception",
                    "REST: IO Exception " + e.getLocalizedMessage())); //service unavailable
        } catch (LoginException e) {
            reportError(req, res, HttpURLConnection.HTTP_UNAUTHORIZED,
                    localStrings.getLocalString("rest.adapter.auth.error", "Error authenticating")); //authentication error
        } catch (Exception e) {
            String msg = localStrings.getLocalString("rest.adapter.server.exception",
                    "An error occurred while processing the request. Please see the server logs for details.");
            reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
            logger.log(Level.INFO, msg, e);
        }
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
            String userAgent = req.getHeader(HEADER_USER_AGENT);
            if (userAgent != null) {
                String accept = req.getHeader(HEADER_ACCEPT);
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

    public Map<String, MediaType> getMimeMappings() {
        return new HashMap<String, MediaType>() {{
            put("xml", MediaType.APPLICATION_XML_TYPE);
            put("json", MediaType.APPLICATION_JSON_TYPE);
            put("html", MediaType.TEXT_HTML_TYPE);
            put("js", new MediaType("text", "javascript"));
        }};
    }


    public Map<String, Boolean> getFeatures() {
        return new HashMap<String, Boolean>() {{
       //    put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
        }};
    }

    /**
     * dynamically load the class that contains all references to Jersey APIs
     * so that Jersey is not loaded when the RestAdapter is loaded at boot time
     * gain a few 100 millis at GlassFish startup time
     */
    public HttpHandler exposeContext(Set<Class<?>> classes, final ServerContext sc, final Habitat habitat) throws EndpointRegistrationException {

        HttpHandler httpHandler = null;
        final Reloader r = new Reloader();

        ResourceConfig rc = new ResourceConfig(classes);

        //rc.services = habitat.getDefault();

        UriConnegFilter.enableFor(rc, getMimeMappings(), null);

        rc.addClasses(CsrfProtectionFilter.class);


//        TODO - JERSEY2
//        RestConfig restConf = ResourceUtil.getRestConfig(habitat);
//        if (restConf != null) {
//            if (restConf.getLogOutput().equalsIgnoreCase("true")) { //enable output logging
//                rc.getContainerResponseFilters().add(LoggingFilter.class);
//            }
//            if (restConf.getLogInput().equalsIgnoreCase("true")) { //enable input logging
//                rc.getContainerRequestFilters().add(LoggingFilter.class);
//            }
//            if (restConf.getWadlGeneration().equalsIgnoreCase("false")) { //disable WADL
//                rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
//            }
//        }
//        else {
//                 rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
//        }
//
        rc.addSingletons(r);
        rc.addClasses(ReloadResource.class);

        /**
         * JRW JRW
         *
         */
        rc.addBinders(getJsonBinder(), new MultiPartBinder(), new AbstractBinder() {

            @Override
            protected void configure() {
                AbstractActiveDescriptor<Reloader> descriptor = BuilderHelper.createConstantDescriptor(r);
                descriptor.addContractType(Reloader.class);
                bind(descriptor);

                AbstractActiveDescriptor<ServerContext> scDescriptor = BuilderHelper.createConstantDescriptor(sc);
                scDescriptor.addContractType(ServerContext.class);
                bind(scDescriptor);

                AbstractActiveDescriptor<Habitat> hDescriptor = BuilderHelper.createConstantDescriptor(habitat);
                hDescriptor.addContractType(Habitat.class);
                bind(hDescriptor);

                RestSessionManager rsm = habitat.getService(RestSessionManager.class);
                AbstractActiveDescriptor<RestSessionManager> rmDescriptor =
                        BuilderHelper.createConstantDescriptor(rsm);
                bind(rmDescriptor);
            }
        });

        //Use common classloader. Jersey artifacts are not visible through
        //module classloader
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader apiClassLoader = sc.getCommonClassLoader();
            Thread.currentThread().setContextClassLoader(apiClassLoader);
            httpHandler = ContainerFactory.createContainer(HttpHandler.class, rc);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
        //add a rest config listener for possible reload of Jersey
        new RestConfigChangeListener(habitat, r, rc, sc);
        return httpHandler;
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
