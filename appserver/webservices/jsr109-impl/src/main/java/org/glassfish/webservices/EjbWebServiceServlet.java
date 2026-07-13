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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package org.glassfish.webservices;

import static com.sun.enterprise.deployment.WebServiceEndpoint.PUBLISHING_SUBCONTEXT;
import static java.util.logging.Level.WARNING;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static jakarta.xml.ws.http.HTTPBinding.HTTP_BINDING;
import static org.glassfish.webservices.LogUtils.AUTH_FAILED;
import static org.glassfish.webservices.LogUtils.EXCEPTION_THROWN;
import static org.glassfish.webservices.LogUtils.INVALID_REQUEST_SCHEME;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.glassfish.api.logging.LogHelper;
import org.glassfish.ejb.api.EjbEndpointFacade;
import org.glassfish.ejb.spi.WSEjbEndpointRegistry;
import org.glassfish.internal.api.Globals;
import org.glassfish.webservices.monitoring.Endpoint;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import org.glassfish.webservices.monitoring.WebServiceTesterServlet;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebServiceEndpoint;

import fish.payara.opentracing.OpenTelemetryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;

/**
 * Servlet responsible for invoking EJB webservice endpoint.
 *
 * <p>EJB web-service endpoints are deployed as a bare Grizzly {@code ServletHandler}
 * ({@link EjbWSAdapter}), bypassing {@code StandardWrapper} and its OTel instrumentation.
 * Telemetry is handled via the {@link EjbWsTelemetry} callback interface, which decouples
 * span lifecycle management from the dispatch logic. The active implementation is chosen
 * once at construction time; when OTel is unavailable a no-op is used.
 *
 * @author Qingqing Ouyang
 * @author Kenneth Saks
 * @author Jan Luehe
 */
public class EjbWebServiceServlet extends HttpServlet {

    private static final long serialVersionUID = -4415390589000169371L;
    private static final Logger logger = LogUtils.getLogger();

    // -------------------------------------------------------------------------
    // Telemetry callback interface
    // -------------------------------------------------------------------------

    /**
     * Callback interface that separates OTel span lifecycle from dispatch logic.
     *
     * <p>EJB JAX-WS span creation is deferred: {@link #beforeDispatch} snapshots the
     * propagation carrier and calls {@link OpenTelemetryService#collectDeferredContext},
     * then {@link EjbEndpointFacadeImpl#startInvocation()} calls
     * {@link OpenTelemetryService#applyDeferredContext()} once the EJB invocation context
     * is on the stack and the correct per-app tracer is available.
     *
     * <p>Known limitation: authentication failures ({@link #authFailed}) never reach the
     * message dispatcher, so {@code applyDeferredContext} is never called and no SERVER
     * span is produced for 401 responses. The deferred thread-local is cleaned up via
     * {@code endDeferredSpan} regardless.
     */
    interface EjbWsTelemetry {
        /** Called before dispatch; snapshots headers and registers deferred span context. */
        void beforeDispatch(HttpServletRequest request, String method, String uri);

        /** Called when authentication fails; response already has 401 written. */
        void authFailed(HttpServletResponse response);

        /** Called after the message dispatcher returns normally. */
        void afterDispatch(HttpServletResponse response);

        /** Called when an exception escapes the message dispatcher. */
        void dispatchFailed(HttpServletResponse response, Throwable t);
    }

    private static final EjbWsTelemetry NOOP = new EjbWsTelemetry() {
        @Override public void beforeDispatch(HttpServletRequest r, String m, String u) {}
        @Override public void authFailed(HttpServletResponse r) {}
        @Override public void afterDispatch(HttpServletResponse r) {}
        @Override public void dispatchFailed(HttpServletResponse r, Throwable t) {}
    };

    /**
     * Active OTel implementation of {@link EjbWsTelemetry}.
     *
     * <p>Span attributes follow HTTP server semantic conventions. Span name is
     * {@code "METHOD /requestUri"} — the same low-cardinality form used by
     * {@code OtelSupport} for servlet endpoints.
     */
    private static final class OtelEjbWsTelemetry implements EjbWsTelemetry {

        private final OpenTelemetryService otelService;

        OtelEjbWsTelemetry(OpenTelemetryService otelService) {
            this.otelService = otelService;
        }

        @Override
        public void beforeDispatch(HttpServletRequest request, String method, String uri) {
            // Snapshot headers into a plain map; lowercase keys to match W3C propagator
            // expectations (e.g. "traceparent") — the Servlet spec does not mandate lowercase.
            HashMap<String, String> carrier = new HashMap<>();
            Collections.list(request.getHeaderNames())
                    .forEach(h -> carrier.put(h.toLowerCase(), request.getHeader(h)));

            otelService.collectDeferredContext(
                    carrier,
                    method + " " + uri,
                    SpanKind.SERVER,
                    Attributes.builder()
                            .put(HttpAttributes.HTTP_REQUEST_METHOD, method)
                            .put(UrlAttributes.URL_SCHEME, request.getScheme())
                            .put(UrlAttributes.URL_PATH, uri)
                            .put(ServerAttributes.SERVER_ADDRESS, request.getServerName())
                            .put(ServerAttributes.SERVER_PORT, (long) request.getServerPort())
                            .build());
        }

        @Override
        public void authFailed(HttpServletResponse response) {
            // applyDeferredContext() was never called — spanHelper is null.
            // endDeferredSpan() is a no-op but cleans up the thread-local.
            otelService.endDeferredSpan(null);
        }

        @Override
        public void afterDispatch(HttpServletResponse response) {
            Span current = Span.current();
            if (current.isRecording()) {
                int status = response.getStatus();
                current.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, (long) status);
                // SOAP faults are marked ERROR by JaxWsContainerRequestTelemetryTracingFilter.
                // This covers non-SOAP error responses (e.g. 503 from the container).
                if (status >= 400) {
                    current.setStatus(StatusCode.ERROR);
                }
            }
            otelService.endDeferredSpan(null);
        }

        @Override
        public void dispatchFailed(HttpServletResponse response, Throwable t) {
            Span current = Span.current();
            if (current.isRecording()) {
                current.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                        (long) response.getStatus());
                current.recordException(t);
                current.setStatus(StatusCode.ERROR);
            }
            otelService.endDeferredSpan(t);
        }
    }

    private SecurityService securityService;
    private OpenTelemetryService openTelemetryService;

    public EjbWebServiceServlet() {
        securityService = Globals.get(SecurityService.class);
        openTelemetryService = Globals.get(OpenTelemetryService.class);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String requestUriRaw = request.getRequestURI();
        String requestUri = requestUriRaw.charAt(0) == '/' ? requestUriRaw.substring(1) : requestUriRaw;
        String query = request.getQueryString();

        WebServiceEjbEndpointRegistry wsejbEndpointRegistry = (WebServiceEjbEndpointRegistry) Globals.get(WSEjbEndpointRegistry.class);
        EjbRuntimeEndpointInfo ejbEndpoint = wsejbEndpointRegistry.getEjbWebServiceEndpoint(requestUri, request.getMethod(), query);

        if (requestUri.contains(PUBLISHING_SUBCONTEXT) && ejbEndpoint == null) {
            requestUri = requestUri.substring(0, requestUri.indexOf(PUBLISHING_SUBCONTEXT) - 1);
            ejbEndpoint = wsejbEndpointRegistry.getEjbWebServiceEndpoint(requestUri, request.getMethod(), query);
        }

        if (ejbEndpoint != null) {

            /*
             * We can actually assert that ejbEndpoint is != null, because this EjbWebServiceServlet would not
             * have been invoked otherwise
             */
            String scheme = request.getScheme();
            WebServiceEndpoint endPoint = ejbEndpoint.getEndpoint();

            if ("http".equals(scheme) && endPoint.isSecure()) {

                // Redirect to correct protocol scheme if needed

                logger.log(WARNING, INVALID_REQUEST_SCHEME, new Object[] { endPoint.getEndpointName(), "https", scheme });

                URL url = endPoint.composeEndpointAddress(new WsUtil().getWebServerInfoForDAS().getWebServerRootURL(true));
                StringBuilder sb = new StringBuilder(url.toExternalForm());
                if (query != null && query.trim().length() > 0) {
                    sb.append("?");
                    sb.append(query);
                }
                response.sendRedirect(response.encodeRedirectURL(sb.toString()));
            } else {
                boolean dispatch = true;

                // Check if it is a tester servlet invocation

                if ("Tester".equalsIgnoreCase(query) && (!(HTTP_BINDING.equals(endPoint.getProtocolBinding())))) {
                    Endpoint endpoint = WebServiceEngineImpl.getInstance().getEndpoint(request.getRequestURI());
                    if ((endpoint.getDescriptor().isSecure()) || (endpoint.getDescriptor().getMessageSecurityBinding() != null)) {
                        String message = endpoint.getDescriptor().getWebService().getName()
                                + "is a secured web service; Tester feature is not supported for secured services";
                        (new WsUtil()).writeInvalidMethodType(response, message);
                        return;
                    }

                    if (Boolean.parseBoolean(endpoint.getDescriptor().getDebugging())) {
                        dispatch = false;
                        WebServiceTesterServlet.invoke(request, response, endpoint.getDescriptor());
                    }
                }

                if ("wsdl".equalsIgnoreCase(query) && (!(HTTP_BINDING.equals(endPoint.getProtocolBinding())))) {
                    if (endPoint.getWsdlExposed() != null && !Boolean.parseBoolean(endPoint.getWsdlExposed())) {
                        response.sendError(SC_NOT_FOUND);
                    }
                }

                if (dispatch) {
                    String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "POST";
                    EjbWsTelemetry telemetry = openTelemetryService.isEnabled()
                            ? new OtelEjbWsTelemetry(openTelemetryService)
                            : NOOP;
                    telemetry.beforeDispatch(request, method, request.getRequestURI());
                    dispatchToEjbEndpoint(request, response, ejbEndpoint, telemetry);
                }
            }
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    private void dispatchToEjbEndpoint(HttpServletRequest request, HttpServletResponse response,
            EjbRuntimeEndpointInfo ejbEndpoint, EjbWsTelemetry telemetry) {
        EjbEndpointFacade container = ejbEndpoint.getContainer();
        ClassLoader savedClassLoader = null;

        boolean authenticated = false;
        try {
            // Set context class loader to application class loader
            savedClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(container.getEndpointClassLoader());

            // Compute realmName
            String realmName = null;
            Application application = ejbEndpoint.getEndpoint().getBundleDescriptor().getApplication();

            if (application != null) {
                realmName = application.getRealm();
            }

            if (realmName == null) {
                realmName = ejbEndpoint.getEndpoint().getRealm();
            }

            if (realmName == null) {
                // Use the same logic as BasicAuthenticator
                realmName = request.getServerName() + ":" + request.getServerPort();
            }

            try {
                if (securityService != null) {
                    WebServiceContextImpl context = (WebServiceContextImpl) ejbEndpoint.getWebServiceContext();
                    authenticated = securityService.doSecurity(request, ejbEndpoint, realmName, context);
                }

            } catch (Exception e) {
                LogHelper.log(logger, WARNING, AUTH_FAILED, e, ejbEndpoint.getEndpoint().getEndpointName());
            }

            if (!authenticated) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
                response.sendError(SC_UNAUTHORIZED);
                telemetry.authFailed(response);
                return;
            }

            // Depending on the jaxrpc or jax-ws version, this will return the
            // right dispatcher.
            ejbEndpoint.getMessageDispatcher().invoke(request, response, getServletContext(), ejbEndpoint);
            telemetry.afterDispatch(response);

        } catch (Throwable t) {
            telemetry.dispatchFailed(response, t);
            logger.log(WARNING, EXCEPTION_THROWN, t);
        } finally {
            // Remove any security context from the thread local before returning
            if (securityService != null) {
                securityService.resetSecurityContext();
                securityService.resetPolicyContext();
            }

            // Restore context class loader
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }
}
