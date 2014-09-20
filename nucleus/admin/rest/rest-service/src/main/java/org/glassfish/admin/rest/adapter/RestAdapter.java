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

package org.glassfish.admin.rest.adapter;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider;
import org.glassfish.admin.rest.provider.ActionReportResultJsonProvider;
import org.glassfish.admin.rest.provider.ActionReportResultXmlProvider;
import org.glassfish.admin.rest.provider.BaseProvider;
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
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.RemoteAdminAccessException;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.annotations.Optional;

/**
 * Adapter for REST interface
 * @author Rajeshwar Patil, Ludovic Champenois
 * @author sanjeeb.sahoo@oracle.com
 */
public abstract class RestAdapter extends HttpHandler implements ProxiedRestAdapter, PostConstruct {
    protected static final String COOKIE_REST_TOKEN = "gfresttoken";
    protected static final String COOKIE_GF_REST_UID = "gfrestuid";
    protected static final String HEADER_ACCEPT = "Accept";
    protected static final String HEADER_USER_AGENT = "User-Agent";
    protected static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";
    protected static final String HEADER_AUTHENTICATE = "WWW-Authenticate";

    protected final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(RestService.class);

    private RestResourceProvider restResourceProvider;

    @Inject
    protected ServiceLocator habitat;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    private CountDownLatch latch = new CountDownLatch(1);

    @Inject
    protected ServerContext sc;

    @Inject
    protected ServerEnvironment serverEnvironment;

    @Inject
    private RestSessionManager sessionManager;

    @Inject @Optional
    protected AdminAccessController adminAuthenticator;

    private volatile JerseyContainer adapter = null;

    protected RestAdapter() {
        setAllowEncodedSlash(true);
    }

    @Override
    public void postConstruct() {
        latch.countDown();
    }

    protected String getContextRoot() {
        return getRestResourceProvider().getContextRoot();
    }

    @Override
    public HttpHandler getHttpService() {
        return this;
    }

    @Override
    public void service(Request req, Response res) {
        RestLogging.restLogger.log(Level.FINER, "Received resource request: {0}", req.getRequestURI());

        try {
            res.setCharacterEncoding(Constants.ENCODING);
            if (latch.await(20L, TimeUnit.SECONDS)) {
                if(serverEnvironment.isInstance()) {
                    if(!Method.GET.equals(req.getMethod()) && !getRestResourceProvider().enableModifAccessToInstances()) {
                        reportError(req, res, HttpURLConnection.HTTP_FORBIDDEN,
                                localStrings.getLocalString("rest.resource.only.GET.on.instance",
                                "Only GET requests are allowed on an instance that is not DAS."));
                        return;
                    }
                }

                if (adminAuthenticator != null) {
                    final Subject subject = adminAuthenticator.loginAsAdmin(req);
                    req.setAttribute(Constants.REQ_ATTR_SUBJECT, subject);
                }

                String context = getContextRoot();
                if ((context != null) && (!"".equals(context)) && (adapter == null)) {
                    RestLogging.restLogger.log(Level.FINE, "Exposing rest resource context root: {0}", context);
                    adapter = exposeContext();
                    RestLogging.restLogger.log(Level.INFO, RestLogging.REST_INTERFACE_INITIALIZED, context);
                }
                //delegate to adapter managed by Jersey.
                adapter.service(req, res);

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
        } catch (RemoteAdminAccessException e) {
            reportError(req, res, HttpURLConnection.HTTP_FORBIDDEN,
                    localStrings.getLocalString("rest.adapter.auth.forbidden",
                    "Remote access not allowed. If you desire remote access, please turn on secure admin"));
        } catch (LoginException e) {
            int status = HttpURLConnection.HTTP_UNAUTHORIZED;
            String msg = localStrings.getLocalString("rest.adapter.auth.userpassword",
                                                     "Invalid user name or password");
            res.setHeader(HEADER_AUTHENTICATE, "BASIC");
            reportError(req, res, status, msg);
        } catch (Exception e) {
            // TODO: This string is duplicated.  Can we pull this text out of the logging bundle?
            String msg = localStrings.getLocalString("rest.adapter.server.exception",
                    "An error occurred while processing the request. Please see the server logs for details.");
            RestLogging.restLogger.log(Level.INFO, RestLogging.SERVER_ERROR, e);
            reportError(req, res, HttpURLConnection.HTTP_UNAVAILABLE, msg); //service unavailable
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

    protected RestResourceProvider getRestResourceProvider() {
        return restResourceProvider;
    }

    protected void setRestResourceProvider(RestResourceProvider rrp) {
        this.restResourceProvider = rrp;
    }

    public static class SubjectReferenceFactory implements Factory<Ref<Subject>> {

        Ref<Request> requestReference;

        @Inject
        public SubjectReferenceFactory(Provider<Ref<Request>> requestReference) {
            this.requestReference = requestReference.get();
        }

        @Override
        public Ref<Subject> provide() {
            Subject subject = (Subject) requestReference.get().getAttribute(Constants.REQ_ATTR_SUBJECT);
            return Refs.of(subject);
        }

        @Override
        public void dispose(Ref<Subject> t) {
        }
    }

    protected Set<? extends Binder> getAdditionalBinders(){
        return Collections.singleton(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(SubjectReferenceFactory.class).to(new TypeLiteral<Ref<Subject>>() {
                }).in(PerLookup.class);
                bindFactory(ReferencingFactory.<Subject>referenceFactory()).to(new TypeLiteral<Ref<Subject>>() {
                }).in(RequestScoped.class);
            }
        });
    }

    /**
     * dynamically load the class that contains all references to Jersey APIs
     * so that Jersey is not loaded when the RestAdapter is loaded at boot time
     * gain a few 100 millis at GlassFish startup time
     */
    protected JerseyContainer exposeContext() throws EndpointRegistrationException {
        Set<Class<?>> classes = getRestResourceProvider().getResourceClasses(habitat);
        // Use common classloader. Jersey artifacts are not visible through
        // module classloader. Actually there is a more important reason to use CommonClassLoader.
        // jax-rs API called RuntimeDelegate makes stupid class loading assumption and throws LinkageError
        // when it finds an implementation of RuntimeDelegate that's part of WLS system class loader.
        // So, we force it to restrict its search space using common class loader.
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader apiClassLoader = sc.getCommonClassLoader();
            Thread.currentThread().setContextClassLoader(apiClassLoader);
            ResourceConfig rc = getRestResourceProvider().getResourceConfig(classes, sc, habitat, getAdditionalBinders());
            return getJerseyContainer(rc);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    protected JerseyContainer getJerseyContainer(ResourceConfig rc) {
        final HttpHandler httpHandler = ContainerFactory.createContainer(HttpHandler.class, rc);
        return new JerseyContainer() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpHandler.service(request, response);
            }
        };
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
