/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.microprofile.metrics.rest;

import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.cdi.MetricUtils;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.writer.MetricsWriter;
import fish.payara.microprofile.metrics.writer.MetricsWriterImpl;
import fish.payara.microprofile.metrics.writer.OpenMetricsExporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.internal.api.Globals;

import static fish.payara.microprofile.Constants.EMPTY_STRING;
import static jakarta.servlet.http.HttpServletResponse.*;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MetricsResource extends HttpServlet {
    
    private static final String GLOBAL_TAG_MALFORMED_EXCEPTION = "Malformed list of Global Tags. Tag names "
            + "must match the following regex [a-zA-Z_][a-zA-Z0-9_]*."
            + " Global Tag values must not be empty."
            + " Global Tag values MUST escape equal signs `=` and commas `,`"
            + " with a backslash `\\` ";

    private static final Logger LOG = Logger.getLogger(MetricsResource.class.getName());
    private static final String APPLICATION_WILDCARD = "application/*";
    private static final Pattern PATTERN_Q_PART = Pattern.compile("\\s*q\\s*=\\s*(.+)");

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>OPTIONS</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        MetricsService metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);

        if (!metricsService.isEnabled()) {
            response.sendError(SC_FORBIDDEN, "MicroProfile Metrics Service is disabled");
            return;
        }
        metricsService.refresh();

        String scopeParameter = request.getParameter("scope") != null ? request.getParameter("scope") : null;
        String metricName = request.getParameter("name") != null ? request.getParameter("name") : null;
        String pathInfo = request.getPathInfo() != null ? request.getPathInfo().substring(1) : EMPTY_STRING;
        String[] pathInfos = pathInfo.split("/");
        boolean availableScope = true;

        if (!pathInfo.isEmpty() && pathInfos.length > 0) {
            response.sendError(SC_NOT_FOUND, "Not available paths to consume");
            return;
        }

        try {
            String contentType = getContentType(request, response);
            if (contentType != null) {
                response.setContentType(contentType);
                response.setCharacterEncoding(UTF_8.name());
                MetricsWriter outputWriter = getOutputWriter(request, response, metricsService, contentType);
                if (outputWriter != null) {
                    if (scopeParameter != null && !scopeParameter.isEmpty()) {
                        String scope;
                        try {
                            if (scopeParameter.equals(MetricRegistry.BASE_SCOPE)) {
                                scope = MetricRegistry.BASE_SCOPE;
                            } else if (scopeParameter.equals(MetricRegistry.VENDOR_SCOPE)) {
                                scope = MetricRegistry.VENDOR_SCOPE;
                            } else if (scopeParameter.equals(MetricRegistry.APPLICATION_SCOPE)) {
                                scope = MetricRegistry.APPLICATION_SCOPE;
                            } else {
                                scope = scopeParameter;
                            }
                        } catch (RuntimeException ex) {
                            throw new NoSuchRegistryException(scopeParameter);
                        }

                        for (String name : metricsService.getContextNames()) {
                            Optional<String> availableScopeOptional = metricsService.getContext(name)
                                    .getRegistries().keySet().stream().filter(k -> k.equals(scope)).findAny();
                            if (!availableScopeOptional.isPresent()) {
                                availableScope = false;
                            } else {
                                availableScope = true;
                            }
                        }

                        if (!availableScope) {
                            response.sendError(SC_NOT_FOUND, "Not available scope to consume");
                        }

                        if (availableScope && scope != null && metricName != null) {
                            outputWriter.write(scope, metricName);
                        } else if (availableScope) {
                            outputWriter.write(scope);
                        }
                    } else {
                        outputWriter.write();
                    }
                }
            }
        } catch (NoSuchRegistryException ex) {
            response.sendError(SC_NOT_FOUND, String.format("[%s] registry not found", scopeParameter));
        } catch (NoSuchMetricException ex) {
            response.sendError(SC_NOT_FOUND, String.format("[%s] metric not found", metricName));
        }
    }

    @SuppressWarnings("resource")
    private static MetricsWriter getOutputWriter(HttpServletRequest request,
            HttpServletResponse response, MetricsService service, String contentType) throws IOException {
        Writer writer = response.getWriter();
        String method = request.getMethod();
        if (GET.equalsIgnoreCase(method)) {
            if (TEXT_PLAIN.equals(contentType)) {
                return new MetricsWriterImpl(new OpenMetricsExporter(writer),
                    service.getContextNames(), service::getContext, MetricUtils.resolveGlobalTagsConfiguration());
            }
        }
        return null;
    }
    
    private static String getContentType(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String accept = request.getHeader(ACCEPT);
        if (accept == null) {
            accept = TEXT_PLAIN;
        }
        switch (method) {
        case GET:
            Optional<String> selectedFormat = parseMetricsAcceptHeader(accept);

            if (selectedFormat.isPresent()) {
                return selectedFormat.get();
            }

            response.sendError(SC_NOT_ACCEPTABLE, String.format("[%s] not acceptable", accept));
            return null;

        case OPTIONS:
        default:
            response.sendError(SC_METHOD_NOT_ALLOWED, String.format("HTTP method [%s] not allowed", method));
        }

        return null;
    }

    static Optional<String> parseMetricsAcceptHeader(String accept) {
        String[] acceptFormats = accept.split(",");
        double qTextFormat = 0;
        for (String format : acceptFormats) {
            if (format.contains(TEXT_PLAIN) || format.contains(MediaType.WILDCARD) || format.contains("text/*")) {
                qTextFormat = parseQValue(format);
            }
        }

        if (qTextFormat == 0) {
            return Optional.empty();
        }

        return Optional.of(MediaType.TEXT_PLAIN);
    }

    private static double parseQValue(final String format) {
        return Stream.of(format.split(";")).skip(1)
                .map(PATTERN_Q_PART::matcher)
                .filter(Matcher::find)
                .mapToDouble(m -> toDouble(m.group(1)))
                .findFirst()
                .orElse(1);
    }

    private static double toDouble(final String text) {
        try {
            if (text.startsWith(".")) {
                return Double.parseDouble("0" + text);
            }
            return Double.parseDouble(text);
        } catch (final NumberFormatException e) {
            LOG.warning(() -> "Invalid q value in " + ACCEPT + " header: " + text);
            return 0f;
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>OPTIONS</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String method = request.getMethod();
        response.sendError(SC_METHOD_NOT_ALLOWED, String.format("HTTP method [%s] not allowed", method));
    }
}
