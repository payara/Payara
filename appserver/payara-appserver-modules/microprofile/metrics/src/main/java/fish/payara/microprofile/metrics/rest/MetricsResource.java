/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.writer.JsonExporter;
import fish.payara.microprofile.metrics.writer.JsonExporter.Mode;
import fish.payara.microprofile.metrics.writer.MetricsWriter;
import fish.payara.microprofile.metrics.writer.MetricsWriterImpl;
import fish.payara.microprofile.metrics.writer.OpenMetricsExporter;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static fish.payara.microprofile.Constants.EMPTY_STRING;
import static java.nio.charset.StandardCharsets.UTF_8;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import jakarta.ws.rs.core.MediaType;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.internal.api.Globals;

public class MetricsResource extends HttpServlet {

    private static final String GLOBAL_TAGS_VARIABLE = "mp.metrics.tags";
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

        String pathInfo = request.getPathInfo() != null ? request.getPathInfo().substring(1) : EMPTY_STRING;
        String[] pathInfos = pathInfo.split("/");
        String registryName = pathInfos.length > 0 ? pathInfos[0] : null;
        String metricName = pathInfos.length > 1 ? pathInfos[1] : null;

        try {
            String contentType = getContentType(request, response);
            if (contentType != null) {
                response.setContentType(contentType);
                response.setCharacterEncoding(UTF_8.name());
                MetricsWriter outputWriter = getOutputWriter(request, response, metricsService, contentType);
                if (outputWriter != null) {
                    if (registryName != null && !registryName.isEmpty()) {
                        Type scope;
                        try {
                            scope = Type.valueOf(registryName.toUpperCase());
                        } catch (RuntimeException ex) {
                            throw new NoSuchRegistryException(registryName);
                        }
                        if (metricName != null && !metricName.isEmpty()) {
                            outputWriter.write(scope, metricName);
                        } else {
                            outputWriter.write(scope);
                        }
                    } else {
                        outputWriter.write();
                    }
                }
            }
        } catch (NoSuchRegistryException ex) {
            response.sendError(SC_NOT_FOUND, String.format("[%s] registry not found", registryName));
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
            if (APPLICATION_JSON.equals(contentType)) {
                return new MetricsWriterImpl(new JsonExporter(writer, Mode.GET, true),
                    service.getContextNames(), service::getContext, getGlobalTags());
            }
            if (TEXT_PLAIN.equals(contentType)) {
                return new MetricsWriterImpl(new OpenMetricsExporter(writer),
                    service.getContextNames(), service::getContext, getGlobalTags());
            }
        }
        if (OPTIONS.equalsIgnoreCase(method)) {
            if (APPLICATION_JSON.equals(contentType)) {
                return new MetricsWriterImpl(new JsonExporter(writer, Mode.OPTIONS, true),
                        service.getContextNames(), service::getContext, getGlobalTags());
            }
        }
        return null;
    }

    private static Tag[] getGlobalTags() {
        Config config = ConfigProvider.getConfig();
        Optional<String> globalTagsProperty = config.getOptionalValue(GLOBAL_TAGS_VARIABLE, String.class);
        if (!globalTagsProperty.isPresent()) {
            return new Tag[0];
        }
        String globalTags = globalTagsProperty.get();
        if (globalTags == null || globalTags.length() == 0) {
            return new Tag[0];
        }
        String[] kvPairs = globalTags.split("(?<!\\\\),");
        Tag[] tags = new Tag[kvPairs.length];
        for (int i = 0; i < kvPairs.length; i++) {
            String kvString = kvPairs[i];
            if (kvString.length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }
            String[] keyValueSplit = kvString.split("(?<!\\\\)=");
            if (keyValueSplit.length != 2 || keyValueSplit[0].length() == 0 || keyValueSplit[1].length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }
            String key = keyValueSplit[0];
            String value = keyValueSplit[1];
            value = value.replace("\\,", ",");
            value = value.replace("\\=", "=");
            tags[i] = new Tag(key, value);
        }
        return tags;
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
            if (accept.contains(APPLICATION_JSON) || accept.contains(APPLICATION_WILDCARD)) {
                return APPLICATION_JSON;
            }
            response.sendError(SC_NOT_ACCEPTABLE, String.format("[%s] not acceptable", accept));
            return null;
        default:
            response.sendError(SC_METHOD_NOT_ALLOWED, String.format("HTTP method [%s] not allowed", method));
        }

        return null;
    }

    static Optional<String> parseMetricsAcceptHeader(String accept) {
        String[] acceptFormats = accept.split(",");
        double qJsonValue = 0;
        double qTextFormat = 0;
        for (String format : acceptFormats) {
            if (format.contains(TEXT_PLAIN) || format.contains(MediaType.WILDCARD) || format.contains("text/*")) {
                qTextFormat = parseQValue(format);
            } else if (format.contains(APPLICATION_JSON) || format.contains(APPLICATION_WILDCARD)) {
                qJsonValue = parseQValue(format);
            } // else { no other formats supported by Payara, ignored }
        }

        // if neither JSON or plain text are supported
        if (qJsonValue == 0 && qTextFormat == 0) {
            return Optional.empty();
        }
        if (qJsonValue > qTextFormat) {
            return Optional.of(MediaType.APPLICATION_JSON);
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
        processRequest(request, response);
    }

}
