/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.core;

import org.apache.catalina.Globals;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.SessionTracker;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.servlet.ServletRequest;

/**
 * This class exposes some of the functionality of
 * org.apache.catalina.connector.Request and
 * org.apache.catalina.connector.Response.
 *
 * It is in this package for purpose of package visibility
 * of methods.
 *
 * @author Shing Wai Chan
 */
public class RequestFacadeHelper {
    //use the same resource properties as in org.apache.catalina.connector.RequestFacade

    private Request request;

    private Response response;

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    public RequestFacadeHelper(Request request) {
        this.request = request;
        this.response = (Response)request.getResponse();
    }

    public static RequestFacadeHelper getInstance(ServletRequest srvRequest) {
        RequestFacadeHelper reqFacHelper =
           (RequestFacadeHelper) srvRequest.getAttribute(Globals.REQUEST_FACADE_HELPER);
        return reqFacHelper;
    }

    /**
     * Increment the depth of application dispatch
     */
    int incrementDispatchDepth() {
        validateRequest();
        return request.incrementDispatchDepth();
    }

    /**
     * Decrement the depth of application dispatch
     */
    int decrementDispatchDepth() {
        validateRequest();
        return request.decrementDispatchDepth();
    }

    /**
     * Check if the application dispatching has reached the maximum
     */
    boolean isMaxDispatchDepthReached() {
        validateRequest();
        return request.isMaxDispatchDepthReached();
    }

    void track(Session localSession) {
        validateRequest();
        SessionTracker sessionTracker = (SessionTracker)
            request.getNote(Globals.SESSION_TRACKER);
        if (sessionTracker != null) {
            sessionTracker.track(localSession);
        }
    }

    String getContextPath(boolean maskDefaultContextMapping) {
        validateRequest();
        return request.getContextPath(maskDefaultContextMapping);
    }

    void disableAsyncSupport() {
        validateRequest();
        request.disableAsyncSupport();
    }

    // --- for Response ---

    // START SJSAS 6374990
    boolean isResponseError() {
        validateResponse();
        return response.isError();
    }

    String getResponseMessage() {
        validateResponse();
        return response.getMessage();
    }

    int getResponseContentCount() {
        validateResponse();
        return response.getContentCount();
    }
    // END SJSAS 6374990

    void resetResponse() {
        validateResponse();
        response.setSuspended(false);
        response.setAppCommitted(false);
    }


    public void clear() {
        request = null;
        response = null;
    }

    private void validateRequest() {
        if (request == null) {
            throw new IllegalStateException(rb.getString(LogFacade.VALIDATE_REQUEST_EXCEPTION));
        }
    }

    private void validateResponse() {
        if (response == null) {
            throw new IllegalStateException(rb.getString(LogFacade.VALIDATE_RESPONSE_EXCEPTION));
        }
    }
}
