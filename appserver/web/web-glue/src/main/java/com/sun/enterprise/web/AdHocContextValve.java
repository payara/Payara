/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Wrapper;
import org.glassfish.web.LogFacade;
import org.glassfish.web.valve.GlassFishValve;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of StandardContextValve which is added as the base valve
 * to a web module's ad-hoc pipeline.
 *
 * A web module's ad-hoc pipeline is invoked for any of the web module's
 * ad-hoc paths.
 *
 * The AdHocContextValve is responsible for invoking the ad-hoc servlet
 * associated with the ad-hoc path.
 *
 * @author Jan Luehe
 */
public class AdHocContextValve implements GlassFishValve {

    private static final Logger logger = LogFacade.getLogger();

    private static final ResourceBundle rb = logger.getResourceBundle();

    private static final String VALVE_INFO =
        "com.sun.enterprise.web.AdHocContextValve";

    // The web module with which this valve is associated
    private WebModule context;


    /**
     * Constructor.
     */
    public AdHocContextValve(WebModule context) {
        this.context = context;
    }


    /**
     * Returns descriptive information about this valve.
     */
    public String getInfo() {
        return VALVE_INFO;
    }


    /**
     * Processes the given request by passing it to the ad-hoc servlet
     * associated with the request path (which has been determined, by the
     * associated web module, to be an ad-hoc path).
     *
     * @param request The request to process
     * @param response The response to return
     */
    public int invoke(Request request, Response response)
            throws IOException, ServletException {

        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        HttpServletResponse hres = (HttpServletResponse) response.getResponse();

        String adHocServletName =
            context.getAdHocServletName(hreq.getServletPath());

        Wrapper adHocWrapper = (Wrapper) context.findChild(adHocServletName);
        if (adHocWrapper != null) {
            Servlet adHocServlet = null;
            try {
                adHocServlet = adHocWrapper.allocate();
                adHocServlet.service(hreq, hres);
            } catch (Throwable t) {
                hres.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                String msg = rb.getString(LogFacade.ADHOC_SERVLET_SERVICE_ERROR);
                msg = MessageFormat.format(
                            msg,
                            new Object[] { hreq.getServletPath() });
                response.setDetailMessage(msg);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, msg, t);
                }
                return END_PIPELINE;
            } finally {
                if (adHocServlet != null) {
                    adHocWrapper.deallocate(adHocServlet);
                }
            }
        } else {
            hres.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = rb.getString(LogFacade.NO_ADHOC_SERVLET);
            msg = MessageFormat.format(
                            msg,
                            new Object[] { hreq.getServletPath() });
            response.setDetailMessage(msg);
            return END_PIPELINE;
        }

        return END_PIPELINE;
    }


    public void postInvoke(Request request, Response response)
            throws IOException, ServletException {
        // Do nothing
    }

}

