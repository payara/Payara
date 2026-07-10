/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.opentracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;

/**
 * Per-request holder for the low-cardinality HTTP route and span name, carried in the OTel
 * {@link Context}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@code StandardWrapper} creates an instance and calls
 *       {@link #setServletMapping(String, String, String)} with the servlet context path,
 *       mapping pattern, and HTTP method, then folds it into the span context via
 *       {@link #storeInContext(Context)}.</li>
 *   <li>Framework layers (JAX-RS, JSF, JAX-WS) detect the instance via
 *       {@link #fromContext(Context)} and contribute their specific route or span name.</li>
 *   <li>At span-end, {@code OtelSupport.endAndRecord} reads {@link #route()} and
 *       {@link #spanName()} from the stashed context to set {@code http.route} and
 *       {@code span.updateName}.</li>
 * </ol>
 *
 * <h3>Resolve rules (last-writer-wins within each tier)</h3>
 * <pre>
 *   route()     = fullRoute                               if setFullRoute was called
 *               | contextPath + servletPath + route       if setRoute was called
 *               | contextPath + servletPath               otherwise
 *
 *   spanName()  = spanNameOverride                        if overrideSpanName was called
 *               | method + " " + route()                  otherwise
 * </pre>
 *
 * <p>Presence of this instance in {@link Context#current()} is also the detection signal for
 * {@code StandardWrapper} span ownership — framework layers that find it present know they should
 * enrich rather than create a new SERVER span.
 */
public final class OtelRouteState implements ImplicitContextKeyed {

    private static final ContextKey<OtelRouteState> KEY =
            ContextKey.named("fish.payara.otel.route-state");

    /**
     * Returns the {@link OtelRouteState} from the given context, or {@code null} if
     * {@code StandardWrapper} has not yet created a SERVER span for this request (e.g. OTel is
     * disabled, or the code is running outside the servlet layer).
     */
    public static OtelRouteState fromContext(Context context) {
        return context.get(KEY);
    }

    @Override
    public Context storeInContext(Context context) {
        return context.with(KEY, this);
    }

    // ---- state ----

    private String method;
    private String contextPath;
    private String servletPath;
    private String route;         // within-servlet suffix, e.g. /users/{id}
    private String fullRoute;     // full override, e.g. /ctx/api/users/{id}
    private String spanNameOverride;

    // ---- setters ----

    /**
     * Called by {@code StandardWrapper} / {@code OtelSupport} at span-start with the servlet
     * layer's knowledge: the HTTP method, context path, and the low-cardinality servlet
     * mapping pattern (e.g. {@code /api/*} rather than raw {@code getServletPath()}).
     */
    public void setServletMapping(String method, String contextPath, String servletPath) {
        this.method = method;
        this.contextPath = nullToEmpty(contextPath);
        this.servletPath = nullToEmpty(servletPath);
    }

    /**
     * Sets the route <em>within</em> the servlet, e.g. the {@code @Path} template suffix
     * {@code /users/{id}}. Combined with the servlet prefix to produce the final route.
     * Last-writer-wins.
     */
    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * Sets the complete {@code http.route}, overriding the servlet context/path prefix entirely,
     * e.g. {@code /ctx/api/users/{id}} as computed by JAX-RS from the matched URI templates.
     * Takes precedence over {@link #setRoute(String)}. Last-writer-wins.
     */
    public void setFullRoute(String route) {
        this.fullRoute = route;
    }

    /**
     * Overrides the span name completely, bypassing the {@code "METHOD route"} default.
     * Intended for JSF view-id names, {@code @WithSpan} values, or JAX-WS operation names.
     * Last-writer-wins.
     */
    public void overrideSpanName(String spanName) {
        this.spanNameOverride = spanName;
    }

    // ---- resolve ----

    /**
     * Resolves the final low-cardinality {@code http.route} value.
     *
     * @return the route string, never {@code null} (falls back to contextPath + servletPath)
     */
    public String route() {
        if (fullRoute != null) {
            return fullRoute;
        }
        String base = nullToEmpty(contextPath) + nullToEmpty(servletPath);
        if (route != null) {
            // Avoid double-slash when base ends with '/' and route starts with '/'
            if (base.endsWith("/") && route.startsWith("/")) {
                return base + route.substring(1);
            }
            return base + route;
        }
        return base;
    }

    /**
     * Resolves the final span name.
     *
     * @return the span name; {@code "METHOD route"} unless overridden
     */
    public String spanName() {
        if (spanNameOverride != null) {
            return spanNameOverride;
        }
        String r = route();
        String m = nullToEmpty(method);
        return r.isEmpty() ? m : m + " " + r;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
