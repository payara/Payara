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

package com.sun.web.security;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;


class HttpResponseWrapper extends HttpServletResponseWrapper 
        implements HttpResponse {
        
    private HttpResponse httpResponse;         

    HttpResponseWrapper(HttpResponse response,
            HttpServletResponse servletResponse) {
        super(servletResponse);
        httpResponse = response;
    }
    
    // ----- HttpResponse Methods -----
    public String getHeader(String name) {
        return httpResponse.getHeader(name);
    }
    
    public Collection<String> getHeaderNames() {
        return httpResponse.getHeaderNames();
    }
    
    public Collection<String> getHeaders(String name) {
        return httpResponse.getHeaders(name);
    }

    public void addSessionCookieInternal(final Cookie cookie) {
        httpResponse.addSessionCookieInternal(cookie);
    }
    
    public String getMessage() {
        return httpResponse.getMessage();
    }

    public int getStatus() {
        return httpResponse.getStatus();
    }
    
    public void reset(int status, String message) {
        httpResponse.reset(status, message);
    }
    
    // ----- Response Methods -----
    public Connector getConnector() {
        return httpResponse.getConnector();
    }
    
    public void setConnector(Connector connector) {
        httpResponse.setConnector(connector);
    }
    
    public int getContentCount() {
        return httpResponse.getContentCount();
    }
    
    public Context getContext() {
        return httpResponse.getContext();
    }

    public void setContext(Context context) {
        httpResponse.setContext(context);
    }

    public void setAppCommitted(boolean appCommitted) {
        httpResponse.setAppCommitted(appCommitted);
    }

    public boolean isAppCommitted() {
        return httpResponse.isAppCommitted();
    }

    public boolean getIncluded() {
        return httpResponse.getIncluded();
    }
    
    public void setIncluded(boolean included) {
        httpResponse.setIncluded(included);
    }

    public String getInfo() {
        return httpResponse.getInfo();
    }

    public Request getRequest() {
        return httpResponse.getRequest();
    }

    public void setRequest(Request request) {
        httpResponse.setRequest(request);
    }

    public ServletResponse getResponse() {
        return super.getResponse();
    }
    
    public OutputStream getStream() {
        return httpResponse.getStream();
    }

    public void setStream(OutputStream stream) {
        httpResponse.setStream(stream);
    }

    public void setSuspended(boolean suspended) {
        httpResponse.setSuspended(suspended);
    }

    public boolean isSuspended() {
        return httpResponse.isSuspended();
    }

    public void setError() {
        httpResponse.setError();
    }

    public boolean isError() {
        return httpResponse.isError();
    }

    public void setDetailMessage(String message) {
        httpResponse.setDetailMessage(message);
    }

    public String getDetailMessage() {
        return httpResponse.getDetailMessage();
    }
    
    public ServletOutputStream createOutputStream() throws IOException {
        return httpResponse.createOutputStream();
    }

    public void finishResponse() throws IOException {
        httpResponse.finishResponse();
    }
    
    public int getContentLength() {
        return httpResponse.getContentLength();
    }
    
    /* Delegate to HttpServletResponse
      public String getContentType() {
      return httpResponse.getContentType();
      }
      */

    public PrintWriter getReporter() throws IOException {
        return httpResponse.getReporter();
    }

    public void recycle() {
        httpResponse.recycle();
    }

    /* Delegate to HttpServletResponse
       public void resetBuffer() {
       httpResponse.resetBuffer();
       }
       */
    
    public void resetBuffer(boolean resetWriterStreamFlags) {
        httpResponse.resetBuffer(resetWriterStreamFlags);
    }
    
    public void sendAcknowledgement() throws IOException {
        httpResponse.sendAcknowledgement();
    }

    public String encode(String url) {
        return httpResponse.encode(url);
    }
}
