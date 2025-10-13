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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package com.sun.web.security;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.*;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;

public class HttpResponseWrapper extends HttpServletResponseWrapper implements HttpResponse {

    private HttpResponse httpResponse;

    public HttpResponseWrapper(HttpResponse response, HttpServletResponse servletResponse) {
        super(servletResponse);
        httpResponse = response;
    }

    // ----- HttpResponse Methods -----
    @Override
    public String getHeader(String name) {
        return httpResponse.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return httpResponse.getHeaderNames();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return httpResponse.getHeaders(name);
    }

    @Override
    public void addSessionCookieInternal(final Cookie cookie) {
        httpResponse.addSessionCookieInternal(cookie);
    }

    @Override
    public String getMessage() {
        return httpResponse.getMessage();
    }

    @Override
    public int getStatus() {
        return httpResponse.getStatus();
    }

    @Override
    public void reset(int status, String message) {
        httpResponse.reset(status, message);
    }

    // ----- Response Methods -----
    @Override
    public Connector getConnector() {
        return httpResponse.getConnector();
    }

    @Override
    public void setConnector(Connector connector) {
        httpResponse.setConnector(connector);
    }

    @Override
    public int getContentCount() {
        return httpResponse.getContentCount();
    }

    @Override
    public Context getContext() {
        return httpResponse.getContext();
    }

    @Override
    public void setContext(Context context) {
        httpResponse.setContext(context);
    }

    @Override
    public void setAppCommitted(boolean appCommitted) {
        httpResponse.setAppCommitted(appCommitted);
    }

    @Override
    public boolean isAppCommitted() {
        return httpResponse.isAppCommitted();
    }

    @Override
    public boolean getIncluded() {
        return httpResponse.getIncluded();
    }

    @Override
    public void setIncluded(boolean included) {
        httpResponse.setIncluded(included);
    }

    @Override
    public String getInfo() {
        return httpResponse.getInfo();
    }

    @Override
    public Request getRequest() {
        return httpResponse.getRequest();
    }

    @Override
    public void setRequest(Request request) {
        httpResponse.setRequest(request);
    }

    @Override
    public ServletResponse getResponse() {
        return super.getResponse();
    }

    @Override
    public OutputStream getStream() {
        return httpResponse.getStream();
    }

    @Override
    public void setStream(OutputStream stream) {
        httpResponse.setStream(stream);
    }

    @Override
    public void setSuspended(boolean suspended) {
        httpResponse.setSuspended(suspended);
    }

    @Override
    public boolean isSuspended() {
        return httpResponse.isSuspended();
    }

    @Override
    public void setError() {
        httpResponse.setError();
    }

    @Override
    public boolean isError() {
        return httpResponse.isError();
    }

    @Override
    public void setDetailMessage(String message) {
        httpResponse.setDetailMessage(message);
    }

    @Override
    public String getDetailMessage() {
        return httpResponse.getDetailMessage();
    }

    @Override
    public ServletOutputStream createOutputStream() throws IOException {
        return httpResponse.createOutputStream();
    }

    @Override
    public void finishResponse() throws IOException {
        httpResponse.finishResponse();
    }

    @Override
    public int getContentLength() {
        return httpResponse.getContentLength();
    }

    /*
     * Delegate to HttpServletResponse public String getContentType() { return httpResponse.getContentType(); }
     */

    @Override
    public PrintWriter getReporter() throws IOException {
        return httpResponse.getReporter();
    }

    @Override
    public void recycle() {
        httpResponse.recycle();
    }

    /*
     * Delegate to HttpServletResponse public void resetBuffer() { httpResponse.resetBuffer(); }
     */

    @Override
    public void resetBuffer(boolean resetWriterStreamFlags) {
        httpResponse.resetBuffer(resetWriterStreamFlags);
    }

    @Override
    public void sendAcknowledgement() throws IOException {
        httpResponse.sendAcknowledgement();
    }

    @Override
    public String encode(String url) {
        return httpResponse.encode(url);
    }
}
