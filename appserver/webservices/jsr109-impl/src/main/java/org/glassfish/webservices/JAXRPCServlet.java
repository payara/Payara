/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package org.glassfish.webservices;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Loader;
import org.glassfish.webservices.monitoring.Endpoint;
import org.glassfish.webservices.monitoring.HttpResponseInfoImpl;
import org.glassfish.webservices.monitoring.ThreadLocalInfo;
import org.glassfish.webservices.monitoring.WebServiceEngineImpl;
import org.glassfish.webservices.monitoring.WebServiceTesterServlet;

import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import com.sun.xml.rpc.spi.runtime.ServletDelegate;

/**
 * The JAX-RPC dispatcher servlet.
 *
 */
public class JAXRPCServlet extends HttpServlet {

    private static final long serialVersionUID = 5486495210696942602L;

    private volatile ServletDelegate delegate;
    private volatile ServletWebServiceDelegate myDelegate;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        try {
            super.init(servletConfig);

            delegate = JaxRpcObjectFactory.newInstance().createServletDelegate();
            myDelegate = new ServletWebServiceDelegate(delegate);

            delegate.setSecondDelegate(myDelegate);
            delegate.init(servletConfig);
        } catch (ServletException e) {
            throw e;
        } catch (Throwable e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        WebServiceEngineImpl wsEngine = WebServiceEngineImpl.getInstance();

        if ("Tester".equalsIgnoreCase(request.getQueryString())) {
            Endpoint endpt = wsEngine.getEndpoint(request.getServletPath());
            if (endpt != null && Boolean.parseBoolean(endpt.getDescriptor().getDebugging())) {
                WebServiceTesterServlet.invoke(request, response, endpt.getDescriptor());
                return;
            }
        }

        if (delegate != null) {
            // check if we need to trace this...
            String messageId = null;
            if (wsEngine.getGlobalMessageListener() != null) {
                Endpoint endpt = wsEngine.getEndpoint(request.getServletPath());
                messageId = wsEngine.preProcessRequest(endpt);
                if (messageId != null) {
                    ThreadLocalInfo config = new ThreadLocalInfo(messageId, request);
                    wsEngine.getThreadLocal().set(config);
                }
            }

            delegate.doPost(request, response);

            if (messageId != null) {
                wsEngine.postProcessResponse(messageId, new HttpResponseInfoImpl(response));
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        // Test for tester servlet invocation.
        if ("Tester".equalsIgnoreCase(request.getQueryString())) {

            Endpoint endpt = WebServiceEngineImpl.getInstance().getEndpoint(request.getServletPath());
            if (endpt != null && Boolean.parseBoolean(endpt.getDescriptor().getDebugging())) {
                Loader loader = (Loader) endpt.getDescriptor().getBundleDescriptor().getExtraAttribute("WEBLOADER");
                if (loader != null) {
                    endpt.getDescriptor().getBundleDescriptor().setClassLoader(loader.getClassLoader());
                    endpt.getDescriptor().getBundleDescriptor().removeExtraAttribute("WEBLOADER");
                }
                WebServiceTesterServlet.invoke(request, response, endpt.getDescriptor());
                return;
            }
        }

        if (delegate != null) {
            delegate.doGet(request, response);
        }
    }

    @Override
    public void destroy() {
        if (delegate != null) {
            delegate.destroy();
        }
        if (myDelegate != null) {
            myDelegate.destroy();
        }
    }
}
