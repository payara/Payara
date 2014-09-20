/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.core;


import org.apache.catalina.*;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.web.valve.GlassFishValve;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.glassfish.grizzly.http.util.DataChunk;
// END GlassFish 1343

/**
 * Valve that implements the default basic behavior for the
 * <code>StandardContext</code> container implementation.
 * <p>
 * <b>USAGE CONSTRAINT</b>:  This implementation is likely to be useful only
 * when processing HTTP requests.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.19 $ $Date: 2007/05/05 05:31:54 $
 */

final class StandardContextValve
    extends ValveBase {


    // ----------------------------------------------------- Instance Variables


    /**
     * The descriptive information related to this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardContextValve/1.0";


    private StandardContext context = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {

        return (info);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Cast to a StandardContext right away, as it will be needed later.
     * 
     * @see org.apache.catalina.Contained#setContainer(org.apache.catalina.Container)
     */
    public void setContainer(Container container) {
        super.setContainer(container);
        if (container instanceof StandardContext) {
            context = (StandardContext) container;
        }
    }


    /**
     * Select the appropriate child Wrapper to process this request,
     * based on the specified request URI.  If no matching Wrapper can
     * be found, return an appropriate HTTP error.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     * @param valveContext Valve context used to forward to the next Valve
     *
     * @exception IOException if an input/output error occurred
     * @exception ServletException if a servlet error occurred
     */
    @Override
    public int invoke(Request request, Response response)
        throws IOException, ServletException {

        Wrapper wrapper = preInvoke(request, response);
        if (wrapper == null) {
            return END_PIPELINE;
        }

        /* GlassFish 1343
        wrapper.getPipeline().invoke(request, response);
        */
        // START GlassFish 1343
        if (wrapper.getPipeline().hasNonBasicValves() ||
                wrapper.hasCustomPipeline()) {
            wrapper.getPipeline().invoke(request, response);
        } else {
            GlassFishValve basic = wrapper.getPipeline().getBasic();
            if (basic != null) {
                basic.invoke(request, response);
                basic.postInvoke(request, response);
            }
        }
        // END GlassFish 1343

        return END_PIPELINE;
    } 


    /**
     * Tomcat style invocation.
     */
    @Override
    public void invoke(org.apache.catalina.connector.Request request,
                       org.apache.catalina.connector.Response response)
            throws IOException, ServletException {

        Wrapper wrapper = preInvoke(request, response);
        if (wrapper == null) {
            return;
        }

        /* GlassFish 1343
        wrapper.getPipeline().invoke(request, response);
        */
        // START GlassFish 1343
        if (wrapper.getPipeline().hasNonBasicValves() ||
                wrapper.hasCustomPipeline()) {
            wrapper.getPipeline().invoke(request, response);
        } else {
            GlassFishValve basic = wrapper.getPipeline().getBasic();
            if (basic != null) {
                basic.invoke(request, response);
                basic.postInvoke(request, response);
            }
        }
        // END GlassFish 1343

        postInvoke(request, response);
    }


    @Override
    public void postInvoke(Request request, Response response)
            throws IOException, ServletException {
    }


    /**
     * Report a "not found" error for the specified resource.  FIXME:  We
     * should really be using the error reporting settings for this web
     * application, but currently that code runs at the wrapper level rather
     * than the context level.
     *
     * @param response The response we are creating
     */
    private void notFound(HttpServletResponse response) {

        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IllegalStateException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        }

    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     *
    private void log(String message) {
        org.apache.catalina.Logger logger = null;
        String containerName = null;
        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }
        if (logger != null) {
            logger.log("StandardContextValve[" + container.getName() + "]: " +
                       message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.info("StandardContextValve[" + containerName + "]: " +
                         message);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     *
    private void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        String containerName = null;
        if (container != null) {
            logger = container.getLogger();
            containerName = container.getName();
        }
        if (logger != null) {
            logger.log("StandardContextValve[" + container.getName() + "]: " +
                message, t, org.apache.catalina.Logger.WARNING);
        } else {
            log.log(Level.WARNING, "StandardContextValve[" + containerName +
                "]: " + message, t);
        }
    }
    */


    private Wrapper preInvoke(Request request, Response response) {

        // Disallow any direct access to resources under WEB-INF or META-INF
        HttpRequest hreq = (HttpRequest) request;
        // START CR 6415120
        if (request.getCheckRestrictedResources()) {
        // END CR 6415120
            DataChunk requestPathDC = hreq.getRequestPathMB();
            if ((requestPathDC.startsWithIgnoreCase("/META-INF/", 0))
                    || (requestPathDC.equalsIgnoreCase("/META-INF"))
                    || (requestPathDC.startsWithIgnoreCase("/WEB-INF/", 0))
                    || (requestPathDC.equalsIgnoreCase("/WEB-INF"))) {
                notFound((HttpServletResponse) response.getResponse());
                return null;
            }
        // START CR 6415120
        }
        // END CR 6415120

        // Wait if we are reloading
        boolean reloaded = false;
        while (((StandardContext) container).getPaused()) {
            reloaded = true;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // Reloading will have stopped the old webappclassloader and
        // created a new one
        if (reloaded &&
                context.getLoader() != null &&
                context.getLoader().getClassLoader() != null) {
            Thread.currentThread().setContextClassLoader(
                    context.getLoader().getClassLoader());
        }

        // Select the Wrapper to be used for this Request
        Wrapper wrapper = request.getWrapper();
        if (wrapper == null) {
            notFound((HttpServletResponse) response.getResponse());
            return null;
        } else if (wrapper.isUnavailable()) {
            // May be as a result of a reload, try and find the new wrapper
            wrapper = (Wrapper) container.findChild(wrapper.getName());
            if (wrapper == null) {
                notFound((HttpServletResponse) response.getResponse());
                return null;
            }
        }

        return wrapper;
    }
}
