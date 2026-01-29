/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDLGPL_1_1.html
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
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]

package com.sun.web.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Request;
import org.glassfish.grizzly.http.util.DataChunk;

public class HttpRequestWrapper implements HttpRequest, ServletRequest {

    private final Request httpRequest;
    private final HttpServletRequest servletRequest;
    private final boolean isDefaultContext;
    private ServletRequest maskedFacade;
    private ServletRequest facade;

    public HttpRequestWrapper(HttpRequest request, HttpServletRequest servletRequest) {
        httpRequest = (Request) request;
        this.servletRequest = servletRequest;
        isDefaultContext = httpRequest.getMappingData().isDefaultContext;
    }

    // ----- HttpRequest Methods -----
    @Override
    public void addCookie(Cookie cookie) {
        httpRequest.addCookie(cookie);
    }

    @Override
    public void addHeader(String name, String value) {
        httpRequest.addHeader(name, value);
    }

    @Override
    public void addLocale(Locale locale) {
        httpRequest.addLocale(locale);
    }

    @Override
    public void addParameter(String name, String values[]) {
        httpRequest.addParameter(name, values);
    }

    @Override
    public void clearCookies() {
        httpRequest.clearCookies();
    }

    @Override
    public void clearHeaders() {
        httpRequest.clearHeaders();
    }

    @Override
    public void clearLocales() {
        httpRequest.clearLocales();
    }

    @Override
    public void clearParameters() {
        httpRequest.clearParameters();
    }

    @Override
    public void replayPayload(byte[] payloadByteArray) {
        httpRequest.replayPayload(payloadByteArray);
    }

    @Override
    public void setAuthType(String type) {
        httpRequest.setAuthType(type);
    }

    @Override
    public void setMethod(String method) {
        httpRequest.setMethod(method);
    }

    @Override
    public void setQueryString(String query) {
        httpRequest.setQueryString(query);
    }

    @Override
    public Session getSessionInternal(boolean create) {
        return httpRequest.getSessionInternal(create);
    }

    @Override
    public String changeSessionId() {
        return httpRequest.changeSessionId();
    }

    @Override
    public void setPathInfo(String path) {
        httpRequest.setPathInfo(path);
    }

    @Override
    public DataChunk getRequestPathMB() {
        return httpRequest.getRequestPathMB();
    }

    @Override
    public void setRequestedSessionCookie(boolean flag) {
        httpRequest.setRequestedSessionCookie(flag);
    }

    @Override
    public void setRequestedSessionCookiePath(String cookiePath) {
        httpRequest.setRequestedSessionCookiePath(cookiePath);
    }

    @Override
    public void setRequestedSessionId(String id) {
        httpRequest.setRequestedSessionId(id);
    }

    @Override
    public void setRequestedSessionURL(boolean flag) {
        httpRequest.setRequestedSessionURL(flag);
    }

    @Override
    public void setRequestURI(String uri) {
        httpRequest.setRequestURI(uri);
    }

    @Override
    public String getDecodedRequestURI() {
        return httpRequest.getDecodedRequestURI();
    }

    @Override
    public void setServletPath(String path) {
        httpRequest.setServletPath(path);
    }

    @Override
    public void setUserPrincipal(Principal principal) {
        httpRequest.setUserPrincipal(principal);
    }

    // ----- Request Methods -----
    @Override
    public String getAuthorization() {
        return httpRequest.getAuthorization();
    }

    @Override
    public Connector getConnector() {
        return httpRequest.getConnector();
    }

    @Override
    public void setConnector(Connector connector) {
        httpRequest.setConnector(connector);
    }

    @Override
    public Context getContext() {
        return httpRequest.getContext();
    }

    @Override
    public void setContext(Context context) {
        httpRequest.setContext(context);
    }

    @Override
    public FilterChain getFilterChain() {
        return httpRequest.getFilterChain();
    }

    @Override
    public void setFilterChain(FilterChain filterChain) {
        httpRequest.setFilterChain(filterChain);
    }

    @Override
    public Host getHost() {
        return httpRequest.getHost();
    }

    @Override
    public void setHost(Host host) {
        httpRequest.setHost(host);
    }

    @Override
    public String getInfo() {
        return httpRequest.getInfo();
    }

    @Override
    public ServletRequest getRequest() {
        return getRequest(false);
    }

    @Override
    public ServletRequest getRequest(boolean maskDefaultContextMapping) {
        ServletRequest rvalue;
        boolean getMasked = maskDefaultContextMapping && isDefaultContext;
        rvalue = getMasked ? maskedFacade : facade;
        if (rvalue == null) {
            rvalue = new RequestFacadeWrapper(httpRequest, servletRequest, getMasked);
            if (getMasked) {
                maskedFacade = rvalue;
            } else {
                facade = rvalue;
            }
        }
        return rvalue;
    }

    @Override
    public Response getResponse() {
        return httpRequest.getResponse();
    }

    @Override
    public void setResponse(Response response) {
        httpRequest.setResponse(response);
    }

    @Override
    public Socket getSocket() {
        return httpRequest.getSocket();
    }

    @Override
    public void setSocket(Socket socket) {
        httpRequest.setSocket(socket);
    }

    @Override
    public InputStream getStream() {
        return httpRequest.getStream();
    }

    @Override
    public void setStream(InputStream stream) {
        httpRequest.setStream(stream);
    }

    @Override
    public Wrapper getWrapper() {
        return httpRequest.getWrapper();
    }

    @Override
    public void setWrapper(Wrapper wrapper) {
        httpRequest.setWrapper(wrapper);
    }

    @Override
    public ServletInputStream createInputStream() throws IOException {
        return httpRequest.createInputStream();
    }

    @Override
    public void finishRequest() throws IOException {
        httpRequest.finishRequest();
    }

    @Override
    public Object getNote(String name) {
        return httpRequest.getNote(name);
    }

    @Override
    public Iterator getNoteNames() {
        return httpRequest.getNoteNames();
    }

    @Override
    public void recycle() {
        httpRequest.recycle();
    }

    @Override
    public void removeNote(String name) {
        httpRequest.removeNote(name);
    }

    @Override
    public void setContentLength(int length) {
        httpRequest.setContentLength(length);
    }

    @Override
    public void setContentType(String type) {
        httpRequest.setContentType(type);
    }

    @Override
    public void setNote(String name, Object value) {
        httpRequest.setNote(name, value);
    }

    @Override
    public void setProtocol(String protocol) {
        httpRequest.setProtocol(protocol);
    }

    @Override
    public void setRemoteAddr(String remote) {
        httpRequest.setRemoteAddr(remote);
    }

    @Override
    public void setSecure(boolean secure) {
        httpRequest.setSecure(secure);
    }

    @Override
    public void setServerName(String name) {
        httpRequest.setServerName(name);
    }

    @Override
    public void setServerPort(int port) {
        httpRequest.setServerPort(port);
    }

    @Override
    public void setCheckRestrictedResources(boolean check) {
        httpRequest.setCheckRestrictedResources(check);
    }

    @Override
    public boolean getCheckRestrictedResources() {
        return httpRequest.getCheckRestrictedResources();
    }

    @Override
    public String getJrouteId() {
        return httpRequest.getJrouteId();
    }

    /**
     * Generate and return a new session ID.
     *
     * This hook allows connectors to provide their own scalable session ID generators.
     */
    @Override
    public String generateSessionId() {
        return httpRequest.generateSessionId();
    }

    /**
     * Disables async support on this request.
     */
    @Override
    public void disableAsyncSupport() {
        httpRequest.disableAsyncSupport();
    }

    @Override
    public Session lockSession() {
        return httpRequest.lockSession();
    }

    @Override
    public void unlockSession() {
        httpRequest.unlockSession();
    }

    // ----- ServletRequest Methods -----
    // implementation of ServletRequest interface
    @Override
    public Object getAttribute(String name) {
        return servletRequest.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return servletRequest.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return servletRequest.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        servletRequest.setCharacterEncoding(env);
    }

    @Override
    public int getContentLength() {
        return servletRequest.getContentLength();
    }

    @Override
    public String getContentType() {
        return servletRequest.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return servletRequest.getInputStream();
    }

    @Override
    public String getParameter(String name) {
        return servletRequest.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return servletRequest.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return servletRequest.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return servletRequest.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return servletRequest.getProtocol();
    }

    @Override
    public String getScheme() {
        return servletRequest.getScheme();
    }

    @Override
    public String getServerName() {
        return servletRequest.getServerName();
    }

    @Override
    public int getServerPort() {
        return servletRequest.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return servletRequest.getReader();
    }

    @Override
    public String getRemoteAddr() {
        return servletRequest.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return servletRequest.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
        servletRequest.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        servletRequest.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return servletRequest.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return servletRequest.getLocales();
    }

    @Override
    public boolean isSecure() {
        return servletRequest.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return servletRequest.getRequestDispatcher(path);
    }

    @Override
    public int getRemotePort() {
        return servletRequest.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return servletRequest.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return servletRequest.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return servletRequest.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return servletRequest.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return servletRequest.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest sRequest, ServletResponse sResponse) throws IllegalStateException {
        return servletRequest.startAsync(sRequest, sResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return servletRequest.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return servletRequest.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return servletRequest.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return servletRequest.getDispatcherType();
    }

    @Override
    public long getContentLengthLong() {
        return servletRequest.getContentLengthLong();
    }

    @Override
    public String getRequestId() {
        return servletRequest.getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return servletRequest.getProtocolRequestId();
    }

    @Override
    public ServletConnection getServletConnection() {
        return servletRequest.getServletConnection();
    }

}
