/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.metrics.Constants;
import fish.payara.microprofile.metrics.MetricsHelper;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.writer.JsonMetadataWriter;
import fish.payara.microprofile.metrics.writer.JsonMetricWriter;
import fish.payara.microprofile.metrics.writer.OutputWriter;
import fish.payara.microprofile.metrics.writer.PrometheusMetricWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.MetricRegistry;

@WebServlet(name = "MetricsResource", urlPatterns = {"/metrics/*"})
public class MetricsResource extends HttpServlet {
    
    private static final Logger LOGGER = Logger.getLogger(MetricsResource.class.getName());
    
    @Inject
    private MetricsHelper helper;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        if (!MetricsHelper.isMetricEnabled()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "MP Metrics is disabled");
            return;
        }
        
        String pathInfo = request.getPathInfo() != null ? request.getPathInfo().substring(1) : "";
        String[] pathInfos = pathInfo.split("/");
        String registryName = pathInfos.length > 0 ? pathInfos[0] : null;
        String matricName = pathInfos.length > 1 ? pathInfos[1] : null;
        if (registryName != null
                && !registryName.isEmpty()
                && MetricRegistry.Type.valueOf(registryName.toUpperCase()) == null) {
            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    String.format("[%s] registry not found", registryName));
        }

        OutputWriter outputWriter = getOutputWriter(request, response);

        if (outputWriter != null) {
            if (outputWriter instanceof JsonMetricWriter) {
                response.setContentType(Constants.JSON_CONTENT_TYPE);
            } else if (outputWriter instanceof JsonMetadataWriter) {
                response.setContentType(Constants.JSON_CONTENT_TYPE);
            } else {
                response.setContentType(Constants.TEXT_CONTENT_TYPE);
            }

            if (registryName != null && !registryName.isEmpty()
                    && matricName != null && !matricName.isEmpty()) {
                try {
                    outputWriter.write(registryName, matricName);
                } catch (NoSuchRegistryException ex) {
                    LOGGER.log(Level.SEVERE, registryName, ex);
                } catch (NoSuchMetricException ex) {
                    LOGGER.log(Level.SEVERE, matricName, ex);
                }
            } else if (registryName != null
                    && !registryName.isEmpty()) {
                try {
                    outputWriter.write(registryName);
                } catch (NoSuchRegistryException ex) {
                    LOGGER.log(Level.SEVERE, registryName, ex);
                }
            } else {
                outputWriter.write();
            }

        }
    }

    private OutputWriter getOutputWriter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String accept = request.getHeader(Constants.ACCEPT_HEADER);
        Writer writer = response.getWriter();

        if (accept == null) {
            accept = Constants.ACCEPT_HEADER_TEXT;
        }

        if (Constants.METHOD_GET.equals(method)) {
            if (accept.contains(Constants.ACCEPT_HEADER_TEXT)) {
                return new PrometheusMetricWriter(writer, helper);
            } else if (accept.contains(Constants.ACCEPT_HEADER_JSON)) {
                return new JsonMetricWriter(writer, helper);
            } else {
                return new PrometheusMetricWriter(writer, helper);
            }
        } else if (Constants.METHOD_OPTIONS.equals(method)) {
            if (accept.contains(Constants.ACCEPT_HEADER_JSON)) {
                return new JsonMetadataWriter(writer, helper);
            } else {
                response.sendError(
                        HttpServletResponse.SC_NOT_ACCEPTABLE,
                        String.format("[%s] not acceptable", accept));
            }
        } else {
            response.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    String.format("HTTP method [%s] not allowed", method));
        }
        return null;
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
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Metrics Resource";
    }// </editor-fold>

}
