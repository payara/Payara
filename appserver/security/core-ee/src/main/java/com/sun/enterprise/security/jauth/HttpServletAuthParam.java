/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.jauth;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An HTTP Servlet authentication parameter that encapsulates
 * HTTP Servlet request and response objects.
 *
 * <p> HttpServletAuthParam may be created with null request or response
 * objects.  The following table describes when it is appropriate to pass null:
 *
 * <pre>
 *                                        Request   Response
 *                                        -------   --------
 *
 * ClientAuthModule.secureRequest         non-null  null
 * ClientAuthModule.validateResponse      null      non-null
 *
 * ServerAuthModule.validateRequest       non-null  null
 * ServerAuthModule.secureResponse        null      non-null
 * </pre>
 *
 * <p> As noted above, in the case of
 * <code>ServerAuthModule.validateRequest</code> the module receives
 * a null response object.  If the implementation of
 * <code>validateRequest</code> encounters an authentication error,
 * it may construct the appropriate response object itself and set it
 * into the HttpServletAuthParam via the <code>setResponse</code> method.
 *
 * @version %I%, %G%
 */
public class HttpServletAuthParam implements AuthParam {

    private HttpServletRequest request;
    private HttpServletResponse response;
    //private static final MessageLayer layer =
    //      new MessageLayer(MessageLayer.HTTP_SERVLET);
    
    /**
     * Create an HttpServletAuthParam with HTTP request and response objects.
     *
     * @param request the HTTP Servlet request object, or null.
     * @param response the HTTP Servlet response object, or null.
     */
    public HttpServletAuthParam(HttpServletRequest request,
				HttpServletResponse response) {
	this.request = request;
	this.response = response;
    }

    /**
     * Create an HttpServletAuthParam with MessageInfo object.
     * @param messageInfo
     *
     */
    public HttpServletAuthParam(MessageInfo messageInfo) {
        this.request = (HttpServletRequest)messageInfo.getRequestMessage();
        this.response = (HttpServletResponse)messageInfo.getResponseMessage();
    }

    /**
     * Get the HTTP Servlet request object.
     *
     * @return the HTTP Servlet request object, or null.
     */
    public HttpServletRequest getRequest() {
	return this.request;
    }

    /**
     * Get the HTTP Servlet response object.
     *
     * @return the HTTP Servlet response object, or null.
     */
    public HttpServletResponse getResponse() {
	return this.response;
    }

    /**
     * Set a new HTTP Servlet response object.
     *
     * <p> If a response has already been set (it is non-null),
     * this method returns.  The original response is not overwritten.
     *
     * @param response the HTTP Servlet response object.
     *
     * @exception IllegalArgumentException if the specified response is null.
     */
    public void setResponse(HttpServletResponse response) {
	if (response == null) {
	    throw new IllegalArgumentException("invalid null response");
	}

	if (this.response == null) {
	    this.response = response;
	}
    }

    /**
     * Get a MessageLayer instance that identifies HttpServlet
     * as the message layer.
     *
     * @return a MessageLayer instance that identifies HttpServlet
     *          as the message layer.
     */
    //public MessageLayer getMessageLayer() {
    //    return layer;
    //};

    /**
     * Get the operation related to the encapsulated HTTP Servlet
     * request and response objects.
     *
     * @return the operation related to the encapsulated request and response
     *          objects, or null.
     */
    public String getOperation() {
	return null;
    }
}
